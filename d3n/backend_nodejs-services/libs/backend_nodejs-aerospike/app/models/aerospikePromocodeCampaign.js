var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikePromocodeCampaign = function AerospikePromocodeCampaign(promocodeCampaign) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(promocodeCampaign);
};
AerospikePromocodeCampaign._namespace = Config.keyvalue.namespace;
AerospikePromocodeCampaign._set = 'promocodeCampaign';
AerospikePromocodeCampaign._ttl = 0;

module.exports = AerospikePromocodeCampaign;

//Copies attributes from a parsed json or other source
AerospikePromocodeCampaign.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the PromocodeCampaign in the database, overwrites existing items
AerospikePromocodeCampaign.prototype.save = function (callback) {
    var self = this;
    AerospikePromocodeCampaign.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePromocodeCampaign,
                    key: key,
                    value: { promoCampaign: new Buffer(JSON.stringify(json)) }
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

//Removes the PromocodeCampaign from the database
AerospikePromocodeCampaign.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePromocodeCampaign,
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

// Publish promocodeCampaign in aerospike
AerospikePromocodeCampaign.publish = function (params, callback) {
    var promocodeCampaign = new AerospikePromocodeCampaign(params);
    return promocodeCampaign.save(callback);
}

// Unpublish promocodeCampaign from aerospike
AerospikePromocodeCampaign.unpublish = function (params, callback) {
    var promocodeCampaign = new AerospikePromocodeCampaign(params);
    return promocodeCampaign.remove(callback);
}

AerospikePromocodeCampaign.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePromocodeCampaign();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePromocodeCampaign,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.promoCampaign));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePromocodeCampaign.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'promocodeCampaignDataModel');
    return _.assign(to, data);
}

function _key(params) {
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
        var PromocodeCampaign = new AerospikePromocodeCampaign();
        result = PromocodeCampaign;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
