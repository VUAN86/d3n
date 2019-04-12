var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var WorkorderBillingModel = RdbmsService.Models.Workorder.WorkorderBillingModel;

module.exports = {
    WORKORDER_BILLING_MODEL_1: {
        id: DataIds.WORKORDER_BILLING_MODEL_1_ID,
        name: 'Workorder Billing Model 1'
    },
    WORKORDER_1: {
        id: DataIds.WORKORDER_1_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description',
        workorderBillingModelId: DataIds.WORKORDER_BILLING_MODEL_1_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'draft',
        itemsRequired: 0,
        type: 'question',
        isCommunity: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        questionsExamples: [{id: DataIds.QUESTION_1_ID, number: 20 }],
        poolsIds: [DataIds.POOL_1_ID],
        tenantsIds: null,
        questionsIds: [DataIds.QUESTION_1_ID]
    },
    WORKORDER_2: {
        id: DataIds.WORKORDER_2_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description',
        workorderBillingModelId: DataIds.WORKORDER_BILLING_MODEL_1_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'inprogress',
        itemsRequired: 1,
        type: 'translation',
        isCommunity: 1,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        questionsExamples: [{ id: DataIds.QUESTION_2_ID, number: 20 }],
        poolsIds: [DataIds.POOL_2_ID],
        tenantsIds: [],
        questionsIds: [DataIds.QUESTION_2_ID]
    },
    WORKORDER_NO_DEPENDENCIES: {
        id: DataIds.WORKORDER_NO_DEPENDENCIES_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description ND',
        workorderBillingModelId: DataIds.WORKORDER_BILLING_MODEL_1_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'draft',
        itemsRequired: 2,
        type: 'media',
        isCommunity: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        questionsExamples: [],
        poolsIds: [],
        tenantsIds: [],
        questionsIds: []
    },
    WORKORDER_TEST: {
        id: DataIds.WORKORDER_TEST_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description',
        workorderBillingModelId: DataIds.WORKORDER_BILLING_MODEL_1_ID,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'draft',
        itemsRequired: 2,
        type: 'media',
        isCommunity: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        questionsExamples: [],
        poolsIds: [],
        tenantsIds: [],
        questionsIds: []
    },

    WORKORDER_1_HAS_POOL_1: {
        workorderId: DataIds.WORKORDER_1_ID,
        poolId: DataIds.POOL_1_ID
    },
    WORKORDER_2_HAS_POOL_2: {
        workorderId: DataIds.WORKORDER_2_ID,
        poolId: DataIds.POOL_2_ID
    },

    // WORKORDER_1_HAS_TENANT_1: {
    //     workorderId: DataIds.WORKORDER_1_ID,
    //     tenantId: DataIds.TENANT_1_ID
    // },
    // WORKORDER_2_HAS_TENANT_2: {
    //     workorderId: DataIds.WORKORDER_2_ID,
    //     tenantId: DataIds.TENANT_2_ID
    // },

    // WORKORDER_1_HAS_RESOURCE: {
    //     workorderId: DataIds.WORKORDER_1_ID,
    //     resourceId: DataIds.LOCAL_USER_ID
    // },
    // WORKORDER_2_HAS_RESOURCE: {
    //     workorderId: DataIds.WORKORDER_2_ID,
    //     resourceId: DataIds.LOCAL_USER_ID
    // },

    WORKORDER_1_HAS_REGIONAL_SETTING_1: {
        workorderId: DataIds.WORKORDER_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    WORKORDER_2_HAS_REGIONAL_SETTING_1: {
        workorderId: DataIds.WORKORDER_2_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },

    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(WorkorderBillingModel, [self.WORKORDER_BILLING_MODEL_1])
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
            .createSeries(WorkorderBillingModel, [self.WORKORDER_BILLING_MODEL_1])
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
            .removeSeries(Workorder, [self.WORKORDER_1, self.WORKORDER_2, self.WORKORDER_NO_DEPENDENCIES, self.WORKORDER_TEST])
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
            .createSeries(Workorder, [self.WORKORDER_1, self.WORKORDER_2, self.WORKORDER_NO_DEPENDENCIES])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

};
