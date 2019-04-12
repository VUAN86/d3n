var _ = require('lodash');
var yargs = require('yargs');
var async = require('async');
var express = require('express');
var bodyParser = require('body-parser');
var Config = require('./app/config/config.js');
//var Errors = require('./app/config/errors.js');
var Routes = require('./app/config/routes.js');
var DefaultWebsocketService = require('nodejs-default-service');
var Datastore = require('nedb');
var ServiceRegistryClient = require('nodejs-service-registry-client');
var httpAuth = require('http-auth');
var logger = require('nodejs-logger')();

var MonitoringService = function (config) {
    var self = this;
    
    self._built = false;
    self._config = config ? config : Config;
    self._express = express();
    
    /* configure express */
    
    // static files
    self._express.use(express.static(__dirname + '/app/resources/'));
    
    // redirect http to https
    self._express.use(function(req, res, next) {
        if (!req.secure) {
            return res.redirect('https://' + req.headers.host + req.url);
        }
        next();
    });  
    
    // body parser
    self._express.use(bodyParser.json());
    self._express.use(bodyParser.urlencoded({ extended: true }));
    
    // http auth
    var basic = httpAuth.basic({
        realm: 'Monitoring Area'
    }, function (u, p, cb) {
        return cb(u === Config.httpAuth.username && p === Config.httpAuth.password);
    });
    self._express.use(httpAuth.connect(basic));
    
    
    self._config.requestListener = self._express;
    self._config.sendStatistics = false;
    
    self._pullStatisticsInterval = self._config.pullStatisticsInterval || 10000;
    
    self._websocketService = new DefaultWebsocketService(self._config);
    
    // create DB 
    self._dbStatistics = new Datastore({
        filename: self._config.dbStatisticsFile,
        timestampData: true
    });
    
    self._dbStatistics.persistence.setAutocompactionInterval(1000*60);
    
    // build SR client for each environment
    self._srClients = {};
    for(var k in self._config.envSegistryServiceURIs) {
        self._srClients[k] = new ServiceRegistryClient({
            registryServiceURIs: self._config.envSegistryServiceURIs[k],
            ownerServiceName: 'monitoring'
        });
    }
    
    Routes({
        http: self._express,
        self: this
    });
};

MonitoringService.prototype.build = function (callback) {
    var self = this;
    if (!self._built) {
        self._websocketService.build(function (err) {
            if (err) {
                return callback(err);
            }
            
            async.series([
                // load DB
                function (next) {
                    try {
                        self._dbStatistics.loadDatabase(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                // pull statistics
                function (next) {
                    try {
                        self.pullStatistics();
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], function (err) {
                if (err) {
                    return callback(err, false);
                }
                self._built = true;
                return callback(null, true);
            });
        });
    } else {
        return setImmediate(callback, null, true);
    }
};


MonitoringService.prototype.pullStatistics = function () {
    logger.debug('MonitoringService.pullStatistics() called');
    
    var self = this;
    
    function _deleteOldStatistics (env, cbDelete) {
        try {
            self._dbStatistics
                .find({environment: env}, {createdAt: 1})
                .sort({createdAt: -1})
                .skip(2).limit(1)
                .exec(function (err, docs) {
                if (err) {
                    return cbDelete(err);
                }
                
                //logger.debug('_deleteOldStatistics() doc:', docs, env);
                
                if (!docs.length) { // nothing to delete
                    return cbDelete();
                }
                
                var doc = docs[0];
                
                var qry = {
                    environment: env,
                    createdAt: {$lte: doc.createdAt}
                };
                self._dbStatistics.remove(qry, {multi: true}, function (err, numRemoved) {
                    if (err) {
                        return cbDelete(err);
                    }
                    
                    logger.debug('_deleteOldStatistics() numRemoved:', numRemoved, env);
                    return cbDelete();
                });
                
            });
        } catch (e) {
            return setImmediate(cbDelete, e);
        }
    };
    
    function _doPullStatistics(cbPullStatistics) {
        try {
            var cbCalled = false;
            var timeoutId;
            function callCb(err, data) {
                if (cbCalled === false) {
                    cbCalled = true;
                    clearTimeout(timeoutId);
                    cbPullStatistics(err, data);
                }
            };
            
            timeoutId = setTimeout(function () {
                callCb(new Error('ERR_TIMED_OUT'));
            }, 20000);
            
            // pull statistics from all environments
            async.map(_.keys(self._config.envSegistryServiceURIs), function (env, cbItem) {
                
                self._srClients[env].getInfrastructureStatistics(function (err, statisticsList) {
                    if (err) {
                        return cbItem(err);
                    }


                    self._dbStatistics.insert({
                        environment: env,
                        statisticsList: statisticsList
                    }, function (err) {
                        if (err) {
                            return cbItem(err);
                        }
                        
                        _deleteOldStatistics(env, cbItem);
                    });

                });
                
            }, function (err) {
                callCb(err);
            });
            
            
        } catch (e) {
            return setImmediate(callCb, e);
        }
    };
    
    _doPullStatistics(function (err, statisticsPull) {
        if (err) {
            logger.error('MonitoringService.pullStatistics() error:', err);
        } else {
            logger.debug('MonitoringService.pullStatistics() success:', statisticsPull);
        }
        
        setTimeout(self.pullStatistics.bind(self), self._pullStatisticsInterval);
    });
    
};

MonitoringService.prototype._createStatistics = function (statisticsList, _services) {
    
    function _setInstancesStatus(serviceInstances) {
        
        var tsNow = parseInt(Date.now()/1000);
        for(var serviceName in serviceInstances) {
            var instances = serviceInstances[serviceName];

            for(var i=0; i<instances.length; i++) {
                var inst = instances[i];
                
                inst.status = 'OK';
                inst.notOkReasons = [];
                
                // check gateway connection
                if (serviceName !== 'gateway' && !inst.countGatewayConnections) {
                    inst.status = 'NOK';
                    inst.notOkReasons.push('NO_CONNECTION_TO_GATEWAY');
                }


                // heartbeat
                if (inst.lastHeartbeatTimestamp !== null && (tsNow-inst.lastHeartbeatTimestamp) > 60) {
                    inst.status = 'NOK';
                    inst.notOkReasons.push('HEARTBEAT');
                }

                // DB 
                var connectionsToDb = inst.connectionsToDb;
                for(var db in connectionsToDb) {
                    if (connectionsToDb[db] === 'NOK') {
                        inst.status = 'NOK';
                        inst.notOkReasons.push('DB_' + db + '_NOK');
                    }
                }
            }
        }
    }
    
    
    // group by service name
    var serviceInstances = {};
    for(var i=0; i<statisticsList.length; i++) {
        var inst = statisticsList[i];
        var serviceName = inst.serviceName;
        if (_.isUndefined(serviceInstances[serviceName])) {
            serviceInstances[serviceName] = [];
        }

        serviceInstances[serviceName].push(inst);
    }
    
    // add services with no instance
    var services = _services || Config.services;
    for(var i=0; i<services.length; i++) {
        var sn = services[i];
        if (_.isUndefined(serviceInstances[sn])) {
            serviceInstances[sn] = [];
        }
    }


    _setInstancesStatus(serviceInstances);
    
    // set environment status
    var environmentStatus = 'OK';
    
    firstFor:
    for(var sn in serviceInstances) {
        var instances = serviceInstances[sn];
        
        if (!instances.length) {
            environmentStatus = 'NOK';
            break firstFor;
        }
        
        for(var i=0; i<instances.length; i++) {
            if (instances[i].status === 'NOK') {
                environmentStatus = 'NOK';
                break firstFor;
            }
        }
    }

    return {
        environmentStatus: environmentStatus,
        environmentStatistics: serviceInstances
    };
};

MonitoringService.prototype.getEnvStatistics = function (env, cb) {
    try {
        var self = this;
        
        self._dbStatistics
            .find({environment: env})
            .sort({createdAt: -1})
            .limit(1)
            .exec(function (err, docs) {
            
            try {
                if (err) {
                    return cb(err);
                }

                if (!docs.length) {
                    return cb(new Error('NO_STATISTICS'));
                }
                
                var ret = self._createStatistics(docs[0].statisticsList);
                
                return cb(false, ret);
                
            } catch (e) {
                return cb(e);
            }            
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

MonitoringService.prototype.shutdown = function (callback) {
    try {
        var self = this;
        self._websocketService.shutdown();
        delete self._express;
        delete self._websocketService;
        self._built = false;
        return setImmediate(callback, null, true);
    } catch (e) {
        return setImmediate(callback, e);
    }
};

MonitoringService.prototype.httpProvider = function () {
    return this._express;
};

module.exports = MonitoringService;

var argp = yargs
    .usage('$0 [options]')
    .options({
    build: {
        boolean: true,
        describe: 'Build NodeJS Media service.'
    }
});
var argv = argp.argv;

if (argv.build === true) {
    var service = new MonitoringService(Config);
    service.build(function (err) {
        if (err) {
            logger.error('Error building Monitoring Service: ', err);
            process.exit(1);
        }
        logger.info('Monitoring Service started successfully');
    }, service._express);
}
