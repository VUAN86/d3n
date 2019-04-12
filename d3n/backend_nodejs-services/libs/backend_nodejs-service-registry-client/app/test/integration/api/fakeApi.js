var _ = require('lodash'),
    Errors = require('./../../../config/errors.js'),
    ProtocolMessage = require('nodejs-protocol');

module.exports = {

    register: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'serviceNamespace') || !message.getContent().serviceNamespace ||
            !_.has(message.getContent(), 'uri') || !message.getContent().uri) {
            return _errorMessage(Errors.SrcApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    multiRegister: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'services')) {
            return _errorMessage(Errors.SrcApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },
    
    unregister: function (message, clientSession) {
        if (!message.getMessage() || !_.isNull(message.getContent()) || _.isNull(message.getSeq())) {
            return _errorMessage(Errors.SrcApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    get: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq()) ||
            !_.has(message.getContent(), 'serviceNamespace') || !message.getContent().serviceNamespace) {
            return _errorMessage(Errors.SrcApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            
            if (message.getContent().serviceNamespace === 'returnNullURIs') {
                pm.setContent({
                    service: {
                        serviceNamespace: message.getContent().serviceNamespace,
                        uri: null
                    }
                });
                return _successMessage(pm, clientSession);
            }
            
            
            pm.setContent({
                service: {
                    serviceNamespace: message.getContent().serviceNamespace,
                    uri: 'wss://localhost:1234'
                }
            });
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    list: function (message, clientSession) {
        if (!message.getMessage() || _.isNull(message.getSeq())) {
            return _errorMessage(Errors.SrcApi.ValidationFailed, clientSession);
        }
        try {
            var pm = _message(message);
            
            if (message.getContent().serviceNamespace === 'returnNullURIs') {
                pm.setContent({
                    services: [
                        {
                            serviceNamespace: message.getContent().serviceNamespace,
                            uri: null
                        }, {
                            serviceNamespace: message.getContent().serviceNamespace,
                            uri: 'wss://localhost:5678'
                        }
                    ]
                });
                return _successMessage(pm, clientSession);
            }
            
            if (_.has(message.getContent(), 'serviceNamespace') && message.getContent().serviceNamespace) {
                pm.setContent({
                    services: [
                        {
                            serviceNamespace: message.getContent().serviceNamespace,
                            uri: 'wss://localhost:1234'
                        }, {
                            serviceNamespace: message.getContent().serviceNamespace,
                            uri: 'wss://localhost:5678'
                        }
                    ]
                });
            } else {
                pm.setContent({
                    services: [
                        {
                            serviceNamespace: 'sampleService',
                            uri: 'wss://localhost:1234'
                        }, {
                            serviceNamespace: 'sampleService',
                            uri: 'wss://localhost:5678'
                        }, {
                            serviceNamespace: 'testService',
                            uri: 'wss://localhost:1234'
                        }
                    ]
                });
            }
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    heartbeat: function (message, clientSession) {
        if (!message.getMessage()) {
            return _errorMessage(Errors.SrcApi.ValidationFailed, callback);
        }
        try {
            var pm = _message(message);
            // imitate "server->client heartbeat" if no content received
            if (!message.getContent()) {
                pm.setMessage('serviceRegistry/heartbeat');
            }
            return _successMessage(pm, clientSession);
        } catch (ex) {
            return _errorMessage(ex, clientSession);
        }
    },

    heartbeatResponse: function (message, clientSession) {
        if (!message.getMessage()) {
            return _errorMessage(Errors.SrcApi.ValidationFailed, callback);
        }
        try {
            var pm = _message(message);
            // make mirrored response
            pm.setMessage('serviceRegistry/heartbeatResponse');
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
            pm.setError(Errors.SrcApi.FatalError);
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
