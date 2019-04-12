//var EventEmitter = require('events').EventEmitter;
//var inherits = require('util').inherits;
var Errors = require('../config/errors.js');
var Constants = require('../config/constants.js');
var _ = require('lodash');
var logger = require('nodejs-logger')();
var DefaultServiceClient = require('nodejs-default-client');
var RegistryServiceClient = require('nodejs-service-registry-client');
var EventClient = require('nodejs-event-service-client');
var ProtocolMessage = require('nodejs-protocol');

/**
 * @typedef {object} ConstructorConfig
 * @property {string} registryServiceURIs - Commaseperated list of uris for service registry
 */

/**
 * Workflow service client constructor.
 *
 * @type 
 * @param {ConstructorConfig} config
 * @constructor
 */
function WorkflowClient (config) {
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
    
    this._eventClient = new EventClient(this._config.registryServiceURIs);
    
};


var o = WorkflowClient.prototype;

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
                    logger.error('WorkflowClient._request() error on request call:', err, JSON.stringify(message), JSON.stringify(response));
                    return cb(err);
                }

                if (ProtocolMessage.isInstance(response) && !_.isNull(response.getError())) {
                    logger.error('WorkflowClient._request() error on response message:', response.getError(), JSON.stringify(message), JSON.stringify(response));
                    return cb(new Error(response.getError().message), response);
                }

                return cb(err, response);
            } catch (e) {
                logger.error('WorkflowClient._request() error on handling response:', e, JSON.stringify(message), JSON.stringify(response));
                return cb(e);
            }
        });
    } catch (e) {
        logger.error('WorkflowClient._request() error calling:', e, JSON.stringify(message));
        return setImmediate(cb, e);
    }
};

/**
 * Start task.
 * @param {String} taskId
 * @param {String} taskType
 * @param {String} userId
 * @param {String} tenantId
 * @param {String} description
 * @param {Object} parameters {<<key>>:<<value>>}
 * @param {Function} cb
 */
o.start = function(taskId, taskType, userId, tenantId, description, parameters, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('WorkflowClient.start() error:', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }
        
        // mandatory fields
        var content = {
            taskId: taskId,
            taskType: taskType,
            userId: userId,
            tenantId: tenantId
        };
        if (description) {
            content.description = description;
        }
        if (parameters) {
            content.parameters = parameters;
        }
        
        
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/start';
        pm.setMessage(api);
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        if (pm.isValid(api) !== true) {
            logger.error('WorkflowClient.start() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        
        self._request(pm, function (err, message) {
            if (err) {
                return cb(err, message);
            }
            return cb(err, message);
        });
        
   } catch (e) {
        logger.error('WorkflowClient.start() error when calling:', e);
        return setImmediate(cb, e);
    }
};

/**
 * Perform task action.
 * @param {String} taskId
 * @param {String} actionType
 * @param {String} userId
 * @param {String} tenantId
 * @param {Object} parameters {<<key>>:<<value>>}
 * @param {Function} cb
 */
o.perform = function(taskId, actionType, userId, tenantId, parameters, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('WorkflowClient.perform() error:', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }
        
        // mandatory fields
        var content = {
            taskId: taskId,
            actionType: actionType
        };
        
        if (userId) {
            content.userId = userId;
        }
        
        if (tenantId) {
            content.tenantId = tenantId;
        }
        
        if (parameters) {
            content.parameters = parameters;
        }
        
        
        
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/perform';
        pm.setMessage(api);
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        if (pm.isValid(api) !== true) {
            logger.error('WorkflowClient.perform() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        self._request(pm, function (err, message) {
            if (err) {
                return cb(err, message);
            }
            return cb(err, message);
        });
        
   } catch (e) {
        logger.error('WorkflowClient.perform() error when calling:', e);
        return setImmediate(cb, e);
    }
};

/**
 * Get task state.
 * @param {String} taskId
 * @param {Function} cb
 */
o.state = function(taskId, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('WorkflowClient.state() error:', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }
        
        // mandatory fields
        var content = {
            taskId: taskId
        };
        
        
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/state';
        pm.setMessage(api);
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        if (pm.isValid(api) !== true) {
            logger.error('WorkflowClient.state() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        return self._request(pm, cb);
        
   } catch (e) {
        logger.error('WorkflowClient.state() error when calling:', e);
        return setImmediate(cb, e);
    }
};

/**
 * Abort task.
 * @param {String} taskId
 * @param {Function} cb
 */
o.abort = function(taskId, cb) {
    try {
        var self = this;
        
        // if callback is not function just return error
        if (!_.isFunction(cb)) {
            logger.error('WorkflowClient.abort() error:', Errors.ERR_CALLBACK_NOT_PROVIDED.message);
            return new Error(Errors.ERR_CALLBACK_NOT_PROVIDED.message);
        }
        
        // mandatory fields
        var content = {
            taskId: taskId
        };
        
        
        var pm = new ProtocolMessage();
        var api = Constants.SERVICE_NAME + '/abort';
        pm.setMessage(api);
        pm.setContent(content);
        pm.setSeq(self._getSeq());
        
        if (pm.isValid(api) !== true) {
            logger.error('WorkflowClient.abort() validation error:', JSON.stringify(pm.lastValidationError), pm.getMessageContainer());
            return setImmediate(cb, new Error(Errors.ERR_VALIDATION_FAILED.message));
        }
        
        self._request(pm, function (err, message) {
            if (err) {
                return cb(err, message);
            }
            return cb(err, message);
        });
        
   } catch (e) {
        logger.error('WorkflowClient.abort() error when calling:', e);
        return setImmediate(cb, e);
    }
};

/**
 * Set a handler for the event sent by workflow when a task state changes.
 * @param {Function} handler
 * @param {Function} cb
 */
o.onStateChange = function (handler, cb) {
    try {
        var self = this;
        var topic = Constants.WORKFLOW_STATE_CHANGE;
        self._eventClient.subscribe(topic, function (err) {
            try {
                if (err) {
                    return cb(err);
                }
                
                function handlerWrapper(eventContent) {
                    try {
                        handler(eventContent);
                    } catch (e) {
                        logger.error('WorkflowClient.onStateChange() error on calling event handler:', e);
                    }
                };
                
                self._eventClient.on('event:' + topic, handlerWrapper);

                return cb();
            } catch (e) {
                logger.error('WorkflowClient.onStateChange() error on handling subscribe response:', e);
                return cb(e);
            }
        });
    } catch (e) {
        logger.error('WorkflowClient.onStateChange() error:', e);
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
        this._eventClient.disconnect(_code, _reason, true);
    } catch (e) {
        logger.error('WorkflowClient.disconnect() tc error:', e);
        throw e;
    }
};


module.exports = WorkflowClient;