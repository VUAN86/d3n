var _ = require('lodash');
var fs = require('fs');
var url = require('url');
var path = require('path');
var https = require('https');
var async = require('async');
var nodepath = require('path');
var httpsRequest = require('superagent');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var RoleService = require('./roleService.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var logger = require('nodejs-logger')();

// Initialize S3 storage with the configuration settings
var S3Client = require('nodejs-s3client');
var S3ClientInstance = S3Client.getInstance(_.merge(S3Client.Config, { amazon_s3: { bucket: Config.blob.bucket } }));

module.exports = {
    cdnStorage: S3ClientInstance.Storage,

    /**
     * Update source profile: removes all duplicated and overlapping roles
     * Remove auth blob from source profile
     * Clone profile picture from source to target (if not already exist)
     * Merge source profile into target profile
     * @param userIdFromToken Source profile ID
     * @param userIdFromProfile Target profile ID
     * @param profile Target profile data
     * @param clientSession
     * @param token
     * @param callback
     * @returns
     */
    merge: function (userIdFromToken, userIdFromProfile, profile, clientSession, token, callback) {
        var self = this;
        var sourceUserId = userIdFromToken;
        var targetUserId = userIdFromProfile;
        var profileFromToken;
        var profileFromProfile;
        return async.series([
            // Get profile by userIdFromProfile
            function (next) {
                try {
                    return clientSession.getConnectionService().getProfileService().getUserProfile({
                        userId: userIdFromProfile,
                        clientInfo: clientSession.getClientInfo()
                    }, undefined, function (err, message) {
                        if (err || (message && message.getError())) {
                            return next(err || message.getError());
                        }
                        // Skip merge process if no destination profile found
                        if (!_.has(message.getContent(), 'profile')) {
                            return next();
                        }
                        profileFromProfile = _.cloneDeep(message.getContent().profile);
                        return next();
                    });
                } catch (ex) {
                    logger.error('ProfileService.merge: profile.updateUserProfile [userIdFromToken]', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
            // Get profile by userIdFromToken
            function (next) {
                try {
                    return clientSession.getConnectionService().getProfileService().getUserProfile({
                        userId: userIdFromToken,
                        clientInfo: clientSession.getClientInfo()
                    }, undefined, function (err, message) {
                        if (err || (message && message.getError())) {
                            return next(err || message.getError());
                        }
                        // Skip merge process if no destination profile found
                        if (!_.has(message.getContent(), 'profile')) {
                            return next();
                        }
                        // Update source profile: removes all duplicated and overlapping roles
                        profileFromToken = _.cloneDeep(message.getContent().profile);
                        profileFromToken.roles = RoleService.excludeDuplicatesFromRight(profile.roles, profileFromToken.roles);
                        if (_.isEqual(profileFromToken.roles, message.getContent().profile.roles)) {
                            return next();
                        }
                        return clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: userIdFromToken,
                            profile: profileFromToken,
                            clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    });
                } catch (ex) {
                    logger.error('ProfileService.merge: profile.updateUserProfile [userIdFromToken]', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
            // Newer user should always be merged into the older user
            function (next) {
                try {
                    if (!profileFromToken) {
                        return setImmediate(next);
                    }
                    if (profileFromToken.createDate && profileFromProfile.createDate && profileFromToken.createDate < profileFromProfile.createDate) {
                        sourceUserId = userIdFromProfile;
                        targetUserId = userIdFromToken;
                    }
                    return setImmediate(next);
                } catch (ex) {
                    logger.error('ProfileService.merge: detect merge target', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
            // Remove auth blob from source profile
            function (next) {
                try {
                    if (!profileFromToken) {
                        return setImmediate(next);
                    }
                    try {
                        return clientSession.getConnectionService().getProfileService().updateUserProfileBlob({
                            userId: userIdFromToken,
                            name: 'auth',
                            value: null,
                            clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('ProfileService.merge: profile.updateUserProfileBlob [userIdFromToken]', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                } catch (ex) {
                    logger.error('ProfileService.merge: profile.updateUserProfileBlob [userIdFromToken]', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
            // Clone profile picture from source to target (if not already exist)
            function (next) {
                try {
                    if (!profileFromToken || !token) {
                        return setImmediate(next);
                    }
                    return self.profileHasPhoto(userIdFromProfile, function (err, result) {
                        if (err) {
                            return next(err);
                        }
                        var profileHasPhotoFromToken = false;
                        try {
                            if (_.has(profileFromToken, 'profilePhotoId') && profileFromToken.profilePhotoId) {
                                var parsedUrl = url.parse(profileFromToken.profilePhotoId);
                                profileHasPhotoFromToken = _.isObject(parsedUrl);
                            }
                        } catch (ex) {
                            profileHasPhotoFromToken = false;
                        }
                        if (!result && profileHasPhotoFromToken) {
                            return self.uploadPicture(profileFromToken, profileFromToken.profilePhotoId, clientSession, token, next);
                        }
                        return next();
                    });
                } catch (ex) {
                    logger.error('ProfileService.merge: profileHasPhoto', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
            // Merge source profile into target profile
            function (next) {
                try {
                    if (!profileFromToken) {
                        return setImmediate(next);
                    }
                    clientSession.getConnectionService().getProfileService().mergeProfile({
                        sourceUserId: sourceUserId,
                        targetUserId: targetUserId,
                        clientInfo: clientSession.getClientInfo()
                    }, undefined, function (err, message) {
                        if (err || (message && message.getError())) {
                            return next(err || message.getError());
                        }
                        return next();
                    });
                } catch (ex) {
                    logger.error('ProfileService.merge: mergeProfile', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
        ], function (err) {
            if (err) {
                return callback(err);
            }
            return callback(null, !_.isUndefined(profileFromToken));
        });
    },

    /**
     * Update profile data requested from provider
     * Upload profile photo if present
     * @param provider
     * @param providerData
     * @param profile
     * @param clientSession
     * @param token
     * @param callback
     * @returns
     */
    update: function (provider, providerData, profile, clientSession, token, callback) {
        var self = this;
        var fieldMappers = {
            facebook: function (provider, profile, callback) {
                try {
                    // map of FB User IDs linked to FB App IDs
                    var facebookIds = [];
                    if (_.isObject(provider.ids_for_business) && _.isArray(provider.ids_for_business.data)) {
                        _.forEach(provider.ids_for_business.data, function (item) {
                            var appId = '';
                            if (_.isObject(item.app) && _.has(item.app, 'id')) {
                                appId = item.app.id;
                            }
                            facebookIds.push({ facebookUserId: item.id.toString(), facebookAppId: appId.toString() });
                        });
                    } else if (_.has(provider, 'id')) {
                        facebookIds.push({ facebookUserId: provider.id.toString(), facebookAppId: '' });
                    }
                    profile.facebookIds = facebookIds;
                    // email
                    if (_.has(provider, 'email')) {
                        if (!_.has(profile, 'emails')) {
                            profile.emails = [];
                        }
                        var existingEmailIndex = _.findIndex(profile.emails, { email: provider.email });
                        if (existingEmailIndex === -1) {
                            profile.emails.push({ email: provider.email, verificationStatus: 'verified' });
                        } else {
                            profile.emails[existingEmailIndex] = { email: provider.email, verificationStatus: 'verified' };
                        }
                    }
                    // first_name
                    if (_.has(provider, 'first_name')) {
                        if (!_.has(profile, 'person')) {
                            profile.person = {};
                        }
                        profile.person.firstName = provider.first_name;
                    }
                    // last_name
                    if (_.has(provider, 'last_name')) {
                        if (!_.has(profile, 'person')) {
                            profile.person = {};
                        }
                        profile.person.lastName = provider.last_name;
                    }
                    // birthday
                    if (_.has(provider, 'birthday')) {
                        if (!_.has(profile, 'person')) {
                            profile.person = {};
                        }
                        var date = provider.birthday.split('/');
                        profile.person.birthDate = date[2] + '-' + date[1] + '-' + date[0] + 'T00:00:00Z';
                    }
                    // gender
                    if (_.has(provider, 'gender')) {
                        if (!_.has(profile, 'person')) {
                            profile.person = {};
                        }
                        profile.person.sex = provider.gender.substring(0, 1).toUpperCase();
                    }
                    // locale
                    if (_.has(provider, 'locale')) {
                        profile.language = provider.locale.substring(0, 2).toLowerCase();
                    }
                    // location
                    if (_.has(provider, 'location') && _.has(provider.location, 'location') && _.isObject(provider.location.location)) {
                        if (!_.has(profile, 'address')) {
                            profile.address = {};
                        }
                        if (_.has(provider.location.location, 'city')) {
                            profile.address.city = provider.location.location.city;
                        }
                        if (_.has(provider.location.location, 'country_code')) {
                            profile.address.country = provider.location.location.country_code.toUpperCase();
                        }
                    }
                    // picture
                    if (_.has(provider, 'picture') && (!profile.picture || _.isEmpty(profile.picture))) {
                        return self.uploadPicture(profile, provider.picture, clientSession, token, callback);
                    }
                    return setImmediate(callback, null, profile);
                } catch (ex) {
                    logger.error('ProfileService.update: update facebook', ex);
                    return setImmediate(callback, ex);
                }
            },
            google: function (provider, profile, callback) {
                try {
                    // email
                    if (_.has(provider, 'email')) {
                        if (!_.has(profile, 'emails')) {
                            profile.emails = [];
                        }
                        var existingEmailIndex = _.findIndex(profile.emails, { email: provider.email });
                        if (existingEmailIndex === -1) {
                            profile.emails.push({ email: provider.email, verificationStatus: 'verified' });
                        } else {
                            profile.emails[existingEmailIndex] = { email: provider.email, verificationStatus: 'verified' };
                        }
                    }
                    // emails
                    if (_.has(provider, 'emails')) {
                        _.forEach(provider.emails, function (emailEntry) {
                            if (_.has(provider, 'email')) {
                                if (!_.has(profile, 'emails')) {
                                    profile.emails = [];
                                }
                                var existingEmailIndex = _.findIndex(profile.emails, { email: emailEntry.value });
                                if (existingEmailIndex === -1) {
                                    profile.emails.push({ email: emailEntry.value, verificationStatus: 'verified' });
                                } else {
                                    profile.emails[existingEmailIndex] = { email: emailEntry.value, verificationStatus: 'verified' };
                                }
                            }
                        });
                    }
                    // name
                    if (_.has(provider, 'name')) {
                        if (!_.has(profile, 'person')) {
                            profile.person = {};
                        }
                        profile.person.firstName = provider.name.givenName;
                        profile.person.lastName = provider.name.familyName;
                    }
                    // gender
                    if (_.has(provider, 'gender')) {
                        if (!_.has(profile, 'person')) {
                            profile.person = {};
                        }
                        profile.person.sex = provider.gender.substring(0, 1).toUpperCase();
                    }
                    // language
                    if (_.has(provider, 'language')) {
                        profile.language = provider.language.substring(0, 2).toLowerCase();
                    }
                    // picture
                    if (_.has(provider, 'picture') && (!profile.picture || _.isEmpty(profile.picture))) {
                        return self.uploadPicture(profile, provider.picture, clientSession, token, callback);
                    }
                    return setImmediate(callback, null, profile);
                } catch (ex) {
                    logger.error('ProfileService.update: update google', ex);
                    return setImmediate(callback, ex);
                }
            }
        };
        if (!profile) {
            profile = {};
        }
        return fieldMappers[provider](providerData, profile, callback);
    },

    /**
     * Calls Media service client to upload profile picture and returns updated profile
     * @param profile Profile data
     * @param picture Picture URL requested from provider
     * @param clientSession
     * @param token Token required by Media service
     * @param callback
     */
    uploadPicture: function (profile, picture, clientSession, token, callback) {
        var self = this;
        var pictureUrl = picture;
        try {
            if (_.isObject(picture)) {
                if (_.has(picture, 'data') && _.has(picture.data, 'is_silhouette') && picture.data.is_silhouette === false && _.has(picture.data, 'url') && !_.isEmpty(picture.data.url)) {
                    pictureUrl = picture.data.url;
                } else {
                    logger.debug('ProfileService.uploadPicture: picture upload failed', JSON.stringify(picture));
                    return setImmediate(callback, null, profile);
                }
            }
            var params = {
                userId: profile.userId,
                action: 'update',
                data: pictureUrl,
                waitResponse: false
            };
            return clientSession.getConnectionService().getMediaService().updateProfilePicture(params, function (err, message) {
                if (err || (message && message.getError())) {
                    // If any error, log and simply skip picture setup
                    logger.error('ProfileService.uploadPicture: updateProfilePicture error', pictureUrl, err || (message && message.getError()));
                } else {
                    profile.profilePhotoId = pictureUrl;
                }
                return setImmediate(callback, null, profile);
            });
        } catch (ex) {
            logger.error('ProfileService.uploadPicture', pictureUrl, ex);
            return setImmediate(callback, ex);
        }
    },

    /**
     * Checks if profile picture already exists for profile: "<Profile ID>.jpg" in S3
     * @param userId Profile ID
     * @param callback
     */
    profileHasPhoto: function (userId, callback) {
        var self = this;
        try {
            return self.cdnStorage.listObjects({
                Prefix: 'profile/' + userId + '.jpg'
            }, function (err, result) {
                if (err) {
                    return callback(err);
                }
                return callback(null, result.Contents.length && result.Contents.length > 0);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    excludeEmail: function (userId, profile, email, clientSession, callback) {
        if (!_.has(profile, 'emails') || _.findIndex(profile.emails, { email: email }) === -1) {
            return setImmediate(callback);
        }
        // Downgrade role to ANONYMOUS if NOT_VALIDATED detected and there are no other login info
        if (RoleService.isNotValidatedUser(profile.roles) && _.isEmpty(profile.phones) && !profile.facebook && !profile.google) {
            _.remove(profile.roles, AerospikeUser.ROLE_NOT_VALIDATED);
            profile.roles.push(AerospikeUser.ROLE_ANONYMOUS);
            profile.roles = RoleService.excludeDuplicates(profile.roles);
        }
        _.remove(profile.emails, function (emailEntry) { return emailEntry.email === email });
        return clientSession.getConnectionService().getProfileService().updateUserProfile({
            userId: userId,
            profile: profile,
            //clientInfo: clientSession.getClientInfo()
        }, undefined, function (err, message) {
            if (err || (message && message.getError())) {
                return callback(err || message.getError());
            }
            return callback();
        });
    }
};