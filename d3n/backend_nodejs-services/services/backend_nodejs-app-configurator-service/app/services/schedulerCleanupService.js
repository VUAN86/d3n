var logger = require('nodejs-logger')();
var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;



module.exports = {
    deleteBefore: function () {
        var offset = 60*60*24*7; // 7 days
        return parseInt(Date.now()/1000)-offset;
    },
    
    generateTaskId: function () {
        return 'scheduler-event-cleanup-' + (new Date()).toISOString().substring(0, 10);
    },
    
    
    cleanup: function (cb) {
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
                        logger.info('schedulerCleanupService.cleanup() task locked by someone else');
                        return cb();
                    }

                    self._cleanup(cb);

                } catch (e) {
                    return cb(e);
                }
            });
            
        } catch (e) {
            logger.error('schedulerCleanupService.cleanup() tc err:', e);
            return setImmediate(cb, e);
        }
    },
    
    _cleanup: function (cb) {
        try {
            logger.debug('schedulerCleanupService._cleanup() called');
            var self = this;
            var ts = self.deleteBefore();
            var transaction = null;
            async.series([
                // create/start transaction
                function (next) {
                    CrudHelper.createTransaction({}, function (err, t) {
                        if (err) {
                            return next(err);
                        }
                        
                        transaction = t;
                        return next();
                    });
                },
                // move to history table
                function (next) {
                    try {
                        var qry = '' + 
                        'INSERT INTO scheduler_event_history(schedulerItemId, scheduledAt, event, emitted) ' + 
                        'SELECT schedulerItemId, scheduledAt, event, emitted FROM scheduler_event WHERE scheduledAt < ' + ts;

                        CrudHelper.rawQuery(qry, {transaction: transaction}, 'INSERT', next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // delete locks
                function (next) {
                    try {
                        var qry ='DELETE FROM scheduler_event_lock WHERE shedulerEventId IN(SELECT id FROM scheduler_event WHERE scheduledAt < ' + ts + ')';

                        CrudHelper.rawQuery(qry, {transaction: transaction}, 'DELETE', next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // delete old events
                function (next) {
                    try {
                        var qry ='DELETE FROM scheduler_event WHERE scheduledAt < ' + ts;

                        CrudHelper.rawQuery(qry, {transaction: transaction}, 'DELETE', next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // commit transaction
                function (next) {
                    transaction.commit().then(next).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    logger.error('schedulerCleanupService._cleanup() err:', err);
                    if(transaction) {
                        transaction.rollback().then(function () {
                            logger.debug('schedulerCleanupService._cleanup() transaction rollback success');
                            return cb(err);
                        }).catch(function (errRollback) {
                            logger.debug('schedulerCleanupService._cleanup() transaction rollback error:', errRollback);
                            return CrudHelper.callbackError(err, cb);
                        });
                    } else {
                        return cb(err);
                    }
                    
                } else {
                    logger.debug('schedulerCleanupService._cleanup() done success');
                    return cb();
                }
            });
        } catch (e) {
            logger.error('schedulerCleanupService._cleanup() tc err:', e);
            return setImmediate(cb, e);
        }
    }
};