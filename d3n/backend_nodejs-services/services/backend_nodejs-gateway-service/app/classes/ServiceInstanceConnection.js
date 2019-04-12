var WebSocketClient = require('websocket').client;
var _ = require('lodash');
var EventEmitter = require('events').EventEmitter;
var inherits = require('util').inherits;
var logger = require('nodejs-logger')();
var Errors = require('nodejs-errors');
var async = require('async');
var Config = require('../config/config.js');

function ServiceInstanceConnection(config) {
    this.config = _.assign({
        checkCert: (process.env.CHECK_CERT ? (process.env.CHECK_CERT === 'true') : true)
    }, config);
    
    this.connection = null;
    this.callbacks = {};

    this._protocolLogging = process.env.PROTOCOL_LOGGING || config.protocolLogging || false;
    
    this._connectTimeout = 8000;
    
    this._destroyed = false;
    
    this._isConnecting = false;
    
    EventEmitter.call(this);
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('ServiceInstanceConnection error on emit event:', error);
    });
    
    // config will contain server adress, private key  etc
};
inherits(ServiceInstanceConnection, EventEmitter);

var o = ServiceInstanceConnection.prototype;

o.isConnecting = function () {
    return this._isConnecting;
};


/**
 * Connect to the service instance using the websocket protocol.
 * @param {type} cb
 * @returns {unresolved}
 */
o.connect = function (cb) {
    var self = this;
    self._isConnecting = true;
    
    var maxTime = Config.connectMaxTime;
    var start = Date.now();
    var retryCount = 0;
    function _doConnect() {
        self._connect(function (err) {
            retryCount++;
            if (err) {
                if ((Date.now()-start) < maxTime) {
                    var interval = self._calculateInterval(retryCount);
                    logger.debug('ServiceInstanceConnection.connect() will retry:', self.config);
                    return setTimeout(_doConnect, interval);
                } else {
                    logger.error('ServiceInstanceConnection.connect() cant connect even after retries:', self.config);
                    self._isConnecting = false;
                    return cb(err);
                }
            }
            logger.debug('ServiceInstanceConnection.connect() connect successfully to instance:', self.config);
            self._isConnecting = false;
            return cb();
        });
    };
    
    _doConnect();
};


o._trimCert = function (cert) {
    return cert.replace('-----BEGIN CERTIFICATE-----', '').replace('-----END CERTIFICATE-----', '').replace(/\n/g, '');
};

o._connect = function (cb, r) {
    try {
        if (this._destroyed) {
            return setImmediate(cb, new Error('ERR_INSTANCE_DESTROYED'));
        }
        
        var self = this;
        var noSignOfLife = true;
        var timeoutId;
        
        var cbCalled = false;
        function callCB(err, res) {
            if(cbCalled === false) {
                cbCalled = true;
                if (timeoutId) {
                    clearTimeout(timeoutId);
                }
                cb(err, res);
            }
        };
        
        // clean up
        try {
            self.wsClient.removeAllListeners();
            self.connection.removeAllListeners();
            delete self.connection;
            delete self.wsClient;
        } catch (e) {}
        
        var tlsOptions = {
            rejectUnauthorized: false
        };
        
        
        if (self.config.secure === true) {
            tlsOptions.key = Config.gateway.keyAsString;
            tlsOptions.cert = Config.gateway.certAsString;
        }
        
        
        self.wsClient = new WebSocketClient({
            tlsOptions: tlsOptions
        });
        
        
        self.wsClient.connect((self.config.secure === true ? 'wss' : 'ws')  + '://' + self.config.ip + ':' + self.config.port + '/', null, null, {
            service_name: 'gateway'
        });
        self.wsClient.on('connect', function (connection) {
            noSignOfLife = false;
            
            if (self.config.checkCert === true && self.config.secure === true) {
                var cert = connection.socket.getPeerCertificate();
                
                
                if (cert === null || _.isEmpty(cert)) {
                    logger.error('ServiceInstanceConnection no cert provided by service', connection.remoteAddress, self.config.serviceName);
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
                    logger.error('ServiceInstanceConnection not a trusted cert provided by service', connection.remoteAddress, self.config.serviceName);
                    connection.drop();
                    return cb(new Error(Errors.ERR_VALIDATION_FAILED.message));
                }
            }
            
            
            self.connection = connection;
            self.connection.socket.setKeepAlive(true, 2000);
            
            // load balancer listen to this event
            self.connection.on('message', function (message) {
                if (self._protocolLogging) {
                    logger.info(JSON.stringify({
                        timestamp: new Date().getTime(),
                        identification: process.cwd()+":"+process.pid,
                        type: 'data',
                        action: 'received',
                        from:  self.connection.logData.remoteAddress + ':' + self.connection.logData.remotePort,
                        to: self.connection.logData.sockName.address + ":" + self.connection.logData.sockName.port,
                        message: message.utf8Data,
                        is_client: true
                    }));
                }
                self.emit('message', message.utf8Data);
            });
            
            // load balancer listen to this event
            self.connection.on('close', function (reasonCode, description) {
                logger.error('GW instance on close(reasonCode, description, self.config):', reasonCode, description, self.config);
                if (self._protocolLogging) {
                    logger.info(JSON.stringify({
                        timestamp: new Date().getTime(),
                        identification: process.cwd()+":"+process.pid,
                        type: 'connection',
                        action: 'removed',
                        from: self.connection.logData.sockName.address + ":" + self.connection.logData.sockName.port,
                        to:  self.connection.logData.remoteAddress + ':' + self.connection.logData.remotePort,
                        is_client: true
                    }));
                }
                
                self._reconnect();
            });
            
            self.connection.on('error', function (err) {
                logger.error('GW instance on error:', err, self.config);
            });
            
            if (self._protocolLogging) {
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
                    from: self.connection.logData.sockName.address + ":" + self.connection.logData.sockName.port,
                    to:  self.connection.logData.remoteAddress + ':' + self.connection.logData.remotePort,
                    is_client: true
                }));
            }

            return callCB(false);
        });

        self.wsClient.on('connectFailed', function (err) {
            logger.error('GW instance on connectFailed:', err, self.config);
            noSignOfLife = false;
            return callCB(err);
        });
        
        timeoutId = setTimeout(function () {
            if (noSignOfLife === true) {
                self.wsClient.removeAllListeners();
                self.wsClient.abort();
                return cb(new Error('ERR_CONNECT_TIMEOUT'));
            }
        }, self._connectTimeout);
        
        
    } catch (e) {
        logger.error('GW ServiceInstanceConnection._connect', e);
        return setImmediate(cb, e);
    }
};


o._calculateInterval = function (currentRetry) {
    var levels = [
        {retryCountGT: 1, interval: 200},
        {retryCountGT: 5, interval: 1000},
        {retryCountGT: 10, interval: 2000},
        {retryCountGT: 20, interval: 5000},
        {retryCountGT: 50, interval: 20000},
        {retryCountGT: 100, interval: 30000}
    ];

    for(var i=levels.length-1; i>=0; i--) {
        var item = levels[i]; 
        if (currentRetry >= item.retryCountGT) {
            return item.interval;
        }
    }
};

o._reconnect = function () {
    try {
        var self = this;
        logger.debug('GW trying to reconnect to instance:', self.config);
        
        self._isConnecting = true;
        
        var maxTime = Config.reconnectMaxTime;
        var start = Date.now();
        var retryCount = 0;
        function _doReconnect() {
            if (self._destroyed) {
                self._isConnecting = false;
                logger.debug('GW tried to reconnect while destroyed:', self.config);
                return;
            }
            
            self._connect(function (err) {
                retryCount++;
                if (err) {
                    if ((Date.now()-start) < maxTime) {
                        // try again
                        var interval = self._calculateInterval(retryCount);
                        logger.debug('GW reconnectin failed, will retry:', self.config, err);
                        return setTimeout(_doReconnect, interval);
                    } else {
                        // no way to reconnect
                        self._isConnecting = false;
                        logger.error('GW reconnectin failed:', self.config, err);
                        self.emit('serviceInstanceDisconnect', self);
                        return;
                    }
                }
                logger.debug('GW reconnectin success:', self.config);
                self._isConnecting = false;
            });
        };
        
        _doReconnect();
        
    } catch (e) {
        logger.error('GW error on reconnecting to instance', e);
    }
};



/**
 * Send message to service instance using the websocket protocol.
 * @param {type} strMessage
 * @returns {e}
 */
o.sendMessage = function (strMessage) {
    try {
        var self = this;

        // if not connected return error
        if (!self.connected()) {
            logger.error('GW instance sendMessage() , insance is not connected(config, message):', self.config, strMessage);
            return new Error('ERR_CONNECTION_ERROR');
        }
        
        if (self._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'data',
                action: 'sent',
                from: self.connection.logData.sockName.address + ":" + self.connection.logData.sockName.port,
                to: self.connection.logData.remoteAddress + ':' + self.connection.logData.remotePort,
                message: strMessage,
                is_client: true
            }));
        }
        self.connection.sendUTF( strMessage );
    } catch (e) {
        logger.error('ServiceInstanceConnection.sendMessage', e);
        return e;
    }
};


o.connected = function () {
    if (this.connection) {
        return this.connection.connected;
    }
    
    return false;
};


o.destroy = function () {
    try {
        this._destroyed = true;
        if (this.wsClient) {
            this.wsClient.removeAllListeners();
        }
        if (this.connection) {
            this.connection.removeAllListeners();
        }
        delete this.connection;
        delete this.wsClient;
    } catch (e) {
        logger.error('GW error on destroy instance connection', e);
        return e;
    }
};

module.exports = ServiceInstanceConnection;
