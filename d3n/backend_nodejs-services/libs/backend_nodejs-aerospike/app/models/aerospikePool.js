var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikePool = function AerospikePool(pool) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(pool);
};
AerospikePool._namespace = Config.keyvalue.namespace;
AerospikePool._set = 'questionPool';
AerospikePool._ttl = 0;

module.exports = AerospikePool;

//Copies attributes from a parsed json or other source
AerospikePool.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
};

//Saves the pool in the database, overwrites existing items
AerospikePool.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikePool.toJSON(self, function (err, json) {
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePool,
                    key: key,
                    value: { question: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    var pool = new AerospikePool(res.question);
                    return _success(pool, callback);
                });
            } catch (ex) {
                logger.error("AerospikePool.save ex:",ex);
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the pool from the database
AerospikePool.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePool,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        logger.error("AerospikePool.remove error:",ex);
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Create new pool and returns it if succesfull, null otherwise
AerospikePool.create = function (params, callback) {
    var pool = new AerospikePool(params);
    return pool.save(callback);
};

//Update pool template and returns it if succesfull, null otherwise
AerospikePool.update = function (params, callback) {
    return AerospikePool.findOne(params, function (err, pool) {
        if (err === 'ERR_ENTRY_NOT_FOUND') {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        } else if (err) {
            return _error(err, callback);
        } else {
            var newpool = new AerospikePool(pool);
            newpool = _copy(newpool, params);
            return newpool.save(callback);
        }
    });
};

AerospikePool.remove = function (params, callback) {
    var pool = new AerospikePool(params);
    return pool.remove(callback);
};

AerospikePool.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePool();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePool,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.question));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        logger.error('Aerospike.findOne Error:',ex);
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePool.toJSON = function (object, callback) {
    var plain = _copy({}, object, false);
    return _success(plain, callback);
};

function _copy(to, from, ext) {
    return _copyObject(to, from, true);
};

function _copyObject(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'poolDataModel');
    return _.assign(to, data);
};

function _invalid(object) {
    return false;
};

function _key(params) {
    return _objectKey(params);
};


function _objectKey(params) {
    return _baseKey(params) + ':' + params.index;
};

function _baseKey(params) {
    return 'category:' + params.poolId + ':type:' + params.type + ':complexity:' + params.complexity + ':language:' + params.language;
};

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
};

function _success(data, callback) {
    var result = data;
    if (!result) {
        var pool = new AerospikePool();
        result = pool;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
};
