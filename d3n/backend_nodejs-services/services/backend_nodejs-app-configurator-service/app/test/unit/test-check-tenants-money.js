var assert = require('chai').assert;
var async = require('async');
var _ = require('lodash');
var ClientInfo = require('nodejs-utils').ClientInfo;
var ProtocolMessage = require('nodejs-protocol');
var Config = require('./../../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var Game = Database.RdbmsService.Models.Game.Game;
var GameApiFactory = require('../../factories/gameApiFactory.js');
var sinon = require('sinon');
var sinonSandbox = sinon.sandbox.create();
var checkTenantsMoneyService = require('../../services/checkTenantsMoneyService.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;



var SchedulerService = require('../../services/schedulerService.js');
var schedulerService = new SchedulerService({
    instanceId: 'test_checkTenantsMoneyService',
    runCleanup: false
});

describe('TEST EACH METHOD', function () {
    this.timeout(20000);
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    beforeEach(function (done) {
        AerospikeTaskLock.remove(checkTenantsMoneyService.generateTaskId(), done);
    });
    
    
    it('getTenantAccountMoney()', function (done) {
        var getAccountBalanceSpy = sinonSandbox.spy(global.wsHelper._paymentServiceFake.messageHandlers, 'getAccountBalance');
        var tenantId = '1';
        checkTenantsMoneyService.getTenantAccountMoney(tenantId, function (err) {
            try {
                assert.ifError(err);
                assert.strictEqual(getAccountBalanceSpy.callCount, 1);
                assert.strictEqual(getAccountBalanceSpy.lastCall.args[0].getContent().tenantId, tenantId);
                assert.strictEqual(getAccountBalanceSpy.lastCall.args[0].getContent().currency, 'MONEY');
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
    });
    
    it('getTenantAccountMoney(), getAccountBalance return error', function (done) {
        var getAccountBalanceStub = sinonSandbox.stub(global.wsHelper._paymentServiceFake.messageHandlers, 'getAccountBalance', function (message, clientSession) {
            var pm = new ProtocolMessage(message);
            pm.setError({
                type: 'server',
                message: 'ERR_FATAL_ERROR'
            });
            clientSession.sendMessage(pm);
        });
        
        var tenantId = '1';
        checkTenantsMoneyService.getTenantAccountMoney(tenantId, function (err) {
            try {
                assert.isOk(err);
                assert.strictEqual(err.message, 'ERR_FATAL_ERROR');
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
    });
    
    
    it('calculatePotentialWinAmountPerGame() call ok gameList', function (done) {
        
        var gameListSpy = sinonSandbox.spy(GameApiFactory, 'gameList');
        
        async.series([
            // activate all games
            function (next) {
                Game.update({status: 'active'}, {where: {status: 'inactive'}}).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            function (next) {
                var tenantId = 1;
                checkTenantsMoneyService.calculatePotentialWinAmountPerGame(tenantId, function (err) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(gameListSpy.callCount, 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], done);
        
    });
    

    it('calculatePotentialWinAmountPerGame() , only active or dirty games', function (done) {
        
        var gameListSpy = sinonSandbox.spy(GameApiFactory, 'gameList');
        
        async.series([
            // set status=inactive
            function (next) {
                Game.update({status: 'inactive'}, {where: {id: {$gt: 0}}}).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            // set status=active for id=1
            function (next) {
                Game.update({status: 'active'}, {where: {id: 1}}).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            function (next) {
                var tenantId = 1;
                checkTenantsMoneyService.calculatePotentialWinAmountPerGame(tenantId, function (err, res) {
                    try {
                        
                        assert.ifError(err);
                        assert.deepEqual(_.keys(res), ['1']);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // set status=dirty for id=1
            function (next) {
                Game.update({status: 'dirty'}, {where: {id: 2}}).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            function (next) {
                var tenantId = 1;
                checkTenantsMoneyService.calculatePotentialWinAmountPerGame(tenantId, function (err, res) {
                    try {
                        
                        assert.ifError(err);
                        assert.deepEqual(_.keys(res), ['1', '2']);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            
        ], done);
        
    });
    
    
    it('calculatePotentialWinAmountPerGame()', function (done) {
        var gameListStub = sinonSandbox.stub(GameApiFactory, 'gameList', function (args, cb) {
            var items = [
                {
                    id: 1,
                    resultConfiguration: {
                        paidWinningComponentPayoutStructure: {
                            items: [
                                {
                                    type: 'MONEY',
                                    amount: 10
                                },
                                
                                {
                                    type: 'BONUS',
                                    amount: 5
                                },
                            ]
                        },
                        
                        freeWinningComponentPayoutStructure: {
                            items: [
                                {
                                    type: 'MONEY',
                                    amount: 30
                                },
                                
                                {
                                    type: 'BONUS',
                                    amount: 5
                                }
                            ]
                        }
                        
                    }
                },
                
                
                {
                    id: 2,
                    resultConfiguration: {
                        paidWinningComponentPayoutStructure: null,
                        
                        freeWinningComponentPayoutStructure: {
                            items: [
                                {
                                    type: 'MONEY',
                                    amount: 10
                                },
                                
                                {
                                    type: 'BONUS',
                                    amount: 30
                                }
                            ]
                        }
                        
                    }
                },
                
                {
                    id: 3,
                    resultConfiguration: {
                    }
                }
                
                
            ];
            
            return cb(false, {
                items: items,
                limit: 0,
                offset: 0,
                total: items.length
            });
        });
        
        var tenantId = 1;
        checkTenantsMoneyService.calculatePotentialWinAmountPerGame(tenantId, function (err, amounts) {
            try {
                assert.ifError(err);
                assert.deepEqual(amounts, {
                    '1': 30,
                    '2': 10,
                    '3': 0
                });
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
        
    });
    
    it('checkTenantMoney(), not enough money', function (done) {
        var actionsOnNotEnoughMoneySpy = sinonSandbox.spy(checkTenantsMoneyService, 'actionsOnNotEnoughMoney');
        
        var checkTenantsMoneyServiceStub = sinonSandbox.stub(checkTenantsMoneyService, 'getTenantAccountMoney', function (tenantId, cb) {
            return cb(false, 5000.45);
        });
        
        var calculatePotentialWinAmountPerGameStub = sinonSandbox.stub(checkTenantsMoneyService, 'calculatePotentialWinAmountPerGame', function (tenantId, cb) {
            return cb(false, {
                '1': 40,
                '2': 10,
                '3': 5
            });
            
            // sum = 55 => potential win = 55*100 
        });
        
        checkTenantsMoneyService.checkTenantMoney(1, function (err) {
            try {
                assert.ifError(err);
                assert.strictEqual(actionsOnNotEnoughMoneySpy.callCount, 1);
                assert.deepEqual(actionsOnNotEnoughMoneySpy.lastCall.args[1], ['1', '2', '3']);
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
    });
    
    it('checkTenantMoney(), not enough money, dirty game unpublish also', function (done) {
        var actionsOnNotEnoughMoneySpy = sinonSandbox.spy(checkTenantsMoneyService, 'actionsOnNotEnoughMoney');
        
        var checkTenantsMoneyServiceStub = sinonSandbox.stub(checkTenantsMoneyService, 'getTenantAccountMoney', function (tenantId, cb) {
            return cb(false, 200.45);
        });
        
        var calculatePotentialWinAmountPerGameStub = sinonSandbox.stub(checkTenantsMoneyService, 'calculatePotentialWinAmountPerGame', function (tenantId, cb) {
            return cb(false, {
                '1': 40
            });
        });
        
        var gameId = 1;
        async.series([
            function (next) {
                var clientInfo = new ClientInfo();
                clientInfo.setTenantId(1);
                
                GameApiFactory.gamePublish({
                    params: {id: gameId},
                    clientInfo: clientInfo,
                    schedulerService: schedulerService
                }, next);
            },
            // set dirty
            function (next) {
                try {
                    gameParams = {};
                    gameParams.id = gameId;
                    gameParams.status = Game.constants().STATUS_DIRTY;
                    return GameApiFactory.gameSetStatus(gameParams, next);
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            
            
            function (next) {
                checkTenantsMoneyService.checkTenantMoney(1, function (err) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(actionsOnNotEnoughMoneySpy.callCount, 1);
                        assert.deepEqual(actionsOnNotEnoughMoneySpy.lastCall.args[1], ['' + gameId]);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // status inactive
            function (next) {
                Game.findOne({where: {id: gameId}}).then(function (dbItem) {
                    try {
                        assert.strictEqual(dbItem.get({plain: true}).status, Game.constants().STATUS_INACTIVE);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(next);
            }
        ], done);
        
        
    });
    it('checkTenantMoney(), enough money', function (done) {
        var actionsOnNotEnoughMoneySpy = sinonSandbox.spy(checkTenantsMoneyService, 'actionsOnNotEnoughMoney');
        
        var checkTenantsMoneyServiceStub = sinonSandbox.stub(checkTenantsMoneyService, 'getTenantAccountMoney', function (tenantId, cb) {
            return cb(false, 6000.45);
        });
        
        var calculatePotentialWinAmountPerGameStub = sinonSandbox.stub(checkTenantsMoneyService, 'calculatePotentialWinAmountPerGame', function (tenantId, cb) {
            return cb(false, {
                '1': 40,
                '2': 10,
                '3': 5
            });
            
            // sum = 55 => potential win = 55*100 
        });
        
        checkTenantsMoneyService.checkTenantMoney(1, function (err) {
            try {
                assert.ifError(err);
                assert.strictEqual(actionsOnNotEnoughMoneySpy.callCount, 0);
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
    });
    
    it('checkTenantsMoney()', function (done) {
        
        var checkTenantMoneySpy = sinonSandbox.spy(checkTenantsMoneyService, 'checkTenantMoney');
        
        checkTenantsMoneyService.checkTenantsMoney(function (err) {
            try {
                assert.ifError(err);
                assert.isAtLeast(checkTenantMoneySpy.callCount, 1);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('checkTenantsMoney() concurrency', function (done) {
        
        var _checkTenantsMoneySpy = sinonSandbox.spy(checkTenantsMoneyService, '_checkTenantsMoney');
        
        async.map([1,2,3], function (i, cbItem) {
            checkTenantsMoneyService.checkTenantsMoney(cbItem);
        }, function (err) {
            try {
                assert.ifError(err);
                assert.strictEqual(_checkTenantsMoneySpy.callCount, 1);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('actionsOnNotEnoughMoney()', function (done) {
        
        var sendEmailSpy = sinonSandbox.spy(checkTenantsMoneyService, 'sendEmail');
        var sendEmailUmsSpy = sinonSandbox.spy(global.wsHelper._umFake.messageHandlers, 'sendEmail');
        var gameUnpublishSpy = sinonSandbox.spy(GameApiFactory, 'gameUnpublish');
        var unscheduleSpy = sinonSandbox.spy(SchedulerService.prototype, 'unscheduleLiveTournament');
        
        var tenantId = 1;
        var gameId = 1;
        async.series([
            function (next) {
                
                var clientInfo = new ClientInfo();
                clientInfo.setTenantId(tenantId);
                
                GameApiFactory.gamePublish({
                    params: {id: gameId},
                    clientInfo: clientInfo,
                    schedulerService: schedulerService
                }, next);
            },
            
            function (next) {
                checkTenantsMoneyService.actionsOnNotEnoughMoney(tenantId, [gameId], 500, 800, next);
            },
            
            function (next) {
                try {
                    assert.isAtLeast(sendEmailSpy.callCount, 1);
                    assert.isAtLeast(sendEmailUmsSpy.callCount, 1);
                    assert.isDefined(sendEmailUmsSpy.lastCall.args[0].getContent().subject);
                    assert.strictEqual(sendEmailUmsSpy.lastCall.args[0].getContent().subject, Config.userMessage.tenantCheckMoney.subject);
                    assert.strictEqual(gameUnpublishSpy.callCount, 1);
                    assert.strictEqual(unscheduleSpy.callCount, 1);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], done);
    });
    
});