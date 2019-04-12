var _ = require('lodash');
var should = require('should');
var assert = require('chai').assert;
var async = require('async');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/question.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var Language = Database.RdbmsService.Models.Question.Language;
var tenantService = require('../../services/tenantService.js');

describe('WS RegionalSetting API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'regionalSettingUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingUpdate:create', function (done) {
                var content = _.clone(Data.REGIONAL_SETTING_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingCreate', ProfileData.CLIENT_INFO_1, content, function(responseContent) {
                    var shouldResponseContent = _.clone(Data.REGIONAL_SETTING_TEST);
                    shouldResponseContent.languagesIds = ShouldHelper.treatAsList(Data.REGIONAL_SETTING_TEST.languagesIds, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.regionalSetting, ['id']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingUpdate:update', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_1_ID, iso: 'IO', name: 'updated' };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.regionalSetting.name).be.equal(content.name);
                    should(responseContent.regionalSetting.iso).be.equal(content.iso);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION: regionalSettingUpdate:create non existing language', function (done) {
                var content = _.clone(Data.REGIONAL_SETTING_TEST);
                content.id = null;
                content.languagesIds = [DataIds.LANGUAGE_TEST_ID];
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: regionalSettingUpdate:update', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_TEST_ID, name: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'regionalSettingAddLanguage', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingAddLanguage', function (done) {
                var content = {
                    regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
                    languageId: DataIds.LANGUAGE_RU_ID,
                };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingAddLanguage', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.REGIONAL_SETTING_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(_.some(responseContent.languageIds, DataIds.LANGUAGE_RU_ID)).be.true;
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION: regionalSettingAddLanguage', function (done) {
                var content = {
                    regionalSettingId: DataIds.REGIONAL_SETTING_TEST_ID,
                    languageId: DataIds.LANGUAGE_RU_ID,
                };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingAddLanguage', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
        });

        describe('[' + serie + '] ' + 'regionalSettingRemoveLanguage', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingRemoveLanguage', function (done) {
                var content = {
                    regionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
                    languageId: DataIds.LANGUAGE_DE_ID,
                };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingRemoveLanguage', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        content = { id: DataIds.REGIONAL_SETTING_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(_.some(responseContent.languageIds, DataIds.LANGUAGE_DE_ID)).be.false;
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: regionalSettingRemoveLanguage', function (done) {
                var content = {
                    regionalSettingId: DataIds.REGIONAL_SETTING_TEST_ID,
                    languageId: DataIds.LANGUAGE_DE_ID,
                };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingRemoveLanguage', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'regionalSettingDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingDelete', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'question/regionalSettingDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_QAPI_FATAL_ERROR: regionalSettingDelete', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_1_ID };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: regionalSettingDelete', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'regionalSettingGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingGet', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.REGIONAL_SETTING_1);
                    shouldResponseContent.languagesIds = ShouldHelper.treatAsList(Data.REGIONAL_SETTING_1.languagesIds, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.regionalSetting, ['createDate', 'iso']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: regionalSettingGet', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'regionalSettingList', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.REGIONAL_SETTING_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.REGIONAL_SETTING_1);
                    shouldResponseContent.languagesIds = ShouldHelper.treatAsList(Data.REGIONAL_SETTING_1.languagesIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['createDate', 'iso']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: regionalSettingList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.REGIONAL_SETTING_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });
        
        // #1412
        describe('[' + serie + '] ' + 'regionalSettingActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingActivate', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.regionalSetting.status).be.equal('active');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: regionalSettingActivate', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        // #1412
        describe('[' + serie + '] ' + 'regionalSettingDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: regionalSettingDeactivate', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/regionalSettingGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.regionalSetting.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: regionalSettingDeactivate', function (done) {
                var content = { id: DataIds.REGIONAL_SETTING_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/regionalSettingDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
    });
});


describe('WS RegionalSetting Language API', function () {
    global.wsHelper.series().forEach(function (serie) {
        var stub_getSessionTenantId;
        var testSessionTenantId = DataIds.TENANT_1_ID;
        before(function (done) {
            stub_getSessionTenantId = global.wsHelper.sinon.stub(tenantService, 'getSessionTenantId', function (message, clientSession, cb) {
                return cb(false, testSessionTenantId);
            });

            return setImmediate(done);
        });
        after(function (done) {
            stub_getSessionTenantId.restore();
            return setImmediate(done);
        });
        
        describe('[' + serie + '] ' + 'languageUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: create', function (done) {
                var content = _.clone(Data.LANGUAGE_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'question/languageCreate', ProfileData.CLIENT_INFO_1, content, function(responseContent) {
                    should.exist(responseContent.language);
                    should(content.name).be.equal(responseContent.language.name);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: update', function (done) {
                var content = { id: DataIds.LANGUAGE_EN_ID, iso: 'eu', name: 'updated' };
                global.wsHelper.apiSecureSucc(serie, 'question/languageUpdate', ProfileData.CLIENT_INFO_1, content, null, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: update', function (done) {
                var content = { id: DataIds.LANGUAGE_TEST_ID, iso: 'eu', name: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'question/languageUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'languageDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: delete', function (done) {
                var content = _.clone(Data.LANGUAGE_RU);
                content.id = null;
                
                var newLanguage = null;
                global.wsHelper.apiSecureSucc(serie, 'question/languageUpdate', ProfileData.CLIENT_INFO_1, content, function (resContent) {
                    newLanguage = resContent.language;
                }, function () {
                    setImmediate(function (done) {
                        var content = { id: newLanguage.id };
                        global.wsHelper.apiSecureSucc(serie, 'question/languageDelete', ProfileData.CLIENT_INFO_1, content, null, done);
                    }, done);
                });
            });
            // TODO should be adjusted and test the deletion of a language with existing region setting -> should fail
            it('[' + serie + '] ' + 'FAILURE ERR_QAPI_FATAL_ERROR: delete', function (done) {
                var content = { id: DataIds.LANGUAGE_EN_ID };
                global.wsHelper.apiSecureFail(serie, 'question/languageDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: delete', function (done) {
                var content = { id: DataIds.LANGUAGE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/languageDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'languageGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: get', function (done) {
                var content = { id: DataIds.LANGUAGE_EN_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/languageGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.LANGUAGE_EN);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.language, ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: get', function (done) {
                var content = { id: DataIds.LANGUAGE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/languageGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'languageList', function () {
            it('[' + serie + '] ' + 'SUCCESS: list', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.LANGUAGE_EN_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/languageList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.LANGUAGE_EN);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: list', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.LANGUAGE_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/languageList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent).has.property('items').which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'isoList', function () {
            it('[' + serie + '] ' + 'SUCCESS: isoCountries', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'question/isoCountries', ProfileData.CLIENT_INFO_1, null, function (responseContent) {
                    should.exist(responseContent);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: isoLanguages', function (done) {
                global.wsHelper.apiSecureSucc(serie, 'question/isoLanguages', ProfileData.CLIENT_INFO_1, null, function (responseContent) {
                    should.exist(responseContent);
                }, done);
            });
        });
        
        
        
        describe('[' + serie + '] ' + 'language APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test language create/update', function (done) {
                testSessionTenantId = DataIds.TENANT_2_ID;
                var content = _.clone(Data.LANGUAGE_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/languageCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            newItemId = responseContent.language.id;
                            var shouldResponseContent = _.clone(Data.LANGUAGE_TEST);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.language, ['id']);
                        }, next);
                    },
                    
                    // check tenantId was set proper
                    function (next) {
                        Language.findOne({ where: { id: newItemId } }).then(function (item) {
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
                        global.wsHelper.apiSecureSucc(serie, 'question/languageUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.language.name).be.equal(content.name);
                        }, next);
                    },
                    
                    // change session tenant and try to update an item not belong to session tenant, should return no record found error
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var content = { id: newItemId, name: 'name test' };
                        global.wsHelper.apiSecureFail(serie, 'question/languageUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // restore session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        return setImmediate(next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'test language get/list', function (done) {
                //return done();
                var item = _.clone(Data.LANGUAGE_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                
                async.series([
                    // create tenant 1 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'question/languageCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.language.id;
                                var shouldResponseContent = _.clone(item);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.language, ['id']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_2_ID;
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'question/languageCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.language.id;
                                var shouldResponseContent = _.clone(item);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.language, ['id']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'question/languageGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.language);
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
                        global.wsHelper.apiSecureSucc(serie, 'question/languageList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0]);
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        testSessionTenantId = DataIds.TENANT_1_ID;
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'question/languageGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
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
                        global.wsHelper.apiSecureSucc(serie, 'question/languageList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
        });
        
        
        
        
    });
});
