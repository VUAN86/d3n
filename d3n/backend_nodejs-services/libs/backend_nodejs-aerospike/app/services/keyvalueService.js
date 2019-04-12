var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var Errors = require('./../config/errors.js');
var CacheProvider = require('./../providers/cacheProvider.js');
var AerospikeProvider = require('./../providers/aerospikeProvider.js');
var ModelUtils = require('nodejs-utils').ModelUtils;
var logger = require('nodejs-logger')();

var cache = null;
var aerospike = null;

function KeyvalueService(config) {
    var self = this;
    self._config = config;
    self._storage = null;

    try {
        var mapping = {};
        ModelUtils.walkKeyvalue(path.join(__dirname, '..', 'models')).forEach(function (mod) {
            var model = mod.model.charAt(0).toUpperCase() + mod.model.slice(1);
            mapping[model] = require(mod.definition);
        });
        KeyvalueService.prototype.Models = mapping;
    } catch (ex) {
        logger.error('KeyvalueService initializing failed', ex);
    }
}

KeyvalueService.prototype.Models = {};

KeyvalueService.getInstance = function (config) {
    return new KeyvalueService(config);
}

KeyvalueService.prototype.connect = function (callback) {
    var self = this;
    if (self._storage) {
        return _success(self._storage, callback);
    }
    if (process.platform === 'win32') {
        var cFound = false;
        try {
            cFound = require.resolve('memory-cache');
        } catch (ex) {
            logger.error('KeyValueService: resolving memory-cache module failed', ex);
            cFound = false;
        }
        if (cFound) {
            cache = require('memory-cache');
            self._storage = cache;
            return _success(cache, callback);
        } else {
            return _error(Errors.DatabaseApi.NoDatabaseEngine, callback);
        }
    } else {
        var asFound = false;
        var options = {
            host: self._config.keyvalue.hosts,
            user: self._config.keyvalue.user,
            password: self._config.keyvalue.password,
            maxConnsPerNode: self._config.keyvalue.maxConnsPerNode,
            log: {
                level: self._config.keyvalue.log.level,
                file: self._config.keyvalue.log.file ? fs.openSync(self._config.keyvalue.log.file, 'a') : 2
            },
            policies: self._config.keyvalue.policies
        };
        try {
            asFound = require.resolve('aerospike');
        } catch (ex) {
            logger.error('KeyValueService: resolving aerospike module failed', ex);
            asFound = false;
        }
        if (asFound) {
            aerospike = require('aerospike');
            aerospike.connect(options, function (err, client) {
                if (err) {
                    logger.error('KeyValueService: connectiong to aerospike failed', err);
                    return _error(Errors.DatabaseApi.FatalError, callback);
                }
                self._storage = client;
                return _success(client, callback);
            });
        } else {
            return _error(Errors.DatabaseApi.NoDatabaseEngine, callback);
        }
    }
}

KeyvalueService.prototype.exec = function (cmd, params, callback) {
    var self = this;
    try {
        if (!callback) {
            return _error(Errors.DatabaseApi.NoDatabaseCallback);
        }
        self.connect(function (err, storage) {
            if (err) {
                return _error(err, callback);
            }
            if (process.platform === 'win32') {
                if (!_.isFunction(CacheProvider[cmd])) {
                    return _error(Errors.DatabaseApi.NoDatabaseFunction, callback);
                }
                return CacheProvider[cmd](cache, storage, params, function (err, res) {
                    if (err) {
                        return _error(err, callback);
                    }
                    return _success(res, callback);
                });
            } else if (aerospike) {
                if (!_.isFunction(AerospikeProvider[cmd])) {
                    return _error(Errors.DatabaseApi.NoDatabaseFunction, callback);
                }
                return AerospikeProvider[cmd](aerospike, storage, params, function (err, res) {
                    if (err) {
                        return _error(err, callback);
                    }
                    return _success(res, callback);
                });
            } else {
                return _error(Errors.DatabaseApi.NoDatabaseEngine, callback);
            }
        });
    } catch (ex) {
        logger.error('KeyValueService: executing command "' + cmd + '" failed', ex);
        return _error(Errors.DatabaseApi.FatalError, callback);
    }
}

KeyvalueService.prototype.load = function () {
    var self = this, _remove = [], _create = [], _indexes = [];
    functions = {
        remove: function (model, entity, key) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entity)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            _remove.push({ model: model, entity: entity, key: _.isUndefined(key) || _.isNull(key) ? 'id' : key });
            return functions;
        },
        removeSeries: function (model, entities, key) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entities) || !_.isArray(entities)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            var count = 0;
            _.each(entities, function (entity) {
                if (entity) {
                    _remove.push({ model: model, entity: entity, key: _.isUndefined(key) || _.isNull(key) ? 'id' : key });
                    count++;
                }
            });
            if (count !== entities.length) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            return functions;
        },
        create: function (model, entity, key) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entity)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            _create.push({ model: model, entity: entity, key: _.isUndefined(key) || _.isNull(key) ? 'id' : key });
            return functions;
        },
        createSeries: function (model, entities, key) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(entities) || !_.isArray(entities)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            var count = 0;
            _.each(entities, function (entity) {
                if (entity) {
                    _create.push({ model: model, entity: entity, key: _.isUndefined(key) || _.isNull(key) ? 'id' : key });
                    count++;
                }
            });
            if (count !== entities.length) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            return functions;
        },
        index: function (model, options) {
            if (_.isUndefined(model)) {
                throw new Error(Errors.DatabaseApi.NoModelDefined);
            }
            if (_.isUndefined(options)) {
                throw new Error(Errors.DatabaseApi.NoEntityData);
            }
            _indexes.push({ model: model, options: options });
            return functions;
        },
        process: function (done) {
            async.series([
                function (remove) {
                    if (_remove.length === 0) {
                        return remove();
                    }
                    return async.everySeries(_remove, function (task, next) {
                        try {
                            var modelInstance = new task.model(task.entity);
                            modelInstance.remove(function (err, count) {
                                if (err && err != Errors.DatabaseApi.NoRecordFound) {
                                    return next(err);
                                }
                                var key = task.entity[task.key] ? task.entity[task.key] : task.key;
                                if (process.env.DATABASE_LOGGER === 'true') {
                                    logger.info(task.model.name + '[' + key + '] removed');
                                }
                                return next(null, true);
                            });
                        } catch (ex) {
                            return next(ex);
                        }
                    }, function (err) {
                        if (err) {
                            return remove(err);
                        }
                        return remove();
                    });
                },
                function (create) {
                    async.series([
                        function (mod) {
                            if (_create.length === 0) {
                                return mod();
                            }
                            return async.everySeries(_create, function (task, next) {
                                try {
                                    var modelInstance = new task.model(task.entity);
                                    modelInstance.save(function (err, record) {
                                        if (err) {
                                            return next(err);
                                        }
                                        var key = task.entity[task.key] ? task.entity[task.key] : task.key;
                                        if (process.env.DATABASE_LOGGER === 'true') {
                                            logger.info(task.model.name + '[' + key + '] created');
                                        }
                                        return next(null, true);
                                    });
                                } catch (ex) {
                                    return next(ex);
                                }
                            }, function (err) {
                                if (err) {
                                    return mod(err);
                                }
                                return mod();
                            });
                        },
                        function (idx) {
                            if (process.platform === 'win32') {
                                return idx();
                            }
                            return async.everySeries(_indexes, function (index, next) {
                                try {
                                    var datatype = aerospike.indexDataType.STRING;
                                    if (index.options.datatype === 'numeric') {
                                        datatype = aerospike.indexDataType.NUMERIC;
                                    }
                                    var options = {
                                        ns: index.model._namespace,
                                        set: index.model._set,
                                        bin: index.options.bin,
                                        index: 'idx_' + index.options.bin,
                                        datatype: datatype
                                    };
                                    self.connect(function (err, storage) {
                                        if (err) {
                                            return next(err);
                                        }
                                        storage.createIndex(options, function (err, job) {
                                            if (err) {
                                                return next(err);
                                            }
                                            job.waitUntilDone(function (err) {
                                                if (err) {
                                                    return next(err);
                                                }
                                                logger.info('Index ' + options.index + ' created');
                                                return next(null, true);
                                            });
                                        });
                                    });
                                } catch (ex) {
                                    return next(ex);
                                }
                            }, function (err) {
                                if (err) {
                                    return idx(err);
                                }
                                return idx();
                            });
                        },
                    ], function (err) {
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

function _success(res, callback) {
    if (callback) {
        setImmediate(function (res, callback) {
            return callback(null, res);
        }, res, callback);
    } else {
        logger.error('KeyValueService: _success has no callback');
        throw new Error(Errors.DatabaseApi.FatalError);
    }
}

function _error(err, callback) {
    if (callback) {
        setImmediate(function (err, callback) {
            return callback(err);
        }, err, callback);
    } else {
        logger.error('KeyValueService: _error has no callback');
        throw new Error(Errors.DatabaseApi.FatalError);
    }
}

module.exports = KeyvalueService;