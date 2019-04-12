var DefaultService = require('nodejs-default-service');
var util = require('util');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var Config = require('./../../config/config.js');
var TestConfig = require('./../config/test.config.js');


function FakeRegistryService () {
    var config = {
        serviceName: 'serviceRegistry',
        ip: Config.ip,
        port: parseInt(Config.port) + 3,
        secure: false,
        auth: TestConfig.auth,
        jwt: TestConfig.jwt,
        key: TestConfig.key,
        cert: TestConfig.cert,
        //registryServiceURIs: 'ws://localhost:9093',
        validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
        validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
        protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
        sendStatistics: false
        
    };
    DefaultService.call(this, config);
    
    this._fakeRegisteredServices = [];
};

util.inherits(FakeRegistryService, DefaultService);

var o = FakeRegistryService.prototype;

o.messageHandlers = _.assign(_.clone(o.messageHandlers), {
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

module.exports = FakeRegistryService;
