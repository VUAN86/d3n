var _ = require('lodash');
var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('./../../../index.js').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Tombola = RdbmsService.Models.TombolaManager.Tombola;
var TombolaHasRegionalSetting = RdbmsService.Models.TombolaManager.TombolaHasRegionalSetting;

module.exports = {
    TOMBOLA_1: {
        $id: DataIds.TOMBOLA_1_ID,
        name: 'Tombola 1',
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'active',
        playoutTarget: 'PERCENT_OF_TICKETS_USED',
        targetDate: null,
        percentOfTicketsAmount: 50,
        totalTicketsAmount: 10,
        currency: 'CREDIT',
        bundles: [
            {
                amount: 10,
                price: 100,
                imageId: '12345.jpg'
            }
        ],
        prizes: [
            {
                id: '1',
                name: 'Prize',
                type: 'CREDIT',
                amount: 10,
                imageId: '12345.jpg',
                voucherId: '12345',
                draws: 2
            }
        ],
        consolationPrize:
        {
            id: '1',
            name: 'Prize',
            type: 'CREDIT',
            amount: 10,
            imageId: '12345.jpg',
            voucherId: '12345',
            draws: 2
        },
        $tenantId: 1
    },
    TOMBOLA_2: {
        $id: DataIds.TOMBOLA_2_ID,
        name: 'Tombola 2',
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'inactive',
        playoutTarget: 'PERCENT_OF_TICKETS_USED',
        targetDate: null,
        percentOfTicketsAmount: 30,
        totalTicketsAmount: 20,
        currency: 'CREDIT',
        bundles: [
            {
                amount: 20,
                price: 100,
                imageId: '12345.jpg'
            }
        ],
        prizes: [
            {
                id: '2',
                name: 'Prize',
                type: 'CREDIT',
                amount: 10,
                imageId: '12345.jpg',
                voucherId: '12345',
                draws: 2
            }
        ],
        consolationPrize:
        {
            id: '2',
            name: 'Prize',
            type: 'CREDIT',
            amount: 10,
            imageId: '12345.jpg',
            voucherId: '12345',
            draws: 2
        },
        $tenantId: 1
    },

    TOMBOLA_1_HAS_REGIONAL_SETTING_1: {
        $tombolaId: DataIds.TOMBOLA_1_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    TOMBOLA_2_HAS_REGIONAL_SETTING_2: {
        $tombolaId: DataIds.TOMBOLA_2_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(TombolaHasRegionalSetting, [this.TOMBOLA_1_HAS_REGIONAL_SETTING_1, this.TOMBOLA_2_HAS_REGIONAL_SETTING_2])
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(Tombola, [this.TOMBOLA_1, this.TOMBOLA_2])
    }
};
