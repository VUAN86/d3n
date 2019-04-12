var DefaultService = require('../../../classes/Service.js');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var Errors = require('nodejs-errors');


function TestService (config) {
    DefaultService.call(this, config);
};

util.inherits(TestService, DefaultService);

var o = TestService.prototype;


o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    
    'testServicePing': function (message, clientSession) {
        try {
            var resMessage = new ProtocolMessage(message);
            resMessage.setContent({
                'evenMoreData': 'pong data'
            });
            resMessage.setError(null);

            clientSession.sendMessage(resMessage);
        } catch (e) {
            logger.error('list', e);
            clientSession.sendMessage(_errorMessage(Errors.ERR_FATAL_ERROR));
        }
    }
    
});

o.eventHandlers = _.assign(_.clone(o.eventHandlers), {
    
    'workorderUpdated': function (emitEventName, obj1, obj2) {
        try {
            this.emitEvent(emitEventName, obj1, obj2);
        } catch (e) {
            //this.emitEvent('workorderUpdatedError', item);
        }
    },
    
    'noArgument': function () {
    }
    
});


function _errorMessage (reqMessage, err) {
    var errMesage = new ProtocolMessage(reqMessage);
    errMesage.setError(err);
};

module.exports = TestService;