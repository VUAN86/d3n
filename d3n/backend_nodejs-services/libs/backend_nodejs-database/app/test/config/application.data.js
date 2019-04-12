var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('./../../../index.js').getInstance(Config);
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
var ApplicationHasGameModule = RdbmsService.Models.Application.ApplicationHasGameModule;
var ApplicationHasRegionalSetting = RdbmsService.Models.Application.ApplicationHasRegionalSetting;
var ApplicationHasTombola = RdbmsService.Models.Application.ApplicationHasTombola;
var ApplicationHasPromocodeCampaign = RdbmsService.Models.Application.ApplicationHasPromocodeCampaign;

module.exports = {

    APPLICATION_1: {
        $id: DataIds.APPLICATION_1_ID,
        $tenantId: DataIds.TENANT_1_ID,
        configuration: {
            menu_color: { appArea: '121212' },
            menu_picture:  { appArea: '12345678qwerty.jpeg'},
            referral_tearing: { 10: { bonusPoints: 1, credits: 1 } }
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
        isDeployed: 0,
        title: 'Application one title',
        status: 'inactive',
        description: 'Application one description',
        releaseDate: _.now(),
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now()
    },
    APPLICATION_2: {
        $id: DataIds.APPLICATION_2_ID,
        $tenantId: DataIds.TENANT_2_ID,
        configuration: null,
        isDeployed: 1,
        title: 'Application two title',
        status: 'active',
        description: 'Application two description',
        releaseDate: _.now(),
        creatorResourceId: DataIds.LOCAL_USER_ID,
        createDate: _.now()
    },

    APPLICATION_ANIMATION_1: {
        $id: DataIds.APPLICATION_ANIMATION_1_ID,
        name: 'Animation one',
        description: 'Animation one descritpion',
        areaUsage: 'Area usage one'
    },
    APPLICATION_ANIMATION_2: {
        $id:  DataIds.APPLICATION_ANIMATION_2_ID,
        name: 'Animation two',
        description: 'Animation two descritpion',
        areaUsage: 'Area usage two'
    },

    APPLICATION_CHARACTER_1: {
        $id: DataIds.APPLICATION_CHARACTER_1_ID,
        name: 'Character one',
        description: 'Character one description'
    },
    APPLICATION_CHARACTER_2: {
        $id: DataIds.APPLICATION_CHARACTER_2_ID,
        name: 'Character two',
        description: 'Character two description'
    },

    APPLICATION_GAME_VALIDATION_RESULT_1: {
        $id: DataIds.APPLICATION_GAME_VALIDATION_RESULT_1_ID,
        createDate: _.now(),
        $applicationId: DataIds.APPLICATION_1_ID,
        result: 'lost',
        warningMessage: 'Warning message'
    },
    APPLICATION_GAME_VALIDATION_RESULT_2: {
        $id: DataIds.APPLICATION_GAME_VALIDATION_RESULT_2_ID,
        createDate: _.now(),
        $applicationId: DataIds.APPLICATION_2_ID,
        result: 'win',
        warningMessage: null
    },

    APPLICATION_1_HAS_APPLICATION_ANIMATION_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $applicationAnimationId: DataIds.APPLICATION_ANIMATION_1_ID,
        status: ApplicationHasApplicationAnimation.constants().STATUS_ACTIVE
    },
    APPLICATION_2_HAS_APPLICATION_ANIMATION_2: {
        $applicationId: DataIds.APPLICATION_2_ID,
        $applicationAnimationId: DataIds.APPLICATION_ANIMATION_2_ID,
        status: ApplicationHasApplicationAnimation.constants().STATUS_INACTIVE
    },

    APPLICATION_1_HAS_APPLICATION_CHARACTER_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $applicationCharacterId: DataIds.APPLICATION_CHARACTER_1_ID,
        status: ApplicationHasApplicationCharacter.constants().STATUS_ACTIVE
    },
    APPLICATION_2_HAS_APPLICATION_CHARACTER_2: {
        $applicationId: DataIds.APPLICATION_2_ID,
        $applicationCharacterId: DataIds.APPLICATION_CHARACTER_2_ID,
        status: ApplicationHasApplicationCharacter.constants().STATUS_INACTIVE
    },

    APPLICATION_1_HAS_PAYMENT_TYPE_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $paymentTypeId: DataIds.PAYMENT_TYPE_1_ID
    },
    APPLICATION_2_HAS_PAYMENT_TYPE_2: {
        $applicationId: DataIds.APPLICATION_2_ID,
        $paymentTypeId: DataIds.PAYMENT_TYPE_2_ID
    },

    APPLICATION_1_HAS_GAME_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $gameId: DataIds.GAME_1_ID,
        status: ApplicationHasGame.constants().STATUS_ACTIVE
    },
    APPLICATION_2_HAS_GAME_2: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $gameId: DataIds.GAME_2_ID,
        status: ApplicationHasGame.constants().STATUS_INACTIVE
    },

    APPLICATION_1_HAS_REGIONAL_SETTING_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    APPLICATION_2_HAS_REGIONAL_SETTING_2: {
        $applicationId: DataIds.APPLICATION_2_ID,
        $regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    APPLICATION_1_HAS_GAME_MODULE_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $gameModuleId: DataIds.GAME_MODULE_1_ID,
        status: ApplicationHasGameModule.constants().STATUS_ACTIVE
    },
    APPLICATION_2_HAS_GAME_MODULE_2: {
        $applicationId: DataIds.APPLICATION_2_ID,
        $gameModuleId: DataIds.GAME_MODULE_2_ID,
        status: ApplicationHasGameModule.constants().STATUS_INACTIVE
    },

    APPLICATION_1_HAS_TOMBOLA_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $tombolaId: DataIds.TOMBOLA_1_ID
    },
    APPLICATION_2_HAS_TOMBOLA_2: {
        $applicationId: DataIds.APPLICATION_2_ID,
        $tombolaId: DataIds.TOMBOLA_2_ID
    },

    APPLICATION_1_HAS_PROMOCODE_CAMPAIGN_1: {
        $applicationId: DataIds.APPLICATION_1_ID,
        $promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_1_ID
    },
    APPLICATION_2_HAS_PROMOCODE_CAMPAIGN_2: {
        $applicationId: DataIds.APPLICATION_2_ID,
        $promocodeCampaignId: DataIds.PROMOCODE_CAMPAIGN_2_ID
    },

    loadManyToMany: function (testSet) {
        return testSet
            .createSeries(ApplicationHasRegionalSetting, [this.APPLICATION_1_HAS_REGIONAL_SETTING_1, this.APPLICATION_2_HAS_REGIONAL_SETTING_2])
            .createSeries(ApplicationHasGame, [this.APPLICATION_1_HAS_GAME_1, this.APPLICATION_2_HAS_GAME_2])
            .createSeries(ApplicationHasPaymentType, [this.APPLICATION_1_HAS_PAYMENT_TYPE_1, this.APPLICATION_2_HAS_PAYMENT_TYPE_2])
            .createSeries(ApplicationHasApplicationCharacter, [this.APPLICATION_1_HAS_APPLICATION_CHARACTER_1, this.APPLICATION_2_HAS_APPLICATION_CHARACTER_2])
            .createSeries(ApplicationHasApplicationAnimation, [this.APPLICATION_1_HAS_APPLICATION_ANIMATION_1, this.APPLICATION_2_HAS_APPLICATION_ANIMATION_2])
            .createSeries(ApplicationHasGameModule, [this.APPLICATION_1_HAS_GAME_MODULE_1, this.APPLICATION_2_HAS_GAME_MODULE_2])
            .createSeries(ApplicationHasTombola, [this.APPLICATION_1_HAS_TOMBOLA_1, this.APPLICATION_2_HAS_TOMBOLA_2])
            .createSeries(ApplicationHasPromocodeCampaign, [this.APPLICATION_1_HAS_PROMOCODE_CAMPAIGN_1, this.APPLICATION_2_HAS_PROMOCODE_CAMPAIGN_2])
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(Application, [this.APPLICATION_1, this.APPLICATION_2])
            .createSeries(ApplicationGameValidationResult, [this.APPLICATION_GAME_VALIDATION_RESULT_1, this.APPLICATION_GAME_VALIDATION_RESULT_2])
    },

    loadClassifiers: function (testSet) {
        return testSet
            .createSeries(ApplicationAnimation, [this.APPLICATION_ANIMATION_1, this.APPLICATION_ANIMATION_2])
            .createSeries(ApplicationCharacter, [this.APPLICATION_CHARACTER_1, this.APPLICATION_CHARACTER_2])
    }
};
