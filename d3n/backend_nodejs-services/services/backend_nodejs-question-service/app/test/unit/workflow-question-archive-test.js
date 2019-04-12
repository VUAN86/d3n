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
var QuestionArchiveWorkflow = require('../../workflows/QuestionArchiveWorkflow.js');
var QuestionTranslationUnpublishWorkflow = require('../../workflows/QuestionTranslationUnpublishWorkflow.js');
var QuestionTranslationWorkflow = require('../../workflows/QuestionTranslationWorkflow.js');
var QuestionTranslationApiFactory = require('./../../factories/questionTranslationApiFactory.js');
var QuestionApiFactory = require('./../../factories/questionApiFactory.js');
var tenantService = require('../../services/tenantService.js');
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;



describe('WORKFLOW QUESTION ARCHIVE', function () {
    this.timeout(20000);
    global.wsHelper.series().forEach(function (serie) {
        before(function (done) {
            
            QuestionWorkflow.removeAllInstances();
            QuestionTranslationWorkflow.removeAllInstances();
            QuestionUnpublishWorkflow.removeAllInstances();
            QuestionTranslationUnpublishWorkflow.removeAllInstances();
            QuestionArchiveWorkflow.removeAllInstances();

            return setImmediate(done);
        });
        after(function (done) {
            return setImmediate(done);
        });
        
        
        describe('[' + serie + '] ' + 'question archive', function () {
            
            it('[' + serie + '] ' + 'questionArchive() success', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var wsStartSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "start");
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError({
                        message: 'NO_SUCH_TASK',
                        type: 'client'
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                async.series([
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    // set status=inactive
                    function (next) {
                        var updQuestion = { status:  Question.constants().STATUS_INACTIVE};
                        Question.update(updQuestion, { where: { id: item.id }, individualHooks: false }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // call questionArchive
                    function (next) {
                        var content = {id: item.id};
                        
                        global.wsHelper.apiSecureSucc(serie, 'question/questionArchive', ProfileData.CLIENT_INFO_3, content, function (responseContent) {
                        }, next);
                    },
                    function (next) {
                        try {
                            assert.strictEqual(wsStartSpy.callCount, 1);
                            assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().taskId, 'QuestionArchive_' + item.id);
                            assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().userId, '' + ProfileData.CLIENT_INFO_1.profile.userId);
                            assert.isString(wsStartSpy.lastCall.args[0].getContent().tenantId);
                            assert.strictEqual(wsStartSpy.lastCall.args[0].getContent().taskType, 'ArchivingWorkflow');
                            
                            return setImmediate(next);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + 'questionArchive() error, status!=inactive', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var wsStartSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "start");
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError({
                        message: 'NO_SUCH_TASK',
                        type: 'client'
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                async.series([
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    // call questionArchive
                    function (next) {
                        var content = {id: item.id};
                        global.wsHelper.apiSecureFail(serie, 'question/questionArchive', ProfileData.CLIENT_INFO_3, content, Errors.QuestionApi.ValidationFailed, next);
                    }
                ], done);
            });
            it('[' + serie + '] ' + 'questionArchive() error, already in workflow', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var wsStartSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "start");
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError(null);
                    response.setContent({
                        availableActionTypes: ['dsdad', 'ExternalReview'],
                        processFinished: false
                    });
                    clientSession.sendMessage(response);
                });
                
                
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                async.series([
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    // set status=inactive
                    function (next) {
                        var updQuestion = { status:  Question.constants().STATUS_INACTIVE};
                        Question.update(updQuestion, { where: { id: item.id }, individualHooks: false }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    // call questionArchive
                    function (next) {
                        var content = {id: item.id};
                        global.wsHelper.apiSecureFail(serie, 'question/questionArchive', ProfileData.CLIENT_INFO_3, content, Errors.QuestionApi.ValidationFailed, next);
                    }
                ], done);
            });
            
            
            it('[' + serie + '] ' + '_eventHandlerArchive() success', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var wsStartSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "start");
                var wsPerformSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._workflowFake.messageHandlers, "perform");
                global.wsHelper.sinonSandbox.stub(global.wsHelper._workflowFake.messageHandlers, "state", function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError({
                        message: 'NO_SUCH_TASK',
                        type: 'client'
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                async.series([
                    function (next) {
                        emptyReviewAndEventLog(next);
                    },
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    // set status=inactive
                    function (next) {
                        var updQuestion = { status:  Question.constants().STATUS_INACTIVE};
                        Question.update(updQuestion, { where: { id: item.id }, individualHooks: false }).then(function (count) {
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    // call questionArchive
                    function (next) {
                        var content = {id: item.id};
                        
                        global.wsHelper.apiSecureSucc(serie, 'question/questionArchive', ProfileData.CLIENT_INFO_3, content, function (responseContent) {
                        }, next);
                    },
                    function (next) {
                        try {
                            var inst = QuestionArchiveWorkflow.getInstance(item.id);                            
                            inst._onStateChange({
                                triggeredAction: 'Archive'
                            });
                            
                            setTimeout(function () {
                                try {
                                    assert.strictEqual(wsPerformSpy.callCount, 1);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().taskId, 'QuestionArchive_' + item.id);
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().actionType, 'Archive');
                                    assert.strictEqual(wsPerformSpy.lastCall.args[0].getContent().parameters.result, 'Success');
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                                
                            }, 500);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    },
                    // event log added
                    function (next) {
                        QuestionOrTranslationEventLog.findOne({where: {questionId: item.id}}).then(function (dbItem) {
                            try {
                                dbItem = dbItem.get({plain: true});
                                assert.strictEqual(dbItem.newStatus, Question.constants().STATUS_ARCHIVED);
                                assert.strictEqual(dbItem.oldStatus, Question.constants().STATUS_INACTIVE);
                                next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(function (err) {
                            next(err);
                        });
                    }
                ], done);
            });
            
            
            it('[' + serie + '] ' + '_eventHandlerSetPriority() success', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                async.series([
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    function (next) {
                        try {
                            var inst = QuestionArchiveWorkflow.getInstance(item.id);                            
                            inst._onStateChange({
                                triggeredAction: 'SetPriority(High)'
                            });
                            
                            setTimeout(function () {
                                try {
                                    Question.findOne({where: {id: item.id}}).then(function (dbItem) {
                                        try {
                                            dbItem = dbItem.get({plain: true});
                                            assert.strictEqual(dbItem.priority, Question.constants().PRIORITY_HIGH);
                                            next();
                                        } catch (e) {
                                            return next(e);
                                        }
                                    }).catch(function (err) {
                                        next(err);
                                    });
                                } catch (e) {
                                    return next(e);
                                }
                                
                            }, 100);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + '_eventHandlerSendEmail(Author) success', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var umSendEmailSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._umFake.messageHandlers, "sendEmail");
                
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                async.series([
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    function (next) {
                        try {
                            var inst = QuestionArchiveWorkflow.getInstance(item.id);                            
                            inst._onStateChange({
                                triggeredAction: 'SendEmail(Author)'
                            });
                            
                            setTimeout(function () {
                                try {
                                    assert.strictEqual(umSendEmailSpy.callCount, 1);
                                    assert.strictEqual(umSendEmailSpy.lastCall.args[0].getContent().userId, '' + ProfileData.CLIENT_INFO_1.profile.userId);
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                                
                            }, 100);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], done);
            });
            
            it('[' + serie + '] ' + '_eventHandlerSendEmail(Admin) success', function (done) {
                process.env.WORKFLOW_ENABLED='true';
                var admins = ['11111111', '2222222'];
                var umSendEmailSpy = global.wsHelper.sinonSandbox.spy(global.wsHelper._umFake.messageHandlers, "sendEmail");
                var profileManagerAdminListByTenantIdStub = global.wsHelper.sinonSandbox.stub(ProfileManagerFakeService.prototype.messageHandlers, 'adminListByTenantId', function (message, clientSession) {
                    var response = new ProtocolMessage(message);
                    response.setError(null);
                    response.setContent({
                        admins: admins
                    });
                    
                    clientSession.sendMessage(response);
                });
                
                var item = _.clone(Data.QUESTION_TEST);
                item.id = null;
                async.series([
                    // create question
                    function (next) {
                        global.wsHelper.apiSecureSucc(serie, 'question/questionCreate', ProfileData.CLIENT_INFO_1, item, function (responseContent) {
                            item.id = responseContent.question.id;
                        }, next);
                    },
                    function (next) {
                        try {
                            var inst = QuestionArchiveWorkflow.getInstance(item.id);                            
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
                                
                            }, 100);
                        } catch (e) {
                            return setImmediate(next, e);
                        }
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
                return next(err);
            });
        }
        
        
    ], cb);
}