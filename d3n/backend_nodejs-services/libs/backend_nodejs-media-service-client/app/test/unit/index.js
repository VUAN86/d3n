var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var _ = require('lodash');
var fs = require('fs');
var async = require('async');
var nodeUrl = require('url');
var Errors = require('../../config/errors.js');
var Constants = require('../../config/constants.js');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeMediaService = require('../classes/FakeMediaService.js');
var DefaultService = require('nodejs-default-service');
var MediaServiceClient = require('../../../index.js');
var assert = require('assert');
var logger = require('nodejs-logger')();

var instancesConfig = [];

var mediaClient;

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
    serviceName: Constants.SERVICE_NAME,
    registryServiceURIs: registryServiceURIs,

    class: FakeMediaService
});

for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8');
    instancesConfig[i].media = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage=false;
}

var serviceInstances = [];

describe('Media', function () {
    before(function (done) {
        async.mapSeries(instancesConfig, function (config, cbItem) {
            var cls = config.class;
            delete config.class;
            var inst = new cls(config);
            return inst.build(function (err) {
                logger.debug('build err:', err);
                return cbItem(err);
            });
        }, function (err) {
            if (err) {
                return done(err);
            }
            mediaClient = new MediaServiceClient({
                registryServiceURIs: registryServiceURIs
            });
            return done();
        });
    });

    it('updateProfilePicture - update', function (done) {
        mediaClient.updateProfilePicture({
            userId: '11-22-33',
            action: 'update',
            data: 'dsljfdslvvcxvncxvnx,cf',
            waitResponse: false,
        }, function (err, response) {
            try {
                assert.ifError(err);
                assert.ifError(response);
                done();
            } catch (e) {
                done(e);
            }
        });
    });

    it('updateProfilePicture - delete', function (done) {
        mediaClient.updateProfilePicture({
            userId: '11-22-33',
            action: 'delete',
            waitResponse: false,
        }, function (err, response) {
            try {
                assert.ifError(err);
                assert.ifError(response);
                done();
            } catch (e) {
                done(e);
            }
        });
    });
});
