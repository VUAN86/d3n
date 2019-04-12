var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikePoolLanguageIndexMeta = function AerospikePoolLanguageIndexMeta(pool) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(pool);
};
AerospikePoolLanguageIndexMeta._namespace = Config.keyvalue.namespace;
AerospikePoolLanguageIndexMeta._set = 'questionPool';
AerospikePoolLanguageIndexMeta._ttl = 0;

module.exports = AerospikePoolLanguageIndexMeta;

//Copies attributes from a parsed json or other source
AerospikePoolLanguageIndexMeta.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the pool in the database, overwrites existing items
AerospikePoolLanguageIndexMeta.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikePoolLanguageIndexMeta.toJSON(self, function (err, json) {
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePoolLanguageIndexMeta,
                    key: key,
                    value: json
                }, function (err, res) {
                    var pool = new AerospikePoolLanguageIndexMeta(json);
                    return _success(pool, callback);
                });
            } catch (ex) {
                logger.error("AerospikePoolLanguageIndexMeta.save ex:",ex);
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the pool from the database
AerospikePoolLanguageIndexMeta.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePoolLanguageIndexMeta,
            key: key
        }, function (err, res) {
            return next(err);
        });
    } catch (ex) {
        logger.error("AerospikePoolLanguageIndexMeta.remove ex:",ex);
        return next(ex);
    }
};

AerospikePoolLanguageIndexMeta.prototype.increment = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikePoolLanguageIndexMeta,
            key: key,
            bin: 'meta',
            value: 1
        }, function (err, newVal) {
            return callback(err, newVal);
        });
    } catch (ex) {
        logger.error("increment error=",ex)
        return setImmediate(callback, ex);
    }
};

//Create new pool and returns it if succesfull, null otherwise
AerospikePoolLanguageIndexMeta.create = function (params, callback) {
    var pool = new AerospikePoolLanguageIndexMeta(params);
    return pool.save(callback);
}

//Get incremented meta
AerospikePoolLanguageIndexMeta.increment = function (params, callback) {
    var pool = new AerospikePoolLanguageIndexMeta(params);
    return pool.increment(callback);
}

//Update pool template and returns it if succesfull, null otherwise
AerospikePoolLanguageIndexMeta.update = function (params, callback) {
    return AerospikePoolLanguageIndexMeta.findOne(params, function (err, pool) {
        if (err || pool === null) {
            return _error(err || Errors.DatabaseApi.NoRecordFound, callback);
        } else {
            var newpool = new AerospikePoolLanguageIndexMeta(pool);
            newpool = _copy(newpool, params);
            return newpool.save(callback);
        }
    });
}

AerospikePoolLanguageIndexMeta.remove = function (params, callback) {
    var pool = new AerospikePoolLanguageIndexMeta(params);
    return pool.remove(callback);
}

AerospikePoolLanguageIndexMeta.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePoolLanguageIndexMeta();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePoolLanguageIndexMeta,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                instance = instance.copy(res);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        logger.error("AerospikePoolLanguageIndexMeta.findOne ex:",ex);
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePoolLanguageIndexMeta.toJSON = function (object, callback) {
    var plain = _copy({}, object, false);
    return _success(plain, callback);
};

function _copy(to, from, ext) {
    return _copyMeta(to, from, true);
}

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'poolLanguageIndexMetaDataModel');
    return _.assign(to, data);
}

function _invalid(object) {
    return false;
}

function _key(params) {
    return _metaKey(params);
}

function _metaKey(params) {
    return _baseKey(params) + ':index:' + params.languageIndex + ':meta';
}

function _baseKey(params) {
    return 'category:' + params.poolId + ':type:' + params.type + ':complexity:' + params.complexity;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var pool = new AerospikePoolLanguageIndexMeta();
        result = pool;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
