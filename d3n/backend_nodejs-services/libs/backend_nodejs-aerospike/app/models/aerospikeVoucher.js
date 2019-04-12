var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikeVoucher = function AerospikeVoucher(voucher) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(voucher);
};
AerospikeVoucher._namespace = Config.keyvalue.namespace;
AerospikeVoucher._set = 'voucher';
AerospikeVoucher._ttl = 0;

module.exports = AerospikeVoucher;

//Copies attributes from a parsed json or other source
AerospikeVoucher.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the Voucher in the database, overwrites existing items
AerospikeVoucher.prototype.save = function (callback) {
    var self = this;
    AerospikeVoucher.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeVoucher,
                    key: key,
                    value: { voucher: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    if (err) {
                        return _error(err, callback);
                    }
                    return _success(self, callback);
                });
            } catch (ex) {
                logger.debug('AerospikeVoucher.prototype.save exception:"', ex, '"');
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });    
};

//Removes the Voucher from the database
AerospikeVoucher.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeVoucher,
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

// Publish voucher in aerospike
AerospikeVoucher.publish = function (params, callback) {
    var voucher = new AerospikeVoucher(params);
    return voucher.save(callback);
}

// Unpublish voucher from aerospike
AerospikeVoucher.unpublish = function (params, callback) {
    var voucher = new AerospikeVoucher(params);
    return voucher.remove(callback);
}

AerospikeVoucher.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeVoucher();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeVoucher,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.voucher));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeVoucher.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'voucherDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'voucher:' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var Voucher = new AerospikeVoucher();
        result = Voucher;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
