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
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage = false;
}


var serviceDefaultConfig = {
    ip: ip,
    port: port+1,
    secure: true,
    serviceName: 'testService',
    key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
    cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
    auth : {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
    },
    registryServiceURIs: registryServiceURIs,
    validateFullMessage: false,
    
    class: DefaultService
    
};

var umClient = null;
var concurrency = 10;

var serviceInstances = [];
var serviceClient;
var ipLookup;
var fakeSRC = new FakeRegistryServiceClient();

var instancesToKill = [];
var workersToKill = [];
var clientsToKill = [];
var serviceRegistryInstance;

describe('Tests namespaces', function() {
    this.timeout(20000);
    
    before(function (done) {
        var dns = require('dns');
        dns.lookup(ip, function (err, address) {
            ipLookup = address;
            done(err);
        });        
    });
    
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
    
    
    it('emit reconnected event, it reconnect to same instance', function (done) {
        var config1 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+10,
            serviceName: 'testReconnect'
        });
        
        var config2 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+11,
            serviceName: 'testReconnect'
        });
        
        
        var instance1;
        var instance2;
        var client;
        async.series([
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            function (next) {
                _createInstance(config2, function (err, instance) {
                    instance2 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            // wait for registration to service registry
            function (next) {
                setTimeout(next, 500);
            },
            
            function (next) {
                client = new DefaultClient({
                    serviceNamespace: config1.serviceName,
                    serviceRegistryClient: fakeSRC,
                    reconnectMaxAttempts: 5, 
                    reconnectInterval: 2000,
                    validateFullMessage: false
                });
                
                client.on('reconnected', function (callback) {
                    try {
                        assert.strictEqual(client.connected(), true);
                        clientsToKill.push(client);
                        callback();
                        next();
                    } catch (e) {
                        callback();
                        next(e);
                    }
                });
                
                async.series([
                    function (next2) {
                        client.connect(next2);
                    },
                    
                    function (next2) {
                        setTimeout(function () {
                            instance1.shutdown();
                            next2();
                        }, 500);
                    },
                    
                    function (next2) {
                        setTimeout(function () {
                            _createInstance(config1, function (err, instance) {
                                instancesToKill.push(instance);
                                next2(err);
                            });
                        }, 1500);
                        
                    }
                ]);
            }
        ], function (err) {
            done(err);
        });
        
    });
    
    it('emit reconnectedToNewInstance event, it reconnect to another instance', function (done) {
        var config1 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+20,
            serviceName: 'testReconnectNewInstance'
        });
        
        var config2 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+21,
            serviceName: 'testReconnectNewInstance'
        });
        
        
        var instance1;
        var instance2;
        var client;
        async.series([
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            function (next) {
                _createInstance(config2, function (err, instance) {
                    instance2 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            // wait for registration to service registry
            function (next) {
                setTimeout(next, 500);
            },
            
            function (next) {
                client = new DefaultClient({
                    serviceNamespace: config1.serviceName,
                    serviceRegistryClient: fakeSRC,
                    reconnectMaxAttempts: 5, 
                    reconnectInterval: 2000,
                    validateFullMessage: false
                });
                
                client.on('reconnectedToNewInstance', function (callback) {
                    callback();
                    try {
                        assert.strictEqual(client.connected(), true);
                        clientsToKill.push(client);
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
                
                async.series([
                    function (next2) {
                        client.connect(next2);
                    },
                    
                    function (next2) {
                        setTimeout(function () {
                            instance1.shutdown();
                            next2();
                        }, 500);
                    }
                ]);
            }
        ], function (err) {
            done(err);
        });
        
    });
    
    it('emit reconnectingFailed event', function (done) {
        var config1 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+30,
            serviceName: 'testreconnectingFailed'
        });
        
        var config2 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+31,
            serviceName: 'testreconnectingFailed'
        });
        
        
        var instance1;
        var instance2;
        var client;
        async.series([
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            function (next) {
                _createInstance(config2, function (err, instance) {
                    instance2 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            // wait for registration to service registry
            function (next) {
                setTimeout(next, 500);
            },
            
            
            function (next) {
                client = new DefaultClient({
                    serviceNamespace: config1.serviceName,
                    serviceRegistryClient: fakeSRC,
                    reconnectMaxAttempts: 5, 
                    reconnectInterval: 2000,
                    validateFullMessage: false
                });
                
                client.on('reconnectingFailed', function () {
                    try {
                        assert.strictEqual(client.connected(), false);
                        clientsToKill.push(client);
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
                
                async.series([
                    function (next2) {
                        client.connect(next2);
                    },
                    
                    function (next2) {
                        setTimeout(function () {
                            instance1.shutdown();
                            instance2.shutdown();
                            next2();
                        }, 500);
                    }
                ]);
            }
        ], function (err) {
            done(err);
        });
        
    });
    
    it('test _findNamespaceURIs for serviceRegsitry namespace', function (done) {
        //var orig = process.env.REGISTRY_SERVICE_URIS;
        
        var client1 = new DefaultClient({
            validateFullMessage: false,
            registryServiceURIs: 'wss://localhost:11,wss://localhost:22'
        });
        var client2 = new DefaultClient({
            validateFullMessage: false
        });
        clientsToKill.push(client1);
        clientsToKill.push(client2);
        
        async.series([
            function (next) {
                client1._findNamespaceURIs('serviceRegistry', function (err, uris) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(client1._registryServiceURIs, uris.join(','));
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            function (next) {
                client2._findNamespaceURIs('serviceRegistry', function (err, uris) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(process.env.REGISTRY_SERVICE_URIS, uris.join(','));
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], function (err) {
            done(err);
        });
    });
    
    it('add a message in front of the queue after reconnect to a new instance', function (done) {
        var config1 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+40,
            serviceName: 'testAddInFront'
        });
        
        var config2 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+41,
            serviceName: 'testAddInFront'
        });
        
        
        var instance1;
        var instance2;
        var client;
        async.series([
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            function (next) {
                _createInstance(config2, function (err, instance) {
                    instance2 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            // wait for registration to service registry
            function (next) {
                setTimeout(next, 500);
            },
            
            
            function (next) {
                client = new DefaultClient({
                    serviceNamespace: config1.serviceName,
                    serviceRegistryClient: fakeSRC,
                    reconnectMaxAttempts: 5, 
                    reconnectInterval: 2000,
                    validateFullMessage: false
                });
                
                client.on('reconnectedToNewInstance', function (callback) {
                    try {
                        clientsToKill.push(client);
                        var m = new ProtocolMessage();
                        m.setMessage('testAddInFront/ping');
                        m.setSeq(555);

                        client.sendMessage(m, function (err, response) {}, true);

                        var items = client._queueGetItems();
                        
                        assert.strictEqual(client.connected(), true);
                        assert.strictEqual(items.length, 11);
                        assert.strictEqual(items[0].data.getSeq(), 555);
                        assert.strictEqual(items[10].data.getSeq(), 10);
                        
                        callback();
                        
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
                
                async.series([
                    function (next2) {
                        client.connect(next2);
                    },
                    
                    function (next2) {
                        setTimeout(function () {
                            instance1.shutdown();
                            setTimeout(next2, 500);
                        }, 500);
                    },
                    
                    // add couple of message into queue, while it client is reconnecting
                    function (next2) {
                        var items = [];
                        for(var i=1; i<=10; i++) {
                            var m = new ProtocolMessage();
                            m.setMessage('testAddInFront/ping');
                            m.setSeq(i);
                            items.push({
                                message: m,
                                client: client
                            });
                        }

                        async.map(items, _sendMessageResponseSuccess, next2);
                        
                    }
                ]);
            }
        ], function (err) {
            done(err);
        });
        
    });
    
    
    it('test reconnect forever', function (done) {
        //return done();
        var config1 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+50,
            serviceName: 'testReconnectForever'
        });
        
        var instance1;
        var instance2;
        var client;
        async.series([
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            // wait for registration to service registry
            function (next) {
                setTimeout(next, 500);
            },
            
            function (next) {
                client = new DefaultClient({
                    serviceNamespace: config1.serviceName,
                    serviceRegistryClient: fakeSRC,
                    reconnectMaxAttempts: 2, 
                    reconnectInterval: 1000,
                    validateFullMessage: false,
                    reconnectForever: true
                });
                
                client.on('reconnected', function (callback) {
                    callback();
                    try {
                        assert.strictEqual(client.connected(), true);
                        clientsToKill.push(client);
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
                
                async.series([
                    function (next2) {
                        client.connect(next2);
                    },
                    
                    function (next2) {
                        setTimeout(function () {
                            instance1.shutdown();
                            next2();
                        }, 500);
                    },
                    
                    function (next2) {
                        setTimeout(function () {
                            _createInstance(config1, function (err, instance) {
                                instance1 = instance;
                                instancesToKill.push(instance);
                                next2(err);
                            });
                        }, 6000);
                    }
                ]);
            }
        ], function (err) {
            done(err);
        });
        
    });
    
    it('test auto connect, forever reconnect mode', function (done) {
        //return done();
        var config1 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+60,
            serviceName: 'testAutoConnect'
        });
        
        var instance1;
        var instance2;
        var client;
        async.series([
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            // wait for registration to service registry
            function (next) {
                setTimeout(next, 500);
            },
            
            function (next) {
                client = new DefaultClient({
                    serviceNamespace: config1.serviceName,
                    serviceRegistryClient: fakeSRC,
                    reconnectMaxAttempts: 2, 
                    reconnectInterval: 1000,
                    validateFullMessage: false,
                    reconnectForever: true,
                    autoConnect: true
                });
                
                var message = new ProtocolMessage();
                message.setMessage('testAutoConnect/ping');
                message.setSeq(1234);
                
                client.sendMessage(message, function (err, response) {
                    try {
                        if (message.getSeq() === null) {
                            assert.ifError(err);
                            return next(false, response);
                        } else {
                            assert.ifError(err);
                            assert.isNull(response.getError());
                            assert.isNotNull(response.getAck());
                            assert.strictEqual(response.getAck()[0], message.getSeq());
                            assert.strictEqual(response.getMessage(), message.getMessage() + 'Response');
                            return next(false, response);
                        }
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], function (err) {
            done(err);
        });
        
    });
    
    it('test auto connect, non forever reconnect mode', function (done) {
        var config1 = _.assign(_.clone(serviceDefaultConfig), {
            port: parseInt(port)+70,
            serviceName: 'testAutoConnectNonForever'
        });
        
        var instance1;
        var instance2;
        var client;
        async.series([
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    next(err);
                });
            },
            // wait for registration to service registry
            function (next) {
                setTimeout(next, 500);
            },
            
            function (next) {
                client = new DefaultClient({
                    serviceNamespace: config1.serviceName,
                    serviceRegistryClient: fakeSRC,
                    reconnectMaxAttempts: 2, 
                    reconnectInterval: 1000,
                    validateFullMessage: false,
                    autoConnect: true
                });
                
                var message = new ProtocolMessage();
                message.setMessage('testAutoConnectNonForever/ping');
                message.setSeq(1234);
                
                client.sendMessage(message, function (err, response) {
                    try {
                        if (message.getSeq() === null) {
                            assert.ifError(err);
                            return next(false, response);
                        } else {
                            assert.ifError(err);
                            assert.isNull(response.getError());
                            assert.isNotNull(response.getAck());
                            assert.strictEqual(response.getAck()[0], message.getSeq());
                            assert.strictEqual(response.getMessage(), message.getMessage() + 'Response');
                            return next(false, response);
                        }
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            // disconnect
            function (next) {
                instance1.shutdown();
                setTimeout(next, 500);
            },
            
            // send message receive error
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testAutoConnectNonForever/ping');
                message.setSeq(22);
                
                client.sendMessage(message, function (err, response) {
                    try {
                        assert.isOk(err);
                        assert.strictEqual(err.message, 'ERR_CONNECTION_ERROR');
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // restart service
            function (next) {
                _createInstance(config1, function (err, instance) {
                    instance1 = instance;
                    instancesToKill.push(instance);
                    setTimeout(next, 1000, err);
                });
            },
            
            // send with success
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testAutoConnectNonForever/ping');
                message.setSeq(44);
                
                client.sendMessage(message, function (err, response) {
                    try {
                        if (message.getSeq() === null) {
                            assert.ifError(err);
                            return next(false, response);
                        } else {
                            assert.ifError(err);
                            assert.isNull(response.getError());
                            assert.isNotNull(response.getAck());
                            assert.strictEqual(response.getAck()[0], message.getSeq());
                            assert.strictEqual(response.getMessage(), message.getMessage() + 'Response');
                            return next(false, response);
                        }
                    } catch (e) {
                        return next(e);
                    }
                });
            }
        ], function (err) {
            done(err);
        });
        
    });
    
    
    it('connect to namespace', function (done) {
        serviceClient = new DefaultClient({
            serviceNamespace: 'testService',
            validateFullMessage: false,
            serviceRegistryClient: fakeSRC
        });
        
        clientsToKill.push(serviceClient);
        
        serviceClient.connect(done);
    });
    
    it('send couple of concurrent messages with success', function (done) {
        var items = [];
        for(var i=1; i<=20; i++) {
            var m = new ProtocolMessage();
            m.setMessage('testService/ping');
            m.setSeq(i%2 ? i : null);
            items.push({
                message: m,
                client: serviceClient
            });
        }
        
        async.map(items, _sendMessageResponseSuccess, done);
    });
    
    it('send couple of concurrent messages with error', function (done) {
        var items = [];
        for(var i=1; i<=10; i++) {
            var m = new ProtocolMessage();
            m.setMessage('testService/pingNotExists');
            m.setSeq(i);
            items.push({
                message: m,
                client: serviceClient,
                errorMessage: Errors.ERR_FATAL_ERROR.message
            });
        }
        
        async.map(items, _sendMessageResponseError, done);
    });
    
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
