var WebSocketServer = require('uws').Server,
    http = require('http'),
    https = require('https'),
    fs = require('fs'),
    ClientSession = require('./ClientSession.js'),
    _ = require('lodash'),
    async = require('async'),
    MessageDispatcher = require('./MessageDispatcher.js'),
    uuid = require('uuid'),
    logger = require('nodejs-logger')(),
    Config = require('../config/config.js')
;


function Gateway(config) {
    try {
        
        this.config = _.assign({
            checkCert: (process.env.CHECK_CERT ? (process.env.CHECK_CERT === 'true') : true)
        }, config);
        
        if (!this.config.gateway.externUrl) {
            this.config.gateway.externUrl = (this.config.gateway.secure === true ? 'wss': 'ws') + '://' + (this.config.gateway.externIp || this.config.gateway.ip) + ':' + this.config.gateway.port;
        }
        
        this.messageDispatcher = new MessageDispatcher({
            serviceInstances: this.config.serviceInstances,
            registryServiceURIs: this.config.registryServiceURIs,
            authConfig: this.config.gateway.auth,
            gatewayIpPort: {
                ip: config.gateway.ip,
                port: config.gateway.port,
                externIp: process.env.EXTERN_IP || config.gateway.ip,
                externUrl: process.env.EXTERN_URL
            },
            gatewayExternUrl: this.config.gateway.externUrl,
            gateway: this
        });
        
        this.clientConnection = {};

        this._protocolLogging = process.env.PROTOCOL_LOGGING || config.protocolLogging || false;
		
		/**
		 * @type {object|undefined} HTTP Server instance to listen to incomming connections
		 * @private
		 */
		this._httpInstance = undefined;
		/**
		 * @type {object|undefined} Websocket instance, using _httpInstance to get websocket connections
		 * @private
		 */
		this._wsInstance = undefined;

    } catch(e) {
        throw e;
    }
};
var o = Gateway.prototype;

o.countClientConnections = function () {
    return _.keys(this.clientConnection).length;
};

o.getClientConnection = function (clientId) {
    return this.clientConnection[clientId];
};

o._createUUID = function () {
    return uuid.v4();
};



/**
 * Send a message through a websocket connection.
 * @param {type} request
 * @returns {undefined}
 */
o.sendMessage = function (connection, strMessage) {
    try {
        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'data',
                action: 'sent',
                from: connection.logData.sockName.address + ":" + connection.logData.sockName.port,
                to: connection.logData.remoteAddress + ':' + connection.logData.remotePort,
                message: strMessage
            }));
        }
        connection.send( strMessage );
    } catch (e) {
        logger.error('Error sending message to the client', e);
        return e;
    }
};

/**
 * Handles the client requests
 * @returns {undefined}
 */
o._onRequest = function (connection) {
    try {
        var self = this;
        
        connection.clientId = self._createUUID();
        
        self.clientConnection[connection.clientId] = connection;

        if (this._protocolLogging) {
            connection.logData = {
                sockName: connection.requestData.sockName,
                remoteAddress: connection.requestData.remoteAddress,
                remotePort: connection.requestData.remotePort
            };
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'connection',
                action: 'created',
                from:  connection.logData.remoteAddress + ':' + connection.logData.remotePort,
                to: connection.logData.sockName.address + ":" + connection.logData.sockName.port
            }));
        }

		
        connection.on('message', function(message) {

            if (self._protocolLogging) {
                logger.info(JSON.stringify({
                    timestamp: new Date().getTime(),
                    identification: process.cwd()+":"+process.pid,
                    type: 'data',
                    action: 'received',
                    from: connection.logData.remoteAddress + ':' + connection.logData.remotePort,
                    to: connection.logData.sockName.address + ":" + connection.logData.sockName.port,
                    message: message
                }));
            }

            // delegate to message dispatcher
            self.messageDispatcher.sendMessageToService(connection.clientId, message, connection.requestData.remoteAddress);
        });
        
        connection.on('close', function(reasonCode, description) {
            if (self._protocolLogging) {
                logger.info(JSON.stringify({
                    timestamp: new Date().getTime(),
                    identification: process.cwd()+":"+process.pid,
                    type: 'connection',
                    action: 'removed',
                    from:  connection.logData.remoteAddress + ':' + connection.logData.remotePort,
                    to: connection.logData.sockName.address + ":" + connection.logData.sockName.port
                }));
            }

            // delegate to message dispatcher
            self.messageDispatcher.disconnectClient(connection.clientId);
            delete self.clientConnection[connection.clientId];
        });
        
    } catch (e) {
        logger.error('Error handling request', e);
    }
    
};

/**
 * Build the websocket server that listen for client connections and forward received messages
 * @param {type} cb
 * @returns {undefined}
 */
o.buildAPIEndpoint = function (cb) {
    try {
        var self = this, cbCalled = false;
        function callCB(err, res) {
            if(cbCalled === false) {
                cbCalled = true;
                cb(err, res);
            }
        };
        
        var server = null;
        if(self.config.gateway.secure === true) {
            server = https.createServer({
                key: fs.readFileSync(self.config.gateway.key, 'utf8'),
                cert: fs.readFileSync(self.config.gateway.cert, 'utf8')
            });
        } else {
            server = http.createServer();
        }
        this._httpInstance = server.listen(self.config.gateway.port, self.config.gateway.ip, function() {
            logger.info('Gateway is listening on port ' + self.config.gateway.port);
            callCB(false);
        });

        server.prependListener('upgrade', function (request, socket, head) {
            request.__upgradeRequestData__ = {
                remoteAddress: request.headers['x-forwarded-for'] ? request.headers['x-forwarded-for'].split(',')[0] : request.socket.remoteAddress,
                remotePort: request.socket.remotePort,
                sockName: request.socket._getsockname ? request.socket._getsockname() : request.socket._peername,
                peerCertificate: self.config.secure ? request.socket.getPeerCertificate() : null,
                headers: request.headers
            };
        });
        
        this._wsInstance = new WebSocketServer({server: server});
        
        this._wsInstance.startAutoPing(20000);
        
        
        this._wsInstance.on('connection', function(connection) {
            connection.requestData = _.assign({}, connection.upgradeReq.__upgradeRequestData__);
            self._onRequest(connection);
        });

        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'service',
                action: 'created',
                on: this.config.gateway.ip + ':' + this.config.gateway.port,
                visibleName: "Gateway"
            }));
        }
        
    } catch (e) {
        return setImmediate(callCB, e);
    }
};

o.build = function (cb) {
    try {
        var self = this;
        async.series([
            
            // init message dispatcher
            function (next) {
                try {
                    self.messageDispatcher.init(function (err) {
                        if(err) {
                            return next(err);
                        }
                        
                        // register the method that will handle the messages sent by message dispatcher
                        self.messageDispatcher.on('message', self._onMessageDispatcherMessage.bind(self));
                        return next(false);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // build api endpoint
            function (next) {
                self.buildAPIEndpoint(next);
            }
        ], function (err) {
            return cb(err);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Shutting down gateway, closing all connections
 */
o.shutdown = function() {
    var self = this;
    if (self._httpInstance) {
        self._httpInstance.close();
        self._httpInstance = undefined;
    }
    if (self._wsInstance) {

        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'service',
                action: 'removed',
                on: this.config.ip + ':' + this.config.port,
                visibleName: "Gateway"
            }));
        }

        self._wsInstance.close();
        self._wsInstance = undefined;
    }
}


/**
 * Find the connection associated to the client and send the message through that connection 
 * @param {type} clientId
 * @param {type} message
 * @returns {undefined}
 */
o._onMessageDispatcherMessage = function (clientId, strMessage) {
    try {
        var self = this,
            conn = self.clientConnection[clientId]
        ;
        
        self.sendMessage(conn, strMessage);
        
    } catch (e) {
        logger.error('Error forwarding message to the client', e);
        return e;
    }
};

module.exports = Gateway;
