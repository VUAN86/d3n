var config = require('../../config/config.js');
var ip =  config.gateway.ip;
var port = config.gateway.port + config.incrementPort();
//port = port-500;
var secure = true;
var registryServiceURIs = config.registryServiceURIsNode;

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('./classes/FakeRegistryService.js');
var FakeEventService = require('./classes/FakeEventService.js');
var FakeQuestionService = require('./classes/FakeQuestionService.js');
var DefaultService = require('nodejs-default-service');
var ServiceClient = require('nodejs-default-client');
var Gateway = require('../../classes/Gateway.js');
var logger = require('nodejs-logger')();
var assert = require('assert');
var fs = require('fs');
var ServiceRegistryClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');
var Errors = require('nodejs-errors');

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
    serviceName: 'mediaservice2',
    registryServiceURIs: registryServiceURIs,
    
    class: DefaultService
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
var gateway2 = null;
describe('TEST RECONNECT', function() {
    this.timeout(20000);
        
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
    
    it('connect to gateway, send messages success', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        function send_mediaservicePing(cb) {
            var m = new ProtocolMessage();
            m.setMessage('mediaservice/ping');
            m.setSeq(3);
            
            client.sendMessage(m, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.strictEqual(response.getError(), null);
                    assert.strictEqual(response.getMessage(), 'mediaservice/pingResponse');
                    return cb();
                } catch (e) {
                    return cb(e);
                }
            });
        };
        
        client.connect(function (err) {
            try {
                assert.ifError(err);
                send_mediaservicePing(done);
            } catch (e) {
                done(e);
            }
        });
        
    });
    
    it('reconnect success', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        client.on('message', function (message) {
            if (message.getMessage() === 'gateway/instanceDisconnect') {
                return done(new Error('gateway/instanceDisconnect emitted'));
            }
        });
        
        
        async.series([
            function (next) {
                client.connect(next);
            },
            // send message success
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('mediaservice/ping');
                m.setSeq(3);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'mediaservice/pingResponse');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            
            // restart media service
            function (next) {
                try {
                    mediaServiceInstance.shutdown();

                    setTimeout(function () {
                        var inst = new DefaultService(mediaServiceConfig);
                        instances.push(inst);
                        inst.build(function (err) {
                            logger.debug('media service build err:', err);
                            setTimeout(next, 1500, err);
                        });
                    }, 1500);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // send message success
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('mediaservice/ping');
                m.setSeq(4);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'mediaservice/pingResponse');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            
        ], done);
        
    });
    
    it('reconnect fail', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        
        
        async.series([
            function (next) {
                client.connect(next);
            },
            
            // send message so gateway will register client
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('mediaservice2/ping');
                m.setSeq(3);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getMessage(), 'mediaservice2/pingResponse');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // disconnect media service2
            function (next) {
                try {
                    
                    client.on('message', function (message) {
                        try {
                            assert.strictEqual(message.getMessage(), 'gateway/instanceDisconnect');
                            done();
                        } catch (e) {
                            return done(e);
                        }
                    });
                    
                    mediaService2Instance.shutdown();
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
            
        ]);
        
    });
    
    it('test instance removed if conection fails', function (done) {
        gateway2 = new Gateway({
            gateway: {
                ip: ip,
                port: port-2,
                secure: secure,
                key: __dirname + '/ssl-certificate/key.pem',
                cert: __dirname + '/ssl-certificate/cert.pem',
                auth: config.gateway.auth
            },
            serviceInstances: {
                'question': [
                    {
                        ip: ip,
                        port: port+2,
                        secure: true
                    },
                    {
                        ip: ip,
                        port: port+32,
                        secure: true
                    }
                ]
            }
        });
        
        gateway2.build(function (err) {
            if (err) {
                return done(err);
            }
            
            try {
                assert.strictEqual(gateway2.messageDispatcher.serviceLoadBalancerManager.serviceLoadBalancers[0].instances.length, 1);
                return done();
            } catch (e) {
                return done(e);
            }
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
        try {
            gateway2.shutdown();
        } catch (e) {}
        
        for(var i=0; i<instances.length; i++) {
            try {
                instances[i].shutdown();
            } catch (e) {}
        }
        
        setImmediate(done);
    });
    
    
});