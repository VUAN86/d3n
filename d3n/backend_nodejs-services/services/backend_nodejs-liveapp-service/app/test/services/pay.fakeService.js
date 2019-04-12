var _ = require('lodash');
var util = require('util');
var DefaultService = require('nodejs-default-service');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');
var DataIds = require('./../config/_id.data.js');


function FakePaymentService () {
    var config = {
        serviceName: 'payment',
        ip: Config.ip,
        port: parseInt(Config.port) + 12,
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

util.inherits(FakePaymentService, DefaultService);

var o = FakePaymentService.prototype;

o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    transferBetweenAccounts: function (message, clientSession) {
        _processPayment(message, clientSession);
    },
    loadOrWithdrawWithoutCoverage: function (message, clientSession) {
        _processPayment(message, clientSession);
    }
});
    
function _processPayment(message, clientSession) {
    try {
        var response = new ProtocolMessage(message);
        response.setContent({
            transactionId: Math.floor(Math.random() * 100) + 1  
        });
        clientSession.sendMessage(response);
    } catch (ex) {
        _errorMessage(ex, message, clientSession);
    }
}

function _errorMessage(err, reqMessage, clientSession) {
    var pm = new ProtocolMessage(reqMessage);
    if (err && _.isObject(err) && _.has(err, 'stack')) {
        pm.setError('ERR_FATAL_ERROR');
    } else {
        pm.setError(err);
    }
    clientSession.sendMessage(pm);
}

module.exports = FakePaymentService;
