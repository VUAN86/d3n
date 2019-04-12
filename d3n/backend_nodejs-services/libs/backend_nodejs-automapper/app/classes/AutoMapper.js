var _ = require('lodash');
var JsonSchemas = require('nodejs-protocol-schemas');
var logger = require('nodejs-logger')();

function AutoMapper(config) {
    this._config = config;
}

AutoMapper.getInstance = function (config) {
    return new AutoMapper(config);
};

AutoMapper.prototype.TYPE_OBJECT = 'object';
AutoMapper.prototype.TYPE_OBJECT_LIST = 'objectList';
AutoMapper.prototype.TYPE_ATTRIBUTE_LIST = 'attributeList';

AutoMapper.prototype.map = function (source, destination, defined) {
    var _source, _destination, _defined = defined;
    var destKeys = _.keys(destination), functions;
    _.extend(destination, _.pick(source, destKeys));
    _source = source;
    _destination = destination;

    functions = {
        forMember: function (sourceKey, desKey, options) {
            var keys = sourceKey.split('.');
            var sourceValue = _source;
            var index = 0;
            var destinationKey = desKey ? desKey : sourceKey;
            var transferValue = false;
            if (!options) {
                options = {
                    type: null,
                    format: null,
                    isOptional: true,
                    isConstraint: false
                };
            }

            // incase sourceKey is a nested object like objectA.Value
            if (keys.length > 1) {
                while (index < keys.length) {
                    sourceValue = sourceValue[keys[index]];
                    index++;
                }
            } else {
                sourceValue = sourceValue[sourceKey];
            }
            // Check empty strings, 0 values for constraint fields to avoid constraint violation errors
            if (!_.isUndefined(sourceValue) && !_.isNull(sourceValue) && options.isConstraint &&
                ((options.type === 'string' && sourceValue === '') || (options.type === 'integer' && sourceValue === 0))) {
                sourceValue = null;
            }
            if (!_defined || !_.isUndefined(sourceValue)) {
                transferValue = true;
            }
            if (_.isNull(sourceValue) && (!_.isUndefined(options.isOptional) || options.isOptional === false)) {
                transferValue = false;
            }
            if (transferValue) {
                // Swagger date/date-time support:
                // Validate date/date-time value against ISO date format, if not valid set to null, if Date from DB -> convert to ISO date string
                if (options.type === 'string' && _.isString(options.format) && options.format.startsWith('date')) {
                    try {
                        if (_.isDate(sourceValue)) {
                            // DB ->
                            var isoDate = sourceValue.toISOString();
                            if (options.format === 'date') {
                                sourceValue = isoDate.substring(0, 10);
                            } else if (options.format === 'date-time') {
                                sourceValue = isoDate.substring(0, 19);
                            }
                        } else if (_.isString(sourceValue)) {
                            // -> DB
                            var isoDate = (new Date(sourceValue)).toISOString();
                            var sourceDate = sourceValue;
                            if (options.format === 'date') {
                                isoDate = isoDate.substring(0, 10);
                                sourceDate = sourceValue.substring(0, 10);
                            } else if (options.format === 'date-time') {
                                isoDate = isoDate.substring(0, 19);
                                sourceDate = sourceValue.substring(0, 19);
                            }
                            if (isoDate !== sourceDate) {
                                sourceValue = null;
                            }
                        }
                    } catch (ex) {
                        sourceValue = null;
                    }
                    //} else if (!_.isNull(sourceValue) && options.type === 'boolean') {
                    //    // Swagger boolean support:
                    //    // Convert integer (0/1) to boolean (false/true) and vice versa
                    //    if (_.isBoolean(sourceValue)) {
                    //        // -> DB: convert to int[0/1]
                    //        sourceValue = sourceValue === true ? 1 : 0;
                    //    } else if (_.isInteger(sourceValue)) {
                    //        // DB ->: convert to boolean
                    //        sourceValue = sourceValue === 1;
                    //    }
                }
                // save array of items as plain array
                if (_.has(sourceValue, 'items')) {
                    _destination[destinationKey] = sourceValue.items;
                } else if (options.type === 'string' && !_.isUndefined(sourceValue) && !_.isNull(sourceValue)) {
                    if (_.isObject(sourceValue)) {
                        _destination[destinationKey] = JSON.stringify(sourceValue);
                    } else {
                        _destination[destinationKey] = sourceValue.toString();
                    }
                } else {
                    _destination[destinationKey] = sourceValue;
                }
            }
            return functions;
        },
        toString: function (sourceKey, desKey) {
            var destinationKey = desKey ? desKey : sourceKey;
            if (_destination[destinationKey]) {
                if (_.isArray(_destination[destinationKey])) {
                    var stringifiedDestination = [];
                    _.forEach(_destination[destinationKey], function (destination) {
                        stringifiedDestination.push(destination.toString());
                    });
                    _destination[destinationKey] = stringifiedDestination;
                } else {
                    _destination[destinationKey] = _destination[destinationKey].toString();
                }
            };
            return functions;
        },
        toLowerCase: function (sourceKey, desKey) {
            var destinationKey = desKey ? desKey : sourceKey;
            if (_destination[destinationKey]) {
                if (_.isArray(_destination[destinationKey])) {
                    var stringifiedDestination = [];
                    _.forEach(_destination[destinationKey], function (destination) {
                        stringifiedDestination.push(destination.toLowerCase());
                    });
                    _destination[destinationKey] = stringifiedDestination;
                } else {
                    _destination[destinationKey] = _destination[destinationKey].toLowerCase();
                }
            };
            return functions;
        },
        toUpperCase: function (sourceKey, desKey) {
            var destinationKey = desKey ? desKey : sourceKey;
            if (_destination[destinationKey]) {
                if (_.isArray(_destination[destinationKey])) {
                    var stringifiedDestination = [];
                    _.forEach(_destination[destinationKey], function (destination) {
                        stringifiedDestination.push(destination.toUpperCase());
                    });
                    _destination[destinationKey] = stringifiedDestination;
                } else {
                    _destination[destinationKey] = _destination[destinationKey].toUpperCase();
                }
            };
            return functions;
        },
        value: function (destinationKey, value) {
            _destination[destinationKey] = value;
            return functions;
        }
    };
    return functions;
};

AutoMapper.prototype.mapDefined = function (source, destination) {
    var self = this;
    return self.map(source, destination, true);
};

AutoMapper.prototype.mapList = function (listSource, listDestination, defined) {
    var self = this;
    var _defined = defined;
    _.each(listDestination, function (destination, i) {
        var source = listSource[i];
        self.map(source, destination, _defined);
    });

    functions = {
        forMember: function (sourceKey, desKey, options) {
            var destinationKey = desKey ? desKey : sourceKey;
            _.each(listDestination, function (destination, i) {
                var source = listSource[i];
                self.map(source, destination, _defined).forMember(sourceKey, destinationKey, options);
            });

            return functions;
        }
    };
    return functions;
};

AutoMapper.prototype.mapListDefined = function (listSource, listDestination) {
    var self = this;
    return self.mapList(listSource, listDestination, true);
};

AutoMapper.prototype.mapBySchema = function (schemaModel, mapping, instance, scalar, defined) {
    var self = this;
    try {
        var totals = {
            total: 1
        };
        var instanceItems = instance;
        if (_.has(instance, 'rows') && _.has(instance, 'count')) {
            instanceItems = instance.rows;
            totals.total = instance.count;
        } else if (_.isArray(instance)) {
            totals.total = instance.length;
        }

        var source = instanceItems;
        if (_.has(instanceItems, 'dataValues')) {
            source = instanceItems.get({ plain: true });
        }

        _.forEach(instanceItems, function (field, key) {
            if (_.isEqual(key, 'properties')) {
                _.assign(source, field);
            }
            if (_.has(field, 'dataValues')) {
                source[key] = field.get({ plain: true });
            }

            var fieldArray = field;
            if (_.has(field, 'rows') && _.has(field, 'count')) {
                fieldArray = field.rows;
                totals[key] = field.count;
            } else if (_.isArray(field)) {
                totals[key] = field.length;
            }

            if (_.isArray(fieldArray)) {
                source[key] = [];
                _.forEach(fieldArray, function (arrayField) {
                    if (_.has(arrayField, 'dataValues')) {
                        source[key].push(arrayField.get({ plain: true }));
                    } else {
                        source[key].push(arrayField);
                    }
                })
            }
        });

        var destination = {};
        // Stringify BLOB fields
        _.forEach(source, function (field, key) {
            if (field instanceof Uint8Array) {
                source[key] = (new Buffer(field)).toString();
            }
        });
        // Get schema from schemas definitions
        var rootModel = _.find(mapping[schemaModel], { destination: '$root' });
        var namespace = undefined;
        if (rootModel && _.has(rootModel, 'model')) {
            namespace = rootModel.model._namespace;
        }
        var schema = _findSchemaDefinition(namespace, schemaModel);
        if (!schema) {
            schema = _findSchemaDefinition(undefined, schemaModel);
        }
        if (!schema) {
            logger.error('AutoMapper.mapBySchema: no schema definition found to map against', schemaModel);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        // Collect scalar/object/list properties
        var properties = { scalars: [], objects: [], lists: [] };
        var scalarTypeList = ['integer', 'string', 'long', 'float', 'double', 'boolean', 'number'];
        _.forEach(schema.properties, function (fieldMeta, fieldName) {
            // If fieldMeta.type is defined as array such as ['integer', 'null'], will consider it as scalar value of type integer
            if (_.isArray(fieldMeta.type) && _.intersection(scalarTypeList, fieldMeta.type).length > 0) {
                properties.scalars.push({ field: fieldName, type: _.intersection(scalarTypeList, fieldMeta.type)[0], format: fieldMeta.format });
            } else if (scalarTypeList.indexOf(fieldMeta.type) !== -1) {
                properties.scalars.push({ field: fieldName, type: fieldMeta.type, format: fieldMeta.format });
            } else if (_.isUndefined(fieldMeta.type) || fieldMeta.type === 'object' || fieldMeta.type === 'array' ||
                (_.isArray(fieldMeta.type) && _.intersection(['object', 'array', 'null'], fieldMeta.type).length === 2)) {
                var fieldType = fieldMeta.type;
                if (_.isArray(fieldMeta.type) && _.intersection(['object', 'array', 'null'], fieldMeta.type).length === 2) {
                    fieldType = _.intersection(['object', 'array'], fieldMeta.type)[0];
                }
                var objectMapping = _.find(mapping[schemaModel], { destination: fieldName });
                if (!_.isUndefined(fieldMeta.format) && fieldMeta.format === 'json-string') {
                    // Objects that we store as scalars do not need to be mapped
                    properties.scalars.push({ field: fieldName, type: fieldType, format: fieldMeta.format });
                } else if (_.isUndefined(objectMapping)) {
                    // Treat to map objects without mapping
                    properties.scalars.push({ field: fieldName, type: fieldType, format: fieldMeta.format });
                } else if (!_.isUndefined(objectMapping) && objectMapping.type === self.TYPE_OBJECT) {
                    // Treat to map objects with mapping
                    properties.objects.push(fieldName);
                } else {
                    // Treat to map objects as lists with mapping
                    properties.lists.push(fieldName);
                }
            }
        });
        // Gather scalar values
        var mapper = self.map(source, destination, defined);
        _.forEach(properties.scalars, function (scalar) {
            // Detect if field is constraint
            var isConstraint = false;
            if (rootModel && _.has(rootModel, 'model')) {
                isConstraint = self.checkConstraint(rootModel.model, scalar.field);
            }
            // Transfer scalar value
            mapper.forMember(scalar.field, scalar.field, { type: scalar.type, format: scalar.format, isConstraint: isConstraint });
        });
        if (scalar) {
            return destination;
        }
        // Gather object values
        _.forEach(properties.objects, function (object) {
            var objectMapping = _.find(mapping[schemaModel], { destination: object });
            var objectProperties = schema.properties[object].properties;
            destination[object] = {};
            mapper = self.map(source[object], destination[object], defined);
            _.forEach(_.keys(objectProperties), function (property, index) {
                var propertySource = property;
                var propertyDestination = property;
                if (objectMapping.attributes) {
                    var propertyMapper = _.find(objectMapping.attributes, property);
                    if (propertyMapper && propertyMapper[property]) {
                        propertySource = propertyMapper[property].field;
                    }
                }
                mapper.forMember(propertySource, propertyDestination);
            });
        });
        // Gather list values
        _.forEach(properties.lists, function (object) {
            var objectMapping = _.find(mapping[schemaModel], { destination: object });
            if (!objectMapping.skip) {
                var objectMappingAttribute = objectMapping.attribute;
                var objectMappingAttributes = _.transform(objectMapping.attributes, _.ary(_.extend, 2), {});
                // Get source records by model plural name
                var sourceRecords = undefined;
                var totalRecords = 0;
                if (_.has(objectMapping, 'alias') && _.has(source, objectMapping.alias)) {
                    sourceRecords = source[objectMapping.alias];
                    totalRecords = totals[objectMapping.alias];
                } else {
                    var mappingModels = [];
                    if (_.has(objectMapping, 'hasModel')) {
                        mappingModels = [objectMapping.hasModel];
                    } else if (_.has(objectMapping, 'model')) {
                        mappingModels = [objectMapping.model];
                    } else if (_.has(objectMapping, 'models')) {
                        mappingModels = objectMapping.models;
                    };
                    // Merge records from multiple sources
                    var multipleSourceRecords = [];
                    _.forEach(mappingModels, function (mappingModel) {
                        if (_.has(source, mappingModel.name)) {
                            if (_.has(objectMapping, 'hasModel')) {
                                var items = _.map(source[mappingModel.name], objectMapping.model.name);
                                multipleSourceRecords = _.union(items, multipleSourceRecords);
                            } else {
                                multipleSourceRecords = _.union(source[mappingModel.name], multipleSourceRecords);
                            }
                            totalRecords += totals[mappingModel.name];
                        } else if (_.has(source, mappingModel.name + 's')) {
                            if (_.has(objectMapping, 'hasModel')) {
                                var items = _.map(source[mappingModel.name + 's'], objectMapping.model.name);
                                multipleSourceRecords = _.union(items, multipleSourceRecords);
                            } else {
                                multipleSourceRecords = _.union(source[mappingModel.name + 's'], multipleSourceRecords);
                            }
                            totalRecords += totals[mappingModel.name + 's'];
                        }
                    });
                    sourceRecords = multipleSourceRecords;
                }
                // Transfer data
                if (sourceRecords) {
                    var definition = schema.properties[object].properties;
                    var definitionType = schema.properties[object].type;
                    var limit = _.has(objectMapping, 'limit') ? objectMapping.limit : self._config.limit;
                    destination[object] = {};
                    if (_.has(definition, 'limit')) {
                        destination[object].limit = limit;
                    }
                    if (_.has(definition, 'offset')) {
                        destination[object].offset = 0;
                    }
                    if (_.has(definition, 'total')) {
                        destination[object].total = totalRecords;
                    }
                    if (_.has(definition, 'items') && definition.items.type === 'array') {
                        // array of objectList
                        var arrayDefinition = definition.items.items;
                        destination[object].items = [];
                        _.forEach(sourceRecords, function (sourceRecord, key) {
                            var item = {};
                            if (objectMappingAttribute && _.isEmpty(objectMappingAttributes)) {
                                item = sourceRecord[objectMappingAttribute.field];
                            } else {
                                if (arrayDefinition.type === 'object') {
                                    var itemMapper = self.map(sourceRecord, item, defined);
                                    _.forEach(arrayDefinition.properties, function (options, property) {
                                        if (_.has(objectMappingAttributes, property)) {
                                            itemMapper.forMember(objectMappingAttributes[property].field, property);
                                        } else {
                                            itemMapper.forMember(property);
                                        }
                                    });
                                } else {
                                    for (var attr = 0; attr < objectMapping.attributes.length; attr++) {
                                        var property = objectMapping.attributes[attr];
                                        item = sourceRecord[_.values(property)[0].field];
                                    };
                                }
                            }
                            destination[object].items.push(item);
                            // Limit to default since Sequelize nested include doesn't contains limitation
                            if (_.has(definition, 'limit') && (key + 1) === limit && limit > 0) {
                                return false;
                            }
                        });
                        if (objectMapping.order !== false) {
                            destination[object].items = destination[object].items.sort();
                        }
                    } else if (definitionType === 'array' && _.isUndefined(definition)) {
                        // array of scalar/object
                        var arrayDefinition = schema.properties[object].items;
                        destination[object] = [];
                        _.forEach(sourceRecords, function (sourceRecord, key) {
                            var item = {};
                            if (objectMappingAttribute && _.isEmpty(objectMappingAttributes)) {
                                item = sourceRecord[objectMappingAttribute.field];
                            } else {
                                if (arrayDefinition.type === 'object') {
                                    var itemMapper = self.map(sourceRecord, item, defined);
                                    _.forEach(arrayDefinition.properties, function (options, property) {
                                        if (_.has(objectMappingAttributes, property)) {
                                            itemMapper.forMember(objectMappingAttributes[property].field, property);
                                        } else {
                                            itemMapper.forMember(property);
                                        }
                                    });
                                } else {
                                    for (var attr = 0; attr < objectMapping.attributes.length; attr++) {
                                        var property = objectMapping.attributes[attr];
                                        item = sourceRecord[_.values(property)[0].field];
                                    };
                                }
                            }
                            destination[object].push(item);
                        });
                        if (objectMapping.order !== false) {
                            destination[object] = destination[object].sort();
                        }
                    }
                }
            }
        });
        return destination;
    } catch (ex) {
        logger.error('AutoMapper.mapBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.mapDefinedBySchema = function (schemaModel, mapping, instance, scalar) {
    var self = this;
    return self.mapBySchema(schemaModel, mapping, instance, scalar, true);
};

AutoMapper.prototype.mapListBySchema = function (schemaModel, mapping, instances, scalar, defined) {
    var self = this;
    try {
        var destinationItems = [];
        _.forEach(instances, function (sourceItem, key) {
            var destinationItem = self.mapBySchema(schemaModel, mapping, sourceItem, scalar, defined);
            if (destinationItem) {
                destinationItems.push(destinationItem);
            }
        });
        return destinationItems;
    } catch (ex) {
        logger.error('AutoMapper.mapListBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.mapListDefinedBySchema = function (schemaModel, mapping, instances, scalar) {
    var self = this;
    return self.mapListBySchema(schemaModel, mapping, instances, scalar, true);
};

AutoMapper.prototype.mapSimpleBySchema = function (schemaModel, instance, defined) {
    var self = this;
    try {
        var instanceItems = instance;
        if (_.has(instance, 'rows') && _.has(instance, 'count')) {
            instanceItems = instance.rows;
        }
        var source = instanceItems;
        if (_.has(instanceItems, 'dataValues')) {
            source = instanceItems.get({ plain: true });
        }

        _.forEach(source, function (field, key) {
            if (_.isEqual(key, 'properties')) {
                _.assign(source, field);
            }
            if (_.has(field, 'dataValues')) {
                source[key] = field.get({ plain: true });
            }

            var fieldArray = field;
            if (_.has(field, 'rows') && _.has(field, 'count')) {
                fieldArray = field.rows;
            }

            if (_.isArray(fieldArray)) {
                source[key] = [];
                _.forEach(fieldArray, function (arrayField) {
                    if (_.has(arrayField, 'dataValues')) {
                        source[key].push(arrayField.get({ plain: true }));
                    } else {
                        source[key].push(arrayField);
                    }
                })
            }
        });

        var destination = {};
        // Stringify BLOB fields
        _.forEach(source, function (field, key) {
            if (field instanceof Uint8Array) {
                source[key] = (new Buffer(field)).toString();
            }
        });
        // Get schema from schemas definitions
        var schema = _findSchemaDefinition(undefined, schemaModel);
        if (!schema) {
            logger.error('AutoMapper.mapSimpleBySchema: no schema definition found to map against', schemaModel);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        // Collect scalar/object/list properties
        var properties = { scalars: [], objects: [], lists: [] };
        if (schema.type === 'object' || _.intersection(['object'], schema.type).length === 1) {
            var scalarTypeList = ['integer', 'string', 'long', 'float', 'double', 'boolean', 'number'];
            _.forEach(schema.properties, function (fieldMeta, fieldName) {
                // Check if fieldMeta.type contains null
                var canBeNull = false;
                // If fieldMeta.type is defined as array such as ['integer', 'null'], will consider it as scalar value of type integer
                if (_.isArray(fieldMeta.type) && _.intersection(scalarTypeList, fieldMeta.type).length > 0) {
                    canBeNull = _.intersection('null', fieldMeta.type).length === 1;
                    properties.scalars.push({ field: fieldName, type: _.intersection(scalarTypeList, fieldMeta.type)[0], format: fieldMeta.format, optional: canBeNull });
                } else if (scalarTypeList.indexOf(fieldMeta.type) !== -1) {
                    properties.scalars.push({ field: fieldName, type: fieldMeta.type, format: fieldMeta.format, optional: canBeNull });
                } else if (_.isUndefined(fieldMeta.type) || fieldMeta.type === 'object' || fieldMeta.type === 'array' ||
                    (_.isArray(fieldMeta.type) && _.intersection(['object', 'array', 'null'], fieldMeta.type).length === 2)) {
                    canBeNull = _.intersection('null', fieldMeta.type).length === 1;
                    var fieldType = fieldMeta.type;
                    if (_.isArray(fieldMeta.type) && _.intersection(['object', 'array', 'null'], fieldMeta.type).length === 2) {
                        fieldType = _.intersection(['object', 'array'], fieldMeta.type)[0];
                    }
                    if (_.has(fieldMeta, 'properties') && _.has(fieldMeta.properties, 'items') &&
                        (fieldMeta.properties.items.type === 'array' || _.intersection(['object', 'array', 'null'], fieldMeta.properties.items.type).length === 2)) {
                        fieldType = 'array';
                    }
                    if (!_.isUndefined(fieldMeta.format) && fieldMeta.format === 'json-string') {
                        // Objects that we store as scalars do not need to be mapped
                        properties.scalars.push({ field: fieldName, type: fieldType, format: fieldMeta.format, optional: canBeNull });
                    } else if (fieldType === 'array') {
                        // Treat to map arrays with mapping
                        properties.lists.push({ field: fieldName, optional: canBeNull });
                    } else {
                        // Treat to map objects with mapping
                        properties.objects.push({ field: fieldName, optional: canBeNull });
                    }
                }
            });
        } else if (schema.type === 'array' || _.intersection(['array'], schema.type).length === 1) {
            // Treat to map arrays with mapping
            var canBeNull = false;
            if (_.isArray(schema.type)) {
                canBeNull = _.intersection(['array'], schema.type).length === 1;
            }
            properties.lists.push({ field: null, optional: canBeNull });
        }
        // Gather scalar values
        var mapper = self.map(source, destination, defined);
        _.forEach(properties.scalars, function (scalar) {
            // Transfer scalar value
            mapper.forMember(scalar.field, scalar.field, { type: scalar.type, format: scalar.format, isOptional: scalar.optional, isConstraint: false });
        });
        // Gather list values
        _.forEach(properties.lists, function (list) {
            var listType;
            var sourceList = source;
            var destinationList;
            if (_.isNull(list.field)) {
                destination = [];
                destinationList = destination;
            } else {
                sourceList = source[list.field];
                destination[list.field] = [];
                destinationList = destination[list.field];
            }
            if (_.has(sourceList, 'items')) {
                sourceList = sourceList.items;
            }
            if (!sourceList && !list.optional) {
                sourceList = [];
            }
            if (sourceList) {
                if (_.isNull(list.field)) {
                    listType = schema.items.type;
                } else {
                    if (_.has(schema.properties[list.field], 'items')) {
                        listType = schema.properties[list.field].items.type;
                    } else if (_.has(schema.properties[list.field], 'properties') && _.has(schema.properties[list.field].properties, 'items')) {
                        destination[list.field] = { items: [] };
                        listType = schema.properties[list.field].properties.items.items.type;
                        destinationList = destination[list.field].items;
                    }
                }
                if (_.isArray(sourceList)) {
                    _.forEach(sourceList, function (item) {
                        var itemValue = item;
                        if (listType === 'string' || _.intersection(['string'], listType).length === 1) {
                            if (_.intersection(['null'], listType).length === 0) {
                                itemValue = '';
                            }
                            if (!_.isNull(item) && !_.isUndefined(item)) {
                                itemValue = item.toString();
                            }
                        }
                        destinationList.push(itemValue);
                    });
                } else if (_.isString(sourceList)) {
                    destinationList = JSON.parse(sourceList);
                }
            }
        });
        // Gather object values
        _.forEach(properties.objects, function (object) {
            var objectProperties = schema.properties[object.field].properties;
            destination[object.field] = {};
            if (_.isString(source[object.field])) {
                destination[object.field] = JSON.parse(source[object.field]);
            } else if (_.isObject(source[object.field])) {
                mapper = self.map(source[object.field], destination[object.field], defined);
                if (objectProperties) {
                    _.forEach(objectProperties, function (fieldMeta, fieldName) {
                        mapper.forMember(fieldName, fieldName, { type: fieldMeta.type, format: fieldMeta.format, isOptional: object.optional, isConstraint: false });
                    });
                } else {
                    destination[object.field] = _.cloneDeep(source[object.field]);
                }
            }
        });
        return destination;
    } catch (ex) {
        logger.error('AutoMapper.mapSimpleBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.mapListSimpleBySchema = function (schemaModel, instance, defined) {
    var self = this;
    try {
        var destinationItems = [];
        _.forEach(instances, function (sourceItem, key) {
            var destinationItem = self.mapSimpleBySchema(schemaModel, sourceItem, defined);
            if (destinationItem) {
                destinationItems.push(destinationItem);
            }
        });
        return destinationItems;
    } catch (ex) {
        logger.error('AutoMapper.mapListSimpleBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.mapSimpleDefinedBySchema = function (schemaModel, instance) {
    var self = this;
    return self.mapSimpleBySchema(schemaModel, instance, true);
};

AutoMapper.prototype.mapListSimpleDefinedBySchema = function (schemaModel, instances) {
    var self = this;
    return self.mapListSimpleBySchema(schemaModel, instances, true);
};

AutoMapper.prototype.mapAssociatesBySchema = function (schemaModel, mapping, source, parent, defined) {
    var self = this;
    try {
        var destination = [];
        // Get schema from schemas definitions
        var rootModel = _.find(mapping[schemaModel], { destination: '$root' });
        var namespace = undefined;
        if (rootModel && _.has(rootModel, 'model')) {
            namespace = rootModel.model._namespace;
        }
        var schema = _findSchemaDefinition(namespace, schemaModel);
        if (!schema) {
            schema = _findSchemaDefinition(undefined, schemaModel);
        }
        if (!schema) {
            logger.error('AutoMapper.mapAssociatesBySchema: no schema definition found to map against', schemaModel);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        // Collect array properties
        var properties = { arrays: [] };
        _.forEach(schema.properties, function (fieldMeta, fieldName) {
            if (fieldMeta.type === 'array') {
                properties.arrays.push(fieldName);
            }
        });
        // Create object model/values keypairs
        _.forEach(properties.arrays, function (object) {
            var objectMapping = _.find(mapping[schemaModel], { destination: object });
            // Skip if no mapping defined
            if (objectMapping) {
                var objectMappingAttribute = objectMapping.attribute;
                var objectMappingAttributes = _.transform(objectMapping.attributes, _.ary(_.extend, 2), {});
                var sourceRecords = source[objectMapping.destination];
                var definition = schema.properties[object].items;
                var mappingModels = [];
                if (_.has(objectMapping, 'model')) {
                    mappingModels = [objectMapping.model];
                } else if (_.has(objectMapping, 'models')) {
                    mappingModels = objectMapping.models;
                };
                _.forEach(mappingModels, function (mappingModel) {
                    destinationItem = { model: mappingModel, values: [] };
                    _.forEach(sourceRecords, function (sourceRecord) {
                        var value = {};
                        value[parent.field] = parent.value;
                        if (definition.type === 'object') {
                            var valueMapper = self.map(sourceRecord, value, defined);
                            _.forEach(definition.properties, function (options, property) {
                                if (_.has(objectMappingAttributes, property)) {
                                    valueMapper.forMember(property, objectMappingAttributes[property].field);
                                } else {
                                    valueMapper.forMember(property);
                                }
                            });
                        } else {
                            if (objectMappingAttribute) {
                                value[objectMappingAttribute.field] = sourceRecord;
                            } else if (!_.isEmpty(objectMappingAttributes)) {
                                _.forEach(objectMappingAttributes, function (options, property) {
                                    value[options.field] = sourceRecord;
                                });
                            }
                        }
                        destinationItem.values.push(value);
                    });
                    if (!defined || (defined && !_.isUndefined(sourceRecords))) {
                        destination.push(destinationItem);
                    }
                });
            }
        });
        return destination;
    } catch (ex) {
        logger.error('AutoMapper.mapAssociatesBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.mapAssociatesDefinedBySchema = function (schemaModel, mapping, source, parent) {
    var self = this;
    return self.mapAssociatesBySchema(schemaModel, mapping, source, parent, true);
};

AutoMapper.prototype.mapFieldBySchema = function (schemaModel, mapping, model, field) {
    var self = this;
    try {
        var mappedField = null;
        var fieldPrefix = field.substring(0, field.indexOf('.'));
        var fieldSuffix = field.substring(field.indexOf('.') + 1);
        // Get schema from schemas definitions
        var rootModel = _.find(mapping[schemaModel], { destination: '$root' });
        var namespace = undefined;
        if (rootModel && _.has(rootModel, 'model')) {
            namespace = rootModel.model._namespace;
        }
        var schema = _findSchemaDefinition(namespace, schemaModel);
        if (!schema) {
            schema = _findSchemaDefinition(undefined, schemaModel);
        }
        if (!schema || !mapping || !mapping[schemaModel]) {
            logger.error('AutoMapper.mapFieldBySchema: no schema definition found to map against', schemaModel);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        // Find mapping table
        var mappingModel = _.find(mapping[schemaModel], function (object) {
            if (_.has(object, 'omitSearchBy') && object.omitSearchBy) {
                return false;
            }
            var passed = false;
            if (_.has(object, 'hasModel')) {
                passed = object.hasModel.name === model.name && object.destination !== '$root';
            } else if (_.has(object, 'model')) {
                passed = object.model.name === model.name && object.destination !== '$root';
            } else if (_.has(object, 'models')) {
                var models = [];
                _.forEach(object.models, function (model) {
                    models.push(model.name);
                });
                passed = _.intersection(models, [model.name]).length > 0 && object.destination !== '$root';
            }
            if (passed) {
                if (fieldPrefix && fieldSuffix) {
                    passed = fieldPrefix === object.destination;
                } else {
                    passed = field === object.destination;
                }
            }
            return passed;
        });
        if (!mappingModel) {
            return mappedField;
        }
        // Map schema field to Sequelize Model.Field
        if (fieldPrefix && fieldSuffix) {
            mappedField = fieldSuffix;
            if (_.has(mappingModel, 'attributes') && mappingModel.attributes.length > 0) {
                var fieldMapper = _.find(mappingModel.attributes, fieldSuffix);
                if (fieldMapper && fieldMapper[fieldSuffix]) {
                    mappedField = fieldMapper[fieldSuffix].field;
                }
            }
        } else {
            if (_.has(mappingModel, 'attribute')) {
                mappedField = mappingModel.attribute.field;
            } else if (_.has(mappingModel, 'attributes') && mappingModel.attributes.length > 0) {
                if (mappingModel.attributes.length === 1) {
                    var firstAttribute = _.values(mappingModel.attributes)[0];
                    mappedField = _.values(firstAttribute)[0].field;
                } else {
                    mappedField = [];
                    _.forEach(mappingModel.attributes, function (attribute) {
                        mappedField.push(_.values(attribute)[0].field);
                    });
                }
            }
        }
        return mappedField;
    } catch (ex) {
        logger.error('AutoMapper.mapFieldBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.mapIncludeBySchema = function (schemaModel, mapping, model) {
    var self = this;
    try {
        var mappedInclude = null;
        // Get schema from schemas definitions
        var rootModel = _.find(mapping[schemaModel], { destination: '$root' });
        var namespace = undefined;
        if (rootModel && _.has(rootModel, 'model')) {
            namespace = rootModel.model._namespace;
        }
        var schema = _findSchemaDefinition(namespace, schemaModel);
        if (!schema) {
            schema = _findSchemaDefinition(undefined, schemaModel);
        }
        if (!schema || !mapping || !mapping[schemaModel]) {
            throw new Error('ERR_VALIDATION_FAILED');
        }
        // Find mapping table with hasModel
        var mappingModel = _.find(mapping[schemaModel], function (object) {
            var passed = false;
            if (_.has(object, 'hasModel')) {
                passed = object.hasModel.name === model.name && object.destination !== '$root';
            }
            return passed;
        });
        if (!mappingModel) {
            return mappedInclude;
        }
        // Map schema field to Sequelize Model.Field
        mappedInclude = { model: mappingModel.model };
        return mappedInclude;
    } catch (ex) {
        logger.error('AutoMapper.mapIncludeBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.mapObjectDefinedBySchema = function (schemaModel, mapping, model) {
    if (!model) {
        return null;
    }
    var self = this;
    try {
        var mappedObject = {};
        // Get schema from schemas definitions
        var rootModel = _.find(mapping[schemaModel], { destination: '$root' });
        var namespace = undefined;
        if (rootModel && _.has(rootModel, 'model')) {
            namespace = rootModel.model._namespace;
        }
        var schema = _findSchemaDefinition(namespace, schemaModel);
        if (!schema) {
            schema = _findSchemaDefinition(undefined, schemaModel);
        }
        if (!schema || !mapping || !mapping[schemaModel]) {
            logger.error('AutoMapper.mapObjectDefinedBySchema: no schema definition found to map against', schemaModel);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        // Iterate through model properties (1 level, recursive feature will added later)
        _.forEach(schema.properties, function (fieldMeta, fieldName) {
            if (fieldMeta.type === 'array' || (_.isArray(fieldMeta.type) && _.intersection(['array'], fieldMeta.type).length === 1)) {
                if (_.isArray(model[fieldName]) && _.has(fieldMeta, 'properties')) {
                    mappedObject[fieldName] = [];
                    _.forEach(model[fieldName], function (item) {
                        var mappedObjectItem = {};
                        _.forEach(_.keys(fieldMeta.properties), function (property) {
                            self.mapDefined(model[fieldName], mappedObjectItem).forMember(property);
                        });
                        mappedObject[fieldName].push(mappedObjectItem);
                    });
                } else {
                    mappedObject[fieldName] = null;
                }
            } else if (fieldMeta.type === 'object' || (_.isArray(fieldMeta.type) && _.intersection(['object'], fieldMeta.type).length === 1)) {
                if (_.isObject(model[fieldName]) && _.has(fieldMeta, 'properties')) {
                    mappedObject[fieldName] = {};
                    _.forEach(_.keys(fieldMeta.properties), function (property) {
                        self.mapDefined(model[fieldName], mappedObject[fieldName]).forMember(property);
                    });
                } else {
                    mappedObject[fieldName] = null;
                }
            } else {
                self.mapDefined(model, mappedObject).forMember(fieldName);
            }
        });
        // Map schema field to Sequelize Model.Field
        return mappedObject;
    } catch (ex) {
        logger.error('AutoMapper.mapObjectDefinedBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.getEnumMappedBySchema = function (schemaModel, mapping, field) {
    var self = this;
    try {
        // Get schema from schemas definitions
        var rootModel = _.find(mapping[schemaModel], { destination: '$root' });
        var namespace = undefined;
        if (rootModel && _.has(rootModel, 'model')) {
            namespace = rootModel.model._namespace;
        }
        var schema = _findSchemaDefinition(namespace, schemaModel);
        if (!schema) {
            schema = _findSchemaDefinition(undefined, schemaModel);
        }
        if (!schema || !mapping || !mapping[schemaModel]) {
            logger.error('AutoMapper.getEnumMappedBySchema: no schema definition found to map against', schemaModel);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        // Locate field's enum from schema properties
        var schemaField = schema.properties[field];
        if (!schemaField) {
            logger.error('AutoMapper.getEnumMappedBySchema: no enum field definition found to map against', schemaModel, field);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        return schemaField.enum;
    } catch (ex) {
        logger.error('AutoMapper.getEnumMappedBySchema: exception', ex);
        throw new Error('ERR_VALIDATION_FAILED');
    }
};

AutoMapper.prototype.checkConstraint = function (model, field) {
    if (!model) {
        return false;
    }
    if (!_.has(model.attributes, field)) {
        return false;
    }
    var attribute = model.attributes[field];
    return _.has(attribute, 'references') && _.isObject(attribute.references);
};

AutoMapper.prototype.limitedMappings = function (instanceMappings) {
    var self = this;
    var mappings = _.cloneDeep(instanceMappings);
    _.forEach(mappings, function (mapping) {
        _.forEach(mapping, function (item) {
            if (_.has(item, 'limit') && item.limit === 0) {
                item.limit = self._config.limit;
            }
        })
    });
    return mappings;
};

module.exports = AutoMapper;

function _findSchemaDefinition(namespace, schemaModel) {
    var schemaDefinition = null;
    // Get schema from schemas definitions
    if (namespace) {
        var schema = JsonSchemas.schemas[namespace];
        schemaDefinition = schema.definitions[schemaModel];
    } else {
        _.forEach(JsonSchemas.schemas, function (schema) {
            if (_.has(schema, 'definitions') && !schemaDefinition) {
                if (_.has(schema.definitions, schemaModel)) {
                    schemaDefinition = schema.definitions[schemaModel];
                }
            }
        });
    }
    return schemaDefinition;
}
