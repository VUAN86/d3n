var _ = require('lodash'),
    ServiceState = require('./ServiceState.js')    
;

function ClientSession(id) {
    
    this._id = id;
    
    this._isAnonymous = true;
    
    // token
    this.token = null;
    
    // token payload 
    this.tokenPayload = null;
    
    // token sent by auth service
    this.authToken = null;
    
    // token payload of auth token
    this.authTokenPayload = null;
    
    this.dbTokenChecked = false;
    
    this.servicesState = [];
    
    this.globalSessionCreated = false;
    
    this.aerospikeClientSession = undefined;
    
    this.clientInfo = {
        profile: null,
        appConfig: null,
        ip: null
    };
    
    this.needRolesUpdate = false;
    
    //this.messageTokenPayload = null;
    
};
var o = ClientSession.prototype;

o.getId  =function () {
    return this._id;
};


o.setIsAnonymous = function (flag) {
    this._isAnonymous = flag;
};

o.getIsAnonymous = function () {
    return this._isAnonymous;
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


o.setTokenPayload = function (tokenPayload) {
    try {
        this.tokenPayload = tokenPayload;
    } catch (e) {
        return e;
    }
};

o.getTokenPayload = function () {
    try {
        return this.tokenPayload;
    } catch (e) {
        return e;
    }
};



/**
 * Set the authToken
 * @param {type} token
 * @returns {e}
 */
o.setAuthToken = function (token) {
    try {
        this.authToken = token;
    } catch (e) {
        return e;
    }
};

/**
 * Return authToken
 * @returns {e|type}
 */
o.getAuthToken = function () {
    try {
        return this.authToken;
    } catch (e) {
        return e;
    }
};

o.setAuthTokenPayload = function (authTokenPayload) {
    try {
        this.authTokenPayload = authTokenPayload;
    } catch (e) {
        return e;
    }
};

o.getAuthTokenPayload = function () {
    try {
        return this.authTokenPayload;
    } catch (e) {
        return e;
    }
};



/**
 * Get the servicesState
 * @returns {nm$_ClientSession.o.serviceState|e}
 */
o.getServicesState = function () {
    try {
        return this.servicesState;
    } catch (e) {
        return e;
    }
};

/**
 * Get the service state by service name. If service state not exist create it.
 * @param {type} serviceName
 * @returns {e|nm$_ClientSession.o.servicesState}
 */
o.getServiceState = function (serviceName) {
    try {
        for(var i=0; i<this.servicesState.length; i++) {
            if (this.servicesState[i].getServiceName() === serviceName) {
                return this.servicesState[i];
            }
        }
        
        this.createServiceState(serviceName);
        
        return this.servicesState[this.servicesState.length-1];
    } catch (e) {
        return e;
    }
};

/**
 * Create a new service state and add into the array of existing services state
 * @param {type} serviceName
 * @returns {Error|e}
 */
o.createServiceState = function (serviceName) {
    try {
        this.servicesState.push(new ServiceState(serviceName));
    } catch (e) {
        return e;
    }
};


/**
 * 
 * @param {type} serviceName
 * @returns {undefined}
 */
o.needAuthentication = function (serviceName) {
    
};


module.exports = ClientSession;