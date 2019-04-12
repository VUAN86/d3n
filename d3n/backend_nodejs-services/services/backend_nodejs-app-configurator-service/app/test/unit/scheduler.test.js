var assert = require('chai').assert;
var SchedulerService = require('../../services/schedulerService.js');
var moment = require('moment');
var async = require('async');
var _ = require('lodash');
var Config = require('./../../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var SchedulerItem = Database.RdbmsService.Models.Scheduler.SchedulerItem;
var SchedulerEvent = Database.RdbmsService.Models.Scheduler.SchedulerEvent;
var SchedulerEmitFrom = Database.RdbmsService.Models.Scheduler.SchedulerEmitFrom;
var sinon = require('sinon');
var sinonSandbox = sinon.sandbox.create();


describe('Scheduler Service Basic Tests', function () {
    this.timeout(20000);
    
    it('constructor', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        
        assert.strictEqual(scheduler._setTimeoutsInterval, 1000*60*10);
        
        scheduler = new SchedulerService({
            setTimeoutsInterval: 3000,
            instanceId: 'localhost:1234'
        });
        assert.strictEqual(scheduler._setTimeoutsInterval, 3000);

        process.env.SCHEDULER_SET_TIMEOUTS_INTERVAL = 4000;
        scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        assert.strictEqual(scheduler._setTimeoutsInterval, 4000);
        
        delete process.env.SCHEDULER_SET_TIMEOUTS_INTERVAL;
        return done();
    });
    
    it('_toTimestamp()', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});

        var ts = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        assert.strictEqual(scheduler._toTimestamp('2017-02-09T14:23:56'), ts);

        var ts = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        assert.strictEqual(scheduler._toTimestamp('2017-02-09 14:23:56'), ts);


        var d = new Date('2017-02-09T14:23:56');
        assert.strictEqual(scheduler._toTimestamp(d), ts);

        // invalid date format
        assert.throws(function () {scheduler._toTimestamp('sadaas');}, 'ERR_VALIDATION_FAILED');
        
        // not string or Date
        assert.throws(function () {scheduler._toTimestamp({});}, 'ERR_VALIDATION_FAILED');

        return done();
    });
    
    it('_expandRepetition() day unit', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-02-12T19:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'day',
            repeat: 1,
            repeatOn: '15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-09 15:34:00').unix(),
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-11 15:34:00').unix(),
            moment.utc('2017-02-12 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        // with exceptions
        var repetitionDef = {
            unit: 'day',
            repeat: 1,
            repeatOn: '15:34',
            exceptDays: [moment.utc('2017-02-10').unix()]
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-09 15:34:00').unix(),
            moment.utc('2017-02-11 15:34:00').unix(),
            moment.utc('2017-02-12 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        return done();
    });

    it('_expandRepetition() day unit, with repeatCount', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'day',
            repeat: 1,
            repeatOn: '15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 6);
        var repetitionsExpected = [
            moment.utc('2017-02-09 15:34:00').unix(),
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-11 15:34:00').unix(),
            moment.utc('2017-02-12 15:34:00').unix(),
            moment.utc('2017-02-13 15:34:00').unix(),
            moment.utc('2017-02-14 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        // with exceptions
        var repetitionDef = {
            unit: 'day',
            repeat: 1,
            repeatOn: '15:34',
            exceptDays: [moment.utc('2017-02-10').unix()]
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 5);
        var repetitionsExpected = [
            moment.utc('2017-02-09 15:34:00').unix(),
            moment.utc('2017-02-11 15:34:00').unix(),
            moment.utc('2017-02-12 15:34:00').unix(),
            moment.utc('2017-02-13 15:34:00').unix(),
            moment.utc('2017-02-14 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        return done();
    });
    
    
    it('_expandRepetition() day unit, repeatOn multiple', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-02-12T19:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'day',
            repeat: 1,
            repeatOn: '15:34, 16:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-09 15:34:00').unix(),
            moment.utc('2017-02-09 16:34:00').unix(),
            
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-10 16:34:00').unix(),
            
            moment.utc('2017-02-11 15:34:00').unix(),
            moment.utc('2017-02-11 16:34:00').unix(),
            
            moment.utc('2017-02-12 15:34:00').unix(),
            moment.utc('2017-02-12 16:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        return done();
    });

    it('_expandRepetition() day unit, repeatOn multiple, with repeatCount', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'day',
            repeat: 1,
            repeatOn: '15:34, 16:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 4);
        var repetitionsExpected = [
            moment.utc('2017-02-09 15:34:00').unix(),
            moment.utc('2017-02-09 16:34:00').unix(),
            
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-10 16:34:00').unix(),
            
            moment.utc('2017-02-11 15:34:00').unix(),
            moment.utc('2017-02-11 16:34:00').unix(),
            
            moment.utc('2017-02-12 15:34:00').unix(),
            moment.utc('2017-02-12 16:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        return done();
    });
    
    it('_expandRepetition() week unit', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-03-12').getTime())/1000);
        
        var repetitionDef = {
            unit: 'week',
            repeat: 1,
            repeatOn: 'Friday 15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-17 15:34:00').unix(),
            moment.utc('2017-02-24 15:34:00').unix(),
            moment.utc('2017-03-03 15:34:00').unix(),
            moment.utc('2017-03-10 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);

        // with exceptions
        var repetitionDef = {
            unit: 'week',
            repeat: 1,
            repeatOn: 'Friday 15:34',
            exceptDays: [moment.utc('2017-02-24').unix()]
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-17 15:34:00').unix(),
            moment.utc('2017-03-03 15:34:00').unix(),
            moment.utc('2017-03-10 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        return done();
    });
    
    it('_expandRepetition() week unit, repeatOn multiple', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09 00:00:00').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-03-12 23:59:59').getTime())/1000);
        
        var repetitionDef = {
            unit: 'week',
            repeat: 1,
            repeatOn: 'Friday 15:34, Sunday 10:00',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        
        var repetitionsExpected = [
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-12 10:00:00').unix(),
            
            moment.utc('2017-02-17 15:34:00').unix(),
            moment.utc('2017-02-19 10:00:00').unix(),
            
            moment.utc('2017-02-24 15:34:00').unix(),
            moment.utc('2017-02-26 10:00:00').unix(),
            
            moment.utc('2017-03-03 15:34:00').unix(),
            moment.utc('2017-03-05 10:00:00').unix(),
            
            moment.utc('2017-03-10 15:34:00').unix(),
            moment.utc('2017-03-12 10:00:00').unix()
            
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        return done();
    });

    it('_expandRepetition() week unit, repeatOn multiple, with repeatCount', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-06 00:00:00').getTime())/1000);
        
        var repetitionDef = {
            unit: 'week',
            repeat: 1,
            repeatOn: 'Monday 10:00, Friday 15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 3); // for three weeks
        
        var repetitionsExpected = [
            // first week
            moment.utc('2017-02-06 10:00:00').unix(),
            moment.utc('2017-02-10 15:34:00').unix(),
            
            // second week
            moment.utc('2017-02-13 10:00:00').unix(),
            moment.utc('2017-02-17 15:34:00').unix(),
            
            //3th week
            moment.utc('2017-02-20 10:00:00').unix(),
            moment.utc('2017-02-24 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        return done();
    });
    

    it('_expandRepetition() week unit, repeatOn multiple, with repeatCount, with except', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-06 00:00:00').getTime())/1000);
        
        var repetitionDef = {
            unit: 'week',
            repeat: 1,
            repeatOn: 'Monday 10:00, Friday 15:34',
            exceptDays: [moment.utc('2017-02-17').unix()]
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 3); // for three weeks
        
        var repetitionsExpected = [
            // first week
            moment.utc('2017-02-06 10:00:00').unix(),
            moment.utc('2017-02-10 15:34:00').unix(),
            
            // second week, 2017-02-17 excluded
            moment.utc('2017-02-13 10:00:00').unix(),
            
            //3th week
            moment.utc('2017-02-20 10:00:00').unix(),
            moment.utc('2017-02-24 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        return done();
    });
    
    
    it('_expandRepetition() month unit', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-08-12T19:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'month',
            repeat: 1,
            repeatOn: '8 15:34', // day 8 each month at 15:34. 8 of february will not be included becouse is before startDate
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-03-08 15:34:00').unix(),
            moment.utc('2017-04-08 15:34:00').unix(),
            moment.utc('2017-05-08 15:34:00').unix(),
            moment.utc('2017-06-08 15:34:00').unix(),
            moment.utc('2017-07-08 15:34:00').unix(),
            moment.utc('2017-08-08 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        var repetitionDef = {
            unit: 'month',
            repeat: 1,
            repeatOn: '08 15:34', // day 8 each month at 15:34. 8 of february will not be included becouse is before startDate
            exceptDays: [moment.utc('2017-05-08').unix()]
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-03-08 15:34:00').unix(),
            moment.utc('2017-04-08 15:34:00').unix(),
            moment.utc('2017-06-08 15:34:00').unix(),
            moment.utc('2017-07-08 15:34:00').unix(),
            moment.utc('2017-08-08 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        return done();
    });
    

    it('_expandRepetition() month unit, repeatOn multiple', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-01T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-04-30T19:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'month',
            repeat: 1,
            repeatOn: '8 15:34, 20 10:10', 
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-08 15:34:00').unix(),
            moment.utc('2017-02-20 10:10:00').unix(),
            
            moment.utc('2017-03-08 15:34:00').unix(),
            moment.utc('2017-03-20 10:10:00').unix(),
            
            moment.utc('2017-04-08 15:34:00').unix(),
            moment.utc('2017-04-20 10:10:00').unix()
            
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        return done();
    });
    
    it('_expandRepetition() month unit, repeatOn multiple, with repeatCount', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-01T14:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'month',
            repeat: 1,
            repeatOn: '8 15:34, 20 10:10', 
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 4);
        var repetitionsExpected = [
            moment.utc('2017-02-08 15:34:00').unix(),
            moment.utc('2017-02-20 10:10:00').unix(),
            
            moment.utc('2017-03-08 15:34:00').unix(),
            moment.utc('2017-03-20 10:10:00').unix(),
            
            moment.utc('2017-04-08 15:34:00').unix(),
            moment.utc('2017-04-20 10:10:00').unix(),
            
            moment.utc('2017-05-08 15:34:00').unix(),
            moment.utc('2017-05-20 10:10:00').unix()
            
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        return done();
    });
    it('_expandRepetition() use last day of month', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-01-01T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-05-12T19:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'month',
            repeat: 1,
            repeatOn: '31 15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-01-31 15:34:00').unix(),
            moment.utc('2017-02-28 15:34:00').unix(),
            moment.utc('2017-03-31 15:34:00').unix(),
            moment.utc('2017-04-30 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        return done();
    });

    
    it('_expandRepetition() repeat > 1', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-03-12').getTime())/1000);
        
        var repetitionDef = {
            unit: 'week',
            repeat: 2,
            repeatOn: 'Friday 15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-24 15:34:00').unix(),
            moment.utc('2017-03-10 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-02-12T19:23:56').getTime())/1000);
        
        var repetitionDef = {
            unit: 'day',
            repeat: 2,
            repeatOn: '15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-09 15:34:00').unix(),
            moment.utc('2017-02-11 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        var tsStart = parseInt((new Date('2017-02-02').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-08-12').getTime())/1000);
        
        var repetitionDef = {
            unit: 'month',
            repeat: 3,
            repeatOn: '8 15:34', // day 8 each month at 15:34
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, tsEnd);
        var repetitionsExpected = [
            moment.utc('2017-02-08 15:34:00').unix(),
            moment.utc('2017-05-08 15:34:00').unix(),
            moment.utc('2017-08-08 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        return done();
    });

    it('_expandRepetition() repeat > 1, with repeatCount', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var tsStart = parseInt((new Date('2017-02-09').getTime())/1000);
        
        var repetitionDef = {
            unit: 'week',
            repeat: 2,
            repeatOn: 'Friday 15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 4);
        var repetitionsExpected = [
            moment.utc('2017-02-10 15:34:00').unix(),
            moment.utc('2017-02-24 15:34:00').unix(),
            moment.utc('2017-03-10 15:34:00').unix(),
            moment.utc('2017-03-24 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        var tsStart = parseInt((new Date('2017-06-05T00:00:00').getTime())/1000);
        
        var repetitionDef = {
            unit: 'day',
            repeat: 2,
            repeatOn: '15:34',
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 6);
        var repetitionsExpected = [
            moment.utc('2017-06-05 15:34:00').unix(),
            moment.utc('2017-06-07 15:34:00').unix(),
            moment.utc('2017-06-09 15:34:00').unix(),
            moment.utc('2017-06-11 15:34:00').unix(),
            moment.utc('2017-06-13 15:34:00').unix(),
            moment.utc('2017-06-15 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        
        var tsStart = parseInt((new Date('2017-06-01').getTime())/1000);
        
        var repetitionDef = {
            unit: 'month',
            repeat: 3,
            repeatOn: '8 15:34', // day 8 each month at 15:34
            exceptDays: []
        };
        var repetitions = scheduler._expandRepetition(repetitionDef, tsStart, undefined, 3);
        var repetitionsExpected = [
            moment.utc('2017-06-08 15:34:00').unix(),
            moment.utc('2017-09-08 15:34:00').unix(),
            moment.utc('2017-12-08 15:34:00').unix()
        ];
        
        assert.deepEqual(repetitions, repetitionsExpected);
        
        return done();
    });


    it('_exceptDaysToTimestamp()', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var exceptDays = ['2017-02-12', 1487635200];
        var exceptDaysExpected = [1486857600, 1487635200];
        
        assert.deepEqual(scheduler._exceptDaysToTimestamp(exceptDays), exceptDaysExpected);
        
        return done();
    });
    
    
    it('_expandRepetition() throws errors', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        
        
        // empty start date and end date
        assert.throws(function () {scheduler._expandRepetition({}, null, null);}, 'ERR_VALIDATION_FAILED');
        
        // start date > end date
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-01-12T19:23:56').getTime())/1000);
        assert.throws(function () {scheduler._expandRepetition({}, tsStart, tsEnd);}, 'ERR_VALIDATION_FAILED');
        
        // invalid repeat definition
        var tsStart = parseInt((new Date('2017-02-09T14:23:56').getTime())/1000);
        var tsEnd = parseInt((new Date('2017-05-12T19:23:56').getTime())/1000);
        assert.throws(function () {scheduler._expandRepetition({}, tsStart, tsEnd);}, 'ERR_VALIDATION_FAILED');
        
        
        return done();
    });
    
    
});


describe('_scheduleLiveTournament() tests', function () {
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
            }
            
        ], done);
    });
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    
    it('forever repetition', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var id = 12;
        var liveTournament;
        var schedulerItemId = null;
        
        var _expandRepetitionSpy = sinonSandbox.spy(scheduler, '_expandRepetition');
        var _buildRepetitionEventsSpy = sinonSandbox.spy(scheduler, '_buildRepetitionEvents');
        
        async.series([
            function (next) {
                scheduler.init(next);
            },
            
            // forever repetition, repeatCount, endDate not set
            function (next) {
                liveTournament = {
                    id: id,
                    repetition: {
                        startDate: '2017-02-23T00:00:00',
                        unit: 'day',
                        repeat: 1,
                        repeatOn: "14:00",
                        exceptDays: [],
                        offsetOpenRegistration: {
                            unit: 'hour',
                            offset: 2
                        },
                        
                        offsetStartGame: {
                            unit: 'minute',
                            offset: 10
                        }
                    },
                    type: 'live'
                };
                
                scheduler.scheduleLiveTournament(liveTournament, function (err) {
                    try {
                        assert.ifError(err);
                        
                        assert.isUndefined(_expandRepetitionSpy.lastCall.args[2]);
                        assert.strictEqual(_expandRepetitionSpy.lastCall.args[3], Config.defaultRepeatCount);
                        
                        assert.strictEqual(_buildRepetitionEventsSpy.returnValues[0].length, Config.defaultRepeatCount);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
                
            },
            
            // check scheduler item
            function (next) {
                SchedulerItem.findOne({where: {itemId: 'liveTournament-' + liveTournament.id}}).then(function (dbItem) {
                    try {

                        dbItem = dbItem.get({plain: true});
                        schedulerItemId = dbItem.id;
                        var definition = JSON.parse(dbItem.definition);

                        
                        var startDateTs = moment.utc(liveTournament.repetition.startDate).unix();

                        assert.strictEqual(definition.repetition.startDate, startDateTs);
                        
                        
                        assert.isUndefined(definition.repetition.endDate);
                        assert.isUndefined(definition.repetition.repeatCount);
                        
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }

                }).catch(function (err) {
                    return next(err);
                });

            }
            
        ], done);
    });
    
    it('specific repeatCount', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var id = 12;
        var schedulerItemId = null;
        
        var _expandRepetitionSpy = sinonSandbox.spy(scheduler, '_expandRepetition');
        var _buildRepetitionEventsSpy = sinonSandbox.spy(scheduler, '_buildRepetitionEvents');
        
        async.series([
            function (next) {
                scheduler.init(next);
            },
            
            // forever repetition, repeatCount, endDate not set
            function (next) {
                var repeatCount = 5;
                var liveTournament = {
                    id: id,
                    repetition: {
                        startDate: '2017-02-23T00:00:00',
                        unit: 'day',
                        repeat: 1,
                        repeatOn: "14:00",
                        repeatCount: repeatCount,
                        exceptDays: [],
                        offsetOpenRegistration: {
                            unit: 'hour',
                            offset: 2
                        },
                        
                        offsetStartGame: {
                            unit: 'minute',
                            offset: 10
                        }
                    },
                    type: 'normal'
                };
                
                scheduler.scheduleLiveTournament(liveTournament, function (err) {
                    try {
                        assert.ifError(err);
                        
                        assert.isUndefined(_expandRepetitionSpy.lastCall.args[2]);
                        assert.strictEqual(_expandRepetitionSpy.lastCall.args[3], repeatCount);
                        
                        assert.strictEqual(_buildRepetitionEventsSpy.returnValues[0].length, repeatCount);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
                
            }
        ], done);
    });
    
    it('specific endDate', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var id = 12;
        var schedulerItemId = null;
        
        var _expandRepetitionSpy = sinonSandbox.spy(scheduler, '_expandRepetition');
        var _buildRepetitionEventsSpy = sinonSandbox.spy(scheduler, '_buildRepetitionEvents');
        
        async.series([
            function (next) {
                scheduler.init(next);
            },
            
            // forever repetition, repeatCount, endDate not set
            function (next) {
                var liveTournament = {
                    id: id,
                    repetition: {
                        startDate: '2017-02-23T00:00:00',
                        unit: 'day',
                        repeat: 1,
                        repeatOn: "14:00",
                        endDate: '2017-02-25T23:59:59',
                        exceptDays: [],
                        offsetOpenRegistration: {
                            unit: 'hour',
                            offset: 2
                        },
                        
                        offsetStartGame: {
                            unit: 'minute',
                            offset: 10
                        }
                    },
                    type: 'normal'
                };
                
                scheduler.scheduleLiveTournament(liveTournament, function (err) {
                    try {
                        assert.ifError(err);
                        
                        assert.strictEqual(_expandRepetitionSpy.lastCall.args[2], parseInt(Date.parse(liveTournament.repetition.endDate)/1000));
                        assert.isUndefined(_expandRepetitionSpy.lastCall.args[3]);
                        
                        assert.strictEqual(_buildRepetitionEventsSpy.returnValues[0].length, 3);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
                
            }
        ], done);
    });
});

describe('Schedule Items', function () {
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
            }
            
        ], done);
    });
    
    
    it('scheduleTombola()', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var id = 12;
        var schedulerItemId = null;
        async.series([
            function (next) {
                scheduler.init(next);
            },
            
            
            // create
            function (next) {
                var y = (new Date()).getFullYear()+1;
                var tombola = {
                    id: id,
                    startDate: y + '-02-23 14:14:00',
                    endDate: y + '-02-27 14:14:00',
                    targetDate: y + '-03-05 09:14:00',
                    preCloseOffsetMinutes: 10
                };
                
                async.series([
                    // create
                    function (next2) {
                        scheduler.scheduleTombola(tombola, function (err) {
                            try {
                                assert.ifError(err);
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                        });
                    },
                    
                    // check scheduler item
                    function (next2) {
                        SchedulerItem.findOne({where: {itemId: 'tombola-' + tombola.id}}).then(function (dbItem) {
                            try {
                                dbItem = dbItem.get({plain: true});
                                schedulerItemId = dbItem.id;
                                var definition = JSON.parse(dbItem.definition);
                                
                                var startDateTs = moment.utc(tombola.startDate).unix();
                                var endDateTs = moment.utc(tombola.endDate).unix();
                                var targetDateTs = moment.utc(tombola.targetDate).unix();
                                
                                assert.strictEqual(definition.startDate, startDateTs);
                                assert.strictEqual(definition.endDate, endDateTs);
                                assert.strictEqual(definition.targetDate, targetDateTs);
                                assert.strictEqual(definition.preCloseOffsetMinutes, tombola.preCloseOffsetMinutes);
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    },
                    
                    // check scheduler events
                    function (next2) {
                        SchedulerEvent.findAll({where: {schedulerItemId: schedulerItemId}}).then(function (dbItems) {
                            try {
                                var items = [];
                                for(var i=0; i<dbItems.length; i++) {
                                    items.push(dbItems[i].get({plain: true}));
                                }
                                
                                assert.strictEqual(items.length, 4);
                                
                                var startDateTs = moment.utc(tombola.startDate).unix();
                                var endDateTs = moment.utc(tombola.endDate).unix();
                                var targetDateTs = moment.utc(tombola.targetDate).unix();
                                
                                
                                var item = _.find(items, ['scheduledAt', startDateTs]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.tombola.openCheckout + tombola.id);
                                
                                var item = _.find(items, ['scheduledAt', endDateTs]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.tombola.closeCheckout + tombola.id);
                                
                                var item = _.find(items, ['scheduledAt', targetDateTs]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.tombola.draw + tombola.id);
                                
                                var item = _.find(items, ['scheduledAt', endDateTs-(tombola.preCloseOffsetMinutes*60)]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.tombola.preCloseCheckout + tombola.id);
                                assert.strictEqual(JSON.parse(item.event).data.minutesToCheckout, tombola.preCloseOffsetMinutes);
                                
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    }
                ], next);
                
            },
            
            // update
            function (next) {
                var y = (new Date()).getFullYear()+2;
                var tombola = {
                    id: id,
                    startDate: y + '-03-23 14:14:00',
                    endDate: y + '-03-27 14:14:00',
                    targetDate: y + '-04-05 09:14:00',
                    preCloseOffsetMinutes: 10
                };
                
                async.series([
                    // update
                    function (next2) {
                        scheduler.scheduleTombola(tombola, function (err) {
                            try {
                                assert.ifError(err);
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                        });
                    },
                    
                    // check scheduler item
                    function (next2) {
                        SchedulerItem.findOne({where: {itemId: 'tombola-' + tombola.id}}).then(function (dbItem) {
                            try {
                                dbItem = dbItem.get({plain: true});
                                schedulerItemId = dbItem.id;
                                var definition = JSON.parse(dbItem.definition);
                                
                                var startDateTs = moment.utc(tombola.startDate).unix();
                                var endDateTs = moment.utc(tombola.endDate).unix();
                                var targetDateTs = moment.utc(tombola.targetDate).unix();
                                
                                assert.strictEqual(definition.startDate, startDateTs);
                                assert.strictEqual(definition.endDate, endDateTs);
                                assert.strictEqual(definition.targetDate, targetDateTs);
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    },
                    
                    // check scheduler events
                    function (next2) {
                        SchedulerEvent.findAll({where: {schedulerItemId: schedulerItemId}}).then(function (dbItems) {
                            try {
                                var items = [];
                                for(var i=0; i<dbItems.length; i++) {
                                    items.push(dbItems[i].get({plain: true}));
                                }
                                
                                assert.strictEqual(items.length, 4);
                                
                                var startDateTs = moment.utc(tombola.startDate).unix();
                                var endDateTs = moment.utc(tombola.endDate).unix();
                                var targetDateTs = moment.utc(tombola.targetDate).unix();
                                
                                
                                var item = _.find(items, ['scheduledAt', startDateTs]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.tombola.openCheckout + tombola.id);
                                
                                var item = _.find(items, ['scheduledAt', endDateTs]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.tombola.closeCheckout + tombola.id);
                                
                                var item = _.find(items, ['scheduledAt', targetDateTs]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.tombola.draw + tombola.id);
                                
                                
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    }
                ], next);
                
            },
            // only one item
            function (next) {
                var options = {
                    where: {itemId: 'tombola-' + id}
                };

                SchedulerItem.count(options).then(function (count) {
                    try {
                        assert.strictEqual(count, 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            function (next) {
                scheduler._stopAllTimers();
                setTimeout(next, 500);
            }
            
            
        ], done);
    });
    
    it('scheduleLiveTournament() , no repetition', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var id = 12;
        var schedulerItemId = null;
        async.series([
            function (next) {
                scheduler.init(next);
            },
            
            // create, no repetition
            function (next) {
                var liveTournament = {
                    id: id,
                    startDate: '2017-02-23 14:14:00',
                    endDate: '2017-02-27 14:14:00',
                    type: 'live'
                };
                
                async.series([
                    // create
                    function (next2) {
                        scheduler.scheduleLiveTournament(liveTournament, function (err) {
                            try {
                                assert.ifError(err);
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                        });
                    },
                    
                    // check scheduler item
                    function (next2) {
                        SchedulerItem.findOne({where: {itemId: 'liveTournament-' + liveTournament.id}}).then(function (dbItem) {
                            try {
                                dbItem = dbItem.get({plain: true});
                                schedulerItemId = dbItem.id;
                                var definition = JSON.parse(dbItem.definition);
                                
                                var startDateTs = moment.utc(liveTournament.startDate).unix();
                                var endDateTs = moment.utc(liveTournament.endDate).unix();
                                
                                assert.strictEqual(definition.startDate, startDateTs);
                                assert.strictEqual(definition.endDate, endDateTs);
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    },
                    
                    // check scheduler events
                    function (next2) {
                        SchedulerEvent.findAll({where: {schedulerItemId: schedulerItemId}}).then(function (dbItems) {
                            try {
                                var items = [];
                                for(var i=0; i<dbItems.length; i++) {
                                    items.push(dbItems[i].get({plain: true}));
                                }
                                
                                assert.strictEqual(items.length, 1);
                                
                                var startDateTs = moment.utc(liveTournament.startDate).unix();
                                var endDateTs = moment.utc(liveTournament.endDate).unix();
                                
                                
                                var item = _.find(items, ['scheduledAt', startDateTs]);
                                assert.strictEqual(JSON.parse(item.event).name, SchedulerService.eventPrefix.liveTournament.openRegistration + liveTournament.id);
                                
                                
                                
                                
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    }
                ], next);
                
            },
            
            function (next) {
                scheduler._stopAllTimers();
                setTimeout(next, 500);
            }
            
            
        ], done);
    });
    
    
    it('scheduleLiveTournament() , with repetition', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        var id = 12;
        var schedulerItemId = null;
        async.series([
            function (next) {
                scheduler.init(next);
            },
            
            // create, with repetition
            function (next) {
                var liveTournament = {
                    id: id,
                    startDate: '2016-02-23 14:14:00',
                    endDate: '2016-02-25 14:14:00',
                    repetition: {
                        startDate: '2017-02-23 00:00:00',
                        endDate: '2017-02-25 23:59:00',
                        unit: 'day',
                        repeat: 1,
                        repeatOn: "14:00",
                        exceptDays: ['2017-03-20', 1491955200],
                        offsetOpenRegistration: {
                            unit: 'hour',
                            offset: 2
                        },
                        
                        offsetStartGame: {
                            unit: 'minute',
                            offset: 10
                        }
                    },
                    type: 'live'
                };
                
                async.series([
                    // create
                    function (next2) {
                        scheduler.scheduleLiveTournament(liveTournament, function (err) {
                            try {
                                assert.ifError(err);
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                        });
                    },
                    
                    // check scheduler item
                    function (next2) {
                        SchedulerItem.findOne({where: {itemId: 'liveTournament-' + liveTournament.id}}).then(function (dbItem) {
                            try {
                                
                                dbItem = dbItem.get({plain: true});
                                schedulerItemId = dbItem.id;
                                var definition = JSON.parse(dbItem.definition);
                                
                                var startDateTs = moment.utc(liveTournament.startDate).unix();
                                var endDateTs = moment.utc(liveTournament.endDate).unix();
                                
                                assert.strictEqual(definition.startDate, startDateTs);
                                assert.strictEqual(definition.endDate, endDateTs);
                                
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    },
                    
                    // check scheduler events
                    function (next2) {
                        SchedulerEvent.findAll({where: {schedulerItemId: schedulerItemId}, order: 'scheduledAt ASC'}).then(function (dbItems) {
                            try {
                                
                                var items = [];
                                for(var i=0; i<dbItems.length; i++) {
                                    var dbItem = dbItems[i].get({plain: true});
                                    items.push({
                                        scheduledAt: dbItem.scheduledAt,
                                        eventName: JSON.parse(dbItem.event).name
                                    });
                                }
                                var itemsExpected = [
                                    {
                                        scheduledAt: moment.utc('2017-02-23 12:00:00').unix(),
                                        eventName: SchedulerService.eventPrefix.liveTournament.openRegistration + liveTournament.id
                                    },
                                    
                                    {
                                        scheduledAt: moment.utc('2017-02-24 12:00:00').unix(),
                                        eventName: SchedulerService.eventPrefix.liveTournament.openRegistration + liveTournament.id
                                    },
                                    
                                    {
                                        scheduledAt: moment.utc('2017-02-25 12:00:00').unix(),
                                        eventName: SchedulerService.eventPrefix.liveTournament.openRegistration + liveTournament.id
                                    }
                                    
                                ];                                
                                assert.strictEqual(items.length, 3);
                                
                                assert.deepEqual(items, itemsExpected);
                                
                                // check startGameDateTime
                                assert.strictEqual(JSON.parse(dbItems[0].get({plain: true}).event).data.startGameDateTime, '2017-02-23T13:50:00Z');
                                
                                return next2();
                            } catch (e) {
                                return next2(e);
                            }
                            
                        }).catch(function (err) {
                            return next2(err);
                        });
                        
                    }
                ], next);
                
            },
            
            function (next) {
                scheduler._stopAllTimers();
                setTimeout(next, 500);
            }
            
            
        ], done);
    });
    
    it('init()', function (done) {
        var scheduler = new SchedulerService({instanceId: 'localhost:1234'});
        
        async.series([
            
            function (next) {
                async.series([
                    function (next2) {
                        SchedulerEvent.destroy({where: {}}).then(function () {
                            next2();
                        }).catch(function (err) {
                            next2(err);
                        });
                    },
                    function (next2) {
                        SchedulerItem.destroy({where: {}}).then(function () {
                            next2();
                        }).catch(function (err) {
                            next2(err);
                        });
                    }

                ], next);
                
            },
            
            
            function (next) {
                SchedulerEmitFrom.destroy({where: {}}).then(function () {
                    next();
                }).catch(function (err) {
                    next(err);
                });
            },
            
            function (next) {
                scheduler.init(next);
                
            },
            
            function (next) {
                scheduler._stopAllTimers();
                SchedulerEmitFrom.count().then(function (count) {
                    try {
                        assert.strictEqual(count, 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            //init again
            function (next) {
                scheduler.init(next);
                
            },
            
            function (next) {
                scheduler._stopAllTimers();
                SchedulerEmitFrom.count().then(function (count) {
                    try {
                        assert.strictEqual(count, 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
            }
            
        ], done);
    });
});