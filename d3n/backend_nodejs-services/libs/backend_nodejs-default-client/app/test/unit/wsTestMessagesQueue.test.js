var ip = process.env.HOST || 'localhost',
    port = process.env.PORT || 4001,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    //workerService = null,
    DefaultService = require('nodejs-default-service'),
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

var _ = require('lodash');

describe('Tests messages queue', function() {
    this.timeout(15000);
    
    it('disconnect, reconnect, messages sent success', function (done) {
        var workerService;
        var sport = port+1;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: sport
            },
            jwt: token,
            reconnectInterval: 1000,
            reconnectMaxAttempts: 5,
            validateFullMessage: false
        });
        
        
        async.series([
            function (next) {
                _createServiceWithWorker({
                    ip: ip,
                    port: sport,
                    secure: true,
                    serviceName: 'test_service',
                    key: sslKey,
                    cert: sslCert,
                    auth: auth,
                    validateFullMessage: false
                }, function (err, worker) {
                    workerService = worker;
                    return next(err);
                });
            },
            
            // connect to service
            function (next) {
                client.connect(next);
            },
            
            // send couple of concurrent messages
            function (next) {
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
            },
            
            // kill service
            function (next) {
                workerService.kill();
                setTimeout(next, 500);
            },
            
            // send messages while service is down then restart the service
            function (next) {
                try {
                    assert.strictEqual(client.connected(), false);
                } catch (e) {
                    return setImmediate(next, e);
                }
                
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
                
                setTimeout(function () {
                    _createServiceWithWorker({
                        ip: ip,
                        port: sport,
                        secure: true,
                        serviceName: 'test_service',
                        key: sslKey,
                        cert: sslCert,
                        auth: auth
                    }, function (err, worker) {
                        if (err) {
                            return next(err);
                        }
                        workerService = worker;
                    });
                    
                }, 500);
            },
            
            function (next) {
                try {
                    assert.strictEqual(client.connected(), true);
                    assert.strictEqual(_.keys(client.callbacks).length, 0);
                    assert.strictEqual(_.keys(client._onTheRoadMessages).length, 0);
                    assert.strictEqual(client.getQueue().length(), 0);
                    client.disconnect(1000, '', true);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], function (err) {
            client.removeAllListeners();
            client.disconnect(1000, '', true);
            workerService.kill();
            done(err);
        });
    });
    
    it('disconnect, reconnect, messages sent success, diconnect, reconect, messages sent success', function (done) {
        var workerService;
        var sport = port+2;
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: sport
            },
            jwt: token,
            reconnectInterval: 1000,
            reconnectMaxAttempts: 5,
            validateFullMessage: false
        });
        
        
        async.series([
            function (next) {
                _createServiceWithWorker({
                    ip: ip,
                    port: sport,
                    secure: true,
                    serviceName: 'test_service',
                    key: sslKey,
                    cert: sslCert,
                    auth: auth
                }, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    workerService = worker;
                    return next();
                });
            },
            
            // connect to service
            function (next) {
                client.connect(next);
            },
            
            // send couple of concurrent messages
            function (next) {
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
            },
            
            // kill service
            function (next) {
                workerService.kill();
                setTimeout(next, 500);
            },
            
            // send messages while service is down then restart the service
            function (next) {
                try {
                    assert.strictEqual(client.connected(), false);
                } catch (e) {
                    return setImmediate(next, e);
                }
                
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
                
                setTimeout(function () {
                    _createServiceWithWorker({
                        ip: ip,
                        port: sport,
                        secure: true,
                        serviceName: 'test_service',
                        key: sslKey,
                        cert: sslCert,
                        auth: auth
                    }, function (err, worker) {
                        if (err) {
                            return next(err);
                        }
                        workerService = worker;
                    });
                    
                }, 500);
            },
            
            // kill service
            function (next) {
                workerService.kill();
                setTimeout(next, 500);
            },
            // send messages while service is down then restart the service
            function (next) {
                try {
                    assert.strictEqual(client.connected(), false);
                } catch (e) {
                    return setImmediate(next, e);
                }
                
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
                
                setTimeout(function () {
                    _createServiceWithWorker({
                        ip: ip,
                        port: sport,
                        secure: true,
                        serviceName: 'test_service',
                        key: sslKey,
                        cert: sslCert,
                        auth: auth
                    }, function (err, worker) {
                        if (err) {
                            return next(err);
                        }
                        workerService = worker;
                    });
                    
                }, 500);
            },
            
            
            function (next) {
                try {
                    assert.strictEqual(client.connected(), true);
                    assert.strictEqual(_.keys(client.callbacks).length, 0);
                    assert.strictEqual(_.keys(client._onTheRoadMessages).length, 0);
                    assert.strictEqual(client.getQueue().length(), 0);
                    client.disconnect(1000, '', true);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], function (err) {
            client.removeAllListeners();
            client.disconnect(1000, '', true);
            workerService.kill();
            done(err);
        });
    });
    
    
    it('disconnect, messages not sent, get error for each message', function (done) {
        var sport = port+3;
        var workerService;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: sport
            },
            jwt: token,
            reconnectInterval: 2000,
            reconnectMaxAttempts: 5,
            validateFullMessage: false
        });
        
        
        async.series([
            function (next) {
                _createServiceWithWorker({
                    ip: ip,
                    port: sport,
                    secure: true,
                    serviceName: 'test_service',
                    key: sslKey,
                    cert: sslCert,
                    auth: auth
                }, function (err, worker) {
                    workerService = worker;
                    return next(err);
                });
            },
            
            // connect to service
            function (next) {
                client.connect(next);
            },
            
            // send a message with success
            function (next) {
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
            },
            
            // kill service
            function (next) {
                workerService.kill();
                setTimeout(next, 500);
            },
            
            // send messages while service is down , receive errors
            function (next) {
                function _sendMessages() {
                    var items = [4, 5, null, null, 6, 7, 8, null, null];
                    async.map(items, function (seq, cbItem) {
                        var m = new ProtocolMessage();
                        m.setMessage('ping');
                        m.setSeq(seq);
                        
                        client.sendMessage(m, function (err) {
                            
                            try {
                                assert.typeOf(err, 'Error');
                                assert.strictEqual(err.message, 'ERR_RECONNECTING_FAILED');
                                return cbItem(false);
                            } catch (e) {
                                return cbItem(e);
                            }
                        });
                    }, function (err, results) {
                        if (err) {
                            return next(err);
                        }
                        
                        try {
                            assert.strictEqual(client.connected(), false);
                            assert.strictEqual(_.keys(client.callbacks).length, 0);
                            assert.strictEqual(_.keys(client._onTheRoadMessages).length, 0);
                            assert.strictEqual(client.getQueue().length(), 0);
                            assert.strictEqual(results.length, items.length);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                        
                    });
                };
                
                _sendMessages();
                
            }
        ], function (err) {
            client.removeAllListeners();
            client.disconnect(1000, '', true);
            workerService.kill();
            done(err);
        });
    });
    
    it('disconnect, messages not sent, get error for each message, client configured to not reconnect', function (done) {
        var sport = port+4;
        var workerService;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: sport
            },
            jwt: token,
            reconnectInterval: 2000,
            reconnectMaxAttempts: 5,
            doNotReconnect: true,
            validateFullMessage: false
        });
        
        
        async.series([
            function (next) {
                _createServiceWithWorker({
                    ip: ip,
                    port: sport,
                    secure: true,
                    serviceName: 'test_service',
                    key: sslKey,
                    cert: sslCert,
                    auth: auth,
                    validateFullMessage: false
                }, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    workerService = worker;
                    return next();
                });
            },
            
            // connect to service
            function (next) {
                client.connect(next);
            },
            
            // send messages with success
            function (next) {
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
            },
            
            // kill service
            function (next) {
                workerService.kill();
                setTimeout(next, 500);
            },
            
            // send messages while service is down , receive errors
            function (next) {
                function _sendMessages() {
                    var items = [4, 5, null, null, 6, 7, 8, null, null];
                    async.map(items, function (seq, cb) {
                        var m = new ProtocolMessage();
                        m.setMessage('ping');
                        m.setSeq(seq);
                        
                        client.sendMessage(m, function (err) {
                            try {
                                assert.typeOf(err, 'Error');
                                assert.strictEqual(err.message, 'ERR_COMPLETELY_DISCONNECTED');
                                return cb(false);
                            } catch (e) {
                                return cb(e);
                            }
                        });
                    }, function (err, results) {
                        if (err) {
                            return next(err);
                        }
                        
                        try {
                            assert.strictEqual(client.connected(), false);
                            assert.strictEqual(_.keys(client.callbacks).length, 0);
                            assert.strictEqual(_.keys(client._onTheRoadMessages).length, 0);
                            assert.strictEqual(client.getQueue().length(), 0);
                            assert.strictEqual(results.length, items.length);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                        
                    });
                };
                
                
                _sendMessages();
                
            }
        ], function (err) {
            client.removeAllListeners();
            //client.disconnect(1000, '', true);
            workerService.kill();
            done(err);
        });
    });
    
    it('add messages in queue, disconnect, receive error for each queued message', function (done) {
        var sport = port+5;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: sport
            },
            jwt: token,
            reconnectInterval: 2000,
            reconnectMaxAttempts: 5,
            doNotReconnect: true,
            validateFullMessage: false
        });
        
        
        async.series([
            function (next) {
                _createServiceWithWorker({
                    ip: ip,
                    port: sport,
                    secure: true,
                    serviceName: 'test_service',
                    key: sslKey,
                    cert: sslCert,
                    auth: auth,
                    validateFullMessage: false
                }, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    workerService = worker;
                    return next();
                });
            },
            
            // connect to service
            function (next) {
                client.connect(next);
            },
            
            // send messages with success
            function (next) {
                _sendMessagesSuccess([1,2,3,null,null,null,6,9,23], client, next);
            },
            
            
            
            // pause queue, send messages, kill service
            function (next) {
                function _sendMessages() {
                    var items = [4, 5, null, null, 6, 7, 8, null, null];
                    async.map(items, function (seq, cb) {
                        var m = new ProtocolMessage();
                        m.setMessage('ping');
                        m.setSeq(seq);
                        
                        client.sendMessage(m, function (err) {
                            try {
                                assert.typeOf(err, 'Error');
                                assert.strictEqual(err.message, 'ERR_CONNECTION_CLOSED');
                                return cb(false);
                            } catch (e) {
                                return cb(e);
                            }
                        });
                    }, function (err, results) {
                        if (err) {
                            return next(err);
                        }
                        
                        try {
                            assert.strictEqual(client.connected(), false);
                            assert.strictEqual(_.keys(client.callbacks).length, 0);
                            assert.strictEqual(_.keys(client._onTheRoadMessages).length, 0);
                            assert.strictEqual(client.getQueue().length(), 0);
                            assert.strictEqual(results.length, items.length);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                        
                    });
                };
                
                client.getQueue().pause();
                _sendMessages();
                workerService.kill();
                
            }
        ], function (err) {
            done(err);
        });
    });
    
    after(function(done) {
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

function _sendMessagesSuccess(items, client, cb) {
    async.map(items, function (seq, cbItem) {
        var m = new ProtocolMessage();
        m.setMessage('ping');
        m.setSeq(seq);

        client.sendMessage(m, function (err, response) {
            try {
                if (seq === null) {
                    assert.ifError(err);
                    return cbItem(false, response);
                } else {
                    assert.ifError(err);
                    assert.isNull(response.getError());
                    assert.isNotNull(response.getAck());
                    assert.strictEqual(response.getAck()[0], seq);
                    return cbItem(false, response);
                }
            } catch (e) {
                return cbItem(e);
            }
        });
    }, function (err, results) {
        if (err) {
            return cb(err);
        }
        try {
            assert.strictEqual(results.length, items.length);
            return cb();
        } catch (e) {
            return cb(e);
        }
    });
};
