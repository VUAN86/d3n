var _ = require('lodash');

var Errors = {
    QuestionApi: {
        ValidationFailed: 'ERR_VALIDATION_FAILED',
        InsufficientRights: 'ERR_INSUFFICIENT_RIGHTS',
        NoResolution: 'ERR_QUESTION_NO_RESOLUTION',
        ProviderTokenNotValid: 'ERR_TOKEN_NOT_VALID',
        FunctionNotImplemented: 'ERR_FUNCTION_NOT_IMPLEMENTED',
        ForeignKeyConstraintViolation: 'ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION',
        UniqueKeyConstraintViolation: 'ERR_UNIQUE_KEY_CONSTRAINT_VIOLATION',
        FatalError: 'ERR_FATAL_ERROR',
        EntityContainBadWords: 'ERR_ENTITY_CONTAIN_BAD_WORDS',
        QuestionHasNoPool: 'ERR_QUESTION_HAS_NO_POOL',
        QuestionHasNoTemplate: 'ERR_QUESTION_HAS_NO_TEMPLATE',
        NoPublishedTranslationFound: 'ERR_NO_PUBLISHED_TRANSLATION_FOUND',
        QuestionTranslationContentHasNoStepsMappings: 'ERR_QUESTION_TRANSLATION_CONTENT_HAS_NO_STEPS_MAPPINGS',
        EntryBlockedByAnotherUser: 'ERR_ENTRY_BLOCKED_BY_ANOTHER_USER',
        NoInstanceAvailable: 'ERR_NO_INSTANCE_AVAILABLE',
        NoDefaultHintDefined: 'ERR_QUESTION_NO_DEFAULT_HINT',
    },
    QuestionTranslationApi: {
        NotValidQuestionStructureDefinition: 'ERR_NOT_VALID_QUESTION_STRUCTURE_DEFINITION',
        MediaNotExist: 'MEDIA_NOT_EXIST',
        MediaCouldNotBeUploaded: 'MEDIA_COULD_NOT_BE_UPLOADED'
    },
    PoolApi: {
        PoolHierarchyInconsistent: 'ERR_POOL_HIERARCHY_INCONSISTENT',
    },
    WorkorderApi: {
        WorkorderHasNoPool: 'ERR_WORKORDER_HAS_NO_POOL',
        WorkorderHasNoResource: 'ERR_WORKORDER_HAS_NO_RESOURCE',
        WorkorderClosedItemsRequiredReached: 'ERR_WORKORDER_CLOSED_ITEMS_REQUIRED_REACHED',
    },
    BillingApi: {
        NoBillSelected: 'ERR_NO_BILL_SELECTED',
        BillNotApproved: 'ERR_BILL_NOT_APPROVED',
    },
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