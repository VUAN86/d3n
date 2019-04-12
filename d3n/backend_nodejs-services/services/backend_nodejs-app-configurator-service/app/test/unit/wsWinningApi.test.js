var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/winning.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var WinningComponent = Database.RdbmsService.Models.WinningManager.WinningComponent;
var GameHasWinningComponent = Database.RdbmsService.Models.Game.GameHasWinningComponent;
var TenantService = require('../../services/tenantService.js');

describe('WS Winning Manager Studio API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        var stub_getSessionTenantId;
        var stub_winningComponentServiceLoadCodes;
        var stub_winningComponentServiceDeleteCodes;
        var testSessionTenantId = DataIds.TENANT_1_ID;
        before(function (done) {
            stub_getSessionTenantId = global.wsHelper.sinon.stub(TenantService, 'getSessionTenantId', function (message, clientSession, cb) {
                return cb(false, testSessionTenantId);
            });
            return setImmediate(done);
        });
        after(function (done) {
            stub_getSessionTenantId.restore();
            return setImmediate(done);
        });

        describe('[' + serie + '] ' + 'winningComponentUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: winningComponentUpdate:create', function (done) {
                var content = _.clone(Data.WINNING_COMPONENT_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WINNING_COMPONENT_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.winningComponent, ['id', 'tenantId']);
                }, done);
            });
            //it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: winningComponentCreate:winningComponentProviderId:0', function (done) {
            //    var content = _.omit(Data.WINNING_COMPONENT_TEST, ['id']);
            //    content.winningComponentProviderId = 0;
            //    global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            //});
            it('[' + serie + '] ' + 'SUCCESS: winningComponentUpdate:update', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_1_ID, title: 'title test' };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.winningComponent.name).be.equal(content.name);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: winningComponentUpdate:update', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_TEST_ID, title: 'title test' };
                global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'winningComponentActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: winningComponentActivate', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.winningComponent.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: winningComponentActivate', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'winningComponentDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: winningComponentDeactivate', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.winningComponent.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: winningComponentDeactivate', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'winningComponentArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: winningComponentArchive', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentActivate', ProfileData.CLIENT_INFO_1, content, Errors.WinningApi.WinningComponentIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: winningComponentArchive', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'winningComponentDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: winningComponentDelete', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: winningComponentDelete', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'winningComponentGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: winningComponentGet', function (done) {
                var content = { id: DataIds.WINNING_COMPONENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WINNING_COMPONENT_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.winningComponent, ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: winningComponentGet', function (done) {
                var content = { userId: DataIds.WINNING_COMPONENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'winningManager/winningComponentGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'winningComponentList', function () {
            it('[' + serie + '] ' + 'SUCCESS: winningComponentList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.WINNING_COMPONENT_1_ID },
                    orderBy: [{ field: 'title', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WINNING_COMPONENT_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: winningComponentList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.WINNING_COMPONENT_TEST_ID },
                    orderBy: [{ field: 'title', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'winningManager/winningComponentList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content).has.property('items').which.has.length(0);
                }, done);
            });
        });

    });
});