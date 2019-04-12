var _ = require('lodash');
var should = require('should');
var assert = require('chai').assert;
var async = require('async');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/questionTemplate.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var tenantService = require('../../services/tenantService.js');

describe('WS QuestionTemplate API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'questionTemplateUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateUpdate create', function (done) {
                var content = _.clone(Data.QUESTION_TEMPLATE_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['tenantId', 'answerTemplates']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateUpdate update', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NEW_ID, name: 'updated' };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_NEW);
                    shouldResponseContent.name = content.name;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['tenantId', 'answerTemplates']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateUpdate update', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_TEST_ID, name: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateDelete', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES questionTemplateDelete', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NEW_ID };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateDelete', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateGet', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NEW_ID };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_NEW);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['tenantId', 'answerTemplates']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateGet', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateList', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.QUESTION_TEMPLATE_NEW_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_NEW);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId', 'answerTemplates']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.QUESTION_TEMPLATE_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateList', ProfileData.CLIENT_INFO_1, content, null, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateActivate', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NEW_ID };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_NEW);
                            shouldResponseContent.status = 'active';
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['tenantId', 'answerTemplates']);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateActivate', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateDeactivate', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NEW_ID };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_NEW);
                            shouldResponseContent.status = 'inactive';
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['tenantId', 'answerTemplates']);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateDeactivate', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateGetHelp', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateGetHelp', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NEW_ID };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateGetHelp', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(Data.QUESTION_TEMPLATE_NEW.help).be.equal(responseContent.help);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateGetHelp', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateGetHelp', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateQuestionStatistic', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateQuestionStatistic', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_NEW_ID };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateQuestionStatistic', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_NEW);
                    shouldResponseContent.questionCount = 1;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplateQuestionStatistic, ['tenantId', 'answerTemplates']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateQuestionStatistic', function (done) {
                var content = { id: DataIds.QUESTION_TEMPLATE_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateQuestionStatistic', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTemplateQuestionStatisticList', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTemplateQuestionStatisticList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.QUESTION_TEMPLATE_NEW_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateQuestionStatisticList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_NEW);
                    shouldResponseContent.questionCount = 1;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId', 'answerTemplates']);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTemplateQuestionStatisticList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.QUESTION_TEMPLATE_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateQuestionStatisticList', ProfileData.CLIENT_INFO_1, content, null, done);
            });
        });
        
        describe('[' + serie + '] ' + 'question template APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test question template create/update', function (done) {
                var testSessionTenantId = ProfileData.CLIENT_INFO_2.appConfig.tenantId;
                var content = _.clone(Data.QUESTION_TEMPLATE_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateCreate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            newItemId = responseContent.questionTemplate.id;
                            var shouldResponseContent = _.clone(Data.QUESTION_TEMPLATE_TEST);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['id', 'answerTemplates']);
                        }, next);
                    },
                    
                    // check tenantId was set proper
                    function (next) {
                        QuestionTemplate.findOne({ where: { id: newItemId } }).then(function (item) {
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
                        global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateUpdate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            should(responseContent.questionTemplate.name).be.equal(content.name);
                        }, next);
                    },
                    
                    // change session tenant and try to update an item not belong to session tenant, should return no record found error
                    function (next) {
                        var content = { id: newItemId, name: 'name test' };
                        global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // restore session tenant
                    function (next) {
                        return setImmediate(next);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'test question template get/list', function (done) {
                //return done();
                var item = _.clone(Data.QUESTION_TEMPLATE_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                
                async.series([
                    // create tenant 1 items
                    function (next) {
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.questionTemplate.id;
                                var shouldResponseContent = _.clone(item);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['id', 'answerTemplates']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateCreate', ProfileData.CLIENT_INFO_2, item, function (responseContent) {
                                item.id = responseContent.questionTemplate.id;
                                var shouldResponseContent = _.clone(item);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['id', 'answerTemplates']);
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTemplate, ['answerTemplates']);
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
                        global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = itm;
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['answerTemplates']);
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'questionTemplate/questionTemplateGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
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
                        global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
        });
        
        
    });
});
