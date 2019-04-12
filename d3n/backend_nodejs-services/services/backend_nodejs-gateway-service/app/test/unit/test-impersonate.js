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



describe('TEST IMPERSONATE', function() {
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
    

    it('impersonate token', function (done) {
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
        var user1PayloadImpersonate = {
            userId: '312123',
            iat: Date.now()+10000,
            tenantId: '1',
            impersonate: true
        };
        
        var user1PayloadImpersonateNoTenant = {
            userId: '312123',
            iat: Date.now()+20000,
            impersonate: true
        };
        
        var user1Token = jwtModule.sign(user1Payload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var user1TokenImpersonate = jwtModule.sign(user1PayloadImpersonate, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var user1TokenImpersonateNoTenant = jwtModule.sign(user1PayloadImpersonateNoTenant, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        sinonSandbox = sinon.sandbox.create();
        
        
        var user1Profile = {
            userId: user1Payload.userId,
            roles: ['EXTERNAL', 'TENANT_1_ADMIN', 'TENANT_2_ADMIN'],
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
        
        var pingSpy = sinonSandbox.spy(FakeQuestionService.prototype.messageHandlers, "ping");
        var getProfileStub = sinonSandbox.stub(FakeProfileService.prototype.messageHandlers, 'getProfile', function (message, clientSession) {
            
            var response = new ProtocolMessage(message);
            var profile;
            if (message.getContent().userId === user1Payload.userId) {
                profile = user1Profile;
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
            
            // send impersonate token, only global and tenant specific roles will be kept
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(user1TokenImpersonate);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(getProfileStub.callCount, 2);
                        
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.userId, user1Profile.userId);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.language, user1Profile.language);
                        assert.strictEqual(pingSpy.lastCall.args[0].getClientInfo().profile.handicap, user1Profile.handicap);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.roles.sort(), ['EXTERNAL', 'TENANT_1_ADMIN'].sort());
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.emails, [user1Profile.emails[0].email]);
                        assert.deepEqual(pingSpy.lastCall.args[0].getClientInfo().profile.phones, [user1Profile.phones[0].phone]);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // impersonate token no tenant
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(user1TokenImpersonateNoTenant);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.deepEqual(response.getError(), Errors.ERR_VALIDATION_FAILED);
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