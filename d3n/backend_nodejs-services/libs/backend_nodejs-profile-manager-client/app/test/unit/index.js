//process.env.VALIDATE_FULL_MESSAGE='false';
var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeProfileManager = require('../classes/FakeProfileManager.js');
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var ProfileManagerClient = require('../../../index.js');
var assert = require('chai').assert;
var fs = require('fs');
var Errors = require('../../../app/config/errors.js');
var Constants = require('../../../app/config/constants.js');
var sinon = require('sinon');
var sinonSandbox;
var ProtocolMessage = require('nodejs-protocol');

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
    port: port+1,
    secure: true,
    serviceName: Constants.SERVICE_NAME,
    registryServiceURIs: registryServiceURIs,
    
    class: FakeProfileManager
});

for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8');
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage=true;
}


var pmClient = null;
var pmService = null;
var concurrency = 50;

var serviceInstances = [];


var testAdminListByTenantIdSuccess = [
    {
        tenantId: '111'
    },
    {
        tenantId: '222'
    }
];

var testUserRoleListByTenantIdSuccess = [
    {
        profileId: '111',
        tenantId: '222'
    },
    {
        profileId: '333',
        tenantId: '444'
    }
];

describe('TEST PROFILE MANAGER CLIENT', function() {
    this.timeout(20000);
    
    before(function (done) {
        return async.mapSeries(instancesConfig, function (config, cbItem) {
            var cls = config.class;
            delete config.class;
            var inst = new cls(config);
            if (config.serviceName === Constants.SERVICE_NAME) {
                pmService = inst;
            }
            return inst.build(function (err) {
                logger.debug('build err:', err);
                return cbItem(err);
            });
        }, function (err) {
            if (err) {
                return done(err);
            }
            pmClient = new ProfileManagerClient({
                registryServiceURIs: registryServiceURIs
            });
            return done();
        });
    });
    
    afterEach(function () {
        if (this.sinonSandbox) {
            this.sinonSandbox.restore();
        }
    });
    
    it('adminListByTenantId success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var wsClientSpy = this.sinonSandbox.spy(pmClient, "adminListByTenantId");
        var wsServiceSpy = this.sinonSandbox.spy(pmService.messageHandlers, "adminListByTenantId");
        return async.mapSeries(testAdminListByTenantIdSuccess, function (item, cbItem) {
            wsClientSpy.reset();
            wsServiceSpy.reset();
            return pmClient.adminListByTenantId(
                item.tenantId,
                function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.strictEqual(wsClientSpy.callCount, 1);
                        assert.strictEqual(wsServiceSpy.callCount, 1);
                        //assert.deepEqual(wsClientSpy.getCall(0).args[0].getContent(), item);
                        return cbItem();
                    } catch (e) {
                        return cbItem(e);
                    }
                }
            );
        }, done);

    });
    
    it('userRoleListByTenantId success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var wsClientSpy = this.sinonSandbox.spy(pmClient, "userRoleListByTenantId");
        var wsServiceSpy = this.sinonSandbox.spy(pmService.messageHandlers, "userRoleListByTenantId");
        return async.mapSeries(testUserRoleListByTenantIdSuccess, function (item, cbItem) {
            wsClientSpy.reset();
            wsServiceSpy.reset();
            return pmClient.userRoleListByTenantId(
                item.profileId,
                item.tenantId,
                function (err, response) {
                    try {
                        assert.instanceOf(err, Error);
                        assert.isUndefined(response);
                        assert.strictEqual(wsClientSpy.callCount, 1);
                        assert.strictEqual(wsServiceSpy.callCount, 1);
                        //assert.deepEqual(wsClientSpy.getCall(0).args[0].getContent(), item);
                        return cbItem();
                    } catch (e) {
                        return cbItem(e);
                    }
                }
            );
        }, done);
    });
    
});