var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var WinningComponent = RdbmsService.Models.WinningManager.WinningComponent;

module.exports = {
    WINNING_COMPONENT_1: {
        $id: DataIds.WINNING_COMPONENT_1_ID,
        title: 'wc1',
        description: 'description',
        rules: 'rules',
        status: WinningComponent.constants().STATUS_INACTIVE,
        type: WinningComponent.constants().TYPE_CASINO_COMPONENT,
        startDate: _.now(),
        endDate: _.now(),
        $imageId: DataIds.MEDIA_1_ID,
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
        $tenantId: DataIds.TENANT_1_ID
    },
    WINNING_COMPONENT_2: {
        $id: DataIds.WINNING_COMPONENT_2_ID,
        title: 'wc2',
        description: 'description',
        rules: 'rules',
        status: WinningComponent.constants().STATUS_INACTIVE,
        type: WinningComponent.constants().TYPE_CASINO_COMPONENT,
        startDate: _.now(),
        endDate: _.now(),
        $imageId: DataIds.MEDIA_2_ID,
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
        $tenantId: DataIds.TENANT_1_ID
    },

    loadClassifiers: function (testSet) {
        return testSet
            .createSeries(WinningComponent, [this.WINNING_COMPONENT_1, this.WINNING_COMPONENT_2])
    }
};
