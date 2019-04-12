var _ = require('lodash');
var async = require('async');
var assert = require('chai').assert;
var TaskLock = require('../../classes/TaskLock.js');


var taskIds = [
    'check-tenant-money-2017-12-05', 
    'profile-sync2017-12-05T10:10:00', 
    'voucher-check-2017-12-05'
];

describe('TaskLock', function () {
    
    beforeEach(function (done) {
        async.mapSeries(taskIds, function (item, cbItem) {
            TaskLock.remove(item, cbItem);
        }, done);
        
    });
    
    it('lock()', function (done) {
        
        async.series([
            // lock success
            function (next) {
                TaskLock.lock(taskIds[0], function (err, lock) {
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
                TaskLock.lock(taskIds[1], function (err, lock) {
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
                TaskLock.lock(taskIds[0], function (err, lock) {
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
    
    
    it('lock() concurrency', function (done) {
        var items = [taskIds[0], taskIds[0], taskIds[0], taskIds[0], taskIds[0], taskIds[0], taskIds[0], taskIds[0]];
        var locksResult = [];
        
        async.map(items, function (item, cbItem) {
            TaskLock.lock(item, function (err, lock) {
                if (err) {
                    return cbItem(err);
                }
                
                locksResult.push(lock);
                
                return cbItem();
            });
        }, function (err) {
            try {
                assert.ifError(err);
                var cntTrue = 0;
                var cntFalse = 0;
                
                for(var i=0; i<locksResult.length; i++) {
                    if (locksResult[i] === true) {
                        cntTrue++;
                    }
                    if (locksResult[i] === false) {
                        cntFalse++;
                    }
                }
                
                assert.strictEqual(cntTrue, 1);
                assert.strictEqual(cntFalse, items.length-1);
                
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });

    it('remove()', function (done) {
        
        async.series([
            // lock success
            function (next) {
                TaskLock.lock(taskIds[0], function (err, lock) {
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
                TaskLock.remove(taskIds[0], function (err) {
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