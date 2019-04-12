var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/question.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var PoolHasTenant = Database.RdbmsService.Models.Question.PoolHasTenant;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikePoolList = KeyvalueService.Models.AerospikePoolList;

describe('WS Pool API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'poolUpdate', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolUpdate:create', function (done) {
                var content = _.clone(Data.POOL_TEST);
                content.id = null;
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'question/poolCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            var shouldResponseContent = _.clone(Data.POOL_TEST);
                            shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.POOL_TEST.questionsIds, 0, 0);
                            shouldResponseContent.tags = ShouldHelper.treatAsList(Data.POOL_TEST.tags, 0, 0);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.pool, ['createDate', 'subpoolsIds', 'mediasIds',
                                'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                                'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                                'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                            ]);
                        }, next);
                    },
                    function (next) {
                        return AerospikePoolList.findOne({ tenantId: DataIds.TENANT_1_ID }, function (err, item) {
                            try {
                                if (err) {
                                    return next(err);
                                }
                                assert.isAtLeast(_.indexOf(_.values(item.pools), content.name), 0);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    }
                ], done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolUpdate:update', function (done) {
                var content = { id: DataIds.POOL_1_ID, name: Data.POOL_1.name + ' updated', parentPoolId: 0 };
                global.wsHelper.apiSecureSucc(serie, 'question/poolUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should(responseContent.pool.name).be.equal(content.name);
                    should.not.exist(responseContent.pool.parentPoolId);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_QUESTION_VALIDATION_FAILED: poolUpdate:update - poolParentId', function (done) {
                var content = { id: DataIds.POOL_1_ID, parentPoolId: DataIds.POOL_2_ID };
                global.wsHelper.apiSecureFail(serie, 'question/poolUpdate', ProfileData.CLIENT_INFO_1, content, Errors.PoolApi.PoolHierarchyInconsistent, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolUpdate:update', function (done) {
                var content = { id: DataIds.POOL_TEST_ID, name: 'updated' };
                global.wsHelper.apiSecureFail(serie, 'question/poolUpdate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'poolDelete', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolDelete', function (done) {
                var content = { id: DataIds.POOL_NO_DEPENDENCIES_ID };
                return async.series([
                    function (next) {
                        return global.wsHelper.apiSecureSucc(serie, 'question/poolDelete', ProfileData.CLIENT_INFO_1, content, null, next);
                    },
                    function (next) {
                        return AerospikePoolList.findOne({ tenantId: DataIds.TENANT_1_ID }, function (err, item) {
                            try {
                                if (err) {
                                    return next(err);
                                }
                                assert.equal(_.indexOf(_.values(item.pools), content.name), -1);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });
                    },
                    function (next) {
                        global.wsHelper.apiSecureFail(serie, 'question/poolDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
                    }
                ], done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolDelete', function (done) {
                var content = { id: DataIds.POOL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/poolDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
            it('[' + serie + '] ' + 'FAILURE: ERR_DATABASE_EXISTING_DEPENDECIES poolDelete', function (done) {
                var content = { id: DataIds.POOL_1_ID };
                global.wsHelper.apiSecureFail(serie, 'question/poolDelete', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.SequelizeForeignKeyConstraintError, done);
            });
        });

        describe('[' + serie + '] ' + 'poolGet', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolGet', function (done) {
                var content = { id: DataIds.POOL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.POOL_1);
                    shouldResponseContent.subpoolsIds = ShouldHelper.treatAsList(Data.POOL_1.subpoolsIds, 0, 0);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.POOL_1.questionsIds, 0, 0);
                    shouldResponseContent.mediasIds = ShouldHelper.treatAsList(Data.POOL_1.mediasIds, 0, 0);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.POOL_1.tags, 0, 0);
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.pool, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolGet', function (done) {
                var content = { id: DataIds.POOL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'poolList', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by name like, subpoolsIds, mediasIds', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { name: { '$like': 'Pool%' }, subpoolsIds: [DataIds.POOL_2_ID], mediasIds: [DataIds.MEDIA_1_ID] },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.POOL_1]);
                    _.forEach(shouldResponseContent, function (poolContent) {
                        poolContent.subpoolsIds = ShouldHelper.treatAsList(content.searchBy.subpoolsIds);
                        poolContent.questionsIds = ShouldHelper.treatAsList(poolContent.questionsIds);
                        poolContent.mediasIds = ShouldHelper.treatAsList(content.searchBy.mediasIds);
                        poolContent.tags = ShouldHelper.treatAsList(poolContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - google like search', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { fulltext: 'Aaaa Bbbb', status: 'active', subpoolsIds: [DataIds.POOL_2_ID], mediasIds: [DataIds.MEDIA_1_ID] },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.POOL_1]);
                    _.forEach(shouldResponseContent, function (poolContent) {
                        poolContent.subpoolsIds = ShouldHelper.treatAsList(content.searchBy.subpoolsIds);
                        poolContent.questionsIds = ShouldHelper.treatAsList(poolContent.questionsIds);
                        poolContent.mediasIds = ShouldHelper.treatAsList(content.searchBy.mediasIds);
                        poolContent.tags = ShouldHelper.treatAsList(poolContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by parentPoolId is null', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { parentPoolId: null },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.POOL_1]);
                    _.forEach(shouldResponseContent, function (poolContent) {
                        poolContent.subpoolsIds = ShouldHelper.treatAsList(poolContent.subpoolsIds);
                        poolContent.questionsIds = ShouldHelper.treatAsList(poolContent.questionsIds);
                        poolContent.mediasIds = ShouldHelper.treatAsList(poolContent.mediasIds);
                        poolContent.tags = ShouldHelper.treatAsList(poolContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by hasQuestionReview = true', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_1_ID, hasQuestionReview: true },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.POOL_1]);
                    _.forEach(shouldResponseContent, function (poolContent) {
                        poolContent.subpoolsIds = ShouldHelper.treatAsList(poolContent.subpoolsIds);
                        poolContent.questionsIds = ShouldHelper.treatAsList(poolContent.questionsIds);
                        poolContent.mediasIds = ShouldHelper.treatAsList(poolContent.mediasIds);
                        poolContent.tags = ShouldHelper.treatAsList(poolContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by hasQuestionReview = false', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_1_ID, hasQuestionReview: false },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by hasQuestionTranslationReview = true', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_1_ID, hasQuestionTranslationReview: true },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by hasQuestionTranslationReview = false', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_1_ID, hasQuestionTranslationReview: false },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.POOL_1]);
                    _.forEach(shouldResponseContent, function (poolContent) {
                        poolContent.subpoolsIds = ShouldHelper.treatAsList(poolContent.subpoolsIds);
                        poolContent.questionsIds = ShouldHelper.treatAsList(poolContent.questionsIds);
                        poolContent.mediasIds = ShouldHelper.treatAsList(poolContent.mediasIds);
                        poolContent.tags = ShouldHelper.treatAsList(poolContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by hasQuestionTranslationNeeded = true', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_1_ID, hasQuestionTranslationNeeded: true },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    should.exist(responseContent);
                    should(responseContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by hasQuestionTranslationNeeded = false', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_1_ID, hasQuestionTranslationNeeded: false },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.POOL_1]);
                    _.forEach(shouldResponseContent, function (poolContent) {
                        poolContent.subpoolsIds = ShouldHelper.treatAsList(poolContent.subpoolsIds);
                        poolContent.questionsIds = ShouldHelper.treatAsList(poolContent.questionsIds);
                        poolContent.mediasIds = ShouldHelper.treatAsList(poolContent.mediasIds);
                        poolContent.tags = ShouldHelper.treatAsList(poolContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'SUCCESS: poolList - search by hasQuestionReview/hasQuestionTranslationReview/hasQuestionTranslationNeeded combined', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: {
                        id: DataIds.POOL_1_ID,
                        hasQuestionReview: true,
                        hasQuestionTranslationReview: false,
                        hasQuestionTranslationNeeded: false,
                    },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.cloneDeep([Data.POOL_1]);
                    _.forEach(shouldResponseContent, function (poolContent) {
                        poolContent.subpoolsIds = ShouldHelper.treatAsList(poolContent.subpoolsIds);
                        poolContent.questionsIds = ShouldHelper.treatAsList(poolContent.questionsIds);
                        poolContent.mediasIds = ShouldHelper.treatAsList(poolContent.mediasIds);
                        poolContent.tags = ShouldHelper.treatAsList(poolContent.tags);
                    });
                    ShouldHelper.deepEqualList(shouldResponseContent, responseContent.items, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (content) {
                    should.exist(content);
                    should(content)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });

        // #1375
        //describe('[' + serie + '] ' + 'poolActivate', function () {
        //    it('[' + serie + '] ' + 'SUCCESS: poolActivate', function (done) {
        //        var content = { id: DataIds.POOL_1_ID };
        //        global.wsHelper.apiSecureSucc(serie, 'question/poolActivate', ProfileData.CLIENT_INFO_1, content, null, function () {
        //            setImmediate(function (done) {
        //                var content = {
        //                    limit: Config.rdbms.limit,
        //                    offset: 0,
        //                    searchBy: { id: DataIds.POOL_1_ID, status: 'active' },
        //                    orderBy: [{ field: 'name', direction: 'asc' }]
        //                };
        //                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (content) {
        //                    should.exist(content);
        //                    should(content)
        //                        .has.property('items')
        //                        .which.has.length(1);
        //                }, done);
        //            }, done);
        //        });
        //    });
        //    it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolActivate', function (done) {
        //        var content = { id: DataIds.POOL_TEST_ID };
        //        global.wsHelper.apiSecureFail(serie, 'question/poolActivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
        //    });
        //});

        // #1375
        //describe('[' + serie + '] ' + 'poolDeactivate', function () {
        //    it('[' + serie + '] ' + 'SUCCESS: poolDeactivate', function (done) {
        //        var content = { id: DataIds.POOL_1_ID };
        //        global.wsHelper.apiSecureSucc(serie, 'question/poolDeactivate', ProfileData.CLIENT_INFO_1, content, null, function () {
        //            setImmediate(function (done) {
        //                var content = {
        //                    limit: Config.rdbms.limit,
        //                    offset: 0,
        //                    searchBy: { id: DataIds.POOL_1_ID, status: 'inactive' },
        //                    orderBy: [{ field: 'name', direction: 'asc' }]
        //                };
        //                global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (content) {
        //                    should.exist(content);
        //                    should(content)
        //                        .has.property('items')
        //                        .which.has.length(1);
        //                }, done);
        //            }, done);
        //        });
        //    });
        //    it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolDeactivate', function (done) {
        //        var content = { id: DataIds.POOL_TEST_ID };
        //        global.wsHelper.apiSecureFail(serie, 'question/poolDeactivate', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
        //    });
        //});
        describe('[' + serie + '] ' + 'poolAddDeleteQuestion', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolAddDeleteQuestion', function (done) {
                var content = {
                    id: DataIds.POOL_1_ID,
                    deleteQuestions: [DataIds.QUESTION_1_ID],
                    addQuestions: [DataIds.QUESTION_2_ID]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolAddDeleteQuestion', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.pool.questionsIds.items).be.eql(content.addQuestions);
                        }, done);
                    }, done);
                });
            });
        });
        describe('[' + serie + '] ' + 'poolAddDeleteMedia', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolAddDeleteMedia', function (done) {
                var content = {
                    id: DataIds.POOL_1_ID,
                    deleteMedias: [DataIds.MEDIA_1_ID],
                    addMedias: [DataIds.MEDIA_2_ID]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolAddDeleteMedia', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.pool.mediasIds.items).be.eql(content.addMedias);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'SUCCESS: poolAddDeleteMedia add only', function (done) {
                var content = {
                    id: DataIds.POOL_1_ID,
                    deleteMedias: [],
                    addMedias: [DataIds.MEDIA_2_ID]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolAddDeleteMedia', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.pool.mediasIds.items).be.eql([DataIds.MEDIA_1_ID, DataIds.MEDIA_2_ID]);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'SUCCESS: poolAddDeleteMedia delete only', function (done) {
                var content = {
                    id: DataIds.POOL_1_ID,
                    deleteMedias: [DataIds.MEDIA_1_ID],
                    addMedias: []
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolAddDeleteMedia', ProfileData.CLIENT_INFO_1, content, null, function () {
                    setImmediate(function (done) {
                        global.wsHelper.apiSecureSucc(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should(responseContent.pool.mediasIds.items).be.eql([DataIds.MEDIA_2_ID]);
                        }, done);
                    }, done);
                });
            });
            it('[' + serie + '] ' + 'FAILURE ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION when add a non existing media: question/poolAddDeleteMedia ', function (done) {
                var content = {
                  id: DataIds.POOL_1_ID,
                  addMedias: [DataIds.MEDIA_TEST_ID]
                };
                global.wsHelper.apiSecureFail(serie, 'question/poolAddDeleteMedia', ProfileData.CLIENT_INFO_1, content, 'ERR_FOREIGN_KEY_CONSTRAINT_VIOLATION', done);
            });

        });
        describe('[' + serie + '] ' + 'poolQuestionStatistic', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolQuestionStatistic', function (done) {
                var content = { id: DataIds.POOL_1_ID };
                global.wsHelper.apiSecureSucc(serie, 'question/poolQuestionStatistic', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.POOL_1);
                    shouldResponseContent.subpoolsIds = ShouldHelper.treatAsList(Data.POOL_1.subpoolsIds, 0, 0);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.POOL_1.questionsIds, 0, 0);
                    shouldResponseContent.mediasIds = ShouldHelper.treatAsList(Data.POOL_1.mediasIds, 0, 0);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.POOL_1.tags, 0, 0);
                    shouldResponseContent.questionCount = 2;
                    shouldResponseContent.questionCountsPerLanguages = [
                        { questionCountPerLanguage: 2, languageId: 1 },
                        { questionCountPerLanguage: 2, languageId: 2 },
                    ];
                    shouldResponseContent.questionCountsPerRegions = [
                        { questionCountPerRegion: 1, regionalSettingId: 1 },
                        { questionCountPerRegion: 1, regionalSettingId: 2 },
                    ];
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.poolQuestionStatistic, ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolQuestionStatistic', function (done) {
                var content = { id: DataIds.POOL_TEST_ID };
                global.wsHelper.apiSecureFail(serie, 'question/poolQuestionStatistic', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, done);
            });
        });

        describe('[' + serie + '] ' + 'poolQuestionStatisticList', function () {
            it('[' + serie + '] ' + 'SUCCESS: poolQuestionStatisticList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_1_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolQuestionStatisticList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                    var shouldResponseContent = _.clone(Data.POOL_1);
                    shouldResponseContent.subpoolsIds = ShouldHelper.treatAsList(Data.POOL_1.subpoolsIds);
                    shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.POOL_1.questionsIds);
                    shouldResponseContent.mediasIds = ShouldHelper.treatAsList(Data.POOL_1.mediasIds);
                    shouldResponseContent.tags = ShouldHelper.treatAsList(Data.POOL_1.tags);
                    shouldResponseContent.questionCount = 2;
                    shouldResponseContent.questionCountsPerLanguages = [
                        { questionCountPerLanguage: 2, languageId: 1 },
                        { questionCountPerLanguage: 2, languageId: 2 },
                    ];
                    shouldResponseContent.questionCountsPerRegions = [
                        { questionCountPerRegion: 1, regionalSettingId: 1 },
                        { questionCountPerRegion: 1, regionalSettingId: 2 }
                    ];
                    ShouldHelper.deepEqual(shouldResponseContent, responseContent.items[0], ['createDate',
                        'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                        'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                        'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                    ]);
                }, done);
            });
            it('[' + serie + '] ' + 'FAILURE ERR_DATABASE_NO_RECORD_FOUND: poolQuestionStatisticList', function (done) {
                var content = {
                    limit: Config.rdbms.limit,
                    offset: 0,
                    searchBy: { id: DataIds.POOL_TEST_ID },
                    orderBy: [{ field: 'name', direction: 'asc' }]
                };
                global.wsHelper.apiSecureSucc(serie, 'question/poolQuestionStatisticList', ProfileData.CLIENT_INFO_1, content, function (receivedContent) {
                    should.exist(receivedContent);
                    should(receivedContent)
                        .has.property('items')
                        .which.has.length(0);
                }, done);
            });
        });
        
        describe('[' + serie + '] ' + 'pool APIs, tenant specific', function () {
            it('[' + serie + '] ' + 'test pool create/update', function (done) {
                //return done();
                var testSessionTenantId = ProfileData.CLIENT_INFO_2.appConfig.tenantId;
                var content = _.clone(Data.POOL_TEST);
                content.id = null;
                var newItemId;
                
                async.series([
                    // create new item
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/poolCreate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            newItemId = responseContent.pool.id;
                            var shouldResponseContent = _.clone(Data.POOL_TEST);
                            shouldResponseContent.questionsIds = ShouldHelper.treatAsList(Data.POOL_TEST.questionsIds, 0, 0);
                            shouldResponseContent.tags = ShouldHelper.treatAsList(Data.POOL_TEST.tags, 0, 0);
                            ShouldHelper.deepEqual(shouldResponseContent, responseContent.pool, ['createDate', 'subpoolsIds', 'mediasIds',
                                'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                                'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                                'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                            ]);
                        }, next);
                    },
                    // check tenantId is set properly
                    function (next) {
                        PoolHasTenant.findOne({ where: { poolId: newItemId, tenantId: testSessionTenantId } }).then(function (item) {
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
                        var content = { id: newItemId, name: 'new title' };
                        global.wsHelper.apiSecureSucc(serie, 'question/poolUpdate', ProfileData.CLIENT_INFO_2, content, function (responseContent) {
                            should(responseContent.pool.name).be.equal(content.name);
                        }, next);
                    },
                    
                    // check tenantId is set properly
                    function (next) {
                        PoolHasTenant.findOne({ where: { poolId: newItemId, tenantId: testSessionTenantId } }).then(function (item) {
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
             
            it('[' + serie + '] ' + 'test pool  get/list', function (done) {
                //return done();
                var item = _.clone(Data.POOL_TEST);
                item.id = null;
                var tenant1Items = [_.clone(item), _.clone(item)];
                var tenant2Items = [_.clone(item), _.clone(item)];
                var testSessionTenantId;
                async.series([
                    // create tenant 1 items
                    function (next) {
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'question/poolCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                                item.id = responseContent.pool.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.questionsIds = ShouldHelper.treatAsList(item.questionsIds, 0, 0);
                                shouldResponseContent.tags = ShouldHelper.treatAsList(item.tags, 0, 0);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.pool, ['createDate', 'subpoolsIds', 'mediasIds',
                                    'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                                    'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                                    'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                                ]);
                            }, cbItem);
                       }, next);
                    },
                    
                    // create tenant 2 items
                    function (next) {
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'question/poolCreate', ProfileData.CLIENT_INFO_2, item, function (responseContent) {
                                item.id = responseContent.pool.id;
                                var shouldResponseContent = _.clone(item);
                                shouldResponseContent.questionsIds = ShouldHelper.treatAsList(item.questionsIds, 0, 0);
                                shouldResponseContent.tags = ShouldHelper.treatAsList(item.tags, 0, 0);
                                ShouldHelper.deepEqual(shouldResponseContent, responseContent.pool, ['createDate', 'subpoolsIds', 'mediasIds',
                                    'stat_assignedQuestions', 'stat_assignedWorkorders', 'stat_numberOfLanguages',
                                    'stat_questionsComplexityLevel1', 'stat_questionsComplexityLevel2', 'stat_questionsComplexityLevel3',
                                    'stat_questionsComplexityLevel4', 'stat_regionsSupported'
                                ]);
                            }, cbItem);
                       }, next);
                    },
                    
                    // get success
                    function (next) {
                        var itm = _.clone(tenant1Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureSucc(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
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
                            orderBy: [{ field: 'name', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                        }, next);
                    },
                    
                    // get error, not belong to session tenant
                    function (next) {
                        var itm = _.clone(tenant2Items[0]);
                        var content = { id: itm.id };
                        global.wsHelper.apiSecureFail(serie, 'question/poolGet', ProfileData.CLIENT_INFO_1, content, Errors.DatabaseApi.NoRecordFound, next);
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
                        global.wsHelper.apiSecureSucc(serie, 'question/poolList', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            should.exist(responseContent);
                            should(responseContent).has.property('items').which.has.length(0);
                        }, next);
                    },
                    // delete items
                    function (next) {
                        async.mapSeries(tenant1Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'question/poolDelete', ProfileData.CLIENT_INFO_1, {id: item.id}, function (responseContent) {
                            }, cbItem);
                       }, next);
                    },
                    // delete items
                    function (next) {
                        async.mapSeries(tenant2Items, function (item, cbItem) {
                            global.wsHelper.apiSecureSucc(serie, 'question/poolDelete', ProfileData.CLIENT_INFO_2, {id: item.id}, function (responseContent) {
                            }, cbItem);
                       }, next);
                    }
                    
                ], done);
            });
            
        });
        
        
    });
});
