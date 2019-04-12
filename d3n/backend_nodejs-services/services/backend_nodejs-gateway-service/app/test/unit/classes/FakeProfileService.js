var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');


function FakeProfileService (config) {
    DefaultService.call(this, config);
};

util.inherits(FakeProfileService, DefaultService);

var o = FakeProfileService.prototype;

o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
    getAppConfiguration: function (message, clientSession) {
        var self = this;
        try {
            var response = new ProtocolMessage(message);
            response.setContent({
                appConfig: message.getContent()
            });
            
            clientSession.sendMessage(response);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },
    
    getProfile: function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent({
            profile: {
                userId: '',
                roles: [],
                language: 'en',
                handicap: 1.2,
                emails: [
                    {email: 'www@gmail.com', verificationStatus: 'verified'},
                    {email: 'www2@gmail.com', verificationStatus: 'notVerified'}
                ],
                phones: [
                    {phone: '11-22', verificationStatus: 'verified'},
                    {phone: '33-44', verificationStatus: 'notVerified'}
                ]
            }
        });
        
        clientSession.sendMessage(response);
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

module.exports = FakeProfileService;

