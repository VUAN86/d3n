var _ = require('lodash');
var async = require('async');
var inherits = require('util').inherits;
var Errors = require('./config/errors.js');
var DefaultConfig = require('./config/config.js');
var DefaultWebsocketClient = require('../../../../index.js');
var ProtocolMessage = require('nodejs-protocol');
var EventEmitter = require('events').EventEmitter;
var logger = require('nodejs-logger')();

ClientService = function (config) {
    var self = this;
    self._config = _.assign(DefaultConfig.hosts(), config || {});
    self._wscInstances = {};
    self._seq = 1;
    EventEmitter.call(self);
};

inherits(ClientService, EventEmitter);

ClientService.prototype.register = function (owner, service, uri, callback) {
    var self = this;
    if (!owner || !service || !uri) {
        return callback(Errors.SrcApi.ValidationFailed);
    }
    return self._request('register', owner, { serviceNamespace: service, uri: uri }, null, callback);
}

ClientService.prototype.unregister = function (owner, callback) {
    var self = this;
    if (!owner) {
        return callback(Errors.SrcApi.ValidationFailed);
    }
    return self._request('unregister', owner, null, self._seq++, callback);
}

ClientService.prototype.get = function (owner, service, callback) {
    var self = this;
    if (!owner || !service) {
        return callback(Errors.SrcApi.ValidationFailed);
    }
    return self._request('get', owner, { serviceNamespace: service }, self._seq++, callback);
}

ClientService.prototype.list = function (owner, service, callback) {
    var self = this;
    if (!owner) {
        return callback(Errors.SrcApi.ValidationFailed);
    }
    var content = {};
    if (!_.isUndefined(service) && !_.isNull(service)) {
        content.serviceNamespace = service;
    }
    return self._request('list', owner, content, self._seq++, callback);
}

ClientService.prototype.disconnect = function (callback) {
    try {
        var self = this;
        _.each(self._wscInstances, function (instance) {
            instance.connection.disconnect(1001, '', false);
        });
        delete self._wscInstances;
        if (callback) {
            return setImmediate(function (callback) {
                return callback(false);
            }, callback);
        }
    } catch (ex) {
        if (callback) {
            return setImmediate(function (callback) {
                return callback(Errors.SrcApi.FatalError);
            }, callback);
        }
    }
}

ClientService.prototype._request = function (api, owner, content, seq, callback) {
    var self = this;
    var message = _message(api, content, seq);
    var response = undefined;
    self._connect(owner, function (err, connection) {
        if (err) {
            return callback(err);
        }
        // save instance config (needed for reconnect)
        if (api === 'register') {
            self._wscInstances[owner].config = content;
        }
        try {
            connection.sendMessage(message, function (err, responseMessage) {
                if (err) {
                    return callback(err);
                }
                return callback(null, responseMessage);
            });
        } catch (ex) {
            return callback(ex);
        }
    }, api);
}

ClientService.prototype._connect = function (owner, callback, api) {
    var self = this;
    if (_.has(self._wscInstances[owner], 'connection')) {
        if (self._wscInstances[owner].connection && self._wscInstances[owner].connection.connected()) {
            logger.debug('SRC', '<-' + owner + ' existing connection');
            return callback(null, self._wscInstances[owner].connection);
        }
        // reconnect
        logger.debug('SRC', '<-' + owner + ' reconnect');
    } else {
        self._wscInstances[owner] = {};
    }
    async.someSeries(self._config, function (host, next) {
        var wsClient = new DefaultWebsocketClient(host);
        try {
            wsClient.connect(function (err, res) {
                logger.debug('SRC', '->' + owner + ' connecting: ' + (err ? err : 'OK'));
                if (err) {
                    return next(null, false);
                }
                wsClient.on('message', function (message) {
                    self._handleMessage(owner, message);
                });
                wsClient.on('connectionClose', function (reasonCode, description) {
                    self._handleConnectionClose(owner, reasonCode, description);
                });
                self._wscInstances[owner].connection = wsClient;
                return next(null, true);
            });
        } catch (ex) {
            return next(null, false);
        }
    }, function (err, connected) {
        if (!connected) {
            return callback(Errors.SrcApi.ConnectionFailed);
        }
        return callback(null, self._wscInstances[owner].connection);
    });
}

ClientService.prototype._getConnection = function (owner) {
    var self = this;
    if (self._wscInstances[owner]) {
        return self._wscInstances[owner].connection;
    }
    return null;
}

ClientService.prototype._heartbeat = function (owner, content) {
    var self = this;
    if (!owner) {
        throw new Error(Errors.SrcApi.ValidationFailed);
    }
    var messageName = 'heartbeat';
    if (content) {
        messageName = 'heartbeatResponse';
    }
    if (self._wscInstances[owner] && self._wscInstances[owner].connection) {
        var resultMessage = _message(messageName, content, null);
        return self._wscInstances[owner].connection.sendMessage(resultMessage, function (err) { });
    } else {
        return self._request(messageName, owner, content, self._seq++, function (err) { });
    }
}

ClientService.prototype._handleMessage = function (owner, message) {
    var self = this;
    if (message.getMessageName() === 'heartbeat') {
        self._heartbeat(owner, { status: 'alive' });
    } else {
        try {
            self.emit('message', message);
        } catch (ex) {
            logger.error('Error on ServiceRegistryClient._handleHeartbeat', ex);
            return ex;
        }
    }
}

ClientService.prototype._handleConnectionClose = function (owner, reasonCode, description) {
    // skip if client disconnecting manually by external request
    if (reasonCode === 1001) {
        return;
    }
    var self = this;
    var instanceConfig = self._wscInstances[owner].config;
    if (!instanceConfig) {
        logger.error('Error on ServiceRegistryClient._handleConnectionClose');
        //throw new Error(Errors.SrcApi.ConnectionFailed);
    }
    self.register(owner, instanceConfig.serviceNamespace, instanceConfig.uri, function () { });
}

function _message(api, content, seq) {
    var pm = new ProtocolMessage();
    pm.setMessage('serviceRegistry/' + api);
    pm.setContent(content);
    if (api !== 'register') {
        pm.setSeq(seq);
    }
    pm.setAck(null);
    pm.setTimestamp(_.now());
    pm.setClientId(1);
    return pm;
}

module.exports = ClientService;