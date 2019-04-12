var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikeTombola = function AerospikeTombola(tombola) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(tombola);
};
AerospikeTombola._namespace = Config.keyvalue.namespace;
AerospikeTombola._set = 'tombola';
AerospikeTombola._ttl = 0;

module.exports = AerospikeTombola;

//Copies attributes from a parsed json or other source
AerospikeTombola.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

AerospikeTombola.prototype.boostCopy = function (object) {
    var self = this;
    return _copyBoost(self, object, true);
}

//Saves the Tombola in the database, overwrites existing items
AerospikeTombola.prototype.save = function (callback) {
    var self = this;
    AerospikeTombola.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeTombola,
                    key: key,
                    value: { tombola: new Buffer(JSON.stringify(json)) }
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
//Saves the Tombola Boost in the database, overwrites existing items
AerospikeTombola.prototype.saveBoost = function (callback) {
    var self = this;
    AerospikeTombola.toBoostJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _boostkey(self);
        logger.debug('saveBoost key=',key, ' JSON.stringify(json)=',JSON.stringify(json));
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeTombola,
                    key: key,
                    value: { tombola: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    if (err) {
                        logger.error('saveBoost err:',err);
                        return _error(err, callback);
                    }
                    return _success(self, callback);
                });
            } catch (ex) {
                logger.error('saveBoost err:',ex);
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the Tombola from the database
AerospikeTombola.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeTombola,
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

// Publish tombola in aerospike
AerospikeTombola.publish = function (params, callback) {
    return AerospikeTombola.findOne(params, function (err, tombola) {
        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
            return next(err);
        }
        if (err === Errors.DatabaseApi.NoRecordFound) {
            tombola = new AerospikeTombola();
        }
        tombola = _copy(tombola, params);
        return tombola.save(callback);
    });
}

// Publish tombola boost in aerospike
AerospikeTombola.publishBoost = function (params, callback) {
    return AerospikeTombola.findBoostOne(params, function (err, tombola) {
        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
            return next(err);
        }
        if (err === Errors.DatabaseApi.NoRecordFound) {
            tombola = new AerospikeTombola();
        }
        tombola = _copyBoost(tombola, params);
        return tombola.saveBoost(callback);
    });
}

// Unpublish tombola from aerospike
AerospikeTombola.unpublish = function (params, callback) {
    var tombola = new AerospikeTombola(params);
    return tombola.remove(callback);
}

AerospikeTombola.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeTombola();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeTombola,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.tombola));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};


AerospikeTombola.findBoostOne = function (params, callback) {
    try {
        var key = _boostkey(params);
        var instance = new AerospikeTombola();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeTombola,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                logger.error("AerospikeTombola.findBoostOne error2: ", err);
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.tombola));
                instance = instance.boostCopy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        logger.error("AerospikeTombola.findBoostOne error: ", ex);
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeTombola.toJSON = function (object, callback) {
    try {
        var plain = _copy({}, object, false);
        return _success(plain, callback);
    } catch (ex) {
        return _error(ex, callback);
    }
};

AerospikeTombola.toBoostJSON = function (object, callback) {
    try {
        var plain = _copyBoost({}, object, false);
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
    var data = AerospikeMapperService.map(from, 'tombolaDataModel');
    return _.assign(to, data);
}

function _copyBoost(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    if (_.has(from, 'status')) {
        from.status = from.status.toUpperCase();
    }
    var data = AerospikeMapperService.map(from, 'tombolaBoostModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'tombola:' + params.id;
}

function _boostkey(params) {
    return 'tombola:' + params.id + ':boost';
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var Tombola = new AerospikeTombola();
        result = Tombola;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
