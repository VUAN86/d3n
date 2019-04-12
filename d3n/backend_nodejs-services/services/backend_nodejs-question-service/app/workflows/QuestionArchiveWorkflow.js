var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var WorkflowClient = require('nodejs-workflow-client');
var ProfileManagerClient = require('nodejs-profile-manager-client');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var PaymentStructure = Database.RdbmsService.Models.Billing.PaymentStructure;
var PaymentAction = Database.RdbmsService.Models.Billing.PaymentAction;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var QuestionApiFactory = require('../factories/questionApiFactory.js');
var QuestionTranslationApiFactory = require('../factories/questionTranslationApiFactory.js');
var WorkflowCommon = require('./WorkflowCommon.js');

var workflowClient = new WorkflowClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var profileManagerClient = new ProfileManagerClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var tenantService = require('../services/tenantService.js');


var TASK_ID_PREFIX = 'QuestionArchive_';

var TASK_TYPE = 'ArchivingWorkflow';

var TENANT_ROLES = ['COMMUNITY', 'INTERNAL', 'EXTERNAL'];


// timeout to prevent queue block
var QUEUE_TASK_TIMEOUT = 30*1000;

function QuestionArchiveWorkflow (itemId) {
    this._itemId = itemId;
    
    this._service = null;
    
    this._reviewType = null;
    
    this._queue = async.queue(this._queueEventHandler.bind(this), 1);
    
    this._queue.error = function (err, task) {
        logger.error('QuestionArchiveWorkflow error on queue:', err, task);
    };
};

var o = QuestionArchiveWorkflow.prototype;


o._eventHandlerSetPriority =  function (task, cb) {
    try {
        var self = this;
        
        var matches = task.eventData.triggeredAction.match(/SetPriority\((.*)\)/);
        var eventPriority = matches[1];
        var eventPriorityToDbPriority = {
            'High': Question.constants().PRIORITY_HIGH
        };
        
        var dbPriority = eventPriorityToDbPriority[eventPriority];
        if (_.isUndefined(dbPriority)) {
            logger.error('QuestionArchiveWorkflow._eventHandlerSetPriority() db priority not found:', task);
            return setImmediate(cb, new Error('DB_PRIORITY_NOT_FOUND'));
        }
        
        async.series([
            function (next) {
                var updQuestion = { priority: dbPriority };
                Question.update(updQuestion, { where: { id: self._itemId }, individualHooks: true }).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            }
        ], cb);
        
    } catch (e) {
        logger.error('QuestionArchiveWorkflow._eventHandlerSetPriority() trycatch error:', e, task);
        return setImmediate(cb, e);
    }    
};

o._getFirstTranslationId = function (cb) {
    try {
        var self = this;
        var options = {
            where: {
                questionId: self._itemId
            },
            order: 'id ASC',
            limit: 1
        };
        
        QuestionTranslation.findAll(options).then(function (dbItems) {
            try {
                return cb(false, dbItems[0].get({plain: true}).id);
            } catch (e) {
                return cb(e);
            }
        }).catch(function (err) {
            return cb(err);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._eventHandlerSendEmail = function (task, cb) {
    try {
        var self = this;
        
        // SendEmail(Author) handler
        if (task.eventData.triggeredAction === 'SendEmail(Author)') {
            var userMessageClient = self._service.getServiceClient('userMessage');
            
            var questionItem;
            async.series([
                // get user id, question name
                function (next) {
                    return Question.findOne({
                        where: { id: self._itemId },
                        include: { model: QuestionTemplate, required: true }
                    }).then(function (question) {
                        questionItem = question.get({ plain: true });
                        return next();
                    }).catch(function (err) {
                        return next(err);
                    });
                },
                
                // send email
                function (next) {
                    var sendEmailParams = {
                        userId: questionItem.creatorResourceId,
                        address: null,
                        subject: Config.userMessage.workflowQuestionActivateEmailAuthor.subject,
                        message: Config.userMessage.workflowQuestionActivateEmailAuthor.message,
                        subject_parameters: null,
                        message_parameters: [questionItem.questionTemplate.name],
                        waitResponse: Config.umWaitForResponse
                    };
                    return userMessageClient.sendEmail(sendEmailParams, next);
                }
            ], cb);
            return;
        }
        
        // SendEmail(Admin) handler
        if (task.eventData.triggeredAction === 'SendEmail(Admin)') {
            var userMessageClient = self._service.getServiceClient('userMessage');
            
            var admins = [];
            var tenantId = null;
            async.series([
                // get tenantId
                function (next) {
                    self._getTenantId(self._itemId, function (err, id) {
                        if (err) {
                            return next(err);
                        }
                        tenantId = id;
                        return next();
                    });
                },
                
                // get admins
                function (next) {
                    profileManagerClient.adminListByTenantId('' + tenantId, function (err, response) {
                        if (err) {
                            return next(err);
                        }
                        
                        admins = response.getContent().admins;
                        
                        return next();
                    });
                },
                
                // send emails
                function (next) {
                    if (!admins.length) {
                        logger.error('QuestionArchiveWorkflow._eventHandlerSendEmail() no admins!', self._itemId, tenantId);
                        return setImmediate(next);
                    }
                    
                    async.mapSeries(admins, function (item, cbItem) {
                        var sendEmailParams = {
                            userId: item,
                            address: null,
                            subject: Config.userMessage.workflowQuestionActivateEmailAdmin.subject,
                            message: Config.userMessage.workflowQuestionActivateEmailAdmin.message,
                            subject_parameters: null,
                            message_parameters: ['unknown'],
                            waitResponse: Config.umWaitForResponse
                        };
                        return userMessageClient.sendEmail(sendEmailParams, function (err) {
                            if (err) {
                                logger.error('QuestionArchiveWorkflow._eventHandlerSendEmail() eror sending email to admin:', item, err);
                            }
                            return cbItem();
                        });
                    }, next);
                }
            ], cb);
            return;
        }
        
        
    } catch (e) {
        logger.error('QuestionArchiveWorkflow._eventHandlerSendEmail() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};

o._eventHandlerArchive = function (task, cb) {
    try {
        var self = this;
        
        var archiveSuccess;
        var questionStatus;
        async.series([
            // get question current status
            function (next) {
                WorkflowCommon.getQuestionStatus(self._itemId, function (err, status) {
                    if (err) {
                        return next(err);
                    }
                    questionStatus = status;
                    return next();
                });
            },
            // archive
            function (next) {
                QuestionApiFactory.questionArchive({id: parseInt(self._itemId)}, null, null, function (err) {
                    if (err) {
                        logger.error('QuestionArchiveWorkflow._eventHandlerArchive(), questionArchive() error:', err);
                        return next(err);
                    }
                    
                    archiveSuccess = true;
                    return next();
                });
            },
            // add log
            function (next) {
                if (!archiveSuccess) {
                    return setImmediate(next);
                }
                
                WorkflowCommon.addEventLog({
                    questionId: self._itemId,
                    questionTranslationId: null,
                    triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_WORKFLOW,
                    oldStatus: questionStatus,
                    newStatus: Question.constants().STATUS_ARCHIVED
                }, next);
            },
            // perform archive action
            function (next) {
                var actionType = 'Archive';
                var parameters = {
                    result: archiveSuccess === true ? 'Success' : 'Failure'
                };
                self.perform(null, null, actionType, parameters, next);
            }
        ], function (err) {
            if (err) {
                logger.error('QuestionArchiveWorkflow._eventHandlerArchive() error:', err, task);
            }
            return cb(err);
        });
        
    } catch (e) {
        logger.error('QuestionArchiveWorkflow._eventHandlerArchive() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};


o._queueEventHandler = function (task, cb) {
    try {
        var self = this;
        var actionType = task.eventData.triggeredAction;
        
        var cbCalled = false;
        function callCb(err) {
            if (cbCalled === false) {
                cbCalled = true;
                return cb(err);
            }
            
            logger.error('QuestionArchiveWorkflow._queueEventHandler() calback already called:', task, cb);
        }
        
        if (_.startsWith(actionType, 'SetPriority(')) {
            return async.timeout(self._eventHandlerSetPriority.bind(self), QUEUE_TASK_TIMEOUT)(task, callCb);
        }
        
        if (_.startsWith(actionType, 'SendEmail(')) {
            return async.timeout(self._eventHandlerSendEmail.bind(self), QUEUE_TASK_TIMEOUT)(task, callCb);
        }
        
        if (actionType === 'Archive') {
            return async.timeout(self._eventHandlerArchive.bind(self), QUEUE_TASK_TIMEOUT)(task, callCb);
        }
        
        logger.error('QuestionArchiveWorkflow._queueEventHandler() no event handler', JSON.stringify(task));
        
        return setImmediate(cb);
        
    } catch (e) {
        logger.error('QuestionArchiveWorkflow._queueEventHandler() error:', e, task);
        return setImmediate(cb, e);
    }
};

o._onStateChange = function (eventData) {
    try {
        var self = this;
        logger.debug('QuestionArchiveWorkflow._onStateChange() eventData:', eventData);
        this._queue.push({
            eventData: eventData
        });
    } catch (e) {
        logger.error('QuestionArchiveWorkflow._onStateChange() handler error:', e, JSON.stringify(eventData));
    }
};

o._generateTaskId = function () {
    return QuestionArchiveWorkflow.generateTaskId(this._itemId);
};


o._getTenantId = function (questionId, cb) {
    try {
        
        return _tenantIdByQuestion(questionId, cb);
        
        function _tenantIdByQuestion(questionId, cbTenantId) {
            try {
                return Question.findOne({
                    where: { id: questionId },
                    include: [{
                        model: QuestionTemplate, required: true,
                        attributes: [QuestionTemplate.tableAttributes.tenantId.field]
                    }]
                }).then(function (question) {
                    if (!question) {
                        logger.error('QuestionArchiveWorkflow._getTenantId no question found:', questionId);
                        return cbTenantId(Errors.DatabaseApi.NoRecordFound);
                    }
                    return cbTenantId(false, question.questionTemplate.get({plain: true}).tenantId);
                }).catch(function (err) {
                    return cbTenantId(err);
                });
            } catch (e) {
                return setImmediate(cbTenantId, e);
            }
        };
    } catch (e) {
        return setImmediate(cb);
    }
};


o.start = function (message, clientSession, cb) {
    try {
        
        var self = this;
        var tenantId = null;
        var updaterResourceId;
        self._service = clientSession.getConnectionService();
        
        async.series([
            function (next) {
                self._getTenantId(self._itemId, function (err, id) {
                    if (err) {
                        return next(err);
                    }
                    tenantId = id;
                    return next();
                });
            },
            
            // get updaterResourceId
            function (next) {
                Question.findOne({where: {id: self._itemId}, attributes: ["updaterResourceId"]}).then(function (dbItem) {
                    try {
                        if (!dbItem) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        updaterResourceId = dbItem.get({plain: true}).updaterResourceId;
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
            },
            function (next) {
                try {
                    var userId = '' + updaterResourceId;
                    var taskId = self._generateTaskId();

                    workflowClient.start(taskId, TASK_TYPE, userId, '' + tenantId, null, null, function (err, response) {
                        try {
                            logger.debug('QuestionArchiveWorkflow.start() response:', JSON.stringify(response), clientSession.getClientInfo());
                            if (err) {
                                logger.error('QuestionArchiveWorkflow.start() error starting workflow:', err);
                                return next(err);
                            }

                            if (response.getContent().processFinished === true) {
                                QuestionArchiveWorkflow.removeInstance(self._itemId);
                                return next();
                            }
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], function (err) {
            if (err) {
                logger.error('QuestionArchiveWorkflow.start() error starting the workflow:', err);
                return cb(err);
            }
            return cb();
        });
    } catch (e) {
        logger.error('QuestionArchiveWorkflow.start() error:', e);
        return setImmediate(cb, e);
    }
};

o.perform = function (message, clientSession, actionType, parameters, cb) {
    try {
        var self = this;
        
        function getUserAndTenant(message, clientSession, cbGetUserAndTenant) {
            if (!message) {
                return setImmediate(cbGetUserAndTenant, false, null, null);
            }
            
            tenantService.getSessionTenantId(message, clientSession, function (err, sessionTenantId) {
                try {
                    if (err) {
                        return cbGetUserAndTenant(err);
                    }
                    return cbGetUserAndTenant(false, '' + clientSession.getUserId(), '' + sessionTenantId);
                } catch (e) {
                    return cbGetUserAndTenant(e);
                }
            });
        };
        
        
        getUserAndTenant(message, clientSession, function (err, userId, tenantId) {
            if (err) {
                logger.error('QuestionArchiveWorkflow.perform() error on getUserAndTenant:', err);
                return cb(err);
            }
            
            workflowClient.perform(self._generateTaskId(), actionType, userId, tenantId, parameters, function (err, response) {
                try {
                    logger.debug('>>>>>QuestionArchiveWorkflow.perform() response:', JSON.stringify(response), actionType, parameters);
                    if (err) {
                        logger.error('QuestionArchiveWorkflow.perform() error perform workflow action:', err, actionType, parameters);
                        return cb(err);
                    }

                    if (response.getContent().processFinished === true) {
                        QuestionArchiveWorkflow.removeInstance(self._itemId);
                    }

                    return cb();
                } catch (e) {
                    logger.error('QuestionArchiveWorkflow.perform() handling workflow action response:', err, JSON.stringify(response), actionType, parameters);
                    return cb(e);
                }
            });
        });
    } catch (e) {
        logger.error('QuestionArchiveWorkflow.perform() error:', e, actionType, parameters);
        return setImmediate(cb, e);
    }
};

o.state = function (cb) {
    workflowClient.state(this._generateTaskId(), cb);
};

o.abort = function (cb) {
    workflowClient.abort(this._generateTaskId(), cb);
};

QuestionArchiveWorkflow.instances = {};

QuestionArchiveWorkflow.workflowClient = workflowClient;

QuestionArchiveWorkflow.getInstance = function (itemId) {
    itemId = '' + itemId;
    if (!_.has(QuestionArchiveWorkflow.instances, itemId)) {
        QuestionArchiveWorkflow.instances[itemId] =  new QuestionArchiveWorkflow(itemId);
    }
    
    return QuestionArchiveWorkflow.instances[itemId];
};

QuestionArchiveWorkflow.removeInstance = function (itemId) {
    try {
        itemId = '' + itemId;
        var taskId = QuestionArchiveWorkflow.instances[itemId]._generateTaskId();
        delete QuestionArchiveWorkflow.instances[itemId];
        
        workflowClient._unsubscribeIfNeeded(taskId, {processFinished: true});
    } catch (e) {
        return e;
    }
};

QuestionArchiveWorkflow.removeAllInstances = function () {
    var keys = _.keys(QuestionArchiveWorkflow.instances);
    for(var i=0; i<keys.length; i++) {
        QuestionArchiveWorkflow.removeInstance(keys[i]);
    }
};

QuestionArchiveWorkflow.hasInstance = function (itemId) {
    itemId = '' + itemId;
    return _.has(QuestionArchiveWorkflow.instances, itemId);
};


QuestionArchiveWorkflow.hasWorkflow = function (itemId, cb) {
    itemId = '' + itemId;
    workflowClient.state(QuestionArchiveWorkflow.generateTaskId(itemId), function (err, response) {
        try {
            if (err) {
                if (response) {
                    return cb(false, false);
                } else {
                    return cb(err);
                }
            } else {
                return cb(false, response.getContent().processFinished === true ? false : true);
            }
        } catch (e) {
            return cb(e);
        }
    });
};

QuestionArchiveWorkflow.generateTaskId = function (itemId) {
    return TASK_ID_PREFIX + itemId;
};



o._getUserId = function (clientSession) {
    return clientSession.getUserId();
};


// USER EVENTS
o.questionArchive = function (message, clientSession, cb) {
    try {
        var self = this;
        
        async.series([
            // disallow if already in workflow
            function (next) {
                QuestionArchiveWorkflow.hasWorkflow(self._itemId, function (err, hasWorkflow) {
                    if (err) {
                        logger.error('QuestionArchiveWorkflow.questionArchive() error on hasWorkflow()', self._itemId, err);
                        return next(err);
                    }
                    
                    if (hasWorkflow) {
                        logger.error('QuestionArchiveWorkflow.questionArchive() already in workflow', self._itemId);
                        return next(Errors.QuestionApi.ValidationFailed);
                    }
                    
                    return next();
                });
            },
            // allow only status=inactive
            function (next) {
                try {
                    Question.findOne({where: {id: self._itemId}}).then(function (dbItem) {
                        try {
                            if (!dbItem) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            var question = dbItem.get({plain: true});
                            
                            if (question.status !== Question.constants().STATUS_INACTIVE) {
                                logger.error('QuestionWorkflow.questionCommitForReview() question not in inactive status');
                                return next(Errors.QuestionApi.ValidationFailed);
                            }
                            
                            return next();
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return next(err);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // start the workflow
            function (next) {
                self.start(message, clientSession, next);
            }
        ], cb);
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

module.exports = QuestionArchiveWorkflow;