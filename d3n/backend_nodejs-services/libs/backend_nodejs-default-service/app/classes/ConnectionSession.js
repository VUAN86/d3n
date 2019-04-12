var ClientSession = require('./ClientSession.js')
;

/**
 * @typedef {object} ConnectionSessionConfig
 * @property {ClientSessionConfig} clientConfig
 */

/**
 * @type ConnectionSession
 * Represents a websocket connection, which could include multiple clients via a gateway service
 * It is possible to access single client session objects in order to emit messages
 *
 * @param {object} connection
 * @param {string} id
 * @param {ConnectionSessionConfig} config
 * @constructor
 */
function ConnectionSession(connection, id, config) {
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
     * @type {Object.<string, ClientSession>}
     * @private
     */
    this._clientSessions = {};
    /**
     * @type {ConnectionSessionConfig}
     * @private
     */
    this._config = config;
}
var o = ConnectionSession.prototype;

/**
 * Handles websockets onclose event
 */
o.onclose = function() {
    var sessions = this.getClientSessions();
    for (var sessionId in sessions) {
        if (sessions.hasOwnProperty(sessionId)) {
            this.deleteClientSession(sessionId);
        }
    }
};

/**
 * Returns id of this connection
 * @returns {string}
 */
o.getId = function () {
    return this._id;
};

/**
 * Returns map of all registered clients in this socket connection
 * @returns {Object.<string, ClientSession>}
 */
o.getClientSessions = function () {
    return this._clientSessions;
};

/**
 * Returns client session of specific client id or null if not found
 * @param {string} clientId
 * @returns {ClientSession|null}
 */
o.getClientSession = function (clientId) {
    if (clientId in this._clientSessions) {
        return this._clientSessions[clientId];
    }  else {
        return null;
    }
};

/**
 * Returns client session of specific client id or creates a client if not found
 * @param {string} clientId
 * @returns {ClientSession}
 */
o.getOrCreateClientSession = function (clientId) {
    if (!this.getClientSession(clientId)) {
        this.createClientSession(clientId);
    }

    return this.getClientSession(clientId);
};

/**
 * Creates client session, if not existing
 * @param {string} clientId
 */
o.createClientSession = function (clientId) {
    if (!this.getClientSession(clientId)) {
        this._clientSessions[clientId] = new ClientSession(this._connection, clientId, this._config.clientConfig);
    }
};

/**
 * Delete a client session, if it exists
 * @param {string} clientId
 */
o.deleteClientSession = function (clientId) {
    if (this.getClientSession(clientId)) {
        this._clientSessions[clientId].onclose();
        delete this._clientSessions[clientId];
    }
};

module.exports = ConnectionSession;