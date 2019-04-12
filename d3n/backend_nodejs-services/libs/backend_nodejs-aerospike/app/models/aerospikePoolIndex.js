var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikePoolIndex = function AerospikePoolIndex(pool) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(pool);
};
AerospikePoolIndex._namespace = Config.keyvalue.namespace;
AerospikePoolIndex._set = 'questionPool';
AerospikePoolIndex._ttl = 0;

module.exports = AerospikePoolIndex;

//Copies attributes from a parsed json or other source
AerospikePoolIndex.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the pool in the database, overwrites existing items
AerospikePoolIndex.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikePoolIndex.toJSON(self, function (err, json) {
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePoolIndex,
                    key: key,
                    value: { index: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    var pool = new AerospikePoolIndex(json.index);
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
AerospikePoolIndex.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePoolIndex,
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
AerospikePoolIndex.create = function (params, callback) {
    var pool = new AerospikePoolIndex(params);
    return pool.save(callback);
}

//Update pool template and returns it if succesfull, null otherwise
AerospikePoolIndex.update = function (params, callback) {
    return AerospikePoolIndex.findOne(params, function (err, pool) {
        if (err || pool === null) {
            return _error(err || Errors.DatabaseApi.NoRecordFound, callback);
        } else {
            var newpool = new AerospikePoolIndex(pool);
            newpool = _copy(newpool, params);
            return newpool.save(callback);
        }
    });
}

AerospikePoolIndex.remove = function (params, callback) {
    var pool = new AerospikePoolIndex(params);
    return pool.remove(callback);
}

AerospikePoolIndex.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePoolIndex();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePoolIndex,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.index));
                instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePoolIndex.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'poolIndexDataModel');
    return _.assign(to, data);
}

function _invalid(object) {
    return false;
}

function _key(params) {
    return _objectKey(params);
}


function _objectKey(params) {
    return _baseKey(params) + ':index:' + params.internationalIndex;
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
        var pool = new AerospikePoolIndex();
        result = pool;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
