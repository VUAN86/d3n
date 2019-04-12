var config = require('../../config/config.js');
var Constants = require('../../config/constants.js');
var ip =  config.gateway.ip;
var port = config.gateway.port;
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
var dns = require('dns');
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
var questionService;
var userIds = ['disc-111-342-45435-asd-w23', 'disc-wer-345sdf-345-sdf-r'];

describe('TEST STATISTICS', function() {
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
    
    
    it('getUserMessageInstancesIps() called only once', function (done) {
        //return done();
        var gwport = port-1;
        var client1 = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: gwport
            }
        });
        var client2 = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: gwport
            }
        });
        clients.push(client1);
        clients.push(client2);
        
        sinonSandbox = sinon.sandbox.create();
        var getUserMessageInstancesIpsSpy = sinonSandbox.spy(gateway.messageDispatcher.serviceLoadBalancerManager, "getUserMessageInstancesIps");
        
        async.series([
            function (next) {
                client1.connect(next);
            },
            function (next) {
                client2.connect(next);
            },
            
            // send a message to create session
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('userMessage/ping');
                m.setSeq(3);
                m.setClientId('assadasdasd');
                
                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                        assert.strictEqual(getUserMessageInstancesIpsSpy.callCount, 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
                
            },
            // send a second message, result si taken from cache
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('userMessage/ping');
                m.setSeq(4);
                m.setClientId('assadasdasdutytyut24fgg');
                
                client1.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                        assert.strictEqual(getUserMessageInstancesIpsSpy.callCount, 1);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // send a message using cleint2, chack is performed
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('userMessage/ping');
                m.setSeq(3);
                m.setClientId('assadasdsad445asd');
                
                client2.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError().message, Errors.ERR_VALIDATION_FAILED.message);
                        assert.strictEqual(getUserMessageInstancesIpsSpy.callCount, 2);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], done);
    });
    
    it('pass validation', function (done) {
        var gwport = port-1;
        var clientHuman = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: gwport
            }
        });
        var clientUserMessageService = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: gwport
            }
        });
        clients.push(clientHuman);
        clients.push(clientUserMessageService);
        
        sinonSandbox = sinon.sandbox.create();
        var clientIdHuman;
        var clientIdUserMessageService;
        var ipOfIp;
        async.series([
            function (next) {
                dns.lookup(ip, function (err, _ip) {
                    ipOfIp = _ip;
                    return next(err);
                });
            },
            // disconnect existing gateway clients
            function (next) {
                for(var i=0; i<clients.length; i++) {
                    try {
                        clients[i].disconnect(1000, '', true);
                    } catch (e) {}
                }
                
                return setImmediate(next);
            },
            function (next) {
                clientHuman.connect(function (err) {
                    clientIdHuman =  _.keys(gateway.clientConnection)[0];
                    return next(err);
                });
            },
            function (next) {
                clientUserMessageService.connect(function (err) {
                    var ids = _.keys(gateway.clientConnection);
                    _.remove(ids, function (item) {
                        return item === clientIdHuman;
                    });
                    clientIdUserMessageService = ids[0];
                    return next(err);
                });
            },
            function (next) {
                sinonSandbox.stub(FakeRegistryService.prototype.messageHandlers, 'list', function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        services: [
                            {
                                serviceName: Constants.USER_MESSAGE_SERVICE_NAME,
                                serviceNamespaces: [],
                                uri: 'wss://' + ipOfIp + ':80'
                            }
                        ]
                    });
                    clientSession.sendMessage(response);
                });
                
                return setImmediate(next);
            },
            // send a message to itself basically
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('userMessage/ping');
                m.setSeq(3);
                m.setClientId(clientIdHuman);
                clientHuman.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // user message service send message to human client
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('userMessage/notification');
                m.setSeq(null);
                m.setContent({
                    notificationContent: 'asdadasda'
                });
                m.setClientId(clientIdHuman);
                
                clientHuman.on('message', function (mFromUserMessageService) {
                    try {
                        assert.deepEqual(mFromUserMessageService.getContent(), m.getContent());
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                    
                });
                clientUserMessageService.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
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