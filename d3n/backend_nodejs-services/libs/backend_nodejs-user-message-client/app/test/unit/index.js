//process.env.VALIDATE_FULL_MESSAGE='false';
var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:4000';

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var FakeUserMessageService = require('../classes/FakeUserMessageService.js');
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var UserMessageClient = require('../../../index.js');
var assert = require('chai').assert;
var fs = require('fs');
var Errors = require('../../../app/config/errors.js');
var sinon = require('sinon');

var instancesConfig = [];

var umClient = null;
var umService = null;

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
    serviceName: 'userMessage',
    registryServiceURIs: registryServiceURIs,
    
    class: FakeUserMessageService
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


var concurrency = 50;

var serviceInstances = [];

var testDataEmailSuccess = [
    {
        userId: '11-22-33',
        address: 'gabriel.dodan@gmail.com',
        subject: 'test subject',
        message: 'test message',
        language: 'en'
    },
    {
        userId: '11-22-33',
        subject: 'test subject',
        message: 'test message',
        language: 'de'
    },
    {
        userId: '11-22-33',
        address: 'gabriel.dodan@gmail.com',
        subject: 'test subject',
        message: 'test message',
        subject_parameters: ['a', 'b'],
        language: 'ro'
    },
    {
        userId: '11-22-33',
        address: 'gabriel.dodan@gmail.com',
        subject: 'test subject',
        message: 'test message',
        message_parameters: ['aa'],
        language: 'en'
    },
    {
        userId: '11-22-33',
        address: 'gabriel.dodan@gmail.com',
        subject: 'test subject',
        message: 'test message',
        message_parameters: ['aa'],
        language: 'en'
    },
    {
        userId: '11-22-33',
        address: 'gabriel.dodan@gmail.com',
        subject: 'test subject',
        subject_parameters: ['a', 'b'],
        message: 'test message',
        message_parameters: ['aa'],
        clientInfo: {
            appConfig: {
                tenantId: 1,
                appId: 1
            }
        }
    }
];

var testDataEmailError = [
    { // userId missing
        address: 'gabriel.dodan@gmail.com',
        message: 'test message',
        language: 'en'
    },
    { // subject missing
        userId: '11-22-33',
        address: 'gabriel.dodan@gmail.com',
        message: 'test message',
        language: 'en'
    },
    { // message missing
        userId: '11-22-33',
        subject: 'test subject',
        language: 'de'
    },
    { // wrong language
        userId: '11-22-33',
        subject: 'test subject',
        language: 'dewe'
    },
    null // params missing
];


var testDataSmsSuccess = [
    {
        userId: '11-22-33',
        phone: '+406752111222',
        message: 'test message',
        language: null
    },
    {
        userId: '11-22-33',
        message: 'test message',
        language: null
    },
    {
        userId: '11-22-33',
        phone: '+407252111222',
        message: 'test message',
        language: null
    },
    {
        userId: '11-22-33',
        phone: '+40752111222',
        message: 'test message',
        message_parameters: ['aa'],
        language: null
    },
    {
        userId: '11-22-33',
        phone: '+407522111222',
        message: 'test message',
        message_parameters: ['aa'],
        language: 'en'
    },
    {
        userId: '11-22-33',
        phone: '+407521114222',
        message: 'test message',
        message_parameters: ['aa'],
        language: 'en',
        clientInfo: {
            appConfig: {
                tenantId: 1,
                appId: 1
            }
        }
    }
];

var testDataSmsError = [
    { // userId missing
        phone: '+406752111222',
        language: null
    },
    { // userId missing
        language: null
    },
    null // params missing
];

function buildResponse(item) {
    var result = {
        userId: item.userId,
        message: item.message
    };
    if (item.phone) {
        result.phone = item.phone;
    }
    if (item.address) {
        result.address = item.address;
    }
    if (item.language) {
        result.language = item.language;
    } else {
        result.language = 'en';
    }
    if (item.subject) {
        result.subject = item.subject;
    }
    if (item.subject_parameters) {
        result.subject_parameters = item.subject_parameters;
    }
    if (item.message_parameters) {
        result.message_parameters = item.message_parameters;
    }
    return result;
}

describe('TEST USER MESSAGE SEND EMAIL', function() {
    this.timeout(20000);
    
    before(function (done) {
        return async.mapSeries(instancesConfig, function (config, cbItem) {
            var cls = config.class;
            delete config.class;
            var inst = new cls(config);
            if (config.serviceName === 'userMessage') {
                umService = inst;
            }
            return inst.build(function (err) {
                logger.debug('build err:', err);
                return cbItem(err);
            });
        }, function (err) {
            if (err) {
                return done(err);
            }
            umClient = new UserMessageClient({
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
    
    it('sendEmail success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var umcSendEmailSpy = this.sinonSandbox.spy(umClient, "sendEmail");
        var umsSendEmailSpy = this.sinonSandbox.spy(umService.messageHandlers, "sendEmail");
        async.mapSeries(testDataEmailSuccess, function (item, cbItem) {
            umcSendEmailSpy.reset();
            umsSendEmailSpy.reset();
            return umClient.sendEmail(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(umcSendEmailSpy.callCount, 1);
                            assert.strictEqual(umsSendEmailSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(umsSendEmailSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(umsSendEmailSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
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

    it('sendEmail validation error', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var umcSendEmailSpy = this.sinonSandbox.spy(umClient, "sendEmail");
        var umsSendEmailSpy = this.sinonSandbox.spy(umService.messageHandlers, "sendEmail");
        async.mapSeries(testDataEmailError, function (item, cbItem) {
            umcSendEmailSpy.reset();
            umsSendEmailSpy.reset();
            return umClient.sendEmail(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(umcSendEmailSpy.callCount, 1);
                            assert.strictEqual(umsSendEmailSpy.callCount, 0);
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });
    
    it('sendEmail success, do not wait for response', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var umcSendEmailSpy = this.sinonSandbox.spy(umClient, "sendEmail");
        var umsSendEmailSpy = this.sinonSandbox.spy(umService.messageHandlers, "sendEmail");
        async.mapSeries(testDataEmailSuccess, function (item, cbItem) {
            umcSendEmailSpy.reset();
            umsSendEmailSpy.reset();
            item.waitResponse = false;
            umClient.sendEmail(item,
                function (err) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(umcSendEmailSpy.callCount, 1);
                            assert.strictEqual(umsSendEmailSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(umsSendEmailSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(umsSendEmailSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
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
    
    it('sendSms success', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var umcSendSmsSpy = this.sinonSandbox.spy(umClient, "sendSms");
        var umsSendSmsSpy = this.sinonSandbox.spy(umService.messageHandlers, "sendSms");
        async.mapSeries(testDataSmsSuccess, function (item, cbItem) {
            umcSendSmsSpy.reset();
            umsSendSmsSpy.reset();
            umClient.sendSms(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(umcSendSmsSpy.callCount, 1);
                            assert.strictEqual(umsSendSmsSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(umsSendSmsSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(umsSendSmsSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
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
    
    it('sendSms validation error', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var umcSendSmsSpy = this.sinonSandbox.spy(umClient, "sendSms");
        var umsSendSmsSpy = this.sinonSandbox.spy(umService.messageHandlers, "sendSms");
        async.mapSeries(testDataSmsError, function (item, cbItem) {
            umcSendSmsSpy.reset();
            umsSendSmsSpy.reset();
            umClient.sendSms(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(umcSendSmsSpy.callCount, 1);
                            assert.strictEqual(umsSendSmsSpy.callCount, 0);
                            return cbItem();
                        } catch (e) {
                            return cbItem(e);
                        }
                    }, cbItem);
                }
            );
        }, done);
    });

    it('sendSms success, do not wait for response', function (done) {
        this.sinonSandbox = sinon.sandbox.create();
        var umcSendSmsSpy = this.sinonSandbox.spy(umClient, "sendSms");
        var umsSendSmsSpy = this.sinonSandbox.spy(umService.messageHandlers, "sendSms");
        async.mapSeries(testDataSmsSuccess, function (item, cbItem) {
            umcSendSmsSpy.reset();
            umsSendSmsSpy.reset();
            item.waitResponse = false;
            umClient.sendSms(item,
                function (err, response) {
                    return setImmediate(function (cbItem) {
                        try {
                            assert.strictEqual(umcSendSmsSpy.callCount, 1);
                            assert.strictEqual(umsSendSmsSpy.callCount, 1);
                            assert.deepEqual(_.sortBy(umsSendSmsSpy.getCall(0).args[0].getContent()), _.sortBy(buildResponse(item)));
                            if (item.clientInfo) {
                                assert.deepEqual(umsSendSmsSpy.getCall(0).args[0].getClientInfo(), item.clientInfo);
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
    
    it('sendEmail error, no callback provided', function () {
        var res = umClient.sendEmail(testDataEmailSuccess[0]);
        assert.instanceOf(res, Error);
        assert.strictEqual(res.message, Errors.ERR_CALLBACK_NOT_PROVIDED.message);
    });
    
    it('sendSms error, no callback provided', function () {
        var res = umClient.sendSms(testDataSmsSuccess[0]);
        assert.instanceOf(res, Error);
        assert.strictEqual(res.message, Errors.ERR_CALLBACK_NOT_PROVIDED.message);
    });
    
    it('send ' + concurrency + ' concurrent emails', function (done) {
        var items = [];
        for(var i=1; i<=concurrency; i++) {
            items.push({
                userId: '111-222-333-444-' + i,
                subject: 'subject ' + i,
                message: 'message ' + i,
                language: 'ro'
            });
        }
        
        async.map(items, function (item, cbItem) {
            umClient.sendEmail(item, cbItem);
        }, function (err, results) {
            try {
                assert.ifError(err);
                for(var i=0; i<concurrency; i++) {
                    assert.isNull(results[i].getError());
                }
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('send ' + concurrency + ' concurrent SMSs', function (done) {
        var items = [];
        for(var i=1; i<=concurrency; i++) {
            items.push({
                userId: '111-222-333-444-' + i,
                message: 'message ' + i,
                language: 'ro'
            });
        }
        
        async.map(items, function (item, cbItem) {
            umClient.sendSms(item, cbItem);
        }, function (err, results) {
            try {
                assert.ifError(err);
                for(var i=0; i<concurrency; i++) {
                    assert.isNull(results[i].getError());
                }
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
});