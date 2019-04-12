var logger = require('nodejs-logger')();

function ClientInfo(clientInfo) {
    if(clientInfo) {
        this._clientInfo = clientInfo; 
    } else {
        this._clientInfo = {
            appConfig: {},
            profile: {},
            ip: null
        };
    }
    
}
var o = ClientInfo.prototype;

/**
 * Returns client info.
 */
o.getClientInfo = function () {
    return this._clientInfo;
};

o.getRoles = function () {
    try {
        return this._clientInfo.profile.roles;
    } catch (e) {
        logger.error('ClientInfo.getRoles() error:', e);
        return [];
    }
};

o.setIp = function (ip) {
    this._clientInfo.ip = ip;
};


o.getIp = function () {
    return this._clientInfo.ip;
};

o.setTenantId = function (tenantId) {
    this._clientInfo.appConfig.tenantId = tenantId;
};

o.getTenantId = function () {
    try {
        return this._clientInfo.appConfig.tenantId;
    } catch (e) {
        logger.error('ClientInfo.getTenantId() error:', e);
        return null;
    }
};

o.setAppId = function (appId) {
    this._clientInfo.appConfig.appId = appId;
};

o.getAppId = function () {
    try {
        return this._clientInfo.appConfig.appId;
    } catch (e) {
        logger.error('ClientInfo.getAppId() error:', e);
        return null;
    }
};



o.setUserId = function (userId) {
    this._clientInfo.profile.userId = userId;
};

o.getUserId = function () {
    try {
        return this._clientInfo.profile.userId;
    } catch (e) {
        logger.error('ClientInfo.getUserId() error:', e);
        return null;
    }    
};

module.exports = ClientInfo;