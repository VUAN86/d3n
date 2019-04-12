var ip = process.env.HOST || 'localhost';
var port = parseInt(process.env.PORT) || 4000;
process.env.REGISTRY_SERVICE_URIS_DEVELOPMENT = 'wss://localhost:' + (port+1);
process.env.REGISTRY_SERVICE_URIS_NIGHTLY = 'wss://localhost:' + (port+2);
process.env.REGISTRY_SERVICE_URIS_STAGING = 'wss://localhost:' + (port+3);
process.env.REGISTRY_SERVICE_URIS_PRODUCTION = 'wss://localhost:' + (port+4);
process.env.REGISTRY_SERVICE_URIS_TESTING = 'wss://localhost:' + (port+5);
process.env.REGISTRY_SERVICE_URIS_NIGHTLY_TESTING = 'wss://localhost:' + (port+6);

var Config = require('../../../app/config/config.js');
var fs = require('fs');
var _ = require('lodash');
var async = require('async');
var assert = require('chai').assert;
var sinon = require('sinon');
var FakeRegistryService = require('../classes/FakeRegistryService.js');
var MonitoringService = require('../../../index.js');
var ProtocolMessage = require('nodejs-protocol');
var request = require('request');

var monitoringService;
describe('TESTS PULL STATISTICS', function () {
    it('build SRs for each environment', function (done) {
        var baseConfig = {
            ip: 'localhost',
            port: port,
            secure: true,
            serviceName: 'serviceRegistry',
            registryServiceURIs: '',

            key: fs.readFileSync(__dirname + '/../ssl-certificate/key.pem', 'utf8'),
            cert: fs.readFileSync(__dirname + '/../ssl-certificate/cert.pem', 'utf8'),
            auth: {
                algorithm: 'RS256',
                publicKey: fs.readFileSync(__dirname + '/../jwt-keys/pubkey.pem', 'utf8')
            },
            validateFullMessage: false,
            validatePermissions: false
        };
        
        
        async.mapSeries([1,2,3,4,5,6], function (idx, cbItem) {
            var config = _.assign({}, baseConfig);
            config.port = config.port+idx;
            
            var sr = new FakeRegistryService(config);
            
            sr.build(cbItem);
            
        }, done);
    });
    
    
    it('build monitoring service instance, pullStatistics', function (done) {
        
        monitoringService = new MonitoringService();
        
        var statistics = {
            serviceName: 'testservicename',
            uri: 'wss://localhost:1111',
            uptime:1234,
            memoryUsage: null,
            cpuUsage:222,
            countGatewayConnections: 3,
            countConnectionsToService: 5,
            countConnectionsFromService: 4,
            countSessionsToService: 20,
            lastHeartbeatTimestamp: 11122233,
            missingDependentServices: null,
            connectionsToDb: {
                mysql: 'OK',
                aerospike: 'NS',
                elastic: 'NA',
                spark: 'NOK'
            },
            connectionsFromService: [
                {serviceName: 'a', address: '1.2.3.4', port: 4},
                {serviceName: 'b', address: '1.2.3.5', port: 5}
            ],
            
            connectionsToService: []
        };
        
        var statistics2 = _.assign(_.assign({}, statistics), {
            serviceName: 'testservicename2',
            uri: 'wss://localhost:222'
        });
        
        var statistics3 = _.assign(_.assign({}, statistics), {
            serviceName: 'testservicename2',
            uri: 'wss://localhost:333',
            connectionsToDb: {
                mysql: 'OK',
                aerospike: 'NS',
                elastic: 'NA',
                spark: 'OK'
            },
            lastHeartbeatTimestamp: parseInt(Date.now()/1000)
        });
        
        var statistics4 = _.assign(_.assign({}, statistics), {
            serviceName: 'testservicename2',
            uri: 'wss://localhost:444',
            connectionsToDb: {
                mysql: 'OK',
                aerospike: 'NS',
                elastic: 'NA',
                spark: 'OK'
            },
            lastHeartbeatTimestamp: parseInt(Date.now()/1000)
        });
        var stub_getInfrastructureStatistics = sinon.stub(FakeRegistryService.prototype.messageHandlers, "getInfrastructureStatistics", function (message, clientSession) {
            var response = new ProtocolMessage(message);
            response.setContent({
                statisticsList: [statistics, statistics2, statistics3, statistics4]
            });
            clientSession.sendMessage(response);
        });
        
        
        async.series([
            function (next) {
                monitoringService.build(function (err) {
                    setTimeout(function () {
                        try {
                            assert.ifError(err);
                            assert.strictEqual(stub_getInfrastructureStatistics.callCount, 6);
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }, 500);
                });
            },
            
            // get statistics
            function (next) {
                monitoringService.getEnvStatistics('development', function (err, statistics) {
                    try {
                        assert.ifError(err);
                        assert.include(['OK', 'NOK'], statistics.environmentStatus);
                        assert.strictEqual(_.keys(statistics.environmentStatistics).length, 2);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            }
            
        ], done);
    });
    
    it('_createStatistics', function () {
        var statistics = {
            serviceName: 'testservicename',
            uri: 'wss://localhost:1111',
            uptime:1234,
            memoryUsage: null,
            cpuUsage:222,
            countGatewayConnections: 3,
            countConnectionsToService: 5,
            countConnectionsFromService: 4,
            countSessionsToService: 20,
            lastHeartbeatTimestamp: parseInt(Date.now()/1000),
            missingDependentServices: null,
            connectionsToDb: {
                mysql: 'OK',
                aerospike: 'NS',
                elastic: 'NA',
                spark: 'OK'
            },
            connectionsFromService: [
                {serviceName: 'a', address: '1.2.3.4', port: 4},
                {serviceName: 'b', address: '1.2.3.5', port: 5}
            ],
            
            connectionsToService: []
        };
        
        
        var res = monitoringService._createStatistics([statistics]);
        assert.strictEqual(res.environmentStatus, 'OK');
        
        //
        var statistics2 = _.assign(_.assign({}, statistics), {
            countGatewayConnections: 0
        });
        var res = monitoringService._createStatistics([statistics2]);
        assert.strictEqual(res.environmentStatus, 'NOK');
        
        //
        var statistics3 = _.assign(_.assign({}, statistics), {
            lastHeartbeatTimestamp: parseInt(Date.now()/1000)-70
        });
        var res = monitoringService._createStatistics([statistics3]);
        assert.strictEqual(res.environmentStatus, 'NOK');
        
        
        //
        var statistics4 = _.assign(_.assign({}, statistics), {
            connectionsToDb: {
                mysql: 'OK',
                aerospike: 'NS',
                elastic: 'NA',
                spark: 'NOK'
            }
        });
        var res = monitoringService._createStatistics([statistics4]);
        assert.strictEqual(res.environmentStatus, 'NOK');
        
        //
        var statistics5 = _.assign(_.assign({}, statistics), {
            connectionsToDb: {
                mysql: 'OK',
                aerospike: 'NS',
                elastic: 'NA',
                spark: 'OK'
            }
        });
        var res = monitoringService._createStatistics([statistics5]);
        assert.strictEqual(res.environmentStatus, 'OK');
        
        // no insance
        var res = monitoringService._createStatistics([statistics], ['testservicename', 'question']);
        assert.strictEqual(res.environmentStatus, 'NOK');
        
    });
    
});

describe('TESTS HTTP', function () {
    it('/ success', function (done) {
        var options = {
            method: 'GET',
            url: 'https://' + ip + ':' + port + '/',
            strictSSL: false,
            auth: {
                username: Config.httpAuth.username,
                password: Config.httpAuth.password
            }
        };
        
        async.mapSeries(_.keys(Config.envSegistryServiceURIs), function (env, cbItem) {
            var ops = _.assign(_.assign({}, options), {
                url: options.url + '?env=' + env
            });
            request(ops, function (err, httpResponse, body) {
                try {
                    assert.ifError(err);
                    assert.strictEqual(httpResponse.statusCode, 200);
                    assert.isString(body);
                    //console.log('>>>>body:', body);
                    return cbItem();
                } catch (e) {
                    return cbItem(e);
                }
            });
            
        }, done);
        
    });
    it('/ error wrong env', function (done) {
        var options = {
            method: 'GET',
            url: 'https://' + ip + ':' + port + '/?env=developmentasdadsad',
            strictSSL: false,
            auth: {
                username: Config.httpAuth.username,
                password: Config.httpAuth.password
            }
        };
        
        request(options, function (err, httpResponse, body) {
            try {
                assert.ifError(err);
                assert.strictEqual(httpResponse.statusCode, 500);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('/ error wrong username', function (done) {
        var options = {
            method: 'GET',
            url: 'https://' + ip + ':' + port + '/?env=development',
            strictSSL: false,
            auth: {
                username: Config.httpAuth.username + 'asdaasd',
                password: Config.httpAuth.password
            }
        };
        
        request(options, function (err, httpResponse, body) {
            try {
                assert.ifError(err);
                assert.strictEqual(httpResponse.statusCode, 401);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    it('/ error wrong password', function (done) {
        var options = {
            method: 'GET',
            url: 'https://' + ip + ':' + port + '/?env=development',
            strictSSL: false,
            auth: {
                username: Config.httpAuth.username,
                password: Config.httpAuth.password + 'asdfdfsd'
            }
        };
        
        request(options, function (err, httpResponse, body) {
            try {
                assert.ifError(err);
                assert.strictEqual(httpResponse.statusCode, 401);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
    it('load resources', function (done) {
        var options = {
            method: 'GET',
            url: 'https://' + ip + ':' + port + '/css/bootstrap.css',
            strictSSL: false,
            auth: {
                username: Config.httpAuth.username,
                password: Config.httpAuth.password
            }
        };
        
        request(options, function (err, httpResponse, body) {
            try {
                assert.ifError(err);
                assert.strictEqual(httpResponse.statusCode, 200);
                assert.isString(body);
                assert.isAbove(body.length, 200);
                return done();
            } catch (e) {
                return done(e);
            }
        });
    });
    
});