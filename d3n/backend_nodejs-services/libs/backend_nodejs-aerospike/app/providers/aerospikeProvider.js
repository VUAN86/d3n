var _ = require('lodash');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

module.exports = {
    get: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        storage.get(asKey, function (err, value, metadata) {
            if (err) {
                if (err.code === engine.status.AEROSPIKE_ERR_RECORD_NOT_FOUND) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
                logger.error('aerospikeProvider.get', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            if (params.includeMetadata === true) {
                value.__metadata__ = metadata;
            }
            return _success(value, callback);
        });
    },
    
    list: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'keys'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKeys = [];
        _.each(params.keys, function (key) {
            var asKey = { key: new engine.Key(params.model._namespace, params.model._set, key), read_all_bins: true };
            asKeys.push(asKey);
        });
        storage.batchRead(asKeys, function (err, results) {
            if (err) {
                if (err.code === engine.status.AEROSPIKE_ERR_RECORD_NOT_FOUND) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
                logger.error('aerospikeProvider.list', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            var values = [], found = true;
            results.forEach(function (result) {
                switch (result.status) {
                    case engine.status.AEROSPIKE_OK:
                        values.push(result.bins);
                        break;
                    default:
                        found = false;
                }
            });
            if (!found) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            }
            return _success(values, callback);
        });
    },
    
    query: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'filters'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asFilters = [];
        _.each(params.filters, function (filter) {
            _.each(_.keys(filter), function (filterFunc) {
                var filterBin = _.keys(filter[filterFunc])[0];
                var filterValue = _.values(filter[filterFunc])[0];
                if (filterFunc === 'equal') {
                    var asFilter = engine.filter.equal(filterBin, filterValue);
                    asFilters.push(asFilter);
                } else if (filterFunc === 'range') {
                    asFilter = engine.filter.range(filterBin, filterValue[0], filterValue[1]);
                    asFilters.push(asFilter);
                } else if (filterFunc === 'contains') {
                    asFilter = engine.filter.contains(filterBin, filterValue);
                    asFilters.push(asFilter);
                }
            });
        });
        if (asFilters.length === 0) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var values = [];
        var asStream = storage.query(params.model._namespace, params.model._set, { filters: asFilters }).execute();
        asStream.on('data', function (value) {
            values.push(value);
        })
        asStream.on('error', function (error) {
            logger.error('aerospikeProvider.query: stream', error);
            return _error(Errors.DatabaseApi.FatalError, callback);
        })
        asStream.on('end', function () {
            if (values.length === 0) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            }
            return _success(values, callback);
        })
    },

    put: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set') && _.has(params, 'value'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        var metadata = _.cloneDeep(Config.keyvalue.metadata);
        if (_.has(params.model, '_ttl')) {
            metadata.ttl = params.model._ttl;
        }
        storage.put(asKey, params.value, metadata, function (err, res) {
            if (err) {
                logger.error('aerospikeProvider.put', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(params.value, callback);
        });
    },
    
    putWithPolicy: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set') && _.has(params, 'value'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        var metadata = _.cloneDeep(Config.keyvalue.metadata);
        if (_.has(params.model, '_ttl')) {
            metadata.ttl = params.model._ttl;
        }
        
        var policy = {};
        for(var k in params.policy) {
            policy[k] = _.get(engine, params.policy[k]);
        }
        
        storage.put(asKey, params.value, metadata, policy, function (err, res) {
            if (err) {
                if (err.code === engine.status.AEROSPIKE_ERR_RECORD_EXISTS) {
                    return _error(Errors.DatabaseApi.RecordExists, callback);
                }
                logger.error('aerospikeProvider.putWithPolicy', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(params.value, callback);
        });
    },
    incr: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set') && _.has(params, 'value'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        var metadata = _.cloneDeep(Config.keyvalue.metadata);
        if (_.has(params.model, '_ttl')) {
            metadata.ttl = params.model._ttl;
        }
        storage.incr(asKey, params.value, metadata, function (err, res) {
            if (err) {
                logger.error('aerospikeProvider.incr', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(params.value, callback);
        });
    },
    
    incrAndGet: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set') && _.has(params, 'bin') && _.has(params, 'value'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        var metadata = _.cloneDeep(Config.keyvalue.metadata);
        if (_.has(params.model, '_ttl')) {
            metadata.ttl = params.model._ttl;
        }
        
        var op = engine.operations;
        var ops = [
            op.incr(params.bin, params.value),
            op.read(params.bin)
        ];
        
        storage.operate(asKey, ops, metadata, function (err, record) {
            if (err) {
                logger.error('aerospikeProvider.incrGetValue', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(record[params.bin], callback);
        });
    },
    
    remove: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set'))) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        storage.remove(asKey, function (err) {
            if (err) {
                if (err.code === engine.status.AEROSPIKE_ERR_RECORD_NOT_FOUND) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
                logger.error('aerospikeProvider.remove', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(true, callback);
        });
    },
    
    'map.put': function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set') )) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        
        var metadata = _.cloneDeep(Config.keyvalue.metadata);
        if (_.has(params.model, '_ttl')) {
            metadata.ttl = params.model._ttl;
        }
        
        // engine = aerospike module; storage = aerospike client
        var maps = engine.maps;
        var ops = [
            maps.put(params.mapBin, params.mapKey, params.mapValue)
        ];
        
        storage.operate(asKey, ops, metadata, function (err, res) {
            if (err) {
                logger.error('aerospikeProvider.map.put', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(params.value, callback);
        });
    },
    
    'map.getByKey': function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set') )) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        
        // engine = aerospike module; storage = aerospike client
        var maps = engine.maps;
        var ops = [
            maps.getByKey(params.mapBin, params.mapKey, maps.returnType.KEY_VALUE)
        ];
        
        storage.operate(asKey, ops, function (err, res) {
            if (err) {
                if (err.code === engine.status.AEROSPIKE_ERR_RECORD_NOT_FOUND) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
                
                
                logger.error('aerospikeProvider.map.getByKey', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(res, callback);
        });
    },

    'map.removeByKey': function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'model') && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params.model, '_namespace') && _.has(params.model, '_set') )) {
            return _error(Errors.DatabaseApi.FatalError, callback);
        }
        var asKey = new engine.Key(params.model._namespace, params.model._set, params.key);
        
        // engine = aerospike module; storage = aerospike client
        var maps = engine.maps;
        var ops = [
            maps.removeByKey(params.mapBin, params.mapKey, maps.returnType.COUNT)
        ];
        
        storage.operate(asKey, ops, function (err, res) {
            if (err) {
                if (err.code === engine.status.AEROSPIKE_ERR_RECORD_NOT_FOUND) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
                
                logger.error('aerospikeProvider.map.removeByKey', err);
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            return _success(res, callback);
        });
    },
}

function _success(res, callback) {
    return setImmediate(function (res, callback) {
        return callback(null, res);
    }, res, callback);
}

function _error(err, callback) {
    return setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}
