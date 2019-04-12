var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Promocode = Database.RdbmsService.Models.PromocodeManager.Promocode;
var PromocodeCampaign = Database.RdbmsService.Models.PromocodeManager.PromocodeCampaign;
var ApplicationHasPromocodeCampaign = Database.RdbmsService.Models.Application.ApplicationHasPromocodeCampaign;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikePromocodeCampaign = KeyvalueService.Models.AerospikePromocodeCampaign;
var AerospikePromocodeCampaignList = KeyvalueService.Models.AerospikePromocodeCampaignList;
var AerospikePromocodeClass = KeyvalueService.Models.AerospikePromocodeClass;
var AerospikePromocodeCounter = KeyvalueService.Models.AerospikePromocodeCounter;
var TenantService = require('../services/tenantService.js');
var PromocodeService = require('../services/promocodeService.js');
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'promocodeCampaignModel': [
        {
            destination: '$root',
            model: PromocodeCampaign
        },
        {
            destination: 'applicationsIds',
            model: ApplicationHasPromocodeCampaign,
            attribute: ApplicationHasPromocodeCampaign.tableAttributes.applicationId
        }
    ],
    'promocodeCampaignCreateModel': [
        {
            destination: '$root',
            model: PromocodeCampaign
        },
        {
            destination: 'applicationsIds',
            model: ApplicationHasPromocodeCampaign,
            attribute: ApplicationHasPromocodeCampaign.tableAttributes.applicationId
        }
    ],
    'promocodeCampaignPublishModel': [
        {
            destination: '$root',
            model: PromocodeCampaign
        },
        {
            destination: 'appIds',
            model: ApplicationHasPromocodeCampaign,
            attribute: ApplicationHasPromocodeCampaign.tableAttributes.applicationId
        }
    ],
    'promocodeModel': [
        {
            destination: '$root',
            model: Promocode
        }
    ],
    'promocodeCreateModel': [
        {
            destination: '$root',
            model: Promocode
        }
    ],
    'promocodeInstanceModel': [
        {
            destination: '$root',
            model: Promocode
        }
    ],
};
INSTANCE_MAPPINGS['promocodeCampaignUpdateModel'] = INSTANCE_MAPPINGS['promocodeCampaignCreateModel'];

module.exports = {

    promocodeCampaignUpdate: function (params, message, clientSession, callback) {
        if (_.has(params, 'startDate') && !_.isUndefined(params.startDate) && !_.isNull(params.startDate) &&
            _.has(params, 'endDate') && !_.isUndefined(params.endDate) && !_.isNull(params.endDate) &&
            new Date(params.startDate).getTime() >= new Date(params.endDate).getTime() ) {
            logger.error("PromocodeApiFactory.promocodeCampaignUpdate: validation failed ", JSON.stringify(params));
            return CrudHelper.callbackError(Errors.PromocodeApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var create = true;
            var promocodeCampaignId = undefined;
            var mapBySchema = 'promocodeCampaignCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                promocodeCampaignId = params.id;
                mapBySchema = 'promocodeCampaignUpdateModel';
            }
            var promocodeCampaignItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var sessionTenantId;
            var promocodeCampaignItemAssociates = undefined;
            async.series([
                // Get tenantId from global session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
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
                        promocodeCampaignItem.tenantId = sessionTenantId;
                        return PromocodeCampaign.create(promocodeCampaignItem).then(function (result) {
                            promocodeCampaignId = result.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        PromocodeCampaign.update(promocodeCampaignItem, { where: { id: promocodeCampaignItem.id, tenantId: sessionTenantId }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // Populate promocode campaign associated entity values
                function (next) {
                    try {
                        promocodeCampaignItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'promocodeCampaignId',
                                value: promocodeCampaignId,
                            }
                        );
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(promocodeCampaignItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(promocodeCampaignItemAssociates, function (itemAssociate, remove) {
                        //if (itemAssociate.values.length === 0) {
                        //    return remove();
                        //}
                        itemAssociate.model.destroy({ where: { promocodeCampaignId: promocodeCampaignId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(promocodeCampaignItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(promocodeCampaignItemAssociates, function (itemAssociate, create) {
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
                params.id = promocodeCampaignId;
                return self.promocodeCampaignGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set PromocodeCampaign status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    promocodeCampaignSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== PromocodeCampaign.constants().STATUS_ACTIVE &&
            params.status !== PromocodeCampaign.constants().STATUS_INACTIVE &&
            params.status !== PromocodeCampaign.constants().STATUS_DIRTY &&
            params.status !== PromocodeCampaign.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.PromocodeApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current status
                function (next) {
                    return PromocodeCampaign.findOne({ where: { id: params.id } }).then(function (promocodeCampaign) {
                        return self.promocodeCampaignCheckStatus(params.status, promocodeCampaign, next);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return PromocodeCampaign.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        if (count[0] === 0) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                        return CrudHelper.callbackSuccess(null, next);
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

    promocodeCampaignCheckStatus: function (status, promocodeCampaign, callback) {
        var self = this;
        if (status !== PromocodeCampaign.constants().STATUS_ACTIVE &&
            status !== PromocodeCampaign.constants().STATUS_INACTIVE &&
            status !== PromocodeCampaign.constants().STATUS_DIRTY &&
            status !== PromocodeCampaign.constants().STATUS_ARCHIVED) {
            return CrudHelper.callbackError(Errors.PromocodeApi.ValidationFailed, callback);
        }
        try {
            if (!promocodeCampaign) {
                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
            }
            // Only inactive (but not archived) promocodeCampaigns can be archived/activated
            if (status === PromocodeCampaign.constants().STATUS_ARCHIVED) {
                if (promocodeCampaign.status === PromocodeCampaign.constants().STATUS_ARCHIVED) {
                    return CrudHelper.callbackError(Errors.PromocodeApi.PromocodeCampaignIsArchived, callback);
                }
                if (promocodeCampaign.status === PromocodeCampaign.constants().STATUS_ACTIVE) {
                    return CrudHelper.callbackError(Errors.PromocodeApi.PromocodeCampaignIsActivated, callback);
                }
            }
            if (status === PromocodeCampaign.constants().STATUS_ACTIVE) {
                if (promocodeCampaign.status === PromocodeCampaign.constants().STATUS_ARCHIVED) {
                    return CrudHelper.callbackError(Errors.PromocodeApi.PromocodeCampaignIsArchived, callback);
                }
            }
            // Only active promocodeCampaigns can be deactivated
            if (status === PromocodeCampaign.constants().STATUS_INACTIVE) {
                if (promocodeCampaign.status === PromocodeCampaign.constants().STATUS_INACTIVE) {
                    return CrudHelper.callbackError(Errors.PromocodeApi.PromocodeCampaignIsDeactivated, callback);
                }
                if (promocodeCampaign.status === PromocodeCampaign.constants().STATUS_ARCHIVED) {
                    return CrudHelper.callbackError(Errors.PromocodeApi.PromocodeCampaignIsArchived, callback);
                }
            }
            return CrudHelper.callbackSuccess(null, callback);
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    promocodeCampaignDelete: function (params, callback) {
        var self = this;
        try {
            // Ensure promocode campaign contains no promocode
            return Promocode.count({
                where: { promocodeCampaignId: params.id }
            }).then(function (count) {
                if (count > 0) {
                    return CrudHelper.callbackError(Errors.PromocodeApi.PromocodeCampaignContainsPromocode, callback);
                }
                return async.series([
                    // Remove promocode campaign relations to applications
                    function (next) {
                        return ApplicationHasPromocodeCampaign.destroy({ where: { promocodeCampaignId: params.id } }).then(function (count) {
                            return CrudHelper.callbackSuccess(null, next);
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    },
                    // Remove promocode campaign
                    function (next) {
                        return PromocodeCampaign.destroy({ where: { id: params.id } }).then(function (count) {
                            if (count === 0) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                            }
                            return CrudHelper.callbackSuccess(null, next);
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                ], callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    promocodeCampaignGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'promocodeCampaign';
            var mapBySchema = 'promocodeCampaignModel';
            var attributes = _.keys(PromocodeCampaign.attributes);
            var include = CrudHelper.include(PromocodeCampaign, [
                ApplicationHasPromocodeCampaign
            ]);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return PromocodeCampaign.findOne({
                    where: { id: params.id, tenantId: tenantId},
                    attributes: attributes,
                    include: include
                }).then(function (promocodeCampaign) {
                    if (!promocodeCampaign) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var promocodeCampaignItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, promocodeCampaign);
                    response[responseItem] = promocodeCampaignItem;
                    return CrudHelper.callbackSuccess(response, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    promocodeCampaignList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'promocodeCampaignModel';
            var attributes = _.keys(PromocodeCampaign.attributes);
            var orderBy = CrudHelper.orderBy(params, PromocodeCampaign);
            var searchBy = CrudHelper.searchBy(params, PromocodeCampaign);
            var promocodeCampaignItems = [];
            var total = 0;
            async.series([
                // get tenantId from global session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        searchBy.tenantId = tenantId;
                        return next();
                    });
                },
                // Populate total count and regional setting ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    var include = CrudHelper.includeDefined(PromocodeCampaign, [
                        ApplicationHasPromocodeCampaign
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return PromocodeCampaign.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (promocodeCampaigns) {
                        total = promocodeCampaigns.count;
                        if (!_.has(searchBy, 'id')) {
                            var promocodeCampaignsIds = [];
                            _.forEach(promocodeCampaigns.rows, function (promocodeCampaign) {
                                promocodeCampaignsIds.push(promocodeCampaign.id);
                            });
                            searchBy = {
                                id: { '$in': promocodeCampaignsIds }
                            };
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Populate model list with all nested model elements
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    var include = CrudHelper.include(PromocodeCampaign, [
                        ApplicationHasPromocodeCampaign
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return PromocodeCampaign.findAll({
                        where: searchBy,
                        order: orderBy,
                        attributes: attributes,
                        include: include,
                        subQuery: false
                    }).then(function (promocodeCampaigns) {
                        promocodeCampaignItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, promocodeCampaigns);
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
                    items: promocodeCampaignItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    promocodeCampaignGetForPublish: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var promocodeCampaignItem;
            var promocodeCampaignAlreadyPublished = false;
            var sessionTenantId;
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
                // Get promocode campaign record by tenant
                function (next) {
                    return PromocodeCampaign.findOne({
                        where: { id: params.id, tenantId: sessionTenantId },
                        include: [
                            {
                                model: ApplicationHasPromocodeCampaign, required: true, where: { promocodeCampaignId: params.id }
                            }
                        ],
                        subQuery: false
                    }).then(function (promocodeCampaign) {
                        if (!promocodeCampaign) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (promocodeCampaign.status === PromocodeCampaign.constants().STATUS_ARCHIVED) {
                            return next(Errors.PromocodeApi.PromocodeCampaignIsArchived);
                        }
                        promocodeCampaignItem = promocodeCampaign.get({ plain: true });
                        promocodeCampaignItem.classes = [];
                        promocodeCampaignItem.promocodes = [];
                        promocodeCampaignAlreadyPublished = promocodeCampaign.status === PromocodeCampaign.constants().STATUS_ACTIVE;
                        return self.promocodeCampaignCheckStatus(params.status, promocodeCampaign, next);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Map data for promocode campaign and application list, which uses this promocode campaign
                function (next) {
                    try {
                        AutoMapper.mapDefined(promocodeCampaignItem, promocodeCampaignItem)
                            .forMember('campaignName', 'name');
                        if (_.has(promocodeCampaignItem, 'startDate')) {
                            promocodeCampaignItem.startDate = DateUtils.isoPublish(promocodeCampaignItem.startDate);
                        }
                        if (_.has(promocodeCampaignItem, 'endDate')) {
                            promocodeCampaignItem.endDate = DateUtils.isoPublish(promocodeCampaignItem.endDate);
                        }
                        return next();
                    } catch (ex) {
                        return next(ex);
                    }
                },
                // Map data for promocode class templates and promocode templates
                function (next) {
                    return Promocode.findAll({
                        where: { promocodeCampaignId: params.id },
                        orderBy: [
                            {
                                field: 'promocodeCampaignId',
                                direction: 'ASC'
                            },
                            {
                                field: 'id',
                                direction: 'ASC'
                            }
                        ]
                    }).then(function (promocodes) {
                        _.forEach(promocodes, function (promocode) {
                            var promocodePlain = promocode.get({ plain: true });
                            var classDefinition = {
                                code: promocodePlain.code,
                                number: promocodePlain.number,
                                isUnique: promocodePlain.isUnique === 1,
                                isQRCode: promocodePlain.isQRCodeRequired === 1,
                            };
                            var promocodeClass = _.find(promocodeCampaignItem.classes, classDefinition);
                            if (!promocodeClass) {
                                promocodeClass = _.clone(classDefinition);
                                promocodeCampaignItem.classes.push(promocodeClass);
                            }
                            promocodeCampaignItem.promocodes.push({
                                code: promocodePlain.code,
                                class: promocodeClass,
                                numberOfUses: _.isInteger(promocodePlain.numberOfUses) && promocodePlain.numberOfUses > 1 ? promocodePlain.numberOfUses : 1,
                                expirationDate: promocodeCampaignItem.endDate,
                                generationDate: DateUtils.isoPublish(),
                                moneyValue: promocodeCampaignItem.money,
                                creditValue: promocodeCampaignItem.credits,
                                bonuspointsValue: promocodeCampaignItem.bonuspoints,
                                promocodeCampaignId: promocodeCampaignItem.id
                            });
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
                var promocodeCampaignPublish = AutoMapper.mapDefinedBySchema('promocodeCampaignPublishModel', INSTANCE_MAPPINGS, promocodeCampaignItem);
                logger.info('PromocodeApiFactory.promocodeCampaignGetForPublish', JSON.stringify(promocodeCampaignPublish));
                return CrudHelper.callbackSuccess({
                    promocodeCampaign: promocodeCampaignPublish,
                    alreadyPublished: promocodeCampaignAlreadyPublished
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    promocodeCampaignPublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var promocodeCampaign;
            var promocodeCampaignAlreadyPublished = false;
            var sessionTenantId;
            async.series([
                // Get promocode campaign data for publishing
                function (next) {
                    try {
                        params.status = PromocodeCampaign.constants().STATUS_ACTIVE;
                        return self.promocodeCampaignGetForPublish(params, message, clientSession, function (err, promocodeCampaignPublish) {
                            if (err) {
                                return next(err);
                            }
                            promocodeCampaignAlreadyPublished = promocodeCampaignPublish.alreadyPublished;
                            promocodeCampaign = promocodeCampaignPublish.promocodeCampaign;
                            promocodeCampaign.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('Get promocode campaign data for publishing ex: ',ex);
                        return setImmediate(next, ex);
                    }
                },
                // Publish promocode campaign
                function (next) {
                    try {
                        if (promocodeCampaignAlreadyPublished) {
                            return setImmediate(next);
                        }
                        return AerospikePromocodeCampaign.publish(promocodeCampaign, next);
                    } catch (ex) {
                        logger.error('Publish promocode campaign ex: ',ex);
                        return setImmediate(next, ex);
                    }
                },
                // Publish promocode classes used by this promocode campaign
                function (next) {
                    try {
                        return AerospikePromocodeClass.publish(promocodeCampaign, next);
                    } catch (ex) {
                        logger.error('Publish promocode classes used by this promocode campaign ex: ',ex);
                        return setImmediate(next, ex);
                    }
                },
                // Add promocode campaign to list of appications, which are using this promocode campaign
                function (next) {
                    try {
                        if (promocodeCampaignAlreadyPublished) {
                            return setImmediate(next);
                        }
                        return AerospikePromocodeCampaignList.publish(promocodeCampaign, next);
                    } catch (ex) {
                        logger.error('Add promocode campaign to list of appications ex: ',ex);
                        return setImmediate(next, ex);
                    }
                },
                // Publish promocodes and its instances
                function (next) {
                    try {
                        return AerospikePromocodeCounter.publish(promocodeCampaign, next);
                    } catch (ex) {
                        logger.error('Publish promocodes and its instances ex: ',ex);
                        return setImmediate(next, ex);
                    }
                },
                // Set promocodeCampaign deployment status
                function (next) {
                    try {
                        if (promocodeCampaignAlreadyPublished) {
                            return setImmediate(next);
                        }
                        promocodeCampaignParams = {};
                        promocodeCampaignParams.id = params.id;
                        promocodeCampaignParams.status = PromocodeCampaign.constants().STATUS_ACTIVE;
                        promocodeCampaignParams.skipCheck = true;
                        return self.promocodeCampaignSetStatus(promocodeCampaignParams, next);
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

    promocodeCampaignUnpublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var promocodeCampaign;
            async.series([
                // Get promocodeCampaign data for publishing
                function (next) {
                    try {
                        params.status = PromocodeCampaign.constants().STATUS_INACTIVE;
                        return self.promocodeCampaignGetForPublish(params, message, clientSession, function (err, promocodeCampaignPublish) {
                            if (err) {
                                return next(err);
                            }
                            promocodeCampaign = promocodeCampaignPublish;
                            promocodeCampaign.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Unpublish promocode campaign
                function (next) {
                    try {
                        return AerospikePromocodeCampaign.unpublish(promocodeCampaign, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Unpublish promocode classes used by this promocode campaign
                function (next) {
                    try {
                        return AerospikePromocodeClass.unpublish(promocodeCampaign, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Remove promocode campaign from list of appications, which are using this promocode campaign
                function (next) {
                    try {
                        return AerospikePromocodeCampaignList.publish(promocodeCampaign, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Unpublish promocodes and its instances
                function (next) {
                    try {
                        return AerospikePromocodeCounter.unpublish(promocodeCampaign, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Reset promocodeCampaign deployment status
                function (next) {
                    try {
                        var promocodeCampaignParams = {};
                        promocodeCampaignParams.id = params.id;
                        promocodeCampaignParams.status = PromocodeCampaign.constants().STATUS_INACTIVE;
                        promocodeCampaignParams.skipCheck = true;
                        return self.promocodeCampaignSetStatus(promocodeCampaignParams, next);
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

    promocodeCampaignGenerateFromFile: function (params, message, clientSession, callback) {
        var self = this;
        var promocodesToImport = [];
        var mapBySchema = 'promocodeCreateModel';
        var sessionTenantId;
        try {
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
                // First check if promocode campaign exist by tenant
                function (next) {
                    return PromocodeCampaign.count({ where: { id: params.id, tenantId: sessionTenantId } }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Get promocode codes and their parameters
                function (next) {
                    try {
                        return PromocodeService.getCodes(params.id, params.key, function (err, codes) {
                            if (err || !codes || codes.length === 0) {
                                return next(Errors.PromocodeApi.PromocodeCampaignCodesAreNotAvailable);
                            }
                            try {
                                _.forEach(codes, function (code) {
                                    var promocodeToImport = {
                                        promocodeCampaignId: params.id,
                                        number: code.number,
                                        isUnique: code.isUnique,
                                        code: code.code,
                                        isQRCodeRequired: code.isQRCodeRequired,
                                        usage: code.numberOfUses > 1 ? Promocode.constants().USAGE_MULTI_PER_PLAYER : Promocode.constants().USAGE_ONCE_PER_PLAYER,
                                        numberOfUses: code.numberOfUses
                                    };
                                    // Code must be unique per record
                                    var exist = _.find(promocodesToImport, {
                                        code: promocodeToImport.code
                                    });
                                    if (exist) {
                                        return next(Errors.PromocodeApi.AlreadyExist);
                                    }
                                    var promocodeItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, promocodeToImport, true);
                                    promocodeItem.createDate = _.now();
                                    promocodesToImport.push(promocodeItem);
                                });
                                return next();
                            } catch (ex) {
                                return setImmediate(next, ex);
                            }
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Bulk insert promocodes for campaign
                function (next) {
                    try {
                        return Promocode.bulkCreate(promocodesToImport).then(function (records) {
                            if (records.length !== promocodesToImport.length) {
                                return next(Errors.QuestionApi.FatalError);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish promocodes in a regular way
                function (next) {
                    try {
                        return self.promocodeCampaignPublish(params, message, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete codes from S3
                function (next) {
                    try {
                        return PromocodeService.deleteCodes(params.key, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
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

    promocodeCreate: function (params, message, clientSession, callback) {
        var self = this;
        var mapBySchema = 'promocodeCreateModel';
        try {
            return async.series([
                // Get tenantId from global session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return CrudHelper.callbackError(err, next);
                        }
                        params.tenantId = tenantId;
                        return CrudHelper.callbackSuccess(null, next);
                    });
                },
                // Check first if promocode campaign exist
                function (next) {
                    return PromocodeCampaign.findOne({ where: { id: params.promocodeCampaignId, tenantId: params.tenantId } }).then(function (promocodeCampaign) {
                        if (!promocodeCampaign) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                        alreadyActivated = promocodeCampaign.status === PromocodeCampaign.constants().STATUS_ACTIVE || promocodeCampaign.status === PromocodeCampaign.constants().STATUS_DIRTY;
                        return CrudHelper.callbackSuccess(null, next);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Check if code already exist
                function (next) {
                    let codesMax = params.isUnique=== 1 ? params.number : 1;
                    logger.debug('Check if code already exist codesMax=', codesMax);
                    let codesCount = 0;
                    let randomCodes=false;
                    if (params.code)
                        params.number = 1;
                    if (!params.code)
                        randomCodes=true;
                    for (var i = 1; i <= codesMax; i++) {
                        let code;
                        if (randomCodes) {
                            code = PromocodeService.generateRandom(params.promocodeCampaignId);
                            logger.debug('Create promocode Random code = ',code);
                        } else {
                            code=params.code;
                        }
                        Promocode.count({
                            where: {
                                id: params.promocodeCampaignId,
                                code: code
                            }
                        })
                            .then(function (count) {
                                if (count > 0) {
                                    logger.error('PromocodeApiFactory.promocodeCreate: already exist', params.promocodeCampaignId, code);
                                    return next(Errors.PromocodeApi.AlreadyExist);
                                }

                                // Create promocode entry
                                var promocodeItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, Object.assign(params, {code: code}), true);
                                promocodeItem.createDate = _.now();
                                logger.debug('Create promocode entry ',code);
                                Promocode.create(promocodeItem)
                                    .then(function (result) {
                                        if (++codesCount === codesMax) {
                                            logger.debug('Create promocode entry all done');
                                            return next();
                                        }

                                    }).catch(function (err) {
                                        logger.error('Create promocode entry err:', err);
                                    return next(err);
                                    });
                            }).catch(function (err) {
                                logger.error('Check if code already exist err:', err);
                                return next(err);
                            });
                    }
                },
                // Reactivate promocode campaign if needed
                function (next) {
                    try {
                        if (!alreadyActivated) {
                            return setImmediate(next);
                        }
                        return self.promocodeCampaignPublish({ id: params.promocodeCampaignId }, message, clientSession, next);
                    } catch (ex) {
                        logger.error('Reactivate promocode campaign if needed ex:', ex);
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
            logger.error('promocodeCreate ex:', ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    promocodeList: function (params, message, clientSession, callback) {
        try {
            var result;
            return async.series([
                // Get tenantId from global session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        params.tenantId = tenantId;
                        return next();
                    });
                },
                // Find promocodes for give promocode campaign and tenantId
                function (next) {
                    var include = CrudHelper.include(Promocode, [
                        PromocodeCampaign
                    ]);
                    include = CrudHelper.includeWhere(include, PromocodeCampaign, { tenantId: params.tenantId }, true);
                    return Promocode.findAndCountAll({
                        where: { promocodeCampaignId: params.promocodeCampaignId },
                        include: include,
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                    }).then(function (promocodes) {
                        var promocodeItems = AutoMapper.mapListDefinedBySchema('promocodeModel', INSTANCE_MAPPINGS, promocodes.rows);
                        result = {
                            items: promocodeItems,
                            limit: params.limit,
                            offset: params.offset,
                            total: promocodes.count,
                        };
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(result, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    promocodeInstanceList: function (params, message, clientSession, callback) {
        try {
            var total = 0;
            var limit = params.limit;
            var offset = params.offset;
            var promocodeInstances = [];
            return async.series([
                // Get total used amount of promocode, setup range for aerospike list query
                function (next) {
                    return AerospikePromocodeCounter.getUsedAmount(params, function (err, amount) {
                        if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                            return next(err);
                        }
                        if (err === Errors.DatabaseApi.NoRecordFound) {
                            return next();
                        }
                        total = amount;
                        if (params.limit === 0) {
                            offset = 0;
                            limit = amount;
                        } else {
                            limit = Math.min(limit, amount);
                        }
                        return next();
                    });
                },
                // Find all instances for given code and within range: offset .. min(limit/amount)
                function (next) {
                    if (total === 0) {
                        return setImmediate(next, null);
                    }
                    return AerospikePromocodeCounter.findAll({ code: params.code, limit: limit, offset: offset }, function (err, instances) {
                        if (err) {
                            return next(err);
                        }
                        promocodeInstances = instances;
                        return next();
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var promocodeInstanceItems = AutoMapper.mapListDefinedBySchema('promocodeInstanceModel', INSTANCE_MAPPINGS, promocodeInstances);
                var result = {
                    items: promocodeInstanceItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                };
                return CrudHelper.callbackSuccess(result, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

}
