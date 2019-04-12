var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

var fakeRegisteredServices = [
    {
        serviceName: 'Event',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 1),
        serviceNamespaces: ['Event']
    },
    {
        serviceName: 'profile',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 2),
        serviceNamespaces: ['profile']
    },
    {
        serviceName: 'userMessage',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 4),
        serviceNamespaces: ['userMessage']
    },
    {
        serviceName: 'auth',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 5),
        serviceNamespaces: ['auth']
    },
];

module.exports = {

    get: function (message, clientSession) {
        var self = this;
        try {
            var response = new ProtocolMessage(message);
            logger.debug('SRS get request -> ', message.getContent());
            var serviceName = message.getContent().serviceName;
            var serviceNamespace = message.getContent().serviceNamespace;

            var item;
            if (!serviceName && !serviceNamespace) {
                response.setError(Errors.AuthApi.ValidationFailed);
                logger.debug('SRS get response -> ', response);
                return clientSession.sendMessage(response);
            }
            if (serviceName) {
                item = _.find(fakeRegisteredServices, { 'serviceName': serviceName });
            } else if (serviceNamespace) {
                for (var i = 0; i < fakeRegisteredServices.length; i++) {
                    if (fakeRegisteredServices[i].serviceNamespaces.indexOf(serviceNamespace) >= 0) {
                        item = fakeRegisteredServices[i];
                        break;
                    }
                }
            }
            if (item) {
                response.setContent({
                    service: item
                });
            } else {
                response.setContent({
                    service: {}
                });
            }

            logger.debug('SRS get response -> ', response);
            clientSession.sendMessage(response);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    },

    register: function (message, clientSession) {
        logger.debug('SRS register request -> ', message.getContent());
        var self = this;
        var response = new ProtocolMessage(message);
        try {
            fakeRegisteredServices.push(message.getContent());
            logger.debug('SRS register request -> ', message.getContent());
            clientSession.sendMessage(response);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    },

    unregister: function (message, clientSession) {
        var response = new ProtocolMessage(message);
        response.setContent(null);
        clientSession.sendMessage(response);
    },

    list: function (message, clientSession) {
        try {
            var self = this;
            var response = new ProtocolMessage(message);
            logger.debug('SRS list request -> ', message.getContent());
            var serviceName = message.getContent().serviceName;
            var serviceNamespace = message.getContent().serviceNamespace;

            var items = [];
            if (!serviceName && !serviceNamespace) {
                items = fakeRegisteredServices;
            } else if (serviceName) {
                items.push(_.find(fakeRegisteredServices, { serviceName: serviceName }));
            } else if (serviceNamespace) {
                for (var i = 0; i < fakeRegisteredServices.length; i++) {
                    if (fakeRegisteredServices[i].serviceNamespaces.indexOf(serviceNamespace) >= 0) {
                        items.push(fakeRegisteredServices[i]);
                    }
                }
            }
            response.setContent({
                services: items
            });

            logger.debug('SRS list response -> ', response);
            clientSession.sendMessage(response);
        } catch (ex) {
            return _errorMessage(ex, message, clientSession);
        }
    },

    heartbeatResponse: function (message, clientSession) {

    }
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
            pm.setError(Errors.AuthApi.FatalError);
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
