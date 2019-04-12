var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;

var Question = RdbmsService.Models.Question.Question;
var Pool = RdbmsService.Models.Question.Pool;
var PoolHasMedia = RdbmsService.Models.Question.PoolHasMedia;
var PoolHasTenant = RdbmsService.Models.Question.PoolHasTenant;
var PoolHasQuestion = RdbmsService.Models.Question.PoolHasQuestion;
var Language = RdbmsService.Models.Question.Language;
var RegionalSetting = RdbmsService.Models.Question.RegionalSetting;

module.exports = {
    LANGUAGE_EN: {
        id: DataIds.LANGUAGE_EN_ID,
        iso: 'en',
        name: 'English'
    },

    QUESTION_1: {
        id: DataIds.QUESTION_1_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        complexity: 1,
        workorderId: null,
        accessibleDate: _.now(),
        expirationDate: null,
        renewDate: null,
        isDeleted: 0,
        status: 'draft',
        isInternational: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        rating: 1,
        deploymentStatus: 'unpublished',
        deploymentDate: null,
        source: 'source',
        countdown: 123,
        regionalSettingsIds: [],
        relatedQuestionsIds: null,
        poolsIds: [DataIds.POOL_1_ID],
        questionTranslationsIds: [],
        tags: []
    },
    QUESTION_2: {
        id: DataIds.QUESTION_2_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        complexity: 2,
        workorderId: null,
        accessibleDate: _.now(),
        expirationDate: null,
        renewDate: null,
        isDeleted: 0,
        status: 'review',
        isInternational: 1,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        rating: 2,
        deploymentStatus: 'unpublished',
        deploymentDate: null,
        source: 'source',
        countdown: 234,
        regionalSettingsIds: [],
        relatedQuestionsIds: [],
        poolsIds: [],
        questionTranslationsIds: [],
        tags: []
    },
    QUESTION_NO_DEPENDENCIES: {
        id: DataIds.QUESTION_NO_DEPENDENCIES_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        complexity: 3,
        workorderId: null,
        accessibleDate: _.now(),
        expirationDate: null,
        renewDate: null,
        isDeleted: 0,
        status: 'approved',
        isInternational: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        rating: 3,
        deploymentStatus: 'unpublished',
        deploymentDate: null,
        source: 'source',
        countdown: 123,
        regionalSettingsIds: [],
        relatedQuestionsIds: [],
        poolsIds: [DataIds.POOL_1_ID],
        questionTranslationsIds: [],
        tags: []
    },
    QUESTION_TEST: {
        id: DataIds.QUESTION_TEST_ID,
        questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        complexity: 3,
        workorderId: null,
        accessibleDate: _.now(),
        expirationDate: null,
        renewDate: null,
        isDeleted: 0,
        status: 'draft',
        isInternational: 0,
        primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        rating: null,
        deploymentStatus: 'unpublished',
        deploymentDate: null,
        source: 'source',
        countdown: 234,
        regionalSettingsIds: [],
        relatedQuestionsIds: [],
        poolsIds: [DataIds.POOL_1_ID],
        questionTranslationsIds: [],
        tags: []
    },

    POOL_1: {
        id: DataIds.POOL_1_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        name: 'Pool 1',
        description: 'Pool 1 Description',
        parentPoolId: null,
        minimumQuestions: 1,
        //minimumQuestionsPerLanguage: 1,
        //minimumQuestionsPerRegion: 1,
        questionsIds: [],
        tenantsIds: [],
        mediasIds: [DataIds.MEDIA_1_ID],
    },
    POOL_2: {
        id: DataIds.POOL_2_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        name: 'Pool 2',
        description: 'Pool 2 Description',
        parentPoolId: DataIds.POOL_1_ID,
        minimumQuestions: 2,
        questionsIds: [],
        tenantsIds: [],
        mediasIds: [DataIds.MEDIA_2_ID],
    },
    POOL_3: {
        id: DataIds.POOL_3_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        name: 'Pool 3',
        description: 'Pool 3 Description',
        parentPoolId: null,
        minimumQuestions: 2,
        questionsIds: [],
        tenantsIds: [],
        mediasIds: [],
    },
    POOL_NO_DEPENDENCIES: {
        id: DataIds.POOL_NO_DEPENDENCIES_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        name: 'Pool 3',
        description: 'Pool 3 Description',
        parentPoolId: null,
        minimumQuestions: 2,
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        questionsIds: [],
        tenantsIds: [],
        mediasIds: [],
    },
    POOL_TEST: {
        id: DataIds.POOL_TEST_ID,
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now(),
        name: 'Pool Test',
        description: 'Pool Test Description',
        parentPoolId: null,
        minimumQuestions: 2,
        //minimumQuestionsPerLanguage: 2,
        //minimumQuestionsPerRegion: 2,
        questionsIds: [],
        tenantsIds: [],
        mediasIds: []
    },

    POOL_1_HAS_MEDIA_1: {
        poolId: DataIds.POOL_1_ID,
        mediaId: DataIds.MEDIA_1_ID
    },
    POOL_1_HAS_MEDIA_2: {
        poolId: DataIds.POOL_1_ID,
        mediaId: DataIds.MEDIA_2_ID
    },
    POOL_2_HAS_MEDIA_3: {
        poolId: DataIds.POOL_2_ID,
        mediaId: DataIds.MEDIA_3_ID
    },
    POOL_2_HAS_MEDIA_4: {
        poolId: DataIds.POOL_2_ID,
        mediaId: DataIds.MEDIA_4_ID
    },

    POOL_1_HAS_QUESTION_1: {
        poolId: DataIds.POOL_1_ID,
        questionId: DataIds.QUESTION_1_ID,
    },
    POOL_2_HAS_QUESTION_2: {
        poolId: DataIds.POOL_2_ID,
        questionId: DataIds.QUESTION_2_ID,
    },

    POOL_1_HAS_TENANT_1: {
        poolId: DataIds.POOL_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: PoolHasTenant.constants().ROLE_VIEW
    },

    POOL_2_HAS_TENANT_2: {
        poolId: DataIds.POOL_2_ID,
        tenantId: DataIds.TENANT_2_ID,
        role: PoolHasTenant.constants().ROLE_VIEW
    },

    POOL_3_HAS_TENANT_1: {
        poolId: DataIds.POOL_3_ID,
        tenantId: DataIds.TENANT_1_ID,
        role: PoolHasTenant.constants().ROLE_VIEW
    },

    REGIONAL_SETTING_1: {
        id: DataIds.REGIONAL_SETTING_1_ID,
        iso: 'AF',
        name: 'Regional Setting 1',
        status: 'active',
        defaultLanguageId: DataIds.LANGUAGE_EN_ID,
        languagesIds: []
    },

    REGIONAL_SETTING_1_HAS_LANGUAGE_EN: {
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
        languageId: DataIds.LANGUAGE_EN_ID,
    },

    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(RegionalSetting, [self.REGIONAL_SETTING_1])
            .removeSeries(Language, [self.LANGUAGE_EN])
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
            .createSeries(Language, [self.LANGUAGE_EN])
            .createSeries(RegionalSetting, [self.REGIONAL_SETTING_1])
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
            .removeSeries(PoolHasQuestion, [self.POOL_1_HAS_QUESTION_1, self.POOL_2_HAS_QUESTION_2], 'poolId')
            .removeSeries(PoolHasMedia, [self.POOL_1_HAS_MEDIA_1, self.POOL_1_HAS_MEDIA_2, self.POOL_2_HAS_MEDIA_3, self.POOL_2_HAS_MEDIA_4], 'poolId')
            .removeSeries(PoolHasTenant, [self.POOL_1_HAS_TENANT_1, self.POOL_2_HAS_TENANT_2, self.POOL_3_HAS_TENANT_1], 'poolId')
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
            .createSeries(PoolHasMedia, [self.POOL_1_HAS_MEDIA_1, self.POOL_1_HAS_MEDIA_2, self.POOL_2_HAS_MEDIA_3, self.POOL_2_HAS_MEDIA_4], 'poolId')
            .createSeries(PoolHasQuestion, [self.POOL_1_HAS_QUESTION_1, self.POOL_2_HAS_QUESTION_2], 'poolId')
            .createSeries(PoolHasTenant, [self.POOL_1_HAS_TENANT_1, self.POOL_2_HAS_TENANT_2, self.POOL_3_HAS_TENANT_1], 'poolId')
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
            .removeSeries(Question, [self.QUESTION_2, self.QUESTION_1, self.QUESTION_NO_DEPENDENCIES, self.QUESTION_TEST])
            .removeSeries(Pool, [self.POOL_1, self.POOL_2, self.POOL_3, self.POOL_NO_DEPENDENCIES])
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
            .createSeries(Pool, [self.POOL_1, self.POOL_2, self.POOL_3, self.POOL_NO_DEPENDENCIES])
            .createSeries(Question, [self.QUESTION_1, self.QUESTION_2, self.QUESTION_NO_DEPENDENCIES])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

};
