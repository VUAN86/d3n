var types = {
    SERVER: "server",
    CLIENT: "client",
    AUTH: "auth",
    VALIDATION: "validation"
};

var errors = {
    'ERR_INSUFFICIENT_RIGHTS': {
        type: types.AUTH,
        message: 'ERR_INSUFFICIENT_RIGHTS'
    },
    
    'ERR_TOKEN_NOT_VALID': {
        type: types.AUTH,
        message: 'ERR_TOKEN_NOT_VALID'
    },

    'ERR_CODE_NOT_VALID': {
        type: types.AUTH,
        message: 'ERR_CODE_NOT_VALID'
    },

    'ERR_VALIDATION_FAILED': {
        type: types.VALIDATION,
        message: 'ERR_VALIDATION_FAILED'
    },
    
    'ERR_CONNECTION_FAILED': {
        type: types.CLIENT,
        message: 'ERR_CONNECTION_FAILED'
    },

    'ERR_ALREADY_VALIDATED': {
        type: types.CLIENT,
        message: 'ERR_ALREADY_VALIDATED'
    },

    'ERR_CONSTRAINTS': {
        type: types.CLIENT,
        message: 'ERR_CONSTRAINTS'
    },
    
    'ERR_ENTRY_NOT_FOUND': {
        type: types.CLIENT,
        message: 'ERR_ENTRY_NOT_FOUND'
    },
    
    'ERR_ENTRY_ALREADY_EXISTS': {
        type: types.CLIENT,
        message: 'ERR_ENTRY_ALREADY_EXISTS'
    },
    
    'ERR_ENTRY_ALREADY_EXISTS': {
        type: types.CLIENT,
        message: 'ERR_ENTRY_ALREADY_EXISTS'
    },

    'ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION': {
        type: types.CLIENT,
        message: 'ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION'
    },

    'ERR_UNIQUE_KEY_CONSTRAINT_VIOLATION': {
        type: types.CLIENT,
        message: 'ERR_UNIQUE_KEY_CONSTRAINT_VIOLATION'
    },

    'ERR_FUNCTION_NOT_IMPLEMENTED': {
        type: types.SERVER,
        message: 'ERR_FUNCTION_NOT_IMPLEMENTED'
    },

    'ERR_CALLBACK_NOT_PROVIDED': {
        type: types.SERVER,
        message: 'ERR_CALLBACK_NOT_PROVIDED'
    },

    'ERR_WRONG_REGISTRY_SERVICE_URIS': {
        type: types.SERVER,
        message: 'ERR_WRONG_REGISTRY_SERVICE_URIS'
    },

    'ERR_FATAL_ERROR': {
        type: types.SERVER,
        message: 'ERR_FATAL_ERROR'
    },

    'ERR_GAME_CONFIGURATION_IS_INVALID': {
        type: types.CLIENT,
        message: 'ERR_GAME_CONFIGURATION_IS_INVALID'
    },

    'ERR_GAME_PUBLISHING_NO_GAME_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_GAME_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_APPLICATION_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_APPLICATION_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_POOL_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_POOL_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_REGIONAL_SETTING_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_REGIONAL_SETTING_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_LANGUAGE_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_LANGUAGE_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_QUESTION_TEMPLATE_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_QUESTION_TEMPLATE_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_ADVERTISEMENT_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_ADVERTISEMENT_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_VOUCHER_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_VOUCHER_FOUND'
    },

    'ERR_GAME_PUBLISHING_NO_WINNING_COMPONENT_FOUND': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_NO_WINNING_COMPONENT_FOUND'
    },

    'ERR_GAME_PUBLISHING_INVALID_AMOUNT_OF_QUESTIONS': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_INVALID_AMOUNT_OF_QUESTIONS'
    },

    'ERR_GAME_PUBLISHING_INVALID_AMOUNT_OF_QUESTIONS': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_INVALID_AMOUNT_OF_QUESTIONS'
    },

    'ERR_GAME_PUBLISHING_INVALID_COMPLEXITY_STRUCTURE': {
        type: types.CLIENT,
        message: 'ERR_GAME_PUBLISHING_INVALID_COMPLEXITY_STRUCTURE'
    },

    'ERR_QUESTION_NO_RESOLUTION': {
        type: types.CLIENT,
        message: 'ERR_QUESTION_NO_RESOLUTION'
    },

    'ERR_QUESTION_HAS_NO_POOL': {
        type: types.CLIENT,
        message: 'ERR_QUESTION_HAS_NO_POOL'
    },

    'ERR_QUESTION_HAS_NO_TEMPLATE': {
        type: types.CLIENT,
        message: 'ERR_QUESTION_HAS_NO_TEMPLATE'
    },

    'ERR_NO_PUBLISHED_TRANSLATION_FOUND': {
        type: types.CLIENT,
        message: 'ERR_NO_PUBLISHED_TRANSLATION_FOUND'
    },

    'ERR_QUESTION_TRANSLATION_CONTENT_HAS_NO_STEPS_MAPPINGS': {
        type: types.CLIENT,
        message: 'ERR_QUESTION_TRANSLATION_CONTENT_HAS_NO_STEPS_MAPPINGS'
    },

    'ERR_APPLICATION_IS_DEPLOYED': {
        type: types.CLIENT,
        message: 'ERR_APPLICATION_IS_DEPLOYED'
    },

    'ERR_APPLICATION_IS_ACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_APPLICATION_IS_ACTIVATED'
    },

    'ERR_APPLICATION_IS_DEACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_APPLICATION_IS_DEACTIVATED'
    },

    'ERR_APPLICATION_IS_ARCHIVED': {
        type: types.CLIENT,
        message: 'ERR_APPLICATION_IS_ARCHIVED'
    },

    'ERR_APPLICATION_NAME_NOT_UNIQUE_PER_TENANT': {
        type: types.CLIENT,
        message: 'ERR_APPLICATION_NAME_NOT_UNIQUE_PER_TENANT'
    },

    'ERR_NO_BILL_SELECTED': {
        type: types.CLIENT,
        message: 'ERR_NO_BILL_SELECTED'
    },

    'ERR_BILL_NOT_APPROVED': {
        type: types.CLIENT,
        message: 'ERR_BILL_NOT_APPROVED'
    },

    'ERR_GAME_IS_DEPLOYED': {
        type: types.CLIENT,
        message: 'ERR_GAME_IS_DEPLOYED'
    },

    'ERR_GAME_IS_ACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_GAME_IS_ACTIVATED'
    },

    'ERR_GAME_IS_DEACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_GAME_IS_DEACTIVATED'
    },

    'ERR_GAME_IS_ARCHIVED': {
        type: types.CLIENT,
        message: 'ERR_GAME_IS_ARCHIVED'
    },

    'ERR_ADVERTISEMENT_IS_DEPLOYED': {
        type: types.CLIENT,
        message: 'ERR_ADVERTISEMENT_IS_DEPLOYED'
    },

    'ERR_ADVERTISEMENT_IS_ACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_ADVERTISEMENT_IS_ACTIVATED'
    },

    'ERR_ADVERTISEMENT_IS_DEACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_ADVERTISEMENT_IS_DEACTIVATED'
    },

    'ERR_ADVERTISEMENT_IS_ARCHIVED': {
        type: types.CLIENT,
        message: 'ERR_ADVERTISEMENT_IS_ARCHIVED'
    },

    'ERR_ADVERTISEMENT_PROVIDER_IS_ACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_ADVERTISEMENT_PROVIDER_IS_ACTIVATED'
    },

    'ERR_ADVERTISEMENT_PROVIDER_IS_DEACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_ADVERTISEMENT_PROVIDER_IS_DEACTIVATED'
    },

    'ERR_ADVERTISEMENT_PROVIDER_IS_ARCHIVED': {
        type: types.CLIENT,
        message: 'ERR_ADVERTISEMENT_PROVIDER_IS_ARCHIVED'
    },

    'ERR_VOUCHER_IS_DEPLOYED': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_IS_DEPLOYED'
    },

    'ERR_VOUCHER_IS_ACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_IS_ACTIVATED'
    },

    'ERR_VOUCHER_IS_DEACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_IS_DEACTIVATED'
    },

    'ERR_VOUCHER_IS_ARCHIVED': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_IS_ARCHIVED'
    },

    'ERR_VOUCHER_PROVIDER_IS_ACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_PROVIDER_IS_ACTIVATED'
    },

    'ERR_VOUCHER_PROVIDER_IS_DEACTIVATED': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_PROVIDER_IS_DEACTIVATED'
    },

    'ERR_VOUCHER_PROVIDER_IS_ARCHIVED': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_PROVIDER_IS_ARCHIVED'
    },

    'ERR_VOUCHER_CODES_ARE_NOT_AVAILABLE': {
        type: types.CLIENT,
        message: 'ERR_VOUCHER_CODES_ARE_NOT_AVAILABLE'
    },

    'ERR_DATABASE_NO_MODEL_DEFINED': {
        type: types.SERVER,
        message: 'ERR_DATABASE_NO_MODEL_DEFINED'
    },

    'ERR_DATABASE_NO_ENTITY_DATA': {
        type: types.SERVER,
        message: 'ERR_DATABASE_NO_ENTITY_DATA'
    },

    'ERR_AUTH_NO_VERIFIED': {
        type: types.CLIENT,
        message: 'ERR_AUTH_NO_VERIFIED'
    },

    'ERR_AUTH_NO_REGISTERED': {
        type: types.CLIENT,
        message: 'ERR_AUTH_NO_REGISTERED'
    },

    'ERR_AUTH_ALREADY_VERIFIED': {
        type: types.CLIENT,
        message: 'ERR_AUTH_ALREADY_VERIFIED'
    },

    'ERR_AUTH_ALREADY_REGISTERED': {
        type: types.CLIENT,
        message: 'ERR_AUTH_ALREADY_REGISTERED'
    },

    'ERR_AUTH_PROVIDER_TOKEN_NOT_VALID': {
        type: types.CLIENT,
        message: 'ERR_AUTH_PROVIDER_TOKEN_NOT_VALID'
    },

    'ERR_AUTH_PROVIDER_NO_EMAIL_RETURNED': {
        type: types.SERVER,
        message: 'ERR_AUTH_PROVIDER_NO_EMAIL_RETURNED'
    },

    'ERR_AUTH_PROVIDER_SERVICE_FATAL_ERROR': {
        type: types.SERVER,
        message: 'ERR_AUTH_PROVIDER_SERVICE_FATAL_ERROR'
    },

    'ERR_AUTH_VERIFICATION_CODE_ERROR': {
        type: types.SERVER,
        message: 'ERR_AUTH_VERIFICATION_CODE_ERROR'
    },
    
    'ERR_NO_INSTANCE_AVAILABLE': {
        type: types.SERVER,
        message: 'ERR_NO_INSTANCE_AVAILABLE'
    },
    
    'ERR_NOT_CONNECTED_TO_EVENT_SERVICE': {
        type: types.SERVER,
        message: 'ERR_NOT_CONNECTED_TO_EVENT_SERVICE'
    },

    'ERR_INVALID_TOPIC': {
        type: types.SERVER,
        message: 'ERR_INVALID_TOPIC'
    },

    'ERR_ALREADY_SUBSCRIBED': {
        type: types.SERVER,
        message: 'ERR_ALREADY_SUBSCRIBED'
    },

    'ERR_SUBSCRIPTION_ID_NOT_FOUND': {
        type: types.SERVER,
        message: 'ERR_SUBSCRIPTION_ID_NOT_FOUND'
    },

    'ERR_NOTIFICATION_CONTENT_IS_REQUIRED': {
        type: types.SERVER,
        message: 'ERR_NOTIFICATION_CONTENT_IS_REQUIRED'
    },

    'ERR_AUTH_SECRET_WRONG': {
        type: types.CLIENT,
        message: 'ERR_AUTH_SECRET_WRONG'
    },
    
    'ERR_DATABASE_NO_RECORD_ID': {
        type: types.SERVER,
        message: 'ERR_DATABASE_NO_RECORD_ID'
    },
    
    'ERR_DATABASE_NO_ENGINE': {
        type: types.SERVER,
        message: 'ERR_DATABASE_NO_ENGINE'
    },
    
    'ERR_DATABASE_NO_FUNCTION': {
        type: types.SERVER,
        message: 'ERR_DATABASE_NO_FUNCTION'
    },
    
    'ERR_DATABASE_NO_FUNCTION_CALLBACK': {
        type: types.SERVER,
        message: 'ERR_DATABASE_NO_FUNCTION_CALLBACK'
    },
    
    'ERR_SERVICE_NOT_FOUND': {
        type: types.CLIENT,
        message: 'ERR_SERVICE_NOT_FOUND'
    },
    
    'ERR_ENTITY_CONTAIN_BAD_WORDS': {
        type: types.CLIENT,
        message: 'ERR_ENTITY_CONTAIN_BAD_WORDS'
    },
    
    'ERR_BAD_WORDS_NOT_A_STRING': {
        type: types.CLIENT,
        message: 'ERR_BAD_WORDS_NOT_A_STRING'
    },

    'ERR_CONNECTION_ERROR': {
        type: types.SERVER,
        message: 'ERR_CONNECTION_ERROR'
    },
    
    'SESSION_STORE_INIT': {
        type: types.SERVER,
        message: 'SESSION_STORE_INIT'
    },
    
    'SESSION_STORE_DESTROY': {
        type: types.SERVER,
        message: 'SESSION_STORE_DESTROY'
    },
    
    'NO_SERVICE_REGISTRY_SPECIFIED': {
        type: types.SERVER,
        message: 'NO_SERVICE_REGISTRY_SPECIFIED'
    },
    
    'SERVICE_CONNECTION_INFORMATION_NOT_FOUND': {
        type: types.SERVER,
        message: 'SERVICE_CONNECTION_INFORMATION_NOT_FOUND'
    },
    
    'VOUCHER_ALREADY_USED': {
        type: types.CLIENT,
        message: 'VOUCHER_ALREADY_USED'
    },
    
    'COMPONENT_ALREADY_USED': {
        type: types.CLIENT,
        message: 'COMPONENT_ALREADY_USED'
    },

    'ERR_POOL_HIERARCHY_INCONSISTENT': {
        type: types.CLIENT,
        message: 'ERR_POOL_HIERARCHY_INCONSISTENT'
    },

    'ERR_WORKORDER_HAS_NO_POOL': {
        type: types.CLIENT,
        message: 'ERR_WORKORDER_HAS_NO_POOL'
    },

    'ERR_MEDIA_API_VALIDATION_FAILED': {
        type: types.CLIENT,
        message: 'ERR_MEDIA_API_VALIDATION_FAILED'
    },

    'ERR_MEDIA_API_CDN_UPLOAD': {
        type: types.CLIENT,
        message: 'ERR_MEDIA_API_CDN_UPLOAD'
    },

    'ERR_MEDIA_API_CDN_READ': {
        type: types.CLIENT,
        message: 'ERR_MEDIA_API_CDN_READ'
    },

    'ERR_MEDIA_API_CDN_DELETE': {
        type: types.CLIENT,
        message: 'ERR_MEDIA_API_CDN_DELETE'
    },
    
    'MEDIA_NOT_EXIST': {
        type: types.CLIENT,
        message: 'MEDIA_NOT_EXIST'
    },

    'MEDIA_COULD_NOT_BE_UPLOADED': {
        type: types.CLIENT,
        message: 'MEDIA_COULD_NOT_BE_UPLOADED'
    },

    'ERR_CONTENT_IS_PUBLISHED': {
        type: types.CLIENT,
        message: 'ERR_CONTENT_IS_PUBLISHED'
    }
};

module.exports = errors;