var _ = require('lodash');
var async  =require('async');
var errors = require('../config/errors.js');
var logger = require('nodejs-logger')();
var DefaultServiceClient = require('nodejs-default-client');
var RegistryServiceClient = require('nodejs-service-registry-client');
var url = require('url');
var ProtocolMessage = require('nodejs-protocol');

/**
 * @typedef {object} ConstructorConfig
 * @property {string} registryServiceURIs - Commaseperated list of uris for service registry
 */

/**
 * User message service client constructor.
 *
 * @type AuthServiceClient
 * @param {ConstructorConfig} config
 * @constructor
 */
function AuthServiceClient (config) {
    /**
     * @type {ConstructorConfig}
     */

    this._config = _.assign({
        registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
    }, config || {});

    if (!_.isString(this._config.registryServiceURIs) || !this._config.registryServiceURIs.length ) {
        return new Error(errors.ERR_WRONG_REGISTRY_SERVICE_URIS);
    }


    this._defaultClient = new DefaultServiceClient({
        serviceNamespace: 'auth',
        reconnectForever: false,
        autoConnect: true,
        serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: this._config.registryServiceURIs}),
        headers: this._config.ownerServiceName ? {service_name: this._config.ownerServiceName} : null
    });

    this._defaultClient.on('message', function () {

    });

    this._seq = 0;

};

var o = AuthServiceClient.prototype;


/**
 * Send a message to user message service.
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
 * Setup user roles
 * @param {Object} params - parameters:
 * - userId - Profile ID
 * - rolesToAdd - Roles to add (array of strings)
 * - rolesToRemove - Roles to remove (array of strings)
 * - waitResponse - wait to response until return callback (optional, default true)
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {function} cb
 * @returns {Error}
 */
o.setUserRole = function(params, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('AuthServiceClient.setUserRole() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        // check arguments
        var content = {
            userId:        params.userId,
            rolesToAdd:    params.rolesToAdd,
            rolesToRemove: params.rolesToRemove
        };

        // if wait for response set seq
        var seq = null;
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            seq = self._getSeq();
        }

        var pm = new ProtocolMessage();
        pm.setMessage('auth/setUserRole');
        pm.setContent(content);
        pm.setSeq(seq);

        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        return self._request(pm, cb);

   } catch (e) {
        logger.error('AuthServiceClient.setUserRole() error: ' , e);
        return setImmediate(cb, e);
    }
};

/**
 * Invite user by email and role, pass optional profile info
 * @param {Object} params - invitation parameters:
 * - email - Profile ID
 * - role - Role
 * - profileInfo - Profile info object
 * - waitResponse - wait to response until return callback (optional, default true)
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {function} cb
 * @returns {Error}
 */
o.inviteUserByEmailAndRole = function(params, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('AuthServiceClient.inviteUserByEmailAndRole() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        // check arguments
        var content = {
            email:       params.email,
            role:        params.role,
            profileInfo: params.profileInfo
        };

        // if wait for response set seq
        var seq = null;
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            seq = self._getSeq();
        }

        var pm = new ProtocolMessage();
        pm.setMessage('auth/inviteUserByEmailAndRole');
        pm.setContent(content);
        pm.setSeq(seq);

        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        return self._request(pm, cb);

   } catch (e) {
        logger.error('AuthServiceClient.inviteUserByEmailAndRole() error: ' , e);
        return setImmediate(cb, e);
    }
};

/**
 * Add a confirmed user in the authentication service. This functionality is being used
 * by the connector service (admin application) to handle the manipulation of tenant users:
 * creation of new tenant admin users.
 * Only e-mail is required and all the users are being created in Aerospike as registered and
 * confirmed users (no need to validate their account).
 * @param {Object} params - parameters:
 * - userInfo - An object that contains { email, phone, firstName, lastName }
 * - waitResponse - wait to response until return callback (optional, default true)
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {function} cb
 * @returns {Error}
 */
o.addConfirmedUser = function(params, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('AuthServiceClient.addConfirmedUser() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        // check arguments
        var content = _.clone(params.userInfo);

        // if wait for response set seq
        var seq = null;
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            seq = self._getSeq();
        }

        var pm = new ProtocolMessage();
        pm.setMessage('auth/addConfirmedUser');
        pm.setContent(content);
        pm.setSeq(seq);

        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        return self._request(pm, cb);

   } catch (e) {
        logger.error('AuthServiceClient.addConfirmedUser() error: ' , e);
        return setImmediate(cb, e);
    }
};

/**
 * Create an impersonation token for the given user identified by email. This functionality is being used
 * only by the tenant admin application to allow super administrator to signin as a tenant admin.
 * @param {Object} params - parameters:
 * - email - Email of the tenant admin user we want to impersonate
 * - tenantId - Identifier of the tenant, user must already be tenant admin for this tenant
 * - waitResponse - wait to response until return callback (optional, default true)
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {function} cb
 * @returns {Error}
 */
o.generateImpersonateToken = function(params, cb) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('AuthServiceClient.generateImpersonateToken() error: ', errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        var content = {
            email:    params.email,
            tenantId: params.tenantId || ''
        };

        // if wait for response set seq
        var seq = null;
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            seq = self._getSeq();
        }

        var pm = new ProtocolMessage();
        pm.setMessage('auth/generateImpersonateToken');
        pm.setContent(content);
        pm.setSeq(seq);

        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        return self._request(pm, cb);

   } catch (e) {
        logger.error('AuthServiceClient.generateImpersonateToken() error:' , e);
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
        logger.error('AuthServiceClient.disconnect() tc error:', e);
        throw e;
    }
};

module.exports = AuthServiceClient;
