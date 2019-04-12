var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikePoolIndexMeta = function AerospikePoolIndexMeta(pool) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(pool);
};
AerospikePoolIndexMeta._namespace = Config.keyvalue.namespace;
AerospikePoolIndexMeta._set = 'questionPool';
AerospikePoolIndexMeta._ttl = 0;

module.exports = AerospikePoolIndexMeta;

//Copies attributes from a parsed json or other source
AerospikePoolIndexMeta.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the pool in the database, overwrites existing items
AerospikePoolIndexMeta.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikePoolIndexMeta.toJSON(self, function (err, json) {
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePoolIndexMeta,
                    key: key,
                    value: json
                }, function (err, res) {
                    var pool = new AerospikePoolIndexMeta(json);
                    return _success(pool, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the pool from the database
AerospikePoolIndexMeta.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePoolIndexMeta,
            key: key
        }, function (err, res) {
            return next(err);
        });
    } catch (ex) {
        return next(ex);
    }
};

AerospikePoolIndexMeta.prototype.increment = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikePoolIndexMeta,
            key: key,
            bin: 'meta',
            value: 1
        }, function (err, newVal) {
            return callback(err, newVal);
        });
    } catch (ex) {
        return setImmediate(callback, ex);
    }
};

//Create new pool and returns it if succesfull, null otherwise
AerospikePoolIndexMeta.create = function (params, callback) {
    var pool = new AerospikePoolIndexMeta(params);
    return pool.save(callback);
}

//Get incremented meta
AerospikePoolIndexMeta.increment = function (params, callback) {
    var pool = new AerospikePoolIndexMeta(params);
    return pool.increment(callback);
}

//Update pool template and returns it if succesfull, null otherwise
AerospikePoolIndexMeta.update = function (params, callback) {
    return AerospikePoolIndexMeta.findOne(params, function (err, pool) {
        if (err || pool === null) {
            return _error(err || Errors.DatabaseApi.NoRecordFound, callback);
        } else {
            var newpool = new AerospikePoolIndexMeta(pool);
            newpool = _copy(newpool, params);
            return newpool.save(callback);
        }
    });
}

AerospikePoolIndexMeta.remove = function (params, callback) {
    var pool = new AerospikePoolIndexMeta(params);
    return pool.remove(callback);
}

AerospikePoolIndexMeta.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePoolIndexMeta();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePoolIndexMeta,
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
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePoolIndexMeta.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'poolIndexMetaDataModel');
    return _.assign(to, data);
}

function _invalid(object) {
    return false;
}

function _key(params) {
    return _metaKey(params);
}

function _metaKey(params) {
    return _baseKey(params) + ':index:meta';
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
        var pool = new AerospikePoolIndexMeta();
        result = pool;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
