var ip = process.env.HOST || 'localhost',
    port = process.env.PORT || 4001,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
    DefaultService = require('nodejs-default-service'),
    Errors = require('nodejs-errors'),
    ServiceClient = require('../../classes/Client'),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    serviceClient = new ServiceClient({
        secure: true,
        service: {
            ip: ip,
            port: port
        },
        jwt: token,
        validateFullMessage: false
    }),
    ProtocolMessage = require('nodejs-protocol'),
    sslKey = fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
    sslCert = fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
    auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
    }
    ;

describe('Service Client', function() {
    before(function(done) {
        //return done();
        this.timeout(10000);
        workerService = cp.fork(__dirname + '/worker-service.js', [], {
            execArgv: [],
            env: {
                'WORKER_CONFIG': JSON.stringify({
                    'service': {
                        secure: true,
                        ip: ip,
                        port: port,
                        serviceName: 'test_service',
                        key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                        cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                        auth: {
                            algorithm: 'RS256',
                            publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                        }
                    }
                })
            }
        });

        workerService.on('message', function (message) {
            if(message.success === true) {
                return done();
            }

            return done(new Error('error starting testing service'));
        });
    });
    
    it('connect without callback', function (done) {
        serviceClient.connect();

        setTimeout(function() {
            assert.strictEqual(serviceClient.connected(), false);
            done();
        },50);
    });

    it('should connect to service', function (done) {
        this.timeout(10000);
        serviceClient.connect(function() {
            assert.strictEqual(serviceClient.connected(), true);
            done();
        });
    });
    
    it('dontWaitResponse=true, seq set', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test_service/sr_register');
        reqMessage.setContent({serviceNamespace: 'asdas'});
        reqMessage.setSeq(serviceClient.getSeq());
        reqMessage.setError(null);
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                assert.ifError(err);
                assert.isNotOk(resMessage);
                done();
            } catch (e) {
                return done(e);
            }
        }, false, true);
        
    });
    
    it('should receive ping response success', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test_service/pingCustom');
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(serviceClient.getSeq());
        reqMessage.setAck(null);
        reqMessage.setError(null);
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            done();
        });
    });

    it('should receive ping response success with token', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test_service/pingCustom');
        reqMessage.setToken(token);
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(serviceClient.getSeq());
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            done();
        });
    });



    it('should receive message handler not found', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test_service/qqwwee');
        reqMessage.setContent(null);
        reqMessage.setSeq(serviceClient.getSeq());
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            assert.isNotNull(resMessage.getError());
            assert.strictEqual(resMessage.getError().message, Errors.ERR_FATAL_ERROR.message);
            done();
        });
    });

    it('should send two concurrent messages', function (done) {
        var reqMessage1 = new ProtocolMessage();
        var reqMessage2 = new ProtocolMessage();
        reqMessage1.setMessage('test_service/pingCustom');
        reqMessage1.setContent({data: 'for ping'});
        reqMessage1.setSeq(serviceClient.getSeq());
        reqMessage1.setAck(null);
        reqMessage1.setError(null);

        reqMessage2.setMessage('test_service/ping');
        reqMessage2.setContent({data: 'for ping'});
        reqMessage2.setSeq(serviceClient.getSeq());
        reqMessage2.setAck(null);
        reqMessage2.setError(null);

        async.parallel([
            function (next) {
                serviceClient.sendMessage(reqMessage1, next);
            },

            function (next) {
                serviceClient.sendMessage(reqMessage2, next);
            }

        ], function (err, responses) {
            var resMessage1 = responses[0];
            var resMessage2 = responses[1];

            assert.strictEqual(resMessage1.getMessage(), reqMessage1.getMessage() + 'Response');
            assert.strictEqual(resMessage2.getMessage(), reqMessage2.getMessage() + 'Response');

            assert.strictEqual(resMessage1.getError(), null);
            assert.strictEqual(resMessage2.getError(), null);
            //console.log('responses:', responses);
            assert.ifError(err);

            done();
        });
    });

    it('send a message without sequence number', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('test_service/qqwwee');
        reqMessage.setContent(null);
        reqMessage.setSeq(null);
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.typeOf(resMessage, 'undefined');
            done();
        });
    });

    it('send a message without messager', function (done) {

        serviceClient.sendMessage(null, function (err, resMessage) {
            assert.isNotFalse(err);
            assert.typeOf(resMessage, 'undefined');
            done();
        });
    });

    it('send authentification', function (done) {

        serviceClient.authenticate(function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), 'global/authResponse');
            done();
        });
    });

    it('send, disconnect, connect, send', function (done) {
        //return done();
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('ping');
        reqMessage.setContent(null);
        reqMessage.setSeq(serviceClient.getSeq());
        reqMessage.setAck(null);
        reqMessage.setError(null);

        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            var onDisconnectHandler = function(reasonCode, description) {
                serviceClient.removeListener('connectionClose', onDisconnectHandler);

                assert.strictEqual(reasonCode, 1000);
                assert.strictEqual(description, "testing disconnect");

                serviceClient.on('reconnected', onReconnectHandler);

            };
            var onReconnectHandler = function(eventCallback) {
                serviceClient.removeListener('reconnected', onReconnectHandler);
                //assert.ifError(err);


                var reqMessage = new ProtocolMessage();
                var seq = serviceClient.getSeq();
                reqMessage.setMessage('ping');
                reqMessage.setContent(null);
                reqMessage.setSeq(seq);
                reqMessage.setAck(null);
                reqMessage.setError(null);
                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    assert.ifError(err);
                    assert.strictEqual(resMessage.getAck()[0], seq);
                    done();
                });
                eventCallback();
            };
            serviceClient.on('connectionClose', onDisconnectHandler);
            serviceClient.disconnect(1000, "testing disconnect", false);
        });
    });

    it('invalid connection', function (done) {
        this.timeout(15000);
        var invalidClient = new ServiceClient({service: {ip:'1337.0.0.2',port:26262}});
        invalidClient.connect(function (err) {
            try {
                assert.isNotFalse(err);
                assert.strictEqual(invalidClient.connected(), false);
                done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('reconnect with invalid max attempts', function (done) {
        var failingClient = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            reconnectInterval: 10,
            reconnectMaxAttempts: -1
        });
        failingClient.connect(function (err) {
            assert.ifError(err);
            var onDisconnectHandler = function(reasonCode, description) {
                console.log("DISCONNECTED ");
                failingClient.removeListener('connectionClose', onDisconnectHandler);

                assert.strictEqual(reasonCode, 1000);
                assert.strictEqual(description, "testing disconnect");
                assert.strictEqual(failingClient.connected(), false);

                setTimeout(function() {
                    assert.strictEqual(failingClient.connected(), false);
                    done();
                },10);

            };
            failingClient.on('connectionClose', onDisconnectHandler);
            failingClient.disconnect(1000, "testing disconnect");
        });

    });

    it('failing reconnect', function (done) {
        var failingClient = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            reconnectInterval: 10,
            reconnectMaxAttempts: 2,
            validateFullMessage: false
        });
        failingClient.connect(function (err) {
            assert.ifError(err);
            var onDisconnectHandler = function(reasonCode, description) {
                failingClient.removeListener('connectionClose', onDisconnectHandler);

                assert.strictEqual(reasonCode, 1000);
                assert.strictEqual(description, "testing disconnect");
                assert.strictEqual(failingClient.connected(), false);

                setTimeout(function() {
                    assert.strictEqual(failingClient.connected(), false);
                    done();
                },200);

            };
            failingClient.on('connectionClose', onDisconnectHandler);
            failingClient.config.service = {ip:'1337.0.0.2',port:26262};
            failingClient.disconnect(1000, "testing disconnect");
        });

    });


    it('automatic reconnect when restarting server', function (done) {
        this.timeout(10000);
        var origMaxAttempts = serviceClient._reconnectMaxAttempts;
        serviceClient._reconnectMaxAttempts = 5;
        
        var onReconnectHandler = function(eventCallback) {
            serviceClient._reconnectMaxAttempts = origMaxAttempts;
            serviceClient.removeListener('reconnected', onReconnectHandler);
            assert.strictEqual(serviceClient.connected(), true);
            eventCallback();
            done();
        };
        var onDisconnectHandler = function(reasonCode, description) {
            serviceClient.removeListener('connectionClose', onDisconnectHandler);

            assert.strictEqual(serviceClient.connected(), false);
            serviceClient.on('reconnected', onReconnectHandler);

            workerService = cp.fork(__dirname + '/worker-service.js', [], {
                execArgv: [],
                env: {
                    'WORKER_CONFIG': JSON.stringify({
                        'service': {
                            secure: true,
                            ip: ip,
                            port: port,
                            serviceName: 'test_service',
                            key: fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8'),
                            cert: fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8'),
                            auth: {
                                algorithm: 'RS256',
                                publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
                            }
                        }
                    })
                }
            });
        };
        serviceClient.on('connectionClose', onDisconnectHandler);

        workerService.kill();

    });
    
    it('emit reconnectingFailed event', function (done) {
        this.timeout(10000);
        var servicePort = parseInt(port)+5;
        var service = new DefaultService({
            ip: ip,
            port: servicePort,
            secure: true,
            serviceName: 'test_service2',
            key: sslKey,
            cert: sslCert,
            auth: auth,
            validateFullMessage: false
        });
        
        var serviceClient = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: servicePort
            },
            jwt: token,
            validateFullMessage: false
        });
        
        
        async.series([
            // start the service
            function (next) {
                service.build(next);
            },
            
            // connect to service
            function (next) {
                serviceClient.connect(next);
            },
            
            // send a message
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('test_service2/ping');
                m.setSeq(123);
                
                serviceClient.sendMessage(m, function (err, result) {
                    try {
                        assert.ifError(err);
                        assert.isNull(result.getError());
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // shutdown and listen for reconnectingFailed event
            function (next) {
                try {
                    serviceClient.on('reconnectingFailed', function () {
                        try {
                            assert.strictEqual(serviceClient.connected(), false);
                            next();
                        } catch (e) {
                            next(e);
                        }
                    });
                    
                    service.shutdown();
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
            
        ], function (err) {
            done(err);
        });
    });
    
    
    it('auto reconnect multiple times', function (done) {
        this.timeout(20000);
        var workerService;
        var servicePort = parseInt(port)+16;
        var serviceConfig = {
            ip: ip,
            port: servicePort,
            secure: true,
            serviceName: 'test_service3',
            key: sslKey,
            cert: sslCert,
            auth: auth,
            validateFullMessage: false
        };
        
        var serviceClient = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: servicePort
            },
            jwt: token,
            validateFullMessage: false,
            reconnectMaxAttempts: 5
        });
        
        
        var cntReconnections = 0;
        serviceClient.on('reconnected', function (eventCallback) {
            cntReconnections++;
            eventCallback();
        });
        
        
        async.series([
            // create service
            function (next) {
                _createServiceWithWorker(serviceConfig, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    
                    workerService = worker;
                    
                    return next();
                });
            },
            
            // connect to service
            function (next) {
                serviceClient.connect(function (err) {
                    setTimeout(function () {
                        next(err);
                    }, 500);
                });
            },
            
            // kill service
            function (next) {
                try {
                    workerService.kill();
                    setTimeout(next, 500);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // re-create service
            function (next) {
                _createServiceWithWorker(serviceConfig, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    
                    workerService = worker;
                    
                    setTimeout(next, 4000);
                });
            },
            
            // kill service
            function (next) {
                try {
                    workerService.kill();
                    setTimeout(next, 500);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // re-create service
            function (next) {
                _createServiceWithWorker(serviceConfig, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    
                    workerService = worker;
                    
                    setTimeout(next, 4000);
                });
            },
            
            function (next) {
                try {
                    assert.strictEqual(cntReconnections, 2);
                    assert.strictEqual(serviceClient.connected(), true);
                    return next();
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // cleanup
            function (next) {
                serviceClient.removeAllListeners();
                serviceClient.disconnect(1000, '', true);
                workerService.kill();
                return setImmediate(next);
            }
            
        ], function (err) {
            done(err);
        });
        
        
    });

    after(function(done) {
        workerService.kill();
        setTimeout(done, 1000);
    });

});





function _createServiceWithWorker (serviceConfig, cb) {
    var args = [JSON.stringify({service: serviceConfig})];

    var workerService = cp.fork(__dirname + '/worker-create-service.js', args);
    
    workerService.on('message', function (message) {
        if(message.success === true) {
            return cb(false, workerService);
        }

        return cb(new Error('error starting service'));
    });
};