var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeLiveMessage = function AerospikeLiveMessage(liveMessage) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(liveMessage);
};
AerospikeLiveMessage._namespace = Config.keyvalue.namespace;
AerospikeLiveMessage._set = 'liveMessage';
AerospikeLiveMessage._ttl = 0;

module.exports = AerospikeLiveMessage;

//Copies attributes from a parsed json or other source
AerospikeLiveMessage.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the live message in the database, overwrites existing items
AerospikeLiveMessage.prototype.save = function (callback) {
    var self = this;
    AerospikeLiveMessage.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeLiveMessage,
                    key: key,
                    value: { liveMessage: new Buffer(JSON.stringify(json)) }
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

//Removes the live message from the database
AerospikeLiveMessage.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeLiveMessage,
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

// Publish live message in aerospike
AerospikeLiveMessage.publish = function (params, callback) {
    return AerospikeLiveMessage.findOne(params, function (err, liveMessage) {
        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
            return next(err);
        }
        if (err === Errors.DatabaseApi.NoRecordFound) {
            liveMessage = new AerospikeLiveMessage();
        }
        liveMessage = _copy(liveMessage, params);
        return liveMessage.save(callback);
    });
}

// Unpublish live message from aerospike
AerospikeLiveMessage.unpublish = function (params, callback) {
    var liveMessage = new AerospikeLiveMessage(params);
    return liveMessage.remove(callback);
}

AerospikeLiveMessage.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeLiveMessage();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeLiveMessage,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.liveMessage));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeLiveMessage.toJSON = function (object, callback) {
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
    if (_.has(from, 'status')) {
        from.status = from.status.toUpperCase();
    }
    var data = AerospikeMapperService.map(from, 'liveMessageDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'liveMessage:' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var liveMessage = new AerospikeLiveMessage();
        result = liveMessage;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
