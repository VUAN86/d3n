var _ = require('lodash');
var should = require('should');
var async = require('async');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Tombola = Database.RdbmsService.Models.TombolaManager.Tombola;
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/tombola.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var TombolaApiFactory = require('../../factories/tombolaApiFactory.js');
var TenantService = require('../../services/tenantService.js');

describe('WS Tombola API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        var stub_getSessionTenantId;
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
        
        
        describe('[' + serie + '] ' + 'tombolaUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: tombolaUpdate:create', function (done) {
                var content = _.cloneDeep(Data.TOMBOLA_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.TOMBOLA_TEST);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.TOMBOLA_TEST.regionalSettingsIds);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.TOMBOLA_TEST.applicationsIds);
                    if (_.has(responseContent.tombola, 'prizes') && _.isArray(responseContent.tombola.prizes)) {
                        _.forEach(responseContent.tombola.prizes, function (prize) {
                            delete prize.id;
                        });
                    }
                    if (_.has(responseContent.tombola, 'consolationPrize') && _.isObject(responseContent.tombola.consolationPrize)) {
                        delete responseContent.tombola.consolationPrize.id;
                    }
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.tombola, ['createDate', 'id', 'tenantId', 'status']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: tombolaUpdate:create, default preCloseOffsetMinutes', function (done) {
                var content = _.cloneDeep(Data.TOMBOLA_TEST);
                delete content.preCloseOffsetMinutes;
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.TOMBOLA_TEST);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.TOMBOLA_TEST.regionalSettingsIds);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.TOMBOLA_TEST.applicationsIds);
                    shouldResponseContent.preCloseOffsetMinutes = Config.defaultPreCloseOffsetMinutes;
                    if (_.has(responseContent.tombola, 'prizes') && _.isArray(responseContent.tombola.prizes)) {
                        _.forEach(responseContent.tombola.prizes, function (prize) {
                            delete prize.id;
                        });
                    }
                    if (_.has(responseContent.tombola, 'consolationPrize') && _.isObject(responseContent.tombola.consolationPrize)) {
                        delete responseContent.tombola.consolationPrize.id;
                    }
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.tombola, ['createDate', 'id', 'tenantId', 'status']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: tombolaUpdate:update', function (done) {
                var content = {
                    id: DataIds.TOMBOLA_2_ID,
                    name: Data.TOMBOLA_2.name + ' updated',
                    playoutTarget: Tombola.constants().PAYOUT_TARGET_TARGET_DATE,
                };
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep(Data.TOMBOLA_2);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.TOMBOLA_2.regionalSettingsIds);
                    shouldResponseContent.applicationsIds = ShouldHelper.treatAsList(Data.TOMBOLA_2.applicationsIds);
                    shouldResponseContent.name = content.name;
                    shouldResponseContent.playoutTarget = content.playoutTarget;
                    var targetDate = Date.parse(shouldResponseContent.endDate.substring(0, 19));
                    targetDate = new Date(targetDate + 1 * 60000);
                    shouldResponseContent.targetDate = DateUtils.isoDate(targetDate.getTime());
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.tombola, ['createDate', 'id', 'tenantId', 'status']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_TOMBOLA_IS_OPEN_CHECKOUT: tombolaUpdate:update', function (done) {
                var content = {
                    id: DataIds.TOMBOLA_2_ID,
                    name: Data.TOMBOLA_2.name + ' updated',
                    percentOfTicketsAmount: 15 // <-- is lower than actual
                };
                TombolaApiFactory.tombolaSetOpenCheckout(content, null, function (err) {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaUpdate', ProfileData.CLIENT_INFO_1, content, Errors.TombolaApi.TombolaIsOpenCheckout, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: tombolaUpdate:update', function (done) {
                var content = { id: DataIds.TOMBOLA_TEST_ID, title: 'updated', startDate: DateUtils.isoFuture(), endDate: DateUtils.isoNow() };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: tombolaUpdate:update', function (done) {
                var content = { id: DataIds.TOMBOLA_TEST_ID, title: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'tombolaActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: tombolaActivate', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.TOMBOLA_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                            setImmediate(function (done) {
                                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                                    should(responseContent.tombola.status).be.equal('active');
                                }, done);
                            }, done);
                        });
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VOUCHER_IS_DEACTIVATED: tombolaActivate', function (done) {
                var content = { id: DataIds.TOMBOLA_1_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaActivate', ProfileData.CLIENT_INFO_1, content, Errors.VoucherApi.VoucherIsDeactivated, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: tombolaActivate', function (done) {
                var content = { id: DataIds.TOMBOLA_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'tombolaDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: tombolaDeactivate', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.TOMBOLA_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                            setImmediate(function (done) {
                                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                                    should(responseContent.tombola.status).be.equal('inactive');
                                }, done);
                            }, done);
                        });
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VOUCHER_IS_DEACTIVATED: tombolaDeactivate', function (done) {
                var content = { id: DataIds.TOMBOLA_1_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.VoucherApi.VoucherIsDeactivated, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: tombolaDeactivate', function (done) {
                var content = { id: DataIds.TOMBOLA_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'tombolaArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: tombolaArchive', function (done) {
                var content = { id: DataIds.TOMBOLA_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaActivate', ProfileData.CLIENT_INFO_1, content, Errors.TombolaApi.TombolaIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: tombolaArchive', function (done) {
                var content = { id: DataIds.TOMBOLA_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'tombolaDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: tombolaDelete', function (done) {
                var content = { id: DataIds.TOMBOLA_INACTIVE };
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: tombolaDelete', function (done) {
                var content = { id: DataIds.TOMBOLA_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES tombolaDelete', function (done) {
                var content = { id: DataIds.TOMBOLA_1_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.SequelizeForeignKeyConstraintError, done);
            });
        });

        describe('[' + serie + '] ' + 'tombolaGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: tombolaGet', function (done) {
                var content = { id: DataIds.TOMBOLA_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                   should(responseContent.tombola.title).be.equal(Data.TOMBOLA_1.title);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: tombolaGet', function (done) {
                var content = { id: DataIds.TOMBOLA_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'tombolaManager/tombolaGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'tombolaList', function () {
            it('[' + serie + '] ' + 'SUCCESS: tombolaList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TOMBOLA_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                   should(responseContent.items[0].title).be.equal(Data.TOMBOLA_1.title);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: tombolaList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TOMBOLA_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'tombola schedule', function () {
            it('[' + serie + '] ' + 'SUCCESS: schedule by create', function (done) {
                var content = _.cloneDeep(Data.TOMBOLA_TEST);
                content.id = null;
                content.startDate = DateUtils.isoDate((new Date().getTime()) + 10000);
                content.endDate = DateUtils.isoDate((new Date().getTime()) + 20000);
                content.targetDate = DateUtils.isoDate((new Date().getTime()) + 20000);
                content.name = 'test schedule';
                content.status = 'ACTIVE';
                global.wsHelper.apiSecureSucc(serie, 'tombolaManager/tombolaCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                }, done);
            });
        });
        
    });
});
