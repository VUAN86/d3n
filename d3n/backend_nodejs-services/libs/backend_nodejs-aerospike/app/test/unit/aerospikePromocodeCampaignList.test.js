var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var PromocodeData = require('./../config/promocode.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikePromocodeCampaignList = KeyvalueService.Models.AerospikePromocodeCampaignList;

describe('PromocodeCampaignList', function () {
    describe('PromocodeCampaignList AS check', function () {
        it('HAVE #publish', function () {
            AerospikePromocodeCampaignList.should.be.have.property('publish');
            AerospikePromocodeCampaignList.publish.should.be.a.Function;
        });
        it('HAVE #unpublish', function () {
            AerospikePromocodeCampaignList.should.be.have.property('unpublish');
            AerospikePromocodeCampaignList.unpublish.should.be.a.Function;
        });
        it('HAVE #findOne', function () {
            AerospikePromocodeCampaignList.should.be.have.property('findOne');
            AerospikePromocodeCampaignList.findOne.should.be.a.Function;
        });
        it('HAVE #toJSON', function () {
            AerospikePromocodeCampaignList.should.be.have.property('toJSON');
            AerospikePromocodeCampaignList.toJSON.should.be.a.Function;
        });
        it('HAVE prototype #copy', function () {
            AerospikePromocodeCampaignList.prototype.should.be.have.property('copy');
            AerospikePromocodeCampaignList.prototype.copy.should.be.a.Function;
        });
        it('HAVE prototype #save', function () {
            AerospikePromocodeCampaignList.prototype.should.be.have.property('save');
            AerospikePromocodeCampaignList.prototype.save.should.be.a.Function;
        });
        it('HAVE prototype #remove', function () {
            AerospikePromocodeCampaignList.prototype.should.be.have.property('remove');
            AerospikePromocodeCampaignList.prototype.remove.should.be.a.Function;
        });
    });

    describe('PromocodeCampaignList AS #publish/#unpublish', function () {
        it('test publish/unpublish list of nonunique promocodes OK', function (done) {
            AerospikePromocodeCampaignList.publish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err, app) {
                AerospikePromocodeCampaignList.unpublish(PromocodeData.PROMOCODE_CAMPAIGN_1, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
        it('test publish/unpublish list of unique promocodes OK', function (done) {
            AerospikePromocodeCampaignList.publish(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err, app) {
                AerospikePromocodeCampaignList.unpublish(PromocodeData.PROMOCODE_CAMPAIGN_2, function (err, app) {
                    if (err) return done(err);
                    done();
                });
            });
        });
    });
});
