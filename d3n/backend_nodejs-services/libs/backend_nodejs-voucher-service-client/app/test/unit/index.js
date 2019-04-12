//process.env.VALIDATE_FULL_MESSAGE='false';
var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeVoucherService = require('../classes/FakeVoucherService.js');
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var VoucherServiceClient = require('../../../index.js');
var assert = require('chai').assert;
var fs = require('fs');
var Constants = require('../../../app/config/constants.js');
var Errors = require('../../../app/config/errors.js');
var sinon = require('sinon');

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
    serviceName: Constants.SERVICE_NAME,
    registryServiceURIs: registryServiceURIs,
    
    class: FakeVoucherService
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

var vClient = null;
var vService = null;

var serviceInstances = [];

var testDataReserveSuccess = [
    {
        voucherId: '123'
    }
];

var testDataReserveError = [
    null // params missing
];


var testDataReleaseSuccess = [
    {
        voucherId: '123'
    }
];

var testDataReleaseError = [
    null // params missing
];

function buildResponse(item) {
    var result = {
        voucherId: item.voucherId,
    };
    return result;
}

describe('TEST VOUCHER CLIENT', function() {
    this.timeout(20000);
    
    before(function (done) {
        return async.mapSeries(instancesConfig, function (config, cbItem) {
            var cls = config.class;
            delete config.class;
            var inst = new cls(config);
            if (config.serviceName === Constants.SERVICE_NAME) {
                vService = inst;
            }
            return inst.build(function (err) {
                logger.debug('build err:', err);
                return cbItem(err);
            });
        }, function (err) {
            if (err) {
                return done(err);
            }
            vClient = new VoucherServiceClient({
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
    
    it('userVoucherReserve success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var vscReserveSpy = this.sinonSandbox.spy(vClient, "userVoucherReserve");
        var vsReserveSpy = this.sinonSandbox.spy(vService.messageHandlers, "userVoucherReserve");
        async.mapSeries(testDataReserveSuccess, function (item, cbItem) {
            vscReserveSpy.reset();
            vsReserveSpy.reset();
            return vClient.userVoucherReserve(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(vscReserveSpy.callCount, 1);
                            assert.strictEqual(vsReserveSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(vsReserveSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(vsReserveSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
                            }
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });

    it('userVoucherReserve validation error', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var vscReserveSpy = this.sinonSandbox.spy(vClient, "userVoucherReserve");
        var vsReserveSpy = this.sinonSandbox.spy(vService.messageHandlers, "userVoucherReserve");
        async.mapSeries(testDataReserveError, function (item, cbItem) {
            vscReserveSpy.reset();
            vsReserveSpy.reset();
            return vClient.userVoucherReserve(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(vscReserveSpy.callCount, 1);
                            assert.strictEqual(vsReserveSpy.callCount, 0);
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });
    
    it('userVoucherReserve success, do not wait for response', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var vscReserveSpy = this.sinonSandbox.spy(vClient, "userVoucherReserve");
        var vsReserveSpy = this.sinonSandbox.spy(vService.messageHandlers, "userVoucherReserve");
        async.mapSeries(testDataReserveSuccess, function (item, cbItem) {
            vscReserveSpy.reset();
            vsReserveSpy.reset();
            item.waitResponse = false;
            vClient.userVoucherReserve(item,
                function (err) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(vscReserveSpy.callCount, 1);
                            assert.strictEqual(vsReserveSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(vsReserveSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(vsReserveSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
                            }
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });
    
    it('userVoucherRelease success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var vscReleaseSpy = this.sinonSandbox.spy(vClient, "userVoucherRelease");
        var vsReleaseSpy = this.sinonSandbox.spy(vService.messageHandlers, "userVoucherRelease");
        async.mapSeries(testDataReleaseSuccess, function (item, cbItem) {
            vscReleaseSpy.reset();
            vsReleaseSpy.reset();
            vClient.userVoucherRelease(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(vscReleaseSpy.callCount, 1);
                            assert.strictEqual(vsReleaseSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(vsReleaseSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(vsReleaseSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
                            }
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });
    
    it('userVoucherRelease validation error', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var vscReleaseSpy = this.sinonSandbox.spy(vClient, "userVoucherRelease");
        var vsReleaseSpy = this.sinonSandbox.spy(vService.messageHandlers, "userVoucherRelease");
        async.mapSeries(testDataReleaseError, function (item, cbItem) {
            vscReleaseSpy.reset();
            vsReleaseSpy.reset();
            vClient.userVoucherRelease(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(vscReleaseSpy.callCount, 1);
                            assert.strictEqual(vsReleaseSpy.callCount, 0);
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });

    it('userVoucherRelease success, do not wait for response', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var vscReleaseSpy = this.sinonSandbox.spy(vClient, "userVoucherRelease");
        var vsReleaseSpy = this.sinonSandbox.spy(vService.messageHandlers, "userVoucherRelease");
        async.mapSeries(testDataReleaseSuccess, function (item, cbItem) {
            vscReleaseSpy.reset();
            vsReleaseSpy.reset();
            item.waitResponse = false;
            vClient.userVoucherRelease(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(vscReleaseSpy.callCount, 1);
                            assert.strictEqual(vsReleaseSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(vsReleaseSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(vsReleaseSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
                            }
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });
    
    it('userVoucherReserve error, no callback provided', function () {
        var res = vClient.userVoucherReserve(testDataReserveSuccess[0]);
        assert.instanceOf(res, Error);
        assert.strictEqual(res.message, Errors.ERR_CALLBACK_NOT_PROVIDED.message);
    });
    
    it('userVoucherRelease error, no callback provided', function () {
        var res = vClient.userVoucherRelease(testDataReleaseSuccess[0]);
        assert.instanceOf(res, Error);
        assert.strictEqual(res.message, Errors.ERR_CALLBACK_NOT_PROVIDED.message);
    });
    
});