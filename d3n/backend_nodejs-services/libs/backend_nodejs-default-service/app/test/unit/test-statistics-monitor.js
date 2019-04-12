var ip =  process.env.HOST || 'localhost';
var port = process.env.PORT ? parseInt(process.env.PORT) : 4200;
port = port+11;
var secure = true;
var registryServiceURIs = process.env.REGISTRY_SERVICE_URIS || 'wss://localhost:' + port;

var async = require('async');
var _ = require('lodash');
var nodeUrl = require('url');
var FakeRegistryService = require('./classes/FakeRegistryService.js');
var DefaultService = require('../../classes/Service.js');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();
var sinon = require('sinon');
var assert = require('chai').assert;
var fs = require('fs');
var ServiceRegistryClient = require('nodejs-service-registry-client');
var ServiceClient = require('nodejs-default-client');
//var serviceRegistryInstancesConfig = [];
var instancesConfig = [];

var serviceRegsitryClientConfig = [];
// service registry instances
_.each(registryServiceURIs.split(','), function (uri) {
    var hostConfig = nodeUrl.parse(uri);
    instancesConfig.push({
        ip: hostConfig.hostname,
        port: hostConfig.port,
        secure: hostConfig.protocol === 'wss:',
        serviceName: 'serviceRegistry',
        registryServiceURIs: '',

        class: FakeRegistryService
    });


    serviceRegsitryClientConfig.push({
        service: {
            ip: hostConfig.hostname,
            port: hostConfig.port
        },
        secure: hostConfig.protocol === 'wss:'
    });
});

// other instances
instancesConfig.push({
    ip: ip,
    port: port+1,
    secure: true,
    serviceName: 'mediaservice',
    registryServiceURIs: registryServiceURIs,

    class: DefaultService
});
instancesConfig.push({
    ip: ip,
    port: port+2,
    secure: true,
    serviceName: 'question',
    serviceNamespaces: ['question', 'workorder', 'application'],
    registryServiceURIs: registryServiceURIs,

    class: DefaultService
});


for(var i=0; i<instancesConfig.length; i++) {
    instancesConfig[i].key = fs.readFileSync(__dirname + '/ssl-certificate/key.pem', 'utf8');
    instancesConfig[i].cert = fs.readFileSync(__dirname + '/ssl-certificate/cert.pem', 'utf8');
    instancesConfig[i].auth = {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(__dirname + '/jwt-keys/pubkey.pem', 'utf8')
    };
    instancesConfig[i].validateFullMessage = false;
    instancesConfig[i].validatePermissions = false;
}

var serviceInstances = [];
var serviceRegistryClient = new ServiceRegistryClient(serviceRegsitryClientConfig);

var serviceInstancesCreated = [];
var sinonSandbox = null;
var questionService = null;
var questionServiceClient = new ServiceClient({
    secure: true,
    service: {
        ip: ip,
        port: port+2
    },
    validateFullMessage: false
});
var clients = [];
describe('TEST STATISTICS MONITOR', function() {
    this.timeout(20000);
    afterEach(function () {
        if (sinonSandbox) {
            sinonSandbox.restore();
        }
    });

    it('create service instances', function (done) {

        function createInstances(configs, cb) {
            async.mapSeries(configs, function (config, cbItem) {
                var cls = config.class;
                delete config.class;
                var inst = new cls(config);
                
                if (config.serviceName === 'question') {
                    questionService = inst;
                }
                
                inst.build(function (err) {
                    logger.debug('build err:', err);
                    if(!err) {
                        serviceInstancesCreated.push(inst);
                    }
                    cbItem(err);
                });
            }, cb);
        };
        createInstances(instancesConfig, function (err) {
            // wait a bit for namespace registration
            setTimeout(function () {
                done(err);
            }, 500);
        });
    });
    it('connections added/removed proper in _connections', function (done) {
        var qsPort = port+2;
        var client = new ServiceClient({
            secure: true,
            service: {
                ip: ip,
                port: qsPort
            },
            headers: {
                service_name: 'gateway'
            },
            validateFullMessage: false
        });
        clients.push(client);
        async.series([
            function (next) {
                try {
                    assert.strictEqual(questionService._connections.length, 0);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            function (next) {
                client.connect(next);
            },
            
            function (next) {
                try {
                    assert.strictEqual(questionService._connections.length, 1);
                    assert.strictEqual(questionService._connections[0].serviceName, 'gateway');
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            function (next) {
                client.disconnect(1000, '', true);
                setTimeout(next, 100);
            },
            function (next) {
                try {
                    assert.strictEqual(questionService._connections.length, 0);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], done);
    });

    it('connections added/removed proper in _connections, concurrency test', function (done) {
        var qsPort = port+2;
        var qsClients = [];
        async.series([
            function (next) {
                try {
                    assert.strictEqual(questionService._connections.length, 0);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            function (next) {
                async.map(_.range(1, 100), function (i, cbItem) {
                    var client = new ServiceClient({
                        secure: true,
                        service: {
                            ip: ip,
                            port: qsPort
                        },
                        headers: {
                            service_name: 'gateway'
                        },
                        validateFullMessage: false
                    });
                    qsClients.push(client);
                    client.connect(cbItem);
                }, next);
            },
            
            function (next) {
                try {
                    assert.strictEqual(questionService._connections.length, qsClients.length);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // disconnect
            function (next) {
                for(var i=0; i<qsClients.length; i++) {
                    qsClients[i].disconnect(1000, '', true);
                }
                setTimeout(next, 500);
            },
            function (next) {
                try {
                    assert.strictEqual(questionService._connections.length, 0);
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], done);
    });

    it('getStatistics', function (done) {
        var qsPort = port+2;
        var qsClients = [];
        async.series([
            function (next) {
                try {
                    questionService.getStatisticsMonitor().generateStatistics(function (err, statistics) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(statistics.countGatewayConnections, 0);
                            assert.strictEqual(statistics.countConnectionsToService, 0);
                            assert.strictEqual(statistics.countSessionsToService, 0);
                            
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            function (next) {
                async.map(_.range(0, 100), function (i, cbItem) {
                    var client = new ServiceClient({
                        secure: true,
                        service: {
                            ip: ip,
                            port: qsPort
                        },
                        headers: {
                            service_name: (i === 5 || i === 6 ? 'gateway' : 's' + i)
                        },
                        validateFullMessage: false
                    });
                    qsClients.push(client);
                    client.connect(cbItem);
                }, next);
            },
            
            // send message to create client sessions
            function (next) {
                async.map(qsClients.slice(20, 25), function (c, cbItem) {
                    var message = new ProtocolMessage();
                    message.setMessage('question/ping');
                    message.setSeq(1);
                    
                    c.sendMessage(message, function (err, response) {
                        return cbItem(err);
                    });
                }, next);
                
            },
            
            function (next) {
                try {
                    questionService.getStatisticsMonitor().generateStatistics(function (err, statistics) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(statistics.countGatewayConnections, 2);
                            assert.strictEqual(statistics.countConnectionsToService, qsClients.length);
                            assert.strictEqual(statistics.countSessionsToService, 5);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // disconnect
            function (next) {
                for(var i=0; i<qsClients.length; i++) {
                    qsClients[i].disconnect(1000, '', true);
                }
                setTimeout(next, 500);
            },
            function (next) {
                try {
                    questionService.getStatisticsMonitor().generateStatistics(function (err, statistics) {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(statistics.countGatewayConnections, 0);
                            assert.strictEqual(statistics.countConnectionsToService, 0);
                            assert.strictEqual(statistics.countSessionsToService, 0);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], done);
    });

    
    after(function(done) {

        serviceRegistryClient.disconnect();
        for(var i=0; i<clients.length; i++) {
            try {
                clients[i].disconnect(1000, '', true);
            } catch (e) {}
        }
        
        for(var i=0; i<serviceInstancesCreated.length; i++) {
            try {
                serviceInstancesCreated[i].shutdown(false);
            } catch (e) {}
        }

        return setTimeout(done, 500);
    });

});
