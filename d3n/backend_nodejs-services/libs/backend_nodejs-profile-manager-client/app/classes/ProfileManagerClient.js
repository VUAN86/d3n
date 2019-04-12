var _ = require('lodash');
var Errors = require('../config/errors.js');
var Constants = require('../config/constants.js');
var logger = require('nodejs-logger')();
var DefaultServiceClient = require('nodejs-default-client');
var RegistryServiceClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');

/**
 * @typedef {object} ConstructorConfig
 * @property {string} registryServiceURIs - Commaseperated list of uris for service registry
 */

/**
 * Profile manager client constructor.
 *
 * @type 
 * @param {ConstructorConfig} config
 * @constructor
 */
function ProfileManagerClient (config) {
    /**
     * @type {ConstructorConfig}
     */
    
    this._config = _.assign({
        registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
    }, config || {});
    
    if (!_.isString(this._config.registryServiceURIs) || !this._config.registryServiceURIs.length ) {
        return new Error(Errors.ERR_WRONG_REGISTRY_SERVICE_URIS.message);
    }
    
    
    this._defaultClient = new DefaultServiceClient({
        serviceNamespace: Constants.SERVICE_NAME,
        reconnectForever: false,
        autoConnect: true,
        serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: this._config.registryServiceURIs}),
        headers: this._config.ownerServiceName ? {service_name: this._config.ownerServiceName} : null
    });
};


var o = ProfileManagerClient.prototype;

/**
 * Send a message to workflow service.
 * @param {ProtocolMessage} message
 * @param {function} cb
 * @returns {unresolved}
 */
o._request = function (message, cb) {
    try {
        var self = this;
        self._defaultClient.sendMessage(message, function (err, response) {
            try {
                if (err) {
                    logger.error('ProfileManagerClient._request() error on request call:', err, JSON.stringify(message), JSON.stringify(response));
                    return cb(err);
                }

                if (ProtocolMessage.isInstance(response) && !_.isNull(response.getError())) {
                    logger.error('ProfileManagerClient._request() error on response message:', response.getError(), JSON.stringify(message), JSON.stringify(response));
                    return cb(new Error(response.getError().message), response);
                }

                return cb(err, response);
            } catch (e) {
                logger.error('ProfileManagerClient._request() error on handling response:', e, JSON.stringify(message), JSON.stringify(response));
                return cb(e);
            }
        });
    } catch (e) {
        logger.error('ProfileManagerClient._request() error calling:', e, JSON.stringify(message));
        return setImmediate(cb, e);
    }
};

o.adminListByTenantId = function(tenantId, cb) {
    try {
        var self = this;
        
        var content = {
            tenantId: tenantId
        };
        
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/adminListByTenantId';
        pm.setMessage(api);
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        self._request(pm, function (err, response) {
            if (err) {
                return cb(err, response);
            }
            
            if (response.getContent() === null) {
                response.setContent({
                    admins: []
                });
            }
            
            return cb(err, response);
        });
        
   } catch (e) {
        logger.error('ProfileManagerClient.adminListByTenantId() error when calling:', e);
        return setImmediate(cb, e);
    }
};

o.userRoleListByTenantId = function (profileId, tenantId, cb) {
    try {
        var self = this;
        
        var content = {
            profileId: '' + profileId,
            tenantId: '' + tenantId
        };
        
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/userRoleListByTenantId';
        pm.setMessage(api);
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        self._request(pm, function (err, response) {
            if (err) {
                return cb(err, response);
            }
            
            if (response.getContent() === null) {
                response.setContent({
                    roles: []
                });
            }
            
            return cb(err, response);
        });
        
        
    } catch (e) {
        logger.error('ProfileManagerClient.userRoleListByTenantId() error when calling:', e);
        return setImmediate(cb, e);
    }
};

o._getSeq = function () {
    return this._defaultClient.getSeq();
};

o.disconnect = function (code, reason) {
    try {
        var _code = code || 1000;
        var _reason = reason || 'client_close';
        this._defaultClient.disconnect(_code, _reason, true);
    } catch (e) {
        logger.error('ProfileManagerClient.disconnect() tc error:', e);
        throw e;
    }
};


module.exports = ProfileManagerClient;