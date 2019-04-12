var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeAdvertisementCounter = KeyvalueService.Models.AerospikeAdvertisementCounter;

var advertisementData = {
    AD_1: {
        id: 1,
        advertisementProviderId:1
    },
    AD_2: {
        id: 2,
        advertisementProviderId:2
    },
    AD_3: {
        id: 3,
        advertisementProviderId:2
    }
};

describe('AdvertisementCounter', function () {
    
    beforeEach(function (done) {
        var items = [];
        for(var k in advertisementData) {
            items.push(advertisementData[k]);
        }
        
        async.mapSeries(items, function (item, cbItem) {
            AerospikeAdvertisementCounter.remove(item, function (err) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return cbItem(err);
                }
                return cbItem();
            });
        }, done);
    });
    
    it('test increment/decrement', function (done) {
        
        async.series([
            function (next) {
                AerospikeAdvertisementCounter.getCounter(advertisementData.AD_1, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // increment
            function (next) {
                AerospikeAdvertisementCounter.increment(advertisementData.AD_1, function (err, newVal) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(newVal, 1);
                        AerospikeAdvertisementCounter.getCounter(advertisementData.AD_1, function (err, res) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(res, 1);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // increment again
            function (next) {
                AerospikeAdvertisementCounter.increment(advertisementData.AD_1, function (err, newVal) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(newVal, 2);
                        AerospikeAdvertisementCounter.getCounter(advertisementData.AD_1, function (err, res) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(res, 2);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // decrement
            function (next) {
                AerospikeAdvertisementCounter.decrement(advertisementData.AD_1, function (err, newVal) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(newVal, 1);
                        AerospikeAdvertisementCounter.getCounter(advertisementData.AD_1, function (err, res) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(res, 1);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // decrement again
            function (next) {
                AerospikeAdvertisementCounter.decrement(advertisementData.AD_1, function (err, newVal) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(newVal, 0);
                        AerospikeAdvertisementCounter.getCounter(advertisementData.AD_1, function (err, res) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(res, 0);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            
        ], done);
    });
    
});