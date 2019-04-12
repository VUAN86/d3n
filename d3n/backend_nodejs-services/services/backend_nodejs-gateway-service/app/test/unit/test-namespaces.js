var config = require('../../config/config.js');
var ip =  config.gateway.ip;
var port = config.gateway.port + config.incrementPort();
//port = port-5;
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
//var serviceRegistryClient = new ServiceRegistryClient(serviceRegsitryClientConfig);

//var gatewayClie

var clients = [];
var instances = [];
var gateway = null;
describe('TEST SERVICE NAMESPACES REGISTRATION', function() {
    this.timeout(20000);
    
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
    
    it('connect to gateway, send messages success', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        function send_questionGet(cb) {
            var m = new ProtocolMessage();
            m.setMessage('question/questionGet');
            m.setSeq(1);
            
            client.sendMessage(m, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.strictEqual(response.getError(), null);
                    assert.strictEqual(response.getMessage(), 'question/questionGetResponse');
                    assert.strictEqual(response.getContent(), 'questionGet');
                    return cb(false);
                } catch (e) {
                    return cb(e);
                }
            });
        };
        
        
        function send_workorderGet(cb) {
            var m = new ProtocolMessage();
            m.setMessage('workorder/workorderGet');
            m.setSeq(2);
            
            client.sendMessage(m, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.strictEqual(response.getError(), null);
                    assert.strictEqual(response.getMessage(), 'workorder/workorderGetResponse');
                    assert.strictEqual(response.getContent(), 'workorderGet');
                    return cb(false);
                } catch (e) {
                    return cb(e);
                }
            });
        };
        
        function send_mediaservicePing(cb) {
            var m = new ProtocolMessage();
            m.setMessage('mediaservice/ping');
            m.setSeq(3);
            
            client.sendMessage(m, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.strictEqual(response.getError(), null);
                    assert.strictEqual(response.getMessage(), 'mediaservice/pingResponse');
                    return cb(false);
                } catch (e) {
                    return cb(e);
                }
            });
        };
        
        client.connect(function (err) {
            try {
                assert.ifError(err);
                
                async.series([
                    function (next) {
                        send_questionGet(next);
                    },
                    
                    function (next) {
                        send_workorderGet(next);
                    },
                    
                    function (next) {
                        send_mediaservicePing(next);
                    }
                ], done);
            } catch (e) {
                done(e);
            }
        });
        
    });
    
    it('connect to gateway, send message to unknown namespace', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        client.connect(function (err) {
            try {
                assert.ifError(err);
                
                var m = new ProtocolMessage();
                m.setMessage('qwqwqw/ping');
                m.setSeq(123);
                
                
                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        
                        assert.deepEqual(response.getError(), Errors.ERR_SERVICE_NOT_FOUND);
                        return done();
                    } catch (e) {
                        return done(e);
                    }
                });
                
            } catch (e) {
                done(e);
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
        
        for(var i=0; i<instances.length; i++) {
            try {
                instances[i].shutdown();
            } catch (e) {}
        }
        
        //setTimeout(done, 2000);
        setImmediate(done);
    });
});