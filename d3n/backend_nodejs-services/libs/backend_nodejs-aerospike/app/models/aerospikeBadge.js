var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeBadge = function AerospikeBadge(badge) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(badge);
};
AerospikeBadge._namespace = Config.keyvalue.namespace;
AerospikeBadge._set = 'badge';
AerospikeBadge._ttl = 0;

module.exports = AerospikeBadge;

//Copies attributes from a parsed json or other source
AerospikeBadge.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the Badge in the database, overwrites existing items
AerospikeBadge.prototype.save = function (callback) {
    var self = this;
    AerospikeBadge.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeBadge,
                    key: key,
                    value: { badge: new Buffer(JSON.stringify(json)) }
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

//Removes the Badge from the database
AerospikeBadge.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeBadge,
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

// Publish badge in aerospike
AerospikeBadge.publish = function (params, callback) {
    var badge = new AerospikeBadge(params);
    return badge.save(callback);
}

// Unpublish badge from aerospike
AerospikeBadge.unpublish = function (params, callback) {
    return AerospikeBadge.findOne(params, function (err, badge) {
        try {
            if (err) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            }
            badge.status = 'inactive';
            return badge.save(callback);
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
    });
}

AerospikeBadge.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeBadge();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeBadge,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.badge));
                instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeBadge.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'badgeDataModel');
    return _.assign(to, data);
}

function _key(params) {
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
        var Badge = new AerospikeBadge();
        result = Badge;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
