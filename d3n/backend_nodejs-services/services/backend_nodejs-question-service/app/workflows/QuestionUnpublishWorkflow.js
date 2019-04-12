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
var QuestionApiFactory = require('../factories/questionApiFactory.js');
var QuestionTranslationApiFactory = require('../factories/questionTranslationApiFactory.js');
var WorkflowCommon = require('./WorkflowCommon.js');
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;

var workflowClient = new WorkflowClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var profileManagerClient = new ProfileManagerClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var tenantService = require('../services/tenantService.js');


var TASK_ID_PREFIX = 'QuestionUnpublish_';

var TASK_TYPE = 'UnpublishingWorkflow';

var TENANT_ROLES = ['COMMUNITY', 'INTERNAL', 'EXTERNAL'];

var REVIEW_TYPE_EXTERNAL = 'EXTERNAL';
var REVIEW_TYPE_INTERNAL = 'INTERNAL';
var REVIEW_TYPE_COMMUNITY = 'COMMUNITY';

// timeout to prevent queue block
var QUEUE_TASK_TIMEOUT = 30*1000;

function QuestionUnpublishWorkflow (itemId) {
    this._itemId = itemId;
    
    this._service = null;
    
    this._reviewType = null;
    
    this._queue = async.queue(this._queueEventHandler.bind(this), 1);
    
    this._queue.error = function (err, task) {
        logger.error('QuestionUnpublishWorkflow error on queue:', err, task);
    };
};

var o = QuestionUnpublishWorkflow.prototype;


o._eventHandlerSetStatus =  function (task, cb) {
    try {
        var self = this;
        
        var matches = task.eventData.triggeredAction.match(/SetStatus\((.*)\)/);
        var eventStatus = matches[1];
        var eventStatusToDbStatus = {
            'Approved': Question.constants().STATUS_APPROVED,
            'Rejected': Question.constants().STATUS_DECLINED,
            'Published': Question.constants().STATUS_ACTIVE,
            'Unpublished': Question.constants().STATUS_INACTIVE,
            'Archived': Question.constants().STATUS_ARCHIVED,
            'Republished': Question.constants().STATUS_ACTIVE
        };
        
        var dbStatus = eventStatusToDbStatus[eventStatus];
        if (_.isUndefined(dbStatus)) {
            logger.error('QuestionUnpublishWorkflow._eventHandlerSetStatus() db status not found:', task);
            return setImmediate(cb, new Error('DB_STATUS_NOT_FOUND'));
        }
        
        var firstTranslationId = null;
        var questionStatus;
        var questionTranslationStatus;
        async.series([
            function (next) {
                self._getFirstTranslationId(function (err, id) {
                    firstTranslationId = id;
                    return next(err);
                });
            },
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
            // get first translation current status
            function (next) {
                WorkflowCommon.getQuestionTranslationStatus(firstTranslationId, function (err, status) {
                    if (err) {
                        return next(err);
                    }
                    questionTranslationStatus = status;
                    return next();
                });
            },
            function (next) {
                var updQuestion = { status: dbStatus };
                Question.update(updQuestion, { where: { id: self._itemId }, individualHooks: true }).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            function (next) {
                var updQuestionTranslation = { status: dbStatus };
                QuestionTranslation.update(updQuestionTranslation, { where: { id: firstTranslationId }, individualHooks: true }).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            // add event log for question
            function (next) {
                WorkflowCommon.addEventLog({
                    questionId: self._itemId,
                    questionTranslationId: null,
                    triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_WORKFLOW,
                    oldStatus: questionStatus,
                    newStatus: dbStatus
                }, next);
            },
            // add event log for first translation
            function (next) {
                WorkflowCommon.addEventLog({
                    questionId: null,
                    questionTranslationId: firstTranslationId,
                    triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_WORKFLOW,
                    oldStatus: questionTranslationStatus,
                    newStatus: dbStatus
                }, next);
            }
        ], cb);
    } catch (e) {
        logger.error('QuestionUnpublishWorkflow._eventHandlerSetStatus() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};

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
            logger.error('QuestionUnpublishWorkflow._eventHandlerSetPriority() db priority not found:', task);
            return setImmediate(cb, new Error('DB_PRIORITY_NOT_FOUND'));
        }
        
        var firstTranslationId = null;
        async.series([
            function (next) {
                self._getFirstTranslationId(function (err, id) {
                    firstTranslationId = id;
                    return next(err);
                });
            },
            function (next) {
                var updQuestion = { priority: dbPriority };
                Question.update(updQuestion, { where: { id: self._itemId }, individualHooks: true }).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            function (next) {
                var updQuestionTranslation = { priority: dbPriority };
                QuestionTranslation.update(updQuestionTranslation, { where: { id: firstTranslationId }, individualHooks: true }).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            }
        ], cb);
        
    } catch (e) {
        logger.error('QuestionUnpublishWorkflow._eventHandlerSetPriority() trycatch error:', e, task);
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
                        subject: Config.userMessage.workflowQuestionDeactivateEmailAuthor.subject,
                        message: Config.userMessage.workflowQuestionDeactivateEmailAuthor.message,
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
                        logger.error('QuestionUnpublishWorkflow._eventHandlerSendEmail() no admins!', self._itemId, tenantId);
                        return setImmediate(next);
                    }
                    
                    async.mapSeries(admins, function (item, cbItem) {
                        var sendEmailParams = {
                            userId: item,
                            address: null,
                            subject: Config.userMessage.workflowQuestionDeactivateEmailAdmin.subject,
                            message: Config.userMessage.workflowQuestionDeactivateEmailAdmin.message,
                            subject_parameters: null,
                            message_parameters: ['unknown'],
                            waitResponse: Config.umWaitForResponse
                        };
                        return userMessageClient.sendEmail(sendEmailParams, function (err) {
                            if (err) {
                                logger.error('QuestionUnpublishWorkflow._eventHandlerSendEmail() eror sending email to admin:', item, err);
                            }
                            return cbItem();
                        });
                    }, next);
                }
            ], cb);
            return;
        }
        
        
    } catch (e) {
        logger.error('QuestionUnpublishWorkflow._eventHandlerSendEmail() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};

o._eventHandlerPublish = function (task, cb) {
    try {
        // publish first translation only
        var self = this;
        var params = {};
        
        self._getFirstTranslationId(function (err, firstTranslationId) {
            if (err) {
                return cb(err);
            }
            
            params.id = firstTranslationId;
            QuestionTranslationApiFactory.questionTranslationPublish(params, null, function (err) {
                var actionType = 'Publish';
                var parameters = {
                    result: err ? 'Failure' : 'Success'
                };

                self.perform(null, null, actionType, parameters, cb);
            });
        });
    } catch (e) {
        logger.error('QuestionUnpublishWorkflow._eventHandlerPublish() trycatch error:', e, task);
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
        logger.error('QuestionUnpublishWorkflow._eventHandlerArchive() trycatch error:', e, task);
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
            
            logger.error('QuestionUnpublishWorkflow._queueEventHandler() calback already called:', task, cb);
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
        logger.error('QuestionUnpublishWorkflow._queueEventHandler() error:', e, task);
        return setImmediate(cb, e);
    }
};

o._onStateChange = function (eventData) {
    try {
        var self = this;
        logger.debug('QuestionUnpublishWorkflow._onStateChange() eventData:', eventData);
        this._queue.push({
            eventData: eventData
        });
    } catch (e) {
        logger.error('QuestionUnpublishWorkflow._onStateChange() handler error:', e, JSON.stringify(eventData));
    }
};

o._generateTaskId = function () {
    return QuestionUnpublishWorkflow.generateTaskId(this._itemId);
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
                            logger.debug('QuestionUnpublishWorkflow.start() response:', JSON.stringify(response), clientSession.getClientInfo());
                            if (err) {
                                logger.error('QuestionUnpublishWorkflow.start() error starting workflow:', err);
                                return next(err);
                            }

                            if (response.getContent().processFinished === true) {
                                QuestionUnpublishWorkflow.removeInstance(self._itemId);
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
                logger.error('QuestionUnpublishWorkflow.start() error starting the workflow:', err);
                return cb(err);
            }
            return cb();
        });
    } catch (e) {
        logger.error('QuestionUnpublishWorkflow.start() error:', e);
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
                logger.error('QuestionUnpublishWorkflow.start() error on getUserAndTenant:', err);
                return cb(err);
            }
            
            workflowClient.perform(self._generateTaskId(), actionType, userId, tenantId, parameters, function (err, response) {
                try {
                    logger.debug('>>>>>QuestionUnpublishWorkflow.perform() response:', JSON.stringify(response), actionType, parameters);
                    if (err) {
                        logger.error('QuestionUnpublishWorkflow.perform() error perform workflow action:', err, actionType, parameters);
                        return cb(err);
                    }

                    if (response.getContent().processFinished === true) {
                        QuestionUnpublishWorkflow.removeInstance(self._itemId);
                    }

                    return cb();
                } catch (e) {
                    logger.error('QuestionUnpublishWorkflow.perform() handling workflow action response:', err, JSON.stringify(response), actionType, parameters);
                    return cb(e);
                }
            });
        });
    } catch (e) {
        logger.error('QuestionUnpublishWorkflow.perform() error:', e, actionType, parameters);
        return setImmediate(cb, e);
    }
};

o.state = function (cb) {
    workflowClient.state(this._generateTaskId(), cb);
};

o.abort = function (cb) {
    workflowClient.abort(this._generateTaskId(), cb);
};

QuestionUnpublishWorkflow.instances = {};

QuestionUnpublishWorkflow.workflowClient = workflowClient;

QuestionUnpublishWorkflow.getInstance = function (itemId) {
    itemId = '' + itemId;
    if (!_.has(QuestionUnpublishWorkflow.instances, itemId)) {
        QuestionUnpublishWorkflow.instances[itemId] =  new QuestionUnpublishWorkflow(itemId);
    }
    
    return QuestionUnpublishWorkflow.instances[itemId];
};

QuestionUnpublishWorkflow.removeInstance = function (itemId) {
    try {
        itemId = '' + itemId;
        var taskId = QuestionUnpublishWorkflow.instances[itemId]._generateTaskId();
        delete QuestionUnpublishWorkflow.instances[itemId];
        
        workflowClient._unsubscribeIfNeeded(taskId, {processFinished: true});
    } catch (e) {
        return e;
    }
};

QuestionUnpublishWorkflow.removeAllInstances = function () {
    var keys = _.keys(QuestionUnpublishWorkflow.instances);
    for(var i=0; i<keys.length; i++) {
        QuestionUnpublishWorkflow.removeInstance(keys[i]);
    }
};

QuestionUnpublishWorkflow.hasInstance = function (itemId) {
    itemId = '' + itemId;
    return _.has(QuestionUnpublishWorkflow.instances, itemId);
};


QuestionUnpublishWorkflow.hasWorkflow = function (itemId, cb) {
    itemId = '' + itemId;
    workflowClient.state(QuestionUnpublishWorkflow.generateTaskId(itemId), function (err, response) {
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

QuestionUnpublishWorkflow.generateTaskId = function (itemId) {
    return TASK_ID_PREFIX + itemId;
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
                        logger.error('QuestionUnpublishWorkflow._getTenantId no question found:', questionId);
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
o.questionUnpublish = function (archive, message, clientSession, err, cb) {
    try {
        var self = this;
        var actionType = 'Unpublish';
        var parameters = {
            result: err ? 'Failure' : 'Success'
        };

        var itemStatus = null;
        async.series([
            function (next) {
                Question.findOne({where: {id: self._itemId}}).then(function (dbItem) {
                    itemStatus = (dbItem.get({plain: true}).status === Question.constants().STATUS_ACTIVE ? 'Published' : 'NotPublished');
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



module.exports = QuestionUnpublishWorkflow;