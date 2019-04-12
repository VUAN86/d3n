var _ = require('lodash');
var cp = require('child_process');
var path = require('path');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DefaultWebsocketClient = require('nodejs-default-client');
var logger = require('nodejs-logger')();

var WsMock = function (serie) {
    this.serie = serie;
    this.clientInstance = null;
    this.gatewayInstance = null;
};

WsMock.prototype.connect = function (callback) {
    var self = this;
    try {
        if (self.serie === 'gateway') {
            logger.debug('Testing through gateway');
            // Connect through gateway
            self.gatewayInstance = cp.fork(path.join(__dirname, 'worker-gateway.js'));
            self.gatewayInstance.on('message', function (message) {
                if (_.has(message, 'built') && message.built) {
                    self.clientInstance = new DefaultWebsocketClient({
                        secure: Config.gateway.secure,
                        service: {
                            ip: Config.gateway.ip,
                            port: Config.gateway.port
                        }
                    });
                    self.clientInstance.connect(function (err, res) {
                        if (err) {
                            logger.debug('Connection to gateway failed', err);
                            return callback(err);
                        }
                        logger.debug('Connected to gateway');
                        return callback();
                    });
                } else {
                    logger.debug('Connection to gateway failed', message.err);
                    return callback(Errors.QuestionApi.FatalError);
                }
            });
        } else {
            logger.debug('Testing through direct connection');
            // Direct connect
            self.clientInstance = new DefaultWebsocketClient({
                secure: Config.secure,
                service: {
                    ip: Config.ip,
                    port: Config.port
                }
            });
            self.clientInstance.connect(function (err, res) {
                if (err) {
                    return callback(err);
                }
                callback();
            });
        }
    } catch (ex) {
        callback(ex);
    }
};

WsMock.prototype.disconnect = function (callback) {
    var self = this;
    try {
        if (self.gatewayInstance) {
            self.gatewayInstance.kill();
        }
        callback();
    } catch (ex) {
        callback(ex);
    }
};

WsMock.prototype.sendMessage = function (message, callback) {
    var self = this;
    try {
        self.clientInstance.sendMessage(message, callback);
    } catch (ex) {
        callback(ex);
    }
};

module.exports = WsMock;