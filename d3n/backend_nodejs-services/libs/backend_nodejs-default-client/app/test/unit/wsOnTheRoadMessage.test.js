var ip = process.env.HOST || 'localhost',
    port = process.env.PORT || 4001,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
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
var helperCreateService = require('../helpers/create-service.js');
var workers = [];
var clients = [];

describe('Tests messages on the road', function() {
    this.timeout(15000);
    before(function(done) {
        helperCreateService.createServiceWithWorker({
            service: {
                ip: ip,
                port: port,
                secure: true,
                serviceName: 'test_service',
                key: sslKey,
                cert: sslCert,
                auth: auth,
                validateFullMessage: false
            },
            workerScript: __dirname + '/worker-create-service.js'
        }, function (err, worker) {
            if (err) {
                return done(err);
            }
            workerService = worker;
            workers.push(worker);
            return done();
        });
    });
    
    it('send messages success', function (done) {
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            },
            jwt: token,
            reconnectInterval: 2000,
            reconnectMaxAttempts: 5,
            validateFullMessage: false
        });
        
        clients.push(client);
        
        client.connect(function (err) {
            if (err) {
                return done(err);
            }
            
            var items = [
                {message: 'test_service/ping', seq: 1},
                {message: 'test_service/ping', seq: 2},
                {message: 'test_service/ping', seq: 3},
                {message: 'test_service/ping', seq: 4}
            ];

            helperCreateService.sendMessagesSuccess(items, client, function (err) {
                if (err) {
                    console.log('err:', err);
                    return done(err);
                }
                
                return done();
            });
            
        });
        
    });
    
    it('messages on the road, kill service, receive error for on the road messages', function (done) {
        var sport = port+3;
        var serviceWorker;
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: sport
            },
            jwt: token,
            reconnectInterval: 2000,
            reconnectMaxAttempts: 5,
            validateFullMessage: false,
            doNotReconnect: true
        });
        clients.push(client);
        
        
        async.series([
            function (next) {
                helperCreateService.createServiceWithWorker({
                    service: {
                        ip: ip,
                        port: sport,
                        secure: true,
                        serviceName: 'test_service2',
                        key: sslKey,
                        cert: sslCert,
                        auth: auth,
                        validateFullMessage: false
                    },
                    workerScript: __dirname + '/worker-create-service.js'
                }, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    serviceWorker = worker;
                    workers.push(worker);
                    return next();
                });
            },
            
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                var items = [
                    {message: 'test_service2/pingSleep', seq: 1, content: null, error: null, ack: null},
                    {message: 'test_service2/ping', seq: 2, content: null, error: null, ack: null},
                    {message: 'test_service2/ping', seq: 3, content: null, error: null, ack: null},
                    {message: 'test_service2/pingSleep', seq: 4, content: null, error: null, ack: null},
                    {message: 'test_service2/pingSleep', seq: 5, content: null, error: null, ack: null}
                ];

                async.map(items, function (item, cbItem) {
                    var m = new ProtocolMessage(item);
                    client.sendMessage(m, function (err, response) {
                        if (m.getMessageName() === 'ping') {
                            try {
                                assert.ifError(err);
                                assert.isNull(response.getError());
                                assert.strictEqual(response.getMessage(), m.getMessage() + 'Response');
                                cbItem(false);
                            } catch (e) {
                                return cbItem(e);
                            }
                        } else {
                            try {
                                assert.typeOf(err, 'Error');
                                assert.strictEqual(err.message, 'ERR_CONNECTION_CLOSED');
                                cbItem(false);
                            } catch (e) {
                                return cbItem(e);
                            }
                        }
                    });
                }, function (err, results) {
                    setTimeout(function () {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(items.length, results.length);
                            assert.strictEqual(_.keys(client._onTheRoadMessages).length, 0);
                            assert.strictEqual(_.keys(client.callbacks).length, 0);
                            next();
                        } catch (e) {
                            next(e);
                        }
                    }, 500);
                });

                setTimeout(function () {
                    serviceWorker.kill();
                }, 500);
            }
        ], done);
        
    });
    
    it('messages on the road, kill service, receive error for on the road messages(test with reconnection)', function (done) {
        //return done();
        var sport = port+4;
        var serviceWorker;
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: sport
            },
            jwt: token,
            reconnectInterval: 500,
            reconnectMaxAttempts: 5,
            validateFullMessage: false
        });
        clients.push(client);
        
        
        async.series([
            function (next) {
                helperCreateService.createServiceWithWorker({
                    service: {
                        ip: ip,
                        port: sport,
                        secure: true,
                        serviceName: 'test_service3',
                        key: sslKey,
                        cert: sslCert,
                        auth: auth,
                        validateFullMessage: false
                    },
                    workerScript: __dirname + '/worker-create-service.js'
                }, function (err, worker) {
                    if (err) {
                        return next(err);
                    }
                    serviceWorker = worker;
                    workers.push(worker);
                    return next();
                });
            },
            
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                var items = [
                    {message: 'test_service3/pingSleep', seq: 1, content: null, error: null, ack: null},
                    {message: 'test_service3/ping', seq: 2, content: null, error: null, ack: null},
                    {message: 'test_service3/ping', seq: 3, content: null, error: null, ack: null},
                    {message: 'test_service3/pingSleep', seq: 4, content: null, error: null, ack: null},
                    {message: 'test_service3/pingSleep', seq: 5, content: null, error: null, ack: null}
                ];

                async.map(items, function (item, cbItem) {
                    var m = new ProtocolMessage(item);
                    client.sendMessage(m, function (err, response) {
                        if (m.getMessageName() === 'ping') {
                            try {
                                assert.ifError(err);
                                assert.isNull(response.getError());
                                assert.strictEqual(response.getMessage(), m.getMessage() + 'Response');
                                cbItem(false);
                            } catch (e) {
                                return cbItem(e);
                            }
                        } else {
                            try {
                                assert.typeOf(err, 'Error');
                                assert.strictEqual(err.message, 'ERR_RECONNECTING_FAILED');
                                cbItem(false);
                            } catch (e) {
                                return cbItem(e);
                            }
                        }
                    });
                }, function (err, results) {
                    setTimeout(function () {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(items.length, results.length);
                            assert.strictEqual(_.keys(client._onTheRoadMessages).length, 0);
                            assert.strictEqual(_.keys(client.callbacks).length, 0);
                            next();
                        } catch (e) {
                            next(e);
                        }
                    }, 500);
                });

                setTimeout(function () {
                    serviceWorker.kill();
                }, 500);
            }
        ], done);
        
    });
    after(function(done) {
        helperCreateService.kill(clients, [], workers);
        setTimeout(done, 500);
    });

});



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
