var _ = require('lodash'),
    Config = require('./../../config/config.js'),
    Errors = require('./../../config/errors.js'),
    DataIds = require('./_id.data.js'),
    Database = require('./../../../index.js').getInstance(Config),
    DatabaseErrors = Database.Errors,
    RdbmsService = Database.RdbmsService,
    PoolHasQuestion = RdbmsService.Models.Question.PoolHasQuestion,
    PoolHasTag = RdbmsService.Models.Question.PoolHasTag,
    PoolHasTenant = RdbmsService.Models.Question.PoolHasTenant,
    PoolHasMedia = RdbmsService.Models.Question.PoolHasMedia,
    QuestionHasRelatedQuestion = RdbmsService.Models.Question.QuestionHasRelatedQuestion,
    QuestionHasTag = RdbmsService.Models.Question.QuestionHasTag,
    RegionalSettingHasLanguage = RdbmsService.Models.Question.RegionalSettingHasLanguage,
    QuestionTranslation = RdbmsService.Models.Question.QuestionTranslation,
    Question = RdbmsService.Models.Question.Question,
    Language = RdbmsService.Models.Question.Language,
    Pool = RdbmsService.Models.Question.Pool,
    RegionalSetting = RdbmsService.Models.Question.RegionalSetting,
    Tag = RdbmsService.Models.Question.Tag;

module.exports = {
    LANGUAGE_EN: {
        $id: DataIds.LANGUAGE_EN_ID,
        iso: 'en',
        name: 'English'
    },
    LANGUAGE_DE: {
        $id: DataIds.LANGUAGE_DE_ID,
        iso: 'de',
        name: 'Deutsch'
    },
    LANGUAGE_TEST: {
        $id: DataIds.LANGUAGE_TEST_ID,
        iso: 'te',
        name: 'Language Test'
    },

    REGIONAL_SETTING_1: {
        $id: DataIds.REGIONAL_SETTING_1_ID,
        iso: 'AF',
        name: 'Regional Setting 1',
        status: 'active',
        $defaultLanguageId: DataIds.LANGUAGE_EN_ID
    },
    REGIONAL_SETTING_2: {
        $id: DataIds.REGIONAL_SETTING_2_ID,
        iso: 'AX',
        name: 'Regional Setting 2',
        status: 'inactive',
        $defaultLanguageId: DataIds.LANGUAGE_DE_ID
    },
    REGIONAL_SETTING_TEST: {
        $id: DataIds.REGIONAL_SETTING_TEST_ID,
        name: 'Regional Setting Test',
        iso: 'ZW',
        status: 'active',
        $defaultLanguageId: DataIds.LANGUAGE_TEST_ID
    },

    REGIONAL_SETTING_1_HAS_LANGUAGE_EN: {
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        $languageId: DataIds.LANGUAGE_EN_ID,
    },
    REGIONAL_SETTING_1_HAS_LANGUAGE_DE: {
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        $languageId: DataIds.LANGUAGE_DE_ID,
    },
    REGIONAL_SETTING_2_HAS_LANGUAGE_EN: {
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        $languageId: DataIds.LANGUAGE_EN_ID,
    },
    REGIONAL_SETTING_2_HAS_LANGUAGE_DE: {
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        $languageId: DataIds.LANGUAGE_DE_ID,
    },

    TAG_1: {
        $tag: DataIds.TAG_1_TAG,
        isApproved: 0,
        approvedDate: null
    },
    TAG_2: {
        $tag: DataIds.TAG_2_TAG,
        isApproved: 1,
        approvedDate: _.now()
    },
    TAG_TEST: {
        $tag: DataIds.TAG_TEST_TAG,
        isApproved: 0,
        approvedDate: null
    },

    QUESTION_1: {
        $id: DataIds.QUESTION_1_ID,
        $questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        isMultiStepQuestion: 0,
        complexity: 1,
        $workorderId: DataIds.WORKORDER_1_ID,
        accessibleDate: _.now(),
        expirationDate: null,
        renewDate: null,
        isDeleted: 0,
        status: 'draft',
        $resolutionImageId: null,
        $exampleQuestionId: null,
        $primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        isInternational: 0,
        rating: 0,
        deploymentStatus: 'unpublished',
        deploymentDate: null,
        $regionalSettingIds: [DataIds.REGIONAL_SETTING_1_ID, DataIds.REGIONAL_SETTING_2_ID],
        $relatedQuestionsIds: null,
        $poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID],
        $tags: [DataIds.TAG_1_TAG, DataIds.TAG_2_TAG]
    },
    QUESTION_2: {
        $id: DataIds.QUESTION_2_ID,
        $questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        isMultiStepQuestion: 1,
        complexity: 2,
        $workorderId: DataIds.WORKORDER_2_ID,
        accessibleDate: _.now(),
        expirationDate: null,
        renewDate: null,
        isDeleted: 0,
        status: 'review',
        $resolutionImageId: DataIds.MEDIA_2_ID,
        $exampleQuestionId: DataIds.QUESTION_1_ID,
        $primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        isInternational: 1,
        rating: 2.5,
        deploymentStatus: 'deployed',
        deploymentDate: _.now(),
        $regionalSettingIds: [DataIds.REGIONAL_SETTING_1_ID, DataIds.REGIONAL_SETTING_2_ID],
        $relatedQuestionsIds: [DataIds.QUESTION_1_ID],
        $poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID],
        $tags: [DataIds.TAG_1_TAG, DataIds.TAG_2_TAG]
    },
    QUESTION_TEST: {
        $id: DataIds.QUESTION_TEST_ID,
        $questionTemplateId: DataIds.QUESTION_TEMPLATE_TEST_ID,
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        isMultiStepQuestion: 1,
        complexity: 3,
        $workorderId: DataIds.WORKORDER_TEST_ID,
        accessibleDate: _.now(),
        expirationDate: null,
        renewDate: null,
        isDeleted: 0,
        status: 'draft',
        $resolutionImageId: null,
        $exampleQuestionId: null,
        $primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        isInternational: 0,
        rating: 0,
        deploymentStatus: 'unpublished',
        deploymentDate: null,
        $regionalSettingIds: [DataIds.REGIONAL_SETTING_TEST_ID],
        $relatedQuestionsIds: null,
        $poolsIds: [DataIds.POOL_TEST_ID],
        $tags: [DataIds.TAG_TEST_TAG]
    },

    QUESTION_1_HAS_TAG_1: {
        $questionId: DataIds.QUESTION_1_ID,
        $tagTag: DataIds.TAG_1_TAG
    },
    QUESTION_2_HAS_TAG_2: {
        $questionId: DataIds.QUESTION_2_ID,
        $tagTag: DataIds.TAG_2_TAG
    },

    QUESTION_1_HAS_RELATED_QUESTION_2: {
        $questionId: DataIds.QUESTION_1_ID,
        $relatedQuestionId: DataIds.QUESTION_2_ID,
    },

    QUESTION_1_TRANSLATION_EN: {
        $id: DataIds.QUESTION_1_TRANSLATION_EN_ID,
        name: 'Question 1 Translation English',
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        $approverResourceId: null,
        approveDate: null,
        $blockerResourceId: null,
        blockDate: null,
        $languageId: DataIds.LANGUAGE_EN_ID,
        $questionId: DataIds.QUESTION_1_ID,
        $workorderId: DataIds.WORKORDER_1_ID,
        resolutionText: 'test 1EN',
        explanation: 'test1EN',
        hint: 'test1EN',
        content: null
    },
    QUESTION_1_TRANSLATION_DE: {
        $id: DataIds.QUESTION_1_TRANSLATION_DE_ID,
        name: 'Question 1 Translation Deutsch',
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        approverResourceId: null,
        approveDate: null,
        $blockerResourceId: null,
        blockDate: null,
        $languageId: DataIds.LANGUAGE_DE_ID,
        $questionId: DataIds.QUESTION_1_ID,
        $workorderId: DataIds.WORKORDER_1_ID,
        resolutionText: 'test 1DE',
        explanation: 'test1DE',
        hint: 'test1DE',
        content: null
    },
    QUESTION_2_TRANSLATION_EN: {
        $id: DataIds.QUESTION_2_TRANSLATION_EN_ID,
        name: 'Question 2 Translation English',
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        $approverResourceId: null,
        approveDate: null,
        $blockerResourceId: null,
        blockDate: null,
        $languageId: DataIds.LANGUAGE_EN_ID,
        $questionId: DataIds.QUESTION_2_ID,
        $workorderId: DataIds.WORKORDER_2_ID,
        resolutionText: 'test 2EN',
        explanation: 'test2EN',
        hint: 'test2EN',
        content: null
    },
    QUESTION_2_TRANSLATION_DE: {
        $id: DataIds.QUESTION_2_TRANSLATION_DE_ID,
        name: 'Question 2 Translation Deutsch',
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        $approverResourceId: null,
        approveDate: null,
        $blockerResourceId: null,
        blockDate: null,
        $languageId: DataIds.LANGUAGE_DE_ID,
        $questionId: DataIds.QUESTION_2_ID,
        $workorderId: DataIds.WORKORDER_2_ID,
        resolutionText: 'test 2DE',
        explanation: 'test1DE',
        hint: 'test1DE',
        content: null
    },

    POOL_1: {
        $id: DataIds.POOL_1_ID,
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        name: 'Pool 1',
        description: 'Pool 1 Description',
        $parentPoolId: null,
        status: 'active',
        iconId: 'Icon 1',
        color: 'white',
        minimumQuestions: 1,
        minimumQuestionsPerLanguage: 1,
        minimumQuestionsPerRegion: 1
    },
    POOL_2: {
        $id: DataIds.POOL_2_ID,
        $creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        name: 'Pool 2',
        description: 'Pool 2 Description',
        $parentPoolId: DataIds.POOL_1_ID,
        status: 'inactive',
        iconId: 'Icon 2',
        color: 'black',
        minimumQuestions: 2,
        minimumQuestionsPerLanguage: 2,
        minimumQuestionsPerRegion: 2
    },

    POOL_1_HAS_MEDIA_1: {
        $poolId: DataIds.POOL_1_ID,
        $mediaId: DataIds.MEDIA_1_ID
    },
    POOL_2_HAS_MEDIA_2: {
        $poolId: DataIds.POOL_2_ID,
        $mediaId: DataIds.MEDIA_2_ID
    },

    POOL_1_HAS_QUESTION_1: {
        $poolId: DataIds.POOL_1_ID,
        $questionId: DataIds.QUESTION_1_ID,
    },
    POOL_2_HAS_QUESTION_2: {
        $poolId: DataIds.POOL_1_ID,
        $questionId: DataIds.QUESTION_2_ID,
    },

    POOL_1_HAS_TAG_1: {
        $poolId: DataIds.POOL_1_ID,
        $tagTag: DataIds.TAG_1_TAG
    },
    POOL_2_HAS_TAG_2: {
        $poolId: DataIds.POOL_2_ID,
        $tagTag: DataIds.TAG_2_TAG
    },

    POOL_1_HAS_REGIONAL_SETTING_1: {
        $poolId: DataIds.POOL_1_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    POOL_2_HAS_REGIONAL_SETTING_2: {
        $poolId: DataIds.POOL_2_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    POOL_1_HAS_TENANT_1: {
        $poolId: DataIds.POOL_1_ID,
        $tenantId: DataIds.TENANT_1_ID,
        role: 'view',
    },
    POOL_2_HAS_TENANT_2: {
        $poolId: DataIds.POOL_2_ID,
        $tenantId: DataIds.TENANT_2_ID,
        role: 'sell',
    },

    loadClassifiers: function (testSet) {
        return testSet
            .createSeries(Tag, [this.TAG_1, this.TAG_2])
            .createSeries(Language, [this.LANGUAGE_EN, this.LANGUAGE_DE])
            .createSeries(RegionalSetting, [this.REGIONAL_SETTING_1, this.REGIONAL_SETTING_2])
            .createSeries(RegionalSettingHasLanguage, [this.REGIONAL_SETTING_1_HAS_LANGUAGE_EN, this.REGIONAL_SETTING_1_HAS_LANGUAGE_DE, this.REGIONAL_SETTING_2_HAS_LANGUAGE_EN, this.REGIONAL_SETTING_2_HAS_LANGUAGE_DE])
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(QuestionHasTag, [this.QUESTION_1_HAS_TAG_1, this.QUESTION_2_HAS_TAG_2])
            .create(QuestionHasRelatedQuestion, this.QUESTION_1_HAS_RELATED_QUESTION_2)
            .createSeries(PoolHasMedia, [this.POOL_1_HAS_MEDIA_1, this.POOL_2_HAS_MEDIA_2])
            .createSeries(PoolHasQuestion, [this.POOL_1_HAS_QUESTION_1, this.POOL_2_HAS_QUESTION_2])
            .createSeries(PoolHasTenant, [this.POOL_1_HAS_TENANT_1, this.POOL_2_HAS_TENANT_2])
            .createSeries(PoolHasTag, [this.POOL_1_HAS_TAG_1, this.POOL_2_HAS_TAG_2])
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(Pool, [this.POOL_1, this.POOL_2])
            .createSeries(Question, [this.QUESTION_1, this.QUESTION_2])
            .createSeries(QuestionTranslation, [this.QUESTION_1_TRANSLATION_EN, this.QUESTION_1_TRANSLATION_DE, this.QUESTION_2_TRANSLATION_EN, this.QUESTION_2_TRANSLATION_DE])
    },

};
