/* 
 * Fake User Messge Service
 */
var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');


function FakeUserMessageService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeUserMessageService, DefaultService);

var o = FakeUserMessageService.prototype;

o.messageHandlers = _.assign(o.messageHandlers, {
    'sendEmail': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        clientSession.sendMessage(response);
    },
    
    'sendSms': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        clientSession.sendMessage(response);
    }
});


module.exports = FakeUserMessageService;

