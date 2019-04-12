var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

var fakeRegisteredServices = [
    {
        serviceNamespace: 'Event',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 1)
    },
    {
        serviceNamespace: 'profile',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 2)
    },
    {
        serviceNamespace: 'userMessage',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 4)
    },
];

module.exports = {

    get: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'serviceNamespace') || !message.getContent().serviceNamespace) {
            return _errorMessage(Errors.QuestionApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var serviceNamespace = message.getContent().serviceNamespace;
            var uri = _.find(fakeRegisteredServices, { serviceNamespace: serviceNamespace }).uri;
            pm.setContent({
                service: {
                    serviceNamespace: serviceNamespace,
                    uri: uri
                }
            });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    register: function (message, clientSession) {
        if (!message.getMessage() ||
            !_.has(message.getContent(), 'serviceNamespace') || !message.getContent().serviceNamespace) {
            return _errorMessage(Errors.QuestionApi.ValidationFailed, clientSession);
        }
        try {
            var serviceNamespace = message.getContent().serviceNamespace;
            var registeredService = _.find(fakeRegisteredServices, { serviceNamespace: serviceNamespace });
            if (_.isUndefined(registeredService)) {
                fakeRegisteredServices.push({
                    serviceNamespace: serviceNamespace,
                    uri: message.getContent().uri
                });
                logger.debug('SRS register new -> ', message.getContent());
            } else {
                registeredService.uri = message.getContent().uri;
                logger.debug('SRS register update -> ', message.getContent());
            }
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    multiRegister: function (message, clientSession) {
        var self = this;
        if (!message.getMessage() ||
            !_.has(message.getContent(), 'services')) {
            return _errorMessage(Errors.QuestionApi.ValidationFailed, clientSession);
        }
        try {
            var services = message.getContent().services;
            for (var i = 0; i < services.length; i++) {
                fakeRegisteredServices.push({
                    serviceNamespace: services[i].serviceNamespace,
                    uri: services[i].uri
                });
                logger.debug('SRS register multi -> ', message.getContent());
            }
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    list: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'serviceNamespace') || !message.getContent().serviceNamespace) {
            return _errorMessage(Errors.QuestionApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            var serviceNamespace = message.getContent().serviceNamespace;
            var uri = _.find(fakeRegisteredServices, { serviceNamespace: serviceNamespace }).uri;
            pm.setContent({
                services: [
                    {
                        serviceNamespace: serviceNamespace,
                        uri: uri
                    }
                ]
            });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },
};

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
            pm.setError(Errors.QuestionApi.FatalError);
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
