var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Voucher = RdbmsService.Models.VoucherManager.Voucher;
var VoucherProvider = RdbmsService.Models.VoucherManager.VoucherProvider;
var VoucherProviderHasRegionalSetting = RdbmsService.Models.VoucherManager.VoucherProviderHasRegionalSetting;

module.exports = {
    VOUCHER_1: {
        id: DataIds.VOUCHER_1_ID,
        name: 'Voucher one',
        company: 'Company 1',
        description: 'Description 1',
        shortTitle: 'Short text',
        type: 'gift',
        category: 'Fashion',
        status: Voucher.constants().STATUS_INACTIVE,
        codeType: 'unique',
        isSpecialPrice: 1,
        isQRCode: 0,
        isExchange: 0,
        bonuspointsCosts: 100,
        redemptionURL: 'test://url',
        winningCondition: 'aa',
        miniImage: 'test_mini.png',
        normalImage: 'test.png',
        bigImage: 'test_full.png',
        expirationDate: DateUtils.isoNow(),
        voucherProviderId: DataIds.VOUCHER_PROVIDER_1_ID
    },
    VOUCHER_2: {
        id: DataIds.VOUCHER_2_ID,
        name: 'Voucher two',
        company: 'Company 2',
        description: 'Description 2',
        shortTitle: 'Short text',
        type: 'cash',
        category: 'Travel',
        status: Voucher.constants().STATUS_INACTIVE,
        codeType: 'fixed',
        isSpecialPrice: 0,
        isQRCode: 1,
        isExchange: 1,
        bonuspointsCosts: 200,
        redemptionURL: 'test2://url',
        winningCondition: 'bb',
        miniImage: 'test2_mini.png',
        normalImage: 'test2.png',
        bigImage: 'test2_full.png',
        expirationDate: DateUtils.isoNow(),
        voucherProviderId: DataIds.VOUCHER_PROVIDER_2_ID
    },
    VOUCHER_TEST: {
        id: DataIds.VOUCHER_TEST_ID,
        name: 'Voucher test',
        company: 'Company 2',
        description: 'Description 2',
        shortTitle: 'Short text',
        type: 'cash',
        category: 'Travel',
        status: Voucher.constants().STATUS_INACTIVE,
        codeType: 'fixed',
        isSpecialPrice: 0,
        isQRCode: 1,
        isExchange: 1,
        bonuspointsCosts: 200,
        redemptionURL: 'test2://url',
        winningCondition: 'bb',
        miniImage: 'test2_mini.png',
        normalImage: 'test2.png',
        bigImage: 'test2_full.png',
        expirationDate: DateUtils.isoNow(),
        voucherProviderId: DataIds.VOUCHER_PROVIDER_1_ID
    },
    VOUCHER_INACTIVE: {
        id: DataIds.VOUCHER_INACTIVE_ID,
        name: 'Voucher test',
        company: 'Company 2',
        description: 'Description 2',
        shortTitle: 'Short text',
        type: 'cash',
        category: 'Travel',
        status: Voucher.constants().STATUS_INACTIVE,
        codeType: 'fixed',
        isSpecialPrice: 0,
        isQRCode: 1,
        isExchange: 1,
        bonuspointsCosts: 200,
        redemptionURL: 'test2://url',
        winningCondition: 'bb',
        miniImage: 'test2_mini.png',
        normalImage: 'test2.png',
        bigImage: 'test2_full.png',
        expirationDate: DateUtils.isoNow(),
        voucherProviderId: DataIds.VOUCHER_PROVIDER_1_ID
    },

    VOUCHER_PROVIDER_1: {
        id: DataIds.VOUCHER_PROVIDER_1_ID,
        name: 'Provide one',
        status: VoucherProvider.constants().STATUS_ACTIVE,
        type: 'manual',
        feederAPI: '',
        feederAttributes: '',
        startDate:  DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        tenantId: DataIds.TENANT_1_ID
    },
    VOUCHER_PROVIDER_2: {
        id: DataIds.VOUCHER_PROVIDER_2_ID,
        name: 'Provide two',
        status: VoucherProvider.constants().STATUS_ACTIVE,
        type: 'feeder',
        feederAPI: 'api://test',
        feederAttributes: 'attributes',
        startDate:  DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        tenantId: DataIds.TENANT_1_ID
    },
    VOUCHER_PROVIDER_TEST: {
        id: DataIds.VOUCHER_PROVIDER_TEST_ID,
        name: 'Provide test',
        status: VoucherProvider.constants().STATUS_ACTIVE,
        type: 'feeder',
        feederAPI: 'api://test',
        feederAttributes: 'attributes',
        startDate:  DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        tenantId: DataIds.TENANT_1_ID
    },
    VOUCHER_PROVIDER_INACTIVE: {
        id: DataIds.VOUCHER_PROVIDER_INACTIVE_ID,
        name: 'Provide inactive',
        status: VoucherProvider.constants().STATUS_INACTIVE,
        type: 'feeder',
        feederAPI: 'api://inactive',
        feederAttributes: 'attributes',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        tenantId: DataIds.TENANT_1_ID
    },

    VOUCHER_PROVIDER_1_HAS_REGIONAL_SETTING_1: {
        voucherProviderId: DataIds.VOUCHER_PROVIDER_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    VOUCHER_PROVIDER_2_HAS_REGIONAL_SETTING_2: {
        voucherProviderId: DataIds.VOUCHER_PROVIDER_2_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(Voucher, [self.VOUCHER_1, self.VOUCHER_2, self.VOUCHER_INACTIVE, self.VOUCHER_TEST])
            .removeSeries(VoucherProvider, [self.VOUCHER_PROVIDER_1, self.VOUCHER_PROVIDER_2, self.VOUCHER_PROVIDER_INACTIVE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(VoucherProvider, [self.VOUCHER_PROVIDER_1, self.VOUCHER_PROVIDER_2, self.VOUCHER_PROVIDER_INACTIVE])
            .createSeries(Voucher, [self.VOUCHER_1, self.VOUCHER_2, self.VOUCHER_INACTIVE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(VoucherProviderHasRegionalSetting, [self.VOUCHER_PROVIDER_1_HAS_REGIONAL_SETTING_1, self.VOUCHER_PROVIDER_2_HAS_REGIONAL_SETTING_2], 'voucherProviderId')
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
            .createSeries(VoucherProviderHasRegionalSetting, [self.VOUCHER_PROVIDER_1_HAS_REGIONAL_SETTING_1, self.VOUCHER_PROVIDER_2_HAS_REGIONAL_SETTING_2], 'voucherProviderId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },
};
