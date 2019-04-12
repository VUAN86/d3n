if (process.env.NEW_RELIC_LICENSE_KEY) {
    try {
    	global.serviceNewrelic = require('newrelic');
    } catch (e) {
    	console.log('Error loading newrelic:', e);
    }
}
var _ = require('lodash');
var async = require('async');
var express = require('express');
var yargs = require('yargs');
var path = require('path');
var Config = require('./app/config/config.js');
var Errors = require('./app/config/errors.js');
var Routes = require('./app/config/routes.js');
var Database = require('nodejs-database');
var Aerospike = require('nodejs-aerospike');
var DefaultService = require('nodejs-default-service');
var SchedulerService = require('./app/services/schedulerService.js');
var DatabasesStatisticsProvider = require('nodejs-statistics-providers')['databasesStatisticsProvider'];
var BackgroundProcessingService = require('nodejs-background-processing');
var ElasticSearch = require('./app/services/elasticService.js');
var logger = require('nodejs-logger')();

var AppConfigurationService = function (config) {
    var self = this;
    self._config = config ? config : Config;
    self._built = false;
    self._httpService = express();
    self._httpService.locals.tenantManagementConfiguration = self._config.tenantManager;
    self._config.requestListener = self._httpService;
    self._websocketService = new DefaultService(self._config);
    self._bpService = BackgroundProcessingService.getInstance(self._config);
    self._httpService.locals.clientInstances = {
        authServiceClientInstance: self._websocketService.getAuthService(),
        mediaServiceClientInstance: self._websocketService.getMediaService(),
        profileServiceClientInstance: self._websocketService.getProfileService(),
        userMessageServiceClientInstance: self._websocketService.getUserMessage(),
    };
    Routes({
        http: self._httpService, 
        ws: self._websocketService
    });
};

AppConfigurationService.prototype.build = function (callback) {
    var self = this;
    if (!self._built) {
        self._websocketService.build(function (err) {
            if (err) {
                return callback(err);
            }

            // start other services
            async.series([

                // start scheduler service, if enabled
                function (next) {
                    try {
                        if (!self._config.schedulerServiceEnabled) {
                            return setImmediate(next);
                        }
                        
                        var ip;
                        if (process.env.EXTERN_IP) {
                            ip = process.env.EXTERN_IP;
                        } else {
                            ip = self._config.ip;
                        }
                        
                        self._websocketService._schedulerService = new SchedulerService({
                            service: self._websocketService,
                            instanceId: ip + ':' + self._config.port
                        });
                        
                        self._websocketService._schedulerService.init(function (err) {
                            if (err) {
                                if (process.env.IGNORE_SCHEDULER_INIT_ERROR === 'true') {
                                    return next();
                                } else {
                                    return next(err);
                                }
                            }
                            return next();
                        });
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },

                // start statistic provider
                function (next) {
                    try {
                        var dbStatisticsProvider = new DatabasesStatisticsProvider({
                            mysqlInstance: Database.getInstance(),
                            aerospikeConnectionModel: Aerospike.getInstance().KeyvalueService.Models.AerospikeConnection,
                            elasticsearchService: ElasticSearch
                        });
                        
                        self._websocketService.getStatisticsMonitor().addProvider(dbStatisticsProvider);
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }

            ], function (err) {
                if (err) {
                    return callback(err, false);
                }
                
                // Server is built
                self._built = true;
                return callback(null, true);
            });
        });
    } else {
        return setImmediate(function (callback) {
            return callback(null, true);
        }, callback);
    }
}

AppConfigurationService.prototype.shutdown = function (callback) {
    var self = this;
    self._bpService.shutdown();
    self._websocketService.shutdown();
    delete self._httpService;
    delete self._websocketService;
    self._built = false;
    return setImmediate(function (callback) {
        return callback(null, true);
    }, callback);
}

AppConfigurationService.prototype.httpProvider = function () {
    return this._httpService;
}

AppConfigurationService.prototype.backgroundProcessingService = function () {
    return this._bpService;
}

module.exports = AppConfigurationService;

var argp = yargs
    .usage('$0 [options]')
    .options({
        build: {
            boolean: true,
            describe: 'Build app configuration service.'
        },
        buildFake: {
            boolean: true,
            describe: 'Build app configuration service with Fake services.'
        },
    }
);
var argv = argp.argv;

if (argv.build === true) {
    var service = new AppConfigurationService(Config);
    service.build(function (err) {
        if (err) {
            logger.error('Error building profile manager  service: ', Errors.QuestionApi.FatalError);
            process.exit(1);
        }
        logger.info('profile manager service started successfully');
    });
}
if (argv.buildFake === true) {
    var FakeEventService = require('./app/test/services/esc.fakeService.js');
    var FakeMediaService = require('./app/test/services/msc.fakeService.js');
    var FakeProfileService = require('./app/test/services/psc.fakeService.js');
    var FakeServiceRegistryService = require('./app/test/services/src.fakeService.js');
    var FakeUserMessageService = require('./app/test/services/umc.fakeService.js');
    var FakeAuthService = require('./app/test/services/auth.fakeService.js');
    var FakeVoucherService = require('./app/test/services/vsc.fakeService.js');
    var srFake = new FakeServiceRegistryService();
    var esFake = new FakeEventService();
    var msFake = new FakeMediaService();
    var psFake = new FakeProfileService();
    var umFake = new FakeUserMessageService();
    var auFake = new FakeAuthService();
    var vsFake = new FakeVoucherService();
    var service = new AppConfigurationService(Config);
    try {
        async.mapSeries([srFake, esFake, msFake, psFake, umFake, auFake, vsFake, service], function (svc, next) {
            svc.build(function (err) {
                return next(err);
            });
        }, function (err) {
            if (err) {
                logger.error('Error building profile manager service with Fake services: ', Errors.QuestionApi.FatalError);
                process.exit(1);
            }
            logger.info('profile manager service with Fake services started successfully');
        });
    } catch (ex) {
        logger.error('Error building profile manager service with Fake services: ', Errors.QuestionApi.FatalError);
        process.exit(1);
    }
}