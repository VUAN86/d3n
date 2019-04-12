var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var WorkflowClient = require('nodejs-workflow-client');
var ProfileManagerClient = require('nodejs-profile-manager-client');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var PaymentStructure = Database.RdbmsService.Models.Billing.PaymentStructure;
var PaymentAction = Database.RdbmsService.Models.Billing.PaymentAction;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var QuestionTranslationApiFactory = require('../factories/questionTranslationApiFactory.js');
var WorkflowCommon = require('./WorkflowCommon.js');

var workflowClient = new WorkflowClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var profileManagerClient = new ProfileManagerClient({
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS
});

var tenantService = require('../services/tenantService.js');


var TASK_ID_PREFIX = 'QuestionTranslation_';

var TASK_TYPE = 'TranslationWorkflow';

var TENANT_ROLES = ['COMMUNITY', 'INTERNAL', 'EXTERNAL'];

var REVIEW_TYPE_EXTERNAL = 'EXTERNAL';
var REVIEW_TYPE_INTERNAL = 'INTERNAL';
var REVIEW_TYPE_COMMUNITY = 'COMMUNITY';

// timeout to prevent queue block
var QUEUE_TASK_TIMEOUT = 30*1000;

function QuestionTranslationWorkflow (itemId) {
    this._itemId = itemId;
    
    this._service = null;
    
    this._reviewType = null;
    
    this._queue = async.queue(this._queueEventHandler.bind(this), 1);
    
    this._queue.error = function (err, task) {
        logger.error('QuestionTranslationWorkflow error on queue:', err, task);
    };
};

var o = QuestionTranslationWorkflow.prototype;

o._managePayment = function (approved, cb) {
    try {
        var self = this;
        
        var workorderId = null;
        var workorder = null;
        var translation = null;
        var tenantId = null;
        async.series([
            // get workorderId
            function (next) {
                QuestionTranslation.findOne({where: {id: self._itemId}}).then(function (dbItem) {
                    if (!dbItem) {
                        return next(Errors.DatabaseApi.NoRecordFound);
                    }
                    translation = dbItem.get({plain: true});
                    workorderId = translation.workorderId;
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
                _paymentForReviewers(translation, workorder, next);
            },
            
            
            function (next) {
                if (!approved) {
                    return setImmediate(next);
                }
                _paymentForCreator(translation, workorder, next);
            }
        ], function (err) {
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
        
        function _paymentForReviewers(translation, workorder, cbPayment) {
            try {
                //console.log('>>>>workorder:', workorder);
                var paymentStructureId = workorder.translationReviewPaymentStructureId;
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
                                questionTranslationId: translation.id,
                                reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_TRANSLATION,
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
                                            questionId: null,
                                            questionTranslationId: translation.id,
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
        
        function _paymentForCreator(translation, workorder, cbPayment) {
            try {
                //console.log('>>>>workorder:', workorder);
                var paymentStructureId = workorder.translationCreatePaymentStructureId;
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
                                resourceId: translation.creatorResourceId,
                                workorderId: workorder.id,
                                questionId: null,
                                questionTranslationId: translation.id,
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
            'Approved': QuestionTranslation.constants().STATUS_APPROVED,
            'Rejected': QuestionTranslation.constants().STATUS_DECLINED,
            'Published': QuestionTranslation.constants().STATUS_ACTIVE,
            'Unpublished': QuestionTranslation.constants().STATUS_INACTIVE,
            'Archived': QuestionTranslation.constants().STATUS_ARCHIVED,
            'Republished': QuestionTranslation.constants().STATUS_ACTIVE,
            'Review': QuestionTranslation.constants().STATUS_REVIEW,
            'AutomaticTranslation': QuestionTranslation.constants().STATUS_AUTOMATIC_TRANSLATION
        };
        
        var dbStatus = eventStatusToDbStatus[eventStatus];
        if (_.isUndefined(dbStatus)) {
            logger.error('QuestionTranslationWorkflow._eventHandlerSetStatus() db status not found:', task);
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
            },
            // if approved make payments
            function (next) {
                if (eventStatus === 'Approved' || eventStatus === 'Review') {
                    return self._managePayment(eventStatus === 'Approved', next);
                }
                return setImmediate(next);
            }
            
        ], cb);
        
    } catch (e) {
        logger.error('QuestionTranslationWorkflow._eventHandlerSetStatus() trycatch error:', e, task);
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
            logger.error('QuestionTranslationWorkflow._eventHandlerSetPriority() db priority not found:', task);
            return setImmediate(cb, new Error('DB_PRIORITY_NOT_FOUND'));
        }
        
        var updQuestion = { priority: dbPriority };
        QuestionTranslation.update(updQuestion, { where: { id: self._itemId }, individualHooks: true }).then(function (count) {
            return cb();
        }).catch(function (err) {
            return cb(err);
        });
    } catch (e) {
        logger.error('QuestionTranslationWorkflow._eventHandlerSetPriority() trycatch error:', e, task);
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
                        subject: Config.userMessage.workflowQuestionTranslationActivateEmailAuthor.subject,
                        message: Config.userMessage.workflowQuestionTranslationActivateEmailAuthor.message,
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
                        logger.error('QuestionTranslationWorkflow._eventHandlerSendEmail() no admins!', self._itemId, tenantId);
                        return setImmediate(next);
                    }
                    
                    async.mapSeries(admins, function (item, cbItem) {
                        var sendEmailParams = {
                            userId: item,
                            address: null,
                            subject: Config.userMessage.workflowQuestionTranslationActivateEmailAdmin.subject,
                            message: Config.userMessage.workflowQuestionTranslationActivateEmailAdmin.message,
                            subject_parameters: null,
                            message_parameters: ['unknown', 'unknown'],
                            waitResponse: Config.umWaitForResponse
                        };
                        return userMessageClient.sendEmail(sendEmailParams, function (err) {
                            if (err) {
                                logger.error('QuestionTranslationWorkflow._eventHandlerSendEmail() eror sending email to admin:', item, err);
                            }
                            return cbItem();
                        });
                    }, next);
                }
            ], cb);
            return;
        }
        
        
    } catch (e) {
        logger.error('QuestionTranslationWorkflow._eventHandlerSendEmail() trycatch error:', e, task);
        return setImmediate(cb, e);
    }
};


o._eventHandlerPublish = function (task, cb) {
    try {
        logger.debug('QuestionTranslationWorkflow._eventHandlerPublish() called');
        var self = this;
        var tenantId;
        
        var params = {};
        params.id = self._itemId;
        
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
                    QuestionTranslationApiFactory.questionTranslationPublish(params, {tenantId: tenantId}, null, function (err) {
                        logger.debug('QuestionTranslationWorkflow._eventHandlerPublish() publish err:', err);
                        var actionType = 'Publish';
                        var parameters = {
                            result: err ? 'Failure' : 'Success'
                        };

                        self.perform(null, null, actionType, parameters, next);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            }
        ], cb);
        
    } catch (e) {
        logger.error('QuestionTranslationWorkflow._eventHandlerPublish() trycatch error:', e, task);
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
        logger.error('QuestionTranslationWorkflow._eventHandlerArchive() trycatch error:', e, task);
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
        QuestionTranslationApiFactory.questionSetDeploymentStatus(params, function (err) {
            var actionType = 'Unpublish';
            var parameters = {
                result: err ? 'Failure' : 'Success'
            };

            self.perform(null, null, actionType, parameters, cb);
        });
    } catch (e) {
        logger.error('QuestionTranslationWorkflow._eventHandlerPublish() trycatch error:', e, task);
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
            
            logger.error('QuestionTranslationWorkflow._queueEventHandler() calback already called:', task, cb);
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
        logger.error('QuestionTranslationWorkflow._queueEventHandler() error:', e, task);
        return setImmediate(cb, e);
    }
};

o._onStateChange = function (eventData) {
    try {
        var self = this;
        logger.debug('QuestionTranslationWorkflow._onStateChange() eventData:', eventData);
        self._queue.push({
            eventData: eventData
        });
    } catch (e) {
        logger.error('QuestionTranslationWorkflow._onStateChange() handler error:', e, JSON.stringify(eventData));
    }
};

o._generateTaskId = function () {
    return QuestionTranslationWorkflow.generateTaskId(this._itemId);
    //return TASK_ID_PREFIX + this._itemId;
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

o._decideReviewType = function (cb) {
    try {
        var self = this;
        QuestionTranslation.findOne({where: {id: self._itemId}}).then(function (dbItem) {
            var workorderId = dbItem.get({plain: true}).workorderId;
            if (workorderId === null) {
                return cb(false, REVIEW_TYPE_EXTERNAL);
            }
            
            Workorder.findOne({where: {id: workorderId}}).then(function (dbItem) {
                dbItem = dbItem.get({plain: true});
                var reviewType;
                if (dbItem.translationReviewIsCommunity) {
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
        var updaterResourceId;
        function _start() {
            self._service = clientSession.getConnectionService();
            
            var tenantId = null;
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
                    QuestionTranslation.findOne({where: {id: self._itemId}, attributes: ["updaterResourceId"]}).then(function (dbItem) {
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
                            translationType: reviewType
                        }, function (err, response) {
                            try {
                                logger.debug('QuestionTranslationWorkflow.start() response:', JSON.stringify(response), clientSession.getClientInfo());
                                if (err) {
                                    logger.error('QuestionTranslationWorkflow.start() error starting workflow:', err);
                                    return next(err);
                                }

                                if (response.getContent().processFinished === true) {
                                    QuestionTranslationWorkflow.removeInstance(self._itemId);
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
                    logger.error('QuestionTranslationWorkflow.start() error starting the workflow:', err);
                    return cb(err);
                }
                return cb();
            });
        };
        
        return _start();
    } catch (e) {
        logger.error('QuestionTranslationWorkflow.start() error:', e);
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
                logger.error('QuestionTranslationWorkflow.perform() error on getUserAndTenant:', err);
                return cb(err);
            }
            
            workflowClient.perform(self._generateTaskId(), actionType, userId, tenantId, parameters, function (err, response) {
                try {
                    logger.debug('>>>>>QuestionTranslationWorkflow.perform() response:', JSON.stringify(response), actionType, parameters);
                    if (err) {
                        logger.error('QuestionTranslationWorkflow.perform() error perform workflow action:', err, actionType, parameters);
                        return cb(err);
                    }

                    if (response.getContent().processFinished === true) {
                        QuestionTranslationWorkflow.removeInstance(self._itemId);
                    }

                    return cb();
                } catch (e) {
                    logger.error('QuestionTranslationWorkflow.perform() handling workflow action response:', err, JSON.stringify(response), actionType, parameters);
                    return cb(e);
                }
            });
        });
    } catch (e) {
        logger.error('QuestionTranslationWorkflow.perform() error:', e, actionType, parameters);
        return setImmediate(cb, e);
    }
};

o.state = function (cb) {
    workflowClient.state(this._generateTaskId(), cb);
};

o.abort = function (cb) {
    workflowClient.abort(this._generateTaskId(), cb);
};

QuestionTranslationWorkflow.instances = {};

QuestionTranslationWorkflow.workflowClient = workflowClient;

QuestionTranslationWorkflow.getInstance = function (itemId) {
    itemId = '' + itemId;
    if (!_.has(QuestionTranslationWorkflow.instances, itemId)) {
        QuestionTranslationWorkflow.instances[itemId] =  new QuestionTranslationWorkflow(itemId);
    }
    
    return QuestionTranslationWorkflow.instances[itemId];
};

QuestionTranslationWorkflow.removeInstance = function (itemId) {
    try {
        itemId = '' + itemId;
        var taskId = QuestionTranslationWorkflow.instances[itemId]._generateTaskId();
        delete QuestionTranslationWorkflow.instances[itemId];
        
        workflowClient._unsubscribeIfNeeded(taskId, {processFinished: true});
    } catch (e) {
        return e;
    }
};

QuestionTranslationWorkflow.generateTaskId = function (itemId) {
    return TASK_ID_PREFIX + itemId;
};


QuestionTranslationWorkflow.hasInstance = function (itemId) {
    itemId = '' + itemId;
    return _.has(QuestionTranslationWorkflow.instances, itemId);
};

QuestionTranslationWorkflow.removeAllInstances = function () {
    var keys = _.keys(QuestionTranslationWorkflow.instances);
    for(var i=0; i<keys.length; i++) {
        QuestionTranslationWorkflow.removeInstance(keys[i]);
    }
};


QuestionTranslationWorkflow.hasWorkflow = function (itemId, cb) {
    itemId = '' + itemId;
    workflowClient.state(QuestionTranslationWorkflow.generateTaskId(itemId), function (err, response) {
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

QuestionTranslationWorkflow.isFirstTranslation = function (translationId, cb) {
    try {
        var questionId = null;
        var firstTranslationId = null;
        async.series([
            function (next) {
                QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
                    try {
                        questionId = dbItem.get({plain: true}).questionId;
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
            },
            
            function (next) {
                var options = {
                    where: {
                        questionId: questionId
                    },
                    order: 'id ASC',
                    limit: 1
                };

                QuestionTranslation.findAll(options).then(function (dbItems) {
                    try {
                        firstTranslationId = dbItems[0].get({plain: true}).id;
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                }).catch(function (err) {
                    return next(err);
                });
                
            }
        ], function (err) {
            if (err) {
                return cb(err);
            }
            
            return cb(false, (parseInt(firstTranslationId) === parseInt(translationId)), questionId);
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

o._getTenantId = function (translationId, cb) {
    try {
        
        QuestionTranslation.findOne({where: {id: translationId}}).then(function (dbItem) {
            if (!dbItem) {
                logger.error('QuestionTranslationWorkflow._getTenantId no translation found:', translationId);
                return cb(Errors.DatabaseApi.NoRecordFound);
            }
            return _tenantIdByQuestion(dbItem.get({plain: true}).questionId, cb);
        }).catch(function (err) {
            return cb(err);
        });
        
        
        function _tenantIdByQuestion(questionId, cbTenantId) {
            try {
                Question.findOne({
                    where: { id: questionId },
                    include: [{
                        model: QuestionTemplate, required: true,
                        attributes: [QuestionTemplate.tableAttributes.tenantId.field]
                    }]
                }).then(function (question) {
                    try {
                        if (!question) {
                            logger.error('QuestionTranslationWorkflow._getTenantId no question found:', questionId);
                            return cbTenantId(Errors.DatabaseApi.NoRecordFound);
                        }
                        return cbTenantId(false, question.questionTemplate.get({plain: true}).tenantId);
                    } catch (e) {
                        return cbTenantId(e);
                    }
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
o.questionTranslationCommitForReview = function (message, clientSession, cb) {
    try {
        var self = this;
        
        async.series([
            // check if can be added in workflow
            function (next) {
                try {
                    QuestionTranslation.findOne({where: {id: self._itemId}}).then(function (dbItem) {
                        try {
                            if (!dbItem) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            var qt = dbItem.get({plain: true});
                            
                            if (qt.status !== QuestionTranslation.constants().STATUS_DRAFT) {
                                logger.error('QuestionTranslationWorkflow.questionTranslationCommitForReview() translation not in draft status');
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
            
            // if first translation throw error
            function (next) {
                QuestionTranslationWorkflow.isFirstTranslation(self._itemId, function (err, isFirst) {
                    if (err) {
                        return next(err);
                    }
                    
                    if (isFirst === true) {
                        logger.error('QuestionTranslationWorkflow.questionTranslationCommitForReview() is first translation');
                        return next(Errors.QuestionApi.ValidationFailed);
                    }
                    
                    return next(); 
                });
            },
            
            // start the workflow
            function (next) {
                self.start(message, clientSession, next);
            },
            
            // "delete" existing reviews
            function (next) {
                WorkflowCommon.deleteQuestionTranslationReviews(self._itemId, next);
            }
            
        ], cb);
        
    } catch (e) {
        return setImmediate(cb, e);
    }    
};

o.questionTranslationReview = function (message, clientSession, review, cb) {
    try {
        var self = this;

        var questionTranslationVersion = 0;

        async.series([
            // if first translation throw validation error
            function (next) {
                QuestionTranslationWorkflow.isFirstTranslation(self._itemId, function (err, isFirst) {
                    if (err) {
                        return next(err);
                    }
                    
                    if (isFirst === true) {
                        logger.error('QuestionTranslationWorkflow.questionTranslationReview() is first translation');
                        return next(Errors.QuestionApi.ValidationFailed);
                    }
                    
                    return next();
                });
            },

            function (next) {
                try {
                    return WorkflowCommon.getQuestionTranslationVersion(self._itemId, function (err, version) {
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

            function (next) {
                self._decideReviewActionType(function (err, actionType) {
                    try {
                        if (err) {
                            return next(err);
                        }

                        if (actionType === null) {
                            return next(new Error('ERR_ACTION_TYPE_NOT_FOUND'));
                        }

                        WorkflowCommon.addReviewAndEventLog({
                            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_TRANSLATION,
                            questionId: null,
                            questionTranslationId: self._itemId,
                            version: questionTranslationVersion,
                            resourceId: self._getUserId(clientSession),
                            difficulty: review.difficulty || null,
                            rating: null,
                            isAccepted: review.isAccepted,
                            errorType: review.errorType || null,
                            errorText: review.errorText || null
                        }, {
                            questionId: null,
                            questionTranslationId: self._itemId,
                            resourceId: self._getUserId(clientSession),
                            triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER
                        }, function (err) {
                            if (err) {
                                return next(err);
                            }

                            var parameters = {
                                reviewOutcome: (review.isAccepted === 1 ? 'Approved' : 'Rejected')
                            };

                            self.perform(message, clientSession, actionType, parameters, next);
                        });

                    } catch (e) {
                        return next(e);
                    }
                });
                
            }
        ], cb);
            
    } catch (e) {
        return setImmediate(cb, e);
    }
};



o.questionTranslationUnpublish = function (message, clientSession, err, cb) {
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



module.exports = QuestionTranslationWorkflow;