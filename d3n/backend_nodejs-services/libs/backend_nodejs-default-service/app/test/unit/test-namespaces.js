var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
port = port+5;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:' + port;

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('./classes/FakeRegistryService.js');
var DefaultService = require('../../classes/Service.js');
var logger = require('nodejs-logger')();
var assert = require('assert');
var fs = require('fs');
var ServiceRegistryClient = require('nodejs-service-registry-client');

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
describe('TEST SERVICE NAMESPACES REGISTRATION', function() {
    this.timeout(20000);

    it('create service instances', function (done) {

        function createInstances(configs, cb) {
            async.mapSeries(configs, function (config, cbItem) {
                var cls = config.class;
                delete config.class;
                var inst = new cls(config);

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
            }, 1500);
        });
    });

    it('registration ok', function (done) {
        serviceRegistryClient.list(null, null, function (err, message) {
            try {
                //console.log('>>>>here:', err, message);
                assert.ifError(err);
                assert.strictEqual(message.getError(), null);

                var expectedServices = [
                    {
                        serviceName: 'mediaservice',
                        serviceNamespaces: ['mediaservice'],
                        uri: 'wss://' + ip + ':' + (port+1)
                    },
                    {
                        serviceName: 'question',
                        serviceNamespaces: ['question', 'workorder', 'application'],
                        uri: 'wss://' + ip + ':' + (port+2)
                    }
                ];
                var actualNamespaces = message.getContent().services;

                assert.deepStrictEqual(actualNamespaces, expectedServices);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    after(function(done) {

        serviceRegistryClient.disconnect();
        for(var i=0; i<serviceInstancesCreated.length; i++) {
            try {
                serviceInstancesCreated[i].shutdown(false);
            } catch (e) {}
        }

        return setTimeout(done, 500);
    });

});
