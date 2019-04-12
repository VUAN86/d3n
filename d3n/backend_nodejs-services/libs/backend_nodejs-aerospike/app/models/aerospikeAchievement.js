var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeAchievement = function AerospikeAchievement(achievement) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(achievement);
};
AerospikeAchievement._namespace = Config.keyvalue.namespace;
AerospikeAchievement._set = 'achievement';
AerospikeAchievement._ttl = 0;

module.exports = AerospikeAchievement;

//Copies attributes from a parsed json or other source
AerospikeAchievement.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the Achievement in the database, overwrites existing items
AerospikeAchievement.prototype.save = function (callback) {
    var self = this;
    AerospikeAchievement.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeAchievement,
                    key: key,
                    value: { achievement: new Buffer(JSON.stringify(json)) }
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

//Removes the Achievement from the database
AerospikeAchievement.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeAchievement,
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

// Publish achievement in aerospike
AerospikeAchievement.publish = function (params, callback) {
    var achievement = new AerospikeAchievement(params);
    return achievement.save(callback);
}

// Unpublish achievement from aerospike
AerospikeAchievement.unpublish = function (params, callback) {
    return AerospikeAchievement.findOne(params, function (err, achievement) {
        try {
            if (err) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            }
            achievement.status = 'inactive';
            return achievement.save(callback);
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
    });
}

AerospikeAchievement.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeAchievement();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeAchievement,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.achievement));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeAchievement.toJSON = function (object, callback) {
    try {
        var plain = _copy({}, object, false);
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
    var data = AerospikeMapperService.map(from, 'achievementDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'achievement:' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var Achievement = new AerospikeAchievement();
        result = Achievement;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
