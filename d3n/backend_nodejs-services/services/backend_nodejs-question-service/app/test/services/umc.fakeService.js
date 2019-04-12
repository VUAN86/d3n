var _ = require('lodash');
var path = require('path');
var util = require('util');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');
var Routes = require('./../config/umc.routes.js');
var DefaultService = require('nodejs-default-service');

var FakeUserMessageService = function () {
    var self = this;
    self._config = {
        serviceName: 'userMessage',
        ip: Config.ip,
        port: parseInt(Config.port) + 4,
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        registryServiceURIs: 'ws://localhost:9093',
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
        sendEmailFile: path.join(__dirname, '../../..', 'temp-send-email.txt'),
        sendStatistics: false
    };
    Routes({ ws: self });
    DefaultService.call(self, self._config);
};

util.inherits(FakeUserMessageService, DefaultService);

module.exports = FakeUserMessageService;