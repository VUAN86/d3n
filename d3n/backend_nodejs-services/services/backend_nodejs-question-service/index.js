if (process.env.NEW_RELIC_LICENSE_KEY) {
    try {
    	global.serviceNewrelic = require('newrelic');
    } catch (e) {
    	console.log('Error loading newrelic:', e);
    }
}
var _ = require('lodash');
var async = require('async');
var yargs = require('yargs');
var path = require('path');
var Config = require('./app/config/config.js');
var Errors = require('./app/config/errors.js');
var Routes = require('./app/config/routes.js');
var Database = require('nodejs-database');
var DefaultService = require('nodejs-default-service');
var express = require('express');
var multer  = require('multer');
var WorkflowEventsHandler = require('./app/workflows/WorkflowEventsHandler.js');
var Aerospike = require('nodejs-aerospike');
var DatabasesStatisticsProvider = require('nodejs-statistics-providers')['databasesStatisticsProvider'];
var BackgroundProcessingService = require('nodejs-background-processing');
var logger = require('nodejs-logger')();

var QuestionService = function (config) {
    var self = this;
    self._config = config ? config : Config;
    self._express = express();
    self._httpUploadMiddleware = multer({ dest: self._config.httpUpload.tempFolder });
    self._config.requestListener = self._express;
    self._built = false;
    self._websocketService = new DefaultService(self._config);
    self._bpService = BackgroundProcessingService.getInstance(self._config);
    Routes({
        http: self._express,
        httpUploadMiddleware: self._httpUploadMiddleware,
        ws: self._websocketService
    });
};

QuestionService.prototype.build = function (callback) {
    var self = this;
    if (!self._built) {
        self._websocketService.build(function (err) {
            if (err) {
                return callback(err);
            }

            // start other services
            async.series([

                // start workflow
                function (next) {
                    try {
                        // workflow active by default
                        if (!_.has(process.env, 'WORKFLOW_ENABLED')) {
                            process.env.WORKFLOW_ENABLED = 'true';
                        }

                        // temporary fix. should be explicitly set by service config file
                        if (!_.has(process.env, 'WORKFLOW_EVENTS_HANDLER')) {
                            process.env.WORKFLOW_EVENTS_HANDLER = 'true';
                        }
                        
                        if (process.env.WORKFLOW_ENABLED === 'true' && process.env.WORKFLOW_EVENTS_HANDLER === 'true') {
                            WorkflowEventsHandler.start(self._websocketService, function (err) {
                                if (err) {
                                    logger.error('Error starting workflow events handler:', err);
                                    return next(err);
                                }
                                return next();
                            });
                        } else {
                            return setImmediate(next);
                        }
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                // db monitoring
                function (next) {
                    try {
                        var dbStatisticsProvider = new DatabasesStatisticsProvider({
                            mysqlInstance: Database.getInstance(),
                            aerospikeConnectionModel: Aerospike.getInstance().KeyvalueService.Models.AerospikeConnection
                        });
                        
                        self._websocketService.getStatisticsMonitor().addProvider(dbStatisticsProvider);
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }

            ], function () {
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

QuestionService.prototype.shutdown = function (callback) {
    var self = this;
    self._bpService.shutdown();
    self._websocketService.shutdown();
    delete self._express;
    delete self._httpUploadMiddleware;
    self._built = false;
    return setImmediate(function (callback) {
        return callback(null, true);
    }, callback);
}

QuestionService.prototype.httpProvider = function () {
    return this._express;
}

QuestionService.prototype.backgroundProcessingService = function () {
    return this._bpService;
}

module.exports = QuestionService;

var argp = yargs
    .usage('$0 [options]')
    .options({
        build: {
            boolean: true,
            describe: 'Build Question Template Factory service.'
        },
        buildFake: {
            boolean: true,
            describe: 'Build Question Template Factory service with Fake services.'
        },
    }
);
var argv = argp.argv;

if (argv.build === true) {
    var service = new QuestionService(Config);
    service.build(function (err) {
        if (err) {
            logger.error('Error building Question Factory service: ', Errors.QuestionApi.FatalError);
            process.exit(1);
        }
        logger.info('Question Factory service started successfully');
    });
}
if (argv.buildFake === true) {
    var FakeEventService = require('./app/test/services/esc.fakeService.js');
    var FakeProfileService = require('./app/test/services/psc.fakeService.js');
    var FakeServiceRegistryService = require('./app/test/services/src.fakeService.js');
    var FakeUserMessageService = require('./app/test/services/umc.fakeService.js');
    var srFake = new FakeServiceRegistryService();
    var esFake = new FakeEventService();
    var psFake = new FakeProfileService();
    var umFake = new FakeUserMessageService();
    var service = new QuestionService(Config);
    try {
        async.mapSeries([srFake, esFake, psFake, umFake, service], function (svc, next) {
            svc.build(function (err) {
                return next(err);
            });
        }, function (err) {
            if (err) {
                logger.error('Error building Question Factory service with Fake services: ', Errors.QuestionApi.FatalError);
                process.exit(1);
            }
            logger.info('Question Factory service with Fake services started successfully');
        });
    } catch (ex) {
        logger.error('Error building Question Factory service with Fake services: ', Errors.QuestionApi.FatalError);
        process.exit(1);
    }
}