var config = require('../../config/config.js'),
    Constants = require('../../config/constants.js'),
    ip = config.gateway.ip,
    port = config.gateway.port,
    secure = true,
    userId = 1,
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
    _ = require('lodash'),
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
//serviceClients.push(serviceClient);



var gateway;
var sinon = require('sinon');
var FakeProfileService = require('./classes/FakeProfileService.js');

var eventServiceClient = new EventServiceClient(registryServiceURIs);

function _invalidateToken (userId, newToken, cb) {
    AerospikeUserToken.create({userId: userId, token: newToken}, function (err) {
        return cb(err);
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
                    console.log('instance started');
                    return cb(false);
                }

                return cb(new Error('error starting testing service'));
            });
            
        }, function (err) {
            assert.ifError(err);
            return setTimeout(done, 1000);
        });
    });
    
        
    it('create gateway', function (done) {
        gateway = new Gateway({
            gateway: {
                ip: ip,
                port: port,
                secure: secure,
                key: __dirname + '/ssl-certificate/key.pem',
                cert: __dirname + '/ssl-certificate/cert.pem',
                auth: config.gateway.auth
            },
            registryServiceURIs: registryServiceURIs
        });
        
        gateway.build(done);
    });
    
    it('invalidate token', function (done) {
        var uid = '11-22-33';
        var userPayload = {
            userId: uid,
            iat: Date.now()-10000
        };
        
        var authPayload = {
            userId: uid,
            iat: Date.now()
        };
        
        var userPayloadNew = {
            userId: uid,
            iat: Date.now()+10000
        };
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var userTokenNew = jwtModule.sign(userPayloadNew, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var authToken = jwtModule.sign(authPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        var stub_getUserProfile = sinon.stub(gateway.messageDispatcher, '_getUserProfile', function (userId, cb) {
            return setImmediate(cb, false, {});
        });
        
        var clientId = 'clientid11111';
        var stub__createUUID = sinon.stub(gateway, '_createUUID', function () {
            return clientId;
        });
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        async.series([
            
            // remove token from aerospike
            function (next) {
                _removeUserToken(uid, next);
            },
            
            function (next) {
                _invalidateToken(uid, authToken, next);
            },
            
            function (next) {
                client.connect(next);
            },
            
            // send message without token, _getUserProfile not called, session token = null
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/ping');
                message.setSeq(1);
                message.setToken(null);
                
                client.sendMessage(message, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.isNull(resMessage.getError());
                        assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                        assert.strictEqual(stub_getUserProfile.callCount, 0);
                        assert.strictEqual(gateway.messageDispatcher._getOrCreateClientSession(clientId).getToken(), null);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send message with token, _getUserProfile called, session token = auth token
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/ping');
                message.setSeq(1);
                message.setToken(userToken);
                
                client.sendMessage(message, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.isNull(resMessage.getError());
                        assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                        assert.strictEqual(stub_getUserProfile.callCount, 1);
                        assert.strictEqual(gateway.messageDispatcher._getOrCreateClientSession(clientId).getToken(), authToken);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send message with new token, _getUserProfile called, session token = new token
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/ping');
                message.setSeq(1);
                message.setToken(userTokenNew);
                stub_getUserProfile.reset();
                client.sendMessage(message, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.isNull(resMessage.getError());
                        assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                        assert.strictEqual(stub_getUserProfile.callCount, 1);
                        assert.strictEqual(gateway.messageDispatcher._getOrCreateClientSession(clientId).getToken(), userTokenNew);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },
            
            // send message without token, _getUserProfile not called, session token = new token
            function (next) {
                var message = new ProtocolMessage();
                message.setMessage('testquestion/ping');
                message.setSeq(1);
                message.setToken(userTokenNew);
                stub_getUserProfile.reset();
                client.sendMessage(message, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.isNull(resMessage.getError());
                        assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');
                        assert.strictEqual(stub_getUserProfile.callCount, 0);
                        assert.strictEqual(gateway.messageDispatcher._getOrCreateClientSession(clientId).getToken(), userTokenNew);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            
            
        ], done);
        
    });
    
    after(function(done) {
        
        try {
            for(var i=0; i<serviceClients.length; i++) {
                try {
                    serviceClients[i].disconnect(1000, '', true);
                } catch (e) {}
            }
            
        } catch (e) {}
        
        
        
        try {
            for(var i=0; i<workers.length; i++) {
                try {
                    workers[i].kill();
                } catch (e) {}
            }
        } catch (e) {}
        
        try {
            gateway.shutdown();
        } catch (e) {}
        
        setTimeout(done, 1500);
    });
    
});

