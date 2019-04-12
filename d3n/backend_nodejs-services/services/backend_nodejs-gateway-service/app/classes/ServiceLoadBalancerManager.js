var EventEmitter = require('events').EventEmitter;
var inherits = require('util').inherits;
var logger = require('nodejs-logger')();
var async = require('async');
var MonitoringService = require('./MonitoringService.js');
var ServiceLoadBalancer = require('./ServiceLoadBalancer.js');
var Constants = require('../config/constants.js');
var nodeurl = require('url');
var QueueNewNamespaceInstance = require('./QueueNewNamespaceInstance.js');
var _ = require('lodash');
var Errors = require('nodejs-errors');
var Utils = require('./Utils.js');
var nodeUrl = require('url');

function ServiceLoadBalancerManager(config) {
    
    this.config = config;
    
    // create monitoring service 
    this.monitoringService = new MonitoringService({
        registryServiceURIs: this.config.registryServiceURIs,
        serviceLoadbalancerManager: this,
        gateway: config.gateway
    });
    
    // keeps load balancers    
    this.serviceLoadBalancers = [];
    
    this._queueAddRemoveInstance = async.queue(this._queueAddRemoveInstanceProcessTask.bind(this), 1);
    this._queueAddRemoveInstance.pause();
    this._queueAddRemoveInstance.error = function (err, task) {
        if (err) {
            logger.error('ServiceLoadBalancerManager error on _queueAddRemoveInstance:', err, task);
        }
    };
    
    
    EventEmitter.call(this);
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('ServiceLoadBalancerManager error on emit event:', error);
    });
    
    
    // some useful reporting
    setInterval(function (self) {
        var instances = {};
        for(var i=0; i<self.serviceLoadBalancers.length; i++) {
            var lb = self.serviceLoadBalancers[i];
            if (_.isUndefined(instances[lb.serviceName])) {
                instances[lb.serviceName] = {
                    serviceName: lb.serviceName,
                    serviceNamespaces: lb.serviceNamespaces,
                    instances: []
                };
            }
            //console.log('>>>>>', instances[lb.serviceName]);
            for(var j=0; j<lb.instances.length; j++) {
                var inst = lb.instances[j];
                instances[lb.serviceName]['instances'].push('ws' + (inst.secure === true ? 's' : '') + '://' + inst.ip + ':' + inst.port);
            }
        }
        
        console.log('GW SLBM instances:', instances);
    }, 3000, this);
};


inherits(ServiceLoadBalancerManager, EventEmitter);

var o = ServiceLoadBalancerManager.prototype;

/**
 * Detect active service instances and create load balancers for each service
 * @param {type} cb
 * @returns {undefined}
 */
o.init = function (cb) {
    try {
        var self = this;
        
        async.series([
            
            // set listener for MonitoringService events
            function (next) {
                try {
                    
                    // token invalidation
                    self.monitoringService.on(Constants.EVT_TOKEN_INVALIDATED, self._onTokenInvalidated.bind(self));
                    
                    // instance available
                    self.monitoringService.on(Constants.EVT_INSTANCE_AVAILABLE, self._onInstanceAvailable.bind(self));
                    
                    // instance unavailable
                    self.monitoringService.on(Constants.EVT_INSTANCE_UNAVAILABLE, self._onInstanceUnavailable.bind(self));
                    
                    return setImmediate(next, false);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // init monitoring service
            function (next) {
                try {
                    if(self.config.serviceInstances) {
                        var items = {};
                        for(var k in self.config.serviceInstances) {
                            for(var i=0; i<self.config.serviceInstances[k].length; i++) {
                                if (_.isUndefined(self.config.serviceInstances[k][i].namespaces)) {
                                    self.config.serviceInstances[k][i].namespaces = [k];
                                }
                            }
                        }
                        
                        self.monitoringService.serviceInstances = self.config.serviceInstances;
                        return setImmediate(next, false);
                    }
                    self.monitoringService.init(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            
            // create and init load balancers
            function (next) {
                try {
                    var services = self.monitoringService.getAllServices();
                    
                    // create load balancer instances
                    for(var i=0; i<services.length; i++) {
                        var serviceName = services[i];
                        var serviceInstancesConfig = self.monitoringService.getServiceInstances(serviceName);
                        var serviceNamespaces = serviceInstancesConfig[0].namespaces;
                        
                        self.serviceLoadBalancers.push(new ServiceLoadBalancer({
                            serviceName: serviceName,
                            serviceNamespaces: serviceNamespaces,
                            instancesConfig: serviceInstancesConfig
                        }));
                    }
                    
                    // init all load balancers
                    async.mapSeries(self.serviceLoadBalancers, function (lb, cbInit) {
                        try {
                            lb.init(function (err) {
                                if(err) {
                                    return cbInit(err);
                                }
                                
                                lb.on('serviceInstanceDisconnect', self._onServiceInstanceDisconnect.bind(self));
                                lb.on('message', self._onServiceLoadBalancerMessage.bind(self));
                                
                                return cbInit(false);
                            });
                        } catch (e) {
                            return setImmediate(cbInit, e);
                        }
                    }, function (err) {
                        if (err) {
                            // TO DO: do clean up for created LB instances
                            return next(err);
                        }
                        
                        return next(false);
                    });
                    
                } catch(e) {
                    return setImmediate(next, e);
                }
            },
            
            // resume add/remove instance tasks
            function (next) {
                try {
                    self._queueAddRemoveInstance.resume();
                    
                    self.monitoringService.startSendStatistics();
                    
                    // token invalidation
                    //self.monitoringService.on(Constants.EVT_TOKEN_INVALIDATED, self._onTokenInvalidated.bind(self));
                    
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], function (err) {
            return cb(err);
        });
        
    } catch (e) {
        logger.error('Error on ServiceLoadBalancerManager.init()', e);
        return setImmediate(cb, e);
    }
};


o.getUserMessageInstancesIps = function (cb) {
    this.monitoringService.serviceRegistryList(Constants.USER_MESSAGE_SERVICE_NAME, null, function (err, response) {
        try {
            if (err) {
                return cb(err);
            }
            var ips = [];
            var services = response.getContent().services;
            
            for(var i=0; i<services.length; i++) {
                var serviceName = services[i].serviceName;
                if (serviceName !== Constants.USER_MESSAGE_SERVICE_NAME) {
                    continue;
                }
                var uriParsed = nodeUrl.parse(services[i].uri);
                ips.push(uriParsed.hostname);
            }
            return cb(false, ips);
        } catch (e) {
            return cb(e);
        }
    });
};

o._createLoadBalancer = function (serviceName, serviceNamespaces, instanceConfig, cb) {
    try {
        var self = this;
        
        var lb = new ServiceLoadBalancer({
            serviceName: serviceName,
            serviceNamespaces: serviceNamespaces,
            instancesConfig: [instanceConfig]
        });
        
        
        lb.init(function (err) {
            if(err) {
                return cb(err);
            }

            lb.on('serviceInstanceDisconnect', self._onServiceInstanceDisconnect.bind(self));
            lb.on('message', self._onServiceLoadBalancerMessage.bind(self));
            
            self.serviceLoadBalancers.push(lb);
            
            return cb(false);
        });        
    } catch (e) {
        return setImmediate(cb, false);
    }
};

o.serviceExists = function (serviceName) {
    for(var i=0; i<this.serviceLoadBalancers.length; i++) {
        if(this.serviceLoadBalancers[i].serviceName === serviceName) {
            return true;
        }
    }
    
    return false;
};

o.instanceExists = function (uri, andConnected) {
    for(var i=0; i<this.serviceLoadBalancers.length; i++) {
        var lb = this.serviceLoadBalancers[i];
        
        for(var j=0; j<lb.instances.length; j++) {
            var instance = lb.instances[j];
            if (Utils.toWSUri(instance.secure, instance.ip, instance.port) === _.trimEnd(uri, '/')) {
                if (andConnected === true) {
                    return instance.instanceConnection.connected();
                } else {
                    return true;
                }
            }
        }
    }
    
    return false;
};

o.getInstanceByUri = function (uri) {
    for(var i=0; i<this.serviceLoadBalancers.length; i++) {
        var lb = this.serviceLoadBalancers[i];
        
        for(var j=0; j<lb.instances.length; j++) {
            var instance = lb.instances[j];
            if (Utils.toWSUri(instance.secure, instance.ip, instance.port) === _.trimEnd(uri, '/')) {
                return instance;
            }
        }
    }
    return null;
};




/**
 * Find a service load balancer according to message namespace and forward the message.
 * @param {type} clientId
 * @param {type} message
 * @returns {undefined}
 */
o.sendMessage = function (clientId, serviceName, strMessage) {
    try {
        var self = this,
            serviceLoadBalancer = null
        ;
        
        // find load balancer
        for(var i=0; i<self.serviceLoadBalancers.length; i++) {
            if(self.serviceLoadBalancers[i].serviceName === serviceName) {
                serviceLoadBalancer = self.serviceLoadBalancers[i];
                break;
            }
        }
        
        if(serviceLoadBalancer === null) {
            return new Error('ERR_SERVICE_NOT_FOUND');
        }
        
        return serviceLoadBalancer.sendMessage(clientId, strMessage);
        
    } catch (e) {
        logger.error('Error on ServiceLoadBalancerManager.sendMessage', e);
        return new Error('ERR_FATAL_ERROR');
    }
};
/**
 * Emit serviceInstanceDisconnect event. MessageDispatcher listen to this event
 * @param {type} instanceClients
 * @param {type} serviceName
 * @returns {undefined}
 */
o._onServiceInstanceDisconnect = function (instanceClients, serviceName) {
    try {
        this.emit('serviceInstanceDisconnect', instanceClients, serviceName);
    } catch(e) {
        logger.error('Error on ServiceLoadBalancerManager._onInstanceDisconnect', e);
        return e;
    }
};

/**
 * Emit message event. MessageDispatcher listen to this event
 * @param {type} message
 * @returns {undefined}
 */
o._onServiceLoadBalancerMessage = function (strMessage) {
    try {
        this.emit('message', strMessage);
    } catch(e) {
        logger.error('Error on ServiceLoadBalancerManager._onServiceLoadBalancerMessage', e);
        return e;
    }
};


/**
 * Call disconnectClient method for each load balancer
 * @param {type} clientId
 * @returns {e}
 */
o.disconnectClient = function (clientId, disconectMessage) {
    try {
        for(var i=0; i<this.serviceLoadBalancers.length; i++) {
            this.serviceLoadBalancers[i].disconnectClient(clientId, disconectMessage);
        }
    } catch (e) {
        logger.error('ServiceLoadBalancerManager.disconnectClient', e);
        return e;
    }
};


/**
 * Propagate the token invalidated event.
 * @param {type} eventContent
 * @returns {undefined}
 */
o._onTokenInvalidated = function (eventContent) {
    this.emit(Constants.EVT_TOKEN_INVALIDATED, eventContent);
};

/**
 * 
 * @param {type} eventContent
 * @returns {undefined}
 */
o._onInstanceAvailable = function (eventContent) {
    try {
        logger.debug('_onInstanceAvailable:', eventContent);
        var self = this;

        self._queueAddRemoveInstance.push({
            type: 'add',
            serviceName: eventContent.serviceName,
            uri: eventContent.uri,
            serviceNamespaces: eventContent.serviceNamespaces
        });
    } catch (e) {
        logger.error('_onInstanceAvailable catch error', e);
    }
};

/**
 * Find load balancer associated to instance then call removeInstance() method
 * @param {type} eventContent
 * @returns {undefined}
 */
o._onInstanceUnavailable = function (eventContent) {
    try {
        logger.debug('_onInstanceUnavailable:', eventContent);
        var self = this;
        
        self._queueAddRemoveInstance.push({
            type: 'remove',
            serviceName: eventContent.serviceName,
            uri: eventContent.uri,
            serviceNamespaces: eventContent.serviceNamespaces
        });
    } catch (e) {
        logger.error('_onInstanceUnavailable catch error', e);
    }
};

o._validURI = function (uri) {
    if (!_.isString(uri) || !uri.length) {
        return false;
    }
    
    return true;
};

o._addNewInstance = function (serviceName, uri, serviceNamespaces, cb) {
    try {
        logger.debug('Start adding new instance for an existing service', serviceName, uri, serviceNamespaces);
        var self = this;
        
        var lb = self._getLoadBalancerByServiceName(serviceName);
        if (lb instanceof Error) {
            logger.error('SLBM._addNewInstance() error on LB lookup', lb);
            return setImmediate(cb, lb);
        }

        if (lb === null) {
            logger.warn('LB not found for service ' + serviceName);
            return setImmediate(cb, false);
        }

        lb.addNewInstance(uri, cb);
    } catch (e) {
        logger.error('LB manager _addNewInstance', e);
        return setImmediate(cb, e);
    }
    
};

/**
 * Create a load balancer 
 * @param {string} serviceName
 * @param {string} uri
 * @param {array} serviceNamespaces
 * @param {callback} cb
 * @returns {unresolved}
 */
o._addNewLoadBalancer = function (serviceName, uri, serviceNamespaces, cb) {
    try {
        logger.debug('Start adding new LB', serviceName, uri, serviceNamespaces);
        var self = this;
        
        var uriParsed = nodeurl.parse(uri);
        
        self._createLoadBalancer(serviceName, serviceNamespaces, {
            ip: uriParsed.hostname,
            port: uriParsed.port,
            secure: uriParsed.protocol === 'wss:'
        }, function (err) {
            if (err) {
                logger.error('SLBM _addNewLoadBalancer errro create new LB for', serviceName, uri, serviceNamespaces);
                return cb(err);
            }
            logger.debug('SLBM _addNewLoadBalancer new LB created for', serviceName, uri, serviceNamespaces);
            return cb();
        });
    } catch (e) {
        logger.error('LB manager _addNewLoadBalancer', e);
        return setImmediate(cb, e);
    }
};


o._queueAddRemoveInstanceProcessTask = function (task, cb) {
    try {
        logger.debug('SLBM._queueAddRemoveInstanceProcessTask process task', task);
        var self = this;
        var serviceName = task.serviceName;
        var uri = task.uri;
        var serviceNamespaces = task.serviceNamespaces;
        
        if (!self._validURI(uri)) {
            logger.warn('SLBM._queueAddRemoveInstanceProcessTask wrong URI:', task);
            return setImmediate(cb);
        }
        
        if (serviceName === Constants.EVENT_SERVICE_NAME || serviceName === Constants.GATEWAY_SERVICE_NAME) {
            logger.warn('SLBM._queueAddRemoveInstanceProcessTask event service:', task);
            return setImmediate(cb);
        }
        
        if (task.type === 'add') {
            var inst = self.getInstanceByUri(uri);
            if (inst && (inst.connected() || inst.isConnecting())) {
                logger.error('SLBM._queueAddRemoveInstanceProcessTask() instance already exists', task);
                return setImmediate(cb);
            }
            if (self.serviceExists(serviceName)) {
                self._addNewInstance(serviceName, uri, serviceNamespaces, cb);
            } else {
                self._addNewLoadBalancer(serviceName, uri, serviceNamespaces, cb);
            }
        } else if (task.type === 'remove') {
            if (!self.instanceExists(uri)) {
                logger.error('SLBM._queueAddRemoveInstanceProcessTask() instance not exists', task);
                return setImmediate(cb);
            }

            var serviceExists = self.serviceExists(serviceName);
            if (!serviceExists) {
                logger.warn('SLBM._queueAddRemoveInstanceProcessTask() LB for serviceName not found', task);
                return setImmediate(cb);
            }

            var uri = nodeurl.parse(uri);

            var lb = self._getLoadBalancerByServiceName(serviceName);

            if (lb instanceof Error) {
                logger.error('SLBM._queueAddRemoveInstanceProcessTask() _getLoadBalancerByServiceName error', task, lb);
                return setImmediate(cb);
            }

            if (lb === null) {
                logger.warn('SLBM._queueAddRemoveInstanceProcessTask() LB not found for service', task);
                return setImmediate(cb);
            }

            lb.removeInstanceByIpAndPort(uri.hostname, uri.port);
            return setImmediate(cb);
        }
    } catch (e) {
        logger.error('SLBM._queueAddRemoveInstanceProcessTask() error processing task', task);
        return setImmediate(cb, e);
    }
};


o._getLoadBalancerByServiceName = function (serviceName) {
    try {
        for (var i=0; i<this.serviceLoadBalancers.length; i++) {
            if (this.serviceLoadBalancers[i].serviceName === serviceName) {
                return this.serviceLoadBalancers[i];
            }
        }
        
        return null;
    } catch (e) {
        return e;
    }
};

/**
 * Get service name by namespace. Return null if no load balancer is found.
 * @param {string} namespace
 * @returns {nm$_ServiceLoadBalancerManager.o.serviceLoadBalancers|e}
 */
o.getServiceNameByNamespace = function (namespace) {
    try {
        for (var i=0; i<this.serviceLoadBalancers.length; i++) {
            if (this.serviceLoadBalancers[i].serviceNamespaces.indexOf(namespace) > -1) {
                return this.serviceLoadBalancers[i].serviceName;
            }
        }
        
        return null;
    } catch (e) {
        return e;
    }
};

o.getServiceNameByNamespaces = function (namespaces) {
    try {
        for (var i=0; i<this.serviceLoadBalancers.length; i++) {
            if ( this.serviceLoadBalancers[i].serviceNamespaces.length === namespaces.length && 
                 _.difference(this.serviceLoadBalancers[i].serviceNamespaces, namespaces).length === 0) {
                return this.serviceLoadBalancers[i].serviceName;
            }
        }
        
        return null;
    } catch (e) {
        return e;
    }
};


module.exports = ServiceLoadBalancerManager;
