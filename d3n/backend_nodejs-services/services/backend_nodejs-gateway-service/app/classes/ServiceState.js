var _ = require('lodash');

function ServiceState(serviceName) {
    this.token = null;
    this.serviceName = serviceName;
    this.authenticated = false;
    this.messageQueue = null;
};

var o = ServiceState.prototype;

/**
 * Get the service name
 * @returns {nm$_ServiceState.o.serviceName|e}
 */
o.getServiceName = function () {
    try {
        return this.serviceName;
    } catch (e) {
        return e;
    }
};


/**
 * Set the token
 * @param {type} token
 * @returns {e}
 */
o.setToken = function (token) {
    try {
        this.token = token;
    } catch (e) {
        return e;
    }
};

/**
 * Return the  token
 * @returns {nm$_ClientSession.o.token|e}
 */
o.getToken = function () {
    try {
        return this.token;
    } catch (e) {
        return e;
    }
};


/**
 * Return authentication status: true or false
 * @returns {Boolean|e}
 */
o.isAuthenticated = function () {
    try {
        return this.authenticated === true ? true : false;
    } catch (e) {
        return e;
    }
};

/**
 * Set that the client is authenticated on the service
 * @returns {e}
 */
o.setIsAuthenticated = function () {
    try {
        this.authenticated = true;
    } catch (e) {
        return e;
    }
};

/**
 * Set that the client is not authenticated on the service
 * @returns {e}
 */
o.setIsNotAuthenticated = function () {
    try {
        this.authenticated = false;
    } catch (e) {
        return e;
    }
};


/**
 * Get message queue
 * @returns {Array|e}
 */
o.getMessageQueue = function () {
    try {
        return this.messageQueue;
    } catch (e) {
        return e;
    }
};

/**
 * Check if there is a queue.
 * @returns {e}
 */
o.hasQueue = function () {
    try {
        return _.isArray(this.messageQueue);
    } catch (e) {
        return e;
    }
};

/**
 * Create message queue.
 * @returns {Error|e}
 */
o.createMessageQueue = function () {
    try {
        if ( this.hasQueue() ) {
            return new Error('ERR_QUEUE_ALREADY_EXISTS');
        }
        
        this.messageQueue = [];
    } catch (e) {
        return e;
    }
};

/**
 * Delete message queue
 * @returns {e}
 */
o.deleteMessageQueue = function () {
    try {
        this.messageQueue = null;
    } catch (e) {
        return e;
    }
};

/**
 * Add a message in queue. If queue not exist return error.
 * @param {type} message
 * @returns {e|Error}
 */
o.addMessageInQueue = function (message) {
    try {
        if ( !this.hasQueue() ) {
            return new Error('ERR_QUEUE_NOT_EXISTS');
        }
        
        this.messageQueue.push(message);
    } catch (e) {
        return e;
    }
};


module.exports = ServiceState;