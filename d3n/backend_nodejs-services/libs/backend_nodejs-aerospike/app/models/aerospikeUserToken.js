var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var KEY_PREFIX = 'token_invalidated_';

var AerospikeUserToken = function AerospikeUserToken(userToken) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(userToken);
};
AerospikeUserToken._namespace = Config.keyvalue.namespace;
AerospikeUserToken._set = 'userToken';
AerospikeUserToken._ttl = 0;

module.exports = AerospikeUserToken;

//Copies attributes from a parsed json or other source
AerospikeUserToken.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the user token in the database, overwrites existing items
AerospikeUserToken.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikeUserToken.toJSON(self, function (err, json) {
        var key = _key(json);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeUserToken,
                    key: key,
                    value: json
                }, function (err, res) {
                    if (err) {
                        return _error(err, callback);
                    }
                    var userToken = new AerospikeUserToken(json);
                    return _success(userToken, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the user token from the database
AerospikeUserToken.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeUserToken,
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

//Create new user token entry and returns it if succesfull, null otherwise
AerospikeUserToken.create = function (params, callback) {
    var userToken = new AerospikeUserToken(params);
    return userToken.save(callback);
} 

//Update user token entry and returns it if succesfull, null otherwise
AerospikeUserToken.update = function (params, callback) {
    return AerospikeUserToken.findOne(params, function (err, userToken) {
        if (err || userToken === null) {
            return _error(err || Errors.DatabaseApi.NoRecordFound, callback);
        } else {
            var newUserToken = new AerospikeUserToken(userToken);
            newUserToken = _copy(newUserToken, params);
            return newUser.save(callback);
        }
    });
}

AerospikeUserToken.remove = function (params, callback) {
    var userToken = new AerospikeUserToken(params);
    return userToken.remove(callback);
}

AerospikeUserToken.validate = function (params, callback) {
    return AerospikeUserToken.findOne(params, function (err, userToken) {
        if (err || userToken === null) {
            return _error(err || Errors.DatabaseApi.NoRecordFound, callback);
        }
        return _success(userToken, callback);
    });
};

AerospikeUserToken.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeUserToken();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeUserToken,
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

AerospikeUserToken.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'authUserIdTokenModel');
    return _.assign(to, data);
}

function _invalid(object) {
    return false;
}

function _key(params, key) {
    return KEY_PREFIX + params.userId;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var userToken = new AerospikeUserToken();
        result = userToken;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
