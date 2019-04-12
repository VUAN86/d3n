var _ = require('lodash');
var RegistryServiceClient = require('nodejs-service-registry-client');
var url = require('url');
var EventEmitter = require('events').EventEmitter;
var inherits = require('util').inherits;
var async = require('async');
var logger = require('nodejs-logger')();
var EventServiceClient = require('nodejs-event-service-client');
var Constants = require('../config/constants.js');
var Utils = require('./Utils.js');
var DatabasesStatisticsProvider = require('nodejs-statistics-providers')['databasesStatisticsProvider'];
var Aerospike = require('nodejs-aerospike');

function MonitoringService(config) {
    
    this.config = _.assign({
        registryServiceURIs: ''
    }, config);
    
    this.config.registryServiceURIs = this.config.registryServiceURIs || '';
    
    
    // create registry service client
    this._registryServiceClient = new RegistryServiceClient(Utils.parseURIs(this.config.registryServiceURIs), Constants.GATEWAY_SERVICE_NAME);
    
    // create event service client
    this._eventServiceClient = new EventServiceClient(this.config.registryServiceURIs, null, Constants.GATEWAY_SERVICE_NAME);
    
    // keeps available instances
    this.serviceInstances = {};
    
    this._gateway = config.gateway;
    
    this._serviceLoadbalancerManager = config.serviceLoadbalancerManager;
    
    this._sendStatisticsInterval = config.sendStatisticsInterval || process.env.SEND_STATISTICS_INTERVAL || 10000;
    
    this._databasesStatisticsProvider = new DatabasesStatisticsProvider({
        aerospikeConnectionModel: Aerospike.getInstance().KeyvalueService.Models.AerospikeConnection
    });
    
    EventEmitter.call(this);
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('MonitoringService error on emit event:', error);
    });
    
};

inherits(MonitoringService, EventEmitter);

var o = MonitoringService.prototype;


/**
 * Get instances by service name
 * @param {type} serviceName
 * @returns {nm$_MonitoringService.o.serviceInstances}
 */
o.getServiceInstances = function (serviceName) {
    return this.serviceInstances[serviceName];
};

/**
 * Get all services
 * @returns {array}
 */
o.getAllServices = function () {
    return _.keys(this.serviceInstances);
};

/**
 * Get all instances
 * @returns {nm$_MonitoringService.o.serviceInstances}
 */
o.getAllInstances = function () {
    return this.serviceInstances;
};




/**
 * Discover available service instances, register to various events.
 * @param {type} cb
 * @returns {unresolved}
 */
o.init = function (cb) {
    try {
        var self = this;
        async.series([
            
            function (next) {
                try {
                    var gconf = self._gateway.config.gateway;
                    var serviceURI = gconf.externUrl;
                    var namespaces = [Constants.GATEWAY_SERVICE_NAME];
                    
                    logger.debug('MonitoringService.init() try register in SR:', 'gateway', serviceURI, namespaces);
                    self._registryServiceClient.register(Constants.GATEWAY_SERVICE_NAME, serviceURI, namespaces, function (err) {
                        if (err) {
                            logger.error('MonitoringService.init() error register in SR:', err);
                            return next(err);
                        }
                        return next();
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // subscribe to instance available event
            function (next) {
                self._subscribeToInstanceAvailable(next);
            },
            
            // subscribe to instance unavailable event
            function (next) {
                self._subscribeToInstanceUnavailable(next);
            },
            
            // subscribe to token invalidated event
            function (next) {
                self._subscribeToTokenInvalidated(next);
            },
            
            // discover available service instances
            function (next) {
                self._discoverAvailableInstances(function (err, instances) {
                    self.serviceInstances = instances;
                    return next(err);
                });
            }
        ], function (err) {
            if (err) {
                logger.error('MonitoringService.init() error', err);
            }
            
            return cb(err);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};


/**
 * Subscribe and set listener for instance unavailable event
 * @param {type} cb
 * @returns {unresolved}
 */
o._subscribeToInstanceUnavailable = function (cb) {
    try {
        var self = this;
        self._eventServiceClient.subscribe(Constants.EVT_INSTANCE_UNAVAILABLE, function (err) {
            try {
                if (err) {
                    logger.error('Error subscribe to instance unavailable event', err);
                    return cb(err);
                }

                self._eventServiceClient.on('event:' + Constants.EVT_INSTANCE_UNAVAILABLE, self._onInstanceUnavailable.bind(self));

                return cb(false);
            } catch (e) {
                return cb(e);
            }
        }, false, {timeout: 20000});
    } catch (e) {
        return setImmediate(cb, e);
    }
    
};

/**
 * Subscribe and set listener for instance available event
 * @param {type} cb
 * @returns {unresolved}
 */
o._subscribeToInstanceAvailable = function (cb) {
    try {
        var self = this;
        self._eventServiceClient.subscribe(Constants.EVT_INSTANCE_AVAILABLE, function (err) {
            try {
                if (err) {
                    logger.error('Error subscribe to instance available event', err);
                    return cb(err);
                }

                self._eventServiceClient.on('event:' + Constants.EVT_INSTANCE_AVAILABLE, self._onInstanceAvailable.bind(self));

                return cb(false);
            } catch (e) {
                return cb(e);
            }
        }, false, {timeout: 20000});
    } catch (e) {
        return setImmediate(cb, e);
    }
    
};


/**
 * Subscribe and set listener for token invalidated event.
 * @param {type} cb
 * @returns {unresolved}
 */
o._subscribeToTokenInvalidated = function (cb) {
    try {
        var self = this;
        self._eventServiceClient.subscribe(Constants.EVT_TOKEN_INVALIDATED, function (err) {
            try {
                if (err) {
                    logger.error('Error subscribe to token invalidated event', err);
                    return cb(err);
                }

                self._eventServiceClient.on('event:' + Constants.EVT_TOKEN_INVALIDATED, self._onTokenInvalidated.bind(self));

                return cb(false);
            } catch (e) {
                return cb(e);
            }
        }, false, {timeout: 20000});
    } catch (e) {
        return setImmediate(cb, e);
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
 * Propagate instance available event
 * @param {type} eventContent
 * @returns {undefined}
 */
o._onInstanceAvailable = function (eventContent) {
    this.emit(Constants.EVT_INSTANCE_AVAILABLE, eventContent);
};

/**
 * Propagate instance unavailable event
 * @param {type} eventContent
 * @returns {undefined}
 */
o._onInstanceUnavailable = function (eventContent) {
    this.emit(Constants.EVT_INSTANCE_UNAVAILABLE, eventContent);
};


o.serviceRegistryList = function (serviceName, serviceNamespace, cb) {
    this._registryServiceClient.list(serviceName, serviceNamespace, cb);
};

/**
 * Get available instances from service registry
 * @param {type} cb
 * @returns {unresolved}
 */
o._discoverAvailableInstances = function (cb) {
    try {
        var self = this;
        self._registryServiceClient.list(null, null, function (err, message) {
            try {
                if (err) {
                    return cb(err);
                }
                
                logger.debug('GW MonitoringService discovered instances', message.getContent().services);

                if (message.getError() !== null) {
                    return cb(message.getError());
                }
                
                var services = message.getContent().services;
                var items = {};
                for(var i=0; i<services.length; i++) {
                    var serviceName = services[i].serviceName;
                    if (serviceName === Constants.EVENT_SERVICE_NAME || serviceName === Constants.GATEWAY_SERVICE_NAME) {
                        continue;
                    }
                    
                    var uriParsed = url.parse(services[i].uri);
                    if (_.isUndefined(items[serviceName])) {
                        items[serviceName] = [];
                    }
                    items[serviceName].push({
                        ip: uriParsed.hostname,
                        port: uriParsed.port,
                        secure: uriParsed.protocol === 'wss:',
                        namespaces: services[i].serviceNamespaces
                    });
                }
                return cb(false, items);
            } catch (e) {
                return cb(e);
            }
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};



o.generateStatistics = function (cb) {
    try {
        var self = this;
        
        var statistics = {};
        
        // service name
        statistics.serviceName = 'gateway';
        
        // uptime
        statistics.uptime = parseInt(process.uptime()*1000);
        
        // memory usage
        statistics.memoryUsage = process.memoryUsage().heapUsed;
        
        // cpu usage
        statistics.cpuUsage = null;
        if (_.isFunction(process.cpuUsage)) {
            statistics.cpuUsage = process.cpuUsage().user;
        }
        
        statistics.countGatewayConnections = 0;
        
        // count of connections
        statistics.countConnectionsToService = self._gateway.countClientConnections();
        
        statistics.connectionsToService = [];
        
        // count of user sessions
        statistics.countSessionsToService = self._gateway.messageDispatcher.countClientSessions();
        
        // connections to other services
        statistics.connectionsFromService = [];
        for(var i=0; i<self._serviceLoadbalancerManager.serviceLoadBalancers.length; i++) {
            var lb = self._serviceLoadbalancerManager.serviceLoadBalancers[i];
            for(var j=0; j<lb.instances.length; j++) {
                var inst = lb.instances[j];
                statistics.connectionsFromService.push({
                    serviceName: lb.serviceName,
                    address: inst.ip,
                    port: parseInt(inst.port)
                });
            }
        }        
        
        statistics.countConnectionsFromService = statistics.connectionsFromService.length;
        
        var ts = self._registryServiceClient.getLastHeartbeatTimestamp();
        statistics.lastHeartbeatTimestamp = (ts === null ? null : parseInt(ts/1000));
        
        self._databasesStatisticsProvider.generateStatistics(function (err, dbStatistics) {
            if (err) {
                return cb(err);
            }
            
            statistics.connectionsToDb = dbStatistics.connectionsToDb;
            
            return cb(false, statistics);
        });
    } catch (e) {
        logger.error('MonitoringService.generateStatistics() catch error:', e);
        return setImmediate(cb, e);
    }
};

o.startSendStatistics = function () {
    this._sendStatistics();
};

o._sendStatistics = function () {
    logger.debug('MonitoringService._sendStatistics() called');
    
    var self = this;
    function _doSendStatistics(cbSendStatistics) {
        try {
            var cbCalled = false;
            var timeoutId;
            function callCb(err, data) {
                if (cbCalled === false) {
                    cbCalled = true;
                    clearTimeout(timeoutId);
                    cbSendStatistics(err, data);
                }
            };
            
            timeoutId = setTimeout(function () {
                callCb(new Error('ERR_TIMED_OUT'));
            }, 10000);
            
            self.generateStatistics(function (err, statistics) {
                if (err) {
                    return callCb(err);
                }
                
                self._registryServiceClient.pushServiceStatistics(statistics, function (errPush) {
                    if (errPush) {
                        return callCb(errPush);
                    }
                    
                    return callCb(false, statistics);
                });
            });
        } catch (e) {
            return setImmediate(callCb, e);
        }
    };
    
    _doSendStatistics(function (err, statisticsSent) {
        if (err) {
            logger.error('MonitoringService._sendStatistics() error:', err);
        } else {
            logger.debug('MonitoringService._sendStatistics() success:', statisticsSent);
        }
        
        setTimeout(self._sendStatistics.bind(self), self._sendStatisticsInterval);
    });
};

module.exports = MonitoringService;