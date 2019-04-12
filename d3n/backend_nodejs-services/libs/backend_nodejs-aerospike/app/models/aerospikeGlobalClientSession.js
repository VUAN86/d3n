var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

var KEY_PREFIX = 'session:';
var BIN_NAME = 'sessionsMap';

function AerospikeGlobalClientSession(userId, clientSession) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self.userId = userId;
    self.clientSession = clientSession;
};
AerospikeGlobalClientSession._namespace = Config.keyvalue.namespace;
AerospikeGlobalClientSession._set = 'session';
AerospikeGlobalClientSession._ttl = 60*60*24*7; // 7 days

AerospikeGlobalClientSession.prototype.getUserSession = function (callback) {
    try {
        var self = this;
        var key = _key(self.userId);
        self.KeyvalueService.exec('get', {
            model: AerospikeGlobalClientSession,
            key: key
        }, function (err, value) {
            //console.log('>>>>>>here:', err, value);
            return callback(err, value);
        });
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.prototype.getUserSessionWithMetadata = function (callback) {
    try {
        var self = this;
        var key = _key(self.userId);
        self.KeyvalueService.exec('get', {
            model: AerospikeGlobalClientSession,
            key: key,
            includeMetadata: true
        }, function (err, value) {
            return callback(err, value);
        });
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.prototype.getUserClientSessions = function (callback) {
    try {
        var self = this;
        var key = _key(self.userId);
        self.KeyvalueService.exec('get', {
            model: AerospikeGlobalClientSession,
            key: key
        }, function (err, data) {
            try {
                if (err) {
                    if (err === Errors.DatabaseApi.NoRecordFound) {
                        return callback(false, []);
                    }
                    return callback(err);
                }
                
                var map = data[BIN_NAME];
                if(_.isEmpty(map)) {
                    return callback(false, []);
                }
                
                var sessions = [];
                for(var clientId in map) {
                    var item = map[clientId];
                    sessions.push(item);
                }

                return callback(false, sessions);
            } catch (e) {
                return callback(e);
            }
        });
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.prototype.updateUserSession = function (clientSessions, callback) {
    try {
        var self = this;
        var key = _key(self.userId);
        var value = {};
        value[BIN_NAME] = clientSessions;
        self.KeyvalueService.exec('put', {
            model: AerospikeGlobalClientSession,
            key: key,
            value: value
        }, callback);
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.prototype.removeUserSession = function (callback) {
    try {
        var self = this;
        var key = _key(self.userId);
        self.KeyvalueService.exec('remove', {
            model: AerospikeGlobalClientSession,
            key: key
        }, function (err) {
            if (err) {
                if (err === Errors.DatabaseApi.NoRecordFound) {
                    return callback();
                }
                return callback(err);
            }
            return callback();
        });
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.prototype.remove = function (callback) {
    return this.removeUserSession(callback);
};

AerospikeGlobalClientSession.prototype.removeClientSession = function (callback) {
    try {
        
        var self = this;
        var key = _key(self.userId);
        
        self.KeyvalueService.exec('map.removeByKey', {
            model: AerospikeGlobalClientSession,
            key: key,
            mapBin: BIN_NAME,
            mapKey: self.clientSession.clientId
        }, function (err, result) {
            if (err) {
                if (err === Errors.DatabaseApi.NoRecordFound) {
                    return callback(Errors.DatabaseApi.NoRecordFound);
                }
                logger.error('AerospikeGlobalClientSession.removeClientSession() error remove client session', self.clientSession.clientId, err);
                return callback(err);
            }
            
            if ( _.isEmpty(result) || result[BIN_NAME] === 0) {
                return callback(Errors.DatabaseApi.NoRecordFound);
            }
            
            return callback();
        });

    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.prototype.getClientSession = function (callback) {
    try {
        
        var self = this;
        var key = _key(self.userId);
        var session;
        
        self.KeyvalueService.exec('map.getByKey', {
            model: AerospikeGlobalClientSession,
            key: key,
            mapBin: BIN_NAME,
            mapKey: self.clientSession.clientId
        }, function (err, result) {
            if (err) {
                if (err === Errors.DatabaseApi.NoRecordFound) {
                    return callback(Errors.DatabaseApi.NoRecordFound);
                }
                logger.error('AerospikeGlobalClientSession.getClientSession() error retrieving client session for client id', self.clientSession.clientId, err);
                return callback(err);
            }
            
            if (_.isEmpty(result) || _.isEmpty(result[BIN_NAME])) {
                return callback(Errors.DatabaseApi.NoRecordFound);
            }
            session = result[BIN_NAME][1];
            return callback(err, session);
        });
        
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.prototype.getTenantId = function (callback) {
    try {
        var self = this;
        self.getClientSession(function (err, data) {
            try {
                if (err) {
                    if (err === Errors.DatabaseApi.NoRecordFound) {
                        return callback(err);
                    }
                    logger.error('AerospikeGlobalClientSession.getTenantId() error retrieving client session:', err);
                    return callback(err);
                }
                
                return callback(false, data.appConfig.tenantId);
            } catch (e) {
                logger.error('AerospikeGlobalClientSession.getTenantId() error handling getClientSession result:', e);
                return callback(e);
            }
        });
    } catch (e) {
        logger.error('AerospikeGlobalClientSession.getTenantId() error handling getTenantId:', e);
        return setImmediate(callback, e);
    }
};


AerospikeGlobalClientSession.prototype.addOrUpdateClientSession = function (callback) {
    try {
        
        var self = this;
        var key = _key(self.userId);
        var mapValue = _.assign({}, self.clientSession);
        
        self.KeyvalueService.exec('map.put', {
            model: AerospikeGlobalClientSession,
            key: key,
            mapBin: BIN_NAME,
            mapKey: self.clientSession.clientId,
            mapValue: mapValue
        }, function (err) {
            if (err) {
                logger.error('AerospikeGlobalClientSession.addOrUpdateClientSession() error saving client session:', err);
            }
            return callback(err);
        });
        
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeGlobalClientSession.addOrUpdateClientSession = function(userId, clientSession, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, clientSession);
    return clientSessionInstance.addOrUpdateClientSession(callback);
};

AerospikeGlobalClientSession.removeClientSession = function(userId, clientSession, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, clientSession);
    return clientSessionInstance.removeClientSession(callback);
};

AerospikeGlobalClientSession.getClientSession = function(userId, clientId, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, {'clientId': clientId });
    return clientSessionInstance.getClientSession(callback);
};

AerospikeGlobalClientSession.getUserClientSessions = function(userId, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, []);
    return clientSessionInstance.getUserClientSessions(callback);
};
AerospikeGlobalClientSession.removeUserSession = function(userId, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, []);
    return clientSessionInstance.removeUserSession(callback);
};
AerospikeGlobalClientSession.getUserSession = function(userId, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, []);
    return clientSessionInstance.getUserSession(callback);
};
AerospikeGlobalClientSession.getUserSessionWithMetadata = function(userId, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, []);
    return clientSessionInstance.getUserSessionWithMetadata(callback);
};

AerospikeGlobalClientSession.getTenantId = function(userId, clientId, callback) {
    var clientSessionInstance = new AerospikeGlobalClientSession(userId, {'clientId': clientId });
    return clientSessionInstance.getTenantId(callback);
};

function _key(userId) {
    return KEY_PREFIX + userId;
}

module.exports = AerospikeGlobalClientSession;
