var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');


function FakeEventService (config) {
    DefaultService.call(this, config);
    
    this._fakeRegisteredServices = [];
};

util.inherits(FakeEventService, DefaultService);

var o = FakeEventService.prototype;

o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    subscribe: function (message, clientSession) {
        var self = this;
        try {
            var pm = _message(message);
            pm.setContent({
                subscription:'asds'
            });
            
            logger.debug('ES subscribe');
            
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

