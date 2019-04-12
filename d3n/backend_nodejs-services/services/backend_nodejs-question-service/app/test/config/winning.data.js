var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var WinningComponent = RdbmsService.Models.WinningManager.WinningComponent;

module.exports = {
    WINNING_COMPONENT_1: {
        id: DataIds.WINNING_COMPONENT_1_ID,
        title: 'wc1',
        description: 'description',
        rules: 'rules',
        status: WinningComponent.constants().STATUS_INACTIVE,
        type: WinningComponent.constants().TYPE_CASINO_COMPONENT,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        imageId: DataIds.MEDIA_1_ID,
        prizeDescription: 'prize',
        defaultIsFree: 1,
        defaultEntryFeeAmount: 5.5,
        defaultEntryFeeCurrency: WinningComponent.constants().DEFAULT_ENTRY_FEE_CURRENCY_BONUS,
        winningConfiguration: {
            casinoComponent: 'SPINNER',
            isVoucherEnabled: false,
            voucherProviderId: 0,
            payoutStructure: {
                items: [
                    {
                        prizeId: "1",
                        prizeIndex: 0,
                        prizeName: "1st",
                        type: "MONEY",
                        amount: 30.1,
                        probability: 3,
                        imageId: DataIds.MEDIA_1_ID
                    },
                    {
                        prizeId: "2",
                        prizeIndex: 1,
                        prizeName: "2nd",
                        type: "BONUS",
                        amount: 40.1,
                        probability: 4,
                        imageId: DataIds.MEDIA_2_ID
                    }
                ]
            }
        },
        tenantId: DataIds.TENANT_1_ID
    },
    WINNING_COMPONENT_2: {
        id: DataIds.WINNING_COMPONENT_2_ID,
        title: 'wc2',
        description: 'description',
        rules: 'rules',
        status: WinningComponent.constants().STATUS_INACTIVE,
        type: WinningComponent.constants().TYPE_CASINO_COMPONENT,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        imageId: DataIds.MEDIA_2_ID,
        prizeDescription: 'prize',
        defaultIsFree: 0,
        defaultEntryFeeAmount: 6.6,
        defaultEntryFeeCurrency: WinningComponent.constants().DEFAULT_ENTRY_FEE_CURRENCY_MONEY,
        winningConfiguration: {
            casinoComponent: 'SPINNER',
            isVoucherEnabled: false,
            voucherProviderId: 0,
            payoutStructure: {
                items: [
                    {
                        prizeId: "1",
                        prizeIndex: 0,
                        prizeName: "1st",
                        type: "MONEY",
                        amount: 10.1,
                        probability: 2,
                        imageId: DataIds.MEDIA_1_ID
                    },
                    {
                        prizeId: "2",
                        prizeIndex: 1,
                        prizeName: "2nd",
                        type: "BONUS",
                        amount: 20.1,
                        probability: 3,
                        imageId: DataIds.MEDIA_2_ID
                    }
                ]
            }
        },
        tenantId: DataIds.TENANT_1_ID
    },
    WINNING_COMPONENT_TEST: {
        id: DataIds.WINNING_COMPONENT_TEST_ID,
        title: 'wcTest',
        description: 'description',
        rules: 'rules',
        status: WinningComponent.constants().STATUS_INACTIVE,
        type: WinningComponent.constants().TYPE_CASINO_COMPONENT,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        imageId: DataIds.MEDIA_2_ID,
        prizeDescription: 'prize',
        defaultIsFree: 0,
        defaultEntryFeeAmount: 6.6,
        defaultEntryFeeCurrency: WinningComponent.constants().DEFAULT_ENTRY_FEE_CURRENCY_MONEY,
        winningConfiguration: {
            casinoComponent: 'SPINNER',
            isVoucherEnabled: false,
            voucherProviderId: 0,
            payoutStructure: {
                items: [
                    {
                        prizeId: "1",
                        prizeIndex: 0,
                        prizeName: "1st",
                        type: "MONEY",
                        amount: 10.1,
                        probability: 2,
                        imageId: DataIds.MEDIA_1_ID
                    },
                    {
                        prizeId: "2",
                        prizeIndex: 1,
                        prizeName: "2nd",
                        type: "BONUS",
                        amount: 20.1,
                        probability: 3,
                        imageId: DataIds.MEDIA_2_ID
                    }
                ]
            }
        },
        tenantId: DataIds.TENANT_1_ID
    },

    loadClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(WinningComponent, [self.WINNING_COMPONENT_1, self.WINNING_COMPONENT_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
