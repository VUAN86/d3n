var _ = require('lodash');
var sequelize = require('sequelize');
var ProtocolMessage = require('nodejs-protocol');
var DateUtils = require('nodejs-utils').DateUtils;
var Errors = require('nodejs-errors');
var logger = require('nodejs-logger')();

function CrudHelper(config) {
    var self = this;
    self._config = config;
    self.AutoMapper = require('./AutoMapper.js').getInstance(config);
    self.RdbmsService = require('nodejs-database').getInstance(config).RdbmsService;
}

CrudHelper.getInstance = function (config) {
    return new CrudHelper(config);
};

/**
 * Scans for API parameters and puts it to an array if found (standard object)
 * @param data WS message or HTTP body
 * @param clientSession Client session object
 * @param params Parameter array for passing to API factory
 * @param names Source API parameter name(s)
 * @param dest Destination API parameter name (optional)
 */
CrudHelper.prototype.pushParam = function (data, clientSession, params, names, dest) {
    var self = this;
    var content = data;
    if (ProtocolMessage.isInstance(data)) {
        content = data.getContent();
    }
    if (_.isArray(names)) {
        _.each(names, function (name) {
            if (_.has(content, name)) {
                var param = self._processSubstitutionVariables(content[name], clientSession);
                params[dest || name] = param;
            }
        });
    } else {
        var name = names;
        if (_.has(content, name)) {
            var param = self._processSubstitutionVariables(content[name], clientSession);
            params[dest || name] = param;
        }
    }
};

/**
 * Makes substitution varibale replacements for param value
 * @param clientSession Client session object
 * @param param Parameter array for passing to API factory
 * @returns Updated params object
 */
CrudHelper.prototype._processSubstitutionVariables = function (param, clientSession) {
    if (!param || _.isEmpty(param) || !clientSession) {
        return param;
    }
    var userId;
    var tenantId;
    var self = this;
    try {
        userId = clientSession.getUserId();
        tenantId = clientSession.getTenantId();
    } catch (ex) {
        logger.error('ApiFactory._processSubstitutionVariables', ex);
        throw new Error('ERR_INSUFFICIENT_RIGHTS');
    }
    try {
        var paramJson = JSON.stringify(param);
        paramJson = paramJson.split('$user').join(userId);
        paramJson = paramJson.split('$tenant').join(tenantId);
        paramJson = paramJson.split('$date').join(DateUtils.isoDatePublish());
        paramJson = paramJson.split('$datetime').join(DateUtils.isoPublish());
        var updatedParam = JSON.parse(paramJson);
        return updatedParam;
    } catch (ex) {
        logger.error('ApiFactory._processSubstitutionVariables', ex);
        return param;
    }
};

CrudHelper.prototype._buildCustomQuery = function (rootModel, query) {
    if (!query) {
        query = 'customQuery';
    }
    var customQuery = rootModel[query];
    var customQueryOptional = rootModel.customQueryOptional;
    if (_.isArray(rootModel.customQueryOptional)) {
        customQueryOptional = '(' + rootModel.customQueryOptional.join(' AND ') + ')';
    }
    if (customQueryOptional) {
        customQuery = customQuery.split('$customQueryOptional').join(customQueryOptional);
    }
    return customQuery;
};

CrudHelper.prototype.getClientInstances = function (clientSession) {
    var clientInstances = {
        clientInfo: clientSession.getClientInfo(),
        authServiceClientInstance: clientSession.getConnectionService().getAuthService(),
        mediaServiceClientInstance: clientSession.getConnectionService().getMediaService(),
        profileServiceClientInstance: clientSession.getConnectionService().getProfileService(),
        userMessageServiceClientInstance: clientSession.getConnectionService().getUserMessage()
    };
    return clientInstances;
};

/**
 * Setup default values for limit and offset.
 * 
 * @param {object}       params        Params object that has/not limit and offset value
 * @param {integer|null} defaultOffset Default offset
 * @param {integer|null} defaultLimit  Default limit 
 */
CrudHelper.prototype.setupLimitDefaults = function (params, defaultOffset, defaultLimit) {
    var offset = _.isUndefined(defaultOffset) ? 0 : parseInt(defaultOffset);
    var limit = _.isUndefined(defaultLimit) ? 10 : parseInt(defaultLimit);

    if (_.has(params, 'limit')) {
        params.limit = parseInt(params.limit, 10);
        if (_.isNaN(params.limit) || params.limit < 0) {
            params.limit = limit;
        }
    } else {
        params.limit = limit;
    }

    if (_.has(params, 'offset')) {
        params.offset = parseInt(params.offset, 10);
        if (_.isNaN(params.offset) || params.offset < 0) {
            params.offset = offset;
        }
    } else {
        params.offset = offset;
    }
};

/**
 * Construct Sequelize SEARCH BY criteria, validate against table definition
 * Criteria field can be passed in following formats:
 *     '[<Model>.]<Field>': '<Criteria>'
 *     '[<Model>.]<Field>': '<Criteria%>'  --> LIKE operation
 *     '[<Model>.]<Field>': ['<Criteria1>', '<Criteria2>', ...]  --> IN operation
 *     '[<Model>.]<Field>': { '$operator': '<Criteria>' }
 * If Model specified, isNested parameter must be true (obsolete, better use include)
 * @param params API params, that should contain searchBy criterias
 * @param model Entity model
 * @param schema Name of API schema
 * @param mapping API schema mapping
 * @param isNested Entity model is nested (optional)
 * @returns SEARCH BY criteria
 */
CrudHelper.prototype.searchBy = function (params, model, schema, mapping, isNested) {
    function capitalizeFirstLetter(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    }
    function applyOperator(key, value) {
        if (key.startsWith('$')) {
            searchBy[key] = value;
        } else if (_.isArray(value)) {
            searchBy[key] = { $in: value };
        } else if (_.isString(value) && (value.includes('%') || value.includes('_'))) {
            searchBy[key] = { $like: value };
        } else {
            searchBy[key] = value;
        }
    }
    if (!model) {
        logger.error('No model defined');
        throw new Error('ERR_FATAL_ERROR');
    }
    if (!params || !_.has(params, 'searchBy')) {
        return {};
    }
    var searchBy = {};
    var fields = _.keys(params.searchBy);
    var criterias = _.values(params.searchBy);
    _.forEach(fields, function (field, index) {
        var criteriaKey = field;
        var criteriaKeyPrefix = criteriaKey.substring(0, criteriaKey.indexOf('.'));
        var criteriaKeySuffix = criteriaKey.substring(criteriaKey.indexOf('.') + 1);
        var criteriaValue = criterias[index];
        if (criteriaKey.startsWith('$')) {
            applyOperator(criteriaKey, criteriaValue);
        } else if (!isNested && _.hasIn(model.tableAttributes, criteriaKey)) {
            applyOperator(criteriaKey, criteriaValue);
        } else if (isNested && criteriaKeyPrefix === model.name && _.hasIn(model.tableAttributes, criteriaKeySuffix)) {
            applyOperator(criteriaKeySuffix, criteriaValue);
        } else if (criteriaKeySuffix === '_allInfo') {
            var rootModel = _.find(mapping[schema], { destination: '$root' });
            var allInfoFields = rootModel.allInfoAttributes;
            searchBy['$or'] = [];
            _.forEach(allInfoFields, function (searchField) {
                var newCriteria = {};
                newCriteria[searchField] = { '$like': '%' + criteriaValue + '%' };
                searchBy['$or'].push(newCriteria);
            });
        }
    });
    return searchBy;
};

/**
 * ### Obsolete function ###
 * Construct Sequelize SEARCH BY criteria, validate against table definition
 * Criteria field can be passed in following formats:
 *     '[<Model>.]<Field>': '<Criteria>'
 *     '[<Model>.]<Field>': '<Criteria%>'  --> LIKE operation
 *     '[<Model>.]<Field>': ['<Criteria1>', '<Criteria2>', ...]  --> IN operation
 *     '[<Model>.]<Field>': { '$operator': '<Criteria>' }
 * @param params API params, that should contain searchBy criterias
 * @param model Entity model
 * @param schema Name of API schema
 * @param mapping API schema mapping
 * @returns SEARCH BY criteria
 */
CrudHelper.prototype.nestedSearchBy = function (params, model, schema, mapping) {
    var self = this;
    return self.searchBy(params, model, schema, mapping, true);
};

/**
 * Construct Sequelize SEARCH BY criteria, merging with google like search conditions
 * If Model specified, isNested parameter must be true (obsolete, better use include)
 * @param params API params, that should contain searchBy criterias
 * @param model Entity model
 * @param schema Name of API schema
 * @param mapping API schema mapping
 * @param clientSession Client session used for substitution variables
 * @returns SEARCH BY criteria
 */
CrudHelper.prototype.searchByFullText = function (params, model, schema, mapping, clientSession) {
    var self = this;
    var rawSearchBy = '';
    var regularSearchBy = self.searchBy(params, model);
    regularSearchBy = model.sequelize.queryInterface.QueryGenerator.whereItemsQuery(regularSearchBy, { prefix: model.name });
    var fullTextWords = params.searchBy.fulltext.toLowerCase().split(' ');
    var rootModel = _.find(mapping[schema], { destination: '$root' });
    if (!_.has(rootModel, 'fullTextQuery')) {
        logger.error('No fulltext query defined for root model');
        throw new Error('ERR_FATAL_ERROR');
    }
    _.forEach(fullTextWords, function (word) {
        if (rawSearchBy.length > 0) {
            rawSearchBy += ' OR ';
        }
        rawSearchBy += rootModel.fullTextQuery.replace(new RegExp('fulltext_word', 'g'), word);
    });
    if (regularSearchBy && regularSearchBy.length > 0) {
        rawSearchBy = '( ' + regularSearchBy + ' ) AND ( ' + rawSearchBy + ' )';
    }
    if (rootModel.custom && _.has(rootModel, 'customQuery')) {
        var customQuery = self._buildCustomQuery(rootModel);
        rawSearchBy = '(' + rawSearchBy + ') AND ( ' + customQuery + ' )';
    }
    rawSearchBy = self._processSubstitutionVariables(rawSearchBy, clientSession);
    return [rawSearchBy, null];
};

/**
 * Construct Sequelize WHERE criteria, merging with google like search conditions
 * @param params API params, that should contain searchBy criterias
 * @param model Entity model
 * @param attribute Model attribute to search by
 * @returns WHERE criteria
 */
CrudHelper.prototype.searchByTags = function (params, model, attribute) {
    var self = this;
    var result = [];
    var fullTextWords = params.searchBy.fulltext.toLowerCase().split(' ');
    _.forEach(fullTextWords, function (word) {
        var column = model.sequelize.queryInterface.QueryGenerator.quoteIdentifier(model.name) + '.' +
            model.sequelize.queryInterface.QueryGenerator.quoteIdentifier(attribute);
        var condition = sequelize.where(sequelize.fn('LOWER', sequelize.col(column)), 'LIKE', '%' + word + '%');
        result.push(condition);
    });
    return result;
};

/**
 * Construct Sequelize SEARCH BY criteria, merging with custom conditions block
 * If Model specified, isNested parameter must be true (obsolete, better use include)
 * @param params API params, that should contain searchBy criterias
 * @param model Entity model
 * @param schema Name of API schema
 * @param mapping API schema mapping
 * @param clientSession Client session used for substitution variables
 * @returns SEARCH BY criteria
 */
CrudHelper.prototype.searchByCustom = function (params, model, schema, mapping, clientSession) {
    var self = this;
    return self.extendSearchByCustom(params, model, schema, mapping, null, null, clientSession);
};

/**
 * Construct Sequelize SEARCH BY criteria, merging with custom conditions block
 * If Model specified, isNested parameter must be true (obsolete, better use include)
 * @param params API params, that should contain searchBy criterias
 * @param model Entity model
 * @param schema Name of API schema
 * @param mapping API schema mapping
 * @param searchBy existing searchBy definition (raw generated by custom query or will be constructed from scratch)
 * @param query Custom SQL query definition (default is customQuery from root model) - will be linked with AND operator
 * @param clientSession Client session used for substitution variables
 * @returns SEARCH BY criteria
 */
CrudHelper.prototype.extendSearchByCustom = function (params, model, schema, mapping, searchBy, query, clientSession) {
    var self = this;
    var regularSearchBy = '';
    if (searchBy && _.isArray(searchBy) && _.size(searchBy) === 2 && _.isString(searchBy[0])) {
        var regularSearchBy = searchBy[0];
    } else {
        regularSearchBy = self.searchBy(params, model);
        regularSearchBy = model.sequelize.queryInterface.QueryGenerator.whereItemsQuery(regularSearchBy, { prefix: model.name });
    }
    var rootModel = _.find(mapping[schema], { destination: '$root' });
    if (!query) {
        query = 'customQuery';
    }
    if (!_.has(rootModel, query)) {
        logger.error('No custom query defined for root model');
        throw new Error('ERR_FATAL_ERROR');
    }
    var rawSearchBy = self._buildCustomQuery(rootModel, query);
    if (regularSearchBy && regularSearchBy.length > 0) {
        rawSearchBy = '( ' + regularSearchBy + ' ) AND ( ' + rawSearchBy + ' )';
    }
    _.forEach(_.keys(params.searchBy), function (param) {
        if (_.isString(param)) {
            rawSearchBy = rawSearchBy.split('$' + param).join(params.searchBy[param]);
        } else if (_.isArray(param)) {
            rawSearchBy = rawSearchBy.split('$' + param).join(params.searchBy[param].join(','));
        }
    });
    rawSearchBy = self._processSubstitutionVariables(rawSearchBy, clientSession);
    return [rawSearchBy, null];
};

/**
 * Extend Sequelize SEARCH BY criteria to omit currently blocked entries by another users
 * @param model
 * @param searchBy
 */
CrudHelper.prototype.extendSearchByOmitBlocked = function (model, clientSession, searchBy) {
    var self = this;
    if (!searchBy) {
        searchBy = {};
    }
    if (model && clientSession) {
        searchBy.$or = [
            { blockerResourceId: { $eq: null } },
            { blockDate: { $eq: null } },
            { blockDate: { $lt: model.sequelize.fn('DATE_SUB', model.sequelize.fn('NOW'), model.sequelize.literal('INTERVAL ' + self._config.entryBlockTimeSeconds + ' SECOND')) } },
            { blockerResourceId: { $eq: clientSession.getUserId() } },
        ];
    }
    return searchBy;
};

/**
 * Construct Sequelize INCLUDE with criteria
 * Criteria fields are validated and transformed through API schema and mapping:
 *     '<Schema Field>': '<Criteria>'
 *     '<Schema Field>': '<Criteria%>'  --> LIKE operation
 *     '<Schema Field>': ['<Criteria1>', '<Criteria2>', ...]  --> IN operation
 *     '<Schema Field>': { '$operator': '<Criteria>' }
 * @param model Base Sequelize model of query
 * @param includeModels Array of includes - Sequelize models of query
 * @param params API params, that should contain searchBy criterias
 * @param schema Name of API schema
 * @param mapping API schema mapping
 * @param defined Skip models, where are no criteria defined
 * @returns INCLUDE part of query with paging and optional nested searchBy criterias
 */
CrudHelper.prototype.include = function (model, includeModels, params, schema, mapping, defined) {
    function applyOperator(where, key, value) {
        if (_.isString(key)) {
            if (_.isArray(value)) {
                where[key] = { $in: value };
            } else if (_.isString(value) && (value.includes('%') || value.includes('_'))) {
                where[key] = { $like: value };
            } else {
                where[key] = value;
            }
        } else if (_.isArray(key) && _.isArray(value)) {
            var whereItems = [];
            _.forEach(value, function (valueItem) {
                var whereItemValues = [];
                for (var keyItem = 0; keyItem < key.length; keyItem++) {
                    var keyItemKey = key[keyItem];
                    var valueItemKey = _.keys(valueItem)[keyItem];
                    var whereItemValue = {};
                    whereItemValue[keyItemKey] = valueItem[valueItemKey];
                    whereItemValues.push(whereItemValue);
                }
                whereItems.push({ $and: whereItemValues });
            });
            where = { $or: whereItems };
        }
        return where;
    }
    var self = this;
    var result = [];
    var rootModel = undefined;
    if (mapping) {
        rootModel = _.find(mapping[schema], { destination: '$root' });
    }
    _.forEach(includeModels, function (includeModel) {
        var resultModel = { model: includeModel };
        var resultWhere = {};
        var resultInclude = {};
        if (mapping) {
            resultInclude = self.AutoMapper.mapIncludeBySchema(schema, mapping, includeModel);
        }
        if (params && schema && mapping) {
            var fields = _.keys(params.searchBy);
            var criterias = _.values(params.searchBy);
            _.forEach(fields, function (field, index) {
                var criteriaKey = field;
                var criteriaValue = criterias[index];
                if (!_.hasIn(model.tableAttributes, criteriaKey)) {
                    var mappedField = self.AutoMapper.mapFieldBySchema(schema, mapping, includeModel, criteriaKey);
                    var criteriaKeyTable = criteriaKey.substring(0, criteriaKey.indexOf('.'));
                    var criteriaKeyField = criteriaKey.substring(criteriaKey.indexOf('.') + 1);
                    if (mappedField && criteriaKeyTable && criteriaKeyField && !_.isEmpty(resultInclude)) {
                        resultInclude.where = applyOperator(resultInclude.where || {}, mappedField, criteriaValue);
                    } else if (mappedField) {
                        resultWhere = applyOperator(resultWhere, mappedField, criteriaValue);
                    }
                }
            });
        }
        if (!_.isEmpty(resultWhere)) {
            resultModel.required = true;
            resultModel.where = resultWhere;
        }
        if (!_.isEmpty(resultInclude)) {
            resultModel.required = true;
            resultModel.include = resultInclude;
        }
        var customInclude = false;
        if (defined && rootModel && _.has(params, 'searchBy')) {
            if (_.has(params.searchBy, 'fulltext') && _.includes(rootModel.fullTextInclude, includeModel)) {
                customInclude = true;
            }
            if (_.has(rootModel, 'customInclude') && _.includes(rootModel.customInclude, includeModel)) {
                customInclude = true;
            }
        }
        if (!defined || (defined && resultModel.required) || (defined && customInclude)) {
            result.push(resultModel);
        }
    });
    return result;
};

/**
 * Construct Sequelize INCLUDE with criteria. Skip models, where are no criteria defined.
 * Criteria fields are validated and transformed through API schema and mapping:
 *     '<Schema Field>': '<Criteria>'
 *     '<Schema Field>': '<Criteria%>'  --> LIKE operation
 *     '<Schema Field>': ['<Criteria1>', '<Criteria2>', ...]  --> IN operation
 *     '<Schema Field>': { '$operator': '<Criteria>' }
 * @param model Base Sequelize model of query
 * @param includeModels Array of includes - Sequelize models of query
 * @param params API params, that should contain searchBy criterias
 * @param schema Name of API schema
 * @param mapping API schema mapping
 * @returns INCLUDE part of query with paging and optional nested searchBy criterias
 */
CrudHelper.prototype.includeDefined = function (model, includeModels, params, schema, mapping) {
    var self = this;
    return self.include(model, includeModels, params, schema, mapping, true);
};

/**
 * Extend INCLUDE object by WHERE criteria for given model
 * @param include Existing INCLUDE object
 * @param model Model entity that present in INCLUDE object
 * @param where Criteria for model entity
 * @returns Updated INCLUDE object
 */
CrudHelper.prototype.includeWhere = function (include, model, where, required) {
    var includeModel = _.find(include, { model: model });
    if (_.isUndefined(includeModel)) {
        include.push({ model: model });
        includeModel = _.find(include, { model: model });
    }
    includeModel.where = where;
    includeModel.required = required;
    return include;
};

/**
 * Extend INCLUDE object by WHERE criteria for given model
 * @param include Existing INCLUDE object
 * @param model Model entity that present in INCLUDE object
 * @param nestedModels Model entity or hierarchical array of model entities that should be added as nested
 * @param nestedWhere Criteria that should be added as nested (to last model entity in case if hierarchical array passed)
 * @returns Updated INCLUDE object
 */
CrudHelper.prototype.includeNestedWhere = function (include, model, nestedModels, nestedWhere, required) {
    var includeModel = _.find(include, { model: model });
    if (_.isUndefined(includeModel)) {
        include.push({ model: model });
        includeModel = _.find(include, { model: model });
    }
    if (!includeModel.include) {
        includeModel.include = [];
    }
    if (_.isArray(nestedModels)) {
        var includeLevel = includeModel;
        _.forEach(nestedModels, function (nestedModel) {
            if (!includeLevel.include) {
                includeLevel.include = [];
            }
            includeLevel.include.push({
                model: nestedModel,
                required: required
            });
            includeLevel = includeLevel.include[includeLevel.include.length - 1];
        });
        includeLevel.where = nestedWhere;
    } else {
        includeModel.include.push({
            model: nestedModels,
            where: nestedWhere,
            required: required
        });
    }
    return include;
};

/**
 * Extend INCLUDE object by ALIAS for given model
 * @param include Existing INCLUDE object
 * @param model Model entity that present in INCLUDE object
 * @param where Alias for model entity
 * @returns Updated INCLUDE object
 */
CrudHelper.prototype.includeAlias = function (include, model, alias) {
    var includeModel = _.find(include, { model: model });
    if (!_.isUndefined(includeModel)) {
        includeModel.as = alias;
    }
    return include;
};

/**
 * Make no attributes for INCLUDE object except for given model
 * @param include Existing INCLUDE object
 * @param model Model entity that present in INCLUDE object
 * @returns Updated INCLUDE object
 */
CrudHelper.prototype.includeNoAttributes = function (include, model) {
    var self = this;
    if (_.isArray(include)) {
    _.forEach(include, function (includeModel) {
            if (model.name !== includeModel.model.name) {
            includeModel.attributes = [];
        }
        if (_.has(includeModel, 'include')) {
            includeModel.include = self.includeNoAttributes(includeModel.include, model);
        }
    });
    } else {
        if (model.name !== include.model.name) {
            include.attributes = [];
        }
        if (_.has(include, 'include')) {
            include.include = self.includeNoAttributes(include.include, model);
        }
    }
    return include;
};

/**
 * Make distinct attribute with same alias as attribute named,
 * which can be included into attribute array of query statement
 * @param attribute Model attribute
 * @returns DISTINCT('model.attribute') AS 'attribute'
 */
CrudHelper.prototype.distinctAttribute = function (attribute) {
    var column = attribute.Model.sequelize.queryInterface.QueryGenerator.quoteIdentifier(attribute.Model.name) + '.' +
        attribute.Model.sequelize.queryInterface.QueryGenerator.quoteIdentifier(attribute.field);
    var attribute = [sequelize.fn('DISTINCT', sequelize.col(column)), attribute.field];
    return attribute;
};

/**
 * Make distinct attribute with same alias as attribute named and return it as attribute array of query statement
 * @param attribute Model attribute
 * @returns [DISTINCT('model.attribute') AS 'attribute']
 */
CrudHelper.prototype.distinctAttributes = function (attribute) {
    var self = this;
    return [self.distinctAttribute(attribute)];
};

/**
 * Construct Sequelize GROUP BY criteria
 * @param attributes Model params, that should contain searchBy criterias
 * @returns GROUP BY criteria
 */
CrudHelper.prototype.groupBy = function (attributes) {
    var result = [];
    if (!_.isArray(attributes)) {
        attributes = [attributes];
    }
    _.forEach(attributes, function (attribute) {
        var groupByCol = attribute.Model.sequelize.queryInterface.QueryGenerator.quoteIdentifier(attribute.Model.name) + '.' +
            attribute.Model.sequelize.queryInterface.QueryGenerator.quoteIdentifier(attribute.field);
        result.push(groupByCol);
    });
    return result;
};

/**
 * Construct Sequelize ORDER BY criteria, validate against table definition
 * @param params API params, that should contain searchBy criterias
 * @param tableAttributes Model entity attributes
 * @returns ORDER BY criteria
 */
CrudHelper.prototype.orderBy = function (params, model) {
    if (!model) {
        logger.error('No model defined');
        throw new Error('ERR_FATAL_ERROR');
    }
    var valid = true;
    var result = [];
    if (!params || !_.has(params, 'orderBy')) {
        return result;
    }
    _.each(params.orderBy, function (criteria) {
        // For now only search by master table field supported
        if (!(_.isObject(criteria) && _.has(criteria, 'field') && _.hasIn(model.tableAttributes, criteria.field))) {
            valid = false;
        }
        if (valid) {
            result.push([criteria.field, criteria.direction.toUpperCase()]);
        }
    });
    if (!valid) {
        throw new Error('ERR_VALIDATION_FAILED');
    }
    return result;
};

/**
 * Constructs aggregation subquery, which can be used in Sequelize find methods
 * @param {String} fieldName Field alias
 * @param {String} sql Raw SQL
 * @returns {String}
 */
CrudHelper.prototype.subQuery = function (fieldAlias, sql) {
    return [
        sequelize.literal(sql),
        fieldAlias
    ];
};

/**
 * Executes SELECT raw query and returns data through callback
 * @param {String} sql Raw SQL
 * @param {String} params SQL replacement parameters
 * @param callback
 * @returns
 */
CrudHelper.prototype.rawQuery = function (sql, params, type, callback) {
    var self = this;
    try {
        var cb;
        var queryType;
        if(_.isFunction(type)) {
            queryType = sequelize.QueryTypes.SELECT;
            cb = type;
        } else {
            queryType = sequelize.QueryTypes[type];
            cb = callback;
        }
        return self.RdbmsService.getStorage().query(sql, {
            replacements: params,
            type: queryType
        }).then(function (data) {
            return self._callback(null, data, cb);
        }).catch(function (err) {
            return self._callback(err, null, cb);
        });
    } catch (ex) {
        return self._callback(ex, null, cb);
    }
};


CrudHelper.prototype.createTransaction = function (options, callback) {
    try {
        var self = this;
        
        var opts = _.assign({
            autocommit: false
        }, options);
        
        return self.RdbmsService.getStorage().transaction(opts).then(function (t) {
            return self._callback(null, t, callback);
        }).catch(function (err) {
            return self._callback(err, null, callback);
        });
    } catch (ex) {
        return self._callback(ex, null, callback);
    }
};

/**
 * Check model entry if it blocked by another user and raise error when block date not expired and less than 2h
 * @param model Model that contain block attributes
 * @param params Params should contain entry ID
 * @param clientSession
 * @param callback
 * @returns
 */
CrudHelper.prototype.checkModelEntryBlockedByUser = function (model, params, clientSession, callback) {
    var self = this;
    return model.findOne({
        where: { id: params.id }
    }).then(function (entry) {
        if (!entry) {
            return callback('ERR_ENTRY_NOT_FOUND');
        }
        if (entry.blockerResourceId && entry.blockDate &&
            entry.blockerResourceId !== clientSession.getUserId() && ((new Date()).getTime - entry.blockDate.getTime()) < self._config.entryBlockTimeSeconds * 1000) {
            return callback('ERR_ENTRY_BLOCKED_BY_ANOTHER_USER');
        }
        return callback();
    }).catch(function (err) {
        return callback(err);
    });
};

/**
 * Check Sequelize error for possible violations
 * @param err Sequelize error coming from catch promise
 * @param options Optional options to override default violation messages
 * @returns Violation error otherwise Fatal error
 */
CrudHelper.prototype.checkViolation = function (err, options) {
    var foreignKeyConstraintViolation = 'ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION';
    var uniqueKeyConstraintViolation = 'ERR_UNIQUE_KEY_CONSTRAINT_VIOLATION';
    var sequelizeValidationError = 'ERR_VALIDATION_FAILED';
    if (options) {
        if (_.has(options, 'foreignKeyConstraintViolation') && options.foreignKeyConstraintViolation) {
            foreignKeyConstraintViolation = options.foreignKeyConstraintViolation;
        }
        if (_.has(options, 'uniqueKeyConstraintViolation') && options.uniqueKeyConstraintViolation) {
            uniqueKeyConstraintViolation = options.uniqueKeyConstraintViolation;
        }
    }
    if (_.has(err, 'name') && err.name === 'SequelizeForeignKeyConstraintError') {
        return foreignKeyConstraintViolation;
    }
    if (_.has(err, 'name') && err.name === 'SequelizeUniqueConstraintError') {
        return uniqueKeyConstraintViolation;
    }
    if (_.has(err, 'name') && err.name === 'SequelizeValidationError') {
        return sequelizeValidationError;
    }
    logger.error('checkViolation:', err);
    return 'ERR_FATAL_ERROR';
};

/**
 * Processes error returned by API factory to Default Service handler
 * Setups some parameters from incoming message: Ack, Token, Client ID
 * @param err Outgoing error ProtocomMessage
 * @param message Incoming message
 * @param clientSession Default Service client session object
 */
CrudHelper.prototype.handleFailure = function (err, message, clientSession) {
    var self = this;
    return self._handleMessage(err, null, message, clientSession);
};

/**
 * Processes results returned by API factory to Default Service handler
 * Setups some parameters from incoming message: Ack, Token, Client ID
 * @param data Outgoing success ProtocomMessage
 * @param message Incoming message
 * @param clientSession Default Service client session object
 */
CrudHelper.prototype.handleProcessed = function (err, data, message, clientSession) {
    var self = this;
    return self._handleMessage(err, data, message, clientSession);
};

/**
 * Passes error through API callback
 * @param err Error object / exception coming from API factory/processor
 * @param callback API callback function
 */
CrudHelper.prototype.callbackError = function (err, callback) {
    var self = this;
    if (ProtocolMessage.isInstance(err)) {
        logger.error('apiFactory.callbackError', err.getMessage());
    } else if (_.isObject(err) && !self.isErrorInstance(err)) {
        logger.error('apiFactory.callbackError', err);
        err = self.checkViolation(err);
    }
    return self._callback(err, null, callback);
};

/**
 * Checks if error is not part of application errors
 * @param {Object} err Error (string or javascript error object)
 * @returns
 */
CrudHelper.prototype.isErrorInstance = function (err) {
    function getErrors(api) {
        var result = [];
        _.forOwn(api, function (value, key) {
            if (_.isObject(value)) {
                result = _.union(result, getErrors(value));
            } else {
                result.push(value);
            }
        });
        return result;
    }

    // Populate application errors
    var errors = getErrors(Errors);

    // Check if error is string or javascript error object
    var error = err;
    if (_.isObject(err)) {
        if (_.has(err, 'message') && _.isString(err.message)) {
            error = err.message;
        } else {
            return false;
        }
    }

    // Check if error is not listed in app defined errors
    if (errors.length === _.union(errors, [error]).length) {
        return true;
    }
    return false;
};

/**
 * Passes data through API callback
 * @param data Data coming from API factory/processor
 * @param callback API callback function
 */
CrudHelper.prototype.callbackSuccess = function (data, callback) {
    var self = this;
    return self._callback(null, data, callback);
};

/**
 * Passes data / error through API callback
 * @param err Error object / exception coming from API factory
 * @param data Data object coming from API factory
 * @param callback API callback function
 */
CrudHelper.prototype._callback = function (err, data, callback) {
    setImmediate(function (err, data, callback) {
        return callback(err, data);
    }, err, data, callback);
};

/**
 * Processes ProtocolMessage returned by API factory to Default Service handler
 * Setups some parameters from incoming message: Ack, Token, Client ID
 * @param err Outgoing error ProtocomMessage (optional)
 * @param data Outgoing success ProtocomMessage (optional)
 * @param message Incoming message
 * @param clientSession Default Service client session object
 */
CrudHelper.prototype._handleMessage = function (err, data, message, clientSession) {
    var self = this;
    var pm = self._outMessage(message);
    if (err) {
        if (err && _.isObject(err)) {
            if (_.has(err, 'message')) {
                pm.setError(err.message);
            } else {
                logger.error('handleMessage:', err);
                pm.setError('ERR_FATAL_ERROR');
            }
        } else {
            pm.setError(err);
        }
    } else if (data) {
        pm.setContent(data);
    }
    clientSession.sendMessage(pm);
};

/**
 * Processes results returned by API factory.
 * 
 * @param object err  Outgoing success ProtocomMessage
 * @param object Data retrieved
 * @param Response Express response
 */
CrudHelper.prototype.handleHttpResponse = function (err, data, res) {
    var self = this;

    // We use only the protocol message to format the response but not sending it all
    var pm = new ProtocolMessage();
    if (err) {
        res.status(404);
        if (err && _.isObject(err)) {
            if (_.has(err, 'message')) {
                pm.setError(err.message);
            } else {
                logger.error('handleMessage:', err);
                pm.setError('ERR_FATAL_ERROR');
            }
        } else {
            pm.setError(err);
        }
    } else if (data) {
        res.status(200);
        pm.setContent(data);
    }

    res.json({
        'content': pm.getContent(),
        'error': pm.getError()
    });
};

/**
 * Generates initial outgoing ProtocolMessage from incoming message
 * @param message Incoming message
 * @returns Outgoing ProtocolMessage
 */
CrudHelper.prototype._outMessage = function (message) {
    var pm = new ProtocolMessage();
    pm.setMessage(message.getMessage() + 'Response');
    pm.setContent(null);
    pm.setSeq(null);
    pm.setAck(message && message.getSeq() ? [message.getSeq()] : null);
    pm.setTimestamp(_.now());
    pm.setClientId(message.getClientId());
    return pm;
};

module.exports = CrudHelper;
