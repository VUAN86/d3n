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
var Data = require('./../config/voucher.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Voucher = Database.RdbmsService.Models.VoucherManager.Voucher;
var VoucherProvider = Database.RdbmsService.Models.VoucherManager.VoucherProvider;
var TenantService = require('../../services/tenantService.js');
var VoucherService = require('../../services/voucherService.js');

describe('WS Voucher API', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        var stub_getSessionTenantId;
        var stub_voucherServiceLoadCodes;
        var stub_voucherServiceDeleteCodes;
        var testSessionTenantId = DataIds.TENANT_1_ID;
        before(function (done) {
            stub_getSessionTenantId = global.wsHelper.sinon.stub(TenantService, 'getSessionTenantId', function (message, clientSession, cb) {
                return cb(false, testSessionTenantId);
            });
            stub_voucherServiceLoadCodes = global.wsHelper.sinon.stub(VoucherService, 'loadCodes', function (key, callback) {
                fs.readFile(path.join(__dirname, '../resources', key), 'utf8', function (err, data) {
                    if (err) {
                        return console.log(err);
                    }
                    console.log(data);
                    return callback(false, data);
                });
            });
            stub_voucherServiceDeleteCodes = global.wsHelper.sinon.stub(VoucherService, 'deleteCodes', function (key, callback) {
                return setImmediate(callback, false);
            });
            return setImmediate(done);
        });
        after(function (done) {
            stub_getSessionTenantId.restore();
            stub_voucherServiceLoadCodes.restore();
            stub_voucherServiceDeleteCodes.restore();
            return setImmediate(done);
        });

        describe('[' + serie + '] ' + 'voucherUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherUpdate:create', function (done) {
                var content = _.clone(Data.VOUCHER_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.VOUCHER_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucher, ['id',
                        'stat_archieved', 'stat_inventoryCount', 'stat_specialPrizes', 'stat_won'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: voucherCreate:voucherProviderId:0', function (done) {
                var content = _.omit(Data.VOUCHER_TEST, ['id']);
                content.voucherProviderId = 0;
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: voucherUpdate:update', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID, name: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.voucher.name).be.equal(content.name);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherUpdate:update', function (done) {
                var content = { id: DataIds.VOUCHER_TEST_ID, name: 'name test' };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherActivate', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.voucher.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherActivate', function (done) {
                var content = { id: DataIds.VOUCHER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherDeactivate', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.voucher.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherDeactivate', function (done) {
                var content = { id: DataIds.VOUCHER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherArchive', function (done) {
                var content = { id: DataIds.VOUCHER_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherActivate', ProfileData.CLIENT_INFO_1, content, Errors.VoucherApi.VoucherIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherArchive', function (done) {
                var content = { id: DataIds.VOUCHER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherDelete', function (done) {
                var content = { id: DataIds.VOUCHER_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucher/voucherDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'voucher/voucherGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherDelete', function (done) {
                var content = { id: DataIds.VOUCHER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucher/voucherDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherGet', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.VOUCHER_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucher, [
                        'stat_archieved', 'stat_inventoryCount', 'stat_specialPrizes', 'stat_won'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherGet', function (done) {
                var content = { userId: DataIds.VOUCHER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherList', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.VOUCHER_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.VOUCHER_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], [
                        'stat_archieved', 'stat_inventoryCount', 'stat_specialPrizes', 'stat_won'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.VOUCHER_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content).has.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherProviderUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderUpdate:create', function (done) {
                var content = _.clone(Data.VOUCHER_PROVIDER_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.VOUCHER_PROVIDER_TEST);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.VOUCHER_PROVIDER_TEST.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucherProvider, ['id', 'tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderUpdate:update', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_1_ID, name: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.voucherProvider.name).be.equal(content.name);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherProviderUpdate:update', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_TEST_ID, name: 'name test' };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherProviderActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderActivate', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.voucherProvider.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherProviderActivate', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherProviderDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderDeactivate', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.voucherProvider.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherProviderDeactivate', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherProviderArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderArchive', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderActivate', ProfileData.CLIENT_INFO_1, content, Errors.VoucherApi.VoucherProviderIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherProviderArchive', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherProviderDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderDelete', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucher/voucherProviderDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'voucher/voucherProviderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION: voucherProviderDelete', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_1_ID };
                global.wsHelper.apiSecureFail(serie, 'voucher/voucherProviderDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherProviderDelete', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucher/voucherProviderDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherProviderGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderGet', function (done) {
                var content = { id: DataIds.VOUCHER_PROVIDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.VOUCHER_PROVIDER_1);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.VOUCHER_PROVIDER_1.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucherProvider, ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherProviderGet', function (done) {
                var content = { userId: DataIds.VOUCHER_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherProviderList', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherProviderList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.VOUCHER_PROVIDER_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.VOUCHER_PROVIDER_1);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.VOUCHER_PROVIDER_1.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: voucherProviderList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.VOUCHER_PROVIDER_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content).has.property('items').which.has.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'voucher provider APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test voucher provider create/update', function (done) {
                testSessionTenantId = DataIds.TENANT_2_ID;
                var content = _.clone(Data.VOUCHER_PROVIDER_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            newItemId = responseContent.voucherProvider.id;
                            var shouldResponseContent = _.clone(Data.VOUCHER_PROVIDER_TEST);
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.VOUCHER_PROVIDER_TEST.regionalSettingsIds);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucherProvider, ['id', 'tenantId']);
                        }, next);
                    },
                    
                    // check tenantId was set proper
                    function (next) {
                        VoucherProvider.findOne({ where: { id: newItemId } }).then(function (item) {
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
                        content = { id: newItemId, name: 'name test updated' };
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.voucherProvider.name).be.equal(content.name);
                        }, next);
                    },
                    
                    // change session tenant and try to update an item not belong to session tenant, should return no record found error
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        content = { id: newItemId, name: 'name test' };
                        global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // restore session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        return setImmediate(next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'test voucher provider get/list', function (done) {
                var item = _.clone(Data.VOUCHER_PROVIDER_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                
                async.series([
                    // create tenant 1 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.voucherProvider.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(item.regionalSettingsIds);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucherProvider, ['id', 'tenantId']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.voucherProvider.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(item.regionalSettingsIds);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucherProvider, ['id', 'tenantId']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(itm.regionalSettingsIds);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.voucherProvider, ['tenantId']);
                        }, next);
                    },
                    
                    // list success
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = tenant1Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'name', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(itm.regionalSettingsIds);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'voucherManager/voucherProviderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    
                    // list error
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = tenant2Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'name', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
        });
        
        describe('[' + serie + '] ' + 'voucherGenerate', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherGenerate', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID, amount: 10, code: 'COCA_COLA_BOTTLE', expirationDate: DateUtils.isoFuture() };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherGenerate', ProfileData.CLIENT_INFO_1, content, null, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherGenerateFromFile', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherGenerateFromFile', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID, key: 'voucher.test.csv' };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherGenerateFromFile', ProfileData.CLIENT_INFO_1, content, null, done);
            });
        });

        describe('[' + serie + '] ' + 'voucherCheck', function () {
            it('[' + serie + '] ' + 'SUCCESS: voucherCheck as CronMasterJob', function (done) {
                var content = { id: DataIds.VOUCHER_1_ID, amount: 10, code: 'COCA_COLA_BOTTLE', expirationDate: DateUtils.isoFuture() };
                global.wsHelper.apiSecureSucc(serie, 'voucherManager/voucherGenerate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        Voucher.update({ status: Voucher.constants().STATUS_ACTIVE }, { where: { status: Voucher.constants().STATUS_INACTIVE } }).then(function (count) {
                            var tickCompleteSpy = global.wsHelper.sinon.spy();
                            var voucherCheckJob = require('./../../jobs/voucherCheckJob.js');
                            voucherCheckJob.on(global.wsHelper.cmaster.EVENTS.TICK_COMPLETE, tickCompleteSpy);
                            voucherCheckJob.start();
                            setTimeout(function () {
                                should(tickCompleteSpy.called).be.true;
                                voucherCheckJob.stop();
                                return done();
                            }, 3000); 
                        });
                    }, done);
                });
            });
        });

    });
});