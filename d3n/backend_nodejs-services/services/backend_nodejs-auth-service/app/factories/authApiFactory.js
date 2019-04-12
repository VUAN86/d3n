var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ApiFactory = require('./apiFactory.js');
var RoleService = require('./../services/roleService.js');
var ProfileService = require('./../services/profileService.js');
var ProviderService = require('./../services/providerService.js');
var JwtUtils = require('./../utils/jwtUtils.js');
var logger = require('nodejs-logger')();
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;

module.exports = {

    authEmail: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileBlob = {};
            var userIdFromToken = null;
            var userIdFromProfile = null;
            var newToken = null;
            async.series([
                // Get userId from token, if it is passed
                function (next) {
                    try {
                        if (clientSession.isAuthenticated()) {
                            userIdFromToken = clientSession.getUserId();
                        }
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.TokenNotValid);
                    }
                },
                // Try to find already existing profile by email
                function (next) {
                    try {
                        // Profile service: call findByIdentifier
                        clientSession.getConnectionService().getProfileService().findByIdentifier({
                            identifierType: 'EMAIL',
                            identifier: params.email,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'userId')) {
                                    // return next(Errors.AuthApi.NotRegistered);
                                    return next(Errors.AuthApi.SecretWrong);
                                }
                                userIdFromProfile = message.getContent().userId;
                                return next();
                            } catch (ex) {
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check email - should be verified
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromProfile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            if (!_.has(message.getContent(), 'profile') || !_.has(message.getContent().profile, 'emails')) {
                                return next(Errors.AuthApi.InvalidEmail);
                            }
                            var notVerifiedEmail = _.find(message.getContent().profile.emails, {
                                email: params.email
                            });
                            if (!notVerifiedEmail) {
                                return next(Errors.AuthApi.InvalidEmail);
                            }
                            profile = message.getContent().profile;
                            if (notVerifiedEmail.verificationStatus === 'notVerified' && !_.includes(profile.roles, AerospikeUser.ROLE_NOT_VALIDATED)) {
                                return next(Errors.AuthApi.NotVerified);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile blob
                function (next) {
                    // Profile service: call getUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().getUserProfileBlob({
                                userId: userIdFromProfile,
                                name: 'auth',
                                //clientInfo: clientSession.getClientInfo()
                            }, undefined, function (err, message) {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                profileBlob = message.getContent().value;
                                return next();
                            });
                        } catch (ex) {
                            logger.error('authApiFactory.authEmail: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Validate password
                function (next) {
                    try {
                        var passwordEncrypted = JwtUtils.encodePass(params.password);
                        if (!profileBlob || passwordEncrypted !== profileBlob.password) {
                            return next(Errors.AuthApi.SecretWrong);
                        }
                        return next();
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: validate password', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Generate new token
                function (next) {
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // In case when userId from token and profile are different:
                //  - update profile with userId from token (only if it differs from userId searched by email): 
                //  - update global role to be always top most: e.g. if there are REGISTERED+ANONYMOUS, then get rid of ANONYMOUS
                //  - remove possible dublicated tenant roles
                //  - remove authorization blob
                //  - merge accounts: userIdFromToken as source -> userIdFromProfile as target
                function (next) {
                    try {
                        if (!userIdFromToken || userIdFromProfile === userIdFromToken) {
                            return next();
                        }
                        return ProfileService.merge(userIdFromToken, userIdFromProfile, profile, clientSession, newToken, function (err, merged) {
                            if (err) {
                                return next(err);
                            }
                            if (!merged) {
                                userIdFromToken = null;
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: merge profiles', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update AerospikeUserToken for userIdFromProfile
                function (next) {
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update AerospikeUserToken for userIdFromToken if it differs from userIdFromProfile
                function (next) {
                    if (_.isNull(userIdFromToken) || userIdFromProfile === userIdFromToken) {
                        return next();
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromToken, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Publish TOKEN_INVALIDATED event with new token
                function (next) {
                    try {
                        return clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: event.publish', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ token: newToken }, callback);
            });
        } catch (ex) {
            return ApiFactory.callbackError(ex, callback);
        }
    },

    authPhone: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileBlob = {};
            var userIdFromToken = null;
            var userIdFromProfile = null;
            var newToken = null;
            async.series([
                // Get userId from token
                function (next) {
                    try {
                        if (clientSession.isAuthenticated()) {
                            userIdFromToken = clientSession.getUserId();
                        }
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.TokenNotValid);
                    }
                },
                // Try to find already existing profile by phone
                function (next) {
                    try {
                        // Profile service: call findByIdentifier
                        clientSession.getConnectionService().getProfileService().findByIdentifier({
                            identifierType: 'PHONE',
                            identifier: params.phone,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'userId')) {
                                    // return next(Errors.AuthApi.NotRegistered);
                                    return next(Errors.AuthApi.SecretWrong);
                                }
                                userIdFromProfile = message.getContent().userId;
                                return next();
                            } catch (ex) {
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check phone - should be verified
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromProfile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            if (!_.has(message.getContent(), 'profile') || !_.has(message.getContent().profile, 'phones')) {
                                return next(Errors.AuthApi.InvalidPhone);
                            }
                            var notVerifiedPhone = _.find(message.getContent().profile.phones, {
                                phone: params.phone
                            });
                            if (!notVerifiedPhone) {
                                return next(Errors.AuthApi.InvalidPhone);
                            }
                            profile = message.getContent().profile;
                            if (notVerifiedPhone.verificationStatus === 'notVerified' && !_.includes(profile.roles, AerospikeUser.ROLE_NOT_VALIDATED)) {
                                return next(Errors.AuthApi.NotVerified);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile blob
                function (next) {
                    // Profile service: call getUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().getUserProfileBlob({
                                userId: userIdFromProfile,
                                name: 'auth',
                                //clientInfo: clientSession.getClientInfo()
                            }, undefined, function (err, message) {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                profileBlob = message.getContent().value;
                                return next();
                            });
                        } catch (ex) {
                            logger.error('authApiFactory.authPhone: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Validate password
                function (next) {
                    try {
                        var passwordEncrypted = JwtUtils.encodePass(params.password);
                        if (!profileBlob || passwordEncrypted !== profileBlob.password) {
                            return next(Errors.AuthApi.SecretWrong);
                        }
                        return next();
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: validate password', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Generate new token
                function (next) {
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // In case when userId from token and profile are different:
                //  - update profile with userId from token (only if it differs from userId searched by email): 
                //  - update global role to be always top most: e.g. if there are REGISTERED+ANONYMOUS, then get rid of ANONYMOUS
                //  - remove possible dublicated tenant roles
                //  - remove authorization blob
                //  - merge accounts: userIdFromToken as source -> userIdFromProfile as target
                function (next) {
                    try {
                        if (_.isNull(userIdFromToken) || userIdFromProfile === userIdFromToken) {
                            return next();
                        }
                        return ProfileService.merge(userIdFromToken, userIdFromProfile, profile, clientSession, newToken, function (err, merged) {
                            if (err) {
                                return next(err);
                            }
                            if (!merged) {
                                userIdFromToken = null;
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: merge profiles', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update AerospikeUserToken for userIdFromProfile
                function (next) {
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update AerospikeUserToken for userIdFromToken if it differs from userIdFromProfile
                function (next) {
                    if (_.isNull(userIdFromToken) || userIdFromProfile === userIdFromToken) {
                        return next();
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromToken, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Publish TOKEN_INVALIDATED event with new token
                function (next) {
                    try {
                        return clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: event.publish', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ token: newToken }, callback);
            });
        } catch (ex) {
            return ApiFactory.callbackError(ex, callback);
        }
    },

    authFacebook: function (params, clientSession, callback) {
        try {
            params.provider = 'facebook';
            params.providerToken = params.facebookToken;
            return this._authProvider(params, clientSession, callback);
        } catch (ex) {
            logger.error('authApiFactory.authFacebook', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    authGoogle: function (params, clientSession, callback) {
        try {
            params.provider = 'google';
            params.providerToken = params.googleToken;
            return this._authProvider(params, clientSession, callback);
        } catch (ex) {
            logger.error('authApiFactory.authGoogle', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    _authProvider: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileVerifiedEmail;
            var userIdFromToken = null;
            var userIdFromProfile = null;
            var newToken = null;
            async.series([
                // Get userId from token
                function (next) {
                    try {
                        if (clientSession.isAuthenticated()) {
                            userIdFromToken = clientSession.getUserId();
                        }
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.TokenNotValid);
                    }
                },
                // Verify provider token and get unique provider ID
                function (next) {
                    try {
                        ProviderService.getProviderData(params.provider, params.providerToken, false, function (err, data) {
                            try {
                                if (err) {
                                    return next(err);
                                }
                                params.providerToken = data.uniqueId;
                                params.providerEmail = data.email;
                                return next();
                            } catch (ex) {
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authProvider: getProfileUniqueId', ex);
                        return next(Errors.AuthApi.ProviderServiceFatalError);
                    }
                },
                // Try to find already existing profile by provider token
                function (next) {
                    try {
                        // Profile service: call findByIdentifier
                        clientSession.getConnectionService().getProfileService().findByIdentifier({
                            identifierType: params.provider.toUpperCase(),
                            identifier: params.providerToken,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'userId')) {
                                    // return next(Errors.AuthApi.NotRegistered);
                                    return next(Errors.AuthApi.SecretWrong);
                                }
                                userIdFromProfile = message.getContent().userId;
                                return next();
                            } catch (ex) {
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authProvider: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromProfile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            if (!_.has(message.getContent(), 'profile')) {
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            profile = message.getContent().profile;
                            profileVerifiedEmail = _.find(profile.emails, {
                                email: params.providerEmail,
                                verificationStatus: 'verified'
                            });
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Validate provider token, should pass or in case if there are multiple tokens (temporary solution), email should pass
                function (next) {
                    try {
                        if (profile[params.provider] == params.providerToken) {
                            return next();
                        }
                        if (params.providerEmail === profileVerifiedEmail) {
                            return next();
                        }
                        return next(Errors.AuthApi.SecretWrong);
                    } catch (ex) {
                        logger.error('authApiFactory.authProvider: validate provider token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // In case when userId from token and profile are different:
                //  - check both profiles, there should not be already another facebook or google entry
                //  - update profile with userId from token (only if it differs from userId searched by email):
                //  - update global role to be always top most: e.g. if there are REGISTERED+ANONYMOUS, then get rid of ANONYMOUS
                //  - remove possible dublicated tenant roles
                //  - remove authorization blob
                //  - merge accounts: userIdFromToken as source -> userIdFromProfile as target
                function (next) {
                    try {
                        if (_.isNull(userIdFromToken) || userIdFromProfile === userIdFromToken) {
                            return next();
                        }
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromToken,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            if (!_.has(message.getContent(), 'profile')) {
                                userIdFromToken = null;
                                return next();
                            }
                            var profileFromToken = message.getContent().profile;
                            var profileFromTokenVerifiedEmail = _.find(profileFromToken.emails, {
                                email: params.providerEmail,
                                verificationStatus: 'verified'
                            });
                            // Single profile cannot have multiple facebook accounts (for different emails)
                            if (profile[params.provider] && _.has(profileFromToken, params.provider) && profileFromToken[params.provider] &&
                                !_.isEqual(profile[params.provider], profileFromToken[params.provider]) &&
                                ((profileVerifiedEmail && params.providerEmail !== profileVerifiedEmail.email) || (profileFromTokenVerifiedEmail && params.providerEmail !== profileFromTokenVerifiedEmail.email))) {
                                if (params.provider === 'facebook') {
                                    return next(Errors.AuthApi.MultipleFacebook);
                                } else if (params.provider === 'google') {
                                    return next(Errors.AuthApi.MultipleGoogle);
                                }
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token
                function (next) {
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // If there is already account with verified email from provider, then merge to it (source: anonymous user)
                function (next) {
                    try {
                        if (_.isNull(userIdFromToken) || userIdFromProfile === userIdFromToken) {
                            return next();
                        }
                        // Anonymous -> Registered only merge allowed for provider authorization
                        if (!RoleService.isAnonymousUser(clientSession.getRoles())) {
                            return next(Errors.AuthApi.AnonymousOnlyMergeAllowed);
                        }
                        return ProfileService.merge(userIdFromToken, userIdFromProfile, profile, clientSession, newToken, function (err, merged) {
                            if (err) {
                                return next(err);
                            }
                            if (!merged) {
                                userIdFromToken = null;
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authProvider: merge profiles', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update AerospikeUserToken for userIdFromProfile
                function (next) {
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.authProvider: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update AerospikeUserToken for userIdFromToken if it differs from userIdFromProfile
                function (next) {
                    if (_.isNull(userIdFromToken) || userIdFromProfile === userIdFromToken) {
                        return next();
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromToken, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.authProvider: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Publish TOKEN_INVALIDATED event with new token
                function (next) {
                    try {
                        return clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authProvider: event.publish', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ token: newToken }, callback);
            });
        } catch (ex) {
            return ApiFactory.callbackError(ex, callback);
        }
    },

    refresh: function (params, clientSession, callback) {
        var profile = {};
        var userIdFromToken = null;
        var newToken = null;
        try {
            async.series([
                // Get userId from token
                function (next) {
                    try {
                        userIdFromToken = clientSession.getUserId();
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.TokenNotValid);
                    }
                },
                // Get registered profile
                function (next) {
                    try {
                        // Profile service: call getProfile
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromToken,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'profile')) {
                                    // return next(Errors.AuthApi.NotRegistered);
                                    return next(Errors.AuthApi.SecretWrong);
                                }
                                profile = message.getContent().profile;
                                return next();
                            } catch (ex) {
                                logger.error('authApiFactory.refresh: get registered profile', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.refresh: profile.getProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userId from profile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: profile.userId, roles: profile.roles });
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: profile.userId, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.refresh: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Publish TOKEN_INVALIDATED event with new token
                function (next) {
                    try {
                        return clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.refresh: event.publish', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ token: newToken }, callback);
            });
        } catch (ex) {
            return ApiFactory.callbackError(ex, callback);
        }
    },

    getPublicKey: function (params, clientSession, callback) {
        try {
            var publicKey = JwtUtils.getPublicKey();
            return ApiFactory.callbackSuccess({ publicKey: publicKey }, callback);
        } catch (ex) {
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    changePassword: function (params, clientSession, callback) {
        var sendEmail = false;
        var sendSms = false;
        try {
            // Check old and new passwords: they must differ
            try {
                if (params.newPassword === params.oldPassword) {
                    return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
                }
            } catch (ex) {
                return ApiFactory.callbackError(ex, callback);
            }
            var profile = {};
            var profileBlob = {};
            var userIdFromToken = null;
            async.series([
                // Get userId from token
                function (next) {
                    try {
                        userIdFromToken = clientSession.getUserId();
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.TokenNotValid);
                    }
                },
                // Get profile
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromToken,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            profile = message.getContent().profile;
                            if (_.has(message.getContent().profile, 'emails')) {
                                _.forEach(message.getContent().profile.emails, function (email) {
                                    if (email.verificationStatus === 'verified') {
                                        sendEmail = true;
                                    }
                                });
                            }
                            if (!sendEmail && _.has(message.getContent().profile, 'phones')) {
                                _.forEach(message.getContent().profile.phones, function (phone) {
                                    if (phone.verificationStatus === 'verified') {
                                        sendSms = true;
                                    }
                                });
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.registerEmailWithoutPassword: profile.getProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile blob
                function (next) {
                    // Profile service: call getUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().getUserProfileBlob({
                                userId: userIdFromToken,
                                name: 'auth',
                                //clientInfo: clientSession.getClientInfo()
                            }, undefined, function (err, message) {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                profileBlob = message.getContent().value;
                                return next();
                            });
                        } catch (ex) {
                            logger.error('authApiFactory.changePassword: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Check old password, update profile blob: set new password
                function (next) {
                    try {
                        // Check old password
                        var oldPasswordEncrypted = JwtUtils.encodePass(params.oldPassword);
                        if (!profileBlob || oldPasswordEncrypted !== profileBlob.password) {
                            return next(Errors.AuthApi.SecretWrong);
                        }
                        // Encrypt new password with SHA256 and update profile BLOB
                        profileBlob.password = JwtUtils.encodePass(params.newPassword);
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Profile service: call updateUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().updateUserProfileBlob({
                                userId: userIdFromToken,
                                name: 'auth',
                                value: profileBlob,
                                //clientInfo: clientSession.getClientInfo()
                            }, undefined, function (err, message) {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                return next();
                            });
                        } catch (ex) {
                            logger.error('authApiFactory.changePassword: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending email
                function (next) {
                    if (!sendEmail) {
                        return next();
                    }
                    try {
                        var sendEmailParams = {
                            userId: userIdFromToken,
                            address: null,
                            subject: Config.userMessage.authChangePasswordEmail.subject,
                            message: Config.userMessage.authChangePasswordEmail.message,
                            subject_parameters: null,
                            message_parameters: null,
                            waitResponse: Config.umWaitForResponse,
                            clientInfo: clientSession.getClientInfo()
                        };
                        clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.changePassword: userMessage.sendEmail', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Notify user by sending sms
                function (next) {
                    if (!sendSms) {
                        return next();
                    }
                    try {
                        var sendSmsParams = {
                            userId: userIdFromToken,
                            phone: null,
                            message: Config.userMessage.authChangePasswordSms.message,
                            message_parameters: null,
                            waitResponse: Config.umWaitForResponse,
                            clientInfo: clientSession.getClientInfo()
                        };
                        clientSession.getConnectionService().getUserMessage().sendSms(sendSmsParams, function (err, message) {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                return next();
                            });
                    } catch (ex) {
                        logger.error('authApiFactory.changePassword: userMessage.sendSms', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return ApiFactory.callbackError(ex, callback);
        }
    },

    generateImpersonateToken: function (params, clientSession, callback) {
        try {
            var profile = {};
            var userId = null;
            var userRoles = [];
            var newToken = null;
            async.series([
                // Try to find already existing profile by email
                function (next) {
                    try {
                        // Profile service: call findByIdentifier
                        clientSession.getConnectionService().getProfileService().findByIdentifier({
                            identifierType: 'EMAIL',
                            identifier: params.email,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'userId')) {
                                    // return next(Errors.AuthApi.NotRegistered);
                                    return next(Errors.AuthApi.SecretWrong);
                                }
                                userId = message.getContent().userId;
                                return next();
                            } catch (ex) {
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.impersonateToken: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check email - should be verified
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userId,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                if (!_.has(message.getContent(), 'profile') || !_.has(message.getContent().profile, 'emails')) {
                                    return next(Errors.AuthApi.InvalidEmail);
                                }
                                var notVerifiedEmail = _.find(message.getContent().profile.emails, {
                                    email: params.email
                                });
                                if (!notVerifiedEmail) {
                                    return next(Errors.AuthApi.InvalidEmail);
                                }
                                profile = message.getContent().profile;
                                if (notVerifiedEmail.verificationStatus === 'notVerified' && !_.includes(profile.roles, AerospikeUser.ROLE_NOT_VALIDATED)) {
                                    logger.error('authApiFactory.impersonateToken: email not valid', profile.emails, 'or account not validated', profile.roles);
                                    return next(Errors.AuthApi.NotVerified);
                                }
                                // Validate that the impersonated user is an admin, will not impersonate other users
                                if (!_.includes(profile.roles, 'TENANT_' + params.tenantId + '_ADMIN')) {
                                    logger.error('authApiFactory.impersonateToken: error impersonating user,', userId, 'not an admin for tenant', params.tenantId, 'available user roles', profile.roles);
                                    return next(Errors.AuthApi.FatalError);
                                }
                                
                                // keep only roles for given tenant and global roles
                                for(var i=0; i<profile.roles.length; i++) {
                                    var role = profile.roles[i];
                                    
                                    if(!_.startsWith(role, 'TENANT_') || _.startsWith(role, 'TENANT_' + params.tenantId + '_')) {
                                        // global role or role for params.tenantId 
                                        userRoles.push(role);
                                        continue;
                                    }
                                }
                                
                                return next();
                                
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.impersonateToken: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token
                function (next) {
                    try {
                        newToken = JwtUtils.encode({ userId: userId, tenantId: params.tenantId, roles: userRoles, impersonate: true }, { expiresIn: Config.jwt.impersonateForSeconds });
                        return next();
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // // Update AerospikeUserToken for userId
                // function (next) {
                //     try {
                //         return AerospikeUserToken.create({ userId: userId, token: newToken }, next);
                //     } catch (ex) {
                //         logger.error('authApiFactory.impersonateToken: AerospikeUserToken.create', ex);
                //         return next(Errors.AuthApi.FatalError);
                //     }
                // },
                // // Publish TOKEN_INVALIDATED event with new token
                // function (next) {
                //     try {
                //         return clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                //             if (err || (message && message.getError())) {
                //                 return next(err || message.getError());
                //             }
                //             return next();
                //         });
                //     } catch (ex) {
                //         logger.error('authApiFactory.impersonateToken: event.publish', ex);
                //         return next(Errors.AuthApi.NoInstanceAvailable);
                //     }
                // },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ token: newToken }, callback);
            });
        } catch (ex) {
            return ApiFactory.callbackError(ex, callback);
        }
    }
};