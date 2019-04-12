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
        name: 'Deutsch',
        tenantId: DataIds.TENANT_1_ID
    },
    LANGUAGE_RU: {
        id: DataIds.LANGUAGE_RU_ID,
        iso: 'ru',
        name: 'Russian',
        tenantId: DataIds.TENANT_1_ID
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
        defaultLanguageId: DataIds.LANGUAGE_RU_ID,
        languagesIds: [DataIds.LANGUAGE_DE_ID, DataIds.LANGUAGE_RU_ID]
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
    REGIONAL_SETTING_2_HAS_LANGUAGE_DE: {
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        languageId: DataIds.LANGUAGE_DE_ID,
    },
    REGIONAL_SETTING_2_HAS_LANGUAGE_RU: {
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        languageId: DataIds.LANGUAGE_RU_ID,
    },

    TAG_1: {
        tag: DataIds.TAG_1_TAG,
        isApproved: 0,
        approvedDate: null,
        tenantId: DataIds.TENANT_1_ID
    },
    TAG_2: {
        tag: DataIds.TAG_2_TAG,
        isApproved: 1,
        approvedDate: DateUtils.isoNow(),
        tenantId: DataIds.TENANT_1_ID
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
        exampleQuestionId: null,
        resolutionImageId: null,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        isInternational: 1,
        isTranslationNeeded: 0,
        rating: 1,
        source: 'source',
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        relatedQuestionsIds: null,
        poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_1_TRANSLATION_EN_ID, DataIds.QUESTION_1_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_1_TAG]
    },
    QUESTION_2: {
        id: DataIds.QUESTION_2_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 2,
        workorderId: DataIds.WORKORDER_2_ID,
        accessibleDate: DateUtils.isoNow(),
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_REVIEW,
        exampleQuestionId: DataIds.QUESTION_2_ID,
        resolutionImageId: DataIds.MEDIA_2_ID,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        isInternational: 1,
        isTranslationNeeded: 0,
        rating: 2,
        source: 'source',
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        relatedQuestionsIds: [DataIds.QUESTION_1_ID],
        poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_2_TRANSLATION_EN_ID, DataIds.QUESTION_2_TRANSLATION_DE_ID, DataIds.QUESTION_TRANSLATION_NO_DEPENDENCIES_ID],
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
        exampleQuestionId: null,
        resolutionImageId: null,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        isInternational: 0,
        isTranslationNeeded: 0,
        rating: 3,
        source: 'source',
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
        exampleQuestionId: null,
        resolutionImageId: DataIds.MEDIA_2_ID,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_2_ID,
        isInternational: 0,
        isTranslationNeeded: 0,
        rating: null,
        source: 'source',
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        relatedQuestionsIds: [DataIds.QUESTION_2_ID],
        poolsIds: [DataIds.POOL_2_ID],
        questionTranslationsIds: [DataIds.QUESTION_2_TRANSLATION_EN_ID, DataIds.QUESTION_2_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_2_TAG]
    },

    QUESTION_NO_POOL: {
        id: DataIds.QUESTION_NO_POOL,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        complexity: 1,
        workorderId: DataIds.WORKORDER_1_ID,
        accessibleDate: DateUtils.isoNow(),
        expirationDate: null,
        renewDate: null,
        status: Question.constants().STATUS_DRAFT,
        exampleQuestionId: null,
        resolutionImageId: null,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        isInternational: 0,
        isTranslationNeeded: 0,
        rating: 1,
        source: 'source',
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        relatedQuestionsIds: null,
        poolsIds: [],
        questionTranslationsIds: [DataIds.QUESTION_1_TRANSLATION_EN_ID, DataIds.QUESTION_1_TRANSLATION_DE_ID],
        tags: [DataIds.TAG_1_TAG]
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

    QUESTION_1_TRANSLATION_EN: {
        id: DataIds.QUESTION_1_TRANSLATION_EN_ID,
        name: 'Question 1 Translation English',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        language: {
            id: DataIds.LANGUAGE_EN_ID,
            iso: 'en',
            name: 'English'
        },
        questionId: DataIds.QUESTION_1_ID,
        question: {
            id: DataIds.QUESTION_1_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID
        },
        workorderId: DataIds.WORKORDER_1_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: true,
        resolutionText: null,
        explanation: 'test1EN',
        hint: 'test1EN',
        content: [
            {
                countdown: 12,
                structure: {},
                mappings: [
                    {
                        value: 'text 1'
                    },
                    {
                        value: '1111111-1111-1111-1111-111111111111.jpg'
                    },
                    {
                        handler: 'SingleChoiceAnswerH?NDLER',
                        answers: ['aaaa-ss-dd', 'aaaa-ss-de']
                    }
                ],
                medias: [
                    '1111111-1111-1111-1111-111111111111.jpg'
                ]
            },
            {
                countdown: 12,
                structure: {},
                mappings: [
                    {
                        value: 'text 2'
                    },
                    {
                        value: '2222222-2222-2222-2222-222222222222.jpg'
                    },
                    {
                        handler: 'SingleChoiceAnswerH?NDLER',
                        answers: ['1111111-1111-1111-1111-111111111111.jpg', '2222222-2222-2222-2222-222222222222.jpg']
                    }
                ],
                medias: [
                    '1111111-1111-1111-1111-111111111111.jpg',
                    '2222222-2222-2222-2222-222222222222.jpg'
                ]
            }]
    },
    QUESTION_1_TRANSLATION_DE: {
        id: DataIds.QUESTION_1_TRANSLATION_DE_ID,
        name: 'Question 1 Translation Deutsch',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_DE_ID,
        language: {
            id: DataIds.LANGUAGE_DE_ID,
            iso: 'de',
            name: 'Deutsch'
        },
        questionId: DataIds.QUESTION_1_ID,
        question: {
            id: DataIds.QUESTION_1_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID
        },
        workorderId: DataIds.WORKORDER_1_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: false,
        resolutionText: null,
        explanation: 'test1DE',
        hint: 'test1DE',
        content: [
            {
                countdown: 12,
                structure: {},
                mappings: [
                    {
                        value: 'text 1'
                    },
                    {
                        value: '1111111-1111-1111-1111-111111111111.jpg'
                    },
                    {
                        handler: 'SingleChoiceAnswerH?NDLER',
                        answers: ['aaaa-ss-dd', 'aaaa-ss-de']
                    }
                ],
                medias: [
                    '1111111-1111-1111-1111-111111111111.jpg'
                ]
            },
            {
                countdown: 12,
                structure: {},
                mappings: [
                    {
                        value: 'text 2'
                    },
                    {
                        value: '2222222-2222-2222-2222-222222222222.jpg'
                    },
                    {
                        handler: 'SingleChoiceAnswerH?NDLER',
                        answers: ['1111111-1111-1111-1111-111111111111.jpg', '2222222-2222-2222-2222-222222222222.jpg']
                    }
                ],
                medias: [
                    '1111111-1111-1111-1111-111111111111.jpg',
                    '2222222-2222-2222-2222-222222222222.jpg'
                ]
            }]
    },
    QUESTION_2_TRANSLATION_EN: {
        id: DataIds.QUESTION_2_TRANSLATION_EN_ID,
        name: 'Question 2 Translation English',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        language: {
            id: DataIds.LANGUAGE_EN_ID,
            iso: 'en',
            name: 'English'
        },
        questionId: DataIds.QUESTION_2_ID,
        question: {
            id: DataIds.QUESTION_2_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID
        },
        workorderId: DataIds.WORKORDER_2_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: false,
        resolutionText: 'test 2EN',
        explanation: 'test2EN',
        hint: 'test2EN',
        content: null
    },
    QUESTION_2_TRANSLATION_DE: {
        id: DataIds.QUESTION_2_TRANSLATION_DE_ID,
        name: 'Question 2 Translation Deutsch',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_DE_ID,
        language: {
            id: DataIds.LANGUAGE_DE_ID,
            iso: 'de',
            name: 'Deutsch'
        },
        questionId: DataIds.QUESTION_2_ID,
        question: {
            id: DataIds.QUESTION_2_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID
        },
        workorderId: DataIds.WORKORDER_2_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: false,
        resolutionText: 'test 2DE',
        explanation: 'test2DE',
        hint: 'test2DE',
        content: null
    },
    QUESTION_TRANSLATION_NO_DEPENDENCIES: {
        id: DataIds.QUESTION_TRANSLATION_NO_DEPENDENCIES_ID,
        name: 'Question Translation No Dependencies',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        language: {
            id: DataIds.LANGUAGE_EN_ID,
            iso: 'en',
            name: 'English'
        },
        questionId: DataIds.QUESTION_2_ID,
        question: {
            id: DataIds.QUESTION_2_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID
        },
        workorderId: DataIds.WORKORDER_2_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: false,
        resolutionText: 'test NODEPS',
        explanation: 'testND',
        hint: 'testND',
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
        language: {
            id: DataIds.LANGUAGE_EN_ID,
            iso: 'en',
            name: 'English'
        },
        questionId: DataIds.QUESTION_2_ID,
        question: {
            id: DataIds.QUESTION_2_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID
        },
        workorderId: DataIds.WORKORDER_2_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: false,
        resolutionText: 'test TEST',
        explanation: 'testT',
        hint: 'testT',
        content: '[]'
    },
    QUESTION_TRANSLATION_PUBLISHED: {
        id: DataIds.QUESTION_TRANSLATION_TEST_ID,
        name: 'Question Translation Test',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        language: {
            id: DataIds.LANGUAGE_EN_ID,
            iso: 'en',
            name: 'English'
        },
        questionId: DataIds.QUESTION_2_ID,
        question: {
            id: DataIds.QUESTION_2_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID
        },
        workorderId: DataIds.WORKORDER_2_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: false,
        resolutionText: 'test TEST',
        explanation: 'testT',
        hint: 'testT',
        content: null,
        publishIndex: 0
    },
    QUESTION_TRANSLATION_NO_POOL: {
        id: DataIds.QUESTION_TRANSLATION_NO_POOL,
        name: 'Question 1 Translation English',
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        approverResourceId: null,
        approveDate: null,
        languageId: DataIds.LANGUAGE_EN_ID,
        language: {
            id: DataIds.LANGUAGE_EN_ID,
            iso: 'en',
            name: 'English'
        },
        questionId: DataIds.QUESTION_NO_POOL,
        question: {
            id: DataIds.QUESTION_NO_POOL,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID
        },
        workorderId: DataIds.WORKORDER_1_ID,
        status: QuestionTranslation.constants().STATUS_INACTIVE,
        isDefaultTranslation: false,
        resolutionText: null,
        explanation: 'test1EN',
        hint: 'test1EN',
        content: [{
            countdown: 12,
            structure: {},
            mappings: [
                {
                    Value: 'text 1'
                },
                {
                    Value: '8b25123e-6cc5-4e46-974d-c0e5d97e4d50'
                },
                {
                    answers: ['aaaa-ss-dd', 'aaaa-ss-de']
                }
            ],
            medias: [
                '8b25123e-6cc5-4e46-974d-c0e5d97e4d50',
                '8b25123e-6cc5-4e46-974d-c0e5d97e4d50'
            ]
        }]
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
        //minimumQuestionsPerLanguage: 1,
        //minimumQuestionsPerRegion: 1,
        subpoolsIds: [DataIds.POOL_2_ID],
        questionsIds: [DataIds.QUESTION_2_ID, DataIds.QUESTION_1_ID],
        //tenantsIds: [DataIds.TENANT_1_ID],
        mediasIds: [DataIds.MEDIA_1_ID, DataIds.MEDIA_2_ID],
        tags: [DataIds.TAG_1_TAG, DataIds.TAG_2_TAG]
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
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        subpoolsIds: [],
        questionsIds: [DataIds.QUESTION_1_ID, DataIds.QUESTION_2_ID],
        //tenantsIds: [DataIds.TENANT_2_ID],
        mediasIds: [DataIds.MEDIA_1_ID, DataIds.MEDIA_2_ID],
        tags: [DataIds.TAG_1_TAG, DataIds.TAG_2_TAG]
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
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        subpoolsIds: [],
        questionsIds: [],
        //tenantsIds: [],
        mediasIds: [],
        tags: []
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
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        subpoolsIds: [],
        questionsIds: [],
        //tenantsIds: [],
        mediasIds: [DataIds.MEDIA_2_ID],
        tags: [DataIds.TAG_1_TAG]
    },

    POOL_1_HAS_MEDIA_1: {
        poolId: DataIds.POOL_1_ID,
        mediaId: DataIds.MEDIA_1_ID
    },
    POOL_1_HAS_MEDIA_2: {
        poolId: DataIds.POOL_1_ID,
        mediaId: DataIds.MEDIA_2_ID
    },
    POOL_2_HAS_MEDIA_1: {
        poolId: DataIds.POOL_2_ID,
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
    POOL_1_HAS_QUESTION_2: {
        poolId: DataIds.POOL_1_ID,
        questionId: DataIds.QUESTION_2_ID,
    },
    POOL_2_HAS_QUESTION_1: {
        poolId: DataIds.POOL_2_ID,
        questionId: DataIds.QUESTION_1_ID,
    },
    POOL_2_HAS_QUESTION_2: {
        poolId: DataIds.POOL_2_ID,
        questionId: DataIds.QUESTION_2_ID,
    },

    POOL_1_HAS_TAG_1: {
        poolId: DataIds.POOL_1_ID,
        tagTag: DataIds.TAG_1_TAG
    },
    POOL_1_HAS_TAG_2: {
        poolId: DataIds.POOL_1_ID,
        tagTag: DataIds.TAG_2_TAG
    },
    POOL_2_HAS_TAG_1: {
        poolId: DataIds.POOL_2_ID,
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
    POOL_1_HAS_REGIONAL_SETTING_2: {
        poolId: DataIds.POOL_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },
    POOL_2_HAS_REGIONAL_SETTING_1: {
        poolId: DataIds.POOL_2_ID,
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
            .removeSeries(RegionalSettingHasLanguage, [self.REGIONAL_SETTING_1_HAS_LANGUAGE_EN, self.REGIONAL_SETTING_1_HAS_LANGUAGE_DE, self.REGIONAL_SETTING_2_HAS_LANGUAGE_DE, self.REGIONAL_SETTING_2_HAS_LANGUAGE_RU], 'regionalSettingId')
            .removeSeries(RegionalSetting, [self.REGIONAL_SETTING_1, self.REGIONAL_SETTING_2, self.REGIONAL_SETTING_NO_DEPENDENCIES, self.REGIONAL_SETTING_TEST])
            .removeSeries(Language, [self.LANGUAGE_EN, self.LANGUAGE_DE, self.LANGUAGE_RU, self.LANGUAGE_TEST])
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
            .createSeries(Language, [self.LANGUAGE_EN, self.LANGUAGE_DE, self.LANGUAGE_RU])
            .createSeries(RegionalSetting, [self.REGIONAL_SETTING_1, self.REGIONAL_SETTING_2, self.REGIONAL_SETTING_NO_DEPENDENCIES])
            .createSeries(RegionalSettingHasLanguage, [self.REGIONAL_SETTING_1_HAS_LANGUAGE_EN, self.REGIONAL_SETTING_1_HAS_LANGUAGE_DE, self.REGIONAL_SETTING_2_HAS_LANGUAGE_DE, self.REGIONAL_SETTING_2_HAS_LANGUAGE_RU], 'regionalSettingId')
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
            .removeSeries(PoolHasTag, [self.POOL_1_HAS_TAG_1, self.POOL_1_HAS_TAG_2, self.POOL_2_HAS_TAG_1, self.POOL_2_HAS_TAG_2], 'poolId')
            .removeSeries(PoolHasTenant, [self.POOL_1_HAS_TENANT_1, self.POOL_2_HAS_TENANT_2], 'poolId')
            .removeSeries(PoolHasQuestion, [self.POOL_1_HAS_QUESTION_1, self.POOL_1_HAS_QUESTION_2, self.POOL_2_HAS_QUESTION_1, self.POOL_2_HAS_QUESTION_2], 'poolId')
            .removeSeries(PoolHasMedia, [self.POOL_1_HAS_MEDIA_1, self.POOL_1_HAS_MEDIA_2, self.POOL_2_HAS_MEDIA_1, self.POOL_2_HAS_MEDIA_2], 'poolId')
            .remove(QuestionHasRelatedQuestion, self.QUESTION_1_HAS_RELATED_QUESTION_2, 'questionId')
            .removeSeries(QuestionHasRegionalSetting, [self.QUESTION_1_HAS_REGIONAL_SETTING_1, self.QUESTION_2_HAS_REGIONAL_SETTING_2], 'questionId')
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
            .createSeries(QuestionHasRegionalSetting, [self.QUESTION_1_HAS_REGIONAL_SETTING_1, self.QUESTION_2_HAS_REGIONAL_SETTING_2], 'questionId')
            .create(QuestionHasRelatedQuestion, self.QUESTION_1_HAS_RELATED_QUESTION_2, 'questionId')
            .createSeries(PoolHasMedia, [self.POOL_1_HAS_MEDIA_1, self.POOL_1_HAS_MEDIA_2, self.POOL_2_HAS_MEDIA_1, self.POOL_2_HAS_MEDIA_2], 'poolId')
            .createSeries(PoolHasQuestion, [self.POOL_1_HAS_QUESTION_1, self.POOL_1_HAS_QUESTION_2, self.POOL_2_HAS_QUESTION_1, self.POOL_2_HAS_QUESTION_2], 'poolId')
            .createSeries(PoolHasTenant, [self.POOL_1_HAS_TENANT_1, self.POOL_2_HAS_TENANT_2], 'poolId')
            .createSeries(PoolHasTag, [self.POOL_1_HAS_TAG_1, self.POOL_1_HAS_TAG_2, self.POOL_2_HAS_TAG_1, self.POOL_2_HAS_TAG_2], 'poolId')
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
            .removeSeries(QuestionTranslation, [self.QUESTION_TRANSLATION_TEST, self.QUESTION_TRANSLATION_NO_DEPENDENCIES, self.QUESTION_1_TRANSLATION_EN, self.QUESTION_1_TRANSLATION_DE, self.QUESTION_2_TRANSLATION_EN, self.QUESTION_2_TRANSLATION_DE], 'questionId')
            .removeSeries(Question, [self.QUESTION_2, self.QUESTION_1, self.QUESTION_NO_DEPENDENCIES, self.QUESTION_TEST])
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
            .createSeries(Question, [self.QUESTION_1, self.QUESTION_2, self.QUESTION_NO_DEPENDENCIES])
            .createSeries(QuestionTranslation, [self.QUESTION_TRANSLATION_NO_DEPENDENCIES, self.QUESTION_1_TRANSLATION_EN, self.QUESTION_1_TRANSLATION_DE, self.QUESTION_2_TRANSLATION_EN, self.QUESTION_2_TRANSLATION_DE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

};
