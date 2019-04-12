var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var TombolaHasRegionalSetting = Database.RdbmsService.Models.TombolaManager.TombolaHasRegionalSetting
var ApplicationHasTombola = Database.RdbmsService.Models.Application.ApplicationHasTombola;
var Tombola = Database.RdbmsService.Models.TombolaManager.Tombola;
var Voucher = Database.RdbmsService.Models.VoucherManager.Voucher;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeTombola = KeyvalueService.Models.AerospikeTombola;
var AerospikeTombolaList = KeyvalueService.Models.AerospikeTombolaList;
let AerospikeVoucherTombolaReserve = KeyvalueService.Models.AerospikeVoucherTombolaReserve;
var TenantService = require('../services/tenantService.js');
var uuid = require('node-uuid');
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'tombolaModel': [
        {
            destination: '$root',
            model: Tombola
        },
        {
            destination: 'regionalSettingsIds',
            model: TombolaHasRegionalSetting,
            attribute: TombolaHasRegionalSetting.tableAttributes.regionalSettingId
        },
        {
            destination: 'applicationsIds',
            model: ApplicationHasTombola,
            attribute: ApplicationHasTombola.tableAttributes.applicationId
        }
    ],
    'tombolaCreateModel': [
        {
            destination: '$root',
            model: Tombola
        },
        {
            destination: 'regionalSettingsIds',
            model: TombolaHasRegionalSetting,
            attribute: TombolaHasRegionalSetting.tableAttributes.regionalSettingId
        },
        {
            destination: 'applicationsIds',
            model: ApplicationHasTombola,
            attribute: ApplicationHasTombola.tableAttributes.applicationId
        }
    ],
    'tombolaPublishModel': [
        {
            destination: '$root',
            model: Tombola
        },
        {
            destination: 'applicationsIds',
            model: ApplicationHasTombola,
            attribute: ApplicationHasTombola.tableAttributes.applicationId
        },
        {
            destination: 'regionalSettingsIds',
            model: TombolaHasRegionalSetting,
            attribute: TombolaHasRegionalSetting.tableAttributes.regionalSettingId
        },
    ],
};
INSTANCE_MAPPINGS['tombolaUpdateModel'] = INSTANCE_MAPPINGS['tombolaCreateModel'];

module.exports = {

    tombolaUpdate: function (params, message, clientSession, callback) {
        if (_.has(params, 'startDate') && !_.isUndefined(params.startDate) && !_.isNull(params.startDate) &&
            _.has(params, 'endDate') && !_.isUndefined(params.endDate) && !_.isNull(params.endDate) &&
            new Date(params.startDate).getTime() >= new Date(params.endDate).getTime() ) {
            logger.error("tombolaApiFactory:tombolaUpdate: validation failed ", JSON.stringify(params));
            return CrudHelper.callbackError(Errors.TombolaApi.ValidationFailed, callback);
        }
        var currentDate = new Date();
        var self = this;
        try {
            var create = true;
            var tombolaId = undefined;
            var mapBySchema = 'tombolaCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                tombolaId = params.id;
                mapBySchema = 'tombolaUpdateModel';
            }
            if (params.consolationPrize.amount === null) {
                params.consolationPrize=null;
            }
            var tombolaItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var sessionTenantId;
            var tombolaItemAssociates = undefined;
            var needSchedule = false;
            async.series([
                // Get tenantId from global session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Don't allow to update whole parameters if tombola open checkout event executed
                function (next) {
                    try {
                        if (create) {
                            if (params.endDate && params.playoutTarget === Tombola.constants().PAYOUT_TARGET_TARGET_DATE && !params.targetDate) {
                                try {
                                    var targetDate = Date.parse(params.endDate.substring(0, 19));
                                    targetDate = new Date(targetDate + 1 * 60000);
                                    tombolaItem.targetDate = DateUtils.isoDate(targetDate.getTime());
                                } catch (ex) {
                                    logger.error('tombolaApiFactory.tombolaUpdate: targetDate setup failed', ex);
                                    throw new Error(Errors.QuestionApi.ValidationFailed);
                                }
                            }
                            
                            if (!_.isInteger(tombolaItem.preCloseOffsetMinutes)) {
                                tombolaItem.preCloseOffsetMinutes = Config.defaultPreCloseOffsetMinutes;
                            }
                            
                            return setImmediate(next);
                        }
                        return Tombola.findOne({ where: { id: tombolaId } }).then(function (tombola) {
                            if (!tombola) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            if (params.playoutTarget === Tombola.constants().PAYOUT_TARGET_TARGET_DATE && !params.targetDate) {
                                try {
                                    var targetDate = tombola.endDate.getTime();
                                    if (params.endDate) {
                                        var targetDate = Date.parse(params.endDate.substring(0, 19));
                                    }
                                    targetDate = new Date(targetDate + 1 * 60000);
                                    tombolaItem.targetDate = DateUtils.isoDate(targetDate.getTime());
                                } catch (ex) {
                                    logger.error('tombolaApiFactory.tombolaUpdate: targetDate setup failed', ex);
                                    throw new Error(Errors.QuestionApi.ValidationFailed);
                                }
                            }
                            try {
                                var isOpenCheckout = tombola.get({ plain: true }).isOpenCheckout === 1;
                                if (isOpenCheckout) {
                                    params.startDate=tombola.startDate;
                                    var valid = true;
                                    if (_.has(params, 'endDate') && params.endDate <= currentDate) {
                                        valid = false;
                                    }
                                    if (_.has(params, 'targetDate') && params.targetDate <= currentDate) {
                                        valid = false;
                                    }
                                    if (_.has(params, 'percentOfTicketsAmount') && params.percentOfTicketsAmount <= tombola.percentOfTicketsAmount) {
                                        valid = false;
                                    }
                                    if (_.has(params, 'totalTicketsAmount') && params.totalTicketsAmount < tombola.totalTicketsAmount && params.totalTicketsAmount>-1) {
                                        valid = false;
                                    }
                                    if (!valid) {
                                        return next(Errors.TombolaApi.TombolaIsOpenCheckout);
                                    }
                                    tombolaItem = AutoMapper.mapDefinedBySchema('tombolaUpdateCheckoutModel', INSTANCE_MAPPINGS, params, true);
                                }
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
                // Create/update main entity
                function (next) {
                    try {
                        if (_.has(tombolaItem, 'prizes') && _.isArray(tombolaItem.prizes)) {
                            _.forEach(tombolaItem.prizes, function (prize) {
                                prize.id = uuid.v4();
                            });
                        }
                        if (_.has(tombolaItem, 'consolationPrize') && _.isObject(tombolaItem.consolationPrize)) {
                            tombolaItem.consolationPrize.id = uuid.v4();
                        }
                        if (create) {
                            tombolaItem.createDate = _.now();
                            tombolaItem.tenantId = sessionTenantId;
                            tombolaItem.creatorResourceId = clientSession.getUserId();
                            return Tombola.create(tombolaItem).then(function (result) {
                                tombolaId = result.get({ plain: true }).id;
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        } else {
                            return Tombola.update(tombolaItem, { where: { id: tombolaItem.id, tenantId: sessionTenantId }, individualHooks: true }).then(function (count) {
                                if (count[0] === 0) {
                                    return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                                }
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        }
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Populate promocode campaign associated entity values
                function (next) {
                    try {
                        tombolaItemAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'tombolaId',
                                value: tombolaId,
                            }
                        );
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(tombolaItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(tombolaItemAssociates, function (itemAssociate, remove) {
                        //if (itemAssociate.values.length === 0) {
                        //    return remove();
                        //}
                        return itemAssociate.model.destroy({ where: { tombolaId: tombolaId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(tombolaItemAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(tombolaItemAssociates, function (itemAssociate, create) {
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
                // re-schedule tombola, if needed
                function (next) {
                    try {
                        if (create) {
                            return setImmediate(next);
                        }
                        return Tombola.findOne({ where: { id: tombolaId } }).then(function (dbTombola) {
                            try {
                                var tombola = dbTombola.get({plain: true});
                                if (tombola.startDate && 
                                    tombola.endDate && 
                                    tombola.targetDate  &&
                                    tombola.status === Tombola.constants().STATUS_ACTIVE) {
                                    return clientSession.getConnectionService()._schedulerService.scheduleTombola({
                                        id: tombola.id,
                                        startDate: tombola.startDate,
                                        endDate: tombola.endDate,
                                        targetDate: tombola.targetDate,
                                        preCloseOffsetMinutes: tombola.preCloseOffsetMinutes,
                                        eventsData: {
                                            tombolaId: tombola.id,
                                            tenantId: clientSession.getTenantId()
                                        }
                                    }, next);
                                }
                                return next();
                            } catch (e) {
                                return next(e);
                            }
                        }).catch (function (err) {
                            return next(err);
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = tombolaId;
                return self.tombolaGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set Tombola status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    tombolaSetStatus: function (params, clientSession, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== Tombola.constants().STATUS_ACTIVE &&
            params.status !== Tombola.constants().STATUS_INACTIVE &&
            params.status !== Tombola.constants().STATUS_DIRTY &&
            params.status !== Tombola.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.TombolaApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current status
                function (next) {
                    if (params.skipCheck) {
                        return setImmediate(next);
                    }
                    return Tombola.findOne({ where: { id: params.id } }).then(function (tombola) {
                        return self.tombolaCheckStatus(params.status, tombola, next);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return Tombola.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
                        if (count[0] === 0) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                        return CrudHelper.callbackSuccess(null, next);
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

    tombolaCheckStatus: function (status, tombola, callback) {
        var self = this;
        if (status !== Tombola.constants().STATUS_ACTIVE &&
            status !== Tombola.constants().STATUS_INACTIVE &&
            status !== Tombola.constants().STATUS_DIRTY &&
            status !== Tombola.constants().STATUS_ARCHIVED) {
            return CrudHelper.callbackError(Errors.TombolaApi.ValidationFailed, callback);
        }
        try {
            if (!tombola) {
                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
            }
            // Only inactive (but not archived) tombolas can be archived
            if (status === Tombola.constants().STATUS_ARCHIVED ) {
                if (tombola.status === Tombola.constants().STATUS_ARCHIVED) {
                    return CrudHelper.callbackError(Errors.TombolaApi.TombolaIsArchived, callback);
                }
            }
            // Only active tombolas can be deactivated
            if (status === Tombola.constants().STATUS_INACTIVE) {
                if (tombola.status === Tombola.constants().STATUS_INACTIVE) {
                    return CrudHelper.callbackError(Errors.TombolaApi.TombolaIsDeactivated, callback);
                }
                if (tombola.status === Tombola.constants().STATUS_ARCHIVED) {
                    return CrudHelper.callbackError(Errors.TombolaApi.TombolaIsArchived, callback);
                }
            }
            return CrudHelper.callbackSuccess(null, callback);
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tombolaSetOpenCheckout: function (params, clientSession, callback) {
        try {
            return async.series([
                // Check current status
                function (next) {
                    return Tombola.findOne({ where: { id: params.id } }).then(function (tombola) {
                        if (!tombola) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Archived tombolas doesn't allow to change open checkout flag
                        if (tombola.status === Tombola.constants().STATUS_ARCHIVED) {
                            return next(Errors.TombolaApi.TombolaIsArchived);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update open checkout flag
                function (next) {
                    return Tombola.update({ isOpenCheckout: 1 }, { where: { id: params.id } }).then(function (count) {
                        if (count[0] === 0) {
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                        return CrudHelper.callbackSuccess(null, next);
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

    tombolaDelete: function (params, clientSession, callback) {
        var self = this;
        try {
            async.mapSeries([
                TombolaHasRegionalSetting
            ], function (model, next) {
                try {
                    return model.destroy({ where: { tombolaId: params.id } }).then(function (count) {
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
                return Tombola.destroy({ where: { id: params.id } }).then(function (count) {
                    if (count === 0) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    
                    // unschedule tombola
                    clientSession.getConnectionService()._schedulerService.unscheduleTombola(params.id, function (errUnschedule) {
                        if (errUnschedule) {
                            return CrudHelper.callbackError(errUnschedule, callback);
                        }
                        return CrudHelper.callbackSuccess(null, callback);
                    });
                    
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tombolaBoost: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id') || !params.hasOwnProperty('boostSize')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var tombola = {};
        tombola.id = params.id;
        tombola.boostSize = params.boostSize;
        try {
            async.series([
                // Publish tombola
                function (next) {
                    try {
                        return AerospikeTombola.publishBoost(tombola, next);
                    } catch (ex) {
                        logger.error("tombolaBoost err1: ", ex,'"');
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    logger.error("tombolaBoost err2: ", err,'"');
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(null, callback);
            });
        } catch (ex) {
            logger.error("tombolaBoost err: ", ex,'"');
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tombolaGetBoost: function (params, message, clientSession, callback) {
        try {
            AerospikeTombola.findBoostOne(params, function (err, tombola) {
                if (err) {
                    logger.error("getTombolaBoost error: ", err);
                    return CrudHelper.callbackError(err, callback);
                }
                var response = {};
                response.id = tombola.id;
                response.boostSize = tombola.boostSize;
                return CrudHelper.callbackSuccess(response, callback);
            });

        } catch (ex) {
            logger.error("getTombolaBoost error: ", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tombolaGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'tombola';
            var mapBySchema = 'tombolaModel';
            var attributes = _.keys(Tombola.attributes);
            var include = CrudHelper.include(Tombola, [
                ApplicationHasTombola,
                TombolaHasRegionalSetting
            ]);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return Tombola.findOne({
                    where: { id: params.id, tenantId: tenantId},
                    attributes: attributes,
                    include: include
                }).then(function (tombola) {
                    if (!tombola) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var tombolaItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, tombola);
                    response[responseItem] = tombolaItem;
                    return CrudHelper.callbackSuccess(response, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tombolaList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'tombolaModel';
            var attributes = _.keys(Tombola.attributes);
            var orderBy = CrudHelper.orderBy(params, Tombola);
            var searchBy = CrudHelper.searchBy(params, Tombola);
            var tombolaItems = [];
            var total = 0;
            async.series([
                // Get tenantId from global session
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
                    var include = CrudHelper.includeDefined(Tombola, [
                        ApplicationHasTombola,
                        TombolaHasRegionalSetting
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return Tombola.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (tombolas) {
                        total = tombolas.count;
                        if (!_.has(searchBy, 'id')) {
                            var tombolasIds = [];
                            _.forEach(tombolas.rows, function (tombola) {
                                tombolasIds.push(tombola.id);
                            });
                            searchBy = {
                                id: { '$in': tombolasIds }
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
                    var include = CrudHelper.include(Tombola, [
                        ApplicationHasTombola,
                        TombolaHasRegionalSetting
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return Tombola.findAll({
                        where: searchBy,
                        order: orderBy,
                        attributes: attributes,
                        include: include,
                        subQuery: false
                    }).then(function (tombolas) {
                        tombolaItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, tombolas);
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
                    items: tombolaItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tombolaGetForPublish: function (params, message, clientSession, callback) {
        try {
            var tombolaPublish;
            var sessionTenantId;
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
                // Get tombola record by tenant
                function (next) {
                    var tombolaItem = {};
                    var attributes = _.keys(Tombola.attributes);
                    var include = CrudHelper.include(Tombola, [
                        ApplicationHasTombola,
                        TombolaHasRegionalSetting
                    ]);
                    return Tombola.findOne({
                        where: { id: params.id, tenantId: sessionTenantId },
                        attributes: attributes,
                        include: include
                    }).then(function (tombola) {
                        if (!tombola) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // if (tombola.status === Tombola.constants().STATUS_ARCHIVED) {
                        //     return next(Errors.TombolaApi.TombolaIsArchived);
                        // }
                        // Custom Tombola mapper object
                        var tombolaPlain = tombola.get({ plain: true });
                        // if (tombolaPlain.isOpenCheckout === 1) {
                        //     AutoMapper.mapDefined(tombolaPlain, tombolaItem)
                        //         .forMember('id')
                        //         .forMember('name')
                        //         .forMember('imageId');
                        //     tombolaItem.applicationHasTombolas = tombolaPlain.applicationHasTombolas;
                        //     tombolaItem.tombolaHasRegionalSettings = tombolaPlain.tombolaHasRegionalSettings;
                        // } else {
                            AutoMapper.mapDefined(tombolaPlain, tombolaItem)
                                .forMember('id')
                                .forMember('name')
                                .forMember('description')
                                .forMember('winningRules')
                                .forMember('imageId')
                                .forMember('playoutTarget')
                                .forMember('percentOfTicketsAmount')
                                .forMember('totalTicketsAmount')
                                .forMember('creatorResourceId')
                                .forMember('prizes')
                                .forMember('consolationPrize');
                            if (_.has(tombolaPlain, 'startDate')) {
                                tombolaItem.startDate = DateUtils.isoPublish(tombolaPlain.startDate);
                            }
                            if (_.has(tombolaPlain, 'endDate')) {
                                tombolaItem.endDate = DateUtils.isoPublish(tombolaPlain.endDate);
                            }
                            if (_.has(tombolaPlain, 'targetDate')) {
                                tombolaItem.targetDate = DateUtils.isoPublish(tombolaPlain.targetDate);
                            }
                            if (_.has(tombolaPlain, 'createDate')) {
                                tombolaItem.createDate = DateUtils.isoPublish(tombolaPlain.createDate);
                            }
                            tombolaItem.applicationHasTombolas = tombolaPlain.applicationHasTombolas;
                            tombolaItem.tombolaHasRegionalSettings = tombolaPlain.tombolaHasRegionalSettings;
                            tombolaItem.status = Tombola.constants().STATUS_INACTIVE;
                            tombolaItem.bundles = _.map(tombolaPlain.bundles, function (bundle) {
                                bundle.currency = tombolaPlain.currency;
                                return bundle;
                            });
                        // }
                        tombolaPublish = AutoMapper.mapDefinedBySchema('tombolaPublishModel', INSTANCE_MAPPINGS, tombolaItem);
                        tombolaPublish.oldStatus = tombolaPlain.status;
                        tombolaPublish.applicationsIds = _.map(tombolaPublish.applicationsIds.items, function (id) { return id.toString(); });
                        tombolaPublish.regionalSettingsIds = _.map(tombolaPublish.regionalSettingsIds.items, function (id) { return id.toString(); });
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                logger.info('TombolaApiFactory.tombolaGetForPublish', JSON.stringify(tombolaPublish));
                return CrudHelper.callbackSuccess(tombolaPublish, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    tombolaPublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        var tombola;
        var sessionTenantId;
        var tombolaIsOpenCheckout = false;
        var voucherDefinitions = [];
        var reservedVouchers = [];
        let oldVouchers = [];
        try {
            async.series([
                // Get tombola data for publishing
                function (next) {
                    try {
                        return self.tombolaGetForPublish(params, message, clientSession, function (err, tombolaPublish) {
                            if (err) {
                                return next(err);
                            }
                            tombola = tombolaPublish;
                            tombola.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Collect vouchers for user, if prize/consolation prize definitions contains vouchers
                function (next) {
                    try {
                        if (tombola.prizes) {
                            _.forEach(tombola.prizes, function (prize) {
                                if (prize && prize.voucherId) {
                                    voucherDefinitions.push({
                                        voucherId: prize.voucherId,
                                        draws: prize.draws ? prize.draws : 1
                                    });
                                }
                            });
                        }
                        if (tombola.consolationPrize && tombola.consolationPrize.voucherId) {
                            voucherDefinitions.push({
                                voucherId: tombola.consolationPrize.voucherId,
                                draws: tombola.consolationPrize.draws ? tombola.consolationPrize.draws : 1
                            });
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tombolaApiFactory.tombolaPublish: voucher reservation failed', ex);
                        return next(Errors.TombolaApi.TombolaUserVoucherReserveFailed);
                    }
                },
                // Check collected vouchers for user: they should exist and be published
                function (next) {
                    try {
                        if (voucherDefinitions.length === 0) {
                            return setImmediate(next);
                        }
                        var uniqueVoucherDefinitions = _.uniq(_.map(voucherDefinitions, 'voucherId'));
                        return Voucher.count({ where: { id: { $in: uniqueVoucherDefinitions }, status: Voucher.constants().STATUS_ACTIVE } }).then(function (count) {
                            if (count !== uniqueVoucherDefinitions.length) {
                                return next(Errors.VoucherApi.VoucherIsDeactivated);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('tombolaApiFactory.tombolaPublish: voucher reservation failed', ex);
                        return next(Errors.TombolaApi.TombolaUserVoucherReserveFailed);
                    }
                },

                // Find old reserved vouchers
                function (next) {
                    if (tombola.oldStatus == Tombola.constants().STATUS_ARCHIVED) {
                        logger.debug("skip find reserved!");
                        return next();
                    }
                    return AerospikeVoucherTombolaReserve.findAll({
                        tombolaId: tombola.id
                    }, function (err, vouchers) {
                        if (err) {
                            if (err === "ERR_ENTRY_NOT_FOUND"){
                                return next();
                            } else {
                                return next();
                            }
                        }
                        oldVouchers = vouchers;
                        return next();
                    });
                },

                // Release old vouchers
                function (next) {
                    try {
                        if (tombola.oldStatus == Tombola.constants().STATUS_ARCHIVED) {
                            return next();
                        }
                        return async.mapSeries(oldVouchers, function (reservedVoucher , nextVoucher) {
                            return clientSession.getConnectionService().getVoucherService().userVoucherRelease({
                                voucherId: reservedVoucher,
                                waitResponse: true
                            }, function (err, message) {
                                if (err || (message && message.getError())) {
                                    logger.error('tombolaApiFactory.tombolaPublish: userVoucherRelease failed', err || (message && message.getError()));
                                    return nextVoucher(Errors.TombolaApi.TombolaUserVoucherReserveFailed);
                                }
                                return nextVoucher();
                            });
                        }, next);

                    } catch (ex) {
                        logger.error('tombolaApiFactory.tombolaPublish: voucher release failed', ex);
                        return next(Errors.TombolaApi.TombolaUserVoucherReleaseFailed);
                    }
                },

                // Reserve collected vouchers for user
                function (next) {
                    try {
                         if (tombola.oldStatus == Tombola.constants().STATUS_ARCHIVED) {
                             return next();
                         }
                        if (voucherDefinitions.length === 0) {
                            return setImmediate(next);
                        }
                        return async.mapSeries(voucherDefinitions, function (voucherDefinition, nextVoucherDefinition) {
                            return async.times(voucherDefinition.draws, function (draw, nextDraw) {
                                try {
                                    return clientSession.getConnectionService().getVoucherService().userVoucherReserve({
                                        voucherId: voucherDefinition.voucherId.toString(),
                                        waitResponse: true
                                    }, function (err, message) {
                                        if (err || (message && message.getError())) {
                                            logger.error('tombolaApiFactory.tombolaPublish: userVoucherReserve failed', err || (message && message.getError()));
                                            return nextDraw(Errors.TombolaApi.TombolaUserVoucherReserveFailed);
                                        }
                                        reservedVouchers.push(voucherDefinition.voucherId.toString());
                                        return nextDraw();
                                    });
                                } catch (ex) {
                                    logger.error('tombolaApiFactory.tombolaPublish: userVoucherReserve failed', ex);
                                    return nextDraw(Errors.QuestionApi.NoInstanceAvailable);
                                }
                            }, nextVoucherDefinition);
                        }, next);
                    } catch (ex) {
                        logger.error('tombolaApiFactory.tombolaPublish: voucher reservation failed', ex);
                        return next(Errors.TombolaApi.TombolaUserVoucherReserveFailed);
                    }
                },
                // Write reserved vouchers
                function (next) {
                    try{
                        if ((reservedVouchers.length === 0 && oldVouchers.length === 0) && tombola.oldStatus == Tombola.constants().STATUS_ARCHIVED){
                            logger.debug("skip release!");
                            return next();
                        }
                        AerospikeVoucherTombolaReserve.save({
                            tombolaId: tombola.id,
                            reservedVouchers: reservedVouchers
                            }, function (err, json) {
                            if (err) {
                                logger.error('tombolaApiFactory.tombolaPublish: voucher release failed', err);
                                return next();
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('tombolaApiFactory.tombolaPublish: voucher release failed', ex);
                        return next(Errors.TombolaApi.TombolaUserVoucherReleaseFailed);
                    }
                },
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
                // Get tombola record by tenant, check status
                function (next) {
                    return Tombola.findOne({
                        where: { id: params.id, tenantId: sessionTenantId }
                    }).then(function (tombola) {
                        return self.tombolaCheckStatus(Tombola.constants().STATUS_ACTIVE, tombola, function (err) {
                            if (err) {
                                return next(err);
                            }
                            var tombolaPlain = tombola.get({ plain: true });
                            tombolaIsOpenCheckout = tombolaPlain.isOpenCheckout === 1;
                            return next();
                        });
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Publish tombola
                function (next) {
                    try {
                        return AerospikeTombola.publish(tombola, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Set tombola deployment status
                function (next) {
                    try {
                        tombolaParams = {};
                        tombolaParams.id = params.id;
                        tombolaParams.status = Tombola.constants().STATUS_ACTIVE;
                        tombolaParams.skipCheck = true;
                        return self.tombolaSetStatus(tombolaParams, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // schedule tombola, if needed
                function (next) {
                    try {
                        Tombola.findOne({ where: { id: params.id } }).then(function (dbTombola) {
                            try {
                                var nowSeconds = parseInt(Date.now()/1000);
                                var tombola = dbTombola.get({plain: true});
                                if (tombola.startDate && 
                                    tombola.endDate && 
                                    tombola.targetDate  &&
                                    (tombola.status === Tombola.constants().STATUS_ACTIVE || (tombola.status === Tombola.constants().STATUS_ARCHIVED && tombola.targetDate < nowSeconds))) {
                                    clientSession.getConnectionService()._schedulerService.scheduleTombola({
                                        id: tombola.id,
                                        startDate: tombola.startDate,
                                        endDate: tombola.endDate,
                                        targetDate: tombola.targetDate,
                                        preCloseOffsetMinutes: tombola.preCloseOffsetMinutes,
                                        eventsData: {
                                            tombolaId: tombola.id,
                                            tenantId: clientSession.getTenantId()
                                        }
                                    }, next);
                            
                                } else {
                                    return next();
                                }
                            } catch (e) {
                                return next(e);
                            }
                        }).catch (function (err) {
                            return next(err);
                        });
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

    tombolaUnpublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            var self = this;
            var tombola;
            var sessionTenantId;
            var tombolaIsOpenCheckout = false;
            var voucherDefinitions = [];
            let oldVouchers = [];
            async.series([
                // Get tombola data for publishing
                function (next) {
                    try {
                        return self.tombolaGetForPublish(params, message, clientSession, function (err, tombolaPublish) {
                            if (err) {
                                return next(err);
                            }
                            tombola = tombolaPublish;
                            tombola.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Find old reserved vouchers
                function (next) {
                    if (tombola.oldStatus == Tombola.constants().STATUS_ARCHIVED) {
                        logger.debug("skip find reserved!");
                        return next();
                    }
                    return AerospikeVoucherTombolaReserve.findAll({
                        tombolaId: tombola.id
                    }, function (err, vouchers) {
                        if (err) {
                            if (err === "ERR_ENTRY_NOT_FOUND"){
                                console.log("AerospikeVoucherTombolaReserve.findAll ERR_ENTRY_NOT_FOUND");
                                return next();
                            } else {
                                console.log("AerospikeVoucherTombolaReserve.findAll err: ",err);
                                return next();
                            }
                        }
                        oldVouchers = vouchers;
                        return next();
                    });
                },

                // Release old vouchers
                function (next) {
                    try {
                        if (tombola.oldStatus == Tombola.constants().STATUS_ARCHIVED) {
                            return next();
                        }
                        return async.mapSeries(oldVouchers, function (reservedVoucher , nextVoucher) {
                            return clientSession.getConnectionService().getVoucherService().userVoucherRelease({
                                voucherId: reservedVoucher,
                                waitResponse: true
                            }, function (err, message) {
                                if (err || (message && message.getError())) {
                                    logger.error('tombolaApiFactory.tombolaUnPublish: userVoucherRelease failed', err || (message && message.getError()));
                                    return nextVoucher(Errors.TombolaApi.TombolaUserVoucherReserveFailed);
                                }
                                return nextVoucher();
                            });
                        }, next);

                    } catch (ex) {
                        logger.error('tombolaApiFactory.tombolaUnPublish: voucher release failed', ex);
                        return next(Errors.TombolaApi.TombolaUserVoucherReleaseFailed);
                    }
                },
                // Clean reserved vouchers
                function (next) {
                    try{
                        if ((oldVouchers.length === 0) && tombola.oldStatus == Tombola.constants().STATUS_ARCHIVED){
                            return next();
                        }
                        AerospikeVoucherTombolaReserve.save({
                            tombolaId: tombola.id,
                            reservedVouchers: []
                        }, function (err, json) {
                            if (err) {
                                logger.error('tombolaApiFactory.tombolaPublish: voucher release failed', err);
                                return next();
                            }
                            return next();
                        });
                    } catch (ex) {
                        logger.error('tombolaApiFactory.tombolaPublish: voucher release failed', ex);
                        return next(Errors.TombolaApi.TombolaUserVoucherReleaseFailed);
                    }
                },
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
                // Get tombola record by tenant, check status
                function (next) {
                    return Tombola.findOne({
                        where: { id: params.id, tenantId: sessionTenantId }
                    }).then(function (tombola) {
                        return self.tombolaCheckStatus(Tombola.constants().STATUS_INACTIVE, tombola, function (err) {
                            if (err) {
                                return next(err);
                            }
                            var tombolaPlain = tombola.get({ plain: true });
                            tombolaIsOpenCheckout = tombolaPlain.isOpenCheckout === 1;
                            return next();
                        });
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Unpublish tombola
                function (next) {
                    try {
                        return AerospikeTombola.unpublish(tombola, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Unpublish tombola list
                function (next) {
                    try {
                        return AerospikeTombolaList.unpublish(tombola, next);
                    } catch (ex) {
                        logger.error("Unpublish tombola list : ",ex);
                        return setImmediate(next, ex);
                    }
                },
                // Reset tombola deployment status
                function (next) {
                    try {
                        var tombolaParams = {};
                        tombolaParams.id = params.id;
                        tombolaParams.status = Tombola.constants().STATUS_INACTIVE;
                        tombolaParams.skipCheck = true;
                        return self.tombolaSetStatus(tombolaParams, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // unschedule tombola
                function (next) {
                    try {
                        return clientSession.getConnectionService()._schedulerService.unscheduleTombola(params.id, next);
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
}
