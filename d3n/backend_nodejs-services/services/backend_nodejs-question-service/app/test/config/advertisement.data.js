var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Advertisement = RdbmsService.Models.AdvertisementManager.Advertisement;
var AdvertisementProvider = RdbmsService.Models.AdvertisementManager.AdvertisementProvider;
var AdvertisementProviderHasRegionalSetting = RdbmsService.Models.AdvertisementManager.AdvertisementProviderHasRegionalSetting;

module.exports = {
    ADVERTISEMENT_1: {
        id: DataIds.ADVERTISEMENT_1_ID,
        company: 'a company',
        summary: 'ad summary',
        type: 'picture',
        category: 'travel',
        status: Advertisement.constants().STATUS_INACTIVE,
        usage: 'random',
        earnCredits: 1,
        creditsAmount: 123,
        videoAvailable: 1,
        videoURL: 'http://www.google.com',
        campaignURL: 'https://campaignurl',
        linkedVoucher: DataIds.VOUCHER_1_ID,
        miniImage: 'miniImage',
        normalImage: 'normalImage',
        emailImage: 'emailImage',
        advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        title: 'title',
        text: 'text',
    },
    ADVERTISEMENT_2: {
        id: DataIds.ADVERTISEMENT_2_ID,
        company: 'a company 2',
        summary: 'ad summary 2',
        type: 'picture',
        category: 'travel',
        status: Advertisement.constants().STATUS_INACTIVE,
        usage: 'random',
        earnCredits: 0,
        creditsAmount: null,
        videoAvailable: 1,
        videoURL: 'http://www.google.com2',
        campaignURL: 'https://campaignurl2',
        linkedVoucher: DataIds.VOUCHER_2_ID,
        miniImage: 'miniImage2',
        normalImage: 'normalImage2',
        emailImage: 'emailImage2',
        advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_2_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        title: 'title',
        text: 'text',
    },
    ADVERTISEMENT_TEST: {
        id: DataIds.ADVERTISEMENT_TEST_ID,
        company: 'a company test',
        summary: 'ad summary test',
        type: 'picture',
        category: 'travel',
        usage: 'random',
        earnCredits: 0,
        creditsAmount: null,
        videoAvailable: 1,
        videoURL: 'http://www.google.com2',
        campaignURL: 'https://campaignurl2',
        linkedVoucher: DataIds.VOUCHER_1_ID,
        miniImage: 'miniImage2',
        normalImage: 'normalImage2',
        emailImage: 'emailImage2',
        advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_2_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        title: 'title',
        text: 'text',
    },

    ADVERTISEMENT_PROVIDER_1: {
        id: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
        name: 'Provide one',
        status: Advertisement.constants().STATUS_ACTIVE,
        type: 'manual',
        feederAPI: '',
        feederAttributes: '',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        tenantId: DataIds.TENANT_1_ID
    },
    ADVERTISEMENT_PROVIDER_2: {
        id: DataIds.ADVERTISEMENT_PROVIDER_2_ID,
        name: 'Provide two',
        status: Advertisement.constants().STATUS_ACTIVE,
        type: 'feeder',
        feederAPI: 'api://test',
        feederAttributes: 'attributes',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        tenantId: DataIds.TENANT_1_ID
    },
    ADVERTISEMENT_PROVIDER_TEST: {
        id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID,
        name: 'Provide test',
        status: Advertisement.constants().STATUS_ACTIVE,
        type: 'feeder',
        feederAPI: 'api://test',
        feederAttributes: 'attributes',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture()
    },

    ADVERTISEMENT_PROVIDER_1_HAS_REGIONAL_SETTING_1: {
        advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    ADVERTISEMENT_PROVIDER_2_HAS_REGIONAL_SETTING_2: {
        advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_2_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    loadClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(AdvertisementProvider, [self.ADVERTISEMENT_PROVIDER_1, self.ADVERTISEMENT_PROVIDER_2])
            .createSeries(Advertisement, [self.ADVERTISEMENT_1, self.ADVERTISEMENT_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(AdvertisementProviderHasRegionalSetting, [self.ADVERTISEMENT_PROVIDER_1_HAS_REGIONAL_SETTING_1, self.ADVERTISEMENT_PROVIDER_2_HAS_REGIONAL_SETTING_2], 'advertisementProviderId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }

};
