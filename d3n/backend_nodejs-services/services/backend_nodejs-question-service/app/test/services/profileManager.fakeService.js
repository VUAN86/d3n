var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');


function FakeProfileManagerService () {
    var config = {
        serviceName: 'profileManager',
        ip: Config.ip,
        port: parseInt(Config.port) + 6,
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        registryServiceURIs: 'ws://localhost:9093',
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
        sendStatistics: false
        
    };
    DefaultService.call(this, config);
};

util.inherits(FakeProfileManagerService, DefaultService);

var o = FakeProfileManagerService.prototype;

o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    adminListByTenantId: function (message, clientSession) {
        var self = this;
        try {
            var response = new ProtocolMessage(message);
            response.setContent({
                admins: ['111-11']
            });
            clientSession.sendMessage(response);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    },
    
    userRoleListByTenantId: function (message, clientSession) {
        var self = this;
        try {
            var response = new ProtocolMessage(message);
            response.setContent({
                roles: ['COMMUNITY']
            });
            clientSession.sendMessage(response);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    }
    
    
});
    
function _errorMessage(err, reqMessage, clientSession) {
    var pm = new ProtocolMessage(reqMessage);
    if (err && _.isObject(err) && _.has(err, 'stack')) {
        pm.setError('ERR_FATAL_ERROR');
    } else {
        pm.setError(err);
    }
    return clientSession.sendMessage(pm);
}

module.exports = FakeProfileManagerService;
