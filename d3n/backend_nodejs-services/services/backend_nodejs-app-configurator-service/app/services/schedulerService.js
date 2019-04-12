var _ = require('lodash');
var async = require('async');
var cp = require('child_process');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var SchedulerItem = Database.RdbmsService.Models.Scheduler.SchedulerItem;
var SchedulerEvent = Database.RdbmsService.Models.Scheduler.SchedulerEvent;
var SchedulerEventLock = Database.RdbmsService.Models.Scheduler.SchedulerEventLock;
var SchedulerEmitFrom = Database.RdbmsService.Models.Scheduler.SchedulerEmitFrom;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var moment = require('moment');

var QUEUE_SCHEDULE_TIMEOUT = 1000*30; // 30 seconds

function SchedulerService (config) {
    
    this._config = config || {};
    
    this._instanceId = this._config.instanceId;
    if (!this._instanceId) {
        throw new Error('ERR_INSTANCE_ID_IS_REQUIRED');
    }
    
    this._service = this._config.service;
    
    this._eventClient = null;
    if (this._service) {
        this._eventClient = this._service.getEventService();
    } else {
        this._eventClient = this._config.eventClient;
    }
    
    this._timeouts = {};
    
    var runCleanup = _.has(config, 'runCleanup') ? config.runCleanup : true;
    
    if (runCleanup === true) {
        this._cleanupInterval = this._config.cleanupInterval || process.env.SCHEDULER_CLEANUP_INTERVAL || 1000*60*30; // run clean up every 30 minutes
        this._cleanupInterval = parseInt(this._cleanupInterval);
        setTimeout(this._cleanup.bind(this), this._cleanupInterval);
    }
    
    
    this._setTimeoutsInterval = this._config.setTimeoutsInterval || process.env.SCHEDULER_SET_TIMEOUTS_INTERVAL || 1000*60*10; // set timeouts every 10 minutes
    this._setTimeoutsInterval = parseInt(this._setTimeoutsInterval);
    
    this._setTimeoutsTimeoutId = null;
    
    this._queueSchedule = async.queue(this._queueScheduleProcessTask.bind(this), 1);
    this._queueSchedule.error = function (err, task) {
        logger.error('SchedulerService error on _queueSchedule:', err, task);
    };
    
};

var o = SchedulerService.prototype;

o._stopAllTimers = function () {
    if (this._setTimeoutsTimeoutId) {
        clearTimeout(this._setTimeoutsTimeoutId);
    }
    
    
    for(var k in this._timeouts) {
        var items = this._timeouts[k];
        for(var i=0; i<items.length; i++) {
            clearTimeout(items[i]);
        }
    }
    this._timeouts = {};
};

o._cleanup = function () {
    try {
        logger.debug('SchedulerService._cleanup() success');
        setTimeout(this._cleanup.bind(this), this._cleanupInterval);
    } catch (e) {
        setTimeout(this._cleanup.bind(this), this._cleanupInterval);
        logger.error('SchedulerService._cleanup() error:', e);
    }
};


o._emitEvent = function (event) {
    try {
        var self = this;
        
        function _deleteTimeout() {
            if (_.isArray(self._timeouts[event.schedulerItemId])) {
                _.pull(self._timeouts[event.schedulerItemId], event.timeoutId);

                if (self._timeouts[event.schedulerItemId].length === 0) {
                    delete self._timeouts[event.schedulerItemId];
                }
            }
        };
        
        async.series([
            
            // check if event is already emitted or doesn't exists anymore
            function (next) {
                SchedulerEvent.findOne({where: {id: event.id, emitted: 'no'}}).then(function (dbItem) {
                    try {
                        if (!dbItem) { // event was already emitted or doesn't exists anymore: tombola/tournament deleted for example
                            _deleteTimeout();
                            return next(new Error('SKIP'));
                        }
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, next);
                });
                
            },
            
            // try to lock the event
            function (next) {
                SchedulerEventLock.create({shedulerEventId: event.id}).then(function (dbItem) {
                    // locked succesfully, start emitting event
                    return next();
                }).catch (function (err) {
                    try {
                        if (err.original.errno === 1062) { // duplicate error, that means the event was taken by other instance https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
                            _deleteTimeout();
                            return next(new Error('SKIP'));
                        }
                        return next(err);
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // emit event
            function (next) {
                try {
                    self._eventClient.publish(event.eventName, event.eventData, true, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // emit internal event
            function (next) {
                try {
                    self._service.emitEvent('schedulerEvent', event.eventName);
                    return setImmediate(next);
                } catch (e) {
                    logger.warn('SchedulerService._emitEvent() internal event emit error:', e);
                    return setImmediate(next);
                }
            },
            
            // mark db event as emitted
            function (next) {
                try {
                    SchedulerEvent.update({
                        emitted: 'yes'
                    }, {where: {id: event.id}}).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // delete timeout
            function (next) {
                try {
                    _deleteTimeout();
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
            
        ], function (err) {
            if (err) {
                if (err.message === 'SKIP') {
                    return;
                }
                logger.error('SchedulerService._emitEvent() error:', err, event.eventName, event.eventData);
                return false;
            } else {
                logger.debug('SchedulerService._emitEvent() success:', event.eventName, event.eventData, self._instanceId);
                return true;
            }
        });
        
        
    } catch (e) {
        logger.error('SchedulerService._emitEvent() catched error:', e);
        return false;
    }
};


o._setTimeoutsForEvents = function (events) {
    var self = this;
    var now = Date.now();
    for(var i=0; i<events.length; i++) {
        var event = events[i];
        var eventEvent = JSON.parse(event.event);

        var ms = (event.scheduledAt*1000) - now;

        var emitEventData = {
            eventName: eventEvent.name,
            eventData: eventEvent.data,
            schedulerItemId : event.schedulerItemId,
            id: event.id
        };

        var timeoutId = setTimeout(self._emitEvent.bind(self), ms, emitEventData);
        emitEventData.timeoutId = timeoutId;

        if (!_.has(self._timeouts, event.schedulerItemId)) {
            self._timeouts[event.schedulerItemId] = [];
        }

        self._timeouts[event.schedulerItemId].push(timeoutId);
    }
};

o._setTimeouts = function (cb) {
    try {
        logger.debug('SchedulerService._setTimeouts() called');
        var self = this;
        
        function _doSetTimeouts(cb) {
            try {
                logger.debug('SchedulerService._doSetTimeouts() start');
                var tstart = Date.now();
                var emitFrom;
                var setTimeoutsIntervalSec = parseInt(self._setTimeoutsInterval/1000);
                var emitTo = parseInt(Date.now()/1000) + setTimeoutsIntervalSec;
                
                var events = [];
                async.series([
                    // get emitFrom
                    function (next) {
                        //return setImmediate(next, new Error('ERR_TEST'));
                        SchedulerEmitFrom.findOne({where: {instanceId: self._instanceId}}).then(function (dbItem) {
                            try {
                                if (!dbItem) {
                                    logger.error('SchedulerService._doSetTimeouts() ERR_EMIT_FROM_NOT_FOUND', self._instanceId);
                                    return next(new Error('ERR_EMIT_FROM_NOT_FOUND'));
                                }
                                emitFrom = dbItem.get({plain: true}).emitFrom;
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    },
                    // update emitFrom
                    function (next) {
                        try {
                            SchedulerEmitFrom.update({
                                emitFrom: emitTo
                            }, {where: {instanceId: self._instanceId}}).then(function () {
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                            
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // load events
                    function (next) {
                        try {

                            var options = {
                                where: {
                                    emitted: 'no',
                                    scheduledAt: {
                                        $gte: emitFrom,
                                        $lt: emitTo
                                    }
                                }
                            };
                            var startLoadEvents = Date.now();
                            SchedulerEvent.findAll(options).then(function (dbItems) {
                                try {
                                    for(var i=0; i<dbItems.length; i++) {
                                        events.push(dbItems[i].get({plain: true}));
                                    }
                                    logger.debug('SchedulerService._doSetTimeouts() load events duration:', (Date.now()-startLoadEvents));
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });

                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // set timeouts
                    function (next) {
                        try {
                            self._setTimeoutsForEvents(events);
                            return setImmediate(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], function (err) {
                    var duration = (Date.now()-tstart);
                    logger.debug('SchedulerService._doSetTimeouts() end', duration, err);
                    if(duration > (1000*60)) { // 1 minute
                        logger.error('SchedulerService._doSetTimeouts() took too much', duration);
                    }
                    if (err) {
                        logger.error('SchedulerService._doSetTimeouts() error:', err);
                    }
                    return cb(err);
                });
                
            } catch (e) {
                return setImmediate(cb, e);
            }
        };
        
        
        async.retry({
            times: 1,
            interval: 1000
        }, _doSetTimeouts, function (err) {
            if (err) {
                logger.error('SchedulerService._setTimeouts() error  after retries:', err);
            } else {
                logger.debug('SchedulerService._setTimeouts() completed');
            }
            self._setTimeoutsTimeoutId = setTimeout(self._setTimeouts.bind(self), self._setTimeoutsInterval);
            if (cb) {
                return cb();
            }
        });
        
    } catch (e) {
        logger.error('SchedulerService._setTimeouts() catched error:', e);
        if (cb) {
            return cb(e);
        }
        self._setTimeoutsTimeoutId = setTimeout(self._setTimeouts.bind(self), self._setTimeoutsInterval);
    }
};

o.init = function (cb) {
    try {
        var self = this;
        var emitFromExists;
        logger.debug('SchedulerService.init() start');
        async.series([
            function (next) {
                SchedulerEmitFrom.count({where: {instanceId: self._instanceId}}).then(function (count) {
                    emitFromExists = (count ? true : false);
                    return next();
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, next);
                });
            },
            
            function (next) {
                if (emitFromExists) {
                    SchedulerEmitFrom.update({
                        emitFrom: parseInt(Date.now()/1000)
                    }, {where: {instanceId: self._instanceId}}).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } else {
                    SchedulerEmitFrom.create({
                        emitFrom: parseInt(Date.now()/1000),
                        instanceId: self._instanceId
                    }).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            },
            
            function (next) {
                self._setTimeouts(next);
            }
        ], function (err) {
            if (err) {
                return cb(err);
            }
            logger.debug('SchedulerService.init() done');
            return cb();
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._queueScheduleProcessTask = function (task, cb) {
    logger.debug('_queueScheduleProcessTask :', task);
    try {
        var self = this;
        
        var cbCalled = false;
        function callCb(err) {
            if (cbCalled === false) {
                cbCalled = true;
                return cb(err);
            }
            
            logger.error('SchedulerService._queueScheduleProcessTask() calback already called:', task, cb);
        }
        
        if (task.type === 'tombola') {
            return async.timeout(self._scheduleTombola.bind(self), QUEUE_SCHEDULE_TIMEOUT)(task, callCb);
        }
        
        if (task.type === 'liveTournament') {
            return async.timeout(self._scheduleLiveTournament.bind(self), QUEUE_SCHEDULE_TIMEOUT)(task, callCb);
        }
        
        if (task.type === 'unschedule') {
            return async.timeout(self._unschedule.bind(self), QUEUE_SCHEDULE_TIMEOUT)(task, callCb);
        }
        
        
    } catch (e) {
        logger.error('SchedulerService._queueScheduleProcessTask() error:', e, task);
        return setImmediate(cb, e);
    }
};

o._unschedule = function (task, cb) {
    try {
        var self = this;
        var schedulerItemItemId = task.schedulerItemItemId;
        
        var schedulerItem;
        async.series([
            function (next) {
                SchedulerItem.findOne({where: {itemId: schedulerItemItemId}}).then(function (dbItem) {
                    if (dbItem) {
                        schedulerItem = dbItem.get({plain: true});
                        return next();
                    }
                    
                    return next(new Error('ERR_SCHEDULER_ITEM_NOT_FOUND'));
                    
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, next);
                });
            },
            
            // clear existing timeouts
            function (next) {
                try {
                    var timeouts = self._timeouts[schedulerItem.id]; 
                    if (_.isArray(timeouts)) {
                        for(var i=0; i<timeouts.length; i++) {
                            clearTimeout(timeouts[i]);
                        }
                        
                        delete self._timeouts[schedulerItem.id];
                    }
                    
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // delete events
            function (next) {
                SchedulerEvent.destroy({where: {schedulerItemId: schedulerItem.id}}).then(function () {
                    return next();
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, next);
                });
            },
            
            // delete scheduler item
            function (next) {
                SchedulerItem.destroy({where: {id: schedulerItem.id}}).then(function () {
                    return next();
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, next);
                });
            }
            
        ], function (err) {
            if (err) {
                if (err.message === 'ERR_SCHEDULER_ITEM_NOT_FOUND') { // ignore if item doesn't exists
                    return cb();
                }
                return cb(err);
            }
            
            return cb();
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._toTimestamp = function (date) {
    if (_.isInteger(date)) {
        return date;
    }
    
    if (_.isString(date)) {
        var d = moment.utc(date);
        if(!d.isValid()) {
            logger.error('SchedulerService._toTimestamp() wrong date:', date);
            throw new Error('ERR_VALIDATION_FAILED');
        }
        
        return d.unix();
    }
    
    if (date instanceof Date) {
        return parseInt(date.getTime()/1000);
    }
    
    logger.error('SchedulerService._toTimestamp() invalid date type:', date);
    throw new Error('ERR_VALIDATION_FAILED');
};

o._upsertSchedulerItemAndEvent = function (itemId, scheduleDefinition, events, cb) {
    try {
        var self = this;
        
        var schedulerItem = null;
        var nowSeconds = parseInt(Date.now()/1000);
        async.series([
            // get scheduler item if exists
            function (next) {
                SchedulerItem.findOne({where: {itemId: itemId}}).then(function (dbItem) {
                    if (dbItem) {
                        schedulerItem = dbItem.get({plain: true});
                    }
                    return next();
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, next);
                });
            },
            
            // clean existing events and timeouts
            function (next) {
                try {
                    if (!schedulerItem) {
                        return setImmediate(next);
                    }
                    
                    var timeouts = self._timeouts[schedulerItem.id]; 
                    if (_.isArray(timeouts)) {
                        for(var i=0; i<timeouts.length; i++) {
                            clearTimeout(timeouts[i]);
                        }
                        
                        delete self._timeouts[schedulerItem.id];
                    }
                    
                    
                    SchedulerEvent.destroy({where: {schedulerItemId: schedulerItem.id}}).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // add/update scheduler item
            function (next) {
                try {
                    if (schedulerItem) {
                        SchedulerItem.update({
                            definition: JSON.stringify(scheduleDefinition)
                        }, {where: {id: schedulerItem.id}}).then(function (count) {
                            if (count[0] === 0) {
                                logger.error('SchedulerService._upsertSchedulerItemAndEvent() no schedulerItem record found:', itemId);
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        SchedulerItem.create({
                            itemId: itemId,
                            definition: JSON.stringify(scheduleDefinition)
                        }).then(function (dbItem) {
                            try {
                                schedulerItem = dbItem.get({plain: true});
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // add events
            function (next) {
                try {
                    var items = [];
                    for(var i=0; i<events.length; i++) {
                        var event = events[i];
                        items.push({
                            schedulerItemId: schedulerItem.id,
                            scheduledAt: event.scheduledAt,
                            event: JSON.stringify({
                                name: event.eventName,
                                data: event.eventData
                            })
                        });
                    }
                    SchedulerEvent.bulkCreate(items).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // set timeots for events scheduled before next _setTimeouts() runs
            function (next) {
                try {
                    var emitFrom;
                    async.series([
                        function (next2) {
                            SchedulerEmitFrom.findOne({where: {instanceId: self._instanceId}}).then(function (dbItem) {
                                try {
                                    if (!dbItem) {
                                        return next2(new Error('ERR_EMIT_FROM_NOT_FOUND'));
                                    }
                                    emitFrom = dbItem.get({plain: true}).emitFrom;
                                    return next2();
                                } catch (e) {
                                    return next2(e);
                                }
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next2);
                            });
                        },
                        
                        function (next2) {

                            var options = {
                                where: {
                                    emitted: 'no',
                                    scheduledAt: {
                                        $gte: nowSeconds,
                                        $lt: emitFrom
                                    },
                                    schedulerItemId: schedulerItem.id
                                }
                            };

                            SchedulerEvent.findAll(options).then(function (dbItems) {
                                try {
                                    var eventsToEmit = [];
                                    for(var i=0; i<dbItems.length; i++) {
                                        eventsToEmit.push(dbItems[i].get({plain: true}));
                                    }
                                    
                                    if (eventsToEmit.length) {
                                        self._setTimeoutsForEvents(eventsToEmit);
                                    }
                                    
                                    return next2();
                                } catch (e) {
                                    return next2(e);
                                }
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next2);
                            });
                            
                        }
                    ], next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._scheduleTombola = function (task, cb) {
    try {
        logger.debug('_scheduleTombola task:', task);
        var self = this;
        var tombola = task.tombola;
        var itemId = 'tombola-' + tombola.id;
        
        var scheduleDefinition = null;
        var insert = false;
        async.series([
            
            // schedule definition
            function (next) {
                try {
                    scheduleDefinition = {
                        startDate: self._toTimestamp(tombola.startDate),
                        endDate: self._toTimestamp(tombola.endDate),
                        targetDate: self._toTimestamp(tombola.targetDate),
                        preCloseOffsetMinutes: tombola.preCloseOffsetMinutes
                    };
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // generate events out of definition
            function (next) {
                try {
                    var scheduledEvents = [];
                    var event;

                    // for startDate
                    var nowSeconds = parseInt(Date.now()/1000);
                    if (scheduleDefinition.endDate > nowSeconds) {
                        scheduledEvents.push({
                            scheduledAt: (scheduleDefinition.startDate <= nowSeconds ? nowSeconds + 10 : scheduleDefinition.startDate),
                            eventName: SchedulerService.eventPrefix.tombola.openCheckout + tombola.id,
                            eventData: _.has(tombola, 'eventsData') ? _.assign({}, tombola.eventsData) : null
                        });
                    }
                    
                    // for endDate
                    scheduledEvents.push({
                        scheduledAt: (scheduleDefinition.endDate <= nowSeconds ? nowSeconds+10 : scheduleDefinition.endDate),
                        eventName: SchedulerService.eventPrefix.tombola.closeCheckout + tombola.id,
                        eventData: _.has(tombola, 'eventsData')  ? _.assign({}, tombola.eventsData) : null
                    });

                    // preCloseCheckout
                    if (_.isInteger(scheduleDefinition.preCloseOffsetMinutes)) {
                        event = {
                            scheduledAt: scheduleDefinition.endDate-(scheduleDefinition.preCloseOffsetMinutes*60),
                            eventName: SchedulerService.eventPrefix.tombola.preCloseCheckout + tombola.id,
                            eventData: _.has(tombola, 'eventsData')  ? _.assign({}, tombola.eventsData) : {}
                        };
                        event.eventData.minutesToCheckout = scheduleDefinition.preCloseOffsetMinutes;
                        scheduledEvents.push(event);
                    }
                    
                    // for targetDate
                    scheduledEvents.push({
                        scheduledAt: (scheduleDefinition.targetDate <= nowSeconds ? nowSeconds+10 : scheduleDefinition.targetDate),
                        eventName: SchedulerService.eventPrefix.tombola.draw + tombola.id,
                        eventData: _.has(tombola, 'eventsData')  ? _.assign({}, tombola.eventsData) : null
                    });
                    
                    self._upsertSchedulerItemAndEvent(itemId, scheduleDefinition, scheduledEvents, next);
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
            
        ], cb);
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.scheduleTombola = function (tombola, cb) {
    try {
        this._queueSchedule.push({
            type: 'tombola',
            tombola: tombola
        }, cb);
    } catch (e) {
        logger.error('SchedulerService.scheduleTombola() catched error:', e);
        return setImmediate(cb, e);
    }
};

o.unscheduleTombola = function (id, cb) {
    try {
        this._queueSchedule.push({
            type: 'unschedule',
            schedulerItemItemId: 'tombola-' + id
        }, cb);
    } catch (e) {
        logger.error('SchedulerService.unscheduleTombola() catched error:', e);
        return setImmediate(cb, e);
    }
};



o._expandRepetition = function (repetitionDef, startDateTimestamp, endDateTimestamp, repeatCount) {
    if (!startDateTimestamp || _.isEmpty(repetitionDef) || _.isNaN(repetitionDef.repeat) || _.isEmpty(repetitionDef.unit) ||
        parseInt(repetitionDef.repeat) === 0 || _.isEmpty(repetitionDef.repeatOn)
    ) {
        logger.error('SchedulerService._expandRepetition() validation error:', repetitionDef, startDateTimestamp, endDateTimestamp);
        throw new Error('ERR_VALIDATION_FAILED');
    }
    
    var startDate = moment.utc(startDateTimestamp*1000);
    var endDate = null;
    if (endDateTimestamp) {
        endDate = moment.utc(endDateTimestamp*1000);
    }
    
    var cnt = 0;
    var totalRepeats = 0;
    var repetitions = [];
    
    repetitionDef.exceptDays = repetitionDef.exceptDays || [];
    
    whileloop:
    while(1) {
        var date = startDate.clone().add(parseInt(repetitionDef.repeat)*cnt, repetitionDef.unit + 's');
        
        // finish by endDate
        if (endDate && date.unix() > endDate.unix()) {
            break;
        }

        // finish by repeatCount
        if (repeatCount && totalRepeats === repeatCount) {
            break;
        }
        
        
        if (repetitionDef.unit === 'week' || repetitionDef.unit === 'month') {
            
            var arrRepeatOn = repetitionDef.repeatOn.split(',');
            for(var i=0; i<arrRepeatOn.length; i++) {
                var parts = arrRepeatOn[i].trim().split(' ');
                var d = parts[0];
                var hhmm = parts[1].trim().split(':');
                var hh = hhmm[0];
                var mm = hhmm[1];
                var dt;
                
                if (repetitionDef.unit === 'week') {
                    dt = date.clone().isoWeekday(d).hour(hh).minute(mm).second(0);
                } else if (repetitionDef.unit === 'month') {
                    var lastDayOfMonth = date.clone().endOf('month').date();

                    // if day > last day of month then use last day of month
                    if (parseInt(d) > lastDayOfMonth) {
                        d = lastDayOfMonth;
                    }
                    
                    dt = date.clone().date(parseInt(d)).hour(hh).minute(mm).second(0);
                }
                
                // skip exceptions
                if (repetitionDef.exceptDays.indexOf(dt.clone().startOf('day').unix()) !== -1) {
                    continue;
                }

                if (endDate && dt.unix() > endDate.unix()) {
                    continue;
                }
                
                if (dt.unix() < startDate.unix()) {
                    continue;
                }

                repetitions.push(dt.unix());
                
            }
            totalRepeats++;
            cnt++;
        }

        if (repetitionDef.unit === 'day') {
            
            // skip exceptions
            if (repetitionDef.exceptDays.indexOf(date.clone().startOf('day').unix()) !== -1) {
                cnt++;
                continue;
            }

            // repeatOn will contain only hour and minute
            var arrRepeatOn = repetitionDef.repeatOn.split(',');
            for(var i=0; i<arrRepeatOn.length; i++) {
                var hm = arrRepeatOn[i].trim().split(':');
                var dt = date.clone().hour(hm[0]).minute(hm[1]).second(0);
                
                if (endDate && dt.unix() > endDate.unix()) {
                    continue;
                }
                
                if (dt.unix() < startDate.unix()) {
                    continue;
                }
                
                repetitions.push(dt.unix());
            }
            totalRepeats++;
            cnt++;
        }
    }
    
    return repetitions;
};


o._buildRepetitionEvents = function (scheduleDefinition, liveTournamentId, eventsData, tournamentType) {
    var self = this;
    var events = [];
    var repetitionDef = scheduleDefinition.repetition;
    var eventPrefixKey = tournamentType + 'Tournament';
    
    if (repetitionDef) {
        var startDateTimestamp = repetitionDef.startDate;
        var repeatCount = repetitionDef.repeatCount;
        var endDateTimestamp = repetitionDef.endDate;
        
        if (!_.has(repetitionDef, 'endDate') && !_.has(repetitionDef, 'repeatCount')) { // never ending repetition
            repeatCount = Config.defaultRepeatCount;
            endDateTimestamp = undefined;
        }
        
        var repetitions = self._expandRepetition(repetitionDef, startDateTimestamp, endDateTimestamp, repeatCount);
        
        
        for(var i=0; i<repetitions.length; i++) {
            var repeatAt = moment.utc(repetitions[i]*1000);
            
            // calculate time for open registration event
            var openRegistrationTime = repeatAt.clone().subtract(repetitionDef.offsetOpenRegistration.offset, repetitionDef.offsetOpenRegistration.unit+'s').unix();

            // calculate time for start game event
            var startGameTime = repeatAt.clone().subtract(repetitionDef.offsetStartGame.offset, repetitionDef.offsetStartGame.unit+'s').unix();
            
            /*
            // if before startDate, skip
            if (openRegistrationTime < startDateTimestamp || startGameTime < startDateTimestamp) {
                logger.error('SchedulerService _buildRepetitionEvents() startGameTime/openRegistrationTime before startDate:', JSON.stringify(scheduleDefinition));
                continue;
            }
            */
            var event = {
                scheduledAt: openRegistrationTime,
                eventName: SchedulerService.eventPrefix[eventPrefixKey].openRegistration + liveTournamentId,
                eventData: !_.isUndefined(eventsData) ? _.assign({}, eventsData) : null
            };
            if (event.eventData === null) {
                event.eventData = {};
            }
            event.eventData.repetition = repeatAt.format("YYYY-MM-DDTHH:mm:ss") + "Z";
            event.eventData.startGameDateTime = moment.utc(startGameTime*1000).format("YYYY-MM-DDTHH:mm:ss") + "Z";
            
            events.push(event);
            
        }
        
    } else {
        events = [
            {
                scheduledAt: scheduleDefinition.startDate,
                eventName: SchedulerService.eventPrefix[eventPrefixKey].openRegistration + liveTournamentId,
                eventData: !_.isUndefined(eventsData) ? _.assign({}, eventsData) : null
            }
        ];
    }

    return events;
};

o._exceptDaysToTimestamp = function (exceptDays) {
    var self = this;
    return exceptDays.map(function (item) {
        return self._toTimestamp(item);
    });    
};

o._scheduleLiveTournament = function (task, cb) {
    try {
        var self = this;
        var liveTournament = task.liveTournament;
        var itemId = 'liveTournament-' + liveTournament.id;
        
        var scheduleDefinition = null;
        var insert = false;
        
        
        async.series([
            
            // schedule definition
            function (next) {
                try {
                    if (liveTournament.repetition) {
                        scheduleDefinition = {
                            repetition: {
                                startDate: self._toTimestamp(liveTournament.repetition.startDate),
                                unit: liveTournament.repetition.unit,
                                repeat: liveTournament.repetition.repeat,
                                repeatOn: liveTournament.repetition.repeatOn,
                                exceptDays: liveTournament.repetition.exceptDays,
                                offsetOpenRegistration: liveTournament.repetition.offsetOpenRegistration,
                                offsetStartGame: liveTournament.repetition.offsetStartGame
                            }
                        };
                        
                        if (_.has(liveTournament, 'startDate')) {
                            scheduleDefinition.startDate = self._toTimestamp(liveTournament.startDate);
                        }
                        if (_.has(liveTournament, 'endDate')) {
                            scheduleDefinition.endDate = self._toTimestamp(liveTournament.endDate);
                        }
                        
                        
                        if (_.has(liveTournament.repetition, 'repeatCount')) {
                            scheduleDefinition.repetition.repeatCount = liveTournament.repetition.repeatCount;
                        }
                        if (_.has(liveTournament.repetition, 'endDate')) {
                            scheduleDefinition.repetition.endDate = self._toTimestamp(liveTournament.repetition.endDate);
                        }
                        
                        
                        scheduleDefinition.repetition.exceptDays = scheduleDefinition.repetition.exceptDays || [];
                        scheduleDefinition.repetition.exceptDays = self._exceptDaysToTimestamp(scheduleDefinition.repetition.exceptDays);
                        
                        
                    } else {
                        scheduleDefinition = {
                            startDate: self._toTimestamp(liveTournament.startDate),
                            endDate: self._toTimestamp(liveTournament.endDate),
                            repetition: null
                        };
                    }
                    
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // generate events out of definition
            function (next) {
                try {
                    var scheduledEvents = self._buildRepetitionEvents(scheduleDefinition, liveTournament.id, liveTournament.eventsData, liveTournament.type);
                    
                    self._upsertSchedulerItemAndEvent(itemId, scheduleDefinition, scheduledEvents, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
            
        ], cb);
        
    } catch (e) {
        return setImmediate(cb, e);
    }    
};

o.scheduleLiveTournament = function (liveTournament, cb) {
    try {
        this._queueSchedule.push({
            type: 'liveTournament',
            liveTournament: liveTournament
        }, cb);
        
    } catch (e) {
        logger.error('SchedulerService.scheduleLiveTournament() catched error:', e);
        return setImmediate(cb, e);
    }
};

o.unscheduleLiveTournament = function (id, cb) {
    try {
        this._queueSchedule.push({
            type: 'unschedule',
            schedulerItemItemId: 'liveTournament-' + id
        }, cb);
    } catch (e) {
        logger.error('SchedulerService.unscheduleLiveTournament() catched error:', e);
        return setImmediate(cb, e);
    }
};


SchedulerService.eventPrefix = {
    'tombola': {
        'openCheckout': 'tombola/openCheckout/',
        'closeCheckout': 'tombola/closeCheckout/',
        'draw': 'tombola/draw/',
        'preCloseCheckout': 'tombola/preCloseCheckout/'
    },
    
    'liveTournament': {
        'openRegistration': 'tournament/openRegistration/',
        'startGame': 'tournament/live/startGame/'
    },
    
    'normalTournament': {
        'openRegistration': 'tournament/openRegistration/'
    }
    
};

module.exports = SchedulerService;
