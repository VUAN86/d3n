var Config = require('../config/config.js');
var _ = require('lodash');
var async = require('async');
var url = require('url');
var uuid = require('uuid');
var http = require('http');
var https = require('https');
var ConnectionSession = require('./ConnectionSession.js');
var BroadcastSystem = require('./BroadcastSystem.js');
var WebSocketServer = require('uws').Server;
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var Errors = require('nodejs-errors');
var Const = require('../config/constants.js');
var JsonSchemas = require('nodejs-protocol-schemas');
var KeyvalueService = require('nodejs-aerospike').getInstance().KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var NewrelicMetrics = require('nodejs-newrelic');
var StatisticsMonitor = require('./StatisticsMonitor.js');

/**
 * @typedef {object} ServiceConfig
 * @property {string} ip - service IP
 * @property {string} externIp - optional ip for external access to the service, will be propagated to the service registry
 * @property {int} port - service port
 * @property {boolean} secure - use ssl or not
 * @property {string} key - Private key to use for ssl
 * @property {string} cert - Public x509 certificate to use
 * @property {string} serviceName - the name of the service
 * @property {array} serviceNamespaces - namespaces provided by the service
 * @property {AuthConfig} auth
 * @property {string} registryServiceURIs - Commaseperated list of uris for service registry
 * @property {Function|undefined} requestListener= - Optional request listener, added to the server
 * @property {boolean} protocolLogging
 */

/**
 * Represents a webservice, listening to websocket connections.
 * You need to start up the service by calling the method build()
 *
 * @type Service
 * @param {ServiceConfig} config
 * @constructor
 */
function Service(config) {
    /**
     * @type {ServiceConfig}
     */
    this.config = _.assign({
        registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || '',
        externIp: process.env.EXTERN_IP || config.externIp || config.ip,
        serviceNamespaces: [],
        validateFullMessage: true,
        validatePermissions: false,
        sendStatistics: true,
        checkCert: (process.env.CHECK_CERT ? (process.env.CHECK_CERT === 'true') : true)
    }, config);
    if (!_.has(this.config, 'requestListener')) {
        this.config.requestListener = undefined;
    }

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

    /**
     * @type {string} Name of the service, used for propagation through the service registry
     * @private
     */
    this._serviceName = this.config.serviceName || 'test';

    /**
     * @type {array} Namespaces provided by the service
     * @private
     */
    this._serviceNamespaces = _.isArray(this.config.serviceNamespaces) ? this.config.serviceNamespaces : [];

    /**
     * @ype {BroadcastSystem}
     * @private
     */
    this._broadcastSystem = new BroadcastSystem();

    /**
     * @type {boolean}
     * @private
     */
    this._protocolLogging = process.env.PROTOCOL_LOGGING || config.protocolLogging || false;
    
    /**
     * Instance used to collect metrics and report. Newrelic by default.
     */
    this._newrelicMetrics = new NewrelicMetrics();
    
    /**
     * Keeps service client instances
     */
    this._serviceClients = {};
    
    /**
     * Keeps all WS connections to this service
     */
    this._connections = [];
    
    this._statisticsMonitor = new StatisticsMonitor({
        service: this,
        sendStatisticsInterval: config.sendStatisticsInterval || process.env.SEND_STATISTICS_INTERVAL || 10000
    });
}
var o = Service.prototype;


o.getStatisticsMonitor = function () {
    return this._statisticsMonitor;
};

o.geNewrelictMetrics = function () {
    return this._newrelicMetrics;
};

/**
 * Creates service client instance.
 * @param {string} service
 * @returns {}
 */
o._createServiceClientInstance = function(service) {
    try {
        var instance;
        var cls;
        var self = this;
        switch (service) {
            case "auth":
                cls = require('nodejs-auth-service-client');
                instance = new cls({
                    registryServiceURIs: this.config.registryServiceURIs,
                    ownerServiceName: self._serviceName
                });
                break;

            case "media":
                cls = require('nodejs-media-service-client');
                instance = new cls({
                    registryServiceURIs: this.config.registryServiceURIs,
                    ownerServiceName: self._serviceName
                });
                break;

            case "profile":
                cls = require('nodejs-profile-service-client');
                instance = new cls(null, null, self._serviceName);
                break;

            case "profileManager":
                cls = require('nodejs-profile-manager-client');
                instance = new cls(null, null, self._serviceName);
                break;

            case "voucher":
                cls = require('nodejs-voucher-service-client');
                instance = new cls(null, null, self._serviceName);
                break;

            case "friend":
                cls = require('nodejs-friend-service-client');
                instance = new cls(null, null, self._serviceName);
                break;

            case "event":
                cls = require('nodejs-event-service-client');
                instance = new cls(null, null, self._serviceName);
                break;

            case "userMessage":
                cls = require('nodejs-user-message-client');
                instance = new cls({
                    registryServiceURIs: this.config.registryServiceURIs,
                    ownerServiceName: self._serviceName
                });
                break;

            case "serviceRegistry":
                cls = require('nodejs-service-registry-client');
                var configsServiceRegistryClient = [];
                _.each(this.config.registryServiceURIs.split(','), function (uri) {
                    var hostConfig = url.parse(uri);
                    configsServiceRegistryClient.push({
                        service: {
                            ip: hostConfig.hostname,
                            port: hostConfig.port
                        },
                        secure: hostConfig.protocol === 'wss:'
                    });
                });

                instance = new cls(configsServiceRegistryClient, self._serviceName);
                break;

            default:
                logger.error('DefaultService._createServiceClientInstance no class for service=', service);
                instance = new Error(Const.ERR_NO_SERVICE_CLIENT_CLASS);
                break;
        }

        return instance;

    } catch (e) {
        logger.error('DefaultService._createServiceClientInstance system error', e);
        return e;
    }

};



/**
 * Lazy load of a service client instance.
 * @param {string} service
 * @returns {}
 */
o.getServiceClient = function (service) {
    if (!_.has(this._serviceClients, service)) {
        this._serviceClients[service] = this._createServiceClientInstance(service);
    }

    return this._serviceClients[service];
};

o.removeServiceClient = function (service) {
    if (!_.has(this._serviceClients, service)) {
        delete this._serviceClients[service];
    }
};


/**
 * Returns logger used by this service
 * @returns {object}
 */
o.getLogger = function () {
    return logger;
};

/**
 * Returns Auth Service Client instance created by this service
 * @returns {object}
 */
o.getAuthService = function () {
    return this.getServiceClient('auth');
};

/**
 * Returns Profile Manager Client instance created by App Configurator service
 * @returns {object}
 */
o.getProfileManager = function () {
    return this.getServiceClient('profileManager');
};

/**
 * Returns Profile Service Client instance created by this service
 * @returns {object}
 */
o.getProfileService = function () {
    return this.getServiceClient('profile');
};

/**
 * Returns Voucher Service Client instance created by this service
 * @returns {object}
 */
o.getVoucherService = function () {
    return this.getServiceClient('voucher');
};

/**
 * Returns Friend Service Client instance created by this service
 * @returns {object}
 */
o.getFriendService = function () {
    return this.getServiceClient('friend');
};

/**
 * Returns Event Service Client instance created by this service
 * @returns {object}
 */
o.getEventService = function () {
    return this.getServiceClient('event');
};

/**
 * Returns Media Service Client instance created by this service
 * @returns {object}
 */
o.getMediaService = function () {
    return this.getServiceClient('media');
};

/**
 * Returns Service Registry Client instance created by this service
 * @returns {object}
 */
o.getServiceRegistry = function () {
    return this.getServiceClient('serviceRegistry');
};

/**
 * Returns User Message Client instance created by this service
 * @returns {object}
 */
o.getUserMessage = function () {
    return this.getServiceClient('userMessage');
};

/**
 * Return the broadcast system instance.
 * @returns {BroadcastSystem}
 */
o.getBroadcastSystem = function () {
    return this._broadcastSystem;
};

/**
 * Emit an internal event. First argument is the event name. The rest of the arguments are the arguments passed to the event handler.
 * @returns {Error|true}
 */
o.emitEvent = function () {
    try {
        var eventName = arguments[0];
        if (!_.isFunction(this.eventHandlers[eventName])) {
            logger.warn('emitEvent warn: event handler not found for event=' + eventName);
            return new Error('ERR_EVENT_HANDLER_NOT_FOUND');
        }

        var handlerArguments = [];
        for(var i=1; i<arguments.length; i++) {
            handlerArguments.push(arguments[i]);
        }

        this.eventHandlers[eventName].apply(this, handlerArguments);

        return true;
    } catch (e) {
        logger.error('emitEvent error:', e);
        return e;
    }
};

/**
 * Create a unique ID
 * @returns {string}
 */
o._createUUID = function () {
    return uuid.v4();
};

/**
 * @type {Object.<string, Function>}
 * Here service developers add methods to handle the messages. key = message name
 */
o.messageHandlers = require('./DefaultMessageHandlers.js');

/**
 * @type {Object.<string, Function>}
 * Here service developers add methods to handle the internal events. key = event name
 */
o.eventHandlers = require('./DefaultEventHandlers.js');

/**
 * Send message via a socket, without client system!
 * Use this only for error handling while no ClientSession is present
 *
 * @param {object} connection
 * @param {ProtocolMessage|null} baseMessage
 * @param {string|null} messageName
 * @param {{code: int, message: string}} error
 * @returns {boolean} false if an error occurred while sending
 * @private
 */
o._sendErrorResponseWithoutSession = function (connection, baseMessage, messageName, error) {
    try {
        var errMessage;
        if (baseMessage) {
            errMessage = new ProtocolMessage(baseMessage);
        } else {
            errMessage = new ProtocolMessage();
        }
        if (messageName) {
            errMessage.setMessage(messageName);
        }
        errMessage.setContent(null);
        errMessage.setError(error);

        var plainMessage = JSON.stringify(errMessage.getMessageContainer());

        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.pid,
                type: 'data',
                action: 'sent',
                source: this.config.ip + ':' + this.config.port,
                message: plainMessage
            }));
        }

        connection.send( plainMessage );
        return true;
    } catch (e) {
        logger.error('Service._sendErrorResponseWithoutSession', e);
        return false;
    }
};

/**
 * Extracts ProtocolMessage out of json string
 * @param {object} connection
 * @param {string} strMessage
 * @returns {ProtocolMessage|null}
 * @private
 */
o._ValidationRequestMessageJson = function(connection, strMessage) {
    try {
        return new ProtocolMessage( JSON.parse(strMessage) );
    } catch (e) {
        logger.error('Error parsing json message:', e, strMessage);
        this._sendErrorResponseWithoutSession(connection,null, 'global/errorResponse', Errors.ERR_VALIDATION_FAILED);
        return null;
    }
};

/**
 * check that the message is a valid Websocket Message by applying schema validation
 * @param {object} connection
 * @param {ProtocolMessage} reqMessage
 * @returns {boolean}
 * @private
 */
o._ValidationRequestMessageSchema = function(connection, reqMessage) {
    var res = reqMessage.isValid('websocketMessage');
    if (res !== true) {
        this._sendErrorResponseWithoutSession(connection, reqMessage, null, Errors.ERR_VALIDATION_FAILED);
        return false;
    }

    if(this.config.validateFullMessage === false) {
        return true;
    }

    // validate full message
    var res = reqMessage.isValid(reqMessage.getMessage());
    if (res !== true) {
        this._sendErrorResponseWithoutSession(connection, reqMessage, null, Errors.ERR_VALIDATION_FAILED);
        return false;
    }

    return true;
};

/**
 * Check that the requested message handler exists
 * @param {ClientSession} clientSession
 * @param {ProtocolMessage} reqMessage
 * @returns {boolean}
 * @private
 */
o._ValidationRequestMessageHandlerExists = function(clientSession, reqMessage) {
    // check that message handler exists
    var handlerName = reqMessage.getMessageName();
    if( !_.isFunction(this.messageHandlers[handlerName]) ) {
        logger.error('DefaultService message handler not exists', reqMessage);
        var errMessage = new ProtocolMessage(reqMessage);
        errMessage.setContent(null);
        errMessage.setError(Errors.ERR_FATAL_ERROR);
        clientSession.sendMessage(errMessage);
        return false;
    }
    return true;
};


/**
 * Check if the required user role has permissions to access the given api endpoint.
 * Returns via callback.
 * @param {ClientSession} clientSession
 * @param {ProtocolMessage} reqMessage
 * @param {Function} callback
 * @private
 */
o._ValidationPermissions = function(clientSession, reqMessage, callback) {
  try {
      // Is security check enabled in the configuration settings?
      if (false === this.config.validatePermissions) {
          logger.debug("BaseService.validatePermission() skipping security check, security disabled");
          return callback(false);
      }

      // Ref#5970 - Do not check security for SERVICE to SERVICE communication
      if (true === clientSession._isDirectConnection) {
          logger.debug("BaseService.validatePermission() skipping security check for direct service connection",
                reqMessage.getMessageNamespace() + "/" + reqMessage.getMessageName());
          return callback(false);
      }

      var userRoles = clientSession.getRoles();
      var userId = clientSession.getUserId();
      var tenantId = clientSession.getTenantId();

      async.series([
          function (next) {
              try {
                  if (reqMessage.validateSecurity(userRoles, tenantId) === true) {
                      return next(false);
                  }
                  return next('ERR_INSUFFICIENT_RIGHTS');
              } catch (err) {
                  return next(err);
              }
          }
      ], function (err, data) {
          if (err) {
              logger.error('BaseService.validatePermission() failure accessing',
                  reqMessage.getMessage(),'by user', JSON.stringify(userId),
                  'having roles', JSON.stringify(userRoles), 'error', err);
              var errMessage = new ProtocolMessage(reqMessage);
              errMessage.setContent(null);
              errMessage.setError(Errors.ERR_INSUFFICIENT_RIGHTS);
              clientSession.sendMessage(errMessage);
              return setImmediate(callback, true);
          }

          return callback(false);
      });
  } catch(e) {
      logger.error('BaseService.validatePermission() exception', err);
      var errMessage = new ProtocolMessage(reqMessage);
      errMessage.setContent(null);
      errMessage.setError(Errors.ERR_FATAL_ERROR);
      clientSession.sendMessage(errMessage);
      return setImmediate(callback, true);
  }
};

/**
 * After all other validations where succesfully executed, handle message
 * on given message handler
 * @param {ClientSession} clientSession
 * @param {ProtocolMessage} reqMessage
 * @param {Function} callback
 * @returns {Number}
 * @private
 */
o._ValidationRequestMessageTriggerHandler = function(clientSession, reqMessage, callback) {
    try {
        var handlerName = reqMessage.getMessageName();
        
        // start recording api call time
        var nrMessage = new ProtocolMessage(reqMessage.getMessageContainer());
        nrMessage.setClientId(reqMessage.getClientId() || clientSession.getId());
        this.geNewrelictMetrics().apiHandlerStart(nrMessage);
        
        this.messageHandlers[handlerName].call(this, reqMessage, clientSession);
        return setImmediate(callback, false);
    } catch(e) {
        logger.error('Error triggering message handler:', e, reqMessage);
        var errMessage = new ProtocolMessage(reqMessage);
        errMessage.setContent(null);
        errMessage.setError(Errors.ERR_FATAL_ERROR);
        clientSession.sendMessage(errMessage);
        return setImmediate(callback, true);
    }
};


/**
 * Handle a message received via a websocket.
 * Do some checks on message then forward the message to a message handler function.
 *
 * @param {object} connection
 * @param {string} strMessage
 * @private
 */
o._handleWebSocketRequestMessage = function (connection, strMessage) {
    try {
        var self = this;

        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.pid,
                type: 'data',
                action: 'received',
                destination: this.config.ip + ':' + this.config.port,
                message: strMessage
            }));
        }

        var reqMessage = this._ValidationRequestMessageJson(connection, strMessage);
        if (!reqMessage) {
            return;
        }

        if (!this._ValidationRequestMessageSchema(connection, reqMessage)) {
            return;
        }

        //Extract information out of received message
        var clientId = reqMessage.getClientId() || connection.session.getId();
        var clientSession = connection.session.getOrCreateClientSession(clientId);

        if (!this._ValidationRequestMessageHandlerExists(clientSession, reqMessage)) {
            return;
        }
        
        if (reqMessage.getClientInfo()) {
            clientSession.setClientInfo(reqMessage.getClientInfo());
        }
        
        async.series([
            // Validate security of endpoint, can be accessed
            function (next) {
                self._ValidationPermissions(clientSession, reqMessage, next);
            },
            function (next) { // send message to message handler
                self._ValidationRequestMessageTriggerHandler(clientSession, reqMessage, next);
            }
        ]);
    } catch (e) {
        logger.error('Error handling message:', e, strMessage);
        this._sendErrorResponseWithoutSession(connection,null, 'global/errorResponse', Errors.ERR_FATAL_ERROR);
        return e;
    }
};



o._verifyClient = function (info, cb) {
    var self = this;
    var request = info.req;
    
    if (self.config.checkCert === true && self.config.secure === true) {
        if (_.has(request.headers, 'origin')) {
            logger.error('DefaultService origin provided', request.socket.remoteAddress, request.headers.origin);
            return setImmediate(cb, false, 400);
        }
        
        var cert = request.socket.getPeerCertificate();
        
        if (cert === null || _.isEmpty(cert)) {
            logger.error('DefaultService no cert provided', request.socket.remoteAddress);
            return setImmediate(cb, false, 400);
        }
        var found = false;
        for(var i=0; i<Config.trustedCerts.length; i++) {
            if (Config.trustedCerts[i] === cert.raw.toString('base64')) {
                found = true;
                break;
            }
        }
        
        if (!found) {
            logger.error('DefaultService not a trusted cert provided', request.socket.remoteAddress);
            return setImmediate(cb, false, 400);
        }
        
        return setImmediate(cb, true);
    } else {
        return setImmediate(cb, true);
    }
};

/**
 * Handle the request. Create connection session, set message handler, handle connection close event
 * @param {object} request
 * @private
 */
o._onWebSocketRequestHandler = function (connection) {
    var self = this;
    /**
     * @type {ConnectionSessionConfig}
     */
    var connectionConfig = {
        clientConfig : {
            protocolLogging : this._protocolLogging,
            authConfig : this.config.auth
        }
    };

    /**
     * @type {ConnectionSession}
     */
    connection.session = new ConnectionSession(connection, self._createUUID(), connectionConfig);
    /**
     * @type {Service}
     */
    connection.service = this;
    
    // add connection to the list of connections
    self._connections.push({
        wsConnection: connection,
        serviceName: connection.requestData.headers['service_name'],
        serviceNamespace: connection.requestData.headers['service_namespace']
    });
    
    connection.on('message', function(message) {
        self._handleWebSocketRequestMessage(connection, message);
    });

    connection.on('close', function() {
        connection.session.onclose();
        delete connection.service;
        delete connection.session;
        
        // remove connection
        for(var i=self._connections.length-1; i>=0; i--) {
            if (self._connections[i].wsConnection === connection) {
                self._connections.splice(i, 1);
            }
        }
    });
};

/**
 * Handle the request. Create connection session, set message handler, handle connection close event
 * @param {Function} callback
 * @private
 */
o._onHTTPServerStarted = function(callback) {
    try {
        if (this.config.registryServiceURIs) {
            // register the service
            var serviceURI = 'ws' + (this.config.secure === true ? 's' : '') + '://' + this.config.externIp + ':' + this.config.port;
            var namespaces = _.uniq([this._serviceName].concat(this._serviceNamespaces));
            this.getServiceRegistry().register(this._serviceName, serviceURI, namespaces, function (err) {
                if (err) {
                    logger.error('DefaultService error registering namespaces:', err);
                }
            });
        }
        return callback(false);
    } catch (e) {
        logger.error('Error on server.listen', e);
        return callback(e);
    }

};

/**
 * Build the service. Create http server, create ws server, set on request handler
 * @param {Function} cb
 */
o.build = function (cb) {
    try {
        var server;
        var self = this;

        //Close existing connections if exist
        this.shutdown(false);

        if(this.config.secure === true) {
            server = https.createServer({
                key: this.config.key,
                cert: this.config.cert,
                requestCert: true,
                rejectUnauthorized: false
            }, this.config.requestListener);
        } else {
            server = http.createServer(this.config.requestListener);
        }

        this._httpInstance = server.listen(this.config.port, this.config.ip, function() {
            self._onHTTPServerStarted(function (err) {
                if (err) {
                    return cb(err);
                }
                if (self._serviceName !== 'serviceRegistry' && self.config.sendStatistics === true) {
                    self.getStatisticsMonitor().startSendStatistics();
                }
                return cb();
            });
        });
        
        server.prependListener('upgrade', function (request, socket, head) {
            request.__upgradeRequestData__ = {
                remoteAddress: request.socket.remoteAddress,
                remotePort: request.socket.remotePort,
                sockName: request.socket._getsockname ? request.socket._getsockname() : request.socket._peername,
                peerCertificate: self.config.secure ? request.socket.getPeerCertificate() : null,
                headers: request.headers
            };
        });
        
        this._wsInstance = new WebSocketServer({
            server: server,
            verifyClient: self._verifyClient.bind(self)
        });
        
        this._wsInstance.startAutoPing(20000);
        
        this._wsInstance.on('connection', function (connection) {
            connection.requestData = _.assign({}, connection.upgradeReq.__upgradeRequestData__);
            self._onWebSocketRequestHandler(connection);
        });
        
        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.pid,
                type: 'service',
                action: 'created',
                name: this.config.ip + ':' + this.config.port,
                visibleName: this._serviceName
            }));
        }
    } catch (e) {
        return setImmediate(cb, e);
    }
};


/**
 * Shutting down service, closing all connections
 */
o.shutdown = function (disconnectFromServiceRegsitry) {
    var self = this;

    var disconnect = (!_.isUndefined(arguments[0]) ? arguments[0] : true);
    if (disconnect) {
        self.getServiceRegistry().unregister(function (err) {
            logger.error('DefaultService error on unregister:', err);
            self.getServiceRegistry().disconnect();
            self.removeServiceClient('serviceRegistry');
        });
    }


    if (self._httpInstance) {
        self._httpInstance.close();
        self._httpInstance = undefined;
    }
    if (self._wsInstance) {
        self._wsInstance.close();
        self._wsInstance = undefined;

        if (this._protocolLogging) {
            logger.info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.pid,
                type: 'service',
                action: 'removed',
                name: this.config.ip + ':' + this.config.port,
                visibleName: this._serviceName
            }));
        }
    }
};

module.exports = Service;
