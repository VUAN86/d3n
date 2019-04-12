var _ = require('lodash');

var Errors = {
    AuthApi: {
        AlreadyRegistered: 'ERR_AUTH_ALREADY_REGISTERED',
    },
    QuestionApi: {
        ValidationFailed: 'ERR_VALIDATION_FAILED',
        InsufficientRights: 'ERR_INSUFFICIENT_RIGHTS',
        ProviderTokenNotValid: 'ERR_TOKEN_NOT_VALID',
        FunctionNotImplemented: 'ERR_FUNCTION_NOT_IMPLEMENTED',
        ForeignKeyConstraintViolation: 'ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION',
        UniqueKeyConstraintViolation: 'ERR_UNIQUE_KEY_CONSTRAINT_VIOLATION',
        FatalError: 'ERR_FATAL_ERROR',
        EntityContainBadWords: 'ERR_ENTITY_CONTAIN_BAD_WORDS',
        NoInstanceAvailable: 'ERR_NO_INSTANCE_AVAILABLE',
    },
    PoolApi: {
        PoolHierarchyInconsistent: 'ERR_POOL_HIERARCHY_INCONSISTENT',
    },
    WorkorderApi: {
        WorkorderHasNoPool: 'ERR_WORKORDER_HAS_NO_POOL',
    },
    ApplicationApi: {
        AppIsDeployed: 'ERR_APPLICATION_IS_DEPLOYED',
        AppIsActivated: 'ERR_APPLICATION_IS_ACTIVATED',
        AppIsDeactivated: 'ERR_APPLICATION_IS_DEACTIVATED',
        AppIsArchived: 'ERR_APPLICATION_IS_ARCHIVED',
        AppNameNotUniquePerTenant: 'ERR_APPLICATION_NAME_NOT_UNIQUE_PER_TENANT',
        AppHasNoPaymentType: 'ERR_APPLICATION_HAS_NO_PAYMENT_TYPE'
    },
    BillingApi: {
        NoBillSelected: 'ERR_NO_BILL_SELECTED',
        BillNotApproved: 'ERR_BILL_NOT_APPROVED',
    },
    GameApi: {
        GameIsDeployed: 'ERR_GAME_IS_DEPLOYED',
        GameIsActivated: 'ERR_GAME_IS_ACTIVATED',
        GameIsDeactivated: 'ERR_GAME_IS_DEACTIVATED',
        GameIsArchived: 'ERR_GAME_IS_ARCHIVED',
        GameHasNoTemplate: 'ERR_GAME_HAS_NO_TEMPLATE',
        GameHasNoApplication: 'ERR_GAME_HAS_NO_APPLICATION',
        GameConfigurationInvalid: 'ERR_GAME_CONFIGURATION_IS_INVALID',
        GamePublishingNoGameFound: 'ERR_ENTRY_NOT_FOUND',
        GamePublishingNoApplicationFound: 'ERR_GAME_PUBLISHING_NO_APPLICATION_FOUND',
        GamePublishingNoPoolFound: 'ERR_GAME_PUBLISHING_NO_POOL_FOUND',
        GamePublishingNoRegionalSettingFound: 'ERR_GAME_PUBLISHING_NO_REGIONAL_SETTING_FOUND',
        GamePublishingNoLanguageFound: 'ERR_GAME_PUBLISHING_NO_LANGUAGE_FOUND',
        GamePublishingNoQuestionTemplateFound: 'ERR_GAME_PUBLISHING_NO_QUESTION_TEMPLATE_FOUND',
        GamePublishingNoQuestionTranslationPublished: 'ERR_GAME_PUBLISHING_NO_QUESTION_TRANSLATION_PUBLISHED',
        GamePublishingNoAdvertisementFound: 'ERR_GAME_PUBLISHING_NO_ADVERTISEMENT_FOUND',
        GamePublishingNoVoucherFound: 'ERR_GAME_PUBLISHING_NO_VOUCHER_FOUND',
        GamePublishingNoWinningComponentFound: 'ERR_GAME_PUBLISHING_NO_WINNING_COMPONENT_FOUND',
        GamePublishingNoEntryFeeAmount: 'ERR_GAME_PUBLISHING_NO_ENTRY_FEE_AMOUNT',
        GamePublishingInvalidAmountOfQuestions: 'ERR_GAME_PUBLISHING_INVALID_AMOUNT_OF_QUESTIONS',
        GamePublishingInvalidComplexityStructure: 'ERR_GAME_PUBLISHING_INVALID_COMPLEXITY_STRUCTURE',
        GamePublishingInvalidJokerConfiguration: 'ERR_GAME_PUBLISHING_INVALID_JOKER_CONFIGURATION',
        GamePublishingNotEnoughMoney: 'ERR_GAME_PUBLISHING_NOT_ENOUGHT_MONEY',
    },
    AdvertisementApi: {
        AdvertisementIsDeployed: 'ERR_ADVERTISEMENT_IS_DEPLOYED',
        AdvertisementIsActivated: 'ERR_ADVERTISEMENT_IS_ACTIVATED',
        AdvertisementIsDeactivated: 'ERR_ADVERTISEMENT_IS_DEACTIVATED',
        AdvertisementIsArchived: 'ERR_ADVERTISEMENT_IS_ARCHIVED',
        AdvertisementProviderIsActivated: 'ERR_ADVERTISEMENT_PROVIDER_IS_ACTIVATED',
        AdvertisementProviderIsDeactivated: 'ERR_ADVERTISEMENT_PROVIDER_IS_DEACTIVATED',
        AdvertisementProviderIsArchived: 'ERR_ADVERTISEMENT_PROVIDER_IS_ARCHIVED',
        ContentIsPublished: 'ERR_CONTENT_IS_PUBLISHED'
    },
    AchievementApi: {
        AchievementIsActivated: 'ERR_ACHIEVEMENT_IS_ACTIVATED',
        AchievementIsDeactivated: 'ERR_ACHIEVEMENT_IS_DEACTIVATED',
        AchievementIsArchived: 'ERR_ACHIEVEMENT_IS_ARCHIVED',
        BadgeIsActivated: 'ERR_BADGE_IS_ACTIVATED',
        BadgeIsDeactivated: 'ERR_BADGE_IS_DEACTIVATED',
        BadgeIsArchived: 'ERR_BADGE_IS_ARCHIVED',
        BadgesAreNotAvailable: 'ERR_BADGE_ARE_NOT_AVAILABLE',
        ContentIsPublished: 'ERR_CONTENT_IS_PUBLISHED'
    },
    VoucherApi: {
        VoucherIsDeployed: 'ERR_VOUCHER_IS_DEPLOYED',
        VoucherIsActivated: 'ERR_VOUCHER_IS_ACTIVATED',
        VoucherIsDeactivated: 'ERR_VOUCHER_IS_DEACTIVATED',
        VoucherIsArchived: 'ERR_VOUCHER_IS_ARCHIVED',
        VoucherProviderIsActivated: 'ERR_VOUCHER_PROVIDER_IS_ACTIVATED',
        VoucherProviderIsDeactivated: 'ERR_VOUCHER_PROVIDER_IS_DEACTIVATED',
        VoucherProviderIsArchived: 'ERR_VOUCHER_PROVIDER_IS_ARCHIVED',
        VoucherCodesAreNotAvailable: 'ERR_VOUCHER_CODES_ARE_NOT_AVAILABLE'
    },
    WinningApi: {
        WinningComponentIsDeployed: 'ERR_WINNING_COMPONENT_IS_DEPLOYED',
        WinningComponentIsActivated: 'ERR_WINNING_COMPONENT_IS_ACTIVATED',
        WinningComponentIsDeactivated: 'ERR_WINNING_COMPONENT_IS_DEACTIVATED',
        WinningComponentIsArchived: 'ERR_WINNING_COMPONENT_IS_ARCHIVED',
    },
    ProfileApi: {
        ValidationFailed: 'ERR_VALIDATION_FAILED',
        NotVerified: 'ERR_AUTH_NO_VERIFIED',
        AlreadyApplied: 'ERR_ALREADY_APPLIED',
        AlreadyApproved: 'ERR_ALREADY_APPROVED',
        AlreadyValidated: 'ERR_ALREADY_VALIDATED',
        InconsistencyDetected: 'ERR_INCONSISTENCY_DETECTED',
        PrivateCommunityRequestCreated: 'ERR_PRIVATE_COMMUNITY_REQUEST_CREATED',
    },
    TombolaApi: {
        ValidationFailed: 'ERR_VALIDATION_FAILED',
        TombolaIsActivated: 'ERR_TOMBOLA_IS_ACTIVATED',
        TombolaIsDeactivated: 'ERR_TOMBOLA_IS_DEACTIVATED',
        TombolaIsOpenCheckout: 'ERR_TOMBOLA_IS_OPEN_CHECKOUT',
        TombolaUserVoucherReserveFailed: 'ERR_TOMBOLA_USER_VOUCHER_RESERVE_FAILED',
        TombolaUserVoucherReleaseFailed: 'ERR_TOMBOLA_USER_VOUCHER_RELEASE_FAILED',
        TombolaIsArchived: 'ERR_TOMBOLA_IS_ARCHIVED'
    },
    PromocodeApi: {
        ValidationFailed: 'ERR_VALIDATION_FAILED',
        AlreadyExist: 'ERR_ENTRY_ALREADY_EXISTS',
        PromocodeCampaignIsActivated: 'ERR_PROMOCODE_CAMPAIGN_IS_ACTIVATED',
        PromocodeCampaignIsDeactivated: 'ERR_PROMOCODE_CAMPAIGN_IS_DEACTIVATED',
        PromocodeCampaignIsArchived: 'ERR_PROMOCODE_CAMPAIGN_IS_ARCHIVED',
        PromocodeCampaignContainsPromocode: 'ERR_PROMOCODE_CAMPAIGN_CONTAINS_PROMOCODE',
        PromocodeCampaignCodesAreNotAvailable: 'ERR_PROMOCODE_CAMPAIGN_CODES_ARE_NOT_AVAILABLE',
}
};

var Database = require('nodejs-database').getInstance();
Errors = _.assign(Errors, Database.Errors);

/**
 * Checks if error is not part of application errors
 * @param {Object} err Error (string or javascript error object)
 * @returns
 */
Errors.isInstance = function (err) {
    function getErrors(api) {
        var result = [];
        _.forOwn(api, function (value, key) {
            if (_.isObject(value)) {
                result = _.union(result, getErrors(value));
            } else {
                result.push(value);
            }
        });
        return result;
    }

    // Populate application errors
    var errors = getErrors(Errors);

    // Check if error is string or javascript error object
    var error = err;
    if (_.isObject(err)) {
        if (_.has(err, 'message') && _.isString(err.message)) {
            error = err.message;
        } else {
            return false;
        }
    }

    // Check if error is not listed in app defined errors
    if (errors.length === _.union(errors, [error]).length) {
        return true;
    }
    return false;
};

module.exports = Errors;