var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/workorder.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;

describe('WS Workorder Billing API', function () {
    //this.timeout(10000);
    //global.wsHelper.series().forEach(function (serie) {
    //    describe('[' + serie + '] ' + 'workorderBillingUpdate', function () {
    //        it('[' + serie + '] ' + 'SUCCESS: workorderBillingUpdate:create', function (done) {
    //            var content = _.clone(Data.WORKORDER_BILLING_MODEL_TEST);
    //            content.id = null;
    //            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderBillingUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
    //                var shouldResponseContent = _.clone(Data.WORKORDER_BILLING_MODEL_TEST);
    //                ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorderBilling, ['id']);
    //            }, done);
    //        });
    //        it('[' + serie + '] ' + 'SUCCESS: workorderBillingUpdate:update', function (done) {
    //            var content = { id: DataIds.WORKORDER_BILLING_MODEL_1_ID, name: Data.WORKORDER_BILLING_MODEL_1.name + ' updated' };
    //            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderBillingUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
    //                var shouldResponseContent = _.clone(Data.WORKORDER_BILLING_MODEL_1);
    //                shouldResponseContent.name = content.name;
    //                ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorderBilling);
    //            }, done);
    //        });
    //        it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderBillingUpdate:update', function (done) {
    //            var content = { id: DataIds.WORKORDER_BILLING_MODEL_TEST_ID, name: 'updated' };
    //            global.wsHelper.apiSecureFail(serie, 'workorder/workorderBillingUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
    //        });
    //    });

    //    describe('[' + serie + '] ' + 'workorderBillingDelete', function () {
    //        it('[' + serie + '] ' + 'SUCCESS: workorderBillingDelete', function (done) {
    //            var content = { id: DataIds.WORKORDER_BILLING_MODEL_NO_DEPENDENCIES_ID };
    //            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderBillingDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
    //                setImmediate(function (done) {
    //                    global.wsHelper.apiSecureFail(serie, 'workorder/workorderBillingDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
    //                }, done);
    //            });
    //        });
    //        it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES workorderBillingDelete', function (done) {
    //            var content = { id: DataIds.WORKORDER_BILLING_MODEL_1_ID };
    //            global.wsHelper.apiSecureFail(serie, 'workorder/workorderBillingDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
    //        });
    //        it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderBillingDelete', function (done) {
    //            var content = { id: DataIds.WORKORDER_BILLING_MODEL_TEST_ID };
    //            global.wsHelper.apiSecureFail(serie, 'workorder/workorderBillingDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
    //        });
    //    });

    //    describe('[' + serie + '] ' + 'workorderBillingGet', function () {
    //        it('[' + serie + '] ' + 'SUCCESS: workorderBillingGet', function (done) {
    //            var content = { id: DataIds.WORKORDER_BILLING_MODEL_1_ID };
    //            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderBillingGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
    //                var shouldResponseContent = _.clone(Data.WORKORDER_BILLING_MODEL_1);
    //                ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorderBilling);
    //            }, done);
    //        });
    //        it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderBillingGet', function (done) {
    //            var content = { id: DataIds.WORKORDER_BILLING_MODEL_TEST_ID };
    //            global.wsHelper.apiSecureFail(serie, 'workorder/workorderBillingGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
    //        });
    //    });

    //    describe('[' + serie + '] ' + 'workorderBillingList', function () {
    //        it('[' + serie + '] ' + 'SUCCESS: workorderBillingList', function (done) {
    //            var content = {
    //                limit: Config.rdbms.limit,
    //                offset: 0,
    //                searchBy: { id: DataIds.WORKORDER_BILLING_MODEL_1_ID },
    //                orderBy: [{ field: 'name', direction: 'asc' }]
    //            };
    //            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderBillingList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
    //                var shouldResponseContent = _.clone(Data.WORKORDER_BILLING_MODEL_1);
    //                ShouldHelper.deepEqual(shouldResponseContent, responseContent.list.items[0]);
    //            }, done);
    //        });
    //        it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderBillingList', function (done) {
    //            var content = {
    //                limit: Config.rdbms.limit,
    //                offset: 0,
    //                searchBy: { id: DataIds.WORKORDER_BILLING_MODEL_TEST_ID },
    //                orderBy: [{ field: 'name', direction: 'asc' }]
    //            };
    //            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderBillingList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
    //                should.exist(receivedContent);
    //                should(receivedContent)
    //                    .has.property('list')
    //                    .which.have.property('items')
    //                    .which.has.length(0);
    //            }, done);
    //        });
    //    });
    //});
});