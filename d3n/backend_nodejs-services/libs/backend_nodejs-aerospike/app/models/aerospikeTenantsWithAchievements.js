var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeTenantsWithAchievements = function AerospikeTenantsWithAchievements(tenantsWithAchievements) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(tenantsWithAchievements);
};
AerospikeTenantsWithAchievements._namespace = Config.keyvalue.namespace;
AerospikeTenantsWithAchievements._set = 'tenantIdSet';
AerospikeTenantsWithAchievements._ttl = 0;

module.exports = AerospikeTenantsWithAchievements;

// Copies attributes from a parsed json or other source
AerospikeTenantsWithAchievements.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

// Saves the tenantsWithAchievements overwriting existing data
AerospikeTenantsWithAchievements.prototype.save = function (callback) {
    var self = this;
    AerospikeTenantsWithAchievements.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeTenantsWithAchievements,
                    key: key,
                    value: { value: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    var tenantsWithAchievements = new AerospikeTenantsWithAchievements(json);
                    return _success(tenantsWithAchievements, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

// Removes the tenantsWithAchievements info from the database
AerospikeTenantsWithAchievements.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeTenantsWithAchievements,
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

// Update tenantsWithAchievements and return it if succesfull, null otherwise
AerospikeTenantsWithAchievements.update = function (params, callback) {
    return AerospikeTenantsWithAchievements.findOne(params, function (err, tenantsWithAchievements) {
        try {
            if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                return _error(Errors.DatabaseApi.FatalError, callback);
            }
            
            var newTenant = new AerospikeTenantsWithAchievements();
            if (_.isObject(tenantsWithAchievements)) {
                newTenant = _copy(newTenant, tenantsWithAchievements, true);
            }
            
            newTenant = _copy(newTenant, params);
            return newTenant.save(callback);
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
    });
}

AerospikeTenantsWithAchievements.remove = function (params, callback) {
    var tenantsWithAchievements = new AerospikeTenantsWithAchievements(params);
    return tenantsWithAchievements.remove(callback);
}

AerospikeTenantsWithAchievements.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeTenantsWithAchievements();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeTenantsWithAchievements,
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

AerospikeTenantsWithAchievements.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'tenantsWithAchievementsDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'tenants';
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var tenantsWithAchievements = new AerospikeTenantsWithAchievements();
        result = tenantsWithAchievements;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
