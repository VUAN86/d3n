var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var should = require('should');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/question.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var QuestionService = require('./../../services/questionService.js');
var CryptoService = require('./../../services/cryptoService.js');
var PublishService = require('./../../services/publishService.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikePool = KeyvalueService.Models.AerospikePool;
var AerospikePoolMeta = KeyvalueService.Models.AerospikePoolMeta;
var AerospikePoolIndex = KeyvalueService.Models.AerospikePoolIndex;
var AerospikePoolIndexMeta = KeyvalueService.Models.AerospikePoolIndexMeta;

describe('WS QuestionTranslation API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        describe('[' + serie + '] ' + 'questionTranslationUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUpdate:create', function (done) {
                var content = _.clone(Data.QUESTION_TRANSLATION_TEST);
                content.id = null;
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TRANSLATION_TEST);
                    should(responseContent.questionTranslation.updaterResourceId).be.equal(ProfileData.CLIENT_INFO_1.profile.userId);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTranslation, ['updaterResourceId', 'status', 'createDate', 'approveDate', 'question', 'poolsIds', 'isDefaultTranslation',
                        'stat_associatedGames', 'stat_associatedPools', 'stat_averageAnswerSpeed',
                        'stat_gamesPlayed', 'stat_rating', 'stat_rightAnswers', 'stat_wrongAnswers'
                    ]);
                    should.strictEqual(responseContent.questionTranslation.question.id, DataIds.QUESTION_2_ID);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUpdate:create - without workorder', function (done) {
                var content = _.clone(Data.QUESTION_TRANSLATION_TEST);
                content.id = null;
                content.workorderId = null;
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_TRANSLATION_TEST);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTranslation, ['updaterResourceId', 'status', 'createDate', 'approveDate', 'workorderId', 'question', 'poolsIds', 'isDefaultTranslation',
                        'stat_associatedGames', 'stat_associatedPools', 'stat_averageAnswerSpeed',
                        'stat_gamesPlayed', 'stat_rating', 'stat_rightAnswers', 'stat_wrongAnswers'
                    ]);
                    should.strictEqual(responseContent.questionTranslation.question.id, DataIds.QUESTION_2_ID);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUpdate:update', function (done) {
                var content = { id: DataIds.QUESTION_1_TRANSLATION_EN_ID, name: 'updated', content: 'blob-update', status: 'review' };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.questionTranslation.updaterResourceId).be.equal(ProfileData.CLIENT_INFO_1.profile.userId);
                    should(responseContent.questionTranslation.name).be.equal(content.name);
                    should(responseContent.questionTranslation.content).be.equal(content.content);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTranslationUpdate:update', function (done) {
                var content = { id: DataIds.QUESTION_TRANSLATION_TEST_ID, name: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'question/questionTranslationUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTranslationGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationGet', function (done) {
                var content = { id: DataIds.QUESTION_1_TRANSLATION_EN_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.QUESTION_1_TRANSLATION_EN);
                    shouldResponseContent.poolsIds = ShouldHelper.treatAsList([DataIds.POOL_1_ID, DataIds.POOL_2_ID]);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTranslation, ['status', 'createDate', 'approveDate', 'question',
                        'stat_associatedGames', 'stat_associatedPools', 'stat_averageAnswerSpeed',
                        'stat_gamesPlayed', 'stat_rating', 'stat_rightAnswers', 'stat_wrongAnswers'
                    ]);
                    should.strictEqual(responseContent.questionTranslation.question.id, DataIds.QUESTION_1_ID);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: questionTranslationGet', function (done) {
                var content = { id: DataIds.QUESTION_TRANSLATION_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/questionTranslationGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTranslationList', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        questionsIds: [DataIds.QUESTION_1_ID],
                        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID],
                        creatorResourceId: '$user'
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationList by fulltext', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        fulltext: 'Aaaa Bbbb',
                        questionsIds: [DataIds.QUESTION_1_ID],
                        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID],
                        creatorResourceId: '$user'
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationList by poolsIds', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID]
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(5);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationList by question.questionTemplateId', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        'question.questionTemplateId': [DataIds.QUESTION_TEMPLATE_NEW_ID, DataIds.QUESTION_TEMPLATE_APPROVED_ID]
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(5);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationList - question has review (no items)', function (done) {
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
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(0);
                        }, next);
                    },
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {
                                isAlreadyReviewedByUser: true
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(0);
                        }, next);
                    },
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {
                                isAlreadyReviewedByUser: false
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(5);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationList - question translation has review (got items)', function (done) {
                return async.series([
                    function (next) {
                        return QuestionOrTranslationReview.create({
                            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_TRANSLATION,
                            questionId: null,
                            questionTranslationId: DataIds.QUESTION_1_TRANSLATION_EN_ID,
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
                                isAlreadyReviewedByUser: true,
                                isDefaultTranslation: true,
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
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
                                isAlreadyReviewedByUser: false,
                                isDefaultTranslation: false,
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(4);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationList - is default translation', function (done) {
                return async.series([
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {
                                isDefaultTranslation: true
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
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
                                isDefaultTranslation: false
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(4);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList - base', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        questionsIds: [DataIds.QUESTION_1_ID],
                        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID],
                        creatorResourceId: '$user'
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList by poolsIds', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        questionsIds: [DataIds.QUESTION_1_ID],
                        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID],
                        creatorResourceId: '$user',
                        poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID]
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList by fulltext', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        fulltext: 'Aaaa Bbbb',
                        questionsIds: [DataIds.QUESTION_1_ID],
                        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID],
                        creatorResourceId: '$user'
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList by fulltext, poolsIds', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        fulltext: 'Aaaa Bbbb',
                        questionsIds: [DataIds.QUESTION_1_ID],
                        languagesIds: [DataIds.LANGUAGE_EN_ID, DataIds.LANGUAGE_DE_ID],
                        creatorResourceId: '$user',
                        poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID]
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(1);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList by question.questionTemplateId', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        'question.questionTemplateId': [DataIds.QUESTION_TEMPLATE_NEW_ID, DataIds.QUESTION_TEMPLATE_APPROVED_ID]
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList by fulltext, question.questionTemplateId', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        fulltext: 'Aaaa Bbbb',
                        'question.questionTemplateId': [DataIds.QUESTION_TEMPLATE_NEW_ID, DataIds.QUESTION_TEMPLATE_APPROVED_ID]
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(2);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList - question has review (no items)', function (done) {
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
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(0);
                        }, next);
                    },
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {
                                isAlreadyReviewedByUser: false
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(2);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList - question translation has review (got items)', function (done) {
                return async.series([
                    function (next) {
                        return QuestionOrTranslationReview.create({
                            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_TRANSLATION,
                            questionId: null,
                            questionTranslationId: DataIds.QUESTION_1_TRANSLATION_EN_ID,
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
                                isAlreadyReviewedByUser: true,
                                isDefaultTranslation: true,
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
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
                                isAlreadyReviewedByUser: false,
                                isDefaultTranslation: false,
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(2);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationUniqueList - is default translation', function (done) {
                return async.series([
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {
                                isDefaultTranslation: true
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
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
                                isDefaultTranslation: false
                            },
                            orderBy: [{ field: 'name', direction: 'asc' }],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUniqueList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(2);
                        }, next);
                    },
                ], done);
            });
            it('[' + serie + '] ' + 'SUCC ERR_DATABASE_NO_RECORD_FOUND: questionTranslationList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        questionId: DataIds.QUESTION_TEST_ID
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }],
                };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTranslationBlock', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationBlock', function (done) {
                return async.series([
                    // should be 5 entries as initial setup
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {},
                            orderBy: [],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(5);
                        }, next);
                    },
                    // get should be successful
                    function (next) {
                        var content = { id: DataIds.QUESTION_1_TRANSLATION_EN_ID };
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = _.clone(Data.QUESTION_1_TRANSLATION_EN);
                            shouldResponseContent.poolsIds = ShouldHelper.treatAsList([DataIds.POOL_1_ID, DataIds.POOL_2_ID]);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.questionTranslation, ['status', 'createDate', 'approveDate', 'question',
                                'stat_associatedGames', 'stat_associatedPools', 'stat_averageAnswerSpeed',
                                'stat_gamesPlayed', 'stat_rating', 'stat_rightAnswers', 'stat_wrongAnswers'
                            ]);
                        }, done);
                    },
                    // block translation
                    function (next) {
                        var content = {
                            id: DataIds.QUESTION_1_TRANSLATION_EN_ID
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationBlock', ProfileData.CLIENT_INFO_1, content, null, next);
                    },
                    // update translation block to another user: should found entry
                    function (next) {
                        return QuestionTranslation.update({ blockerResourceId: DataIds.FACEBOOK_USER_ID }, { where: { id: DataIds.QUESTION_1_TRANSLATION_EN_ID } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    // should give 1 entry less
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {},
                            orderBy: [],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(4);
                        }, next);
                    },
                    // get should fail with ERR_ENTRY_BLOCKED_BY_ANOTHER_USER
                    function (next) {
                        var content = { id: DataIds.QUESTION_1_TRANSLATION_EN_ID };
                        return global.wsHelper.apiSecureFail(serie, 'question/questionTranslationGet', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.EntryBlockedByAnotherUser, next);
                    },
                    // entry should be updated and previous block released
                    function (next) {
                        var content = { id: DataIds.QUESTION_1_TRANSLATION_EN_ID, name: 'updated', content: 'blob-update', status: 'review' };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.questionTranslation.name).be.equal(content.name);
                            should(responseContent.questionTranslation.content).be.equal(content.content);
                        }, done);
                    },
                    // should be back 5 entries, as block of item released
                    function (next) {
                        var content = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: {},
                            orderBy: [],
                        };
                        return global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent)
                                .has.property('items')
                                .which.has.length(5);
                        }, next);
                    },
                ], done);
            });
        });

        describe('[' + serie + '] ' + 'questionTranslationActivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationActivate', function (done) {
                global.wsHelper.sinonSandbox.stub(QuestionService, 'questionMediaList').callsArgWithAsync(1, null, [DataIds.MEDIA_1_ID]);
                global.wsHelper.sinonSandbox.stub(CryptoService, 'getObject').callsArgWithAsync(1, null, new Buffer(fs.readFileSync(path.join(__dirname, './../resources/test.jpg'))));
                global.wsHelper.sinonSandbox.stub(PublishService, 'uploadObject').callsArgWithAsync(3, null, {Key: 'testUDID'});
                global.wsHelper.sinonSandbox.stub(AerospikePool, 'create').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(AerospikePoolMeta, 'increment').callsArgWithAsync(1, null, 1);
                global.wsHelper.sinonSandbox.stub(AerospikePoolIndex, 'create').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(AerospikePoolIndexMeta, 'increment').callsArgWithAsync(1, null, 1);
                var content = { id: DataIds.QUESTION_1_TRANSLATION_EN_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.questionTranslation.status).be.equal(QuestionTranslation.constants().STATUS_ACTIVE);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED (No pool): questionTranslationActivate', function (done) {
                var content = { id: DataIds.QUESTION_TRANSLATION_NO_POOL };
                content.content = null;
                global.wsHelper.apiSecureFail(serie, 'question/questionTranslationActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.ERR_VALIDATION_FAILED, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_VALIDATION_FAILED (No content): questionTranslationActivate', function (done) {
                var content = { id: DataIds.QUESTION_TRANSLATION_NO_POOL };
                global.wsHelper.apiSecureFail(serie, 'question/questionTranslationActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.ERR_VALIDATION_FAILED, done);
            });
        });

        describe('[' + serie + '] ' + 'questionTranslationDeactivate', function () {
            it('[' + serie + '] ' + 'SUCCESS: questionTranslationDeactivate', function (done) {
                global.wsHelper.sinonSandbox.stub(AerospikePool, 'remove').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(AerospikePoolIndex, 'remove').callsArgWithAsync(1, null, {});
                global.wsHelper.sinonSandbox.stub(PublishService, 'deleteObjects').callsArgWithAsync(1, null,
                {
                    Deleted: [{ Key: 'testUDID' }]
                });
                var content = { id: DataIds.QUESTION_1_TRANSLATION_EN_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.questionTranslation.status).be.equal(QuestionTranslation.constants().STATUS_INACTIVE);
                        }, done);
                    }, done);
                });
            });
        });
    });
});
