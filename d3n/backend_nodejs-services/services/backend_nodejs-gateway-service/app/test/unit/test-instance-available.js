var config = require('../../config/config.js'),
    Constants = require('../../config/constants.js'),
    ip = config.gateway.ip,
    port = config.gateway.port,
    secure = true,
    registryServiceURIs = config.registryServiceURIs,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    Errors = require('nodejs-errors'),
    workerService = null,
    url = require('url'),
    ServiceClient = require('nodejs-default-client'),
    logger = require('nodejs-logger')(),
    EventServiceClient = require('nodejs-event-service-client'),
    Gateway = require('../../classes/Gateway.js'),
    serviceClient = new ServiceClient({
        secure: true,
        service: {
            ip: ip,
            port: port
        }
    }),
    ProtocolMessage = require('nodejs-protocol'),
    instances = {
        'question': [
            {
                ip: ip,
                port: port+4,
                secure: secure
            },
            {
                ip: ip,
                port: port+5,
                secure: secure
            },
            {
                ip: ip,
                port: port+6,
                secure: secure
            }
            
        ],
        'workorder': [
            {
                ip: ip,
                port: port+7,
                secure: secure
            },
            {
                ip: ip,
                port: port+8,
                secure: secure
            }
        ],
        'testNewInstance': [
            {
                ip: ip,
                port: port+9,
                secure: secure
            }
        ],
        'testRemoveThenAdd': [
            {
                ip: ip,
                port: port+10,
                secure: secure
            }
        ]
        
    },
    
    workers = [],
    serviceClients = [],
    workersWorkorder = [],
    workerServiceRegistry = null,
    testRemoveThenAdd
;

serviceClients.push(serviceClient);

describe('TEST INSTANCE AVAILABLE', function() {
    this.timeout(20000);
    
    it('create service instances', function (done) {
        var items = [];
        for(var k in instances) {
            for(var i=0; i<instances[k].length; i++) {
                var config = instances[k][i];
                config.serviceName = k;
                items.push(config);
            }
        }
        
        async.mapSeries(items, function (item, cb) {
            var args = [
                JSON.stringify({
                    'service': {
                        secure: item.secure,
                        ip: item.ip,
                        port: item.port,
                        serviceName: item.serviceName,
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        },
                        registryServiceURIs: registryServiceURIs
                    }
                })
            ];
            
            var workerService = cp.fork(__dirname + '/worker-create-service.js', args);
            
            workers.push(workerService);
            
            if(item.serviceName === 'workorder') {
                workersWorkorder.push(workerService);
            }
            if(item.serviceName === 'testRemoveThenAdd') {
                testRemoveThenAdd = workerService;
            }
            
            workerService.on('message', function (message) {
                if(message.success === true) {
                    return cb(false);
                }

                return cb(new Error('error starting testing service'));
            });
            
        }, function (err) {
            assert.ifError(err);
            return setTimeout(done, 2000);
        });
    });
    
        
    it('create gateway', function (done) {
        var args = [
            JSON.stringify({
                'gateway': {
                    secure: secure,
                    ip: ip,
                    port: port,
                    key: __dirname + '/ssl-certificate/key.pem',
                    cert: __dirname + '/ssl-certificate/cert.pem',
                    auth: config.gateway.auth
                },
                registryServiceURIs: registryServiceURIs
            })
        ];
        var worker = cp.fork(__dirname + '/worker-create-gateway.js', args);

        workers.push(worker);

        worker.on('message', function (message) {
            if(message.success === true) {
                return setTimeout(done, 2000);
            }

            return done(new Error('error starting gateway'));
        });
        
    });
    
    
    
    it('connect to gateway', function (done) {
        serviceClient.connect(done);
    });
    
    
    it('receive ping response success', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/ping');
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(1);
        reqMessage.setAck(null);
        reqMessage.setError(null);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getError(), null);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            done();
        });
    });
    
    it('create new instance for non existing service', function (done) {
        var newInstancePort = port+20;
        var newInstanceConfig = {
            secure: secure,
            port: newInstancePort,
            ip: ip,
            uri: 'ws' + (secure === true ? 's' : '') + '://' + ip + ':' + newInstancePort,
            serviceName: 'newService'
        };
        
        var createNewInstance = function (cb) {
            var args = [
                JSON.stringify({
                    'service': {
                        secure: newInstanceConfig.secure,
                        ip: newInstanceConfig.ip,
                        port: newInstanceConfig.port,
                        serviceName: newInstanceConfig.serviceName,
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        },
                        registryServiceURIs: registryServiceURIs
                    }
                })
            ];
            
            var workerService = cp.fork(__dirname + '/worker-create-service.js', args);
            workers.push(workerService);
            
            
            workerService.on('message', function (message) {
                logger.debug('>>new instance creation result:', message);
                if(message.success === true) {
                    return cb(false);
                }

                return cb(new Error('error starting new service instance'));
            });
            
        };
        
        
        var publishNewInstanceEvent = function (cb) {
            try {
                var eventServiceClient = new EventServiceClient(registryServiceURIs);
                
                eventServiceClient.publish(Constants.EVT_INSTANCE_AVAILABLE, {
                    serviceNamespace: newInstanceConfig.serviceName,
                    uri: newInstanceConfig.uri
                }, cb);
            } catch (e) {
                return setImmediate(cb, e);
            }
            
        };
        
        var sendPingToNewInstance = function (cb) {
            try {
                var reqMessage = new ProtocolMessage();
                reqMessage.setMessage(newInstanceConfig.serviceName + '/ping');
                reqMessage.setContent({data: 'pingdata_newService'});
                reqMessage.setSeq(1);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    //console.log('>>>>>>>>>>here:', err, resMessage);
                    try {
                        assert.ifError(err);
                        
                        assert.strictEqual(resMessage.getError(), null);
                        
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        return cb(false);
                    } catch (e) {
                        return cb(e);
                    }
                });
            } catch (e) {
                return setImmediate(cb, e);
            }
        };
        
        
        async.series([
            function (next) {
                createNewInstance(next);
            },
            /*
            function (next) {
                publishNewInstanceEvent(next);
            },*/
            
            function (next) {
                // wait a bit
                setTimeout(next, 4000);
            },
            
            function (next) {
                sendPingToNewInstance(next);
            }
            
        ], function (err) {
            assert.ifError(err);
            return done(err);
        });
        
    });
    
    it('create new instance(multiple namespaces) for non existing service', function (done) {
        var newInstancePort = port+25;
        var newInstanceConfig = {
            secure: secure,
            port: newInstancePort,
            ip: ip,
            uri: 'ws' + (secure === true ? 's' : '') + '://' + ip + ':' + newInstancePort,
            serviceName: 'newServiceWIthNamespaces',
            serviceNamespaces: ['newServicens1', 'newServicens2']
        };
        
        var createNewInstance = function (cb) {
            var args = [
                JSON.stringify({
                    'service': {
                        secure: newInstanceConfig.secure,
                        ip: newInstanceConfig.ip,
                        port: newInstanceConfig.port,
                        serviceName: newInstanceConfig.serviceName,
                        serviceNamespaces: newInstanceConfig.serviceNamespaces,
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        },
                        registryServiceURIs: registryServiceURIs
                    }
                })
            ];
            
            var workerService = cp.fork(__dirname + '/worker-create-service.js', args);
            workers.push(workerService);
            
            
            workerService.on('message', function (message) {
                logger.debug('>>new instance creation result:', message);
                if(message.success === true) {
                    return cb(false);
                }

                return cb(new Error('error starting new service instance'));
            });
            
        };
        
        
        var publishNewInstanceEvent = function (cb) {
            try {
                var eventServiceClient = new EventServiceClient(registryServiceURIs);
                
                eventServiceClient.publish(Constants.EVT_INSTANCE_AVAILABLE, {
                    serviceNamespace: newInstanceConfig.serviceName,
                    uri: newInstanceConfig.uri
                }, cb);
            } catch (e) {
                return setImmediate(cb, e);
            }
            
        };
        
        var sendPingToNewInstance = function (cb) {
            async.series([
                function (next) {
                    var reqMessage = new ProtocolMessage();
                    reqMessage.setMessage(newInstanceConfig.serviceName + '/ping');
                    reqMessage.setContent({data: 'pingdata_newService'});
                    reqMessage.setSeq(1);
                    reqMessage.setAck(null);
                    reqMessage.setError(null);

                    serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(resMessage.getError(), null);
                            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                            return next(false);
                        } catch (e) {
                            return next(e);
                        }
                    });
                },
                
                function (next) {
                    var reqMessage = new ProtocolMessage();
                    reqMessage.setMessage(newInstanceConfig.serviceNamespaces[0] + '/ping');
                    reqMessage.setContent({data: 'pingdata_newService'});
                    reqMessage.setSeq(2);
                    reqMessage.setAck(null);
                    reqMessage.setError(null);

                    serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(resMessage.getError(), null);
                            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                            return next(false);
                        } catch (e) {
                            return next(e);
                        }
                    });
                }
                
            ], cb);
            
        };
        
        
        async.series([
            function (next) {
                createNewInstance(next);
            },
            /*
            function (next) {
                publishNewInstanceEvent(next);
            },*/
            
            function (next) {
                // wait a bit
                setTimeout(next, 4000);
            },
            
            function (next) {
                sendPingToNewInstance(next);
            }
            
        ], function (err) {
            assert.ifError(err);
            return done(err);
        });
        
    });
    
    it('remove instance then add', function (done) {
        var newInstancePort = port+40;
        var newInstanceConfig = {
            secure: secure,
            port: newInstancePort,
            ip: ip,
            uri: 'ws' + (secure === true ? 's' : '') + '://' + ip + ':' + newInstancePort,
            serviceName: 'testRemoveThenAdd'
        };
        
        var createNewInstance = function (cb) {
            var args = [
                JSON.stringify({
                    'service': {
                        secure: newInstanceConfig.secure,
                        ip: newInstanceConfig.ip,
                        port: newInstanceConfig.port,
                        serviceName: newInstanceConfig.serviceName,
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        },
                        registryServiceURIs: registryServiceURIs
                    }
                })
            ];
            
            var workerService = cp.fork(__dirname + '/worker-create-service.js', args);
            workers.push(workerService);
            
            
            workerService.on('message', function (message) {
                logger.debug('>>new instance creation result:', message);
                if(message.success === true) {
                    return cb(false);
                }

                return cb(new Error('error starting new service instance'));
            });
            
        };
        
        
        var publishNewInstanceEvent = function (cb) {
            try {
                var eventServiceClient = new EventServiceClient(registryServiceURIs);
                
                eventServiceClient.publish(Constants.EVT_INSTANCE_AVAILABLE, {
                    serviceNamespace: newInstanceConfig.serviceName,
                    uri: newInstanceConfig.uri
                }, cb);
            } catch (e) {
                return setImmediate(cb, e);
            }
            
        };
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        
        var sendMessage = function (client, msg, cb) {
            try {
                var reqMessage = new ProtocolMessage();
                reqMessage.setMessage(msg);
                reqMessage.setContent({data: 'pingdata_newService'});
                reqMessage.setSeq(1);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                client.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        
                        assert.strictEqual(resMessage.getError(), null);
                        
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        return cb(false);
                    } catch (e) {
                        return cb(e);
                    }
                });
            } catch (e) {
                return setImmediate(cb, e);
            }
        };
        
        var sendMessageErrNoInstance = function (client, msg, cb) {
            try {
                var reqMessage = new ProtocolMessage();
                reqMessage.setMessage(msg);
                reqMessage.setContent({data: 'pingdata_newService'});
                reqMessage.setSeq(1);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                client.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        //console.log('>>>>>>>', resMessage.getError());
                        assert.ifError(err);
                        
                        assert.isNotNull(resMessage.getError());
                        
                        assert.strictEqual(resMessage.getError().message, 'ERR_NO_INSTANCE_AVAILABLE');
                        return cb(false);
                    } catch (e) {
                        return cb(e);
                    }
                });
            } catch (e) {
                return setImmediate(cb, e);
            }
        };
        
        async.series([
            function (next) {
                client.connect(next); 
            },
            
            function (next) {
                sendMessage(client, 'testRemoveThenAdd/ping', next);
            },
            
            function (next) {
                testRemoveThenAdd.kill();
                setTimeout(next, 1000);
            },
            
            function (next) {
                sendMessageErrNoInstance(client, 'testRemoveThenAdd/ping', next);
            },
            
            function (next) {
                createNewInstance(function (err) {
                    if (err) {
                        return next(err);
                    }
                    
                    setTimeout(next, 2000);
                });
            },
            
            function (next) {
                sendMessage(client, 'testRemoveThenAdd/ping', next);
            }
            
            //
            //function (next) {
                //publishNewInstanceEvent(next);
            //},
            /*
            function (next) {
                // wait a bit
                setTimeout(next, 4000);
            },
            
            function (next) {
                createTwoClients(next);
            },
            
            function (next) {
                sendPingToNewInstance(client1, 'ping', next);
            },
            
            function (next) {
                sendPingToNewInstance(client2, 'ping2', next);
            }*/
            
        ], function (err) {
            assert.ifError(err);
            return done(err);
        });
        
    });
    it('create new instance for an existing service', function (done) {
        var newInstancePort = port+30;
        var newInstanceConfig = {
            secure: secure,
            port: newInstancePort,
            ip: ip,
            uri: 'ws' + (secure === true ? 's' : '') + '://' + ip + ':' + newInstancePort,
            serviceName: 'testNewInstance'
        };
        
        var createNewInstance = function (cb) {
            var args = [
                JSON.stringify({
                    'service': {
                        secure: newInstanceConfig.secure,
                        ip: newInstanceConfig.ip,
                        port: newInstanceConfig.port,
                        serviceName: newInstanceConfig.serviceName,
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        },
                        registryServiceURIs: registryServiceURIs
                    },
                    'ping2': true
                })
            ];
            
            var workerService = cp.fork(__dirname + '/worker-create-service.js', args);
            workers.push(workerService);
            
            
            workerService.on('message', function (message) {
                logger.debug('>>new instance creation result:', message);
                if(message.success === true) {
                    return cb(false);
                }

                return cb(new Error('error starting new service instance'));
            });
            
        };
        
        
        var publishNewInstanceEvent = function (cb) {
            try {
                var eventServiceClient = new EventServiceClient(registryServiceURIs);
                
                eventServiceClient.publish(Constants.EVT_INSTANCE_AVAILABLE, {
                    serviceNamespace: newInstanceConfig.serviceName,
                    uri: newInstanceConfig.uri
                }, cb);
            } catch (e) {
                return setImmediate(cb, e);
            }
            
        };
        
        var client1 = null, client2 = null;
        var createTwoClients = function (cb) {
            client1 = new ServiceClient({
                secure: true,
                service: {
                    ip: ip,
                    port: port
                }
            });
            
            serviceClients.push(client1);
            
            client2 = new ServiceClient({
                secure: true,
                service: {
                    ip: ip,
                    port: port
                }
            });
            serviceClients.push(client2);
            
            client1.connect(function (err) {
                if (err) {
                    return cb(err);
                }
                
                client2.connect(cb);
            });
            
        };
        
        var sendPingToNewInstance = function (client, ping, cb) {
            try {
                var reqMessage = new ProtocolMessage();
                reqMessage.setMessage(newInstanceConfig.serviceName + '/' + ping);
                reqMessage.setContent({data: 'pingdata_newService'});
                reqMessage.setSeq(1);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                client.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        
                        assert.strictEqual(resMessage.getError(), null);
                        
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        return cb(false);
                    } catch (e) {
                        return cb(e);
                    }
                });
            } catch (e) {
                return setImmediate(cb, e);
            }
        };
        
        
        async.series([
            function (next) {
                createNewInstance(next);
            },
            //
            //function (next) {
                //publishNewInstanceEvent(next);
            //},
            
            function (next) {
                // wait a bit
                setTimeout(next, 4000);
            },
            
            function (next) {
                createTwoClients(next);
            },
            
            function (next) {
                sendPingToNewInstance(client1, 'ping', next);
            },
            
            function (next) {
                sendPingToNewInstance(client2, 'ping2', next);
            }
            
        ], function (err) {
            assert.ifError(err);
            return done(err);
        });
        
    });
    
    
    
    after(function(done) {
        for(var i=0; i<serviceClients.length; i++) {
            try {
                if(serviceClients[i].connected()) {
                    serviceClients[i].disconnect(1000, 'tests done, disconnect', true);
                }
            } catch (e) {
            }
        }
        
        for(var i=0; i<workers.length; i++) {
            try {
                workers[i].kill();
            } catch (e) {
            }

        }
        setTimeout(done, 1500);
    });
    
});
