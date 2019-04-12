var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var should = require('should');
var assert = require('assert');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/question.data.js');
var WorkorderData = require('./../config/workorder.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionTranslationApiFactory = require('../../factories/questionTranslationApiFactory');
var Tag = Database.RdbmsService.Models.Question.Tag;
var QuestionHasTag = Database.RdbmsService.Models.Question.QuestionHasTag;
var PoolHasTag = Database.RdbmsService.Models.Question.PoolHasTag;
var MediaHasTag = Database.RdbmsService.Models.Media.MediaHasTag;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var QuestionService = require('./../../services/questionService.js');
var CryptoService = require('./../../services/cryptoService.js');
var PublishService = require('./../../services/publishService.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikePool = KeyvalueService.Models.AerospikePool;
var AerospikePoolMeta = KeyvalueService.Models.AerospikePoolMeta;
var AerospikePoolIndex = KeyvalueService.Models.AerospikePoolIndex;
var AerospikePoolIndexMeta = KeyvalueService.Models.AerospikePoolIndexMeta;

describe('WS Question API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
       
        //describe('[' + serie + '] ' + 'questionUpdate status', function () {
        //    it('[' + serie + '] ' + 'SUCCESS: questionUpdate, update status as draft', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'draft' };
        //        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
        //            should(responseContent.question.status).be.equal(content.status);
        //        }, done);
        //    });
        //    it('[' + serie + '] ' + 'SUCCESS: questionUpdate, update status as review', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'review' };
        //        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
        //            should(responseContent.question.status).be.equal(content.status);
        //        }, done);
        //    });
        //    it('[' + serie + '] ' + 'SUCCESS: questionUpdate, update status as approved', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'approved' };
        //        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
        //            should(responseContent.question.status).be.equal(content.status);
        //        }, done);
        //    });
        //    it('[' + serie + '] ' + 'SUCCESS: questionUpdate, update status as declined', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'declined' };
        //        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
        //            should(responseContent.question.status).be.equal(content.status);
        //        }, done);
        //    });
        //    it('[' + serie + '] ' + 'SUCCESS: questionUpdate, update status as unpublished', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'unpublished' };
        //        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
        //            should(responseContent.question.status).be.equal(content.status);
        //        }, done);
        //    });
        //    it('[' + serie + '] ' + 'SUCCESS: questionUpdate, update status as published', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'published' };
        //        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
        //            should(responseContent.question.status).be.equal(content.status);
        //        }, done);
        //    });
        //    it('[' + serie + '] ' + 'SUCCESS: questionUpdate, update status as archived', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'archived' };
        //        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
        //            should(responseContent.question.status).be.equal(content.status);
        //        }, done);
        //    });
        //    it('[' + serie + '] ' + 'ERROR: questionUpdate, update status as not_allowed_status', function (done) {
        //        var content = { id: DataIds.QUESTION_1_ID, status: 'not_allowed_status' };
        //        global.wsHelper.apiSecureCall(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (err, response) {
        //            try {
        //                assert.ifError(err);
        //                assert.notStrictEqual(response.getError(), null);
        //                done();
        //            } catch (e) {
        //                return done(e);
        //            }
        //        });
        //    });
        //});
        
        describe('[' + serie + '] ' + 'questionUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionUpdate:create', function (done) {
                var content = _.clone(Data.QUESTION_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TEST);
                    // pools, regional settings are appended from workorder, primary regional setting is copied from workorder
                    shouldResponseContent.primaryRegionalSettingId = WorkorderData.WORKORDER_1.primaryRegionalSettingId;
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList([DataIds.REGIONAL_SETTING_1_ID, DataIds.REGIONAL_SETTING_2_ID], 0, 0);
                    shouldResponseContent.relatedQuestionsIds = ShouldHelper.treatAsList(Data.QUESTION_1.relatedQuestionsIds, 0, 0);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.QUESTION_TEST.tags, 0, 0);
                    should(responseContent.question.updaterResourceId).be.equal(ProfileData.CLIENT_INFO_1.profile.userId);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.question, ['updaterResourceId', 'createDate', 'id', 'relatedQuestionsIds', 'pools', 'poolsIds', 'questionTranslationsIds']);
                    should.strictEqual(responseContent.question.pools.items.length, 2);
                }, done);
            });
            
            it('[' + serie + '] ' + 'FAILURE ERR_QUESTION_NO_RESOLUTION: questionCreate create: hasResolution', function (done) {
                var content = _.clone(Data.QUESTION_TEST);
                content.id = null;
                content.questionTemplateId = DataIds.QUESTION_TEMPLATE_APPROVED_ID;
                content.resolutionImageId = null;
                global.wsHelper.apiSecureFail(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.NoResolution, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionUpdate:update', function (done) {
                var content = {
                    id: DataIds.QUESTION_1_ID,
                    poolsIds: Data.QUESTION_TEST.poolsIds,
                    accessibleDate: '2016-10-10',
                    expirationDate: '2016-11-10',
                    renewDate: '2016-10-20',
                    regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID, DataIds.REGIONAL_SETTING_2_ID],
                    workorderId: null,
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_1);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(content.regionalSettingsIds, 0, 0);
                    shouldResponseContent.relatedQuestionsIds = ShouldHelper.treatAsList(Data.QUESTION_1.relatedQuestionsIds, 0, 0);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.QUESTION_1.tags, 0, 0);
                    should(responseContent.question.updaterResourceId).be.equal(ProfileData.CLIENT_INFO_1.profile.userId);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.question, ['updaterResourceId', 'createDate', 'accessibleDate', 'id', 'workorderId', 'pools', 'poolsIds', 'questionTranslationsIds']);
                    should.strictEqual(responseContent.question.pools.items.length, 1);
                    should(responseContent).has.not.property('workorderId');
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionUpdate:update set isInternational', function (done) {
                var content = { id: DataIds.QUESTION_1_ID, isInternational: 1 };
                global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent).has.property('question').which.have.property('isInternational').which.is.equal(1);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_QUESTION_NO_RESOLUTION: questionUpdate update: hasResolution', function (done) {
                var content = { id: DataIds.QUESTION_2_ID, resolutionImageId: null };
                global.wsHelper.apiSecureFail(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.NoResolution, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionUpdate:update', function (done) {
                var content = { id: DataIds.QUESTION_TEST_ID, name: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_WORKORDER_CLOSED_ITEMS_REQUIRED_REACHED questionUpdate:update', function (done) {
                return async.series([
                    function (next) {
                        var content = {
                            id: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
                            hasResolution: 0
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'questionTemplate/questionTemplateUpdate', ProfileData.CLIENT_INFO_1, content, null, next);
                    },
                    function (next) {
                        var content = {
                            id: DataIds.WORKORDER_1_ID
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'workorder/workorderClose', ProfileData.CLIENT_INFO_1, content, null, next);
                    },
                    function (next) {
                        var content = {
                            id: DataIds.QUESTION_2_ID,
                            workorderId: DataIds.WORKORDER_1_ID,
                        };
                        return global.wsHelper.apiSecureFail(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, Errors.WorkorderApi.WorkorderClosedItemsRequiredReached, next);
                    },
                ], done);
            });
        });
        
        describe('[' + serie + '] ' + 'questionDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionDelete', function (done) {
                var content = { id: DataIds.QUESTION_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'question/questionDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES questionDelete', function (done) {
                var content = { id: DataIds.QUESTION_1_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionDelete', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionDelete', function (done) {
                var content = { id: DataIds.QUESTION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'questionDeleteInactive', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionDeleteInactive', function (done) {
                var content = { id: DataIds.QUESTION_NO_DEPENDENCIES_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionDeleteInactive', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureFail(serie, 'question/questionDeleteInactive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES questionDeleteInactive', function (done) {
                var content = { id: DataIds.QUESTION_1_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionDeleteInactive', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ForeignKeyConstraintViolation, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionDeleteInactive', function (done) {
                var content = { id: DataIds.QUESTION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionDeleteInactive', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionGet', function (done) {
                var content = { id: DataIds.QUESTION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_1);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.QUESTION_1.regionalSettingsIds, 0, 0);
                    shouldResponseContent.relatedQuestionsIds = ShouldHelper.treatAsList(Data.QUESTION_1.relatedQuestionsIds, 0, 0);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(Data.QUESTION_1.poolsIds, 0, 0);
                    shouldResponseContent.questionTranslationsIds = ShouldHelper.treatAsList(Data.QUESTION_1.questionTranslationsIds, 0, 0);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.QUESTION_1.tags, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.question, ['createDate', 'pools']);
                    should.strictEqual(responseContent.question.pools.items.length, 2);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionGet', function (done) {
                var content = { id: DataIds.QUESTION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'questionList', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        id: DataIds.QUESTION_1_ID,
                        poolsIds: [DataIds.POOL_1_ID],
                    },
                    orderBy: [{ field: 'status', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_1);
                    shouldResponseContent.regionalSettingsIds = ShouldHelper.treatAsList(Data.QUESTION_1.regionalSettingsIds);
                    shouldResponseContent.relatedQuestionsIds = ShouldHelper.treatAsList(Data.QUESTION_1.relatedQuestionsIds);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList(content.searchBy.poolsIds);
                    shouldResponseContent.questionTranslationsIds = ShouldHelper.treatAsList(Data.QUESTION_1.questionTranslationsIds);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.QUESTION_1.tags);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['createDate', 'pools']);
                    should.strictEqual(responseContent.items[0].pools.items.length, 1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList - by pools.name/status', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        'pools.name': ['Pool 1', 'Pool 2'],
                        'pools.status': 'active',
                    },
                    orderBy: [{ field: 'status', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.strictEqual(responseContent.items.length, 2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList - google like search', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { fulltext: 'Aaaa Bbbb', poolsIds: [DataIds.POOL_1_ID] },
                    orderBy: [{ field: 'status', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.QUESTION_1, Data.QUESTION_2]);
                    _.forEach(shouldResponseContent, function (questionContent) {
                        questionContent.regionalSettingsIds = ShouldHelper.treatAsList(questionContent.regionalSettingsIds);
                        questionContent.relatedQuestionsIds = ShouldHelper.treatAsList(questionContent.relatedQuestionsIds);
                        questionContent.poolsIds = ShouldHelper.treatAsList(content.searchBy.poolsIds);
                        questionContent.questionTranslationsIds = ShouldHelper.treatAsList(questionContent.questionTranslationsIds);
                        questionContent.tags = ShouldHelper.treatAsList(questionContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate', 'pools',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList - question has review', function (done) {
                return async.series([
                    function (next) {
                        return QuestionOrTranslationReview.create({
                            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_QUESTION,
                            questionId: DataIds.QUESTION_1_ID,
                            questionTranslationId: null,
                            version: 1,
                            resourceId: DataIds.LOCAL_USER_ID,
                            difficulty: 1,
                            rating: 1,
                            isAccepted: 1
                        }).then(function (questionTranslation) {
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {
                                isAlreadyReviewedByUser: true
                            },
                            orderBy: [{ field: 'status', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(1);
                        }, next);
                    },
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {
                                isAlreadyReviewedByUser: false
                            },
                            orderBy: [{ field: 'status', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(1);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList order by rating asc', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        id: {
                            '$in': [DataIds.QUESTION_1_ID, DataIds.QUESTION_2_ID, DataIds.QUESTION_NO_DEPENDENCIES_ID]
                        }
                    },
                    orderBy: [{ field: 'rating', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.strictEqual(responseContent.items.length, 3);
                    should.strictEqual(responseContent.items[0].rating, 1);
                    should.strictEqual(responseContent.items[1].rating, 2);
                    should.strictEqual(responseContent.items[2].rating, 3);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList order by rating desc', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        id: {
                            '$in': [DataIds.QUESTION_1_ID, DataIds.QUESTION_2_ID, DataIds.QUESTION_NO_DEPENDENCIES_ID]
                        }
                    },
                    orderBy: [{ field: 'rating', direction: 'desc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.strictEqual(responseContent.items.length, 3);
                    should.strictEqual(responseContent.items[0].rating, 3);
                    should.strictEqual(responseContent.items[1].rating, 2);
                    should.strictEqual(responseContent.items[2].rating, 1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList filter within timeframe until now', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        createDate: {
                            '$lte': DateUtils.isoFuture()
                        }
                    },
                    orderBy: [{ field: 'rating', direction: 'desc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.strictEqual(responseContent.items.length, 2);
                    should.strictEqual(responseContent.items[0].rating, 2);
                    should.strictEqual(responseContent.items[1].rating, 1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCC ERR_DATABASE_NO_RECORD_FOUND: questionList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.QUESTION_TEST_ID },
                    orderBy: [{ field: 'status', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED: questionList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {},
                    orderBy: [{ field: 'userId', direction: 'asc' }]
                };
                global.wsHelper.apiSecureFail(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList filtered by status review', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        status: 'review'
                    },
                    orderBy: [{ field: 'rating', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    assert(responseContent.items.length > 0);
                    for (var i = 0; i < responseContent.items.length; i++) {
                        assert.strictEqual(responseContent.items[i].status, 'review');
                    }
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList filtered by status non_existing_status', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        status: 'non_existing_status'
                    },
                    orderBy: [{ field: 'rating', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    assert(responseContent.items.length === 0);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList filtered by rating=2', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        rating: 2
                    },
                    orderBy: [{ field: 'rating', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    assert(responseContent.items.length > 0);
                    for (var i = 0; i < responseContent.items.length; i++) {
                        assert.strictEqual(responseContent.items[i].rating, 2);
                    }
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionList filtered by rating, no item', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        rating: 2234322
                    },
                    orderBy: [{ field: 'rating', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    assert(responseContent.items.length === 0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'questionActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionActivate', function (done) {
                var content = { id: DataIds.QUESTION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.question.status).be.equal(Question.constants().STATUS_ACTIVE);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionActivate', function (done) {
                var content = { id: DataIds.QUESTION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'questionActivateEntire', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionActivateEntire', function (done) {
                var content = { id: DataIds.QUESTION_1_ID };
                global.wsHelper.sinonSandbox.stub(QuestionService, 'questionMediaList').callsArgWithAsync(1, null, [DataIds.MEDIA_1_ID]);
                global.wsHelper.sinonSandbox.stub(CryptoService, 'getObject').callsArgWithAsync(1, null, new Buffer(fs.readFileSync(path.join(__dirname, './../resources/test.jpg'))));
                global.wsHelper.sinonSandbox.stub(PublishService, 'uploadObject').callsArgWithAsync(3, null, { Key: 'testUDID' });
                global.wsHelper.sinonSandbox.stub(AerospikePool, 'create').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(AerospikePoolMeta, 'increment').callsArgWithAsync(1, null, 1);
                global.wsHelper.sinonSandbox.stub(AerospikePoolIndex, 'create').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(AerospikePoolIndexMeta, 'increment').callsArgWithAsync(1, null, 1);
                async.series([
                    // activate manually
                    function (next) {
                        try {
                            global.wsHelper.apiSecureSucc(serie, 'question/questionActivateEntire', ProfileData.CLIENT_INFO_1, content, null, next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    
                    // question status=active, isActivatedManually=1
                    function (next) {
                        try {
                            Question.findOne({where: {id: DataIds.QUESTION_1_ID}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isActivatedManually, 1);
                                    assert.strictEqual(item.status, Question.constants().STATUS_ACTIVE);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // question translations status=active, isActivatedManually=1
                    function (next) {
                        try {
                            QuestionTranslation.findAll({where: {questionId: DataIds.QUESTION_1_ID}}).then(function (dbItems) {
                                try {
                                    for(var i=0; i<dbItems.length; i++) {
                                        var item = dbItems[i].get({plain: true});
                                        assert.strictEqual(item.status, QuestionTranslation.constants().STATUS_ACTIVE);
                                        assert.strictEqual(item.isActivatedManually, 1);
                                    }
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // deactivate
                    function (next) {
                        try {
                            global.wsHelper.sinonSandbox.stub(QuestionTranslationApiFactory, 'questionTranslationUnpublish', function (params, message, clientSession, callback) {
                                
                                var translationParams = {};
                                translationParams.id = params.id;
                                translationParams.status = QuestionTranslation.constants().STATUS_INACTIVE;
                                return QuestionTranslationApiFactory.questionTranslationSetDeploymentStatus(translationParams, clientSession, callback);
                            });
                            
                            global.wsHelper.apiSecureSucc(serie, 'question/questionDeactivate', ProfileData.CLIENT_INFO_1, content, null, next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // question status=inactive, isActivatedManually=0
                    function (next) {
                        try {
                            Question.findOne({where: {id: DataIds.QUESTION_1_ID}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isActivatedManually, 0);
                                    assert.strictEqual(item.status, Question.constants().STATUS_INACTIVE);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // question translations status=inactive, isActivatedManually=0
                    function (next) {
                        try {
                            QuestionTranslation.findAll({where: {questionId: DataIds.QUESTION_1_ID}}).then(function (dbItems) {
                                try {
                                    for(var i=0; i<dbItems.length; i++) {
                                        var item = dbItems[i].get({plain: true});
                                        assert.strictEqual(item.status, QuestionTranslation.constants().STATUS_INACTIVE);
                                        assert.strictEqual(item.isActivatedManually, 0);
                                    }
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },

                ], done);
            });
            
            it('[' + serie + '] ' + 'SUCCESS: questionActivateEntire, deactivate one single translation only', function (done) {
                var content = { id: DataIds.QUESTION_1_ID };
                var transId = DataIds.QUESTION_1_TRANSLATION_DE_ID;
                global.wsHelper.sinonSandbox.stub(QuestionService, 'questionMediaList').callsArgWithAsync(1, null, [DataIds.MEDIA_1_ID]);
                global.wsHelper.sinonSandbox.stub(CryptoService, 'getObject').callsArgWithAsync(1, null, new Buffer(fs.readFileSync(path.join(__dirname, './../resources/test.jpg'))));
                global.wsHelper.sinonSandbox.stub(PublishService, 'uploadObject').callsArgWithAsync(3, null, { Key: 'testUDID' });
                global.wsHelper.sinonSandbox.stub(AerospikePool, 'create').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(AerospikePoolMeta, 'increment').callsArgWithAsync(1, null, 1);
                global.wsHelper.sinonSandbox.stub(AerospikePoolIndex, 'create').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(AerospikePoolIndexMeta, 'increment').callsArgWithAsync(1, null, 1);
                async.series([
                    // activate manually
                    function (next) {
                        try {
                            global.wsHelper.apiSecureSucc(serie, 'question/questionActivateEntire', ProfileData.CLIENT_INFO_1, content, null, next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    
                    // question status=active, isActivatedManually=1
                    function (next) {
                        try {
                            Question.findOne({where: {id: DataIds.QUESTION_1_ID}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isActivatedManually, 1);
                                    assert.strictEqual(item.status, Question.constants().STATUS_ACTIVE);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // question translations status=active, isActivatedManually=1
                    function (next) {
                        try {
                            QuestionTranslation.findAll({where: {questionId: DataIds.QUESTION_1_ID}}).then(function (dbItems) {
                                try {
                                    for(var i=0; i<dbItems.length; i++) {
                                        var item = dbItems[i].get({plain: true});
                                        assert.strictEqual(item.status, QuestionTranslation.constants().STATUS_ACTIVE);
                                        assert.strictEqual(item.isActivatedManually, 1);
                                    }
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // deactivate
                    function (next) {
                        try {
                            global.wsHelper.sinonSandbox.stub(QuestionTranslationApiFactory, 'questionTranslationUnpublish', function (params, message, clientSession, callback) {
                                var translationParams = {};
                                translationParams.id = params.id;
                                translationParams.status = QuestionTranslation.constants().STATUS_INACTIVE;
                                return QuestionTranslationApiFactory.questionTranslationSetDeploymentStatus(translationParams, clientSession, callback);
                            });
                            
                            global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationDeactivate', ProfileData.CLIENT_INFO_1, {
                                id: transId
                            }, null, next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // question status=active, isActivatedManually=1
                    function (next) {
                        try {
                            Question.findOne({where: {id: DataIds.QUESTION_1_ID}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isActivatedManually, 1);
                                    assert.strictEqual(item.status, Question.constants().STATUS_ACTIVE);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // deactivated translation status=inactive, isActivatedManually=0
                    function (next) {
                        try {
                            QuestionTranslation.findOne({where: {id: transId}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isActivatedManually, 0);
                                    assert.strictEqual(item.status, QuestionTranslation.constants().STATUS_INACTIVE);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionActivateEntire', function (done) {
                var content = { id: DataIds.QUESTION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionActivateEntire', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            
        });

        describe('[' + serie + '] ' + 'questionDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionDeactivate', function (done) {
                var content = { id: DataIds.QUESTION_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.question.status).be.equal(Question.constants().STATUS_INACTIVE);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionDeactivate', function (done) {
                var content = { id: DataIds.QUESTION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });
        
        describe('[' + serie + '] ' + 'tagList', function () {
            it('[' + serie + '] ' + 'SUCCESS: tagList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { tag: DataIds.TAG_1_TAG },
                    orderBy: [{ field: 'approvedDate', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/tagList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.TAG_1);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['tenantId']);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: tagList LIKE', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { tag: '%-%' },
                    orderBy: [{ field: 'approvedDate', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/tagList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: tagList IN', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { tag: [DataIds.TAG_1_TAG, DataIds.TAG_2_TAG] },
                    orderBy: [{ field: 'approvedDate', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/tagList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCC ERR_DATABASE_NO_RECORD_FOUND: tagList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { tag: DataIds.TAG_TEST_TAG },
                    orderBy: [{ field: 'approvedDate', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/tagList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'tagDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: tagDelete', function (done) {
                var content = { tag: 'test' };
                global.wsHelper.apiSecureSucc(serie, 'question/tagDelete', ProfileData.CLIENT_INFO_1, content, null, function () {
                    return done();
                });
            });
        });

        describe('[' + serie + '] ' + 'tagReplace', function () {
            it('[' + serie + '] ' + 'SUCCESS: tagReplace', function (done) {
                var oldTag = DataIds.TAG_1_TAG;
                var newTag = DataIds.TAG_1_TAG + '_new';
                
                async.series([
                    function (next) {
                        var content = { oldTag: oldTag, newTag: newTag };
                        global.wsHelper.apiSecureSucc(serie, 'question/tagReplace', ProfileData.CLIENT_INFO_1, content, null, function () {
                            return next();
                        });
                    },
                    
                    // old tag doesn't exists anymore
                    function (next) {
                        Tag.count({where: {tag: oldTag}}).then(function (cnt) {
                            try {
                                should(cnt).be.equal(0);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(next);
                    },
                    
                    // new tag exists
                    function (next) {
                        Tag.count({where: {tag: newTag}}).then(function (cnt) {
                            try {
                                should(cnt).be.equal(1);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(next);
                    },
                    
                    function (next) {
                        async.mapSeries([
                            QuestionHasTag,
                            PoolHasTag
                        ], function (model, cbItem) {
                            model.count({where: {tagTag: newTag}}).then(function (cnt) {
                                try {
                                    should(cnt).be.aboveOrEqual(1);
                                    return cbItem();
                                } catch (e) {
                                    return cbItem(e);
                                }
                            }).catch(next);
                        }, next);
                    },
                    function (next) {
                        async.mapSeries([
                            QuestionHasTag,
                            PoolHasTag
                        ], function (model, cbItem) {
                            model.count({where: {tagTag: oldTag}}).then(function (cnt) {
                                try {
                                    should(cnt).be.equal(0);
                                    return cbItem();
                                } catch (e) {
                                    return cbItem(e);
                                }
                            }).catch(next);
                        }, next);
                    }
                ], done);
            });
            
            
            
        });
        describe('[' + serie + '] ' + ' test bad words on question update', function () {
            it('[' + serie + '] ' + ' question source not contain bad words', function (done) {
                var content = { id: DataIds.QUESTION_1_ID, source: 'no bad word' };
                global.wsHelper.apiSecureCall(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getContent().question.source, content.source);
                        done();
                    } catch (e) {
                        return done(e);
                    }
                });
            });
            it('[' + serie + '] ' + ' question source contain bad words', function (done) {
                var content = { id: DataIds.QUESTION_1_ID, source: 'a stupid source' };
                global.wsHelper.apiSecureCall(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError().message, Errors.QuestionApi.EntityContainBadWords);
                        done();
                    } catch (e) {
                        return done(e);
                    }
                });
            });
            it('[' + serie + '] ' + ' question source not provided', function (done) {
                var content = { id: DataIds.QUESTION_1_ID, complexity: 5 };
                global.wsHelper.apiSecureCall(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getContent().question.complexity, content.complexity);
                        done();
                    } catch (e) {
                        return done(e);
                    }
                });
            });
            
            it('[' + serie + '] ' + ' question tags not contain bad words', function (done) {
                var content = { id: DataIds.QUESTION_1_ID, tags: ['tag1', 'tag2'] };
                global.wsHelper.apiSecureCall(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        done();
                    } catch (e) {
                        return done(e);
                    }
                });
            });
            it('[' + serie + '] ' + ' question tags not provided', function (done) {
                var content = { id: DataIds.QUESTION_1_ID, complexity: 5 };
                global.wsHelper.apiSecureCall(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError(), null);
                        assert.strictEqual(response.getContent().question.complexity, content.complexity);
                        done();
                    } catch (e) {
                        return done(e);
                    }
                });
            });
            it('[' + serie + '] ' + ' question tags contain bad words', function (done) {
                var content = { id: DataIds.QUESTION_1_ID, tags: ['tag1', 'tag2 stupid tag'] };
                global.wsHelper.apiSecureCall(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (err, response) {
                    try {
                        assert.ifError(err);
                        assert.strictEqual(response.getError().message, Errors.QuestionApi.EntityContainBadWords);
                        done();
                    } catch (e) {
                        return done(e);
                    }
                });
            });
        });
        
        describe('[' + serie + '] ' + 'tag APIs, tenant specific', function () {
            
            it('[' + serie + '] ' + 'test application tag/tagList', function (done) {
                
                async.series([
                    // list success
                    function (next) {
                        var itm = Data.TAG_1;
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'tag', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'tag/tagList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            assert.strictEqual(responseContent.items[0].tag, itm.tag);
                        }, next);
                    },
                    
                    // list error
                    function (next) {
                        var itm = Data.TAG_1;
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { id: itm.id },
                            orderBy: [{ field: 'tag', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'tag/tagList', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    }
                ], done);
            });
        });
        

    });
});
