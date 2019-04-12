var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var KEY_PREFIX = 'profile:';

var AerospikeProfileSync = function AerospikeProfileSync(profileSync) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(profileSync);
};
AerospikeProfileSync._namespace = Config.keyvalue.namespace;
AerospikeProfileSync._set = 'profileSync';
AerospikeProfileSync._ttl = 0;

module.exports = AerospikeProfileSync;

//Copies attributes from a parsed json or other source
AerospikeProfileSync.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the profile sync entry in the database, overwrites existing items
AerospikeProfileSync.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikeProfileSync.toJSON(self, function (err, json) {
        var key = _key(json);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeProfileSync,
                    key: key,
                    value: json
                }, function (err, res) {
                    if (err) {
                        return _error(err, callback);
                    }
                    var profileSync = new AerospikeProfileSync(json);
                    return _success(profileSync, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the profile sync entry from the database
AerospikeProfileSync.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeProfileSync,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            delete self.token;
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Create new profile sync entry entry and returns it if succesfull, null otherwise
AerospikeProfileSync.create = function (params, callback) {
    var profileSync = new AerospikeProfileSync(params);
    return profileSync.save(callback);
}

//Update profile sync entry entry and returns it if succesfull, null otherwise
AerospikeProfileSync.update = function (params, callback) {
    return AerospikeProfileSync.findOne(params, function (err, profileSync) {
        if (err || profileSync === null) {
            return _error(err || Errors.DatabaseApi.NoRecordFound, callback);
        } else {
            var newProfileSync = new AerospikeProfileSync(profileSync);
            newProfileSync = _copy(newProfileSync, params);
            return newUser.save(callback);
        }
    });
}

//Remove profile sync entry entry
AerospikeProfileSync.remove = function (params, callback) {
    var profileSync = new AerospikeProfileSync(params);
    return profileSync.remove(callback);
}

//Find profile sync entry entry by profile
AerospikeProfileSync.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeProfileSync();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeProfileSync,
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

//Find profile sync entry by specifying filter criteria
AerospikeProfileSync.findAll = function (params, callback) {
    if (!_.has(params, 'filters')) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
    try {
        var instance = new AerospikeProfileSync();
        return instance.KeyvalueService.exec('query', {
            model: AerospikeProfileSync,
            filters: params.filters
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                return _success(res, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeProfileSync.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'profileSyncDataModel');
    return _.assign(to, data);
}

function _invalid(object) {
    return false;
}

function _key(params, key) {
    return KEY_PREFIX + params.profile;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var profileSync = new AerospikeProfileSync();
        result = profileSync;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
