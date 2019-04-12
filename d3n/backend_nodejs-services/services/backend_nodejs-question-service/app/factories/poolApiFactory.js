var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var Pool = Database.RdbmsService.Models.Question.Pool;
var PoolHasTag = Database.RdbmsService.Models.Question.PoolHasTag;
var PoolHasTenant = Database.RdbmsService.Models.Question.PoolHasTenant;
var PoolHasQuestion = Database.RdbmsService.Models.Question.PoolHasQuestion;
var PoolHasMedia = Database.RdbmsService.Models.Question.PoolHasMedia;
var Tag = Database.RdbmsService.Models.Question.Tag;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikePoolList = KeyvalueService.Models.AerospikePoolList;
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'poolModel': [
        {
            destination: '$root',
            model: Pool,
            fullTextQuery: 'LOWER(`poolHasTags`.`tagTag`) LIKE "%fulltext_word%" OR LOWER(`pool`.`name`) LIKE "%fulltext_word%" OR LOWER(`pool`.`description`) LIKE "%fulltext_word%"',
            fullTextInclude: [PoolHasTag],
            custom: false,
            customQueryHasQuestionReview: 'EXISTS (SELECT 1 FROM pool_has_question INNER JOIN question ON question.id = pool_has_question.questionId AND question.status = \'review\' WHERE pool_has_question.poolId = `pool`.`id`)',
            customQueryHasNotQuestionReview: 'NOT EXISTS (SELECT 1 FROM pool_has_question INNER JOIN question ON question.id = pool_has_question.questionId AND question.status = \'review\' WHERE pool_has_question.poolId = `pool`.`id`)',
            customQueryHasQuestionTranslationReview: 'EXISTS (SELECT 1 FROM pool_has_question INNER JOIN question ON question.id = pool_has_question.questionId INNER JOIN question_translation ON question_translation.questionId = question.id AND question_translation.status = \'review\' WHERE pool_has_question.poolId = `pool`.`id`)',
            customQueryHasNotQuestionTranslationReview: 'NOT EXISTS (SELECT 1 FROM pool_has_question INNER JOIN question ON question.id = pool_has_question.questionId INNER JOIN question_translation ON question_translation.questionId = question.id AND question_translation.status = \'review\' WHERE pool_has_question.poolId = `pool`.`id`)',
            customQueryHasQuestionTranslationNeeded: 'EXISTS (SELECT 1 FROM pool_has_question INNER JOIN question ON question.id = pool_has_question.questionId AND question.isTranslationNeeded = 1 WHERE pool_has_question.poolId = `pool`.`id`)',
            customQueryHasNotQuestionTranslationNeeded: 'NOT EXISTS (SELECT 1 FROM pool_has_question INNER JOIN question ON question.id = pool_has_question.questionId AND question.isTranslationNeeded = 1 WHERE pool_has_question.poolId = `pool`.`id`)',
        },
        {
            destination: 'tags',
            model: PoolHasTag,
            attributes: [
                { 'tag': PoolHasTag.tableAttributes.tagTag }
            ],
            limit: 0
        },
        {
            destination: 'subpoolsIds',
            model: Pool,
            alias: 'subPools',
            attribute: Pool.tableAttributes.id,
            limit: 0
        },
        {
            destination: 'questionsIds',
            model: PoolHasQuestion,
            attribute: PoolHasQuestion.tableAttributes.questionId,
            limit: 0
        },
        {
            destination: 'mediasIds',
            model: PoolHasMedia,
            attribute: PoolHasMedia.tableAttributes.mediaId,
            limit: 0
        },
    ],
    'poolCreateModel': [
        {
            destination: '$root',
            model: Pool
        },
        {
            destination: 'tags',
            model: PoolHasTag,
            attributes: [
                { 'tag': PoolHasTag.tableAttributes.tagTag }
            ]
        }
    ]
};
INSTANCE_MAPPINGS['poolUpdateModel'] = INSTANCE_MAPPINGS['poolCreateModel'];
INSTANCE_MAPPINGS['poolQuestionStatisticModel'] = INSTANCE_MAPPINGS['poolModel'];

module.exports = {

    poolUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var poolId = undefined;
            var mapBySchema = 'poolCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                poolId = params.id;
                mapBySchema = 'poolUpdateModel';
            }
            var poolItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            if (_.has(poolItem, 'parentPoolId') && poolItem.parentPoolId === 0) {
                poolItem.parentPoolId = null;
            }
            var sessionTenantId;
            var poolItemAssociates = undefined;
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
                // Ensure that pools which are children of a parent pool can not children of their own
                function (next) {
                    if (create || !_.has(params, 'parentPoolId')) {
                        return next();
                    }
                    return Pool.count({ where: { id: params.parentPoolId, parentPoolId: params.id } }).then(function (count) {
                        if (count > 0) {
                            return next(Errors.PoolApi.PoolHierarchyInconsistent);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
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
                    if (create) {
                        poolItem.createDate = _.now();
                        poolItem.creatorResourceId = clientSession.getUserId();
                        return Pool.create(poolItem).then(function (pool) {
                            poolId = pool.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Pool.update(poolItem, { where: { id: poolItem.id } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // Populate pool associated entity values
                function (next) {
                    try {
                        poolItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'poolId',
                                value: poolId,
                            }
                        );
                        // add tenant association
                        poolItemAssociates.push({
                            model: PoolHasTenant,
                            values: [{
                                poolId: poolId,
                                tenantId: sessionTenantId
                            }]
                        });
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (_.isUndefined(poolItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(poolItemAssociates, function (itemAssociate, remove) {
                        return itemAssociate.model.destroy({ where: { poolId: poolId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(poolItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(poolItemAssociates, function (itemAssociate, create) {
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
                // Update aerospike pool list by tenant
                function (next) {
                    if (!create) {
                        return setImmediate(next, null);
                    }
                    try {
                        var include = CrudHelper.include(Pool, [
                            PoolHasTenant
                        ]);
                        include = CrudHelper.includeWhere(include, PoolHasTenant, { tenantId: sessionTenantId }, true);
                        return Pool.findAll({
                            include: include
                        }).then(function (pools) {
                            var poolMapping = {};
                            _.forEach(pools, function (pool) {
                                poolMapping[pool.id.toString()] = pool.name;
                            });
                            return AerospikePoolList.update({
                                tenantId: sessionTenantId,
                                pools: poolMapping
                            }, next);
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return CrudHelper.callbackError(ex, next);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = poolId;
                return self.poolGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    poolDelete: function (params, message, clientSession, callback) {
        try {
            async.mapSeries([
                PoolHasTenant,
                PoolHasQuestion,
                PoolHasTag,
                PoolHasMedia,
            ], function (model, next) {
                try {
                    return model.destroy({ where: { poolId: params.id } }).then(function (count) {
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
                var sessionTenantId;
                return async.series([
                    // Delete pool
                    function (next) {
                        return Pool.destroy({ where: { id: params.id } }).then(function (count) {
                            if (count === 0) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    },
                    // Get tenant from session
                    function (next) {
                        return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                            if (err) {
                                return next(err);
                            }
                            sessionTenantId = tenantId;
                            return next();
                        });
                    },
                    // Update aerospike pool list by tenant
                    function (next) {
                        var include = CrudHelper.include(Pool, [
                            PoolHasTenant
                        ]);
                        include = CrudHelper.includeWhere(include, PoolHasTenant, { tenantId: sessionTenantId }, true);
                        return Pool.findAll({
                            include: include
                        }).then(function (pools) {
                            var poolMapping = {};
                            _.forEach(pools, function (pool) {
                                poolMapping[pool.id.toString()] = pool.name;
                            });
                            return AerospikePoolList.update({
                                tenantId: sessionTenantId,
                                pools: poolMapping
                            }, next);
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
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    poolGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'pool';
            var mapBySchema = 'poolModel';
            if (params.includeStatistic) {
                responseItem = 'poolQuestionStatistic';
                mapBySchema = 'poolQuestionStatisticModel';
            }
            var sessionTenantId;
            var poolItem;
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
                // Get pool by id within tenant
                function (next) {
                    var include = CrudHelper.include(Pool, [
                        PoolHasTenant
                    ]);
                    include = CrudHelper.includeWhere(include, PoolHasTenant, { tenantId: sessionTenantId }, true);
                    include = CrudHelper.includeNoAttributes(include, Pool);
                    return Pool.findOne({
                        where: { id: params.id },
                        include: include
                    }).then(function (pool) {
                        if (!pool) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        poolItem = pool;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged subpool ids
                function (next) {
                    return Pool.findAndCountAll({
                        where: { parentPoolId: params.id }
                    }).then(function (subPools) {
                        poolItem.subPools = subPools;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged media ids
                function (next) {
                    return PoolHasMedia.findAndCountAll({
                        where: { poolId: params.id }
                    }).then(function (poolMedias) {
                        poolItem.poolHasMedias = poolMedias;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged tags by tenant
                function (next) {
                    var include = CrudHelper.include(PoolHasTag, [
                        Tag
                    ]);
                    include = CrudHelper.includeWhere(include, Tag, { tenantId: sessionTenantId }, true);
                    return PoolHasTag.findAndCountAll({
                        where: { poolId: params.id },
                        include: include
                    }).then(function (poolTags) {
                        poolItem.poolHasTags = poolTags;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged question ids
                function (next) {
                    return PoolHasQuestion.findAndCountAll({
                        where: { poolId: params.id }
                    }).then(function (poolQuestions) {
                        poolItem.poolHasQuestions = poolQuestions;
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
                    return _gatherPoolItemStatistics(mapBySchema, params.id, function (err, statistics) {
                        _.merge(poolItem.dataValues, statistics);
                        return next(err);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                response[responseItem] = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, poolItem);
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    poolList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'poolModel';
            if (params.includeStatistic) {
                mapBySchema = 'poolQuestionStatisticModel';
            }
            var orderBy = CrudHelper.orderBy(params, Pool);
            var searchBy = CrudHelper.searchBy(params, Pool);
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                searchBy = CrudHelper.searchByFullText(params, Pool, mapBySchema, INSTANCE_MAPPINGS, clientSession);
            }
            var rootModel = _.find(INSTANCE_MAPPINGS[mapBySchema], { destination: '$root' });
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'hasQuestionReview')) {
                rootModel.custom = true;
                var customQuery = params.searchBy.hasQuestionReview ? 'customQueryHasQuestionReview' : 'customQueryHasNotQuestionReview';
                searchBy = CrudHelper.extendSearchByCustom(params, Pool, mapBySchema, INSTANCE_MAPPINGS, searchBy, customQuery);
            }
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'hasQuestionTranslationReview')) {
                rootModel.custom = true;
                var customQuery = params.searchBy.hasQuestionTranslationReview ? 'customQueryHasQuestionTranslationReview' : 'customQueryHasNotQuestionTranslationReview';
                searchBy = CrudHelper.extendSearchByCustom(params, Pool, mapBySchema, INSTANCE_MAPPINGS, searchBy, customQuery);
            }
            if (_.has(params, 'searchBy') && _.has(params.searchBy, 'hasQuestionTranslationNeeded')) {
                rootModel.custom = true;
                var customQuery = params.searchBy.hasQuestionTranslationNeeded ? 'customQueryHasQuestionTranslationNeeded' : 'customQueryHasNotQuestionTranslationNeeded';
                searchBy = CrudHelper.extendSearchByCustom(params, Pool, mapBySchema, INSTANCE_MAPPINGS, searchBy, customQuery);
            }
            var include = CrudHelper.includeDefined(Pool, [
                Pool,
                PoolHasMedia,
                PoolHasTag,
                PoolHasTenant,
                PoolHasQuestion
            ], params, mapBySchema, INSTANCE_MAPPINGS);
            var attributes = CrudHelper.distinctAttributes(Pool.tableAttributes.id);
            var poolItems = [];
            var total = 0;
            var sessionTenantId;
            async.series([
                // Get tenant id from client session object
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if(err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        include = CrudHelper.includeAlias(include, Pool, 'subPools');
                        include = CrudHelper.includeWhere(include, PoolHasTenant, { tenantId: tenantId }, true);
                        if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                            include = CrudHelper.includeNestedWhere(include, PoolHasTag, Tag, { tenantId: tenantId }, false);
                        }
                        include = CrudHelper.includeNoAttributes(include, Pool);
                        return next();
                    });
                },
                // Populate total count and pool ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    return Pool.count({
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
                // Populate total count and pool ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    return Pool.findAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        attributes: attributes,
                        where: searchBy,
                        include: include,
                        order: orderBy,
                        subQuery: false,
                        raw: true
                    }).then(function (pools) {
                        if (!_.has(searchBy, 'id')) {
                            var poolsIds = [];
                            _.forEach(pools, function (pool) {
                                poolsIds.push(pool.id);
                            });
                            searchBy = {
                                id: { '$in': poolsIds }
                            };
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate paged pool list without nested items
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return Pool.findAll({
                        where: searchBy,
                        order: orderBy,
                        subQuery: false
                    }).then(function (pools) {
                        poolItems = pools;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate nested items for each pool separately
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return async.mapSeries(poolItems, function (poolItem, nextPool) {
                        return async.series([
                            // Populate paged subpool ids
                            function (nextItem) {
                                var where = { parentPoolId: poolItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'subPoolsIds')) {
                                    where.id = { '$in': params.searchBy.subPoolsIds }
                                }
                                return Pool.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (subPools) {
                                    poolItem.subPools = subPools;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged media ids
                            function (nextItem) {
                                var where = { poolId: poolItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'mediasIds')) {
                                    where.mediaId = { '$in': params.searchBy.mediasIds }
                                }
                                return PoolHasMedia.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (poolMedias) {
                                    poolItem.poolHasMedias = poolMedias;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged tags by tenant
                            function (nextItem) {
                                var include = CrudHelper.include(PoolHasTag, [
                                    Tag
                                ]);
                                include = CrudHelper.includeWhere(include, Tag, { tenantId: sessionTenantId }, true);
                                var whereTags = [];
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'tags')) {
                                    whereTags.push({ '$in': params.searchBy.tags });
                                }
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'fulltext')) {
                                    whereTags = CrudHelper.searchByTags(params, PoolHasTag, PoolHasTag.tableAttributes.tagTag.field);
                                }
                                var where = { poolId: poolItem.id };
                                if (whereTags.length > 0) {
                                    where = { '$and': [where, { '$or': whereTags }] };
                                }
                                return PoolHasTag.findAndCountAll({
                                    where: where,
                                    include: include,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (poolTags) {
                                    poolItem.poolHasTags = poolTags;
                                    return nextItem();
                                }).catch(function (err) {
                                    return CrudHelper.callbackError(err, nextItem);
                                });
                            },
                            // Populate paged question ids
                            function (nextItem) {
                                var where = { poolId: poolItem.id };
                                if (_.has(params, 'searchBy') && _.has(params.searchBy, 'questionsIds')) {
                                    where.questionId = { '$in': params.searchBy.questionsIds }
                                }
                                return PoolHasQuestion.findAndCountAll({
                                    where: where,
                                    limit: Config.rdbms.limit,
                                    offset: 0
                                }).then(function (poolQuestions) {
                                    poolItem.poolHasQuestions = poolQuestions;
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
                                return _gatherPoolItemStatistics(mapBySchema, poolItem.id, function (err, statistics) {
                                    _.merge(poolItem.dataValues, statistics);
                                    return nextItem(err);
                                });
                            }
                        ], nextPool);
                    }, next);
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var instanceMappings = AutoMapper.limitedMappings(INSTANCE_MAPPINGS);
                var poolItemsMapped = AutoMapper.mapListDefinedBySchema(mapBySchema, instanceMappings, poolItems);
                return CrudHelper.callbackSuccess({
                    items: poolItemsMapped,
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
     * Set Pool status
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {{}} callback
     * @returns {*}
     */
    poolSetStatus: function (params, clientSession, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (params.status !== 'active' && params.status !== 'inactive')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            return Pool.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
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
     * Add or remove question from the pool
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {*} callback
     * @returns {*}
     */
    poolAddDeleteQuestion: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(ex, callback);
        }
        try {
            async.series([
                // Create/update main entity
                function (next) {
                    if (!params.hasOwnProperty('deleteQuestions')) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(params.deleteQuestions, function (questionId, remove) {
                        try {
                            var poolQuestionQuery = { poolId: params.id, questionId: questionId };
                            return PoolHasQuestion.destroy({ where: poolQuestionQuery }).then(function (count) {
                                return remove();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, remove);
                            });
                        } catch (ex) {
                            return remove(ex);
                        }
                    }, next);
                },
                function (next) {
                    if (!params.hasOwnProperty('addQuestions')) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(params.addQuestions, function (questionId, addCb) {
                        var poolQuestion = { poolId: params.id, questionId: questionId };
                        return PoolHasQuestion.create(poolQuestion).then(function (created) {
                            return addCb();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, addCb);
                        });
                    }, next);
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

    /**
     * Add or remove media from the pool
     * @param {{}} params
     * @param {ClientSession} clientSession
     * @param {*} callback
     * @returns {*}
     */
    poolAddDeleteMedia: function (params, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(ex, callback);
        }
        try {
            async.series([
                // Create/update main entity
                function (next) {
                    if (!params.hasOwnProperty('deleteMedias')) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(params.deleteMedias, function (mediaId, remove) {
                        try {
                            var poolMediaQuery = { poolId: params.id, mediaId: mediaId };
                            return PoolHasMedia.destroy({ where: poolMediaQuery }).then(function (count) {
                                return remove();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, remove);
                            });
                        } catch (ex) {
                            return remove(ex);
                        }
                    }, next);
                },
                function (next) {
                    if (!params.hasOwnProperty('addMedias')) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(params.addMedias, function (mediaId, addCb) {
                        var poolMedia = { poolId: params.id, mediaId: mediaId };
                        PoolHasMedia.create(poolMedia).then(function (created) {
                            return addCb();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, addCb);
                        });
                    }, next);
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

};

function _gatherPoolItemStatistics(mapBySchema, poolId, callback) {
    var statistics = {};
    return async.series([
        function (next) {
            // Calculate question count per language for given poolId
            return _poolQuestionStatistic(poolId, function (err, data) {
                if (err) {
                    return next(err);
                }
                try {
                    statistics.questionCount = data[0].questionCount;
                    return next();
                } catch (ex) {
                    return next(ex);
                }
            });
        },
        function (next) {
            // Calculate question count per language for given poolId
            return _poolQuestionPerLanguageStatistic(poolId, function (err, data) {
                if (err) {
                    return next(err);
                }
                try {
                    statistics.questionCountsPerLanguages = data;
                    return next();
                } catch (ex) {
                    return next(ex);
                }
            });
        },
        function (next) {
            // Calculate question count per region for given poolId
            return _poolQuestionPerRegionStatistic(poolId, function (err, data) {
                if (err) {
                    return next(err);
                }
                try {
                    statistics.questionCountsPerRegions = data;
                    return next();
                } catch (ex) {
                    return next(ex);
                }
            });
        }
    ], function (err) {
        if (err) {
            return setImmediate(callback, err);
        }
        return setImmediate(callback, null, statistics);
    });
}

function _poolQuestionStatistic(poolId, callback) {
    return CrudHelper.rawQuery(
        'SELECT COUNT(DISTINCT `pool_has_question`.`questionId`) AS questionCount' +
        '  FROM `pool_has_question`' +
        '  INNER JOIN `question` ON `question`.`id` = `pool_has_question`.`questionId`' +
        ' WHERE `pool_has_question`.`poolId` = :poolId' +
        '    AND (EXISTS(SELECT 1' +
        '                  FROM `pool`' +
        '                 WHERE `pool`.`id` = `pool_has_question`.`poolId`) OR' +
        '         EXISTS (SELECT 1' +
        '           FROM `pool`' +
        '          INNER JOIN `pool` AS `subPool` ON `subPool`.`parentPoolId` = `pool`.`id`' +
        '          WHERE `subPool`.`id` = `pool_has_question`.`poolId`))', { poolId: poolId }, callback);
}

function _poolQuestionPerLanguageStatistic(poolId, callback) {
    return CrudHelper.rawQuery(
        'SELECT COUNT(DISTINCT `pool_has_question`.`questionId`) AS `questionCountPerLanguage`' +
        '      ,`question_translation`.`languageId`' +
        '  FROM `pool_has_question`' +
        ' INNER JOIN `question` ON `question`.`id` = `pool_has_question`.`questionId`' +
        ' INNER JOIN `question_translation` ON `question_translation`.`questionId` = `question`.`id`' +
        ' WHERE `pool_has_question`.`poolId` = :poolId' +
        '   AND (EXISTS (SELECT 1' +
        '                  FROM `pool`' +
        '                 WHERE `pool`.`id` = `pool_has_question`.`poolId`) OR' +
        '        EXISTS (SELECT 1' +
        '                  FROM `pool`' +
        '                 INNER JOIN `pool` AS `subPool` ON `subPool`.`parentPoolId` = `pool`.`id`' +
        '                 WHERE `subPool`.`id` = `pool_has_question`.`poolId`))' +
        ' GROUP BY `question_translation`.`languageId`', { poolId: poolId }, callback);
}

function _poolQuestionPerRegionStatistic(poolId, callback) {
    return CrudHelper.rawQuery(
        'SELECT COUNT(DISTINCT `pool_has_question`.`questionId`) AS `questionCountPerRegion`' +
        '      ,`question_has_regional_setting`.`regionalSettingId`' +
        '  FROM `pool_has_question`' +
        ' INNER JOIN `question` ON `question`.`id` = `pool_has_question`.`questionId`' +
        ' INNER JOIN `question_has_regional_setting` ON `question_has_regional_setting`.`questionId` = `question`.`id`' +
        ' WHERE `pool_has_question`.`poolId` = :poolId' +
        '   AND (EXISTS (SELECT 1' +
        '                  FROM `pool`' +
        '                 WHERE `pool`.`id` = `pool_has_question`.`poolId`) OR' +
        '        EXISTS (SELECT 1' +
        '                  FROM `pool`' +
        '                 INNER JOIN `pool` AS `subPool` ON `subPool`.`parentPoolId` = `pool`.`id`' +
        '                 WHERE `subPool`.`id` = `pool_has_question`.`poolId`))' +
        ' GROUP BY `question_has_regional_setting`.`regionalSettingId`', { poolId: poolId }, callback);
}
