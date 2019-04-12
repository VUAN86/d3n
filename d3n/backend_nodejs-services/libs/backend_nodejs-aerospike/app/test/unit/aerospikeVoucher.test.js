var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var VoucherData = require('./../config/voucher.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeVoucher = KeyvalueService.Models.AerospikeVoucher;

describe('Voucher', function () {
    describe('Voucher AS check', function () {
        it('HAVE #publish', function () {
            AerospikeVoucher.should.be.have.property('publish');
            AerospikeVoucher.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikeVoucher.should.be.have.property('unpublish');
            AerospikeVoucher.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikeVoucher.should.be.have.property('findOne');
            AerospikeVoucher.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikeVoucher.should.be.have.property('toJSON');
            AerospikeVoucher.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikeVoucher.prototype.should.be.have.property('copy');
            AerospikeVoucher.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikeVoucher.prototype.should.be.have.property('save');
            AerospikeVoucher.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikeVoucher.prototype.should.be.have.property('remove');
            AerospikeVoucher.prototype.remove.should.be.a.Function;
        });
    });

    describe('Voucher AS #publish', function () {
        it('test publish OK', function (done) {
            AerospikeVoucher.publish(VoucherData.VOUCHER_1, function (err, app) {
                AerospikeVoucher.unpublish(VoucherData.VOUCHER_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
