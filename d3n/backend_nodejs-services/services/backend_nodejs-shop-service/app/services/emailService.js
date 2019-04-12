var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();
var UserMessageClient = require('nodejs-user-message-client');

module.exports = EmailService;
    
/**
 * Create an email service for a specific method
 * @param emails list of parameters for each email
 */
function EmailService(emails) {
    this.userMessageClient = _getUserMessageClient();
    this.emails = emails;
}

/**
 * Send multiple emails
 * @param callback method to be called when receiving response
 */
EmailService.prototype.send = function (callback) {
    try {
        async.each(this.emails, function (email, cbItem) {
            try {
                this.userMessageClient.sendEmail(email, cbItem);
            } catch (err) {
                logger.error('error sending email to admin', item, err);
                cbItem(err);
            }
        }.bind(this), callback);
    } catch (ex) {
        logger.error("Error sending email to admins", ex);
        setImmediate(callback, Errors.FatalError);
    }
};

function _getUserMessageClient() {
    return new UserMessageClient({
        registryServiceURIs: Config.registryServiceURIs,
        ownerServiceName: 'shopService'
    });
}