var _ = require('lodash');
var async = require('async');
var inherits = require('util').inherits;
var Errors = require('./../config/errors.js');
var DefaultConfig = require('./../config/config.js');
var DefaultClient = require('nodejs-default-client');
var ProtocolMessage = require('nodejs-protocol');
var EventEmitter = require('events').EventEmitter;
var logger = require('nodejs-logger')();

function ServiceRegistryClient(config, ownerServiceName) {
    var registryServiceURIs;
    if (!config) {
        registryServiceURIs = process.env.REGISTRY_SERVICE_URIS;
    } else if (config && _.has(config, 'registryServiceURIs')) {
        registryServiceURIs = config.registryServiceURIs;
    } else if (_.isArray(config)) {
        // backward compatibility
        var hosts = [];
        for(var i=0; i<config.length; i++) {
            hosts.push((config[i].secure ? 'wss' : 'ws') + '://' + config[i].service.ip + ':' + config[i].service.port);
        }
        registryServiceURIs = hosts.join(',');
    }
    
    this._registryServiceURIs = registryServiceURIs;
    
    this._defaultClient = new DefaultClient({
        serviceNamespace: DefaultConfig.SERVICE_REGISTRY_NAME,
        registryServiceURIs: this._registryServiceURIs,
        headers: ownerServiceName ? {service_name: ownerServiceName} : null
    });
    
    // delay between reinitialization
    if(_.isPlainObject(config) && _.gt(config.reconnectTryForeverInterval, 0)) {
        this._reconnectTryForeverInterval = parseInt(config.reconnectTryForeverInterval);
    } else {
        this._reconnectTryForeverInterval = 5*1000;
    }
    
    this._lastHeartbeatTimestamp = null; // milliseconds
    
    this._servicesForReregister = [];
    
    this._queue = async.queue(this._processQueueTask.bind(this), 1);
    
    this._queueStarted = false;
    
    // push initialization task in front of the queue, so the messages will be sent only after initialization
    this._queue.pause();
    this._queue.push({
        type: 'init'
    });
    
    this._queue.error = function (err, task) {
        logger.error('ServiceRegistryClient error on queue:', err, task);
    };
    
    this._inited = false;
    
    EventEmitter.call(this);
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('ServiceRegistryClient error event emitted:', error);
    });
    
};

inherits(ServiceRegistryClient, EventEmitter);

var o = ServiceRegistryClient.prototype;

/**
 * Call serviceRegistry/register API.
 * @param {string} serviceName
 * @param {string} uri
 * @param {array} serviceNamespaces
 * @param {function} callback
 * @returns {unresolved}
 */
o.register = function (serviceName, uri, serviceNamespaces, callback) {
    try {
        logger.debug('ServiceRegistryClient.register() called:', serviceName, uri, serviceNamespaces);
        var self = this;
        
        if (!serviceName || !uri || !_.isArray(serviceNamespaces) || !serviceNamespaces.length) {
            return setImmediate(callback, Errors.SrcApi.ValidationFailed);
        }
        
        var serviceModel = {
            serviceName: serviceName, 
            uri: uri, 
            serviceNamespaces: serviceNamespaces
        };
        
        self._register(serviceName, uri, serviceNamespaces, function (err, response) {
            try {
                logger.debug('ServiceRegistryClient.register() response:', err, JSON.stringify(response));
                self._servicesForReregister.push(_.clone(serviceModel));
                // keep unique only
                self._servicesForReregister = _.uniqBy(self._servicesForReregister, function (elm) {
                    return elm.serviceName + ':' + elm.uri;
                });

                callback(err, response);
                //setImmediate(callback, err, response);
                if(err || !_.isNull(response.getError())) {
                    self._defaultClient.disconnect(1000, 'registerError', false);
                }
            } catch (e) {
                return callback(e);
            }
        });
        
    } catch (e) {
        logger.error('ServiceRegistryClient.register() error:', e);
        return setImmediate(callback, e);
    }
};


o._register = function (serviceName, uri, serviceNamespaces, callback, addInFront) {
    try {
        var self = this;
        var serviceModel = {
            serviceName: serviceName, 
            uri: uri, 
            serviceNamespaces: serviceNamespaces
        };
        
        self._request('register', serviceModel, self._defaultClient.getSeq(), callback, (addInFront === true ? true : false));
        
    } catch (e) {
        logger.error('ServiceRegistryClient._register() error:', e);
        return setImmediate(callback, e);
    }
};


/**
 * Call serviceRegistry/unregister API.
 * @param {function} callback
 * @returns {undefined}
 */
o.unregister = function (callback) {
    logger.debug('ServiceRegistryClient.unregister() called:');
    var self = this;
    self._request('unregister', null, self._defaultClient.getSeq(), callback);
};

/**
 * Call serviceRegistry/get API.
 * @param {string} serviceName
 * @param {string} serviceNamespace
 * @param {function} callback
 * @returns {unresolved}
 */
o.get = function (serviceName, serviceNamespace, callback) {
    var self = this;
    if (!serviceName && !serviceNamespace) {
        return setImmediate(callback, Errors.SrcApi.ValidationFailed);
    }
    
    var content = {};
    if (serviceName) {
        content.serviceName = serviceName;
    }
    if (serviceNamespace) { 
        content.serviceNamespace = serviceNamespace;
    }
    
    self._request('get', content, self._defaultClient.getSeq(), function (err, response) {
        try {
            if (err) {
                return callback(err);
            }
            
            if (!_.isNull(response.getError())) {
                return callback(new Error(response.getError().message));
            }
            
            // if null URI return ampty object
            if (_.isNull(response.getContent().service.uri)) {
                response.setContent({
                    service: {}
                });
            }
            
            return callback(false, response);
        } catch (e) {
            return callback(e);
        }
    });
};

/**
 * Call serviceRegistry/list API.
 * @param {string|null|undefined} serviceName
 * @param {string|null|undefined} serviceNamespace
 * @param {function} callback
 * @returns {undefined}
 */
o.list = function (serviceName, serviceNamespace, callback) {
    var self = this;
    
    var content = {};
    if (serviceName) {
        content.serviceName = serviceName;
    }
    if (serviceNamespace) { 
        content.serviceNamespace = serviceNamespace;
    }
    
    
    self._request('list', content, self._defaultClient.getSeq(), function (err, response) {
        try {
            if (err) {
                return callback(err);
            }
            
            if (!_.isNull(response.getError())) {
                return callback(new Error(response.getError().message));
            }
            
            // if null URIs return empty array
            var services = response.getContent().services;
            for(var i=0; i<services.length; i++) {
                if (_.isNull(services[i].uri)) {
                    response.setContent({
                        services: []
                    });
                    return callback(false, response);
                }
            }
            return callback(false, response);
        } catch (e) {
            return callback(e);
        }
        
    });
};

o.pushServiceStatistics = function (statistics, cb) {
    var self = this;
    var content = {
        statistics: statistics
    };
    
    self._request('pushServiceStatistics', content, null, function (err) {
        try {
            if (err) {
                return cb(err);
            }
            return cb();
        } catch (e) {
            return cb(e);
        }
        
    });
};

o.getInfrastructureStatistics = function (cb) {
    var self = this;
    var content = null;
    
    self._request('getInfrastructureStatistics', content, self._defaultClient.getSeq(), function (err, response) {
        try {
            if (err) {
                return cb(err);
            }
            
            if (!_.isNull(response.getError())) {
                return cb(new Error(response.getError().message));
            }
            
            return cb(false, response.getContent().statisticsList);
        } catch (e) {
            return cb(e);
        }
        
    });
};


o.disconnect = function (callback) {
    try {
        if (this._defaultClient.connected()) {
            this._defaultClient.disconnect(1000, '', true);
        }
        
        this.getQueue().kill();
        
        if(callback) {
            return setImmediate(callback);    
        }
    } catch (e) {
        if(callback) {
            return setImmediate(callback, e);
        } else {
            throw e;
        }
    }
};

o.connected = function () {
    return this._defaultClient.connected();
};

o.getLastHeartbeatTimestamp = function () {
    return this._lastHeartbeatTimestamp;
};

o.getQueue = function () {
    return this._queue;
};

/**
 * Build the API message then add message into queue.
 * @param {string} api
 * @param {aobject} content - message content
 * @param {integer|null} seq - message seq
 * @param {function} callback
 * @param {boolean} addInFront - add in front of message queue or not
 * @param {boolean} dontWaitResponse - wait for a service response or not
 * @returns {unresolved}
 */
o._request = function (api, content, seq, callback, addInFront, dontWaitResponse) {
    try {
        var self = this;
        var message = _message(api, content, seq);
        self._requestWithMessage(message, callback, addInFront, dontWaitResponse);
    } catch (e) {
        logger.error('ServiceRegistryClient._request() error handling request call:', e, api, content, seq, addInFront, dontWaitResponse);
        return setImmediate(callback, e);
    }
};

/**
 * Adds a task into message queue.
 * @param {ProtocolMessage} message
 * @param {function} callback
 * @param {boolean} addInFront
 * @param {boolean} dontWaitResponse
 * @returns {undefined}
 */
o._requestWithMessage = function (message, callback, addInFront, dontWaitResponse) {
    var queueMethod = (addInFront === true ? 'unshift': 'push');
    this.getQueue()[queueMethod]({
        type: 'api-call',
        message: message,
        callback: callback,
        dontWaitResponse: dontWaitResponse
    });
    
    if (this._queueStarted === false) {
        this.getQueue().resume();
        this._queueStarted = true;
    }
};

/**
 * Process a queue task. Task can be an API call or initialization. 
 * Initialization task is added by default by the constructor in front of the queue.
 * @param {object} task
 * @param {function} callback
 * @returns {unresolved}
 */
o._processQueueTask = function (task, callback) {
    try {
        logger.debug('ServiceRegistryClient._processQueueTask() called:', task.type, task.message);
        var self = this;
        if (task.type === 'init') {
            self._initTryForever(function (err, connected) {
                if (connected !== true) {
                    logger.error('ServiceRegistryClient._processQueueTask() init task, cant connect to SRs');
                    return callback(new Error('ERR_INIT_TASK_CANT_CONNECT'));
                }
                return callback();
            });
        } else if (task.type === 'api-call') {
            self._defaultClient.sendMessage(task.message, task.callback, false, task.dontWaitResponse);
            return callback();
        }
    } catch (e) {
        logger.error('ServiceRegistryClient._processQueueTask() error:', e, task);
        return callback(e);
    }
};


/**
 * Initialize the client. Connect to a registry instance then set handlers.
 * @param {Function} callback
 * @returns {unresolved}
 */
o._init = function (callback) {
    try {
        logger.debug('ServiceRegistryClient._init() called');
        var self = this;
        
        if (self._defaultClient) {
            self._defaultClient.removeAllListeners();
            self._defaultClient.getQueue().kill();
        }
        
        self._defaultClient.connect(function (err) {
            try {
                logger.debug('ServiceRegistryClient._init() default client connect response:', (err && err.message ? err.message : err));
                if(err) {
                    return callback(Errors.SrcApi.ConnectionFailed);
                }

                self._defaultClient.on('reconnected', self._onReconnectedHandler.bind(self));

                self._defaultClient.on('reconnectedToNewInstance', self._onReconnectedHandler.bind(self));
                
                self._defaultClient.on('message', self._onMessageHandler.bind(self));
                
                self._defaultClient.on('reconnectingFailed', self._onReconnectingFailedHandler.bind(self));

                return callback();
            } catch (e) {
                logger.error('ServiceRegistryClient._init() error setting default client handlers:', e);
                return callback(e);
            }
        });
    } catch (e) {
        logger.error('ServiceRegistryClient._init() error handling initialization:', e);
        return setImmediate(callback, e);
    }
};

/**
 * Tries "forever" to initialize the client. Basically tries "forever" until succesfully connect to a SR instance.
 * @param {callback} callback
 * @returns {unresolved}
 */


o._initTryForever = function (callback) {
    try {
        logger.debug('ServiceRegistryClient._initTryForever() called');
        var self = this;
        
        async.forever(function (next) {
            self._init(function (err) {
                logger.debug('ServiceRegistryClient._initTryForever() _init() call response:', err);
                if (err) {
                    // retry
                    return setTimeout(next, self._reconnectTryForeverInterval);
                } else {
                    // connected, do not retry anymore
                    return next(true);
                }
            });
            
        }, function (result) {
            if (result instanceof Error) {
                logger.error('ServiceRegistryClient._initTryForever() error on try init forever:', result);
                return callback(result);
            }
            
            return callback(false, true);
        });
    } catch (e) {
        return setImmediate(callback, e);
    }
};

/**
 * Re-register registered services.
 * @param {function} callback
 * @returns {unresolved}
 */
o._reRegister = function (callback) {
    try {
        logger.debug('ServiceRegistryClient._reRegister() called');
        var self = this;
        
        if (!self._servicesForReregister.length) {
            return setImmediate(callback);
        }
        
        logger.debug('ServiceRegistryClient._reRegister() _servicesForReregister:', self._servicesForReregister);
        async.mapSeries(_.clone(self._servicesForReregister), function (item, cbItem) {
            self._register(item.serviceName, item.uri, item.serviceNamespaces, function (err, response) {
                if (err) {
                    return cbItem(err);
                }
                if(!_.isNull(response.getError())) {
                    return cbItem(new Error(response.getError().message));
                }
                return cbItem();
            }, true);
        }, function (err) {
            if (err) {
                logger.error('ServiceRegistryClient._reRegister() something went wrong on re-registration:', err, self._servicesForReregister);
            } else {
                logger.debug('ServiceRegistryClient._reRegister() success:', self._servicesForReregister);
            }
            return callback(err);
        });
    } catch (e) {
        return setImmediate(callback, e);
    }
};

/**
 * Tries to reinitialize the client then re-register registered namespaces
 * @returns {undefined}
 */
o._reInitAndReRegister = function () {
    try {
        logger.debug('ServiceRegistryClient._reInitAndReRegister() called');
        var self = this;
        
        self.getQueue().pause();
        
        self._initTryForever(function (err, reconnected) {
            if (reconnected !== true) {
                logger.error('ServiceRegistryClient._reInitAndReRegister something unexpected happened!!!');
                return;
            }
            
            // add reregistration in front of the queue
            self._reRegister(function (err) {
                if (err) {
                    logger.error('ServiceRegistryClient._reInitAndReRegister() something went wrong on re-registration:', err, self._servicesForReregister);
                    self._defaultClient.disconnect(1000, 'reRegisterError', false);
                } else {
                    logger.debug('ServiceRegistryClient._reInitAndReRegister() re-register success');
                }
            });
            
            self.getQueue().resume();
        });
    } catch (e) {
        logger.error('ServiceRegistryClient._reInitAndReRegister() error:', e);
    }
    
};

/**
 * Client can't conect to any SR instance. Retry, retry , retry ... 
 * @returns {undefined}
 */
o._onReconnectingFailedHandler = function () {
    logger.debug('ServiceRegistryClient._onReconnectingFailedHandler() called');
    this._reInitAndReRegister();
};


/**
 * WS client succesfully reconnected to a SR instance. Do re-registration
 * @param {function} eventCallback
 * @returns {unresolved}
 */
o._onReconnectedHandler = function (eventCallback) {
    try {
        logger.debug('ServiceRegistryClient._onReconnectedHandler() called');
        var self = this;
        
        self._reRegister(function (err) {
            if (err) {
                logger.error('ServiceRegistryClient._onReconnectedHandler() something went wrong on re-registration:', err, self._servicesForReregister);
                self._defaultClient.disconnect(1000, 'reRegisterError', false);
            } else {
                logger.debug('ServiceRegistryClient._onReconnectedHandler() re-register success');
            }
        });
        
        return setImmediate(eventCallback);
        
    } catch (e) {
        logger.error('ServiceRegistryClient._onReconnectedHandler() reconnected event:', e);
        return setImmediate(eventCallback);
    }
};

/**
 * Handle messages sent by SR. If heartbeat replay with heartbeat response.
 * @param {ProtocolMessage} message
 * @returns {undefined}
 */
o._onMessageHandler = function (message) {
    try {
        var self = this;
        if(message.getMessageName() === 'heartbeat') {
            self._lastHeartbeatTimestamp = Date.now();
            // replay back to service registry
            var responseMessage = new ProtocolMessage(message);
            responseMessage.setContent({status: 'alive'});
            
            self._requestWithMessage(responseMessage, function (err) {
                if (err) {
                    logger.error('ServiceRegistryClient._onMessageHandler() error sending heartbeat response:', err, message, responseMessage);
                }
            }, false, true);
        } else {
            self.emit('message', message);
        }
    } catch (e) {
        logger.error('ServiceRegistryClient._onMessageHandler() error:', e, message);
    }
};



function _message(api, content, seq) {
    var pm = new ProtocolMessage();
    pm.setMessage(DefaultConfig.SERVICE_REGISTRY_NAME + '/' + api);
    pm.setContent(content);
    pm.setSeq(seq);
    return pm;
}

module.exports = ServiceRegistryClient;