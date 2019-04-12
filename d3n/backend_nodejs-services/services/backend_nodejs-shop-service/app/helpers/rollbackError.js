var _ = require('lodash');
var Errors = require('./../config/errors.js');

module.exports = {
    
    /**
     * Returns the rollback error message
     * @param emailErr error from sending email
     */
    message: function (emailErr) {
        if (!emailErr) {
            return Errors.Rollback;
        } else {
            if (Errors.is(emailErr, Errors.NotFound)) {
                return Errors.RollbackWithoutNotificationRecipient;
            } else {
                return Errors.RollbackWithoutNotification;
            }
        }
    }
}
