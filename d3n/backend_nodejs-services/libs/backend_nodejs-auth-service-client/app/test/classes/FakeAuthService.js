/*
 * Fake Auth Service
 */
var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var fs = require('fs');


function FakeAuthService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeAuthService, DefaultService);

var o = FakeAuthService.prototype;

o.messageHandlers = _.assign(o.messageHandlers, {
    setUserRole: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeAuthService.setUserRole: ', message);

            resMessage.setContent(null);
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeAuthService.setUserRole', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },
    
    inviteUserByEmailAndRole: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeAuthService.inviteUserByEmailAndRole: ', message);

            resMessage.setContent(null);
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeAuthService.inviteUserByEmailAndRole', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },

    addConfirmedUser: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeAuthService.addConfirmedUser: ', message);

            resMessage.setContent(null);
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeAuthService.addConfirmedUser', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },

    generateImpersonateToken: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeAuthService.generateImpersonateToken: ', message);

            resMessage.setContent(null);
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeAuthService.generateImpersonateToken', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    }


});


function _errorMessage (reqMessage, err) {
    var errMesage = new ProtocolMessage(reqMessage);
    errMesage.setError(err);
    return errMesage;
};

module.exports = FakeAuthService;
