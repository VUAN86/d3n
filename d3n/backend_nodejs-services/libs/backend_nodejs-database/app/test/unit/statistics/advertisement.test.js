var should = require('should');
var Util = require('./../../util/statistics.js');
var DateUtils = require('nodejs-utils').DateUtils;
var DataIds = require('./../../config/_id.data.js');
var Game = Util.RdbmsModels.Game.Game;
var Advertisement = Util.RdbmsModels.AdvertisementManager.Advertisement;

describe('ADVERTISEMENT', function () {
    it('create bulk', function (done) {
        Advertisement.bulkCreate([
            {
                id: DataIds.ADVERTISEMENT_3_ID,
                company: 'a company',
                summary: 'ad summary',
                type: 'picture',
                category: 'travel',
                isDeployed: 0,
                status: 'active',
                usage: 'random',
                earnCredits: 1,
                creditsAmount: 123,
                videoAvailable: 1,
                videoURL: 'http://www.google.com',
                campaignURL: 'https://campaignurl',
                linkedVoucherId: DataIds.VOUCHER_1_ID,
                miniImage: 'miniImage',
                normalImage: 'normalImage',
                emailImage: 'emailImage',
                advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
                startDate: DateUtils.isoNow(),
                endDate: DateUtils.isoFuture(),
                title: 'title',
                text: 'text',
            }
        ]).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_2_ID
            }});
        }).then(function (game) {
            game.stat_adsCount.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('create single instance', function (done) {
        Advertisement.create({
            id: DataIds.ADVERTISEMENT_3_ID,
            company: 'a company',
            summary: 'ad summary',
            type: 'picture',
            category: 'travel',
            isDeployed: 0,
            status: 'active',
            usage: 'random',
            earnCredits: 1,
            creditsAmount: 123,
            videoAvailable: 1,
            videoURL: 'http://www.google.com',
            campaignURL: 'https://campaignurl',
            linkedVoucherId: DataIds.VOUCHER_1_ID,
            miniImage: 'miniImage',
            normalImage: 'normalImage',
            emailImage: 'emailImage',
            advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
            startDate: DateUtils.isoNow(),
            endDate: DateUtils.isoFuture(),
            title: 'title',
            text: 'text',
        }).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_2_ID
            }});
        }).then(function (game) {
            game.stat_adsCount.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete single instance', function (done) {
        Advertisement.create({
            id: DataIds.ADVERTISEMENT_3_ID,
            company: 'a company',
            summary: 'ad summary',
            type: 'picture',
            category: 'travel',
            isDeployed: 0,
            status: 'active',
            usage: 'random',
            earnCredits: 1,
            creditsAmount: 123,
            videoAvailable: 1,
            videoURL: 'http://www.google.com',
            campaignURL: 'https://campaignurl',
            linkedVoucherId: DataIds.VOUCHER_1_ID,
            miniImage: 'miniImage',
            normalImage: 'normalImage',
            emailImage: 'emailImage',
            advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
            startDate: DateUtils.isoNow(),
            endDate: DateUtils.isoFuture(),
            title: 'title',
            text: 'text',
        }).then(function() { 
            return Advertisement.findOne({ where: {
                id: DataIds.ADVERTISEMENT_3_ID
            }});
        }).then(function (advertisement) {
            return advertisement.destroy();
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_2_ID
            }});
        }).then(function (game) {
            game.stat_adsCount.should.equal(1);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete bulk', function (done) {
        Advertisement.bulkCreate([
            {
                id: DataIds.ADVERTISEMENT_3_ID,
                company: 'a company',
                summary: 'ad summary',
                type: 'picture',
                category: 'travel',
                isDeployed: 0,
                status: 'active',
                usage: 'random',
                earnCredits: 1,
                creditsAmount: 123,
                videoAvailable: 1,
                videoURL: 'http://www.google.com',
                campaignURL: 'https://campaignurl',
                linkedVoucherId: DataIds.VOUCHER_1_ID,
                miniImage: 'miniImage',
                normalImage: 'normalImage',
                emailImage: 'emailImage',
                advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
                startDate: DateUtils.isoNow(),
                endDate: DateUtils.isoFuture(),
                title: 'title',
                text: 'text',
            }
        ]).then(function() { 
            return Advertisement.destroy({ 
                where: { id: DataIds.ADVERTISEMENT_3_ID  },
                individualHooks: true
            });
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Game.findOne({ where: {
                id: DataIds.GAME_2_ID
            }});
        }).then(function (game) {
            game.stat_adsCount.should.equal(1);
            done();
        }).catch(function(err) {
            done(err);
        });
    });
});
