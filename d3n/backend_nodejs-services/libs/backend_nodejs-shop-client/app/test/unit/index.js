//process.env.VALIDATE_FULL_MESSAGE='false';
var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeShopService = require('../classes/FakeShopService.js');
var logger = require('nodejs-logger')();
var ShopClient = require('../../../index.js');
var assert = require('chai').assert;
var fs = require('fs');
var Errors = require('../../../app/config/errors.js');
var sinon = require('sinon');

var instancesConfig = [];

var shopClient = null;
var shopService = null;

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
    serviceName: 'shop',
    registryServiceURIs: registryServiceURIs,
    
    class: FakeShopService
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

var serviceInstances = [];

var testTenant = {
    id: 1,
    title: 'Tenant 1',
    description: 'Tenant one description',
    iconUrl: 'https://f4m.com/logo.png',
    termsUrl: 'https://f4m.com',
    street: 'Circumvalatiunii',
    streetNumber: '11 A',
    city: 'Timisoara',
    country: 'RO',
    zip: '1234',
    phone: '1234567890',
    email: 'test@ascendro.de',
    taxNumber: '1234'
};

function buildResponse(item, create) {
    var result = {
        id: create? '' : item.id + ''
    };
    if (item.title) {
        result.title = item.title;
    }
    if (item.description) {
        result.description = item.description;
    }
    if (item.iconURL) {
        result.iconURL = item.iconURL;
    }
    if (item.termsURL) {
        result.termsURL = item.termsURL;
    }
    if (item.street) {
        result.street = item.street;
    }
    if (item.streetNumber) {
        result.streetNumber = item.streetNumber;
    }
    if (item.city) {
        result.city = item.city;
    }
    if (item.country) {
        result.country = item.country;
    }
    if (item.zip) {
        result.zip = item.zip;
    }
    if (item.phone) {
        result.phone = item.phone;
    }
    if (item.email) {
        result.email = item.email;
    }
    if (item.taxNumber) {
        result.taxNumber = item.taxNumber;
    }
    return result;
}

describe('Create/update tenant in shop', function() {
    this.timeout(20000);
    
    before(function (done) {
        return async.mapSeries(instancesConfig, function (config, cbItem) {
            var cls = config.class;
            var inst = new cls(config);
            if (config.serviceName === 'shop') {
                shopService = inst;
            }
            return inst.build(function (err) {
                logger.debug('build err:', err);
                return cbItem(err);
            });
        }, function (err) {
            if (err) {
                return done(err);
            }
            shopClient = new ShopClient({
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
    
    it('tenantAdd success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var scTenantAddSpy = this.sinonSandbox.spy(shopClient, "tenantAdd");
        var ssTenantAddSpy = this.sinonSandbox.spy(shopService.messageHandlers, "tenantAdd");

        scTenantAddSpy.reset();
        ssTenantAddSpy.reset();

        shopClient.tenantAdd(testTenant, function (err, response) {
            try {
                assert.strictEqual(scTenantAddSpy.callCount, 1);
                assert.strictEqual(ssTenantAddSpy.callCount, 1);
                assert.deepEqual(_.sortBy(ssTenantAddSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(testTenant, true)));
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('tenantAdd error, no callback provided', function () {
        var res = shopClient.tenantAdd(testTenant);
        assert.instanceOf(res, Error);
        assert.strictEqual(res.message, Errors.ERR_CALLBACK_NOT_PROVIDED.message);
    });
    
    it('tenantUpdate success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var scTenantUpdateSpy = this.sinonSandbox.spy(shopClient, "tenantUpdate");
        var ssTenantUpdateSpy = this.sinonSandbox.spy(shopService.messageHandlers, "tenantUpdate");

        scTenantUpdateSpy.reset();
        ssTenantUpdateSpy.reset();

        shopClient.tenantUpdate(testTenant, function (err, response) {
            try {
                assert.strictEqual(scTenantUpdateSpy.callCount, 1);
                assert.strictEqual(ssTenantUpdateSpy.callCount, 1);
                assert.deepEqual(_.sortBy(ssTenantUpdateSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(testTenant)));
                if (testTenant.clientInfo) {
                    assert.deepEqual(ssTenantUpdateSpy.getCall(0).args[0].getClientInfo(), testTenant.clientInfo);
                }
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('tenantUpdate validation error', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var scTenantUpdateSpy = this.sinonSandbox.spy(shopClient, "tenantUpdate");
        var ssTenantUpdateSpy = this.sinonSandbox.spy(shopService.messageHandlers, "tenantUpdate");

        scTenantUpdateSpy.reset();
        ssTenantUpdateSpy.reset();

        var invalidTenant = _.cloneDeep(testTenant);
        delete invalidTenant.id;

        shopClient.tenantUpdate(invalidTenant, function (err, response) {
            try {
                assert.strictEqual(scTenantUpdateSpy.callCount, 1);
                assert.strictEqual(ssTenantUpdateSpy.callCount, 0);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('tenantUpdate error, no callback provided', function () {
        var res = shopClient.tenantUpdate(testTenant);
        assert.instanceOf(res, Error);
        assert.strictEqual(res.message, Errors.ERR_CALLBACK_NOT_PROVIDED.message);
    });
});