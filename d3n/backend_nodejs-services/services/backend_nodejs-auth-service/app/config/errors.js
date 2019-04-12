var Errors = {
    DatabaseApi: {
        NotRegistered: 'ERR_ENTRY_NOT_FOUND'
    },
    AuthApi: {
        NotRegistered: 'ERR_AUTH_NO_REGISTERED',
        NotVerified: 'ERR_AUTH_NO_VERIFIED',
        AlreadyRegistered: 'ERR_AUTH_ALREADY_REGISTERED',
        AlreadyVerified: 'ERR_AUTH_ALREADY_VERIFIED',
        InvalidEmail: 'ERR_AUTH_INVALID_EMAIL',
        InvalidPhone: 'ERR_AUTH_INVALID_PHONE',
        SecretWrong: 'ERR_AUTH_SECRET_WRONG',
        ValidationFailed: 'ERR_VALIDATION_FAILED',
        TokenNotValid: 'ERR_TOKEN_NOT_VALID',
        CodeNotValid: 'ERR_CODE_NOT_VALID',
        MultipleFacebook: 'ERR_MULTIPLE_FACEBOOK',
        MultipleGoogle: 'ERR_MULTIPLE_GOOGLE',
        AnonymousOnlyMergeAllowed: 'ERR_AUTH_ANONYMOUS_ONLY_MERGE_ALLOWED',
        ProviderTokenNotValid: 'ERR_AUTH_PROVIDER_TOKEN_NOT_VALID',
        ProviderNoEmailReturned: 'ERR_AUTH_PROVIDER_NO_EMAIL_RETURNED',
        FunctionNotImplemented: 'ERR_FUNCTION_NOT_IMPLEMENTED',
        ProviderServiceFatalError: 'ERR_AUTH_PROVIDER_SERVICE_FATAL_ERROR',
        NoInstanceAvailable: 'ERR_NO_INSTANCE_AVAILABLE',
        FatalError: 'ERR_FATAL_ERROR'
    }
};

module.exports = Errors;