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
 * voucher service client constructor.
 *
 * @type VoucherServiceClient
 * @param {ConstructorConfig} config
 * @constructor
 */
function VoucherServiceClient (config) {
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
        serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: this._config.registryServiceURIs})
    });
};

var o = VoucherServiceClient.prototype;


/**
 * Send a message to voucher service.
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
 * Reserve voucher to user using voucher service.
 * @param {Object} params - Email parameters:
 * - voucherId - Voucher ID
 * - waitResponse - wait to response until return callback (optional, default true)
 * @param {Function} cb
 * @returns {Error|Boolean}
 */
o.userVoucherReserve = function(params, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('VoucherServiceClient.userVoucherReserve() error: ', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }

        // if there are no parameters just return error
        if (!_.isObject(params) || !_.has(params, 'voucherId')) {
            logger.error('VoucherServiceClient.userVoucherReserve() error: ', Errors.ERR_VALIDATION_FAILED.message);
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }

        // Setup protocol message
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/userVoucherReserve';
        pm.setMessage(api);

        // Setup mandatory parameters
        var content = {
            voucherId: params.voucherId
        };
        pm.setContent(content);

        // Setup if callback need to wait for response
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            pm.setSeq(self._getSeq());
        }
        
        // Validate protocol message against schema
        if (pm.isValid(api) !== true) {
            logger.error('VoucherServiceClient.userVoucherReserve() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        return self._request(pm, cb);
        
   } catch (e) {
        logger.error('VoucherServiceClient.userVoucherReserve() error: ' , e);
        return setImmediate(cb, e);
    }
};

/**
 * Release voucher reserved to user using voucher service.
 * @param {Object} params - SMS parameters:
 * - voucherId - Voucher ID
 * - waitResponse - wait to response until return callback (optional, default true)
 * @param {Function} cb
 * @returns {Error|Boolean}
 */
o.userVoucherRelease = function (params, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('VoucherServiceClient.userVoucherRelease() error: ', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }

        // if there are no parameters just return error
        if (!_.isObject(params) || !_.has(params, 'voucherId')) {
            logger.error('VoucherServiceClient.userVoucherRelease() error: ', Errors.ERR_VALIDATION_FAILED.message);
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }

        // Setup protocol message
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/userVoucherRelease';
        pm.setMessage(api);

        // Setup mandatory parameters
        var content = {
            voucherId: params.voucherId
        };
        pm.setContent(content);
        
        // Setup if callback need to wait for response
        if (_.isUndefined(params.waitResponse) || params.waitResponse === true) {
            pm.setSeq(self._getSeq());
        }
        
        if (pm.isValid(api) !== true) {
            logger.error('VoucherServiceClient.userVoucherRelease() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        return self._request(pm, cb);
        
    } catch (e) {
        logger.error('VoucherServiceClient.userVoucherRelease() error: ', e);
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
        logger.error('VoucherServiceClient.disconnect() tc error:', e);
        throw e;
    }
};


module.exports = VoucherServiceClient;