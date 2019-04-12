var _ = require('lodash');
var util = require('util');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');
var Routes = require('./../config/frs.routes.js');
var DefaultService = require('nodejs-default-service');

var FakeFriendService = function () {
    var self = this;
    self._config = {
        serviceName: 'friend',
        ip: Config.ip,
        port: parseInt(Config.port) + 1,
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        registryServiceURIs: _.isUndefined(process.env.REGISTRY_SERVICE_URIS) ? 'ws://localhost:9203' : process.env.REGISTRY_SERVICE_URIS,
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? true : (process.env.VALIDATE_PERMISSIONS === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    };
    Routes({ ws: self });
    DefaultService.call(self, self._config);
};

util.inherits(FakeFriendService, DefaultService);

module.exports = FakeFriendService;
