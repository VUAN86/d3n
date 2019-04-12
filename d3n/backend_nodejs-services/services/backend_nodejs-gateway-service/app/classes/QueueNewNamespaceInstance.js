var logger = require('nodejs-logger')();
var _ = require('lodash');
var Constants = require('../config/constants.js');

function QueueNewNamespaceInstance(loadBalancerManager) {
    
    this._loadBalancerManager = loadBalancerManager;
    
    this._queue = [];
    
    this._frequency = 3000;
    
    this._integrateRunning = false;
    
};

var o = QueueNewNamespaceInstance.prototype;


o.addNamespace = function (namespace, uri) {
    if(namespace === Constants.EVENT_SERVICE_NAME) {
        return;
    }
    this._queue.push({
        serviceNamespace: namespace,
        uri: uri
    });
};

o.start = function () {
    this._integrate.call(this);
};


/**
 * Remove items from queue by uri.
 * @param {string} uri
 * @returns {undefined}
 */
o._removeItemsByURI = function (uri) {
    for(var i = this._queue.length -1; i >= 0 ; i--){
        if(this._queue[i].uri === uri){
            this._queue.splice(i, 1);
        }
    }    
};

o._integrate = function () {
    try {
        var self = this;
        /*
        if (self._integrateRunning) {
            //return setTimeout(self._integrate.bind(self), self._frequency);
            return;
        }*/
        
        self._integrateRunning = true;
        
        if (!self._queue.length) {
            self._integrateRunning = false;
            return setTimeout(self._integrate.bind(self), self._frequency);
        }

        var item = self._queue[0];
        logger.debug('QueueNewNamespaceInstance._integrate() item:', item);
        
        self._loadBalancerManager.monitoringService.listAvailableNamespaces(function (err, namespaces) {
            //console.log('>>>>>>>namespaces:', namespaces);
            try {
                if (err) {
                    logger.error('QueueNewNamespaceInstance._integrate() error 1:', err);
                    self._removeItemsByURI(item.uri);
                    self._integrateRunning = false;
                    return setTimeout(self._integrate.bind(self), self._frequency);
                }

                if (!namespaces.length) {
                    logger.warn('QueueNewNamespaceInstance._integrate() no namespace for item:', item);
                    self._removeItemsByURI(item.uri);
                    self._integrateRunning = false;
                    return setTimeout(self._integrate.bind(self), self._frequency);
                }
                logger.debug('QueueNewNamespaceInstance._integrate() namespaces detected:', namespaces);
                var serviceNamespaces = [];

                // keep only namespaces with same uri
                for(var i=0; i<namespaces.length; i++) {
                    if (namespaces[i].uri === item.uri) {
                        serviceNamespaces.push(namespaces[i].serviceNamespace);
                    }
                }
                
                if (!serviceNamespaces.length) {
                    logger.warn('QueueNewNamespaceInstance._integrate() no namespace for item:', item);
                    self._removeItemsByURI(item.uri);
                    self._integrateRunning = false;
                    return setTimeout(self._integrate.bind(self), self._frequency);
                }
                
                var serviceName = self._loadBalancerManager.getServiceNameByNamespaces(item.serviceNamespace);
                
                if (serviceName === null) {
                    serviceName = serviceNamespaces.sort().join(',');
                }
                
                logger.debug('QueueNewNamespaceInstance._integrate() service to integrate:', serviceName);
                
                //logger.debug('@@@@@@integration of ', serviceName);
                
                self._removeItemsByURI(item.uri);
                self._loadBalancerManager.makeInstanceAvailable(serviceName, item.uri, function (err) {
                    self._integrateRunning = false;
                    if(err) {
                        logger.error('QueueNewNamespaceInstance._integrate() error 4:', err);
                    }
                    
                    
                    logger.debug('QueueNewNamespaceInstance._integrate() queue=', self._queue);
                    return setTimeout(self._integrate.bind(self), self._frequency);
                });
                
            } catch (e) {
                logger.error('QueueNewNamespaceInstance._integrate() error 2:', e);
                self._removeItemsByURI(item.uri);
                self._integrateRunning = false;
                return setTimeout(self._integrate.bind(self), self._frequency);
            }
        });
    } catch (e) {
        logger.error('QueueNewNamespaceInstance._integrate() error 3:', e);
        if(item) {
            self._removeItemsByURI(item.uri);
        }
        self._integrateRunning = false;
        return setTimeout(self._integrate.bind(self), self._frequency);
    }
    
};

module.exports = QueueNewNamespaceInstance;