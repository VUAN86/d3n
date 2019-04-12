var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikeVoucherList = function AerospikeVoucherList(voucherList) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(voucherList);
};
AerospikeVoucherList._namespace = Config.keyvalue.namespace;
AerospikeVoucherList._set = 'voucherList';
AerospikeVoucherList._ttl = 0;

module.exports = AerospikeVoucherList;

//Copies attributes from a parsed json or other source
AerospikeVoucherList.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the VoucherList in the database, overwrites existing items
AerospikeVoucherList.prototype.save = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('put', {
            model: AerospikeVoucherList,
            key: key,
            value: { vouchers: self.vouchers }
        }, function (err, res) {
            var VoucherList = new AerospikeVoucherList(self);
            return _success(VoucherList, callback);
        });
    } catch (ex) {
        logger.debug('AerospikeVoucherList save error="', ex);
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the VoucherList from the database
AerospikeVoucherList.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeVoucherList,
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

// Publish VoucherList in aerospike
AerospikeVoucherList.publish = function (params, callback) {
    var voucherKey = _keyVoucher(params);
    var voucherInfo;
    async.series([
        // Setup voucher info
        function (next) {
            var voucher = new AerospikeVoucherList();
            _copyVoucher(voucher, params, false);
            return AerospikeVoucherList.toJSON(voucher, function (err, json) {
                if (err) {
                    return next(err);
                }
                voucherInfo = JSON.stringify(json);
                return next();
            });
        },
        // Create/update voucher in tenant where this voucher used
        function (next) {
            return AerospikeVoucherList.findOne({ tenantId: params.tenantId }, function (err, tenant) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return next(err);
                }
                if (!tenant || !_.has(tenant, 'vouchers')) {
                    tenant = {
                        vouchers: {}
                    };
                }
                tenant.tenantId = params.tenantId;
                tenant.vouchers[voucherKey] = voucherInfo;
                // Update back tenant data to aerospike
                var voucherList = new AerospikeVoucherList(tenant);
                return voucherList.save(next);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

// Unpublish voucherList in aerospike
AerospikeVoucherList.unpublish = function (params, callback) {
    var voucherKey = _keyVoucher(params);
    async.series([
        // Remove voucher from tenant where this voucher used
        function (next) {
            return AerospikeVoucherList.findOne({ tenantId: params.tenantId }, function (err, tenant) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return next(err);
                }
                if (err === Errors.DatabaseApi.NoRecordFound) {
                    return next();
                }
                if (!tenant || !_.has(tenant, 'vouchers')) {
                    tenant = {
                        vouchers: {}
                    };
                }
                tenant.tenantId = params.tenantId;
                delete tenant.vouchers[voucherKey];
                // Update back tenant data to aerospike
                var voucherList = new AerospikeVoucherList(tenant);
                if (_.isEmpty(tenant.vouchers)) {
                    return voucherList.remove(next);
                }
                return voucherList.save(next);
            });
        }
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

AerospikeVoucherList.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeVoucherList();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeVoucherList,
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

AerospikeVoucherList.toJSON = function (object, callback) {
    try {
        if (_.has(object, 'id')) {
            var plain = _copyVoucher({}, object, false);
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
            vouchers: {}
        };
    }
    if (_.isObject(from) && _.has(from, 'tenantId')) {
        to.tenantId = from.tenantId;
    }
    if (_.isObject(from) && _.has(from, 'vouchers')) {
        to.vouchers = from.vouchers;
    }
    return to;
}

function _copyVoucher(to, from, ext) {
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
    return 'tenantId:' + params.tenantId;
}

function _keyVoucher(params) {
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
        var VoucherList = new AerospikeVoucherList();
        result = VoucherList;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
