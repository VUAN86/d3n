var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ApiFactory = require('./apiFactory.js');
var RoleService = require('./../services/roleService.js');
var AnalyticEventService = require('./../services/analyticEventService.js');
var JwtUtils = require('./../utils/jwtUtils.js');
var logger = require('nodejs-logger')();
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeUser = KeyvalueService.Models.AerospikeUser;
var AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken;

module.exports = {

    setUserRole: function (params, clientSession, callback) {
        var profile = {};
        var newToken = undefined;
        var sendEmail = false;
        var sendSms = false;
        var oldRoles = '<>';
        var newRoles = '<>';
        var newRolesDescriptions = [];
        try {
            try {
                // Cannot add and remove the same roles
                var rolesToCheck = _.union(params.rolesToAdd, params.rolesToRemove);
                if (_.size(rolesToCheck) !== _.size(params.rolesToAdd) + _.size(params.rolesToRemove)) {
                    return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
                }
                // Cannot add or remove invalid or empty roles
                if (!RoleService.checkRolesAreValid(rolesToCheck)) {
                    return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
                }
                // Cannot grant roles to same user as caller except community access
                if (clientSession.isAuthenticated() &&
                    params.userId === clientSession.getUserId()) {
                    var requestCommunityAccess = true;
                    _.forEach(params.rolesToAdd, function (role) {
                        if (!(role.startsWith('TENANT_') && role.endsWith('_COMMUNITY'))) {
                            requestCommunityAccess = false;
                        }
                    });
                    _.forEach(params.rolesToRemove, function (role) {
                        if (!(role.startsWith('TENANT_') && role.endsWith('_COMMUNITY'))) {
                            requestCommunityAccess = false;
                        }
                    });
                    if (!requestCommunityAccess) {
                        return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
                    }
                }
            } catch (ex) {
                logger.error('roleApiFactory.setUserRole: validate', ex);
                return ApiFactory.callbackError(Errors.AuthApi.ValidationFailed, callback);
            }
            async.series([
                // Get existing profile data
                function (next) {
                    try {
                        // Profile service: call getProfile
                        clientSession.getConnectionService().getProfileService().getUserProfile({
                            userId: params.userId,
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
                                profile.roles = message.getContent().profile.roles;
                                if (profile.roles && _.isArray(profile.roles)) {
                                    oldRoles = profile.roles.join(',');
                                }
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
                            } catch (ex) {
                                logger.error('roleApiFactory.setUserRole: get registered profile', ex);
                                return next(Errors.AuthApi.FatalError);
                            }
                        });
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: profile.getProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Merge roles, exclude duplicates
                function (next) {
                    try {
                        profile.roles = _.union(profile.roles, params.rolesToAdd);
                        _.each(params.rolesToRemove, function (value) {
                            _.pull(profile.roles, value);
                        });
                        profile.roles = RoleService.excludeDuplicates(profile.roles);
                        if (profile.roles && _.isArray(profile.roles)) {
                            newRoles = profile.roles.join(',');
                            var roleDescription;
                            _.forEach(params.rolesToAdd, function (role) {
                                if (role.startsWith('TENANT_')) {
                                    var roleSuffix = role.substring(role.lastIndexOf('_') + 1);
                                    roleDescription = _.find(RoleService.ROLE_DESCRIPTIONS, { role: roleSuffix });
                                    if (roleDescription) {
                                        newRolesDescriptions.push('TENANT_X_' + roleDescription.role + ': ' + roleDescription.description);
                                    }
                                } else {
                                    roleDescription = _.find(RoleService.ROLE_DESCRIPTIONS, { role: role });
                                    if (roleDescription) {
                                        newRolesDescriptions.push(roleDescription.role + ': ' + roleDescription.description);
                                    }
                                }
                            });
                            newRolesDescriptions = _.uniq(newRolesDescriptions);
                        }
                        return next();
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: update registered profile roles', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Update profile data
                function (next) {
                    // Profile service: call updateUserProfile
                    try {
                        clientSession.getConnectionService().getProfileService().updateUserProfile({
                            userId: params.userId,
                            profile: profile,
                            //clientInfo: clientSession.getClientInfo()
                        }, undefined, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: profile.updateUserProfile', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Share token with gateway - make Aerospike entry
                function (next) {
                    try {
                        newToken = JwtUtils.encode({ userId: params.userId, roles: profile.roles });
                    } catch (ex) {
                        return next(Errors.AuthApi.FatalError);
                    }
                    try {
                        AerospikeUserToken.create({ userId: params.userId, token: newToken }, function (err, userToken) {
                            return next(err);
                        });
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: AerospikeUserToken.create', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Issue REGISTERED analytic event (only if rolesToAdd contains REGISTERED role and ClientInfo is provided)
                function (next) {
                    try {
                        if (!_.includes(params.rolesToAdd, AerospikeUser.ROLE_REGISTERED) ||
                            !clientSession.getClientInfo() || !_.isObject(clientSession.getClientInfo().appConfig) ||
                            !clientSession.getClientInfo().appConfig.appId || !clientSession.getClientInfo().appConfig.tenantId) {
                            return next();
                        }
                        var clientInfo = {
                            userId: params.userId,
                            appId: clientSession.getClientInfo().appConfig.appId,
                            tenantId: clientSession.getClientInfo().appConfig.tenantId,
                            sessionIp: clientSession.getClientInfo().ip,
                            sessionId: clientSession.getId()
                        };
                        return AnalyticEventService.issueRegisteredEvent(clientInfo, next);
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: AnalyticEventService.issueRegisteredEvent', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Issue FULLY_REGISTERED analytic event (only if rolesToAdd contains FULLY_REGISTERED role and ClientInfo is provided)
                function (next) {
                    try {
                        if (!_.includes(params.rolesToAdd, AerospikeUser.ROLE_FULLY_REGISTERED) ||
                            !clientSession.getClientInfo() || !_.isObject(clientSession.getClientInfo().appConfig) ||
                            !clientSession.getClientInfo().appConfig.appId || !clientSession.getClientInfo().appConfig.tenantId) {
                            return next();
                        }
                        var clientInfo = {
                            userId: params.userId,
                            appId: clientSession.getClientInfo().appConfig.appId,
                            tenantId: clientSession.getClientInfo().appConfig.tenantId,
                            sessionIp: clientSession.getClientInfo().ip,
                            sessionId: clientSession.getId()
                        };
                        return AnalyticEventService.issueFullyRegisteredEvent(clientInfo, next);
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: AnalyticEventService.issueFullyRegisteredEvent', ex);
                        return next(Errors.AuthApi.FatalError);
                    }
                },
                // Publish TOKEN_INVALIDATED event with token
                function (next) {
                    try {
                        clientSession.getConnectionService().getEventService().publish('TOKEN_INVALIDATED', { token: newToken }, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: event.publish', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Notify user by sending email
                function (next) {
                    if (!sendEmail || oldRoles === newRoles) {
                        return next();
                    }
                    try {
                        var sendEmailParams = {
                            userId: params.userId,
                            address: null,
                            subject: Config.userMessage.authSetUserRoleEmail.subject,
                            message: Config.userMessage.authSetUserRoleEmail.message,
                            subject_parameters: null,
                            message_parameters: [oldRoles, newRoles, newRolesDescriptions.join(';')],
                            waitResponse: Config.umWaitForResponse,
                            clientInfo: clientSession.getClientInfo()
                        };
                        /*clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, function (err, message) {
                            if (err || (message && message.getError())) {
                                return next(err || message.getError());
                            }
                            return next();
                        });*/
                        return next();  //new line instead top comment
                    } catch (ex) {
                        logger.error('roleApiFactory.setUserRole: userMessage.sendEmail', ex);
                        return next(Errors.AuthApi.NoInstanceAvailable);
                    }
                },
                // Notify user by sending sms
                function (next) {
                    if (!sendSms || oldRoles === newRoles) {
                        return next();
                    }
                    try {
                        var sendSmsParams = {
                            userId: params.userId,
                            phone: null,
                            message: Config.userMessage.authSetUserRoleSms.message,
                            message_parameters: [oldRoles, newRoles, newRolesDescriptions.join(';')],
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
                        logger.error('roleApiFactory.setUserRole: userMessage.sendSms', ex);
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

};