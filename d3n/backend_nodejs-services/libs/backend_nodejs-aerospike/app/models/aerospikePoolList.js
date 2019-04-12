var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikePoolList = function AerospikePoolList(poolList) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(poolList);
};
AerospikePoolList._namespace = Config.keyvalue.namespace;
AerospikePoolList._set = 'poolList';
AerospikePoolList._ttl = 0;

module.exports = AerospikePoolList;

// Copies attributes from a parsed json or other source
AerospikePoolList.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

// Saves the poolList overwriting existing data
AerospikePoolList.prototype.save = function (callback) {
    var self = this;
    AerospikePoolList.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePoolList,
                    key: key,
                    value: { pools: new Buffer(JSON.stringify(json.pools)) }
                }, function (err, res) {
                    var poolList = new AerospikePoolList(json);
                    return _success(poolList, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

// Update poolList and return it if succesfull, null otherwise
AerospikePoolList.update = function (params, callback) {
    var poolList = new AerospikePoolList(params);
    return poolList.save(callback);
}

AerospikePoolList.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePoolList();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePoolList,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                try {
                    var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.pools));
                    instance = instance.copy(params);
                    instance = instance.copy({ pools: resJson });
                    return _success(instance, callback);
                } catch (ex) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePoolList.toJSON = function (object, callback) {
    try {
        var plain;
        plain = _copy({}, object, false);
        return _success(plain, callback);
    } catch (ex) {
        return _error(ex, callback);
    }
};

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'poolListDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'tenantId:' + params.tenantId;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var poolList = new AerospikePoolList();
        result = poolList;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
