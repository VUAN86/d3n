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

describe('TEST NEWRELIC', function() {
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
    
    
    it('metrics generated and sent', function (done) {
        var client = new ServiceClient({
            secure: secure,
            service: {
                ip: ip,
                port: port-1
            }
        });
        clients.push(client);
        
        //return done();
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        sinonSandbox = sinon.sandbox.create();
        var spyApiHandlerStart = sinonSandbox.spy(questionService._newrelicMetrics, "apiHandlerStart");
        var spyApiHandlerEnd = sinonSandbox.spy(questionService._newrelicMetrics, "apiHandlerEnd");
        var clientId = null;
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
            
            function (next) {
                
                async.map([12,54], function (seq, cbItem) {
                    var reqMessage = new ProtocolMessage();
                    reqMessage.setMessage('question/ping');
                    reqMessage.setToken(null);
                    reqMessage.setSeq(seq);
                    client.sendMessage(reqMessage, function (err, resMessage) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                            assert.isNull(resMessage.getError());
                            assert.strictEqual(spyApiHandlerStart.lastCall.args[0].getClientId(), clientId);
                            assert.strictEqual(spyApiHandlerEnd.lastCall.args[0].getClientId(), clientId);
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    });
                    
                }, next);
                
            },
            
            function (next) {
                try {
                    assert.strictEqual(spyApiHandlerStart.callCount, 2);
                    assert.strictEqual(spyApiHandlerEnd.callCount, 2);
                    return setImmediate(next);
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