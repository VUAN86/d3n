var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var QuestionTranslationApiFactory = require('./../factories/questionTranslationApiFactory.js');
var QuestionTranslationWorkflow = require('../workflows/QuestionTranslationWorkflow.js');
var QuestionUnpublishWorkflow = require('../workflows/QuestionUnpublishWorkflow.js');
var QuestionTranslationUnpublishWorkflow = require('../workflows/QuestionTranslationUnpublishWorkflow.js');
var Database = require('nodejs-database').getInstance(Config);
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;

module.exports = {

    /**
     * WS question/questionTranslationCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionTranslationApiFactory.questionTranslationUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionTranslationUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionTranslationApiFactory.questionTranslationUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionTranslationBlock
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationBlock: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionTranslationApiFactory.questionTranslationBlock(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionTranslationCommitForReview
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationCommitForReview: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            
            QuestionTranslationWorkflow.getInstance(params.id).questionTranslationCommitForReview(message, clientSession, function (err) {
                return CrudHelper.handleProcessed(err, null, message, clientSession);                    
            });
            
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
        
    },

    /**
     * WS question/questionTranslationGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionTranslationApiFactory.questionTranslationGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionTranslationList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionTranslationApiFactory.questionTranslationList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionTranslationUniqueList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationUniqueList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params._unique = true;
            QuestionTranslationApiFactory.questionTranslationList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionTranslationActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            QuestionTranslationApiFactory.questionTranslationPublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS question/questionTranslationDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    questionTranslationDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            
            // check if the translation was activated manually
            QuestionTranslation.findOne({where: {id: params.id}}).then(function(dbItem) {
                if(!dbItem) {
                    return CrudHelper.handleFailure(Errors.DatabaseApi.NoRecordFound, message, clientSession);
                }
                var isActivatedManually = dbItem.get({plain: true}).isActivatedManually;
                if(isActivatedManually === 1) {
                    QuestionTranslationApiFactory.questionTranslationDeactivateManually(params, message, clientSession, function (err, data) {
                        CrudHelper.handleProcessed(err, data, message, clientSession);
                    });
                } else {
                    QuestionTranslationApiFactory.questionTranslationUnpublish(params, message, clientSession, function (err, data) {
                        if (process.env.WORKFLOW_ENABLED === 'true') {
                            _workflowPath();
                        } else {
                            _defaultPath();
                        }

                        function _workflowPath() {
                            QuestionTranslationUnpublishWorkflow.getInstance(params.id).questionTranslationUnpublish(params.archive, message, clientSession, err, function (errWorkflow) {
                                return CrudHelper.handleProcessed(errWorkflow, data, message, clientSession);                    
                            });
                        };

                        function _defaultPath() {
                            CrudHelper.handleProcessed(err, data, message, clientSession);
                        };
                    });
                }
            }).catch(function (err) {
                CrudHelper.handleFailure(err, message, clientSession);
            });
            
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

};
