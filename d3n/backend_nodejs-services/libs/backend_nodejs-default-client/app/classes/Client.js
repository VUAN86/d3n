var WebSocketClient = require('websocket').client;
var _ = require('lodash');
var EventEmitter = require('events').EventEmitter;
var inherits = require('util').inherits;
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();
var async = require('async');
var nodeUrl = require('url');
var Errors = require('nodejs-errors');
var Config = require('../config/config.js');
/**
 * @typedef {object} ClientConfig
 * @property {number} reconnectInterval
 * @property {number} reconnectMaxAttempts
 * @property {boolean} secure
 * @property {{ip: string, port: number}} service
 * @property {Object} jwt
 * @property {boolean} protocolLogging
 */

/**
 * Client class represents a client connection
 * within the F4M WebSocket Protocol infrastructure.
 *
 * @constructor
 * @param {ClientConfig} config
 */
function Client(config) {
    this.config = _.assign({
        key: Config.sslKey,
        cert: Config.sslCert,
        checkCert: (process.env.CHECK_CERT ? (process.env.CHECK_CERT === 'true') : true)
    }, config);
    
    this.connection = null;
    this.callbacks = {};
    
    this._seq = 0;
    
    this._reconnectInterval = config.reconnectInterval || 1000; // milliseconds
    
    this._reconnectMaxAttempts = parseInt(config.reconnectMaxAttempts) || 3;
    
    this._doNotReconect = !_.isUndefined(config.doNotReconnect) ? config.doNotReconnect : false;
    
    this._connectTimeout = _.isInteger(config.connectTimeout) ? config.connectTimeout : 10000;
    
    this._reconnectForever = (config.reconnectForever === true ? true : false);
    this._reconnectForeverInterval = _.isInteger(config.reconnectForeverInterval) ? config.reconnectForeverInterval : 3000;
    this._isInReconnectForeverState = false;
    
    this._protocolLogging = false;
    if (!_.isUndefined(config.protocolLogging)) {
        this._protocolLogging = config.protocolLogging;
    } else if (process.env.PROTOCOL_LOGGING) {
        this._protocolLogging = process.env.PROTOCOL_LOGGING;
    }
    
    this._serviceNamespace = config.serviceNamespace;
    
    this._serviceNamespaceURI = undefined;
    
    this._registryServiceURIs = config.registryServiceURIs || process.env.REGISTRY_SERVICE_URIS;
    
    this._serviceRegistryClient = config.serviceRegistryClient;
    
    this._queue = async.queue(this._queueSendMessage.bind(this), 1);
	
	// Flag showing, if system currently tries to connect or not, in order to prevent multiple connections
	this._tryingToConnect = false;
    
    // true when reconnection failed or when client diconnects and  _doNotReconect = true
    this._isCompletelyDisconnected = undefined;
    
    this._validateFullMessage = true;
    if (_.has(config, 'validateFullMessage')) {
        this._validateFullMessage = config.validateFullMessage;
    } else if (_.has(process.env, 'VALIDATE_FULL_MESSAGE')) {
        this._validateFullMessage = (process.env.VALIDATE_FULL_MESSAGE === 'true');
    }
    
    // keeps on the road messages
    this._onTheRoadMessages = {};
    
    EventEmitter.call(this);
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('DefaultClient error event emitted:', error);
    });
    
    this._autoConnect = (config.autoConnect === true ? true : false);
    this._autoConnectStarted = false;
}
inherits(Client, EventEmitter);

var o = Client.prototype;


o.removeMessageCallbacks = function (message) {
    var seq = message.getSeq();
    delete this.callbacks[seq];
    delete this._onTheRoadMessages[seq];
};

/**
 * Return message queue.
 * @returns {nm$_Client.o._queue}
 */
o.getQueue = function () {
    return this._queue;
};

/**
 * Unique sequence number generator for this connection
 * @returns {number}
 */
o.getSeq = function () {
    return ++this._seq;
};

/*
o._doReconnect = function (cb) {
    try {
        var self = this;
        if (self._serviceNamespace) {
            self._reconnectByNamespace(self._serviceNamespace, cb);
        } else {
            self._reconnectByIpPort(self.config.service.ip, self.config.service.port, self.config.secure, cb);
        }
    } catch (e) {
        logger.error('DefaultClient error handling reconnect', e);
        return setImmediate(cb, e);
    }
    
};
*/


o._reconnectTryForever = function (emitEvents, cb) {
    try {
        var self = this;
        if (self._isInReconnectForeverState) {
            logger.error('DefaultClient _reconnectTryForever called while in reconnect forever state!');
            return;
        }
        
        self._isInReconnectForeverState = true;
        var oldServiceNamespaceURI = self._serviceNamespaceURI;
        
        logger.debug('DefaultClient trying to reconnect forever');
        
        async.forever(function (next) {
            //logger.debug('DefaultClient trying to reconnect forever, try');
            if (self._serviceNamespace) {
                logger.debug('DefaultClient._reconnectTryForever() _connectByNamespace calling:', self._serviceNamespace);
                self._connectByNamespace(self._serviceNamespace, function (err) {
                    logger.debug('DefaultClient._reconnectTryForever() _connectByNamespace call err:', (err && err.message ? err.message: err));
                    // retry
                    if(err) {
                        return setTimeout(next, self._reconnectForeverInterval);
                    }
                    // connected, do not retry anymore
                    return next(true);
                });
            } else {
                logger.debug('DefaultClient._reconnectTryForever() _connectByIpPort calling:', self.config.service);
                self._connectByIpPort(self.config.service.ip, self.config.service.port, self.config.secure, function (err) {
                    logger.debug('DefaultClient._reconnectTryForever() _connectByIpPort call err:', (err && err.message ? err.message: err));
                    // retry
                    if(err) {
                        return setTimeout(next, self._reconnectForeverInterval);
                    }
                    // connected, do not retry anymore
                    return next(true);
                });
            }
            
        }, function (result) {
            self._isInReconnectForeverState = false;
            if (result instanceof Error) {
                return cb(result);
            } 
            
            // it is connected
            
            if (emitEvents !== true) {
                return cb();
            }
            
            if (self._serviceNamespace) {
                if (oldServiceNamespaceURI === self._serviceNamespaceURI) { // reconnected to same instance
                    logger.debug('DefaultClient emit reconnected by namespace on try forever', self.config.service, self._serviceNamespace);
                    self._emitEventReconnected();
                } else { // reconnected to a different instance
                    logger.debug('DefaultClient emit reconnectedToNewInstance by namespace on try forever', self.config.service, self._serviceNamespace);
                    self._emitEventReconnectedToNewInstance();
                }
            } else {
                logger.debug('DefaultClient emit reconnected on try forever', self.config.service, self._serviceNamespace);
                self._emitEventReconnected();
            }
            
            return cb();
        });
    } catch (e) {
        logger.error('DefaultClient error on _reconnectTryForever', e);
        self._isInReconnectForeverState = false;
        return setImmediate(cb, e);
    }
};

/**
 * Tries to reconnect to service
 * @private
 */
o._reconnect = function () {
    var self = this;
    
    if (!_.isFinite(this._reconnectMaxAttempts) || !(this._reconnectMaxAttempts > 0)) {	
        return;
    }
    
    logger.debug('DefaultClient trying to reconnect to ', self.config.service, self._serviceNamespace);
    if (self._serviceNamespace) {
        self._reconnectByNamespace(self._serviceNamespace, function (err, reconnected) {
            if (reconnected === 'same-instance') {
                logger.debug('DefaultClient emit reconnected', self.config.service, self._serviceNamespace);
                self._emitEventReconnected();
            } else if (reconnected === 'another-instance') {
                logger.debug('DefaultClient emit reconnectedToNewInstance', self.config.service, self._serviceNamespace);
                self._emitEventReconnectedToNewInstance();
            } else {
                if (self._reconnectForever) { // try reconect forever
                    self._reconnectTryForever(true, function () {});
                } else {
                    self._queueCleanUp('ERR_RECONNECTING_FAILED');
                    logger.debug('DefaultClient emit reconnectingFailed', self.config.service, self._serviceNamespace);
                    self.emit('reconnectingFailed');
                }
            }
        });
    } else {
        self._reconnectByIpPort(self.config.service.ip, self.config.service.port, self.config.secure, function (err, reconnected) {
            if (reconnected === true) {
                logger.debug('DefaultClient emit reconnected', self.config.service, self._serviceNamespace);
                self._emitEventReconnected();
            } else {
                if (self._reconnectForever) { // try reconect forever
                    self._reconnectTryForever(true, function () {});
                } else {
                    self._queueCleanUp('ERR_RECONNECTING_FAILED');
                    logger.debug('DefaultClient emit reconnectingFailed', self.config.service, self._serviceNamespace);
                    self.emit('reconnectingFailed');
                }
            }
        });
    }
};

o._emitEventReconnected  = function () {
    var self = this;
    if (self.listenerCount('reconnected')) {
        // call asyncronious all event listeners
        async.mapSeries(self.listeners('reconnected'), function (listener, cbListener) {
            listener(function (err) {
                if (err) {
                    logger.warn('DefaultClient reconnected event listener error:', err);
                }
                return cbListener();
            });
        }, function () {
            // resume the queue after all listeners have been called
            self.getQueue().resume();
        });
    } else {
        self.getQueue().resume();
    }
};

o._emitEventReconnectedToNewInstance  = function () {
    var self = this;
    
    if (self.listenerCount('reconnectedToNewInstance')) {
        // call asyncronious all event listeners
        async.mapSeries(self.listeners('reconnectedToNewInstance'), function (listener, cbListener) {
            listener(function (err) {
                if (err) {
                    logger.warn('DefaultClient reconnectedToNewInstance event listener error:', err);
                }
                return cbListener();
            });
        }, function () {
            // resume the queue after all listeners have been called
            self.getQueue().resume();
        });
    } else {
        self.getQueue().resume();
    }
};

/**
 * Tries to reconnect to a service instance using ip and port.
 * @param {string} ip
 * @param {integer|string} port
 * @param {boolean} secure
 * @param {function} cb
 * @returns {unresolved}
 */
o._reconnectByIpPort = function (ip, port, secure, cb) {
    try {
        
        var self = this;

        async.retry({
            times: self._reconnectMaxAttempts,
            interval: self._reconnectInterval
        }, function (callback) {
            self._connectByIpPort(ip, port, secure, function (err) {
                if (err) {
                    // retry
                    return callback(true);
                } else {
                    // do not retry, return true
                    return callback(false, true);
                }
            });
        }, function (err, reconnected) {
            return cb(null, reconnected === true ? true : false);
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Tries to reconnect to a service instance using service namespace. 
 * First it try to reconnect to same instance, if fails obtain a list of available instances form service registry an try to connect to one of them.  
 * @param {string} namespace
 * @param {function} cb
 * @returns {undefined}
 */
o._reconnectByNamespace = function (namespace, cb) {
    var self = this;
    
    var uriParsed = nodeUrl.parse(self._serviceNamespaceURI);
    
    self._reconnectByIpPort(uriParsed.hostname, uriParsed.port, (uriParsed.protocol === 'wss:'), function (err, reconnected) {
        if (reconnected === true) {
            return cb(null, 'same-instance');
        } else {
            self._connectByNamespace(namespace, function (err) {
                if (err) {
                    return cb(null, false);
                } else {
                    return cb(null, 'another-instance');
                }
            });
        }
    });
};


/**
 * Opens a connection to the service specified inside the configuration
 * @param {Function} cb
 * @returns {*}
 */
o.connect = function (cb) {
    try {
        var self = this;
        if(!_.isFunction(cb)) {
            logger.warn('DefaultClient connect: callback not provided!');
            return;
        }
		
	if (self._tryingToConnect) {
            logger.warn('DefaultClient connect: already trying to connect!');
            return setImmediate(cb, 'connect: already trying to connect!');
        }	
        self._tryingToConnect = true;
        
        function afterConnect(err) {
            self._tryingToConnect = false;
            if (err) {
                return cb(err);
            }
            
            self._isCompletelyDisconnected = false;
            self.getQueue().resume();
            return cb();
        };
        
        if (self._serviceNamespace) {
            self._connectByNamespace(self._serviceNamespace, afterConnect);
        } else {
            self._connectByIpPort(self.config.service.ip, self.config.service.port, self.config.secure, afterConnect);
        }
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._trimCert = function (cert) {
    return cert.replace('-----BEGIN CERTIFICATE-----', '').replace('-----END CERTIFICATE-----', '').replace(/\n/g, '');
};

/**
 * Connects to service instance directly using ip and port.
 * @param {string} ip
 * @param {integer|string} port
 * @param {boolean} secure
 * @param {function} cb
 * @returns {unresolved}
 */
o._connectByIpPort = function (ip, port, secure, cb) {
    try {
        var self = this;
        var noSignOfLife = true;
        
        var tlsOptions = {
            rejectUnauthorized: false
        };
        
        if (secure === true) {
            tlsOptions.key = self.config.key;
            tlsOptions.cert = self.config.cert;
        }
        
        var wsClient = new WebSocketClient({
            tlsOptions: tlsOptions
        });
        
        // create URL and connect
        wsClient.connect('ws' + (secure === true ? 's': '') + '://' + ip + ':' + port + '/', null, null, self.config.headers || null);

        wsClient.on('connectFailed', function (error) {
            noSignOfLife = false;
            return self._connectFailed(error, cb);
        });

        wsClient.on('connect', function (connection) {
            noSignOfLife = false;
            
            if (self.config.checkCert === true && secure === true) {
                var cert = connection.socket.getPeerCertificate();
                
                if (cert === null || _.isEmpty(cert)) {
                    logger.error('DefaultClient no cert provided by service', connection.remoteAddress);
                    connection.drop();
                    return cb(new Error(Errors.ERR_VALIDATION_FAILED.message));
                }
                var found = false;
                for(var i=0; i<Config.trustedCerts.length; i++) {
                    if (Config.trustedCerts[i] === cert.raw.toString('base64')) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    logger.error('DefaultClient not a trusted cert provided by service', connection.remoteAddress);
                    connection.drop();
                    return cb(new Error(Errors.ERR_VALIDATION_FAILED.message));
                }
            }
            
            return self._onconnect(connection, cb);
        });
        
        
        setTimeout(function () {
            if (noSignOfLife === true) {
                wsClient.removeAllListeners();
                wsClient.abort();
                return cb(new Error('ERR_CONNECT_TIMEOUT'));
            }
        }, self._connectTimeout);
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Connects to service instance using namespace. Obtain list of available instances from service registry then connect to one of them. 
 * @param {string} namespace
 * @param {function} cb
 * @returns {unresolved}
 */
o._connectByNamespace = function (namespace, cb) {
    try {
        var self = this;
        var uris = [];
        async.series([
            // find namespace URIs
            function (next) {
                self._findNamespaceURIs(namespace, function (err, foundUris) {
                    try {
                        if (err) {
                            return next(err);
                        }
                        if (!_.isArray(foundUris) || !foundUris.length) {
                            return next(new Error('ERR_NO_NAMESPACE_INSTANCE_FOUND'));
                        }

                        uris = foundUris;

                        return next();
                    } catch (e) {
                        return next(e);
                    }
                    
                });
            },
            
            // connect to an instance
            function (next) {
                async.someSeries(uris, function (uri, cbConnect) {
                    try {
                        var uriParsed = nodeUrl.parse(uri);
                        
                        self._connectByIpPort(uriParsed.hostname, uriParsed.port, (uriParsed.protocol === 'wss:'), function (err) {
                            if (err) {
                                return cbConnect(null, false);
                            }
                            
                            self._serviceNamespaceURI = uri;
                            
                            return cbConnect(null, true);
                        });

                    } catch (e) {
                        return cbConnect(e);
                    }
                }, function (err, connected) {
                    if (err) {
                        return next(err);
                    }
                    
                    if (!connected) {
                        return next(new Error('ERR_NO_SERVICE_INSTANCE_AVAILABLE'));
                    }
                    
                    return next();
                });
            }
        ], function (err) {
            return cb(err);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Find namespace available instances. 
 * If namespace = serviceRegistry then simply return REGISTRY_SERVICE_URIS enviroment variable
 * If namespace != serviceRegistry then obtain list of available instances from service registry
 * @param {string} namespace
 * @param {function} cb
 * @returns {unresolved}
 */
o._findNamespaceURIs = function (namespace, cb) {
    try {
        var self = this;
        var uris = [];
        if (namespace === 'serviceRegistry') {
            if (_.isString(self._registryServiceURIs) && _.isArray(self._registryServiceURIs.split(','))) {
                uris = self._registryServiceURIs.split(',');
            }
            
            return setImmediate(cb, false, uris);
        }
        
        self._serviceRegistryClient.list(null, namespace, function (err, response) {
            try {
                if (err) {
                    return cb(err);
                }

                if (response.getError() !== null) {
                    return cb(new Error(response.getError()));
                }
                
                if (response.getContent() === null) {
                    return cb(new Error('ERR_WRONG_CONTENT_SR_LIST'));
                }

                var services = response.getContent().services;
                for(var i=0; i<services.length; i++) {
                    if (services[i].serviceNamespaces.indexOf(namespace) >= 0 ) {
                        var uri = services[i].uri;
                        if (!_.isString(uri) || !uri.length) {
                            logger.warn('DefaultClient wrong URI received from SR:', uri);
                            continue;
                        }
                        uris.push(uri);
                    }
                }
                
                return cb(false, uris);
            } catch (e) {
                return cb(e);
            }
            
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};


/**
 * Returns connection status
 * @returns {boolean}
 */
o.connected = function () {
    if (this.connection) {
        return this.connection.connected;
    }
    return false;
};

/**
 * Disconnects service
 * @param {int} reasonCode
 * @param {string} description
 * @param {boolean} doNotReconnect=false
 */
o.disconnect = function (reasonCode, description, doNotReconnect) {
    try {
        this._doNotReconect = !_.isUndefined(doNotReconnect) ? doNotReconnect : false;
        if (this.connection) {
            this.connection.close(reasonCode, description);
        }
        if(this._serviceRegistryClient) {
            this._serviceRegistryClient.disconnect();
        }
    } catch (e) {
        throw e;
    }
};

/**
 * Handles errors on connection, callback is the callback
 * provided by the client in the connection method
 * @param error
 * @param cb
 * @returns {*}
 * @private
 */
o._connectFailed = function(error, cb) {
    logger.error('DefaultClient _connectFailed():', error);
    return cb(error);
};

/**
 * Handles successful connection
 * @param connection
 * @param cb
 * @returns {*}
 * @private
 */
o._onconnect = function(connection, cb) {
    try {
        var self = this;
        logger.debug('DefaultClient _onconnect():', self.config.service, self._serviceNamespace);
        
        this.connection = connection;

        this.connection.on('message', this._onmessage.bind(this));

        this.connection.on('close', this._onclose.bind(this));

        if (this._protocolLogging) {
            if (!connection.socket._getsockname) {
                var sockName = connection.socket.socket._peername;
            } else {
                var sockName = connection.socket._getsockname();
            }
            self.connection.logData = {
                sockName: sockName,
                remoteAddress: connection.socket.remoteAddress,
                remotePort: connection.socket.remotePort
            };
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'connection',
                action: 'created',
                from: this.connection.logData.sockName.address + ":" + this.connection.logData.sockName.port,
                to:  this.connection.logData.remoteAddress + ':' + this.connection.logData.remotePort,
                is_client: true
            }));
        }
        return cb(null);
    } catch (e) {
        logger.error('DefaultClient default client error on connect handler', e);
        return cb(e);
    }
};

/**
 * Handles websockets onclose event
 * @param {number} reasonCode
 * @param {string} description
 * @private
 */
o._onclose = function (reasonCode, description) {
    var self = this;
    logger.debug('DefaultClient _onclose():', self.config.service, self._serviceNamespace);
    
    if (this._autoConnect && !this._reconnectForever) {
        this.connection.removeAllListeners();
        this.connection = null;
        return;
    }
    
    this.getQueue().pause();
    if (this._protocolLogging) {
        logger.info(JSON.stringify({
            timestamp: new Date().getTime(),
            identification: process.cwd()+":"+process.pid,
            type: 'connection',
            action: 'removed',
            from: this.connection.logData.sockName.address + ":" + this.connection.logData.sockName.port,
            to:  this.connection.logData.remoteAddress + ':' + this.connection.logData.remotePort,
            is_client: true
        }));
    }
    
    this.connection.removeAllListeners();
    this.connection = null;
    
    this.emit('connectionClose', reasonCode, description);
    if (!this._doNotReconect) {
        this._reconnect();
    } else {
        this._queueCleanUp('ERR_CONNECTION_CLOSED');
    }
};

/**
 * Handles received message
 * If received message acknowledges a sent message,
 * the specified callback listener is executed
 *
 * Emits "message" for all received messages
 * Emits "message:"messageNameWithoutNamespace"" for messages without an acknowledgement
 *
 * @param {string} message
 * @private
 */
o._onmessage = function (message) {
    try {
        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'data',
                action: 'received',
                from:  this.connection.logData.remoteAddress + ':' + this.connection.logData.remotePort,
                to: this.connection.logData.sockName.address + ":" + this.connection.logData.sockName.port,
                message: message,
                is_client: true
            }));
        }
    } catch (e) {
        logger.error('DefaultClient _onmessage error protocolLogging:', e, message, this._protocolLogging);
    }

    try {
        var self = this,
            resMessage = new ProtocolMessage(JSON.parse(message.utf8Data)),
            ack = resMessage.getAck()
        ;
        
        // validate response message against JSON schema
        if (!(this._validateFullMessage === false)) {
            var res = resMessage.isValid(resMessage.getMessage());
            if (res !== true) {
                logger.error('DefaultClient JSON schema invalid', resMessage.lastValidationError, resMessage);
                if (resMessage.getError() === null) {
                    resMessage.setError(Errors.ERR_VALIDATION_FAILED);
                }
            }
        }
        
        self.emit('message', resMessage);
        
        if(ack === null || !ack.length) {
            self.emit("message:" + resMessage.getMessageName(), resMessage);
            return;
        }

        for (var i = 0; i < ack.length; i++) {
            var cbKey = ack[i];
            var callback = self.callbacks[cbKey];
            if (callback) {
                //callback(false, resMessage);
                setImmediate(callback, false, resMessage);
                self.callbacks[cbKey] = undefined;
                delete self.callbacks[cbKey];
                delete self._onTheRoadMessages[ack[i]];
            }
        }
    } catch (e) {
        logger.error('DefaultClient _onmessage error:');
        logger.error(e);
    }
};

/**
 * Sends an authentication request to the server
 * @param {Function} cb
 * @returns {Number}
 */
o.authenticate = function (cb) {
    try {
        var self = this;

        var message = new ProtocolMessage();
        message.setMessage('global/auth');
        message.setToken(self.config.jwt);
        message.setSeq(this.getSeq());

        self.sendMessage(message, cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Sends a message to the server. Basically push the message into messages queue
 * In case a sequence number is defined, connect a callback listener
 * @param {ProtocolMessage} message
 * @param {Function} cb
 * @param {Boolean} addInFront - if true add in front of the messages queue
 * @param {Boolean} dontWaitResponse - if to wait for a response or not
 * @returns {Number}
 */
o.sendMessage = function (message, cb, addInFront, dontWaitResponse) {
    try {
        var self = this;
        
        if(self._autoConnect) {
            if (self._reconnectForever && !self._autoConnectStarted) {
                self._autoConnectStarted = true;
                self.getQueue().pause();
                
                self._reconnectTryForever(false, function (err) {
                    if (err) {
                        // if error reject queued message with an eror
                        self._queueCleanUp('ERR_CONNECTION_ERROR');
                    } else {
                        // resume the queue
                        self.getQueue().resume();
                    }
                });
            } else if (!self.connected() && !self._autoConnectStarted) {
                self._autoConnectStarted = true;
                self.getQueue().pause();
                self.connect(function (err) {
                    self._autoConnectStarted = false;
                    if (err) {
                        // if error reject queued message with an error
                        self._queueCleanUp('ERR_CONNECTION_ERROR', true, false);
                    } else {
                        // resume the queue
                        self.getQueue().resume();
                    }
                });
            }
        } else {
            if (self._isCompletelyDisconnected === true) {
                return setImmediate(cb, new Error('ERR_COMPLETELY_DISCONNECTED'));
            }
        }
        
        var seq = message.getSeq();
        var queueMethod = (addInFront === true ? 'unshift': 'push');
        if(self._seqIsEmpty(seq) || dontWaitResponse === true) { // just send the message, do not wait for response
            self.getQueue()[queueMethod](message, function (err) {
                return setImmediate(cb, err);
            });
        } else {
            self.callbacks[seq] = cb;
            self.getQueue()[queueMethod](message, function (err) {
                if (err) {
                    // if something went wrong when sending the message, remove callback from callbacks object and return the error
                    delete self.callbacks[message.getSeq()];
                    return setImmediate(cb, err);
                }
                // do nothing, cb will be called when service send response
            });
        }
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Function that is called by async queue for each message in the queue. Just send the message through websocket connection.
 * @param {ProtocolMessage} message
 * @param {Function} cb
 * @returns {unresolved}
 */
o._queueSendMessage = function (message, cb) {
    try {
        var self = this;
        var plainMessage = JSON.stringify(message.getMessageContainer());

        if (self._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'data',
                action: 'sent',
                from: self.connection.logData.sockName.address + ":" + self.connection.logData.sockName.port,
                to: self.connection.logData.remoteAddress + ':' + self.connection.logData.remotePort,
                message: plainMessage,
                is_client: true
            }));
        }
        
        self.connection.sendUTF(plainMessage);
        
        var seq = message.getSeq();
        if( !self._seqIsEmpty(seq) ) {
            self._onTheRoadMessages[seq] = true;
        }
        
        return setImmediate(cb);
    } catch (e) {
        logger.error('DefaultClient._queueSendMessage error', e);
        return setImmediate(cb, e);
    }
};

o._seqIsEmpty =  function (seq) {
    return (_.isUndefined(seq) || _.isNull(seq) ? true : false);
};

/**
 * Get queue items.
 * @returns {Array|Error}
 */
o._queueGetItems = function () {
    try {
        if (!_.isUndefined(this.getQueue().tasks)) {
            return this.getQueue().tasks;
        } else {
            return this.getQueue()._tasks;
        }
    } catch (e) {
        logger.error('DefaultClient._queueGetMessages error', e);
        return e;
    }
};

/**
 * Clean up the queue. Call callbacks associated to queued messages, kill the queue.
 * @returns {null|Error}
 */
o._queueCleanUp = function (errorMessage, killQueue, setCompletelyDisconnected) {
    try {
        
        var _killQueue = (killQueue === false ? false : true);
        var _setCompletelyDisconnected = (setCompletelyDisconnected === false ? false : true);
        
        if (_setCompletelyDisconnected) {
            this._isCompletelyDisconnected = true;
        }
        
        
        // call callback with error for messages on the road
        for(var seq in this._onTheRoadMessages) {
            if(!this._onTheRoadMessages.hasOwnProperty(seq)) {
                continue;
            }
            
            if (!_.isFunction(this.callbacks[seq])) {
                continue;
            }
            
            setImmediate(this.callbacks[seq], new Error(errorMessage));
            delete this.callbacks[seq];
        }
        this._onTheRoadMessages = {};
        
        var items = this._queueGetItems();
        if (items instanceof Error) {
            return items;
        }
        
        // iterate over queued messages and call associated callback with error
        for(var i=0; i<items.length; i++) {
            
            if (!_.isFunction(items[i].callback)) {
                continue;
            }
            
            setImmediate(items[i].callback, new Error(errorMessage));
            
        }
        
        this.callbacks = {};
        
        if (_killQueue) {
            this.getQueue().kill();
        }
        
        
    } catch (e) {
        logger.error('DefaultClient._queueCleanUp error', e);
        return e;
    }
};

module.exports = Client;


