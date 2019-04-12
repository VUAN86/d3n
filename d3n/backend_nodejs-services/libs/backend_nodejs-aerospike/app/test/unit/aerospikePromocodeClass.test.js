var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var PromocodeData = require('./../config/promocode.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePromocodeClass = KeyvalueService.Models.AerospikePromocodeClass;

describe('PromocodeClass', function () {
    describe('PromocodeClass AS check', function () {
        it('HAVE #publish', function () {
            AerospikePromocodeClass.should.be.have.property('publish');
            AerospikePromocodeClass.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikePromocodeClass.should.be.have.property('unpublish');
            AerospikePromocodeClass.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikePromocodeClass.should.be.have.property('findOne');
            AerospikePromocodeClass.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikePromocodeClass.should.be.have.property('toJSON');
            AerospikePromocodeClass.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikePromocodeClass.prototype.should.be.have.property('copy');
            AerospikePromocodeClass.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikePromocodeClass.prototype.should.be.have.property('save');
            AerospikePromocodeClass.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikePromocodeClass.prototype.should.be.have.property('remove');
            AerospikePromocodeClass.prototype.remove.should.be.a.Function;
        });
    });

    describe('PromocodeClass AS #publish/#unpublish', function () {
        it('test publish/unpublish promocode classes OK', function (done) {
            AerospikePromocodeClass.publish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err, app) {
                AerospikePromocodeClass.unpublish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
