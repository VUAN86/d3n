/*
 * Fake Media Service
 */
var _ = require('lodash');
var util = require('util');
var DefaultService = require('nodejs-default-service');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

function FakeMediaService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeMediaService, DefaultService);

var o = FakeMediaService.prototype;

o.messageHandlers = _.assign(o.messageHandlers, {
    updateProfilePicture: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeMediaService.updateProfilePicture: ', message);

            resMessage.setContent(null);
            resMessage.setError(null);

            setImmediate(function () {
                clientSession.sendMessage(resMessage);
            });
        } catch (e) {
            logger.error('FakeMediaService.updateProfilePicture', e);
            clientSession.sendMessage(_errorMessage({
                code: 0,
                message: 'ERR_FATAL_ERROR'
            }));
        }
    },
});


function _errorMessage (reqMessage, err) {
    var errMesage = new ProtocolMessage(reqMessage);
    errMesage.setError(err);
    return errMesage;
};

module.exports = FakeMediaService;
