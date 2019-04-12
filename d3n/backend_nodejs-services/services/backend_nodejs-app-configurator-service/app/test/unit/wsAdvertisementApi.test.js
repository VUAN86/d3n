var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/advertisement.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var AerospikeAdvertisementCounter = KeyvalueService.Models.AerospikeAdvertisementCounter;
var Advertisement = Database.RdbmsService.Models.AdvertisementManager.Advertisement;
var AdvertisementProvider = Database.RdbmsService.Models.AdvertisementManager.AdvertisementProvider;
var PublishService = require('../../services/advertisementService.js');
var TenantService = require('../../services/tenantService.js');

describe('WS Advertisement API', function () {
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

        describe('[' + serie + '] ' + 'advertisementUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementUpdate:create', function (done) {
                var content = _.clone(Data.ADVERTISEMENT_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ADVERTISEMENT_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisement, ['id', 'status',
                        'stat_appsUsed', 'stat_earnedCredits', 'stat_gamesUsed', 'stat_views'
                    ]);
                }, done);
            });
            
            it('[' + serie + '] ' + 'SUCCESS: advertisementUpdate:update', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_1_ID, summary: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.advertisement.summary).be.equal(content.summary);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementUpdate:update', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_TEST_ID, summary: 'name test' };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'advertisementActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementActivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.advertisement.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementActivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementDeactivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.advertisement.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementDeactivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementArchive', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementActivate', ProfileData.CLIENT_INFO_1, content, Errors.AdvertisementApi.AdvertisementIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementArchive', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'advertisementDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementDelete', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisement/advertisementDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'advertisement/advertisementGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementDelete', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisement/advertisementDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementGet', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ADVERTISEMENT_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisement, [
                        'stat_appsUsed', 'stat_earnedCredits', 'stat_gamesUsed', 'stat_views'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementGet', function (done) {
                var content = { userId: DataIds.ADVERTISEMENT_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementList', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.ADVERTISEMENT_1_ID },
                    orderBy: [{ field: 'summary', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ADVERTISEMENT_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], [
                        'stat_appsUsed', 'stat_earnedCredits', 'stat_gamesUsed', 'stat_views'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.ADVERTISEMENT_TEST_ID },
                    orderBy: [{ field: 'summary', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content).has.property('items').which.has.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'advertisementProviderUpdate', function () {
            // add tenantId on global session
            beforeEach(function (done) {
                AerospikeGlobalClientSession.removeUserSession(DataIds.LOCAL_USER_ID, function () {
                    var clientGlobalSession = {clientId: DataIds.CLIENT_SESSION_ID, appConfig: { tenantId: DataIds.TENANT_1_ID, device: {a: 'a'}}};
                    
                    // add a client session
                    AerospikeGlobalClientSession.addOrUpdateClientSession(DataIds.LOCAL_USER_ID, clientGlobalSession, function (err) {
                        try {
                            should.ifError(err);
                            return done();
                        } catch (e) {
                            return done(e);
                        }
                    });
                });
            });
            
            it('[' + serie + '] ' + 'SUCCESS: advertisementProviderUpdate:create', function (done) {
                var content = _.clone(Data.ADVERTISEMENT_PROVIDER_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ADVERTISEMENT_PROVIDER_TEST);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.ADVERTISEMENT_PROVIDER_TEST.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisementProvider, ['id', 'tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: advertisementProviderUpdate:update', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_1_ID, name: 'name test' };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.advertisementProvider.name).be.equal(content.name);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementProviderUpdate:update', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID, name: 'name test' };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'advertisementProviderActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementProviderActivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.advertisementProvider.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementProviderActivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementProviderDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementProviderDeactivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_2_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.advertisementProvider.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementProviderDeactivate', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementProviderArchive', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementProviderArchive', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_INACTIVE_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderArchive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderActivate', ProfileData.CLIENT_INFO_1, content, Errors.AdvertisementApi.AdvertisementProviderIsArchived, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementProviderArchive', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderArchive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementProviderDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementProviderDelete', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisement/advertisementProviderDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'advertisement/advertisementProviderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION: advertisementProviderDelete', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_1_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisement/advertisementProviderDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementProviderDelete', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisement/advertisementProviderDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementProviderGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: advertisementProviderGet', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.ADVERTISEMENT_PROVIDER_1);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.ADVERTISEMENT_PROVIDER_1.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisementProvider, ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: advertisementProviderGet', function (done) {
                var content = { id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'advertisementProviderList', function () {
            it('[' + serie + '] ' + 'SUCCESS: list one', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.ADVERTISEMENT_PROVIDER_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent).has.property('items').which.has.length(1);
                    var shouldResponseContent = _.clone(Data.ADVERTISEMENT_PROVIDER_1);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.ADVERTISEMENT_PROVIDER_1.regionalSettingsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                }, done);
            });
            
            it('[' + serie + '] ' + 'SUCCESS: list all', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0
                };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    assert.isAtLeast(responseContent.items.length, 2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCC ERR_DATABASE_NO_RECORD_FOUND: list', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.ADVERTISEMENT_PROVIDER_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('items').which.has.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'advertisement provider APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test advertisement provider create/update', function (done) {
                var testSessionTenantId = ProfileData.CLIENT_INFO_2.appConfig.tenantId; //DataIds.TENANT_2_ID;
                var content = _.clone(Data.ADVERTISEMENT_PROVIDER_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderCreate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            newItemId = responseContent.advertisementProvider.id;
                            var shouldResponseContent = _.clone(Data.ADVERTISEMENT_PROVIDER_TEST);
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.ADVERTISEMENT_PROVIDER_TEST.regionalSettingsIds);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisementProvider, ['id', 'tenantId']);
                        }, next);
                    },
                    
                    // check tenantId was set proper
                    function (next) {
                        AdvertisementProvider.findOne({ where: { id: newItemId } }).then(function (item) {
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
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderUpdate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            should(responseContent.advertisementProvider.name).be.equal(content.name);
                        }, next);
                    },
                    
                    // change session tenant and try to update an item not belong to session tenant, should return no record found error
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_1_ID;
                        var content = { id: newItemId, name: 'name test' };
                        global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // restore session tenant
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_2_ID;
                        return setImmediate(next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'test advertisement provider get/list', function (done) { 
                var item = _.clone(Data.ADVERTISEMENT_PROVIDER_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                var testSessionTenantId;
                async.series([
                    // create tenant 1 items
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_1_ID;
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.advertisementProvider.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(item.regionalSettingsIds);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisementProvider, ['id', 'tenantId']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_2_ID;
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderCreate', ProfileData.CLIENT_INFO_2, item, function (responseContent) {
                                item.id = responseContent.advertisementProvider.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(item.regionalSettingsIds);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisementProvider, ['id', 'tenantId']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(itm.regionalSettingsIds);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.advertisementProvider, ['tenantId']);
                        }, next);
                    },
                    
                    // list success
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = tenant1Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'name', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(itm.regionalSettingsIds);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementProviderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    
                    // list error
                    function (next) {
                        //testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = tenant2Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'name', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementProviderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
            
        });
        
        
        describe('[' + serie + '] ' + 'advertisement activate/deactivate API', function () {
            this.timeout(60000);
            var dbAdsToDelete = [];
            beforeEach(function (done) {
                var items = [Data.ADVERTISEMENT_1, Data.ADVERTISEMENT_2, Data.ADVERTISEMENT_TEST];

                async.mapSeries(items, function (item, cbItem) {
                    AerospikeAdvertisementCounter.remove(item, function (err) {
                        console.log('err:', err);
                        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                            return cbItem(err);
                        }
                        return cbItem();
                    });
                }, done);
            });
            
            beforeEach(function (done) {
                var s3Client = PublishService.getS3Client();
                s3Client.listObjects({
                    Prefix: 'provider_'
                }, function (err, result) {
                    if (err) {
                        return done(err);
                    }
                    
                    if (!result.Contents.length) {
                        return done();
                    }
                    var keys = [];
                    for(var i=0; i<result.Contents.length; i++) {
                        keys.push(result.Contents[i].Key);
                    }
                    //console.log('>>>>keys:', keys);
                    s3Client.deleteObjects({
                        objectKeys: keys
                    }, done);
                });
                
            });
            
            afterEach(function (done) {
                async.mapSeries(dbAdsToDelete, function (item, cbItem) {
                    Advertisement.destroy({ where: { id: item.id } }).then(function (count) {
                        return cbItem();
                    }).catch(function (err) {
                        return cbItem(err);
                    });
                }, done);
            });
            
            
            it('[' + serie + '] ' + 'SUCCESS: advertisementActivate/advertisementDeactivate', function (done) {
                var ad1 = _.clone(Data.ADVERTISEMENT_TEST);
                var ad2 = _.clone(Data.ADVERTISEMENT_TEST);
                var ad3 = _.clone(Data.ADVERTISEMENT_TEST);
                var ad4 = _.clone(Data.ADVERTISEMENT_TEST);
                var ad5 = _.clone(Data.ADVERTISEMENT_TEST);
                
                var ads = [ad1, ad2, ad3, ad4, ad5];
                var adProvider1 = _.clone(Data.ADVERTISEMENT_TEST);
                adProvider1.advertisementProviderId = DataIds.ADVERTISEMENT_PROVIDER_1_ID;
                //adProvider2.
                //var dbAds = [];
                
                function publishStateNothing(providerId, cb) {
                    async.series([
                        // nothing on mysql
                        function (next) {
                            Advertisement.findAll({
                                attributes: [[Advertisement.sequelize.fn('COUNT', Advertisement.sequelize.col('id')), 'cntIds']],
                                where: {
                                    publishIdx: {
                                        $not: null
                                    },
                                    advertisementProviderId: providerId
                                }
                            }).then(function (items) {
                                var item = items[0].get({plain: true});
                                try {
                                    assert.strictEqual(item.cntIds, 0);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                return next(err);
                            });
                        },
                        
                        // nothing on aerospike
                        function (next) {
                            AerospikeAdvertisementCounter.getCounter({advertisementProviderId: providerId, id: 1}, function (err, counter) {
                                try {
                                    assert.ifError(err);
                                    assert.strictEqual(counter, 0);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        },
                        
                        // nothing in cdn
                        function (next) {
                            var s3Client = PublishService.getS3Client();
                            s3Client.listObjects({
                                Prefix: 'provider_' + providerId
                            }, function (err, result) {
                                try {
                                    assert.ifError(err);
                                    assert.strictEqual(result.Contents.length, 0);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        }
                    ], cb);
                    
                };
                
                function publishStateOk(dbItemsIds, cb) {
                    var dbItems = [];
                    async.series([
                        // get items from mysql
                        function (next) {
                            Advertisement.findAll({
                                where: {
                                    id: {
                                        $in: dbItemsIds
                                    }
                                },
                                order: 'id ASC'
                            }).then(function (items) {
                                try {
                                    for(var i=0; i<items.length; i++) {
                                        var item = items[i].get({plain: true});
                                        dbItems.push(item);
                                    }
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                return next(err);
                            });
                        },
                        // aerospike counter ok
                        function (next) {
                            AerospikeAdvertisementCounter.getCounter(dbItems[0], function (err, counter) {
                                try {
                                    assert.ifError(err);
                                    assert.strictEqual(counter, dbItems.length);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        },
                        // cdn count ok
                        function (next) {
                            var s3Client = PublishService.getS3Client();
                            s3Client.listObjects({
                                Prefix: 'provider_' + dbItems[0].advertisementProviderId
                            }, function (err, result) {
                                try {
                                    assert.ifError(err);
                                    assert.strictEqual(result.Contents.length, dbItems.length);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        },
                        
                        // cdn files ok
                        function (next) {
                            var s3Client = PublishService.getS3Client();
                            async.mapSeries(dbItems, function (item, cbItem) {
                                var fileName = PublishService.buildPublishKey(item);
                                s3Client.getObjectAsBuffer({
                                    objectId: fileName
                                }, function (err, data) {
                                    try {
                                        assert.ifError(err);
                                        var s3Object = JSON.parse(data.toString('utf8'));
                                        assert.strictEqual(s3Object.id, item.id);
                                        return cbItem(err);
                                    } catch (e) {
                                        return cbItem(e);
                                    }
                                });
                            }, next);
                        }
                    ], cb);
                };
                
                async.series([
                    // create a few ads
                    function (next) {
                        async.mapSeries(ads, function (ad, cbItem) {
                            ad.id = null;
                            global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementCreate', Data.LOCAL_TOKEN, ad, function (responseContent) {
                                ad.id = responseContent.advertisement.id;
                            }, cbItem);
                        }, next);
                    },
                    // create ad with a different provider id
                    function (next) {
                        adProvider1.id = null;
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementCreate', Data.LOCAL_TOKEN, adProvider1, function (responseContent) {
                            adProvider1.id = responseContent.advertisement.id;
                        }, next);
                    },
                    // publish the ad
                    function (next) {
                        async.mapSeries([adProvider1], function (ad, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementActivate', Data.LOCAL_TOKEN, {id: ad.id}, function (responseContent) {
                            }, cbItem);
                        }, next);
                    },
                    
                    // publish the ads
                    function (next) {
                        async.mapSeries(ads, function (ad, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementActivate', Data.LOCAL_TOKEN, {id: ad.id}, function (responseContent) {
                            }, cbItem);
                        }, next);
                    },
                    
                    function (next) {
                        publishStateOk(ads.map(function (item) {return item.id;}), next);
                    },
                    
                    
                    // unpublish an item from the middle
                    function (next) {
                        var idx = Math.ceil((ads.length-1)/2);
                        var unpublishItem = ads.splice(idx, 1)[0];
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementDeactivate', Data.LOCAL_TOKEN, {id: unpublishItem.id}, function (responseContent) {
                        }, function (err) {
                            if (err) {
                                return next(err);
                            }
                            
                            publishStateOk(ads.map(function (item) {return item.id;}), next);
                        });
                    },
                    
                    // unpublish an item from the end
                    function (next) {
                        var idx = ads.length-1;
                        var unpublishItem = ads.splice(idx, 1)[0];
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementDeactivate', Data.LOCAL_TOKEN, {id: unpublishItem.id}, function (responseContent) {
                        }, function (err) {
                            if (err) {
                                return next(err);
                            }
                            
                            publishStateOk(ads.map(function (item) {return item.id;}), next);
                        });
                    },
                    
                    // unpublish an item from the begining
                    function (next) {
                        var idx = 0;
                        var unpublishItem = ads.splice(idx, 1)[0];
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementDeactivate', Data.LOCAL_TOKEN, {id: unpublishItem.id}, function (responseContent) {
                        }, function (err) {
                            if (err) {
                                return next(err);
                            }
                            
                            publishStateOk(ads.map(function (item) {return item.id;}), next);
                        });
                    },
                    // unpublish an item from the begining
                    function (next) {
                        var idx = 0;
                        var unpublishItem = ads.splice(idx, 1)[0];
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementDeactivate', Data.LOCAL_TOKEN, {id: unpublishItem.id}, function (responseContent) {
                        }, function (err) {
                            if (err) {
                                return next(err);
                            }
                            
                            publishStateOk(ads.map(function (item) {return item.id;}), next);
                        });
                    },
                    
                    // unpublish last existing published item
                    function (next) {
                        var idx = 0;
                        var unpublishItem = ads.splice(idx, 1)[0];
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementDeactivate', Data.LOCAL_TOKEN, {id: unpublishItem.id}, function (responseContent) {
                        }, function (err) {
                            if (err) {
                                return next(err);
                            }
                            
                            publishStateNothing(unpublishItem.advertisementProviderId, next);
                        });
                    },
                    
                    // clean ups, delete created ads
                    function (next) {
                        async.mapSeries(ads.concat([adProvider1]), function (item, cbItem) {
                            Advertisement.destroy({ where: { id: item.id } }).then(function (count) {
                                return cbItem();
                            }).catch(function (err) {
                                return cbItem(err);
                            });
                        }, next);
                    }
                ], done);
                
            });
            
            
            it('[' + serie + '] ' + 'ERROR: trying to unpublish a non published ad', function (done) {
                var ad = _.clone(Data.ADVERTISEMENT_TEST);
                async.series([
                    function (next) {
                        ad.id = null;
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementCreate', Data.LOCAL_TOKEN, ad, function (responseContent) {
                            ad.id = responseContent.advertisement.id;
                            dbAdsToDelete.push(ad);
                        }, next);
                    },
                    
                    function (next) {
                        global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementDeactivate', ProfileData.CLIENT_INFO_1, ad, Errors.DatabaseApi.ValidationFailed, next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'ERROR: trying to delete a published ad', function (done) {
                var ad = _.clone(Data.ADVERTISEMENT_TEST);
                async.series([
                    function (next) {
                        ad.id = null;
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementCreate', Data.LOCAL_TOKEN, ad, function (responseContent) {
                            ad.id = responseContent.advertisement.id;
                            dbAdsToDelete.push(ad);
                        }, next);
                    },
                    
                    // publish
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementActivate', Data.LOCAL_TOKEN, {id: ad.id}, function (responseContent) {
                        }, next);
                    },
                    
                    // try to delete delete
                    function (next) {
                        global.wsHelper.apiSecureFail(serie, 'advertisementManager/advertisementDelete', ProfileData.CLIENT_INFO_1, {id: ad.id}, Errors.AdvertisementApi.ContentIsPublished, next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'SUCCESS: re-publish by update', function (done) {
                var ad = _.clone(Data.ADVERTISEMENT_TEST);
                async.series([
                    function (next) {
                        ad.id = null;
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementCreate', Data.LOCAL_TOKEN, ad, function (responseContent) {
                            ad.id = responseContent.advertisement.id;
                            dbAdsToDelete.push(ad);
                        }, next);
                    },
                    // counter 0
                    function (next) {
                        AerospikeAdvertisementCounter.getCounter(ad, function (err, counter) {
                            try {
                                assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    
                    // publish
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementActivate', Data.LOCAL_TOKEN, {id: ad.id}, function (responseContent) {
                        }, next);
                    },
                    // re-pubish by updating a published ad
                    function (next) {
                        ad.summary = 'new summary re-publish';
                        global.wsHelper.apiSecureSucc(serie, 'advertisementManager/advertisementUpdate', ProfileData.CLIENT_INFO_1, ad, function (responseContent) {
                        }, next);
                    },
                    function (next) {
                        AerospikeAdvertisementCounter.getCounter(ad, function (err, counter) {
                            try {
                                assert.ifError(err);
                                assert.strictEqual(counter, 1);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    
                    // check cdn file updated accordingly
                    function (next) {
                        Advertisement.findOne({
                            where: {
                                id: ad.id
                            }
                        }).then(function (item) {
                            try {
                                item = item.get({plain: true});
                                //console.log('>>>item:', item);
                                var s3Client = PublishService.getS3Client();
                                var fileName = PublishService.buildPublishKey(item);
                                s3Client.getObjectAsBuffer({
                                    objectId: fileName
                                }, function (err, data) {
                                    try {
                                        assert.ifError(err);
                                        var s3Object = JSON.parse(data.toString('utf8'));
                                        assert.strictEqual(s3Object.id, item.id);
                                        assert.strictEqual(s3Object.summary, ad.summary);
                                        return next(err);
                                    } catch (e) {
                                        return next(e);
                                    }
                                });
                            } catch (e) {
                                return next(e);
                            }
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    }
                ], done);
            });
        });
    });
});
