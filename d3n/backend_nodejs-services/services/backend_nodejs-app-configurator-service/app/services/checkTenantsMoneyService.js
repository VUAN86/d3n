var logger = require('nodejs-logger')();
var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var DefaultClient = require('nodejs-default-client');
var ProtocolMessage = require('nodejs-protocol');
var RegistryServiceClient = require('nodejs-service-registry-client');
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Tenant = RdbmsService.Models.Tenant.Tenant;
var Game = Database.RdbmsService.Models.Game.Game;
var GameApiFactory = require('../factories/gameApiFactory.js');
var ClientInfo = require('nodejs-utils').ClientInfo;
var ProfileApiFactory = require('../factories/profileApiFactory.js');
var UserMessageClient = require('nodejs-user-message-client');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;


var userMessageClient = new UserMessageClient({
    registryServiceURIs: Config.registryServiceURIs,
    ownerServiceName: 'checkTenantsMoneyService'
});

var SchedulerService = require('./schedulerService.js');
var schedulerService = new SchedulerService({
    instanceId: 'checkTenantsMoneyService',
    runCleanup: false
});

var paymentServiceName = 'payment';
var paymentServiceClient = new DefaultClient({
    serviceNamespace: paymentServiceName,
    reconnectForever: false,
    autoConnect: true,
    serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: Config.registryServiceURIs}),
    headers: {service_name: 'checkTenantsMoneyService'}
});

module.exports = {
    generateTaskId: function () {
        return 'check-tenant-money-' + (new Date()).toISOString().substring(0, 10);
    },
    
    getTenantAccountMoney: function (tenantId, cb) {
        try {
            var pm = new ProtocolMessage();
            pm.setMessage(paymentServiceName + '/getAccountBalance');
            pm.setSeq(paymentServiceClient.getSeq());
            pm.setContent({
                tenantId: '' + tenantId,
                currency: 'MONEY'
            });
            
            paymentServiceClient.sendMessage(pm, function (err, response) {
                try {
                    if (err) {
                        return cb(err);
                    }
                    
                    if (response.getError()) {
                        return cb(new Error(response.getError().message));
                    }
                    
                    return cb(false, response.getContent().amount);
                } catch (e) {
                    return cb(e);
                }
            });
            
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    
    calculatePotentialWinAmountPerGame: function (tenantId, cb) {
        try {
            var params = {
                "limit": 0,
                "offset": 0,
                "orderBy": [
                  {
                    "field": "id",
                    "direction": "asc"
                  }
                ],
                "searchBy": {
                    "status": {$or: [Game.constants().STATUS_ACTIVE, Game.constants().STATUS_DIRTY]}
                }
            };
            
            var clientInfo = new ClientInfo();
            clientInfo.setTenantId(tenantId);
            
            var args = {
                params: params,
                clientInfo: clientInfo
            };
            GameApiFactory.gameList(args, function (err, response) {
                try {
                    
                    if (err) {
                        return cb(err);
                    }
                    
                    var games = response.items;
                    var amountPerGame = {};
                    for(var i=0; i<games.length; i++) {
                        var game = games[i];
                        var gameId = game.id;
                        var amounts = [];
                        
                        amountPerGame[gameId] = 0;
                        
                        if (_.has(game, 'resultConfiguration.paidWinningComponentPayoutStructure.items')) {
                            var items = game.resultConfiguration.paidWinningComponentPayoutStructure.items;
                            for(var j=0; j<items.length; j++) {
                                if (items[j].type === 'MONEY') {
                                    amounts.push(parseFloat(items[j].amount));
                                }
                            }
                        }
                        
                        if (_.has(game, 'resultConfiguration.freeWinningComponentPayoutStructure.items')) {
                            var items = game.resultConfiguration.freeWinningComponentPayoutStructure.items;
                            for(var j=0; j<items.length; j++) {
                                if (items[j].type === 'MONEY') {
                                    amounts.push(parseFloat(items[j].amount));
                                }
                            }
                        }
                        
                        if (amounts.length) {
                            amountPerGame[gameId] = _.max(amounts);
                        }
                        
                        
                    }
                    
                    return cb(false, amountPerGame);
                } catch (e) {
                    return cb(e);
                }
            });
            
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    checkTenantMoney: function (tenantId, cb) {
        try {
            var self = this;
            
            var tenantAccountMoney;
            var amountPerGame;
            async.series([
                // get tenant money
                function (next) {
                    self.getTenantAccountMoney(tenantId, function (err, amount) {
                        if (err) {
                            logger.error('checkTenantsMoneyService.checkTenantMoney() error on getTenantAccountMoney() call:', err, tenantId);
                            return next(err);
                        }
                        
                        tenantAccountMoney = amount;
                        
                        return next();
                    });
                },
                
                // calculate potential win per game
                function (next) {
                    self.calculatePotentialWinAmountPerGame(tenantId, function (err, result) {
                        if (err) {
                            logger.error('checkTenantsMoneyService.checkTenantMoney() error on calculatePotentialWinAmountPerGame() call:', err, tenantId);
                            return next(err);
                        }
                        
                        amountPerGame = result;
                        return next();
                    });
                },
                
                // if potential win > accont money then deactivate games and send email
                function (next) {
                    try {
                        var potentialWin = _.sum(_.values(amountPerGame))*100;
                        
                        if (!(potentialWin > tenantAccountMoney)) {
                            return setImmediate(next);
                        }
                        logger.debug('checkTenantsMoney.checkTenantMoney() not enough money:', tenantId, tenantAccountMoney, potentialWin);
                        self.actionsOnNotEnoughMoney(tenantId, _.keys(amountPerGame), tenantAccountMoney, potentialWin, next);
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], function (err) {
                if(err) {
                    logger.error('checkTenantsMoneyService.checkTenantMoney err:', err, tenantId);
                    return cb(err);
                }
                
                return cb();
            });
            
        } catch (e) {
            logger.error('checkTenantsMoneyService.checkTenantMoney tc err:', e, tenantId);
            return setImmediate(cb, e);
        }
    },
    
    checkTenantsMoney: function (cb) {
        try {
            var self = this;
            
            var taskId = self.generateTaskId();

            AerospikeTaskLock.lock(taskId, function (err, lock) {
                try {
                    if (err) {
                        return cb(err);
                    }

                    // locked by someone else
                    if (lock !== true) {
                        logger.info('checkTenantsMoneyService.checkTenantsMoney() task locked by someone else');
                        return cb();
                    }

                    self._checkTenantsMoney(cb);

                } catch (e) {
                    return cb(e);
                }
            });
            
        } catch (e) {
            logger.error('checkTenantsMoneyService.checkTenantsMoney() tc err:', e);
            return setImmediate(cb, e);
        }
    },
    
    _checkTenantsMoney: function (cb) {
        try {
            logger.debug('checkTenantsMoneyService._checkTenantsMoney() called');
            var self = this;
            var tenantIds = [];
            async.series([
                // get tenant IDs
                function (next) {
                    
                    Tenant.findAll({
                        where: {status: Tenant.constants().STATUS_ACTIVE},
                        attributes: [Tenant.tableAttributes.id.field]
                    }).then(function (dbItems) {
                        try {
                            for(var i=0; i<dbItems.length; i++) {
                                tenantIds.push(dbItems[i].get({plain: true}).id);
                            }
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                
                // check money for each tenant
                function (next) {
                    async.mapSeries(tenantIds, function (tenantId, cbItem) {
                        self.checkTenantMoney(tenantId, function () {
                            return cbItem();
                        });
                    }, next);
                }
            ], function (err) {
                if (err) {
                    logger.error('checkTenantsMoneyService._checkTenantsMoney() err:', err);
                    return cb(err);
                } else {
                    logger.debug('checkTenantsMoneyService._checkTenantsMoney() done success');
                    return cb();
                }
            });
        } catch (e) {
            logger.error('checkTenantsMoneyService._checkTenantsMoney() tc err:', e);
            return setImmediate(cb, e);
        }
    },
    
    actionsOnNotEnoughMoney: function (tenantId, gameIds, tenantAccountMoney, potentialWin, cb) {
        try {
            var self = this;
            var clientInfo = new ClientInfo();
            clientInfo.setTenantId(tenantId);
            
            var gameData = {};
            async.series([
                // load some game data: status and name
                function (next) {
                    var options = {
                        where: {
                            id: {
                                'in': gameIds.map(function (item) {return parseInt(item);})
                            }
                        },
                        attributes: [Game.tableAttributes.id.field, Game.tableAttributes.title.field, Game.tableAttributes.status.field]
                    };
                    Game.findAll(options).then(function (dbItems) {
                        
                        try {
                            for(var i=0; i<dbItems.length; i++) {
                                var game = dbItems[i].get({plain: true});
                                gameData['' + game.id] = {
                                    title: game.title,
                                    status: game.status
                                };
                            }
                            
                            return next();
                            
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                
                // deactivate games
                function (next) {
                    async.mapSeries(gameIds, function (gameId, cbItem) {
                        try {
                            GameApiFactory.gameUnpublish({
                                params: {id: gameId},
                                clientInfo: clientInfo,
                                schedulerService: schedulerService
                            }, function (err) {
                                if (err) {
                                    logger.error('checkTenantsMoneyService.actionsOnNotEnoughMoney error on unpublish:', err, gameId, tenantId);
                                }

                                return cbItem();
                            });
                            
                        } catch (e) {
                            logger.error('checkTenantsMoneyService.actionsOnNotEnoughMoney tc error on gameUnpublish:', e, gameId, tenantId);
                            return cbItem();
                        }
                    }, next);
                },
                
                // send email to tenant admins
                function (next) {
                    self.sendEmail(tenantId, tenantAccountMoney, potentialWin, gameData, next);
                }
            ], cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    sendEmail: function (tenantId, tenantAccountMoney, potentialWin, gameData, cb) {
        try {
            var admins = [];
            var tenantName;
            async.series([
                // get tenant name
                function (next) {
                    Tenant.findOne({where: {id: tenantId}, attributes: [Tenant.tableAttributes.name.field]}).then(function (dbItem) {
                        try {
                            tenantName = dbItem.get({plain: true}).name;
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // get admins
                function (next) {
                    ProfileApiFactory.adminListByTenantId({tenantId: '' + tenantId}, {}, function (err, response) {
                        if (err) {
                            return next(err);
                        }
                        
                        admins = response.admins;
                        return next();
                    });
                },
                
                // send emails
                function (next) {
                    if (!admins.length) {
                        logger.error('checkTenantsMoneyService.sendEmail() no admins', tenantId);
                        return setImmediate(next);
                    }
                    
                    async.mapSeries(admins, function (item, cbItem) {
                        try {
                            var message = 'The automatic balance validation found that tenant ' + tenantName + ' (ID: ' + tenantId + ') did not had enough money on his account to support the winnings of the following game IDs: ' + _.keys(gameData).join(', ') + '. ';
                            message += 'Expectec balance ' + potentialWin + ', found balance ' + tenantAccountMoney + '. The games where deactivated.';
                            
                            var sendEmailParams = {
                                userId: item,
                                address: null,
                                subject: Config.userMessage.tenantCheckMoney.subject,
                                message: message,
                                waitResponse: false
                            };
                            userMessageClient.sendEmail(sendEmailParams, function (err) {
                                if (err) {
                                    logger.error('checkTenantsMoneyService.sendEmail() eror sending email to admin:', item, err);
                                }
                                return cbItem();
                            });
                            
                        } catch (e) {
                            logger.error('checkTenantsMoneyService.sendEmail() eror sending email to admin, tc:', e, item);
                            return cbItem();
                        }
                        
                    }, next);
                }
            ], cb);
            
        } catch (e) {
            return setImmediate(cb, e);
        }
    }
};