var _ = require('lodash');
var Config = require('./../../config/config.js');
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
    {
        serviceNamespace: 'auth',
        uri: 'ws://' + Config.ip + ':' + (parseInt(Config.port) + 5)
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
                response.setError('ERR_VALIDATION_FAILED');
                logger.debug('SRS get response -> ', response);
                return clientSession.sendMessage(response);
            }

            if (serviceName) {
                item = _.find(self._fakeRegisteredServices, ['serviceName', serviceName]);
            } else if (serviceNamespace) {
                for(var i=0; i<self._fakeRegisteredServices.length; i++) {
                    if (self._fakeRegisteredServices[i].serviceNamespaces.indexOf(serviceNamespace) >= 0) {
                        item = self._fakeRegisteredServices[i];
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
        /*
        if (!message.getMessage() ||
            !_.has(message.getContent(), 'serviceNamespace') || !message.getContent().serviceNamespace) {
            return _errorMessage('ERR_VALIDATION_FAILED', clientSession);
        }
        */
        try {
            self._fakeRegisteredServices.push(message.getContent());
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
                items = self._fakeRegisteredServices;
            }

            if (serviceName) {
                for(var i=0; i<self._fakeRegisteredServices.length; i++) {
                    if (self._fakeRegisteredServices[i].serviceName === serviceName) {
                        items.push(self._fakeRegisteredServices[i]);
                    }
                }
            } else if (serviceNamespace) {
                for(var i=0; i<self._fakeRegisteredServices.length; i++) {
                    if (self._fakeRegisteredServices[i].serviceNamespaces.indexOf(serviceNamespace) >= 0) {
                        items.push(self._fakeRegisteredServices[i]);
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
