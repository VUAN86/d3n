var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/workorder.data.js');
var QuestionData = require('./../config/question.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var WorkorderHasResource = Database.RdbmsService.Models.Workorder.WorkorderHasResource;
var WorkorderHasTenant = Database.RdbmsService.Models.Workorder.WorkorderHasTenant;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var tenantService = require('../../services/tenantService.js');

describe('WS Workorder API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'workorderUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderUpdate create', function (done) {
                var content = _.clone(Data.WORKORDER_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WORKORDER_TEST);
                    shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(Data.WORKORDER_TEST.questionsExamples, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.WORKORDER_TEST.poolsIds, 0, 0);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.WORKORDER_TEST.regionalSettingsIds, 0, 0);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList([], 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorder, ['createDate', 'id',
                        'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                        'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                        'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                        'stat_questionsInProgress', 'stat_questionsInTranslation',
                        'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                        'stat_translationsPublished'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: workorderUpdate update', function (done) {
                var content = { id: DataIds.WORKORDER_1_ID, description: 'updated', poolsIds: Data.WORKORDER_2.poolsIds };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WORKORDER_1);
                    shouldResponseContent.description = content.description;
                    shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsExamples, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(content.poolsIds, 0, 0);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.regionalSettingsIds, 0, 0);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsIds, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorder, ['createDate', 'id',
                        'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                        'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                        'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                        'stat_questionsInProgress', 'stat_questionsInTranslation',
                        'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                        'stat_translationsPublished'
                    ]);
                }, done);
            });
            // #1394
            it('[' + serie + '] ' + 'SUCCESS: workorderUpdate create question example for new workorder', function (done) {
                var content = _.clone(QuestionData.QUESTION_TEST);
                content.id = null;
                var receivedQuestionContent = null;
                // create new question and get its content back
                global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent)
                        .has.property('question')
                        .which.have.property('id')
                        .which.is.a.Number();
                    receivedQuestionContent = responseContent;
                }, function () {
                    setImmediate(function (done) {
                        content = _.clone(Data.WORKORDER_TEST);
                        content.id = null;
                        content.questionsExamples = [{ id: receivedQuestionContent.question.id, number: 20 }];
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('workorder')
                                .which.has.property('questionsExamples')
                                .which.has.property('items')
                                .which.has.length(1);
                            responseContent.workorder.questionsExamples.items[0].should.have.property('id').which.is.equal(DataIds.QUESTION_TEST_ID);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: workorderUpdate create (community promotion)', function (done) {
                var content = _.cloneDeep(Data.WORKORDER_TEST);
                content.id = null;
                content.isCommunityPromoted = 1;
                content.communityPromotionFromDate = DateUtils.isoFuture();
                content.communityPromotionToDate = DateUtils.isoNow();
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: workorderUpdate update (community promotion)', function (done) {
                var content = {
                    id: DataIds.WORKORDER_1_ID,
                    isCommunityPromoted: 1,
                    communityPromotionFromDate: DateUtils.isoNow()
                };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderUpdate update', function (done) {
                var content = { id: DataIds.WORKORDER_TEST_ID, title: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'workorderDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderDelete', function (done) {
                var content = { id: DataIds.WORKORDER_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_QAPI_FATAL_ERROR: workorderDelete', function (done) {
                var content = { id: DataIds.WORKORDER_1_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderDelete', function (done) {
                var content = { id: DataIds.WORKORDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'workorderGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderGet', function (done) {
                var content = { id: DataIds.WORKORDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WORKORDER_1);
                    shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsExamples, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.poolsIds, 0, 0);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.regionalSettingsIds, 0, 0);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsIds, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorder, ['createDate',
                        'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                        'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                        'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                        'stat_questionsInProgress', 'stat_questionsInTranslation',
                        'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                        'stat_translationsPublished'
                    ]);
                }, done);
            });
            // #1391, #1392
            it('[' + serie + '] ' + 'SUCCESS: questions filter', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { workorderId: DataIds.WORKORDER_1_ID },
                    orderBy: []
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent)
                        .has.property('items')
                        .which.has.length(1);
                    receivedContent.items[0].should.have.property('id').which.is.equal(DataIds.QUESTION_1_ID);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderGet', function (done) {
                var content = { id: DataIds.WORKORDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'workorderList', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.WORKORDER_1_ID, questionsExamples: [{ id: DataIds.QUESTION_1_ID, number: 20 }] },
                    orderBy: [{ field: 'status', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WORKORDER_1);
                    shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsExamples);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.poolsIds);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.regionalSettingsIds);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsIds);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['createDate',
                        'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                        'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                        'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                        'stat_questionsInProgress', 'stat_questionsInTranslation',
                        'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                        'stat_translationsPublished'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE: workorderList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.WORKORDER_TEST_ID },
                    orderBy: [{ field: 'status', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'workorderByResourceList', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderByResourceList', function (done) {
                var content = {
                    limit: 20,
                    offset: 0,
                    resourceId: DataIds.LOCAL_USER_ID
                };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderByResourceList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = [
                        {
                            workorderId: DataIds.WORKORDER_1_ID,
                            actions: [WorkorderHasResource.constants().ACTION_QUESTION_CREATE, WorkorderHasResource.constants().ACTION_QUESTION_REVIEW]
                        }
                    ];
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items);
                }, done);
            });
        });

        // #1417
        describe('[' + serie + '] ' + 'workorderActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderActivate', function (done) {
                var content = { id: DataIds.WORKORDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.workorder.status).be.equal('inprogress');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_WORKORDER_HAS_NO_POOL: workorderActivate - no pool', function (done) {
                var content = { id: DataIds.WORKORDER_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderActivate', ProfileData.CLIENT_INFO_1, content, Errors.WorkorderApi.WorkorderHasNoPool, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderActivate', function (done) {
                var content = { id: DataIds.WORKORDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        // #1418
        describe('[' + serie + '] ' + 'workorderDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderDeactivate', function (done) {
                var content = { id: DataIds.WORKORDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.workorder.status).be.equal('inactive');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderDeactivate', function (done) {
                var content = { id: DataIds.WORKORDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        // #1410
        describe('[' + serie + '] ' + 'workorderClose', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderClose', function (done) {
                var content = { id: DataIds.WORKORDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderClose', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        var content = { id: DataIds.WORKORDER_1_ID };
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                            should.exist(receivedContent);
                            should(receivedContent)
                                .has.property('workorder')
                                .which.have.property('status')
                                .which.is.equal('closed');
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderClose', function (done) {
                var content = { id: DataIds.WORKORDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderClose', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        // #1403
        describe('[' + serie + '] ' + 'workorderAssignRemoveResources', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderAssignRemoveResources', function (done) {
                var content = {
                    id: DataIds.WORKORDER_1_ID,
                    removes: [DataIds.LOCAL_USER_ID],
                    assigns: [DataIds.FACEBOOK_USER_ID],
                    action: WorkorderHasResource.constants().ACTION_QUESTION_CREATE,
                };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderAssignRemoveResources', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderGetResources', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.not.exist(_.find(responseContent.resources, {
                                resourceId: content.removes[0], action: content.action
                            }));
                            should.exist(_.find(responseContent.resources, {
                                resourceId: content.assigns[0], action: content.action
                            }));
                        }, done);
                    }, done);
                });
            });
        });

        describe('[' + serie + '] ' + 'workorderQuestionStatistic', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderQuestionStatistic', function (done) {
                var content = { id: DataIds.WORKORDER_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderQuestionStatistic', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WORKORDER_1);
                    shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsExamples, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.poolsIds, 0, 0);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.regionalSettingsIds, 0, 0);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsIds, 0, 0);
                    shouldResponseContent.draftedQuestionCount = 1;
                    shouldResponseContent.submittedQuestionCount = 0;
                    shouldResponseContent.completedQuestionCount = 0;
                    shouldResponseContent.totalQuestionCount = 1;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorderQuestionStatistic, ['createDate',
                        'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                        'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                        'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                        'stat_questionsInProgress', 'stat_questionsInTranslation',
                        'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                        'stat_translationsPublished'    
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderQuestionStatistic', function (done) {
                var content = { id: DataIds.WORKORDER_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'workorder/workorderQuestionStatistic', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'workorderQuestionStatisticList', function () {
            it('[' + serie + '] ' + 'SUCCESS: workorderQuestionStatisticList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.WORKORDER_1_ID },
                    orderBy: [{ field: 'status', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderQuestionStatisticList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.WORKORDER_1);
                    shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsExamples);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.poolsIds);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.regionalSettingsIds);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.WORKORDER_1.questionsIds);
                    shouldResponseContent.draftedQuestionCount = 1;
                    shouldResponseContent.submittedQuestionCount = 0;
                    shouldResponseContent.completedQuestionCount = 0;
                    shouldResponseContent.totalQuestionCount = 1;
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['createDate', 
                        'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                        'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                        'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                        'stat_questionsInProgress', 'stat_questionsInTranslation',
                        'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                        'stat_translationsPublished'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: workorderQuestionStatisticList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.WORKORDER_TEST_ID },
                    orderBy: [{ field: 'status', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'workorder/workorderQuestionStatisticList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });
        
        
        describe('[' + serie + '] ' + 'workorder APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test workorder provider create/update', function (done) {
                //return done();
                var testSessionTenantId = ProfileData.CLIENT_INFO_2.appConfig.tenantId;
                var content = _.clone(Data.WORKORDER_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderCreate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            newItemId = responseContent.workorder.id;
                            var shouldResponseContent = _.clone(Data.WORKORDER_TEST);
                            shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(Data.WORKORDER_TEST.questionsExamples, 0, 0);
                            shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.WORKORDER_TEST.poolsIds, 0, 0);
                            shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.WORKORDER_TEST.regionalSettingsIds, 0, 0);
                            shouldResponseContent.questionsIds = ShouldHelper.treatAsList([], 0, 0);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorder, ['createDate', 'id',
                                    'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                                    'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                                    'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                                    'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                                    'stat_questionsInProgress', 'stat_questionsInTranslation',
                                    'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                                    'stat_translationsPublished'
                            ]);
                        }, next);
                    },
                    // check tenantId is set properly
                    function (next) {
                        WorkorderHasTenant.findOne({ where: { workorderId: newItemId, tenantId: testSessionTenantId } }).then(function (item) {
                            try {
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
                        var content = { id: newItemId, title: 'new title' };
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderUpdate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            should(responseContent.workorder.title).be.equal(content.title);
                        }, next);
                    },
                    
                    // check tenantId is set properly
                    function (next) {
                        WorkorderHasTenant.findOne({ where: { workorderId: newItemId, tenantId: testSessionTenantId } }).then(function (item) {
                            try {
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            return next(err);
                        });
                    }
                ], done);
            });
             
            it('[' + serie + '] ' + 'test workorder  get/list', function (done) {
                var item = _.clone(Data.WORKORDER_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                var testSessionTenantId;
                async.series([
                    // create tenant 1 items
                    function (next) {
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.workorder.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(item.questionsExamples, 0, 0);
                                shouldResponseContent.poolsIds = ShouldHelper.treatAsList(item.poolsIds, 0, 0);
                                shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(item.regionalSettingsIds, 0, 0);
                                shouldResponseContent.questionsIds = ShouldHelper.treatAsList([], 0, 0);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorder, ['createDate', 'id',
                                    'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                                    'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                                    'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                                    'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                                    'stat_questionsInProgress', 'stat_questionsInTranslation',
                                    'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                                    'stat_translationsPublished'
                                ]);
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderCreate', ProfileData.CLIENT_INFO_2, item, function (responseContent) {
                                item.id = responseContent.workorder.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.questionsExamples = ShouldHelper.treatAsList(item.questionsExamples, 0, 0);
                                shouldResponseContent.poolsIds = ShouldHelper.treatAsList(item.poolsIds, 0, 0);
                                shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(item.regionalSettingsIds, 0, 0);
                                shouldResponseContent.questionsIds = ShouldHelper.treatAsList([], 0, 0);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.workorder, ['createDate', 'id',
                                    'stat_numberOfLanguages', 'stat_progressPercentage', 'stat_questionTranslationsCompleted',
                                    'stat_questionTranslationsInProgress', 'stat_questionsCompleted', 
                                    'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2',
                                    'stat_questionsComplexityLevel3', 'stat_questionsComplexityLevel4',
                                    'stat_questionsInProgress', 'stat_questionsInTranslation',
                                    'stat_questionsPublished', 'stat_questionsRequested', 'stat_translationsProgressPercentage',
                                    'stat_translationsPublished'
                                ]);
                            }, cbItem);
                       }, next);
                    },
                    // get success
                    function (next) {
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            //assert.strictEqual(responseContent.workorder);
                        }, next);
                    },
                    
                    // list success
                    function (next) {
                        var itm = tenant1Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'title', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'workorder/workorderGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    },
                    // list error
                    function (next) {
                        var itm = tenant2Items[0];
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'title', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'workorder/workorderList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    },
                    // delete items
                    function (next) {
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderDelete', ProfileData.CLIENT_INFO_1, {id: item.id}, function (responseContent) {
                            }, cbItem);
                       }, next);
                    },
                    // delete items
                    function (next) {
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'workorder/workorderDelete', ProfileData.CLIENT_INFO_2, {id: item.id}, function (responseContent) {
                            }, cbItem);
                       }, next);
                    }
                    
                ], done);
            });
            
        });
        
    });
});
