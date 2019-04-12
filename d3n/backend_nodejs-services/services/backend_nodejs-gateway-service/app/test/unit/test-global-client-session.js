process.env.ADD_FAKE_APP_CONFIG='false';
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
var FakeProfileService = require('./classes/FakeProfileService.js');
var FakeQuestionService = require('./classes/FakeQuestionService.js');
var DefaultService = require('nodejs-default-service');
var ServiceClient = require('nodejs-default-client');
var Gateway = require('../../classes/Gateway.js');
var logger = require('nodejs-logger')();
var assert = require('chai').assert;
var fs = require('fs');
var ServiceRegistryClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');
var Errors = require('nodejs-errors');
var sinon = require('sinon');
var sinonSandbox;
var KeyvalueService = require('nodejs-aerospike').getInstance().KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;

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
var mediaServiceConfig = {
    ip: ip,
    port: port+1,
    secure: true,
    serviceName: 'mediaservice',
    registryServiceURIs: registryServiceURIs,
    
    class: DefaultService
};
var mediaServiceInstance = null;
var mediaService2Instance = null;
instancesConfig.push(mediaServiceConfig);

instancesConfig.push({
    ip: ip,
    port: port+2,
    secure: true,
    serviceName: 'question',
    serviceNamespaces: ['question', 'workorder', 'application'],
    registryServiceURIs: registryServiceURIs,
    
    class: FakeQuestionService
});

instancesConfig.push({
    ip: ip,
    port: port+3,
    secure: true,
    serviceName: 'event',
    registryServiceURIs: registryServiceURIs,
    
    class: FakeEventService
});

instancesConfig.push({
    ip: ip,
    port: port+4,
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
var userIds = ['111-342-45435-asd-w23', 'wer-345sdf-345-sdf-r'];
var appConfig = {
    appId: 'appid',
    deviceUUID: 'deviceUUID',
    IMEI: '0752-333-444',
    tenantId: '12345',
    device: {
        a: 'aa',
        b: 'bb'
    }
};
var appConfig2 = {
    appId: 'appid2',
    deviceUUID: 'deviceUUID2',
    tenantId: '123452',
    device: {
        a: 'aa2'
    }
};
var appConfigWrong = {
    appId: 'appid2',
    deviceUUID: 'deviceUUID2',
    tenantId: 123452,
    device: {
        a: 'aa2'
    }
};



describe('TEST GLOBAL CLIENT SESSION', function() {
    this.timeout(20000);
    
    beforeEach(function (done) {
        async.mapSeries(userIds, function (userId, cb) {
            AerospikeGlobalClientSession.removeUserSession(userId, cb);
        }, done);
    });
    
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
                if (config.serviceName === 'mediaservice') {
                    mediaServiceInstance = inst;
                }
                if (config.serviceName === 'mediaservice2') {
                    mediaService2Instance = inst;
                }
                instances.push(inst);
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
    
    it('send only anonymous requests, no global session will be created', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        var aerospikeaddOrUpdateGlobalClientSessionSpy = sinonSandbox.spy(gateway.messageDispatcher._aerospike, "addOrUpdateGlobalClientSession");
        var aerospikeRemoveGlobalClientSesionSpy = sinonSandbox.spy(gateway.messageDispatcher._aerospike, "removeGlobalClientSesion");
        
        async.series([
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                async.map([11,34, 76], function (seq, cb) {
                    var m = new ProtocolMessage();
                    m.setMessage('question/ping');
                    m.setSeq(seq);

                    client.sendMessage(m, function (err, response) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(response.getError(), null);
                            assert.strictEqual(response.getMessage(), 'question/pingResponse');
                            return cb();
                        } catch (e) {
                            return cb(e);
                        }
                    });
                }, function (err) {
                    client.disconnect(1000, '', true);
                    setTimeout(next, 500, err);
                });
            },
            
            function (next) {
                try {
                    assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 0);
                    assert.strictEqual(aerospikeRemoveGlobalClientSesionSpy.callCount, 0);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], done);
        
    });
    it('test external url', function (done) {
        var gwport = port-9;
        var client1 = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: gwport
            }
        });
        clients.push(client1);
        var client1Id;
        var userid = userIds[0];
        var userPayload = {
            userId: userid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        sinonSandbox = sinon.sandbox.create();
        var aerospikeaddOrUpdateGlobalClientSessionSpy;
        
        var gw;
        //process.env.EXTERN_IP = "gatewayurl.com";
        var gatewayURL = 'gatewayurl.com:20';
        async.series([
            function (next) {
                
                gw = new Gateway({
                    gateway: {
                        ip: ip,
                        port: gwport,
                        secure: secure,
                        externUrl: "wss://gatewayurl.com:20",
                        key: __dirname + '/ssl-certificate/key.pem',
                        cert: __dirname + '/ssl-certificate/cert.pem',
                        auth: config.gateway.auth
                    },
                    registryServiceURIs: registryServiceURIs
                });
                aerospikeaddOrUpdateGlobalClientSessionSpy = sinonSandbox.spy(gw.messageDispatcher._aerospike, "addOrUpdateGlobalClientSession");
                gw.build(function (err) {
                    setTimeout(next, 500, err);
                });
            },
            
            
            function (next) {
                try {
                    client1.connect(function (err) {
                        try {
                            if (err) {
                                return next(err);
                            }
                            client1Id = _.keys(gw.clientConnection)[0];
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(userToken);

                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 1);
                        AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                            try {
                                //console.log('\n\n\n\n>>>>>sessions:', err, sessions);
                                assert.ifError(err);
                                assert.strictEqual(sessions.length, 1);
                                assert.deepEqual(_.omit(sessions[0], 'timestamp'), {clientId: client1Id, gatewayURL: gatewayURL});
                                //console.log('\n\n\n\n\n\n\n>>>>>>>here')
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                delete process.env.EXTERN_IP;
                client1.disconnect(1000, '', true);
                gw.shutdown();
                return setImmediate(next);
            }
            
        ], done);
    });
    
    it('create global client session', function (done) {
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
        
        var userid = userIds[0];
        var userPayload = {
            userId: userid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        var client1Id;
        var client2Id;
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        sinonSandbox = sinon.sandbox.create();
        var aerospikeaddOrUpdateGlobalClientSessionSpy = sinonSandbox.spy(gateway.messageDispatcher._aerospike, "addOrUpdateGlobalClientSession");
        var aerospikeRemoveGlobalClientSesionSpy = sinonSandbox.spy(gateway.messageDispatcher._aerospike, "removeGlobalClientSesion");
        
        
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
            
            // send message without token, no user session is created
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);

                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 0);
                        
                        AerospikeGlobalClientSession.getUserSession(userid, function (err) {
                            try {
                                assert.strictEqual(err, 'ERR_ENTRY_NOT_FOUND');
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send message with token, user session will be created
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(userToken);

                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 1);
                        AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                            try {
                                //console.log('>>>>>sessions:', sessions);
                                assert.ifError(err);
                                assert.strictEqual(sessions.length, 1);
                                assert.deepEqual(_.omit(sessions[0], 'timestamp'), {clientId: client1Id, gatewayURL: gatewayURL});
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // send a second message with token, nothing change on global client session
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(userToken);

                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 1);
                        
                        AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(sessions.length, 1);
                                assert.deepEqual(_.omit(sessions[0], 'timestamp'), {clientId: client1Id, gatewayURL: gatewayURL});
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                client2.connect(function (err) {
                    if (err) {
                        return next(err);
                    }
                    client2Id = _.without(_.keys(gateway.clientConnection), client1Id)[0];
                    return next();
                });
            },
            // send message with token, user will have two client sessions
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(userToken);

                client2.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 2);
                        
                        AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                            try {
                                //console.log(sessions);
                                assert.ifError(err);
                                assert.strictEqual(sessions.length, 2);
                                
                                var dbSessions = _.orderBy(sessions, 'clientId');
                                var expectedSessions = _.orderBy([{clientId: client1Id, gatewayURL: gatewayURL}, {clientId: client2Id, gatewayURL: gatewayURL}], 'clientId');
                                
                                
                                assert.deepEqual(_.omit(dbSessions[0], 'timestamp'), expectedSessions[0]);
                                assert.deepEqual(_.omit(dbSessions[1], 'timestamp'), expectedSessions[1]);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // disconnect client 1, only one client session will remain
            function (next) {
                client1.disconnect(1000, '', true);
                setTimeout(function () {
                    assert.strictEqual(aerospikeRemoveGlobalClientSesionSpy.callCount, 1);
                    AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                        try {
                            //console.log(sessions);
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 1);
                            assert.deepEqual(_.omit(sessions[0], 'timestamp'), {clientId: client2Id, gatewayURL: gatewayURL});
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                }, 500);
            },
            
            // disconnect client 2, everything removed
            function (next) {
                client2.disconnect(1000, '', true);
                setTimeout(function () {
                    assert.strictEqual(aerospikeRemoveGlobalClientSesionSpy.callCount, 2);
                    AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(sessions.length, 0);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                }, 500);
            }
            
            
        ], done);
    });
    
    
    it('change token user id, old session must be removed', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        var gatewayURL = ip + ':' + (port-1);
        
        var userid = userIds[0];
        var useridNew = userIds[1];
        
        var userPayload = {
            userId: userid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        var newUserPayload = {
            userId: useridNew,
            roles: ['manager', 'admin'],
            iat: Date.now()+10000
        };
        
        var clientId;
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var newUserToken = jwtModule.sign(newUserPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        sinonSandbox = sinon.sandbox.create();
        var aerospikeaddOrUpdateGlobalClientSessionSpy = sinonSandbox.spy(gateway.messageDispatcher._aerospike, "addOrUpdateGlobalClientSession");
        var aerospikeRemoveGlobalClientSesionSpy = sinonSandbox.spy(gateway.messageDispatcher._aerospike, "removeGlobalClientSesion");
        
        
        async.series([
            function (next) {
                client.connect(function (err) {
                    if (err) {
                        return next(err);
                    }
                    clientId = _.keys(gateway.clientConnection)[0];
                    return next();
                });
            },
            
            // send message with token, user session will be created
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(userToken);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 1);
                        AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                            try {
                                //console.log('>>>>>sessions:', sessions);
                                assert.ifError(err);
                                assert.strictEqual(sessions.length, 1);
                                assert.deepEqual(_.omit(sessions[0], 'timestamp'), {clientId: clientId, gatewayURL: gatewayURL});
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // send a second message with token, nothing change on global client session
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(userToken);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 1);
                        
                        AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(sessions.length, 1);
                                assert.deepEqual(_.omit(sessions[0], 'timestamp'), {clientId: clientId, gatewayURL: gatewayURL});
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send message with new token, new user session will be created, old one will be deleted
            function (next) {
                //return next();
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(3);
                m.setToken(newUserToken);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'question/pingResponse');
                        assert.strictEqual(aerospikeaddOrUpdateGlobalClientSessionSpy.callCount, 2);
                        AerospikeGlobalClientSession.getUserClientSessions(useridNew, function (err, sessions) {
                            try {
                                //console.log('>>>>>sessions:', sessions);
                                assert.ifError(err);
                                assert.strictEqual(sessions.length, 1);
                                assert.deepEqual(_.omit(sessions[0], 'timestamp'), {clientId: clientId, gatewayURL: gatewayURL});
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                AerospikeGlobalClientSession.getUserClientSessions(userid, function (err, sessions) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(sessions.length, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
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