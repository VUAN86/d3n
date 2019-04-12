var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var VoucherData = require('./../config/voucher.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeVoucherCounter = KeyvalueService.Models.AerospikeVoucherCounter;

describe('VoucherCounter', function () {
    
    beforeEach(function (done) {
        var items = [];
        for(var k in VoucherData) {
            items.push(VoucherData[k]);
        }
        
        async.mapSeries(items, function (item, cbItem) {
            AerospikeVoucherCounter.remove(item, function (err) {
                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                    return cbItem(err);
                }
                return cbItem();
            });
        }, done);
    });
    
    it('test VoucherCounter generate', function (done) {
        
        async.series([
            function (next) {
                AerospikeVoucherCounter.getAmount(VoucherData.VOUCHER_1, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                AerospikeVoucherCounter.getUsedAmount(VoucherData.VOUCHER_1, function (err) {
                    try {
                        assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },

            // generate
            function (next) {
                AerospikeVoucherCounter.generate(VoucherData.VOUCHER_1, function (err) {
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
    
    it('test VoucherCounter generate codes', function (done) {
        AerospikeVoucherCounter.generateCodes({
            codes: [
                { code: 'CODE_1', expirationDate: '2017-12-11T09:00:00Z' },
                { code: 'CODE_2', expirationDate: '2017-12-12T09:00:00Z' },
                { code: 'CODE_3', expirationDate: '2017-12-13T09:00:00Z' },
                { code: 'CODE_4', expirationDate: '2017-12-14T09:00:00Z' },
            ],
            amount: 4
        }, function(err) {
            try {
                assert.ifError(err);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });

});