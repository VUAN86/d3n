var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./../config/_id.data.js');
var ProfileData = require('./../config/profile.data.js');
var Data = require('./../config/question.data.js');
var WorkorderData = require('./../config/workorder.data.js');
var WorkorderData = require('./../config/workorder.data.js');
var ShouldHelper = require('nodejs-automapper').getInstance(Config).ShouldHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var Tag = Database.RdbmsService.Models.Question.Tag;
var ProtocolMessage = require('nodejs-protocol');
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var ProfileManagerFakeService = require('../services/profileManager.fakeService.js');
var QuestionWorkflow = require('../../workflows/QuestionWorkflow.js');
var QuestionUnpublishWorkflow = require('../../workflows/QuestionUnpublishWorkflow.js');
var QuestionTranslationUnpublishWorkflow = require('../../workflows/QuestionTranslationUnpublishWorkflow.js');
var QuestionTranslationWorkflow = require('../../workflows/QuestionTranslationWorkflow.js');
var QuestionTranslationApiFactory = require('./../../factories/questionTranslationApiFactory.js');
var QuestionApiFactory = require('./../../factories/questionApiFactory.js');
var tenantService = require('../../services/tenantService.js');

describe('WORKFLOW QUESTION TRANSLATION UNPUBLISH', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        before(function (done) {
            
            QuestionWorkflow.removeAllInstances();
            QuestionTranslationWorkflow.removeAllInstances();
            QuestionUnpublishWorkflow.removeAllInstances();
            QuestionTranslationUnpublishWorkflow.removeAllInstances();

            return setImmediate(done);
        });
        after(function (done) {
            return setImmediate(done);
        });
        
        
        describe('[' + serie + '] ' + 'question translation unpublish basic tests', function () {
            
            it('[' + serie + '] ' + 'question translation workflow test instance removed on perform action', function (done) {
                //return done();
                process.env.WORKFLOW_ENABLED='true';
                var stubWFPerform = global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "perform", function (message, clientSession) {
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
                /*
                var token = ProfileData._encodeToken({userId:userId, roles:['TENANT_' + DataIds.TENANT_1_ID + '_EXTERNAL']});
                var tokenReviewer = ProfileData._encodeToken({userId:DataIds.WF_2_USER_ID, roles:['TENANT_' + DataIds.TENANT_1_ID + '_EXTERNAL']});
                */
                var token = ProfileData.CLIENT_INFO_1; token.profile.userId = userId;
                var tokenReviewer = ProfileData.CLIENT_INFO_3; tokenReviewer.profile.userId = DataIds.WF_2_USER_ID;
                
                var content = _.clone(Data.QUESTION_TEST);
                content.id = null;
                
                var itemId, translationId;
                async.series([
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', token, content, function (responseContent) {
                            itemId = responseContent.question.id;
                            assert.strictEqual(QuestionWorkflow.hasInstance(itemId), false);
                        }, next);
                    },
                    
                    function (next) {
                        var contentTranslation = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        contentTranslation.id = null;
                        contentTranslation.questionId = itemId;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', token, contentTranslation, function (responseContent) {
                            translationId = responseContent.questionTranslation.id;
                        }, next);
                    },
                    
                    function (next) {
                        var contentTranslation = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        contentTranslation.id = null;
                        contentTranslation.questionId = itemId;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', token, contentTranslation, function (responseContent) {
                            translationId = responseContent.questionTranslation.id;
                        }, next);
                    },
                    function (next) {
                        var content = {id: translationId};
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationDeactivate', tokenReviewer, content, null, function () {
                            setTimeout(function () {
                                try {
                                    assert.strictEqual(stubWFPerform.lastCall.args[0].getContent().taskId, 'QuestionTranslationUnpublish_' + translationId);
                                    assert.strictEqual(QuestionTranslationUnpublishWorkflow.hasInstance(translationId), false);
                                    next();
                                } catch (e) {
                                    next(e);
                                }
                            }, 500);
                        });
                    }
                ], done);
                
            });
            
        });
        
        describe('[' + serie + '] ' + 'question unpublish workflow', function () {
            
            it('[' + serie + '] ' + 'question unpublish workflow', function (done) {
                //return done();
                process.env.WORKFLOW_ENABLED='true';
                var admins = [DataIds.WF_1_USER_ID, DataIds.WF_2_USER_ID];
                var wsStartSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "start");
                var wsPerformSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "perform");
                var umSendEmailSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._umFake.messageHandlers, "sendEmail");
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
                
                global.wsHelper.sinonSandbox.stub(QuestionTranslationApiFactory, "questionTranslationUnpublish", function (params, message, clientSession, callback) {
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
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', tokenCreator, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    
                    function (next) {
                        var contentTranslation = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        contentTranslation.id = null;
                        contentTranslation.questionId = item.id;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', tokenCreator, contentTranslation, function (responseContent) {
                            //translationId = responseContent.questionTranslation.id;
                        }, next);
                    },
                    function (next) {
                        var contentTranslation = _.clone(Data.QUESTION_TRANSLATION_TEST);
                        contentTranslation.id = null;
                        contentTranslation.questionId = item.id;
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationCreate', tokenCreator, contentTranslation, function (responseContent) {
                            translationId = responseContent.questionTranslation.id;
                        }, next);
                    },
                    
                    function (next) {
                        QuestionTranslation.update({ status: QuestionTranslation.constants().STATUS_DRAFT }, { where: {id: translationId}, individualHooks: true }).then(function (dbItem) {
                            return next();
                        }).catch(function (err) {
                            next(err);
                        });
                    },
                    
                    // unpublish question translation
                    function (next) {
                        wsPerformSpy.reset();
                        wsStartSpy.reset();
                        
                        var content = {id: translationId};
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationDeactivate', tokenReviewer, content, null, function () {
                            setTimeout(function () {
                                try {
                                    assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().parameters.archive, false);
                                    assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().parameters.itemStatus, 'NotPublished');
                                    
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'QuestionTranslationUnpublish_' + translationId);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().userId, '' + userId2);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().tenantId, '' + DataIds.TENANT_1_ID);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().actionType, 'Unpublish');
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.result, 'Success');
                                    next();
                                } catch (e) {
                                    next(e);
                                }
                            }, 500);
                        });
                    },
                    
                    function (next) {
                        QuestionTranslation.update({ status: QuestionTranslation.constants().STATUS_ACTIVE }, { where: {id: translationId}, individualHooks: true }).then(function (dbItem) {
                            return next();
                        }).catch(function (err) {
                            next(err);
                        });
                    },
                    // unpublish question translation
                    function (next) {
                        wsPerformSpy.reset();
                        wsStartSpy.reset();
                        
                        var content = {id: translationId, archive: true};
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationDeactivate', tokenReviewer, content, null, function () {
                            setTimeout(function () {
                                try {
                                    assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().parameters.archive, true);
                                    assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().parameters.itemStatus, 'Published');
                                    
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'QuestionTranslationUnpublish_' + translationId);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().userId, '' + userId2);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().tenantId, '' + DataIds.TENANT_1_ID);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().actionType, 'Unpublish');
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.result, 'Success');
                                    next();
                                } catch (e) {
                                    next(e);
                                }
                            }, 500);
                        });
                    },
                    // unpublish question translation
                    function (next) {
                        wsPerformSpy.reset();
                        wsStartSpy.reset();
                        
                        QuestionTranslationApiFactory.questionTranslationUnpublish.restore();
                        global.wsHelper.sinonSandbox.stub(QuestionTranslationApiFactory, "questionTranslationUnpublish", function (params, message, clientSession, callback) {
                            return setImmediate(callback, true, {});
                        });
                        
                        var content = {id: translationId, archive: true};
                        global.wsHelper.apiSecureSucc(serie, 'question/questionTranslationDeactivate', tokenReviewer, content, null, function () {
                            setTimeout(function () {
                                try {
                                    assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().parameters.archive, true);
                                    assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().parameters.itemStatus, 'Published');
                                    
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'QuestionTranslationUnpublish_' + translationId);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().userId, '' + userId2);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().tenantId, '' + DataIds.TENANT_1_ID);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().actionType, 'Unpublish');
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.result, 'Failure');
                                    next();
                                } catch (e) {
                                    next(e);
                                }
                            }, 500);
                        });
                    },
                    
                    // test SetStatus event
                    function (next) {
                        var inst = QuestionTranslationUnpublishWorkflow.getInstance(translationId);
                        var eventStatusToDbStatus = {
                            'Approved': Question.constants().STATUS_APPROVED,
                            'Rejected': Question.constants().STATUS_DECLINED,
                            'Published': Question.constants().STATUS_ACTIVE,
                            'Unpublished': Question.constants().STATUS_INACTIVE,
                            'Archived': Question.constants().STATUS_ARCHIVED,
                            'Republished': Question.constants().STATUS_ACTIVE
                        };
                        var items = _.keys(eventStatusToDbStatus);
                        
                        async.mapSeries(items, function (status, cbItem) {
                            inst._onStateChange({
                                triggeredAction: 'SetStatus(' + status + ')'
                            });
                            
                            setTimeout(function () {
                                QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                    try {
                                        dbItem = dbItem.get({plain: true});
                                        assert.strictEqual(dbItem.status, eventStatusToDbStatus[status]);
                                        cbItem();
                                    } catch (e) {
                                        return cbItem(e);
                                    }
                                }).catch(function (err) {
                                    cbItem(err);
                                });
                            }, 500);
                        }, next);
                    },
                    
                    // emit SetPriority(High) event
                    function (next) {
                        var inst = QuestionTranslationUnpublishWorkflow.getInstance(translationId);
                        
                        inst._onStateChange({
                            triggeredAction: 'SetPriority(High)'
                        });
                        
                        setTimeout(function () {
                            QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                try {
                                    dbItem = dbItem.get({plain: true});
                                    assert.strictEqual(dbItem.priority, QuestionTranslation.constants().PRIORITY_HIGH);
                                    next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                next(err);
                            });
                        }, 500);
                    },
                    
                    // emit SendEmail(Author) event
                    function (next) {
                        var inst = QuestionTranslationUnpublishWorkflow.getInstance(translationId);
                        
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
                        var inst = QuestionTranslationUnpublishWorkflow.getInstance(translationId);
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
                    
                    
                    // emit archive event
                    function (next) {
                        wsPerformSpy.reset();
                        wsStartSpy.reset();
                        
                        var inst = QuestionTranslationUnpublishWorkflow.getInstance(translationId);
                        
                        inst._onStateChange({
                            triggeredAction: 'Archive'
                        });
                        
                        setTimeout(function () {
                            QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                                try {
                                    dbItem = dbItem.get({plain: true});
                                    //assert.strictEqual(dbItem.status, Question.constants().STATUS_ARCHIVED);
                                    
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'QuestionTranslationUnpublish_' + translationId);
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
                    },
                    
                    function (next) {
                        QuestionWorkflow.removeAllInstances();
                        QuestionTranslationWorkflow.removeAllInstances();
                        QuestionUnpublishWorkflow.removeAllInstances();
                        QuestionTranslationUnpublishWorkflow.removeAllInstances();
                        return setImmediate(next);
                    }
                ], done);
            });
            
        });
    });
});


