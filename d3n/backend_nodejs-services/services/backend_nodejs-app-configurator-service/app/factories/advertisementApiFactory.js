var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Advertisement = Database.RdbmsService.Models.AdvertisementManager.Advertisement;
var AdvertisementProvider = Database.RdbmsService.Models.AdvertisementManager.AdvertisementProvider;
var AdvertisementProviderHasRegionalSetting = Database.RdbmsService.Models.AdvertisementManager.AdvertisementProviderHasRegionalSetting;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var AerospikeAdvertisementCounter = KeyvalueService.Models.AerospikeAdvertisementCounter;
var PublishService = require('../services/advertisementService.js');
var TenantService = require('../services/tenantService.js');
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'advertisementModel': [
        {
            destination: '$root',
            model: Advertisement
        }
    ],
    'advertisementCreateModel': [
        {
            destination: '$root',
            model: Advertisement
        }
    ],
    'advertisementProviderModel': [
        {
            destination: '$root',
            model: AdvertisementProvider
        },
        {
            destination: 'regionalSettingsIds',
            model: AdvertisementProviderHasRegionalSetting,
            attribute: AdvertisementProviderHasRegionalSetting.tableAttributes.regionalSettingId
        }
    ],
    'advertisementProviderCreateModel': [
        {
            destination: '$root',
            model: AdvertisementProvider
        },
        {
            destination: 'regionalSettingsIds',
            model: AdvertisementProviderHasRegionalSetting,
            attribute: AdvertisementProviderHasRegionalSetting.tableAttributes.regionalSettingId
        }
    ],
    'advertisementPublishModel': [
        {
            destination: '$root',
            model: Advertisement
        }
    ]
};
INSTANCE_MAPPINGS['advertisementUpdateModel'] = INSTANCE_MAPPINGS['advertisementCreateModel'];
INSTANCE_MAPPINGS['advertisementProviderUpdateModel'] = INSTANCE_MAPPINGS['advertisementProviderCreateModel'];

function isAdvertisementPublished(advertisement) {
    return (_.isNumber(advertisement.publishIdx) && advertisement.publishIdx >= 1);
}

module.exports = {

    advertisementUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var advertisementId = undefined;
            var mapBySchema = 'advertisementCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                advertisementId = params.id;
                mapBySchema = 'advertisementUpdateModel';
            }
            var advertisementItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var dbAdvertisment;
            async.series([
                // Create/update main entity
                function (next) {
                    if (create) {
                        return Advertisement.create(advertisementItem).then(function (advertisement) {
                            advertisementId = advertisement.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Advertisement.update(advertisementItem, { where: { id: advertisementItem.id }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                
                // publish if needed
                function (next) {
                    if (create === false) {
                        return self.advertisementGetForPublishUnpublish({ id: advertisementId }, message, clientSession, function (err, item) {
                            if (err) {
                                return next(err);
                            }
                            
                            // if already published, re-publish
                            if (isAdvertisementPublished(item)) {
                                self.advertisementPublish({ id: advertisementId }, message, clientSession, next);
                            } else {
                                return next();
                            }
                        });
                    } else {
                        return setImmediate(next);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = advertisementId;
                return self.advertisementGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set Advertisement status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    advertisementSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== Advertisement.constants().STATUS_ACTIVE &&
            params.status !== Advertisement.constants().STATUS_INACTIVE &&
            params.status !== Advertisement.constants().STATUS_DIRTY &&
            params.status !== Advertisement.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current status
                function (next) {
                    return Advertisement.findOne({ where: { id: params.id } }).then(function (advertisement) {
                        if (!advertisement) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Only inactive (but not archived) advertisements can be archived/activated
                        if (params.status === Advertisement.constants().STATUS_ARCHIVED || params.status === Advertisement.constants().STATUS_ACTIVE) {
                            if (advertisement.status === Advertisement.constants().STATUS_ARCHIVED) {
                                return next(Errors.AdvertisementApi.AdvertisementIsArchived);
                            }
                        }
                        // Only active advertisements can be deactivated
                        if (params.status === Advertisement.constants().STATUS_INACTIVE) {
                            if (advertisement.status === Advertisement.constants().STATUS_INACTIVE) {
                                return next(Errors.AdvertisementApi.AdvertisementIsDeactivated);
                            }
                            if (advertisement.status === Advertisement.constants().STATUS_ARCHIVED) {
                                return next(Errors.AdvertisementApi.AdvertisementIsArchived);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return Advertisement.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
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

    advertisementDelete: function (params, callback) {
        try {
            return Advertisement.findOne({
                where: { id: params.id }
            }).then(function (item) {
                if (!item) {
                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                }
                if (isAdvertisementPublished(item.get({plain: true}))) {
                    return CrudHelper.callbackError(Errors.AdvertisementApi.ContentIsPublished, callback);
                } else {
                    Advertisement.destroy({ where: { id: params.id } }).then(function (count) {
                        if (count === 0) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                        }
                        return CrudHelper.callbackSuccess(null, callback);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, callback);
                    });
                }
            }).catch(function (err) {
                return CrudHelper.callbackError(err, next);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    advertisementGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'advertisement';
            var mapBySchema = 'advertisementModel';
            var attributes = _.keys(Advertisement.attributes);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var include = CrudHelper.include(Advertisement, [
                    AdvertisementProvider
                ]);
                include = CrudHelper.includeWhere(include, AdvertisementProvider, { tenantId: tenantId }, true);
                return Advertisement.findOne({
                    where: { id: params.id },
                    attributes: attributes,
                    include: include,
                    subQuery: false
                }).then(function (advertisement) {
                    if (!advertisement) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var advertisementItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, advertisement);
                    response[responseItem] = advertisementItem;
                    return CrudHelper.callbackSuccess(response, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    advertisementList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'advertisementModel';
            var attributes = _.keys(Advertisement.attributes);
            var orderBy = CrudHelper.orderBy(params, Advertisement);
            var searchBy = CrudHelper.searchBy(params, Advertisement);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var include = CrudHelper.include(Advertisement, [
                    AdvertisementProvider
                ]);
                include = CrudHelper.includeWhere(include, AdvertisementProvider, { tenantId: tenantId }, true);
                return Advertisement.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy,
                    include: include,
                    distinct: true,
                    subQuery: false
                }).then(function (advertisement) {
                    var advertisementItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, advertisement.rows);
                    return CrudHelper.callbackSuccess({
                        items: advertisementItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: advertisement.count
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    advertisementGetForPublishUnpublish: function (params, message, clientSession, callback) {
        try {
            var advertisement;
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
                // Get advertisement record by tenant
                function (next) {
                    return Advertisement.findOne({
                        where: { id: params.id },
                        include: { model: AdvertisementProvider, where: { tenantId: sessionTenantId } }
                    }).then(function (advertisementItem) {
                        if (!advertisementItem) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (advertisementItem.status === Advertisement.constants().STATUS_ARCHIVED) {
                            return next(Errors.AdvertisementApi.AdvertisementIsArchived);
                        }
                        advertisement = advertisementItem;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                try {
                    var advertisementItem = advertisement.get({ plain: true });
                    logger.info('AdvertisementApiFactory.advertisementGetForPublishUnpublish', JSON.stringify(advertisementItem));
                    return CrudHelper.callbackSuccess(advertisementItem, callback);
                } catch (ex) {
                    return CrudHelper.callbackError(ex, callback);
                }
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
        
    },
    
    advertisementPublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        var publishIdx = undefined;
        var advertisementId = params.id;
        var alreadyPublished = false;
        try {
            var advertisement;
            async.series([

                function (next) {
                    try {
                        return self.advertisementGetForPublishUnpublish(params, message, clientSession, function (err, advertisementPublish) {
                            if (err) {
                                return next(err);
                            }
                            advertisement = advertisementPublish;
                            if (isAdvertisementPublished(advertisement)) {
                                alreadyPublished = true;
                            }
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        if (alreadyPublished) {
                            return setImmediate(next);
                        }
                        return AerospikeAdvertisementCounter.increment(advertisement, function (err, val) {
                            if (err) {
                                return next(err);
                            }
                            publishIdx = val;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        if (alreadyPublished) {
                            return setImmediate(next);
                        }
                        return Advertisement.update({publishIdx: publishIdx}, { where: { id: advertisementId }, individualHooks: true }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            advertisement.publishIdx = publishIdx;
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
                        return PublishService.publish(advertisement, function () {}, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        params.status = Advertisement.constants().STATUS_ACTIVE;
                        return self.advertisementSetStatus(params, next);
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

    advertisementUnpublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var advertisementToUnpublish;
            var adWithHighestPublishIdx;
            async.series([

                function (next) {
                    return self.advertisementGetForPublishUnpublish(params, message, clientSession, function (err, item) {
                        if (err) {
                            return next(err);
                        }
                        if (!isAdvertisementPublished(item)) {
                            logger.warn('Trying to unpublih non published ad:', item);
                            return next(Errors.QuestionApi.ValidationFailed);
                        }
                        return next();
                    });
                },

                function (next) {
                    try {
                        var sequelize = Advertisement.sequelize;

                        return sequelize.transaction(function (t) {
                            var options = {
                                where: { id: params.id },
                                attributes: _.keys(Advertisement.attributes),
                                transaction: t
                            };

                            return Advertisement.findOne(options)
                            .then(function (advertisement) {
                                if (!advertisement) {
                                    throw new Error(Errors.DatabaseApi.NoRecordFound);
                                }
                                
                                advertisementToUnpublish = advertisement.get({ plain: true });
                        
                                // get the ad with highest publish idx
                                var options = {
                                    where: {
                                        advertisementProviderId: advertisementToUnpublish.advertisementProviderId,
                                        publishIdx: {
                                            $not: null
                                        }
                                    },
                                    attributes: _.keys(Advertisement.attributes),
                                    order: 'publishIdx DESC',
                                    limit: 1,
                                    transaction: t
                                };

                                return Advertisement.findOne(options);
                            })
                            .then(function (item) {
                                adWithHighestPublishIdx = item.get({ plain: true });
                        
                                // set publishIdx=null
                                var options = {
                                    where: { id: advertisementToUnpublish.id },
                                    individualHooks: true,
                                    transaction: t
                                };
                                return Advertisement.update({publishIdx: null}, options);
                            })
                            .then (function () {
                                if (adWithHighestPublishIdx.publishIdx === advertisementToUnpublish.publishIdx) {
                                    return;
                                }
                                var options = {
                                    where: { id: adWithHighestPublishIdx.id },
                                    individualHooks: true,
                                    transaction: t
                                };
                                return Advertisement.update({publishIdx: advertisementToUnpublish.publishIdx}, options);
                            });

                        }).then(function () {
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                // decrement aerospike counter
                function (next) {
                    try {
                        return AerospikeAdvertisementCounter.decrement(advertisementToUnpublish, function (err, val) {
                            if (err) {
                                return next(err);
                            }
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                
                function (next) {
                    try {
                        return PublishService.unpublish(advertisementToUnpublish.advertisementProviderId, advertisementToUnpublish.publishIdx, adWithHighestPublishIdx.publishIdx, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        params.status = Advertisement.constants().STATUS_INACTIVE;
                        return self.advertisementSetStatus(params, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
                
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }        
    },

    advertisementProviderGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'advertisementProvider';
            var mapBySchema = 'advertisementProviderModel';
            var attributes = _.keys(AdvertisementProvider.attributes);
            var include = CrudHelper.include(AdvertisementProvider, [
                AdvertisementProviderHasRegionalSetting
            ]);
            var where = {id: params.id};
            async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if(err) {
                            return next(err);
                        }
                        where.tenantId = tenantId;
                        return next();
                    });
                },
                // Get advertisement provider record
                function (next) {
                    AdvertisementProvider.findOne({
                        where: where,
                        attributes: attributes,
                        include: include
                    }).then(function (advertisementProvider) {
                        if (!advertisementProvider) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        var advertisementProviderItem  = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, advertisementProvider);
                        response[responseItem] = advertisementProviderItem;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(response, callback);
            });
            
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    advertisementProviderList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'advertisementProviderModel';
            var attributes = _.keys(AdvertisementProvider.attributes);
            var orderBy = CrudHelper.orderBy(params, AdvertisementProvider);
            var searchBy = CrudHelper.searchBy(params, AdvertisementProvider);
            var advertisementProviderItems = [];
            var total = 0;
            async.series([
                // add tenantId filter
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if(err) {
                            return next(err);
                        }
                        searchBy.tenantId = tenantId;
                        return next();
                    });
                },
                
                // Populate total count and regional setting ids (involve base model and nested models only with defined search criterias)
                function (next) {
                    var include = CrudHelper.includeDefined(AdvertisementProvider, [
                        AdvertisementProviderHasRegionalSetting
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return AdvertisementProvider.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (advertisementProviders) {
                        total = advertisementProviders.count;
                        if (!_.has(searchBy, 'id')) {
                            var advertisementProvidersIds = [];
                            _.forEach(advertisementProviders.rows, function (advertisementProvider) {
                                advertisementProvidersIds.push(advertisementProvider.id);
                            });
                            searchBy = {
                                id: { '$in': advertisementProvidersIds }
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
                        return next();
                    }
                    var include = CrudHelper.include(AdvertisementProvider, [
                        AdvertisementProviderHasRegionalSetting
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return AdvertisementProvider.findAll({
                        where: searchBy,
                        order: orderBy,
                        attributes: attributes,
                        include: include,
                        subQuery: false
                    }).then(function (advertisementProviders) {
                        advertisementProviderItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, advertisementProviders);
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
                    items: advertisementProviderItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    advertisementProviderUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var advertisementProviderId = undefined;
            var mapBySchema = 'advertisementProviderCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                advertisementProviderId = params.id;
                mapBySchema = 'advertisementProviderUpdateModel';
            }
            var advertisementProviderAssociates = undefined;
            var advertisementProviderItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var sessionTenantId = undefined;
            async.series([
                // get tenantId from global session
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
                        advertisementProviderItem.tenantId = sessionTenantId;
                        return AdvertisementProvider.create(advertisementProviderItem).then(function (advertisementProvider) {
                            advertisementProviderId = advertisementProvider.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        var options = {
                            where: {
                                id: advertisementProviderItem.id,
                                tenantId: sessionTenantId
                            }
                        };
                        
                        AdvertisementProvider.update(advertisementProviderItem, options).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // Populate advertisement associated entity values
                function (next) {
                    try {
                        advertisementProviderAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'advertisementProviderId',
                                value: advertisementProviderId
                            }
                        );
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(advertisementProviderAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(advertisementProviderAssociates, function (itemAssociate, remove) {
                        //if (itemAssociate.values.length === 0) {
                        //    return remove();
                        //}
                        itemAssociate.model.destroy({ where: { advertisementProviderId: advertisementProviderId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(advertisementProviderAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(advertisementProviderAssociates, function (itemAssociate, create) {
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
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = advertisementProviderId;
                return self.advertisementProviderGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set AdvertisementProvider status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    advertisementProviderSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== AdvertisementProvider.constants().STATUS_ACTIVE &&
            params.status !== AdvertisementProvider.constants().STATUS_INACTIVE &&
            params.status !== AdvertisementProvider.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current deployment status
                function (next) {
                    return Advertisement.count({ where: { advertisementProviderId: params.id, status: Advertisement.constants().STATUS_ACTIVE } }).then(function (count) {
                        // Only undeployed and inactive (but not archived) advertisement providers can be archived/activated
                        if (params.status === AdvertisementProvider.constants().STATUS_ARCHIVED || params.status === AdvertisementProvider.constants().STATUS_ACTIVE) {
                            if (count > 0) {
                                return next(Errors.AdvertisementApi.AdvertisementIsDeployed);
                            }
                        }
                        // Only undeployed and active advertisement providers can be deactivated
                        if (params.status === AdvertisementProvider.constants().STATUS_INACTIVE) {
                            if (count > 0) {
                                return next(Errors.AdvertisementApi.AdvertisementIsDeployed);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Check current advertisement provider status
                function (next) {
                    return AdvertisementProvider.findOne({ where: { id: params.id } }).then(function (advertisementProvider) {
                        if (!advertisementProvider) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Only undeployed and inactive (but not archived) advertisement providers can be archived/activated
                        if (params.status === AdvertisementProvider.constants().STATUS_ARCHIVED || params.status === AdvertisementProvider.constants().STATUS_ACTIVE) {
                            if (advertisementProvider.status === AdvertisementProvider.constants().STATUS_ARCHIVED) {
                                return next(Errors.AdvertisementApi.AdvertisementProviderIsArchived);
                            }
                        }
                        // Only undeployed and active advertisement providers can be deactivated
                        if (params.status === AdvertisementProvider.constants().STATUS_INACTIVE) {
                            if (advertisementProvider.status === AdvertisementProvider.constants().STATUS_INACTIVE) {
                                return next(Errors.AdvertisementApi.AdvertisementProviderIsDeactivated);
                            }
                            if (advertisementProvider.status === AdvertisementProvider.constants().STATUS_ARCHIVED) {
                                return next(Errors.AdvertisementApi.AdvertisementProviderIsArchived);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return AdvertisementProvider.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
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

    advertisementProviderDelete: function (params, callback) {
        try {
            return AdvertisementProvider.destroy({ where: { id: params.id } }).then(function (count) {
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
    
    // Comment out code per #5458 - discussed with Maxim the connection between an
    // advertisementProvider and a tenant may not be required. Code was not unit tested.
    /*
    advertisementProviderAddRemoveTenant: function (params, callback) {
        var self = this;

        try {
            async.series([
                function (next) {
                    try {
                        if (_.size(_.union(params.tenantsToAdd, params.tenantsToRemove)) !== _.size(params.tenantsToAdd) + _.size(params.tenantsToRemove)) {
                            return CrudHelper.callbackError('ERR_VALIDATION_FAILED', callback);
                        }
                        return next();
                    } catch (ex) {
                        logger.error('advertisementApiFactory.advertisementProviderAddRemoveTenant: validate', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Delete tenants
                function (next) {
                    try {
                        if (!_.isEmpty(params.tenantsToRemove)) {
                            return AdvertisementCampaignHasTenant.destroy({where: {
                                'campaignId': params.campaignId,
                                'tenantId': { '$in': params.tenantsToRemove }
                            }}).then(function (count) {
                                return next();
                            }).catch(function(err) {
                                logger.error('advertisementApiFactory.advertisementProviderAddRemoveTenant: removing tenants', params, err);
                                return CrudHelper.callbackError(err, next);
                            });
                        } else {
                            return next();
                        }
                    } catch (ex) {
                        logger.error('advertisementApiFactory.advertisementProviderAddRemoveTenant: generic error removing tenants', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Add tenants to campaign
                function (next) {
                    try {
                        if (!_.isEmpty(params.tenantsToAdd)) {
                            var newCampaignTenants = [];
                            _.forEach(params.tenantsToAdd, function (tenantId) {
                                newCampaignTenants.push({
                                    'campaignId': params.campaignId,
                                    'tenantId': tenantId
                                });
                            });

                            return AdvertisementCampaignHasTenant.bulkCreate(newCampaignTenants)
                            .then(function () {
                                return next();
                            })
                            .catch(function(err) {
                                logger.error('advertisementApiFactory.advertisementProviderAddRemoveTenant: creating tenants', newCampaignTenants, err);
                                return CrudHelper.callbackError(err, next);
                            });
                        } else {
                            return next();
                        }
                    } catch (ex) {
                        logger.error('advertisementApiFactory.advertisementProviderAddRemoveTenant: generic error creating tenants', ex);
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
    */

};
