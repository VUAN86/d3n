var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var PromocodeData = require('./../config/promocode.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePromocodeCounter = KeyvalueService.Models.AerospikePromocodeCounter;

describe('PromocodeCounter', function () {
    
    it('test PromocodeCounter publish nonunique promocodes', function (done) {
        async.series([
            function (next) {
                AerospikePromocodeCounter.getAmount(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikePromocodeCounter.getUsedAmount(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // publish
            function (next) {
                AerospikePromocodeCounter.publish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // unpublish
            function (next) {
                AerospikePromocodeCounter.unpublish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
        ], done);
    });
    
    it('test PromocodeCounter publish unique promocodes', function (done) {
        async.series([
            function (next) {
                AerospikePromocodeCounter.getAmount(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikePromocodeCounter.getUsedAmount(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // publish
            function (next) {
                AerospikePromocodeCounter.publish(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // unpublish
            function (next) {
                AerospikePromocodeCounter.unpublish(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
        ], done);
    });

    it('test PromocodeCounter findAll', function (done) {
        var filter = {
            code: 'TEST',
            limit: 2,
            offset: 0
        };
        var INSTANCE = {
            code: 'TEST',
            userId: DataIds.EMAIL_USER_ID,
            expirationDate: '2017-01-01T09:00:00Z',
            usedOnDate: '2018-01-01T09:00:00Z',
            moneyValue: 2,
            creditValue: 0,
            bonuspointsValue: 0,
            moneyTransactionId: '22222',
            creditTransactionId: 0,
            bonuspointsTransactionId: 0,
        };
        async.series([
            function (next) {
                AerospikePromocodeCounter.findAll(filter, function (err, res) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikePromocodeCounter.getUsedAmount(filter, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                var instance = new AerospikePromocodeCounter();
                instance.copyInstance(INSTANCE);
                instance.instanceId = 0;
                instance.saveInstance(function (err) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                var instance = new AerospikePromocodeCounter();
                instance.copyInstance(INSTANCE);
                instance.instanceId = 1;
                instance.saveInstance(function (err) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikePromocodeCounter.incrementUsed(filter, function (err, amount) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikePromocodeCounter.incrementUsed(filter, function (err, amount) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikePromocodeCounter.findAll(filter, function (err, res) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(res.length, filter.limit);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikePromocodeCounter.getUsedAmount(filter, function (err, amount) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(amount, filter.limit);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
        ], done);
    });

});