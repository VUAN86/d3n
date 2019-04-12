/* 
 * Fake Shop Service
 */
var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');


function FakeShopService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeShopService, DefaultService);

var o = FakeShopService.prototype;

o.messageHandlers = _.assign(o.messageHandlers, {
    'tenantAdd': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        clientSession.sendMessage(response);
    },
    
    'tenantUpdate': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        clientSession.sendMessage(response);
    }
});


module.exports = FakeShopService;

