var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var TextDecoder = require('text-encoding').TextDecoder;
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);
var logger = require('nodejs-logger')();

var AerospikePromocodeCounter = function AerospikePromocodeCounter(promocodeCounter) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(promocodeCounter);
};
AerospikePromocodeCounter._namespace = Config.keyvalue.namespace;
AerospikePromocodeCounter._set = 'promocode';
AerospikePromocodeCounter._ttl = 0;

module.exports = AerospikePromocodeCounter;

//Copies attributes from a itself or other source
AerospikePromocodeCounter.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
};

//Copies attributes from a itself or other source
AerospikePromocodeCounter.prototype.copyCode = function (object) {
    var self = this;
    return _copyCode(self, object, true);
};

//Copies attributes from a itself or other source
AerospikePromocodeCounter.prototype.copyInstance = function (object) {
    var self = this;
    return _copyInstance(self, object, true);
};

//Saves the promocode creation counter in the database, overwrites existing items (not used in current implementation)
AerospikePromocodeCounter.prototype.save = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('put', {
            model: AerospikePromocodeCounter,
            key: key,
            value: { creationCnt: self.number, consumingCnt: 0 }
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(res.creationCnt, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Saves the promocode code in the database, overwrites existing items
AerospikePromocodeCounter.prototype.saveCode = function (callback) {
    var self = this;
    try {
        var key = _keyCode(self);
        self.KeyvalueService.exec('put', {
            model: AerospikePromocodeCounter,
            key: key,
            value: {
                promocode: new Buffer(JSON.stringify({
                    code: self.code,
                    numberOfUses: 1, //self.numberOfUses, // #7372 hardoded 1 for now
                    expirationDate: self.expirationDate,
                    generationDate: self.generationDate,
                    moneyValue: self.moneyValue,
                    creditValue: self.creditValue,
                    bonuspointsValue: self.bonuspointsValue,
                    promocodeClassId: self.promocodeClassId,
                    promocodeCampaignId: self.promocodeCampaignId.toString(),
                }))
            }
        }, function (err, res) {
            if (err) {
                logger.error('AerospikePromocodeCounter.saveCode  error:',err)
                return _error(err, callback);
            }
            return _success(res.promocode, callback);
        });
    } catch (ex) {
        logger.error('AerospikePromocodeCounter.saveCode  ex:',ex)
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Saves the promocode code instance in the database, overwrites existing items (not used in current implementation)
AerospikePromocodeCounter.prototype.saveInstance = function (callback) {
    var self = this;
    try {
        var key = _keyInstance(self);
        self.KeyvalueService.exec('put', {
            model: AerospikePromocodeCounter,
            key: key,
            value: {
                promocode: new Buffer(JSON.stringify({
                    id: self.id,
                    code: self.code,
                    userId: self.userId,
                    expirationDate: self.expirationDate,
                    usedOnDate: self.usedOnDate,
                    moneyValue: self.moneyValue,
                    creditValue: self.creditValue,
                    bonuspointsValue: self.bonuspointsValue,
                    moneyTransactionId: self.moneyTransactionId,
                    creditTransactionId: self.creditTransactionId,
                    bonuspointsTransactionId: self.bonuspointsTransactionId,
                }))
            }
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(res.promocode, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the promocode counter from the database
AerospikePromocodeCounter.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePromocodeCounter,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return callback(ex);
    }
};

//Removes the promocode code from the database
AerospikePromocodeCounter.prototype.removeCode = function (callback) {
    try {
        var self = this;
        var key = _keyCode(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePromocodeCounter,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return callback(ex);
    }
};

//Removes the promocode code instance from the database
AerospikePromocodeCounter.prototype.removeInstance = function (callback) {
    try {
        var self = this;
        var key = _keyInstance(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePromocodeCounter,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return callback(ex);
    }
};

AerospikePromocodeCounter.prototype.increment = function (bin, callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikePromocodeCounter,
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

AerospikePromocodeCounter.prototype.decrement = function (bin, callback) {
    try {
        var self = this;
        var key = _key(self);
        self.KeyvalueService.exec('incrAndGet', {
            model: AerospikePromocodeCounter,
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

AerospikePromocodeCounter.publish = function (params, callback) {
    try {
        return async.mapSeries(params.promocodes, function (promocodeTemplate, nextPromocodeTemplate) {
            var promocodes = [];
            // let maxClassNumber=promocodeTemplate.class.number > 9999 ? 9999 : promocodeTemplate.class.number;
            // for (var number = 1; number <= maxClassNumber; number++) {
            var promocodeCode = promocodeTemplate.code;
            var promocodeClassId = promocodeTemplate.code;
            // if (promocodeTemplate.class.isUnique) {
            //     promocodeCode += number;
            //     promocodeClassId += ':' + number;
            // }
            var promocode = {
                code: promocodeCode,
                number: promocodeTemplate.class.isUnique ? 1 : promocodeTemplate.class.number,
                numberOfUses: promocodeTemplate.numberOfUses,
                expirationDate: promocodeTemplate.expirationDate,
                generationDate: promocodeTemplate.generationDate,
                moneyValue: promocodeTemplate.moneyValue,
                creditValue: promocodeTemplate.creditValue,
                bonuspointsValue: promocodeTemplate.bonuspointsValue,
                promocodeClassId: promocodeClassId,
                promocodeCampaignId: promocodeTemplate.promocodeCampaignId.toString(),
            }
            logger.debug('AerospikePromocodeCounter.publish promocode=', promocode)
            var promocodeFound = _.find(promocodes, promocode);
            if (!promocodeFound) {
                promocodes.push(promocode);
            }
            // }
            return async.mapSeries(promocodes, function (promocode, nextPromocode) {
                var alreadyPublished = false;
                return async.series([
                    // Check if promocode counter already published
                    function (nextPromocodeEntry) {
                        try {
                            return AerospikePromocodeCounter.getAmount(promocode, function (err, amount) {
                                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                    return nextPromocodeEntry(err);
                                }
                                alreadyPublished = amount && amount > 0;
                                return nextPromocodeEntry();
                            });
                        } catch (ex) {
                            return setImmediate(nextPromocodeEntry, ex);
                        }
                    },
                    // Setup promocode counter
                    function (nextPromocodeEntry) {
                        try {
                            if (alreadyPublished) {
                                return setImmediate(nextPromocodeEntry);
                            }
                            var promocodeCounter = new AerospikePromocodeCounter(promocode);
                            return promocodeCounter.save(nextPromocodeEntry);
                        } catch (ex) {
                            return setImmediate(nextPromocodeEntry, ex);
                        }
                    },
                    // Setup promocode instance counter
                    function (nextPromocodeEntry) {
                        try {
                            if (alreadyPublished) {
                                return setImmediate(nextPromocodeEntry);
                            }
                            var promocodeCounter = new AerospikePromocodeCounter();
                            _copyCode(promocodeCounter, promocode);
                            return promocodeCounter.saveCode(nextPromocodeEntry);
                        } catch (ex) {
                            return setImmediate(nextPromocodeEntry, ex);
                        }
                    },
                ], nextPromocode);
            }, nextPromocodeTemplate);
        }, function (err) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(ex, callback);
    }
};

AerospikePromocodeCounter.unpublish = function (params, callback) {
    try {
        return async.mapSeries(params.promocodes, function (promocodeTemplate, nextPromocodeTemplate) {
            var promocodes = [];
            for (var number = 1; number <= promocodeTemplate.class.number; number++) {
                var promocodeCode = promocodeTemplate.code;
                var promocodeClassId = promocodeTemplate.code;
                if (promocodeTemplate.class.isUnique) {
                    promocodeCode += number;
                    promocodeClassId += ':' + number;
                }
                var promocode = {
                    code: promocodeCode,
                    number: promocodeTemplate.class.isUnique ? 1 : promocodeTemplate.class.number,
                    numberOfUses: promocodeTemplate.numberOfUses,
                    expirationDate: promocodeTemplate.expirationDate,
                    generationDate: promocodeTemplate.generationDate,
                    moneyValue: promocodeTemplate.moneyValue,
                    creditValue: promocodeTemplate.creditValue,
                    bonuspointsValue: promocodeTemplate.bonuspointsValue,
                    promocodeClassId: promocodeClassId,
                    promocodeCampaignId: promocodeTemplate.promocodeCampaignId.toString(),
                }
                var promocodeFound = _.find(promocodes, promocode);
                if (!promocodeFound) {
                    promocodes.push(promocode);
                }
            }
            return async.mapSeries(promocodes, function (promocode, nextPromocode) {
                try {
                    var promocodeCounter = new AerospikePromocodeCounter();
                    _copyCode(promocodeCounter, promocode);
                    return promocodeCounter.removeCode(nextPromocode);
                } catch (ex) {
                    return nextPromocode(ex);
                }
            }, nextPromocodeTemplate);
        }, function (err) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(ex, callback);
    }
};

AerospikePromocodeCounter.incrementUsed = function (params, callback) {
    var promocodeCounter = new AerospikePromocodeCounter(params);
    return promocodeCounter.increment('consumingCnt', callback);
};

AerospikePromocodeCounter.decrementUsed = function (params, callback) {
    var promocodeCounter = new AerospikePromocodeCounter(params);
    return promocodeCounter.decrement('consumingCnt', callback);
};

AerospikePromocodeCounter.remove = function (params, callback) {
    var promocodeCounter = new AerospikePromocodeCounter(params);
    return promocodeCounter.remove(callback);
};

AerospikePromocodeCounter.getAmount = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePromocodeCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePromocodeCounter,
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

AerospikePromocodeCounter.getUsedAmount = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePromocodeCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePromocodeCounter,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                return _success(res.consumingCnt, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePromocodeCounter.findOne = function (params, callback) {
    try {
        var key = _keyInstance(params);
        var instance = new AerospikePromocodeCounter();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePromocodeCounter,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.promocode));
                instance.copyInstance(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

//Find profile sync entry by specifying filter criteria
AerospikePromocodeCounter.findAll = function (params, callback) {
    if (!_.has(params, 'code')) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
    try {
        var keys = [];
        var fromId = params.offset || 0;
        var tillId = params.limit || Config.keyvalue.limit;
        for (instanceId = fromId; instanceId < tillId; instanceId++) {
            var key = _keyInstance({ code: params.code, instanceId: instanceId });
            keys.push(key);
        }
        var instance = new AerospikePromocodeCounter();
        return instance.KeyvalueService.exec('list', {
            model: AerospikePromocodeCounter,
            keys: keys
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var instances = [];
                _.forEach(res, function (item) {
                    var itemJson = JSON.parse(new TextDecoder("utf-8").decode(item.promocode));
                    var instance = new AerospikePromocodeCounter();
                    instance.copyInstance(itemJson);
                    instances.push(instance);
                });
                return _success(instances, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePromocodeCounter.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'promocodeDataModel');
    return _.assign(to, data);
};

function _copyCode(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'promocodeCodeDataModel');
    return _.assign(to, data);
};

function _copyInstance(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'promocodeInstanceDataModel');
    return _.assign(to, data);
};

function _key(params) {
    return 'promocode:' + params.code + ':counter';
};

function _keyCode(params) {
    return 'promocode:' + params.code;
};

function _keyInstance(params) {
    return 'promocode:' + params.code + ':' + params.instanceId;
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
