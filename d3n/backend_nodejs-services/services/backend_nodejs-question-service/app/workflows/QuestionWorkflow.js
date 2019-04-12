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


var TASK_ID_PREFIX = 'Question_';

var TASK_TYPE = 'ContentGenerationWorkflow';

var TENANT_ROLES = ['COMMUNITY', 'INTERNAL', 'EXTERNAL'];

var REVIEW_TYPE_EXTERNAL = 'EXTERNAL';
var REVIEW_TYPE_INTERNAL = 'INTERNAL';
var REVIEW_TYPE_COMMUNITY = 'COMMUNITY';

// timeout to prevent queue block
var QUEUE_TASK_TIMEOUT = 30*1000;

function QuestionWorkflow (itemId) {
    this._itemId = itemId;
    
    this._service = null;
    
    this._reviewType = null;
    
    this._queue = async.queue(this._queueEventHandler.bind(this), 1);
    
    this._queue.error = function (err, task) {
        logger.error('QuestionWorkflow error on queue:', err, task);
    };
};

var o = QuestionWorkflow.prototype;

o._managePayment = function (approved, cb) {
    try {
        var self = this;
        
        var workorderId = null;
        var workorder = null;
        var question = null;
        var tenantId = null;
        async.series([
            // get workorderId
            function (next) {
                Question.findOne({where: {id: self._itemId}}).then(function (dbItem) {
                    if (!dbItem) {
                        return next(Errors.DatabaseApi.NoRecordFound);
                    }
                    question = dbItem.get({plain: true});
                    workorderId = question.workorderId;
                    if (!workorderId) { // no workorder no payment
                        return next(new Error('skip'));
                    }
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
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
            
            // get workorder
            function (next) {
                Workorder.findOne({where: {id: workorderId}}).then(function (dbItem) {
                    if (!dbItem) {
                        return next(Errors.DatabaseApi.NoRecordFound);
                    }
                    workorder = dbItem.get({plain: true});
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            function (next) {
                _paymentForReviewers(question, workorder, next);
            },
            
            
            function (next) {
                if (!approved) {
                    return setImmediate(next);
                }
                _paymentForCreator(question, workorder, next);
            }
        ], function (err) {
            //console.log('\n\n\n\n>>>>>manage payment err:', err);
            if (err && err.message !== 'skip') {
                return cb(err);
            }
            return cb();
        });
        
        
        function _addPaymentAction(paymentAction, cbPaymentAction) {
            try {
                profileManagerClient.userRoleListByTenantId(paymentAction.resourceId, tenantId, function (err, response) {
                    try {
                        if (err) {
                            return cbPaymentAction(err);
                        }
                        
                        paymentAction.isBillable = 1;
                        var roles = response.getContent().roles;
                        if (roles.indexOf(ProfileHasRole.constants().ROLE_ADMIN) >= 0 || roles.indexOf(ProfileHasRole.constants().ROLE_INTERNAL) >= 0) {
                            paymentAction.isBillable = 0;
                        }
                        
                        PaymentAction.create(paymentAction).then(function () {
                            return cbPaymentAction();
                        }).catch(function (err) {
                            return cbPaymentAction(err);
                        });
                        
                    } catch (e) {
                        return cbPaymentAction(e);
                    }
                });
            } catch (e) {
                return setImmediate(cbPaymentAction, e);
            }
        };
        
        function _paymentForReviewers(question, workorder, cbPayment) {
            try {
                //console.log('>>>>workorder:', workorder);
                var paymentStructureId = workorder.questionReviewPaymentStructureId;
                if (!paymentStructureId) {
                    return setImmediate(cbPayment);
                }
                
                var paymentStructure = null;
                async.series([
                    function (next) {
                        PaymentStructure.findOne({where: {id: paymentStructureId}}).then(function (dbItem) {
                            if (!dbItem) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }

                            paymentStructure = dbItem.get({plain: true});
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    function (next) {
                        try {
                            
                            // add payment only if payment structure type = instant
                            if (paymentStructure.type !== PaymentStructure.constants().TYPE_INSTANT) {
                                return setImmediate(next);
                            }
                            var where = {
                                questionId: question.id,
                                reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_QUESTION,
                                deleted: 0
                            };
                            QuestionOrTranslationReview.findAll({where: where}).then(function (dbItems) {
                                try {
                                    if (!dbItems) {
                                        return next(Errors.DatabaseApi.NoRecordFound);
                                    }

                                    // collect reviewers
                                    var items = [];
                                    for(var i=0; i<dbItems.length; i++) {
                                        items.push(dbItems[i].get({plain: true}).resourceId);
                                    }

                                    // add payment action for each reviewer
                                    async.mapSeries(items, function (resourceId, cbItem) {
                                        var paymentAction = {
                                            createDate: DateUtils.isoNow(),
                                            resourceId: resourceId,
                                            workorderId: workorder.id,
                                            questionId: question.id,
                                            questionTranslationId: null,
                                            paymentStructureId: paymentStructureId,
                                            type: PaymentAction.constants().TYPE_REVIEW
                                        };

                                        _addPaymentAction(paymentAction, cbItem);
                                    }, next);

                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                return next(err);
                            });
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], function (err) {
                    if (err) {
                        return cbPayment(err);
                    }
                    return cbPayment();
                });
            } catch (e) {
                return setImmediate(cbPayment, e);
            }
        };
        
        function _paymentForCreator(question, workorder, cbPayment) {
            try {
                //console.log('>>>>workorder:', workorder);
                var paymentStructureId = workorder.questionCreatePaymentStructureId;
                if (!paymentStructureId) {
                    return setImmediate(cbPayment);
                }
                
                var paymentStructure = null;
                async.series([
                    function (next) {
                        PaymentStructure.findOne({where: {id: paymentStructureId}}).then(function (dbItem) {
                            
                            if (!dbItem) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }

                            paymentStructure = dbItem.get({plain: true});
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    },
                    
                    function (next) {
                        try {
                            
                            // add payment only if payment structure type = instant
                            if (paymentStructure.type !== PaymentStructure.constants().TYPE_INSTANT) {
                                return setImmediate(next);
                            }

                            var paymentAction = {
                                createDate: DateUtils.isoNow(),
                                resourceId: question.creatorResourceId,
                                workorderId: workorder.id,
                                questionId: question.id,
                                questionTranslationId: null,
                                paymentStructureId: paymentStructureId,
                                type: PaymentAction.constants().TYPE_CREATION
                            };

                            _addPaymentAction(paymentAction, next);
                            
                        } catch (e) {
                            return setImmediate(next, e);
                        }
                    }
                ], function (err) {
                    if (err) {
                        return cbPayment(err);
                    }
                    return cbPayment();
                });
            } catch (e) {
                return setImmediate(cbPayment, e);
            }
        };
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

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
            'Republished': Question.constants().STATUS_ACTIVE,
            'Review': Question.constants().STATUS_REVIEW
        };
        
        var dbStatus = eventStatusToDbStatus[eventStatus];
        if (_.isUndefined(dbStatus)) {
            logger.error('QuestionWorkflow._eventHandlerSetStatus() db status not found:', task);
            return setImmediate(cb, new Error('DB_STATUS_NOT_FOUND'));
        }
        
        var firstTranslationId = null;
        var questionStatus;
        var questionTranslationStatus;
        async.series([
            // get first translation
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
            // update question status
            function (next) {
                var updQuestion = { status: dbStatus };
                Question.update(updQuestion, { where: { id: self._itemId }, individualHooks: true }).then(function (count) {
                    return next();
                }).catch(function (err) {
                    return next(err);
                });
            },
            // update first translatin status
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
            },
            // if approved make payments
            function (next) {
                if (eventStatus === 'Approved' || eventStatus === 'Review') {
                    return self._managePayment(eventStatus === 'Approved', next);
                }
                return setImmediate(next);
            },
            // if approved close workorder
            function (next) {
                if (eventStatus === 'Approved') {
                    return WorkflowCommon.closeWorkorder(self._itemId, next);
                }
                return setImmediate(next);
            }
        ], cb);
    } catch (e) {
        logger.error('QuestionWorkflow._eventHandlerSetStatus() trycatch error:', e, task);
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
            logger.error('QuestionWorkflow._eventHandlerSetPriority() db priority not found:', task);
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
        logger.error('QuestionWorkflow._eventHandlerSetPriority() trycatch error:', e, task);
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
                        logger.error('QuestionWorkflow._eventHandlerSendEmail() no admins!', self._itemId, tenantId);
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
                                logger.error('QuestionWorkflow._eventHandlerSendEmail() eror sending email to admin:', item, err);
                            }
                            return cbItem();
                        });
                    }, next);
                }
            ], cb);
            return;
        }
        
        
    } catch (e) {
        logger.error('QuestionWorkflow._eventHandlerSendEmail() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};


o._eventHandlerPublish = function (task, cb) {
    try {
        logger.debug('QuestionWorkflow._eventHandlerPublish() called');
        // publish first translation only
        var self = this;
        var params = {};
        
        var tenantId;
        
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
                self._getFirstTranslationId(function (err, firstTranslationId) {
                    if (err) {
                        return next(err);
                    }

                    params.id = firstTranslationId;
                    QuestionTranslationApiFactory.questionTranslationPublish(params, {tenantId: tenantId}, null, function (err) {
                        logger.debug('QuestionWorkflow._eventHandlerPublish() publish err:', err);
                        var actionType = 'Publish';
                        var parameters = {
                            result: err ? 'Failure' : 'Success'
                        };

                        self.perform(null, null, actionType, parameters, next);
                    });
                });
            }
        ], cb);
        
    } catch (e) {
        logger.error('QuestionWorkflow._eventHandlerPublish() trycatch error:', e, task);
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
        logger.error('QuestionWorkflow._eventHandlerArchive() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};


/*
o._eventHandlerUnpublish = function (task, cb) {
    try {
        var self = this;
        var params = {};
        params.id = self._itemId;
        params.deploymentStatus = 'unpublished';
        params.deploymentDate = null;
        QuestionApiFactory.questionSetDeploymentStatus(params, function (err) {
            var actionType = 'Unpublish';
            var parameters = {
                result: err ? 'Failure' : 'Success'
            };

            self.perform(null, null, actionType, parameters, cb);
        });
    } catch (e) {
        logger.error('QuestionWorkflow._eventHandlerPublish() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};
*/


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
            
            logger.error('QuestionWorkflow._queueEventHandler() calback already called:', task, cb);
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
        
        if (actionType === 'Publish') {
            return async.timeout(self._eventHandlerPublish.bind(self), QUEUE_TASK_TIMEOUT)(task, callCb);
        }
        /*
        if (actionType === 'Unpublish') {
            return async.timeout(self._eventHandlerUnpublish.bind(self), QUEUE_TASK_TIMEOUT)(task, callCb);
        }
        */
        
        if (actionType === 'Archive') {
            return async.timeout(self._eventHandlerArchive.bind(self), QUEUE_TASK_TIMEOUT)(task, callCb);
        }
        
        return setImmediate(cb);
        
    } catch (e) {
        logger.error('QuestionWorkflow._queueEventHandler() error:', e, task);
        return setImmediate(cb, e);
    }
};

o._onStateChange = function (eventData) {
    try {
        var self = this;
        logger.debug('QuestionWorkflow._onStateChange() eventData:', eventData);
        this._queue.push({
            eventData: eventData
        });
    } catch (e) {
        logger.error('QuestionWorkflow._onStateChange() handler error:', e, JSON.stringify(eventData));
    }
};

o._generateTaskId = function () {
    return QuestionWorkflow.generateTaskId(this._itemId);
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
                        logger.error('QuestionWorkflow._getTenantId no question found:', questionId);
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


o._decideReviewType = function (cb) {
    try {
        var self = this;
        Question.findOne({where: {id: self._itemId}}).then(function (dbItem) {
            var workorderId = dbItem.get({plain: true}).workorderId;
            if (workorderId === null) {
                return cb(false, REVIEW_TYPE_EXTERNAL);
            }
            
            Workorder.findOne({where: {id: workorderId}}).then(function (dbItem) {
                dbItem = dbItem.get({plain: true});
                var reviewType;
                if (dbItem.questionReviewIsCommunity) {
                    reviewType = REVIEW_TYPE_COMMUNITY;
                } else {
                    reviewType = REVIEW_TYPE_EXTERNAL;
                }
                return cb(false, reviewType);
            }).catch(function (err) {
                return cb(err);
            });
            
        }).catch(function (err) {
            return cb(err);
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.start = function (message, clientSession, cb) {
    try {
        
        var self = this;
        var tenantId = null;
        var updaterResourceId;
        self._service = clientSession.getConnectionService();
                    //console.log('\n\n\n\n\n\n>>>>>>>>userId:', userId);
        
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
                self._decideReviewType(function (err, reviewType) {
                    if (err) {
                        return next(err);
                    }
                    
                    self._reviewType = reviewType;
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
                    var reviewType = self._reviewType;

                    workflowClient.start(taskId, TASK_TYPE, userId, '' + tenantId, null, {
                        reviewType: reviewType
                    }, function (err, response) {
                        try {
                            logger.debug('QuestionWorkflow.start() response:', JSON.stringify(response), clientSession.getClientInfo());
                            if (err) {
                                logger.error('QuestionWorkflow.start() error starting workflow:', err);
                                return next(err);
                            }

                            if (response.getContent().processFinished === true) {
                                QuestionWorkflow.removeInstance(self._itemId);
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
                logger.error('QuestionWorkflow.start() error starting the workflow:', err);
                return cb(err);
            }
            return cb();
        });
    } catch (e) {
        logger.error('QuestionWorkflow.start() error:', e);
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
                logger.error('QuestionWorkflow.perform() error on getUserAndTenant:', err);
                return cb(err);
            }
            
            workflowClient.perform(self._generateTaskId(), actionType, userId, tenantId, parameters, function (err, response) {
                try {
                    logger.debug('>>>>>QuestionWorkflow.perform() response:', JSON.stringify(response), actionType, parameters);
                    if (err) {
                        logger.error('QuestionWorkflow.perform() error perform workflow action:', err, actionType, parameters);
                        return cb(err);
                    }

                    if (response.getContent().processFinished === true) {
                        QuestionWorkflow.removeInstance(self._itemId);
                    }

                    return cb();
                } catch (e) {
                    logger.error('QuestionWorkflow.perform() handling workflow action response:', err, JSON.stringify(response), actionType, parameters);
                    return cb(e);
                }
            });
        });
    } catch (e) {
        logger.error('QuestionWorkflow.perform() error:', e, actionType, parameters);
        return setImmediate(cb, e);
    }
};

o.state = function (cb) {
    workflowClient.state(this._generateTaskId(), cb);
};

o.abort = function (cb) {
    workflowClient.abort(this._generateTaskId(), cb);
};

QuestionWorkflow.instances = {};

QuestionWorkflow.workflowClient = workflowClient;

QuestionWorkflow.getInstance = function (itemId) {
    itemId = '' + itemId;
    if (!_.has(QuestionWorkflow.instances, itemId)) {
        QuestionWorkflow.instances[itemId] =  new QuestionWorkflow(itemId);
    }
    
    return QuestionWorkflow.instances[itemId];
};

QuestionWorkflow.removeInstance = function (itemId) {
    try {
        itemId = '' + itemId;
        var taskId = QuestionWorkflow.instances[itemId]._generateTaskId();
        delete QuestionWorkflow.instances[itemId];
        
        workflowClient._unsubscribeIfNeeded(taskId, {processFinished: true});
    } catch (e) {
        return e;
    }
};

QuestionWorkflow.removeAllInstances = function () {
    var keys = _.keys(QuestionWorkflow.instances);
    for(var i=0; i<keys.length; i++) {
        QuestionWorkflow.removeInstance(keys[i]);
    }
};

QuestionWorkflow.hasInstance = function (itemId) {
    itemId = '' + itemId;
    return _.has(QuestionWorkflow.instances, itemId);
};


QuestionWorkflow.hasWorkflow = function (itemId, cb) {
    itemId = '' + itemId;
    workflowClient.state(QuestionWorkflow.generateTaskId(itemId), function (err, response) {
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

QuestionWorkflow.generateTaskId = function (itemId) {
    return TASK_ID_PREFIX + itemId;
};


o._decideReviewActionType = function (cb) {
    try {
        var self = this;
        
        self.state(function (err, response) {
            try {
                if (err) {
                    return cb(err);
                }
                
                var availableActionTypes = response.getContent().availableActionTypes;
                
                for(var i=0; i<availableActionTypes.length; i++) {
                    var actionType = availableActionTypes[i];
                    if (/.*Review$/.test(actionType)) {
                        return cb(false, actionType);
                    }
                }
                
                return cb(false, null);
            } catch (e) {
                return cb(e);
            }
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._getUserId = function (clientSession) {
    return clientSession.getUserId();
};


// USER EVENTS
o.questionCommitForReview = function (message, clientSession, cb) {
    try {
        var self = this;
        
        async.series([
            // check if can be added in workflow
            function (next) {
                try {
                    Question.findOne({where: {id: self._itemId}}).then(function (dbItem) {
                        try {
                            if (!dbItem) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            var question = dbItem.get({plain: true});
                            
                            if (question.status !== Question.constants().STATUS_DRAFT) {
                                logger.error('QuestionWorkflow.questionCommitForReview() question not in draft status');
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
            },
            
            // "delete" existing reviews
            function (next) {
                WorkflowCommon.deleteQuestionReviews(self._itemId, next);
            }
        ], cb);
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.questionReview = function (message, clientSession, review, cb) {
    try {
        var self = this;

        
        var questionVersion = 0;
        var questionTranslationVersion = 0;
        var firstTranslationId;
        var performActionType;
        async.series([

            function (next) {
                self._decideReviewActionType(function (err, actionType) {
                    try {
                        if (err) {
                            return next(err);
                        }

                        if (actionType === null) {
                            return next(new Error('ERR_ACTION_TYPE_NOT_FOUND'));
                        }

                        performActionType = actionType;

                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });
            },

            function (next) {
                var parameters = {
                    reviewOutcome: (review.isAccepted === 1 ? 'Approved' : 'Rejected')
                };

                self.perform(message, clientSession, performActionType, parameters, next);
            },

            function (next) {
                try {
                    return WorkflowCommon.getQuestionVersion(self._itemId, function (err, version) {
                        if (err) {
                            return next(err);
                        }
                        questionVersion = version;
                        return next();
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            // get first translation
            function (next) {
                self._getFirstTranslationId(function (err, id) {
                    firstTranslationId = id;
                    return next(err);
                });
            },
            // get version of first translation
            function (next) {
                try {
                    return WorkflowCommon.getQuestionTranslationVersion(firstTranslationId, function (err, version) {
                        if (err) {
                            return next(err);
                        }
                        questionTranslationVersion = version;
                        return next();
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            // add review and log for question
            function (next) {
                try {
                    return WorkflowCommon.addReviewAndEventLog({
                        reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_QUESTION,
                        questionId: self._itemId,
                        questionTranslationId: null,
                        version: questionVersion,
                        resourceId: self._getUserId(clientSession),
                        difficulty: review.difficulty,
                        rating: review.rating,
                        isAccepted: review.isAccepted,
                        errorType: review.errorType || null,
                        errorText: review.errorText || null
                    }, {
                        questionId: self._itemId,
                        questionTranslationId: null,
                        resourceId: self._getUserId(clientSession),
                        triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER
                    }, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // add review and log for first translation
            function (next) {
                try {
                    return WorkflowCommon.addReviewAndEventLog({
                        reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_TRANSLATION,
                        questionId: null,
                        questionTranslationId: firstTranslationId,
                        version: questionTranslationVersion,
                        resourceId: self._getUserId(clientSession),
                        difficulty: review.difficulty,
                        rating: review.rating,
                        isAccepted: review.isAccepted,
                        errorType: review.errorType || null,
                        errorText: review.errorText || null
                    }, {
                        questionId: null,
                        questionTranslationId: firstTranslationId,
                        resourceId: self._getUserId(clientSession),
                        triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER
                    }, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },

            // add analytics events
            function (next) {
                try {

                    var eventsData = [];

                    // reviewed
                    eventsData.push({
                        questionsReviewed: 1
                    });

                    if (review.isAccepted === 1) {
                        eventsData.push({
                            questionsApprovedReviewed: 1
                        });
                    }

                    if (_.has(review, 'rating') && review.rating !== null) {
                        // rated
                        eventsData.push({
                            questionsRated: 1
                        });
                    }
                    
                    async.mapSeries(eventsData, function (eventData, cbItem) {
                        WorkflowCommon.addQuestionAnalyticEvent(clientSession, eventData, cbItem);
                    }, next);

                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], function (err) {
            if (err) {
                logger.error('QuestionWorkflow.questionReview() error:', err);
                return cb(err);
            }
            return cb();
        });
        
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o.questionUnpublish = function (message, clientSession, err, cb) {
    try {
        var self = this;
        var actionType = 'Unpublish';
        var parameters = {
            result: err ? 'Failure' : 'Success'
        };

        self.perform(message, clientSession, actionType, parameters, cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};



module.exports = QuestionWorkflow;
