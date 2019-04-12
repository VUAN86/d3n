var SessionDataStorage = require('./SessionDataStorage.js');
var Auth = require('./Auth.js');
var logger = require('nodejs-logger')();
var ProtocolMessage = require('nodejs-protocol');
var _ = require('lodash');

/**
 * @typedef {object} ClientSessionConfig
 * @proparty {boolean} protocolLogging
 * @property {AuthConfig} authConfig
 */

/**
 * @typedef {object} ClientSession
 * Represents a connected client including its auth state and data storage.
 * It is also possible, to emit messages to that client.
 *
 * @param {object} connection
 * @param {string} id
 * @param {ClientSessionConfig} config
 * @constructor
 */
function ClientSession(connection, id, config) {
    /**
     * @type {ClientSessionConfig}
     * @private
     */
    this._config = config;
    /**
     * @type {Object}
     * @private
     */
    this._connection = connection;
    /**
     * @type {string}
     * @private
     */
    this._id = id;

    /**
     * @type {SessionDataStorage}
     * @private
     */
    this._dataStorage = new SessionDataStorage();
    /**
     * @type {Auth}
     * @private
     */
    this._auth = new Auth(config.authConfig);
    /**
     * @type {boolean}
     * @private
     */
    this._isDirectConnection = this.getId() === this.getConnectionSession().getId();
    
    /**
     * @type {Integer}
     * @private
     */
    this._tenantId = null;
    
    /**
     * @type {Array}
     * @private
     */
    this._roles = null;
    
    this._clientInfo = null;
}
var o = ClientSession.prototype;

/**
 * Check if client is authenticated ot not
 * @returns {Boolean}
 */
o.isAuthenticated = function () {
    return (_.has(this._clientInfo, 'profile.userId') && !_.isEmpty(this._clientInfo.profile.userId));
};


/**
 * Returns authentication for this session.
 * @returns {Auth}
 */
o.getAuthentication = function() {
    return this._auth;
};

/**
 * Returns datastorage for this session
 * @returns {SessionDataStorage}
 */
o.getDataStorage = function () {
    return this._dataStorage;
};

/**
 * Returns session id
 * @returns {string|*}
 */
o.getId = function () {
    return this._id;
};

/**
 * Returns client info.
 */
o.getClientInfo = function () {
    return this._clientInfo;
};


/**
 * Send message through attached ws connection
 * adding client id in order to ensure correct broadcasting
 * @param {ProtocolMessage} message
 * @returns {boolean} false if an error occurred while sending
 */
o.sendMessage = function (message) {
    if (!this._isDirectConnection) {
        message.setClientId(this._id);
    }

    try {
        var plainMessage = JSON.stringify(message.getMessageContainer());
        
        if (this._config.protocolLogging) {
            this._connection.service.getLogger().info(JSON.stringify({
                timestamp: new Date().getTime(),
                identification: process.cwd()+":"+process.pid,
                type: 'data',
                action: 'sent',
                from: this._connection.requestData.sockName.address + ":" + this._connection.requestData.sockName.port,
                to: this._connection.requestData.remoteAddress + ':' + this._connection.requestData.remotePort,
                message: plainMessage
            }));
        }
        
        var nrMessage = new ProtocolMessage(message.getMessageContainer());
        nrMessage.setClientId(message.getClientId() || this._id);
        this.getConnectionService().geNewrelictMetrics().apiHandlerEnd(nrMessage);
        
        this._connection.send( plainMessage );
        return true;
    } catch (e) {
        logger.error('ClientSession.sendMessage', e);
        return false;
    }
};

/**
 * Returns websocket session, this client is connected over
 * @returns {ConnectionSession}
 */
o.getConnectionSession = function() {
    return this._connection.session;
};

/**
 *
 * @returns {Service}
 */
o.getConnectionService = function() {
    return this._connection.service;
};

/**
 * Called when connection is closed,
 * cleaning up, emitting connection closed event.
 */
o.onclose = function() {
    this._connection.service.getBroadcastSystem().removeFromAllRooms(this);
};

/**
 * Set client tenantId.
 * @param {Integer} tenantId
 */
o.setTenantId = function (tenantId) {
    this._tenantId = tenantId;
};

/**
 * Set client info.
 * @param {Object} clientInfo
 */
o.setClientInfo = function (clientInfo) {
    this._clientInfo = clientInfo;
};


/**
 * Set client roles.
 * @param {Array} roles
 */
o.setRoles = function (roles) {
    this._roles = roles;
};


o.getRoles = function () {
    try {
        return this._clientInfo.profile.roles;
    } catch (e) {
        logger.error('ClientSession.getRoles() error:', e);
        return [];
    }
};

o.getTenantId = function () {
    try {
        return this._clientInfo.appConfig.tenantId;
    } catch (e) {
        logger.error('ClientSession.getTenantId() error:', e);
        return null;
    }
};

o.getAppId = function () {
    try {
        return this._clientInfo.appConfig.appId;
    } catch (e) {
        logger.error('ClientSession.getAppId() error:', e);
        return null;
    }
};


o.getUserId = function () {
    try {
        return this._clientInfo.profile.userId;
    } catch (e) {
        logger.error('ClientSession.getUserId() error:', e);
        return null;
    }    
};

module.exports = ClientSession;