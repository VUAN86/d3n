var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
port = port+15;
var secure = true;
var registryServiceURIs = 'wss://localhost:' + port;

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
var sinonSandbox = null;
var clients = [];
var serviceInstancesCreated = [];
describe('TEST SEND STATISTICS', function() {
    this.timeout(20000);
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });

    it('_sendStatistics', function (done) {
        
        var srService;
        var mediaService;
        async.series([
            // build SR
            function (next) {
                var hostConfig = nodeUrl.parse(registryServiceURIs.split(',')[0]);
                srService = new FakeRegistryService({
                    ip: hostConfig.hostname,
                    port: hostConfig.port,
                    secure: hostConfig.protocol === 'wss:',
                    serviceName: 'serviceRegistry',
                    registryServiceURIs: '',
                    
                    key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                    cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                    auth: {
                        algorithm: 'RS256',
                        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                    },
                    validateFullMessage: false,
                    validatePermissions: false
                });
                serviceInstancesCreated.push(srService);
                srService.build(next);
            },
            
            // build media service
            function (next) {
                mediaService = new DefaultService({
                    ip: ip,
                    port: port+1,
                    secure: true,
                    serviceName: 'media',
                    registryServiceURIs: registryServiceURIs.split(',')[0],
                    
                    key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                    cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                    auth: {
                        algorithm: 'RS256',
                        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                    },
                    validateFullMessage: false,
                    validatePermissions: false,
                    sendStatisticsInterval: 1000
                });
                serviceInstancesCreated.push(mediaService);
                mediaService.build(next);
            },
            
            function (next) {
                try {
                    sinonSandbox = sinon.sandbox.create();
                    var spyPushStatistics = sinonSandbox.spy(srService.messageHandlers, 'pushServiceStatistics');
                    spyPushStatistics.reset();
                    setTimeout(function () {
                        try {
                            assert.strictEqual(spyPushStatistics.callCount, 4);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }, 3100);
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
                
            }
            
        ], done);
    });

    after(function(done) {

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
