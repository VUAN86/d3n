var assert = require('chai').assert;
var async = require('async');
var _ = require('lodash');
var ClientInfo = require('nodejs-utils').ClientInfo;
var ProtocolMessage = require('nodejs-protocol');
var Config = require('./../../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var SchedulerEvent = Database.RdbmsService.Models.Scheduler.SchedulerEvent;
var SchedulerEventLock = Database.RdbmsService.Models.Scheduler.SchedulerEventLock;
var SchedulerItem = Database.RdbmsService.Models.Scheduler.SchedulerItem;
var SchedulerEventHistory = Database.RdbmsService.Models.Scheduler.SchedulerEventHistory;
var sinon = require('sinon');
var sinonSandbox = sinon.sandbox.create();
var schedulerCleanupService = require('../../services/schedulerCleanupService.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;



describe('TEST SCHEDULER CLEAN UP', function () {
    this.timeout(20000);
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    beforeEach(function (done) {
        async.series([
            function (next) {
                try {
                    SchedulerEvent.destroy({truncate: true}).then(next).catch(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            function (next) {
                try {
                    SchedulerEventHistory.destroy({truncate: true}).then(next).catch(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            function (next) {
                try {
                    AerospikeTaskLock.remove(schedulerCleanupService.generateTaskId(), done);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }

        ], done);
    });
    
    /*
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
    */
    
    it('cleanup() concurrency', function (done) {
        
        var _cleanupSpy = sinonSandbox.stub(schedulerCleanupService, '_cleanup', function (cb) {
            return setImmediate(cb);
        });
        
        async.map([1,2,3,4,5,6,7,,8,9,10], function (i, cbItem) {
            schedulerCleanupService.cleanup(cbItem);
        }, function (err) {
            try {
                assert.ifError(err);
                assert.strictEqual(_cleanupSpy.callCount, 1);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('_cleanup()', function (done) {
        
        var schedulerItemId;
        var numItems = 10;
        var schedulerEventIds = [];
        async.series([
            // ad scheduler item
            function (next) {
                try {
                    SchedulerItem.create({
                        itemId: 'www',
                        definition: 'def'
                    }).then(function (dbItem) {
                        schedulerItemId = dbItem.get({plain: true}).id;
                        return next();
                    }).catch(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            // add events
            function (next) {
                try {
                    var ts = schedulerCleanupService.deleteBefore();
                    var events = [];
                    // events which will be archived
                    for(var i=0; i<numItems; i++) {
                        events.push({
                            schedulerItemId: schedulerItemId,
                            scheduledAt: ts-(i+1000),
                            event: 'asdas',
                            emitted: 'yes'
                            
                        });
                    }
                    
                    // events which will be kept
                    for(var i=0; i<numItems; i++) {
                        events.push({
                            schedulerItemId: schedulerItemId,
                            scheduledAt: ts+(i+1000),
                            event: 'asdas',
                            emitted: 'no'
                            
                        });
                    }
                    
                    
                    SchedulerEvent.bulkCreate(events).then(function () {
                        return next();
                    }).catch(next);
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // add locks
            function (next) {
                try {
                    SchedulerEvent.findAll().then(function (dbItems) {
                        var locks = [];
                        for(var i=0; i<dbItems.length; i++) {
                            locks.push({shedulerEventId: dbItems[i].get({plain: true}).id});
                        }
                        
                        SchedulerEventLock.bulkCreate(locks).then(function () {
                            return next();
                        }).catch(next);
                        
                    }).catch(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            
            function (next) {
                try {
                    schedulerCleanupService.cleanup(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // event history count ok
            function (next) {
                try {
                    SchedulerEventHistory.count().then(function (cnt) {
                        try {
                            assert.strictEqual(cnt, numItems);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            // event count ok
            function (next) {
                try {
                    SchedulerEvent.count().then(function (cnt) {
                        try {
                            assert.strictEqual(cnt, numItems);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // locks count ok
            function (next) {
                try {
                    SchedulerEventLock.count().then(function (cnt) {
                        try {
                            assert.strictEqual(cnt, numItems);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }



        ], done);
    });
});