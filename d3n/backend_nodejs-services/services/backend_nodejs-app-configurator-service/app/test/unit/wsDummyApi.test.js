var _ = require('lodash');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var StandardErrors = require('nodejs-errors');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/tenant.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var ProtocolMessage = require('nodejs-protocol');
var TenantContractAuditLog = Database.RdbmsService.Models.Tenant.TenantContractAuditLog;

describe('WS Dummy API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'appReferralLink', function () {
            it('[' + serie + '] ' + 'FAIL: appReferralLink', function (done) {
                global.wsHelper.sinonSandbox.stub(global.wsHelper._psFake.messageHandlers, "getProfile", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError(null);
                    response.setContent({
                        profile: {
                            roles: ['ANONYMOUS']
                        }
                    });
                    clientSession.sendMessage(response);
                });
                
                var content = {
                    referralUserId: '12321-3435'
                };
                global.wsHelper.apiSecureFail(serie, 'dummy/appReferralLink', ProfileData.CLIENT_INFO_1, content, StandardErrors.ERR_INSUFFICIENT_RIGHTS.message, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: appReferralLink', function (done) {
                var buddyAddForUserSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._fsFake.messageHandlers, "buddyAddForUser");
                global.wsHelper.sinonSandbox.stub(global.wsHelper._psFake.messageHandlers, "getProfile", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError(null);
                    response.setContent({
                        profile: {
                            roles: ['REGISTERED']
                        }
                    });
                    clientSession.sendMessage(response);
                });
                
                
                var referralUserId = '2222-4444';
                var content = {
                    referralUserId: referralUserId
                };
                global.wsHelper.apiSecureSucc(serie, 'dummy/appReferralLink', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    assert.strictEqual(buddyAddForUserSpy.callCount, 2);
                    assert.strictEqual(buddyAddForUserSpy.getCall(0).args[0].getContent().userId, ProfileData.CLIENT_INFO_1.profile.userId);
                    assert.strictEqual(buddyAddForUserSpy.getCall(0).args[0].getContent().userIds[0], referralUserId);
                    assert.strictEqual(buddyAddForUserSpy.getCall(1).args[0].getContent().userId, referralUserId);
                    assert.strictEqual(buddyAddForUserSpy.getCall(1).args[0].getContent().userIds[0], ProfileData.CLIENT_INFO_1.profile.userId);
                }, done);
            });
        });
    });
});