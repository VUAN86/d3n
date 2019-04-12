var _ = require('lodash');
var Errors = require('../config/errors.js');
var Constants = require('../config/constants.js');
var DefaultServiceClient = require('nodejs-default-client');
var RegistryServiceClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

/**
 * @typedef {object} ConstructorConfig
 * @property {string} registryServiceURIs - Commaseperated list of uris for service registry
 */

/**
 * User message service client constructor.
 *
 * @type UserMessageClient
 * @param {ConstructorConfig} config
 * @constructor
 */
function UserMessageClient (config) {
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

var o = UserMessageClient.prototype;


/**
 * Send a message to user message service.
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
 * Send email using user message service.
 * @param {Object} params - Email parameters:
 * - userId - Profile ID
 * - address - Email address (optional). If not passed, then email will be sent to first confirmed or last unconfirmed email address
 * - subject - Email subject string
 * - message - Email message string
 * - subject_parameters - array of subject parameters, that can be met as substitution variables in email subject template like {0}, {1} .. {n} (optional)
 * - message_parameters - array of message parameters, that can be met as substitution variables in email message template like {0}, {1} .. {n} (optional)
 * - language - Email template language (optional, default 'en')
 * - waitResponse - wait to response until return callback (optional, default true)
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {Function} cb
 * @returns {Error|Boolean}
 */
o.sendEmail = function(params, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('UserMessageClient.sendEmail() error: ', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }

        // if there are no parameters just return error
        if (!_.isObject(params)) {
            logger.error('UserMessageClient.sendEmail() error: ', Errors.ERR_VALIDATION_FAILED.message);
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }

        // Setup protocol message
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/sendEmail';
        pm.setMessage(api);

        // Setup mandatory and optinal parameters, default values
        var content = {
            userId: params.userId,
            subject: params.subject,
            message: params.message,
            language: 'auto'
        };
        if (params.language) {
            content.language = params.language;
        }
        if (params.address) {
            content.address = params.address;
        }
        if (params.subject_parameters) {
            content.subject_parameters = params.subject_parameters;
        }
        if (params.message_parameters) {
            content.parameters = params.message_parameters;
        }
        pm.setContent(content);

        // Setup if callback need to wait for response
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            pm.setSeq(self._getSeq());
        }
        
        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        // Validate protocol message against schema
        if (pm.isValid(api) !== true) {
            logger.error('UserMessageClient.sendEmail() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        return self._request(pm, cb);
        
   } catch (e) {
        logger.error('UserMessageClient.sendEmail() error: ' , e);
        return setImmediate(cb, e);
    }
};

/**
 * Send SMS using user message service.
 * @param {Object} params - SMS parameters:
 * - userId - Profile ID
 * - phone - Phone number (optional). If not passed, then SMS will be sent to first confirmed or last unconfirmed phone number
 * - message - SMS message string
 * - message_parameters - array of SMS message parameters, that can be met as substitution variables in email message template like {0}, {1} .. {n} (optional)
 * - language - SMS template language (optional, default 'en')
 * - waitResponse - wait to response until return callback (optional, default true)
 * - clientInfo - client info object, contains appConfig and other info, can be used for some additional substitution variables like [object.field] (optional)
 * @param {Function} cb
 * @returns {Error|Boolean}
 */
o.sendSms = function (params, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('UserMessageClient.sendSms() error: ', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }

        // if there are no parameters just return error
        if (!_.isObject(params)) {
            logger.error('UserMessageClient.sendSms() error: ', Errors.ERR_VALIDATION_FAILED.message);
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }

        // Setup protocol message
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/sendSms';
        pm.setMessage(api);

        // Setup mandatory and optinal parameters, default values
        var content = {
            userId: params.userId,
            message: params.message,
            language: 'auto'
        };
        if (params.language) {
            content.language = params.language;
        }
        if (params.phone) {
            content.phone = params.phone;
        }
        if (params.message_parameters) {
            content.parameters = params.message_parameters;
        }
        pm.setContent(content);
        
        // Setup if callback need to wait for response
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            pm.setSeq(self._getSeq());
        }
        
        // Setup client info object if provided
        if (params.clientInfo) {
            pm.setClientInfo(params.clientInfo);
        }

        if (pm.isValid(api) !== true) {
            logger.error('UserMessageClient.sendSms() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        return self._request(pm, cb);
        
    } catch (e) {
        logger.error('UserMessageClient.sendSms() error: ', e);
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
        logger.error('UserMessageClient.disconnect() tc error:', e);
        throw e;
    }
};

module.exports = UserMessageClient;