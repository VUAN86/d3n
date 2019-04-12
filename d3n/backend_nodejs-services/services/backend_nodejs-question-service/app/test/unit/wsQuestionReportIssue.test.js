var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationIssue = Database.RdbmsService.Models.Question.QuestionOrTranslationIssue;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/question.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var QuestionService = require('./../../services/questionService.js');
var CryptoService = require('./../../services/cryptoService.js');
var PublishService = require('./../../services/publishService.js');
var WorkflowCommon = require('./../../workflows/WorkflowCommon.js');
var QuestionWorkflow = require('./../../workflows/QuestionWorkflow.js');
var QuestionTranslationWorkflow = require('./../../workflows/QuestionTranslationWorkflow.js');
var QuestionApiFactory = require('./../../factories/questionApiFactory.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikePool = KeyvalueService.Models.AerospikePool;
var AerospikePoolMeta = KeyvalueService.Models.AerospikePoolMeta;
var AerospikePoolIndex = KeyvalueService.Models.AerospikePoolIndex;
var AerospikePoolIndexMeta = KeyvalueService.Models.AerospikePoolIndexMeta;

describe('WS Question Report Issue API', function () {
    this.timeout(10000);
    global.wsHelper.series().forEach(function (serie) {
        
        describe('[' + serie + '] ' + 'Report Issue', function () {
            var blobKeys = ['a-df6eee3c-ee9c-4e83-8212-24c738930491', 'b-259d7697-794a-49b1-ae90-f6b6c06d8a39'];
            var translationId = Data.QUESTION_1_TRANSLATION_EN.id;
            var questionId = Data.QUESTION_1_TRANSLATION_EN.questionId;
            it('[' + serie + '] ' + 'QuestionApiFactory._questionIssueGetTranslationByBlobKey()', function (done) {
                async.series([
                    // update questionBlobKeys field
                    function (next) {
                        try {
                            var update = {
                                questionBlobKeys: blobKeys
                            };
                            QuestionTranslation.update(update, {where: {id: translationId}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // find success
                    function (next) {
                        try {
                            QuestionApiFactory._questionIssueGetTranslationByBlobKey(blobKeys[0], function (err, trans) {
                                try {
                                    if(err) {
                                        return next(err);
                                    }
                                    assert.strictEqual(trans.id, translationId);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // find success
                    function (next) {
                        try {
                            QuestionApiFactory._questionIssueGetTranslationByBlobKey(blobKeys[1], function (err, trans) {
                                try {
                                    if(err) {
                                        return next(err);
                                    }
                                    assert.strictEqual(trans.id, translationId);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // entry not found
                    function (next) {
                        try {
                            QuestionApiFactory._questionIssueGetTranslationByBlobKey(blobKeys[1] + 'a', function (err, trans) {
                                try {
                                    assert.strictEqual(err, Errors.DatabaseApi.NoRecordFound);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], done);
            });
            
            
            it('[' + serie + '] ' + 'QuestionApiFactory._questionIssueNeedEscalate()', function (done) {
                var translation = null;
                
                function _addIssueForTranslation(cb) {
                    var dataIssue = {
                        issueFor: QuestionOrTranslationIssue.constants().ISSUE_FOR_TRANSLATION,
                        questionId: null,
                        questionTranslationId: translationId,
                        version: 1,
                        resourceId: '1111',
                        issueType: QuestionOrTranslationIssue.constants().ISSUE_TYPE_SPELLING_AND_GRAMMAR,
                        issueText: null
                    };

                    var dataEvent = {
                        questionId: null,
                        questionTranslationId: translationId,
                        resourceId: '1111',
                        triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER
                    };


                    WorkflowCommon.addIssueAndEventLog(dataIssue, dataEvent, cb);
                }
                
                async.series([
                    // load trans
                    function (next) {
                        try {
                            QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                if(!dbItem) {
                                    return next('ENTRY_NOT_FOUND'); 
                                }
                                
                                translation = dbItem.get({plain: true});
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    
                    function (next) {
                        try {
                            _addIssueForTranslation(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },

                    // needEscalate = false
                    function (next) {
                        try {
                            QuestionApiFactory._questionIssueNeedEscalate({
                                isQuestionIssue: false,
                                questionId: translation.questionId,
                                questionTranslationId: translation.id,
                                version: 1
                            }, function (err, res) {
                                try {
                                    assert.ifError(err);
                                    assert.strictEqual(res, false);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // add 4 issues
                    function (next) {
                        try {
                            async.timesSeries(4, function (i, cbItem) {
                                _addIssueForTranslation(cbItem);
                            }, next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // needEscalate = true
                    function (next) {
                        try {
                            QuestionApiFactory._questionIssueNeedEscalate({
                                isQuestionIssue: false,
                                questionId: translation.questionId,
                                questionTranslationId: translation.id,
                                version: 1
                            }, function (err, res) {
                                try {
                                    assert.ifError(err);
                                    assert.strictEqual(res, true);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },

                ], done);
            });
            
            
            it('[' + serie + '] ' + 'question/questionReportIssue', function (done) {
                //return done()
                var translation = null;
                
                function _addIssueForTranslation(cb) {
                    var content = {
                        blobKey: blobKeys[0],
                        issueType: QuestionOrTranslationIssue.constants().ISSUE_TYPE_SPELLING_AND_GRAMMAR
                    };
                    global.wsHelper.apiSecureSucc(serie, 'question/questionReportIssue', ProfileData.CLIENT_INFO_1, content, null, function () {
                        return cb();
                    });
                }
                
                async.series([
                    // delete events
                    function (next) {
                        try {
                            QuestionOrTranslationEventLog.destroy({where: {id: {'$gt': 0}}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // delete issues
                    function (next) {
                        try {
                            QuestionOrTranslationIssue.destroy({where: {id: {'$gt': 0}}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },

                    // update questionBlobKeys field
                    function (next) {
                        try {
                            var update = {
                                questionBlobKeys: blobKeys,
                                publishedVersion: 1
                            };
                            QuestionTranslation.update(update, {where: {id: translationId}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    
                    // load trans
                    function (next) {
                        try {
                            QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                if(!dbItem) {
                                    return next('ENTRY_NOT_FOUND'); 
                                }
                                
                                translation = dbItem.get({plain: true});
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    function (next) {
                        try {
                            _addIssueForTranslation(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // not escalated
                    function (next) {
                        try {
                            QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isEscalated, null);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // add 4 issues
                    function (next) {
                        try {
                            async.timesSeries(4, function (i, cbItem) {
                                _addIssueForTranslation(cbItem);
                            }, next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // escalated
                    function (next) {
                        try {
                            QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isEscalated, 1);
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
            
            it('[' + serie + '] ' + 'question/questionReportIssue, escalate question', function (done) {
                //return done()
                var translation = null;
                
                function _addIssueForQuestion(cb) {
                    var content = {
                        blobKey: blobKeys[0],
                        issueType: QuestionOrTranslationIssue.constants().ISSUE_TYPE_NOT_CLEAR
                    };
                    global.wsHelper.apiSecureSucc(serie, 'question/questionReportIssue', ProfileData.CLIENT_INFO_1, content, null, function () {
                        return cb();
                    });
                }
                
                async.series([
                    // delete events
                    function (next) {
                        try {
                            QuestionOrTranslationEventLog.destroy({where: {id: {'$gt': 0}}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // delete issues
                    function (next) {
                        try {
                            QuestionOrTranslationIssue.destroy({where: {id: {'$gt': 0}}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },

                    // update questionBlobKeys field
                    function (next) {
                        try {
                            var update = {
                                questionBlobKeys: blobKeys,
                                publishedVersion: 1
                            };
                            QuestionTranslation.update(update, {where: {id: translationId}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // update questin version
                    function (next) {
                        try {
                            var update = {
                                publishedVersion: 1
                            };
                            Question.update(update, {where: {id: questionId}}).then(function () {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    
                    // load trans
                    function (next) {
                        try {
                            QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                if(!dbItem) {
                                    return next('ENTRY_NOT_FOUND'); 
                                }
                                
                                translation = dbItem.get({plain: true});
                                //console.log('>>>>.translation:', translation);
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    function (next) {
                        try {
                            _addIssueForQuestion(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // not escalated
                    function (next) {
                        try {
                            Question.findOne({where: {id: translation.questionId}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isEscalated, null);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // add 2 issues
                    function (next) {
                        try {
                            async.timesSeries(2, function (i, cbItem) {
                                _addIssueForQuestion(cbItem);
                            }, next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // escalated
                    function (next) {
                        try {
                            Question.findOne({where: {id: translation.questionId}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isEscalated, 1);
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
        });
        
        describe('[' + serie + '] ' + 'question resolve', function () {
            var questionId = Data.QUESTION_1.id;
            it('[' + serie + '] ' + 'QuestionApi.questionIssueResolve', function (done) {
                var stubCommitForReview = global.wsHelper.sinonSandbox.stub(QuestionWorkflow.prototype, 'questionCommitForReview', function (message, clientSession, callback) {
                    return setImmediate(callback);
                });
                
                async.series([
                    function (next) {
                        try {
                            var set = {
                                isEscalated: 1,
                                status: Question.constants().STATUS_APPROVED
                            };
                            QuestionTranslation.update(set, { where: { id: questionId }, individualHooks: false }).then(function (count) {
                                return next();
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },

                    function (next) {
                        try {
                            var content = {
                                id: questionId
                            };
                            
                            global.wsHelper.apiSecureSucc(serie, 'question/questionIssueResolve', ProfileData.CLIENT_INFO_1, content, null, next);
                            
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    function (next) {
                        try {
                            Question.findOne({where: {id: questionId}}).then(function (dbItem) {
                                try {
                                    var item = dbItem.get({plain: true});
                                    assert.strictEqual(item.isEscalated, 0);
                                    return next();
                                } catch (e) {
                                    return nex(e);
                                }
                            }).catch(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },

                    function (next) {
                        try {
                            assert.strictEqual(stubCommitForReview.callCount, 1);
                            assert.strictEqual(stubCommitForReview.lastCall.args[0].getContent().id, questionId);
                            return setImmediate(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    function (next) {
                        try {
                            stubCommitForReview.reset();
                            return setImmediate(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], done);
            });
        });
        
        describe('[' + serie + '] ' + 'translation resolve', function () {
            var translationId = Data.QUESTION_1_TRANSLATION_EN.id;
            it('[' + serie + '] ' + 'QuestionApi.questionTranslationIssueResolve', function (done) {
                var stubCommitForReview = global.wsHelper.sinonSandbox.stub(QuestionTranslationWorkflow.prototype, 'questionTranslationCommitForReview', function (message, clientSession, callback) {
                    return setImmediate(callback);
                });
                
                async.series([
                    function (next) {
                        try {
                            var content = {
                                id: translationId
                            };
                            
                            global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationIssueResolve', ProfileData.CLIENT_INFO_1, content, null, next);
                            
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    function (next) {
                        try {
                            assert.strictEqual(stubCommitForReview.callCount, 1);
                            assert.strictEqual(stubCommitForReview.lastCall.args[0].getContent().id, translationId);
                            return setImmediate(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    function (next) {
                        try {
                            stubCommitForReview.reset();
                            return setImmediate(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], done);
            });
        });
        
    });
});
