var _ = require('lodash');
var async = require('async');
var uuid = require('node-uuid');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var Data = require('./../config/psc.data.js');
var ProtocolMessage = require('nodejs-protocol');
var FakeEventService = require('./../services/esc.fakeService.js');
var FakeMediaService = require('./../services/msc.fakeService.js');
var FakeProfileService = require('./../services/psc.fakeService.js');
var FakeServiceRegistryService = require('./../services/src.fakeService.js');
var FakeUserMessageService = require('./../services/umc.fakeService.js');
var AuthService = require('./../../../index.js');
var WsMock = require('./../helpers/wsMock.js');

var WsHelper = function (series) {
    if (process.env.FAKE_SERVICES === 'true') {
        this._esFake = new FakeEventService();
        this._msFake = new FakeMediaService();
        this._psFake = new FakeProfileService();
        this._srFake = new FakeServiceRegistryService();
        this._umFake = new FakeUserMessageService();
    }
    this._authService = new AuthService();
    this._series = series ? series : ['direct', 'gateway'];
    this._wsMocks = {};
    this.sinon = require('sinon');
    this.sinonSandbox = null;
    this._profiles = undefined;
    this._profileBlobs = undefined;
}

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
                self._authService.build(function (err) {
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
        return done(Errors.AuthApi.FatalError);
    }
}

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
                self._authService.shutdown(function (err) {
                    return next(err);
                });
            },
            function (next) {
                if (process.env.FAKE_SERVICES === 'true') {
                    self._esFake.shutdown();
                    self._msFake.shutdown();
                    self._psFake.shutdown();
                    self._umFake.shutdown();
                    self._srFake.shutdown();
                }
                return next();
            },
        ], function (err) {
            if (err) {
                return done(err);
            }
            return done();
        });
    });
}

WsHelper.prototype.initProfiles = function () {
    this._profiles = [
        { userId: DataIds.EMAIL_USER_ID, roles: ['ANONYMOUS'] },
        { userId: DataIds.TEST_EMAIL_USER_ID, roles: ['ANONYMOUS'] },
        { userId: DataIds.TEST_PHONE_USER_ID, roles: ['ANONYMOUS'] },
        { userId: DataIds.TEST_FACEBOOK_USER_ID, roles: ['ANONYMOUS'] },
        { userId: DataIds.TEST_GOOGLE_USER_ID, roles: ['ANONYMOUS'] },
        { 
            userId: Data.IMPERSONATE_ADMIN_USER.userId, 
            roles: Data.IMPERSONATE_ADMIN_USER.userRoles, 
            emails: [ { 
                'email': Data.IMPERSONATE_ADMIN_USER.email, 
                'verificationStatus': 'verified' 
            }]
        },
        {
            userId: Data.IMPERSONATE_NO_ADMIN_USER.userId, 
            roles: Data.IMPERSONATE_NO_ADMIN_USER.userRoles, 
            emails: [ { 
                'email': Data.IMPERSONATE_NO_ADMIN_USER.email, 
                'verificationStatus': 'verified' 
            }]
        }
    ];
    this._profileBlobs = [];
}

WsHelper.prototype.series = function () {
    return this._series;
}

WsHelper.prototype.apiSecureSucc = function (serie, api, token, content, contentCallback, doneCallback) {
    var self = this;
    try {
        self._wsMocks[serie].sendMessage(_message(api, token, content), function (err, message) {
            if (err) {
                return doneCallback(err);
            }
            try {
                should.exist(message);
                should.not.exist(message.getError());
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
        doneCallback(ex);
    }
}

WsHelper.prototype.apiSucc = function (serie, api, content, contentCallback, doneCallback) {
    return this.apiSecureSucc(serie, api, null, content, contentCallback, doneCallback);
}

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
}

WsHelper.prototype.apiFail = function (serie, api, content, error, doneCallback) {
    return this.apiSecureFail(serie, api, null, content, error, doneCallback);
}

function _message(api, token, content) {
    var message = new ProtocolMessage();
    message.setMessage(api);
    if (token) {
        if (_.isString(token)) {
            message.setToken(token);
        } else if (_.isObject(token)) {
            var tokenValue = null;
            if (_.has(token, 'token')) {
                tokenValue = token.token;
            }
            message.setToken(tokenValue);
            message.setClientInfo(token);
        }
    }
    message.setContent(content);
    message.setSeq(2);
    message.setAck([2]);
    message.setError(null);
    message.setClientId(1);
    message.setTimestamp(_.now());
    return message;
}