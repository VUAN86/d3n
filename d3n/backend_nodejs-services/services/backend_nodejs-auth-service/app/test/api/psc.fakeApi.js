var _ = require('lodash');
var jwt = require('jsonwebtoken');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var TestData = require('./../config/psc.data.js');
var ProtocolMessage = require('nodejs-protocol');

module.exports = {

    createProfile: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq())) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
        }
        if (!global.wsHelper._profiles) {
            global.wsHelper.initProfiles();
            message.setContent({ userId: TestData.TEST_EMAIL_USER.userId });
        } else {
            message.setContent({ userId: TestData.TEST_EMAIL_NEW_USER.userId });
        }
        try {
            var pm = _message(message);
            var profileData = _profile(message);
            var profileIndex = _.findIndex(global.wsHelper._profiles, { userId: profileData.userId });
            if (profileIndex > -1) {
                global.wsHelper._profiles.splice(profileIndex, 1);
            }
            global.wsHelper._profiles.push(profileData);
            pm.setContent({ userId: profileData.userId });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    updateProfile: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'profile')) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = undefined;
            var profileData = _profile(message);
            var profileIndex = _.findIndex(global.wsHelper._profiles, { userId: profileData.userId });
            if (profileIndex > -1) {
                global.wsHelper._profiles[profileIndex] = _.assign(global.wsHelper._profiles[profileIndex], profileData);
                profile = global.wsHelper._profiles[profileIndex];
            }
            pm.setContent({ profile: profile });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    getProfile: function (message, clientSession) {
        if (_.isNull(message.getSeq()) ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = _.find(global.wsHelper._profiles, { userId: message.getContent().userId });
            pm.setContent({ profile: profile });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    deleteProfile: function (message, clientSession) {
        if (_.isNull(message.getSeq()) || !message.getToken()) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
        }
        try {
            var profileIndex = _.findIndex(global.wsHelper._profiles, { userId: message.getContent().userId });
            if (profileIndex > -1) {
                global.wsHelper._profiles.splice(profileIndex, 1);
            }
            var pm = _message(message);
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    mergeProfile: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'source') || !_.has(message.getContent(), 'target')) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profileIndex = _.findIndex(global.wsHelper._profiles, { userId: message.getContent().source });
            if (profileIndex > -1) {
                global.wsHelper._profiles.splice(profileIndex, 1);
            }
            var profile = _.find(global.wsHelper._profiles, { userId: message.getContent().target });
            pm.setContent({ profile: profile });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    findByIdentifier: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'identifierType') || !_.has(message.getContent(), 'identifier')) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var profile = {};
            if (message.getContent().identifierType === 'EMAIL') {
                for (var i = 0; i < global.wsHelper._profiles.length; i++) {
                    if (_.has(global.wsHelper._profiles[i], 'emails') && _.find(global.wsHelper._profiles[i].emails, { email: message.getContent().identifier })) {
                        profile = global.wsHelper._profiles[i];
                    }
                }
            } else if (message.getContent().identifierType === 'PHONE') {
                for (var i = 0; i < global.wsHelper._profiles.length; i++) {
                    if (_.has(global.wsHelper._profiles[i], 'phones') && _.find(global.wsHelper._profiles[i].phones, { phone: message.getContent().identifier })) {
                        profile = global.wsHelper._profiles[i];
                    }
                }
            } else if (message.getContent().identifierType === 'FACEBOOK') {
                for (var i = 0; i < global.wsHelper._profiles.length; i++) {
                    if (_.has(global.wsHelper._profiles[i], 'facebook') && global.wsHelper._profiles[i].facebook === message.getContent().identifier) {
                        profile = global.wsHelper._profiles[i];
                    }
                }
            } else if (message.getContent().identifierType === 'GOOGLE') {
                for (var i = 0; i < global.wsHelper._profiles.length; i++) {
                    if (_.has(global.wsHelper._profiles[i], 'google') && global.wsHelper._profiles[i].google === message.getContent().identifier) {
                        profile = global.wsHelper._profiles[i];
                    }
                }
            }
            pm.setContent({ userId: profile.userId });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    getAppConfiguration: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'appId') || !_.has(message.getContent(), 'deviceUUID') || !_.has(message.getContent(), 'device')) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
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
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
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
            return _errorMessage(ex, clientSession);
        }
    },

    getProfileBlob: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'name') ||
            !(_.has(message.getContent(), 'userId') || message.getToken())) {
            return _errorMessage(Errors.AuthApi.ValidationFailed, clientSession);
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
    var profile = { emails:[], phones:[], roles:[] };
    if (_.has(content, 'profile')) {
        profile = content.profile;
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
    if (_.isUndefined(profile.userId)) {
        if (_.has(content, 'email') && content.email == TestData.TEST_EMAIL_NEW_USER.email) {
            profile.userId = TestData.TEST_EMAIL_NEW_USER.userId;
        } else {
            profile.userId = TestData.TEST_PROFILE.userId;
        }
    }
    return profile;
}

function _profileBlob(message) {
    var content = message.getContent();
    var profileBlob = {};
    if (_.has(content, 'userId') && content.userId) {
        profileBlob.userId = content.userId;
    } else {
        profileBlob.userId = TestData.TEST_PROFILE_BLOB.userId;
    }
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
            pm.setError(Errors.AuthApi.FatalError);
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
