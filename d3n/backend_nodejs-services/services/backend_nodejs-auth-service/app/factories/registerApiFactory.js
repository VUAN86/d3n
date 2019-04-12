var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ApiFactory = require('./apiFactory.js');
var RoleService = require('./../services/roleService.js');
var AnalyticEventService = require('./../services/analyticEventService.js');
var ProfileService = require('./../services/profileService.js');
var ProviderService = require('./../services/providerService.js');
var JwtUtils = require('./../utils/jwtUtils.js');
var DateUtils = require('nodejs-utils').DateUtils;
var logger = require('nodejs-logger')();
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;

module.exports = {

    registerAnonymous: function (params, clientSession, callback) {
        try {
            var profile = {};
            var userIdFromProfile = undefined;
            var newToken = null;
            async.series([
                // Create anonymous profile
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().createProfile({
                            clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                userIdFromProfile = message.getContent().userId;
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.registerAnonymous: create anonymous profile', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerAnonymous: profile.createProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update anonymous profile
                function (next) {
                    // Profile service: call updateUserProfile
                    try {
                        profile.roles = [AerospikeUser.ROLE_ANONYMOUS];
                        clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: userIdFromProfile,
                            profile: profile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerAnonymous: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromProfile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerAnonymous: generate new token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.registerAnonymous: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
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

    registerEmail: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileBlob = {};
            var userIdFromToken = null;
            var newToken = null;
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
                // Try to find already existing profile by email - should not exist
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
                                if (_.has(message.getContent(), 'userId')) {
                                    return next(Errors.AuthApi.AlreadyRegistered);
                                }
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.registerEmail: findByIdentifier', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile roles
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromToken,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            profile.roles = message.getContent().profile.roles;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: profile.getProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update anonymous profile: 
                // - set email and verification status = 'notVerified'
                // - remove Anonymous role (only if had Anonymous)
                // - add NotValidated role (only if had Anonymous)
                function (next) {
                    // Profile service: call updateUserProfile
                    try {
                        profile.emails = _.union(profile.emails, [
                            {
                                email: params.email,
                                verificationStatus: 'notVerified'
                            }
                        ]);
                        if (_.includes(profile.roles, AerospikeUser.ROLE_ANONYMOUS)) {
                            profile.roles = _.union(profile.roles, [AerospikeUser.ROLE_NOT_VALIDATED]);
                        }
                        profile.roles = RoleService.excludeDuplicates(profile.roles);
                        clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: userIdFromToken,
                            profile: profile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: set verification code and user credentials
                function (next) {
                    // Generate verification code
                    try {
                        profileBlob.code = ApiFactory.generateVerificationCode('email');
                        profileBlob.codeValidUntil = DateUtils.isoFuture(1000 * Config.codeConfirmation.expiresInSeconds);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: generate verification code', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Encrypt password with SHA256
                    try {
                        profileBlob.password = JwtUtils.encodePass(params.password);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: encode password', ex);
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
                            logger.error('registerApiFactory.registerEmail: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending email
                function (next) {
                    try {
                        var sendEmailParams = {
                            userId: userIdFromToken,
                            address: params.email,
                            subject: Config.userMessage.authRegisterEmail.subject,
                            message: Config.userMessage.authRegisterEmail.message,
                            subject_parameters: null,
                            message_parameters: [profileBlob.code],
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
                        logger.error('registerApiFactory.registerEmail: userMessage.sendEmail', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromToken
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromToken, roles: profile.roles });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: new token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromToken, token: newToken }, next);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmail: AerospikeUserToken.create', ex);
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
                        logger.error('registerApiFactory.registerEmail: event.publish', ex);
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
            logger.error('registerApiFactory.registerEmail', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    registerPhone: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileBlob = {};
            var userIdFromToken = null;
            var newToken = null;
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
                // Try to find already existing profile by phone - should not exist
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
                                if (_.has(message.getContent(), 'userId')) {
                                    return next(Errors.AuthApi.AlreadyRegistered);
                                }
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.registerPhone: findByIdentifier', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhone: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile roles
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromToken,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            profile.roles = message.getContent().profile.roles;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhone: profile.getProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update anonymous profile: 
                // - set phone and verification status = 'notVerified'
                // - remove Anonymous role (only if had Anonymous)
                // - add NotValidated role (only if had Anonymous)
                function (next) {
                    // Profile service: call updateUserProfile
                    try {
                        profile.phones = _.union(profile.phones, [
                            {
                                phone: params.phone,
                                verificationStatus: 'notVerified'
                            }
                        ]);
                        if (_.includes(profile.roles, AerospikeUser.ROLE_ANONYMOUS)) {
                            profile.roles = _.union(profile.roles, [AerospikeUser.ROLE_NOT_VALIDATED]);
                        }
                        profile.roles = RoleService.excludeDuplicates(profile.roles);
                        clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: userIdFromToken,
                            profile: profile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhone: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: set verification code and user credentials
                function (next) {
                    // Generate verification code
                    try {
                        profileBlob.code = ApiFactory.generateVerificationCode('phone');
                        profileBlob.codeValidUntil = DateUtils.isoFuture(1000 * Config.codeConfirmation.expiresInSeconds);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhone: generate verification code', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Encrypt password with SHA256
                    try {
                        profileBlob.password = JwtUtils.encodePass(params.password);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhone: encode password', ex);
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
                            logger.error('registerApiFactory.registerPhone: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhone: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending sms
                function (next) {
                    try {
                        var sendSmsParams = {
                            userId: userIdFromToken,
                            phone: params.phone,
                            message: Config.userMessage.authRegisterPhone.message,
                            message_parameters: [profileBlob.code],
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
                        logger.error('registerApiFactory.registerPhone: userMessage.sendSms', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromToken
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromToken, roles: profile.roles });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhone: new token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromToken, token: newToken }, next);
                    } catch (ex) {
                        logger.error('authApiFactory.registerPhone: AerospikeUserToken.create', ex);
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
                        logger.error('registerApiFactory.registerPhone: event.publish', ex);
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
            logger.error('registerApiFactory.registerPhone', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    registerFacebook: function (params, clientSession, callback) {
        try {
            params.provider = 'facebook';
            params.providerToken = params.facebookToken;
            return this._registerProvider(params, clientSession, callback);
        } catch (ex) {
            logger.error('registerApiFactory.registerFacebook', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    registerGoogle: function (params, clientSession, callback) {
        try {
            params.provider = 'google';
            params.providerToken = params.googleToken;
            return this._registerProvider(params, clientSession, callback);
        } catch (ex) {
            logger.error('registerApiFactory.registerGoogle', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    _registerProvider: function (params, clientSession, callback) {
        var profile = {};
        var profileBlob = {};
        var profileVerifiedEmail;
        var profileToUpdate;
        var userIdFromToken = null;
        var userIdFromEmail = null;
        var isAnonymousUser = true;
        var mergeToEmail = false;
        var newToken = null;
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
            // Verify provider token and get unique provider ID
            function (next) {
                try {
                    ProviderService.getProviderData(params.provider, params.providerToken, true, function (err, data) {
                        try {
                            if (err) {
                                logger.error('registerApiFactory.registerProvider: getProfileUniqueId err', err);
                                return next(err);
                            }
                            params.providerToken = data.uniqueId;
                            params.providerEmail = data.email;
                            params.providerData = data.providerData;
                            return next();
                        } catch (ex) {
                            logger.error('registerApiFactory.registerProvider: setup provider token', ex);
                            return next(Errors.AuthApi.FatalError);
                        }
                    });
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: getProfileUniqueId', ex);
                    return next(Errors.AuthApi.ProviderServiceFatalError);
                }
            },
            // Try to find already existing profile by provider token - should not exist
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
                                logger.error('registerApiFactory.registerProvider: findByIdentifier err', err || (message && message.getError()));
                                return next(err || message.getError());
                            }
                            if (_.has(message.getContent(), 'userId')) {
                                return next(Errors.AuthApi.AlreadyRegistered);
                            }
                            return next();
                        } catch (ex) {
                            logger.error('registerApiFactory.registerProvider: findByIdentifier', ex);
                            return next(Errors.AuthApi.FatalError);
                        }
                    });
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: profile.findByIdentifier', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
            // Get profile, setup REGISTERED role
            function (next) {
                try {
                    clientSession.getConnectionService().getProfileService().getUserProfile({
                        userId: userIdFromToken,
                        //clientInfo: clientSession.getClientInfo()
                    }, undefined, function (err, message) {
                        if (err || (message && message.getError())) {
                            logger.error('registerApiFactory.registerProvider: getUserProfile err', err || (message && message.getError()));
                            return next(err || message.getError());
                        }
                        profile = message.getContent().profile;
                        profileVerifiedEmail = _.find(profile.emails, {
                            email: params.providerEmail,
                            verificationStatus: 'verified'
                        });
                        isAnonymousUser = RoleService.isAnonymousUser(profile.roles);
                        profile.roles = _.union(profile.roles, [AerospikeUser.ROLE_REGISTERED]);
                        profile.roles = RoleService.excludeDuplicates(profile.roles);
                        profileToUpdate = profile;
                        return next();
                    });
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: profile.getProfile', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
            // Try to find already existing profile by provider email:
            //  - if existing profile already has different facebook or google account, then return error
            //  - if existing profile is registered and email is verified, then do merge: userIdFromToken as source -> userIdFromEmail as target
            function (next) {
                try {
                    clientSession.getConnectionService().getProfileService().findByIdentifier({
                        identifierType: 'EMAIL',
                        identifier: params.providerEmail,
                        //clientInfo: clientSession.getClientInfo()
                    }, undefined, function (err, message) {
                        try {
                            if (err || (message && message.getError())) {
                                logger.error('registerApiFactory.registerProvider: findByIdentifier [EMAIL] err', err || (message && message.getError()));
                                return next(err || message.getError());
                            }
                            if (_.has(message.getContent(), 'userId')) {
                                userIdFromEmail = message.getContent().userId;
                            }
                            return next();
                        } catch (ex) {
                            logger.error('registerApiFactory.registerProvider: findByIdentifier [EMAIL]', ex);
                            return next(Errors.AuthApi.FatalError);
                        }
                    });
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: profile.findByIdentifier [EMAIL]', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
            function (next) {
                try {
                    if (!userIdFromEmail || userIdFromEmail === userIdFromToken) {
                        return next();
                    }
                    clientSession.getConnectionService().getProfileService().getUserProfile({
                        userId: userIdFromEmail,
                        //clientInfo: clientSession.getClientInfo()
                    }, undefined, function (err, message) {
                        if (err || (message && message.getError())) {
                            logger.error('registerApiFactory.registerProvider: getUserProfile [EMAIL] err', err || (message && message.getError()));
                            return next(err || message.getError());
                        }
                        var profileFromEmail = message.getContent().profile;
                        var profileFromEmailVerifiedEmail = _.find(profileFromEmail.emails, {
                            email: params.providerEmail,
                            verificationStatus: 'verified'
                        });
                        var profileFromEmailNotVerifiedEmail = _.find(profileFromEmail.emails, {
                            email: params.providerEmail,
                            verificationStatus: 'notVerified'
                        });
                        // Single profile cannot have multiple facebook accounts (for different emails)
                        if (profile[params.provider] && _.has(profileFromEmail, params.provider) && profileFromEmail[params.provider] &&
                            !_.isEqual(profile[params.provider], profileFromEmail[params.provider]) &&
                            ((profileVerifiedEmail && params.providerEmail !== profileVerifiedEmail.email) || (profileFromEmailVerifiedEmail && params.providerEmail !== profileFromEmailVerifiedEmail.email))) {
                            if (params.provider === 'facebook') {
                                return next(Errors.AuthApi.MultipleFacebook);
                            } else if (params.provider === 'google') {
                                return next(Errors.AuthApi.MultipleGoogle);
                            }
                        }
                        // If there is already account with verified email from provider, then merge to it (source: anonymous user)
                        if (profileFromEmailVerifiedEmail) {
                            mergeToEmail = true;
                            profileToUpdate = profileFromEmail;
                            // Anonymous -> Registered only merge allowed for provider registration
                            if (!isAnonymousUser) {
                                return next(Errors.AuthApi.AnonymousOnlyMergeAllowed);
                            }
                        } else if (profileFromEmailNotVerifiedEmail) {
                            return ProfileService.excludeEmail(userIdFromEmail, profileFromEmail, params.providerEmail, clientSession, next)
                        }
                        return next();
                    });
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: profile.getProfile [EMAIL]', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
            // Generate new token
            // Update email profile from provider data (if merge detected)
            // Upload email profile photo (if merge detected)
            // Update profile from provider data (if no merge detected)
            // Upload profile photo (if no merge detected)
            // Merge to email profile (if merge detected)
            function (next) {
                try {
                    newToken = JwtUtils.encode({
                        userId: mergeToEmail ? userIdFromEmail : userIdFromToken,
                        roles: profileToUpdate.roles
                    });
                    return next();
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: new token', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
            function (next) {
                try {
                    if (params.provider === 'facebook') {
                        profileToUpdate.facebook = params.providerToken;
                    } else if (params.provider === 'google') {
                        profileToUpdate.google = params.providerToken;
                    }
                    return ProfileService.update(params.provider, params.providerData, profileToUpdate, clientSession, newToken, function (err, updatedProfile) {
                        if (err) {
                            logger.error('registerApiFactory.registerProvider: ProfileService.update', err);
                            return next(err);
                        }
                        // Profile service: call updateUserProfile
                        try {
                            clientSession.getConnectionService().getProfileService().updateUserProfile({
                                userId: mergeToEmail ? userIdFromEmail : userIdFromToken,
                                profile: updatedProfile,
                                //clientInfo: clientSession.getClientInfo()
                            }, undefined, function (err, message) {
                                if (err || (message && message.getError())) {
                                    logger.error('registerApiFactory.registerProvider: updateUserProfile err', err || (message && message.getError()));
                                    return next(err || message.getError());
                                }
                                return next();
                            });
                        } catch (ex) {
                            logger.error('registerApiFactory.registerProvider: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    });
                } catch (ex) {
                    logger.error('authApiFactory.registerProvider: merge profiles [EMAIL]', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
            function (next) {
                try {
                    if (!mergeToEmail) {
                        return next();
                    }
                    return ProfileService.merge(userIdFromToken, userIdFromEmail, profile, clientSession, null, function (err, merged) {
                        if (err) {
                            return next(err);
                        }
                        if (merged) {
                            userIdFromToken = userIdFromEmail;
                        }
                        return next();
                    });
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: new token', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
            // Notify user by sending email
            function (next) {
                try {
                    var sendEmailParams = {
                        userId: userIdFromToken,
                        address: params.providerEmail,
                        subject_parameters: null,
                        message_parameters: null,
                        waitResponse: Config.umWaitForResponse,
                        clientInfo: clientSession.getClientInfo()
                    };
                    if (params.provider === 'facebook') {
                        sendEmailParams.subject = Config.userMessage.authRegisterFacebook.subject;
                        sendEmailParams.message = Config.userMessage.authRegisterFacebook.message;
                    } else if (params.provider === 'google') {
                        sendEmailParams.subject = Config.userMessage.authRegisterGoogle.subject;
                        sendEmailParams.message = Config.userMessage.authRegisterGoogle.message;
                    }
                    clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, function (err, message) {
                        if (err || (message && message.getError())) {
                            logger.error('registerApiFactory.registerProvider: sendEmail err', err || (message && message.getError()));
                            return next(err || message.getError());
                        }
                        return next();
                    });
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: userMessage.sendEmail', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
            // Update AerospikeUserToken for userIdFromToken (standard case) or userIdFromEmail (in case when merge processed)
            function (next) {
                try {
                    return AerospikeUserToken.create({ userId: userIdFromToken, token: newToken }, next);
                } catch (ex) {
                    logger.error('registerApiFactory.registerProvider: AerospikeUserToken.create', ex);
                    return next(Errors.AuthApi.FatalError);
                }
            },
            // Issue REGISTERED analytic event
            function (next) {
                try {
                    var clientInfo = {
                        userId: userIdFromToken,
                        appId: clientSession.getClientInfo().appConfig.appId,
                        tenantId: clientSession.getClientInfo().appConfig.tenantId,
                        sessionIp: clientSession.getClientInfo().ip,
                        sessionId: clientSession.getId()
                    };
                    return AnalyticEventService.issueRegisteredEvent(clientInfo, next);
                } catch (ex) {
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
                    logger.error('registerApiFactory.registerProvider: event.publish', ex);
                    return next(Errors.AuthApi.NoInstanceAvailable);
                }
            },
        ], function (err) {
            if (err) {
                return ApiFactory.callbackError(err, callback);
            }
            return ApiFactory.callbackSuccess({ token: newToken }, callback);
        });
    },

    registerEmailNewCode: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileBlob = {};
            var userIdFromProfile = null;
            var newToken = null;
            async.series([
                // Try to find already existing profile by email
                function (next) {
                    try {
                        // Profile service: call findByIdentifier
                        clientSession.getConnectionService().getProfileService().findByIdentifier({
                            identifierType: 'EMAIL',
                            identifier: params.email
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
                                logger.error('registerApiFactory.registerEmailNewCode: findByIdentifier', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmailNewCode: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check email - should be not verified
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromProfile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            profile.emails = message.getContent().profile.emails;
                            var verifiedEmail = _.find(profile.emails, {
                                email: params.email,
                                verificationStatus: 'verified'
                            });
                            if (verifiedEmail) {
                                return next(Errors.AuthApi.AlreadyVerified);
                            }
                            profile.roles = message.getContent().profile.roles;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmailNewCode: profile.getUserProfile', ex);
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
                            logger.error('registerApiFactory.registerEmailNewCode: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmailNewCode: getUserProfileBlob', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile blob: set new verification code
                function (next) {
                    // Generate new verification code
                    try {
                        profileBlob.code = ApiFactory.generateVerificationCode('email');
                        profileBlob.codeValidUntil = DateUtils.isoFuture(1000 * Config.codeConfirmation.expiresInSeconds);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmailNewCode: update code and date valid until', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Profile service: call updateUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().updateUserProfileBlob({
                                userId: userIdFromProfile,
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
                            logger.error('registerApiFactory.registerEmailNewCode: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmailNewCode: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending email
                function (next) {
                    try {
                        var sendEmailParams = {
                            userId: userIdFromProfile,
                            address: params.email,
                            subject: Config.userMessage.authRegisterEmailNewcode.subject,
                            message: Config.userMessage.authRegisterEmailNewcode.message,
                            subject_parameters: null,
                            message_parameters: [profileBlob.code],
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
                        logger.error('registerApiFactory.registerEmailNewCode: userMessage.sendEmail', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromProfile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmailNewCode: new token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerEmailNewCode: AerospikeUserToken.create', ex);
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
                        logger.error('registerApiFactory.registerEmailNewCode: event.publish', ex);
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
            logger.error('registerApiFactory.registerEmailNewCode', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    registerPhoneNewCode: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileBlob = {};
            var userIdFromProfile = null;
            var newToken = null;
            async.series([
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
                                logger.error('registerApiFactory.registerPhoneNewCode: findByIdentifier', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhoneNewCode: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check phone - should be not verified
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: userIdFromProfile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            profile.phones = message.getContent().profile.phones;
                            var verifiedPhone = _.find(message.getContent().profile.phones, {
                                phone: params.phone,
                                verificationStatus: 'verified'
                            });
                            if (verifiedPhone) {
                                return next(Errors.AuthApi.AlreadyVerified);
                            }
                            profile.roles = message.getContent().profile.roles;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhoneNewCode: profile.getUserProfile', ex);
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
                            logger.error('registerApiFactory.registerPhoneNewCode: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhoneNewCode: getUserProfileBlob', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile blob: set new verification code
                function (next) {
                    // Generate new verification code
                    try {
                        profileBlob.code = ApiFactory.generateVerificationCode('phone');
                        profileBlob.codeValidUntil = DateUtils.isoFuture(1000 * Config.codeConfirmation.expiresInSeconds);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhoneNewCode: update code and date valid until', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Profile service: call updateUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().updateUserProfileBlob({
                                userId: userIdFromProfile,
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
                            logger.error('registerApiFactory.registerPhoneNewCode: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhoneNewCode: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending sms
                function (next) {
                    try {
                        var sendSmsParams = {
                            userId: userIdFromProfile,
                            phone: params.phone,
                            message: Config.userMessage.authRegisterPhoneNewcode.message,
                            message_parameters: [profileBlob.code],
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
                        logger.error('registerApiFactory.registerPhoneNewCode: userMessage.sendSms', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromProfile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhoneNewCode: new token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('registerApiFactory.registerPhoneNewCode: AerospikeUserToken.create', ex);
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
                        logger.error('registerApiFactory.registerPhoneNewCode: event.publish', ex);
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
            logger.error('registerApiFactory.registerPhoneNewCode', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    inviteUserByEmail: function (params, clientSession, callback) {
        try {
            if (!params.emails || params.emails.length === 0) {
                return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
            }
            var profile = {};
            var profileBlob = {};
            var userIdFromProfile = null;
            var generatedPassword = null;
            var newToken = null;
            async.series([
                // Try to find already existing profile by all given emails, should not exist
                function (next) {
                    try {
                        return async.mapSeries(params.emails, function (email, nextEmail) {
                            // Profile service: call findByIdentifier
                            return clientSession.getConnectionService().getProfileService().findByIdentifier({
                                identifierType: 'EMAIL',
                                identifier: email,
                                //clientInfo: clientSession.getClientInfo()
                            }, undefined, function (err, message) {
                                try {
                                    if (err || (message && message.getError())) {
                                        return nextEmail(err || message.getError());
                                    }
                                    if (_.has(message.getContent(), 'userId')) {
                                        return nextEmail(Errors.AuthApi.AlreadyRegistered);
                                    }
                                    return nextEmail();
                                } catch (ex) {
                                    logger.error('registerApiFactory.inviteUserByEmail: findByIdentifier', ex);
                                    return nextEmail(Errors.AuthApi.FatalError);
                                }
                            });
                        }, next);
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Create anonymous profile
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().createProfile({
                            clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                userIdFromProfile = message.getContent().userId;
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.inviteUserByEmail: create anonymous profile', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: profile.createProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update anonymous profile:
                // - set Registered role
                // - set email and verification status = 'verified'
                function (next) {
                    // Profile service: call updateUserProfile
                    try {
                        profile.roles = [AerospikeUser.ROLE_REGISTERED];
                        profile.emails = [];
                        _.forEach(params.emails, function (email) {
                            profile.emails.push({
                                email: email,
                                verificationStatus: 'verified'
                            });
                        });
                        clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: userIdFromProfile,
                            profile: profile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: set verification code and user credentials
                function (next) {
                    // Generate password with fixed salt, encrypt password with SHA256
                    try {
                        generatedPassword = ApiFactory.generatePassword('email');
                        profileBlob.password = JwtUtils.encodePass(JwtUtils.encodePassWithFixedSalt(generatedPassword));
                        profileBlob.code = null;
                        profileBlob.codeValidUntil = null;
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: generate and update password, reset code and date valid until', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Profile service: call updateUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().updateUserProfileBlob({
                                userId: userIdFromProfile,
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
                            logger.error('registerApiFactory.inviteUserByEmail: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending invitation with password to all given emails
                function (next) {
                    try {
                        return async.mapSeries(params.emails, function (email, nextEmail) {
                            var sendEmailParams = {
                                userId: userIdFromProfile,
                                address: email,
                                subject: Config.userMessage.authInviteUserByEmail.subject,
                                message: Config.userMessage.authInviteUserByEmail.message,
                                subject_parameters: null,
                                message_parameters: [email, generatedPassword, params.invitationText, params.invitationPerson],
                                waitResponse: Config.umWaitForResponse,
                                clientInfo: clientSession.getClientInfo()
                            };
                            clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, function (err, message) {
                                    if (err || (message && message.getError())) {
                                        return nextEmail(err || message.getError());
                                    }
                                    return nextEmail();
                                });
                        }, next);
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: userMessage.sendEmail', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Issue REGISTERED analytic event
                function (next) {
                    try {
                        var clientInfo = {
                            userId: userIdFromProfile,
                            appId: clientSession.getClientInfo().appConfig.appId,
                            tenantId: clientSession.getClientInfo().appConfig.tenantId,
                            sessionIp: clientSession.getClientInfo().ip,
                            sessionId: clientSession.getId()
                        };
                        return AnalyticEventService.issueRegisteredEvent(clientInfo, next);
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromProfile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: new token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmail: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ userId: userIdFromProfile, token: newToken }, callback);
            });
        } catch (ex) {
            logger.error('registerApiFactory.inviteUserByEmail', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    inviteUserByEmailAndRole: function (params, clientSession, callback) {
        try {
            var profile = {};
            var profileBlob = {};
            var userIdFromProfile = null;
            var generatedPassword = null;
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
                                if (_.has(message.getContent(), 'userId')) {
                                    return next(Errors.AuthApi.AlreadyRegistered);
                                }
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.inviteUserByEmailAndRole: findByIdentifier', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Create anonymous profile
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().createProfile({
                            clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                userIdFromProfile = message.getContent().userId;
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.inviteUserByEmailAndRole: create anonymous profile', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: profile.createProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update anonymous profile:
                // - set Anonymous role and given role
                // - set email and verification status = 'notVerified'
                function (next) {
                    // Profile service: call updateUserProfile
                    try {
                        profile.roles = [AerospikeUser.ROLE_NOT_VALIDATED, params.role];
                        profile.emails = [
                            {
                                email: params.email,
                                verificationStatus: 'notVerified'
                            }
                        ];
                        profile.person = {
                            firstName: params.profileInfo.firstName,
                            lastName: params.profileInfo.lastName
                        };
                        profile.organization = params.profileInfo.organization;
                        clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: userIdFromProfile,
                            profile: profile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: set verification code and user credentials
                function (next) {
                    // Generate verification code, store null as password (will be generated later and sent through confirmEmail)
                    try {
                        profileBlob.code = ApiFactory.generateVerificationCode('email');
                        profileBlob.codeValidUntil = DateUtils.isoFuture(1000 * Config.codeConfirmation.expiresInSeconds);
                        generatedPassword = ApiFactory.generatePassword('email');
                        profileBlob.password = JwtUtils.encodePass(JwtUtils.encodePassWithFixedSalt(generatedPassword));
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: generate verification code and date valid until', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Profile service: call updateUserProfileBlob
                    try {
                        try {
                            clientSession.getConnectionService().getProfileService().updateUserProfileBlob({
                                userId: userIdFromProfile,
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
                            logger.error('registerApiFactory.inviteUserByEmailAndRole: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending email
                function (next) {
                    try {
                        var sendEmailParams = {
                            userId: userIdFromProfile,
                            address: params.email,
                            subject: Config.userMessage.authInviteUserByEmailAndRole.subject,
                            message: Config.userMessage.authInviteUserByEmailAndRole.message,
                            subject_parameters: null,
                            message_parameters: [params.role, params.email, generatedPassword, profileBlob.code],
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
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: userMessage.sendEmail', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromProfile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: new token', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        return AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, next);
                    } catch (ex) {
                        logger.error('registerApiFactory.inviteUserByEmailAndRole: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ token: newToken }, callback);
            });
        } catch (ex) {
            logger.error('registerApiFactory.inviteUserByEmailAndRole', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    addConfirmedUser: function (params, message, clientSession, callback) {
        try {
            if (!params.email) {
                return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
            }
            var userIdEmail = false;
            var userIdPhone = false;
            var userId = false;
            var newProfile = false;
            var profile = {};
            var profileBlob = {};
            var generatedPassword = null;
            async.series([
                // Try to find already existing profile by email
                function (next) {
                    try {
                        return clientSession.getConnectionService().getProfileService().findByIdentifier({
                            identifierType: 'EMAIL',
                            identifier: params.email,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                if (_.has(message.getContent(), 'userId')) {
                                    userIdEmail = message.getContent().userId;
                                }
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.addConfirmedUser: findByIdentifier(email)', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.addConfirmedUser: profile.findByIdentifier(email)', params, ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                function (next) {
                    try {
                        if (_.has(params, 'phone') && params.phone.length > 0) {
                            // Profile service: call findByIdentifier
                            return clientSession.getConnectionService().getProfileService().findByIdentifier({
                                identifierType: 'PHONE',
                                identifier: params.phone,
                                //clientInfo: clientSession.getClientInfo()
                            }, undefined, function (err, message) {
                                try {
                                    if (err || (message && message.getError())) {
                                        return next(err || message.getError());
                                    }
                                    if (_.has(message.getContent(), 'userId')) {
                                        userIdPhone = message.getContent().userId;
                                    }
                                    return next();
                                } catch (ex) {
                                    logger.error('registerApiFactory.addConfirmedUser: findByIdentifier(phone)', params, ex);
                                    return next(Errors.AuthApi.FatalError);
                                }
                            });
                        } else {
                            return setImmediate(next);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.addConfirmedUser: profile.findByIdentifier(phone)', params, ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },

                // If the email and the phone belongs to different users merge the two
                function (next) {
                    try {
                        if (userIdEmail && userIdPhone && userIdEmail != userIdPhone) {
                            logger.info('registerApiFactory.addConfirmedUser: merge users', userIdEmail, userIdPhone);
                            return ProfileService.merge(userIdPhone, userIdEmail, profile, clientSession, message.getToken(), next);
                        }
                        return setImmediate(next, false);
                    } catch (ex) {
                        logger.error('registerApiFactory.addConfirmedUser: merge profiles [EMAIL]', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },

                // Create profile if no userId
                function (next) {
                    try {
                        // Skip creation of anonymous profile if we already have the userId
                        if (userIdEmail) {
                            userId = userIdEmail;
                            logger.info('registerApiFactory.addConfirmedUser: userId by email found no need to create a new profile', userIdEmail)
                            return setImmediate(next);
                        }

                        if (userIdPhone) {
                            userId = userIdPhone;
                            logger.info('registerApiFactory.addConfirmedUser: userId by phone found no need to create a new profile', userIdPhone)
                            return setImmediate(next);
                        }

                        return clientSession.getConnectionService().getProfileService().createProfile({
                            "email": params.email,
                            "phone": params.phone,
                            "clientInfo": clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                userId = message.getContent().userId;
                                newProfile = true;
                                return setImmediate(next);
                            } catch (ex) {
                                logger.error('registerApiFactory.addConfirmedUser: create anonymous profile', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.addConfirmedUser: profileService.createProfile exception', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },

                // Retrieve profile by userId
                function (next) {
                    try {
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            "userId": userId,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            try {
                                if (err || (message && message.getError())) {
                                    return next(err || message.getError());
                                }
                                profile = message.getContent().profile;
                                return next();
                            } catch (ex) {
                                logger.error('registerApiFactory.addConfirmedUser: retrieve profile by userId', userId, ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.addConfirmedUser: profile.getProfile by userId exception', userId, ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },

                // Update profile:
                // - set Registered role
                // - set email and verification status = 'verified'
                function (next) {
                    try {
                        // Make the user profile registered automatically
                        profile.roles = _.union(profile.roles, [AerospikeUser.ROLE_REGISTERED]);
                        profile.roles = RoleService.excludeDuplicates(profile.roles);

                        // Validate e-mail in the profile configuration
                        if (params.email) {
                            if (!_.isArray(profile.emails)) {
                                profile.emails = [];
                            }

                            var emailFound = false;
                            if (profile.emails.length > 0) {
                                _.forEach(profile.emails, function (value, key) {
                                    if (value.email == params.email) {
                                        emailFound = true;
                                        profile.emails[key].verificationStatus = 'verified';
                                    }
                                });
                            }
                            if (!emailFound) {
                                profile.emails.push({
                                    email: params.email,
                                    verificationStatus: 'verified'
                                });
                            }
                        }

                        // Validate phone in the profile configuration
                        if (params.phone) {
                            if (!_.isArray(profile.phones)) {
                                profile.phones = [];
                            }

                            var phoneFound = false;
                            if (profile.phones.length > 0) {
                                _.forEach(profile.phones, function (value, key) {
                                    if (value.phone == params.phone) {
                                        phoneFound = true;
                                        profile.phones[key].verificationStatus = 'verified';
                                    }
                                });
                            }
                            if (!phoneFound) {
                                profile.phones.push({
                                    phone: params.phone,
                                    verificationStatus: 'verified'
                                });
                            }
                        }

                        if (_.isUndefined(profile.person)) {
                            profile.person = {};
                        }

                        profile.person.firstName = params.firstName;
                        profile.person.lastName = params.lastName;

                        clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: userId,
                            profile: profile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.addConfirmedUser: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },

                // Issue REGISTERED analytic event
                function (next) {
                    try {
                        var clientInfo = {
                            userId: userId,
                            appId: clientSession.getClientInfo().appConfig.appId,
                            tenantId: clientSession.getClientInfo().appConfig.tenantId,
                            sessionIp: clientSession.getClientInfo().ip,
                            sessionId: clientSession.getId()
                        };
                        return AnalyticEventService.issueRegisteredEvent(clientInfo, next);
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },

                // Update new profile blob for new profile, set password
                function (next) {
                    try {
                        if (false === newProfile) {
                            return setImmediate(next);
                        }

                        // Create account and send out e-mail with the password to the user
                        async.series([
                            function (nextNewProfile) {
                                try {
                                    // Update user and send out notification 
                                    generatedPassword = ApiFactory.generatePassword('email');
                                    profileBlob.password = JwtUtils.encodePass(JwtUtils.encodePassWithFixedSalt(generatedPassword));
                                    profileBlob.code = null;
                                    profileBlob.codeValidUntil = null;

                                    clientSession.getConnectionService().getProfileService().updateUserProfileBlob({
                                        userId: userId,
                                        name: 'auth',
                                        value: profileBlob,
                                        //clientInfo: clientSession.getClientInfo()
                                    }, undefined, function (err, message) {
                                        if (err || (message && message.getError())) {
                                            return nextNewProfile(err || message.getError());
                                        }
                                        return nextNewProfile();
                                    });
                                } catch (ex) {
                                    logger.error('registerApiFactory.addConfirmedUser: profile.updateUserProfile', ex);
                                    return nextNewProfile(Errors.AuthApi.NoInstanceAvailable);
                                }
                            },
                            function (nextNewProfile) {
                                try {
                                    var sendEmailParams = {
                                        userId: userId,
                                        address: params.email,
                                        subject: Config.userMessage.authInviteUserByEmail.subject,
                                        message: Config.userMessage.authInviteUserByEmail.message,
                                        subject_parameters: null,
                                        message_parameters: [params.email, generatedPassword, "", "Tenant Manager"],
                                        waitResponse: Config.umWaitForResponse,
                                        clientInfo: clientSession.getClientInfo()
                                    };
                                    clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, function (err, message) {
                                            if (err || (message && message.getError())) {
                                                return nextNewProfile(err || message.getError());
                                            }
                                            return nextNewProfile();
                                        });
                                } catch (ex) {
                                    logger.error('registerApiFactory.addConfirmedUser: userMessage.sendEmail', ex);
                                    return nextNewProfile(Errors.AuthApi.NoInstanceAvailable);
                                }
                            }
                        ], function (err) {
                            return setImmediate(next, err);
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.addConfirmedUser: update new user profile with password', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
            ], function (err) {
                if (err) {
                    return ApiFactory.callbackError(err, callback);
                }
                return ApiFactory.callbackSuccess({ profile: _.pick(profile, ['userId', 'roles', 'person', 'emails', 'phones']) }, callback);
            });
        } catch (ex) {
            logger.error('registerApiFactory.addConfirmedUser', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },
};