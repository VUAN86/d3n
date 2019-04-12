var config =require('../../config/config.js'),
    ip = config.gateway.ip,
    port = config.gateway.port,   
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    _ = require('lodash'),
    logger = require('nodejs-logger')(),
    jwtModule = require('jsonwebtoken'),
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
            },
            {
                ip: ip,
                port: port+5,
                secure: true
            }
        ],
        
        'mediaservice': [
            {
                ip: ip,
                port: port+6,
                secure: true
            },
            {
                ip: ip,
                port: port+7,
                secure: true
            }
        ]
    },
    
    workers = [],
    serviceClients = [],
    workersWorkorder = []
;

var allServices = _.keys(instances);
var allApis = ['echo', 'echo2'];
var allApisSecured = ['echoSecured', 'echo2Secured'];
var nrConcurrentMessages = 20;
var nrConcurentClients = 20;


serviceClients.push(serviceClient);
describe('TEST CONCURRENCY', function() {
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
    
    it('one client send ' + nrConcurrentMessages + ' concurrent messages to public apis => success', function (done) {
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        sendConcurrentMessages(client, nrConcurrentMessages, allServices, allApis, null, function (err) {
            assert.ifError(err);
            done(err);
        });
    });
    
    it('one client send ' + nrConcurrentMessages + ' concurrent messages to secured apis with token(token only for first message) => success', function (done) {
        
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        var uid = _.uniqueId();
        var userPayload = {
            userId: uid,
            roles: ['manager', 'admin'],
            iat: Date.now()
        };
        
        var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
        
        sendConcurrentMessages(client, nrConcurrentMessages, allServices, allApisSecured, userToken, function (err) {
            assert.ifError(err);
            done(err);
        });
    });
    
    it('one client send ' + nrConcurrentMessages + ' concurrent messages to secured api without token => error', function (done) {
        //return done();
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        serviceClients.push(client);
        
        sendConcurrentMessages_receiveNotAllowed(client, nrConcurrentMessages, allServices, allApisSecured, null, function (err) {
            assert.ifError(err);
            done(err);
        });
    });
    
    it(nrConcurentClients + ' concurrent clients send ' + nrConcurrentMessages + ' concurrent messages to public apis => success', function (done) {
        var clients = [];
        for (var i=1; i<=nrConcurentClients; i++) {
            var client = new ServiceClient({
                secure: true,
                service: {
                    ip: ip,
                    port: port
                }
            });
            
            clients.push(client);
            
            serviceClients.push(client);
        }
        
        
        async.map(clients, function (client, cbMap) {
            sendConcurrentMessages(client, nrConcurrentMessages, allServices, allApis, null, function (err) {
                return cbMap(err);
            });
            
        }, function (err) {
            assert.ifError(err);
            done(err);
        });
        
    });
    
    it(nrConcurentClients + ' concurrent clients send ' + nrConcurrentMessages + ' concurrent messages to secured apis with token(token only for first message) => success', function (done) {
        var items = [];
        for (var i=1; i<=nrConcurentClients; i++) {
            var client = new ServiceClient({
                secure: true,
                service: {
                    ip: ip,
                    port: port
                }
            });
            
            var uid = _.uniqueId();
            var userPayload = {
                userId: uid,
                roles: ['manager', 'admin'],
                iat: Date.now()
            };

            var userToken = jwtModule.sign(userPayload, config.gateway.auth.privateKey, {algorithm: config.gateway.auth.algorithm});
            
            
            items.push({client: client, token: userToken});
            
            serviceClients.push(client);
        }
        
        
        async.map(items, function (item, cbMap) {
            sendConcurrentMessages(item.client, nrConcurrentMessages, allServices, allApisSecured, item.token, function (err) {
                return cbMap(err);
            });
            
        }, function (err) {
            assert.ifError(err);
            done(err);
        });
        
    });
    
    it(nrConcurentClients + ' concurrent clients send ' + nrConcurrentMessages + ' concurrent messages to secured apis without token => error', function (done) {
        var items = [];
        for (var i=1; i<=nrConcurentClients; i++) {
            var client = new ServiceClient({
                secure: true,
                service: {
                    ip: ip,
                    port: port
                }
            });
            
            items.push({client: client, token: null});
            
            serviceClients.push(client);
        }
        
        
        async.map(items, function (item, cbMap) {
            sendConcurrentMessages_receiveNotAllowed(item.client, nrConcurrentMessages, allServices, allApisSecured, item.token, function (err) {
                return cbMap(err);
            });
            
        }, function (err) {
            assert.ifError(err);
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


function sendConcurrentMessages(client, nrMessages, services, apis, token, cb) {
    try {
        var items = [], tokenSent = false;
        for (var i=1; i<=nrMessages; i++) {
            var m = new ProtocolMessage();
            var service = _.sample(services);
            var api = _.sample(apis);
            
            m.setMessage(service + '/'  +api);
            m.setSeq(client.getSeq());
            
            m.setContent({
                data: 'data-' + _.uniqueId()
            });
            
            if (token && !tokenSent) {
                m.setToken(token);
                tokenSent = true;
            }
            
            items.push(m);
        }
        
        async.series([
            // connect client if needed
            function (next) {
               if (client.connected()) {
                   return setImmediate(next, false);
               }
               client.connect(next);
            },
           
            // send messages
            function (next) {
                async.map(items, function (item, cbMap) {
                    client.sendMessage(item, function (err, resMessage) {
                        try {
                            if (err) {
                                return cbMap(err);
                            }
                            
                            //logger.debug('concurrent response, sent message=' + resMessage.getMessage() + ', sent content.data=' + item.getContent().data + ' , received content.data=' + resMessage.getContent().data + ', sent token=' + (item.getToken() ? 'true' : 'false'));
                            if (resMessage.getError()) {
                                //console.log('>>>>>>>here', resMessage.getError());
                            }
                             
                            assert.isNull(resMessage.getError());
                            assert.strictEqual(resMessage.getMessage(), item.getMessage() + 'Response');
                            assert.strictEqual(resMessage.getContent().data, item.getContent().data);

                            return cbMap(false);
                        } catch (e) {
                            return cbMap(e);
                        }
                    });
                }, function (err) {
                    return next(err);
                });
            }
        ], function (err) {
            return cb(err);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

function sendConcurrentMessages_receiveNotAllowed(client, nrMessages, services, apis, token, cb) {
    try {
        var items = [], tokenSent = false;
        for (var i=1; i<=nrMessages; i++) {
            var m = new ProtocolMessage();
            var service = _.sample(services);
            var api = _.sample(apis);
            
            m.setMessage(service + '/'  +api);
            m.setSeq(client.getSeq());
            
            m.setContent({
                data: 'data-' + _.uniqueId()
            });
            
            if (token && !tokenSent) {
                m.setToken(token);
                tokenSent = true;
            }
            
            items.push(m);
        }
        
        async.series([
            // connect client if needed
            function (next) {
               if (client.connected()) {
                   return setImmediate(next, false);
               }
               client.connect(next);
            },
           
            // send messages
            function (next) {
                async.map(items, function (item, cbMap) {
                    client.sendMessage(item, function (err, resMessage) {
                        try {
                            if (err) {
                                return cbMap(err);
                            }
                            
                            //logger.debug('concurrent response errors, sent message=' + resMessage.getMessage() + ', sent content.data=' + item.getContent().data + ' , received content=' + resMessage.getContent() + ', sent token=' + (item.getToken() ? 'true' : 'false') + ', error=' + resMessage.getError().message);
                            
                             
                            assert.strictEqual(resMessage.getMessage(), item.getMessage() + 'Response');
                            assert.isNotNull(resMessage.getError());
                            assert.strictEqual(resMessage.getError().message, 'ERR_NOT_ALLOWED');

                            return cbMap(false);
                        } catch (e) {
                            return cbMap(e);
                        }
                    });
                }, function (err) {
                    return next(err);
                });
            }
        ], function (err) {
            return cb(err);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};