var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Advertisement = RdbmsService.Models.AdvertisementManager.Advertisement;
var AdvertisementProvider = RdbmsService.Models.AdvertisementManager.AdvertisementProvider;
var AdvertisementProviderHasRegionalSetting = RdbmsService.Models.AdvertisementManager.AdvertisementProviderHasRegionalSetting;

module.exports = {
    ADVERTISEMENT_1: {
        $id: DataIds.ADVERTISEMENT_1_ID,
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
        $linkedVoucherId: DataIds.VOUCHER_1_ID,
        miniImage: 'miniImage',
        normalImage: 'normalImage',
        emailImage: 'emailImage',
        $advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        title: 'title',
        text: 'text',
    },
    ADVERTISEMENT_2: {
        $id: DataIds.ADVERTISEMENT_2_ID,
        company: 'a company 2',
        summary: 'ad summary 2',
        type: 'picture',
        category: 'travel',
        isDeployed: 0,
        status: 'active',
        usage: 'random',
        earnCredits: 0,
        creditsAmount: null,
        videoAvailable: 1,
        videoURL: 'http://www.google.com2',
        campaignURL: 'https://campaignurl2',
        miniImage: 'miniImage2',
        normalImage: 'normalImage2',
        emailImage: 'emailImage2',
        $advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_2_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        title: 'title',
        text: 'text',
    },

    ADVERTISEMENT_PROVIDER_1: {
        $id: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
        name: 'Provide one',
        status: 'inactive',
        type: 'manual',
        feederAPI: '',
        feederAttributes: '',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        $tenantId: DataIds.TENANT_1_ID
    },
    ADVERTISEMENT_PROVIDER_2: {
        $id: DataIds.ADVERTISEMENT_PROVIDER_2_ID,
        name: 'Provide two',
        status: 'active',
        type: 'feeder',
        feederAPI: 'api://test',
        feederAttributes: 'attributes',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        $tenantId: DataIds.TENANT_1_ID
    },

    ADVERTISEMENT_PROVIDER_1_HAS_REGIONAL_SETTING_1: {
        $advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_1_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    ADVERTISEMENT_PROVIDER_2_HAS_REGIONAL_SETTING_2: {
        $advertisementProviderId: DataIds.ADVERTISEMENT_PROVIDER_2_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    loadClassifiers: function (testSet) {
        return testSet
            .createSeries(AdvertisementProvider, [this.ADVERTISEMENT_PROVIDER_1, this.ADVERTISEMENT_PROVIDER_2])
            .createSeries(Advertisement, [this.ADVERTISEMENT_1, this.ADVERTISEMENT_2])
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(AdvertisementProviderHasRegionalSetting, [this.ADVERTISEMENT_PROVIDER_1_HAS_REGIONAL_SETTING_1, this.ADVERTISEMENT_PROVIDER_2_HAS_REGIONAL_SETTING_2])
    }

};
