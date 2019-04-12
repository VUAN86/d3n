var _ = require('lodash');
var logger = require('nodejs-logger')();
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var QuestionRating = Database.RdbmsService.Models.QuestionRating.QuestionRating;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'questionRatingModel': [
        {
            destination: '$root',
            model: QuestionRating
        }
    ],
    'questionRatingUpdateModel': [
        {
            destination: '$root',
            model: QuestionRating
        },
    ]
};

module.exports = {

    questionRatingUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var entityId = undefined;
            var entityData = AutoMapper.mapDefinedBySchema('questionRatingUpdateModel', INSTANCE_MAPPINGS, params, true);
            var questionItemAssociates = undefined;
            if (_.has(params, 'id') && params.id) {
                create = false;
                entityId = params.id;
            }
            var questionId = entityData.questionId;
            async.series([
                // update/create main entity
                function (next) {
                    if (create) {
                        if (!_.has(entityData, 'resourceId')) {
                            entityData.resourceId = clientSession.getUserId();
                        }
                        return QuestionRating.create(entityData).then(function (entity) {
                            entityId = entity.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return QuestionRating.update(entityData, { where: { id: entityData.id } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // get questionId, used to update question rating
                function (next) {
                    if (!isNaN(questionId)) {
                        return setImmediate(next, false);
                    }
                    return QuestionRating.findOne({
                            where: { id: entityId }
                    }).then(function (entity) {
                        if(entity) {
                            questionId = entity.questionId;
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // update question rating
                function (next) {
                    if (isNaN(questionId)) {
                        return setImmediate(next, false);
                    }
                    return updateQuestionRating(questionId, next);
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params = {
                    id: entityId
                };
                return self.questionRatingGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionRatingGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var questionRatingItem;
        try {
            var include = CrudHelper.include(QuestionRating, [
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
                // Get quetion rating record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Question, QuestionTemplate, { tenantId: sessionTenantId }, true);
                    return QuestionRating.findOne({
                        where: { id: params.id },
                        include: include,
                        subQuery: false
                    }).then(function (questionRating) {
                        if (!questionRating) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        questionRatingItem = AutoMapper.mapDefinedBySchema('questionRatingModel', INSTANCE_MAPPINGS, questionRating);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ questionRating: questionRatingItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionRatingList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var questionRatingItems = [];
        var questionRatingTotal = 0;
        try {
            var orderBy = CrudHelper.orderBy(params, QuestionRating);
            var searchBy = CrudHelper.searchBy(params, QuestionRating);
            var include = CrudHelper.include(QuestionRating, [
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
                // Get quetion rating record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Question, QuestionTemplate, { tenantId: sessionTenantId }, true);
                    return QuestionRating.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (questionRatings) {
                        questionRatingTotal = questionRatings.count;
                        questionRatingItems = AutoMapper.mapListDefinedBySchema('questionRatingModel', INSTANCE_MAPPINGS, questionRatings.rows);
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
                    items: questionRatingItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: questionRatingTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    questionRatingDelete: function (params, callback) {
        try {
            var questionId = null;
            async.series([
                // get questionId, used to update question rating
                function (next) {
                    return QuestionRating.findOne({
                         where: { id: params.id }
                    }).then(function (entity) {
                        if(entity) {
                            questionId = entity.questionId;
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // delete entity
                function (next) {
                    return QuestionRating.destroy({ where: { id: params.id } }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // update question rating
                function (next) {
                    if (isNaN(questionId)) {
                        return setImmediate(next, false);
                    }
                    return updateQuestionRating(questionId, next);
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    }
};

/**
 * Aggregate question rating based on QuestinRating records.
 * @param {integer} questionId
 * @param {function} callback
 * @returns {unresolved}
 */
function updateQuestionRating(questionId, callback) {
    try {
        questionId = parseInt(questionId);
        // calculate total rating
        return QuestionRating.sum('rating', { where: { questionId: questionId } }).then(function(sum) {
            var totalRating = (isNaN(sum) ? 0 : sum);
            var entityData = {
                rating: totalRating
            };
            // update question rating
            return Question.update(entityData, { where: { id: questionId }, individualHooks: true }).then(function (count) {
                if (count[0] === 0) {
                    return callback(Errors.DatabaseApi.NoRecordFound);
                }
                return callback();
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        }).catch(function (err) {
            return CrudHelper.callbackError(err, callback);
        });
    } catch (ex) {
        return setImmediate(callback, ex);
    }
};