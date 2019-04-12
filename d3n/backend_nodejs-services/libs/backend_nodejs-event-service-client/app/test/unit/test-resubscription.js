var async = require('async');
var assert = require('chai').assert;
var should = require('should');
var fs = require('fs');
var cp = require('child_process');
var url = require('url');
var config = require('../../config/config.js');
var errors = require('../../config/errors.js');
var EventServiceClient = require('../../classes/EventServiceClient.js');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var eventServiceClient = null;
var sinon = require('sinon');

describe('Test resubscription', function() {
    
    it('should create and init event service client', function (done) {
        //return done();
        this.timeout(10000);
        
        eventServiceClient = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        done(null);
    });
    
    it('subscribe try forever, default params', function (done) {
        this.timeout(10000);
        var _requestStub = sinon.stub(eventServiceClient, "_request", function (message, cb) {
            return setImmediate(cb, new Error('CONNECTION_ERROR'));
        });
        
        eventServiceClient.subscribe('tryforever', function (err, response) {
            try {
                assert.ifError(err);
                assert.isNull(response.getError());
                _requestStub.restore();
                return done();
            } catch (e) {
                return done(e);
            }
        }, false, true);
        
        
        setTimeout(function () {
            try {
                assert.strictEqual(_requestStub.callCount, 3);
                
                _requestStub.restore();
                _requestStub = sinon.stub(eventServiceClient, "_request", function (message, cb) {
                    var response = new ProtocolMessage(message);
                    return setImmediate(cb, false, response);
                });
            } catch (e) {
                return done(e);
            }
        }, 5000);
    });

    it('subscribe try forever, custom params', function (done) {
        this.timeout(10000);
        //eventServiceClient._request.restore();
        var _requestStub = sinon.stub(eventServiceClient, "_request", function (message, cb) {
            return setImmediate(cb, new Error('CONNECTION_ERROR'));
        });
        
        eventServiceClient.subscribe('tryforever2', function (err, response) {
            try {
                assert.ifError(err);
                assert.isNull(response.getError());
                _requestStub.restore();
                return done();
            } catch (e) {
                return done(e);
            }
        }, false, {
            interval: 500
        });
        
        
        setTimeout(function () {
            try {
                assert.strictEqual(_requestStub.callCount, 3);
                
                _requestStub.restore();
                _requestStub = sinon.stub(eventServiceClient, "_request", function (message, cb) {
                    var response = new ProtocolMessage(message);
                    return setImmediate(cb, false, response);
                });
            } catch (e) {
                return done(e);
            }
        }, 1200);
    });
    
    it('subscribe try forever, timeout', function (done) {
        this.timeout(10000);
        var _requestStub = sinon.stub(eventServiceClient, "_request", function (message, cb) {
            setTimeout(function () {
                var response = new ProtocolMessage(message);
                return cb(false, response);
            }, 700);
        });
        
        eventServiceClient.subscribe('tryforever3', function (err, response) {
            try {
                assert.ifError(err);
                assert.isNull(response.getError());
                _requestStub.restore();
                return done();
            } catch (e) {
                return done(e);
            }
        }, false, {
            timeout: 600,
            interval: 500
        });
        
        
        setTimeout(function () {
            try {
                assert.strictEqual(_requestStub.callCount, 2);
                
                _requestStub.restore();
                _requestStub = sinon.stub(eventServiceClient, "_request", function (message, cb) {
                    var response = new ProtocolMessage(message);
                    return setImmediate(cb, false, response);
                });
            } catch (e) {
                return done(e);
            }
        }, 1200);
    });
    
    it('client disconnect use same event service and resubscribe ok', function (done) {
        //return done();
        this.timeout(20000);
        
        var publisher = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
        var listener1 = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        var listener2 = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
            
            var cntReceived = 0, expectedReceived = 3;
            listener1.on('event:testmultiple', function (content) {
                try {
                    assert.strictEqual(content.name, 'alice');
                    cntReceived++;
                } catch (e) {
                    return done(e);
                }
            });
            
            listener2.on('event:testmultiple', function (content) {
                try {
                    assert.strictEqual(content.name, 'alice');
                    cntReceived++;
                } catch (e) {
                    return done(e);
                }
            });
            
            // set a second listener
            listener2.on('event:testmultiple', function (content) {
                try {
                    assert.strictEqual(content.name, 'alice');
                    cntReceived++;
                } catch (e) {
                    return done(e);
                }
            });
            
            async.mapSeries([listener1, listener2], function (item, cb) {
                item.subscribe('testmultiple', cb);
            }, function (err) {
                assert.ifError(err);
                
                // disconnect ws client
                listener1._defaultClient.disconnect(1000, '', false);
                listener2._defaultClient.disconnect(1000, '', false);
                
                // wait a bit for resubscription then publish 
                setTimeout(function () {
                    publisher.publish('testmultiple', {name: 'alice'}, function () {
                        if (err) {
                            return done(err);
                        }
                        
                        setTimeout(function () {
                            try {
                                assert.strictEqual(expectedReceived, cntReceived);
                                
                                listener1._defaultClient.disconnect(1000, '', true);
                                listener2._defaultClient.disconnect(1000, '', true);
                                publisher._defaultClient.disconnect(1000, '', true);
                                
                                return done();
                            } catch (e) {
                                return done(e);
                            }
                        }, 2000);
                    });
                }, 2000);
                
            });

        
    });
    
    it('event service down and resubscribe to a new event service ok', function (done) {
        //return done();
        this.timeout(20000);
        
        var publisher = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
        var listener1 = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        var listener2 = new EventServiceClient(config.registryServiceURIs, config.eventServiceName);
        
            
            var cntReceived = 0, expectedReceived = 3;
            listener1.on('event:testmultiple', function (content) {
                try {
                    assert.strictEqual(content.name, 'alice');
                    cntReceived++;
                } catch (e) {
                    return done(e);
                }
            });
            
            listener2.on('event:testmultiple', function (content) {
                try {
                    assert.strictEqual(content.name, 'alice');
                    cntReceived++;
                } catch (e) {
                    return done(e);
                }
            });
            
            // set a second listener
            listener2.on('event:testmultiple', function (content) {
                try {
                    assert.strictEqual(content.name, 'alice');
                    cntReceived++;
                } catch (e) {
                    return done(e);
                }
            });
            
            
            async.mapSeries([listener1, listener2], function (item, cb) {
                item.subscribe('testmultiple', cb);
            }, function (err) {
                assert.ifError(err);
                
                
                listener1._onReconnectedToNewInstanceHandler(function () {});
                listener2._onReconnectedToNewInstanceHandler(function () {});
                
                // wait a bit for resubscription then publish 
                setTimeout(function () {
                    publisher.publish('testmultiple', {name: 'alice'}, function () {
                        if (err) {
                            return done(err);
                        }
                        
                        setTimeout(function () {
                            try {
                                assert.strictEqual(expectedReceived, cntReceived);
                                
                                listener1._defaultClient.disconnect(1000, '', true);
                                listener2._defaultClient.disconnect(1000, '', true);
                                publisher._defaultClient.disconnect(1000, '', true);
                                
                                return done();
                            } catch (e) {
                                return done(e);
                            }
                        }, 2000);
                    });
                }, 2000);
                
            });

        
    });
    
});