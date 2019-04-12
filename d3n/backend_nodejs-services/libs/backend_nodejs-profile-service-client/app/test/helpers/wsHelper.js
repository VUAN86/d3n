var _ = require('lodash');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var ProtocolMessage = require('nodejs-protocol');
var FakeProfileService = require('./../services/psc.fakeService.js');
var FakeServiceRegistryService = require('./../services/src.fakeService.js');
var ProfileServiceClient = require('./../../../index.js');

var WsHelper = function (registryServiceURIs, profileServiceName) {
    var self = this;
    if (process.env.FAKE_SERVICES === 'true') {
        self._psFake = new FakeProfileService();
        self._srFake = new FakeServiceRegistryService();
    }
    self._pscValidInstance = new ProfileServiceClient();
    self._pscInvalidInstance = new ProfileServiceClient(Config.registryServiceURIs, Config.profileServiceName + 'Invalid');
}

module.exports = WsHelper;

WsHelper.prototype.build = function (done) {
    var self = this;
    try {
        async.series([
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
                    self._srFake.build(function (err) {
                        return next(err);
                    });
                } else {
                    return next();
                }
            },
        ], function (err) {
            if (err) {
                return done(err);
            }
            return done();
        });
    } catch (ex) {
        return done(new Error(Errors.PscApi.FatalError));
    }
}

WsHelper.prototype.shutdown = function (done) {
    var self = this;
	async.series([
        function (next) {
            self._pscValidInstance.disconnect(function (err) {
                return next(err);
            });
        },
		function (next) {
			if (process.env.FAKE_SERVICES === 'true') {
				self._psFake.shutdown();
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
}

WsHelper.prototype.pscValid = function () {
    var self = this;
    return self._pscValidInstance;
}

WsHelper.prototype.pscInvalid = function () {
    var self = this;
    return self._pscInvalidInstance;
}