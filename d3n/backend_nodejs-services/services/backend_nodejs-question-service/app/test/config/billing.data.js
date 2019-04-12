var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var PaymentStructure = Database.RdbmsService.Models.Billing.PaymentStructure;
var PaymentStructureTier = Database.RdbmsService.Models.Billing.PaymentStructureTier;
var PaymentAction = Database.RdbmsService.Models.Billing.PaymentAction;
var PaymentTenantBillOfMaterial = Database.RdbmsService.Models.Billing.PaymentTenantBillOfMaterial;
var PaymentResourceBillOfMaterial = Database.RdbmsService.Models.Billing.PaymentResourceBillOfMaterial;
var PaymentType = RdbmsService.Models.Billing.PaymentType;

module.exports = {
    PAYMENT_STRUCTURE_1: {
        id: DataIds.PAYMENT_STRUCTURE_1_ID,
        name: 'Workorder Billing Model 1',
        isActive: 1,
        type: PaymentStructure.constants().TYPE_INSTANT,
        autoPayment: 1,
        autoPaymentLimit: 50,
        usagePaymentAmountMoneyEur: 0,
        usagePaymentAmountBonus: 0,
        usagePaymentAmountCredits: 0,
        tenantId: DataIds.TENANT_1_ID,
        unit: 1,
        payUnit: 10,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },
    PAYMENT_STRUCTURE_2: {
        id: DataIds.PAYMENT_STRUCTURE_2_ID,
        name: 'Workorder Billing Model 2',
        isActive: 1,
        type: PaymentStructure.constants().TYPE_INSTANT,
        autoPayment: 0,
        autoPaymentLimit: 0,
        usagePaymentAmountMoneyEur: 0,
        usagePaymentAmountBonus: 0,
        usagePaymentAmountCredits: 0,
        tenantId: DataIds.TENANT_1_ID,
        unit: 2,
        payUnit: 20,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },
    PAYMENT_STRUCTURE_NO_DEPENDENCIES: {
        id: DataIds.PAYMENT_STRUCTURE_NO_DEPENDENCIES_ID,
        name: 'Workorder Billing Model No Dependencies',
        isActive: 1,
        type: PaymentStructure.constants().TYPE_INSTANT,
        autoPayment: 0,
        autoPaymentLimit: 0,
        usagePaymentAmountMoneyEur: 0,
        usagePaymentAmountBonus: 0,
        usagePaymentAmountCredits: 0,
        tenantId: DataIds.TENANT_1_ID,
        unit: 3,
        payUnit: 30,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },
    PAYMENT_STRUCTURE_TEST: {
        id: DataIds.PAYMENT_STRUCTURE_TEST_ID,
        name: 'Workorder Billing Model Test',
        isActive: 1,
        type: PaymentStructure.constants().TYPE_INSTANT,
        autoPayment: 0,
        autoPaymentLimit: 0,
        usagePaymentAmountMoneyEur: 0,
        usagePaymentAmountBonus: 0,
        usagePaymentAmountCredits: 0,
        unit: 4,
        payUnit: 40,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },

    PAYMENT_STRUCTURE_TIER_1_1: {
        id: DataIds.PAYMENT_STRUCTURE_TIER_1_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        quantityMin: 0,
        amountMoneyEur: 1,
        amountBonus: 100,
        amountCredits: 10
    },
    PAYMENT_STRUCTURE_TIER_1_2: {
        id: DataIds.PAYMENT_STRUCTURE_TIER_1_2_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        quantityMin: 10,
        amountMoneyEur: 1.1,
        amountBonus: 110,
        amountCredits: 11
    },
    PAYMENT_STRUCTURE_TIER_2_1: {
        id: DataIds.PAYMENT_STRUCTURE_TIER_2_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        quantityMin: 0,
        amountMoneyEur: 0.5,
        amountBonus: 100,
        amountCredits: 0
    },
    PAYMENT_STRUCTURE_TIER_2_2: {
        id: DataIds.PAYMENT_STRUCTURE_TIER_2_2_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        quantityMin: 10,
        amountMoneyEur: 10,
        amountBonus: 1000,
        amountCredits: 0
    },
    PAYMENT_STRUCTURE_TIER_TEST: {
        id: DataIds.PAYMENT_STRUCTURE_TIER_TEST_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        quantityMin: 0,
        amountMoneyEur: 5,
        amountBonus: 500,
        amountCredits: 0
    },

    PAYMENT_ACTION_1: {
        id: DataIds.PAYMENT_ACTION_1_ID,
        createDate: DateUtils.isoNow(),
        resourceId: DataIds.LOCAL_USER_ID,
        workorderId: DataIds.WORKORDER_1_ID,
        questionId: DataIds.QUESTION_1_ID,
        questionTranslationId: DataIds.QUESTION_1_TRANSLATION_EN_ID,
        status: PaymentAction.constants().STATUS_POTENTIAL,
        rejectionReason: null,
        isBillable: 1,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        type: PaymentAction.constants().TYPE_CREATION,
        paymentTenantBillOfMaterialId: null,
        paymentResourceBillOfMaterialId: null
    },
    PAYMENT_ACTION_2: {
        id: DataIds.PAYMENT_ACTION_2_ID,
        createDate: DateUtils.isoNow(),
        resourceId: DataIds.LOCAL_USER_ID,
        workorderId: DataIds.WORKORDER_1_ID,
        questionId: DataIds.QUESTION_2_ID,
        questionTranslationId: DataIds.QUESTION_2_TRANSLATION_EN_ID,
        status: PaymentAction.constants().STATUS_POTENTIAL,
        rejectionReason: null,
        isBillable: 1,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        type: PaymentAction.constants().TYPE_CREATION,
        paymentTenantBillOfMaterialId: null,
        paymentResourceBillOfMaterialId: null
    },

    PAYMENT_TENANT_BILL_OF_MATERIAL_1: {
        id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        amountMoneyEur: 100,
        amountBonus: 20,
        amountCredits: 30,
        status: PaymentTenantBillOfMaterial.constants().STATUS_UNAPPROVED,
        approvalDate: null,
        paymentDate: null,
        adjustmentReason: 'test 1',
        transactionId: null,
        description: 'Description 1',
        totalTransactions: 100,
    },
    PAYMENT_TENANT_BILL_OF_MATERIAL_2: {
        id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_2_ID,
        tenantId: DataIds.TENANT_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        amountMoneyEur: 200,
        amountBonus: 40,
        amountCredits: 60,
        status: PaymentTenantBillOfMaterial.constants().STATUS_APPROVED,
        approvalDate: DateUtils.isoNow(),
        paymentDate: null,
        adjustmentReason: 'test 2',
        transactionId: null,
        description: 'Description 2',
        totalTransactions: 200,
    },
    PAYMENT_TENANT_BILL_OF_MATERIAL_TEST: {
        id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST_ID,
        tenantId: DataIds.TENANT_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        amountMoneyEur: 200,
        amountBonus: 40,
        amountCredits: 60,
        status: PaymentTenantBillOfMaterial.constants().STATUS_UNAPPROVED,
        approvalDate: DateUtils.isoNow(),
        paymentDate: null,
        adjustmentReason: 'test test',
        transactionId: null,
        description: 'Description test',
        totalTransactions: 300,
    },

    PAYMENT_RESOURCE_BILL_OF_MATERIAL_1: {
        id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID,
        resourceId: DataIds.LOCAL_USER_ID,
        workorderId: DataIds.WORKORDER_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        amountMoneyEur: 150,
        amountBonus: 25,
        amountCredits: 35,
        status: PaymentResourceBillOfMaterial.constants().STATUS_UNAPPROVED,
        approvalDate: null,
        paymentDate: null,
        adjustmentReason: 'test 1',
        transactionId: null,
        description: 'Description 1',
        totalTransactions: 100,
    },
    PAYMENT_RESOURCE_BILL_OF_MATERIAL_2: {
        id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_2_ID,
        resourceId: DataIds.LOCAL_USER_ID,
        workorderId: DataIds.WORKORDER_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        amountMoneyEur: 250,
        amountBonus: 35,
        amountCredits: 45,
        status: PaymentResourceBillOfMaterial.constants().STATUS_APPROVED,
        approvalDate: DateUtils.isoNow(),
        paymentDate: null,
        adjustmentReason: 'test 2',
        transactionId: null,
        description: 'Description 2',
        totalTransactions: 200,
    },
    PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST: {
        id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST_ID,
        resourceId: DataIds.LOCAL_USER_ID,
        workorderId: DataIds.WORKORDER_1_ID,
        paymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        amountMoneyEur: 250,
        amountBonus: 35,
        amountCredits: 45,
        status: PaymentResourceBillOfMaterial.constants().STATUS_UNAPPROVED,
        approvalDate: DateUtils.isoNow(),
        paymentDate: null,
        adjustmentReason: 'test test',
        transactionId: null,
        description: 'Description test',
        totalTransactions: 300,
    },

    PAYMENT_TYPE_1: {
        id: DataIds.PAYMENT_TYPE_1_ID,
        name: 'Payment type one'
    },
    PAYMENT_TYPE_2: {
        id: DataIds.PAYMENT_TYPE_2_ID,
        name: 'Payment type two'
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(PaymentAction, [self.PAYMENT_ACTION_1, self.PAYMENT_ACTION_2])
            .removeSeries(PaymentResourceBillOfMaterial, [self.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1, self.PAYMENT_RESOURCE_BILL_OF_MATERIAL_2])
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
            .removeSeries(PaymentTenantBillOfMaterial, [self.PAYMENT_TENANT_BILL_OF_MATERIAL_1, self.PAYMENT_TENANT_BILL_OF_MATERIAL_2])
            .removeSeries(PaymentStructureTier, [self.PAYMENT_STRUCTURE_TIER_1_1, self.PAYMENT_STRUCTURE_TIER_1_2, self.PAYMENT_STRUCTURE_TIER_2_1, self.PAYMENT_STRUCTURE_TIER_2_2, self.PAYMENT_STRUCTURE_TIER_TEST])
            .removeSeries(PaymentStructure, [self.PAYMENT_STRUCTURE_1, self.PAYMENT_STRUCTURE_2, self.PAYMENT_STRUCTURE_NO_DEPENDENCIES, self.PAYMENT_STRUCTURE_TEST])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(PaymentType, [self.PAYMENT_TYPE_1, self.PAYMENT_TYPE_2])
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
            .createSeries(PaymentResourceBillOfMaterial, [self.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1, self.PAYMENT_RESOURCE_BILL_OF_MATERIAL_2])
            .createSeries(PaymentAction, [self.PAYMENT_ACTION_1, self.PAYMENT_ACTION_2])
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
            .createSeries(PaymentStructure, [self.PAYMENT_STRUCTURE_1, self.PAYMENT_STRUCTURE_2, self.PAYMENT_STRUCTURE_NO_DEPENDENCIES])
            .createSeries(PaymentStructureTier, [self.PAYMENT_STRUCTURE_TIER_1_1, self.PAYMENT_STRUCTURE_TIER_1_2, self.PAYMENT_STRUCTURE_TIER_2_1, self.PAYMENT_STRUCTURE_TIER_2_2])
            .createSeries(PaymentTenantBillOfMaterial, [self.PAYMENT_TENANT_BILL_OF_MATERIAL_1, self.PAYMENT_TENANT_BILL_OF_MATERIAL_2])
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
            .createSeries(PaymentType, [self.PAYMENT_TYPE_1, self.PAYMENT_TYPE_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

};
