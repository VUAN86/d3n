var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeVoucherCounter = function AerospikeVoucherCounter(voucherCounter) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(voucherCounter);
};
AerospikeVoucherCounter._namespace = Config.keyvalue.namespace;
AerospikeVoucherCounter._set = 'userVoucher';
AerospikeVoucherCounter._ttl = 0;

module.exports = AerospikeVoucherCounter;

//Copies attributes from a itself or other source
AerospikeVoucherCounter.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
};

//Saves the voucher creation counter in the database, overwrites existing items
AerospikeVoucherCounter.prototype.save = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('put', {
            model: AerospikeVoucherCounter,
            key: key,
            value: { creationCnt: self.amount }
        }, function (err, res) {
            return _success(res.creationCnt, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Saves the voucher instance in the database, overwrites existing items
AerospikeVoucherCounter.prototype.saveInstance = function (callback) {
    var self = this;
    try {
        var key = _keyInstance(self);
        self.KeyvalueService.exec('put', {
            model: AerospikeVoucherCounter,
            key: key,
            value: { usrVoucher: new Buffer(JSON.stringify({ code: self.code, expirationDate: self.expirationDate })) }
        }, function (err, res) {
            return _success(res.creationCnt, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the voucher counter from the database
AerospikeVoucherCounter.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeVoucherCounter,
            key: key
        }, function (err, res) {
            return callback(err);
        });
    } catch (ex) {
        return callback(ex);
    }
};

//Removes the voucher instance from the database
AerospikeVoucherCounter.prototype.removeInstance = function (callback) {
    try {
        var self = this;
        var key = _keyInstance(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeVoucherCounter,
            key: key
        }, function (err, res) {
            return callback(err);
        });
    } catch (ex) {
        return callback(ex);
    }
};

AerospikeVoucherCounter.prototype.increment = function (bin, callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikeVoucherCounter,
            key: key,
            bin: bin,
            value: 1
        }, function (err, newVal) {
            return callback(err, newVal);
        });
    } catch (ex) {
        return setImmediate(callback, ex);
    }
};

AerospikeVoucherCounter.prototype.decrement = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikeVoucherCounter,
            key: key,
            bin: bin,
            value: -1
        }, function (err, newVal) {
            return callback(err, newVal);
        });
    } catch (ex) {
        return setImmediate(callback, ex);
    }
};

AerospikeVoucherCounter.generate = function (params, callback) {
    async.series([
        // Increase amount of voucher instances (if already exists)
        function (next) {
            try {
                var key = _key(params);
                var instance = new AerospikeVoucherCounter();
                return instance.KeyvalueService.exec('get', {
                    model: AerospikeVoucherCounter,
                    key: key
                }, function (err, res) {
                    if (!err && _.has(res, 'creationCnt') && _.isNumber(res.creationCnt)) {
                        params.amount += res.creationCnt;
                    }
                    return next();
                });
            } catch (ex) {
                return _error(ex, next);
            }
        },
        // Setup voucher creation counter
        function (next) {
            try {
                var voucherCounter = new AerospikeVoucherCounter(params);
                return voucherCounter.save(next);
            } catch (ex) {
                return _error(ex, next);
            }
        },
        // Generate voucher instances
        function (next) {
            try {
                // Repeat x times (x = amount) to save each voucher instance
                return async.timesSeries(params.amount, function (index, nextInstance) {
                    var instanceParams = _.clone(params);
                    instanceParams.instanceId = index;
                    var voucherCounter = new AerospikeVoucherCounter(instanceParams);
                    return voucherCounter.saveInstance(nextInstance);
                }, next);
            } catch (ex) {
                return _error(ex, next);
            }
        },
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
};

AerospikeVoucherCounter.generateCodes = function (params, callback) {
    async.series([
        // Increase amount of voucher instances (if already exists)
        function (next) {
            try {
                var key = _key(params);
                var instance = new AerospikeVoucherCounter();
                return instance.KeyvalueService.exec('get', {
                    model: AerospikeVoucherCounter,
                    key: key
                }, function (err, res) {
                    if (!err && _.has(res, 'creationCnt') && _.isNumber(res.creationCnt)) {
                        params.amount += res.creationCnt;
                    }
                    return next();
                });
            } catch (ex) {
                return _error(ex, next);
            }
        },
        // Setup voucher creation counter
        function (next) {
            try {
                var voucherCounter = new AerospikeVoucherCounter(params);
                return voucherCounter.save(next);
            } catch (ex) {
                return _error(ex, next);
            }
        },
        // Generate voucher instances
        function (next) {
            try {
                // Create one voucher instance per code
                var index = 0;
                return async.mapSeries(params.codes, function (code, nextInstance) {
                    var instanceParams = _.clone(params);
                    instanceParams.instanceId = index;
                    instanceParams.code = code.code;
                    instanceParams.expirationDate = code.expirationDate;
                    var voucherCounter = new AerospikeVoucherCounter(instanceParams);
                    return voucherCounter.saveInstance(nextInstance);
                }, next);
            } catch (ex) {
                return _error(ex, next);
            }
        },
    ], function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
};

AerospikeVoucherCounter.incrementConsuming = function (params, callback) {
    var voucherCounter = new AerospikeVoucherCounter(params);
    return voucherCounter.increment('consumingCnt', callback);
};

AerospikeVoucherCounter.incrementLock = function (params, callback) {
    var voucherCounter = new AerospikeVoucherCounter(params);
    return voucherCounter.increment('lockCnt', callback);
};

AerospikeVoucherCounter.decrementConsuming = function (params, callback) {
    var voucherCounter = new AerospikeVoucherCounter(params);
    return voucherCounter.decrement('consumingCnt', callback);
};

AerospikeVoucherCounter.decrementLock = function (params, callback) {
    var voucherCounter = new AerospikeVoucherCounter(params);
    return voucherCounter.decrement('lockCnt', callback);
};

AerospikeVoucherCounter.remove = function (params, callback) {
    var voucherCounter = new AerospikeVoucherCounter(params);
    return voucherCounter.remove(callback);
};

AerospikeVoucherCounter.getAmount = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeVoucherCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeVoucherCounter,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                return _success(res.creationCnt, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeVoucherCounter.getUsedAmount = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeVoucherCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeVoucherCounter,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var createdCount = res.creationCnt || 0;
                var consumedCount = res.consumingCnt || 0;
                var lockedCount = res.lockCnt || 0;
                var usedAmount = createdCount - consumedCount - lockedCount;
                return _success(usedAmount, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeVoucherCounter.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeVoucherCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeVoucherCounter,
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

AerospikeVoucherCounter.toJSON = function (object, callback) {
    var plain = _copy({}, object, false);
    return _success(plain, callback);
};

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'voucherInstanceDataModel');
    return _.assign(to, data);
};

function _key(params) {
    return 'voucher:' + params.id + ':counter';
};

function _keyInstance(params) {
    return 'voucher:' + params.id + ':' + params.instanceId;
};

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
