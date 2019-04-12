var _ = require('lodash');
var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Promocode = RdbmsService.Models.PromocodeManager.Promocode;
var PromocodeCampaign = RdbmsService.Models.PromocodeManager.PromocodeCampaign;

module.exports = {
    PROMOCODE_CAMPAIGN_1: {
        id: DataIds.PROMOCODE_CAMPAIGN_1_ID,
        campaignName: 'Promocode Campaign 1',
        description: 'Promocode Campaign 1 Description',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        money: 0,
        credits: 10,
        bonuspoints: 1000,
        tenantId: DataIds.TENANT_1_ID,
        applicationsIds: [DataIds.APPLICATION_1_ID]
    },
    PROMOCODE_CAMPAIGN_2: {
        id: DataIds.PROMOCODE_CAMPAIGN_2_ID,
        campaignName: 'Promocode Campaign 2',
        description: 'Promocode Campaign 2 Description',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        money: 10,
        credits: 0,
        bonuspoints: 0,
        tenantId: DataIds.TENANT_1_ID,
        applicationsIds: [DataIds.APPLICATION_2_ID]
    },
    PROMOCODE_CAMPAIGN_TEST: {
        id: DataIds.PROMOCODE_CAMPAIGN_TEST_ID,
        campaignName: 'Promocode Campaign Test',
        description: 'Promocode Campaign Test Description',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        money: 10,
        credits: 0,
        bonuspoints: 0,
        tenantId: DataIds.TENANT_1_ID,
        applicationsIds: [DataIds.APPLICATION_2_ID]
    },
    PROMOCODE_CAMPAIGN_INACTIVE: {
        id: DataIds.PROMOCODE_CAMPAIGN_INACTIVE_ID,
        campaignName: 'Promocode Campaign Inactive',
        description: 'Promocode Campaign Inactive Description',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        money: 10,
        credits: 0,
        bonuspoints: 0,
        tenantId: DataIds.TENANT_1_ID,
        applicationsIds: [DataIds.APPLICATION_2_ID]
    },

    PROMOCODE_UNIQUE: {
        id: DataIds.PROMOCODE_UNIQUE_ID,
        promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID,
        code: 'Promocode_Unique',
        number: 3,
        isUnique: 1,
        isQRCodeRequired: 0,
        usage: PromocodeCampaign.constants().USAGE_ONCE_PER_PLAYER,
        numberOfUses: null,
        createDate: DateUtils.isoNow(),
    },
    PROMOCODE_NONUNIQUE: {
        id: DataIds.PROMOCODE_NONUNIQUE_ID,
        promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID,
        code: 'Promocode_NonUnique',
        number: 3,
        isUnique: 0,
        isQRCodeRequired: 0,
        usage: PromocodeCampaign.constants().USAGE_MULTI_PER_PLAYER,
        numberOfUses: 3,
        createDate: DateUtils.isoNow(),
    },
    PROMOCODE_TEST: {
        id: DataIds.PROMOCODE_TEST_ID,
        promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_2_ID,
        code: null,
        number: 3,
        isUnique: 1,
        isQRCodeRequired: 0,
        usage: PromocodeCampaign.constants().USAGE_ONCE_PER_PLAYER,
        numberOfUses: null,
        createDate: DateUtils.isoNow(),
    },

    loadEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(PromocodeCampaign, [self.PROMOCODE_CAMPAIGN_1, self.PROMOCODE_CAMPAIGN_2, self.PROMOCODE_CAMPAIGN_INACTIVE])
            .createSeries(Promocode, [self.PROMOCODE_UNIQUE, self.PROMOCODE_NONUNIQUE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
