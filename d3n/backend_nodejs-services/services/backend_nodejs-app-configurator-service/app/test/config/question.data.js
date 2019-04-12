var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var PoolHasQuestion = RdbmsService.Models.Question.PoolHasQuestion;
var PoolHasTag = RdbmsService.Models.Question.PoolHasTag;
var PoolHasTenant = RdbmsService.Models.Question.PoolHasTenant;
var PoolHasMedia = RdbmsService.Models.Question.PoolHasMedia;
var QuestionHasRegionalSetting = RdbmsService.Models.Question.QuestionHasRegionalSetting;
var QuestionHasRelatedQuestion = RdbmsService.Models.Question.QuestionHasRelatedQuestion;
var QuestionHasTag = RdbmsService.Models.Question.QuestionHasTag;
var RegionalSetting = RdbmsService.Models.Question.RegionalSetting;
var RegionalSettingHasLanguage = RdbmsService.Models.Question.RegionalSettingHasLanguage;
var QuestionTranslation = RdbmsService.Models.Question.QuestionTranslation;
var Question = RdbmsService.Models.Question.Question;
var Language = RdbmsService.Models.Question.Language;
var Pool = RdbmsService.Models.Question.Pool;
var Tag = RdbmsService.Models.Question.Tag;

module.exports = {
    LANGUAGE_EN: {
        id: DataIds.LANGUAGE_EN_ID,
        iso: 'en',
        name: 'English',
        tenantId: DataIds.TENANT_1_ID
    },
    LANGUAGE_DE: {
        id: DataIds.LANGUAGE_DE_ID,
        iso: 'de',
        name: 'Deutsch'
    },
    LANGUAGE_RU: {
        id: DataIds.LANGUAGE_RU_ID,
        iso: 'ru',
        name: 'Russian'
    },
    LANGUAGE_TEST: {
        id: DataIds.LANGUAGE_TEST_ID,
        iso: 'te',
        name: 'Language Test'
    },

    REGIONAL_SETTING_1: {
        id: DataIds.REGIONAL_SETTING_1_ID,
        iso: 'AF',
        name: 'Regional Setting 1',
        status: RegionalSetting.constants().STATUS_ACTIVE,
        defaultLanguageId: DataIds.LANGUAGE_EN_ID,
        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID]
    },
    REGIONAL_SETTING_2: {
        id: DataIds.REGIONAL_SETTING_2_ID,
        iso: 'AX',
        name: 'Regional Setting 2',
        status: RegionalSetting.constants().STATUS_ACTIVE,
        defaultLanguageId: DataIds.LANGUAGE_DE_ID,
        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID]
    },
    REGIONAL_SETTING_NO_DEPENDENCIES: {
        id: DataIds.REGIONAL_SETTING_NO_DEPENDENCIES_ID,
        iso: 'YE',
        name: 'Regional Setting 3',
        status: RegionalSetting.constants().STATUS_ACTIVE,
        defaultLanguageId: DataIds.LANGUAGE_DE_ID,
        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID]
    },
    REGIONAL_SETTING_TEST: {
        id: DataIds.REGIONAL_SETTING_TEST_ID,
        iso: 'ZW',
        name: 'Regional Setting Test',
        status: RegionalSetting.constants().STATUS_ACTIVE,
        defaultLanguageId: DataIds.LANGUAGE_DE_ID,
        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID]
    },

    REGIONAL_SETTING_1_HAS_LANGUAGE_EN: {
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
    },
    REGIONAL_SETTING_1_HAS_LANGUAGE_DE: {
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        languageId: DataIds.LANGUAGE_DE_ID,
    },
    REGIONAL_SETTING_2_HAS_LANGUAGE_EN: {
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
    },
    REGIONAL_SETTING_2_HAS_LANGUAGE_DE: {
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        languageId: DataIds.LANGUAGE_DE_ID,
    },

    TAG_1: {
        tag: DataIds.TAG_1_TAG,
        isApproved: 0,
        approvedDate: null
    },
    TAG_2: {
        tag: DataIds.TAG_2_TAG,
        isApproved: 1,
        approvedDate: DateUtils.isoNow()
    },
    TAG_TEST: {
        tag: DataIds.TAG_TEST_TAG,
        isApproved: 0,
        approvedDate: null
    },

    QUESTION_1: {
        id: DataIds.QUESTION_1_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 1,
        workorderId: DataIds.WORKORDER_1_ID,
        accessibleDate: DateUtils.isoNow(),
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_DRAFT,
        isInternational: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        rating: 1,
        source: 'source',
        countdown: 123,
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        relatedQuestionsIds: null,
        poolsIds: [DataIds.POOL_1_ID],
        questionTranslationsIds: [DataIds.QUESTION_1_TRANSLATION_EN_ID, DataIds.QUESTION_1_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_1_TAG]
    },
    QUESTION_2: {
        id: DataIds.QUESTION_2_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 1,
        workorderId: DataIds.WORKORDER_2_ID,
        accessibleDate: DateUtils.isoNow(),
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_REVIEW,
        exampleQuestionId: DataIds.QUESTION_2_ID,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        rating: 2,
        source: 'source',
        countdown: 234,
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        relatedQuestionsIds: [DataIds.QUESTION_1_ID],
        poolsIds: [DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_2_TRANSLATION_EN_ID, DataIds.QUESTION_2_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_2_TAG]
    },
    QUESTION_3: {
        id: DataIds.QUESTION_3_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 1,
        workorderId: DataIds.WORKORDER_2_ID,
        accessibleDate: DateUtils.isoNow(),
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_ACTIVE,
        isInternational: 1,
        exampleQuestionId: DataIds.QUESTION_2_ID,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        rating: 2,
        source: 'source',
        countdown: 234,
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        relatedQuestionsIds: [DataIds.QUESTION_1_ID],
        poolsIds: [DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_2_TRANSLATION_EN_ID, DataIds.QUESTION_2_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_2_TAG]
    },
    QUESTION_4: {
        id: DataIds.QUESTION_4_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 1,
        workorderId: DataIds.WORKORDER_2_ID,
        accessibleDate: null,
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_ACTIVE,
        isInternational: 1,
        exampleQuestionId: DataIds.QUESTION_2_ID,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        rating: 2,
        source: 'source',
        countdown: 234,
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        relatedQuestionsIds: [DataIds.QUESTION_1_ID],
        poolsIds: [DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_2_TRANSLATION_EN_ID, DataIds.QUESTION_2_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_2_TAG]
    },
    QUESTION_NO_DEPENDENCIES: {
        id: DataIds.QUESTION_NO_DEPENDENCIES_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 3,
        workorderId: null,
        accessibleDate: DateUtils.isoNow(),
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_ARCHIVED,
        isInternational: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        rating: 3,
        source: 'source',
        countdown: 123,
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        relatedQuestionsIds: [],
        poolsIds: [DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_2_TRANSLATION_EN_ID, DataIds.QUESTION_2_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_2_TAG]
    },
    QUESTION_TEST: {
        id: DataIds.QUESTION_TEST_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 3,
        workorderId: DataIds.WORKORDER_1_ID,
        accessibleDate: DateUtils.isoNow(),
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_DRAFT,
        isInternational: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        rating: null,
        source: 'source',
        countdown: 234,
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        relatedQuestionsIds: [DataIds.QUESTION_2_ID],
        poolsIds: [DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_2_TRANSLATION_EN_ID, DataIds.QUESTION_2_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_2_TAG]
    },
    QUESTION_1_HAS_TAG_1: {
        questionId: DataIds.QUESTION_1_ID,
        tagTag: DataIds.TAG_1_TAG
    },
    QUESTION_2_HAS_TAG_2: {
        questionId: DataIds.QUESTION_2_ID,
        tagTag: DataIds.TAG_2_TAG
    },

    QUESTION_1_HAS_RELATED_QUESTION_2: {
        questionId: DataIds.QUESTION_1_ID,
        relatedQuestionId: DataIds.QUESTION_2_ID,
    },

    QUESTION_1_HAS_REGIONAL_SETTING_1: {
        questionId: DataIds.QUESTION_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
    },
    QUESTION_2_HAS_REGIONAL_SETTING_2: {
        questionId: DataIds.QUESTION_2_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
    },
    QUESTION_3_HAS_REGIONAL_SETTING_1: {
        questionId: DataIds.QUESTION_3_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
    },
    QUESTION_4_HAS_REGIONAL_SETTING_1: {
        questionId: DataIds.QUESTION_4_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
    },

    QUESTION_1_TRANSLATION_EN: {
        id: DataIds.QUESTION_1_TRANSLATION_EN_ID,
        name: 'Question 1 Translation English',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        questionId: DataIds.QUESTION_1_ID,
        workorderId: DataIds.WORKORDER_1_ID,
        content: null,
        publishIndex: 0
    },
    QUESTION_1_TRANSLATION_DE: {
        id: DataIds.QUESTION_1_TRANSLATION_DE_ID,
        name: 'Question 1 Translation Deutsch',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_DE_ID,
        questionId: DataIds.QUESTION_1_ID,
        workorderId: DataIds.WORKORDER_1_ID,
        content: null,
        publishIndex: 1
    },
    QUESTION_2_TRANSLATION_EN: {
        id: DataIds.QUESTION_2_TRANSLATION_EN_ID,
        name: 'Question 2 Translation English',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        questionId: DataIds.QUESTION_2_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null,
        publishIndex: 0
    },
    QUESTION_2_TRANSLATION_DE: {
        id: DataIds.QUESTION_2_TRANSLATION_DE_ID,
        name: 'Question 2 Translation Deutsch',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_DE_ID,
        questionId: DataIds.QUESTION_2_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null,
        publishIndex: 1
    },
    QUESTION_3_TRANSLATION_EN: {
        id: DataIds.QUESTION_3_TRANSLATION_EN_ID,
        name: 'Question 3 Translation English',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        questionId: DataIds.QUESTION_3_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null,
        publishIndex: 0
    },
    QUESTION_3_TRANSLATION_DE: {
        id: DataIds.QUESTION_3_TRANSLATION_DE_ID,
        name: 'Question 2 Translation Deutsch',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_DE_ID,
        questionId: DataIds.QUESTION_3_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null,
        publishIndex: 1
    },
    QUESTION_4_TRANSLATION_EN: {
        id: DataIds.QUESTION_4_TRANSLATION_EN_ID,
        name: 'Question 4 Translation English',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        questionId: DataIds.QUESTION_4_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null,
        publishIndex: 0
    },
    QUESTION_4_TRANSLATION_DE: {
        id: DataIds.QUESTION_4_TRANSLATION_DE_ID,
        name: 'Question 4 Translation Deutsch',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_DE_ID,
        questionId: DataIds.QUESTION_4_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null,
        publishIndex: 1
    },

    QUESTION_TRANSLATION_NO_DEPENDENCIES: {
        id: DataIds.QUESTION_TRANSLATION_NO_DEPENDENCIES_ID,
        name: 'Question Translation No Dependencies',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        questionId: DataIds.QUESTION_2_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null
    },
    QUESTION_TRANSLATION_TEST: {
        id: DataIds.QUESTION_TRANSLATION_TEST_ID,
        name: 'Question Translation Test',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        questionId: DataIds.QUESTION_2_ID,
        workorderId: DataIds.WORKORDER_2_ID,
        content: null
    },

    POOL_1: {
        id: DataIds.POOL_1_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        name: 'Pool 1',
        description: 'Pool 1 Description',
        parentPoolId: null,
        status: Pool.constants().STATUS_ACTIVE,
        minimumQuestions: 1,
        iconId: 'icon23',
        color: 'color12',
        //minimumQuestionsPerLanguage: 1,
        //minimumQuestionsPerRegion: 1,
        subpoolsIds: [DataIds.POOL_2_ID],
        questionsIds: [DataIds.QUESTION_1_ID],
        tenantsIds: [DataIds.TENANT_1_ID],
        mediasIds: [DataIds.MEDIA_1_ID],
        tags: [DataIds.TAG_1_TAG]
    },
    POOL_2: {
        id: DataIds.POOL_2_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        name: 'Pool 2',
        description: 'Pool 2 Description',
        parentPoolId: DataIds.POOL_1_ID,
        status: Pool.constants().STATUS_ACTIVE,
        minimumQuestions: 2,
        iconId: 'icon12',
        color: 'color23',
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        subpoolsIds: [],
        questionsIds: [DataIds.QUESTION_2_ID],
        tenantsIds: [DataIds.TENANT_2_ID],
        mediasIds: [DataIds.MEDIA_2_ID],
        tags: [DataIds.TAG_2_TAG]
    },
    POOL_NO_DEPENDENCIES: {
        id: DataIds.POOL_NO_DEPENDENCIES_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        name: 'Pool 3',
        description: 'Pool 3 Description',
        parentPoolId: null,
        status: Pool.constants().STATUS_ACTIVE,
        minimumQuestions: 2,
        iconId: 'iconND',
        color: 'colorND',
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        subpoolsIds: [],
        questionsIds: [],
        tenantsIds: [],
        mediasIds: [DataIds.MEDIA_2_ID],
        tags: [DataIds.TAG_1_TAG]
    },
    POOL_TEST: {
        id: DataIds.POOL_TEST_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        name: 'Pool Test',
        description: 'Pool Test Description',
        parentPoolId: null,
        status: Pool.constants().STATUS_ACTIVE,
        minimumQuestions: 2,
        iconId: 'iconTest',
        color: 'colorTest',
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        subpoolsIds: [],
        questionsIds: [],
        tenantsIds: [],
        mediasIds: [],
        tags: [DataIds.TAG_1_TAG]
    },

    POOL_1_HAS_MEDIA_1: {
        poolId: DataIds.POOL_1_ID,
        mediaId: DataIds.MEDIA_1_ID
    },
    POOL_2_HAS_MEDIA_2: {
        poolId: DataIds.POOL_2_ID,
        mediaId: DataIds.MEDIA_2_ID
    },

    POOL_1_HAS_QUESTION_1: {
        poolId: DataIds.POOL_1_ID,
        questionId: DataIds.QUESTION_1_ID,
    },

    POOL_1_HAS_QUESTION_3: {
        poolId: DataIds.POOL_1_ID,
        questionId: DataIds.QUESTION_3_ID,
    },

    POOL_1_HAS_QUESTION_4: {
        poolId: DataIds.POOL_1_ID,
        questionId: DataIds.QUESTION_4_ID,
    },


    POOL_2_HAS_QUESTION_2: {
        poolId: DataIds.POOL_2_ID,
        questionId: DataIds.QUESTION_2_ID,
    },

    POOL_1_HAS_TAG_1: {
        poolId: DataIds.POOL_1_ID,
        tagTag: DataIds.TAG_1_TAG
    },
    POOL_2_HAS_TAG_2: {
        poolId: DataIds.POOL_2_ID,
        tagTag: DataIds.TAG_2_TAG
    },

    POOL_1_HAS_REGIONAL_SETTING_1: {
        poolId: DataIds.POOL_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    POOL_2_HAS_REGIONAL_SETTING_2: {
        poolId: DataIds.POOL_2_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    POOL_1_HAS_TENANT_1: {
        poolId: DataIds.POOL_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: 'view',
    },
    POOL_2_HAS_TENANT_2: {
        poolId: DataIds.POOL_2_ID,
        tenantId: DataIds.TENANT_2_ID,
        role: 'sell',
    },

    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(RegionalSettingHasLanguage, [self.REGIONAL_SETTING_1_HAS_LANGUAGE_EN, self.REGIONAL_SETTING_1_HAS_LANGUAGE_DE, self.REGIONAL_SETTING_2_HAS_LANGUAGE_EN, self.REGIONAL_SETTING_2_HAS_LANGUAGE_DE], 'regionalSettingId')
            .removeSeries(RegionalSetting, [self.REGIONAL_SETTING_1, self.REGIONAL_SETTING_2, self.REGIONAL_SETTING_NO_DEPENDENCIES, self.REGIONAL_SETTING_TEST])
            .removeSeries(Language, [self.LANGUAGE_EN, self.LANGUAGE_DE, self.LANGUAGE_TEST])
            .removeSeries(Tag, [self.TAG_1, self.TAG_2, self.TAG_TEST], 'tag')
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
            .createSeries(Tag, [self.TAG_1, self.TAG_2], 'tag')
            .createSeries(Language, [self.LANGUAGE_EN, self.LANGUAGE_DE])
            .createSeries(RegionalSetting, [self.REGIONAL_SETTING_1, self.REGIONAL_SETTING_2, self.REGIONAL_SETTING_NO_DEPENDENCIES])
            .createSeries(RegionalSettingHasLanguage, [self.REGIONAL_SETTING_1_HAS_LANGUAGE_EN, self.REGIONAL_SETTING_1_HAS_LANGUAGE_DE, self.REGIONAL_SETTING_2_HAS_LANGUAGE_EN, self.REGIONAL_SETTING_2_HAS_LANGUAGE_DE], 'regionalSettingId')
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
            .removeSeries(PoolHasTag, [self.POOL_1_HAS_TAG_1, self.POOL_2_HAS_TAG_2], 'poolId')
            .removeSeries(PoolHasTenant, [self.POOL_1_HAS_TENANT_1, self.POOL_2_HAS_TENANT_2], 'poolId')
            .removeSeries(PoolHasQuestion, [self.POOL_1_HAS_QUESTION_1, self.POOL_2_HAS_QUESTION_2, self.POOL_1_HAS_QUESTION_3, self.POOL_1_HAS_QUESTION_4], 'poolId')
            .removeSeries(PoolHasMedia, [self.POOL_1_HAS_MEDIA_1, self.POOL_2_HAS_MEDIA_2], 'poolId')
            .remove(QuestionHasRelatedQuestion, self.QUESTION_1_HAS_RELATED_QUESTION_2, 'questionId')
            .removeSeries(QuestionHasRegionalSetting, [self.QUESTION_1_HAS_REGIONAL_SETTING_1, self.QUESTION_2_HAS_REGIONAL_SETTING_2, self.QUESTION_3_HAS_REGIONAL_SETTING_1, self.QUESTION_4_HAS_REGIONAL_SETTING_1], 'questionId')
            .removeSeries(QuestionHasTag, [self.QUESTION_1_HAS_TAG_1, self.QUESTION_2_HAS_TAG_2], 'questionId')
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
            .createSeries(QuestionHasTag, [self.QUESTION_1_HAS_TAG_1, self.QUESTION_2_HAS_TAG_2], 'questionId')
            .createSeries(QuestionHasRegionalSetting, [self.QUESTION_1_HAS_REGIONAL_SETTING_1, self.QUESTION_2_HAS_REGIONAL_SETTING_2, self.QUESTION_3_HAS_REGIONAL_SETTING_1, self.QUESTION_4_HAS_REGIONAL_SETTING_1], 'questionId')
            .create(QuestionHasRelatedQuestion, self.QUESTION_1_HAS_RELATED_QUESTION_2, 'questionId')
            .createSeries(PoolHasMedia, [self.POOL_1_HAS_MEDIA_1, self.POOL_2_HAS_MEDIA_2], 'poolId')
            .createSeries(PoolHasQuestion, [self.POOL_1_HAS_QUESTION_1, self.POOL_2_HAS_QUESTION_2, self.POOL_1_HAS_QUESTION_3, self.POOL_1_HAS_QUESTION_4], 'poolId')
            .createSeries(PoolHasTenant, [self.POOL_1_HAS_TENANT_1, self.POOL_2_HAS_TENANT_2], 'poolId')
            .createSeries(PoolHasTag, [self.POOL_1_HAS_TAG_1, self.POOL_2_HAS_TAG_2], 'poolId')
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
            .removeSeries(QuestionTranslation, [self.QUESTION_TRANSLATION_TEST, self.QUESTION_TRANSLATION_NO_DEPENDENCIES, 
                self.QUESTION_1_TRANSLATION_EN, self.QUESTION_1_TRANSLATION_DE,
                self.QUESTION_2_TRANSLATION_EN, self.QUESTION_2_TRANSLATION_DE,
                self.QUESTION_3_TRANSLATION_EN, self.QUESTION_3_TRANSLATION_DE,
                self.QUESTION_4_TRANSLATION_EN, self.QUESTION_4_TRANSLATION_DE
            ], 'questionId')
            .removeSeries(Question, [self.QUESTION_4, self.QUESTION_3, self.QUESTION_2, self.QUESTION_1, self.QUESTION_NO_DEPENDENCIES, self.QUESTION_TEST])
            .removeSeries(Pool, [self.POOL_2, self.POOL_1, self.POOL_NO_DEPENDENCIES])
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
            .createSeries(Pool, [self.POOL_1, self.POOL_2, self.POOL_NO_DEPENDENCIES])
            .createSeries(Question, [self.QUESTION_1, self.QUESTION_2, self.QUESTION_3, self.QUESTION_4, self.QUESTION_NO_DEPENDENCIES])
            .createSeries(QuestionTranslation, [self.QUESTION_TRANSLATION_NO_DEPENDENCIES, 
                self.QUESTION_1_TRANSLATION_EN, self.QUESTION_1_TRANSLATION_DE, 
                self.QUESTION_2_TRANSLATION_EN, self.QUESTION_2_TRANSLATION_DE,
                self.QUESTION_3_TRANSLATION_EN, self.QUESTION_3_TRANSLATION_DE,
                self.QUESTION_4_TRANSLATION_EN, self.QUESTION_4_TRANSLATION_DE
            ])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

};
