var _ = require('lodash');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
let logger = require('nodejs-logger')();

var AerospikeVoucherTombolaReserve = function AerospikeVoucherTombolaReserve(game) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(game);
};

//Copies attributes from a parsed json or other source
AerospikeVoucherTombolaReserve.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

AerospikeVoucherTombolaReserve._namespace = Config.keyvalue.namespace;
AerospikeVoucherTombolaReserve._set = 'userVoucher';
AerospikeVoucherTombolaReserve._ttl = 0;

module.exports = AerospikeVoucherTombolaReserve;

AerospikeVoucherTombolaReserve.findAll = function (params, callback) {
    try {
        var key = _key(params);
        logger.debug("AerospikeVoucherTombolaReserve.findAll by key = ",key)
        var items = new AerospikeVoucherTombolaReserve();
        return items.KeyvalueService.exec('get', {
            model: AerospikeVoucherTombolaReserve,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                logger.debug("AerospikeVoucherTombolaReserve.findAll err = ",err)
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                try {
                    logger.debug("AerospikeVoucherTombolaReserve.findAll res.value = ",res.value);
                    const vouchers=[];
                    const obj = JSON.parse(res.value);
                    for (let j in obj){
                        vouchers.push(obj[j]);
                    }
                    return _success(vouchers, callback);
                } catch (ex) {
                    logger.debug("AerospikeVoucherTombolaReserve.findAll ex = ",ex)
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeVoucherTombolaReserve.save = function (params,callback) {
    return AerospikeVoucherTombolaReserve.toJSON(params.reservedVouchers, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(params);
        logger.debug("AerospikeVoucherTombolaReserve.save by key = ",key)
        if (key) {
            try {
                var tombolaReserve = new AerospikeVoucherTombolaReserve();
                return tombolaReserve.KeyvalueService.exec('put', {
                    model: AerospikeVoucherTombolaReserve,
                    key: key,
                    value: { value: JSON.stringify(json) }
                }, function (err, res) {
                    return _success(json, callback);
                });
            } catch (ex) {
                logger.debug("AerospikeVoucherTombolaReserve.save ex:",ex);
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            logger.debug("AerospikeVoucherTombolaReserve.save no key");
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};


function _key(params) {
    return 'voucher:tombola:reserved:' + params.tombolaId;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var Game = new AerospikeVoucherTombolaReserve();
        result = Game;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}

AerospikeVoucherTombolaReserve.toJSON = function (object, callback) {
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
    // var data = AerospikeMapperService.map(from, 'gameDataModel');
    return _.assign(to, from);
}
//Copies attributes from a parsed json or other source
AerospikeVoucherTombolaReserve.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}