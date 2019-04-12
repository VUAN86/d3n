var config =require('../../config/config.js'),
    ip = config.gateway.ip,
    port = config.gateway.port,
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
    ServiceClient = require('nodejs-default-client'),
    Errors = require('nodejs-errors'),
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

describe('TEST WITHOUT TOKENS', function() {
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
                    cert: __dirname + '/ssl-certificate/cert.pem'
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
    
    
    it('should receive ping response success', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/ping');
        reqMessage.setContent({data: 'for ping'});
        reqMessage.setSeq(1);
        reqMessage.setAck(null);
        reqMessage.setError(null);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.isNull(resMessage.getError());
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            done();
        });
    });
    
    
    it('should receive wrong JSON', function (done) {
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
            client.on('message', function (message) {
                assert.strictEqual(message.getMessage(), 'global/errorResponse');
                assert.deepEqual(message.getError(), Errors.ERR_VALIDATION_FAILED);
                done();
            });

            client.connection.sendUTF('asdas asdasds');
        });
    });
    
    
    
    
    
    it('should receive message handler not found', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question/qqwwee');
        reqMessage.setContent(null);
        reqMessage.setSeq(1);
        reqMessage.setAck(null);
        reqMessage.setError(null);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            assert.isNotNull(resMessage.getError());
            assert.deepEqual(resMessage.getError(), Errors.ERR_FATAL_ERROR);
            done();
        });
    });
    
    
    it('should receive gateway service not found', function (done) {
        var reqMessage = new ProtocolMessage();
        reqMessage.setMessage('question_sdsdsfsd/ping');
        reqMessage.setContent(null);
        reqMessage.setSeq(1);
        reqMessage.setAck(null);
        reqMessage.setError(null);
        
        serviceClient.sendMessage(reqMessage, function (err, resMessage) {
            assert.ifError(err);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            assert.isNotNull(resMessage.getError());
            assert.deepEqual(resMessage.getError(), Errors.ERR_SERVICE_NOT_FOUND);
            done();
        });
    });
    
    it('send clientDisconnect message', function (done) {
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
            reqMessage.setMessage('question/ping');
            reqMessage.setContent({data: 'for ping'});
            reqMessage.setSeq(1);
            reqMessage.setAck(null);
            reqMessage.setError(null);
            
            client.sendMessage(reqMessage, function (err) {
                assert.ifError(err);
                
                client.disconnect(1000, '', true);
                
                done();
            });
        });
    });
    
    
    
    
    it('should send two concurrent messages', function (done) {
        
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
            var reqMessage1 = new ProtocolMessage();
            var reqMessage2 = new ProtocolMessage();
            reqMessage1.setMessage('question/ping');
            reqMessage1.setContent({data: 'for ping'});
            reqMessage1.setSeq(1);
            reqMessage1.setAck(null);
            reqMessage1.setError(null);

            reqMessage2.setMessage('workorder/ping');
            reqMessage2.setContent({data: 'for ping'});
            reqMessage2.setSeq(2);
            reqMessage2.setAck(null);
            reqMessage2.setError(null);

            async.parallel([
                function (next) {
                    client.sendMessage(reqMessage1, next);
                },

                function (next) {
                    client.sendMessage(reqMessage2, next);
                }

            ], function (err, responses) {
                var resMessage1 = responses[0];
                var resMessage2 = responses[1];

                assert.strictEqual(resMessage1.getMessage(), reqMessage1.getMessage() + 'Response');
                assert.strictEqual(resMessage2.getMessage(), reqMessage2.getMessage() + 'Response');

                assert.strictEqual(resMessage1.getError(), null);
                assert.strictEqual(resMessage2.getError(), null);
                assert.ifError(err);

                done();
            });
            
        });
    });
    
    
    it('receive instanceDisconnect message', function (done) {
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
            reqMessage.setMessage('workorder/ping');
            reqMessage.setContent({data: 'for ping'});
            reqMessage.setSeq(1);
            reqMessage.setAck(null);
            reqMessage.setError(null);
            
            client.sendMessage(reqMessage, function (err, resMessage) {
                assert.ifError(err);
                assert.isNull(resMessage.getError());
                
                client.on('message', function (message) {
                    assert.strictEqual(message.getMessage(), 'gateway/instanceDisconnect');
                    done();
                });
                
                for(var i=0; i<workersWorkorder.length; i++) {
                    workersWorkorder[i].kill();
                }
                
            });
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