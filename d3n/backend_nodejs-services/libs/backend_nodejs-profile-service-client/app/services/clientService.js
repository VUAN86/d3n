var _ = require('lodash');
var url = require('url');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var DefaultConfig = require('./../config/config.js');
var DefaultWebsocketClient = require('nodejs-default-client');
var ServiceRegistryClient = require('nodejs-service-registry-client');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

/**
 * Constructor of Profile Service Client instance
 * @param {string} registryServiceURIs Service Registry URIs
 * @param {string} profileServiceName Profile Service name
 */
ClientService = function (registryServiceURIs, profileServiceName, ownerServiceName) {
    if (!registryServiceURIs) {
        registryServiceURIs = Config.registryServiceURIs;
    }
    if (!profileServiceName) {
        profileServiceName = Config.profileServiceName;
    }
    
    // profile service client
    this._profileServiceName = profileServiceName || 'profile';
    
    this._defaultClient = new DefaultWebsocketClient({
        serviceNamespace: this._profileServiceName,
        reconnectForever: false,
        autoConnect: true,
        serviceRegistryClient: new ServiceRegistryClient({registryServiceURIs: registryServiceURIs}),
        headers: ownerServiceName ? {service_name: ownerServiceName} : null
    });
    
    this._seq = 1;
};

/**
 * Calls Profile Service 'createProfile' API method
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.createProfile = function (params, token, callback) {
    var self = this;
	var content = {};
    var clientInfo = null;
    if (_.has(params, 'email')) {
		content["email"] = params.email;
	}
    if (_.has(params, 'phone')) {
		content["phone"] = params.phone;
	}
    if (_.has(params, 'facebook')) {
		content["facebook"] = params.facebook;
	}
    if (_.has(params, 'google')) {
		content["google"] = params.google;
	}
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('createProfile', content, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'updateProfile' API method for registered user from token
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.updateProfile = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'profile')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('updateProfile', { profile: params.profile }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'updateProfile' API method for specified user (other that comes in token)
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.updateUserProfile = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!(_.has(params, 'userId') || _.has(params, 'profile'))) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (!_.has(params, 'profile')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('updateProfile', {
        userId: params.userId,
        profile: params.profile
    }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'getProfile' API method for registered user from token
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.getProfile = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('getProfile', params, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'getProfile' API method for specified user (other that comes in token)
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.getUserProfile = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'userId')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('getProfile', { userId: params.userId }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'deleteProfile' API method
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.deleteProfile = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'userId')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('deleteProfile', { userId: params.userId }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'mergeProfile' API method
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.mergeProfile = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'sourceUserId') || !_.has(params, 'targetUserId') ||
        params.sourceUserId === params.targetUserId) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('mergeProfile', {
        source: params.sourceUserId,
        target: params.targetUserId
    }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'findByIdentifier' API method
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.findByIdentifier = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'identifierType') || !_.has(params, 'identifier') ||
        !_.some(['PHONE', 'EMAIL', 'FACEBOOK', 'GOOGLE'], function (identifierType) { return identifierType == params.identifierType; })) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('findByIdentifier', {
        identifierType: params.identifierType,
        identifier: params.identifier
    }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'getAppConfiguration' API method
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.getAppConfiguration = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'appId') || !_.has(params, 'deviceUUID') || !_.has(params, 'device')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('getAppConfiguration', params, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'updateProfileBlob' API method
 * @param {Object} params Required parameters of API method for authorized user
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.updateProfileBlob = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'name') || !_.has(params, 'value')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('updateProfileBlob', {
        name: params.name,
        value: params.value
    }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'updateProfileBlob' API method intended to use for internal services
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.updateUserProfileBlob = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'userId') || !_.has(params, 'name') || !_.has(params, 'value')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('updateProfileBlob', {
        userId: params.userId,
        name: params.name,
        value: params.value
    }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'getProfileBlob' API method for authorized user
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.getProfileBlob = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'name')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('getProfileBlob', {
        name: params.name
    }, clientInfo, token, callback);
}

/**
 * Calls Profile Service 'getProfileBlob' API method intended to use for internal services
 * @param {Object} params Required parameters of API method
 * @param {string} token Token of registered user
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.getUserProfileBlob = function (params, token, callback) {
    var self = this;
    var clientInfo = null;
    if (!_.has(params, 'userId') || !_.has(params, 'name')) {
        return callback(new Error(Errors.PscApi.ValidationFailed));
    }
    if (_.has(params, 'clientInfo')) {
        clientInfo = params.clientInfo;
    }
    return self._request('getProfileBlob', {
        userId: params.userId,
        name: params.name
    }, clientInfo, token, callback);
}

/**
 * Disconnects from Profile Service and Service Registry
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype.disconnect = function (callback) {
    try {
        var self = this;
        if (self._pscInstance) {
            self._pscInstance.disconnect();
            delete self._pscInstance;
        }
        if (self._srcInstance) {
            self._srcInstance.disconnect();
            delete self._srcInstance;
        }
        
        self._defaultClient.disconnect(1000, 'client_close', true);
        
        if(callback) {
            return setImmediate(callback, false);    
        }
    } catch (ex) {
        if(callback) {
            return setImmediate(callback, ex);    
        } else {
            throw ex;
        }
    }
}

/**
 * Generate ProtocolMessage from given content and send this message
 * to given Profile Service API method and return response message
 * @param {string} api Profile Service API method
 * @param {Object} content Outgoing message content object (optional)
 * @param {string} token Message Token
 * @param {callback} callback
 * @returns {unresolved}
 */
ClientService.prototype._request = function (api, content, clientInfo, token, callback) {
    var self = this;
    var message = self._message(api, content, clientInfo, token);
    
    self._defaultClient.sendMessage(message, callback);
}

ClientService.prototype._message = function (api, content, clientInfo, token) {
    var self = this;
    var pm = new ProtocolMessage();
    pm.setMessage(self._profileServiceName + '/' + api);
    pm.setContent(content);
    pm.setToken(token);
    pm.setSeq(self._seq++);
    if (clientInfo) {
        pm.setClientInfo(clientInfo);
    }
    return pm;
};

module.exports = ClientService;