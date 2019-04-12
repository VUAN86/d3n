var _ = require('lodash');
var jwt = require('jsonwebtoken');
var Errors = require('./../../config/errors.js');
var TestData = require('./../config/psc.data.js');
var ProtocolMessage = require('nodejs-protocol');

module.exports = {

    createProfile: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq())) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = _profile(message);
            pm.setContent({ userId: profile.userId });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    updateProfile: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'profile')) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = _profile(message);
            pm.setContent({ profile: profile });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    getProfile: function (message, clientSession) {
        if (_.isNull(message.getSeq()) ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = _profile(message);
            pm.setContent({ profile: profile });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    deleteProfile: function (message, clientSession) {
        if (_.isNull(message.getSeq()) || !message.getToken()) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    mergeProfile: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'source') || !_.has(message.getContent(), 'target')) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = _profile(message);
            pm.setContent({ profile: profile });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    findByIdentifier: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'identifierType') || !_.has(message.getContent(), 'identifier')) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = _profile(message);
            pm.setContent({ userId: profile.userId });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    getAppConfiguration: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'appId') || !_.has(message.getContent(), 'deviceUUID') || !_.has(message.getContent(), 'device')) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var appConfig = _appConfig(message);
            pm.setContent({ appConfig: appConfig });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    updateProfileBlob: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'name') || !_.has(message.getContent(), 'value') ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profileBlob = _profileBlob(message);
            pm.setContent(profileBlob);
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    getProfileBlob: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'name') ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.PscApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profileBlob = _profileBlob(message);
            pm.setContent(profileBlob);
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
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
    
function _profile(message) {
    var content = message.getContent();
    var profile = {};
    if (_.has(content, 'profile')) {
        profile = content.profile;
    } else {
        profile = _.clone(TestData.TEST_PROFILE);
        profile.emails.push({ email: 'test@ascendro.de', verificationStatus: 'verified' });
        profile.phones.push({ phone: '1234567890', verificationStatus: 'verified' });
    }
    if (_.has(content, 'email') && content.email) {
        profile.emails.push({ email: content.email, verificationStatus: 'notVerified' });
    }
    if (_.has(content, 'phone') && content.phone) {
        profile.phones.push({ phone: content.phone, verificationStatus: 'notVerified' });
    }
    if (_.has(content, 'facebook') && content.facebook) {
        profile.facebook = content.facebook;
    }
    if (_.has(content, 'google') && content.google) {
        profile.google = content.google;
    }
    if (_.has(content, 'userId') && content.userId) {
        profile.userId = content.userId;
    }
    if (message.getToken()) {
        var payload = jwt.decode(message.getToken(), { complete: true }).payload;
        profile.userId = payload.userId;
        profile.roles = payload.roles;
    }
    return profile;
}

function _profileBlob(message) {
    var content = message.getContent();
    var profileBlob = {};
    if (_.has(content, 'name') && content.name) {
        profileBlob.name = content.name;
    } else {
        profileBlob.name = TestData.TEST_PROFILE_BLOB.name;
    }
    if (_.has(content, 'value') && content.value) {
        profileBlob.value = content.value;
    } else {
        profileBlob.value = TestData.TEST_PROFILE_BLOB.value;
    }
    return profileBlob;
}

function _appConfig(message) {
    var appConfig = { config: 'fake' };
    return appConfig;
}

function _errorMessage(err, clientSession) {
    setImmediate(function (err, clientSession) {
        var pm = _message(new ProtocolMessage());
        if (err && _.isObject(err) && _.has(err, 'stack')) {
            pm.setError(Errors.PscApi.FatalError);
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
