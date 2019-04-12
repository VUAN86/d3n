var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeAdvertisementCounter = function AerospikeAdvertisementCounter(advertisementCounter) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(advertisementCounter);
};
AerospikeAdvertisementCounter._namespace = Config.keyvalue.namespace;
AerospikeAdvertisementCounter._set = 'advertisement';
AerospikeAdvertisementCounter._ttl = 0;

module.exports = AerospikeAdvertisementCounter;

//Copies attributes from a parsed json or other source
AerospikeAdvertisementCounter.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
};

//Removes the advertisementCounter from the database
AerospikeAdvertisementCounter.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeAdvertisementCounter,
            key: key
        }, function (err, res) {
            return callback(err);
        });
    } catch (ex) {
        return callback(ex);
    }
};

AerospikeAdvertisementCounter.prototype.increment = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikeAdvertisementCounter,
            key: key,
            bin: 'counter',
            value: 1
        }, function (err, newVal) {
            return callback(err, newVal);
        });
    } catch (ex) {
        return setImmediate(callback, ex);
    }
};

AerospikeAdvertisementCounter.prototype.decrement = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikeAdvertisementCounter,
            key: key,
            bin: 'counter',
            value: -1
        }, function (err, newVal) {
            return callback(err, newVal);
        });
    } catch (ex) {
        return setImmediate(callback, ex);
    }
};


AerospikeAdvertisementCounter.increment = function (params, callback) {
    var advertisementCounter = new AerospikeAdvertisementCounter(params);
    return advertisementCounter.increment(callback);
};

AerospikeAdvertisementCounter.decrement = function (params, callback) {
    var advertisementCounter = new AerospikeAdvertisementCounter(params);
    return advertisementCounter.decrement(callback);
};

AerospikeAdvertisementCounter.remove = function (params, callback) {
    var advertisementCounter = new AerospikeAdvertisementCounter(params);
    return advertisementCounter.remove(callback);
};

AerospikeAdvertisementCounter.getCounter = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeAdvertisementCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeAdvertisementCounter,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                return _success(res.counter, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeAdvertisementCounter.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeAdvertisementCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeAdvertisementCounter,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                instance.copy(res);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeAdvertisementCounter.toJSON = function (object, callback) {
    var plain = _copy({}, object, false);
    return _success(plain, callback);
};

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'advertisementDataModel');
    return _.assign(to, data);
};

function _invalid(object) {
    return false;
};

function _key(params) {
    return 'provider:' + params.advertisementProviderId + ':counter';
};


function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
