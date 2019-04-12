var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var GameApiFactory = require('../factories/gameApiFactory.js');
var ClientInfo = require('nodejs-utils').ClientInfo;
var UserMessageClient = require('nodejs-user-message-client');
var ProfileService = require('./profileService.js');
var SchedulerService = require('./schedulerService.js');
var RdbmsService = require('nodejs-database').getInstance(Config).RdbmsService;
var Game = RdbmsService.Models.Game.Game;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var logger = require('nodejs-logger')();

var userMessageClient = new UserMessageClient({
    registryServiceURIs: Config.registryServiceURIs,
    ownerServiceName: 'gameValidityCheckService'
});

var schedulerService = new SchedulerService({
    instanceId: 'gameValidityCheckService',
    runCleanup: false
});

module.exports = {

    /**
     * Validate active games, disable games that are not valid and email to admins
     * @param {*} callback
     * @returns {*}
     */
    gameValidityCheck: function (callback) {
        var self = this;
        var activeGames = [];
        var activeAdmins = [];
        try {
            async.series([
                // Gather active games for all tenants
                function (serie) {
                    return Game.findAll({
                        where: {
                            status: Game.constants().STATUS_ACTIVE,
                            isAutoValidationEnabled: 1
                        }
                    }).then(function (games) {
                        _.forEach(games, function (game) {
                            var gamePlain = game.get({ plain: true });
                            activeGames.push({ id: gamePlain.id, tenantId: gamePlain.tenantId });
                        });
                        return serie();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, serie);
                    });
                },
                // Gather admins for all tenants
                function (serie) {
                    try {
                        return ProfileService.getAdminTenantList(function (err, admins) {
                            activeAdmins = admins;
                            return serie();
                        });
                    } catch (ex) {
                        return serie(ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return setImmediate(callback, err);
                }
                return async.everySeries(activeGames, function (activeGame, every) {
                    logger.info('GameValidityCheckService: checking ' + activeGame.id);
                    var notify = false;
                    var deactivate = false;
                    async.series([
                        // Validate game
                        function (next) {
                            try {
                                // Disable game validation check for now
                                return setImmediate(next, null);
                                //return GameApiFactory.gameValidateInternal({
                                //    id: activeGame.id,
                                //    tenantId: activeGame.tenantId
                                //}, function (err, result) {
                                //    if (err) {
                                //        return next(err);
                                //    }
                                //    if (result && result.data && _.isArray(result.data.errors) && result.data.errors.length > 0) {
                                //        notify = true;
                                //    }
                                //    deactivate = result.deactivate;
                                //    return next();
                                //});
                            } catch (ex) {
                                logger.error('GameValidityCheckService: validate game', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Unpublish game
                        function (next) {
                            if (!deactivate) {
                                return setImmediate(next, null);
                            }
                            try {
                                var clientInfo = new ClientInfo();
                                clientInfo.setTenantId(activeGame.tenantId);
                                return GameApiFactory.gameUnpublish({
                                    params: { id: activeGame.id },
                                    clientInfo: clientInfo,
                                    schedulerService: schedulerService
                                }, null, next);
                            } catch (ex) {
                                logger.error('GameValidityCheckService: unpublish game', ex);
                                return setImmediate(next, ex);
                            }
                        },
                        // Send email to all admins of game's tenant
                        function (next) {
                            if (!notify) {
                                return setImmediate(next, null);
                            }
                            try {
                                var tenantAdmins = _.map(activeAdmins, function (admin) {
                                    if (admin.tenantId === activeGame.tenantId) {
                                        return admin.profileId;
                                    }
                                });
                                return async.everySeries(tenantAdmins, function (tenantAdmin, everyAdmin) {
                                    try {
                                        var sendEmailParams = {
                                            userId: tenantAdmin,
                                            address: null,
                                            subject: Config.userMessage.gameInvalidDeactivatedEmail.subject,
                                            message: Config.userMessage.gameInvalidDeactivatedEmail.message,
                                            subject_parameters: [activeGame.id.toString()],
                                            message_parameters: [activeGame.id.toString()],
                                            waitResponse: Config.umWaitForResponse
                                        };
                                        return userMessageClient.sendEmail(sendEmailParams, function (err, message) {
                                            if (err || (message && message.getError())) {
                                                logger.error('GameValidityCheckService: sendEmail', err || message.getError());
                                                return everyAdmin();
                                            }
                                            return everyAdmin();
                                        });
                                    } catch (ex) {
                                        logger.error('GameValidityCheckService: sendEmail', ex);
                                        return everyAdmin(Errors.QuestionApi.NoInstanceAvailable);
                                    }
                                }, function (err) {
                                    if (err) {
                                        return next(err);
                                    }
                                    return next(null, true);
                                });
                            } catch (ex) {
                                logger.error('GameValidityCheckService: email to admins', ex);
                                return setImmediate(next, ex);
                            }
                        },
                    ], function (err) {
                        if (err) {
                            return every(err);
                        }
                        return every(null, true);
                    });
                }, function (err) {
                    if (err) {
                        return setImmediate(callback, err);
                    }
                    return setImmediate(callback);
                });
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },
    
};