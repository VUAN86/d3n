var _ = require('lodash');
var async = require('async');
var Config = require('../config/config.js');
var Errors = require('../config/errors.js');
var QuestionApiFactory = require('./../factories/questionApiFactory.js');
var WorkorderApiFactory = require('./../factories/workorderApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var Question = Database.RdbmsService.Models.Question.Question;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var WorkorderHasResource = Database.RdbmsService.Models.Workorder.WorkorderHasResource;
var ServiceClient = require('nodejs-default-client');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

module.exports = {

    /**
     * Get workorder associated resources, find a message service instance, send email to resources.
     * @param {ClientSession} clientSession
     * @param {Integer} workorderId
     * @returns {}
     */
    workorderActivate: function (clientSession, workorderId) {
        try {
            var self = this;
            var resources = [];
            async.series([
                // Get workorder resources, workorder title
                function (next) {
                    try {
                        return Workorder.findOne({
                            where: { id: workorderId },
                            include: { model: WorkorderHasResource, required: true }
                        }).then(function (workorder) {
                            if (!workorder) {
                                return next(Errors.WorkorderApi.WorkorderHasNoResource);
                            }
                            var workorderItem = workorder.get({ plain: true });
                            _.forEach(workorder.workorderHasResources, function (workorderHasResource) {
                                var workorderResourceItem = workorderHasResource.get({ plain: true });
                                resources.push({
                                    action: workorderResourceItem.action,
                                    resourceId: workorderResourceItem.resourceId,
                                    workorderTitle: workorderItem.title
                                });
                            });
                            if (_.isEmpty(resources)) {
                                return next(Errors.WorkorderApi.WorkorderHasNoResource);
                            }
                            return next();
                        }).catch(function (err) {
                            return next(err);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Send email to each resource
                function (next) {
                    try {
                        return async.mapSeries(resources, function (resource, resourceSerie) {
                            try {
                                var sendEmailParams = {
                                    userId: resource.resourceId,
                                    address: null,
                                    subject: Config.userMessage.workorderActivateEmail.subject,
                                    message: Config.userMessage.workorderActivateEmail.message,
                                    subject_parameters: [resource.workorderTitle],
                                    message_parameters: [resource.workorderTitle],
                                    waitResponse: Config.umWaitForResponse,
                                    clientInfo: clientSession.getClientInfo()
                                };
                                return clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, resourceSerie);
                            } catch (ex) {
                                return resourceSerie(ex);
                            }
                        }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err && err.message !== 'NO_RESOURCE') {
                    logger.error('workorderActivate send email error:', err);
                }
            });
        } catch (ex) {
            logger.error('workorderActivate error:', ex);
        }
    },

    /**
     * Check workorder associated questions if all are approved, is so than close workorder
     * @param {ClientSession} clientSession
     * @param {Integer} workorderId
     * @returns {}
     */
    workorderClose: function (message, clientSession, questionId) {
        try {
            var self = this;
            var workorderId = null;
            var workorderClose = false;
            async.series([
                // get question entry, save workorderId
                function (next) {
                    try {
                        return QuestionApiFactory.questionGet({
                            id: questionId
                        }, message, clientSession, function (err, result) {
                            if (err) {
                                return next(err);
                            }
                            workorderId = result.question.workorderId;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // list questions of workorder that are not approved (at least one is enough)
                function (next) {
                    try {
                        return QuestionApiFactory.questionList({
                            limit: 1,
                            offset: 0,
                            searchBy: {
                                workorderId: workorderId,
                                status: { '$ne': Question.constants().STATUS_APPROVED },
                            },
                            orderBy: [],
                        }, message, clientSession, function (err, result) {
                            if (err) {
                                return next(err);
                            }
                            workorderClose = result.items.length === 0;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // if all question are approved, close workorder
                function (next) {
                    if (!workorderClose) {
                        return next();
                    }
                    try {
                        return WorkorderApiFactory.workorderClose({
                            id: workorderId
                        }, clientSession, function (err, result) {
                            return next(err);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    logger.error('workorderClose error:', err);
                }
            });
        } catch (ex) {
            logger.error('workorderClose error:', ex);
        }
    },
};
