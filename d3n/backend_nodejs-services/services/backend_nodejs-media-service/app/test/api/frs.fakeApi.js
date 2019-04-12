var _ = require('lodash');
var jwt = require('jsonwebtoken');
var Errors = require('./../../config/errors.js');
var TestData = require('./../config/frs.data.js');
var ProtocolMessage = require('nodejs-protocol');

module.exports = {

    groupGet: function (message, clientSession) {
        try {
            if (!message.getMessage() || _.isNull(message.getSeq()) ||
                !_.has(message.getContent(), 'userId') || 
                !_.has(message.getContent(), 'groupId') ||
                !_.has(message.getContent(), 'tenantId')) {
                return _errorMessage(Errors.MediaApi.ValidationFailed, clientSession);
            }

            var pm = _message(message);
            pm.setContent({ group: TestData.GROUP_1 });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    groupUpdate: function (message, clientSession) {
        try {
            if (!message.getMessage() || _.isNull(message.getSeq())) {
                return _errorMessage(Errors.MediaApi.ValidationFailed, clientSession);
            }

            var pm = _message(message);
            pm.setContent({ group: TestData.GROUP_1 });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    playerIsGroupMember: function (message, clientSession) {
        try {
            if (!message.getMessage() || _.isNull(message.getSeq()) ||
                !_.has(message.getContent(), 'userId') || 
                !_.has(message.getContent(), 'groupId') ||
                !_.has(message.getContent(), 'tenantId')) {
                    return _errorMessage(Errors.MediaApi.ValidationFailed, clientSession);
            }

            var pm = _message(message);
            pm.setContent({ userIsMember: true });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    }
};

function _message(message) {
    var pm = new ProtocolMessage();
    pm.setMessage(message.getMessage() + 'Response');
    pm.setContent(null);
    pm.setSeq(message && message.getSeq() ? message.getSeq() : null);
    pm.setAck(message && message.getSeq() ? [message.getSeq()] : null);
    pm.setClientId(message && message.getClientId() ? message.getClientId() : null);
    pm.setTimestamp(_.now());
    pm.setToken(null);
    return pm;
}

function _errorMessage(err, clientSession) {
    setImmediate(function (err, clientSession) {
        var pm = _message(new ProtocolMessage());
        if (err && _.isObject(err) && _.has(err, 'stack')) {
            pm.setError(Errors.MediaApi.FatalError);
        } else {
            pm.setError(err);
        }
        return clientSession.sendMessage(pm);
    }, err, clientSession);
}

function _successMessage(pm, clientSession) {
    setImmediate(function (pm, clientSession) {
        return clientSession.sendMessage(pm);
    }, pm, clientSession);
}
