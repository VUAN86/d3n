var config =require('../../config/config.js'),
    ip = config.gateway.ip,
    port = config.gateway.port,   
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    Errors = require('nodejs-errors'),
    workerService = null,
    ServiceClient = require('nodejs-default-client'),
    Gateway = require('../../classes/Gateway.js'),
    serviceClient = new ServiceClient({
        secure: true,
        service: {
            ip: ip,
            port: port
        }
    }),
    token = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhIjoiYXNkIiwiYiI6MTIzLCJpYXQiOjE0NjU5MDQwMzB9.WGKKroIKmu4BZu_hWZc7AdUNj2ItUU1K-b7-OvC1AA4G6vE8nYkDpHoqdRazoRbPa6wXJY_WKEPpUrSNphBMDb3803vbKRloCr8WAt3ekrKeo1fiTiE1-AhGYAk3omu90sR4zrDiGxC2-8vlvnt5O7ZgjyEktIjNPmeW-hGrnrg_ST8903aLCGBq7pOxfiyDbtRalzoawiv_I1krQKYQ6FujZHM7y0486ZnJ8TnILTBuFn8YHEiVon63kUF6VzMclD_byS7d4vI0zybQnjjos_ueb8J_y8wvHn5Dm3y9MOrdNtda-OsTDG93jjDomWF-hr2HWS_3PHlM_gxKFlQxjA',
    ProtocolMessage = require('nodejs-protocol'),
    instances = {
        'question': [
            {
                ip: ip,
                port: port+2,
                secure: true
            },
            {
                ip: ip,
                port: port+3,
                secure: true
            }
        ],
        'workorder': [
            {
                ip: ip,
                port: port+4,
                secure: true
            }
        ]
    },
    
    workers = [],
    serviceClients = [],
    workersWorkorder = []
;
serviceClients.push(serviceClient);
describe('TEST WITH TOKENS', function() {
    this.timeout(20000);
    it('should create service instances', function (done) {
        var items = [];
        for(var k in instances) {
            for(var i=0; i<instances[k].length; i++) {
                var config = instances[k][i];
                config.serviceName = k;
                items.push(config);
            }
        }
        
        //console.log('items:', items); process.exit();
        
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
                        }
                    }
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
            //process.exit();
            assert.ifError(err);
            done();
        });
    });
    
    
    it('should create gateway', function (done) {
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
                serviceInstances: instances
            })
        ];
        var worker = cp.fork(__dirname + '/worker-create-gateway.js', args);

        workers.push(worker);

        worker.on('message', function (message) {
            if(message.success === true) {
                return done(false);
            }

            return done(new Error('error starting gateway'));
        });
        
    });
    
    it('should connect to gateway', function (done) {
        serviceClient.connect(done);
    });
    
    it('test null token', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/ping');
        reqMessage.setSeq(5);
        reqMessage.setToken(null);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            try {
                
                assert.ifError(err);
                assert.strictEqual(null, reqMessage.getToken());
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.isNull(resMessage.getError());
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('should receive not allowed', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/pingSecured');
        reqMessage.setSeq(5);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            assert.isNotNull(resMessage.getError());
            assert.strictEqual(resMessage.getError().message, 'ERR_NOT_ALLOWED');
            done();
        });
    });
    
    it('should receive allowed', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/pingSecured');
        reqMessage.setSeq(6);
        reqMessage.setToken(token);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            assert.strictEqual(resMessage.getError(), null);
            done();
        });
    });
    
    
    it('should receive invalid token', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/pingSecured');
        reqMessage.setSeq(5);
        reqMessage.setToken(token + 'asdas');
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            //console.log('resMessage:', resMessage);
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), 'global/authResponse');
            assert.isNotNull(resMessage.getError());
            assert.deepEqual(resMessage.getError(), Errors.ERR_TOKEN_NOT_VALID);
            done();
        });
    });
    
    it('should auto authenticate, series', function (done) {
        var items = [];
        items.push(new ProtocolMessage({
            message: 'workorder/ping',
            token: token,
            seq: 1,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'workorder/pingSecured',
            seq: 2,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'question/ping',
            seq: 3,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'question/pingSecured',
            seq: 4,
            content: null,
            ack: null,
            error: null
        }));
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        client.connect(function (err) {
            assert.ifError(err);
            
            async.mapSeries(items, function (item, cbItem) {
                client.sendMessage(item, function (err, message) {
                    if(err) {
                        return cbItem(err);
                    }
                    assert.strictEqual(message.getMessage(), item.getMessage() + 'Response');
                    assert.strictEqual(message.getError(), null);
                    
                    return cbItem(false);
                });
            }, function (err) {
                assert.ifError(err);
                done();
            });
        });
        
    });
    
    it('should auto authenticate, parallel', function (done) {
        var items = [];
        items.push(new ProtocolMessage({
            message: 'workorder/pingSecured',
            token: token,
            seq: 1,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'workorder/pingSecured',
            seq: 2,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'question/pingSecured',
            seq: 3,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'question/pingSecured',
            seq: 4,
            content: null,
            ack: null,
            error: null
        }));
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        client.connect(function (err) {
            assert.ifError(err);
            
            async.map(items, function (item, cbItem) {
                client.sendMessage(item, function (err, message) {
                    if(err) {
                        return cbItem(err);
                    }
                    
                    assert.strictEqual(message.getMessage(), item.getMessage() + 'Response');
                    assert.strictEqual(message.getError(), null);
                    
                    return cbItem(false);
                });
            }, function (err) {
                assert.ifError(err);
                done();
            });
        });
        
    });
    
    it('should simulate gateway message queue', function (done) {
        var items = [];
        items.push(new ProtocolMessage({
            message: 'workorder/pingSecured',
            token: token,
            seq: 1,
            content: {
                'TESTING_AUTH_WAIT': 3000
            },
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'workorder/pingSecured',
            seq: 2,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'question/pingSecured',
            seq: 3,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'question/pingSecured',
            seq: 4,
            content: null,
            ack: null,
            error: null
        }));
        
        items.push(new ProtocolMessage({
            message: 'question/pingSecured',
            seq: 5,
            content: null,
            ack: null,
            error: null
        }));
        
        var firstItem = items.splice(0, 1)[0];
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        client.connect(function (err) {
            assert.ifError(err);
            
            // send first message that contin token and auth wait parameter
            client.sendMessage(firstItem, function () {
            });
            
            // wait 1 second to be sure the first message has arrived at gateway
            setTimeout(function () {
                async.map(items, function (item, cbItem) {
                    client.sendMessage(item, function (err, message) {
                        if(err) {
                            return cbItem(err);
                        }

                        //console.log('>>>>message:', message);

                        assert.strictEqual(message.getMessage(), item.getMessage() + 'Response');
                        assert.strictEqual(message.getError(), null);

                        return cbItem(false);
                    });
                }, function (err) {
                    assert.ifError(err);
                    done();
                });
            }, 1000);
        });
        
    });
    
    
    it('should auto authenticate on second service', function (done) {
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        client.connect(function (err) {
            assert.ifError(err);
            var reqMessage = new ProtocolMessage();
            reqMessage.setMessage('question/pingSecured');
            reqMessage.setContent({data: 'for ping'});
            reqMessage.setSeq(2);
            reqMessage.setToken(token);
            reqMessage.setAck(null);
            reqMessage.setError(null);
            
            client.sendMessage(reqMessage, function (err, resMessage) {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.strictEqual(resMessage.getError(), null);
                
                var reqMessage2 = new ProtocolMessage();
                reqMessage2.setMessage('workorder/pingSecured');
                reqMessage2.setContent({data: 'for ping'});
                reqMessage2.setSeq(3);
                //reqMessage2.setToken(null);
                reqMessage2.setAck(null);
                reqMessage2.setError(null);
                
                client.sendMessage(reqMessage2, function (err, resMessage) {
                    assert.ifError(err);
                    assert.strictEqual(resMessage.getMessage(), reqMessage2.getMessage() + 'Response');
                    assert.strictEqual(resMessage.getError(), null);
                    done();
                });
            });
        });
        
    });
    
    it('call global/auth with invalid token, receive error', function (done) {
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        
        client.connect(function (err) {
            if (err) {
                return done(err);
            }
            var m = new ProtocolMessage();
            m.setMessage('global/auth');
            m.setSeq(123);
            m.setToken(token + 'asdaasd');
            client.sendMessage(m, function (err, response) {
                try {
                    assert.ifError(err);
                    assert.strictEqual(response.getMessage(), 'global/authResponse');
                    assert.isNotNull(response.getTimestamp());
                    assert.isNotNull(response.getError());
                    assert.deepEqual(response.getError(), Errors.ERR_TOKEN_NOT_VALID);
                    done();
                } catch (e) {
                    done(e);
                }
            });
        });
        
    });
    
    it('call global/auth with valid token, send messages with success', function (done) {
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        async.series([
            function (next) {
                client.connect(next);
            },
            
            // send global/auth message
            function (next) {
                var m = new ProtocolMessage();
                m.setMessage('global/auth');
                m.setSeq(123);
                m.setToken(token);
                client.sendMessage(m, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getMessage(), m.getMessage() + 'Response');
                        assert.isNotNull(response.getTimestamp());
                        assert.isNull(response.getError());
                        next();
                    } catch (e) {
                        next(e);
                    }
                });
            },
            
            // send messges without token, already authenticated
            function (next) {
                var items = [
                    {seq: 11, m: 'question/pingSecured'},
                    {seq: 21, m: 'workorder/pingSecured'}
                ];
                async.mapSeries(items, function (item, cbItem) {
                    var message = new ProtocolMessage();
                    message.setMessage(item.m);
                    message.setSeq(item.seq);

                    client.sendMessage(message, function (err, resMessage) {
                        assert.ifError(err);
                        assert.isNull(resMessage.getError());
                        assert.strictEqual(resMessage.getMessage(), message.getMessage() + 'Response');

                        return cbItem(err);
                    });
                }, next);
            }
        ], function (err) {
            done(err);
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