var _ = require('lodash');
var path = require('path');
var async = require('async');
var uuid = require('node-uuid');
var sequelize = require('sequelize');
var Errors = require('./../config/errors.js');
var ModelFactory = require('nodejs-utils').ModelUtils;
var StatisticsService = require('./statisticsService.js');
var logger = require('nodejs-logger')();

var RdbmsService = function (storage) {
    var self = this;
    self._storage = storage;
    self._connected = false;

    var models = {};
    self._models = {};
    ModelFactory.walkRdbms(path.join(__dirname, '..', 'models')).forEach(function (model) {
        var modelDefinition = self._storage.import(model.definition);
        modelDefinition._namespace = model.interface;
        models[modelDefinition.name] = modelDefinition;
        self._models[modelDefinition.name] = {
            interface: model.interface.charAt(0).toUpperCase() + model.interface.slice(1),
            model: model.model.charAt(0).toUpperCase() + model.model.slice(1),
            definition: modelDefinition
        };
    });
    _.each(_.keys(models), function (model) {
        if (_.has(models[model], 'associate')) {
            models[model].associate(models);
        }
    });

    var interfaces = {};
    _.each(self._models, function (model) {
        if (_.has(interfaces, model.interface)) {
            var interfaceModels = interfaces[model.interface];
            interfaceModels.push({ model: model.model, definition: model.definition });
            interfaces[model.interface] = interfaceModels;
        } else {
            var interfaceModels = [];
            interfaceModels.push({ interface: model.interface, model: model.model, definition: model.definition });
            interfaces[model.interface] = interfaceModels;
        }
    });
    var mapping = {};
    _.each(interfaces, function (models, interface) {
        if (!_.has(models, interface)) {
            mapping[interface] = {};
        }
        _.each(models, function (model) {
            mapping[interface][model.model] = model.definition;
        });
    });
    RdbmsService.prototype.Models = mapping;
};

RdbmsService.getInstance = function (config) {
    if (_.isUndefined(RdbmsService._sequelize)) {
        RdbmsService._sequelize = new sequelize(config.rdbms.schema, config.rdbms.username, config.rdbms.password, {
            host: config.rdbms.host,
            port: config.rdbms.port,
            dialect: 'mysql',
            pool: config.rdbms.pool,
            logging: config.rdbms.logging,
            define: {
                charset: 'utf8',
                collate: 'utf8_general_ci'
            },
            initialAutoIncrement: 1
        });
        RdbmsService.prototype.StatisticsService = new StatisticsService(RdbmsService._sequelize);
    }
    return new RdbmsService(RdbmsService._sequelize);
}

RdbmsService.prototype.Models = {};

RdbmsService.prototype.getModelNamespace = function (model) {
    var self = this;
    var namespace = _.find(self.models, { model: model });
    return namespace;
}

RdbmsService.prototype.connect = function (callback) {
    var self = this;
    if (self._connected) {
        return setImmediate(function (callback) {
            return callback(false);
        }, callback);
    }
    self._storage.authenticate().then(function () {
        logger.log('Connection has been established successfully.');
        self._connected = true;
        return setImmediate(function (callback) {
            return callback(false);
        }, callback);
    }).catch(function (err) {
        logger.log('Unable to connect to the database:', err);
        return setImmediate(function (callback, err) {
            return callback(err);
        }, callback, err);
    });
}

RdbmsService.prototype.sync = function (options, callback) {
    var self = this;
    self.connect(function (err) {
        if (err) {
            return setImmediate(function (err) {
                return callback(err);
            }, err);
        }
        self._storage.sync(options).then(function () {
            return setImmediate(function () {
                return callback(false);
            });
        });
    });
}

RdbmsService.prototype.getStorage = function () {
    var self = this;
    return self._storage;
}

RdbmsService.prototype.load = function () {
    var _remove = [], _create = [];
    functions = {
        remove: function (model, entity, keys) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entity)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            _remove.push({ model: model, entity: entity, keys: _.isUndefined(keys) || _.isNull(keys) ? 'id' : keys });
            return functions;
        },
        removeSeries: function (model, entities, keys) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entities) || !_.isArray(entities)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            var count = 0;
            _.each(entities, function (entity) {
                if (entity) {
                    _remove.push({ model: model, entity: entity, keys: _.isUndefined(keys) || _.isNull(keys) ? 'id' : keys });
                    count++;
                }
            });
            if (count !== entities.length) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            return functions;
        },
        create: function (model, entity, keys) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entity)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            _create.push({ model: model, entity: entity, keys: _.isUndefined(keys) || _.isNull(keys) ? 'id' : keys });
            return functions;
        },
        createSeries: function (model, entities, keys) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entities) || !_.isArray(entities)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            var count = 0;
            _.each(entities, function (entity) {
                if (entity) {
                    _create.push({ model: model, entity: entity, keys: _.isUndefined(keys) || _.isNull(keys) ? 'id' : keys });
                    count++;
                }
            });
            if (count !== entities.length) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            return functions;
        },
        process: function (done) {
            async.series([
                function (remove) {
                    async.everySeries(_remove, function (task, next) {
                        var ids, where = {};
                        if (_.isArray(task.keys)) {
                            _.each(_.keys(task.keys), function (key) {
                                ids += (ids ? ',' : '' + task.entity[key]);
                                where[key] = task.entity[key];
                            });
                        } else {
                            ids = task.entity[task.keys];
                            where[task.keys] = task.entity[task.keys];
                        }
                        task.model.destroy({ where: where }).then(function (count) {
                            if (process.env.DATABASE_LOGGER === 'true') {
                                logger.info(task.model.name + '[' + ids + '] removed');
                            }
                            return next(null, true);
                        }).catch(function (err) {
                            logger.error('RdbmsService.process: remove', err);
                            return next(err);
                        });
                    }, function (err) {
                        if (err) {
                            return remove(err);
                        }
                        return remove();
                    });
                },
                function (create) {
                    async.everySeries(_create, function (task, next) {
                        var ids;
                        if (_.isArray(task.keys)) {
                            _.each(_.keys(task.keys), function (key) {
                                ids += (ids ? ',' : '' + task.entity[key]);
                            });
                        } else {
                            ids = task.entity[task.keys];
                        }
                        task.model.create(task.entity).then(function (record) {
                            if (process.env.DATABASE_LOGGER === 'true') {
                                logger.info(task.model.name + '[' + ids + '] created');
                            }
                            return next(null, true);
                        }).catch(function (err) {
                            logger.error('RdbmsService.process: create', err);
                            return next(err);
                        });
                    }, function (err) {
                        if (err) {
                            return create(err);
                        }
                        return create();
                    });
                },
            ], function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
        }
    };
    return functions;
}

RdbmsService.prototype.migrate = function (options, callback) {
    var self = this;
    self.connect(function (err) {
        if (err) {
            return setImmediate(function (err) {
                return callback(err);
            }, err);
        }
        // TODO
        return setImmediate(function () {
            return callback(Errors.DatabaseApi.FunctionNotImplemented);
        });
    });
}

module.exports = RdbmsService;
