var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var TombolaData = require('./../config/tombola.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeTombolaCounter = KeyvalueService.Models.AerospikeTombolaCounter;

describe('TombolaCounter', function () {
    
    it('test TombolaCounter increment', function (done) {
        var purchasedAmount;
        var lockedAmount;
        async.series([
            function (next) {
                AerospikeTombolaCounter.incrementPurchased(TombolaData.TOMBOLA_1, function (err, amount) {
                    try {
                        purchasedAmount = amount;
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikeTombolaCounter.incrementPurchased(TombolaData.TOMBOLA_1, function (err, amount) {
                    try {
                        assert.strictEqual(amount, purchasedAmount + 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikeTombolaCounter.incrementLocked(TombolaData.TOMBOLA_1, function (err, amount) {
                    try {
                        lockedAmount = amount;
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                AerospikeTombolaCounter.incrementLocked(TombolaData.TOMBOLA_1, function (err, amount) {
                    try {
                        assert.strictEqual(amount, lockedAmount + 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
        ], done);
    });
    
});