var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeAchievementList = function AerospikeAchievementList(achievementList) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(achievementList);
};
AerospikeAchievementList._namespace = Config.keyvalue.namespace;
AerospikeAchievementList._set = 'achieveList';
AerospikeAchievementList._ttl = 0;

module.exports = AerospikeAchievementList;

//Copies attributes from a parsed json or other source
AerospikeAchievementList.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the AchievementList in the database, overwrites existing items
AerospikeAchievementList.prototype.save = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('put', {
            model: AerospikeAchievementList,
            key: key,
            value: { achievements: self.achievements }
        }, function (err, res) {
            var AchievementList = new AerospikeAchievementList(self);
            return _success(AchievementList, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the AchievementList from the database
AerospikeAchievementList.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeAchievementList,
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

// Publish AchievementList in aerospike
AerospikeAchievementList.publish = function (params, callback) {
    var achievementKey = _keyAchievement(params);
    var achievementInfo;
    async.series([
        // Setup achievement info
        function (next) {
            var achievement = new AerospikeAchievementList();
            achievement = _copyAchievement(achievement, params, false);
            return AerospikeAchievementList.toJSON(achievement, function (err, json) {
                if (err) {
                    return next(err);
                }
                achievementInfo = JSON.stringify(json);
                return next();
            });
        },
        // Create/update achievement in tenant where this achievement used
        function (next) {
            return AerospikeAchievementList.findOne({ tenantId: params.tenantId }, function (err, tenant) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return next(err);
                }
                if (!tenant || !_.has(tenant, 'achievements')) {
                    tenant = {
                        achievements: {}
                    };
                }
                tenant.tenantId = params.tenantId;
                tenant.achievements[achievementKey] = achievementInfo;
                // Update back tenant data to aerospike
                var achievementList = new AerospikeAchievementList(tenant);
                return achievementList.save(next);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

// Unpublish achievementList in aerospike
AerospikeAchievementList.unpublish = function (params, callback) {
    var achievementKey = _keyAchievement(params);
    async.series([
        // Remove achievement from tenant where this achievement used
        function (next) {
            return AerospikeAchievementList.findOne({ tenantId: params.tenantId }, function (err, tenant) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return next(err);
                }
                if (err === Errors.DatabaseApi.NoRecordFound) {
                    return next();
                }
                if (!tenant || !_.has(tenant, 'achievements')) {
                    tenant = {
                        achievements: {}
                    };
                }
                tenant.tenantId = params.tenantId;
                delete tenant.achievements[achievementKey];
                // Update back tenant data to aerospike
                var achievementList = new AerospikeAchievementList(tenant);
                if (_.isEmpty(tenant.achievements)) {
                    return achievementList.remove(next);
                }
                return achievementList.save(next);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

AerospikeAchievementList.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeAchievementList();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeAchievementList,
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

AerospikeAchievementList.toJSON = function (object, callback) {
    try {
        if (_.has(object, 'id')) {
            var plain = _copyAchievement({}, object, false);
            return _success(plain, callback);
        } else {
            var plain = _copy({}, object, false);
            return _success(plain, callback);
        }
    } catch (ex) {
        return _error(ex, callback);
    }
};

function _copy(to, from, ext) {
    if (!to) {
        to = {
            achievements: {}
        };
    }
    if (_.isObject(from) && _.has(from, 'tenantId')) {
        to.tenantId = from.tenantId;
    }
    if (_.isObject(from) && _.has(from, 'achievements')) {
        to.achievements = from.achievements;
    }
    return to;
}

function _copyAchievement(to, from, ext) {
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
    return 'tenantId:' + params.tenantId;
}

function _keyAchievement(params) {
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
        var AchievementList = new AerospikeAchievementList();
        result = AchievementList;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
