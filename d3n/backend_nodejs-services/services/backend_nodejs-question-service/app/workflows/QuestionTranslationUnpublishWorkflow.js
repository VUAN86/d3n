var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var WorkflowClient = require('nodejs-workflow-client');
var ProfileManagerClient = require('nodejs-profile-manager-client');
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var QuestionTranslationApiFactory = require('../factories/questionTranslationApiFactory.js');
var QuestionWorkflow = require('./QuestionWorkflow.js');
var WorkflowCommon = require('./WorkflowCommon.js');
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;


var workflowClient = new WorkflowClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var profileManagerClient = new ProfileManagerClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var tenantService = require('../services/tenantService.js');


var TASK_ID_PREFIX = 'QuestionTranslationUnpublish_';

var TASK_TYPE = 'UnpublishingWorkflow';

var TENANT_ROLES = ['COMMUNITY', 'INTERNAL', 'EXTERNAL'];

var REVIEW_TYPE_EXTERNAL = 'EXTERNAL';
var REVIEW_TYPE_INTERNAL = 'INTERNAL';
var REVIEW_TYPE_COMMUNITY = 'COMMUNITY';

// timeout to prevent queue block
var QUEUE_TASK_TIMEOUT = 30*1000;

function QuestionTranslationUnpublishWorkflow (itemId) {
    this._itemId = itemId;
    
    this._service = null;
    
    this._reviewType = null;
    
    this._queue = async.queue(this._queueEventHandler.bind(this), 1);
    
    this._queue.error = function (err, task) {
        logger.error('QuestionTranslationUnpublishWorkflow error on queue:', err, task);
    };
};

var o = QuestionTranslationUnpublishWorkflow.prototype;

o._eventHandlerSetStatus =  function (task, cb) {
    try {
        var self = this;
        
        var matches = task.eventData.triggeredAction.match(/SetStatus\((.*)\)/);
        var eventStatus = matches[1];
        var eventStatusToDbStatus = {
            'Approved': QuestionTranslation.constants().STATUS_APPROVED,
            'Rejected': QuestionTranslation.constants().STATUS_DECLINED,
            'Published': QuestionTranslation.constants().STATUS_ACTIVE,
            'Unpublished': QuestionTranslation.constants().STATUS_INACTIVE,
            'Archived': QuestionTranslation.constants().STATUS_ARCHIVED,
            'Republished': QuestionTranslation.constants().STATUS_ACTIVE
        };
        
        var dbStatus = eventStatusToDbStatus[eventStatus];
        if (_.isUndefined(dbStatus)) {
            logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerSetStatus() db status not found:', task);
            return setImmediate(cb, new Error('DB_STATUS_NOT_FOUND'));
        }
        
        
        
        var questionTranslationStatus;
        async.series([
            // get translation status
            function (next) {
                WorkflowCommon.getQuestionTranslationStatus(self._itemId, function (err, status) {
                    if (err) {
                        return next(err);
                    }
                    questionTranslationStatus = status;
                    return next();
                });
            },
            // update status
            function (next) {
                var updQuestion = { status: dbStatus };
                QuestionTranslation.update(updQuestion, { where: { id: self._itemId }, individualHooks: true }).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            // add event log
            function (next) {
                WorkflowCommon.addEventLog({
                    questionTranslationId: self._itemId,
                    triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_WORKFLOW,
                    oldStatus: questionTranslationStatus,
                    newStatus: dbStatus
                }, next);
            }
        ], cb);
        
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerSetStatus() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};

o._eventHandlerSetPriority =  function (task, cb) {
    try {
        var self = this;
        
        var matches = task.eventData.triggeredAction.match(/SetPriority\((.*)\)/);
        var eventPriority = matches[1];
        var eventPriorityToDbPriority = {
            'High': QuestionTranslation.constants().PRIORITY_HIGH
        };
        
        var dbPriority = eventPriorityToDbPriority[eventPriority];
        if (_.isUndefined(dbPriority)) {
            logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerSetPriority() db priority not found:', task);
            return setImmediate(cb, new Error('DB_PRIORITY_NOT_FOUND'));
        }
        
        var updQuestion = { priority: dbPriority };
        QuestionTranslation.update(updQuestion, { where: { id: self._itemId }, individualHooks: true }).then(function (count) {
            return cb();
        }).catch(function (err) {
            return cb(err);
        });
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerSetPriority() trycatch error:', e, task);
        return setImmediate(cb, e);
    }    
};

o._eventHandlerSendEmail = function (task, cb) {
    try {
        var self = this;
        
        // SendEmail(Author) handler
        if (task.eventData.triggeredAction === 'SendEmail(Author)') {
            var userMessageClient = self._service.getServiceClient('userMessage');
            
            var questionTranslationItem;
            async.series([
                // get user id, question name, question translation name
                function (next) {
                    return QuestionTranslation.findOne({
                        where: { id: self._itemId },
                        include: {
                            model: Question, required: true,
                            include: {
                                model: QuestionTemplate, required: true
                            }
                        }
                    }).then(function (questionTranslation) {
                        questionTranslationItem = questionTranslation.get({ plain: true });
                        return next();
                    }).catch(function (err) {
                        return next(err);
                    });
                },
                
                // send email
                function (next) {
                    var sendEmailParams = {
                        userId: questionTranslationItem.creatorResourceId,
                        address: null,
                        subject: Config.userMessage.workflowQuestionTranslationDeactivateEmailAuthor.subject,
                        message: Config.userMessage.workflowQuestionTranslationDeactivateEmailAuthor.message,
                        subject_parameters: null,
                        message_parameters: [questionTranslationItem.question.questionTemplate.name, questionTranslationItem.name],
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
                        logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerSendEmail() no admins!', self._itemId, tenantId);
                        return setImmediate(next);
                    }
                    
                    async.mapSeries(admins, function (item, cbItem) {
                        var sendEmailParams = {
                            userId: item,
                            address: null,
                            subject: Config.userMessage.workflowQuestionTranslationDeactivateEmailAdmin.subject,
                            message: Config.userMessage.workflowQuestionTranslationDeactivateEmailAdmin.message,
                            subject_parameters: null,
                            message_parameters: ['unknown', 'unknown'],
                            waitResponse: Config.umWaitForResponse
                        };
                        return userMessageClient.sendEmail(sendEmailParams, function (err) {
                            if (err) {
                                logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerSendEmail() eror sending email to admin:', item, err);
                            }
                            return cbItem();
                        });
                    }, next);
                }
            ], cb);
            return;
        }
        
        
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerSendEmail() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};

o._eventHandlerArchive = function (task, cb) {
    try {
        var self = this;
        
        var actionType = 'Archive';
        var parameters = {
            result: 'Success'
        };

        self.perform(null, null, actionType, parameters, cb);
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow._eventHandlerArchive() trycatch error:', e, task);
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
            
            logger.error('QuestionTranslationUnpublishWorkflow._queueEventHandler() calback already called:', task, cb);
        }
        
        if (_.startsWith(actionType, 'SetStatus(')) {
            return async.timeout(self._eventHandlerSetStatus.bind(self), QUEUE_TASK_TIMEOUT)(task, callCb);
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
        
        return setImmediate(cb);
        
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow._queueEventHandler() error:', e, task);
        return setImmediate(cb, e);
    }
};

o._onStateChange = function (eventData) {
    try {
        var self = this;
        logger.debug('QuestionTranslationUnpublishWorkflow._onStateChange() eventData:', eventData);
        self._queue.push({
            eventData: eventData
        });
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow._onStateChange() handler error:', e, JSON.stringify(eventData));
    }
};

o._generateTaskId = function () {
    return QuestionTranslationUnpublishWorkflow.generateTaskId(this._itemId);
};

o._getTenantRole = function (tenantId, tokenRoles) {
    try {
        for(var i=0; i<TENANT_ROLES.length; i++) {
            if (tokenRoles.indexOf('TENANT_' + tenantId + '_' + TENANT_ROLES[i]) >=0 ) {
                return TENANT_ROLES[i];
            }
        }
        return null;
    } catch (e) {
        return e;
    }
};


o.start = function (parameters, message, clientSession, cb) {
    try {
        var self = this;
        var tenantId = null;
        
        function _start() {
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

                function (next) {
                    try {
                        var userId = '' + clientSession.getUserId();
                        var taskId = self._generateTaskId();
                        workflowClient.start(taskId, TASK_TYPE, userId, '' + tenantId, null, parameters, function (err, response) {
                            try {
                                logger.debug('QuestionTranslationUnpublishWorkflow.start() response:', JSON.stringify(response), clientSession.getClientInfo());
                                if (err) {
                                    logger.error('QuestionTranslationUnpublishWorkflow.start() error starting workflow:', err);
                                    return next(err);
                                }

                                if (response.getContent().processFinished === true) {
                                    QuestionTranslationUnpublishWorkflow.removeInstance(self._itemId);
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
                    logger.error('QuestionTranslationUnpublishWorkflow.start() error starting the workflow:', err);
                    return cb(err);
                }
                return cb();
            });
        };
        
        return _start();
        
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow.start() error:', e);
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
                logger.error('QuestionTranslationUnpublishWorkflow.start() error on getUserAndTenant:', err);
                return cb(err);
            }
            
            workflowClient.perform(self._generateTaskId(), actionType, userId, tenantId, parameters, function (err, response) {
                try {
                    logger.debug('>>>>>QuestionTranslationUnpublishWorkflow.perform() response:', JSON.stringify(response), actionType, parameters);
                    if (err) {
                        logger.error('QuestionTranslationUnpublishWorkflow.perform() error perform workflow action:', err, actionType, parameters);
                        return cb(err);
                    }

                    if (response.getContent().processFinished === true) {
                        QuestionTranslationUnpublishWorkflow.removeInstance(self._itemId);
                    }

                    return cb();
                } catch (e) {
                    logger.error('QuestionTranslationUnpublishWorkflow.perform() handling workflow action response:', err, JSON.stringify(response), actionType, parameters);
                    return cb(e);
                }
            });
        });
    } catch (e) {
        logger.error('QuestionTranslationUnpublishWorkflow.perform() error:', e, actionType, parameters);
        return setImmediate(cb, e);
    }
};

o.state = function (cb) {
    workflowClient.state(this._generateTaskId(), cb);
};

o.abort = function (cb) {
    workflowClient.abort(this._generateTaskId(), cb);
};

QuestionTranslationUnpublishWorkflow.instances = {};

QuestionTranslationUnpublishWorkflow.workflowClient = workflowClient;

QuestionTranslationUnpublishWorkflow.getInstance = function (itemId) {
    itemId = '' + itemId;
    if (!_.has(QuestionTranslationUnpublishWorkflow.instances, itemId)) {
        QuestionTranslationUnpublishWorkflow.instances[itemId] =  new QuestionTranslationUnpublishWorkflow(itemId);
    }
    
    return QuestionTranslationUnpublishWorkflow.instances[itemId];
};

QuestionTranslationUnpublishWorkflow.removeInstance = function (itemId) {
    try {
        itemId = '' + itemId;
        var taskId = QuestionTranslationUnpublishWorkflow.instances[itemId]._generateTaskId();
        delete QuestionTranslationUnpublishWorkflow.instances[itemId];
        
        workflowClient._unsubscribeIfNeeded(taskId, {processFinished: true});
    } catch (e) {
        return e;
    }
};

QuestionTranslationUnpublishWorkflow.generateTaskId = function (itemId) {
    return TASK_ID_PREFIX + itemId;
};


QuestionTranslationUnpublishWorkflow.hasInstance = function (itemId) {
    itemId = '' + itemId;
    return _.has(QuestionTranslationUnpublishWorkflow.instances, itemId);
};

QuestionTranslationUnpublishWorkflow.removeAllInstances = function () {
    var keys = _.keys(QuestionTranslationUnpublishWorkflow.instances);
    for(var i=0; i<keys.length; i++) {
        QuestionTranslationUnpublishWorkflow.removeInstance(keys[i]);
    }
};


QuestionTranslationUnpublishWorkflow.hasWorkflow = function (itemId, cb) {
    itemId = '' + itemId;
    workflowClient.state(QuestionTranslationUnpublishWorkflow.generateTaskId(itemId), function (err, response) {
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

o._getTenantId = function (translationId, cb) {
    try {
        
        QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
            if (!dbItem) {
                logger.error('QuestionTranslationUnpublishWorkflow._getTenantId no translation found:', translationId);
                return cb(Errors.DatabaseApi.NoRecordFound);
            }
            return _tenantIdByQuestion(dbItem.get({plain: true}).questionId, cb);
        }).catch(function (err) {
            return cb(err);
        });
        
        
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
                        logger.error('QuestionTranslationUnpublishWorkflow._getTenantId no question found:', questionId);
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


// USER EVENTS
o.questionTranslationUnpublish = function (archive, message, clientSession, err, cb) {
    try {
        var self = this;
        var actionType = 'Unpublish';
        var parameters = {
            result: err ? 'Failure' : 'Success'
        };
        
        var itemStatus = null;
        async.series([
            function (next) {
                WorkflowCommon.isFirstTranslation(self._itemId, function (err, isFirst) {
                    if (err) {
                        return next(err);
                    }
                    
                    if (isFirst === true) {
                        logger.error('QuestionTranslationUnpublishWorkflow.questionTranslationUnpublish() is first translation');
                        return next(Errors.QuestionApi.ValidationFailed);
                    }
                    
                    return next();
                });
            },
            
            function (next) {
                QuestionTranslation.findOne({where: {id: self._itemId}}).then(function (dbItem) {
                    itemStatus = (dbItem.get({plain: true}).status === QuestionTranslation.constants().STATUS_ACTIVE ? 'Published' : 'NotPublished');
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            function (next) {
                self.start({archive: (archive === true), itemStatus: itemStatus}, message, clientSession, next);
            },
            function (next) {
                self.perform(message, clientSession, actionType, parameters, next);
            }
        ], cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};



module.exports = QuestionTranslationUnpublishWorkflow;