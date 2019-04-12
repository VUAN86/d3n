/* 
 * Fake User Messge Service
 */
var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');


function FakeVoucherService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeVoucherService, DefaultService);

var o = FakeVoucherService.prototype;

o.messageHandlers = _.assign(o.messageHandlers, {
    'userVoucherReserve': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        clientSession.sendMessage(response);
    },
    
    'userVoucherRelease': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        clientSession.sendMessage(response);
    }
});


module.exports = FakeVoucherService;

