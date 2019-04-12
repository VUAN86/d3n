var _ = require('lodash');
var async = require('async');
var uuid = require('node-uuid');
var nodepath = require('path');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var QuestionService = require('./../services/questionService.js');
var CryptoService = require('./../services/cryptoService.js');
var PublishService = require('./../services/publishService.js');
var TenantService = require('../services/tenantService.js');
var Database = require('nodejs-database').getInstance(Config);
var QuestionTranslationWorkflow = require('./../workflows/QuestionTranslationWorkflow.js');
var AnalyticEvent = require('../services/analyticEvent.js');
var WorkflowCommon = require('./../workflows/WorkflowCommon.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikePool = KeyvalueService.Models.AerospikePool;
var AerospikePoolMeta = KeyvalueService.Models.AerospikePoolMeta;
var AerospikePoolIndex = KeyvalueService.Models.AerospikePoolIndex;
var AerospikePoolIndexMeta = KeyvalueService.Models.AerospikePoolIndexMeta;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var QuestionHasRegionalSetting = Database.RdbmsService.Models.Question.QuestionHasRegionalSetting;
var QuestionHasRelatedQuestion = Database.RdbmsService.Models.Question.QuestionHasRelatedQuestion;
var RegionalSettingHasLanguage = Database.RdbmsService.Models.Question.RegionalSettingHasLanguage;
var RegionalSetting = Database.RdbmsService.Models.Question.RegionalSetting;
var QuestionHasRegionalSetting = Database.RdbmsService.Models.Question.QuestionHasRegionalSetting;
var QuestionHasTag = Database.RdbmsService.Models.Question.QuestionHasTag;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var Language = Database.RdbmsService.Models.Question.Language;
var Pool = Database.RdbmsService.Models.Question.Pool;
var PoolHasQuestion = Database.RdbmsService.Models.Question.PoolHasQuestion;
var WorkorderHasQuestionExample = Database.RdbmsService.Models.Workorder.WorkorderHasQuestionExample;
var Tag = Database.RdbmsService.Models.Question.Tag;
var Media = Database.RdbmsService.Models.Media.Media;


var INSTANCE_MAPPINGS = {
    'questionTranslationModel': [
        {
            destination: '$root',
            model: QuestionTranslation,
            custom: false,
            customQuery: '`questionTranslation`.`id` = (SELECT MIN(qtOne.id) FROM question_translation AS qtOne WHERE qtOne.questionId = `question`.`id` AND $customQueryOptional)',
            customQueryOptional: ['1 = 1'],
            customQueryPoolsIds: 'EXISTS (SELECT 1 FROM pool_has_question WHERE `question`.`id` = pool_has_question.questionId AND pool_has_question.poolId IN ($poolsIds))',
            customQueryHasReview: 'EXISTS (SELECT 1 FROM question_or_translation_review qtr WHERE `questionTranslation`.`id` = qtr.questionTranslationId AND `questionTranslation`.`version` = qtr.version AND qtr.resourceId = "$user")',
            customQueryHasNotReview: 'NOT EXISTS (SELECT 1 FROM question_or_translation_review qtr WHERE `questionTranslation`.`id` = qtr.questionTranslationId AND `questionTranslation`.`version` = qtr.version AND qtr.resourceId = "$user")',
            customQueryOptionalHasReview: 'EXISTS (SELECT 1 FROM question_or_translation_review qtr WHERE qtOne.`id` = qtr.questionTranslationId AND qtOne.`version` = qtr.version AND qtr.resourceId = "$user")',
            customQueryOptionalHasNotReview: 'NOT EXISTS (SELECT 1 FROM question_or_translation_review qtr WHERE qtOne.`id` = qtr.questionTranslationId AND qtOne.`version` = qtr.version AND qtr.resourceId = "$user")',
            customQueryIsDefault: 'EXISTS (SELECT 1 FROM question qq INNER JOIN regional_setting qrs ON qrs.id = qq.primaryRegionalSettingId WHERE `questionTranslation`.`questionId` = qq.id AND `questionTranslation`.`languageId` = qrs.defaultLanguageId)',
            customQueryIsNotDefault: 'NOT EXISTS (SELECT 1 FROM question qq INNER JOIN regional_setting qrs ON qrs.id = qq.primaryRegionalSettingId WHERE `questionTranslation`.`questionId` = qq.id AND `questionTranslation`.`languageId` = qrs.defaultLanguageId)',
            customQueryOptionalIsDefault: 'EXISTS (SELECT 1 FROM question qq INNER JOIN regional_setting qrs ON qrs.id = qq.primaryRegionalSettingId WHERE qtOne.`questionId` = qq.id AND qtOne.`languageId` = qrs.defaultLanguageId)',
            customQueryOptionalIsNotDefault: 'NOT EXISTS (SELECT 1 FROM question qq INNER JOIN regional_setting qrs ON qrs.id = qq.primaryRegionalSettingId WHERE qtOne.`questionId` = qq.id AND qtOne.`languageId` = qrs.defaultLanguageId)',
            fullTextQuery: 'LOWER(`question.questionHasTags`.`tagTag`) LIKE "%fulltext_word%" OR LOWER(`questionTranslation`.`content`) LIKE "%fulltext_word%" OR LOWER(`questionTranslation`.`explanation`) LIKE "%fulltext_word%" OR LOWER(`questionTranslation`.`hint`) LIKE "%fulltext_word%"',
            fullTextInclude: [Question, QuestionHasTag]
        },
        {
            destination: 'question',
            model: Question,
            type: AutoMapper.TYPE_OBJECT
        },
        {
            destination: 'questionsIds',
            model: Question,
            attribute: Question.tableAttributes.id,
            skip: true
        },
        {
            destination: 'language',
            model: Language,
            type: AutoMapper.TYPE_OBJECT
        },
        {
            destination: 'languagesIds',
            model: Language,
            attribute: Language.tableAttributes.id,
            skip: true
        },
        {
            destination: 'isAlreadyReviewedByUser',
            skip: true
        },
    ],
    'questionTranslationCreateModel': [
        {
            destination: '$root',
            model: QuestionTranslation
        },
    ],
    'questionTranslationPublishModel': [
        {
            destination: '$root',
            model: QuestionTranslation
        },
        {
            destination: 'pools',
            model: Pool,
            attribute: Pool.tableAttributes.id
        }
    ]
}
INSTANCE_MAPPINGS['questionTranslationUpdateModel'] = INSTANCE_MAPPINGS['questionTranslationCreateModel'];

module.exports = {

    /**
     * Create or update question translation
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {function} callback
     * @returns {*}
     */
    questionTranslationUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var questionTranslationId = undefined;
            var mapBySchema = 'questionTranslationCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                questionTranslationId = params.id;
                mapBySchema = 'questionTranslationUpdateModel';
            }
            var questionTranslationItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var questionTranslationItemAssociates = undefined;
            if (_.has(params, 'id') && params.id) {
                create = false;
                questionTranslationId = params.id;
            }
            
            var currentStatus;
            async.series([
                // prevent update if in workflow
                function (next) {
                    return setImmediate(next);
                    //if (!(process.env.WORKFLOW_ENABLED === 'true') || create) {
                    //    return setImmediate(next);
                    //}
                    //QuestionTranslationWorkflow.hasWorkflow(params.id, function (err, hasWorkflow) {
                    //    if (err) {
                    //        logger.error('questionTranslationUpdate error on hasWorkflow():', err);
                    //        return next(err);
                    //    }
                    //    if (hasWorkflow) {
                    //        logger.error('questionTranslationUpdate question is in workflow');
                    //        return next(Errors.QuestionApi.ValidationFailed);
                    //    }
                    //    return next();
                    //});
                },

                // prevent update if entry is blocked by another user and block date not expired and less than 2h
                function (next) {
                    if (create) {
                        return setImmediate(next);
                    }
                    return CrudHelper.checkModelEntryBlockedByUser(QuestionTranslation, params, clientSession, next);
                },

                // get current status
                function (next) {
                    if (create) {
                        return setImmediate(next);
                    }
                    WorkflowCommon.getQuestionTranslationStatus(questionTranslationId, function (err, status) {
                        currentStatus = status;
                        return next(err);
                    });
                },
                
                // Create/update main entity
                function (next) {
                    questionTranslationItem.updaterResourceId = clientSession.getUserId();
                    if (create) {
                        if (!_.has(questionTranslationItem, 'createDate')) {
                            questionTranslationItem.createDate = _.now();
                        }
                        // Creator not set -> set current user
                        if (!_.has(questionTranslationItem, 'creatorResourceId')) {
                            questionTranslationItem.creatorResourceId = clientSession.getUserId();
                        }
                        
                        return QuestionTranslation.create(questionTranslationItem).then(function (questionTranslation) {
                            questionTranslationId = questionTranslation.get({ plain: true }).id;
                            var translatedLanguages = [];
                            var questionLanguages = [];

                            return async.series([

                                // find all translated languages
                                function (languageNext) {
                                    return Language.findAll({
                                        include: [{
                                            model: QuestionTranslation, required: true,
                                            where: {questionId: params.questionId}}
                                        ]
                                    }).then(function (languages) {
                                        _.forEach(languages, function (language) {
                                            var languageItem = language.get({ plain: true });
                                            translatedLanguages.push(languageItem.iso);
                                        });
                                        translatedLanguages = _.uniq(translatedLanguages);
                                        return languageNext();
                                    }).catch(function (err) {
                                        return CrudHelper.callbackError(err, languageNext);
                                    });
                                },

                                // find all question languages
                                function (languageNext) {
                                    return RegionalSetting.findAll({
                                        include: [
                                            {
                                                model: QuestionHasRegionalSetting, required: true,
                                                where: { questionId: params.questionId }
                                            },
                                            {
                                                model: Language, required: true,
                                            },
                                            {
                                                model: RegionalSettingHasLanguage, required: true,
                                                include: [
                                                    {
                                                        model: Language, required: true,
                                                    }
                                                ]
                                            }                                        ]
                                    }).then(function (regionalSettings) {
                                        _.forEach(regionalSettings, function (regionalSetting) {
                                            var regionalSettingItem = regionalSetting.get({ plain: true });
                                            questionLanguages.push(regionalSettingItem.language.iso);
                                            _.forEach(regionalSettingItem.regionalSettingHasLanguages, function (regionalSettingHasLanguage) {
                                                questionLanguages.push(regionalSettingHasLanguage.language.iso);
                                            });
                                        });
                                        questionLanguages = _.uniq(questionLanguages);
                                        return languageNext();
                                    }).catch(function (err) {
                                        return CrudHelper.callbackError(err, languageNext);
                                    });
                                },

                            ], function (err) {
                                if (err) {
                                    return next(err);
                                }
                                var isTranslatedAll =
                                    questionLanguages.length === translatedLanguages.length &&
                                    translatedLanguages.length === _.union(questionLanguages, translatedLanguages).length;
                                var isTranslationNeeded = isTranslatedAll ? 0 : 1;
                                return Question.update(
                                    {
                                        isTranslationNeeded: isTranslationNeeded
                                    },
                                    {
                                        where: { id: params.questionId },
                                        individualHooks: true
                                    }).then(function (count) {
                                    return next();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, next);
                                });
                            });
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return QuestionTranslation.update(questionTranslationItem, { where: { id: questionTranslationItem.id }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                
                // add event log if needed
                function (next) {
                    try {
                        if (!create && _.has(params, 'status') && params.status !== currentStatus) {
                            WorkflowCommon.addEventLog({
                                questionId: null,
                                questionTranslationId: questionTranslationId,
                                triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                                resourceId: clientSession.getUserId(),
                                oldStatus: currentStatus,
                                newStatus: params.status
                            }, next);
                        } else {
                            return setImmediate(next);
                        }
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                
                // add analytic event
                function (next) {
                    try {
                        if (!create) {
                            return setImmediate(next);
                        }
                        
                        
                        var clientInfo = {
                            userId: clientSession.getUserId(),
                            appId: clientSession.getClientInfo().appConfig.appId,
                            sessionIp: clientSession.getClientInfo().ip,
                            sessionId: clientSession.getId()
                        };
                        
                        var eventData = {
                            questionsTranslated: 1
                        };
                        
                        AnalyticEvent.addQuestionEvent(clientInfo, eventData, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
                
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = questionTranslationId;
                return self.questionTranslationGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var questionTranslationItem;
        try {
            var include = CrudHelper.include(QuestionTranslation, [
                Question,
                Language
            ]);
            return async.series([
                // Prevent listing if entry is blocked by another user and block date not expired and less than 2h
                function (next) {
                    return CrudHelper.checkModelEntryBlockedByUser(QuestionTranslation, params, clientSession, next);
                },
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
                // Get question translation record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Question, QuestionTemplate, { tenantId: sessionTenantId }, true);
                    return QuestionTranslation.findOne({
                        where: { id: params.id },
                        include: include
                    }).then(function (questionTranslation) {
                        if (!questionTranslation) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        questionTranslationItem = AutoMapper.mapDefinedBySchema('questionTranslationModel', INSTANCE_MAPPINGS, questionTranslation);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Manually populate pools ids
                function (next) {
                    return PoolHasQuestion.findAndCountAll({
                        where: { questionId: questionTranslationItem.questionId },
                        attributes: [PoolHasQuestion.tableAttributes.poolId.field],
                        limit: Config.rdbms.limit,
                        offset: 0
                    }).then(function (poolHasQuestions) {
                        var poolsIds = _.map(poolHasQuestions.rows, function (item) {
                            return item.dataValues.poolId;
                        });
                        questionTranslationItem.poolsIds = {
                            items: poolsIds,
                            limit: Config.rdbms.limit,
                            offset: 0,
                            total: poolHasQuestions.count
                        };
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Manually populate isDefaultTranslation dynamic flag
                function (next) {
                    var where = { id: questionTranslationItem.id };
                    where = CrudHelper.extendSearchByCustom(params, QuestionTranslation, 'questionTranslationModel', INSTANCE_MAPPINGS, where, 'customQueryIsDefault', clientSession);
                    return QuestionTranslation.count({
                        where: where,
                        limit: Config.rdbms.limit,
                        offset: 0,
                        subQuery: false
                    }).then(function (count) {
                        questionTranslationItem.isDefaultTranslation = count > 0;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ questionTranslation: questionTranslationItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var total = 0;
        var questionTranslationItems = [];
        var questionTranslationTotal = 0;
        var mapBySchema = 'questionTranslationModel';
        try {
            var orderBy = CrudHelper.orderBy(params, QuestionTranslation);
            var searchBy = CrudHelper.searchBy(params, QuestionTranslation);
            searchBy = CrudHelper.extendSearchByOmitBlocked(QuestionTranslation, clientSession, searchBy);
            var instanceMappings = _.cloneDeep(INSTANCE_MAPPINGS);
            var rootModel = _.find(instanceMappings[mapBySchema], { destination: '$root' });
            if (_.has(params, '_unique') && params._unique) {
                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'isAlreadyReviewedByUser')) {
                    var customQueryOptional = params.searchBy.isAlreadyReviewedByUser ? 'customQueryOptionalHasReview' : 'customQueryOptionalHasNotReview';
                    rootModel.customQueryOptional.push(rootModel[customQueryOptional]);
                }
                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'isDefaultTranslation')) {
                    var customQueryOptional = params.searchBy.isDefaultTranslation ? 'customQueryOptionalIsDefault' : 'customQueryOptionalIsNotDefault';
                    rootModel.customQueryOptional.push(rootModel[customQueryOptional]);
                }
                rootModel.custom = true;
            }
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                searchBy = CrudHelper.searchByFullText(params, QuestionTranslation, mapBySchema, instanceMappings, clientSession);
            } else if (params._unique) {
                searchBy = CrudHelper.searchByCustom(params, QuestionTranslation, mapBySchema, instanceMappings, clientSession);
            }
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'poolsIds')) {
                searchBy = CrudHelper.extendSearchByCustom(params, QuestionTranslation, mapBySchema, instanceMappings, searchBy, 'customQueryPoolsIds');
            }
            if (!params._unique) {
                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'isAlreadyReviewedByUser')) {
                    var customQuery = params.searchBy.isAlreadyReviewedByUser ? 'customQueryHasReview' : 'customQueryHasNotReview';
                    searchBy = CrudHelper.extendSearchByCustom(params, QuestionTranslation, mapBySchema, instanceMappings, searchBy, customQuery, clientSession);
                }
                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'isDefaultTranslation')) {
                    var customQuery = params.searchBy.isDefaultTranslation ? 'customQueryIsDefault' : 'customQueryIsNotDefault';
                    searchBy = CrudHelper.extendSearchByCustom(params, QuestionTranslation, mapBySchema, instanceMappings, searchBy, customQuery, clientSession);
                }
            }
            var include = CrudHelper.include(QuestionTranslation, [
                Question,
                Language
            ], params, mapBySchema, instanceMappings);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        var questionTemplateWhere = { tenantId: sessionTenantId };
                        include = CrudHelper.includeNestedWhere(include, Question, QuestionTemplate, questionTemplateWhere, true);
                        if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                            include = CrudHelper.includeNestedWhere(include, Question, [QuestionHasTag, Tag], { tenantId: sessionTenantId }, false);
                        }
                        return next();
                    });
                },
                // Get question translation record
                function (next) {
                    return QuestionTranslation.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (questionTranslations) {
                        questionTranslationTotal = questionTranslations.count;
                        questionTranslationItems = AutoMapper.mapListDefinedBySchema(mapBySchema, instanceMappings, questionTranslations.rows);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Manually populate pools ids for each question translation separately
                function (next) {
                    if (questionTranslationTotal === 0 || !(_.has(params, 'searchBy') && _.has(params.searchBy, 'poolsIds'))) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(questionTranslationItems, function (questionTranslationItem, nextQuestionTranslation) {
                        return PoolHasQuestion.findAndCountAll({
                            where: { questionId: questionTranslationItem.questionId },
                            attributes: [PoolHasQuestion.tableAttributes.poolId.field],
                            limit: Config.rdbms.limit,
                            offset: 0
                        }).then(function (poolHasQuestions) {
                            var poolsIds = _.map(poolHasQuestions.rows, function (item) {
                                return item.dataValues.poolId;
                            });
                            questionTranslationItem.poolsIds = {
                                items: poolsIds,
                                limit: Config.rdbms.limit,
                                offset: 0,
                                total: poolHasQuestions.count
                            };
                            return nextQuestionTranslation();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, nextQuestionTranslation);
                        });
                    }, next);
                },
                // Manually populate isDefaultTranslation dynamic flag
                function (next) {
                    if (questionTranslationTotal === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(questionTranslationItems, function (questionTranslationItem, nextQuestionTranslation) {
                        var where = { id: questionTranslationItem.id };
                        where = CrudHelper.extendSearchByCustom(params, QuestionTranslation, mapBySchema, instanceMappings, where, 'customQueryIsDefault', clientSession);
                        return QuestionTranslation.count({
                            where: where,
                            limit: Config.rdbms.limit,
                            offset: 0,
                            subQuery: false
                        }).then(function (count) {
                            questionTranslationItem.isDefaultTranslation = count > 0;
                            return nextQuestionTranslation();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, nextQuestionTranslation);
                        });
                    }, next);
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({
                    items: questionTranslationItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: questionTranslationTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationBlock: function (params, message, clientSession, callback) {
        try {
            var sessionTenantId;
            var updQuestionTranslation = {
                blockerResourceId: clientSession.getUserId(),
                blockDate: _.now()
            };
            var include = CrudHelper.include(QuestionTranslation, [
                Question
            ]);
            return async.series([
                // Prevent update if entry alteady is blocked by another user and block date not expired and less than 2h
                function (next) {
                    return CrudHelper.checkModelEntryBlockedByUser(QuestionTranslation, params, clientSession, next);
                },
                // Get tenant id from client session object
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get question translation record by tenant
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Question, QuestionTemplate, { tenantId: sessionTenantId }, true);
                    return QuestionTranslation.findOne({
                        where: { id: params.id },
                        include: include
                    }).then(function (questionTranslation) {
                        if (!questionTranslation) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update question translation block attributes
                function (next) {
                    return QuestionTranslation.update(updQuestionTranslation, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        if (count[0] === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
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
    },

    questionTranslationReject: function (params, clientSession, callback) {
        try {
            var updQuestionTranslation = {
                approverResourceId: null,
                approveDate: null
            };
            return QuestionTranslation.update(updQuestionTranslation, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                if (count[0] === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationApprove: function (params, clientSession, callback) {
        try {
            var updQuestionTranslation = {
                approverResourceId: clientSession.getUserId(),
                approveDate: _.now()
            };
            return QuestionTranslation.update(updQuestionTranslation, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                if (count[0] === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationGetForPublish: function (params, message, clientSession, callback) {
        try {
            var translation;
            var sessionTenantId;
            async.series ([
                // Get tenant id from session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },

                // Select question translation by tenant
                function (next) {
                    return QuestionTranslation.findOne({
                        where: { id: params.id },
                        include: [
                            {
                                model: Question, required: true,
                                include: [
                                    { model: QuestionTemplate, required: true, where: { tenantId: sessionTenantId } },
                                    { model: RegionalSetting, required: true, include: { model: Language, required: true } },
                                ]
                            },
                            { model: Language, required: true }
                        ]
                    }).then(function (questionTranslation) {
                        if (!questionTranslation) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        translation = questionTranslation;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },

                // Select all international translations for question being published, populate hints if they are defined
                function (next) {
                    return QuestionTranslation.findAll({
                        where: { questionId: translation.question.id },
                        include: [
                            { model: Language, required: true }
                        ]
                    }).then(function (questionTranslations) {
                        if (!questionTranslations) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        translation.translationsIndex = questionTranslations;
                        var hints = {};
                        _.forEach(questionTranslations, function (questionTranslation) {
                            var questionTranslationItem = questionTranslation.get({ plain: true });
                            if (!_.isEmpty(questionTranslationItem.hint)) {
                                hints[questionTranslationItem.language.iso] = questionTranslationItem.hint;
                            }
                        });
                        // If there is at least one hint defined, there should be present also hint of primary regional setting's default language
                        if (!_.isEmpty(hints)) {
                            if (!_.has(hints, translation.question.regionalSetting.language.iso)) {
                                return next(Errors.QuestionApi.NoDefaultHintDefined);
                            }
                            hints['default'] = hints[translation.question.regionalSetting.language.iso];
                            if (!translation.hint) {
                                translation.hint = hints['default'];
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },

                function (next) {
                    return Pool.findAll({
                        include: [
                            { model: PoolHasQuestion, required: true, where: {
                                questionId: translation.questionId
                            }
                        }],
                        subQuery: false
                    }).then(function (pools) {
                        if (!pools) {
                            return next(Errors.QuestionApi.QuestionHasNoPool);
                        }
                        translation.pools = pools;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },

                function (next) {
                    return QuestionTemplate.findOne({
                        where: { id: translation.question.questionTemplateId }
                    }).then(function (questionTemplate) {
                        if (!questionTemplate) {
                            return next(Errors.QuestionApi.QuestionHasNoTemplate);
                        }
                        translation.questionTemplate = questionTemplate;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },

            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                try {
                    var questionTranslationItem = AutoMapper.mapDefinedBySchema('questionTranslationPublishModel', INSTANCE_MAPPINGS, translation);
                    logger.info('QuestionTranslationApiFactory.questionTranslationGetForPublish', JSON.stringify(questionTranslationItem));
                    return CrudHelper.callbackSuccess(questionTranslationItem, callback);
                } catch (ex) {
                    return CrudHelper.callbackError(ex, callback);
                }
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationSetDeploymentStatus: function (params, clientSession, callback) {
        try {
            return QuestionTranslation.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                if (count[0] === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationPublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var questionTranslation;
            var mediasIds = [];
            var encryptedMediasIds = [];
            var mediaMapping = {};
            var answers = {};
            var correctAnswers = [];
            var questionSteps = [];
            var answerMaxTimes = [];
            var questionBlobKeys;
            async.series ([
                function (next) {
                    try {
                        return self.questionTranslationGetForPublish(params, message, clientSession, function (err, questionTranslationPublish) {
                            if (err) {
                                return next(err);
                            }
                            if (!questionTranslationPublish) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            questionTranslation = questionTranslationPublish;
                            try {
                                if (questionTranslation.questionTemplate.structureDefinition && !_.isObject(questionTranslation.questionTemplate.structureDefinition)) {
                                    questionTranslation.questionTemplate.structureDefinition = JSON.parse(questionTranslation.questionTemplate.structureDefinition);
                                }
                            } catch (ex) {
                                logger.error('questionTranslationPublish error on JSON parsing: structureDefinition', questionTranslation.questionTemplate.structureDefinition, ex);
                                return next(Errors.QuestionApi.ValidationFailed);
                            }
                            try {
                                if (questionTranslation.content && !_.isObject(questionTranslation.content)) {
                                    questionTranslation.content = JSON.parse(questionTranslation.content);
                                }
                            } catch (ex) {
                                logger.error('questionTranslationPublish error on JSON parsing: content', questionTranslation.content, ex);
                                return next(Errors.QuestionApi.ValidationFailed);
                            }
                            if (!_.isObject(questionTranslation.questionTemplate.structureDefinition) ||
                                !_.isArray(questionTranslation.content) ||
                                !_.isArray(questionTranslation.questionTemplate.structureDefinition.frames) ||
                                questionTranslation.content.length !== questionTranslation.questionTemplate.structureDefinition.frames.length) {
                                return next(Errors.QuestionTranslationApi.NotValidQuestionStructureDefinition);
                            }
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        return QuestionService.questionMediaList(questionTranslation.content, function (err, mediaList) {
                            if (err) {
                                return next(err);
                            }
                            if (_.isEmpty(mediaList)) {
                                return next();
                            }
                            mediasIds = _.uniq(mediaList);
                            Media.findAll({ where: { id: { '$in': mediasIds } } }).then(function (mediaItems) {
                                if (mediaItems.length !== mediasIds.length) {
                                    return next(Errors.QuestionTranslationApi.MediaNotExist);
                                }
                                async.mapSeries(mediaItems, function (media, mediaSerie) {
                                    try {
                                        var mediaItem = media.get({ plain: true });
                                        var encryptionKey = CryptoService.generateEncryptionKey();
                                        var originalMediaName = nodepath.parse(mediaItem.id).name;
                                        var encryptedMediaName = uuid.v4();
                                        var extension = nodepath.extname(mediaItem.id);

                                        var resolutions = [{
                                            original: originalMediaName + extension,
                                            encrypted: encryptedMediaName
                                        }];
                                        _.forEach(PublishService.getResolutions(mediaItem.namespace), function (resolution) {
                                            resolutions.push({
                                                original: PublishService.getMediaFileFormatTemplate().replace(/%BASENAME%/, originalMediaName)
                                                    .replace(/%LABEL%/, resolution.label)
                                                    .replace(/%EXTENSION%/, extension),
                                                encrypted: PublishService.getMediaFileFormatTemplate().replace(/%BASENAME%/, encryptedMediaName)
                                                    .replace(/%LABEL%/, resolution.label)
                                                    .replace(/%EXTENSION%/, '')
                                            });
                                        });
                                        async.mapSeries(resolutions, function (resolution, resolutionSerie) {
                                            var objectKey = mediaItem.namespace + '/' + resolution.original;
                                            return CryptoService.encryptObject(objectKey, encryptionKey, function (err, encryptedBlob) {
                                                if (err) {
                                                    return resolutionSerie(err);
                                                }
                                                PublishService.publishMedia({
                                                    encryptedMediaName: resolution.encrypted,
                                                    encryptedMedia: encryptedBlob
                                                }, function (evt) { }, resolutionSerie);
                                            });
                                        }, function (err) {
                                            if (err) {
                                                logger.error('questionTranslationPublish error1: ', err);
                                                return mediaSerie(err);
                                            }
                                            mediaMapping[mediaItem.id] = {
                                                blobId: resolutions[0].encrypted,
                                                key: encryptionKey.toString('hex')
                                            };
                                            encryptedMediasIds.push(resolutions[0].encrypted);
                                            return mediaSerie();
                                        });
                                    } catch (ex) {
                                        logger.error('questionTranslationPublish error2: ', err);
                                        return mediaSerie(ex);
                                    }
                                }, function (err) {
                                    if (err) {
                                        logger.error('questionTranslationPublish error3: ', err);
                                        return next(Errors.QuestionTranslationApi.MediaCouldNotBeUploaded);
                                    }
                                    return next();
                                });
                            }).catch(function (err) {
                                logger.error('questionTranslationPublish error4: ', err);
                                return CrudHelper.callbackError(err, next);
                            });
                        });
                    } catch (ex) {
                        logger.error('questionTranslationPublish error5: ', err);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        return QuestionService.questionStepsList(questionTranslation.content, function (err, stepsJson) {
                            if (err) {
                                return next(err);
                            }
                            var steps = stepsJson;
                            if (_.isString(stepsJson)) {
                                steps = JSON.parse(stepsJson);
                            }
                            // Validate steps: mappings should exist
                            var mappingExist = true;
                            _.forEach(steps, function (step) {
                                if (!_.has(step, 'mappings')) {
                                    mappingExist = false;
                                }
                            });
                            if (!mappingExist) {
                                return next(Errors.QuestionApi.QuestionTranslationPublishingNoStepsMappings);
                            }
                            async.mapSeries(steps, function (step, serie) {
                                try {
                                    answerMaxTimes.push(step.countdown * 1000);
                                    _.forEach(step.mappings, function(object) {
                                        if (_.has(object, 'answers')) {
                                            var answerKeys = [];
                                            _.forEach(object.answers, function(answer) {
                                                var id = uuid();
                                                answerKeys.push([id, answer]);
                                            });

                                            if (object.handler === 'SortingAnswerHandler') {
                                                _.forEach(answerKeys, function(answer) {
                                                    correctAnswers.push(answer[0]);
                                                });
                                            } else if (object.handler === 'SingleChoiceAnswerHandler') {
                                                correctAnswers.push(answerKeys[0][0]);
                                            }

                                            var shuffledAnswers = _.shuffle(answerKeys);

                                            _.forEach(shuffledAnswers, function(answer) {
                                                answers[answer[0]] = answer[1];
                                            });

                                            object.answers = answers;
                                        }
                                    });
                                    step['medias'] = mediaMapping;
                                    step['structure'] = questionTranslation.questionTemplate.structureDefinition.frames[_.indexOf(questionTranslation.content, step)];
                                    var encryptedStepName = uuid.v4();
                                    const encryptionKey = CryptoService.generateEncryptionKey();
                                    return CryptoService.encryptData(step, encryptedStepName, encryptionKey, function (err, encryptedBlob) {
                                        if (err) {
                                            return serie(err);
                                        }
                                        PublishService.publishQuestionStep({
                                            encryptedStepName: encryptedStepName,
                                            encryptedStep: encryptedBlob,
                                        }, function (evt) { }, function(err, data) {
                                            if (err) {
                                                return serie(err);
                                            }
                                            questionSteps.push({
                                                encryptedStepName: encryptedStepName,
                                                encryptionKey: encryptionKey.toString('hex')
                                            });
                                            return serie();
                                        });
                                    });
                                } catch (ex) {
                                    return serie(ex);
                                }
                            }, function (err) {
                                return next(err);
                            });
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        var translationParams = {};
                        translationParams.id = params.id.toString();
                        translationParams.questionId = questionTranslation.question.id.toString();
                        translationParams.questionCreatorResourceId = questionTranslation.question.creatorResourceId;
                        translationParams.poolNames = questionTranslation.pools.items;
                        translationParams.templateName = questionTranslation.questionTemplate.name;
                        translationParams.complexity = questionTranslation.question.complexity;
                        translationParams.questionResolutionImage = questionTranslation.question.resolutionImageId;
                        translationParams.source = questionTranslation.question.source;
                        translationParams.language = questionTranslation.language.iso;
                        translationParams.stepCount = questionSteps.length;
                        translationParams.mediaMapping = mediaMapping;
                        translationParams.questionBlobKeys = _.map(questionSteps, 'encryptedStepName');
                        translationParams.decryptionKeys = _.map(questionSteps, 'encryptionKey');
                        translationParams.hint = questionTranslation.hint;
                        translationParams.medias = encryptedMediasIds;
                        translationParams.steps = questionSteps;
                        translationParams.type = questionTranslation.questionTemplate.id.toString();
                        translationParams.answerMaxTimes = answerMaxTimes;
                        translationParams.questionResolutionText = questionTranslation.resolutionText;
                        translationParams.questionExplanation = questionTranslation.explanation;
                        translationParams.correctAnswers = correctAnswers;
                        translationParams.answers = _.keys(answers);
                        
                        async.mapSeries(translationParams.poolNames, function (poolName, poolSerie) {
                            translationParams.multiIndex = {
                                poolId: poolName.toString(),
                                complexity: translationParams.complexity,
                                type: translationParams.type,
                                index: {}
                            }
                            _.forEach(questionTranslation.translationsIndex, function(translation) {
                                translationParams.multiIndex.index[translation.language.iso] = translation.publishIndex;
                            });
                            var metaExists = true;
                            async.series([
                                // get count of questions in the pool (meta)
                                function(poolNext) {
                                    translationParams.poolId = poolName.toString();
                                    if (_.isNull(questionTranslation.publishIndex)) {
                                        return AerospikePoolMeta.increment(translationParams, function (err, poolMeta) {
                                            if (err) {
                                                return poolNext(err);
                                            }
                                            translationParams.index = poolMeta - 1;
                                            return poolNext();
                                        });
                                    } else {
                                        translationParams.index = questionTranslation.publishIndex;
                                        return poolNext();
                                    }
                                },

                                // deploy question translation to Aerospike.
                                function(poolNext) {
                                    translationParams.multiIndex.index[questionTranslation.language.iso] = translationParams.index;
                                    // save question blob
                                    return AerospikePool.create(translationParams, function (err, data) {
                                        if (err) {
                                            return poolNext(err);
                                        }
                                        // Update other translations with new multiIndex
                                        async.mapSeries(questionTranslation.translationsIndex, function (translation, indexSerie) {
                                            var otherTranslation = {
                                                poolId: translationParams.poolId,
                                                complexity: translationParams.complexity,
                                                language: translation.language.iso,
                                                index: translation.publishIndex,
                                                multiIndex: translationParams.multiIndex
                                            };
                                            return AerospikePool.update(otherTranslation, function (err, data) {
                                                if (err === Errors.DatabaseApi.NoRecordFound) {
                                                    return indexSerie();
                                                }
                                                return indexSerie(err);
                                            });
                                        }, poolNext);
                                    });

                                },

                                // publish multiIndex if needed
                                function(poolNext) {
                                    if (!questionTranslation.question.isInternational) {
                                        return poolNext();
                                    }
                                    async.series([
                                        // get count of international questions in the pool (meta)
                                        function(internationalNext) {
                                            translationParams.poolId = poolName.toString();
                                            if (_.isNull(questionTranslation.publishMultiIndex)) {
                                                return AerospikePoolIndexMeta.increment(translationParams, function (err, poolMeta) {
                                                    if (err) {
                                                        return internationalNext(err);
                                                    }
                                                    translationParams.internationalIndex = poolMeta - 1;
                                                    return internationalNext();
                                                });
                                            } else {
                                                translationParams.internationalIndex = questionTranslation.publishMultiIndex;
                                                return internationalNext();
                                            }
                                        },
                                        // deploy question multiIndex to Aerospike.
                                        function (internationalNext) {
                                            // save mutiIndex
                                            translationParams.multiIndex = translationParams.multiIndex.index;
                                            return AerospikePoolIndex.create(translationParams, internationalNext);
                                        },
                                        // save question index for a translation
                                        function (internationalNext) {
                                            return QuestionTranslation.update({publishMultiIndex: translationParams.index}, {where: {id: params.id}, individualHooks: true }).then(function (count) {
                                                if (count[0] === 0) {
                                                    return internationalNext(Errors.DatabaseApi.NoRecordFound);
                                                }
                                                return internationalNext();
                                            }).catch(function (err) {
                                                return CrudHelper.callbackError(err, internationalNext);
                                            });
                                        }
                                    ], poolNext);
                                },

                                // save question index for a translation
                                function(poolNext) {
                                    return QuestionTranslation.update({publishIndex: translationParams.index}, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                                        if (count[0] === 0) {
                                            return next(Errors.DatabaseApi.NoRecordFound);
                                        }
                                        questionBlobKeys = translationParams.questionBlobKeys;
                                        return poolNext();
                                    }).catch(function (err) {
                                        return CrudHelper.callbackError(err, poolNext);
                                    });
                                },

                            ], poolSerie);
                        }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        return QuestionTranslation.update({ questionBlobKeys: questionBlobKeys }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        translationParams = {};
                        translationParams.id = params.id;
                        translationParams.status = QuestionTranslation.constants().STATUS_ACTIVE;
                        return self.questionTranslationSetDeploymentStatus(translationParams, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
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
    },

    questionTranslationUnpublish: function (params, message, clientSession, callback) {
        var self = this;
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            var questionTranslation;
            async.series ([

                function (next) {
                    try {
                        return self.questionTranslationGetForPublish(params, message, clientSession, function (err, questionTranslationPublish) {
                            if (err) {
                                return next(err);
                            }
                            questionTranslation = questionTranslationPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function(next) {
                    try {
                        var translationParams = {};
                        translationParams.poolNames = questionTranslation.pools.items;
                        translationParams.complexity = questionTranslation.question.complexity;
                        translationParams.language = questionTranslation.language.iso;
                        translationParams.index = questionTranslation.publishIndex;
                        translationParams.type = questionTranslation.questionTemplate.id.toString();

                        async.mapSeries(translationParams.poolNames, function (poolName, poolSerie) {
                            var removedQuestionTranslation;
                            translationParams.poolId = poolName;
                            translationParams.multiIndex = {
                                poolId: poolName,
                                complexity: translationParams.complexity,
                                index: {}
                            }
                            _.forEach(questionTranslation.translationsIndex, function(translation) {
                                translationParams.multiIndex.index[translation.language.iso] = translation.publishIndex;
                            });
                            async.series([

                                // remove question translation from Aerospike.
                                function(poolNext) {
                                    return AerospikePool.findOne(translationParams, function (err, pool) {
                                        if (err) {
                                            return poolNext(err);
                                        }
                                        removedQuestionTranslation = pool;
                                        return poolNext();
                                    });
                                },

                                // remove question translation from Aerospike.
                                function(poolNext) {
                                    _.unset(translationParams.multiIndex.index, questionTranslation.language.iso);

                                    // save question blob
                                    logger.warn('unpublish: AerospikePool', JSON.stringify(translationParams));
                                    AerospikePool.remove(translationParams, function (err, data) {
                                        if (err) {
                                            return poolNext(err);
                                        }

                                        async.series([
                                            // Update other translations with new multiIndex
                                            function(translationNext) {
                                                async.mapSeries(questionTranslation.translationsIndex, function (translation, translationSerie) {
                                                    if (translation.language.iso === questionTranslation.language.iso) {
                                                        // skip removed translation
                                                        return translationSerie();
                                                    }
                                                    var otherTranslation = {
                                                        poolId: translationParams.poolId,
                                                        complexity: translationParams.complexity,
                                                        language: translation.language.iso,
                                                        index: translation.publishIndex,
                                                        multiIndex: translationParams.multiIndex
                                                    };
                                                    return AerospikePool.update(otherTranslation, translationSerie);
                                                }, translationNext);
                                            },

                                            // Remove S3 objects
                                            function(translationNext) {
                                                var objectsToRemove = removedQuestionTranslation.medias;
                                                objectsToRemove = _.union(objectsToRemove, removedQuestionTranslation.questionBlobKeys);
                                                logger.warn('unpublish: objectsToRemove', JSON.stringify(objectsToRemove));
                                                if (!objectsToRemove || objectsToRemove.length === 0) {
                                                    return translationNext();
                                                }
                                                return PublishService.unpublishObjects(objectsToRemove,
                                                    function (err, list) {
                                                        if (err) {
                                                            return translationNext(err);
                                                        }
                                                        if (list.Deleted.length < objectsToRemove.length) {
                                                            return translationNext(Errors.QuestionApi.FatalError);
                                                        }
                                                        return translationNext();
                                                    }
                                                )
                                            }

                                        ], poolNext);
                                    });
                                },

                                // unpublish multiIndex if needed
                                function(poolNext) {
                                    if (!questionTranslation.question.isInternational) {
                                        return poolNext();
                                    }
                                    translationParams.internationalIndex = questionTranslation.publishMultiIndex;
                                    // remove question multiIndex from Aerospike.
                                    logger.warn('unpublish: AerospikePoolIndex', JSON.stringify(translationParams));
                                    return AerospikePoolIndex.remove(translationParams, poolNext);
                                },

                            ], poolSerie);
                        }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        return QuestionTranslation.update({ questionBlobKeys: null }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        translationParams = {};
                        translationParams.id = params.id;
                        translationParams.status = QuestionTranslation.constants().STATUS_INACTIVE;
                        return self.questionTranslationSetDeploymentStatus(translationParams, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
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
    },

    questionTranslationBulkPublish: function (params, message, clientSession, callback) {
        var self = this;
        if (!params.hasOwnProperty('questionTranslationIds')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.mapSeries(params.questionTranslationIds, function (questionTranslationId, serie) {
                try {
                    var params = {};
                    params.id = questionTranslationId;
                    return self.questionTranslationPublish(params, message, clientSession, serie);
                } catch (ex) {
                    return setImmediate(serie, ex);
                }
            }, function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTranslationBulkUnpublish: function (params, message, clientSession, callback) {
        var self = this;
        if (!params.hasOwnProperty('questionTranslationIds')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.mapSeries(params.questionTranslationIds, function (questionTranslationId, serie) {
                try {
                    var params = {};
                    params.id = questionTranslationId;
                    return self.questionTranslationUnpublish(params, message, clientSession, serie);
                } catch (ex) {
                    return setImmediate(serie, ex);
                }
            }, function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    questionTranslationDeactivateManually: function (params, message, clientSession, callback) {
        var self = this;
        return async.series([
            // unpublish
            function (next) {
                try {
                    self.questionTranslationUnpublish(params, message, clientSession, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // unset isActivatedManually flag for translation
            function (next) {
                try {
                    var update = { isActivatedManually: 0 };
                    var options = {
                        where: {
                            'id': params.id
                        },
                        individualHooks: false
                    };
                    return QuestionTranslation.update(update, options).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // Add event log for translation
            function (next) {
                try {
                    return WorkflowCommon.addEventLog({
                        questionId: null,
                        questionTranslationId: params.id,
                        triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                        oldStatus: QuestionTranslation.constants().STATUS_ACTIVE,
                        newStatus: QuestionTranslation.constants().STATUS_INACTIVE
                    }, next);
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            }
        ], function (err) {
            if (err) {
                return CrudHelper.callbackError(err, callback);
            }
            return CrudHelper.callbackSuccess(null, callback);
        });
    }
};
