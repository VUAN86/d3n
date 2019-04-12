var assert = require('chai').assert;
var SchedulerService = require('../../services/schedulerService.js');
var moment = require('moment');
var async = require('async');
var _ = require('lodash');
var Config = require('./../../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var SchedulerItem = Database.RdbmsService.Models.Scheduler.SchedulerItem;
var SchedulerEvent = Database.RdbmsService.Models.Scheduler.SchedulerEvent;
var SchedulerEventLock = Database.RdbmsService.Models.Scheduler.SchedulerEventLock;
var SchedulerEmitFrom = Database.RdbmsService.Models.Scheduler.SchedulerEmitFrom;
var sinon = require('sinon');
var sinonSandbox = sinon.sandbox.create();

//console.log('>>>:', moment.utc().unix()); process.exit();

describe('Scheduler emit events', function () {
    this.timeout(20000);
    
    beforeEach(function (done) {
        async.series([
            function (next) {
                SchedulerEvent.destroy({where: {}}).then(function () {
                    next();
                }).catch(function (err) {
                    next(err);
                });
            },
            function (next) {
                SchedulerItem.destroy({where: {}}).then(function () {
                    next();
                }).catch(function (err) {
                    next(err);
                });
            },
            
            function (next) {
                SchedulerEventLock.destroy({where: {}}).then(function () {
                    next();
                }).catch(function (err) {
                    next(err);
                });
            }
            
            
        ], done);
    });
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    it('_setTimeouts() called periodically', function (done) {
        var interval = 100;
        var scheduler = new SchedulerService({
            setTimeoutsInterval: interval,
            instanceId: 'localhost:1234'
        });
        
        var setTimeoutsSpy = sinonSandbox.spy(scheduler, "_setTimeouts");
        
        scheduler.init(function (err) {
            try {
                assert.ifError(err);
                
                setTimeout(function () {
                    try {
                        assert.strictEqual(setTimeoutsSpy.callCount, 4);
                        scheduler._stopAllTimers();
                        return done();
                    } catch (e) {
                        return done(e);
                    }
                }, 400);
                
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('_setTimeouts() pull events proper', function (done) {
        //return done();
        var interval = 2000;
        var scheduler = new SchedulerService({
            setTimeoutsInterval: interval,
            instanceId: 'localhost:1234'
        });
        
        //console.log('asda');
        var emittedEvents = [];
        
        var setTimeoutsSpy = sinonSandbox.spy(scheduler, "_setTimeouts");
        var emitEventStub = sinonSandbox.stub(scheduler, "_emitEvent", function (event) {
            emittedEvents.push({
                eventName: event.eventName,
                eventData: event.eventData,
                emittedAt: parseInt(Date.now()/1000)
            });
        });
        var timers = [];
        var events;
        async.series([
            function (next) {
                // scheduleEvents
                var nowSec = parseInt(Date.now()/1000);
                events = [
                    {
                        schedulerItemId: 1,
                        scheduledAt: nowSec+1,
                        event: JSON.stringify({
                            name: 'tombola/openCheckout/1',
                            data: null
                        })
                    },
                    
                    {
                        schedulerItemId: 2,
                        scheduledAt: nowSec + 3,
                        event: JSON.stringify({
                            name: 'tombola/openCheckout/2',
                            data: null
                        })
                    },
                    
                    {
                        schedulerItemId: 3,
                        scheduledAt: nowSec + 5,
                        event: JSON.stringify({
                            name: 'tombola/openCheckout/3',
                            data: null
                        })
                    }
                ];
                
                
                async.mapSeries(events, function (item, cbItem) {
                    
                    SchedulerItem.create({
                        itemId: 'tombola-' + item.schedulerItemId,
                        definition: ''
                    }).then(function () {
                        SchedulerEvent.create(item).then(function () {
                            return cbItem();
                        }).catch(function (err) {
                            return cbItem(err);
                        });
                    }).catch(function (err) {
                        return cbItem(err);
                    });
                    
                }, next);
                
            },
            
            function (next) {
                scheduler.init(next);
            },
            
            function (next) {
                
                
                // after first run
                var t1 = setTimeout(function () {
                    try {
                        assert.strictEqual(emittedEvents.length, 1);
                        assert.strictEqual(_.keys(scheduler._timeouts).length, 1);
                        var event = events[0];
                        assert.deepEqual(emittedEvents[0], {
                            eventName: JSON.parse(event.event).name,
                            eventData: JSON.parse(event.event).data,
                            emittedAt: event.scheduledAt 
                        });
                        //next();
                    } catch (e) {
                        return next(e);
                    }
                }, 2000);
                
                timers.push(t1);
                
                // after second run
                var t2 = setTimeout(function () {
                    try {
                        assert.strictEqual(emittedEvents.length, 2);
                        assert.strictEqual(_.keys(scheduler._timeouts).length, 2);
                        var event = events[1];
                        assert.deepEqual(emittedEvents[1], {
                            eventName: JSON.parse(event.event).name,
                            eventData: JSON.parse(event.event).data,
                            emittedAt: event.scheduledAt 
                        });
                        //next();
                    } catch (e) {
                        return next(e);
                    }
                }, 4000);
                timers.push(t2);
                
                // after 3th run
                var t3 = setTimeout(function () {
                    try {
                        assert.strictEqual(emittedEvents.length, 3);
                        assert.strictEqual(_.keys(scheduler._timeouts).length, 3);
                        var event = events[2];
                        assert.deepEqual(emittedEvents[2], {
                            eventName: JSON.parse(event.event).name,
                            eventData: JSON.parse(event.event).data,
                            emittedAt: event.scheduledAt 
                        });
                        //next();
                    } catch (e) {
                        return next(e);
                    }
                }, 6000);
                timers.push(t3);
                
                // check that events was emitted at specified time
                var t4 = setTimeout(function () {
                    try {
                        
                        var emittedEventsExpected = [];
                        for(var i=0; i<events.length; i++) {
                            var event = events[i];
                            emittedEventsExpected.push({
                                eventName: JSON.parse(event.event).name,
                                eventData: JSON.parse(event.event).data,
                                emittedAt: event.scheduledAt 
                            });
                        }
                        assert.strictEqual(emitEventStub.callCount, 3);
                        assert.strictEqual(emittedEvents.length, events.length);
                        assert.deepEqual(emittedEvents, emittedEventsExpected);
                        //console.log(emittedEvents);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }, 7000);
                timers.push(t4);
            }
            
        ], function (err) {
            for(var i=0; i<timers.length; i++) {
                clearTimeout(timers[i]);
            }
            scheduler._stopAllTimers();
            return done(err);
        });
    });
    
    
    it('_emitEvent()', function (done) {
        return done();
        var receivedItems = [];
        var eventClient = {
            publish: function (eventName, eventData, virtual, cb) {
                receivedItems.push({
                    eventName: eventName,
                    eventData: eventData,
                    virtual: virtual
                });
                return setImmediate(cb);
            }
        };
        var scheduler = new SchedulerService({
            eventClient: eventClient
        });
        
        var items = [];
        var len = 10;
        for(var i=0; i<len; i++) {
            items.push({
                id: 1,
                eventName: 'event/' + i,
                eventData: null,
                virtual: true
            });
        }
        for(var i=0; i<len; i++) {
            scheduler._emitEvent(items[i]);
        }
        
        assert.deepEqual(receivedItems, items);
        
        return done();
    });
    
    it('set proper timeouts for events scheduled before next run', function (done) {
        var receivedItems = [];
        var eventClient = {
            publish: function (eventName, eventData, virtual, cb) {
                receivedItems.push({
                    eventName: eventName,
                    eventData: eventData,
                    virtual: virtual
                });
                return setImmediate(cb);
            }
        };
        var scheduler = new SchedulerService({
            eventClient: eventClient,
            setTimeoutsInterval: 2000,
            instanceId: 'localhost:1234'
        });
        
        var tombolas = [];
        var now = parseInt(Date.now()/1000);
        tombolas.push({
            id: 1,
            startDate: moment.utc((now+1)*1000).format('YYYY-MM-DD HH:mm:ss'),
            endDate: moment.utc((now+1)*1000).format('YYYY-MM-DD HH:mm:ss'),
            targetDate: moment.utc((now+1)*1000).format('YYYY-MM-DD HH:mm:ss')
        });
        
        tombolas.push({
            id: 2,
            startDate: moment.utc((now+3)*1000).format('YYYY-MM-DD HH:mm:ss'),
            endDate: moment.utc((now+3)*1000).format('YYYY-MM-DD HH:mm:ss'),
            targetDate: moment.utc((now+5)*1000).format('YYYY-MM-DD HH:mm:ss')
        });
        
        async.series([
            function (next) {
                try {
                    assert.strictEqual(_.keys(scheduler._timeouts).length, 0);
                    scheduler.init(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // schedule tombolas
            function (next) {
                async.map(tombolas, function (tombola, cbItem) {
                    scheduler.scheduleTombola(tombola, cbItem);
                }, next);
            },
            
            function (next) {
                try {
                    assert.strictEqual(_.keys(scheduler._timeouts).length, 1);
                    var k = _.keys(scheduler._timeouts)[0];
                    assert.strictEqual(scheduler._timeouts[k].length, 3);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // after two seconds events for id=1 are emitted and some events for id=2 are scheduled
            function (next) {
                setTimeout(function () {
                    try {
                        assert.strictEqual(_.keys(scheduler._timeouts).length, 1);
                        var k = _.keys(scheduler._timeouts)[0];
                        assert.strictEqual(scheduler._timeouts[k].length, 2);
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }, 2100);
            }
        ], function (err) {
            scheduler._stopAllTimers();
            return done(err);
        });
        
    });
    
    it('unsheduleTombola', function (done) {
        var receivedItems = [];
        var eventClient = {
            publish: function (eventName, eventData, virtual, cb) {
                receivedItems.push({
                    eventName: eventName,
                    eventData: eventData,
                    virtual: virtual
                });
                return setImmediate(cb);
            }
        };
        var scheduler = new SchedulerService({
            eventClient: eventClient,
            setTimeoutsInterval: 2000,
            instanceId: 'localhost:1234'
        });
        
        var tombolas = [];
        var now = parseInt(Date.now()/1000);
        tombolas.push({
            id: 1,
            startDate: moment.utc((now+1)*1000).format('YYYY-MM-DD HH:mm:ss'),
            endDate: moment.utc((now+1)*1000).format('YYYY-MM-DD HH:mm:ss'),
            targetDate: moment.utc((now+1)*1000).format('YYYY-MM-DD HH:mm:ss')
        });
        
        tombolas.push({
            id: 2,
            startDate: moment.utc((now+3)*1000).format('YYYY-MM-DD HH:mm:ss'),
            endDate: moment.utc((now+3)*1000).format('YYYY-MM-DD HH:mm:ss'),
            targetDate: moment.utc((now+5)*1000).format('YYYY-MM-DD HH:mm:ss')
        });
        
        async.series([
            function (next) {
                try {
                    assert.strictEqual(_.keys(scheduler._timeouts).length, 0);
                    scheduler.init(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // schedule tombolas
            function (next) {
                async.map(tombolas, function (tombola, cbItem) {
                    scheduler.scheduleTombola(tombola, cbItem);
                }, next);
            },
            
            function (next) {
                try {
                    assert.strictEqual(_.keys(scheduler._timeouts).length, 1);
                    var k = _.keys(scheduler._timeouts)[0];
                    assert.strictEqual(scheduler._timeouts[k].length, 3);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                scheduler.unscheduleTombola(1, next);
            },
            
            function (next) {
                try {
                    assert.strictEqual(_.keys(scheduler._timeouts).length, 0);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                SchedulerEvent.count({where: {schedulerItemId: 1}}).then(function (cnt) {
                    try {
                        assert.strictEqual(cnt, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            function (next) {
                SchedulerItem.count({where: {id: 1}}).then(function (cnt) {
                    try {
                        assert.strictEqual(cnt, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
            }
            
        ], function (err) {
            scheduler._stopAllTimers();
            return done(err);
        });
        
    });
    
    it('test concurrency single instance', function (done) {
        this.timeout(10000);
        var receivedItems = [];
        var eventClient = {
            publish: function (eventName, eventData, virtual, cb) {
                receivedItems.push({
                    eventName: eventName,
                    eventData: eventData,
                    virtual: virtual
                });
                return setImmediate(cb);
            }
        };
        var scheduler = new SchedulerService({
            eventClient: eventClient,
            instanceId: 'localhost:1234'
        });
        
        
        var tombolas = [];
        var len = 100;
        var nowSec = parseInt(Date.now()/1000);
        for(var i=1; i<=len; i++) {
            var t = nowSec + _.random(1, 5);
            tombolas.push({
                id: i,
                startDate: moment.utc((t+1)*1000).format('YYYY-MM-DD HH:mm:ss'),
                endDate: moment.utc((t+2)*1000).format('YYYY-MM-DD HH:mm:ss'),
                targetDate: moment.utc((t+3)*1000).format('YYYY-MM-DD HH:mm:ss')
            });
        }
        
        async.series([
            function (next) {
                scheduler.init(next);
            },
            
            // schedule tombolas
            function (next) {
                async.map(tombolas, function (tombola, cbItem) {
                    scheduler.scheduleTombola(tombola, cbItem);
                }, next);
            },
            
            function (next) {
                setTimeout(function () {
                    return next();
                }, 8000);
            },
            
            function (next) {
                try {
                    assert.strictEqual(_.keys(scheduler._timeouts).length, 0);
                    assert.strictEqual(receivedItems.length, len*3);
                    
                    SchedulerEvent.count({where: {emitted: 'no'}}).then(function (cnt) {
                        try {
                            assert.strictEqual(cnt, 0);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return next(err);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                scheduler._stopAllTimers();
                setTimeout(next, 500);
            }
            
        ], done);
        
    });
    
    it('test concurrency multiple instances', function (done) {
        var receivedItems = [];
        var eventClient = {
            publish: function (eventName, eventData, virtual, cb) {
                receivedItems.push({
                    eventName: eventName,
                    eventData: eventData,
                    virtual: virtual
                });
                return setImmediate(cb);
            }
        };
        
        var schedulers = [
            new SchedulerService({eventClient: eventClient, setTimeoutsInterval: 2000, instanceId: 'localhost:1231'}),
            new SchedulerService({eventClient: eventClient, setTimeoutsInterval: 2000, instanceId: 'localhost:1232'}),
            new SchedulerService({eventClient: eventClient, setTimeoutsInterval: 2000, instanceId: 'localhost:1233'}),
            new SchedulerService({eventClient: eventClient, setTimeoutsInterval: 2000, instanceId: 'localhost:1234'}),
            new SchedulerService({eventClient: eventClient, setTimeoutsInterval: 2000, instanceId: 'localhost:1235'})
        ];
        
        
        
        var tombolas = [];
        var len = 100;
        var nowSec = parseInt(Date.now()/1000);
        for(var i=1; i<=len; i++) {
            var t = nowSec + _.random(1, 2);
            //var t = nowSec;
            tombolas.push({
                id: i,
                startDate: moment.utc((t+4)*1000).format('YYYY-MM-DD HH:mm:ss'),
                endDate: moment.utc((t+4)*1000).format('YYYY-MM-DD HH:mm:ss'),
                targetDate: moment.utc((t+4)*1000).format('YYYY-MM-DD HH:mm:ss')
            });
        }
        
        async.series([
            function (next) {
                SchedulerEventLock.count().then(function (cnt) {
                    try {
                        assert.strictEqual(cnt, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    next(err);
                });
            },
            
            function (next) {
                async.mapSeries(schedulers, function (scheduler, cbItem) {
                    scheduler.init(cbItem);
                }, next);
            },
            
            // pick random schedulers and schedule tombolas
            function (next) {
                async.map(tombolas, function (tombola, cbItem) {
                    _.shuffle(schedulers)[0].scheduleTombola(tombola, cbItem);
                }, next);
            },
            
            function (next) {
                setTimeout(function () {
                    return next();
                }, 8000);
            },
            
            function (next) {
                try {
                    for(var i=0; i<schedulers.length; i++) {
                        assert.strictEqual(_.keys(schedulers[i]._timeouts).length, 0);
                    }
                    
                    assert.strictEqual(receivedItems.length, len*3);
                    
                    SchedulerEvent.count({where: {emitted: 'no'}}).then(function (cnt) {
                        try {
                            assert.strictEqual(cnt, 0);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return next(err);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                for(var i=0; i<schedulers.length; i++) {
                    schedulers[i]._stopAllTimers();
                }
                
                setTimeout(next, 500);
            }
            
        ], done);
        
    });
});

