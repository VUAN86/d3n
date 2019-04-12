var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeTenant = function AerospikeTenant(tenant) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(tenant);
};
AerospikeTenant._namespace = Config.keyvalue.namespace;
AerospikeTenant._set = 'tenant';
AerospikeTenant._ttl = 0;

module.exports = AerospikeTenant;

// Copies attributes from a parsed json or other source
AerospikeTenant.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

// Saves the tenant overwriting existing data
AerospikeTenant.prototype.save = function (callback) {
    var self = this;
    AerospikeTenant.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeTenant,
                    key: key,
                    value: { value: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    var tenant = new AerospikeTenant(json);
                    return _success(tenant, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

// Removes the Tenant info from the database
AerospikeTenant.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeTenant,
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
AerospikeTenant.update = function (params, callback) {
    return AerospikeTenant.findOne(params, function (err, Tenant) {
        try {
            if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                return _error(Errors.DatabaseApi.FatalError, callback);
            }

            var newTenant = new AerospikeTenant();
            if (_.isObject(Tenant)) {
                newTenant = _copy(newTenant, Tenant, true);
            }

            newTenant = _copy(newTenant, params);
            return newTenant.save(callback);
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
    });
}

AerospikeTenant.remove = function (params, callback) {
    var Tenant = new AerospikeTenant(params);
    return Tenant.remove(callback);
}

AerospikeTenant.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeTenant();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeTenant,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
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

AerospikeTenant.toJSON = function (object, callback) {
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
    if (_.has(from, 'tenantId')) {
        to.tenantId = from.tenantId;
    }
    if (!_.has(from, 'apiId') && !_.has(from, 'mainCurrency') && !_.has(from, 'exchangeRates')) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'tenantDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'tenant:' + params.tenantId;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var tenant = new AerospikeTenant();
        result = tenant;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
