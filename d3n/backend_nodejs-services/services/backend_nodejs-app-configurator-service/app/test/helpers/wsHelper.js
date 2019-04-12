var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var Data = require('./../config/profile.data.js');
var DataLoader = require('./../config/index.js');
var ProtocolMessage = require('nodejs-protocol');
var Service = require('./../../../index.js');
var FakeAuthService = require('./../services/auth.fakeService.js');
var FakeEventService = require('./../services/esc.fakeService.js');
var FakeMediaService = require('./../services/msc.fakeService.js');
var FakeProfileService = require('./../services/psc.fakeService.js');
var FakeServiceRegistryService = require('./../services/src.fakeService.js');
var FakeUserMessageService = require('./../services/umc.fakeService.js');
var FakeVoucherService = require('./../services/vsc.fakeService.js');
var FakeFriendService = require('./../services/fsc.fakeService.js');
var FakePaymentService = require('./../services/payment.fakeService.js');
var WsMock = require('./../helpers/wsMock.js');
var logger = require('nodejs-logger')();

var WsHelper = function (series) {
    if (process.env.FAKE_SERVICES === 'true') {
        this._auFake = new FakeAuthService();
        this._esFake = new FakeEventService();
        this._msFake = new FakeMediaService();
        this._psFake = new FakeProfileService();
        this._srFake = new FakeServiceRegistryService();
        this._umFake = new FakeUserMessageService();
        this._vsFake = new FakeVoucherService();
        this._fsFake = new FakeFriendService();
        this._paymentServiceFake = new FakePaymentService();
    }
    this._service = new Service();
    this._series = series ? series : ['direct', 'gateway'];
    this._wsMocks = {};
    this.sinon = require('sinon');
    this.sinonSandbox = null;
    this.cmaster = require('cron-master');
};

module.exports = WsHelper;

WsHelper.prototype.build = function (done) {
    var self = this;
    try {
        async.series([
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._srFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._esFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._msFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._psFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._umFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._auFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._vsFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._fsFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._paymentServiceFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
            function (next) {
                try {
                    DataLoader.loadSession(function (err, res) {
                        return next(err);
                    });
                } catch (ex) {
                    return next(Errors.QuestionApi.FatalError);
                }
            },
            function (next) {
                process.env.IGNORE_SCHEDULER_INIT_ERROR = 'true';
                self._service.build(function (err) {
                    return next(err);
                });
            },
            function (next) {
                async.everySeries(self._series, function (serie, mock) {
                    self._wsMocks[serie] = new WsMock(serie);
                    self._wsMocks[serie].connect(function (err, res) {
                        if (err) {
                            return mock(err);
                        }
                        return mock(null, true);
                    });
                }, function (err) {
                    return next(err);
                });
            }
        ], function (err) {
            if (err) {
                return done(err);
            }
            return done();
        });
    } catch (ex) {
        return done(Errors.QuestionApi.FatalError);
    }
};

WsHelper.prototype.loadData = function (done) {
    var self = this;
    try {
        DataLoader.loadData(function (err, res) {
            if (err) {
                return done(err);
            }
            done();
        });
    } catch (ex) {
        return done(Errors.QuestionApi.FatalError);
    }
};

WsHelper.prototype.shutdown = function (done) {
    var self = this;
    async.everySeries(self._series, function (serie, next) {
        if (self._wsMocks[serie] && _.isFunction(self._wsMocks[serie].disconnect)) {
            self._wsMocks[serie].disconnect(function (err, res) {
                if (err) {
                    return next(err);
                }
                return next(null, true);
            });
        } else {
            return next(null, true);
        }
    }, function (err) {
        if (err) {
            return done(err);
        }
        async.series([
            function (next) {
                self._service.shutdown(function (err) {
                    return next(err);
                });
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._auFake.shutdown();
                    self._esFake.shutdown();
                    self._msFake.shutdown();
                    self._psFake.shutdown();
                    self._umFake.shutdown();
                    self._vsFake.shutdown();
                    self._fsFake.shutdown();
                    self._srFake.shutdown();
                    self._paymentServiceFake.shutdown();
                }
                return next();
            },
            function (next) {
                try {
                    DataLoader.cleanSession(function (err, res) {
                        return next(err);
                    });
                } catch (ex) {
                    return next(Errors.QuestionApi.FatalError);
                }
            },
        ], function (err) {
            if (err) {
                return done(err);
            }
            return done();
        });
    });
};

WsHelper.prototype.series = function () {
    return this._series;
};

WsHelper.prototype.getService = function () {
    return this._service;
};

WsHelper.prototype.apiSecureCall = function (serie, api, token, content, callback) {
    try {
        var self = this;
        self._wsMocks[serie].sendMessage(_message(api, token, content), function (err, message) {
            if (message.getError()) {
                logger.debug('lastValidationError', message.lastValidationError);
            }
            _setError(message);
            return callback(err, message);
        });
    } catch (ex) {
        return setImmediate(callback, ex);
    }
};

WsHelper.prototype.apiSecureSucc = function (serie, api, token, content, contentCallback, doneCallback) {
    var self = this;
    try {
        self._wsMocks[serie].sendMessage(_message(api, token, content), function (err, message) {
            if (err) {
                return doneCallback(err);
            }
            try {
                should.exist(message);
                if (message.getError()) {
                    logger.debug('lastValidationError', message.lastValidationError);
                }
                should.not.exist(message.getError());
                _setError(message);
                ProtocolMessage.isInstance(message).should.be.eql(true);
                message.isValid('websocketMessage').should.be.eql(true);
                if (contentCallback) {
                    contentCallback(message.getContent());
                }
                return doneCallback();
            } catch (ex) {
                return doneCallback(ex);
            }
        });
    } catch (ex) {
        return doneCallback(ex);
    }
};

WsHelper.prototype.apiSucc = function (serie, api, content, contentCallback, doneCallback) {
    return this.apiSecureSucc(serie, api, null, content, contentCallback, doneCallback);
};

WsHelper.prototype.apiSecureFail = function (serie, api, token, content, error, doneCallback) {
    var self = this;
    try {
        self._wsMocks[serie].sendMessage(_message(api, token, content), function (err, message) {
            if (err) {
                return doneCallback(err);
            }
            try {
                should.exist(message);
                should.exist(message.getError());
                logger.debug('lastValidationError', message.lastValidationError);
                _setError(message);
                ProtocolMessage.isInstance(message).should.be.eql(true);
                message.isValid('websocketMessage').should.be.eql(true);
                if (error) {
                    error.should.be.eql(message.getError().message);
                }
                return doneCallback();
            } catch (ex) {
                return doneCallback(ex);
            }
        });
    } catch (ex) {
        return doneCallback(ex);
    }
};

WsHelper.prototype.apiFail = function (serie, api, content, error, doneCallback) {
    return this.apiSecureFail(serie, api, null, content, error, doneCallback);
};

function _message(api, token, content) {
    var message = new ProtocolMessage();
    message.setMessage(api);
    if (token) {
        if (_.isString(token)) {
            message.setToken(token);
        } else if (_.isObject(token)) {
            message.setToken(null);
            message.setClientInfo(token);
        }
        
    }
    message.setContent(content);
    message.setSeq(2);
    message.setAck([2]);
    message.setError(null);
    message.setClientId(DataIds.CLIENT_SESSION_ID);
    message.setTimestamp(_.now());
    return message;
}

function _setError(message) {
    if (_.isPlainObject(message.getError())) {
        return; // do nothing
    }

    if (_.isString(message.getError())) {
        message.setError({
            type: 'server',
            message: message.getError()
        });
    }
}
