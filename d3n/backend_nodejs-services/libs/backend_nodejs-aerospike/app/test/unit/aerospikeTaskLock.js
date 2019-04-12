var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var TaskLockData = require('./../config/tasklock.data.js');
var Aerospike = require('./../../../index.js').getInstance();
var KeyvalueService = Aerospike.KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;

describe('AerospikeTaskLock', function () {
    
    beforeEach(function (done) {
        var items = TaskLockData.TASK_IDS;
        
        async.mapSeries(items, function (item, cbItem) {
            AerospikeTaskLock.remove(item, cbItem);
        }, done);
        
    });
    
    it('lock()', function (done) {
        
        async.series([
            // lock success
            function (next) {
                AerospikeTaskLock.lock(TaskLockData.TASK_IDS[0], function (err, lock) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(lock, true);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // lock success
            function (next) {
                AerospikeTaskLock.lock(TaskLockData.TASK_IDS[1], function (err, lock) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(lock, true);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // lock return  false. it is already locked
            function (next) {
                AerospikeTaskLock.lock(TaskLockData.TASK_IDS[0], function (err, lock) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(lock, false);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], done);
    });

    it('remove()', function (done) {
        
        async.series([
            // lock success
            function (next) {
                AerospikeTaskLock.lock(TaskLockData.TASK_IDS[0], function (err, lock) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(lock, true);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // remove success
            function (next) {
                AerospikeTaskLock.remove(TaskLockData.TASK_IDS[0], function (err) {
                    try {
                        assert.ifError(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            
        ], done);
    });
    
});
