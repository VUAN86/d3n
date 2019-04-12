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
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var Aerospike = require('nodejs-aerospike');
var DatabasesStatisticsProvider = require('nodejs-statistics-providers')['databasesStatisticsProvider'];

var LiveAppService = function (config) {
    var self = this;
    self._config = config ? config : Config;
    self._built = false;
    self._websocketService = new DefaultService(self._config);
    Routes(self._websocketService);
};

LiveAppService.prototype.build = function (callback) {
    var self = this;
    if (!self._built) {
        self._websocketService.build(function (err) {
            if (err) {
                return callback(err);
            }
            
            async.series([
                // db monitoring
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

LiveAppService.prototype.shutdown = function (callback) {
    var self = this;
    self._websocketService.shutdown();
    self._built = false;
    return setImmediate(function (callback) {
        return callback(null, true);
    }, callback);
}

module.exports = LiveAppService;

var argp = yargs
    .usage('$0 [options]')
    .options({
        build: {
            boolean: true,
            describe: 'Build LiveApp Manager service.'
        }
    }
);
var argv = argp.argv;

if (argv.build === true) {
    var service = new LiveAppService(Config);
    service.build(function (err) {
        if (err) {
            logger.error('Error building LiveApp Manager service: ', err);
            process.exit(1);
        }
        logger.info('LiveApp Manager service started successfully');
    });
}