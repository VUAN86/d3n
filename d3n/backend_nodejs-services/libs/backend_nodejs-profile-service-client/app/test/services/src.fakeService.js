var _ = require('lodash');
var util = require('util');
var yargs = require('yargs');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');
var Routes = require('./../config/src.routes.js');
var DefaultService = require('nodejs-default-service');

var FakeServiceRegistryService = function () {
    var self = this;
    self._config = {
        serviceName: 'serviceRegistry',
        ip: 'localhost',
        port: 9093,
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        // registryServiceURIs: Config.registryServiceURIs,
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    };
    
    Routes({ ws: self });
    DefaultService.call(self, self._config);
    self._fakeRegisteredServices = [];
}

util.inherits(FakeServiceRegistryService, DefaultService);

module.exports = FakeServiceRegistryService;
