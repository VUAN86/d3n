if (process.env.NEW_RELIC_LICENSE_KEY) {
    try {
    	global.serviceNewrelic = require('newrelic');
    } catch (e) {
    	console.log('Error loading newrelic:', e);
    }
}
var _ = require('lodash');
var yargs = require('yargs');
var async = require('async');
var express = require('express');
var multer  = require('multer');
var Config = require('./app/config/config.js');
var Errors = require('./app/config/errors.js');
var Routes = require('./app/config/routes.js');
var Database = require('nodejs-database');
var Aerospike = require('nodejs-aerospike');
var DefaultWebsocketService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var DatabasesStatisticsProvider = require('nodejs-statistics-providers')['databasesStatisticsProvider'];

var MediaService = function (config) {
    var self = this;
    self._config = config ? config : Config;
    self._express = express();
    self._httpUploadMiddleware = multer({ dest: self._config.httpUpload.tempFolder });
    self._config.requestListener = self._express;
    self._built = false;
    self._websocketService = new DefaultWebsocketService(self._config);
    var clientInstances = {
        friendServiceClient: self._websocketService.getFriendService()
    };
    self._express.locals.clientInstances = clientInstances;
    Routes({
        http: self._express,
        httpUploadMiddleware: self._httpUploadMiddleware,
        ws: self._websocketService
    });
};

MediaService.prototype.build = function (callback) {
    var self = this;
    if (!self._built) {
        self._websocketService.build(function (err) {
            if (err) {
                return callback(err);
            }
            
            async.series([
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
            ], function (err) {
                if (err) {
                    return callback(err, false);
                }
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

MediaService.prototype.shutdown = function (callback) {
    var self = this;
    self._websocketService.shutdown();
    delete self._express;
    delete self._httpUploadMiddleware;
    delete self._websocketService;
    self._built = false;
    return setImmediate(function (callback) {
        return callback(null, true);
    }, callback);
}

MediaService.prototype.httpProvider = function () {
    return this._express;
}

module.exports = MediaService;

var argp = yargs
    .usage('$0 [options]')
    .options({
        build: {
            boolean: true,
            describe: 'Build NodeJS Media service.'
        },
        buildFake: {
            boolean: true,
            describe: 'Build app configuration service with Fake services.'
        },
    }
);
var argv = argp.argv;

if (argv.build === true) {
    var service = new MediaService(Config);
    service.build(function (err) {
        if (err) {
            logger.error('Error building NodeJS Media Service: ', Errors.MediaApi.FatalError);
            process.exit(1);
        }
        logger.info('NodeJS Media Service started successfully');
    }, service._express);
}

if (argv.buildFake === true) {
    var FakeFriendService = require('./app/test/services/frs.fakeService.js');
    var FakeServiceRegistryService = require('./app/test/services/src.fakeService.js');
    var srFake = new FakeServiceRegistryService();
    var frFake = new FakeFriendService();
    var service = new MediaService(Config);
    try {
        async.mapSeries([srFake, frFake, service], function (svc, next) {
            svc.build(function (err) {
                return next(err);
            });
        }, function (err) {
            if (err) {
                logger.error('Error building media manager with Fake services: ', Errors.MediaApi.FatalError);
                process.exit(1);
            }
            logger.info('media service with Fake services started successfully');
        });
    } catch (ex) {
        logger.error('Error building media service with Fake services: ', Errors.MediaApi.FatalError);
        process.exit(1);
    }
}