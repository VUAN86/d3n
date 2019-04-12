var _ = require('lodash');
var jwt = require('jsonwebtoken');
var Errors = require('./../../config/errors.js');
var ProfileData = require('./../config/profile.data.js');
var ProtocolMessage = require('nodejs-protocol');

module.exports = {

    getProfile: function (message, clientSession) {
        if (_.isNull(message.getSeq()) ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.FatalError, message, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = _.find(global.wsHelper._profiles, { userId: message.getContent().userId });
            pm.setContent({ profile: profile });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    },

    updateProfileBlob: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'name') || !_.has(message.getContent(), 'value') ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.FatalError, message, clientSession);
        }
        try {
            var pm = _message(message);
            var profileBlobEntry = _profileBlob(message);
            var userId = null;
            if (_.has(message.getContent(), 'userId')) {
                userId = message.getContent().userId;
            } else {
                var payload = jwt.decode(message.getToken(), { complete: true }).payload;
                userId = payload.userId;
            }
            var profileBlobIndex = _.findIndex(global.wsHelper._profileBlobs, { userId: userId, name: message.getContent().name });
            if (profileBlobIndex > -1) {
                global.wsHelper._profileBlobs[profileBlobIndex].value = profileBlobEntry.value;
            } else {
                global.wsHelper._profileBlobs.push({
                    userId: profileBlobEntry.userId,
                    name: profileBlobEntry.name,
                    value: profileBlobEntry.value
                });
            }
            pm.setContent({ name: profileBlobEntry.name, value: profileBlobEntry.value });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    },

    getProfileBlob: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'name') ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.FatalError, message, clientSession);
        }
        try {
            var pm = _message(message);
            var userId = null;
            if (_.has(message.getContent(), 'userId')) {
                userId = message.getContent().userId;
            } else {
                var payload = jwt.decode(message.getToken(), { complete: true }).payload;
                userId = payload.userId;
            }
            var profileBlobEntry = _.find(global.wsHelper._profileBlobs, { userId: userId, name: message.getContent().name });
            pm.setContent({ name: profileBlobEntry.name, value: profileBlobEntry.value });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    },

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

function _profileBlob(message) {
    var content = message.getContent();
    var profileBlob = {};
    if (_.has(content, 'userId') && content.userId) {
        profileBlob.userId = content.userId;
    } else {
        profileBlob.userId = ProfileData.TEST_PROFILE_BLOB.userId;
    }
    if (_.has(content, 'name') && content.name) {
        profileBlob.name = content.name;
    } else {
        profileBlob.name = ProfileData.TEST_PROFILE_BLOB.name;
    }
    if (_.has(content, 'value') && content.value) {
        profileBlob.value = content.value;
    } else {
        profileBlob.value = ProfileData.TEST_PROFILE_BLOB.value;
    }
    return profileBlob;
}

function _errorMessage(err, message, clientSession) {
    var pm = _message(message);
    if (err && _.isObject(err) && _.has(err, 'stack')) {
        pm.setError(Errors.FatalError);
    } else {
        pm.setError(err);
    }
    setImmediate(function (err, clientSession) {
        return clientSession.sendMessage(pm);
    }, pm, clientSession);
}

function _successMessage(pm, clientSession) {
    setImmediate(function (pm, clientSession) {
        return clientSession.sendMessage(pm);
    }, pm, clientSession);
}
