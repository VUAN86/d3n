var _ = require('lodash'),
    yargs = require('yargs'),
    Config = require('./../../../config/config.js'),
    Errors = require('./../../../config/errors.js'),
    Routes = require('./../config/routes.js'),
    DefaultWebsocketService = require('nodejs-default-service');

global.config = Config;

var FakeService = function (config) {
    var self = this;
    if (config) {
        global.config = config;
    }
    self._built = false;
    self._websocketService = new DefaultWebsocketService({
        ip: global.config.fakeHost.service.ip,
        port: global.config.fakeHost.service.port,
        secure: global.config.fakeHost.secure,
        key: global.config.fakeHost.key,
        cert: global.config.fakeHost.cert
    });
    Routes({ ws: self._websocketService });
}

FakeService.prototype.build = function (callback) {
    var self = this;
    if (!self._built) {
        self._built = true;
        self._websocketService.build(callback);
    } else {
        return setImmediate(function (callback) {
            return callback(null, true);
        }, callback);
    }
}

FakeService.prototype.shutdown = function (callback) {
    var self = this;
    self._websocketService.shutdown();
    self._built = false;
    return setImmediate(function (callback) {
        return callback(null, true);
    }, callback);
}

module.exports = FakeService;

var argp = yargs
    .usage('$0 [options]')
    .options({
        build: {
            boolean: true,
            describe: 'Build NodeJS Fake Service Registry Service.'
        }
    });
var argv = argp.argv;

if (argv.build === true) {
    var service = new FakeService(Config);
    service.build(function (err) {
        if (err) {
            console.log('Error building NodeJS Fake Service Registry Service: ', Errors.SrcApi.FatalError);
            process.exit(1);
        }
        console.log('NodeJS Fake Service Registry Service started successfully');
    });
}