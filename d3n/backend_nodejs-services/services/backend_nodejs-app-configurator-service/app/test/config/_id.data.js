var uuid = require('node-uuid');

module.exports = {

    CLIENT_SESSION_ID: 'client-session-id',
    // Media
    MEDIA_1_ID: '1111111-1111-1111-1111-111111111111',
    MEDIA_2_ID: '2222222-2222-2222-2222-222222222222',
    MEDIA_TEST_ID: '9999999-9999-9999-9999-999999999999',

    // Profile
    LOCAL_USER_ID: '1111111-1111-1111-1111-111111111111',
    FACEBOOK_USER_ID: '2222222-2222-2222-2222-222222222222',
    GOOGLE_USER_ID: '3333333-3333-3333-3333-333333333333',
    TEST_USER_ID: '9999999-9999-9999-9999-999999999999',

    PROFILE_HAS_ROLE_1_ID: 1,
    PROFILE_HAS_ROLE_2_ID: 2,

    // QuestionTemplate
    QUESTION_TEMPLATE_NEW_ID: 1,
    QUESTION_TEMPLATE_APPROVED_ID: 2,
    QUESTION_TEMPLATE_NO_DEPENDENCIES_ID: 3,
    QUESTION_TEMPLATE_TEST_ID: 4,

    // Tenant
    TENANT_1_ID: 1,
    TENANT_2_ID: 2,
    TENANT_NO_DEPENDENCIES_ID: 3,
    TENANT_TEST_ID: 4,

    // TenantContract
    TENANT_CONTRACT_1_ID: 1,
    TENANT_CONTRACT_2_ID: 2,
    TENANT_CONTRACT_TEST_ID: 4,

    // TenantInvoice
    TENANT_INVOICE_1_ID: 1,
    TENANT_INVOICE_2_ID: 2,
    TENANT_INVOICE_TEST_ID: 4,

    // TenantInvoiceEndCustomer
    TENANT_INVOICE_END_CUSTOMER_TEST_ID: 4,

    // TenantAuditLog
    TENANT_AUDIT_LOG_1_ID: 1,
    TENANT_AUDIT_LOG_2_ID: 2,
    TENANT_AUDIT_LOG_TEST_ID: 4,

    // TenantContractAuditLog
    TENANT_CONTRACT_AUDIT_LOG_1_ID: 1,
    TENANT_CONTRACT_AUDIT_LOG_2_ID: 2,
    TENANT_CONTRACT_AUDIT_LOG_TEST_ID: 4,

    // TenantAdminAuditLog
    TENANT_ADMIN_AUDIT_LOG_1_ID: 1,
    TENANT_ADMIN_AUDIT_LOG_2_ID: 2,
    TENANT_ADMIN_AUDIT_LOG_TEST_ID: 4,

    // Pool
    POOL_1_ID: 1,
    POOL_2_ID: 2,
    POOL_NO_DEPENDENCIES_ID: 3,
    POOL_TEST_ID: 4,

    // Question
    QUESTION_1_ID: 1,
    QUESTION_2_ID: 2,
    QUESTION_3_ID: 3,
    QUESTION_4_ID: 4,
    QUESTION_NO_DEPENDENCIES_ID: 5,
    QUESTION_TEST_ID: 6,

    // QuestionRating
    QUESTION_RATING_1_ID: 1,
    QUESTION_RATING_2_ID: 2,
    QUESTION_RATING_3_ID: 3,
    QUESTION_RATING_4_ID: 4,

    // QuestionVoting
    QUESTION_VOTING_1_ID: 1,
    QUESTION_VOTING_2_ID: 2,
    QUESTION_VOTING_3_ID: 3,
    QUESTION_VOTING_4_ID: 4,

    // Tag
    TAG_1_TAG: 'AAAAAAA-1111-1111-1111-000000000000',
    TAG_2_TAG: 'BBBBBBB-2222-2222-2222-000000000000',
    TAG_TEST_TAG: 'FFFFFFF-9999-9999-9999-000000000000',

    // QuestionTranslation
    QUESTION_1_TRANSLATION_EN_ID: 1,
    QUESTION_1_TRANSLATION_DE_ID: 2,
    QUESTION_2_TRANSLATION_EN_ID: 3,
    QUESTION_2_TRANSLATION_DE_ID: 4,
    QUESTION_3_TRANSLATION_EN_ID: 5,
    QUESTION_3_TRANSLATION_DE_ID: 6,
    QUESTION_4_TRANSLATION_EN_ID: 7,
    QUESTION_4_TRANSLATION_DE_ID: 8,
    QUESTION_TRANSLATION_NO_DEPENDENCIES_ID: 9,
    QUESTION_TRANSLATION_TEST_ID: 10,

    // Language
    LANGUAGE_EN_ID: 1,
    LANGUAGE_DE_ID: 2,
    LANGUAGE_RU_ID: 3,
    LANGUAGE_TEST_ID: 4,

    // RegionalSetting
    REGIONAL_SETTING_1_ID: 1,
    REGIONAL_SETTING_2_ID: 2,
    REGIONAL_SETTING_NO_DEPENDENCIES_ID: 3,
    REGIONAL_SETTING_TEST_ID: 4,

    // WorkorderBillingModel
    WORKORDER_BILLING_MODEL_1_ID: 1,
    WORKORDER_BILLING_MODEL_2_ID: 2,
    WORKORDER_BILLING_MODEL_NO_DEPENDENCIES_ID: 3,
    WORKORDER_BILLING_MODEL_TEST_ID: 4,

    // Workorder
    WORKORDER_1_ID: 1,
    WORKORDER_2_ID: 2,
    WORKORDER_NO_DEPENDENCIES_ID: 3,
    WORKORDER_TEST_ID: 4,

    // Billing
    PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID: 1,
    PAYMENT_RESOURCE_BILL_OF_MATERIAL_2_ID: 2,

    PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID: 1,
    PAYMENT_TENANT_BILL_OF_MATERIAL_2_ID: 2,

    PAYMENT_ACTION_1_ID: 1,
    PAYMENT_ACTION_2_ID: 2,

    PAYMENT_STRUCTURE_TIER_1_1_ID: 1,
    PAYMENT_STRUCTURE_TIER_1_2_ID: 2,
    PAYMENT_STRUCTURE_TIER_2_1_ID: 3,
    PAYMENT_STRUCTURE_TIER_2_2_ID: 4,
    PAYMENT_STRUCTURE_TIER_TEST_ID: 5,

    PAYMENT_STRUCTURE_1_ID: 1,
    PAYMENT_STRUCTURE_2_ID: 2,
    PAYMENT_STRUCTURE_NO_DEPENDENCIES_ID: 3,
    PAYMENT_STRUCTURE_TEST_ID: 4,

    PAYMENT_TYPE_1_ID: 1,
    PAYMENT_TYPE_2_ID: 2,

    // Application
    APPLICATION_1_ID: 1,
    APPLICATION_2_ID: 2,
    APPLICATION_TEST_ID: 3,
    APPLICATION_NO_DEPENDENCIES_ID: 4,
    APPLICATION_INACTIVE_ID: 5,

    APPLICATION_ANIMATION_1_ID: 1,
    APPLICATION_ANIMATION_2_ID: 2,
    APPLICATION_ANIMATION_TEST_ID: 3,

    APPLICATION_GAME_VALIDATION_RESULT_1_ID: 1,
    APPLICATION_GAME_VALIDATION_RESULT_2_ID: 2,

    APPLICATION_CHARACTER_1_ID: 1,
    APPLICATION_CHARACTER_2_ID: 2,
    APPLICATION_CHARACTER_TEST_ID: 3,

    // Game
    GAME_1_ID: 1,
    GAME_2_ID: 2,
    GAME_TEST_ID: 3,
    GAME_NO_DEPENDENCIES_ID: 4,
    GAME_INACTIVE_ID: 5,

    GAME_MODULE_1_ID: 1,
    GAME_MODULE_2_ID: 2,
    GAME_MODULE_TEST_ID: 3,

    GAME_1_POOL_VALIDATION_RESULT_1_ID: 1,
    GAME_2_POOL_VALIDATION_RESULT_2_ID: 2,

    LIVE_TOURNAMENT_1_ID: 1,
    LIVE_TOURNAMENT_2_ID: 2,

    // Achievement
    ACHIEVEMENT_1_ID: 1,
    ACHIEVEMENT_2_ID: 2,
    ACHIEVEMENT_TEST_ID: 3,
    ACHIEVEMENT_INACTIVE_ID: 4,

    BADGE_1_ID: 1,
    BADGE_2_ID: 2,
    BADGE_TEST_ID: 3,
    BADGE_INACTIVE_ID: 4,

    // Advertisement
    ADVERTISEMENT_1_ID: 1,
    ADVERTISEMENT_2_ID: 2,
    ADVERTISEMENT_TEST_ID: 3,
    ADVERTISEMENT_INACTIVE_ID: 4,

    ADVERTISEMENT_PROVIDER_1_ID: 1,
    ADVERTISEMENT_PROVIDER_2_ID: 2,
    ADVERTISEMENT_PROVIDER_TEST_ID: 3,
    ADVERTISEMENT_PROVIDER_INACTIVE_ID: 4,

    // Winning
    WINNING_COMPONENT_1_ID: 1,
    WINNING_COMPONENT_2_ID: 2,
    WINNING_COMPONENT_TEST_ID: 3,
    WINNING_COMPONENT_INACTIVE_ID: 4,

    // Voucher
    VOUCHER_1_ID: 1,
    VOUCHER_2_ID: 2,
    VOUCHER_TEST_ID: 3,
    VOUCHER_INACTIVE_ID: 4,

    VOUCHER_PROVIDER_1_ID: 1,
    VOUCHER_PROVIDER_2_ID: 2,
    VOUCHER_PROVIDER_TEST_ID: 3,
    VOUCHER_PROVIDER_INACTIVE_ID: 4,

    //Tombola
    TOMBOLA_1_ID: 1,
    TOMBOLA_2_ID: 2,
    TOMBOLA_TEST_ID: 3,
    TOMBOLA_INACTIVE_ID: 4,

    // Promocode
    PROMOCODE_CAMPAIGN_1_ID: 1,
    PROMOCODE_CAMPAIGN_2_ID: 2,
    PROMOCODE_CAMPAIGN_TEST_ID: 3,
    PROMOCODE_CAMPAIGN_INACTIVE_ID: 4,

    PROMOCODE_UNIQUE_ID: 1,
    PROMOCODE_NONUNIQUE_ID: 2,
    PROMOCODE_TEST_ID: 3,

    // Workorder events
    WWMSG: {
        LANGUAGE: {
            ID1: 100,
            ID2: 101
        },
        REGIONAL_SETTINGS: {
            ID1: 100,
            ID2: 101
        },
        WORKORDER: {
            ID1: 100,
            ID2: 101,
            ID_NO_RESOURCE: 102,
            ID_2_RESOURCES: 103
        },
        RESOURCE: {
            ID1: '8888888-8888-8888-8888-888888888888',
            ID2: '8888888-8888-8888-1111-888888888888'
        },
        BILLING_MODEL: {
            ID1: 100,
            ID2: 101
        },
        OWNER_RESOURCE: {
            ID1: 100
        }
    },

};