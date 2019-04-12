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
var MessageDispatcher = require('../../classes/MessageDispatcher.js');
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
    port: port,
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
    port: port+1,
    secure: true,
    serviceName: 'question',
    serviceNamespaces: ['question', 'workorder', 'application'],
    registryServiceURIs: registryServiceURIs,
    
    class: FakeQuestionService
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



describe('TEST CLIENT INFO', function() {
    this.timeout(20000);
    
    beforeEach(function () {
        process.env.ADD_FAKE_APP_CONFIG='true';
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
    
    it('send only anonymous requests, appConfig=default app config, profile=null', function (done) {
        
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        sinonSandbox = sinon.sandbox.create();
        
        var pingSpy = sinonSandbox.spy(FakeQuestionService.prototype.messageHandlers, "ping");
        
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
                            assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig, MessageDispatcher.defaultAppConfig);
                            assert.isNull(pingSpy.lastCall.args[0].getClientInfo().profile);
                            return cb();
                        } catch (e) {
                            return cb(e);
                        }
                    });
                }, function (err) {
                    client.disconnect(1000, '', true);
                    setTimeout(next, 500, err);
                });
            }
        ], done);
        
    });

    it('send token , user related data set ok', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        var user1Payload = {
            userId: '312123',
            iat: Date.now()-10000
        };
        var user2Payload = {
            userId: '312123-2',
            iat: Date.now()+10000
        };
        var user2PayloadNewer = {
            userId: '312123-2',
            iat: Date.now()+20000
        };
        
        var user1Token = jwtModule.sign(user1Payload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var user2Token = jwtModule.sign(user2Payload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var user2TokenNewer = jwtModule.sign(user2PayloadNewer, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        sinonSandbox = sinon.sandbox.create();
        
        
        var user1Profile = {
            userId: user1Payload.userId,
            roles: ['EXTERNAL', 'TENANT_1_ADMIN'],
            language: 'en',
            handicap: 1.2,
            emails: [
                {email: 'www@gmail.com', verificationStatus: 'verified'},
                {email: 'www2@gmail.com', verificationStatus: 'notVerified'}
            ],
            phones: [
                {phone: '11-22', verificationStatus: 'verified'},
                {phone: '33-44', verificationStatus: 'notVerified'}
            ]
        };
        var user2Profile = {
            userId: user2Payload.userId,
            roles: ['EXTERNAL', 'TENANT_1_ADMIN', 'INTERNAL'],
            language: 'de',
            handicap: 1.4,
            emails: [
                {email: 'www@gmail.com', verificationStatus: 'verified'},
                {email: 'www2@gmail.com', verificationStatus: 'notVerified'}
            ],
            phones: [
                {phone: '11-22', verificationStatus: 'verified'},
                {phone: '33-44', verificationStatus: 'notVerified'}
            ]
        };
        
        var pingSpy = sinonSandbox.spy(FakeQuestionService.prototype.messageHandlers, "ping");
        var getProfileStub = sinonSandbox.stub(FakeProfileService.prototype.messageHandlers, 'getProfile', function (message, clientSession) {
            
            var response = new ProtocolMessage(message);
            var profile;
            if (message.getContent().userId === user1Payload.userId) {
                profile = user1Profile;
            }
            if (message.getContent().userId === user2Payload.userId) {
                profile = user2Profile;
            }
            
            response.setContent({
                profile: profile
            });
            clientSession.sendMessage(response);
        });
        
        async.series([
            function (next) {
                client.connect(next);
            },
            
            // user id and roles set ok
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(user1Token);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(getProfileStub.callCount, 1);
                        assert.isNotNull(pingSpy.lastCall.args[0].getClientInfo().ip);
                        assert.isString(pingSpy.lastCall.args[0].getClientInfo().ip);
                        assert.isAbove(pingSpy.lastCall.args[0].getClientInfo().ip.length, 5);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.userId, user1Profile.userId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.language, user1Profile.language);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.handicap, user1Profile.handicap);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.roles.sort(), user1Profile.roles.sort());
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.emails, [user1Profile.emails[0].email]);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.phones, [user1Profile.phones[0].phone]);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send a second message without token. nothing changes on profile or appConfig
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(getProfileStub.callCount, 1);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.userId, user1Profile.userId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.language, user1Profile.language);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.handicap, user1Profile.handicap);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.roles.sort(), user1Profile.roles.sort());
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.emails, [user1Profile.emails[0].email]);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.phones, [user1Profile.phones[0].phone]);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send a new toke having different user id
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(user2Token);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(getProfileStub.callCount, 2);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.userId, user2Profile.userId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.language, user2Profile.language);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.handicap, user2Profile.handicap);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.roles.sort(), user2Profile.roles.sort());
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.emails, [user2Profile.emails[0].email]);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.phones, [user2Profile.phones[0].phone]);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send a new toke having same user id, nothing changes 
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(user2TokenNewer);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(getProfileStub.callCount, 3);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.userId, user2Profile.userId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.language, user2Profile.language);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.handicap, user2Profile.handicap);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.roles.sort(), user2Profile.roles.sort());
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.emails, [user2Profile.emails[0].email]);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.phones, [user2Profile.phones[0].phone]);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            
        ], function (err) {
            client.disconnect(1000, '', true);
            return done(err);
        });
        
    });
    
    it('token invalidation', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        var userPayload = {
            userId: '312123',
            iat: Date.now()-10000
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        sinonSandbox = sinon.sandbox.create();
        
        var userProfile1 = {
            userId: userPayload.userId,
            roles: ['EXTERNAL', 'TENANT_1_ADMIN'],
            language: 'en',
            handicap: 1.2,
            emails: [
                {email: 'www@gmail.com', verificationStatus: 'verified'},
                {email: 'www2@gmail.com', verificationStatus: 'notVerified'}
            ],
            phones: [
                {phone: '11-22', verificationStatus: 'verified'},
                {phone: '33-44', verificationStatus: 'notVerified'}
            ]
        };
        var userProfile2 = {
            userId: userPayload.userId,
            roles: ['EXTERNAL', 'TENANT_1_ADMIN', 'INTERNAL'],
            language: 'de',
            handicap: 23,
            emails: [
                {email: 'www@gmail.com', verificationStatus: 'verified'},
                {email: 'www2@gmail.com', verificationStatus: 'notVerified'}
            ],
            phones: [
                {phone: '11-22', verificationStatus: 'verified'},
                {phone: '33-44', verificationStatus: 'notVerified'}
            ]
        };
        
        var pingSpy = sinonSandbox.spy(FakeQuestionService.prototype.messageHandlers, "ping");
        var getProfileStub = null;
        async.series([
            function (next) {
                client.connect(next);
            },
            
            
            function (next) {
                getProfileStub = sinonSandbox.stub(FakeProfileService.prototype.messageHandlers, 'getProfile', function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    
                    response.setContent({
                        profile: userProfile1
                    });
                    clientSession.sendMessage(response);
                });
                
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(userToken);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(getProfileStub.callCount, 1);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.userId, userProfile1.userId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.language, userProfile1.language);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.handicap, userProfile1.handicap);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.roles.sort(), userProfile1.roles.sort());
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.emails, [userProfile1.emails[0].email]);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.phones, [userProfile1.phones[0].phone]);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // invalidate token
            function (next) {
                getProfileStub.restore();
                getProfileStub = sinonSandbox.stub(FakeProfileService.prototype.messageHandlers, 'getProfile', function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        profile: userProfile2
                    });
                    clientSession.sendMessage(response);
                });
                
                
                gateway.messageDispatcher._onTokenInvalidated({
                    token: userToken
                });
                
                setTimeout(next, 500);
            },
            
            // send a second message without token. clientInfo not changed
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(getProfileStub.callCount, 1);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.userId, userProfile2.userId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.language, userProfile2.language);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.handicap, userProfile2.handicap);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.roles.sort(), userProfile2.roles.sort());
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.emails, [userProfile2.emails[0].email]);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.phones, [userProfile2.phones[0].phone]);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            
        ], function (err) {
            client.disconnect(1000, '', true);
            return done(err);
        });
        
    });
    
    it('test getAppConfiguration', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        var appConfig = {
            appId: 'appid',
            deviceUUID: 'deviceUUID',
            tenantId: '12345'
        };
        
        var userPayload = {
            userId: '312123',
            iat: Date.now()-10000
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        sinonSandbox = sinon.sandbox.create();
        
        var pingSpy = sinonSandbox.spy(FakeQuestionService.prototype.messageHandlers, "ping");
        var getAppConfigSpy = sinonSandbox.spy(FakeProfileService.prototype.messageHandlers, "getAppConfiguration");
        async.series([
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(userToken);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.appId, MessageDispatcher.defaultAppConfig.appId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.tenantId, MessageDispatcher.defaultAppConfig.tenantId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.deviceUUID, MessageDispatcher.defaultAppConfig.deviceUUID);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('profile/getAppConfiguration');
                m.setSeq(1);
                m.setContent(appConfig);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(getAppConfigSpy.lastCall.args[0].getClientInfo().appConfig.appId, appConfig.appId);
                        assert.strictEqual(getAppConfigSpy.lastCall.args[0].getClientInfo().appConfig.tenantId, appConfig.tenantId);
                        assert.strictEqual(getAppConfigSpy.lastCall.args[0].getClientInfo().appConfig.deviceUUID, appConfig.deviceUUID);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // make a new call nothing changes on appConfig
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(userToken);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.appId, appConfig.appId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.tenantId, appConfig.tenantId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.deviceUUID, appConfig.deviceUUID);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            
        ], function (err) {
            client.disconnect(1000, '', true);
            return done(err);
        });
        
    });
    
    it('test default app config', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        var appConfig = {
            appId: 'appid',
            deviceUUID: 'deviceUUID',
            tenantId: '12345'
        };
        
        var userPayload = {
            userId: '312123',
            iat: Date.now()-10000
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        sinonSandbox = sinon.sandbox.create();
        
        var pingSpy = sinonSandbox.spy(FakeQuestionService.prototype.messageHandlers, "ping");
        var getAppConfigSpy = sinonSandbox.spy(FakeProfileService.prototype.messageHandlers, "getAppConfiguration");
        async.series([
            function (next) {
                client.connect(next);
            },
            
            // only default app config
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(userToken);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.appId, MessageDispatcher.defaultAppConfig.appId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.tenantId, MessageDispatcher.defaultAppConfig.tenantId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.deviceUUID, MessageDispatcher.defaultAppConfig.deviceUUID);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('profile/getAppConfiguration');
                m.setSeq(1);
                m.setContent(appConfig);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(getAppConfigSpy.lastCall.args[0].getClientInfo().appConfig.appId, appConfig.appId);
                        assert.strictEqual(getAppConfigSpy.lastCall.args[0].getClientInfo().appConfig.tenantId, appConfig.tenantId);
                        assert.strictEqual(getAppConfigSpy.lastCall.args[0].getClientInfo().appConfig.deviceUUID, appConfig.deviceUUID);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(userToken);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.appId, appConfig.appId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.tenantId, appConfig.tenantId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().appConfig.deviceUUID, appConfig.deviceUUID);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            
        ], function (err) {
            client.disconnect(1000, '', true);
            return done(err);
        });
        
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