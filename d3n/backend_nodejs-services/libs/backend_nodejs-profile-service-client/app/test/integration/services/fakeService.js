var _ = require('lodash');
var util = require('util');
var yargs = require('yargs');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');
var Errors = require('./../../config/errors.js');
var Routes = require('./../config/psc.routes.js');
var DefaultService = require('nodejs-default-service');

var FakeProfileService = function () {
    var self = this;
    self._config = {
        serviceName: 'profile',
        ip: Config.ip,
        port: parseInt(Config.port) + 2,
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        registryServiceURIs: 'ws://localhost:9093',
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    };
    Routes({ ws: self });
    DefaultService.call(self, self._config);
}

util.inherits(FakeProfileService, DefaultService);

module.exports = FakeProfileService;