var _ = require('lodash');
var async = require('async');
var assert = require('chai').assert;
var sinon = require('sinon');
var NewrelicMetrics = require('../../../index.js');
var ProtocolMessage = require('nodejs-protocol');

var sinonSandbox = null;
describe('UNIT TESTS', function() {
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    it('test _metricNameByMessage', function () {
        var metrics = new NewrelicMetrics();
        var msg1 = new ProtocolMessage({
            message: 'question/questionCreate',
            seq: 1
        });
        
        var msg2 = new ProtocolMessage({
            message: 'question/questionUpdateResponse',
            seq: 1
        });
        
        assert.strictEqual(metrics._metricNameByMessage(msg1), 'Custom/ApiHandler/question.questionCreate');
        assert.strictEqual(metrics._metricNameByMessage(msg2), 'Custom/ApiHandler/question.questionUpdate');
    });
    
    it('test start/end record', function (done) {
        // fake newrelic
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        
        var metrics = new NewrelicMetrics();
        
        var recordId1 = 'record_id_1';
        var recordId2 = 'record_id_2';
        var metricName = 'ApiCalls';
        //var metricName2 = 'apicalls2';
        
        
        async.series([
            function (next) {
                try {
                    metrics.startRecord(recordId1, metricName);
                    assert.strictEqual(_.keys(metrics._timeStart).length, 1);
                    assert.strictEqual(_.keys(metrics._timeStart)[0], recordId1);
                    setTimeout(next, 500);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                try {
                    metrics.startRecord(recordId2, metricName);
                    assert.strictEqual(_.keys(metrics._timeStart).length, 2);
                    assert.strictEqual(_.keys(metrics._timeStart)[1], recordId2);
                    setTimeout(next, 500);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                try {
                    metrics.endRecord(recordId1, metricName);
                    assert.strictEqual(_.keys(metrics._timeStart).length, 1);
                    assert.strictEqual(_.keys(metrics._timeStart)[0], recordId2);
                    assert.strictEqual(metrics._metricsValuesBuffer[metricName].length, 1);
                    
                    var timeTook = metrics._metricsValuesBuffer[metricName][0];
                    assert.isAbove(timeTook, 500);
                    assert.isBelow(timeTook, 1200);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                try {
                    metrics.endRecord(recordId2, metricName);
                    assert.strictEqual(_.keys(metrics._timeStart).length, 0);
                    assert.strictEqual(metrics._metricsValuesBuffer[metricName].length, 2);
                    
                    var timeTook = metrics._metricsValuesBuffer[metricName][1];
                    assert.isAbove(timeTook, 0);
                    assert.isBelow(timeTook, 520);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // send metrics
            function (next) {
                try {
                    sinonSandbox = sinon.sandbox.create();
                    var recordMetricSpy = sinonSandbox.spy(fake_serviceNewrelic, "recordMetric");
                    metrics._sendMetrics();
                    
                    assert.strictEqual(recordMetricSpy.callCount, 1);
                    assert.strictEqual(recordMetricSpy.lastCall.args[0], metricName);
                    assert.sameDeepMembers(_.keys(recordMetricSpy.lastCall.args[1]), ['count', 'total', 'min', 'max', 'sumOfSquares']);
                    assert.strictEqual(_.keys(metrics._metricsValuesBuffer).length, 0);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
            
        ], done);
    });
    
    it('test apiHandlerStart/apiHandlerEnd', function (done) {
        // fake newrelic
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        
        var metrics = new NewrelicMetrics();
        var msgSend = new ProtocolMessage({
            message: 'question/questionCreate',
            seq: 1,
            clientId: '111sdas'
        });
        
        var msgResponse = new ProtocolMessage({
            message: 'question/questionCreateResponse',
            seq: null,
            ack: [1],
            clientId: '111sdas'
        });
        
        metrics.apiHandlerStart(msgSend);
        
        setTimeout(function () {
            try {
                metrics.apiHandlerEnd(msgResponse);
                assert.strictEqual(_.keys(metrics._timeStart).length, 0);
                assert.strictEqual(_.keys(metrics._metricsValuesBuffer).length, 1);
                return done();
            } catch (e) {
                return done(e);
            }
        }, 500);
        
    });
    
    it('test apiHandlerStart/apiHandlerEnd error, seq/ack not match', function (done) {
        // fake newrelic
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        
        var metrics = new NewrelicMetrics();
        var msgSend = new ProtocolMessage({
            message: 'question/questionCreate',
            seq: 1
        });
        
        var msgResponse = new ProtocolMessage({
            message: 'question/questionCreateResponse',
            seq: null,
            ack: [2]
        });
        
        metrics.apiHandlerStart(msgSend);
        
        setTimeout(function () {
            try {
                assert.strictEqual(_.keys(metrics._timeStart).length, 1);
                metrics.apiHandlerEnd(msgResponse);
                var key = _.keys(metrics._metricsValuesBuffer)[0];
                assert.strictEqual(metrics._metricsValuesBuffer[key].length, 0);
                return done();
            } catch (e) {
                return done(e);
            }
        }, 500);
        
    });
    
    it('test periodic send metrics', function (done) {
        // fake newrelic
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        
        var metrics = new NewrelicMetrics({
            sendMetricsInterval: 100
        });
        
        sinonSandbox = sinon.sandbox.create();
        var sendMetricsSpy = sinonSandbox.spy(metrics, "_sendMetrics");
        
        setTimeout(function () {
            try {
                assert.strictEqual(sendMetricsSpy.callCount, 3);
                return done();
            } catch (e) {
                return done(e);
            }
        }, 420);
    });
    
    it('test periodic cleanup', function (done) {
        // fake newrelic
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        
        var metrics = new NewrelicMetrics({
            cleanupInterval: 100,
            cleanupOlderThan: 100
        });
        
        sinonSandbox = sinon.sandbox.create();
        var cleanupSpy = sinonSandbox.spy(metrics, "_cleanup");
        
        // start and not end
        metrics.startRecord('id1', 'metric1');
        assert.strictEqual(_.keys(metrics._timeStart).length, 1);
        
        setTimeout(function () {
            try {
                assert.strictEqual(cleanupSpy.callCount, 3);
                assert.strictEqual(_.keys(metrics._timeStart).length, 0);
                return done();
            } catch (e) {
                return done(e);
            }
        }, 420);
    });

    it('test cleanup only old metrics', function (done) {
        // strange behaviour of spy.callCount . it doesn't count first call of  _cleanup !!!!
        // fake newrelic
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        
        var metrics = new NewrelicMetrics({
            cleanupInterval: 100,
            cleanupOlderThan: 300
        });
        sinonSandbox = sinon.sandbox.create();
        var cleanupSpy = sinonSandbox.spy(metrics, "_cleanup");
        
        // start and not end
        metrics.startRecord('id1', 'metric1');
        assert.strictEqual(_.keys(metrics._timeStart).length, 1);
        
        
        // after 210 ms id is not deleted
        setTimeout(function () {
            try {
                assert.isAbove(cleanupSpy.callCount, 0);
                assert.strictEqual(_.keys(metrics._timeStart).length, 1);
            } catch (e) {
                return done(e);
            }
        }, 210);
        
        // after 350 ms id should be deleted
        setTimeout(function () {
            try {
                assert.strictEqual(_.keys(metrics._timeStart).length, 0);
                return done();
            } catch (e) {
                return done(e);
            }
        }, 350);
        
    });

    
});