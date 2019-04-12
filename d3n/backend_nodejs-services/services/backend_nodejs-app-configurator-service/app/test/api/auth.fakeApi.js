var _ = require('lodash');
var fs = require('fs');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();
var DataIds = require('./../config/_id.data.js');

module.exports = {

    'setUserRole': function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('sending to auth user roles:', message);

            resMessage.setContent({
                'messageId': _.uniqueId()
            });
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('setUserRole', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },

    'inviteUserByEmailAndRole': function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('inviting user by email and role: ', message);

            resMessage.setContent({'token': 'dummy-token'});
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('inviteUserByEmailAndRole', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },

    'addConfirmedUser': function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('add a confirmed user: ', message);

            var reqContent = message.getContent();

            var profile = {
                userId: DataIds.LOCAL_USER_ID,
                person: {
                    firstName: reqContent.firstName,
                    lastName: reqContent.lastName
                },
                emails: [{
                    email: reqContent.email,
                    verificationStatus: "verified"
                }]
            };

            if (reqContent.phone) {
                profile.phones = [{ phone: reqContent.phone, verificationStatus: "verified"}];
            }

            resMessage.setContent({ profile: profile });
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('addConfirmedUser', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },

    'generateImpersonateToken': function (message, clientSession) {
        try {
            var self = this;
            var reqContent = message.getContent();
            var resMessage = new ProtocolMessage(message);

            logger.debug('generate impersonate token for user: ', message);

            if (_.includes(reqContent.email, 'admin' )) {
                resMessage.setContent({'token': 'dummy-token'});
                resMessage.setError(null);
            } else {
                resMessage.setContent(null);
                resMessage.setError('ERR_FATAL_ERROR');
            }

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('generateImpersonateToken', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },
};

function _errorMessage(reqMessage, err) {
    var errMesage = new ProtocolMessage(reqMessage);
    errMesage.setError(err);
    return errMesage;
};
