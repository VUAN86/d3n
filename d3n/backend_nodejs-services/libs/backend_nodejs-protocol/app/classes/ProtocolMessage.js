var fs = require('fs');
var jjv = require('jjv')();
var _ = require('lodash');
var protocolSchemas = require('nodejs-protocol-schemas');
var schemas = protocolSchemas.schemas;
var Errors = require('nodejs-errors');
var SecurityModule = require('./Security.js');
var Security = new SecurityModule();
var logger = require('nodejs-logger')();

/**
 * Loads API Endpoints schemas(for both request and response message) into the json schema validation library
 * @param schemasSet
 */
var loadApiSchemas = function(schemasSet) {
    var apiEndpoints = schemasSet['paths'];
    for (var apiEndpoint in apiEndpoints) {
        if (!apiEndpoints.hasOwnProperty(apiEndpoint)) {
            continue;
        }

        var schemaKey = apiEndpoint[0] === '/' ? apiEndpoint.substring(1) : apiEndpoint;
        if (_.has(apiEndpoints[apiEndpoint], 'put.parameters') &&
            _.isArray(apiEndpoints[apiEndpoint].put.parameters) &&
            !_.isUndefined(apiEndpoints[apiEndpoint].put.parameters[0].schema) )
        {

            jjv.addSchema(schemaKey, apiEndpoints[apiEndpoint].put.parameters[0].schema);
        }

        if (_.has(apiEndpoints[apiEndpoint], 'put.responses.200.schema')) {
            jjv.addSchema(schemaKey + 'Response', apiEndpoints[apiEndpoint].put.responses['200'].schema);
        }

    }
};

// load first the base message schema: websocketMessage
jjv.addSchema('websocketMessage', schemas['default'].definitions.websocketMessage);


//Loading Schemas into JJV Json Schema Validator
for (var schema in schemas) {
    if (!schemas.hasOwnProperty(schema)) {
        continue;
    }

    loadApiSchemas(schemas[schema]);
}


/**
 * @typedef {object} ProtocolMessage.MessageContainer
 * @property {string|null} message
 * @property {object|null} content
 * @property {string} token=
 * @property {int} timestamp=
 * @property {int|null} seq
 * @property {int[]|null} ack
 * @property {{type:ProtocolMessage.ErrorType, message:string}|null} error
 * @property {string} clientId=
 */
/**
 * @typedef {object} ProtocolMessage
 * @property {ProtocolMessage.MessageContainer} messageContainer
 * @property {object} lastValidationError
 */

/**
 * Constructor.
 * - If message is instance of ProtocolMessage it is interpreted like a request message.
 *   It creates a response message out of the provided request message
 * - If message is plain object then the components are copied from the plain object
 * - If message is not provided then the required message components contain null values
 * @constructor
 * @param {ProtocolMessage=} message
 * @returns {ProtocolMessage}
 */
function ProtocolMessage(message) {
    /**
     * @type {ProtocolMessage.MessageContainer}
     */
    this.messageContainer = {};
    this.messageContainer.message = null;
    this.messageContainer.content = null;
    this.messageContainer.seq = null;
    this.messageContainer.ack = null;
    this.messageContainer.error = null;

    /**
     * @type {{validation: Object.<string, Object> , schema: "unknown"}|null}
     */
    this.lastValidationError = null;

    if( message && _.isFunction(message.getMessageContainer) ) {
        this.messageContainer = _.cloneDeep(message.getMessageContainer());

        this.setMessage(message.getMessage() + 'Response');
        this.setSeq(null);
        this.setAck(message.getSeq() ? [message.getSeq()] : []);
        this.deleteToken();
        this.deleteClientInfo();
    } else if ( _.isPlainObject(message) ) {
        this.messageContainer = _.cloneDeep(message);
    }
}

/**
 * Enum for different error types.
 * @readonly
 * @enum {string}
 */
ProtocolMessage.ErrorType = {
    SERVER: "server",
    CLIENT: "client",
    AUTH: "auth",
    VALIDATION: "validation"
};

var o = ProtocolMessage.prototype;


/**
 * Validates message content against a schema
 * @param {object} schema - Protocol Message schema, describing the message
 * @returns {boolean} - true or list of validation errors
 */
o.isValid = function (schema) {
    try {
        var res = jjv.validate(schema, this.getMessageContainer());
        this.lastValidationError = res;
        if(res === null) {
            return true;
        }
    } catch (Error) {
        logger.error('_ValidationRequestMessageSchema: Error="', Error,'"');
        this.lastValidationError = { schema: "unknown" };
        return false;
    }
    return false;
};

/**
 * Returns last validation error or null
 * @returns {{validation: Object.<string, Object> , schema: "unknown"}|null}
 */
o.getLastError = function() {
    return this.lastValidationError;
};

/**
 * Returns the payload of the message
 * @returns {ProtocolMessage.MessageContainer}
 */
o.getMessageContainer = function () {
    return this.messageContainer;
};

/**
 * Returns name and namespace of the message
 * @returns {string|null}
 */
o.getMessage = function () {
    return this.messageContainer.message;
};

/**
 * Extracts namespace of the message.
 * Empty string if non existing
 * @returns {string}
 */
o.getMessageNamespace = function () {
    var message = this.getMessage();
    if (message === null) {
        return "";
    }
    var parts = this.messageContainer.message.split('/');
    if (parts.length > 1) {
        return parts[0];
    }
    return "";
};

/**
 * Exctacts message name (without namespace)
 * @returns {string}
 */
o.getMessageName = function () {
    var message = this.getMessage();
    if (message === null) {
        return "";
    }
    var parts = this.messageContainer.message.split('/');
    if (parts.length > 1) {
        return parts.slice(1).join("/");
    }
    return parts[0];
};

/**
 * Returns content of the message
 * @returns {Object|null}
 */
o.getContent = function () {
    return this.messageContainer.content;
};

/**
 * Returns token of the message or undefined if not present
 * @returns {string|undefined}
 */
o.getToken = function () {
    return this.messageContainer.token;
};

/**
 * Returns the timestamp of the message or undefined if not present
 * @returns {int|undefined}
 */
o.getTimestamp = function () {
    return this.messageContainer.timestamp;
};

/**
 * Returns the sequence number of the message or null if not present
 * @returns {int|null}
 */
o.getSeq = function () {
    return this.messageContainer.seq;
};

/**
 * Returns acknowledgment numbers of the message or null if not present
 * @returns {int[]|null}
 */
o.getAck = function () {
    return this.messageContainer.ack;
};

/**
 * Returns the messages error object or null if not present
 * @returns {{type:ProtocolMessage.ErrorType, message:string}|null}
 */
o.getError = function () {
    return this.messageContainer.error;
};

/**
 * Returns clientId of client in the message or null if not present
 * @returns {string|undefined}
 */
o.getClientId = function () {
    return this.messageContainer.clientId;
};

/**
 * Gets client info
 * @returns {Object}
 */
o.getClientInfo = function () {
    return this.messageContainer.clientInfo;
};

/**
 * Sets complete message container of message
 * @param {ProtocolMessage.MessageContainer} messageContainer
 */
o.setMessageContainer = function (messageContainer) {
    this.messageContainer = messageContainer;
};

/**
 * Sets message string of message, including namespace
 * @param {string} message
 */
o.setMessage = function (message) {
    this.messageContainer.message = message;
};

/**
 * Sets/unsets content object
 * @param {object|null} content
 */
o.setContent = function (content) {
    this.messageContainer.content = content;
};

/**
 * @param {string} token
 */
o.setToken = function (token) {
    this.messageContainer.token = token;
};

/**
 * @param {int} timestamp
 */
o.setTimestamp = function (timestamp) {
    this.messageContainer.timestamp = timestamp;
};

/**
 * Sets/unsets sequence number
 * @param {int|null} seq
 */
o.setSeq = function (seq) {
    this.messageContainer.seq = seq;
};

/**
 * Sets/unsets acknowledged numbers
 * @param {int[]|null} ack
 */
o.setAck = function (ack) {
    this.messageContainer.ack = ack;
};

/**
 * Sets/unsets error object
 * As of protocol version 0.5 all errors need to have a type, the code will be omitted, in order to make the transition
 * smooth, we still support the old style as well as the intermediate solution to just have a string.
 * In these cases ProtocolMessage.ErrorType.SERVER is used as the type.
 * @param {{type:ProtocolMessage.ErrorType, message:string}|string|{code:int, message:string}|null} error
 */
o.setError = function (error) {
    if (!error) {
        this.messageContainer.error = null;
    } else if (typeof error === "string") {
        if (!_.isUndefined(Errors[error])) {
            this.messageContainer.error = Errors[error];
        } else {
            this.messageContainer.error = {
                type: ProtocolMessage.ErrorType.SERVER,
                message: error
            };
        }

    } else if (_.has(error, 'code')) {
        this.messageContainer.error = {
            type: ProtocolMessage.ErrorType.SERVER,
            message: error.message
        };
    } else {
        this.messageContainer.error = error;
    }
};

/**
 * Sets client id
 * @param {string} clientId
 */
o.setClientId = function (clientId) {
    this.messageContainer.clientId = clientId;
};


/**
 * Deletes client id
 */
o.deleteClientId = function () {
    delete this.messageContainer.clientId;
};

/**
 * Sets client info
 * @param {Object} clientInfo
 */
o.setClientInfo = function (clientInfo) {
    this.messageContainer.clientInfo = clientInfo;
};


/**
 * Deletes token
 */
o.deleteToken = function () {
    delete this.messageContainer.token;
};

/**
 * Deletes clientInfo
 */
o.deleteClientInfo = function () {
    delete this.messageContainer.clientInfo;
};

/**
 * Deletes timestamp
 */
o.deleteTimestamp = function () {
    delete this.messageContainer.timestamp;
};

/**
 * Check if this message can be processed, security checks out
 *
 * @param  {array} userRoles  Array of roles the user has
 * @param  {integer} tenantId Id of the tenant
 * @return {boolean}          Returns true if message is allowed to be processed
 */
o.validateSecurity = function (userRoles, tenantId) {
    return Security.roleCanAccessApi(userRoles, tenantId, '/' + this.messageContainer.message);
};

o.isPrivate = function () {
    return (Security.getPrivateApis().indexOf('/' + this.getMessage()) >= 0);
};

/**
 * Check if message is instance of ProtocolMessage (temporary fix)
 * @param {type} message
 * @returns {boolean}
 */
ProtocolMessage.isInstance = function (message) {
    return (message && _.isFunction(message.getMessageContainer));
};

module.exports = ProtocolMessage;
