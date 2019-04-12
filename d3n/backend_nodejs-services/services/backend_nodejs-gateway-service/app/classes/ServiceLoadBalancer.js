var _ = require('lodash');
var async  =require('async');
var InstanceConnection = require('./ServiceInstanceConnection.js');
var EventEmitter = require('events').EventEmitter;
var inherits = require('util').inherits;
var logger = require('nodejs-logger')();
var nodeurl = require('url');
var Errors = require('nodejs-errors');

function ServiceLoadBalancer(config) {
    this.config = config;
    
    this.serviceName = this.config.serviceName;
    
    this.serviceNamespaces = this.config.serviceNamespaces;
    
    this.instancesConfig = this.config.instancesConfig;
    
    this.instances = [];
    
    EventEmitter.call(this);
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('ServiceLoadBalancer error on emit event:', error);
    });
    
};

inherits(ServiceLoadBalancer, EventEmitter);

var o = ServiceLoadBalancer.prototype;


o.init = function (cb) {
    try {
        var self = this;

        for(var i=0; i<self.instancesConfig.length; i++) {
            var inst = self.instancesConfig[i];
            //console.log('inst:', inst);
            self.instances.push({
                ip: inst.ip,
                port: inst.port,
                secure: inst.secure,
                clients: [],
                instanceConnection: new InstanceConnection({
                    ip: inst.ip,
                    port: inst.port,
                    secure: inst.secure,
                    serviceName: self.serviceName
                })
            });
        }
        
        // connect to instances
        async.mapSeries(self.instances, function (instance, cbConnect) {
            try {
                instance.instanceConnection.connect(function (err) {
                    if(err) {
                        logger.warn('Error connecting to ' + self.serviceName + ' instance: ', err, instance.instanceConnection.config);
                        return cbConnect();
                    }
                    
                    
                    instance.instanceConnection.on('serviceInstanceDisconnect', self._onInstanceDisconnect.bind(self));
                    instance.instanceConnection.on('message', self._onInstanceMessage.bind(self));
                    return cbConnect();
                });
            } catch (e) {
                return setImmediate(cbConnect, e);
            }
        }, function (err) {
            // remove unconnected instances
            for(var i = self.instances.length-1; i >= 0 ; i--){
                if(!self.instances[i].instanceConnection.connected()){
                    self.instances.splice(i, 1);
                }
            }            
            
            if(err) {
                logger.error('LB for service ' + self.serviceName + ' starting error: ', err);
            } else {
                if (self.instances.length) {
                    logger.info('LB for service ' + self.serviceName + ' started');
                } else {
                    logger.warn('LB for service ' + self.serviceName + ' started but no instance is connected');
                }
            }
            return cb(err);
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};


/**
 * Remove an instance identified by IP and port
 * @returns {undefined}
 */
o.removeInstanceByIpAndPort = function (ip, port) {
    try {
        var self = this;
        for(var i=0; i<self.instances.length; i++) {
            var instance = self.instances[i];
            if (instance.ip === ip && parseInt(instance.port) === parseInt(port)) {
                return self.removeInstance(instance.instanceConnection);
            }
        }
    } catch (e) {
        return e;
    }
};


/**
 * Remove an instance and emit serviceInstanceDisconnect event
 * @param {type} instanceConnection
 * @returns {e}
 */
o.removeInstance = function (instanceConnection) {
    try {
        var self = this;
        for(var i=0; i<self.instances.length; i++) {
            if(self.instances[i].instanceConnection === instanceConnection) {
                logger.debug('SLB removeInstance instance removed:', instanceConnection.config);
                self.emit('serviceInstanceDisconnect', self.instances[i].clients.slice(0), self.serviceName);
                
                // remove the instance
                self.instances[i].clients = [];
                self.instances[i].instanceConnection.destroy();
                self.instances.splice(i, 1);
                break;
            }
        }
    } catch(e) {
        logger.error('ServiceLoadBalancer.removeInstance', e);
        return e;
    }
};

o.addNewInstance = function (uri, cb) {
    try {
        var self = this;
        var uriParsed = nodeurl.parse(uri);
        
        self.instances.push({
            ip: uriParsed.hostname,
            port: uriParsed.port,
            secure: uriParsed.protocol === 'wss:',
            clients: [],
            instanceConnection: new InstanceConnection({
                ip: uriParsed.hostname,
                port: uriParsed.port,
                secure: uriParsed.protocol === 'wss:',
                serviceName: self.serviceName
            })
        });
        
        var instance = self.instances[self.instances.length-1];
        
        instance.instanceConnection.connect(function (err) {
            if(err) {
                logger.warn('Error connecting to new instance: ' + uri, err);
                self.instances.splice(self.instances.length-1, 1);
                return cb(false);
            }

            logger.debug('SLB addNewInstance instance added, uri=', uri);
            instance.instanceConnection.on('serviceInstanceDisconnect', self._onInstanceDisconnect.bind(self));
            instance.instanceConnection.on('message', self._onInstanceMessage.bind(self));
            return cb(false);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._sortInstances = function (a, b) {
    return a.cntClients - b.cntClients;
};

o.getNewInstance = function (clientId) {
    var self = this, clientsPerInstance = [];
    for(var i=0; i<self.instances.length; i++) {
        //if(self.instances[i].instanceConnection.connection.connected !== true ) {
        if(!self.instances[i].instanceConnection.connected()) {
            continue;
        }
        clientsPerInstance.push({
            instanceIdx: i,
            cntClients: self.instances[i].clients.length
        });
    }
    
    if( !clientsPerInstance.length ) {
        return null;
    }
    
    clientsPerInstance.sort(self._sortInstances);
    var idx = clientsPerInstance[0].instanceIdx;
    self.instances[idx].clients.push(clientId);
    return self.instances[idx];
};

o.getClientInstance = function (clientId) {
    var self = this;
    for(var i=0; i<self.instances.length; i++) {
        if(self.instances[i].clients.indexOf(clientId) >= 0) {
            return self.instances[i]; 
        }
    }

    return self.getNewInstance(clientId);
};

/**
 * Find a service instance and send the message.
 * @param {type} clientId
 * @param {type} message
 * @returns {undefined}
 */
o.sendMessage = function (clientId, strMessage) {
    try {
        var instance = this.getClientInstance(clientId);
        if( instance === null ) { // no instance available
            return new Error('ERR_NO_INSTANCE_AVAILABLE');
        }
        
        return instance.instanceConnection.sendMessage( strMessage );
    } catch (e) {
        logger.error('ServiceLoadBalancer.sendMessage', e);
        return new Error('ERR_FATAL_ERROR');
    }
};


/**
 * Emit message event. ServiceLoadBalancerManager listen to this event.
 * @param {type} strMessage
 * @returns {undefined}
 */
o._onInstanceMessage = function (strMessage) {
    try {
        this.emit('message', strMessage);
    } catch (e) {
        logger.error('ServiceLoadBalancer._onInstanceMessage', e);
        return e;
    }
};

/**
 * Find instance that has been disconnected. 
 * Emit serviceInstanceDisconnect event (ServiceLoadBalancerManager listen to this event). 
 * Empty the list of clients associated to disconnected instance.
 * @param {type} instanceConnection
 * @returns {e}
 */
o._onInstanceDisconnect = function (instanceConnection) {
    this.removeInstance(instanceConnection);
};



/**
 * Detect which instances the client is connected to and send the disconect message
 * @param {type} clientId
 * @param {type} disconectMessage
 * @returns {e}
 */
o.disconnectClient = function (clientId, disconectMessage) {
    try {
        for(var i=0; i<this.instances.length; i++) {
            if(this.instances[i].clients.indexOf(clientId) >= 0) {
                _.pull(this.instances[i].clients, clientId);
                this.instances[i].instanceConnection.sendMessage( disconectMessage );
            }
        }
    } catch (e) {
        logger.error('ServiceLoadBalancerManager.disconnectClient', e);
        return e;
    }
};


module.exports = ServiceLoadBalancer;