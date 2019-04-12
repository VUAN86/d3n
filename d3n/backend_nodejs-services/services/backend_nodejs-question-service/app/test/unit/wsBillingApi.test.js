var _ = require('lodash');
var should = require('should');
var assert = require('chai').assert;
var async = require('async');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/billing.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var PaymentStructure = Database.RdbmsService.Models.Billing.PaymentStructure;
var tenantService = require('../../services/tenantService.js');

describe('WS Billing API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'paymentStructureUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureUpdate:create', function (done) {
                var content = _.clone(Data.PAYMENT_STRUCTURE_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructure, ['id', 'tenantId', 'createDate']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_1_ID, name: Data.PAYMENT_STRUCTURE_1.name + ' updated' };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_1);
                    shouldResponseContent.name = content.name;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructure, ['tenantId', 'createDate']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TEST_ID, name: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentStructureDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureDelete', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureDelete', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentStructureGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureGet', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructure, ['tenantId', 'createDate']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureGet', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentStructureList', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_STRUCTURE_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId', 'createDate']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_STRUCTURE_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent).which.have.property('items').which.has.length(0);
                }, done);
            });
        });
        
        
        describe('[' + serie + '] ' + 'payment structure APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test payment structure create/update', function (done) {
                var testSessionTenantId = ProfileData.CLIENT_INFO_2.appConfig.tenantId;
                var content = _.clone(Data.PAYMENT_STRUCTURE_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureCreate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            newItemId = responseContent.paymentStructure.id;
                            var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_TEST);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructure, ['id', 'createDate']);
                        }, next);
                    },
                    
                    // check tenantId was set proper
                    function (next) {
                        PaymentStructure.findOne({ where: { id: newItemId } }).then(function (item) {
                            try {
                                assert.strictEqual(item.get({plain: true}).tenantId, testSessionTenantId);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // update succesully
                    function (next) {
                        var content = { id: newItemId, name: 'name test updated' };
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureUpdate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            should(responseContent.paymentStructure.name).be.equal(content.name);
                        }, next);
                    },
                    
                    // change session tenant and try to update an item not belong to session tenant, should return no record found error
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_1_ID;
                        var content = { id: newItemId, name: 'name test' };
                        global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // restore session tenant
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_2_ID;
                        return setImmediate(next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'test payment structure get/list', function (done) {
                //return done();
                var item = _.clone(Data.PAYMENT_STRUCTURE_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                async.series([
                    // create tenant 1 items
                    function (next) {
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.paymentStructure.id;
                                var shouldResponseContent = _.clone(item);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructure, ['id', 'createDate']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureCreate', ProfileData.CLIENT_INFO_2, item, function (responseContent) {
                                item.id = responseContent.paymentStructure.id;
                                var shouldResponseContent = _.clone(item);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructure, ['id', 'createDate']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructure, ['createDate']);
                        }, next);
                    },
                    
                    // list success
                    function (next) {
                        var itm = tenant1Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'name', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['createDate']);
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    
                    // list error
                    function (next) {
                        var itm = tenant2Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'name', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
        });
        
        describe('[' + serie + '] ' + 'paymentStructureActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureActivate', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentStructure.isActive).be.equal(1);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureActivate', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentStructureDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureDeactivate', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentStructure.isActive).be.equal(0);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureDeactivate', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
    });
});

describe('WS Billing Structure Tier API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'paymentStructureTierUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureTierUpdate:create', function (done) {
                var content = _.clone(Data.PAYMENT_STRUCTURE_TIER_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureTierUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_TIER_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructureTier, ['id']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureTierUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TIER_1_1_ID, quantityMin: 10 };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureTierUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_TIER_1_1);
                    shouldResponseContent.quantityMin = content.quantityMin;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructureTier, ['quantityMin']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureTierUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TIER_TEST_ID, quantityMin: 10 };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureTierUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentStructureTierDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureTierDelete', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TIER_2_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureTierDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureTierDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureTierDelete', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TIER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureTierDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentStructureTierGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureTierGet', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TIER_1_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureTierGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_TIER_1_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentStructureTier);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureTierGet', function (done) {
                var content = { id: DataIds.PAYMENT_STRUCTURE_TIER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentStructureTierGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentStructureTierList', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentStructureTierList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_STRUCTURE_TIER_1_1_ID },
                    orderBy: [{ field: 'paymentStructureId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureTierList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_STRUCTURE_TIER_1_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentStructureTierList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_STRUCTURE_TIER_TEST_ID },
                    orderBy: [{ field: 'paymentStructureId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentStructureTierList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent).which.have.property('items').which.has.length(0);
                }, done);
            });
        });
    });
});

describe('WS Billing Tenant Bill Of Material API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'paymentTenantBillOfMaterialUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentTenantBillOfMaterialUpdate:create', function (done) {
                
                var content = _.clone(Data.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialUpdate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST);
                    shouldResponseContent.tenantId = ProfileData.CLIENT_INFO_2.appConfig.tenantId;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentTenantBillOfMaterial, ['id', 'approvalDate', 'paymentStructureId', 'totalTransactions']);
                }, done);
                
            });
            it('[' + serie + '] ' + 'SUCCESS: paymentTenantBillOfMaterialUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID, description: 'updated' };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_TENANT_BILL_OF_MATERIAL_1);
                    shouldResponseContent.description = content.description;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentTenantBillOfMaterial, ['id', 'paymentStructureId', 'totalTransactions']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentTenantBillOfMaterialUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST_ID_NOTEXISTS, description: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentTenantBillOfMaterialUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentTenantBillOfMaterialGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentTenantBillOfMaterialGet', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_TENANT_BILL_OF_MATERIAL_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentTenantBillOfMaterial, ['totalTransactions']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentTenantBillOfMaterialGet', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentTenantBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentTenantBillOfMaterialList', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentTenantBillOfMaterialList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID },
                    orderBy: [{ field: 'description', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_TENANT_BILL_OF_MATERIAL_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['totalTransactions']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentTenantBillOfMaterialList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST_ID },
                    orderBy: [{ field: 'description', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent).which.have.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentTenantBillOfMaterialApprove', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentTenantBillOfMaterialApprove', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialApprove', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentTenantBillOfMaterial.status).be.equal('approved');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentTenantBillOfMaterialApprove', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentTenantBillOfMaterialApprove', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentTenantBillOfMaterialReject', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentTenantBillOfMaterialReject', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialReject', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentTenantBillOfMaterial.status).be.equal('unapproved');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentTenantBillOfMaterialReject', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentTenantBillOfMaterialReject', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentTenantBillOfMaterialPay', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentTenantBillOfMaterialPay', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialPay', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentTenantBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentTenantBillOfMaterial.status).be.equal('payed');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_BILL_NOT_APPROVED: paymentTenantBillOfMaterialPay', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentTenantBillOfMaterialPay', ProfileData.CLIENT_INFO_1, content, Errors.BillingApi.BillNotApproved, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentTenantBillOfMaterialPay', function (done) {
                var content = { id: DataIds.PAYMENT_TENANT_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentTenantBillOfMaterialPay', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
    });
});

describe('WS Billing Resource Bill Of Material API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'paymentResourceBillOfMaterialUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentResourceBillOfMaterialUpdate:create', function (done) {
                var content = _.clone(Data.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentResourceBillOfMaterial, ['id', 'approvalDate', 'paymentStructureId', 'totalTransactions']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: paymentResourceBillOfMaterialUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID, description: 'updated' };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1);
                    shouldResponseContent.description = content.description;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentResourceBillOfMaterial, ['id', 'paymentStructureId', 'totalTransactions']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentResourceBillOfMaterialUpdate:update', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST_ID, description: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentResourceBillOfMaterialUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentResourceBillOfMaterialGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentResourceBillOfMaterialGet', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentResourceBillOfMaterial, ['totalTransactions']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentResourceBillOfMaterialGet', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentResourceBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentResourceBillOfMaterialList', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentResourceBillOfMaterialList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID },
                    orderBy: [{ field: 'description', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['totalTransactions']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentResourceBillOfMaterialList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST_ID },
                    orderBy: [{ field: 'description', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent).which.have.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentResourceBillOfMaterialApprove', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentResourceBillOfMaterialApprove', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialApprove', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentResourceBillOfMaterial.status).be.equal('approved');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentResourceBillOfMaterialApprove', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentResourceBillOfMaterialApprove', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentResourceBillOfMaterialReject', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentResourceBillOfMaterialReject', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialReject', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentResourceBillOfMaterial.status).be.equal('unapproved');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentResourceBillOfMaterialReject', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentResourceBillOfMaterialReject', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'paymentResourceBillOfMaterialPay', function () {
            it('[' + serie + '] ' + 'SUCCESS: paymentResourceBillOfMaterialPay', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialPay', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentResourceBillOfMaterialGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.paymentResourceBillOfMaterial.status).be.equal('payed');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_BILL_NOT_APPROVED: paymentResourceBillOfMaterialPay', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_1_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentResourceBillOfMaterialPay', ProfileData.CLIENT_INFO_1, content, Errors.BillingApi.BillNotApproved, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentResourceBillOfMaterialPay', function (done) {
                var content = { id: DataIds.PAYMENT_RESOURCE_BILL_OF_MATERIAL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'billing/paymentResourceBillOfMaterialPay', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'batchBillCalculation', function () {
            it('[' + serie + '] ' + 'SUCCESS: batchBillCalculation as API method', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'billing/batchBillCalculation', ProfileData.CLIENT_INFO_1, null, function (responseContent) {
                    should(responseContent).be.empty;
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: batchBillCalculation as CronMasterJob', function (done) {
                var tickCompleteSpy = global.wsHelper.sinon.spy();
                var billingCalcJob = require('./../../jobs/billingCalcJob.js');
                billingCalcJob.on(global.wsHelper.cmaster.EVENTS.TICK_COMPLETE, tickCompleteSpy);
                billingCalcJob.start();
                setTimeout(function () {
                    should(tickCompleteSpy.called).be.true;
                    billingCalcJob.stop();
                    return done();
                }, 3000); 
            });
        });
    });
});

//describe('WS Billing Action API', function () {
//    this.timeout(10000);
//    global.wsHelper.series().forEach(function (serie) {
//        describe('[' + serie + '] ' + 'paymentActionUpdate', function () {
//            it('[' + serie + '] ' + 'SUCCESS: paymentActionUpdate:create', function (done) {
//                var content = _.clone(Data.PAYMENT_ACTION_TEST);
//                content.id = null;
//                global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
//                    var shouldResponseContent = _.clone(Data.PAYMENT_ACTION_TEST);
//                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentAction, ['id', 'approvalDate', 'paymentStructureId', 'totalTransactions']);
//                }, done);
//            });
//            it('[' + serie + '] ' + 'SUCCESS: paymentActionUpdate:update', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_1_ID, description: 'updated' };
//                global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
//                    var shouldResponseContent = _.clone(Data.PAYMENT_ACTION_1);
//                    shouldResponseContent.description = content.description;
//                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentAction, ['id', 'paymentStructureId', 'totalTransactions']);
//                }, done);
//            });
//            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentActionUpdate:update', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_TEST_ID, description: 'updated' };
//                global.wsHelper.apiSecureFail(serie, 'billing/paymentActionUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
//            });
//        });

//        describe('[' + serie + '] ' + 'paymentActionGet', function () {
//            it('[' + serie + '] ' + 'SUCCESS: paymentActionGet', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_1_ID };
//                global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
//                    var shouldResponseContent = _.clone(Data.PAYMENT_ACTION_1);
//                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.paymentAction, ['totalTransactions']);
//                }, done);
//            });
//            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentActionGet', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_TEST_ID };
//                global.wsHelper.apiSecureFail(serie, 'billing/paymentActionGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
//            });
//        });

//        describe('[' + serie + '] ' + 'paymentActionList', function () {
//            it('[' + serie + '] ' + 'SUCCESS: paymentActionList', function (done) {
//                var content = {
//                    limit: Config.rdbms.limit,
//                    offset: 0,
//                    searchBy: { id: DataIds.PAYMENT_ACTION_1_ID },
//                    orderBy: [{ field: 'description', direction: 'asc' }]
//                };
//                global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
//                    var shouldResponseContent = _.clone(Data.PAYMENT_ACTION_1);
//                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['totalTransactions']);
//                }, done);
//            });
//            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentActionList', function (done) {
//                var content = {
//                    limit: Config.rdbms.limit,
//                    offset: 0,
//                    searchBy: { id: DataIds.PAYMENT_ACTION_TEST_ID },
//                    orderBy: [{ field: 'description', direction: 'asc' }]
//                };
//                global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
//                    should.exist(receivedContent);
//                    should(receivedContent).which.have.property('items').which.has.length(0);
//                }, done);
//            });
//        });

//        describe('[' + serie + '] ' + 'paymentActionActivate', function () {
//            it('[' + serie + '] ' + 'SUCCESS: paymentActionActivate', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_1_ID };
//                global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
//                    setImmediate(function (done) {
//                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
//                            should(responseContent.paymentAction.status).be.equal('active');
//                        }, done);
//                    }, done);
//                });
//            });
//            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentActionActivate', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_TEST_ID };
//                global.wsHelper.apiSecureFail(serie, 'billing/paymentActionActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
//            });
//        });

//        describe('[' + serie + '] ' + 'paymentActionDeactivate', function () {
//            it('[' + serie + '] ' + 'SUCCESS: paymentActionDeactivate', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_1_ID };
//                global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
//                    setImmediate(function (done) {
//                        global.wsHelper.apiSecureSucc(serie, 'billing/paymentActionGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
//                            should(responseContent.paymentAction.status).be.equal('inactive');
//                        }, done);
//                    }, done);
//                });
//            });
//            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: paymentActionDeactivate', function (done) {
//                var content = { id: DataIds.PAYMENT_ACTION_TEST_ID };
//                global.wsHelper.apiSecureFail(serie, 'billing/paymentActionDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
//            });
//        });
//    });
//});
