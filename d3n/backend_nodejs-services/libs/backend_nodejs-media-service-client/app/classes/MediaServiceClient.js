var _ = require('lodash');
var async  =require('async');
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
 * @type MediaServiceClient
 * @param {ConstructorConfig} config
 * @constructor
 */
function MediaServiceClient (config) {
    /**
     * @type {ConstructorConfig}
     */

    this._config = _.assign({
        registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
    }, config || {});

    if (!_.isString(this._config.registryServiceURIs) || !this._config.registryServiceURIs.length ) {
        return new Error(Errors.ERR_WRONG_REGISTRY_SERVICE_URIS);
    }


    this._defaultClient = new DefaultServiceClient({
        serviceNamespace: Constants.SERVICE_NAME,
        reconnectForever: false,
        autoConnect: true,
        serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: this._config.registryServiceURIs})
    });

    this._defaultClient.on('message', function () {

    });

    this._seq = 0;

};

var o = MediaServiceClient.prototype;

/**
 * Send a message to user message service.
 * @param {ProtocolMessage} message
 * @param {function} callback
 * @returns {unresolved}
 */
o._request = function (message, callback) {
    try {
        var self = this;
        return self._defaultClient.sendMessage(message, callback);
    } catch (e) {
        return setImmediate(callback, e);
    }
};

/**
 * Upload or delete profile picture
 * @param {Object} params
 *  - userId - Profile ID
 *  - action - "update" used to upload profile picture (data parameter should contain binary content), "delete" used to delete profile picture
 *  - data - profile picture binary data string (used only with action = "update")
 *  - waitResponse - wait for response from media service or return callback immediately
 * @returns {Error}
 */
o.updateProfilePicture = function(params, callback) {
    try {
        var self = this;

        // if callback is not function just return error
        if (!_.isFunction(callback)) {
            logger.error('MediaServiceClient.updateProfilePicture() error: ', Errors.ERR_CALLBACK_NOT_PROVIDED);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED);
        }

        // if there are no parameters just return error
        if (!_.isObject(params)) {
            logger.error('MediaServiceClient.updateProfilePicture() error: ', Errors.ERR_VALIDATION_FAILED.message);
            return setImmediate(callback, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }

        // check arguments
        var content = {
            userId: params.userId,
            action: params.action,
            data: params.data
        };

        // if wait for response set seq
        var seq = null;
        if (params.waitResponse === true) {
            seq = self._getSeq();
        }

        var pm = new ProtocolMessage();
        pm.setMessage(Constants.SERVICE_NAME + '/mediaUpdateProfilePicture');
        pm.setContent(content);
        pm.setSeq(seq);

        return self._request(pm, callback);

   } catch (e) {
        logger.error('MediaServiceClient.updateProfilePicture() error: ' , e);
        return setImmediate(callback, e);
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
        logger.error('MediaServiceClient.disconnect() tc error:', e);
        throw e;
    }
};


module.exports = MediaServiceClient;
