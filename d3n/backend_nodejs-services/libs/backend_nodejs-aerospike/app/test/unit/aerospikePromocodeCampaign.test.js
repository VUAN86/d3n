var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var PromocodeData = require('./../config/promocode.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePromocodeCampaign = KeyvalueService.Models.AerospikePromocodeCampaign;

describe('PromocodeCampaign', function () {
    describe('PromocodeCampaign AS check', function () {
        it('HAVE #publish', function () {
            AerospikePromocodeCampaign.should.be.have.property('publish');
            AerospikePromocodeCampaign.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikePromocodeCampaign.should.be.have.property('unpublish');
            AerospikePromocodeCampaign.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikePromocodeCampaign.should.be.have.property('findOne');
            AerospikePromocodeCampaign.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikePromocodeCampaign.should.be.have.property('toJSON');
            AerospikePromocodeCampaign.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikePromocodeCampaign.prototype.should.be.have.property('copy');
            AerospikePromocodeCampaign.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikePromocodeCampaign.prototype.should.be.have.property('save');
            AerospikePromocodeCampaign.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikePromocodeCampaign.prototype.should.be.have.property('remove');
            AerospikePromocodeCampaign.prototype.remove.should.be.a.Function;
        });
    });

    describe('PromocodeCampaign AS #publish/#unpublish nonunique promocodes', function () {
        it('test publish/unpublish nonunique promocodes OK', function (done) {
            AerospikePromocodeCampaign.publish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err, app) {
                AerospikePromocodeCampaign.unpublish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
        it('test publish/unpublish unique promocodes OK', function (done) {
            AerospikePromocodeCampaign.publish(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err, app) {
                AerospikePromocodeCampaign.unpublish(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
