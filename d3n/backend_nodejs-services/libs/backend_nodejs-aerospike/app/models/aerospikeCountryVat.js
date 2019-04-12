var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeCountryVat = function AerospikeCountryVat(countryVat) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(countryVat);
};
AerospikeCountryVat._namespace = Config.keyvalue.namespace;
AerospikeCountryVat._set = 'vat';
AerospikeCountryVat._ttl = 0;

module.exports = AerospikeCountryVat;

// Copies attributes from a parsed json or other source
AerospikeCountryVat.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

// Saves the vat overwriting existing data
AerospikeCountryVat.prototype.save = function (callback) {
    var self = this;
    AerospikeCountryVat.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeCountryVat,
                    key: key,
                    value: { value: new Buffer(JSON.stringify(json)) }
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
AerospikeCountryVat.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeCountryVat,
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
AerospikeCountryVat.update = function (params, callback) {
    return AerospikeCountryVat.findOne(params, function (err, CountryVat) {
        try {
            if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                return _error(Errors.DatabaseApi.FatalError, callback);
            }

            var newCountryVat = new AerospikeCountryVat();
            if (_.isObject(CountryVat)) {
                newCountryVat = _copy(newCountryVat, CountryVat, true);
            }

            newCountryVat = _copy(newCountryVat, params);
            return newCountryVat.save(callback);
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
    });
}

AerospikeCountryVat.remove = function (params, callback) {
    var CountryVat = new AerospikeCountryVat(params);
    return CountryVat.remove(callback);
}

AerospikeCountryVat.save = function (params, callback) {
    var CountryVat = new AerospikeCountryVat(params);
    return CountryVat.save(callback);
}

AerospikeCountryVat.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeCountryVat();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeCountryVat,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            } else {
                try {
                    var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.value));
                    instance = instance.copy(resJson);
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

AerospikeCountryVat.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'vatDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'vat:' + params.country.toUpperCase();
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var tenant = new AerospikeCountryVat();
        result = tenant;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
