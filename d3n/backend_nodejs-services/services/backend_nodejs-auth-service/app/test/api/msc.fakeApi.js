var _ = require('lodash');
var fs = require('fs');
var Errors = require('./../../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

module.exports = {

    updateProfilePicture: function (message, clientSession) {
        try {
            var self = this;
            var resMessage = new ProtocolMessage(message);

            logger.debug('FakeMediaService.updateProfilePicture: ', message);

            resMessage.setContent(null);
            resMessage.setError(null);

            return setImmediate(function () {
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
};

function _errorMessage(reqMessage, err) {
    var errMesage = new ProtocolMessage(reqMessage);
    errMesage.setError(err);
    return errMesage;
};
