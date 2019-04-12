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
var express = require('express');
var bodyParser = require('body-parser');
var Config = require('./app/config/config.js');
var Errors = require('./app/config/errors.js');
var Routes = require('./app/config/routes.js');
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var Aerospike = require('nodejs-aerospike');
var DatabasesStatisticsProvider = require('nodejs-statistics-providers')['databasesStatisticsProvider'];

var AuthService = function (config) {
    var self = this;
    self._config = config ? config : Config;
    self._express = express();
    self._express.use(bodyParser.json());
    self._express.use(bodyParser.urlencoded({ extended: true }));
    self._config.requestListener = self._express;
    self._built = false;
    Routes({ http: self._express });
    self._websocketService = new DefaultService(self._config);
    Routes({ ws: self._websocketService });
}

AuthService.prototype.build = function (callback) {
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

AuthService.prototype.shutdown = function (callback) {
    var self = this;
    self._websocketService.shutdown();
    delete self._express;
    delete self._websocketService;
    self._built = false;
    if (callback) {
        return callback(null, true);
    }
    return true;
}

AuthService.prototype.httpProvider = function () {
    return this._express;
}

module.exports = AuthService;

var argp = yargs
    .usage('$0 [options]')
    .options({
        build: {
            boolean: true,
            describe: 'Build NodeJS Auth Service.'
        },
        buildFake: {
            boolean: true,
            describe: 'Build NodeJS Auth Service with Fake services.'
        },
    });
var argv = argp.argv;

if (argv.build === true) {
    var service = new AuthService(Config);
    service.build(function (err) {
        if (err) {
            logger.error('Error building NodeJS Auth Service: ', Errors.AuthApi.FatalError);
            process.exit(1);
        }
        logger.info('NodeJS Auth Service started successfully');
    }, service._express);
}
if (argv.buildFake === true) {
    var FakeEventService = require('./app/test/services/esc.fakeService.js');
    var FakeMediaService = require('./app/test/services/msc.fakeService.js');
    var FakeProfileService = require('./app/test/services/psc.fakeService.js');
    var FakeServiceRegistryService = require('./app/test/services/src.fakeService.js');
    var FakeUserMessageService = require('./app/test/services/umc.fakeService.js');
    var srFake = new FakeServiceRegistryService();
    var esFake = new FakeEventService();
    var msFake = new FakeMediaService();
    var psFake = new FakeProfileService();
    var umFake = new FakeUserMessageService();
    var service = new AuthService(Config);
    try {
        async.mapSeries([srFake, esFake, msFake, psFake, umFake, service], function (svc, next) {
            svc.build(function (err) {
                return next(err);
            }, svc._express);
        }, function (err) {
            if (err) {
                console.log('Error building NodeJS Auth service with Fake services: ', Errors.AuthApi.FatalError);
                process.exit(1);
            }
            console.log('NodeJS Auth service with Fake services started successfully');
        });
    } catch (ex) {
        console.log('Error building NodeJS Auth service with Fake services: ', Errors.AuthApi.FatalError);
        process.exit(1);
    }
}