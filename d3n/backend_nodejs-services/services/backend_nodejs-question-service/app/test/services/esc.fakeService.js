var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');


function FakeEventService (config) {
    var config = {
        serviceName: 'event',
        ip: Config.ip,
        port: parseInt(Config.port) + 1,
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

util.inherits(FakeEventService, DefaultService);

var o = FakeEventService.prototype;

o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    subscribe: function (message, clientSession) {
        var self = this;
        try {
            var pm = _message(message);
            pm.setContent({
                subscription: 111111
            });
            
            logger.debug('ES subscribe');
            
            return _successMessage(pm, clientSession);
            
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
        
    },
    
    unsubscribe: function (message, clientSession) {
        var self = this;
        try {
            var pm = _message(message);
            pm.setContent({
            });
            
            logger.debug('ES unsubscribe');
            
            return _successMessage(pm, clientSession);
            
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
        
    }
    
    
});

function _message(message) {
    var pm = new ProtocolMessage();
    pm.setMessage(message.getMessage() + 'Response');
    pm.setContent(null);
    pm.setSeq(message && message.getSeq() ? message.getSeq() : null);
    pm.setAck(message && message.getSeq() ? [message.getSeq()] : null);
    pm.setClientId(message && message.getClientId() ? message.getClientId() : null);
    pm.setTimestamp(_.now());
    return pm;
}
    
function _errorMessage(err, clientSession) {
    setImmediate(function (err, clientSession) {
        var pm = _message(new ProtocolMessage());
        if (err && _.isObject(err) && _.has(err, 'stack')) {
            pm.setError('ERR_FATAL_ERROR');
        } else {
            pm.setError(err);
        }
        return clientSession.sendMessage(pm);
    }, err, clientSession);
}

function _successMessage(pm, clientSession) {
    setImmediate(function (pm, clientSession) {
        return clientSession.sendMessage(pm);
    }, pm, clientSession);
}

module.exports = FakeEventService;
