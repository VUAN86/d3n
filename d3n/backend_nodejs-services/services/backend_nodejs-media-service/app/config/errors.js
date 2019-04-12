var _ = require('lodash');

var Errors = {
    MediaApi: {
        ValidationFailed: 'ERR_VALIDATION_FAILED',
        InsufficientRights: 'ERR_INSUFFICIENT_RIGHTS',
        CdnUploadError: 'ERR_MEDIA_API_CDN_UPLOAD',
        CdnReadError: 'ERR_MEDIA_API_CDN_READ',
        CdnDeleteError: 'ERR_MEDIA_API_CDN_DELETE',
        ForeignKeyConstraintViolation: 'ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION',
        UniqueKeyConstraintViolation: 'ERR_UNIQUE_KEY_CONSTRAINT_VIOLATION',
        FunctionNotImplemented: 'ERR_FUNCTION_NOT_IMPLEMENTED',
        FatalError: 'ERR_FATAL_ERROR'
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