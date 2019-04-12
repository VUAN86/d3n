var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
port = port+11;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:' + port;

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('./classes/FakeRegistryService.js');
var DefaultService = require('../../classes/Service.js');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();
var sinon = require('sinon');
var assert = require('chai').assert;
var fs = require('fs');
var ServiceRegistryClient = require('nodejs-service-registry-client');
var ServiceClient = require('nodejs-default-client');
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
    serviceName: 'mediaservice',
    registryServiceURIs: registryServiceURIs,

    class: DefaultService
});
instancesConfig.push({
    ip: ip,
    port: port+2,
    secure: true,
    serviceName: 'question',
    serviceNamespaces: ['question', 'workorder', 'application'],
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
    instancesConfig[i].validatePermissions = false;
}

var serviceInstances = [];
var serviceRegistryClient = new ServiceRegistryClient(serviceRegsitryClientConfig);

var serviceInstancesCreated = [];
var sinonSandbox = null;
var questionService = null;
var questionServiceClient = new ServiceClient({
    secure: true,
    service: {
        ip: ip,
        port: port+2
    },
    validateFullMessage: false
});
var clients = [];
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
                
                if (config.serviceName === 'question') {
                    questionService = inst;
                }
                
                inst.build(function (err) {
                    logger.debug('build err:', err);
                    if(!err) {
                        serviceInstancesCreated.push(inst);
                    }
                    cbItem(err);
                });
            }, cb);
        };
        createInstances(instancesConfig, function (err) {
            // wait a bit for namespace registration
            setTimeout(function () {
                done(err);
            }, 500);
        });
    });
    it('should connect to question service', function (done) {
        this.timeout(10000);
        clients.push(questionServiceClient);
        questionServiceClient.connect(done);
    });

    it('metrics generated and sent', function (done) {
        //return done();
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        sinonSandbox = sinon.sandbox.create();
        var spyApiHandlerStart = sinonSandbox.spy(questionService._newrelicMetrics, "apiHandlerStart");
        var spyApiHandlerEnd = sinonSandbox.spy(questionService._newrelicMetrics, "apiHandlerEnd");
        
        async.series([
            function (next) {
                
                async.map([12,54], function (seq, cbItem) {
                    var reqMessage = new ProtocolMessage();
                    reqMessage.setMessage('question/ping');
                    reqMessage.setToken(null);
                    reqMessage.setSeq(seq);
                    reqMessage.setClientId('1111c');
                    questionServiceClient.sendMessage(reqMessage, function (err, resMessage) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                            assert.isNull(resMessage.getError());
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

    it('client id set proper', function (done) {
        //return done();
        var fake_serviceNewrelic = {
            recordMetric: function (metricName, value) {
                
            }
        };
        global.serviceNewrelic = fake_serviceNewrelic; 
        sinonSandbox = sinon.sandbox.create();
        var spyApiHandlerStart = sinonSandbox.spy(questionService._newrelicMetrics, "apiHandlerStart");
        var spyApiHandlerEnd = sinonSandbox.spy(questionService._newrelicMetrics, "apiHandlerEnd");
        
        var clientId = '11-22-asda-asd';        
        async.series([
            // use message client id
            function (next) {
                var reqMessage = new ProtocolMessage();
                reqMessage.setMessage('question/ping');
                reqMessage.setToken(null);
                reqMessage.setSeq(12);
                reqMessage.setClientId(clientId);
                questionServiceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(spyApiHandlerStart.lastCall.args[0].getClientId(), clientId);
                        assert.strictEqual(spyApiHandlerEnd.lastCall.args[0].getClientId(), clientId);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // use conection id
            function (next) {
                var reqMessage = new ProtocolMessage();
                reqMessage.setMessage('question/ping');
                reqMessage.setToken(null);
                reqMessage.setSeq(12);
                questionServiceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        var cid = spyApiHandlerStart.lastCall.args[0].getClientId();
                        assert.isString(cid);
                        assert.notEqual(cid, clientId);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
                
            },
        ], done);
    });
    
    after(function(done) {

        serviceRegistryClient.disconnect();
        for(var i=0; i<clients.length; i++) {
            try {
                clients[i].disconnect(1000, '', true);
            } catch (e) {}
        }
        
        for(var i=0; i<serviceInstancesCreated.length; i++) {
            try {
                serviceInstancesCreated[i].shutdown(false);
            } catch (e) {}
        }

        return setTimeout(done, 500);
    });

});
