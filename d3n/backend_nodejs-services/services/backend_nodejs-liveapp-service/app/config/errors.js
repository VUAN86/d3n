﻿var _ = require('lodash');

var Errors = {
    NoRecordFound: 'ERR_ENTRY_NOT_FOUND',
    ValidationFailed: 'ERR_VALIDATION_FAILED'
};

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

    if (!err) return true;
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

/**
 * Checks if error have given message
 * @param {Object} err Error (string or javascript error object)
 * @param {string} message Error message
 * @returns
 */
Errors.is = function (err, message) {
    return (err && message && ((_.isString(err) && err == message) ||
        (_.isObject(err) && _.has(err, "message") && err.message == message)));
};
module.exports = Errors;