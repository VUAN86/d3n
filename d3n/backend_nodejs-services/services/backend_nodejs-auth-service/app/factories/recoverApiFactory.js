var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ApiFactory = require('./apiFactory.js');
var RoleService = require('./../services/roleService.js');
var ProviderService = require('./../services/providerService.js');
var AnalyticEventService = require('./../services/analyticEventService.js');
var JwtUtils = require('./../utils/jwtUtils.js');
var DateUtils = require('nodejs-utils').DateUtils;
var logger = require('nodejs-logger')();
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;

module.exports = {

    recoverPasswordEmail: function (params, clientSession, callback) {
        var profile = {};
        var profileBlob = {};
        var userIdFromProfile = null;
        var newToken = null;
        try {
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
                // Get existing profile, check email - should be registered
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
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            var notVerifiedEmail = _.find(message.getContent().profile.emails, {
                                email: params.email
                            });
                            if (!notVerifiedEmail) {
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            profile = message.getContent().profile;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authEmail: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile blob, check confirmation code
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
                            logger.error('recoverApiFactory.recoverPasswordEmail: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile blob: generate verification code
                function (next) {
                    try {
                        profileBlob.code = ApiFactory.generateVerificationCode('email');
                        profileBlob.codeValidUntil = DateUtils.isoFuture(1000 * Config.codeConfirmation.expiresInSeconds);
                    } catch (ex) {
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
                            logger.error('recoverApiFactory.recoverPasswordEmail: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending email
                function (next) {
                    try {
                        var sendEmailParams = {
                            userId: userIdFromProfile,
                            address: params.email,
                            subject: Config.userMessage.authRecoverPasswordEmail.subject,
                            message: Config.userMessage.authRecoverPasswordEmail.message,
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
                        logger.error('recoverApiFactory.recoverPasswordEmail: userMessage.sendEmail', ex);
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
            logger.error('recoverApiFactory.recoverPasswordEmail', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    recoverPasswordPhone: function (params, clientSession, callback) {
        var profile = {};
        var profileBlob = {};
        var userIdFromProfile = null;
        var newToken = null;
        try {
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
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check phone - should be registered
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
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            var notVerifiedPhone = _.find(message.getContent().profile.phones, {
                                phone: params.phone
                            });
                            if (!notVerifiedPhone) {
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            profile = message.getContent().profile;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('authApiFactory.authPhone: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile blob, check confirmation code
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
                            logger.error('recoverApiFactory.recoverPasswordPhone: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile blob: generate verification code
                function (next) {
                    try {
                        profileBlob.code = ApiFactory.generateVerificationCode('phone');
                        profileBlob.codeValidUntil = DateUtils.isoFuture(1000 * Config.codeConfirmation.expiresInSeconds);
                    } catch (ex) {
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
                            logger.error('recoverApiFactory.recoverPasswordPhone: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending sms
                function (next) {
                    try {
                        var sendSmsParams = {
                            userId: userIdFromProfile,
                            phone: params.phone,
                            message: Config.userMessage.authRecoverPasswordSms.message,
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
                        logger.error('recoverApiFactory.recoverPasswordPhone: userMessage.sendSms', ex);
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
            logger.error('recoverApiFactory.recoverPasswordPhone', ex);
            return ApiFactory.callbackError(Errors.AuthApi.FatalError, callback);
        }
    },

    recoverPasswordConfirmEmail: function (params, clientSession, callback) {
        var profile = {};
        var profileBlob = {};
        var userIdFromProfile = null;
        var newToken = null;
        try {
            async.series([
                // Try to find already existing profile by email - should exist
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
                        logger.error('recoverApiFactory.recoverPasswordConfirmEmail: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check email - should be registered
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
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            profile.emails = message.getContent().profile.emails;
                            profile.roles = message.getContent().profile.roles;
                            var verifiedEmail = _.find(profile.emails, {
                                email: params.email
                            });
                            if (!verifiedEmail) {
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('recoverApiFactory.recoverPasswordConfirmEmail: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile blob, check confirmation code
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
                                // Check confirmation code: should exist
                                if (!_.has(message.getContent(), 'value') || !_.has(message.getContent().value, 'code') || !_.has(message.getContent().value, 'codeValidUntil')) {
                                    // return next(Errors.AuthApi.NotRegistered);
                                    return next(Errors.AuthApi.SecretWrong);
                                }
                                if (!message.getContent().value.code) {
                                    return next(Errors.AuthApi.AlreadyVerified);
                                }
                                // Check confirmation code: compare code, expration date
                                if (message.getContent().value.code !== params.code || message.getContent().value.codeValidUntil < DateUtils.isoNow()) {
                                    return next(Errors.AuthApi.CodeNotValid);
                                }
                                profileBlob = message.getContent().value;
                                return next();
                            });
                        } catch (ex) {
                            logger.error('recoverApiFactory.recoverPasswordConfirmEmail: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile: 
                // - locate email and set verification status = 'verified' if it isn't yet
                // - remove Anonymous role, add Registered role
                function (next) {
                    try {
                        var emailIndex = _.findIndex(profile.emails, {
                            email: params.email
                        });
                        if (profile.emails[emailIndex].verificationStatus !== 'verified') {
                            profile.emails[emailIndex].verificationStatus = 'verified';
                        }
                        profile.roles = _.union(profile.roles, [AerospikeUser.ROLE_REGISTERED]);
                        profile.roles = RoleService.excludeDuplicates(profile.roles);
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Profile service: call updateUserProfile
                    try {
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
                        logger.error('confirmApiFactory.recoverPasswordConfirmEmail: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: encrypt new password with SHA256 and update password
                function (next) {
                    try {
                        profileBlob.password = JwtUtils.encodePass(params.newPassword);
                        profileBlob.code = null;
                        profileBlob.codeValidUntil = null;
                    } catch (ex) {
                        logger.error('recoverApiFactory.recoverPasswordConfirmEmail: encodePass', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
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
                        logger.error('recoverApiFactory.recoverPasswordConfirmEmail: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Notify user by sending email
                function (next) {
                    try {
                        var sendEmailParams = {
                            userId: userIdFromProfile,
                            address: params.email,
                            subject: Config.userMessage.authRecoverPasswordConfirmEmail.subject,
                            message: Config.userMessage.authRecoverPasswordConfirmEmail.message,
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
                        logger.error('confirmApiFactory.recoverPasswordConfirmEmail: userMessage.sendEmail', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromProfile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, function (err, userToken) {
                            return next(err);
                        });
                    } catch (ex) {
                        logger.error('confirmApiFactory.recoverPasswordConfirmEmail: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
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
                // Publish TOKEN_INVALIDATED event with new token
                function (next) {
                    try {
                        clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('confirmApiFactory.recoverPasswordConfirmEmail: event.publish', ex);
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

    recoverPasswordConfirmPhone: function (params, clientSession, callback) {
        var profile = {};
        var profileBlob = {};
        var userIdFromProfile = null;
        var newToken = null;
        try {
            async.series([
                // Try to find already existing profile by phone - should exist
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
                        logger.error('recoverApiFactory.recoverPasswordConfirmPhone: profile.findByIdentifier', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get existing profile, check phone - should be registered
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
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            profile.phones = message.getContent().profile.phones;
                            profile.roles = message.getContent().profile.roles;
                            var verifiedPhone = _.find(profile.phones, {
                                phone: params.phone
                            });
                            if (!verifiedPhone) {
                                // return next(Errors.AuthApi.NotRegistered);
                                return next(Errors.AuthApi.SecretWrong);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('recoverApiFactory.recoverPasswordConfirmPhone: profile.getUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Get profile blob, check confirmation code
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
                                // Check confirmation code: should exist
                                if (!_.has(message.getContent(), 'value') || !_.has(message.getContent().value, 'code') || !_.has(message.getContent().value, 'codeValidUntil')) {
                                    // return next(Errors.AuthApi.NotRegistered);
                                    return next(Errors.AuthApi.SecretWrong);
                                }
                                if (!message.getContent().value.code) {
                                    return next(Errors.AuthApi.AlreadyVerified);
                                }
                                // Check confirmation code: compare code, expration date
                                if (message.getContent().value.code !== params.code || message.getContent().value < DateUtils.isoNow()) {
                                    return next(Errors.AuthApi.CodeNotValid);
                                }
                                profileBlob = message.getContent().value;
                                return next();
                            });
                        } catch (ex) {
                            logger.error('recoverApiFactory.recoverPasswordConfirmPhone: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile: 
                // - locate phone and set verification status = 'verified' if it isn't yet
                // - remove Anonymous role, add Registered role
                function (next) {
                    try {
                        var phoneIndex = _.findIndex(profile.phones, {
                            phone: params.phone
                        });
                        if (profile.phones[phoneIndex].verificationStatus !== 'verified') {
                            profile.phones[phoneIndex].verificationStatus = 'verified';
                        }
                        profile.roles = _.union(profile.roles, [AerospikeUser.ROLE_REGISTERED]);
                        profile.roles = RoleService.excludeDuplicates(profile.roles);
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                    // Profile service: call updateUserProfile
                    try {
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
                        logger.error('confirmApiFactory.recoverPasswordConfirmPhone: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: encrypt new password with SHA256 and update password
                function (next) {
                    try {
                        profileBlob.password = JwtUtils.encodePass(params.newPassword);
                        profileBlob.code = null;
                        profileBlob.codeValidUntil = null;
                    } catch (ex) {
                        logger.error('recoverApiFactory.recoverPasswordConfirmPhone: encodePass', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
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
                        logger.error('recoverApiFactory.recoverPasswordConfirmPhone: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Notify user by sending sms
                function (next) {
                    try {
                        var sendSmsParams = {
                            userId: userIdFromProfile,
                            phone: params.phone,
                            message: Config.userMessage.authRecoverPasswordConfirmSms.message,
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
                        logger.error('confirmApiFactory.recoverPasswordConfirmPhone: userMessage.sendSms', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Generate new token and update AerospikeUserToken for userIdFromProfile
                function (next) {
                    // Generate new token
                    try {
                        newToken = JwtUtils.encode({ userId: userIdFromProfile, roles: profile.roles });
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        AerospikeUserToken.create({ userId: userIdFromProfile, token: newToken }, function (err, userToken) {
                            return next(err);
                        });
                    } catch (ex) {
                        logger.error('confirmApiFactory.recoverPasswordConfirmPhone: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
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
                // Publish TOKEN_INVALIDATED event with new token
                function (next) {
                    try {
                        clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('confirmApiFactory.recoverPasswordConfirmPhone: event.publish', ex);
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
};