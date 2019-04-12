var _ = require('lodash');
var util = require('util');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');
var Routes = require('./../config/auth.routes.js');
var DefaultService = require('nodejs-default-service');

var FakeAuthService = function () {
    var self = this;
    self._config = {
        serviceName: 'auth',
        ip: Config.ip,
        port: (parseInt(Config.port) + 5),
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        registryServiceURIs: 'ws://localhost:' + (parseInt(Config.port) + 3),
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true')
    };
    Routes({ ws: self });
    DefaultService.call(self, self._config);
};

util.inherits(FakeAuthService, DefaultService);

module.exports = FakeAuthService;
