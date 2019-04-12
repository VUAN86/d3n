var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var Constants = require('./../../config/constants.js');

function FakeProfileManager (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeProfileManager, DefaultService);

var o = FakeProfileManager.prototype;

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

module.exports = FakeProfileManager;
