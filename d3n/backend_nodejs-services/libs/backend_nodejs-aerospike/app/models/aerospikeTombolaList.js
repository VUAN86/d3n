var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikeTombolaList = function AerospikeTombolaList(tombolaList) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(tombolaList);
};
AerospikeTombolaList._namespace = Config.keyvalue.namespace;
AerospikeTombolaList._set = 'availableTombolaList';
AerospikeTombolaList._ttl = 0;

module.exports = AerospikeTombolaList;

//Copies attributes from a parsed json or other source
AerospikeTombolaList.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the TombolaList in the database, overwrites existing items
AerospikeTombolaList.prototype.save = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('put', {
            model: AerospikeTombolaList,
            key: key,
            value: { tombolas: self.tombolas }
        }, function (err, res) {
            var TombolaList = new AerospikeTombolaList(self);
            return _success(TombolaList, callback);
        });
    } catch (ex) {
        logger.error("AerospikeTombolaList.save : ",ex);
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the TombolaList from the database
AerospikeTombolaList.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeTombolaList,
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

// Publish TombolaList in aerospike
AerospikeTombolaList.publish = function (params, callback) {
    var oldApplicationsIds;
    var tombolaKey = _keyTombola(params);
    return async.mapSeries(params.applicationsIds, function (applicationId, nextApplicationId) {
        try {
            var tombolaInfo;
            var appId = parseInt(applicationId);
            return async.series([
                // Setup tombola info
                function (next) {
                    var tombola = new AerospikeTombolaList();
                    _copyTombola(tombola, params, false);
                    return AerospikeTombolaList.toJSON(tombola, function (err, json) {
                        if (err) {
                            return next(err);
                        }
                        tombolaInfo = JSON.stringify(json);
                        return next();
                    });
                },
                // Create/update tombola in app where this tombola used
                function (next) {
                    return AerospikeTombolaList.findOne({ appId: appId }, function (err, app) {
                        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                            return next(err);
                        }
                        if (!app || !_.has(app, 'tombolas')) {
                            app = {
                                tombolas: {}
                            };
                        } else if (!oldApplicationsIds && err !== Errors.DatabaseApi.NoRecordFound && app.tombolas[tombolaKey]) {
                            oldApplicationsIds = JSON.parse(app.tombolas[tombolaKey]).applicationsIds;
                            _.forEach(params.applicationsIds, function (newApplicationId) {
                                _.pull(oldApplicationsIds, newApplicationId);
                            });
                        }
                        app.appId = appId;
                        app.tombolas[tombolaKey] = tombolaInfo;
                        var tombolaList = new AerospikeTombolaList(app);
                        return tombolaList.save(next);
                    });
                }
            ], nextApplicationId);
        } catch (ex) {
            return _error(ex, nextApplicationId);
        }
    }, function (err) {
        if (err) {
            return _error(err, callback);
        }
        // Remove list entries by old application ids
        if (!_.isEmpty(oldApplicationsIds)) {
            params.applicationsIds = oldApplicationsIds;
            return AerospikeTombolaList.unpublish(params, callback);
        }
        return _success(null, callback);
    });
}

// Unpublish tombolaList in aerospike
AerospikeTombolaList.unpublish = function (params, callback) {
    var tombolaKey = _keyTombola(params);
    return async.mapSeries(params.applicationsIds, function (applicationId, nextApplicationId) {
        try {
            var tombolaInfo;
            var appId = parseInt(applicationId);
            return async.series([
                // Remove tombola from app where this tombola used
                function (next) {
                    return AerospikeTombolaList.findOne({ appId: appId }, function (err, app) {
                        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                            return next(err);
                        }
                        if (err === Errors.DatabaseApi.NoRecordFound) {
                            return next();
                        }
                        if (!app || !_.has(app, 'tombolas')) {
                            app = {
                                tombolas: {}
                            };
                        }
                        app.appId = appId;
                        if (app.tombolas[tombolaKey]) {
                            delete app.tombolas[tombolaKey];
                            // Update back app data to aerospike
                            var tombolaList = new AerospikeTombolaList(app);
                            if (_.isEmpty(app.tombolas)) {
                                return tombolaList.remove(next);
                            }
                            return tombolaList.save(next);
                        }
                        return next();
                    });
                }
            ], nextApplicationId);
        } catch (ex) {
            logger.error("Unpublish tombolaList in aerospike : ",ex);
            return _error(ex, nextApplicationId);
        }
    }, callback);
}

AerospikeTombolaList.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeTombolaList();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeTombolaList,
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

AerospikeTombolaList.toJSON = function (object, callback) {
    try {
        if (_.has(object, 'id')) {
            var plain = _copyTombola({}, object, false);
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
            tombolas: {}
        };
    }
    if (_.isObject(from) && _.has(from, 'appId')) {
        to.appId = from.appId;
    }
    if (_.isObject(from) && _.has(from, 'tombolas')) {
        to.tombolas = from.tombolas;
    }
    return to;
}

function _copyTombola(to, from, ext) {
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

function _key(params) {
    return 'appId:' + params.appId;
}

function _keyTombola(params) {
    return '' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var TombolaList = new AerospikeTombolaList();
        result = TombolaList;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
