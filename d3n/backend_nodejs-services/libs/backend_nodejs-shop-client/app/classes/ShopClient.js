var _ = require('lodash');
var Errors = require('../config/errors.js');
var Constants = require('../config/constants.js');
var DefaultServiceClient = require('nodejs-default-client');
var RegistryServiceClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

/**
 * Shop service client constructor.
 *
 * @type ShopClient
 * @param {ConstructorConfig} config
 * @constructor
 */
function ShopClient (config) {
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

var o = ShopClient.prototype;


/**
 * Send a message to shop service.
 * @param {ProtocolMessage} message
 * @param {function} cb
 * @returns {unresolved}
 */
o._request = function (message, cb) {
    try {
        var self = this;
        return self._defaultClient.sendMessage(message, function (err, response) {
            if (err) {
                return cb(err);
            }
            
            if (ProtocolMessage.isInstance(response) && !_.isNull(response.getError())) {
                return cb(new Error(response.getError().message), response);
            }
            
            return cb(err, response);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Create tenant in shop using shop service.
 * @param {Object} params - Tenant parameters:
 * - id - tenant ID
 * - title - tenant name
 * - description - tenant description
 * - iconURL - tenant URL for icon
 * - termsURL - tenant URL for terms and conditions
 * - street - tenant street
 * - streetNumber - tenant street number
 * - city - tenant city
 * - country - tenant country, 2 letter ISO code
 * - zip - tenant zip code
 * - phone - tenant phone number
 * - email - tenant email
 * - taxNumber - tenant tax number
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {Function} cb
 * @returns {Error|Boolean}
 */
o.tenantAdd = function(params, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('ShopClient.tenantAdd() error: ', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }

        // if there are no parameters just return error
        if (!_.isObject(params)) {
            logger.error('ShopClient.tenantAdd() error: ', Errors.ERR_VALIDATION_FAILED.message);
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }

        // Setup protocol message
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/tenantAdd';
        pm.setMessage(api);

        // Setup mandatory and optional parameters
        var content = {
            id: '' //this must be string in shop
        };
        if (params.title) {
            content.title = params.title;
        }
        if (params.description) {
            content.description = params.description;
        }
        if (params.iconURL) {
            content.iconURL = params.iconURL;
        }
        if (params.termsURL) {
            content.termsURL = params.termsURL;
        }
        if (params.street) {
            content.street = params.street;
        }
        if (params.streetNumber) {
            content.streetNumber = params.streetNumber;
        }
        if (params.city) {
            content.city = params.city;
        }
        if (params.country) {
            content.country = params.country;
        }
        if (params.zip) {
            content.zip = params.zip;
        }
        if (params.phone) {
            content.phone = params.phone;
        }
        if (params.email) {
            content.email = params.email;
        }
        if (params.taxNumber) {
            content.taxNumber = params.taxNumber;
        }
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        // Validate protocol message against schema
        if (pm.isValid(api) !== true) {
            logger.error('ShopClient.tenantAdd() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        return self._request(pm, cb);
        
   } catch (e) {
        logger.error('ShopClient.tenantAdd() error: ' , e);
        return setImmediate(cb, e);
    }
};

/**
 * Update tenant in shop using shop service.
 * @param {Object} params - Tenant parameters:
 * - id - tenant ID
 * - title - tenant name
 * - description - tenant description
 * - iconURL - tenant URL for icon
 * - termsURL - tenant URL for terms and conditions
 * - street - tenant street
 * - streetNumber - tenant street number
 * - city - tenant city
 * - country - tenant country, 2 letter ISO code
 * - zip - tenant zip code
 * - phone - tenant phone number
 * - email - tenant email
 * - taxNumber - tenant tax number
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {Function} cb
 * @returns {Error|Boolean}
 */
o.tenantUpdate = function (params, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('ShopClient.tenantUpdate() error: ', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }

        // if there are no parameters just return error
        if (!_.isObject(params)) {
            logger.error('ShopClient.tenantUpdate() error: ', Errors.ERR_VALIDATION_FAILED.message);
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }

        // Setup protocol message
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/tenantUpdate';
        pm.setMessage(api);

        // Setup mandatory and optional parameters
        var content = {};
        if (_.has(params, 'id') && params.id) {
            content.id = params.id + '' //this must be string in shop
        }
        if (params.title) {
            content.title = params.title;
        }
        if (params.description) {
            content.description = params.description;
        }
        if (params.iconURL) {
            content.iconURL = params.iconURL;
        }
        if (params.termsURL) {
            content.termsURL = params.termsURL;
        }
        if (params.street) {
            content.street = params.street;
        }
        if (params.streetNumber) {
            content.streetNumber = params.streetNumber;
        }
        if (params.city) {
            content.city = params.city;
        }
        if (params.country) {
            content.country = params.country;
        }
        if (params.zip) {
            content.zip = params.zip;
        }
        if (params.phone) {
            content.phone = params.phone;
        }
        if (params.email) {
            content.email = params.email;
        }
        if (params.taxNumber) {
            content.taxNumber = params.taxNumber;
        }
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        if (pm.isValid(api) !== true) {
            logger.error('ShopClient.tenantUpdate() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        return self._request(pm, cb);
        
    } catch (e) {
        logger.error('ShopClient.tenantUpdate() error: ', e);
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
        logger.error('ShopClient.disconnect() tc error:', e);
        throw e;
    }
};

module.exports = ShopClient;