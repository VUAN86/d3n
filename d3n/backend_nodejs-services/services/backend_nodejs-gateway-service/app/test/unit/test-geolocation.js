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
var GeoLocation = require('../../classes/GeoLocation.js');


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


describe('TEST GEO LOCATION', function() {
    this.timeout(20000);
    
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });
    
    it('GeoLocation.getCountryCode()', function () {
        var geoLocation = new GeoLocation();
        assert.strictEqual(geoLocation.getCountryCode('5.2.185.247'), 'RO');
        assert.strictEqual(geoLocation.getCountryCode('5.185.247'), null); // unknown
        assert.strictEqual(geoLocation.getCountryCode('127.0.0.1'), null); // unknown
        
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
    
    it('call GeoLocation.getCountryCode() only once', function (done) {
        sinonSandbox = sinon.sandbox.create();
        var spy_getCountryCode = sinonSandbox.spy(gateway.messageDispatcher._geoLocation, "getCountryCode");
        var spy_ping = sinonSandbox.spy(FakeQuestionService.prototype.messageHandlers, 'ping');
        
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
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(null);
                m.setSeq(1);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(spy_getCountryCode.callCount, 1);
                        assert.deepEqual(spy_ping.lastCall.args[0].getClientInfo().countryCode, null);
                        
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
                
            },
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('question/ping');
                m.setToken(null);
                m.setSeq(2);

                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        
                        assert.strictEqual(spy_getCountryCode.callCount, 1);
                        assert.deepEqual(spy_ping.lastCall.args[0].getClientInfo().countryCode, null);
                        
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