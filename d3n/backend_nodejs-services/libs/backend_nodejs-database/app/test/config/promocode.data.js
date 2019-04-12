var _ = require('lodash');
var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('./../../../index.js').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Promocode = RdbmsService.Models.PromocodeManager.Promocode;
var PromocodeCampaign = RdbmsService.Models.PromocodeManager.PromocodeCampaign;

module.exports = {
    PROMOCODE_CAMPAIGN_1: {
        $id: DataIds.PROMOCODE_CAMPAIGN_1_ID,
        campaignName: 'Promocode Campaign 1',
        description: 'Promocode Campaign 1 Description',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        money: 0,
        credits: 10,
        bonuspoints: 1000,
        $tenantId: 1
    },
    PROMOCODE_CAMPAIGN_2: {
        $id: DataIds.PROMOCODE_CAMPAIGN_2_ID,
        campaignName: 'Promocode Campaign 2',
        description: 'Promocode Campaign 2 Description',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        money: 10,
        credits: 0,
        bonuspoints: 0,
        $tenantId: 1
    },

    PROMOCODE_1: {
        $id: DataIds.PROMOCODE_1_ID,
        $promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID,
        number: 1,
        isUnique: 1,
        code: 'Promocode_1',
        isQRCodeRequired: 0,
        usage: PromocodeCampaign.constants().USAGE_ONCE_PER_PLAYER,
        numberOfUses: null,
        createDate: DateUtils.isoNow(),
    },
    PROMOCODE_2: {
        $id: DataIds.PROMOCODE_2_ID,
        $promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_2_ID,
        number: 3,
        isUnique: 0,
        code: 'Promocode_2',
        isQRCodeRequired: 0,
        usage: PromocodeCampaign.constants().USAGE_MULTI_PER_PLAYER,
        numberOfUses: 3,
        createDate: DateUtils.isoNow(),
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(PromocodeCampaign, [this.PROMOCODE_CAMPAIGN_1, this.PROMOCODE_CAMPAIGN_2])
            .createSeries(Promocode, [this.PROMOCODE_1, this.PROMOCODE_2])
    }
};
