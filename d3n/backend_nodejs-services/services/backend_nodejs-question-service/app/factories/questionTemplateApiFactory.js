var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var QuestionTemplate = Database.RdbmsService.Models.QuestionTemplate.QuestionTemplate;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var tenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'questionTemplateModel': [
        {
            destination: '$root',
            model: QuestionTemplate
        }
    ],
    'questionTemplateCreateModel': [
        {
            destination: '$root',
            model: QuestionTemplate
        },
    ],
};
INSTANCE_MAPPINGS['questionTemplateUpdateModel'] = INSTANCE_MAPPINGS['questionTemplateCreateModel'];
INSTANCE_MAPPINGS['questionTemplateQuestionStatisticModel'] = INSTANCE_MAPPINGS['questionTemplateModel'];

module.exports = {
    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    questionTemplateUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var questionTemplateId = undefined;
            var mapBySchema = 'questionTemplateCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                questionTemplateId = params.id;
                mapBySchema = 'questionTemplateUpdateModel';
            }
            var questionTemplateItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var sessionTenantId;
            async.series([
                // get tenantId from global session
                function (next) {
                    tenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                
                // Create/update main entity
                function (next) {
                    if (create) {
                        questionTemplateItem.tenantId = sessionTenantId;
                        return QuestionTemplate.create(questionTemplateItem).then(function (questionTemplate) {
                            questionTemplateId = questionTemplate.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return QuestionTemplate.update(questionTemplateItem, { where: { id: questionTemplateItem.id, tenantId: sessionTenantId } }).then(function (count) {
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
                params.id = questionTemplateId;
                return self.questionTemplateGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTemplateDelete: function (params, callback) {
        try {
            return QuestionTemplate.destroy({ where: { id: params.id } }).then(function (count) {
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
    },

    questionTemplateGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'questionTemplate';
            var mapBySchema = 'questionTemplateModel';
            var attributes = _.keys(QuestionTemplate.attributes);
            if (params.includeStatistic) {
                responseItem = 'questionTemplateQuestionStatistic';
                mapBySchema = 'questionTemplateQuestionStatisticModel';
                attributes = attributes.concat(_questionTemplateQuestionStatistic());
            }
            
            tenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                
                return QuestionTemplate.findOne({
                    where: { id: params.id, tenantId: tenantId },
                    attributes: attributes
                }).then(function (questionTemplate) {
                    if (!questionTemplate) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var questionTemplateItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, questionTemplate);
                    response[responseItem] = questionTemplateItem;
                    return CrudHelper.callbackSuccess(response, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
            
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTemplateList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'questionTemplateModel';
            var attributes = _.keys(QuestionTemplate.attributes);
            if (params.includeStatistic) {
                responseItem = 'questionTemplateQuestionStatistic';
                mapBySchema = 'questionTemplateQuestionStatisticModel';
                attributes = attributes.concat(_questionTemplateQuestionStatistic());
            }
            var orderBy = CrudHelper.orderBy(params, QuestionTemplate);
            var searchBy = CrudHelper.searchBy(params, QuestionTemplate);
            tenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                searchBy.tenantId = tenantId;
                return QuestionTemplate.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy,
                    attributes: attributes
                }).then(function (questionTemplates) {
                    var questionTemplateItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, questionTemplates.rows);
                    return CrudHelper.callbackSuccess({
                        items: questionTemplateItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: questionTemplates.count,
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
            
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    questionTemplateActivate: function (params, callback) {
        try {
            var updQuestionTemplate = { status: 'active' };
            return QuestionTemplate.update(updQuestionTemplate, { where: { id: params.id } }).then(function (count) {
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

    questionTemplateDeactivate: function (params, callback) {
        try {
            var updQuestionTemplate = { status: 'inactive' };
            return QuestionTemplate.update(updQuestionTemplate, { where: { id: params.id } }).then(function (count) {
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

    questionTemplateGetHelp: function (params, callback) {
        try {
            return QuestionTemplate.findOne({
                where: { id: params.id },
                attributes: [ QuestionTemplate.tableAttributes.help.field ]
            }).then(function (questionTemplate) {
                if (!questionTemplate) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                var questionTemplateItem = AutoMapper.mapDefinedBySchema('questionTemplateModel', INSTANCE_MAPPINGS, questionTemplate);
                return CrudHelper.callbackSuccess({ help: questionTemplateItem.help }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
};

function _questionTemplateQuestionStatistic() {
    return [
        CrudHelper.subQuery('questionCount',
            '(SELECT COUNT(`question`.`id`)' +
            '   FROM `question`' +
            '  WHERE `question`.`questionTemplateId` = `questionTemplate`.`id`)'
        ),
    ];
}