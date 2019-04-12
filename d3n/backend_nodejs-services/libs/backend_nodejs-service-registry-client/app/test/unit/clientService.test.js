var async = require('async');
//var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var FakeService = require('./../classes/FakeRegistryService.js');
var ServiceRegistryClient = require('./../../classes/ServiceRegistryClient.js');
var ProtocolMessage = require('nodejs-protocol');
var _ = require('lodash');
var sinon = require('sinon');
var fakeService;
var clients = [];
var registryServiceURIs = Config.fakeHostURI();
var sinonSandbox;
var client;

describe('SRC EXTRA TESTS', function () {
    before(function (done) {
        var cfg = {
            ip: Config.fakeHost.service.ip,
            port: Config.fakeHost.service.port,
            secure: Config.fakeHost.secure,
            key: Config.fakeHost.key,
            cert: Config.fakeHost.cert,
            validateFullMessage: false
        };
        
        fakeService = new FakeService(cfg);
        fakeService.build(done);
    });
    
    after(function (done) {
        async.eachSeries(clients, function (client, next) {
            if (client) {
                return client.disconnect(next);
            }
            next(true);
        }, function (err) {
            fakeService.shutdown(false);
            done(err);
        });
    });
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
        if (client) {
            client._defaultClient.disconnect(1000, '', true);
        }
    });
    
    it('register success', function (done) {
        this.timeout(20000);
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        var registerSpy = sinonSandbox.spy(fakeService.messageHandlers, "register");
        
        var serviceModel = {
            serviceName: 'question',
            uri: 'wss://localhost:1234',
            serviceNamespaces: ['question', 'workorder']
        };
        
        client.register(serviceModel.serviceName, serviceModel.uri, serviceModel.serviceNamespaces, function (err, response) {
            try {
                assert.ifError(err);
                assert.isNull(response.getError());
                assert.strictEqual(registerSpy.callCount, 1);
                assert.deepEqual(registerSpy.getCall(0).args[0].getContent(), serviceModel);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('unregister success', function (done) {
        this.timeout(20000);
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        var unregisterSpy = sinonSandbox.spy(fakeService.messageHandlers, "unregister");
        
        client.unregister(function (err, response) {
            try {
                assert.ifError(err);
                assert.isNull(response.getError());
                assert.strictEqual(unregisterSpy.callCount, 1);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('get success', function (done) {
        this.timeout(20000);
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        var getSpy = sinonSandbox.spy(fakeService.messageHandlers, "get");
        
        async.series([
            // get by serviceName
            function (next) {
                client.get('question', null, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(getSpy.callCount, 1);
                        assert.deepEqual(getSpy.getCall(0).args[0].getContent(), {serviceName: 'question'});
                        assert.property(response.getContent(), 'service');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // get by serviceNamespace
            function (next) {
                client.get(null, 'ns1', function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(getSpy.callCount, 2);
                        assert.deepEqual(getSpy.getCall(1).args[0].getContent(), {serviceNamespace: 'ns1'});
                        assert.property(response.getContent(), 'service');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], done);
    });
    
    it('list success', function (done) {
        this.timeout(20000);
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        var listSpy = sinonSandbox.spy(fakeService.messageHandlers, "list");
        
        async.series([
            // get by serviceName
            function (next) {
                client.list('question', null, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(listSpy.callCount, 1);
                        assert.deepEqual(listSpy.getCall(0).args[0].getContent(), {serviceName: 'question'});
                        assert.property(response.getContent(), 'services');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // get by serviceNamespace
            function (next) {
                client.list(null, 'ns1', function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(listSpy.callCount, 2);
                        assert.deepEqual(listSpy.getCall(1).args[0].getContent(), {serviceNamespace: 'ns1'});
                        assert.property(response.getContent(), 'services');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], done);
    });
    
    
    it('heartbeat', function (done) {
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        var heartbeatResponseStub = sinonSandbox.stub(fakeService.messageHandlers, "heartbeatResponse", function (message, clientSession) {
            try {
                assert.strictEqual(message.getMessage(), 'serviceRegistry/heartbeatResponse');
                assert.strictEqual(message.getAck().length, 1);
                assert.strictEqual(message.getAck()[0], 123);
                return done();
            } catch (e) {
                return done(e);
            }
        });
        
        var message = new ProtocolMessage();
        message.setMessage('serviceRegistry/heartbeat');
        message.setSeq(123);
        client._onMessageHandler(message);
    });
    
    
    it('list return empty array if response contain null URIs', function (done) {
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        
        var services = [
            {
                serviceName: 's1',
                uri: 'wss://localhost:11',
                serviceNamespaces: ['a', 'b']
            },
            {
                serviceName: 's2',
                uri: null,
                serviceNamespaces: ['a2', 'b2']
            }
        ];
        var listStub = sinonSandbox.stub(fakeService.messageHandlers, "list", function (message, clientSession) {
            var response = new ProtocolMessage(message);
            response.setContent({
                services: services
            });
            clientSession.sendMessage(response);
        });
        
        client.list('returnNullURIs', null, function (err, response) {
            try {
                assert.ifError(err);
                assert.strictEqual(listStub.callCount, 1);
                assert.typeOf(response.getContent().services, 'Array');
                assert.strictEqual(response.getContent().services.length, 0);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('get return empty array if response contain null URIs', function (done) {
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        
        var service = {
            serviceName: 's2',
            uri: null,
            serviceNamespaces: ['a2', 'b2']
        };
        
        var getStub = sinonSandbox.stub(fakeService.messageHandlers, "get", function (message, clientSession) {
            var response = new ProtocolMessage(message);
            response.setContent({
                service: service
            });
            clientSession.sendMessage(response);
        });
        
        client.get('returnNullURIs', null, function (err, response) {
            try {
                assert.ifError(err);
                assert.strictEqual(getStub.callCount, 1);
                assert.deepEqual(response.getContent().service, {});
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    /*
    it('get return empty object if response contain null URI', function (done) {
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        
        client.get('returnNullURIs', function (err, response) {
            try {
                assert.ifError(err);
                assert.deepEqual(response.getContent().service, {});
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });*/
    
    
    it('register, disconnect, should re-register', function (done) {
        this.timeout(20000);
        client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 2000});
        
        sinonSandbox = sinon.sandbox.create();
        var registerSpy = sinonSandbox.spy(fakeService.messageHandlers, "register");
        //var multiRegisterSpy = sinonSandbox.spy(fakeService._websocketService.messageHandlers, "multiRegister");
        
        var regs = [
            {
                serviceName: 'question',
                uri: 'wss://localhost:1',
                serviceNamespaces: ['ns1', 'ns2']
            },
            {
                serviceName: 'auth',
                uri: 'wss://localhost:2',
                serviceNamespaces: ['auth']
            }
        ];
        
        
        async.series([
            // register
            function (next) {
                client.register(regs[0].serviceName, regs[0].uri, regs[0].serviceNamespaces, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(registerSpy.callCount, 1);
                        assert.deepEqual(registerSpy.getCall(0).args[0].getContent(), regs[0]);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // register
            function (next) {
                client.register(regs[1].serviceName, regs[1].uri, regs[1].serviceNamespaces, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(registerSpy.callCount, 2);
                        assert.deepEqual(registerSpy.getCall(1).args[0].getContent(), regs[1]);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            function (next) {
                try {
                    assert.deepEqual(client._servicesForReregister, regs);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // disconnect, should reconnect and re-register
            function (next) {
                try {
                    client._defaultClient.disconnect(1000, '', false);
                    
                    setTimeout(function () {
                        try {
                            assert.deepEqual(registerSpy.callCount, 4);
                            assert.deepEqual(registerSpy.getCall(2).args[0].getContent(), regs[0]);
                            assert.deepEqual(registerSpy.getCall(3).args[0].getContent(), regs[1]);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }, 500);
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // shutdown SR then create new one, should reconnect and re-register
            function (next) {
                try {
                    fakeService.shutdown(false);
                    
                    setTimeout(function () {
                        fakeService.build(function () {
                            
                        });
                        
                        setTimeout(function () {
                            try {
                                assert.deepEqual(registerSpy.callCount, 6);
                                assert.deepEqual(registerSpy.getCall(4).args[0].getContent(), regs[0]);
                                assert.deepEqual(registerSpy.getCall(5).args[0].getContent(), regs[1]);
                                next();
                            } catch (e) {
                                next(e);
                            }
                        }, 2000);
                        
                    }, 5000);
                } catch (e) {
                    return setImmediate(next, e);
                }
                
            }
            
        ], function (err) {
            done(err);
        });
        
    });
    
    
    it('call register while SR is down, then start SR', function (done) {
         this.timeout(10000);
        
        sinonSandbox = sinon.sandbox.create();
        var registerSpy = sinonSandbox.spy(fakeService.messageHandlers, "register");
        var listSpy = sinonSandbox.spy(fakeService.messageHandlers, "list");
        
        var reg = {
            serviceName: 'www',
            uri: 'wss://localhost:23',
            serviceNamespaces: ['aaa', 'bbb']
        };
        
        async.series([
            function (next) {
                fakeService.shutdown(false);
                return setImmediate(next);
            },
            
            // while SR is down call APIs, should be added in the queue
            function (next) {
                try {
                    client = new ServiceRegistryClient({registryServiceURIs: registryServiceURIs, reconnectTryForeverInterval: 500});
                    assert.strictEqual(registerSpy.callCount, 0);
                    
                    client.list('www', null, function (err, response) {
                        try {
                            //console.log('>>>here:', response.getContent());
                            assert.ifError(err);
                            assert.isNull(response.getError());
                            assert.strictEqual(registerSpy.callCount, 1);
                            assert.strictEqual(listSpy.callCount, 1);
                            done();
                        } catch (e) {
                            return done(e);
                        }
                    });
                    
                    client.register(reg.serviceName, reg.uri, reg.serviceNamespaces, function (err, response) {
                        try {
                            assert.ifError(err);
                            assert.isNull(response.getError());
                            assert.strictEqual(registerSpy.callCount, 1);
                            assert.deepEqual(registerSpy.getCall(0).args[0].getContent(), reg);
                        } catch (e) {
                            return done(e);
                        }
                    });
                    
                    setTimeout(function () {
                        fakeService.build(function () {
                            
                        });
                    }, 1500);
                    
                } catch (e) {
                    return setImmediate(done, e);
                }
            }
        ]);
        
    });
    
    
});
