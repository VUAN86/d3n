var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikePoolLanguageIndex = function AerospikePoolLanguageIndex(pool) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(pool);
};
AerospikePoolLanguageIndex._namespace = Config.keyvalue.namespace;
AerospikePoolLanguageIndex._set = 'questionPool';
AerospikePoolLanguageIndex._ttl = 0;

module.exports = AerospikePoolLanguageIndex;

//Copies attributes from a parsed json or other source
AerospikePoolLanguageIndex.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the pool in the database, overwrites existing items
AerospikePoolLanguageIndex.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikePoolLanguageIndex.toJSON(self, function (err, json) {
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePoolLanguageIndex,
                    key: key,
                    value: { index: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    var pool = new AerospikePoolLanguageIndex(json.index);
                    return _success(pool, callback);
                });
            } catch (ex) {
                logger.error("increment error=",ex)
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the pool from the database
AerospikePoolLanguageIndex.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePoolLanguageIndex,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Create new pool and returns it if succesfull, null otherwise
AerospikePoolLanguageIndex.create = function (params, callback) {
    var pool = new AerospikePoolLanguageIndex(params);
    return pool.save(callback);
}

//Update pool template and returns it if succesfull, null otherwise
AerospikePoolLanguageIndex.update = function (params, callback) {
    return AerospikePoolLanguageIndex.findOne(params, function (err, pool) {
        if (err || pool === null) {
            return _error(err || Errors.DatabaseApi.NoRecordFound, callback);
        } else {
            var newpool = new AerospikePoolLanguageIndex(pool);
            newpool = _copy(newpool, params);
            return newpool.save(callback);
        }
    });
}

AerospikePoolLanguageIndex.remove = function (params, callback) {
    var pool = new AerospikePoolLanguageIndex(params);
    return pool.remove(callback);
}

AerospikePoolLanguageIndex.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePoolLanguageIndex();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePoolLanguageIndex,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.index));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePoolLanguageIndex.toJSON = function (object, callback) {
    var plain = _copy({}, object, false);
    return _success(plain, callback);
};

function _copy(to, from, ext) {
    return _copyObject(to, from, true);
}

function _copyObject(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    from.multiIndex = _.clone(from.index);
    var data = AerospikeMapperService.map(from, 'poolLanguageIndexDataModel');
    return _.assign(to, data);
}

function _invalid(object) {
    return false;
}

function _key(params) {
    return _objectKey(params);
}


function _objectKey(params) {
    return _baseKey(params) + ':index:' + params.languageIndex + ':' + params.internationalIndex;
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
        var pool = new AerospikePoolLanguageIndex();
        result = pool;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
