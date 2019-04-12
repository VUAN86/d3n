var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var QuestionApiFactory = require('./../factories/questionApiFactory.js');
var QuestionWorkflow = require('../workflows/QuestionWorkflow.js');
var QuestionUnpublishWorkflow = require('../workflows/QuestionUnpublishWorkflow.js');
var QuestionArchiveWorkflow = require('../workflows/QuestionArchiveWorkflow.js');
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var ClientInfo = require('nodejs-utils').ClientInfo;
var logger = require('nodejs-logger')();

module.exports = {

    /**
     * WS question/questionCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionCreate: function (message, clientSession) {
        var params = {};
        var self = this;
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionUpdate: function (message, clientSession) {
        var params = {};
        var self = this;
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
                if (!err) {
                    self.emitEvent('workorderClose', message, clientSession, params.id);
                }
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionDeleteByStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionDeleteInactive
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionDeleteInactive: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = 'inactive';
            QuestionApiFactory.questionDeleteByStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    questionCommitForReview: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            
            
            QuestionWorkflow.getInstance(params.id).questionCommitForReview(message, clientSession, function (err) {
                return CrudHelper.handleProcessed(err, null, message, clientSession);                    
            });
            
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
        
    },
    /**
     * WS question/questionArchive
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionArchive: function (message, clientSession) {
        var params = {};
        var self = this;
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            
            if (process.env.WORKFLOW_ENABLED === 'true') {
                _workflowPath();
            } else {
                _defaultPath();
            }


            function _workflowPath() {
                QuestionArchiveWorkflow.getInstance(params.id).questionArchive(message, clientSession, function (errWorkflow) {
                    return CrudHelper.handleProcessed(errWorkflow, {}, message, clientSession);                    
                });
            };

            function _defaultPath() {
                QuestionApiFactory.questionArchive(params, message, clientSession, function (err, data) {
                    CrudHelper.handleProcessed(err, data, message, clientSession);
                });
            };
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS question/questionGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionAddRemoveTags
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionAddRemoveTags: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionAddRemoveTags(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionAddRemoveKeywords
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionAddRemoveKeywords: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionAddRemoveKeywords(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = Question.constants().STATUS_ACTIVE;
            QuestionApiFactory.questionSetDeploymentStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionActivateEntire
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionActivateEntire: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionActivateEntire(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            
            // check if the question was activated manually
            Question.findOne({where: {id: params.id}}).then(function(dbItem) {
                if(!dbItem) {
                    return CrudHelper.handleFailure(Errors.DatabaseApi.NoRecordFound, message, clientSession);
                }
                var isActivatedManually = dbItem.get({plain: true}).isActivatedManually;
                if(isActivatedManually === 1) {
                    QuestionApiFactory.questionDeactivateManually(params, message, clientSession, function (err, data) {
                        CrudHelper.handleProcessed(err, data, message, clientSession);
                    });
                } else {
                    params.status = Question.constants().STATUS_INACTIVE;
                    QuestionApiFactory.questionSetDeploymentStatus(params, function (err, data) {
                        if (process.env.WORKFLOW_ENABLED === 'true') {
                            _workflowPath();
                        } else {
                            _defaultPath();
                        }

                        function _workflowPath() {
                            QuestionUnpublishWorkflow.getInstance(params.id).questionUnpublish(params.archive, message, clientSession, err, function (errWorkflow) {
                                return CrudHelper.handleProcessed(errWorkflow, data, message, clientSession);                    
                            });
                        };

                        function _defaultPath() {
                            CrudHelper.handleProcessed(err, data, message, clientSession);
                        };
                    });
                }
            }).catch(function (err) {
                logger.error("questionDeactivate findOne err:",err);
                CrudHelper.handleFailure(err, message, clientSession);
            });
        } catch (ex) {
            logger.error("questionDeactivate ex:",ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/tagList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tagList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.tagList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/tagDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tagDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.tagDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    /**
     * WS question/tagReplace
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tagReplace: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.tagReplace(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    questionReportIssue: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionReportIssue(params, new ClientInfo(clientSession.getClientInfo()), function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    questionIssueResolve: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionIssueResolve(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    questionTranslationIssueResolve: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionTranslationIssueResolve(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionApprove
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionApprove: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionApprove(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionMassUpload
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionMassUpload: function (message, clientSession) {
        var params = {};
        var self = this;
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionMassUpload(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionMassUploadStatus
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionMassUploadStatus: function (message, clientSession) {
        var params = {};
        var self = this;
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionApiFactory.questionMassUploadStatus(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

}
