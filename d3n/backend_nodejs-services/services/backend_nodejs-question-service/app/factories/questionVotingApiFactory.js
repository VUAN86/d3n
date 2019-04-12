var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var QuestionVoting = Database.RdbmsService.Models.QuestionVoting.QuestionVoting;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'questionVotingModel': [
        {
            destination: '$root',
            model: QuestionVoting
        }
    ],
    'questionVotingUpdateModel': [
        {
            destination: '$root',
            model: QuestionVoting
        },
    ]
};

module.exports = {

    questionVotingUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var entityId = undefined;
            var entityData = AutoMapper.mapDefinedBySchema('questionVotingUpdateModel', INSTANCE_MAPPINGS, params, true);
            var questionItemAssociates = undefined;
            if (_.has(params, 'id') && params.id) {
                create = false;
                entityId = params.id;
            }
            async.series([
                function (next) {
                    if (create) {
                        if (!_.has(entityData, 'resourceId')) {
                            entityData.resourceId = clientSession.getUserId();
                        }
                        if (!_.has(entityData, 'createDate')) {
                            entityData.createDate = _.now();
                        }
                        return QuestionVoting.create(entityData).then(function (entity) {
                            entityId = entity.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return QuestionVoting.update(entityData, { where: { id: entityData.id } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params = {
                    id: entityId
                };
                return self.questionVotingGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionVotingGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var questionVotingItem;
        try {
            var include = CrudHelper.include(QuestionVoting, [
                Question
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get question voting record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Question, QuestionTemplate, { tenantId: sessionTenantId }, true);
                    return QuestionVoting.findOne({
                        where: { id: params.id },
                        include: include,
                        subQuery: false
                    }).then(function (questionVoting) {
                        if (!questionVoting) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        questionVotingItem = AutoMapper.mapDefinedBySchema('questionVotingModel', INSTANCE_MAPPINGS, questionVoting);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ questionVoting: questionVotingItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionVotingList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var questionVotingItems = [];
        var questionVotingTotal = 0;
        try {
            var orderBy = CrudHelper.orderBy(params, QuestionVoting);
            var searchBy = CrudHelper.searchBy(params, QuestionVoting);
            var include = CrudHelper.include(QuestionVoting, [
                Question
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get quetion voting record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Question, QuestionTemplate, { tenantId: sessionTenantId }, true);
                    return QuestionVoting.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (questionVotings) {
                        questionVotingTotal = questionVotings.count;
                        questionVotingItems = AutoMapper.mapListDefinedBySchema('questionVotingModel', INSTANCE_MAPPINGS, questionVotings.rows);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({
                    items: questionVotingItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: questionVotingTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    questionVotingDelete: function (params, callback) {
        try {
            return QuestionVoting.destroy({ where: { id: params.id } }).then(function (count) {
                if (count === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    }
    
};