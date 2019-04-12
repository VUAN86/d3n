var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var VoucherData = require('./../config/voucher.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeVoucherList = KeyvalueService.Models.AerospikeVoucherList;

describe('VoucherList', function () {
    describe('VoucherList AS check', function () {
        it('HAVE #publish', function () {
            AerospikeVoucherList.should.be.have.property('publish');
            AerospikeVoucherList.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeVoucherList.should.be.have.property('unpublish');
            AerospikeVoucherList.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeVoucherList.should.be.have.property('findOne');
            AerospikeVoucherList.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeVoucherList.should.be.have.property('toJSON');
            AerospikeVoucherList.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeVoucherList.prototype.should.be.have.property('copy');
            AerospikeVoucherList.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeVoucherList.prototype.should.be.have.property('save');
            AerospikeVoucherList.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeVoucherList.prototype.should.be.have.property('remove');
            AerospikeVoucherList.prototype.remove.should.be.a.Function;
        });
    });

    describe('VoucherList AS #publish', function () {
        it('test publish list OK', function (done) {
            async.series([
                function (next) {
                    return AerospikeVoucherList.publish(VoucherData.VOUCHER_1, next);
                },
                function (next) {
                    return AerospikeVoucherList.publish(VoucherData.VOUCHER_2, next);
                },
                function (next) {
                    return AerospikeVoucherList.unpublish(VoucherData.VOUCHER_1, next);
                },
                function (next) {
                    return AerospikeVoucherList.unpublish(VoucherData.VOUCHER_2, next);
                },
            ], done);
        });
    });
});
