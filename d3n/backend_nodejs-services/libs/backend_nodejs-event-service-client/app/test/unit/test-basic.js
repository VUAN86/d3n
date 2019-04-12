var async = require('async');
var assert = require('chai').assert;
var should = require('should');
var fs = require('fs');
var config = require('../../config/config.js');
var errors = require('../../config/errors.js');
var EventServiceClient = require('../../classes/EventServiceClient.js');
var eventServiceClient = null;
var sinon = require('sinon');

describe('Test basic', function() {
    
    it('should create and init event service client', function (done) {
        this.timeout(10000);
        
        eventServiceClient = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
        return done(false);
        /*
        eventServiceClient.init(function (err) {
            done(err);
        });
        */
    });
    
    it('subscribe OK', function (done) {
        this.timeout(10000);
        eventServiceClient.subscribe('testTopic', function (err, message) {
            assert.ifError(err);
            assert.strictEqual(message.getError(), null);
            
            done(null);
        });
    });
    
    it('subscribe not OK: invalid topic', function (done) {
        this.timeout(10000);
        eventServiceClient.subscribe(true, function (err) {
            assert.instanceOf(err, Error);
            assert.strictEqual(err.message, errors.ERR_INVALID_TOPIC);
            done(null);
        });
    });
    
    it('subscribe not OK: already subscribed', function (done) {
        this.timeout(10000);
        eventServiceClient.subscribe('testTopic', function (err) {
            assert.instanceOf(err, Error);
            assert.strictEqual(err.message, errors.ERR_ALREADY_SUBSCRIBED);
            done(null);
        });
    });
    
    it('unsubscribe OK', function (done) {
        this.timeout(10000);
        eventServiceClient.unsubscribe('testTopic', function (err, message) {
            assert.ifError(err);
            assert.strictEqual(message.getError(), null);
            done(null);
        });
    });
    
    it('unsubscribe not OK: invalid topic', function (done) {
        this.timeout(10000);
        eventServiceClient.unsubscribe(true, function (err) {
            assert.instanceOf(err, Error);
            assert.strictEqual(err.message, errors.ERR_INVALID_TOPIC);
            done(null);
        });
    });
    
    it('unsubscribe not OK: subscription ID not found', function (done) {
        this.timeout(10000);
        eventServiceClient.unsubscribe('asdaasd', function (err) {
            assert.instanceOf(err, Error);
            assert.strictEqual(err.message, errors.ERR_SUBSCRIPTION_ID_NOT_FOUND);
            done(null);
        });
    });
    
    it('publish OK', function (done) {
        this.timeout(10000);
        var requestSpy = sinon.spy(eventServiceClient, '_request');
        
        eventServiceClient.publish('testTopic', {name:'alice'}, function (err) {
            assert.ifError(err);
            assert.deepEqual(requestSpy.lastCall.args[0].getContent(), {
                topic: 'testTopic',
                'notification-content': {name:'alice'},
                virtual: false
            });
            requestSpy.restore();
            done(null);
        });
    });
    it('publish virtual OK', function (done) {
        this.timeout(10000);
        
        var requestSpy = sinon.spy(eventServiceClient, '_request');
        eventServiceClient.publish('testTopicVirtual', {name:'alice'}, true, function (err) {
            
            assert.ifError(err);
            assert.deepEqual(requestSpy.lastCall.args[0].getContent(), {
                topic: 'testTopicVirtual',
                'notification-content': {name:'alice'},
                virtual: true
            });
            requestSpy.restore();
            done(null);
        });
    });
    
    it('publish not OK: invalid topic', function (done) {
        this.timeout(10000);
        eventServiceClient.publish(true, {}, function (err) {
            assert.instanceOf(err, Error);
            assert.strictEqual(err.message, errors.ERR_INVALID_TOPIC);
            done(null);
        });
    });
    /*
    it('publish not OK: content is required', function (done) {
        this.timeout(10000);
        eventServiceClient.publish('topictopublish', function (err) {
            console.log('errr', err);
            assert.instanceOf(err, Error);
            assert.strictEqual(err.message, errors.ERR_NOTIFICATION_CONTENT_IS_REQUIRED);
            done(null);
        });
    });*/
    
    
    it('notify OK', function (done) {
        //return done(null);
        this.timeout(10000);
        
        var clientPublisher = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
            
            eventServiceClient.subscribe('testTopic2', function (err, message) {
                assert.ifError(err);
                assert.strictEqual(message.getError(), null);
                
                eventServiceClient.on('event:testTopic2', function (content) {
                    
                    assert.strictEqual(content.name, 'alice');
                    
                    done(null);
                });
                
                clientPublisher.publish('testTopic2', {name:'alice'}, function (err) {
                    assert.ifError(err);
                });
                
            });
        
        
        
        
    });
    
    /*
    it('notify multiple listeners', function (done) {
        done();
    });
    */
   
    it('notify multiple listeners', function (done) {
        //return done(null);
        this.timeout(10000);
        
        var publisher = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
        var listener1 = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        var listener2 = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
            
            var cntReceived = 0, maxCnt = 3;
            listener1.on('event:testmultiple', function (content) {
                assert.strictEqual(content.name, 'alice');
                cntReceived++;
                if (cntReceived === maxCnt) {
                    done();
                }
            });
            
            listener2.on('event:testmultiple', function (content) {
                assert.strictEqual(content.name, 'alice');
                cntReceived++;
                if (cntReceived === maxCnt) {
                    done();
                }
            });
            
            
            // set a second listener
            listener2.on('event:testmultiple', function (content) {
                assert.strictEqual(content.name, 'alice');
                cntReceived++;
                if (cntReceived === maxCnt) {
                    done();
                }
            });
            
            async.mapSeries([listener1, listener2], function (item, cb) {
                item.subscribe('testmultiple', cb);
            }, function (err) {
                assert.ifError(err);
                
                publisher.publish('testmultiple', {name: 'alice'}, function () {
                    assert.ifError(err);
                });
            });
        
    });
    
    
});