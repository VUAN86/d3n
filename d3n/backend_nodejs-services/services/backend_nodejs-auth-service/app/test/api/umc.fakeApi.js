var _ = require('lodash');
var fs = require('fs');
var Errors = require('./../../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

module.exports = {

    'sendEmail': function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('sending mail:', message);

            var fileContent;
            try {
                fileContent = fs.readFileSync(self.config.sendEmailFile, 'utf8').trim();
                if (!fileContent) {
                    fileContent = '[]';
                }
            } catch (e) {
                fileContent = '[]';
            }

            var emailsSent = JSON.parse(fileContent);
            resMessage.setContent({
                'messageId': _.uniqueId()
            });
            resMessage.setError(null);

            emailsSent.push(resMessage.getMessageContainer());
            fs.writeFileSync(self.config.sendEmailFile, JSON.stringify(emailsSent), 'utf8');

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });

        } catch (e) {
            logger.error('sendEmail', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_SENDING_EMAIL'
            }));
        }
    },

    'sendSms': function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('sending sms:', message);

            var fileContent;
            try {
                fileContent = fs.readFileSync(self.config.sendEmailFile, 'utf8').trim();
                if (!fileContent) {
                    fileContent = '[]';
                }
            } catch (e) {
                fileContent = '[]';
            }

            var smsSent = JSON.parse(fileContent);
            resMessage.setContent({
                'messageId': _.uniqueId()
            });
            resMessage.setError(null);

            smsSent.push(resMessage.getMessageContainer());
            fs.writeFileSync(self.config.sendEmailFile, JSON.stringify(smsSent), 'utf8');

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });

        } catch (e) {
            logger.error('sendSms', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_SENDING_SMS'
            }));
        }
    },
};

function _errorMessage(reqMessage, err) {
    var errMesage = new ProtocolMessage(reqMessage);
    errMesage.setError(err);
    return errMesage;
};
