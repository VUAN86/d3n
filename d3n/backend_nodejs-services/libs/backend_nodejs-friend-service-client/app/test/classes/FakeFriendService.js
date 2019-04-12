/*
 * Fake Friend Service
 */
var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var fs = require('fs');
var Data = require('./../unit/_id.data.js');

function FakeFriendService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeFriendService, DefaultService);

var o = FakeFriendService.prototype;

o.messageHandlers = _.assign(o.messageHandlers, {
    groupGet: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeFriendService.groupGet: ', message);

            resMessage.setContent({ group: Data.GROUP_1 });
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeFriendService.groupGet', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },
    
    groupUpdate: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeFriendService.groupUpdate: ', message);

            resMessage.setContent({ group: Data.GROUP_1});
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeFriendService.groupUpdate', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },

    playerIsGroupMember: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeFriendService.playerIsGroupMember: ', message);

            resMessage.setContent({
                userIsMember: true
            });
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeFriendService.playerIsGroupMember', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },
    buddyAddForUser: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeFriendService.buddyAddForUser: ', message);

            resMessage.setContent(null);
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeFriendService.buddyAddForUser', e);
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

module.exports = FakeFriendService;
