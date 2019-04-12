var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/question.data.js');
var DataBilling = require('./../config/billing.data.js');
var DataWorkorder = require('./../config/workorder.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var Question = Database.RdbmsService.Models.Question.Question;
var PaymentAction = Database.RdbmsService.Models.Billing.PaymentAction;
var PaymentStructure = Database.RdbmsService.Models.Billing.PaymentStructure;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var Tag = Database.RdbmsService.Models.Question.Tag;
var ProtocolMessage = require('nodejs-protocol');
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var ProfileManagerFakeService = require('../services/profileManager.fakeService.js');
var QuestionWorkflow = require('../../workflows/QuestionWorkflow.js');
var WorkflowCommon = require('../../workflows/WorkflowCommon.js');
var QuestionTranslationWorkflow = require('../../workflows/QuestionTranslationWorkflow.js');
var QuestionTranslationApiFactory = require('./../../factories/questionTranslationApiFactory.js');
var tenantService = require('../../services/tenantService.js');
var AnalyticEvent = require('../../services/analyticEvent.js');

describe('WORKFLOW QUESTION', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        before(function (done) {

            QuestionWorkflow.removeAllInstances();
            QuestionTranslationWorkflow.removeAllInstances();

            return setImmediate(done);
        });
        after(function (done) {
            return setImmediate(done);
        });
        
        /*
        describe('[' + serie + '] ' + 'questionUpdate allow/disallow', function () {
            it('allow/disallow', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var content = {
                    id: DataIds.QUESTION_1_ID,
                    poolsIds: Data.QUESTION_TEST.poolsIds,
                    accessibleDate: '2016-10-10',
                    expirationDate: '2016-11-10',
                    renewDate: '2016-10-20',
                    workorderId: null
                };
                
                async.series([
                    // update allowed
                    function (next) {
                        global.wsHelper.sinonSandbox.stub(QuestionWorkflow, "hasWorkflow", function (id, cb) {
                            return setImmediate(cb, false, false);
                        });
                        
                        global.wsHelper.apiSecureSucc(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                        }, next);
                    },
                    
                    // update not allowed
                    function (next) {
                        QuestionWorkflow.hasWorkflow.restore();
                        global.wsHelper.sinonSandbox.stub(QuestionWorkflow, "hasWorkflow", function (id, cb) {
                            return setImmediate(cb, false, true);
                        });
                        
                        global.wsHelper.apiSecureFail(serie, 'question/questionUpdate', ProfileData.CLIENT_INFO_1, content, Errors.QuestionApi.ValidationFailed, next);
                    },
                    
                    function (next) {
                        QuestionWorkflow.hasWorkflow.restore();
                        return setImmediate(next);
                    }
                    
                    
                ], done);
            });
        });
        */
        describe('[' + serie + '] ' + 'WorkflowCommon', function () {
            
            it('closeWorkorder, workorderId=null', function (done) {
                var questionId;
                async.series([
                    // create question with workorderId=null
                    function (next) {
                        var content = _.clone(Data.QUESTION_TEST);
                        content.workorderId = null;
                        content.id = null;
                        
                        Question.create(content).then(function (dbItem) {
                            questionId = dbItem.get({plain: true}).id;
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    function (next) {
                        WorkflowCommon.closeWorkorder(questionId, next);
                    }
                ], done);
            });
            
            it('closeWorkorder, not all questions approved => does not close', function (done) {
                var questionId;
                var workorderId;
                async.series([
                    // create workorder
                    function (next) {
                        var content = _.clone(DataWorkorder.WORKORDER_TEST);
                        content.status = Workorder.constants().STATUS_DRAFT;
                        content.id = null;
                        
                        Workorder.create(content).then(function (dbItem) {
                            workorderId = dbItem.get({plain: true}).id;
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // create question
                    function (next) {
                        var content = _.clone(Data.QUESTION_TEST);
                        content.workorderId = workorderId;
                        content.status = Question.constants().STATUS_DRAFT;
                        content.id = null;
                        
                        Question.create(content).then(function (dbItem) {
                            questionId = dbItem.get({plain: true}).id;
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    function (next) {
                        WorkflowCommon.closeWorkorder(questionId, next);
                    },
                    // workorder status not changed
                    function (next) {
                        Workorder.findOne({where: {id: workorderId}}).then(function (dbItem) {
                            try {
                                
                                assert.strictEqual(dbItem.get({plain: true}).status, Workorder.constants().STATUS_DRAFT);
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
            
            it('closeWorkorder, all questions approved => does close', function (done) {
                var questionId;
                var workorderId;
                async.series([
                    // create workorder
                    function (next) {
                        var content = _.clone(DataWorkorder.WORKORDER_TEST);
                        content.status = Workorder.constants().STATUS_DRAFT;
                        content.id = null;
                        
                        Workorder.create(content).then(function (dbItem) {
                            workorderId = dbItem.get({plain: true}).id;
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // create question
                    function (next) {
                        var content = _.clone(Data.QUESTION_TEST);
                        content.workorderId = workorderId;
                        content.status = Question.constants().STATUS_APPROVED;
                        content.id = null;
                        
                        Question.create(content).then(function (dbItem) {
                            questionId = dbItem.get({plain: true}).id;
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    function (next) {
                        WorkflowCommon.closeWorkorder(questionId, next);
                    },
                    // workorder status = closed
                    function (next) {
                        Workorder.findOne({where: {id: workorderId}}).then(function (dbItem) {
                            try {
                                
                                assert.strictEqual(dbItem.get({plain: true}).status, Workorder.constants().STATUS_CLOSED);
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
        });
        
        describe('[' + serie + '] ' + 'question basic tests', function () {
            
            it('[' + serie + '] ' + 'question workflow test review type', function (done) {
                async.series([
                    // workorderId = null => COMMUNITY
                    function (next) {
                        var questionItem = _.clone(Data.QUESTION_TEST);
                        questionItem.id = null;
                        questionItem.workorderId = null;
                        Question.create(questionItem).then(function (question) {
                            
                            var questionId = question.get({ plain: true }).id;
                            var inst = new QuestionWorkflow(questionId);
                            
                            inst._decideReviewType(function (err, reviewType) {
                                try {
                                    if (err) {
                                        return next(err);
                                    }
                                    assert.strictEqual(reviewType, 'EXTERNAL');
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // workorderId != null and questionReviewIsCommunity=true => COMMUNITY
                    function (next) {
                        var workorderItem = _.clone(DataWorkorder.WORKORDER_TEST);
                        workorderItem.id = null;
                        workorderItem.questionReviewIsCommunity=1;
                        
                        Workorder.create(workorderItem).then(function (workorder) {
                            
                            var workorderId = workorder.get({ plain: true }).id;
                            var questionItem = _.clone(Data.QUESTION_TEST);
                            questionItem.id = null;
                            questionItem.workorderId = workorderId;
                            Question.create(questionItem).then(function (question) {

                                var questionId = question.get({ plain: true }).id;
                                var inst = new QuestionWorkflow(questionId);

                                inst._decideReviewType(function (err, reviewType) {
                                    try {
                                        if (err) {
                                            return next(err);
                                        }
                                        assert.strictEqual(reviewType, 'COMMUNITY');
                                        return next();
                                    } catch (e) {
                                        return next(e);
                                    }
                                });
                            }).catch(function (err) {
                                return next(err);
                            });
                            
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // workorderId != null and questionReviewIsCommunity=false => EXTERNAL
                    function (next) {
                        var workorderItem = _.clone(DataWorkorder.WORKORDER_TEST);
                        workorderItem.id = null;
                        workorderItem.questionReviewIsCommunity=0;
                        
                        Workorder.create(workorderItem).then(function (workorder) {
                            
                            var workorderId = workorder.get({ plain: true }).id;
                            var questionItem = _.clone(Data.QUESTION_TEST);
                            questionItem.id = null;
                            questionItem.workorderId = workorderId;
                            Question.create(questionItem).then(function (question) {

                                var questionId = question.get({ plain: true }).id;
                                var inst = new QuestionWorkflow(questionId);

                                inst._decideReviewType(function (err, reviewType) {
                                    try {
                                        if (err) {
                                            return next(err);
                                        }
                                        assert.strictEqual(reviewType, 'EXTERNAL');
                                        return next();
                                    } catch (e) {
                                        return next(e);
                                    }
                                });
                            }).catch(function (err) {
                                return next(err);
                            });
                            
                        }).catch(function (err) {
                            return next(err);
                        });
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'question workflow test instance removed on perform action', function (done) {
                //return done();
                process.env.WORKFLOW_ENABLED='true';
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "perform", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        triggeredActionType: 'Publish',
                        availableActionTypes: ['x', 'y'],
                        processFinished: true
                    });
                    
                    clientSession.sendMessage(response);
                });
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        availableActionTypes: ['dsdad', 'ExternalReview'],
                        processFinished: false
                    });
                    
                    clientSession.sendMessage(response);
                });

                var userId = DataIds.WF_1_USER_ID;
                var token = ProfileData._encodeToken({userId:userId, roles:['TENANT_' + DataIds.TENANT_1_ID + '_EXTERNAL']});
                var tokenReviewer = ProfileData._encodeToken({ userId: DataIds.WF_2_USER_ID, roles:['TENANT_' + DataIds.TENANT_1_ID + '_EXTERNAL']});
                var content = _.clone(Data.QUESTION_TEST);
                content.id = null;
                
                var itemId, translationId;
                async.series([
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            itemId = responseContent.question.id;
                            assert.strictEqual(QuestionWorkflow.hasInstance(itemId), false);
                        }, next);
                    },
                    function (next) {
                        var contentTranslation = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        contentTranslation.id = null;
                        contentTranslation.questionId = itemId;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', ProfileData.CLIENT_INFO_1, contentTranslation, function (responseContent) {
                        }, next);
                        
                    },
                    
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCommitForReview', ProfileData.CLIENT_INFO_1, {id: itemId}, function (responseContent) {
                            assert.strictEqual(QuestionWorkflow.hasInstance(itemId), true);
                        }, next);
                    },
                    
                    function (next) {
                        var content = {
                            id: itemId,
                            difficulty: 2,
                            rating: 2,
                            isAccepted: 1
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/questionReview', ProfileData.CLIENT_INFO_1, content, null, function () {
                            setTimeout(function () {
                                try {
                                    assert.strictEqual(QuestionWorkflow.hasInstance(itemId), false);
                                    next();
                                } catch (e) {
                                    next(e);
                                }
                            }, 500);
                        });
                    }
                ], done);
                
            });
            
            it('[' + serie + '] ' + 'question workflow test instance removed on start', function (done) {
                //return done()
                process.env.WORKFLOW_ENABLED='true';
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "start", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        availableActionTypes: ['x', 'y'],
                        processFinished: true
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                var userId = DataIds.WF_1_USER_ID;
                var token = ProfileData._encodeToken({userId:userId, roles:['TENANT_' + DataIds.TENANT_1_ID + '_EXTERNAL']});
                var tokenReviewer = ProfileData._encodeToken({ userId: DataIds.WF_2_USER_ID, roles:['TENANT_' + DataIds.TENANT_1_ID + '_EXTERNAL']});
                var content = _.clone(Data.QUESTION_TEST);
                content.id = null;
                
                var itemId, translationId;
                async.series([
                    
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, content, function (responseContent) {
                            itemId = responseContent.question.id;
                            assert.strictEqual(QuestionWorkflow.hasInstance(itemId), false);
                        }, next);
                    },
                    
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCommitForReview', ProfileData.CLIENT_INFO_1, {id: itemId}, function (responseContent) {
                            assert.strictEqual(QuestionWorkflow.hasInstance(itemId), false);
                        }, next);
                    }
                ], done);
                
            });
            
            it('[' + serie + '] ' + 'WorkflowCommon.addReview()', function (done) {
                async.series([
                    function (next) {
                        emptyReviewAndEventLog(next);
                    },
                    
                    // add review for question
                    function (next) {
                        WorkflowCommon.addReview({
                            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_QUESTION,
                            questionId: 1,
                            rating: 2,
                            difficulty: 3,
                            isAccepted: 0,
                            resourceId: DataIds.LOCAL_USER_ID
                        }, next);
                    },
                    // add review for translation
                    function (next) {
                        WorkflowCommon.addReview({
                            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_TRANSLATION,
                            questionTranslationId: 5,
                            rating: 2,
                            difficulty: 3,
                            isAccepted: 0,
                            resourceId: DataIds.LOCAL_USER_ID
                        }, next);
                    },
                    
                    // add event log
                    function (next) {
                        WorkflowCommon.addEventLog({
                            questionId: 1,
                            triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                            resourceId: DataIds.LOCAL_USER_ID,
                            oldStatus: 'a',
                            newStatus: 'b',
                            reviewId: 1,
                            createDate: DateUtils.isoNow()
                        }, next);
                    },
                    // add revie and event log
                    function (next) {
                        WorkflowCommon.addReviewAndEventLog({
                            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_TRANSLATION,
                            questionTranslationId: 5,
                            rating: 2,
                            difficulty: 3,
                            isAccepted: 0,
                            resourceId: DataIds.LOCAL_USER_ID
                        }, {
                            questionId: 1,
                            triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_WORKFLOW,
                            resourceId: DataIds.LOCAL_USER_ID,
                            reviewId: 5,
                            createDate: DateUtils.isoNow()
                        }, next);
                        
                    }
                    
                ], done);
            });
            
        });
        
        describe('[' + serie + '] ' + 'question workflow', function () {
            
            it('[' + serie + '] ' + 'question workflow', function (done) {
                //return done();
                process.env.WORKFLOW_ENABLED='true';
                var admins = [DataIds.WF_1_USER_ID, DataIds.WF_2_USER_ID];
                var wsStartSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "start");
                var wsPerformSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "perform");
                var umSendEmailSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._umFake.messageHandlers, "sendEmail");
                var analyticEventSpy = global.wsHelper.sinonSandbox.spy(AnalyticEvent, "addQuestionEvent");
                var profileManagerAdminListByTenantIdStub = global.wsHelper.sinonSandbox.stub(ProfileManagerFakeService.prototype.messageHandlers, 'adminListByTenantId', function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError(null);
                    response.setContent({
                        admins: admins
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        availableActionTypes: ['dsdad', 'ExternalReview'],
                        processFinished: false
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                global.wsHelper.sinonSandbox.stub(QuestionTranslationApiFactory, "questionTranslationUnpublish", function (params, clientSession, callback) {
                    return setImmediate(callback, false, {});
                });
                
                global.wsHelper.sinonSandbox.stub(QuestionTranslationApiFactory, "questionTranslationPublish", function (params, message, clientSession, callback) {
                    return setImmediate(callback, false, {});
                });
                
                var userId1 = DataIds.WF_3_USER_ID;
                var userId2 = DataIds.WF_4_USER_ID;
                
                /*
                var tokenCreator = ProfileData._encodeToken({userId:userId1, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                var tokenReviewer = ProfileData._encodeToken({userId:userId2, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                */
                var tokenCreator = ProfileData.CLIENT_INFO_1; tokenCreator.profile.userId = userId1;
                var tokenReviewer = ProfileData.CLIENT_INFO_3; tokenReviewer.profile.userId = userId2;
                
                
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                var translationId;
                async.series([
                    // remove all reviews and events log
                    function (next) {
                        emptyReviewAndEventLog(next);
                    },
                    
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', tokenCreator, item, function (responseContent) {
                            item.id = responseContent.question.id;
                            assert.strictEqual(wsStartSpy.callCount, 0);
                            assert.strictEqual(analyticEventSpy.callCount, 1);
                            assert.deepEqual(analyticEventSpy.lastCall.args[1], {questionsCreated: 1});
                        }, next);
                    },
                    // create first translation
                    function (next) {
                        var contentTranslation = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        contentTranslation.id = null;
                        contentTranslation.questionId = item.id;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', tokenCreator, contentTranslation, function (responseContent) {
                            translationId = responseContent.questionTranslation.id;
                            assert.strictEqual(QuestionWorkflow.hasInstance(item.id), false);
                            assert.strictEqual(QuestionTranslationWorkflow.hasInstance(translationId), false);
                            assert.strictEqual(analyticEventSpy.callCount, 2);
                            assert.deepEqual(analyticEventSpy.lastCall.args[1], {questionsTranslated: 1});
                        }, next);
                    },
                    // start workflow
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCommitForReview', ProfileData.CLIENT_INFO_1, {id: item.id}, function (responseContent) {
                            assert.strictEqual(wsStartSpy.callCount, 1);
                            assert.strictEqual(wsStartSpy.getCall(0).args[0].getContent().taskId, 'Question_' + item.id);
                            assert.strictEqual(wsStartSpy.getCall(0).args[0].getContent().userId, '' + userId1);
                            assert.strictEqual(wsStartSpy.getCall(0).args[0].getContent().tenantId, '' + DataIds.TENANT_1_ID);
                            assert.strictEqual(QuestionWorkflow.hasInstance(item.id), true);
                            assert.strictEqual(QuestionTranslationWorkflow.hasInstance(translationId), false);
                        }, next);
                    },
                    
                    
                    // approve
                    function (next) {
                        wsPerformSpy.reset();
                        var content = {
                            id: item.id,
                            rating:2,
                            difficulty: 5,
                            isAccepted: 1
                        };
                        analyticEventSpy.reset();
                        
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/questionReview', tokenReviewer, content, null, function () {
                            setTimeout(function () {
                                try {
                                    //assert.strictEqual(wsPerformSpy.callCount, 1);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'Question_' + item.id);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().userId, '' + userId2);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().tenantId, '' + DataIds.TENANT_1_ID);
                                    assert.include(['InternalReview', 'ExternalReview', 'CommunityReview'], wsPerformSpy.lastCall.args[0].getContent().actionType);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.reviewOutcome, 'Approved');
                                    assert.strictEqual(analyticEventSpy.callCount, 3);
                                    
                                    next();
                                } catch (e) {
                                    next(e);
                                }
                            }, 500);
                        });
                    },
                    
                    // reject
                    function (next) {
                        wsPerformSpy.reset();
                        var content = {
                            id: item.id,
                            rating:2,
                            difficulty: 5,
                            isAccepted: 0,
                            errorType: QuestionOrTranslationReview.constants().ERROR_TYPE_TOO_EASY,
                            errorText: 'something'
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/questionReview', tokenReviewer, content, null, function () {
                            setTimeout(function () {
                                try {
                                    //assert.strictEqual(wsPerformSpy.callCount, 3);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'Question_' + item.id);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().userId, '' + userId2);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().tenantId, '' + DataIds.TENANT_1_ID);
                                    assert.include(['InternalReview', 'ExternalReview', 'CommunityReview'], wsPerformSpy.lastCall.args[0].getContent().actionType);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.reviewOutcome, 'Rejected');
                                    next();
                                } catch (e) {
                                    next(e);
                                }
                            }, 500);
                        });
                    },
                    
                    // test SetStatus event
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(item.id);
                        var eventStatusToDbStatus = {
                            'Approved': Question.constants().STATUS_APPROVED,
                            'Rejected': Question.constants().STATUS_DECLINED,
                            'Published': Question.constants().STATUS_ACTIVE,
                            'Unpublished': Question.constants().STATUS_INACTIVE,
                            'Archived': Question.constants().STATUS_ARCHIVED,
                            'Republished': Question.constants().STATUS_ACTIVE,
                            'Review': Question.constants().STATUS_REVIEW
                        };
                        var items = _.keys(eventStatusToDbStatus);
                        
                        async.mapSeries(items, function (status, cbItem) {
                            inst._onStateChange({
                                triggeredAction: 'SetStatus(' + status + ')'
                            });
                            
                            setTimeout(function () {
                                async.series([
                                    function (next) {
                                        Question.findOne({where: {id: item.id}}).then(function (dbItem) {
                                            try {
                                                dbItem = dbItem.get({plain: true});
                                                assert.strictEqual(dbItem.status, eventStatusToDbStatus[status]);
                                                next();
                                            } catch (e) {
                                                return next(e);
                                            }
                                        }).catch(function (err) {
                                            next(err);
                                        });
                                    },
                                    // first ranslation updated also
                                    function (next) {
                                        QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                            try {
                                                dbItem = dbItem.get({plain: true});
                                                assert.strictEqual(dbItem.status, eventStatusToDbStatus[status]);
                                                next();
                                            } catch (e) {
                                                return next(e);
                                            }
                                        }).catch(function (err) {
                                            next(err);
                                        });
                                    }
                                    
                                ], cbItem);
                                
                                
                            }, 500);
                        }, next);
                    },
                    
                    // emit SetPriority(High) event
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(item.id);
                        
                        inst._onStateChange({
                            triggeredAction: 'SetPriority(High)'
                        });
                        
                        setTimeout(function () {
                            
                            async.series([
                                function (next1) {
                                    Question.findOne({where: {id: item.id}}).then(function (dbItem) {
                                        try {
                                            dbItem = dbItem.get({plain: true});
                                            assert.strictEqual(dbItem.priority, Question.constants().PRIORITY_HIGH);
                                            next1();
                                        } catch (e) {
                                            return next1(e);
                                        }
                                    }).catch(function (err) {
                                        next1(err);
                                    });
                                },
                                
                                function (next1) {
                                    QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                        try {
                                            dbItem = dbItem.get({plain: true});
                                            assert.strictEqual(dbItem.priority, QuestionTranslation.constants().PRIORITY_HIGH);
                                            next1();
                                        } catch (e) {
                                            return next1(e);
                                        }
                                    }).catch(function (err) {
                                        next1(err);
                                    });
                                }
                                
                            ], next);
                            
                        }, 500);
                    },
                    
                    // emit SendEmail(Author) event
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(item.id);
                        
                        inst._onStateChange({
                            triggeredAction: 'SendEmail(Author)'
                        });
                        
                        setTimeout(function () {
                            try {
                                assert.strictEqual(umSendEmailSpy.callCount, 1);
                                assert.strictEqual(umSendEmailSpy.lastCall.args[0].getContent().userId, '' + userId1);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }, 500);
                    },
                    
                    // emit SendEmail(Admin) event
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(item.id);
                        umSendEmailSpy.reset();
                        
                        inst._onStateChange({
                            triggeredAction: 'SendEmail(Admin)'
                        });
                        
                        setTimeout(function () {
                            try {
                                assert.strictEqual(umSendEmailSpy.callCount, 2);
                                assert.strictEqual(umSendEmailSpy.getCall(0).args[0].getContent().userId, '' + admins[0]);
                                assert.strictEqual(umSendEmailSpy.getCall(1).args[0].getContent().userId, '' + admins[1]);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }, 500);
                    },
                    
                    // emit publish event
                    function (next) {
                        // fix here
                        //return next();
                        var inst = QuestionWorkflow.getInstance(item.id);
                        wsPerformSpy.reset();
                        inst._onStateChange({
                            triggeredAction: 'Publish'
                        });
                        
                        setTimeout(function () {
                            Question.findOne({where: {id: item.id}}).then(function (dbItem) {
                                try {
                                    dbItem = dbItem.get({plain: true});
                                    //assert.strictEqual(dbItem.deploymentStatus, Question.constants().DEPLOYMENT_STATUS_DEPLOYED);
                                    
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'Question_' + item.id);
                                    assert.isUndefined(wsPerformSpy.lastCall.args[0].getContent().userId);
                                    assert.isUndefined(wsPerformSpy.lastCall.args[0].getContent().tenantId);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().actionType, 'Publish');
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.result, 'Success');
                                    
                                    next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                next(err);
                            });
                        }, 500);
                    },
                    
                    // emit archive event
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(item.id);
                        wsPerformSpy.reset();
                        inst._onStateChange({
                            triggeredAction: 'Archive'
                        });
                        
                        setTimeout(function () {
                            Question.findOne({where: {id: item.id}}).then(function (dbItem) {
                                try {
                                    dbItem = dbItem.get({plain: true});
                                    //assert.strictEqual(dbItem.status, Question.constants().STATUS_ARCHIVED);
                                    
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'Question_' + item.id);
                                    assert.isUndefined(wsPerformSpy.lastCall.args[0].getContent().userId);
                                    assert.isUndefined(wsPerformSpy.lastCall.args[0].getContent().tenantId);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().actionType, 'Archive');
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.result, 'Success');
                                    
                                    next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                next(err);
                            });
                        }, 500);
                    }
                ], done);
            });
            
        });
        
        describe('[' + serie + '] ' + 'test billing', function () {
            
            beforeEach(function (done) {
                async.series([
                    // remove all review
                    function (next) {
                        emptyReviewAndEventLog(next);
                    },
                    // remove all payment action
                    function (next) {
                        PaymentAction.destroy({truncate: true}).then(function () {
                            PaymentAction.count().then(function(c) {
                                try {
                                    assert.strictEqual(c, 0);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });                       
                        }).catch(function (err) {
                            return next(err);
                        });
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'no workorder => no payment', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        availableActionTypes: ['dsdad', 'ExternalReview'],
                        processFinished: false
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                
                var userId1 = DataIds.WF_5_USER_ID;
                var userId2 = DataIds.WF_6_USER_ID;
                /*
                var tokenCreator = ProfileData._encodeToken({userId:userId1, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                var tokenReviewer = ProfileData._encodeToken({userId:userId2, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                */
                var tokenCreator = ProfileData.CLIENT_INFO_1; tokenCreator.profile.userId = userId1;
                var tokenReviewer = ProfileData.CLIENT_INFO_3; tokenReviewer.profile.userId = userId2;
                
                
                var item = _.clone(Data.QUESTION_TEST);
                var questionId = null;
                var translationId = null;
                
                async.series([
                    // create question
                    function (next) {
                        item.id = null;
                        item.workorderId = null;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', tokenCreator, item, function (responseContent) {
                            questionId = responseContent.question.id;
                        }, next);
                    },
                    // create first translation
                    function (next) {
                        var contentTranslation = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        contentTranslation.workorderId = null;
                        contentTranslation.id = null;
                        contentTranslation.questionId = questionId;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', tokenCreator, contentTranslation, function (responseContent) {
                            translationId = responseContent.questionTranslation.id;
                        }, next);
                    },
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCommitForReview', tokenCreator, {id: questionId}, function (responseContent) {
                            assert.strictEqual(QuestionWorkflow.hasInstance(questionId), true);
                            assert.strictEqual(QuestionTranslationWorkflow.hasInstance(translationId), false);
                        }, next);
                    },
                    
                    // approve
                    function (next) {
                        var content = {
                            id: questionId,
                            rating: 3,
                            difficulty: 4,
                            isAccepted: 1
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/questionReview', tokenReviewer, content, null, next);
                    },
                    // reviewer record added
                    function (next) {
                        var options = {
                            where: {
                                questionId: questionId
                            }
                        };
                        QuestionOrTranslationReview.findAll(options).then(function(dbItems) {
                            try {
                                assert.strictEqual(dbItems.length, 1);
                                assert.strictEqual(dbItems[0].get({plain: true}).resourceId, userId2);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });                       
                    },
                    // simulate approve, no payment action is added
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(questionId);
                        var managePaymentSpy = global.wsHelper.sinonSandbox.spy(inst, "_managePayment");
                        inst._onStateChange({
                            triggeredAction: 'SetStatus(Approved)'
                        });
                        
                        setTimeout(function () {
                            PaymentAction.count().then(function(c) {
                                try {
                                    assert.strictEqual(managePaymentSpy.callCount, 1);
                                    assert.strictEqual(c, 0);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });                       
                        }, 500);
                    }
                ], done);
                
            });
            
            it('[' + serie + '] ' + 'payment structure not instant => no payment', function (done) {
                //return done();
                process.env.WORKFLOW_ENABLED='true';
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        availableActionTypes: ['dsdad', 'ExternalReview'],
                        processFinished: false
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                
                var userId1 = DataIds.WF_7_USER_ID;
                var userId2 = DataIds.WF_8_USER_ID;
                /*
                var tokenCreator = ProfileData._encodeToken({userId:userId1, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                var tokenReviewer = ProfileData._encodeToken({userId:userId2, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                */
                var tokenCreator = ProfileData.CLIENT_INFO_1; tokenCreator.profile.userId = userId1;
                var tokenReviewer = ProfileData.CLIENT_INFO_3; tokenReviewer.profile.userId = userId2;
                
                
                
                
                var questionId = null;
                var translationId = null;
                var paymentStructure = null;
                var workorder = null;
                async.series([
                    // create payment structure
                    function (next) {
                        var item = _.clone(DataBilling.PAYMENT_STRUCTURE_TEST);
                        item.id = null;
                        item.type = PaymentStructure.constants().TYPE_USAGE;
                        PaymentStructure.create(item).then(function (dbItem) {
                            paymentStructure = dbItem.get({plain: true});
                            return next();
                        }).catch(next);
                    },
                    // create workorder
                    function (next) {
                        var item = _.clone(DataWorkorder.WORKORDER_TEST);
                        item.id = null;
                        item.translationCreatePaymentStructureId = paymentStructure.id;
                        item.translationReviewPaymentStructureId = paymentStructure.id;
                        Workorder.create(item).then(function (dbItem) {
                            workorder = dbItem.get({plain: true});
                            return next();
                        }).catch(next);
                    },
                    
                    // create question
                    function (next) {
                        var item = _.clone(Data.QUESTION_TEST);
                        item.id = null;
                        item.workorderId = workorder.id;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', tokenCreator, item, function (responseContent) {
                            questionId = responseContent.question.id;
                        }, next);
                    },
                    
                    
                    // create first translation
                    function (next) {
                        var item = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        item.id = null;
                        item.questionId = questionId;
                        item.workorderId = workorder.id;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', tokenCreator, item, function (responseContent) {
                            translationId = responseContent.questionTranslation.id;
                        }, next);

                    },
                    
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCommitForReview', tokenCreator, {id: questionId}, function (responseContent) {
                            assert.strictEqual(QuestionWorkflow.hasInstance(questionId), true);
                            assert.strictEqual(QuestionTranslationWorkflow.hasInstance(translationId), false);
                        }, next);
                    },
                    
                    // approve
                    function (next) {
                        var content = {
                            id: questionId,
                            rating: 2,
                            isAccepted: 1,
                            difficulty: 3
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/questionReview', tokenReviewer, content, null, next);
                    },
                    // reviewer record added
                    function (next) {
                        var options = {
                            where: {
                                questionId: questionId
                            }
                        };
                        QuestionOrTranslationReview.findAll(options).then(function(dbItems) {
                            try {
                                assert.strictEqual(dbItems.length, 1);
                                assert.strictEqual(dbItems[0].get({plain: true}).resourceId, userId2);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });                       
                    },
                    // simulate approve, no payment action is added
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(questionId);
                        var managePaymentSpy = global.wsHelper.sinonSandbox.spy(inst, "_managePayment");
                        inst._onStateChange({
                            triggeredAction: 'SetStatus(Approved)'
                        });
                        
                        setTimeout(function () {
                            PaymentAction.count().then(function(c) {
                                try {
                                    assert.strictEqual(managePaymentSpy.callCount, 1);
                                    assert.strictEqual(c, 0);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });                       
                        }, 500);
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'create payment action', function (done) {
                //return done();
                process.env.WORKFLOW_ENABLED='true';
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setContent({
                        availableActionTypes: ['dsdad', 'ExternalReview'],
                        processFinished: false
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                var userRoleListByTenantIdStub = global.wsHelper.sinonSandbox.stub(ProfileManagerFakeService.prototype.messageHandlers, 'userRoleListByTenantId', function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError(null);
                    response.setContent({
                        roles: [ProfileHasRole.constants().ROLE_COMMUNITY]
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                
                
                var userId1 = DataIds.WF_9_USER_ID;
                var userId2 = DataIds.WF_10_USER_ID;
                /*
                var tokenCreator = ProfileData._encodeToken({userId:userId1, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                var tokenReviewer = ProfileData._encodeToken({userId:userId2, roles:['TENANT_' + DataIds.TENANT_1_ID + '_INTERNAL']});
                */
                
                var tokenCreator = ProfileData.CLIENT_INFO_1; tokenCreator.profile.userId = userId1;
                var tokenReviewer = ProfileData.CLIENT_INFO_3; tokenReviewer.profile.userId = userId2;
                
                
                
                var questionId = null;
                var translationId = null;
                var paymentStructure = null;
                var workorder = null;
                async.series([
                    // create payment structure
                    function (next) {
                        var item = _.clone(DataBilling.PAYMENT_STRUCTURE_TEST);
                        item.id = null;
                        item.type = PaymentStructure.constants().TYPE_INSTANT;
                        PaymentStructure.create(item).then(function (dbItem) {
                            paymentStructure = dbItem.get({plain: true});
                            return next();
                        }).catch(next);
                    },
                    // create workorder
                    function (next) {
                        var item = _.clone(DataWorkorder.WORKORDER_TEST);
                        item.id = null;
                        item.questionCreatePaymentStructureId = paymentStructure.id;
                        item.questionReviewPaymentStructureId = paymentStructure.id;
                        Workorder.create(item).then(function (dbItem) {
                            workorder = dbItem.get({plain: true});
                            return next();
                        }).catch(next);
                    },
                    // create question
                    function (next) {
                        var item = _.clone(Data.QUESTION_TEST);
                        item.id = null;
                        item.workorderId = workorder.id;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', tokenCreator, item, function (responseContent) {
                            questionId = responseContent.question.id;
                        }, next);
                    },
                    // create first translation
                    function (next) {
                        var item = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        item.id = null;
                        item.questionId = questionId;
                        item.workorderId = workorder.id;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', tokenCreator, item, function (responseContent) {
                            translationId = responseContent.questionTranslation.id;
                        }, next);

                    },
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCommitForReview', tokenCreator, {id: questionId}, function (responseContent) {
                            assert.strictEqual(QuestionWorkflow.hasInstance(questionId), true);
                            assert.strictEqual(QuestionTranslationWorkflow.hasInstance(translationId), false);
                        }, next);
                    },
                    
                    // approve
                    function (next) {
                        var content = {
                            id: questionId,
                            rating:5,
                            difficulty:3,
                            isAccepted:1
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/questionReview', tokenReviewer, content, null, next);
                    },
                    // reviewer record added
                    function (next) {
                        var options = {
                            where: {
                                questionId: questionId
                            }
                        };
                        QuestionOrTranslationReview.findAll(options).then(function(dbItems) {
                            try {
                                assert.strictEqual(dbItems.length, 1);
                                assert.strictEqual(dbItems[0].get({plain: true}).resourceId, userId2);
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        });                       
                    },
                    // simulate approve, two payments should be added
                    function (next) {
                        var inst = QuestionWorkflow.getInstance(questionId);
                        var managePaymentSpy = global.wsHelper.sinonSandbox.spy(inst, "_managePayment");
                        inst._onStateChange({
                            triggeredAction: 'SetStatus(Approved)'
                        });
                        
                        setTimeout(function () {
                            PaymentAction.findAll().then(function(dbItems) {
                                try {
                                    assert.strictEqual(managePaymentSpy.callCount, 1);
                                    assert.strictEqual(dbItems.length, 2);
                                    
                                    var paymentAction1 = dbItems[0].get({plain: true});
                                    var paymentAction2 = dbItems[1].get({plain: true});
                                    
                                    // first record shoudl be review type
                                    assert.deepEqual([
                                        paymentAction1.workorderId,
                                        paymentAction1.questionId,
                                        paymentAction1.questionTranslationId,
                                        paymentAction1.resourceId,
                                        paymentAction1.paymentStructureId,
                                        paymentAction1.type,
                                        paymentAction1.isBillable
                                    ], [
                                        workorder.id,
                                        questionId,
                                        null,
                                        userId2,
                                        paymentStructure.id,
                                        PaymentAction.constants().TYPE_REVIEW,
                                        1
                                    ]);
                                    // first record shoudl be creation type
                                    assert.deepEqual([
                                        paymentAction2.workorderId,
                                        paymentAction2.questionId,
                                        paymentAction2.questionTranslationId,
                                        paymentAction2.resourceId,
                                        paymentAction2.paymentStructureId,
                                        paymentAction2.type,
                                        paymentAction1.isBillable
                                    ], [
                                        workorder.id,
                                        questionId,
                                        null,
                                        userId1,
                                        paymentStructure.id,
                                        PaymentAction.constants().TYPE_CREATION,
                                        1
                                    ]);
                                    
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            });                       
                        }, 500);
                    }
                ], done);
            });
            
        });
        
        
        
    });
});


function emptyReviewAndEventLog(cb) {
    async.series([
        function (next) {
            QuestionOrTranslationEventLog.destroy({where: {id: {$gt: 0}}}).then(function () {
                QuestionOrTranslationEventLog.count().then(function(c) {
                    try {
                        assert.strictEqual(c, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });                       
            }).catch(function (err) {
                return next(err);
            });
        },
        
        function (next) {
            QuestionOrTranslationReview.destroy({where: {id: {$gt: 0}}}).then(function () {
                QuestionOrTranslationReview.count().then(function(c) {
                    try {
                        assert.strictEqual(c, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });                       
            }).catch(function (err) {
                //console.log('>>>>>>>>>here\n\n\n\n\n\n:', err);
                return next(err);
            });
        }
        
        
    ], cb);
}
