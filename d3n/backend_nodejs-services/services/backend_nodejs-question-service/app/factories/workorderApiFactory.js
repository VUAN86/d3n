var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var WorkorderHasPool = Database.RdbmsService.Models.Workorder.WorkorderHasPool;
var WorkorderHasQuestionExample = Database.RdbmsService.Models.Workorder.WorkorderHasQuestionExample;
var WorkorderHasResource = Database.RdbmsService.Models.Workorder.WorkorderHasResource;
var WorkorderHasRegionalSetting = Database.RdbmsService.Models.Workorder.WorkorderHasRegionalSetting;
var WorkorderHasTenant = Database.RdbmsService.Models.Workorder.WorkorderHasTenant;
var WorkorderBillingModel = Database.RdbmsService.Models.Workorder.WorkorderBillingModel;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var TenantService = require('../services/tenantService.js');
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'workorderModel': [
        {
            destination: '$root',
            model: Workorder
        },
        {
            destination: 'questionsExamples',
            model: WorkorderHasQuestionExample,
            attributes: [
                { 'id': WorkorderHasQuestionExample.tableAttributes.questionId },
                { 'number': WorkorderHasQuestionExample.tableAttributes.number }
            ],
            limit: 0
        },
        {
            destination: 'poolsIds',
            model: WorkorderHasPool,
            attribute: WorkorderHasPool.tableAttributes.poolId,
            limit: 0
        },
        {
            destination: 'regionalSettingsIds',
            model: WorkorderHasRegionalSetting,
            attribute: WorkorderHasRegionalSetting.tableAttributes.regionalSettingId,
            limit: 0
        },
        {
            destination: 'questionsIds',
            model: Question,
            attribute: Question.tableAttributes.id,
            limit: 0
        },
    ],
    'workorderCreateModel': [
        {
            destination: '$root',
            model: Workorder
        },
        {
            destination: 'questionsExamples',
            model: WorkorderHasQuestionExample,
            attributes: [
                { 'id': WorkorderHasQuestionExample.tableAttributes.questionId },
                { 'number': WorkorderHasQuestionExample.tableAttributes.number }
            ]
        },
        {
            destination: 'poolsIds',
            model: WorkorderHasPool,
            attribute: WorkorderHasPool.tableAttributes.poolId
        },
        {
            destination: 'regionalSettingsIds',
            model: WorkorderHasRegionalSetting,
            attribute: WorkorderHasRegionalSetting.tableAttributes.regionalSettingId
        }
    ]
};
INSTANCE_MAPPINGS['workorderUpdateModel'] = INSTANCE_MAPPINGS['workorderCreateModel'];
INSTANCE_MAPPINGS['workorderQuestionStatisticModel'] = INSTANCE_MAPPINGS['workorderModel'];

module.exports = {

    /**
     *
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {{}} callback
     * @returns {*}
     */
    workorderUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var workorderId = undefined;
            var mapBySchema = 'workorderCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                workorderId = params.id;
                mapBySchema = 'workorderUpdateModel';
            }
            var workorderItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var workorderItemAssociates = undefined;
            var sessionTenantId;
            async.series([
                // get session tenant
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if(err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                
                // Create/update main entity
                function (next) {
                    if (create) {
                        // Check community promotion mandatory fields
                        if (workorderItem.isCommunityPromoted) {
                            if (!workorderItem.communityPromotionFromDate ||
                                new Date(workorderItem.communityPromotionFromDate).getTime() >= new Date(workorderItem.communityPromotionToDate).getTime() ||
                                !workorderItem.communityPromotionCurrency ||
                                !workorderItem.communityPromotionAmount) {
                                return next(Errors.QuestionApi.ValidationFailed);
                            }
                        }
                        if (!_.has(workorderItem, 'createDate')) {
                            workorderItem.createDate = _.now();
                        }
                        // Owner not set -> set current user
                        if (!_.has(workorderItem, 'ownerResourceId')) {
                            workorderItem.ownerResourceId = clientSession.getUserId();
                        }
                        return Workorder.create(workorderItem).then(function (workorder) {
                            workorderId = workorder.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Workorder.findOne({
                            where: { id: params.id }
                        }).then(function (workorder) {
                            if (!workorder) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            // Check community promotion mandatory fields
                            var workorderUpdate = _.assign(workorder, workorderItem);
                            if (workorderUpdate.isCommunityPromoted) {
                                if (!workorderUpdate.communityPromotionFromDate ||
                                    new Date(workorderUpdate.communityPromotionFromDate).getTime() >= new Date(workorderUpdate.communityPromotionToDate).getTime() ||
                                    !workorderUpdate.communityPromotionCurrency ||
                                    !workorderUpdate.communityPromotionAmount) {
                                    return next(Errors.QuestionApi.ValidationFailed);
                                }
                            }
                            return Workorder.update(workorderItem, { where: { id: workorderItem.id } }).then(function (count) {
                                if (count[0] === 0) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                
                // Populate workorder associated entity values
                function (next) {
                    try {
                        workorderItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'workorderId',
                                value: workorderId,
                            }
                        );
                        // add tenant association
                        workorderItemAssociates.push({
                            model: WorkorderHasTenant,
                            values: [{
                                workorderId: workorderId,
                                tenantId: sessionTenantId
                            }]
                        });
                        return next();
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (_.isUndefined(workorderItemAssociates)) {
                        return next();
                    }
                    async.mapSeries(workorderItemAssociates, function (itemAssociate, remove) {
                        //if (itemAssociate.values.length === 0) {
                        //    return remove();
                        //}
                        itemAssociate.model.destroy({ where: { workorderId: workorderId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(workorderItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(workorderItemAssociates, function (itemAssociate, create) {
                        if (itemAssociate.values.length === 0) {
                            return create();
                        }
                        itemAssociate.model.bulkCreate(itemAssociate.values).then(function (records) {
                            if (records.length !== itemAssociate.values.length) {
                                return create(Errors.QuestionApi.FatalError);
                            }
                            return create();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, create);
                        });
                    }, next);
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = workorderId;
                return self.workorderGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    workorderDelete: function (params, callback) {
        try {
            async.mapSeries([
                WorkorderHasTenant,
                WorkorderHasResource,
                WorkorderHasQuestionExample,
                WorkorderHasPool,
                WorkorderHasRegionalSetting
            ], function (model, next) {
                try {
                    model.destroy({ where: { workorderId: params.id } }).then(function (count) {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                } catch (ex) {
                    return setImmediate(next, ex);
                }
            }, function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return Workorder.destroy({ where: { id: params.id } }).then(function (count) {
                    if (count === 0) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    return CrudHelper.callbackSuccess(null, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    workorderGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'workorder';
            var mapBySchema = 'workorderModel';
            if (params.includeStatistic) {
                responseItem = 'workorderQuestionStatistic';
                mapBySchema = 'workorderQuestionStatisticModel';
            }
            var include = CrudHelper.include(Workorder, [
                WorkorderHasTenant
            ]);
            var sessionTenantId;
            var workorderItem;
            async.series([
                // Get tenant id from session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if(err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get workorder item within tenant
                function (next) {
                    include = CrudHelper.includeWhere(include, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    include = CrudHelper.includeNoAttributes(include, Workorder);
                    return Workorder.findOne({
                        where: { id: params.id },
                        include: include
                    }).then(function (workorder) {
                        if (!workorder) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        workorderItem = workorder;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged question example ids
                function (next) {
                    return WorkorderHasQuestionExample.findAndCountAll({
                        where: { workorderId: params.id }
                    }).then(function (workorderQuestions) {
                        workorderItem.workorderHasQuestionExamples = workorderQuestions;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged pool ids
                function (next) {
                    return WorkorderHasPool.findAndCountAll({
                        where: { workorderId: params.id }
                    }).then(function (workorderPools) {
                        workorderItem.workorderHasPools = workorderPools;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged regional setting ids
                function (next) {
                    return WorkorderHasRegionalSetting.findAndCountAll({
                        where: { workorderId: params.id }
                    }).then(function (workorderRegionalSettings) {
                        workorderItem.workorderHasRegionalSettings = workorderRegionalSettings;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged question ids
                function (next) {
                    return Question.findAndCountAll({
                        where: { workorderId: params.id }
                    }).then(function (workorderQuestions) {
                        workorderItem.questions = workorderQuestions;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate statistics by request
                function (next) {
                    if (!params.includeStatistic) {
                        return setImmediate(next, null);
                    }
                    return _gatherWorkorderItemStatistics(mapBySchema, params.id, function (err, statistics) {
                        _.merge(workorderItem.dataValues, statistics);
                        return next(err);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                response[responseItem] = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, workorderItem);
                return CrudHelper.callbackSuccess(response, callback);
            });
            
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    workorderList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'workorderModel';
            if (params.includeStatistic) {
                mapBySchema = 'workorderQuestionStatisticModel';
            }
            var orderBy = CrudHelper.orderBy(params, Workorder);
            var searchBy = CrudHelper.searchBy(params, Workorder);
            // By default filter ony active workorders
            if (!_.has(searchBy, 'status')) {
                searchBy.status = { '$in': [Workorder.constants().STATUS_DRAFT, Workorder.constants().STATUS_INPROGRESS] };
            }
            var include = CrudHelper.includeDefined(Workorder, [
                WorkorderHasQuestionExample,
                WorkorderHasPool,
                WorkorderHasResource,
                WorkorderHasRegionalSetting,
                WorkorderHasTenant,
                Question
            ], params, mapBySchema, INSTANCE_MAPPINGS);
            var attributes = CrudHelper.distinctAttributes(Workorder.tableAttributes.id);
            var workorderItems = [];
            var total = 0;
            var sessionTenantId;
            async.series([
                // Get tenant id from session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if(err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        include = CrudHelper.includeWhere(include, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                        include = CrudHelper.includeNoAttributes(include, Workorder);
                        return next();
                    });
                },
                // Populate total count
                function (next) {
                    return Workorder.count({
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
                // Populate total count and workorder ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    return Workorder.findAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        attributes: attributes,
                        where: searchBy,
                        include: include,
                        order: orderBy,
                        subQuery: false,
                        raw: true
                    }).then(function (workorders) {
                        if (!_.has(searchBy, 'id')) {
                            var workordersIds = [];
                            _.forEach(workorders, function (workorder) {
                                workordersIds.push(workorder.id);
                            });
                            searchBy = {
                                id: { '$in': workordersIds }
                            };
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged workorder list without nested items
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return Workorder.findAll({
                        where: searchBy,
                        order: orderBy,
                        subQuery: false
                    }).then(function (workorders) {
                        workorderItems = workorders;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate nested items for each workorder separately
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(workorderItems, function (workorderItem, nextWorkorder) {
                        return async.series([
                            // Populate paged question example attributes
                            function (nextItem) {
                                var where = { workorderId: workorderItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'questionsExamples')) {
                                    var whereQuestionExamples = [];
                                    _.forEach(params.searchBy.questionsExamples, function (questionsExample) {
                                        whereQuestionExamples.push({ '$and': [{ questionId: questionsExample.id }, { number: questionsExample.number }] });
                                    });
                                    where = { '$and': [where, { '$or': whereQuestionExamples }] };
                                }
                                return WorkorderHasQuestionExample.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (workorderQuestionExamples) {
                                    workorderItem.workorderHasQuestionExamples = workorderQuestionExamples;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged pool ids
                            function (nextItem) {
                                var where = { workorderId: workorderItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'poolsIds')) {
                                    where.poolId = { '$in': params.searchBy.poolsIds }
                                }
                                return WorkorderHasPool.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (workorderPools) {
                                    workorderItem.workorderHasPools = workorderPools;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged regional setting ids
                            function (nextItem) {
                                var where = { workorderId: workorderItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'regionalSettingsIds')) {
                                    where.regionalSettingId = { '$in': params.searchBy.regionalSettingsIds }
                                }
                                return WorkorderHasRegionalSetting.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (workorderRegionalSettings) {
                                    workorderItem.workorderHasRegionalSettings = workorderRegionalSettings;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged question ids
                            function (nextItem) {
                                var where = { workorderId: workorderItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'questionsIds')) {
                                    where.questionId = { '$in': params.searchBy.questionsIds }
                                }
                                return Question.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (workorderQuestions) {
                                    workorderItem.questions = workorderQuestions;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate statistics by request
                            function (nextItem) {
                                if (!params.includeStatistic || total === 0) {
                                    return setImmediate(nextItem, null);
                                }
                                return _gatherWorkorderItemStatistics(mapBySchema, workorderItem.id, function (err, statistics) {
                                    _.merge(workorderItem.dataValues, statistics);
                                    return nextItem(err);
                                });
                            }
                        ], nextWorkorder);
                    }, next);
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var instanceMappings = AutoMapper.limitedMappings(INSTANCE_MAPPINGS);
                var workorderItemsMapped = AutoMapper.mapListDefinedBySchema(mapBySchema, instanceMappings, workorderItems);
                return CrudHelper.callbackSuccess({
                    items: workorderItemsMapped,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    workorderByResourceList: function (params, message, clientSession, callback) {
        try {
            var workorderItems = [];
            var sessionTenantId;
            var total = 0;
            async.series([
                // Add tenantId filter
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Populate model list with all nested model elements
                function (next) {
                    var include = [
                        {
                            model: WorkorderHasTenant,
                            required: true,
                            where: { tenantId: sessionTenantId }
                        },
                        {
                            model: WorkorderHasResource,
                            required: true,
                            where: { resourceId: params.resourceId }
                        },
                    ];
                    Workorder.findAndCountAll({
                        include: include,
                        order: 'id ASC',
                        limit: params.limit,
                        offset: params.offset,
                        distinct: true,
                        subQuery: false
                    }).then(function (workorders) {
                        total = workorders.count;
                        _.forEach(workorders.rows, function (workoder) {
                            var actions = _.map(workoder.workorderHasResources, function (workoderHasResource) {
                                return workoderHasResource.get({ plain: true }).action;
                            });
                            workorderItems.push({ workorderId: workoder.id, actions: actions });
                        });
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({
                    items: workorderItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set Workorder status
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {{}} callback
     * @returns {*}
     */
    workorderSetStatus: function (params, clientSession, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (params.status !== 'inprogress' && params.status !== 'inactive' && params.status !== 'closed')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Workorder should exist
                function (next) {
                    if (params.status !== 'inprogress') {
                        return setImmediate(next, null);
                    }
                    return Workorder.count({ where: { id: params.id } }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // ->Activate: workorder should have at least one pool
                function (next) {
                    if (params.status !== 'inprogress') {
                        return setImmediate(next, null);
                    }
                    return WorkorderHasPool.count({ where: { workorderId: params.id } }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.WorkorderApi.WorkorderHasNoPool);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Change status
                function (next) {
                    return Workorder.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
                        if (count[0] === 0) {
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

    /**
     * Closes a workorder
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {*} callback
     * @returns {*}
     */
    workorderClose: function (params, clientSession, callback) {
        var self = this;
        try {
            return Workorder.findOne({where: { id: params.id }}).then(function (workorder) {
                if (!workorder) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                if (workorder.status === 'closed') {
                    return CrudHelper.callbackSuccess(null, callback);
                } else if (workorder.status === 'archived') {
                    return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
                } else {
                    return self.workorderSetStatus({status: 'closed', id: params.id}, clientSession, callback);
                }
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Assignes and removes resources to a workorder
     * TODO check if workorder is community only
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {*} callback
     * @returns {*}
     */
    workorderAssignRemoveResources: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('id') || (params.hasOwnProperty('assigns') && !params.hasOwnProperty('action'))) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        // Same resources cannot be added and removed
        if (params.hasOwnProperty('assigns') && params.hasOwnProperty('removes') && _.intersection(params.assigns, params.removes).length > 0) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            var assigns = [];
            var assignEmails = [];
            var assignPhones = [];
            var workorderTitle;
            async.series([
                // Remove entries
                function (next) {
                    if (!params.hasOwnProperty('removes')) {
                        return setImmediate(next, null);
                    }
                    try {
                        return WorkorderHasResource.destroy({ where: { workorderId: params.id, resourceId: { '$in': params.removes }, action: params.action } }).then(function (count) {
                            if (count !== params.removes.length) {
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
                // Save entries, that not already exist
                function (next) {
                    if (!params.hasOwnProperty('assigns')) {
                        return setImmediate(next, null);
                    }
                    return WorkorderHasResource.findAll({
                        where: { workorderId: params.id },
                        attributes: [WorkorderHasResource.tableAttributes.resourceId.fieldName]
                    }).then(function (existingResources) {
                        var existing = [];
                        _.forEach(existingResources, function (existingResource) {
                            var existingResourcePlain = existingResource.get({ plain: true });
                            existing.push({
                                resourceId: existingResourcePlain.resourceId,
                                action: existingResourcePlain.action
                            });
                        });
                        _.forEach(params.assigns, function (resourceId) {
                            if (!_.includes(existing, { resourceId: resourceId, action: params.action })) {
                                assigns.push({ workorderId: params.id, resourceId: resourceId, action: params.action });
                            }
                        });
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Read workorder attributes
                function (next) {
                    if (assigns.length === 0) {
                        return setImmediate(next, null);
                    }
                    return Workorder.findOne({
                        where: { id: params.id },
                        attributes: [Workorder.tableAttributes.title.fieldName]
                    }).then(function (workorder) {
                        workorderTitle = workorder.get({ plain: true }).title;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Collect email addresses, phones and users which are registered with facebook/google for all new added resources (email/phone should exist and be confirmed)
                function (next) {
                    if (assigns.length === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(assigns, function (assign, nextAssign) {
                        try {
                            clientSession.getConnectionService().getProfileService().getUserProfile({
                                userId: assign.resourceId
                            }, undefined, function (err, message) {
                                if (err || (message && message.getError())) {
                                    logger.error('WorkorderApiFactory.workorderAssignRemoveResources: getUserProfile', assign.resourceId, err || (message && message.getError()));
                                    return nextAssign(Errors.DatabaseApi.NoRecordFound);
                                }
                                if (!message.getContent().profile || _.isEmpty(message.getContent().profile)) {
                                    return nextAssign(Errors.DatabaseApi.NoRecordFound);
                                }
                                var found = false;
                                if (message.getContent().profile.emails) {
                                    var verifiedEmail = _.find(message.getContent().profile.emails, {
                                        verificationStatus: 'verified'
                                    });
                                    if (verifiedEmail && verifiedEmail.email) {
                                        assignEmails.push({ userId: assign.resourceId, email: verifiedEmail.email });
                                        found = true;
                                    }
                                }
                                if (!found && message.getContent().profile.phones) {
                                    var verifiedPhone = _.find(message.getContent().profile.phones, {
                                        verificationStatus: 'verified'
                                    });
                                    if (verifiedPhone && verifiedPhone.phone) {
                                        assignPhones.push({ userId: assign.resourceId, phone: verifiedPhone.phone });
                                    }
                                }
                                if (!found && (message.getContent().profile.facebook || message.getContent().profile.google)) {
                                    assignEmails.push({ userId: assign.resourceId, email: null });
                                }
                                return nextAssign();
                            });
                        } catch (ex) {
                            logger.error('WorkorderApiFactory.workorderAssignRemoveResources: profile.getUserProfile', ex);
                            return nextAssign(Errors.QuestionApi.NoInstanceAvailable);
                        }
                    }, function (err) {
                        return next(err);
                    });
                },
                // Create entries, that not already exist
                function (next) {
                    if (assigns.length === 0) {
                        return setImmediate(next, null);
                    }
                    try {
                        return WorkorderHasResource.bulkCreate(assigns).then(function (records) {
                            if (records.length !== assigns.length) {
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
                // Send email notifications to all found emails
                function (next) {
                    if (assignEmails.length === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(assignEmails, function (assignEmail, nextAssign) {
                        try {
                            var sendEmailParams = {
                                userId: assignEmail.userId,
                                address: assignEmail.email,
                                subject: Config.userMessage.workorderAssignEmail.subject,
                                message: Config.userMessage.workorderAssignEmail.message,
                                subject_parameters: [workorderTitle],
                                message_parameters: [workorderTitle],
                                waitResponse: Config.umWaitForResponse,
                                clientInfo: clientSession.getClientInfo()
                            };
                            clientSession.getConnectionService().getUserMessage().sendEmail(sendEmailParams, function (err, message) {
                                if (err || (message && message.getError())) {
                                    logger.error('WorkorderApiFactory.workorderAssignRemoveResources: sendEmail', sendEmailParams, err || (message && message.getError()));
                                }
                                return nextAssign();
                            });
                        } catch (ex) {
                            return nextAssign(Errors.QuestionApi.NoInstanceAvailable);
                        }
                    }, function (err) {
                        return next(err);
                    });
                },
                // Send phone notifications to all found phones
                function (next) {
                    if (assignPhones.length === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(assignPhones, function (assignPhone, nextAssign) {
                        try {
                            var sendSmsParams = {
                                userId: assignPhone.userId,
                                phone: assignPhone.phone,
                                message: Config.userMessage.workorderAssignSms.message,
                                message_parameters: [workorderTitle],
                                waitResponse: Config.umWaitForResponse,
                                clientInfo: clientSession.getClientInfo()
                            };
                            clientSession.getConnectionService().getUserMessage().sendSms(sendSmsParams, function (err, message) {
                                if (err || (message && message.getError())) {
                                    logger.error('WorkorderApiFactory.workorderAssignRemoveResources: sendSms', sendSmsParams, err || (message && message.getError()));
                                }
                                return nextAssign();
                            });
                        } catch (ex) {
                            return nextAssign(Errors.QuestionApi.NoInstanceAvailable);
                        }
                    }, function (err) {
                        return next(err);
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

    /**
     * Gets resources to a workorder
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {*} callback
     * @returns {*}
     */
    workorderGetResources: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            return WorkorderHasResource.findAndCountAll({
                where: { workorderId: params.id }
            }).then(function (workorderHasResources) {
                var resources = [];
                _.forEach(workorderHasResources.rows, function (workorderHasResource) {
                    resources.push({ action: workorderHasResource.action, resourceId: workorderHasResource.resourceId });
                });
                return CrudHelper.callbackSuccess({
                    resources: resources
                }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
};

function _gatherWorkorderItemStatistics(mapBySchema, workorderId, callback) {
    var statistics = {};
    try {
        return CrudHelper.rawQuery(
            'SELECT (SELECT COUNT(DISTINCT `question`.`id`) AS draftedQuestionCount' +
            '          FROM `question`' +
            '         WHERE `question`.`workorderId` = `workorder`.`id`' +
            '           AND `question`.`status` = "draft") AS draftedQuestionCount,' +
            '       (SELECT COUNT(DISTINCT`question`.`id`) AS draftedQuestionCount' +
            '         FROM `question`' +
            '        WHERE `question`.`workorderId` = `workorder`.`id`' +
            '          AND `question`.`status` IN ("review", "approved", "declined", "translation")) AS submittedQuestionCount,' +
            '       (SELECT COUNT(DISTINCT`question`.`id`) AS draftedQuestionCount' +
            '          FROM `question`' +
            '         WHERE `question`.`workorderId` = `workorder`.`id`' +
            '           AND `question`.`status` = "active") AS approvedQuestionCount, ' +
            '       (SELECT COUNT(DISTINCT`question`.`id`) AS draftedQuestionCount' +
            '          FROM `question`' +
            '         WHERE `question`.`workorderId` = `workorder`.`id`' +
            '           AND `question`.`status` = "completed") AS completedQuestionCount, ' +
            '       (SELECT COUNT(DISTINCT`question`.`id`) AS draftedQuestionCount' +
            '          FROM `question`' +
            '         WHERE `question`.`workorderId` = `workorder`.`id`) AS totalQuestionCount' +
            '  FROM `workorder` WHERE `workorder`.`id` = :workorderId', { workorderId: workorderId }, function (err, data) {
            if (err) {
                return setImmediate(callback, err);
            }
            statistics.draftedQuestionCount = data[0].draftedQuestionCount;
            statistics.submittedQuestionCount = data[0].submittedQuestionCount;
            statistics.approvedQuestionCount = data[0].approvedQuestionCount;
            statistics.completedQuestionCount = data[0].completedQuestionCount;
            statistics.totalQuestionCount = data[0].totalQuestionCount;
            return setImmediate(callback, null, statistics);
        });
    } catch (ex) {
        return setImmediate(callback, err);
    }
}