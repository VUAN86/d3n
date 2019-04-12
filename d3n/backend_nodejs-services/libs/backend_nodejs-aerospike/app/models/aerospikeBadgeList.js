var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeBadgeList = function AerospikeBadgeList(badgeList) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(badgeList);
};
AerospikeBadgeList._namespace = Config.keyvalue.namespace;
AerospikeBadgeList._set = 'badgeList';
AerospikeBadgeList._ttl = 0;

module.exports = AerospikeBadgeList;

//Copies attributes from a parsed json or other source
AerospikeBadgeList.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the BadgeList in the database, overwrites existing items
AerospikeBadgeList.prototype.save = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('put', {
            model: AerospikeBadgeList,
            key: key,
            value: { badges: self.badges }
        }, function (err, res) {
            var BadgeList = new AerospikeBadgeList();
            BadgeList = BadgeList.copy(self);
            return _success(BadgeList, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the BadgeList from the database
AerospikeBadgeList.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeBadgeList,
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

// Publish BadgeList in aerospike
AerospikeBadgeList.publish = function (params, callback) {
    var badgeKey = _keyBadge(params);
    var badgeInfo;
    async.series([
        // Setup badge info
        function (next) {
            var badge = new AerospikeBadgeList();
            _copyBadge(badge, params, false);
            return AerospikeBadgeList.toJSON(badge, function (err, json) {
                if (err) {
                    return next(err);
                }
                badgeInfo = JSON.stringify(json);
                return next();
            });
        },
        // Create/update badge in tenant where this badge used
        function (next) {
            return AerospikeBadgeList.findOne({ tenantId: params.tenantId }, function (err, tenant) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return next(err);
                }
                if (!tenant || !_.has(tenant, 'badges')) {
                    tenant = {
                        badges: {}
                    };
                }
                tenant.tenantId = params.tenantId;
                tenant.badges[badgeKey] = badgeInfo;
                // Update back tenant data to aerospike
                var badgeList = new AerospikeBadgeList(tenant);
                return badgeList.save(next);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

// Unpublish badgeList in aerospike
AerospikeBadgeList.unpublish = function (params, callback) {
    var badgeKey = _keyBadge(params);
    async.series([
        // Remove badge from tenant where this badge used
        function (next) {
            return AerospikeBadgeList.findOne({ tenantId: params.tenantId }, function (err, tenant) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return next(err);
                }
                if (err === Errors.DatabaseApi.NoRecordFound) {
                    return next();
                }
                if (!tenant || !_.has(tenant, 'badges')) {
                    tenant = {
                        badges: {}
                    };
                }
                tenant.tenantId = params.tenantId;
                delete tenant.badges[badgeKey];
                // Update back tenant data to aerospike
                var badgeList = new AerospikeBadgeList(tenant);
                if (_.isEmpty(tenant.badges)) {
                    return badgeList.remove(next);
                }
                return badgeList.save(next);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

AerospikeBadgeList.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeBadgeList();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeBadgeList,
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

AerospikeBadgeList.toJSON = function (object, callback) {
    try {
        if (_.has(object, 'id')) {
            var plain = _copyBadge({}, object, false);
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
            badges: {}
        };
    }
    if (_.isObject(from) && _.has(from, 'tenantId')) {
        to.tenantId = from.tenantId;
    }
    if (_.isObject(from) && _.has(from, 'badges')) {
        to.badges = from.badges;
    }
    return to;
}

function _copyBadge(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'badgeDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'tenantId:' + params.tenantId + ':' + params.type;
}

function _keyBadge(params) {
    return 'badge:' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var BadgeList = new AerospikeBadgeList();
        result = BadgeList;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
