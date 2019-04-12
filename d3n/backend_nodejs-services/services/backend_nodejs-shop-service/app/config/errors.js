var _ = require('lodash');

var Errors = {
    RequestMapping: 'ERR_REQUEST_MAPPING',
    ResponseMapping: 'ERR_RESPONSE_MAPPING',
    ShopCall: 'ERR_SHOP_CALL',
    ProfileCall: 'ERR_PROFILE_CALL',
    PaymentCall: 'ERR_PAYMENT_CALL',
    MissingPrice: 'ERR_MISSING_PRICE',
    Payment: 'ERR_PAYMENT',
    PaymentNotCovered: 'ERR_INSUFFICIENT_FUNDS',
    NoReward: 'ERR_NO_REWARD',
    ClearingShoppingCart: 'ERR_CLEARING_SHOPPING_CART',
    NoRewardAndClearingShoppingCart: 'ERR_NO_REWARD_AND_CLEARING_SHOPPING_CART',
    PaymentWithoutNotification: 'ERR_PAYMENT_WITHOUT_NOTIFICATION',
    PaymentWithoutNotificationRecipient: 'ERR_PAYMENT_WITHOUT_NOTIFICATION_RECIPIENT',
    PaymentRollback: 'ERR_PAYMENT_ROLLBACK',
    PaymentRollbackWithoutNotification: 'ERR_PAYMENT_ROLLBACK_WITHOUT_NOTIFICATION',
    PaymentRollbackWithoutNotificationRecipient: 'ERR_PAYMENT_ROLLBACK_WITHOUT_NOTIFICATION_RECIPIENT',
    Rollback: 'ERR_ROLLBACK',
    RollbackWithoutNotification: 'ERR_ROLLBACK_WITHOUT_NOTIFICATION',
    RollbackWithoutNotificationRecipient: 'ERR_ROLLBACK_WITHOUT_NOTIFICATION_RECIPIENT',
    NotFound: 'ERR_NOT_FOUND',
    AlreadyExists: 'ERR_ALREADY_EXISTS',
    AlreadyFinalized: 'ERR_ALREADY_FINALIZED',
    FatalError: 'ERR_FATAL_ERROR',
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