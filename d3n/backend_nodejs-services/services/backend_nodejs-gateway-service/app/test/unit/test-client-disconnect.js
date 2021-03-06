var config = require('../../config/config.js');
var ip =  config.gateway.ip;
var port = config.gateway.port + config.incrementPort();
var jwtModule = require('jsonwebtoken');
//port = port-500;
var secure = true;
var registryServiceURIs = config.registryServiceURIsNode;

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('./classes/FakeRegistryService.js');
var FakeEventService = require('./classes/FakeEventService.js');
var DefaultService = require('nodejs-default-service');
var ServiceClient = require('nodejs-default-client');
var Gateway = require('../../classes/Gateway.js');
var logger = require('nodejs-logger')();
var assert = require('chai').assert;
var fs = require('fs');
var ServiceRegistryClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');
var FakeProfileService = require('./classes/FakeProfileService.js');
var Errors = require('nodejs-errors');
var sinon = require('sinon');
var sinonSandbox;

//var serviceRegistryInstancesConfig = [];
var instancesConfig = [];

var serviceRegsitryClientConfig = [];
// service registry instances
_.each(registryServiceURIs.split(','), function (uri) {
    var hostConfig = nodeUrl.parse(uri);
    instancesConfig.push({
        ip: hostConfig.hostname,
        port: hostConfig.port,
        secure: hostConfig.protocol === 'wss:',
        serviceName: 'serviceRegistry',
        registryServiceURIs: '',
        
        class: FakeRegistryService
    });
    
    
    serviceRegsitryClientConfig.push({
        service: {
            ip: hostConfig.hostname,
            port: hostConfig.port
        },
        secure: hostConfig.protocol === 'wss:'
    });
});

// other instances
instancesConfig.push({
    ip: ip,
    port: port+1,
    secure: true,
    serviceName: 'question',
    serviceNamespaces: ['question', 'workorder', 'application'],
    registryServiceURIs: registryServiceURIs,
    
    class: DefaultService
});

instancesConfig.push({
    ip: ip,
    port: port+2,
    secure: true,
    serviceName: 'event',
    registryServiceURIs: registryServiceURIs,
    
    class: FakeEventService
});
instancesConfig.push({
    ip: ip,
    port: port+3,
    secure: true,
    serviceName: 'profile',
    registryServiceURIs: registryServiceURIs,
    
    class: FakeProfileService
});


for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8');
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage = false;
}

var serviceInstances = [];

var clients = [];
var instances = [];
var gateway = null;
var questionService;
var userIds = ['disc-111-342-45435-asd-w23', 'disc-wer-345sdf-345-sdf-r'];

describe('TEST CLIENT DISCONNECT(LOG OUT)', function() {
    this.timeout(20000);
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    it('create service instances', function (done) {
        
        function createInstances(configs, cb) {
            async.mapSeries(configs, function (config, cbItem) {
                var cls = config.class;
                delete config.class;
                var inst = new cls(config);
                instances.push(inst);
                if (config.serviceName === 'question') {
                    questionService = inst;
                }
                inst.build(function (err) {
                    logger.debug('build err:', err);
                    cbItem(err);
                });
            }, cb);
        };
        
        createInstances(instancesConfig, function (err) {
            // wait a bit for namespace registration
            setTimeout(function () {
                done(err);
            }, 1500);
        });
    });
    
    it('create gateway', function (done) {
        gateway = new Gateway({
            gateway: {
                ip: ip,
                port: port-1,
                secure: secure,
                key: __dirname + '/ssl-certificate/key.pem',
                cert: __dirname + '/ssl-certificate/cert.pem',
                auth: config.gateway.auth
            },
            registryServiceURIs: registryServiceURIs
        });
        
        gateway.build(done);
    });
    
    
    it('create two clients, send token then disconnect', function (done) {
        var client1 = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client1);
        var client2 = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client2);
        var gatewayURL = ip + ':' + (port-1);
        
        //var userid = '111-222-333';
        var user1Payload = {
            userId: userIds[0],
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        var user2Payload = {
            userId: userIds[1],
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        var client1Id;
        var client2Id;
        
        var user1Token = jwtModule.sign(user1Payload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var user2Token = jwtModule.sign(user2Payload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        sinonSandbox = sinon.sandbox.create();
        var serviceClientDisconnectSpy = sinonSandbox.spy(questionService.messageHandlers, "clientDisconnect");
        
        async.series([
            function (next) {
                client1.connect(function (err) {
                    if (err) {
                        return next(err);
                    }
                    client1Id = _.keys(gateway.clientConnection)[0];
                    return next();
                });
            },
            function (next) {
                client2.connect(function (err) {
                    if (err) {
                        return next(err);
                    }
                    client2Id = _.keys(gateway.clientConnection)[1];
                    return next();
                });
            },
            
            // client 1 send message with token(log in)
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(user1Token);
                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // client 1 send message without token
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // client 2 send message with token(log in)
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(user2Token);
                client2.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // disconnect (log out) client 1
            function (next) {
                try {
                    assert.strictEqual(_.keys(gateway.clientConnection).length, 2);
                    assert.strictEqual(_.keys(gateway.messageDispatcher.clientsSession).length, 2);
                    assert.strictEqual(_.keys(gateway.messageDispatcher.clientsQueue).length, 2);
                    
                    client1.disconnect(1000, '', true);
                    
                    setTimeout(function () {
                        try {
                            // client 1 connection removed from gateway
                            assert.notProperty(gateway.clientConnection, client1Id);
                            assert.strictEqual(_.keys(gateway.clientConnection).length, 1);
                            assert.strictEqual(_.keys(gateway.clientConnection)[0], client2Id);

                            // client 1 session removed from message dispatcher
                            assert.notProperty(gateway.messageDispatcher.clientsSession, client1Id);
                            assert.strictEqual(_.keys(gateway.messageDispatcher.clientsSession).length, 1);
                            assert.strictEqual(_.keys(gateway.messageDispatcher.clientsSession)[0], client2Id);

                            // client 1 message queue removed from message dispatcher
                            assert.notProperty(gateway.messageDispatcher.clientsQueue, client1Id);
                            assert.strictEqual(_.keys(gateway.messageDispatcher.clientsQueue).length, 1);
                            assert.strictEqual(_.keys(gateway.messageDispatcher.clientsQueue)[0], client2Id);

                            // service disconnection
                            assert.strictEqual(serviceClientDisconnectSpy.callCount, 1);
                            assert.strictEqual(serviceClientDisconnectSpy.getCall(0).args[0].getClientId(), client1Id);
                            return next();
                            
                        } catch (e) {
                            return next(e);
                        }
                    }, 500);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // disconnect (log out) client 2
            function (next) {
                try {
                    client2.disconnect(1000, '', true);
                    
                    setTimeout(function () {
                        try {
                            assert.strictEqual(_.keys(gateway.clientConnection).length, 0);
                            assert.strictEqual(_.keys(gateway.messageDispatcher.clientsSession).length, 0);
                            assert.strictEqual(_.keys(gateway.messageDispatcher.clientsQueue).length, 0);

                            // service disconnection
                            assert.strictEqual(serviceClientDisconnectSpy.callCount, 2);
                            assert.strictEqual(serviceClientDisconnectSpy.getCall(1).args[0].getClientId(), client2Id);

                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }, 500);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], done);
    });
    
    
    after(function (done) {
        
        for(var i=0; i<clients.length; i++) {
            try {
                clients[i].disconnect(1000, '', true);
            } catch (e) {}
        }
        
        try {
            gateway.shutdown();
        } catch (e) {}
        
        for(var i=0; i<instances.length; i++) {
            try {
                instances[i].shutdown();
            } catch (e) {}
        }
        
        setImmediate(done);
    });
    
    
});