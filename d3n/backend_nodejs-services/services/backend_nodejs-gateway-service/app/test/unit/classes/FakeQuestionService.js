var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');


function FakeQuestionService (config) {
    DefaultService.call(this, config);
    
    this._fakeRegisteredServices = [];
};

util.inherits(FakeQuestionService, DefaultService);

var o = FakeQuestionService.prototype;

o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    questionGet: function (message, clientSession) {
        var self = this;
        try {
            var response = new ProtocolMessage(message);
            response.setContent('questionGet');
            clientSession.sendMessage(response);
            
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
        
    },
    
    workorderGet: function (message, clientSession) {
        var self = this;
        try {
            var response = new ProtocolMessage(message);
            response.setContent('workorderGet');
            clientSession.sendMessage(response);
            
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

module.exports = FakeQuestionService;

