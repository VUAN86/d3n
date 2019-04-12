var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var WorkorderHasPool = RdbmsService.Models.Workorder.WorkorderHasPool;
var WorkorderHasTenant = RdbmsService.Models.Workorder.WorkorderHasTenant;
var WorkorderHasResource = RdbmsService.Models.Workorder.WorkorderHasResource;
var WorkorderHasRegionalSetting = RdbmsService.Models.Workorder.WorkorderHasRegionalSetting;
var WorkorderHasQuestionExample = RdbmsService.Models.Workorder.WorkorderHasQuestionExample;

module.exports = {

    WORKORDER_1: {
        id: DataIds.WORKORDER_1_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description',
        questionCreatePaymentStructureId: DataIds.PAYMENT_STRUCTURE_1_ID,
        questionReviewPaymentStructureId: null,
        translationCreatePaymentStructureId: null,
        translationReviewPaymentStructureId: null,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'draft',
        itemsRequired: 1,
        type: 'question',
        questionCreateIsCommunity: 1,
        questionReviewIsCommunity: 0,
        translationCreateIsCommunity: 0,
        translationReviewIsCommunity: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        questionsExamples: [{id: DataIds.QUESTION_1_ID, number: 20 }],
        poolsIds: [DataIds.POOL_1_ID],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        questionsIds: [DataIds.QUESTION_1_ID]
    },
    WORKORDER_2: {
        id: DataIds.WORKORDER_2_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description',
        questionCreatePaymentStructureId: null,
        questionReviewPaymentStructureId: null,
        translationCreatePaymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        translationReviewPaymentStructureId: null,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'inprogress',
        itemsRequired: 1,
        type: 'translation',
        questionCreateIsCommunity: 0,
        questionReviewIsCommunity: 0,
        translationCreateIsCommunity: 1,
        translationReviewIsCommunity: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        questionsExamples: [{ id: DataIds.QUESTION_2_ID, number: 20 }],
        poolsIds: [DataIds.POOL_2_ID],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        questionsIds: [DataIds.QUESTION_2_ID]
    },
    WORKORDER_NO_DEPENDENCIES: {
        id: DataIds.WORKORDER_NO_DEPENDENCIES_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description ND',
        questionCreatePaymentStructureId: null,
        questionReviewPaymentStructureId: null,
        translationCreatePaymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        translationReviewPaymentStructureId: null,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'draft',
        itemsRequired: 2,
        type: 'media',
        questionCreateIsCommunity: 0,
        questionReviewIsCommunity: 0,
        translationCreateIsCommunity: 1,
        translationReviewIsCommunity: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        questionsExamples: [],
        poolsIds: [],
        regionalSettingsIds: [],
        questionsIds: []
    },
    WORKORDER_TEST: {
        id: DataIds.WORKORDER_TEST_ID,
        ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        title: 'Title',
        description: 'Description',
        questionCreatePaymentStructureId: null,
        questionReviewPaymentStructureId: null,
        translationCreatePaymentStructureId: DataIds.PAYMENT_STRUCTURE_2_ID,
        translationReviewPaymentStructureId: null,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        status: 'draft',
        itemsRequired: 2,
        type: 'media',
        questionCreateIsCommunity: 0,
        questionReviewIsCommunity: 0,
        translationCreateIsCommunity: 1,
        translationReviewIsCommunity: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        questionsExamples: [],
        poolsIds: [],
        regionalSettingsIds: [],
        questionsIds: []
    },

    WORKORDER_1_HAS_MEDIA_1: {
        workorderId: DataIds.WORKORDER_1_ID,
        mediaId: DataIds.MEDIA_1_ID
    },
    WORKORDER_2_HAS_MEDIA_2: {
        workorderId: DataIds.WORKORDER_2_ID,
        mediaId: DataIds.MEDIA_2_ID
    },

    WORKORDER_1_HAS_POOL_1: {
        workorderId: DataIds.WORKORDER_1_ID,
        poolId: DataIds.POOL_1_ID
    },
    WORKORDER_2_HAS_POOL_2: {
        workorderId: DataIds.WORKORDER_2_ID,
        poolId: DataIds.POOL_2_ID
    },

    WORKORDER_1_HAS_TENANT_1: {
        workorderId: DataIds.WORKORDER_1_ID,
        tenantId: DataIds.TENANT_1_ID
    },
    WORKORDER_2_HAS_TENANT_2: {
        workorderId: DataIds.WORKORDER_2_ID,
        tenantId: DataIds.TENANT_2_ID
    },

    WORKORDER_1_HAS_RESOURCE_1: {
        workorderId: DataIds.WORKORDER_1_ID,
        resourceId: DataIds.LOCAL_USER_ID,
        action: WorkorderHasResource.constants().ACTION_QUESTION_CREATE
    },
    WORKORDER_1_HAS_RESOURCE_2: {
        workorderId: DataIds.WORKORDER_1_ID,
        resourceId: DataIds.LOCAL_USER_ID,
        action: WorkorderHasResource.constants().ACTION_QUESTION_REVIEW
    },
    WORKORDER_2_HAS_RESOURCE: {
        workorderId: DataIds.WORKORDER_2_ID,
        resourceId: DataIds.LOCAL_USER_ID,
        action: WorkorderHasResource.constants().ACTION_TRANSLATION_CREATE
    },

    WORKORDER_1_HAS_REGIONAL_SETTING_1: {
        workorderId: DataIds.WORKORDER_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    WORKORDER_2_HAS_REGIONAL_SETTING_2: {
        workorderId: DataIds.WORKORDER_2_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    WORKORDER_1_HAS_QUESTION_1_EXAMPLE: {
        workorderId: DataIds.WORKORDER_1_ID,
        questionId: DataIds.QUESTION_1_ID,
        number: 20
    },
    WORKORDER_2_HAS_QUESTION_2_EXAMPLE: {
        workorderId: DataIds.WORKORDER_2_ID,
        questionId: DataIds.QUESTION_2_ID,
        number: 20
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(WorkorderHasQuestionExample, [self.WORKORDER_1_HAS_QUESTION_1_EXAMPLE, self.WORKORDER_2_HAS_QUESTION_2_EXAMPLE], 'workorderId')
            .removeSeries(WorkorderHasRegionalSetting, [self.WORKORDER_1_HAS_REGIONAL_SETTING_1, self.WORKORDER_2_HAS_REGIONAL_SETTING_2], 'workorderId')
            .removeSeries(WorkorderHasResource, [self.WORKORDER_1_HAS_RESOURCE_1, self.WORKORDER_1_HAS_RESOURCE_2, self.WORKORDER_2_HAS_RESOURCE], 'workorderId')
            .removeSeries(WorkorderHasTenant, [self.WORKORDER_1_HAS_TENANT_1, self.WORKORDER_2_HAS_TENANT_2], 'workorderId')
            .removeSeries(WorkorderHasPool, [self.WORKORDER_1_HAS_POOL_1, self.WORKORDER_2_HAS_POOL_2], 'workorderId')
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
            .createSeries(WorkorderHasPool, [self.WORKORDER_1_HAS_POOL_1, self.WORKORDER_2_HAS_POOL_2], 'workorderId')
            .createSeries(WorkorderHasTenant, [self.WORKORDER_1_HAS_TENANT_1, self.WORKORDER_2_HAS_TENANT_2], 'workorderId')
            .createSeries(WorkorderHasResource, [self.WORKORDER_1_HAS_RESOURCE_1, self.WORKORDER_1_HAS_RESOURCE_2, self.WORKORDER_2_HAS_RESOURCE], 'workorderId')
            .createSeries(WorkorderHasRegionalSetting, [self.WORKORDER_1_HAS_REGIONAL_SETTING_1, self.WORKORDER_2_HAS_REGIONAL_SETTING_2], 'workorderId')
            .createSeries(WorkorderHasQuestionExample, [self.WORKORDER_1_HAS_QUESTION_1_EXAMPLE, self.WORKORDER_2_HAS_QUESTION_2_EXAMPLE], 'workorderId')
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
