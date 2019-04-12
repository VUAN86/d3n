var _ = require('lodash');
var async  =require('async');
var logger = require('nodejs-logger')();
var DefaultServiceClient = require('nodejs-default-client');
var RegistryServiceClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');

/**
 * @typedef {object} ConstructorConfig
 * @property {string} registryServiceURIs - Commaseperated list of uris for service registry
 */

/**
 * Friend service client constructor.
 *
 * @type FriendServiceClient
 * @param {ConstructorConfig} config
 * @constructor
 */
function FriendServiceClient (config) {
    /**
     * @type {ConstructorConfig}
     */

    this._config = _.assign({
        registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
    }, config || {});

    if (!_.isString(this._config.registryServiceURIs) || !this._config.registryServiceURIs.length ) {
        return new Error('ERR_WRONG_REGISTRY_SERVICE_URIS');
    }


    this._defaultClient = new DefaultServiceClient({
        serviceNamespace: 'friend',
        reconnectForever: false,
        autoConnect: true,
        serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: this._config.registryServiceURIs}),
        headers: this._config.ownerServiceName ? {service_name: this._config.ownerServiceName} : null
    });

    this._defaultClient.on('message', function () {

    });

    this._seq = 0;

};

var o = FriendServiceClient.prototype;


/**
 * Send a message to friend service.
 * @param {ProtocolMessage} message
 * @param {function} cb
 * @returns {unresolved}
 */
o._request = function (message, cb) {
    try {
        var self = this;
        return self._defaultClient.sendMessage(message, cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Get group information
 *
 * @param {string} groupId
 * @param {string} tenantId
 * @param {boolean} waitResponse
 * @param {function} cb
 * @returns {Error} 
 */
o.groupGet = function(groupId, tenantId, userId, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('FriendServiceClient.groupGet() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        // check arguments
        var content = { 
            groupId: groupId,
            tenantId: tenantId,
            userId: userId
        };

        // if wait for response set seq
        var seq = self._getSeq();

        var pm = new ProtocolMessage();
        pm.setMessage('friend/groupGet');
        pm.setContent(content);
        pm.setSeq(seq);

        return self._request(pm, cb);
   } catch (e) {
        logger.error('FriendServiceClient.groupGet() error: ' , e);
        return setImmediate(cb, e);
    }
};

/**
 * Update group
 *
 * @param {string} groupId
 * @param {string} tenantId
 * @param {string} name
 * @param {string} image
 * @param {boolean} waitResponse
 * @param {function} cb
 * @returns {Error} 
 */
o.groupUpdate = function(groupId, tenantId, userId, name, type, image, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('FriendServiceClient.groupUpdate() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        // check arguments
        var content = { 
            groupId:  groupId,
            tenantId: tenantId,
            userId:   userId,
            name:     name,
            type:     type,
            image:    image
        };

        // if wait for response set seq
        var seq = self._getSeq();

        var pm = new ProtocolMessage();
        pm.setMessage('friend/groupUpdate');
        pm.setContent(content);
        pm.setSeq(seq);

        return self._request(pm, cb);
   } catch (e) {
        logger.error('FriendServiceClient.groupUpdate() error: ' , e);
        return setImmediate(cb, e);
    }
};


/**
 * Get information if user is part of a given group
 *
 * @param {string} groupId
 * @param {string} tenantId
 * @param {string} userId
 * @param {boolean} waitResponse
 * @param {function} cb
 * @returns {Error} 
 */
o.playerIsGroupMember = function(groupId, tenantId, userId, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('FriendServiceClient.playerIsGroupMember() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        // check arguments
        var content = { 
            groupId: groupId,
            tenantId: tenantId,
            userId: userId
        };

        var seq = self._getSeq();

        var pm = new ProtocolMessage();
        pm.setMessage('friend/playerIsGroupMember');
        pm.setContent(content);
        pm.setSeq(seq);

        return self._request(pm, cb);
   } catch (e) {
        logger.error('FriendServiceClient.playerIsGroupMember() error: ' , e);
        return setImmediate(cb, e);
    }
};

o.buddyAddForUser = function (userId, userIds, favorite, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('FriendServiceClient.buddyAddForUser() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        var content = { 
            userId: userId,
            userIds: userIds
        };
        
        if (!_.isUndefined(favorite)) {
            content.favorite =  favorite;
        }
        
        var seq = self._getSeq();

        var pm = new ProtocolMessage();
        pm.setMessage('friend/buddyAddForUser');
        pm.setContent(content);
        pm.setSeq(seq);

        return self._request(pm, cb);
    } catch (e) {
        logger.error('FriendServiceClient.buddyAddForUser() error: ' , e);
        return setImmediate(cb, e);
    }
    
};


o._getSeq = function () {
    return ++this._seq;
};


o.disconnect = function (code, reason) {
    try {
        var _code = code || 1000;
        var _reason = reason || 'client_close';
        this._defaultClient.disconnect(_code, _reason, true);
    } catch (e) {
        logger.error('FriendServiceClient.disconnect() tc error:', e);
        throw e;
    }
};

module.exports = FriendServiceClient;
