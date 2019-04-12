var _ = require('lodash');
var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Tombola = RdbmsService.Models.TombolaManager.Tombola;
var TombolaHasRegionalSetting = RdbmsService.Models.TombolaManager.TombolaHasRegionalSetting;

module.exports = {
    TOMBOLA_1: {
        id: DataIds.TOMBOLA_1_ID,
        name: 'Tombola 1',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: Tombola.constants().STATUS_INACTIVE,
        playoutTarget: 'PERCENT_OF_TICKETS_USED',
        targetDate: null,
        percentOfTicketsAmount: 50,
        totalTicketsAmount: 10,
        currency: 'CREDIT',
        applicationsIds: [DataIds.APPLICATION_1_ID],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        bundles: [
            {
                amount: 10,
                price: 100,
                imageId: '12345.jpg'
            }
        ],
        prizes: [
            {
                name: 'Prize',
                type: 'CREDIT',
                amount: 10,
                imageId: '12345.jpg',
                voucherId: DataIds.VOUCHER_1_ID.toString(),
                draws: 2
            }
        ],
        consolationPrize:
        {
            name: 'Prize',
            type: 'CREDIT',
            amount: 10,
            imageId: '12345.jpg',
            voucherId: DataIds.VOUCHER_1_ID.toString(),
            draws: 2
        },
        tenantId: 1
    },
    TOMBOLA_2: {
        id: DataIds.TOMBOLA_2_ID,
        name: 'Tombola 2',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: Tombola.constants().STATUS_INACTIVE,
        playoutTarget: 'PERCENT_OF_TICKETS_USED',
        targetDate: null,
        percentOfTicketsAmount: 30,
        totalTicketsAmount: 20,
        currency: 'CREDIT',
        applicationsIds: [DataIds.APPLICATION_2_ID],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        bundles: [
            {
                amount: 20,
                price: 100,
                imageId: '12345.jpg'
            }
        ],
        prizes: [
            {
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
            name: 'Prize',
            type: 'CREDIT',
            amount: 10,
            imageId: '12345.jpg',
            voucherId: '12345',
            draws: 2
        },
        tenantId: 1
    },
    TOMBOLA_TEST: {
        id: DataIds.TOMBOLA_TEST_ID,
        name: 'Tombola test',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: Tombola.constants().STATUS_INACTIVE,
        preCloseOffsetMinutes: 20,
        playoutTarget: 'PERCENT_OF_TICKETS_USED',
        targetDate: null,
        percentOfTicketsAmount: 50,
        totalTicketsAmount: 10,
        currency: 'CREDIT',
        applicationsIds: [DataIds.APPLICATION_2_ID],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        bundles: [
            {
                amount: 10,
                price: 100,
                imageId: '12345.jpg'
            }
        ],
        prizes: [
            {
                type: 'CREDIT',
                amount: 10,
                imageId: '12345.jpg',
                voucherId: '12345',
                draws: 2
            }
        ],
        consolationPrize:
        {
            name: 'Prize',
            type: 'CREDIT',
            amount: 10,
            imageId: '12345.jpg',
            voucherId: '12345',
            draws: 2
        },
        tenantId: 1
    },
    TOMBOLA_INACTIVE: {
        id: DataIds.TOMBOLA_INACTIVE_ID,
        name: 'Tombola inactive',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        description: 'Info',
        winningRules: 'Winning rule',
        imageId: '12345.jpg',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: Tombola.constants().STATUS_INACTIVE,
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
                type: 'CREDIT',
                amount: 10,
                imageId: '12345.jpg',
                voucherId: '12345',
                draws: 2
            }
        ],
        consolationPrize:
        {
            name: 'Prize',
            type: 'CREDIT',
            amount: 10,
            imageId: '12345.jpg',
            voucherId: '12345',
            draws: 2
        },
        tenantId: 1
    },

    TOMBOLA_1_HAS_REGIONAL_SETTING_1: {
      tombolaId: DataIds.TOMBOLA_1_ID,
      regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    TOMBOLA_2_HAS_REGIONAL_SETTING_2: {
      tombolaId: DataIds.TOMBOLA_2_ID,
      regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(TombolaHasRegionalSetting, [self.TOMBOLA_1_HAS_REGIONAL_SETTING_1, self.TOMBOLA_2_HAS_REGIONAL_SETTING_2], 'tombolaId')
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
            .createSeries(TombolaHasRegionalSetting, [self.TOMBOLA_1_HAS_REGIONAL_SETTING_1, self.TOMBOLA_2_HAS_REGIONAL_SETTING_2], 'tombolaId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    cleanEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(Tombola, [self.TOMBOLA_1, self.TOMBOLA_2, self.TOMBOLA_INACTIVE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(Tombola, [self.TOMBOLA_1, self.TOMBOLA_2, self.TOMBOLA_INACTIVE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
