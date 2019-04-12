var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikePromocodeCampaignList = function AerospikePromocodeCampaignList(promocodeCampaignList) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(promocodeCampaignList);
};
AerospikePromocodeCampaignList._namespace = Config.keyvalue.namespace;
AerospikePromocodeCampaignList._set = 'promocodeCampaignList';
AerospikePromocodeCampaignList._ttl = 0;

module.exports = AerospikePromocodeCampaignList;

//Copies attributes from a parsed json or other source
AerospikePromocodeCampaignList.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the PromocodeCampaignList in the database, overwrites existing items
AerospikePromocodeCampaignList.prototype.save = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('put', {
            model: AerospikePromocodeCampaignList,
            key: key,
            value: { promoCampaigns: self.promocodeCampaigns }
        }, function (err, res) {
            var promocodeCampaignList = new AerospikePromocodeCampaignList(self);
            return _success(promocodeCampaignList, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the PromocodeCampaignList from the database
AerospikePromocodeCampaignList.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikePromocodeCampaignList,
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

// Publish PromocodeCampaignList in aerospike
AerospikePromocodeCampaignList.publish = function (params, callback) {
    var promocodeCampaignKey = _keyCampaign(params);
    var promocodeCampaignInfo;
    async.series([
        // Setup promocode campaign info
        function (next) {
            var promocodeCampaign = new AerospikePromocodeCampaignList();
            _copyCampaign(promocodeCampaign, params, false);
            return AerospikePromocodeCampaignList.toJSON(promocodeCampaign, function (err, json) {
                if (err) {
                    return next(err);
                }
                promocodeCampaignInfo = JSON.stringify(json);
                return next();
            });
        },
        // Create/update promocode campaign in application list where this campaign list used
        function (next) {
            return async.mapSeries(params.appIds, function (appId, appNext) {
                return AerospikePromocodeCampaignList.findOne({ appId: appId }, function (err, app) {
                    if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                        return appNext(err);
                    }
                    if (!app || !_.has(app, 'promocodeCampaigns')) {
                        app = {
                            promocodeCampaigns: {}
                        };
                    }
                    app.appId = appId;
                    app.promocodeCampaigns[promocodeCampaignKey] = promocodeCampaignInfo;
                    // Update back application data to aerospike
                    var promocodeCampaignList = new AerospikePromocodeCampaignList(app);
                    return promocodeCampaignList.save(appNext);
                });
            }, function (err) {
                return next(err);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

// Unpublish promocodeCampaignList in aerospike
AerospikePromocodeCampaignList.unpublish = function (params, callback) {
    var promocodeCampaignKey = _keyCampaign(params);
    async.series([
        // Remove promocode campaign from application list where this campaign list used
        function (next) {
            return async.mapSeries(params.appIds, function (appId, appNext) {
                return AerospikePromocodeCampaignList.findOne({ appId: appId }, function (err, app) {
                    if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                        return appNext(err);
                    }
                    if (err === Errors.DatabaseApi.NoRecordFound) {
                        return appNext();
                    }
                    if (!app || !_.has(app, 'promocodeCampaigns')) {
                        app = {
                            promocodeCampaigns: {}
                        };
                    }
                    app.appId = appId;
                    delete app.promocodeCampaigns[promocodeCampaignKey];
                    // Update back application data to aerospike
                    var promocodeCampaignList = new AerospikePromocodeCampaignList(app);
                    if (_.isEmpty(app.promocodeCampaigns)) {
                        return promocodeCampaignList.remove(appNext);
                    }
                    return promocodeCampaignList.save(appNext);
                });
            }, function (err) {
                return next(err);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

AerospikePromocodeCampaignList.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePromocodeCampaignList();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePromocodeCampaignList,
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

AerospikePromocodeCampaignList.toJSON = function (object, callback) {
    try {
        if (_.has(object, 'id')) {
            var plain = _copyCampaign({}, object, false);
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
            promocodeCampaigns: {}
        };
    }
    if (_.isObject(from) && _.has(from, 'appId')) {
        to.appId = from.appId;
    }
    if (_.isObject(from) && _.has(from, 'promocodeCampaigns')) {
        to.promocodeCampaigns = from.promocodeCampaigns;
    }
    return to;
}

function _copyCampaign(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'promocodeCampaignDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'app:' + params.appId;
}

function _keyCampaign(params) {
    return 'promocodeCampaign:' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var promocodeCampaignList = new AerospikePromocodeCampaignList();
        result = promocodeCampaignList;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
