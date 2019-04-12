var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Voucher = RdbmsService.Models.VoucherManager.Voucher;
var VoucherProvider = RdbmsService.Models.VoucherManager.VoucherProvider;
var VoucherProviderHasRegionalSetting = RdbmsService.Models.VoucherManager.VoucherProviderHasRegionalSetting;

module.exports = {
    VOUCHER_1: {
        $id: DataIds.VOUCHER_1_ID,
        name: 'Voucher one',
        company: 'Company 1',
        description: 'Description 1',
        shortTitle: 'text',
        type: 'gift',
        category: 'Fashion',
        isDeployed: 0,
        status: 'active',
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
        $voucherProviderId: DataIds.VOUCHER_PROVIDER_1_ID
    },
    VOUCHER_2: {
        $id: DataIds.VOUCHER_2_ID,
        name: 'Voucher two',
        company: 'Company 2',
        description: 'Description 2',
        shortTitle: 'text',
        type: 'cash',
        category: 'Travel',
        isDeployed: 0,
        status: 'inactive',
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
        $voucherProviderId: DataIds.VOUCHER_PROVIDER_2_ID
    },

    VOUCHER_PROVIDER_1: {
        $id: DataIds.VOUCHER_PROVIDER_1_ID,
        name: 'Provide one',
        status: 'inactive',
        type: 'manual',
        feederAPI: '',
        feederAttributes: '',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        $tenantId: DataIds.TENANT_1_ID
    },
    VOUCHER_PROVIDER_2: {
        $id: DataIds.VOUCHER_PROVIDER_2_ID,
        name: 'Provide two',
        status: 'active',
        type: 'feeder',
        feederAPI: 'api://test',
        feederAttributes: 'attributes',
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        $tenantId: DataIds.TENANT_2_ID
    },

    VOUCHER_PROVIDER_1_HAS_REGIONAL_SETTING_1: {
        $voucherProviderId: DataIds.VOUCHER_PROVIDER_1_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    VOUCHER_PROVIDER_2_HAS_REGIONAL_SETTING_2: {
        $voucherProviderId: DataIds.VOUCHER_PROVIDER_2_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    loadClassifiers: function (testSet) {
        return testSet
            .createSeries(VoucherProvider, [this.VOUCHER_PROVIDER_1, this.VOUCHER_PROVIDER_2])
            .createSeries(Voucher, [this.VOUCHER_1, this.VOUCHER_2])
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(VoucherProviderHasRegionalSetting, [this.VOUCHER_PROVIDER_1_HAS_REGIONAL_SETTING_1, this.VOUCHER_PROVIDER_2_HAS_REGIONAL_SETTING_2])
    }

};
