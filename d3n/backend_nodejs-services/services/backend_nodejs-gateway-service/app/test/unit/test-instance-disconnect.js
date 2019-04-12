var ip = process.env.HOST || 'localhost',
    port = process.env.PORT || 4000,    
    cp = require('child_process'),
    fs = require('fs'),
    async = require('async'),
    should = require('should'),
    assert = require('chai').assert,
    workerService = null,
    ServiceClient = require('nodejs-default-client'),
    Gateway = require('../lib/Gateway.js'),
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
    
    workers = []
;


//for(var i)

describe('Testing gateway communication', function() {
    
    it('should create service instances', function (done) {
        this.timeout(10000);
        
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
                        serviceName: 'test_service',
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
            
            workerService.on('message', function (message) {
                if(message.success === true) {
                    return cb(false);
                }

                return cb(new Error('error starting testing service'));
            });
            
        }, function (err) {
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
        this.timeout(10000);
        serviceClient.connect(done);
    });
    
    
    it('should receive wrong JSON', function (done) {
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: port
            }
        });
        client.connect(function (err) {
            assert.ifError(err);
            
            var message = new ProtocolMessage();
            message.setMessage('workorder/ping');
            message.setSeq(1);
            client.on('message', function (message) {
                assert.strictEqual(message.getMessage(), 'gateway/serviceDisconnect');
                //assert.strictEqual(message.getContent().message, 'ERR_GATEWAY_WRONG_JSON');
                done();
            });
            
            client.sendMessage(message, function () {
                
            });
            
            //client.connection.sendUTF('asdas asdasds');
        });
    });
    
    
    
    after(function() {
        try {
            for(var i=0; i<workers.length; i++) {
                workers[i].kill();
            }
        } catch (e) {
            
        }
    });
    
});