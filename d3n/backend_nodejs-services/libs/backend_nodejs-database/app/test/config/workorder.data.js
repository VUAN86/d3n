var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('./../../../index.js').getInstance(Config);
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
        $id: DataIds.WORKORDER_1_ID,
        $ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        title: 'Title',
        description: 'Description',
        $questionCreatePaymentStructureId: DataIds.PAYMENT_STRUCTURE_MODEL_1_ID,
        $questionReviewPaymentStructureId: null,
        $translationCreatePaymentStructureId: null,
        $translationReviewPaymentStructureId: null,
        startDate: _.now(),
        endDate: _.now() + 10000,
        status: 'draft',
        iconId: 'Icon 1',
        color: 'white',
        itemsRequired: 0,
        type: 'question',
        questionCreateIsCommunity: 1,
        questionReviewIsCommunity: 0,
        translationCreateIsCommunity: 0,
        translationReviewIsCommunity: 0,
        $primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        $questionsExamplesIds: [DataIds.QUESTION_1_ID, DataIds.QUESTION_2_ID],
        $poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID],
        $tenantsIds: [DataIds.TENANT_1_ID, DataIds.TENANT_2_ID],
        $questionsIds: [DataIds.QUESTION_1_ID, DataIds.QUESTION_2_ID]
    },
    WORKORDER_2: {
        $id: DataIds.WORKORDER_2_ID,
        $ownerResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        title: 'Title',
        description: 'Description',
        $questionCreatePaymentStructureId: null,
        $questionReviewPaymentStructureId: null,
        $translationCreatePaymentStructureId: DataIds.PAYMENT_STRUCTURE_MODEL_2_ID,
        $translationReviewPaymentStructureId: null,
        startDate: _.now(),
        endDate: _.now() + 10000,
        status: 'inprogress',
        iconId: 'Icon 2',
        color: 'black',
        itemsRequired: 1,
        type: 'translation',
        questionCreateIsCommunity: 0,
        questionReviewIsCommunity: 0,
        translationCreateIsCommunity: 1,
        translationReviewIsCommunity: 0,
        $primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        $questionsExamplesIds: [DataIds.QUESTION_1_ID, DataIds.QUESTION_2_ID],
        $poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID],
        $tenantsIds: [DataIds.TENANT_1_ID, DataIds.TENANT_2_ID],
        $questionsIds: [DataIds.QUESTION_1_ID, DataIds.QUESTION_2_ID]
    },
    WORKORDER_TEST: {
        $id: DataIds.WORKORDER_TEST_ID,
        $ownerResourceId: DataIds.TEST_USER_ID,
        createDate: _.now(),
        title: 'Title',
        description: 'Description',
        $questionCreatePaymentStructureId: null,
        $questionReviewPaymentStructureId: null,
        $translationCreatePaymentStructureId: DataIds.PAYMENT_STRUCTURE_MODEL_2_ID,
        $translationReviewPaymentStructureId: null,
        startDate: _.now(),
        endDate: _.now() + 10000,
        status: 'draft',
        iconId: 'Icon Test',
        color: 'yellow',
        itemsRequired: 2,
        type: 'media',
        questionCreateIsCommunity: 0,
        questionReviewIsCommunity: 0,
        translationCreateIsCommunity: 1,
        translationReviewIsCommunity: 0,
        $primaryRegionalSettingId: DataIds.REGIONAL_SETTING_TEST_ID,
    },

    WORKORDER_1_HAS_MEDIA_1: {
        $workorderId: DataIds.WORKORDER_1_ID,
        $mediaId: DataIds.MEDIA_1_ID
    },
    WORKORDER_2_HAS_MEDIA_2: {
        $workorderId: DataIds.WORKORDER_2_ID,
        $mediaId: DataIds.MEDIA_2_ID
    },

    WORKORDER_1_HAS_POOL_1: {
        $workorderId: DataIds.WORKORDER_1_ID,
        $poolId: DataIds.POOL_1_ID
    },
    WORKORDER_2_HAS_POOL_2: {
        $workorderId: DataIds.WORKORDER_2_ID,
        $poolId: DataIds.POOL_2_ID
    },

    WORKORDER_1_HAS_TENANT_1: {
        $workorderId: DataIds.WORKORDER_1_ID,
        $tenantId: DataIds.TENANT_1_ID
    },
    WORKORDER_2_HAS_TENANT_2: {
        $workorderId: DataIds.WORKORDER_2_ID,
        $tenantId: DataIds.TENANT_2_ID
    },

    WORKORDER_1_HAS_RESOURCE: {
        $workorderId: DataIds.WORKORDER_1_ID,
        $resourceId: DataIds.LOCAL_USER_ID,
        action: WorkorderHasResource.constants().ACTION_QUESTION_CREATE
    },
    WORKORDER_2_HAS_RESOURCE: {
        $workorderId: DataIds.WORKORDER_2_ID,
        $resourceId: DataIds.LOCAL_USER_ID,
        action: WorkorderHasResource.constants().ACTION_TRANSLATION_CREATE
    },

    WORKORDER_1_HAS_REGIONAL_SETTING_1: {
        $workorderId: DataIds.WORKORDER_1_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    WORKORDER_2_HAS_REGIONAL_SETTING_2: {
        $workorderId: DataIds.WORKORDER_2_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    WORKORDER_1_HAS_QUESTION_1_EXAMPLE: {
        $workorderId: DataIds.WORKORDER_1_ID,
        $questionId: DataIds.QUESTION_1_ID,
        number: 1
    },
    WORKORDER_2_HAS_QUESTION_2_EXAMPLE: {
        $workorderId: DataIds.WORKORDER_2_ID,
        $questionId: DataIds.QUESTION_2_ID,
        number: 2
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(WorkorderHasPool, [this.WORKORDER_1_HAS_POOL_1, this.WORKORDER_2_HAS_POOL_2])
            .createSeries(WorkorderHasTenant, [this.WORKORDER_1_HAS_TENANT_1, this.WORKORDER_2_HAS_TENANT_2])
            .createSeries(WorkorderHasResource, [this.WORKORDER_1_HAS_RESOURCE, this.WORKORDER_2_HAS_RESOURCE])
            .createSeries(WorkorderHasRegionalSetting, [this.WORKORDER_1_HAS_REGIONAL_SETTING_1, this.WORKORDER_2_HAS_REGIONAL_SETTING_2])
            .createSeries(WorkorderHasQuestionExample, [this.WORKORDER_1_HAS_QUESTION_1_EXAMPLE, this.WORKORDER_2_HAS_QUESTION_2_EXAMPLE])
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(Workorder, [this.WORKORDER_1, this.WORKORDER_2])
    },

};
