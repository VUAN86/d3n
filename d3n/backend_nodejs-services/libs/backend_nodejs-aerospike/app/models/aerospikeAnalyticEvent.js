var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeAnalyticEvent = function AerospikeAnalyticEvent(analyticEvent) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(analyticEvent);
};
AerospikeAnalyticEvent._namespace = Config.keyvalue.namespace;
AerospikeAnalyticEvent._set = 'analytics';
AerospikeAnalyticEvent._ttl = 0;

module.exports = AerospikeAnalyticEvent;

// Copies attributes from a parsed json or other source
AerospikeAnalyticEvent.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

// Saves the vat overwriting existing data
AerospikeAnalyticEvent.prototype.save = function (callback) {
    var self = this;
    AerospikeAnalyticEvent.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeAnalyticEvent,
                    key: key,
                    value: { timestamp: new Buffer(JSON.stringify(json.timestamp)), content: new Buffer(JSON.stringify(json.content)) }
                }, function (err, res) {
                    if (err) {
                        return _error(err, callback);
                    }
                    return _success(self, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

// Removes the vat info from the database
AerospikeAnalyticEvent.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeAnalyticEvent,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

// Update Tenant and return it if succesfull, null otherwise
AerospikeAnalyticEvent.update = function (params, callback) {
    return AerospikeAnalyticEvent.findOne(params, function (err, CountryVat) {
        try {
            if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                return _error(Errors.DatabaseApi.FatalError, callback);
            }

            var newCountryVat = new AerospikeAnalyticEvent();
            if (_.isObject(CountryVat)) {
                _copy(newCountryVat, CountryVat, true);
            }

            _copy(newCountryVat, params);
            return newCountryVat.save(callback);
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
    });
}

AerospikeAnalyticEvent.remove = function (params, callback) {
    var analyticEvent = new AerospikeAnalyticEvent(params);
    return analyticEvent.remove(callback);
}

AerospikeAnalyticEvent.add = function (params, callback) {
    var analyticEvent = new AerospikeAnalyticEvent(params);
    return analyticEvent.save(callback);
}

AerospikeAnalyticEvent.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeAnalyticEvent();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeAnalyticEvent,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            } else {
                try {
                    var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.value));
                    instance.copy(resJson);
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

AerospikeAnalyticEvent.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'analyticEventDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'analytics:' + params.key;
}

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
