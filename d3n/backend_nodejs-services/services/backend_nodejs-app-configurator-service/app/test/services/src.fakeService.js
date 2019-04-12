var _ = require('lodash');
var util = require('util');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');
var Routes = require('./../config/src.routes.js');
var DefaultService = require('nodejs-default-service');

var FakeServiceRegistryService = function () {
    var self = this;
    self._config = {
        serviceName: 'serviceRegistry',
        ip: Config.ip,
        port: (parseInt(Config.port) + 3),
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        // registryServiceURIs: Config.registryServiceURIs,
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    };

    Routes({ ws: self });
    DefaultService.call(self, self._config);
    self._fakeRegisteredServices = [];
};

util.inherits(FakeServiceRegistryService, DefaultService);

module.exports = FakeServiceRegistryService;
