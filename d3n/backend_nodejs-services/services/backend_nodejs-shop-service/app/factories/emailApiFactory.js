var _ = require('lodash');
var logger = require('nodejs-logger')();
var Emails = require('./../config/emails.js');
var Errors = require('./../config/errors.js');
var EmailService = require('./../services/emailService.js');

module.exports = {

    sendEmailMoneyPaymentFailed: function(clientSession, tenantInformation, amount, error, callback) {
        _sendEmail(clientSession, tenantInformation, amount, error, null, "moneyPaymentFailed", callback);
    },

    sendEmailBonusPaymentFailed: function(clientSession, tenantInformation, amount, error, transactionId, callback) {
        _sendEmail(clientSession, tenantInformation, amount, error, transactionId, "bonusPaymentFailed", callback);
    },

    sendEmailCreditPaymentFailed: function(clientSession, tenantInformation, amount, error, callback) {
        _sendEmail(clientSession, tenantInformation, amount, error, null, "creditPaymentFailed", callback);
    },

    sendEmailMoneyRollbackSucceeded: function(clientSession, tenantInformation, amount, transactionId, callback) {
        _sendEmail(clientSession, tenantInformation, amount, null, transactionId, "moneyRollbackSucceeded", callback);
    },

    sendEmailMoneyRollbackFailed: function(clientSession, tenantInformation, amount, error, transactionId, callback) {
        _sendEmail(clientSession, tenantInformation, amount, error, transactionId, "moneyRollbackFailed", callback);
    },

    sendEmailBonusRollbackSucceeded: function(clientSession, tenantInformation, amount, transactionId, callback) {
        _sendEmail(clientSession, tenantInformation, amount, null, transactionId, "bonusRollbackSucceeded", callback);
    },

    sendEmailBonusRollbackFailed: function(clientSession, tenantInformation, amount, error, transactionId, callback) {
        _sendEmail(clientSession, tenantInformation, amount, error, transactionId, "bonusRollbackFailed", callback);
    },

    sendEmailCreditRollbackSucceeded: function(clientSession, tenantInformation, amount, transactionId, callback) {
        _sendEmail(clientSession, tenantInformation, amount, null, transactionId, "creditRollbackSucceeded", callback);
    },

    sendEmailCreditRollbackFailed: function(clientSession, tenantInformation, amount, error, transactionId, callback) {
        _sendEmail(clientSession, tenantInformation, amount, error, transactionId, "creditRollbackFailed", callback);
    },

    sendEmailAddingAnalyticEventFailed: function(clientSession, tenantInformation, error, shopOrderId, callback) {
        _sendEmail(clientSession, tenantInformation, null, error, shopOrderId, "addingAnalyticEventFailed", callback);
    }
};

function _sendEmail(clientSession, tenantInformation, amount, error, transactionId, emailType, callback) {
    try {
        var emails = _getEmails(clientSession, tenantInformation, amount, error, transactionId, emailType);
        var emailService = new EmailService(emails);
        emailService.send(function (err) {
            setImmediate(callback, err);
        });
    } catch (ex) {
        logger.error("Error sending email to admins", ex);
        setImmediate(callback, ex == Errors.NotFound ? ex : Errors.FatalError);
    }
}  

function _getEmails(clientSession, tenantInformation, amount, error, transactionId, emailType) {
    if (!(tenantInformation && tenantInformation.adminList instanceof Array && tenantInformation.adminList.length)) {
        logger.error("Error getting tenant admin emails", "no emails found");
        throw Errors.NotFound;
    }

    if (error instanceof Error) {
        error = error.message;
    } else if (error instanceof Object) {
        error = error.message ? error.message : error.toString()
    }

    var tenantId = clientSession.getTenantId();
    var userId = clientSession.getUserId();
    var emailConfig = Emails[emailType];
    var subject = emailConfig.subject;
    var message = emailConfig.message
        .replace(Emails.key.user, "id " + userId)
        .replace(Emails.key.tenant, tenantInformation.name + " (id " + tenantId + ")")
        .replace(Emails.key.amount, amount)
        .replace(Emails.key.transactionId, transactionId)
        .replace(Emails.key.error, error);

    var emails = [];
    _.forEach(tenantInformation.adminList, function(tenantAdmin) {
        emails.push({
            userId: tenantAdmin,
            address: null,
            subject: subject,
            message: message,
            language: 'en',
            waitResponse: false
        });
    });

    return emails;
}