var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var StandardErrors = require('nodejs-errors');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var logger = require('nodejs-logger')();
var ProfileHasGlobalRole = RdbmsService.Models.ProfileManager.ProfileHasGlobalRole;

module.exports = {
    appReferralLink: function (params, message, clientSession, callback) {
        try {
            if (!_.has(params, 'referralUserId')) {
                return CrudHelper.callbackError(StandardErrors.ERR_VALIDATION_FAILED.message, callback);
            }
            
            var self = this;
            var friendServiceClient = clientSession.getConnectionService().getFriendService();
            var profileServiceClient = clientSession.getConnectionService().getProfileService();
            var userId = clientSession.getUserId();
            var referralUserId = params.referralUserId;
            async.series([
                function (next) {
                    profileServiceClient.getUserProfile({
                        userId: referralUserId,
                        clientInfo: clientSession.getClientInfo()
                    }, undefined, function (err, response) {
                        try {
                            
                            if (err) {
                                return next(err);
                            }

                            if (response.getError() !== null) {
                                return next(response.getError().message);
                            }

                            var content = response.getContent();
                            if (!_.has(content, 'profile')) {
                                return next(StandardErrors.ERR_INSUFFICIENT_RIGHTS.message);
                            }
                            
                            
                            var roles = content.profile.roles;
                            if (roles.indexOf(ProfileHasGlobalRole.constants().ROLE_REGISTERED) === -1) {
                                return next(StandardErrors.ERR_INSUFFICIENT_RIGHTS.message);
                            }
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                function (next) {
                    friendServiceClient.buddyAddForUser(userId, [referralUserId], false, next);
                },
                function (next) {
                    friendServiceClient.buddyAddForUser(referralUserId, [userId], false, next);
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    }
};