var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionWorkflow = require('../workflows/QuestionWorkflow.js');
var QuestionTranslationWorkflow = require('../workflows/QuestionTranslationWorkflow.js');
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var Profile = Database.RdbmsService.Models.ProfileManager.Profile;
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'eventModel': [
        {
            destination: '$root',
            model: QuestionOrTranslationEventLog
        },
        {
            destination: 'users',
            model: Profile,
            attributes: [
                {'firstName': Profile.tableAttributes.firstName},
                {'lastName': Profile.tableAttributes.lastName},
                {'nickname': Profile.tableAttributes.nickname}
            ]
        },
        {
            destination: 'reviews',
            model: QuestionOrTranslationReview,
            attributes: [
                {'difficulty': QuestionOrTranslationReview.tableAttributes.difficulty},
                {'rating': QuestionOrTranslationReview.tableAttributes.rating},
                {'isAccepted': QuestionOrTranslationReview.tableAttributes.isAccepted},
                {'errorType': QuestionOrTranslationReview.tableAttributes.errorType},
                {'errorText': QuestionOrTranslationReview.tableAttributes.errorText}
            ]
        }
    ]
};


module.exports = {
    questionReview: function (params, message, clientSession, callback) {
        try {
            var self = this;
            
            async.series([
                // add workflow review
                function (next) {
                    QuestionWorkflow.getInstance(params.id).questionReview(message, clientSession, params, function (err) {
                        return next(err);
                    });                    
                },
                
                // update question rating field
                function (next) {
                    updateQuestionRating(params.id, next);
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (e) {
            return CrudHelper.callbackError(e, callback);
        }
    },
    
    questionTranslationReview: function (params, message, clientSession, callback) {
        try {
            var self = this;
            
            async.series([
                // add workflow review
                function (next) {
                    QuestionTranslationWorkflow.getInstance(params.id).questionTranslationReview(message, clientSession, params, function (err) {
                        return next(err);
                    });                    
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (e) {
            return CrudHelper.callbackError(e, callback);
        }
    },
    
    eventList: function (params, message, clientSession, callback) {
        try {
            var orderBy = CrudHelper.orderBy(params, QuestionOrTranslationEventLog);
            var searchBy = CrudHelper.searchBy(params, QuestionOrTranslationEventLog);
            var eventsTotal;
            var eventsItems = [];
            var translationsIds = [];
            var total;
            var tenantId;
            var questionId = searchBy.questionId;
            delete searchBy.questionId;
            var include = CrudHelper.include(QuestionOrTranslationEventLog, [
                QuestionTranslation
            ]);
            async.series([
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tid) {
                        if (err) {
                            return next(err);
                        }
                        
                        tenantId = tid;
                        
                        
                        var questionTemplateWhere = { tenantId: tenantId };
                        include = CrudHelper.includeNestedWhere(include, QuestionTranslation, [Question, QuestionTemplate], questionTemplateWhere, true);
                        
                        return next();
                    });
                },
                
                
                // get translations IDs
                function (next) {
                    try {
                        if(questionId) {
                            //console.log('>>>>questionId:', questionId);
                            QuestionTranslation.findAll({
                                attributes: ['id'],
                                where: {
                                    questionId: questionId
                                },
                                order: 'id ASC'
                            }).then(function (dbItems) {
                                try {
                                    for(var i=0; i<dbItems.length; i++) {
                                        translationsIds.push(dbItems[i].get({plain: true}).id);
                                    }
                                    return next();
                                } catch (e) {
                                    return next(e);
                                }
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        } else {
                            return setImmediate(next);
                        }
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                function (next) {
                    try {
                        if (questionId) {
                            searchBy.questionTranslationId = {
                                '$in': (searchBy.questionTranslationId === -1 ? translationsIds : [translationsIds[0] || -1])
                            };
                        }
                        
                        
                        QuestionOrTranslationEventLog.findAll({
                            limit: params.limit === 0 ? null : params.limit,
                            offset: params.limit === 0 ? null : params.offset,
                            include: include,
                            where: searchBy,
                            order: orderBy,
                            subQuery: false
                        }).then(function (dbItems) {
                            eventsItems = dbItems;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // Populate total count
                function (next) {
                    QuestionOrTranslationEventLog.count({
                        include: include,
                        where: searchBy
                    }).then(function (count) {
                        total = count;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                
                // load reviews
                function (next) {
                    async.mapSeries(eventsItems, function (item, cbItem) {
                        if(item.reviewId === null) {
                            item.questionOrTranslationReview = [];
                            return cbItem();
                        }
                        
                        QuestionOrTranslationReview.findAndCountAll({
                            where: {id: item.reviewId},
                            limit: Config.rdbms.limit,
                            offset: 0                            
                        }).then(function (dbItems) {
                            //console.log('>>>>dbItems:', dbItems);
                            item.questionOrTranslationReview = dbItems;
                            return cbItem();
                        }).catch(cbItem);
                        
                    }, next);
                },
                
                // load users
                function (next) {
                    async.mapSeries(eventsItems, function (item, cbItem) {
                        if(item.resourceId === null) {
                            item.profile = [];
                            return cbItem();
                        }
                        
                        Profile.findAndCountAll({
                            where: {userId: item.resourceId},
                            limit: Config.rdbms.limit,
                            offset: 0                            
                        }).then(function (dbItems) {
                            item.profile = dbItems;
                            return cbItem();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, cbItem);
                        });
                        
                    }, next);
                }
                
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                //console.log('>>>>>eventsItems:', eventsItems);
                var itemsMapped = AutoMapper.mapListDefinedBySchema('eventModel', INSTANCE_MAPPINGS, eventsItems);
                return CrudHelper.callbackSuccess({
                    items: itemsMapped,
                    limit: params.limit,
                    offset: params.offset,
                    total: total
                }, callback);
            });
        } catch (e) {
            return CrudHelper.callbackError(e, callback);
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
        var where = {
            questionId: questionId,
            reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_QUESTION
        };
        return QuestionOrTranslationReview.sum('rating', { where: where }).then(function(sum) {
            var totalRating = (isNaN(sum) ? 0 : sum);
            var entityData = {
                rating: totalRating
            };
            // update question rating
            return Question.update(entityData, { where: { id: questionId }, individualHooks: false }).then(function (count) {
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