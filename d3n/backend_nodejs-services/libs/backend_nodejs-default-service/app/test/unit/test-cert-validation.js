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
describe('TEST CERT VALIDATION', function() {
    this.timeout(20000);
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });

    it('create service instances', function (done) {
        process.env.CHECK_CERT='true';
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
    it('origin set => reject', function (done) {
        var qsPort = port+2;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: qsPort
            },
            headers: {
                service_name: 'clientService',
                origin: 'theorigin'
            },
            validateFullMessage: false
        });
        clients.push(client);
        
        async.series([
            function (next) {
                client.connect(function (err) {
                    try {
                        assert.isOk(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                    
                });
            }
        ], done);
    });

    it('origin not set => connect success', function (done) {
        var qsPort = port+2;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: qsPort
            },
            headers: {
                service_name: 'clientService'
            },
            validateFullMessage: false
        });
        
        clients.push(client);
        
        async.series([
            function (next) {
                client.connect(next);
            }
            
        ], done);
    });
    
    it('wrong certificate => reject', function (done) {
        var qsPort = port+2;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: qsPort
            },
            headers: {
                service_name: 'clientService'
            },
            validateFullMessage: false
        });
        
        client.config.cert = fs.readFileSync(__dirname + '/ssl-certificate/cert-not-trusted.pem', 'utf8');
        
        clients.push(client);
        
        async.series([
            function (next) {
                client.connect(function (err) {
                    try {
                        assert.isOk(err);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                    
                });
            }
            
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
