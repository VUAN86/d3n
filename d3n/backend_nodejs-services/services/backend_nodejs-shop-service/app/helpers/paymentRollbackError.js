var _ = require('lodash');
var Errors = require('./../config/errors.js');

module.exports = {

    /**
     * Returns the payment with successful rollback error message
     * @param emailErr error from sending email
     */
    message: function(emailErr) {
        if (!emailErr) {
            return Errors.PaymentRollback;
        } else {
            if (Errors.is(emailErr, Errors.NotFound)) {
                return Errors.PaymentRollbackWithoutNotificationRecipient;
            } else {
                return Errors.PaymentRollbackWithoutNotification;
            }
        }
    }
}