var Config = require('../../config/config.js');
var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4001;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';
process.env.REGISTRY_SERVICE_URIS = registryServiceURIs;

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeRegistryServiceClient = require('../classes/FakeRegistryServiceClient/FakeRegistryServiceClient.js');
var DefaultService = require('nodejs-default-service');
var DefaultClient = require('../../classes/Client');
var logger = require('nodejs-logger')();
var fs = require('fs');
var assert = require('chai').assert;
var ProtocolMessage = require('nodejs-protocol');
var Errors = require('nodejs-errors');
var helperCreateService = require('../helpers/create-service.js');

//var serviceRegistryInstancesConfig = [];
var instancesConfig = [];

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
});

// other instances
instancesConfig.push({
    ip: ip,
    port: port,
    secure: true,
    serviceName: 'testService',
    registryServiceURIs: registryServiceURIs,
    
    class: DefaultService
});
instancesConfig.push({
    ip: ip,
    port: port+1,
    secure: true,
    serviceName: 'testService',
    registryServiceURIs: registryServiceURIs,
    
    class: DefaultService
});

for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8');
    //instancesConfig[i].checkCert = false;
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage = false;
}



var serviceInstances = [];
var serviceClient;
var ipLookup;
var fakeSRC = new FakeRegistryServiceClient();

var instancesToKill = [];
var workersToKill = [];
var clientsToKill = [];
var serviceRegistryInstance;

describe('Tests secure', function() {
    this.timeout(20000);
    
    it('create service instances', function (done) {
        
        function createInstances(configs, cb) {
            async.mapSeries(configs, function (config, cbItem) {
                var cls = config.class;
                delete config.class;
                var inst = new cls(config);
                inst.build(function (err) {
                    logger.debug('build err:', err);
                    config.instance = inst;
                    //if (config.serviceName !== 'serviceRegistry') {
                        instancesToKill.push(inst);
                    //}
                    
                    cbItem(err);
                });
            }, cb);
        };
        createInstances(instancesConfig, function (err) {
            setTimeout(function () {
                done(err);
            }, 1000);
        });
    });
    /*
    it('connect by ip/port succes', function (done) {
        var serviceClient = new DefaultClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            validateFullMessage: false
        });
        
        clientsToKill.push(serviceClient);
        
        serviceClient.connect(function (err) {
            //console.log('\n\n\n\n\n>>>>>>>here', err);
            return done(err);
        });
    });
    */
    /*
    it('connect by namespace succes', function (done) {
        var serviceClient = new DefaultClient({
            serviceNamespace: 'testService',
            validateFullMessage: false,
            serviceRegistryClient: fakeSRC
        });
        
        clientsToKill.push(serviceClient);
        
        serviceClient.connect(done);
    });
    */
    it('connect by ip/port error, wrong certificate', function (done) {
        var serviceClient = new DefaultClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            validateFullMessage: false,
            sslKey: Config.sslKey.substring(0, 50) + 'asdasad' + Config.sslKey.substring(50),
            sslCert: Config.sslCert.substring(0, 50) + 'asdasad' + Config.sslCert.substring(50)
        });
        
        clientsToKill.push(serviceClient);
        
        serviceClient.connect(function (err) {
            try {
                assert.instanceOf(err, Error);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    /*
    
    it('connect by ip/port error, wrong fingerprint', function (done) {
        var serviceClient = new DefaultClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            validateFullMessage: false,
            sslKey: Config.sslKey,
            sslCert: Config.sslCert,
            sslFingerprints: ['asdaasdasd']
        });
        
        clientsToKill.push(serviceClient);
        
        serviceClient.connect(function (err) {
            try {
                assert.instanceOf(err, Error);
                assert.strictEqual(err.message, Errors.ERR_VALIDATION_FAILED.message);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    */
    after(function (done) {
        fakeSRC.disconnect(function () {
            helperCreateService.kill(clientsToKill, instancesToKill, workersToKill);
            setTimeout(done, 500);
        });
    });
});

function _sendMessageResponseSuccess (args, cb) {
    try {
        var message = args.message;
        var client = args.client;
        
        client.sendMessage(message, function (err, response) {
            try {
                if (message.getSeq() === null) {
                    assert.ifError(err);
                    return cb(false, response);
                } else {
                    assert.ifError(err);
                    assert.isNull(response.getError());
                    assert.isNotNull(response.getAck());
                    assert.strictEqual(response.getAck()[0], message.getSeq());
                    assert.strictEqual(response.getMessage(), message.getMessage() + 'Response');
                    return cb(false, response);
                }
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

function _sendMessageResponseError (args, cb) {
    try {
        var message = args.message;
        var client = args.client;
        var errorMessage = args.errorMessage;
        
        client.sendMessage(message, function (err, response) {
            try {
                assert.ifError(err);
                assert.isNotNull(response.getError());
                assert.isNotNull(response.getAck());
                assert.strictEqual(response.getAck()[0], message.getSeq());
                assert.strictEqual(response.getError().message, errorMessage);
                return cb(false, response);
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

function _createInstance(config, cb) {
    var cls = config.class;
    var inst = new cls(config);
    inst.build(function (err) {
        cb(err, inst);
    });
};
