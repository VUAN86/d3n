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
            }            
        ],
        
        'workorder': [
            {
                ip: ip,
                port: port+5,
                secure: secure
            }         
        ],
        'testkill': [
            {
                ip: ip,
                port: port+7,
                secure: secure
            }
        ]
        
    },
    
    workers = [],
    serviceClients = [],
    workersWorkorder = [],
    workerServiceRegistry = null
;

serviceClients.push(serviceClient);

describe('TEST INSTANCE UNAVAILABLE', function() {
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
            //console.log('ping response', resMessage);
            assert.ifError(err);
            assert.strictEqual(resMessage.getError(), null);
            assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
            done();
        });
    });
    
    it('receive no instance available', function (done) {
        
        var instanceToRemove = {
            serviceName: 'question',
            ip: instances['question'][0].ip,
            port: instances['question'][0].port,
            uri: 'wss://' + instances['question'][0].ip + ':' + instances['question'][0].port
        };
        
        
        var publishInstanceUnavailableEvent = function (cb) {
            try {
                var eventServiceClient = new EventServiceClient(registryServiceURIs);
                
                eventServiceClient.publish(Constants.EVT_INSTANCE_UNAVAILABLE, {
                    serviceName: instanceToRemove.serviceName,
                    uri: instanceToRemove.uri
                }, cb);
            } catch (e) {
                return setImmediate(cb, e);
            }
            
        };
        
        var sendPingToService = function (cb) {
            try {
                var reqMessage = new ProtocolMessage();
                reqMessage.setMessage(instanceToRemove.serviceName + '/ping');
                reqMessage.setContent({data: 'pingdata_newService'});
                reqMessage.setSeq(1);
                reqMessage.setAck(null);
                reqMessage.setError(null);

                serviceClient.sendMessage(reqMessage, function (err, resMessage) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                        assert.isNotNull(resMessage.getError());
                        assert.deepEqual(resMessage.getError(), Errors.ERR_NO_INSTANCE_AVAILABLE);
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
                publishInstanceUnavailableEvent(next);
            },
            
            function (next) {
                // wait a bit
                setTimeout(next, 2000);
            },
            
            function (next) {
                sendPingToService(next);
            }
            
        ], function (err) {
            assert.ifError(err);
            return done(err);
        });
        
    });
    

    it('concurrent receive disconnect: from instance and from event service', function (done) {
        
        var instanceToRemove = {
            serviceName: 'workorder',
            ip: instances['workorder'][0].ip,
            port: instances['workorder'][0].port,
            uri: 'wss://' + instances['workorder'][0].ip + ':' + instances['workorder'][0].port
        };
        
        
        var publishInstanceUnavailableEventAndKillInstance = function (cb) {
            try {
                //var eventServiceClient = new EventServiceClient(registryServiceURIs);
                // kill instances
                workersWorkorder[0].kill();
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
        
        
        client.connect(function (err) {
            assert.ifError(err);
            
            
            
            
            
            // send a messsage, so become a client
            var reqMessage = new ProtocolMessage();
            reqMessage.setMessage('workorder/ping');
            reqMessage.setContent({data: 'pingdata_newService'});
            reqMessage.setSeq(1);
            reqMessage.setAck(null);
            reqMessage.setError(null);

            client.sendMessage(reqMessage, function (err, resMessage) {
                assert.ifError(err);
                assert.strictEqual(resMessage.getMessage(), reqMessage.getMessage() + 'Response');
                assert.isNull(resMessage.getError());
                
                // wait for disconnect message
                client.on('message', function (message) {
                    assert.strictEqual(message.getMessage(), 'gateway/instanceDisconnect');
                    done();
                });
                
                // make first instance unavailable 
                publishInstanceUnavailableEventAndKillInstance(function() {});
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
