/* 
 * Fake Workflow Service
 */
var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');


function FakeWorkflowService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeWorkflowService, DefaultService);

var o = FakeWorkflowService.prototype;

o.messageHandlers = _.assign(o.messageHandlers, {
    'start': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        
        response.setContent({
            availableActionTypes: ['a', 'b'],
            processFinished: false
        });
        clientSession.sendMessage(response);
    },
    
    'perform': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent({
            triggeredActionType: 'www',
            availableActionTypes: ['a', 'b'],
            processFinished: false
        });
        clientSession.sendMessage(response);
    },
    
    'state': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent({
            availableActionTypes: ['a', 'b'],
            processFinished: false
        });
        clientSession.sendMessage(response);
    },
    
    'abort': function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent({});
        clientSession.sendMessage(response);
    }
    
    
});


module.exports = FakeWorkflowService;

