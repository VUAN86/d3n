var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var MediaHasTag = Database.RdbmsService.Models.Media.MediaHasTag;
var Language = Database.RdbmsService.Models.Question.Language;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionHasRegionalSetting = Database.RdbmsService.Models.Question.QuestionHasRegionalSetting;
var QuestionHasRelatedQuestion = Database.RdbmsService.Models.Question.QuestionHasRelatedQuestion;
var QuestionHasTag = Database.RdbmsService.Models.Question.QuestionHasTag;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationIssue = Database.RdbmsService.Models.Question.QuestionOrTranslationIssue;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;
var RegionalSettingHasLanguage = Database.RdbmsService.Models.Question.RegionalSettingHasLanguage;
var RegionalSetting = Database.RdbmsService.Models.Question.RegionalSetting;
var Pool = Database.RdbmsService.Models.Question.Pool;
var PoolHasTag = Database.RdbmsService.Models.Question.PoolHasTag;
var PoolHasQuestion = Database.RdbmsService.Models.Question.PoolHasQuestion;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var WorkorderHasPool = Database.RdbmsService.Models.Workorder.WorkorderHasPool;
var WorkorderHasTenant = Database.RdbmsService.Models.Workorder.WorkorderHasTenant;
var WorkorderHasRegionalSetting = Database.RdbmsService.Models.Workorder.WorkorderHasRegionalSetting;
var WorkorderHasQuestionExample = Database.RdbmsService.Models.Workorder.WorkorderHasQuestionExample;
var PaymentStructure = Database.RdbmsService.Models.Billing.PaymentStructure;
var PaymentAction = Database.RdbmsService.Models.Billing.PaymentAction;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var Tag = Database.RdbmsService.Models.Question.Tag;
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var BadWords = require('nodejs-bad-words');
var badWords = new BadWords();
var TenantService = require('../services/tenantService.js');
var AnalyticEvent = require('../services/analyticEvent.js');
var QuestionWorkflow = require('../workflows/QuestionWorkflow.js');
var QuestionTranslationWorkflow = require('../workflows/QuestionTranslationWorkflow.js');
var WorkflowCommon = require('../workflows/WorkflowCommon.js');
var QuestionTranslationApiFactory = require('./questionTranslationApiFactory.js');
var RegionalSettingApiFactory = require('./../factories/regionalSettingApiFactory.js');
var QuestionTemplateApiFactory = require('./../factories/questionTemplateApiFactory.js');
var PoolApiFactory = require('./../factories/poolApiFactory.js');
var fs = require('fs');
var xlsxj = require("xlsx-to-json");
var request = require('request');
let stacks={};
const Media = Database.RdbmsService.Models.Media.Media;


let imagesFolder = Config.httpUpload.tempFolder + '/images';

var INSTANCE_MAPPINGS = {
    'questionModel': [
        {
            destination: '$root',
            model: Question,
            custom: false,
            customQueryHasReview: 'EXISTS (SELECT 1 FROM question_or_translation_review qtr WHERE `question`.`id` = qtr.questionId AND `question`.`version` = qtr.version AND qtr.resourceId = "$user")',
            customQueryHasNotReview: 'NOT EXISTS (SELECT 1 FROM question_or_translation_review qtr WHERE `question`.`id` = qtr.questionId AND `question`.`version` = qtr.version AND qtr.resourceId = "$user")',
            fullTextQuery: 'LOWER(`questionHasTags`.`tagTag`) LIKE "%fulltext_word%" OR LOWER(`questionTranslations`.`content`) LIKE "%fulltext_word%" OR LOWER(`questionTranslations`.`explanation`) LIKE "%fulltext_word%" OR LOWER(`questionTranslations`.`hint`) LIKE "%fulltext_word%"',
            fullTextInclude: [QuestionHasTag, QuestionTranslation]
        },
        {
            destination: 'tags',
            model: QuestionHasTag,
            attributes: [
                { 'tag': QuestionHasTag.tableAttributes.tagTag }
            ],
            type: AutoMapper.TYPE_ATTRIBUTE_LIST,
            limit: 0,
        },
        {
            destination: 'regionalSettingsIds',
            model: QuestionHasRegionalSetting,
            attribute: QuestionHasRegionalSetting.tableAttributes.regionalSettingId,
            type: AutoMapper.TYPE_ATTRIBUTE_LIST,
            limit: 0
        },
        {
            destination: 'relatedQuestionsIds',
            model: QuestionHasRelatedQuestion,
            attribute: QuestionHasRelatedQuestion.tableAttributes.questionId,
            type: AutoMapper.TYPE_ATTRIBUTE_LIST,
            limit: 0
        },
        {
            destination: 'poolsIds',
            model: PoolHasQuestion,
            attribute: PoolHasQuestion.tableAttributes.poolId,
            type: AutoMapper.TYPE_ATTRIBUTE_LIST,
            limit: 0
        },
        {
            destination: 'pools',
            model: Pool,
            hasModel: PoolHasQuestion,
            type: AutoMapper.TYPE_OBJECT_LIST,
            limit: 0
        },
        {
            destination: 'questionTranslationsIds',
            model: QuestionTranslation,
            attribute: QuestionTranslation.tableAttributes.id,
            type: AutoMapper.TYPE_ATTRIBUTE_LIST,
            limit: 0
        },
        {
            destination: 'isAlreadyReviewedByUser',
            skip: true
        },
    ],
    'questionCreateModel': [
        {
            destination: '$root',
            model: Question
        },
        {
            destination: 'tags',
            model: QuestionHasTag,
            attributes: [
                { 'tag': QuestionHasTag.tableAttributes.tagTag }
            ]
        },
        {
            destination: 'poolsIds',
            model: PoolHasQuestion,
            attribute: PoolHasQuestion.tableAttributes.poolId
        },
        {
            destination: 'regionalSettingsIds',
            model: QuestionHasRegionalSetting,
            attribute: QuestionHasRegionalSetting.tableAttributes.regionalSettingId
        }
    ],
    'tagModel': [
        {
            destination: '$root',
            model: Tag
        }
    ],
};
INSTANCE_MAPPINGS['questionUpdateModel'] = INSTANCE_MAPPINGS['questionCreateModel'];

const questionApiExports = {

    /**
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {function} callback
     * @returns {*}
     */
    questionUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            
            var create = true;
            var questionId = undefined;
            var mapBySchema = 'questionCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                questionId = params.id;
                mapBySchema = 'questionUpdateModel';
            }
            var questionItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var sessionTenantId;
            var questionItemAssociates = undefined;
            var questionCount = 0;
            async.series([
                // prevent update if in workflow
                function (next) {
                    return setImmediate(next);
                    //if (!(process.env.WORKFLOW_ENABLED === 'true') || create) {
                    //    return setImmediate(next);
                    //}
                    //QuestionWorkflow.hasWorkflow(params.id, function (err, hasWorkflow) {
                    //    if (err) {
                    //        logger.error('questionUpdate error on hasWorkflow():', err);
                    //        return next(err);
                    //    }
                    //    if (hasWorkflow) {
                    //        logger.error('questionUpdate question is in workflow');
                    //        return next(Errors.QuestionApi.ValidationFailed);
                    //    }
                    //    return next();
                    //});
                },
                // Get tenant id from session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // check bad words on question source
                function (next) {
                    return badWords.hasBadWords(questionItem, ['source'], function (err, result) {
                        if (err) {
                            return next(err);
                        }
                        if (result === true) {
                            return next(Errors.QuestionApi.EntityContainBadWords);
                        }
                        return next();
                    });
                },
                // check bad words on question tags
                function (next) {
                    try {
                        if (!params.tags || !params.tags.length) {
                            return setImmediate(next, false);
                        }
                        var entity = {
                            tags: params.tags.join(' ')
                        };
                        return badWords.hasBadWords(entity, ['tags'], function (err, result) {
                            if (err) {
                                return next(err);
                            }
                            if (result === true) {
                                return next(Errors.QuestionApi.EntityContainBadWords);
                            }
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // check resolution image if questionTemplate.hasResolution is set
                function (next) {
                    if (_.has(questionItem, 'resolutionImageId') && questionItem.resolutionImageId !== null) {
                        return setImmediate(next, false);
                    }
                    if (create) {
                        return QuestionTemplate.count({
                            where: {
                                id: questionItem.questionTemplateId,
                                hasResolution: { $eq: 1 }
                            }
                        }).then(function (count) {
                            if (count === 1) {
                                return next(Errors.QuestionApi.NoResolution);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Question.count({
                            include: [
                                {
                                    model: QuestionTemplate,
                                    where: { hasResolution: { $eq: 1 } }
                                }
                            ],
                            where: {
                                id: questionItem.id,
                            },
                            subQuery: false
                        }).then(function (count) {
                            if (count === 1) {
                                return next(Errors.QuestionApi.NoResolution);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // Check workorder's items required if workorderId passed and workorder is closed
                function (next) {
                    if (!_.has(params, 'workorderId') || !params.workorderId) {
                        return setImmediate(next, null);
                    }
                    var where = { workorderId: params.workorderId };
                    if (!create) {
                        where = { $and: [where, { id: { $ne: params.id }}] };
                    }
                    return Question.count({
                        where: where
                    }).then(function (count) {
                        questionCount = count;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                function (next) {
                    if (!_.has(params, 'workorderId') || !params.workorderId) {
                        return setImmediate(next, null);
                    }
                    var include = CrudHelper.include(Workorder, [
                        WorkorderHasTenant
                    ]);
                    include = CrudHelper.includeWhere(include, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    return Workorder.findOne({
                        where: { id: params.workorderId },
                        include: include
                    }).then(function (workorder) {
                        if (!workorder) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        var workorderPlain = workorder.get({ plain: true });
                        if (workorderPlain.status === Workorder.constants().STATUS_CLOSED &&
                            workorderPlain.itemsRequired && workorderPlain.itemsRequired > 0 && workorderPlain.itemsRequired <= questionCount) {
                            return next(Errors.WorkorderApi.WorkorderClosedItemsRequiredReached);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // If defined params.workorderId, associate question with workorder pools and regional settings: 
                // append to params.poolsIds, params.regionalSettingsIds, update params.primaryRegionalSettingId and proceed to next step
                function (next) {
                    if (!_.has(params, 'workorderId') || !params.workorderId) {
                        return setImmediate(next, null);
                    }
                    async.series([
                        // Copy pools if found
                        function (woNext) {
                            try {
                                return WorkorderHasPool.findAndCountAll({
                                    limit: null,
                                    offset: null,
                                    where: { workorderId: params.workorderId },
                                    order: []
                                }).then(function (workorderHasPools) {
                                    if (workorderHasPools.count > 0) {
                                        if (!_.has(params, 'poolsIds')) {
                                            params.poolsIds = [];
                                        }
                                        _.forEach(workorderHasPools.rows, function (workorderHasPool) {
                                            if (!_.includes(params.poolsIds, workorderHasPool.poolId)) {
                                                params.poolsIds.push(workorderHasPool.poolId);
                                            }
                                        });
                                    }
                                    return woNext();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, woNext);
                                });
                            } catch (ex) {
                                return woNext(ex);
                            }
                        },
                        // Copy primary regional setting
                        function (woNext) {
                            try {
                                return Workorder.find({
                                    where: { id: params.workorderId }
                                }).then(function (workorder) {
                                    questionItem.primaryRegionalSettingId = workorder.primaryRegionalSettingId;
                                    return woNext();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, woNext);
                                });
                            } catch (ex) {
                                return woNext(ex);
                            }
                        },
                        // Copy regional settings and primary regional setting if found
                        function (woNext) {
                            try {
                                return WorkorderHasRegionalSetting.findAndCountAll({
                                    limit: null,
                                    offset: null,
                                    where: { workorderId: params.workorderId },
                                    order: []
                                }).then(function (workorderHasRegionalSettings) {
                                    if (workorderHasRegionalSettings.count > 0) {
                                        if (!_.has(params, 'regionalSettingsIds')) {
                                            params.regionalSettingsIds = [];
                                        }
                                        _.forEach(workorderHasRegionalSettings.rows, function (workorderHasRegionalSetting) {
                                            if (!_.includes(params.regionalSettingsIds, workorderHasRegionalSetting.regionalSettingId)) {
                                                params.regionalSettingsIds.push(workorderHasRegionalSetting.regionalSettingId);
                                            }
                                        });
                                    }
                                    return woNext();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, woNext);
                                });
                            } catch (ex) {
                                return woNext(ex);
                            }
                        },
                    ], function (err) {
                        return next(err);
                    });
                },
                // Optional bulk upsert tags if passed: create tags which are not exists
                function (next) {
                    if (!_.has(params, 'tags')) {
                        return next();
                    }
                    var tagRecords = [];
                    _.forEach(params.tags, function (tag) {
                        tagRecords.push({ tag: tag, tenantId: sessionTenantId });
                    });
                    return Tag.bulkCreate(tagRecords, { updateOnDuplicate: [Tag.tableAttributes.tag.field, Tag.tableAttributes.tenantId.field] }).then(function (records) {
                        if (records.length !== tagRecords.length) {
                            return next(Errors.QuestionApi.FatalError);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Create/update main entity
                function (next) {
                    questionItem.updaterResourceId = clientSession.getUserId();
                    if (create) {
                        if (!_.has(questionItem, 'createDate')) {
                            questionItem.createDate = _.now();
                        }
                        // Creator not set -> set current user
                        if (!_.has(questionItem, 'creatorResourceId')) {
                            questionItem.creatorResourceId = clientSession.getUserId();
                        }
                        return Question.create(questionItem).then(function (question) {
                            questionId = question.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Question.update(questionItem, { where: { id: questionItem.id }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // update isTranslationNeeded on question update: only if regions are submitted to update
                function (next) {
                    if (create || !_.has(params, 'regionalSettingsIds')) {
                        return setImmediate(next);
                    }
                    var translatedLanguages = [];
                    var questionLanguages = [];

                    return async.series([

                        // find all translated languages
                        function (languageNext) {
                            return Language.findAll({
                                include: [{
                                    model: QuestionTranslation, required: true,
                                    where: { questionId: params.id }
                                }
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
                                        where: { questionId: params.id }
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
                                    }]
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
                                where: { id: params.id },
                                individualHooks: true
                            }).then(function (count) {
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                    });
                },
                // Populate question associated entity values
                function (next) {
                    try {
                        questionItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'questionId',
                                value: questionId,
                            }
                        );
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(questionItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(questionItemAssociates, function (itemAssociate, remove) {
                        return itemAssociate.model.destroy({ where: { questionId: questionId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(questionItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(questionItemAssociates, function (itemAssociate, create) {
                        if (itemAssociate.values.length === 0) {
                            return create();
                        }
                        return itemAssociate.model.bulkCreate(itemAssociate.values).then(function (records) {
                            if (records.length !== itemAssociate.values.length) {
                                return create(Errors.QuestionApi.FatalError);
                            }
                            return create();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, create);
                        });
                    }, next);
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
                            questionsCreated: 1
                        };
                        
                        AnalyticEvent.addQuestionEvent(clientInfo, eventData, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], function (err) {
                if (err) {
                    self._questionUpdateRoolback({
                        create: create,
                        questionId: questionId,
                        questionItemAssociates: questionItemAssociates
                    }, function (errRollback) {
                        if(errRollback) {
                            logger.error('Error on _questionUpdateRoolback:', errRollback);
                        }
                        return CrudHelper.callbackError(err, callback);
                    });
                    return;
                }
                params.id = questionId;
                return self.questionGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    // clean ups in case question create fails
    _questionUpdateRoolback: function (args, cb) {
        try {
            var questionId = args.questionId;
            if (!args.create || !args.questionId) {
                return setImmediate(cb);
            }

            async.series([
                // delete associations
                function (next) {
                    if (!_.isArray(args.questionItemAssociates)) {
                        return setImmediate(next);
                    }

                    async.mapSeries(args.questionItemAssociates, function (itemAssociate, remove) {
                        itemAssociate.model.destroy({ where: { questionId: questionId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },

                // delete question
                function (next) {
                    Question.destroy({where: {id: questionId}}).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    questionDeleteByStatus: function (params, callback) {
        var self = this;
        try {
            async.series([
                function (next) {
                    try {
                        self.questionCheckNotUsed(params, function (err, notUsed) {
                            if (err) {
                                return next(err);
                            }
                            if (!notUsed) {
                                return next(Errors.QuestionApi.ForeignKeyViolation);
                            }
                            return next();
                        });
                    } catch (ex) {
                        return next(ex);
                    }
                },
                function (next) {
                    async.mapSeries([
                        QuestionHasTag,
                        QuestionHasRegionalSetting,
                        QuestionHasRelatedQuestion,
                        PoolHasQuestion,
                        QuestionTranslation
                    ], function (model, serie) {
                        try {
                            return model.destroy({ where: { questionId: params.id } }).then(function (count) {
                                return serie();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, serie);
                            });
                        } catch (ex) {
                            return serie(ex);
                        }
                    }, function (err) {
                        return next(err);
                    });
                },
                function (next) {
                    var where = {};
                    where[Question.tableAttributes.id.field] = params.id;
                    if (_.has(params, Question.tableAttributes.status.field)) {
                        where[Question.tableAttributes.status.field] = params.status;
                    }
                    return Question.destroy({ where: where }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
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

    questionGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'question';
            var mapBySchema = 'questionModel';
            var sessionTenantId;
            var questionItem;
            async.series([
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
                // Populate question by tenant
                function (next) {
                    var include = CrudHelper.include(Question, [
                        QuestionTemplate
                    ]);
                    include = CrudHelper.includeWhere(include, QuestionTemplate, { tenantId: sessionTenantId }, true);
                    include = CrudHelper.includeNoAttributes(include, Question);
                    return Question.findOne({
                        where: { id: params.id },
                        include: include
                    }).then(function (question) {
                        if (!question) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        questionItem = question;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged tags by tenant
                function (next) {
                    var include = CrudHelper.include(QuestionHasTag, [
                        Tag
                    ]);
                    include = CrudHelper.includeWhere(include, Tag, { tenantId: sessionTenantId }, true);
                    return QuestionHasTag.findAndCountAll({
                        where: { questionId: params.id },
                        include: include
                    }).then(function (questionTags) {
                        questionItem.questionHasTags = questionTags;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged regional setting ids
                function (next) {
                    return QuestionHasRegionalSetting.findAndCountAll({
                        where: { questionId: params.id }
                    }).then(function (regionalSettings) {
                        questionItem.questionHasRegionalSettings = regionalSettings;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged regional setting ids
                function (next) {
                    return QuestionHasRelatedQuestion.findAndCountAll({
                        where: { relatedQuestionId: params.id },
                        limit: Config.rdbms.limit,
                        offset: 0
                    }).then(function (relatedQuestions) {
                        questionItem.questionHasRelatedQuestions = relatedQuestions;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged pool ids
                function (next) {
                    var where = { id: questionItem.id };
                    var include = CrudHelper.includeDefined(Question, [
                        PoolHasQuestion
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return Question.findOne({
                        where: where,
                        include: include,
                        limit: Config.rdbms.limit,
                        offset: 0,
                        distinct: true,
                        subQuery: false
                    }).then(function (question) {
                        if (!question) {
                            return next();
                        }
                        questionItem.poolHasQuestions = question.poolHasQuestions;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged question translation ids
                function (next) {
                    return QuestionTranslation.findAndCountAll({
                        where: { questionId: params.id }
                    }).then(function (questionTranslations) {
                        questionItem.questionTranslations = questionTranslations;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                response[responseItem] = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, questionItem);
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'questionModel';
            var orderBy = CrudHelper.orderBy(params, Question);
            var searchBy = CrudHelper.searchBy(params, Question);
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                searchBy = CrudHelper.searchByFullText(params, Question, mapBySchema, INSTANCE_MAPPINGS, clientSession);
            }
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'isAlreadyReviewedByUser')) {
                var customQuery = params.searchBy.isAlreadyReviewedByUser ? 'customQueryHasReview' : 'customQueryHasNotReview';
                searchBy = CrudHelper.extendSearchByCustom(params, Question, mapBySchema, INSTANCE_MAPPINGS, searchBy, customQuery, clientSession);
            }
            var include = CrudHelper.includeDefined(Question, [
                QuestionHasTag,
                QuestionHasRegionalSetting,
                QuestionHasRelatedQuestion,
                PoolHasQuestion,
                QuestionTemplate,
                QuestionTranslation,
            ], params, mapBySchema, INSTANCE_MAPPINGS);
            var attributes = CrudHelper.distinctAttributes(Question.tableAttributes.id);
            var questionItems = [];
            var total = 0;
            var sessionTenantId;
            async.series([
                // Get tenant id from session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        include = CrudHelper.includeWhere(include, QuestionTemplate, { tenantId: sessionTenantId }, true);
                        if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                            include = CrudHelper.includeNestedWhere(include, QuestionHasTag, Tag, { tenantId: sessionTenantId }, false);
                        }
                        include = CrudHelper.includeNoAttributes(include, Question);
                        return next();
                    });
                },
                // Populate total count
                function (next) {
                    return Question.count({
                        where: searchBy,
                        include: include,
                        distinct: true,
                        subQuery: false
                    }).then(function (count) {
                        total = count;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate question ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    return Question.findAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        attributes: attributes,
                        where: searchBy,
                        include: include,
                        order: orderBy,
                        subQuery: false,
                        raw: true
                    }).then(function (questions) {
                        if (!_.has(searchBy, 'id')) {
                            var questionsIds = [];
                            _.forEach(questions, function (question) {
                                questionsIds.push(question.id);
                            });
                            searchBy = {
                                id: { '$in': questionsIds }
                            };
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged question list without nested items
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return Question.findAll({
                        where: searchBy,
                        order: orderBy,
                        subQuery: false
                    }).then(function (questions) {
                        questionItems = questions;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate nested items for each question separately
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(questionItems, function (questionItem, nextQuestion) {
                        return async.series([
                            // Populate paged tags by tenant
                            function (nextItem) {
                                var include = CrudHelper.include(QuestionHasTag, [
                                    Tag
                                ]);
                                include = CrudHelper.includeWhere(include, Tag, { tenantId: sessionTenantId }, true);
                                var whereTags = [];
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'tags')) {
                                    whereTags.push({ '$in': params.searchBy.tags });
                                }
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                                    whereTags = CrudHelper.searchByTags(params, QuestionHasTag, QuestionHasTag.tableAttributes.tagTag.field);
                                }
                                var where = { questionId: questionItem.id };
                                if (whereTags.length > 0) {
                                    where = { '$and': [where, { '$or': whereTags }] };
                                }
                                return QuestionHasTag.findAndCountAll({
                                    where: where,
                                    include: include,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (questionTags) {
                                    questionItem.questionHasTags = questionTags;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged regional setting ids
                            function (nextItem) {
                                var where = { questionId: questionItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'regionalSettingsIds')) {
                                    where.regionalSettingId = { '$in': params.searchBy.regionalSettingsIds }
                                }
                                return QuestionHasRegionalSetting.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (regionalSettings) {
                                    questionItem.questionHasRegionalSettings = regionalSettings;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged related question ids
                            function (nextItem) {
                                var where = { relatedQuestionId: questionItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'relatedQuestionsIds')) {
                                    where.questionId = { '$in': params.searchBy.relatedQuestionsIds }
                                }
                                return QuestionHasRelatedQuestion.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (relatedQuestions) {
                                    questionItem.questionHasRelatedQuestions = relatedQuestions;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged pool ids
                            function (nextItem) {
                                var where = { id: questionItem.id };
                                var include = CrudHelper.includeDefined(Question, [
                                    PoolHasQuestion
                                ], params, mapBySchema, INSTANCE_MAPPINGS);
                                return Question.findOne({
                                    where: where,
                                    include: include,
                                    limit: Config.rdbms.limit,
                                    offset: 0,
                                    distinct: true,
                                    subQuery: false
                                }).then(function (question) {
                                    if (!question) {
                                        return nextItem();
                                    }
                                    questionItem.poolHasQuestions = question.poolHasQuestions;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged question translation ids
                            function (nextItem) {
                                var where = { questionId: questionItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'questionTranslationsIds')) {
                                    where.id = { '$in': params.searchBy.questionTranslationsIds }
                                }
                                return QuestionTranslation.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (questionTranslations) {
                                    questionItem.questionTranslations = questionTranslations;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                        ], nextQuestion);
                    }, next);
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var instanceMappings = AutoMapper.limitedMappings(INSTANCE_MAPPINGS);
                var questionItemsMapped = AutoMapper.mapListDefinedBySchema(mapBySchema, instanceMappings, questionItems);
                return CrudHelper.callbackSuccess({
                    items: questionItemsMapped,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionReject: function (params, message, clientSession, callback) {
        try {
            var updQuestion = { status: 'declined' };
            return Question.update(updQuestion, { where: { id: params.id }, individualHooks: true }).then(function (count) {
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

    questionApprove: function (params, message, clientSession, callback) {
        try {
            var updQuestion = { status: 'approved' };
            return Question.update(updQuestion, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                if (count[0] === 0) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                CrudHelper.callbackSuccess(null, callback);
                clientSession.getConnectionService().emitEvent('workorderClose', message, clientSession, params.id);
                return;
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    questionArchive: function (params, message, clientSession, callback) {
        try {
            var updQuestion = { status: 'archived' };
            return Question.update(updQuestion, { where: { id: params.id }, individualHooks: true }).then(function (count) {
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
    
    /**
     * Activate question and it's translations and do other stuff like by workflow (except payments) but without calling it explicitly with admin rights
     * @param params
     * @param message
     * @param clientSession
     * @param callback
     */
    questionActivateEntire: function (params, message, clientSession, callback) {
        var self = this;
        var workorderId;
        var questionStatus;
        var questionTranslationIds = [];
        var questionTranslationStatuses = [];
        return async.series([
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
            // Get first translation and current statuses of question and translation; optionally workorderId, payment structures
            function (next) {
                try {
                    return QuestionTranslation.findAll({
                        where: {
                            questionId: params.id
                        },
                        include: {
                            model: Question,
                            required: true,
                            include: {
                                model: Workorder,
                                required: false
                            }
                        }
                    }).then(function (questionTranslations) {
                        try {
                            if (questionTranslations.length === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            _.forEach(questionTranslations, function (questionTranslation) {
                                var questionTranslationItem = questionTranslation.get({ plain: true });
                                workorderId = questionTranslationItem.question.workorderId;
                                questionStatus = questionTranslationItem.question.status;
                                questionTranslationIds.push(questionTranslationItem.id);
                                questionTranslationStatuses.push(questionTranslationItem.status);
                            });
                            return next();
                        } catch (ex) {
                            return next(ex);
                        }
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // Publish all translations, update statuses
            function (next) {
                try {
                    params.questionTranslationIds = questionTranslationIds;
                    return QuestionTranslationApiFactory.questionTranslationBulkPublish(params, message, clientSession, next);
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // set isActivatedManually flag
            function (next) {
                try {
                    var update = { isActivatedManually: 1 };
                    var options = {
                        where: {
                            'id': {'$in': questionTranslationIds}
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
            // Add event logs for translations
            function (next) {
                try {
                    return async.forEachOf(questionTranslationIds, function (questionTranslationId, index, nextQuestionTranslationId) {
                        try {
                            return WorkflowCommon.addEventLog({
                                questionId: null,
                                questionTranslationId: questionTranslationId,
                                triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                                oldStatus: questionTranslationStatuses[index],
                                newStatus: QuestionTranslation.constants().STATUS_ACTIVE
                            }, nextQuestionTranslationId);
                        } catch (ex) {
                            return nextQuestionTranslationId(ex);
                        }
                    }, next);
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // Update question status
            function (next) {
                try {
                    var updQuestion = { status: Question.constants().STATUS_ACTIVE };
                    return Question.update(updQuestion, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // set isActivatedManually flag
            function (next) {
                try {
                    var updQuestion = { isActivatedManually: 1 };
                    return Question.update(updQuestion, { where: { id: params.id }, individualHooks: false }).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // Add event log for question
            function (next) {
                try {
                    return WorkflowCommon.addEventLog({
                        questionId: params.id,
                        questionTranslationId: null,
                        triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                        oldStatus: questionStatus,
                        newStatus: Question.constants().STATUS_ACTIVE
                    }, next);
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            },
            // When all questions of workorder are approved, close workorder
            function (next) {
                try {
                    if (!workorderId) {
                        return setImmediate(next);
                    }
                    return Question.count({
                        where: {
                            workorderId: workorderId,
                            status: { '$ne': Question.constants().STATUS_APPROVED }
                        }
                    }).then(function (notApprovedCount) {
                        try {
                            if (notApprovedCount > 0) {
                                return next();
                            }
                            return Workorder.update({ status: Workorder.constants().STATUS_CLOSED }, { where: { id: workorderId } }).then(function (count) {
                                if (count[0] === 0) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        } catch (e) {
                            return next(e);
                        }
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
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
    },

    questionDeactivateManually: function (params, message, clientSession, callback) {
        var self = this;
        var questionStatus;
        var questionTranslationIds = [];
        var questionTranslationStatuses = [];
        return async.series([
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
            // set status inactive
            function (next) {
                try {
                    self.questionSetDeploymentStatus({
                        status: Question.constants().STATUS_INACTIVE,
                        id: params.id
                    }, function (err) {
                        return next(err);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // load question translations
            function (next) {
                try {
                    return QuestionTranslation.findAll({
                        where: {
                            questionId: params.id,
                            isActivatedManually: 1,
                            status: QuestionTranslation.constants().STATUS_ACTIVE
                        }
                    }).then(function (questionTranslations) {
                        try {
                            _.forEach(questionTranslations, function (questionTranslation) {
                                var questionTranslationItem = questionTranslation.get({ plain: true });
                                questionTranslationIds.push(questionTranslationItem.id);
                                questionTranslationStatuses.push(questionTranslationItem.status);
                            });
                            return next();
                        } catch (ex) {
                            return next(ex);
                        }
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // unpublish all question translations
            function (next) {
                try {
                    if(!questionTranslationIds.length) {
                        return next();
                    }
                    return QuestionTranslationApiFactory.questionTranslationBulkUnpublish({questionTranslationIds: questionTranslationIds}, message, clientSession, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // unset isActivatedManually flag for question
            function (next) {
                try {
                    var update = { isActivatedManually: 0 };
                    var options = {
                        where: {
                            'id': params.id
                        },
                        individualHooks: false
                    };
                    return Question.update(update, options).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                    
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // unset isActivatedManually flag for translations
            function (next) {
                try {
                    var update = { isActivatedManually: 0 };
                    var options = {
                        where: {
                            'id': {'$in': questionTranslationIds}
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
            // Add event logs for translations
            function (next) {
                try {
                    return async.forEachOf(questionTranslationIds, function (questionTranslationId, index, nextQuestionTranslationId) {
                        try {
                            return WorkflowCommon.addEventLog({
                                questionId: null,
                                questionTranslationId: questionTranslationId,
                                triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                                oldStatus: QuestionTranslation.constants().STATUS_ACTIVE,
                                newStatus: QuestionTranslation.constants().STATUS_INACTIVE
                            }, nextQuestionTranslationId);
                        } catch (e) {
                            return nextQuestionTranslationId(e);
                        }
                    }, next);
                } catch (e) {
                    return setImmediate(next, e);
                }
            },
            // Add event log for question
            function (next) {
                try {
                    return WorkflowCommon.addEventLog({
                        questionId: params.id,
                        questionTranslationId: null,
                        triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                        oldStatus: Question.constants().STATUS_ACTIVE,
                        newStatus: Question.constants().STATUS_INACTIVE
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
    },
    questionAddRemoveTags: function (params, callback) {
        return CrudHelper.callbackError(Errors.QuestionApi.FunctionNotImplemented, callback);
    },

    questionAddRemoveKeywords: function (params, callback) {
        return CrudHelper.callbackError(Errors.QuestionApi.FunctionNotImplemented, callback);
    },

    questionCheckNotUsed: function (params, callback) {
        try {
            var usedCount = 0;
            async.mapSeries([
                { model: WorkorderHasQuestionExample, field: WorkorderHasQuestionExample.tableAttributes.questionId.field },
            ], function (serie, next) {
                try {
                    var where = {};
                    where[serie.field] = params.id;
                    return serie.model.count({ where: where }).then(function (count) {
                        usedCount += count;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    return next(ex);
                }
            }, function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(usedCount === 0, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionSetDeploymentStatus: function (params, callback) {
        try {
            return Question.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
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

    tagList: function (params, message, clientSession, callback) {
        try {
            var orderBy = CrudHelper.orderBy(params, Tag);
            var searchBy = CrudHelper.searchBy(params, Tag);
            
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                searchBy.tenantId = tenantId;
                return Tag.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy,
                }).then(function (tags) {
                    var tagItems = AutoMapper.mapListDefinedBySchema('tagModel', INSTANCE_MAPPINGS, tags.rows);
                    return CrudHelper.callbackSuccess({
                        items: tagItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: tags.count,
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
            
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tagDelete: function (params, callback) {
        var self = this;
        try {
            async.series([
                function (next) {
                    async.mapSeries([
                        QuestionHasTag,
                        PoolHasTag,
                        MediaHasTag
                    ], function (model, serie) {
                        try {
                            model.destroy({ where: { tagTag: params.tag } }).then(function (count) {
                                return serie();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, serie);
                            });
                        } catch (ex) {
                            return serie(ex);
                        }
                    }, function (err) {
                        return next(err);
                    });
                },
                function (next) {
                    Tag.destroy({ where: { tag: params.tag } }).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
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

    tagReplace: function (params, callback) {
        var self = this;
        try {
            async.series([
                function (next) {
                    Tag.update({tag: params.newTag}, { where: { tag: params.oldTag } }).then(function (count) {
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
    questionIssueResolve: function (params, message, clientSession, callback) {
        try {
            var self = this;
            async.series([
                // update fields
                function (next) {
                    var set = {
                        isEscalated: 0,
                        status: Question.constants().STATUS_DRAFT
                    };
                    Question.update(set, { where: { id: params.id }, individualHooks: false }).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // add to workflow again
                function (next) {
                    try {
                        QuestionWorkflow.getInstance(params.id).questionCommitForReview(message, clientSession, next);
                    } catch (e) {
                        return setImmediate(next, e);
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
    questionTranslationIssueResolve: function (params, message, clientSession, callback) {
        try {
            var self = this;
            async.series([
                // update fields
                function (next) {
                    var set = {
                        isEscalated: 0,
                        status: QuestionTranslation.constants().STATUS_DRAFT
                    };
                    QuestionTranslation.update(set, { where: { id: params.id }, individualHooks: false }).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // add to workflow again
                function (next) {
                    try {
                        QuestionTranslationWorkflow.getInstance(params.id).questionTranslationCommitForReview(message, clientSession, next);
                    } catch (e) {
                        return setImmediate(next, e);
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
    questionReportIssue: function (params, clientInfo, callback) {
        
        try {
            var self = this;
            var translation = null;
            var isQuestionIssue = (params.issueType !== QuestionOrTranslationIssue.constants().ISSUE_TYPE_SPELLING_AND_GRAMMAR);
            var version = null;
            async.series([
                // find translation by blob key
                function (next) {
                    try {
                        self._questionIssueGetTranslationByBlobKey(params.blobKey, function (err, trans) {
                            if(err) {
                                return next(err);
                            }
                            translation = trans;
                            return next();
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // load question/translation version
                function (next) {
                    try {
                        var method = (isQuestionIssue ? 'getQuestionPublishedVersion' : 'getQuestionTranslationPublishedVersion');
                        var id = (isQuestionIssue ? translation.questionId : translation.id);
                        
                        WorkflowCommon[method](id, function (err, ver) {
                            if(err) {
                                return next(err);
                            }
                            version = ver;
                            return next(err);
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },

                // add issue and event log
                function (next) {
                    try {
                        var dataIssue = {
                            issueFor: isQuestionIssue ? QuestionOrTranslationIssue.constants().ISSUE_FOR_QUESTION : QuestionOrTranslationIssue.constants().ISSUE_FOR_TRANSLATION,
                            questionId: isQuestionIssue ? translation.questionId : null,
                            questionTranslationId: isQuestionIssue ? null : translation.id,
                            version: version,
                            resourceId: clientInfo.getUserId(),
                            issueType: params.issueType,
                            issueText: params.issueText || null
                        };
                        
                        var dataEvent = {
                            questionId: isQuestionIssue ? translation.questionId : null,
                            questionTranslationId: isQuestionIssue ? null : translation.id,
                            resourceId: clientInfo.getUserId(),
                            triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER
                        };
                        
                        
                        WorkflowCommon.addIssueAndEventLog(dataIssue, dataEvent, next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // escalate if needed
                function (next) {
                    try {
                        self._questionIssueEscalateQuestion({
                            isQuestionIssue: isQuestionIssue,
                            questionId: translation.questionId,
                            questionTranslationId: translation.id,
                            version: version
                        }, next);
                    } catch (e) {
                        return setImmediate(next, e);
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
    
    _questionIssueGetTranslationByBlobKey: function (blobKey, cb) {
        try {
            QuestionTranslation.findOne({
                where: {
                    questionBlobKeys: {
                        '$like': '%"' + blobKey + '"%'
                    }
                }
            }).then(function (dbItem) {
                try {
                    if(!dbItem) {
                        return cb(Errors.DatabaseApi.NoRecordFound);
                    }
                    return cb(false, dbItem.get({plain: true}));
                } catch (e) {
                    return cb(e);
                }
            }).catch(cb);
            
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    _questionIssueNeedEscalate: function (args, cb) {
        try {
            var isQuestionIssue = args.isQuestionIssue;
            var questionId = args.questionId;
            var questionTranslationId = args.questionTranslationId;
            var version = args.version;
            
            var issues = [];
            var needEscalate = false;
            var issueThreshold = {};
            if(isQuestionIssue) {
                issueThreshold[QuestionOrTranslationIssue.constants().ISSUE_TYPE_NOT_CLEAR] = 3;
                issueThreshold[QuestionOrTranslationIssue.constants().ISSUE_TYPE_WRONG_ANSWER] = 1;
                issueThreshold[QuestionOrTranslationIssue.constants().ISSUE_TYPE_WRONG_COMPLEXITY] = 3;
                issueThreshold[QuestionOrTranslationIssue.constants().ISSUE_TYPE_WRONG_CATEGORY] = 5;
                issueThreshold[QuestionOrTranslationIssue.constants().ISSUE_TYPE_OFFENSIVE_CONTENT] = 2;
                issueThreshold[QuestionOrTranslationIssue.constants().ISSUE_TYPE_OTHER] = 10;
            } else {
                issueThreshold[QuestionOrTranslationIssue.constants().ISSUE_TYPE_SPELLING_AND_GRAMMAR] = 5;
            }
            
            async.series([
                // load issues
                function (next) {
                    try {
                        var where = {
                            deleted: 0,
                            version: version
                        };
                        if (isQuestionIssue) {
                            where.questionId = questionId;
                        } else {
                            where.questionTranslationId = questionTranslationId;
                        }
                        
                        QuestionOrTranslationIssue.findAll({where: where}).then(function (dbItems) {
                            try {
                                for(var i=0; i<dbItems.length; i++) {
                                    issues.push(dbItems[i].get({plain: true}));
                                }
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // check if need to be escalated
                function (next) {
                    try {
                        var issueCount = {};
                        for(var i=0; i<issues.length; i++) {
                            var item = issues[i];
                            if (_.isUndefined(issueCount[item.issueType])) {
                                issueCount[item.issueType] = 0;
                            }
                            
                            issueCount[item.issueType]++;
                        }
                        
                        for(var issueType in issueCount) {
                            if(issueCount[issueType] >= issueThreshold[issueType]) {
                                needEscalate = true;
                                break;
                            }
                        }
                        return setImmediate(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], function (err) {
                return cb(err, needEscalate);
            });
        } catch (e) {
            return setImmediate(cb, e);
        }
        
    },
    
    _questionIssueEscalateQuestion: function (args, cb) {
        try {
            var self = this;
            var isQuestionIssue = args.isQuestionIssue;
            var questionId = args.questionId;
            var questionTranslationId = args.questionTranslationId;
            var version = args.version;
            
            var needEscalate = false;
            async.series([
                function (next) {
                    try {
                        self._questionIssueNeedEscalate(args, function (err, res) {
                            if(err) {
                                return next(err);
                            }
                            needEscalate = res;
                            
                            return next();
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // do escalation
                function (next) {
                    try {
                        //console.log('\n\n\n\nhere:', needEscalate);
                        if(!needEscalate) {
                            return setImmediate(next);
                        }
                        
                        var Model = null;
                        var where = null;
                        if(isQuestionIssue) {
                            Model = Question;
                            where = {
                                id: questionId
                            };
                        } else {
                            Model = QuestionTranslation;
                            where = {
                                id: questionTranslationId
                            };
                        }
                        Model.update({isEscalated: 1}, {where: where, individualHooks: false}).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(next);
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    },

    questionMassUploadStatus: function (params, message, clientSession, callback) {
        logger.debug('params: ', params);
        const stack = params.stack;
        if (stacks[stack]){
            CrudHelper.callbackSuccess(stacks[stack], callback);
        } else {
            CrudHelper.callbackSuccess(null, callback);
        }
    },

    questionMassUpload: function (params, message, clientSession, callback) {
        logger.debug('params: ', params);
        let questions;
        let regionList = [];
        let langList = [];
        let templateName;
        try {

            const questionsStack = Config.httpUpload.tempFolder  + '/' + params.stack;
            if (!fs.existsSync(questionsStack)){
                return CrudHelper.callbackError('NO_STACK_AVALABLE', callback);
            }
            const poolsIds = params.poolsIds;
            const templateId = params.template;

            async.series([
                //read excel file
                function (next) {
                    try {
                        xlsxj({
                            input: questionsStack,
                            output: 'output.json'
                        }, function (err, result) {
                            if (err) {
                                return next('ERR_FILE_CONVERTING');
                            } else {
                                questions = result.filter(item => item["Language"]);
                                return next();
                            }
                        });
                    } catch (e) {
                        logger.error('ERR_FILE_CONVERT', e);
                        return next('ERR_FILE_CONVERT');
                    }
                },
                //upload questions
                function (next) {
                    QuestionTemplateApiFactory.questionTemplateGet({id : templateId}, message, clientSession, function (err, data) {
                        if (err) {
                            logger.error("template getting error:", err);
                            return next('ERR_TEMPLATE_EXCEPTION')
                        }

                        templateName=data.questionTemplate.name;
                        return next();
                    });
                },
                //read regionalSettingList
                function (next) {
                    RegionalSettingApiFactory.regionalSettingList({
                        limit: 0,
                        offset: 0
                    }, message, clientSession, false, function (err, data) {
                        if (err) {
                            logger.error("regionalSettingList getting error:", err);
                            return next('ERR_REGIONAL_LIST_EXCEPTION')
                        }
                        data.items.forEach(function (item) {
                            regionList[item.iso]=item.id;
                        });
                        return next();
                    });

                },
                //read languageList
                function (next) {
                    RegionalSettingApiFactory.languageList({
                        limit: 0,
                        offset: 0
                    }, message, clientSession, false, function (err, data) {
                        if (err) {
                            logger.error("languageListSettingList getting error:", err);
                            return next('ERR_LANGUAGE_LIST_EXCEPTION')
                        }
                        data.items.forEach(function (item) {
                            langList[item.iso]=item.id;
                        });
                        return next();
                    });

                },
                //check questions
                function (next) {
                    let errors = "";
                    if (questions, questions.length < 1) {
                        next('ERR_NO_QUESTIONS');
                    }
                    stackInc(params.stack,questions.length);
                    questions.forEach(function (question) {

                        if (question['INT/NAT'] !== "yes" && question['INT/NAT'] !== "no"
                            && question['INT / NAT'] !== "yes" && question['INT / NAT'] !== "no") {
                            errors += '\nInvalid INT/NAT, must be "yes" or "no"';
                        }

                        if (langList[question["Language"].toLowerCase()]) {
                            question.language = langList[question["Language"].toLowerCase()];
                        } else {
                            errors += '\nInvalid Language ' + question["Language"] + ' is not by ISO';
                        }
                        logger.debug("language=",question.language);

                        const regions = question["Region"].split(";")
                        regions.forEach(function (region) {
                            if (regionList[region.toUpperCase()]) {
                                if (!question.regions)
                                    question.regions=[];
                                question.regions.push(+regionList[region.toUpperCase()]);
                            } else {
                                errors += '\nInvalid region ' + region + ' is not by ISO';
                            }
                        });
                        logger.debug("regions=",question.regions);

                        const images = [];
                        if (question['Correct Pic']) {
                            images.push(question['Correct Pic']);
                        }
                        if (question['Wrong Pic 1']) {
                            images.push(question['Wrong Pic 1']);
                        }
                        if (question['Wrong Pic 2']) {
                            images.push(question['Wrong Pic 2']);
                        }
                        if (question['Wrong Pic 3']) {
                            images.push(question['Wrong Pic 3']);
                        }
                        if (question['Correct picture 1']) {
                            images.push(question['Correct picture 1']);
                        }
                        if (question['Correct picture 2']) {
                            images.push(question['Correct picture 2']);
                        }
                        if (question['Correct picture 3']) {
                            images.push(question['Correct picture 3']);
                        }
                        if (question['Correct picture 4']) {
                            images.push(question['Correct picture 4']);
                        }
                        if (question['Question Pic']) {
                            images.push(question['Question Pic']);
                        }
                        if (question['Resolution Pic']) {
                            images.push(question['Resolution Pic']);
                        }
                        if (images.length > 0) {
                            images.forEach(function (image) {
                                const filePath = imagesFolder + '/' + getImageFileName(image);
                                if (!filePath) {
                                    errors += '\n' + image + ' is not exist';
                                }
                            });
                        }
                    });
                    next(errors);
                },
                function (next) {
                    let answer;
                    logger.error('Start ', templateName);
                    switch (true) {
                        case /Text Question/.test(templateName):
                            answer=importSimple(questions, "ANSWER_TEXT",_.uniq(poolsIds),templateId,params.stack,message, clientSession,params.token);
                            break;
                        case /4Pic Question/.test(templateName):
                            answer=importPictures(questions, "ANSWER_IMAGE",_.uniq(poolsIds),templateId,params.stack,message, clientSession,params.token);
                            break;
                        case /2 step/.test(templateName):
                            answer=import2StepPictures(questions, "2_STEP",_.uniq(poolsIds),templateId,params.stack,message, clientSession,params.token);
                            break;
                        case /Text sorting/.test(templateName):
                            answer=importSimple(questions, "ANSWER_TEXT_SORT",_.uniq(poolsIds),templateId,params.stack,message, clientSession,params.token);
                            break;
                        case /4Pic Sorting/.test(templateName):
                            answer=importPictures(questions, "ANSWER_IMAGE_SORT",_.uniq(poolsIds),templateId,params.stack,message, clientSession,params.token);
                            break;
                        default:
                            logger.error('Undefined mass question load template: ' + templateName);
                            return next('ERR_UNDEFINED_TEMPLATE');
                    }
                    answer
                        .then(res => {
                            logger.error('End ', res);
                            return next();
                        })
                        .catch(error => {
                            return next(error);
                        });
                }
            ], function (err) {
                if (stacks[params.stack])
                    stacks[params.stack].status = 'done';
                if (err) {
                    logger.error('httpQuestionApi.loadQuestions global failure', err);
                    stackIncError(params.stack, err);
                    if (questionsStack && fs.existsSync(questionsStack)) {
                        fs.unlinkSync(questionsStack)
                    }
                    return CrudHelper.callbackError(ex, callback);
                }
                CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    }
};

module.exports = questionApiExports;

function stackInc(stack,total){
    if (stacks[stack]){
        stacks[stack].done++;
    } else {
        stacks[stack]=
            {
                total:total,
                done: 0,
                status: "progress",
                errors: []
            }
    }
}

function stackIncError(stack,error){
    if (stacks[stack]){
        stacks[stack].errors.push(error);
    }
}

function getImageFileName(filename) {
    const nameArray = filename.split(".");
    const hasExt = (nameArray[nameArray.length - 1] === "jpg") || (nameArray[nameArray.length - 1] === "png");

    let path = `${imagesFolder}/${filename}${hasExt ? "" : ".jpg"}`;
    let finalPath;
    if (fs.existsSync(path)) {
        finalPath = `${filename}${hasExt ? "" : ".jpg"}`;
    } else {
        if (!hasExt) {
            path = `${imagesFolder}/${filename}${".png"}`;
            if (fs.existsSync(path)) {
                finalPath = `${filename}${".png"}`;
            }
        }
    }
    return finalPath;
}



function importPictures(questions,type,poolsIds,templateId,stack,message, clientSession, token) {
    let promise = new Promise(resolve => resolve());
    for (const el of questions) {
        promise = promise.then(function (element) {
            logger.debug('start createPictureQuestion')
            stackInc(stack,questions.length);
            return createQuestion({
                poolsIds: poolsIds,
                regions: element.regions,
                complexity: +element.Difficulty,
                source: element.Source,
                accessibleDate: element["Valid from"],
                expirationDate: element["Valid to"],
                renewDate: element["Renew date"],
                rating: element.Rating,
                isInternational: element["INT/NAT"] ? element["INT/NAT"] : element["INT / NAT"],
                tags: [element.Tags],
                hint: element.Hint,
                explanation: element.Explanation,
                countdown: element.Countdown,
                translations: [{
                    language: element.language,
                    question: element.Question,
                    correctPic: element["Correct Pic"] ? element["Correct Pic"] : element["Correct picture 1"],
                    wrongPic1: element["Wrong Pic 1"] ? element["Wrong Pic 1"] : element["Correct picture 2"],
                    wrongPic2: element["Wrong Pic 2"] ? element["Wrong Pic 2"] : element["Correct picture 3"],
                    wrongPic3: element["Wrong Pic 3"] ? element["Wrong Pic 3"] : element["Correct picture 4"],
                }],
                typeOfQuestion: type
            },templateId,stack,message, clientSession, token);
        }.bind(null, el))
            .catch((err) => {
                stackIncError(stack,err);
                logger.error('importPictures error:',err);
            })
            .then((res) => {
                logger.debug('importPictures list:',res.data)
                return new Promise((resolve) => setTimeout(resolve, 200));
            });
    }
    return promise;
}

function importSimple(questions,type,poolsIds,templateId,stack,message, clientSession, token) {
    let promise = new Promise(resolve => resolve());
    for (const el of questions) {
        promise = promise.then(function (element) {
            logger.debug('start createSimpleQuestion')
            stackInc(stack,questions.length);
            return createQuestion({
                poolsIds: poolsIds,
                regions: element.regions,
                complexity: +element.Difficulty,
                source: element.Source,
                accessibleDate: element["Valid from"],
                expirationDate: element["Valid to"],
                renewDate: element["Renew date"],
                rating: element.Rating,
                isInternational: element["INT/NAT"] ? element["INT/NAT"] : element["INT / NAT"],
                tags: [element.Tags],
                hint: element.Hint,
                explanation: element.Explanation,
                countdown: element.Countdown,
                translations: [{
                    language: element.language,
                    question: element.Question,
                    correctAnswer: element["Correct answer"] ? element["Correct answer"] : element["Correct answer 1"],
                    wrongAnswer1: element["Wrong answer 1"] ? element["Wrong answer 1"] : element["Correct answer 2"],
                    wrongAnswer2: element["Wrong answer 2"] ? element["Wrong answer 2"] : element["Correct answer 3"],
                    wrongAnswer3: element["Wrong answer 3"] ? element["Wrong answer 3"] : element["Correct answer 4"],
                }],
                typeOfQuestion: type
            },templateId,stack,message, clientSession, token);
        }.bind(null, el))
            .catch((err) => {
                stackIncError(stack,err);
                logger.error('importPictures error:',err);
            })
            .then((res) => {
                logger.debug('importPictures list:',res.data)
                return new Promise((resolve) => setTimeout(resolve, 200));
            });
    }
    return promise;
}

function import2StepPictures(questions,type,poolsIds,templateId,stack,message, clientSession, token) {
    let promise = new Promise(resolve => resolve());
    for (const el of questions) {
        promise = promise.then(function (element) {
            logger.debug('start create2StepPictures')
            stackInc(stack,questions.length);
            return createQuestion({
                poolsIds: poolsIds,
                regions: element.regions,
                complexity: +element.Difficulty,
                source: element.Source,
                accessibleDate: element["Valid from"],
                expirationDate: element["Valid to"],
                renewDate: element["Renew date"],
                rating: element.Rating,
                isInternational: element["INT/NAT"] ? element["INT/NAT"] : element["INT / NAT"],
                tags: [element.Tags],
                hint: element.Hint,
                explanation: element.Explanation,
                countdown: element.Countdown,
                countdownImage: element["Pic-Time"],
                translations: [{
                    language: element.language,
                    question: element.Question,
                    correctAnswer: element["Correct answer"] ? element["Correct answer"] : element["Correct answer 1"],
                    wrongAnswer1: element["Wrong answer 1"] ? element["Wrong answer 1"] : element["Correct answer 2"],
                    wrongAnswer2: element["Wrong answer 2"] ? element["Wrong answer 2"] : element["Correct answer 3"],
                    wrongAnswer3: element["Wrong answer 3"] ? element["Wrong answer 3"] : element["Correct answer 4"],
                    questionPic: element["Question Pic"],
                    questionPicTxt: element["Question Pic-Txt"],
                    resolutionPic: element["Resolution Pic"],
                    resolutionPicTxt: element["Resolution Pic-Txt"],
                }],
                typeOfQuestion: type
            },templateId,stack,message, clientSession, token);
        }.bind(null, el))
            .catch((err) => {
                stackIncError(stack,err);
                logger.error('import2StepPictures error:',err);
            })
            .then((res) => {
                logger.debug('import2StepPictures list:',res.data)
                return new Promise((resolve) => setTimeout(resolve, 200));
            });
    }
    return promise;
}
function createQuestion({
                            poolsIds,
                            regions,
                            complexity,
                            source,
                            accessibleDate,
                            expirationDate,
                            renewDate,
                            rating,
                            isInternational,
                            tags,
                            hint,
                            explanation,
                            countdown,
                            countdownImage,
                            translations,
                            typeOfQuestion
                        },
                        templateId,
                        stack,
                        message,
                        clientSession,
                        token) {
    let resultQuestion;
    let questionActivateEntireRes;
    let resolutionImageId;
    return new Promise((resolve, reject) => {
        async.series([
            function (next) {
                if (!/2_STEP/.test(typeOfQuestion)) {
                    return next()
                }
                if (translations[0].resolutionPic) {
                    const imageFileName = getImageFileName(translations[0].resolutionPic);
                    addMedia(imageFileName, token, 'question', 'image')
                        .then(res => {
                            logger.debug('createPictureQuestion resolutionImage result=', res);
                            resolutionImageId=res.content.media.id;
                            return next();
                        })
                        .catch(error => {
                            logger.error("createPictureQuestion resolutionImage error:", error);
                            stackIncError(stack,error);
                            return next("ERR_LOAD_MEDIA");
                        });
                } else {
                    stackIncError(stack,"EMRTY RESOLUTION IMAGE");
                    return next("ERR_EMRTY_RESOLUTION_IMAGE");
                }
            },
            function (next) {
                const resolutionImage={resolutionImageId: resolutionImageId};
                const question = {
                    questionTemplateId: templateId,
                    poolsIds: poolsIds,
                    regionalSettingsIds: regions,
                    primaryRegionalSettingId: regions[0],
                    complexity,
                    source,
                    accessibleDate,
                    expirationDate,
                    renewDate,
                    rating,
                    isInternational: isInternational == 'yes' ? 1 : 0,
                    tags,
                    explanation,
                    hint
                };
                if (/2_STEP/.test(typeOfQuestion)) {
                    Object.assign(question,resolutionImage);
                }
                if (question.accessibleDate === '') {
                    delete question.accessibleDate;
                }
                if (question.expirationDate === '') {
                    delete question.expirationDate;
                }
                if (question.renewDate === '') {
                    delete question.renewDate;
                }
                logger.debug("Create question :", question);

                questionApiExports.questionUpdate(question, message, clientSession, function (err, data) {
                    if (err) {
                        stackIncError(stack,"Create question error:" + err);
                        logger.error("Create question error:", err);
                        return next(err)
                    }
                    resultQuestion = data.question;
                    logger.debug('resultQuestion=', resultQuestion);
                    return next();
                });
            },
            function (next) {

                for (const translation of translations) {
                    let picsIds = [];
                    let imageCount = 0;
                    async.series([
                        function (next2) {

                        if (!/ANSWER_IMAGE/.test(typeOfQuestion) && !/2_STEP/.test(typeOfQuestion)) {
                            return next2()
                        }

                            function fillImageArray(fileName, title, namespace, array, minCount) {
                                if (fileName) {
                                    const imageFileName = getImageFileName(fileName);
                                    addMedia(imageFileName, token, namespace, title)
                                        .then(res => {
                                            logger.debug('createPictureQuestion addMedia result=', res);
                                            array.push(res.content.media.id);
                                            if (++imageCount === minCount) {
                                                logger.debug('createPictureQuestion addMedia return2')
                                                return next2();
                                            }
                                        })
                                        .catch(error => {
                                            logger.error("createPictureQuestion addMedia error:", error);
                                            stackIncError(stack,"createPictureQuestion error:" + error);
                                            return next2("ERR_LOAD_MEDIA");
                                        });
                                } else {
                                    stackIncError(stack,"createPictureQuestion error: empty image");
                                    return next2("ERR_EMRTY_IMAGE");
                                }
                            }

                            if (/ANSWER_IMAGE/.test(typeOfQuestion)) {
                                fillImageArray(translation.correctPic, 'correct', 'answer', picsIds, 4);
                                fillImageArray(translation.wrongPic1, 'wrong', 'answer', picsIds, 4);
                                fillImageArray(translation.wrongPic2, 'wrong', 'answer', picsIds, 4);
                                fillImageArray(translation.wrongPic3, 'wrong', 'answer', picsIds, 4);
                            }

                            if (/2_STEP/.test(typeOfQuestion)) {
                                if (translation.questionPic) {
                                    fillImageArray(translation.questionPic, 'image', 'question', picsIds, 1);
                                } else {
                                    stackIncError(stack,"createPictureQuestion error: empty question image");
                                    return next("ERR_EMRTY_QUESTION_IMAGE");
                                }
                            }

                            },
                        function (next2) {
                            if (!/ANSWER_IMAGE/.test(typeOfQuestion)) {
                                return next2()
                            }

                            if (!picsIds.length)
                                return next2("ERR_NO_IMAGES");
                            logger.debug('createPictureQuestion poolsIds[0]=',poolsIds[0], ' picsIds=',picsIds);

                            PoolApiFactory.poolAddDeleteMedia({
                                id: poolsIds[0],
                                addMedias: picsIds
                            }, clientSession, function (err, data) {
                                if (err) {
                                    logger.error("poolAddDeleteMedia error:", err);
                                    stackIncError(stack,"Add Media in pool error: "+err);
                                    return next2(err)
                                }
                                return next2();
                            });

                        },
                        function (next2) {
                            logger.debug('createQuestionTranslate resultQuestion.id=',resultQuestion.id)
                            let request = {
                                questionId: resultQuestion.id,
                                hint,
                                explanation,
                                countdown,
                                language: translation.language,
                                question: translation.question,
                                typeOfQuestion: typeOfQuestion
                            };
                            let answers;
                            if (/ANSWER_IMAGE/.test(typeOfQuestion)) {
                                answers={
                                    correctPic: picsIds[0],
                                    wrongPic1: picsIds[1],
                                    wrongPic2: picsIds[2],
                                    wrongPic3: picsIds[3]
                                };
                            } else if (/ANSWER_TEXT/.test(typeOfQuestion) || /2_STEP/.test(typeOfQuestion)){
                                answers={
                                    correctAnswer: translation.correctAnswer,
                                    wrongAnswer1: translation.wrongAnswer1,
                                    wrongAnswer2: translation.wrongAnswer2,
                                    wrongAnswer3: translation.wrongAnswer3
                                };
                            }
                            if (/2_STEP/.test(typeOfQuestion)) {
                                const twoStep = {
                                    countdownImage,
                                    questionPicTxt: translation.questionPicTxt,
                                    questionPic: picsIds[0],
                                    resolutionText: translation.resolutionPicTxt
                                }
                                Object.assign(request, twoStep);
                            }
                            Object.assign(request, answers);
                            createQuestionTranslate(request, message, clientSession, function (err, data) {
                                logger.debug('createQuestionTranslate data=', data)
                                if (err) {
                                    logger.error("questionTranslationUpdate error:", err);
                                    stackIncError(stack,"create Question Translate error:"+err);
                                    return next2(err);
                                }
                                return next2();
                            });
                        }
                    ], function (err) {
                        if (err) {
                            logger.error('next2 error:', err);
                            stackIncError(stack," error:"+err);
                            return next(err);
                        }
                        return next();
                    });
                }
            },
            function (next) {
                logger.debug('questionActivateEntire start resultQuestion.id=',resultQuestion.id)
                questionApiExports.questionActivateEntire(
                    {id: resultQuestion.id}, message, clientSession, function (err, data) {
                        logger.debug('questionActivateEntire end data=',data)

                        if (err) {
                            logger.error("questionActivateEntire error:", err);
                            stackIncError(stack,"Activate question error:"+err);
                            return next(err);
                        }
                        questionActivateEntireRes=data;
                        return next();
                    });

            }
        ], function (err) {
            if (err) {
                stackIncError(stack," error:"+err);
                logger.error('createPictureQuestion error:', err);
                reject(err);
            }
            resolve({data: questionActivateEntireRes});
        });
    })
}

function createQuestionTranslate({
                                     language,
                                     hint,
                                     explanation,
                                     countdown,
                                     question,
                                     correctAnswer,
                                     wrongAnswer1,
                                     wrongAnswer2,
                                     wrongAnswer3,
                                     correctPic,
                                     wrongPic1,
                                     wrongPic2,
                                     wrongPic3,
                                     questionId,
                                     typeOfQuestion,
                                     countdownImage,
                                     questionPicTxt,
                                     questionPic,
                                     resolutionText,
                                 }, message, clientSession, callback) {

    const mappings = [{
        "id": "QUESTION",
        "settings": {
            "timer": true,
            "close": true,
            "pager": true,
            "sound": true,
            "vibration": true
        },
        "value": question
    }];
    let medias = [];
    let mappings2step = [];
    logger.debug("createQuestionTranslate typeOfQuestion=",typeOfQuestion);

    switch (typeOfQuestion) {
        case 'ANSWER_TEXT_SORT':
            mappings.push({
                "id": "ANSWER_TEXT_SORT",
                "handler": "SortingAnswerHandler",
                "settings": {},
                "answers": [
                    correctAnswer,
                    wrongAnswer1,
                    wrongAnswer2,
                    wrongAnswer3,
                ]
            });
            break;
        case 'ANSWER_TEXT':
            mappings.push({
                "id": "ANSWER_TEXT",
                "handler": "SingleChoiceAnswerHandler",
                "settings": {},
                "answers": [
                    correctAnswer,
                    wrongAnswer1,
                    wrongAnswer2,
                    wrongAnswer3,
                ]
            });
            break;
        case 'ANSWER_IMAGE':
            logger.debug("createQuestionTranslate typeOfQuestion is ANSWER_IMAGE");
            const answers = [];
            if (correctPic != '') {
                answers.push(correctPic)
            }
            if (wrongPic1 != '') {
                answers.push(wrongPic1)
            }
            if (wrongPic2 != '' && wrongPic2 != null) {
                answers.push(wrongPic2)
            }
            if (wrongPic3 != '' && wrongPic3 != null) {
                answers.push(wrongPic3)
            }
            mappings.push({
                "id": "ANSWER_IMAGE",
                "handler": "SingleChoiceAnswerHandler",
                "settings": {},
                answers
            });
            medias = answers;
            logger.debug("createQuestionTranslate medias=",medias);
            break;
        case 'ANSWER_IMAGE_SORT':
            const answersSort = [];
            if (correctPic != '') {
                answersSort.push(correctPic)
            }
            if (wrongPic1 != '') {
                answersSort.push(wrongPic1)
            }
            if (wrongPic2 != '') {
                answersSort.push(wrongPic2)
            }
            if (wrongPic3 != '') {
                answersSort.push(wrongPic3)
            }
            mappings.push({
                "id": "ANSWER_IMAGE_SORT",
                "handler": "SortingAnswerHandler",
                "settings": {},
                answers: answersSort
            });
            medias = answersSort;
            break;
        case '2_STEP':
            const answers2step = [];

            if (questionPic) {
                answers2step.push(questionPic);
            }

            mappings.push({
                "id": "ANSWER_TEXT",
                "handler": "SingleChoiceAnswerHandler",
                "settings": {},
                "answers": [
                    correctAnswer,
                    wrongAnswer1,
                    wrongAnswer2,
                    wrongAnswer3,
                ]
            });

            mappings2step = [
                {
                    "id": "IMAGE",
                    "settings": {
                        "timer": true,
                        "text": true
                    },
                    "value": questionPic,
                    "text": questionPicTxt
                },
                {
                    "id": "BUTTON_NEXT",
                    "settings": {},
                    "value": null
                }
            ];

            medias2step = answers2step;
            break;
    }

    mappings.push(
        {
            "id": "BUTTON_SUBMIT",
            "settings": {},
            "value": null
        }
    );

    let content = [{
        name: "The question",
        countdown,
        mappings,
        medias
    }];

    if (typeOfQuestion == '2_STEP') {
        content = [{
            name: "Image",
            countdown: countdownImage,
            mappings: mappings2step,
            medias: medias2step
        }, {
            name: "The question",
            countdown,
            mappings,
            medias
        }];
    }

    const translate = {
        questionId,
        name: "some name",
        hint,
        explanation,
        languageId: language,
        content: content
    };

    if (typeOfQuestion == '2_STEP') {
        translate.resolutionText = resolutionText;
    }
    logger.debug("questionTranslationUpdate start");
    logger.debug("questionTranslationUpdate translate=",translate);
    QuestionTranslationApiFactory.questionTranslationUpdate(translate, message, clientSession, function (err, data) {
        logger.debug("questionTranslationUpdate data=",data);
        if (err) {
            logger.error("questionTranslationUpdate error:", err);
            return CrudHelper.callbackError(err, callback);
        }
        return CrudHelper.callbackSuccess(data, callback);
    });

}

function addMedia(filename,token,namespace,title){
    var formData = {
        namespace: namespace,
        token: token,
        content_type: 'image',
        title: title,
        media_file: {
            value: fs.createReadStream(imagesFolder + '/' + getImageFileName(filename)),
            options: {
                filename: filename,
                contentType: 'image/jpeg'
            }
        }
    };
    return new Promise((resolve, reject) => {
        request.post({
            url: Config.addMediaServiceURIs,
            rejectUnauthorized: false,
            requestCert: false,
            agent: false,
            formData: formData
        }, function (err, response, body) {
            if (err) {
                logger.error('addMedia Error : ', err)
                reject(err);
            } else if (response.statusCode == 200) {
                logger.debug('good ', body)
                resolve(JSON.parse(body));
            } else {
                logger.error('addMedia bad ', response.statusMessage)
                reject (response.statusMessage);
            }
        });
    })


}