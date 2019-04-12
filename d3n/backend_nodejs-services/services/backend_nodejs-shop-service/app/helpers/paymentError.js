var _ = require('lodash');
var Errors = require('./../config/errors.js');

module.exports = {

    /**
     * Returns the payment error message
     * @param emailErr error from sending email
     */
    message: function(emailErr) {
        if (!emailErr) {
            return Errors.Payment;
        } else {
            if (Errors.is(emailErr, Errors.NotFound)) {
                return Errors.PaymentWithoutNotificationRecipient;
            } else {
                return Errors.PaymentWithoutNotification;
            }
        }
    }
}