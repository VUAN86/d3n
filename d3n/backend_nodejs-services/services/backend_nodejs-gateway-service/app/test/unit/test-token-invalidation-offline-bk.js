var config = require('../../config/config.js'),
    Constants = require('../../config/constants.js'),
    ip = config.gateway.ip,
    port = config.gateway.port,
    secure = true,
    userId = 1,
    _ = require('lodash'),
    registryServiceURIs = config.registryServiceURIs,
    jwtModule = require('jsonwebtoken'),
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
    url = require('url'),
    ServiceClient = require('nodejs-default-client'),
    logger = require('nodejs-logger')(),
    EventServiceClient = require('nodejs-event-service-client'),
    Gateway = require('../../classes/Gateway.js'),
    ProtocolMessage = require('nodejs-protocol'),
    KeyvalueService = require('nodejs-aerospike').getInstance().KeyvalueService,
    AerospikeUserToken = KeyvalueService.Models.AerospikeUserToken,
    instances = {
        'testquestion': [
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
            },
            {
                ip: ip,
                port: port+9,
                secure: secure
            }
            
            
        ]
    },
    
    workers = [],
    serviceClients = [],
    workersWorkorder = [],
    workerServiceRegistry = null
;

var eventServiceClient = new EventServiceClient(registryServiceURIs);

function _invalidateToken (userId, newToken, cb) {
    AerospikeUserToken.create({userId: userId, token: newToken}, function (err) {
        if (err) {
            return cb(err);
        }
        
        // dispach event
        eventServiceClient.publish(Constants.EVT_TOKEN_INVALIDATED, {
            token: newToken
        }, cb);
        
    });
};

function _removeUserToken (userId, cb) {
    AerospikeUserToken.remove({userId: userId}, function (err, res) {
        if (err) {
            if (err === 'ERR_ENTRY_NOT_FOUND') {
                return cb(false, res);
            } else {
                return cb(err);
            }
        }
        
        return cb(err, res);
    });
};


describe('TEST OFFLINE TOKEN INVALIDATION', function() {
    this.timeout(15000);
    
    it('create service instances', function (done) {
        this.timeout(20000);
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
                    },
                    protocolLogging: false
                })
            ];
            
            var workerService = cp.fork(__dirname + '/worker-create-service.js', args);
            
            workers.push(workerService);
            
            if(item.serviceName === 'workorder') {
                workersWorkorder.push(workerService);
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
                    secure: true,
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
    
    
    it('send outdated token, receive admin role needed error', function (done) {
        var uid = _.uniqueId();
        var userPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        
        var authPayload = {
            userId: uid,
            roles: ['manager'],
            iat: Date.now()
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var authToken = jwtModule.sign(authPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        
        async.series([
            function (next) {
                _removeUserToken(uid, next);
            },
            
            function (next) {
                client.connect(next);
            },
            
            // send a message having admin role
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(1);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            },
            
            // disconect
            function (next) {
                client.disconnect(1000, '', true);
                next();
            },
            
            // invalidate the token
            function (next) {
                _invalidateToken(uid, authToken, next);
            },
            
            // wait a bit for event propagation
            function (next) {
                setTimeout(function () {
                    return next(false);
                }, 2000);
            },
            
            // connect again
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(2);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNotNull(resMessage.getError());
                    assert.strictEqual(resMessage.getError().message, 'ERR_ADMIN_ROLE_NEEDED');
                    
                    return next(err);
                });
                
            }
            
            
        ], function (err) {
            _removeUserToken(uid, function () {
                return done(err);
            });
        });
        
    });
    
    it('invalidate but same roles, success', function (done) {
        var uid = _.uniqueId();
        var userPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        
        
        var authPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var authToken = jwtModule.sign(authPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        
        async.series([
            function (next) {
                _removeUserToken(uid, next);
            },
            
            
            function (next) {
                client.connect(next);
            },
            
            // send a message having admin role
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(1);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            },
            
            // disconect
            function (next) {
                client.disconnect(1000, '', true);
                next(false);
            },
            
            // invalidate the token
            function (next) {
                _invalidateToken(uid, authToken, next);
            },
            
            // wait a bit for event propagation
            function (next) {
                setTimeout(function () {
                    return next(false);
                }, 2000);
            },
            
            // connect again
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(2);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            }
            
            
        ], function (err) {
            _removeUserToken(uid, function () {
                return done(err);
            });
        });
        
    });
    
    it('invalidate, get new token, send message success', function (done) {
        var uid = _.uniqueId();
        var userPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        
        
        var authPayload = {
            userId: uid,
            roles: ['manager'],
            iat: Date.now()
        };
        
        var userPayloadNew = {
            userId: userId+2,
            roles: ['manager'],
            iat: Date.now()+10000
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var authToken = jwtModule.sign(authPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var userTokenNew = jwtModule.sign(userPayloadNew, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        
        async.series([
            function (next) {
                _removeUserToken(uid, next);
            },
            
            function (next) {
                client.connect(next);
            },
            
            // send a message having admin role
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(1);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            },
            
            // disconect
            function (next) {
                client.disconnect(1000, '', true);
                next(false);
            },
            
            // invalidate the token
            function (next) {
                _invalidateToken(uid, authToken, next);
            },
            
            // wait a bit for event propagation
            function (next) {
                setTimeout(function () {
                    return next(false);
                }, 2000);
            },
            
            // connect again
            function (next) {
                client.connect(next);
            },
            
            // error, need admin
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(3);
                message.setToken(userTokenNew);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNotNull(resMessage.getError());
                    assert.strictEqual(resMessage.getError().message, 'ERR_ADMIN_ROLE_NEEDED');
                    
                    return next(err);
                });
                
            },
            
            // success, has manager role
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needManagerRole');
                message.setSeq(4);
                message.setToken(userTokenNew);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            }
            
            
        ], function (err) {
            _removeUserToken(uid, function () {
                return done(err);
            });
        });
        
    });
    
    it('send message without token, send message with token, token invalidated, send message error', function (done) {
        var uid = _.uniqueId();
        var userPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        
        var authPayload = {
            userId: uid,
            roles: ['manager'],
            iat: Date.now()
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var authToken = jwtModule.sign(authPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        
        async.series([
            function (next) {
                _removeUserToken(uid, next);
            },
            
            function (next) {
                client.connect(next);
            },
            
            // send a message without token
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/ping');
                message.setSeq(1);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            },
            
            // send a message having admin role
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(5);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            },
            
            // disconect
            function (next) {
                client.disconnect(1000, '', true);
                next();
            },
            
            // invalidate the token
            function (next) {
                _invalidateToken(uid, authToken, next);
            },
            
            // wait a bit for event propagation
            function (next) {
                setTimeout(function () {
                    return next(false);
                }, 2000);
            },
            
            // connect again
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                async.mapSeries([2,3], function (seq, cbItem) {
                    var message = new ProtocolMessage();
                    message.setMessage('testquestion/needAdminRole');
                    message.setSeq(seq);
                    message.setToken(userToken);

                    client.sendMessage(message, function (err, resMessage) {
                        assert.ifError(err);
                        assert.isNotNull(resMessage.getError());
                        assert.strictEqual(resMessage.getError().message, 'ERR_ADMIN_ROLE_NEEDED');

                        return cbItem(err);
                    });
                }, next);
            }
            
        ], function (err) {
            _removeUserToken(uid, function () {
                return done(err);
            });
        });
        
    });
    
    
    it('success, invalidate, error, get new token, success', function (done) {
        var uid = _.uniqueId();
        var userPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        
        
        var authPayload = {
            userId: uid,
            roles: ['manager'],
            iat: Date.now()
        };
        
        var newTokenPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var authToken = jwtModule.sign(authPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        var newToken = jwtModule.sign(newTokenPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        
        async.series([
            function (next) {
                _removeUserToken(uid, next);
            },
            
            function (next) {
                client.connect(next);
            },
            
            // success, it auto-authenticate on workorder service
            function (next) {
                var items = [
                    {seq: 1, m: 'testquestion/needAdminRole', token: userToken},
                    {seq: 2, m: 'workorder/needAdminRole'}
                ];
                async.mapSeries(items, function (item, cbItem) {
                var message = new ProtocolMessage();
                    message.setMessage(item.m);
                    message.setSeq(item.seq);
                    if (item.token) {
                        message.setToken(userToken);
                    }

                    client.sendMessage(message, function (err, resMessage) {
                        assert.ifError(err);
                        assert.isNull(resMessage.getError());
                        assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');

                        return cbItem(err);
                    });
                }, next);
            },
            
            // disconnect , invalidate, connect
            function (next) {
                client.disconnect(1000, '', true);
                setTimeout(function () {
                    _invalidateToken(uid, authToken, function (err) {
                        if (err) {
                            return next(err);
                        }
                        
                        setTimeout(function () {
                            client.connect(next);
                        }, 1000);
                    });
                }, 500);
            },
            
            // error
            function (next) {
                var items = [
                    {seq: 1, m: 'testquestion/needAdminRole'},
                    {seq: 2, m: 'workorder/needAdminRole'}
                ];
                async.map(items, function (item, cbItem) {
                var message = new ProtocolMessage();
                    message.setMessage(item.m);
                    message.setSeq(item.seq);
                    message.setToken(userToken);

                    client.sendMessage(message, function (err, resMessage) {
                        assert.ifError(err);
                        assert.isNotNull(resMessage.getError());
                        assert.strictEqual(resMessage.getError().message, 'ERR_ADMIN_ROLE_NEEDED');

                        return cbItem(err);
                    });
                }, next);
            },
            
            // disconnect , invalidate, connect
            function (next) {
                client.disconnect(1000, '', true);
                setTimeout(function () {
                    _invalidateToken(uid, newToken, function (err) {
                        if (err) {
                            return next(err);
                        }
                        
                        setTimeout(function () {
                            client.connect(next);
                        }, 1000);
                    });
                }, 500);
            },
            
            
            // success
            function (next) {
                var items = [
                    {seq: 11, m: 'testquestion/needAdminRole'},
                    {seq: 12, m: 'workorder/needAdminRole'}
                ];
                async.map(items, function (item, cbItem) {
                var message = new ProtocolMessage();
                    message.setMessage(item.m);
                    message.setSeq(item.seq);
                    message.setToken(userToken);

                    client.sendMessage(message, function (err, resMessage) {
                        assert.ifError(err);
                        assert.isNull(resMessage.getError());
                        assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');

                        return cbItem(err);
                    });
                }, next);
            }
            
            
        ], function (err) {
            _removeUserToken(uid, function () {
                return done(err);
            });
        });
        
        
    });
    
    
    it('try multiple times to use an outdated token', function (done) {
        var uid = _.uniqueId();
        var userPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()-10000
        };
        
        var authPayload = {
            userId: uid,
            roles: ['manager'],
            iat: Date.now()
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var authToken = jwtModule.sign(authPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        
        async.series([
            function (next) {
                _removeUserToken(uid, next);
            },
            
            function (next) {
                client.connect(next);
            },
            
            // send a message having admin role
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/needAdminRole');
                message.setSeq(1);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    assert.ifError(err);
                    assert.isNull(resMessage.getError());
                    assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                    
                    return next(err);
                });
                
            },
            
            // disconect
            function (next) {
                client.disconnect(1000, '', true);
                next();
            },
            
            // invalidate the token
            function (next) {
                _invalidateToken(uid, authToken, next);
            },
            
            // wait a bit for event propagation
            function (next) {
                setTimeout(function () {
                    return next(false);
                }, 2000);
            },
            
            // connect again
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                async.map([2,3], function (seq, cbItem) {
                    var message = new ProtocolMessage();
                    message.setMessage('testquestion/needAdminRole');
                    message.setSeq(seq);
                    message.setToken(userToken);

                    client.sendMessage(message, function (err, resMessage) {
                        assert.ifError(err);
                        assert.isNotNull(resMessage.getError());
                        assert.strictEqual(resMessage.getError().message, 'ERR_ADMIN_ROLE_NEEDED');

                        return cbItem(err);
                    });
                }, next);
            },
            
            // disconect and connect again
            function (next) {
                client.disconnect(1000, '', true);
                
                client.connect(function (err) {
                    if (err) {
                        return next(err);
                    }
                    setTimeout(next, 1000);
                });
                
                
            },
            
            function (next) {
                async.map([9,12], function (seq, cbItem) {
                    var message = new ProtocolMessage();
                    message.setMessage('testquestion/needAdminRole');
                    message.setSeq(seq);
                    message.setToken(userToken);

                    client.sendMessage(message, function (err, resMessage) {
                        assert.ifError(err);
                        assert.isNotNull(resMessage.getError());
                        assert.strictEqual(resMessage.getError().message, 'ERR_ADMIN_ROLE_NEEDED');

                        return cbItem(err);
                    });
                }, next);
            }
            
            
        ], function (err) {
            _removeUserToken(uid, function () {
                return done(err);
            });
        });
        
    });
    
    after(function(done) {
        try {
            for(var i=0; i<serviceClients.length; i++) {
                if(serviceClients[i].connected()) {
                    serviceClients[i].disconnect(1000, '', true);
                }
            }
            
        } catch (e) {}
        
        try {
            for(var i=0; i<workers.length; i++) {
                workers[i].kill();
            }
        } catch (e) {}
        
        
        setTimeout(done, 1500);
        
    });
    
});