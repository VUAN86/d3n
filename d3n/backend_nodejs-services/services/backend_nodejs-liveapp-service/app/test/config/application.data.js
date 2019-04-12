var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Application = RdbmsService.Models.Application.Application;
var ApplicationCharacter = RdbmsService.Models.Application.ApplicationCharacter;
var ApplicationAnimation = RdbmsService.Models.Application.ApplicationAnimation;
var ApplicationGameValidationResult = RdbmsService.Models.Application.ApplicationGameValidationResult;
var ApplicationHasApplicationAnimation = RdbmsService.Models.Application.ApplicationHasApplicationAnimation;
var ApplicationHasApplicationCharacter = RdbmsService.Models.Application.ApplicationHasApplicationCharacter;
var ApplicationHasPaymentType = RdbmsService.Models.Application.ApplicationHasPaymentType;
var ApplicationHasGame = RdbmsService.Models.Application.ApplicationHasGame;
var ApplicationHasTombola = RdbmsService.Models.Application.ApplicationHasTombola;
var ApplicationHasPromocodeCampaign = RdbmsService.Models.Application.ApplicationHasPromocodeCampaign;
var ApplicationHasGameModule = RdbmsService.Models.Application.ApplicationHasGameModule;
var ApplicationHasRegionalSetting = RdbmsService.Models.Application.ApplicationHasRegionalSetting;

module.exports = {

    APPLICATION_1: {
        id: DataIds.APPLICATION_1_ID,
        tenantId: DataIds.TENANT_1_ID,
        configuration: {
            menu_color: { appArea: '121212' },
            menu_picture:  { appArea: '12345678qwerty.jpeg'},
            referral_tearing: { 10: { bonusPoints: 1, credits: 1 } },
            boxStype: 'character',
            moneyGames: false,
            minimumEntryFee: 100,
            maximumEntryFee: 1000,
            gamblingComponent: false,
            paymentNeeded: true,
            onlyAnonymous: true,
            minimumGameLevel: 3,
            pushNotification: true,
            internationalGames: true,
            firstTimeSignUpBonusPoints: 10,
            firstTimeSignUpCredits: 5,
            referralBonusPoints: 30,
            referralCredits: 10,
            referalProgram: {
                bonuspoints: 10,
                credits: 5,
                tiering: [{
                    quantityMin: 1,
                    credits: 10,
                    bonuspoints: 5
                }]
            },
            bonusPointsForSharing: 0,
            bonusPointsForRating: 0,
            applicationLoadingScreenMediaId: '',
            firstRunMediaIds: ['1','2','3','4'],
            termsAndConditions: '',
            privacyStatement: '',
            gameRules: '',
            applicationIconMediaId: '',
            backgroundColor: 'white',
            backgroundMediaId: '',
            loadingPageMediaId: '',
            logoMediaId: '',
            fyber: {
                clientSecurityToken: 'a',
                rewardHandlingSecurityToken: 'b',
                appId: 'c'
            },
            theming: {
                autoQuestionSumbit: true
            }
        },
        deployment: {
            android: {
                name: 'Android deployment',
                serviceVersion: 'v.0.0.1',
                packageName: 'org.f4m.application1',
                usesFeatureString: 'camera',
                versionCode: '1',
                packageSize: 1234,
                targetPlattform: 'Android',
                targetPublisher: 'unknown',
                targetMarket: 'google play',
                typeOfRelease: 'Alpha',
                targetFTPServer: 'host',
                targetFTPUser: 'user',
                targetFTPPassword: 'qwerty',
                targetFTPKey: '1234',
                targetFTPProtocol: 'ftp',
                targetFTPAuthType: 'anonymous'
            },
            ios: {
                name: 'iOS deployment',
                serviceVersion: 'v.0.0.1',
                packageName: 'org.f4m.application1',
                usesFeatureString: 'camera',
                versionCode: '1',
                packageSize: 1234,
                targetPlattform: 'iOS',
                targetPublisher: 'unknown',
                targetMarket: 'iTunes',
                typeOfRelease: 'Alpha',
                targetFTPServer: 'host',
                targetFTPUser: 'user',
                targetFTPPassword: 'qwerty',
                targetFTPKey: '1234',
                targetFTPProtocol: 'ftp',
                targetFTPAuthType: 'anonymous'
            },
            web: null
        },
        title: 'Application one title',
        status: Application.constants().STATUS_INACTIVE,
        description: 'Application one description',
        releaseDate: DateUtils.isoNow(),
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        activeAnimationsIds: [DataIds.APPLICATION_ANIMATION_1_ID],
        activeCharactersIds: [DataIds.APPLICATION_CHARACTER_1_ID],
        gameModulesIds: [DataIds.GAME_MODULE_1_ID],
        gamesIds: [DataIds.GAME_1_ID],
        paymentTypesIds: [DataIds.PAYMENT_TYPE_1_ID],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        versionInfo: '1.1'
    },
    APPLICATION_2: {
        id: DataIds.APPLICATION_2_ID,
        tenantId: DataIds.TENANT_1_ID,
        configuration: null,
        title: 'Application two title',
        status: Application.constants().STATUS_INACTIVE,
        description: 'Application two description',
        releaseDate: DateUtils.isoNow(),
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },
    APPLICATION_TEST: {
        id: DataIds.APPLICATION_TEST_ID,
        configuration: {
            menu_color: { appArea: '121212' },
            menu_picture: { appArea: '12345678qwerty.jpeg' },
            referral_tearing: { 10: { bonusPoints: 2, credits: 2 } },
            fyber: {
                clientSecurityToken: 'a',
                rewardHandlingSecurityToken: 'b',
                appId: 'c'
            },
            theming: {
                autoQuestionSumbit: false
            }
        },
        deployment: {
            android: {
                name: 'Android deployment',
                serviceVersion: 'v.0.0.2',
                packageName: 'org.f4m.application2',
                usesFeatureString: 'camera',
                versionCode: '1',
                packageSize: 1234,
                targetPlattform: 'Android',
                targetPublisher: 'unknown',
                targetMarket: 'google play',
                typeOfRelease: 'Alpha',
                targetFTPServer: 'host',
                targetFTPUser: 'user',
                targetFTPPassword: 'qwerty',
                targetFTPKey: '1234',
                targetFTPProtocol: 'ftp',
                targetFTPAuthType: 'anonymous'
            },
            ios: {
                name: 'iOS deployment',
                serviceVersion: 'v.0.0.2',
                packageName: 'org.f4m.application2',
                usesFeatureString: 'camera',
                versionCode: '1',
                packageSize: 1234,
                targetPlattform: 'iOS',
                targetPublisher: 'unknown',
                targetMarket: 'iTunes',
                typeOfRelease: 'Alpha',
                targetFTPServer: 'host',
                targetFTPUser: 'user',
                targetFTPPassword: 'qwerty',
                targetFTPKey: '1234',
                targetFTPProtocol: 'ftp',
                targetFTPAuthType: 'anonymous'
            },
            web: null
        },
        title: 'Application test title',
        status: Application.constants().STATUS_INACTIVE,
        description: 'Application teste description',
        releaseDate: DateUtils.isoNow(),
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow(),
        activeAnimationsIds: [DataIds.APPLICATION_ANIMATION_1_ID],
        activeCharactersIds: [DataIds.APPLICATION_CHARACTER_1_ID],
        gameModulesIds: [DataIds.GAME_MODULE_1_ID],
        gamesIds: [DataIds.GAME_1_ID],
        paymentTypesIds: [DataIds.PAYMENT_TYPE_1_ID],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID]
    },
    APPLICATION_INACTIVE: {
        id: DataIds.APPLICATION_INACTIVE_ID,
        tenantId: DataIds.TENANT_2_ID,
        configuration: null,
        title: 'Application inactive title',
        status: Application.constants().STATUS_INACTIVE,
        description: 'Application inactive description',
        releaseDate: DateUtils.isoNow(),
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },
    APPLICATION_NO_DEPENDENCIES: {
        id: DataIds.APPLICATION_NO_DEPENDENCIES_ID,
        tenantId: DataIds.TENANT_2_ID,
        configuration: null,
        title: 'Application nodeps title',
        status: Application.constants().STATUS_INACTIVE,
        description: 'Application nodeps description',
        releaseDate: DateUtils.isoNow(),
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: DateUtils.isoNow()
    },

    APPLICATION_ANIMATION_1: {
        id: DataIds.APPLICATION_ANIMATION_1_ID,
        name: 'Animation one',
        description: 'Animation one descritpion',
        areaUsage: 'Area usage one'
    },
    APPLICATION_ANIMATION_2: {
        id:  DataIds.APPLICATION_ANIMATION_2_ID,
        name: 'Animation two',
        description: 'Animation two descritpion',
        areaUsage: 'Area usage two'
    },
    APPLICATION_ANIMATION_TEST: {
        id: DataIds.APPLICATION_ANIMATION_TEST_ID,
        name: 'Animation test',
        description: 'Animation test descritpion',
        areaUsage: 'Area usage test'
    },

    APPLICATION_CHARACTER_1: {
        id: DataIds.APPLICATION_CHARACTER_1_ID,
        name: 'Character one',
        description: 'Character one description'
    },
    APPLICATION_CHARACTER_2: {
        id: DataIds.APPLICATION_CHARACTER_2_ID,
        name: 'Character two',
        description: 'Character two description'
    },
    APPLICATION_CHARACTER_TEST: {
        id: DataIds.APPLICATION_CHARACTER_TEST_ID,
        name: 'Character test',
        description: 'Character test description'
    },

    APPLICATION_GAME_VALIDATION_RESULT_1: {
        id: DataIds.APPLICATION_GAME_VALIDATION_RESULT_1_ID,
        createDate: DateUtils.isoNow(),
        applicationId: DataIds.APPLICATION_1_ID,
        result: 'lost',
        warningMessage: 'Warning message'
    },
    APPLICATION_GAME_VALIDATION_RESULT_2: {
        id: DataIds.APPLICATION_GAME_VALIDATION_RESULT_2_ID,
        createDate: DateUtils.isoNow(),
        applicationId: DataIds.APPLICATION_2_ID,
        result: 'win',
        warningMessage: null
    },

    APPLICATION_1_HAS_APPLICATION_ANIMATION_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        applicationAnimationId: DataIds.APPLICATION_ANIMATION_1_ID
    },
    APPLICATION_2_HAS_APPLICATION_ANIMATION_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        applicationAnimationId: DataIds.APPLICATION_ANIMATION_2_ID
    },

    APPLICATION_1_HAS_APPLICATION_CHARACTER_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        applicationCharacterId: DataIds.APPLICATION_CHARACTER_1_ID
    },
    APPLICATION_2_HAS_APPLICATION_CHARACTER_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        applicationCharacterId: DataIds.APPLICATION_CHARACTER_2_ID
    },

    APPLICATION_1_HAS_PAYMENT_TYPE_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        paymentTypeId: DataIds.PAYMENT_TYPE_1_ID
    },
    APPLICATION_2_HAS_PAYMENT_TYPE_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        paymentTypeId: DataIds.PAYMENT_TYPE_2_ID
    },

    APPLICATION_1_HAS_GAME_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        gameId: DataIds.GAME_1_ID
    },
    APPLICATION_2_HAS_GAME_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        gameId: DataIds.GAME_2_ID
    },

    APPLICATION_1_HAS_REGIONAL_SETTING_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    APPLICATION_2_HAS_REGIONAL_SETTING_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    APPLICATION_1_HAS_GAME_MODULE_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        gameModuleId: DataIds.GAME_MODULE_1_ID
    },
    APPLICATION_2_HAS_GAME_MODULE_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        gameModuleId: DataIds.GAME_MODULE_2_ID
    },

    APPLICATION_1_HAS_TOMBOLA_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        tombolaId: DataIds.TOMBOLA_1_ID
    },
    APPLICATION_2_HAS_TOMBOLA_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        tombolaId: DataIds.TOMBOLA_2_ID
    },

    APPLICATION_1_HAS_PROMOCODE_CAMPAIGN_1: {
        applicationId: DataIds.APPLICATION_1_ID,
        promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
    },
    APPLICATION_2_HAS_PROMOCODE_CAMPAIGN_2: {
        applicationId: DataIds.APPLICATION_2_ID,
        promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_2_ID
    },
    APPLICATION_2_HAS_PROMOCODE_CAMPAIGN_INACTIVE: {
        applicationId: DataIds.APPLICATION_2_ID,
        promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_INACTIVE_ID
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(ApplicationHasGameModule, [self.APPLICATION_1_HAS_GAME_MODULE_1, self.APPLICATION_2_HAS_GAME_MODULE_2], 'applicationId')
            .removeSeries(ApplicationHasApplicationAnimation, [self.APPLICATION_1_HAS_APPLICATION_ANIMATION_1, self.APPLICATION_2_HAS_APPLICATION_ANIMATION_2], 'applicationId')
            .removeSeries(ApplicationHasApplicationCharacter, [self.APPLICATION_1_HAS_APPLICATION_CHARACTER_1, self.APPLICATION_2_HAS_APPLICATION_CHARACTER_2], 'applicationId')
            .removeSeries(ApplicationHasPaymentType, [self.APPLICATION_1_HAS_PAYMENT_TYPE_1, self.APPLICATION_2_HAS_PAYMENT_TYPE_2], 'applicationId')
            .removeSeries(ApplicationHasGame, [self.APPLICATION_1_HAS_GAME_1, self.APPLICATION_2_HAS_GAME_2], 'applicationId')
            .removeSeries(ApplicationHasRegionalSetting, [self.APPLICATION_1_HAS_REGIONAL_SETTING_1, self.APPLICATION_2_HAS_REGIONAL_SETTING_2], 'applicationId')
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
            .createSeries(ApplicationHasRegionalSetting, [self.APPLICATION_1_HAS_REGIONAL_SETTING_1, self.APPLICATION_2_HAS_REGIONAL_SETTING_2], 'applicationId')
            .createSeries(ApplicationHasGame, [self.APPLICATION_1_HAS_GAME_1, self.APPLICATION_2_HAS_GAME_2], 'applicationId')
            .createSeries(ApplicationHasPaymentType, [self.APPLICATION_1_HAS_PAYMENT_TYPE_1, self.APPLICATION_2_HAS_PAYMENT_TYPE_2], 'applicationId')
            .createSeries(ApplicationHasApplicationCharacter, [self.APPLICATION_1_HAS_APPLICATION_CHARACTER_1, self.APPLICATION_2_HAS_APPLICATION_CHARACTER_2], 'applicationId')
            .createSeries(ApplicationHasApplicationAnimation, [self.APPLICATION_1_HAS_APPLICATION_ANIMATION_1, self.APPLICATION_2_HAS_APPLICATION_ANIMATION_2], 'applicationId')
            .createSeries(ApplicationHasGameModule, [self.APPLICATION_1_HAS_GAME_MODULE_1, self.APPLICATION_2_HAS_GAME_MODULE_2], 'applicationId')
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
            .removeSeries(ApplicationGameValidationResult, [self.APPLICATION_GAME_VALIDATION_RESULT_1, self.APPLICATION_GAME_VALIDATION_RESULT_2])
            .removeSeries(Application, [self.APPLICATION_1, self.APPLICATION_2, self.APPLICATION_NO_DEPENDENCIES, self.APPLICATION_INACTIVE, self.APPLICATION_TEST])
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
            .createSeries(Application, [self.APPLICATION_1, self.APPLICATION_2, self.APPLICATION_NO_DEPENDENCIES, self.APPLICATION_INACTIVE])
            .createSeries(ApplicationGameValidationResult, [self.APPLICATION_GAME_VALIDATION_RESULT_1, self.APPLICATION_GAME_VALIDATION_RESULT_2])
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
            .removeSeries(ApplicationCharacter, [self.APPLICATION_CHARACTER_1, self.APPLICATION_CHARACTER_2])
            .removeSeries(ApplicationAnimation, [self.APPLICATION_ANIMATION_1, self.APPLICATION_ANIMATION_2])
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
            .createSeries(ApplicationAnimation, [self.APPLICATION_ANIMATION_1, self.APPLICATION_ANIMATION_2])
            .createSeries(ApplicationCharacter, [self.APPLICATION_CHARACTER_1, self.APPLICATION_CHARACTER_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
