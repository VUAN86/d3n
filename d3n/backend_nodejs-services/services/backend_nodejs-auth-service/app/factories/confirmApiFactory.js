var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ApiFactory = require('./apiFactory.js');
var RoleService = require('./../services/roleService.js');
var AnalyticEventService = require('./../services/analyticEventService.js');
var JwtUtils = require('./../utils/jwtUtils.js');
var DateUtils = require('nodejs-utils').DateUtils;
var logger = require('nodejs-logger')();
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;

module.exports = {

    confirmEmail: function (params, clientSession, callback) {
        var profile = {};
        var profileBlob = {};
        var userIdFromProfile = null;
        var newToken = null;
        var passwordExist = true;
        var generatedPassword;
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
                        logger.error('registerApiFactory.confirmEmail: profile.findByIdentifier', ex);
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
                            if (verifiedEmail.verificationStatus === 'verified') {
                                return next(Errors.AuthApi.AlreadyVerified);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.confirmEmail: profile.getUserProfile', ex);
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
                                // Check password existance (can be set up as null if user registered through auth/inviteUserByEmailAndRole)
                                if (_.isNull(message.getContent().value.password)) {
                                    passwordExist = false;
                                }
                                profileBlob = message.getContent().value;
                                return next();
                            });
                        } catch (ex) {
                            logger.error('registerApiFactory.confirmEmail: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile: 
                // - locate email and set verification status = 'verified'
                // - remove Anonymous role
                // - add Registered role
                function (next) {
                    try {
                        var emailIndex = _.findIndex(profile.emails, {
                            email: params.email
                        });
                        profile.emails[emailIndex].verificationStatus = 'verified';
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
                        logger.error('confirmApiFactory.confirmEmail: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: update password (if it previously set up as null)
                function (next) {
                    if (passwordExist) {
                        return next();
                    }
                    // Generate verification code
                    try {
                        generatedPassword = ApiFactory.generatePassword('email');
                        profileBlob.password = JwtUtils.encodePass(JwtUtils.encodePassWithFixedSalt(generatedPassword));
                    } catch (ex) {
                        logger.error('registerApiFactory.confirmEmail: generate verification code', ex);
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
                            logger.error('registerApiFactory.confirmEmail: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.confirmEmail: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending email
                function (next) {
                    try {
                        var sendEmailParams = {
                            userId: userIdFromProfile,
                            address: params.email,
                            subject: Config.userMessage.authConfirmEmail.subject,
                            message: Config.userMessage.authConfirmEmail.message,
                            subject_parameters: null,
                            message_parameters: null,
                            waitResponse: Config.umWaitForResponse,
                            clientInfo: clientSession.getClientInfo()
                        };
                        if (!passwordExist) {
                            sendEmailParams.subject = Config.userMessage.authConfirmEmailMessageWithPassword.subject;
                            sendEmailParams.message = Config.userMessage.authConfirmEmailMessageWithPassword.message;
                            sendEmailParams.subject_parameters = null;
                            sendEmailParams.message_parameters = [generatedPassword];
                        }
                        clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('confirmApiFactory.confirmEmail: userMessage.sendEmail', ex);
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
                        logger.error('confirmApiFactory.confirmEmail: AerospikeUserToken.create', ex);
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
                        logger.error('confirmApiFactory.confirmEmail: event.publish', ex);
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

    confirmPhone: function (params, clientSession, callback) {
        var profile = {};
        var profileBlob = {};
        var userIdFromProfile = null;
        var newToken = null;
        var passwordExist = true;
        var generatedPassword;
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
                        logger.error('registerApiFactory.confirmPhone: profile.findByIdentifier', ex);
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
                            if (verifiedPhone.verificationStatus === 'verified') {
                                return next(Errors.AuthApi.AlreadyVerified);
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('registerApiFactory.confirmPhone: profile.getUserProfile', ex);
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
                                // Check password existance (can be set up as null if user registered through auth/inviteUserByEmailAndRole)
                                if (_.isNull(message.getContent().value.password)) {
                                    passwordExist = false;
                                }
                                profileBlob = message.getContent().value;
                                return next();
                            });
                        } catch (ex) {
                            logger.error('registerApiFactory.confirmPhone: profile.getUserProfileBlob', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile: 
                // - locate phone and set verification status = 'verified'
                // - remove Anonymous role
                // - add Registered role
                function (next) {
                    try {
                        var phoneIndex = _.findIndex(profile.phones, {
                            phone: params.phone
                        });
                        profile.phones[phoneIndex].verificationStatus = 'verified';
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
                        logger.error('confirmApiFactory.confirmPhone: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Update profile blob: update password (if it previously set up as null)
                function (next) {
                    if (passwordExist) {
                        return next();
                    }
                    // Generate verification code
                    try {
                        generatedPassword = ApiFactory.generatePassword('email');
                        profileBlob.password = JwtUtils.encodePass(JwtUtils.encodePassWithFixedSalt(generatedPassword));
                    } catch (ex) {
                        logger.error('registerApiFactory.confirmEmail: generate verification code', ex);
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
                            logger.error('registerApiFactory.confirmEmail: profile.updateUserProfile', ex);
                            return next(Errors.AuthApi.NoInstanceAvailable);
                        }
                    } catch (ex) {
                        logger.error('registerApiFactory.confirmEmail: updateUserProfile', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Notify user by sending sms
                function (next) {
                    try {
                        var sendSmsParams = {
                            userId: userIdFromProfile,
                            phone: params.phone,
                            message: Config.userMessage.authConfirmPhone.message,
                            message_parameters: null,
                            waitResponse: Config.umWaitForResponse,
                            clientInfo: clientSession.getClientInfo()
                        };
                        if (!passwordExist) {
                            sendSmsParams.message = Config.userMessage.authConfirmPhoneSmsWithPassword.message;
                            sendSmsParams.message_parameters = [generatedPassword];
                        }
                        clientSession.getConnectionService().getUserMessage().sendSms(sendSmsParams, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('confirmApiFactory.confirmPhone: userMessage.sendSms', ex);
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
                        logger.error('confirmApiFactory.confirmPhone: AerospikeUserToken.create', ex);
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
                        logger.error('confirmApiFactory.confirmPhone: event.publish', ex);
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