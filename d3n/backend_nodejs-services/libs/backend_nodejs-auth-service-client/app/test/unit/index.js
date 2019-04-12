process.env.VALIDATE_FULL_MESSAGE='false';
var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeAuthService = require('../classes/FakeAuthService.js');
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var AuthServiceClient = require('../../../index.js');
var assert = require('assert');
var fs = require('fs');

var instancesConfig = [];

var authClient;
var authService;

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
    serviceName: 'auth',
    registryServiceURIs: registryServiceURIs,

    class: FakeAuthService
});

for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8');
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage=false;
}

var serviceInstances = [];

describe('Auth', function() {
    this.timeout(20000);

    before(function (done) {
        return async.mapSeries(instancesConfig, function (config, cbItem) {
            var cls = config.class;
            delete config.class;
            var inst = new cls(config);
            if (config.serviceName === 'auth') {
                authService = inst;
            }
            return inst.build(function (err) {
                logger.debug('build err:', err);
                return cbItem(err);
            });
        }, function (err) {
            if (err) {
                return done(err);
            }
            authClient = new AuthServiceClient({
                registryServiceURIs: registryServiceURIs
            });
            return done();
        });
    });

    it('setUserRole', function (done) {
        authClient.setUserRole({
            userId: 'aaaa',
            rolesToAdd: ['TENANT_1_ADMIN'],
            rolesToRemove: ['TENANT_1_COMMUNITY']
        }, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.ifError(response);
                    done();
                } catch (e) {
                    done(e);
                }
            }
        );
    });

    it('inviteUserByEmailAndRole', function (done) {
        authClient.inviteUserByEmailAndRole({
            email: 'test@ascendro.de',
            tenantId: 'TENANT_1_COMMUNITY', 
            profileInfo: { firstName: 'Test', lastName: 'Account', organization: 'Ascendro' }
        }, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.ifError(response);
                    done();
                } catch (e) {
                    done(e);
                }
            }
        );
    });

    it('addConfirmedUser', function (done) {
        authClient.addConfirmedUser({
            userInfo: { email: 'test@ascendro.de', phone: '112121211', firstName: 'John', lastName: 'Doe' }
        }, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.ifError(response);
                    done();
                } catch (e) {
                    done(e);
                }
            }
        );
    });

    it('generateImpersonateToken', function (done) {
        authClient.generateImpersonateToken({
            email: 'test@ascendro.de', 
            tenantId: '1'
        }, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.ifError(response);
                    done();
                } catch (e) {
                    done(e);
                }
            }
        );
    });

});
