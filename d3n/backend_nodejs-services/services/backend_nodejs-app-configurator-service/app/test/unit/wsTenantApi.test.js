var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/tenant.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;

var TenantContractAuditLog = Database.RdbmsService.Models.Tenant.TenantContractAuditLog;

describe('WS Tenant API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'tenantUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: tenantUpdate update', function (done) {
                var content = {
                    name: 'updated',
                    contactLastName: 'updated'
                };

                global.wsHelper.apiSecureSucc(serie, 'tenant/tenantUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.TENANT_1);
                    shouldResponseContent.name = content.name;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.tenant, ['id']);
                }, function () {
                    var today = new Date().toISOString().slice(0,10) + "T00:00:00Z";
                    var queryParams = {
                        limit: 1,
                        offset: 0,
                        searchBy: { createDate: { '$gt': today } }
                    };

                    global.httpHelper.postSuccess('/tenantManagementConnector/tenantTrailList', queryParams, function(trailResponseContent) {
                        should.exist(trailResponseContent);
                        ShouldHelper.deepEqual({
                            'creatorProfileId': DataIds.LOCAL_USER_ID,
                            'type': 'dataChange',
                            'tenantId': DataIds.TENANT_1_ID,
                            'items': {
                                "contactLastName": { "old": Data.TENANT_1.contactLastName, "new": content.contactLastName },
                                "name": { "old": Data.TENANT_1.name, "new": content.name },
                            }
                        }, trailResponseContent.items[0], ['id', 'createDate', 'updateDate']);
                    }, done);
                });
            });
        });
        describe('[' + serie + '] ' + 'tenantGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: tenantGet', function (done) {
                var content = { };
                global.wsHelper.apiSecureSucc(serie, 'tenant/tenantGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.TENANT_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.tenant, ['id']);
                }, done);
            });
        });

        // Ref #5810 - tenantList is not available for everyone, users have access only to the current tenant information
        /*
        describe('[' + serie + '] ' + 'tenantList', function () {
            it('[' + serie + '] ' + 'SUCCESS: list one', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TENANT_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/tenantList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(1);
                    ShouldHelper.deepEqual(Data.TENANT_1, responseContent.items[0], ['id']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: list all', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/tenantList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCC ERR_DATABASE_NO_RECORD_FOUND: list', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TENANT_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/tenantList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });
        */

        describe('[' + serie + '] ' + 'contractList', function () {
            it('[' + serie + '] ' + 'SUCCESS: list one', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TENANT_CONTRACT_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/contractList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(1);
                    ShouldHelper.deepEqual(Data.TENANT_1_CONTRACT_1, responseContent.items[0], ['id']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: list all', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/contractList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: not list contract for another tenant than the one in session', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { tenantId: DataIds.TENANT_2_ID }
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/contractList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(2);
                }, done);
            });

            it('[' + serie + '] ' + 'SUCC ERR_DATABASE_NO_RECORD_FOUND: list', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TENANT_CONTRACT_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/contractList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'contractChange', function () {
            it('[' + serie + '] ' + 'SUCCESS: tenantChange', function (done) {
                var content = {
                    id: DataIds.TENANT_CONTRACT_1_ID,
                    tenantId: DataIds.TENANT_1_ID,
                    action: 'hold'
                };

                global.wsHelper.apiSecureSucc(serie, 'tenant/contractChange', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.assign({}, Data.TENANT_1_CONTRACT_1, content);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.contract, ['id']);
                }, function () {
                    TenantContractAuditLog.findOne(
                      {
                        where: { contractId: DataIds.TENANT_CONTRACT_1_ID }, 
                        order: 'createDate DESC'
                      }).then(function (auditLogResult) {
                           var auditLog = auditLogResult.get();

                           ShouldHelper.deepEqual({
                            'creatorProfileId': DataIds.LOCAL_USER_ID,
                            'type': 'holdRequest',
                            'contractId': DataIds.TENANT_CONTRACT_1_ID,
                            'items': {
                                'action': { 'old': Data.TENANT_1_CONTRACT_1.action, "new": content.action },
                            }
                          }, auditLog, ['id', 'createDate']);
                        
                          return setImmediate(done);
                      })
                      .catch(function (ex) {
                          console.log("Exception retrieving last contract audit log entry", ex);
                          return setImmediate(done, ex);
                      });
                });
            });
        });

        describe('[' + serie + '] ' + 'invoiceList', function () {
            it('[' + serie + '] ' + 'SUCCESS: list one', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TENANT_INVOICE_1_ID },
                    orderBy: [{ field: 'number', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/invoiceList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(1);
                    ShouldHelper.deepEqual(Data.TENANT_1_INVOICE_1, responseContent.items[0], ['id', 'date', 'paymentDate']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: list all', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/invoiceList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCC ERR_DATABASE_NO_RECORD_FOUND: list', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.TENANT_INVOICE_TEST_ID },
                    orderBy: [{ field: 'number', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'tenant/invoiceList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });
    });
});