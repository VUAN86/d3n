var EventEmitter = require('events').EventEmitter;
var inherits = require('util').inherits;
var logger = require('nodejs-logger')();
var async = require('async');
var ServiceLoadBalancerManager = require('./ServiceLoadBalancerManager.js');
var ProtocolMessage = require('nodejs-protocol');
var ClientSession = require('./ClientSession.js');
var _ = require('lodash');
var Constants = require('../config/constants.js');
var Auth = require('./Auth.js');
var Aerospike = require('./Aerospike.js');
var Errors = require('nodejs-errors');
var NewrelicMetrics = require('nodejs-newrelic');
var ProfileServiceClient = require('nodejs-profile-service-client');
var Config = require('../config/config.js');
var GeoLocation = require('./GeoLocation.js');

function MessageDispatcher(config) {
    
    this.config = config;
    
    this._geoLocation = new GeoLocation();
    
    this.serviceLoadBalancerManager = new ServiceLoadBalancerManager({
        serviceInstances: this.config.serviceInstances,
        registryServiceURIs: this.config.registryServiceURIs,
        gateway: config.gateway
    });
    
    this.profileServiceClient = new ProfileServiceClient(this.config.registryServiceURIs, null, 'gateway');
    
    this._addFakeAppConfig = true;
    if (_.has(process.env, 'ADD_FAKE_APP_CONFIG')) {
        this._addFakeAppConfig = process.env.ADD_FAKE_APP_CONFIG === 'true';
    }
    
    this._rejectPrivateApiFlag = true;
    if (_.has(process.env, 'REJECT_PRIVATE_API')) {
        this._rejectPrivateApiFlag = process.env.REJECT_PRIVATE_API === 'true';
    }
    
    this._auth = new Auth(this.config.authConfig);
    
    this._aerospike = new Aerospike();
    
    /**
     * Instance used to collect metrics and report. Newrelic by default.
     */
    this._newrelicMetrics = new NewrelicMetrics();
    
    this.clientsSession = {};
    
    this.clientsQueue = {};
    
    EventEmitter.call(this);
    
    // prevent nodejs crash
    this.on('error', function (error) {
        logger.error('MessageDispatcher error on emit event:', error);
    });
    
};


inherits(MessageDispatcher, EventEmitter);

var o = MessageDispatcher.prototype;

o.geNewrelictMetrics = function () {
    return this._newrelicMetrics;
};

/**
 * Init the message dispatcher. Mainly init the load balancers manager and set handlers for message and service instance disconect event.
 * @param {type} cb
 * @returns {unresolved}
 */
o.init = function (cb) {
    try {
        var self = this;
        async.series([
            
            // init service load balancer manager
            function (next) {
                self.serviceLoadBalancerManager.init(next);
            },
            
            // set event listeners
            function (next) {
                try {
                    self.serviceLoadBalancerManager.on('message', self._onServiceLoadBalancerManagerMessage.bind(self));
                    
                    self.serviceLoadBalancerManager.on('serviceInstanceDisconnect', self._onServiceInstanceDisconnect.bind(self));
                    
                    self.serviceLoadBalancerManager.on(Constants.EVT_TOKEN_INVALIDATED, self._onTokenInvalidated.bind(self));
                    
                    return setImmediate(next, false);
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], function (err) {
            return cb(err);
        });
        
    } catch (e) {
        logger.error('Error on init MessageDispatcher', e);
        return setImmediate(cb, e);
    }
};

/**
 * Send a single message. IF there is any error then it return an error to the gateway.
 * @param {type} clientId
 * @param {type} serviceName
 * @param {type} message
 * @returns {e}
 */
o._sendMessageToService = function (clientId, serviceName, message) {
    try {
        var self = this;
        // if client is not connected anymre ignore the message
        if(!self.config.gateway.getClientConnection(clientId)) {
            return;
        }
        var res = self.serviceLoadBalancerManager.sendMessage(clientId, serviceName, JSON.stringify(message.getMessageContainer()));
        
        if (res instanceof Error) {
            var errMessage = new ProtocolMessage(message);
            errMessage.setError(Errors[res.message]);
            errMessage.setTimestamp(Date.now());
            errMessage.deleteClientId();
            errMessage.deleteClientInfo();
            
            logger.error('MessageDispatcher._sendMessageToService', res);

            // send to gateway
            return self.sendMessageToClient(clientId, JSON.stringify(errMessage.getMessageContainer()));
        }
    } catch (e) {
        logger.error('MessageDispatcher._sendMessageToService', e);
        return e;
    }
};

/**
 * Send message to the client through the gateway component.
 * @param {type} clientId
 * @param {type} strMessage
 * @returns {e|o@call;emit}
 */
o.sendMessageToClient = function (clientId, strMessage) {
    try {
        var tmpMessage = new ProtocolMessage( JSON.parse(strMessage) );
        tmpMessage.setClientId(clientId);
        this.geNewrelictMetrics().apiHandlerEnd(tmpMessage);
        
        return this.emit('message', clientId, strMessage);
    } catch (e) {
        logger.error('MessageDispatcher.sendMessageToClient', e);
        return e;
    }
};

/**
 * Helpful metod to send an error message to client.
 * @param {string} clientId
 * @param {ProtocolMessage|null} reqMessage
 * @param {string} messageComponent
 * @param {type} error
 * @returns {Boolean}
 */
o._sendErrorToClient = function (clientId, reqMessage, messageComponent, error) {
    try {
        var errMessage;
        if (reqMessage) {
            errMessage = new ProtocolMessage(reqMessage);
        } else {
            errMessage = new ProtocolMessage();
        }
        if (messageComponent) {
            errMessage.setMessage(messageComponent);
        }
        errMessage.setContent(null);
        errMessage.setError(error);
        errMessage.setTimestamp(Date.now());

        this.sendMessageToClient(clientId, JSON.stringify(errMessage.getMessageContainer()));
        
        return true;
    } catch (e) {
        logger.error('_sendErrorToClient', clientId, messageComponent, reqMessage, error);
        return false;
    }
};

/**
 * Manage tokens. Can be multiple tokens for a client: message token, session token, auth token. 
 * Check which tokens exists and select newest one and put on client session.
 * @param {type} clientSession
 * @param {type} message
 * @param {type} cb
 * @returns {unresolved}
 */
o._manageTokens = function (clientSession, message, cb) {
    try {
        var self = this;
        var clientId = clientSession.getId();
        var messageToken = message.getToken();
        var messageTokenPayload = null;
        
        var authToken = null;
        var authTokenPayload = null;
        
        var sessionToken = clientSession.getToken();
        var sessionTokenPayload = clientSession.getTokenPayload();
        
        clientSession.needRolesUpdate = false;
        async.series([
            // verify message token
            function (next) {
                if (!_.isString(messageToken)) {
                    return setImmediate(next);
                }
                
                self._auth.verifyToken(messageToken, function (err, tokenPayload) {
                    if (err) {
                        return next(Errors.ERR_TOKEN_NOT_VALID);
                    }
                    
                    messageTokenPayload = tokenPayload;
                    
                    clientSession.setIsAnonymous(false);
                    
                    // if token user id changed set globalSessionCreated=false
                    if (_.isString(sessionToken) && sessionTokenPayload.userId !== messageTokenPayload.userId) {
                        clientSession.globalSessionCreated = false;
                        clientSession._fakeGlobalClientSessionCreated = false;
                        self._removeGlobalClientSession(sessionTokenPayload.userId, clientSession.getId(), function (err) {
                            if (err) {
                                logger.error('GW error deleting global session for old user id:', err);
                            }
                            return next();
                        });
                    } else {
                        return next();
                    }
                    
                    
                });
            },
            
            // check and verify auth token
            function (next) {
                if (!_.isString(messageToken)) {
                    return setImmediate(next);
                }
                
                var userId = messageTokenPayload.userId;
                self._aerospike.getUserAuthToken(userId, function (err, auth_token) {
                    if (err) {
                        return next(err);
                    }

                    if (_.isUndefined(auth_token) || _.isNull(auth_token)) { // no token sent by auth service
                        return next();
                    }
                    
                    self._auth.verifyToken(auth_token, function (err, tokenPayload) {
                        if (err) {
                            return next(Errors.ERR_TOKEN_NOT_VALID);
                        }
                        
                        authToken = auth_token;
                        authTokenPayload = tokenPayload;
                        return next();
                        
                    });
                    
                });
            },
            
            // set session token with newest token
            function (next) {
                try {
                    if (!_.isString(messageToken)) {
                        return setImmediate(next);
                    }
                    
                    
                    
                    var tokens = [];
                    tokens.push({
                        token: messageToken,
                        payload: messageTokenPayload
                    });
                    tokens.push({
                        token: authToken,
                        payload: authTokenPayload
                    });
                    tokens.push({
                        token: sessionToken,
                        payload: sessionTokenPayload
                    });

                    tokens.sort(function (a, b) {
                        var a_iat = (a.payload ? a.payload.iat : 0);
                        var b_iat = (b.payload ? b.payload.iat : 0);
                        return a_iat - b_iat;
                    });

                    var newestToken = tokens.pop();
                    
                    // client has no token on session but send one in message update clientInfo.profile
                    if (!_.isString(sessionToken)) {
                        clientSession.needRolesUpdate = true;
                    } else {
                        if (newestToken.token !== sessionToken) {
                            clientSession.needRolesUpdate = true;
                        }
                    }
                    
                    // session always keep the newest token
                    clientSession.setToken(newestToken.token);
                    clientSession.setTokenPayload(newestToken.payload);
                    
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
                
            }
        ], function (err) {
            if (err) {
                if (err instanceof Error) { // system error
                    logger.error('Error manage tokens:', err, message);
                    self._sendErrorToClient(clientId, message, null, Errors.ERR_FATAL_ERROR);
                } else if (err === Errors.ERR_TOKEN_NOT_VALID) {
                    self._sendErrorToClient(clientId, message, 'global/authResponse', err);
                } else {
                    self._sendErrorToClient(clientId, message, null, err);
                }
                
                return cb(true);
            }
            
            return cb();
        });
    } catch (e) {
        logger.error('Error handling tokens:', e, message);
        self._sendErrorToClient(clientId, message, null, Errors.ERR_FATAL_ERROR);
        return setImmediate(cb, true);
    }
};


o._getUserProfile = function (userId, cb) {
    try {
        this.profileServiceClient.getUserProfile({userId: userId}, null, function (err, response) {
            try {
                if (err) {
                    return cb(err);
                }

                if (!_.isNull(response.getError())) {
                    return cb(response.getError());
                }
                
                var profile = response.getContent().profile;
                var retProfile = {
                    userId: profile.userId
                };
                
                var fields = ['language', 'handicap', 'roles', 'originCountry'];
                for(var i=0; i<fields.length; i++) {
                    var field = fields[i];
                    if (_.has(profile, field)) {
                        retProfile[field] = profile[field];
                    }
                }
                if (_.has(profile, 'emails')) {
                    retProfile.emails = profile.emails.filter(function (item) {
                        return item.verificationStatus === 'verified';
                    }).map(function (item) {
                        return item.email;
                    });
                }
                if (_.has(profile, 'phones')) {
                    retProfile.phones = profile.phones.filter(function (item) {
                        return item.verificationStatus === 'verified';
                    }).map(function (item) {
                        return item.phone;
                    });
                }
                
                return cb(false, retProfile);
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};


o._manageClientInfo = function (clientSession, message, clientIp, cb) {
    try {
        var self = this;
        
        clientSession.clientInfo.ip = clientIp;
        
        function _setAppConfig(appConfig) {
            var fields = ['tenantId', 'appId', 'deviceUUID'];
            clientSession.clientInfo.appConfig = {};
            for(var i=0; i<fields.length; i++) {
                var field = fields[i];
                if (_.has(appConfig, field)) {
                    clientSession.clientInfo.appConfig[field] = appConfig[field];
                }
            }
            
        };
        
        async.series([
            // geo location
            function (next) {
                try {
                    if (_.has(clientSession.clientInfo, 'countryCode')) {
                        // already detected
                        return setImmediate(next);
                    }
                    
                    clientSession.clientInfo.countryCode = self._geoLocation.getCountryCode(clientSession.clientInfo.ip);
                    
                    return setImmediate(next);
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // if is getAppConfiguration call then set appId, tenantId, deviceUUID
            function (next) {
                try {
                    if (message.getMessage() !== Constants.GET_APP_CONFIG_MESSAGE) {
                        return setImmediate(next);
                    }
                    
                    var isValid = message.isValid(Constants.GET_APP_CONFIG_MESSAGE);
                    if(isValid !== true) {
                        return setImmediate(next, Errors.ERR_VALIDATION_FAILED);
                    }
                    var content = message.getContent();
                    
                    _setAppConfig(content);
                    
                    return setImmediate(next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // set default app config
            function (next) {
                try {
                    if (clientSession.clientInfo.appConfig || !self._addFakeAppConfig) {
                        return setImmediate(next);
                    }
                    
                    _setAppConfig(MessageDispatcher.defaultAppConfig);
                    
                    return setImmediate(next);
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            
            // user specific data
            function (next) {
                try {
                    if (!_.isString(clientSession.getToken())) { // no token on session , do nothing
                        return setImmediate(next);
                    }
                    
                    var tokenUserId = clientSession.getTokenPayload().userId;
                    if (clientSession.needRolesUpdate !== true) {
                        return setImmediate(next);
                    }
                    
                    self._getUserProfile(tokenUserId, function (err, userProfile) {
                        try {
                            if (err) {
                                return next(err);
                            }
                            
                            var sessionTokenPayload = clientSession.getTokenPayload();
                            if (sessionTokenPayload.impersonate === true) {
                                
                                if(!sessionTokenPayload.tenantId) {
                                    logger.error('MessageDispatcher._manageClientInfo() missing tenantId on impersonate token', sessionTokenPayload);
                                    return next(Errors.ERR_VALIDATION_FAILED.message);
                                }
                                
                                // keep only roles for given tenant and global roles
                                var newRoles = [];
                                for(var i=0; i<userProfile.roles.length; i++) {
                                    var role = userProfile.roles[i];
                                    
                                    if(!_.startsWith(role, 'TENANT_') || _.startsWith(role, 'TENANT_' + sessionTokenPayload.tenantId + '_')) {
                                        // global role or role for token.tenantId 
                                        newRoles.push(role);
                                        continue;
                                    }
                                }
                                
                                userProfile.roles = newRoles;
                            }
                            
                            clientSession.clientInfo.profile = userProfile;

                            return next();
                            
                        } catch (e) {
                            return next(e);
                        }
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], function (err) {
            if (err) {
                logger.error('MessageDispatcher._manageClientInfo() error:', err);
                self._sendErrorToClient(clientSession.getId(), message, null, (err instanceof Error ? Errors.ERR_FATAL_ERROR : err));
                return cb(err);
            }
            
            return cb();
        });
    } catch (e) {
        logger.error('MessageDispatcher._manageClientInfo() Error handling client info:', e, message);
        self._sendErrorToClient(clientSession.getId(), message, null, Errors.ERR_FATAL_ERROR);
        return setImmediate(cb, true);
    }
};

/**
 * Add global client session on aerospike, if not already added.
 * @param {ClientSession} clientSession
 * @param {function} cb
 * @returns {unresolved}
 */
o._addGlobalClientSession = function (clientSession, message, cb) {
    try {
        var self = this;
        if (clientSession.globalSessionCreated) {
            return setImmediate(cb);
        }
        
        if (clientSession.getIsAnonymous()) {
            return setImmediate(cb);
        }
        
        var aerospikeClientSession = {
            clientId: clientSession.getId(),
            gatewayURL: self.config.gatewayExternUrl.replace('wss://', '').replace('ws://', ''),
            timestamp: parseInt(Date.now()/1000)
        };
        self._aerospike.addOrUpdateGlobalClientSession(clientSession.getTokenPayload().userId, aerospikeClientSession, function (err) {
            if (err) {
                logger.error('MessageDispatcher._addGlobalClientSession() error saving client session', err);
                self._sendErrorToClient(clientSession.getId(), message, null, Errors.ERR_FATAL_ERROR);
                return cb(err);
            }
            
            clientSession.globalSessionCreated = true;
            clientSession.aerospikeClientSession = aerospikeClientSession;
            return cb();
        });
    } catch (e) {
        logger.error('MessageDispatcher._addGlobalClientSession() error handling saving client session', e);
        self._sendErrorToClient(clientSession.getId(), message, null, Errors.ERR_FATAL_ERROR);
        return setImmediate(cb, e);
    }
};

/**
 * Remove global client session.
 * @param {string} userId
 * @param {string} clientId
 * @param {function} cb
 * @returns {unresolved}
 */
o._removeGlobalClientSession = function (userId, clientId, cb) {
    try {
        var self = this;
        self._aerospike.removeGlobalClientSesion(userId, {clientId: clientId, gatewayURL: ''}, cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Validate JSON, validate message shema. If not valid send error message to the client
 * @param {type} clientId
 * @param {type} strMessage
 * @param {type} cb
 * @returns {unresolved}
 */
o._validateMessage = function (clientId, strMessage, cb) {
    try {
        var self = this;
        
        // validate JSON
        try {
            var messageContainer = JSON.parse(strMessage);
        } catch (e) {
            logger.error('Message JSON not valid:', e, strMessage);
            
            self._sendErrorToClient(clientId, null, 'global/errorResponse', Errors.ERR_VALIDATION_FAILED);
            
            return setImmediate(cb, true);
        }
        
        
        // validate default schema
        var message = new ProtocolMessage(messageContainer);
        if (message.isValid('websocketMessage') !== true) {
            logger.error('Message schema not valid:', message.lastValidationError, message);
            
            self._sendErrorToClient(clientId, message, null, Errors.ERR_VALIDATION_FAILED);
            
            return setImmediate(cb, true);
        }
        
        // everything ok, return the message
        return setImmediate(cb, false, message);
        
    } catch (e) {
        logger.error('Error handling message validation:', e, strMessage);
        
        if (message) {
            self._sendErrorToClient(clientId, message, null, Errors.ERR_FATAL_ERROR);
        } else {
            self._sendErrorToClient(clientId, null, 'global/errorResponse', Errors.ERR_FATAL_ERROR);
        }
        
        return setImmediate(cb, true);
    }
};

o._isTrustedServiceUserMessageInstance = function (clientId, cb) {
    try {
        var self = this;
        var conn = self.config.gateway.getClientConnection(clientId);
        
        if (!_.isUndefined(conn._isTrustedUserMessageInstance)) {
            // already checked
            return setImmediate(cb, false, conn._isTrustedUserMessageInstance);
        }
        
        self.serviceLoadBalancerManager.getUserMessageInstancesIps(function (err, ips) {
            if (err) {
                return cb(err);
            }
            var connIp = conn.requestData.remoteAddress;
            conn._isTrustedUserMessageInstance = (ips.indexOf(connIp) >= 0);
            
            return cb(false, conn._isTrustedUserMessageInstance);
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._handleServiceAsClientToClientMessages = function(clientSession, message, cb) {
    try {
        var self = this;
        var sessClientId = clientSession.getId();
        
        if (!message.getClientId()) {
            // not a connection from userMessage service
            return setImmediate(cb);
        }
        self._isTrustedServiceUserMessageInstance(sessClientId, function (err, trusted) {
            if (err) {
                logger.error("Error on _isTrustedServiceInstance() call:", err, message);
                self._sendErrorToClient(sessClientId, message, null, Errors.ERR_FATAL_ERROR);
                return setImmediate(cb, true);
            }
            
            if (trusted !== true) {
                logger.error("Not a trusted userMessage client:", message);
                self._sendErrorToClient(sessClientId, message, null, Errors.ERR_VALIDATION_FAILED);
                return setImmediate(cb, true);
            }
            
            
            var clientId = message.getClientId();
            var api = message.getMessage();
            var resMessage = new ProtocolMessage(message);
            resMessage.setMessage(api);
            resMessage.setTimestamp(Date.now());
            resMessage.deleteClientId();
            resMessage.deleteClientInfo();
            
            self.sendMessageToClient(clientId, JSON.stringify(resMessage.getMessageContainer()));

            //True as we want to prevent any further message processing
            return setImmediate(cb, true);
        });
    } catch (e) {
        logger.error("Error on Handle Service As Client To Client Message",e, sessClientId, message);
        this._sendErrorToClient(sessClientId, message, null, Errors.ERR_FATAL_ERROR);
        return setImmediate(cb, true);
    }
};

o._rejectPrivateApi = function (clientSession, message, cb) {
    try {
        var self = this;
        var clientId = clientSession.getId();
        
        if (self._rejectPrivateApiFlag === true && message.isPrivate()) {
            logger.error('Call to private API:', message);
            self._sendErrorToClient(clientId, message, null, Errors.ERR_VALIDATION_FAILED);
            return setImmediate(cb, true);
        }
        
        return setImmediate(cb, false);
    } catch (e) {
        logger.error('Error on private API check:', e, message);
        self._sendErrorToClient(clientId, message, null, Errors.ERR_FATAL_ERROR);
        return setImmediate(cb, true);
    }
};

/**
 * Get or create client session.
 * @param {string} clientId
 * @returns {ClientSession|e}
 */
o._getOrCreateClientSession = function (clientId) {
    try {
        // create client sesion if not exists
        if ( _.isUndefined(this.clientsSession[clientId]) ) {
            this.clientsSession[clientId] = new ClientSession(clientId);
        }

        return this.clientsSession[clientId];
    } catch (e) {
        return e;
    }
};

/**
 * Get or create client queue.
 * @param {string} clientId
 * @returns {}
 */
o._getClientQueue = function (clientId) {
    if ( _.isUndefined(this.clientsQueue[clientId]) ) {
        this.clientsQueue[clientId] = async.queue(this._processQueueMessage.bind(this), 1);
    }
    
    return this.clientsQueue[clientId];
};

/**
 * Send message to service after couple of verifications.
 * @param {type} task
 * @param {type} cb
 * @returns {unresolved}
 */
o._processQueueMessage = function (task, cb) {
    try {
        var self = this;
        var clientId = task.clientId;
        var strMessage = task.strMessage;
        var clientIp = task.clientIp;
        
        var clientSession = self._getOrCreateClientSession(clientId);
        var message = null;
        
        async.series([
            
            // validate message: json and schema
            function (next) {
                self._validateMessage(clientId, strMessage, function (err, msg) {
                    if (err) {
                        return next(err);
                    }

                    message = msg;
                    return next();
                });
            },
            
            // send messages from internal services as clients to clients. 
            function (next) {
                self._handleServiceAsClientToClientMessages(clientSession, message, next);
            },
            // reject private messages
            function (next) {
                self._rejectPrivateApi(clientSession, message, next);
            },
            
            // manage tokens
            function (next) {
                self._manageTokens(clientSession, message, next);
            },
            
            // add global client session if needed
            function (next) {
                self._addGlobalClientSession(clientSession, message, next);
            },
            
            function (next) {
                self._manageClientInfo(clientSession, message, clientIp, next);
            },
            
            // forward the message
            function (next) {
                try {
                    if (message.getMessage() === 'global/auth') {
                        var resMessage = new ProtocolMessage(message);
                        resMessage.setContent(null);
                        resMessage.setError(null);
                        resMessage.setTimestamp(Date.now());
                        self.sendMessageToClient(clientId, JSON.stringify(resMessage.getMessageContainer()));
                        return setImmediate(next, false);
                    }
                    
                    
                    var serviceName = self.serviceLoadBalancerManager.getServiceNameByNamespace(message.getMessageNamespace());
                    
                    // inject clientInfo, clinetId into message. set token = null
                    message.setClientId(clientId);
                    message.setClientInfo(clientSession.clientInfo);
                    message.setToken(null);
                    
                    
                    self.geNewrelictMetrics().apiHandlerStart(message);
                    
                    self._sendMessageToService(clientId, serviceName, message);
                    return setImmediate(next, false);
                } catch (e) {
                    logger.error('MessageDispatcher.sendMessage()', e, message);
                    self._sendErrorToClient(clientId, message, null, Errors.ERR_FATAL_ERROR);
                    return setImmediate(next, true);
                }
            }
        ], function () {
            cb();
        });
    } catch (e) {
        logger.error('MessageDispatcher.sendMessage()', e, message);
        self._sendErrorToClient(clientId, message, (message ? null : 'global/errorResponse'), Errors.ERR_FATAL_ERROR);
        return setImmediate(cb);
    }
    
};


/**
 * Just add message into client queue.
 * @param {type} clientId
 * @param {type} message
 * @returns {undefined}
 */
o.sendMessageToService = function (clientId, strMessage, clientIp) {
    this._getClientQueue(clientId).push({
        clientId: clientId,
        clientIp: clientIp,
        strMessage: strMessage
    });
    return;
};

/**
 * Receive message sent by load balancer manager, parse it according to protocol, delete clientId component then emit the message event. Gateway listen to message event.
 * @param {type} strMessage
 * @returns {e}
 */
o._onServiceLoadBalancerManagerMessage = function (strMessage) {
    try {
        var self = this;
        var message = new ProtocolMessage( JSON.parse(strMessage) );
        var clientId = message.getClientId();
        
        message.setTimestamp(Date.now());
        message.deleteClientId();
        message.deleteClientInfo();
        self.sendMessageToClient(clientId, JSON.stringify(message.getMessageContainer()));
        return;
    } catch (e) {
        logger.error('MessageDispatcher._onServiceLoadBalancerManagerMessage', e);
        return e;
    }
};

/**
 * Find session by user ID. Use token payload. 
 * @param {type} userId
 * @returns {nm$_MessageDispatcher.o._getSessionByUserId.session|MessageDispatcher.prototype._getSessionByUserId.session|o._getSessionByUserId.session|nm$_MessageDispatcher.o._getSessionByUserId@arr;clientsSession|e}
 */
o._getSessionByUserId = function (userId) {
    try {
        for (var k in this.clientsSession) {
            if (!this.clientsSession.hasOwnProperty(k)) {
                continue;
            }
            
            var session = this.clientsSession[k];
            
            var payload = session.getTokenPayload();
            if (payload !== null && payload.userId === userId) {
                return session;
            }
        }
        
        return null;
        
    } catch (e) {
        return e;
    }
};

/**
 * Handle the event sent by auth service. If user is connected set aut token on session.
 * @param {type} eventContent
 * @returns {e}
 */
o._onTokenInvalidated = function (eventContent) {
    try {
        //logger.debug('_onTokenInvalidated event received, eventContent=', eventContent);
        var self = this;
        var token = eventContent.token;
        
        // decode the token
        self._auth.verifyToken(token, function (err, payload) {
            try {
                if (err) {
                    logger.error('Auth token not valid', err);
                    return;
                }

                var userId = payload.userId;
                var session = self._getSessionByUserId(userId);

                if (session instanceof Error) {
                    logger.error('Eror on finding user session', session);
                    return;
                }

                if (session !== null) { // user is connected
                    
                    var userProfile = null;
                    // try at most 3 times to get user profile
                    async.mapSeries([1,2,3], function (item, cbItem) {
                        self._getUserProfile(payload.userId, function (err, up) {
                            if (err) {
                                logger.error('Eror on _getUserProfile on token invalidation', payload.userId, err);
                                return cbItem();
                            }
                            
                            userProfile = up;
                            return cbItem(new Error('skip'));
                        });
                    }, function () {
                        if (!userProfile) {
                            session.clientInfo.profile = null;
                        } else {
                            session.clientInfo.profile = userProfile;
                        }
                        
                        // set session token
                        session.setToken(token);
                        session.setTokenPayload(payload);
                    });

                }
            } catch (e) {
                logger.error('Error _onTokenInvalidated', e);
            }
        });
    } catch (e) {
        logger.error('Error _onTokenInvalidated', e);
    }
};


/**
 * 
 * @param {type} instanceClients
 * @param {type} serviceName
 * @returns {e}
 */
o._onServiceInstanceDisconnect = function (instanceClients, serviceName) {
    try {
        var self = this;
        for(var i=0; i<instanceClients.length; i++) {
            var message = new ProtocolMessage();
            message.setMessage('gateway/instanceDisconnect');
            message.setContent({
                serviceName: serviceName
            });
            
            message.setTimestamp(Date.now());
            
            self.sendMessageToClient(instanceClients[i], JSON.stringify(message.getMessageContainer()));
        }
    } catch (e) {
        logger.error('MessageDispatcher._onServiceInstanceDisconnect', e);
        return e;
    }
};


/**
 * Call load balancers manager disconnectClient method
 * @param {type} clientId
 * @returns {e}
 */
o.disconnectClient = function (clientId) {
    try {
        var self = this;
        // kill message queue
        if(this.clientsQueue[clientId]) {
            this.clientsQueue[clientId].kill();
        }
        var clientSession = self._getOrCreateClientSession(clientId);
        var userId;
        var removeGlobalClientSessionNeeded;
        if (clientSession.globalSessionCreated && clientSession.getTokenPayload() && clientSession.getTokenPayload().userId) {
            removeGlobalClientSessionNeeded = true;
            userId = clientSession.getTokenPayload().userId;
        }
        
        // create clientDisconect message
        var message = new ProtocolMessage();
        message.setMessage('gateway/clientDisconnect');
        message.setClientId(clientId);
        
        delete this.clientsSession[clientId];
        delete this.clientsQueue[clientId];
        
        this.serviceLoadBalancerManager.disconnectClient(clientId, JSON.stringify(message.getMessageContainer()));
        
        if (removeGlobalClientSessionNeeded) {
            self._removeGlobalClientSession(userId, clientId, function (err) {
                if (err) {
                    logger.error('MessageDispatcher.disconnectClient error removing global client session', clientId, err);
                } else {
                    logger.debug('MessageDispatcher.disconnectClient global client session removed, clientId:', clientId);
                }
            });
        }
    } catch (e) {
        logger.error('MessageDispatcher.disconnectClient', e);
        return e;
    }
};


o.countClientSessions = function () {
    return _.keys(this.clientsSession).length;
};

MessageDispatcher.defaultAppConfig = {
    tenantId: 1,
    appId: 'default_appId',
    deviceUUID: 'default_deviceUUID'
};

module.exports = MessageDispatcher;

