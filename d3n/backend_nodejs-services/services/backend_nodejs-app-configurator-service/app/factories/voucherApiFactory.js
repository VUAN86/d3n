var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DependencyHelper = AutoMapperInstance.DependencyHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var TenantService = require('../services/tenantService.js');
var ProfileService = require('../services/profileService.js');
var VoucherService = require('../services/voucherService.js');
var Database = require('nodejs-database').getInstance(Config);
var Voucher = Database.RdbmsService.Models.VoucherManager.Voucher;
var VoucherProvider = Database.RdbmsService.Models.VoucherManager.VoucherProvider;
var RegionalSetting = Database.RdbmsService.Models.Question.RegionalSetting;
var VoucherProviderHasRegionalSetting = Database.RdbmsService.Models.VoucherManager.VoucherProviderHasRegionalSetting;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var AerospikeVoucher = KeyvalueService.Models.AerospikeVoucher;
var AerospikeVoucherList = KeyvalueService.Models.AerospikeVoucherList;
var AerospikeVoucherCounter = KeyvalueService.Models.AerospikeVoucherCounter;
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'voucherManagerModel': [
        {
            destination: '$root',
            model: Voucher
        }
    ],
    'voucherManagerCreateModel': [
        {
            destination: '$root',
            model: Voucher
        }
    ],
    'voucherProviderModel': [
        {
            destination: '$root',
            model: VoucherProvider
        },
        {
            destination: 'regionalSettingsIds',
            model: VoucherProviderHasRegionalSetting,
            attribute: VoucherProviderHasRegionalSetting.tableAttributes.regionalSettingId
        }
    ],
    'voucherProviderCreateModel': [
        {
            destination: '$root',
            model: VoucherProvider
        },
        {
            destination: 'regionalSettingsIds',
            model: VoucherProviderHasRegionalSetting,
            attribute: VoucherProviderHasRegionalSetting.tableAttributes.regionalSettingId
        }
    ],
    'voucherPublishModel': [
        {
            destination: '$root',
            model: Voucher
        }
    ],
    'voucherListPublishModel': [
        {
            destination: '$root',
            model: Voucher
        }
    ]
};
INSTANCE_MAPPINGS['voucherManagerUpdateModel'] = INSTANCE_MAPPINGS['voucherManagerCreateModel'];
INSTANCE_MAPPINGS['voucherProviderUpdateModel'] = INSTANCE_MAPPINGS['voucherProviderCreateModel'];

module.exports = {
    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    voucherUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var voucherId = undefined;
            var mapBySchema = 'voucherManagerCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                voucherId = params.id;
                mapBySchema = 'voucherManagerUpdateModel';
            }
            var voucherItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // Create/update main entity
                function (next) {
                    if (create) {
                        return Voucher.create(voucherItem).then(function (voucher) {
                            voucherId = voucher.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return Voucher.update(voucherItem, { where: { id: voucherItem.id }, individualHooks: true }).then(function (count) {
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
                params.id = voucherId;
                return self.voucherGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set Voucher status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    voucherSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== Voucher.constants().STATUS_ACTIVE &&
            params.status !== Voucher.constants().STATUS_INACTIVE &&
            params.status !== Voucher.constants().STATUS_DIRTY &&
            params.status !== Voucher.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current status
                function (next) {
                    return Voucher.findOne({ where: { id: params.id } }).then(function (voucher) {
                        if (!voucher) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Only undeployed and inactive (but not archived) vouchers can be archived/activated
                        if (params.status === Voucher.constants().STATUS_ARCHIVED || params.status === Voucher.constants().STATUS_ACTIVE) {
                            if (voucher.status === Voucher.constants().STATUS_ARCHIVED) {
                                return next(Errors.VoucherApi.VoucherIsArchived);
                            }
                        }
                        // Only undeployed and active vouchers can be deactivated
                        if (params.status === Voucher.constants().STATUS_INACTIVE) {
                            if (voucher.status === Voucher.constants().STATUS_INACTIVE) {
                                return next(Errors.VoucherApi.VoucherIsDeactivated);
                            }
                            if (voucher.status === Voucher.constants().STATUS_ARCHIVED) {
                                return next(Errors.VoucherApi.VoucherIsArchived);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return Voucher.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
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

    voucherDelete: function (params, callback) {
        try {
            return Voucher.destroy({ where: { id: params.id } }).then(function (count) {
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

    voucherGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'voucher';
            var mapBySchema = 'voucherManagerModel';
            var attributes = _.keys(Voucher.attributes);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                var include = CrudHelper.include(Voucher, [
                    VoucherProvider
                ]);
                include = CrudHelper.includeWhere(include, VoucherProvider, { tenantId: tenantId }, true);
                return Voucher.findOne({
                    where: { id: params.id },
                    attributes: attributes,
                    include: include,
                    subQuery: false
                }).then(function (voucher) {
                    if (!voucher) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var voucherItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, voucher);
                    response[responseItem] = voucherItem;
                    return CrudHelper.callbackSuccess(response, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    voucherList: function (params, message, clientSession, callback) {
        try {
            try {
                var mapBySchema = 'voucherManagerModel';
                var attributes = _.keys(Voucher.attributes);
                var orderBy = CrudHelper.orderBy(params, Voucher);
                var searchBy = CrudHelper.searchBy(params, Voucher);
                TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                    if (err) {
                        return CrudHelper.callbackError(err, callback);
                    }
                    var include = CrudHelper.include(Voucher, [
                        VoucherProvider
                    ]);
                    include = CrudHelper.includeWhere(include, VoucherProvider, { tenantId: tenantId }, true);
                    return Voucher.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        attributes: attributes,
                        include: include,
                        distinct: true,
                        subQuery: false
                    }).then(function (vouchers) {
                        var voucherItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, vouchers.rows);
                        return CrudHelper.callbackSuccess({
                            items: voucherItems,
                            limit: params.limit,
                            offset: params.offset,
                            total: vouchers.count,
                        }, callback);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, callback);
                    });
                });
            } catch (ex) {
                return CrudHelper.callbackError(ex, callback);
            }
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    voucherProviderUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var voucherProviderId = undefined;
            var mapBySchema = 'voucherProviderCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                voucherProviderId = params.id;
                mapBySchema = 'voucherProviderUpdateModel';
            }
            var voucherProviderAssociates = undefined;
            var voucherProviderItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // get tenantId from global session
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
                        voucherProviderItem.tenantId = sessionTenantId;
                        return VoucherProvider.create(voucherProviderItem).then(function (voucherProvider) {
                            voucherProviderId = voucherProvider.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        var options = {
                            where: {
                                id: voucherProviderItem.id,
                                tenantId: sessionTenantId
                            }
                        };

                        return VoucherProvider.update(voucherProviderItem, options).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    }
                },
                // Populate voucher associated entity values
                function (next) {
                    try {
                        voucherProviderAssociates = AutoMapper.mapAssociatesDefinedBySchema(
                            mapBySchema, INSTANCE_MAPPINGS, params, {
                                field: 'voucherProviderId',
                                value: voucherProviderId
                            }
                        );
                        return setImmediate(next, null);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Delete child entities for every updateable associated entity
                function (next) {
                    if (create || _.isUndefined(voucherProviderAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(voucherProviderAssociates, function (itemAssociate, remove) {
                        //if (itemAssociate.values.length === 0) {
                        //    return remove();
                        //}
                        return itemAssociate.model.destroy({ where: { voucherProviderId: voucherProviderId } }).then(function (count) {
                            return remove();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, remove);
                        });
                    }, next);
                },
                // Bulk insert populated values for every updateable associated entity
                function (next) {
                    if (_.isUndefined(voucherProviderAssociates)) {
                        return setImmediate(next, null);
                    }
                    async.mapSeries(voucherProviderAssociates, function (itemAssociate, create) {
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
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                params.id = voucherProviderId;
                return self.voucherProviderGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set VoucherProvider status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    voucherProviderSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== VoucherProvider.constants().STATUS_ACTIVE &&
            params.status !== VoucherProvider.constants().STATUS_INACTIVE &&
            params.status !== VoucherProvider.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current deployment status
                function (next) {
                    return Voucher.count({ where: { voucherProviderId: params.id, status: Voucher.constants().STATUS_ACTIVE } }).then(function (count) {
                        // Only undeployed and inactive (but not archived) voucher providers can be archived/activated
                        if (params.status === VoucherProvider.constants().STATUS_ARCHIVED || params.status === VoucherProvider.constants().STATUS_ACTIVE) {
                            if (count > 0) {
                                return next(Errors.VoucherApi.VoucherIsDeployed);
                            }
                        }
                        // Only undeployed and active voucher providers can be deactivated
                        if (params.status === VoucherProvider.constants().STATUS_INACTIVE) {
                            if (count > 0) {
                                return next(Errors.VoucherApi.VoucherIsDeployed);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Check current voucher provider status
                function (next) {
                    return VoucherProvider.findOne({ where: { id: params.id } }).then(function (voucherProvider) {
                        if (!voucherProvider) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Only undeployed and inactive (but not archived) voucher providers can be archived/activated
                        if (params.status === VoucherProvider.constants().STATUS_ARCHIVED || params.status === VoucherProvider.constants().STATUS_ACTIVE) {
                            if (voucherProvider.status === VoucherProvider.constants().STATUS_ARCHIVED) {
                                return next(Errors.VoucherApi.VoucherProviderIsArchived);
                            }
                        }
                        // Only undeployed and active voucher providers can be deactivated
                        if (params.status === VoucherProvider.constants().STATUS_INACTIVE) {
                            if (voucherProvider.status === VoucherProvider.constants().STATUS_INACTIVE) {
                                return next(Errors.VoucherApi.VoucherProviderIsDeactivated);
                            }
                            if (voucherProvider.status === VoucherProvider.constants().STATUS_ARCHIVED) {
                                return next(Errors.VoucherApi.VoucherProviderIsArchived);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return VoucherProvider.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
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

    voucherProviderDelete: function (params, callback) {
        try {
            return VoucherProvider.destroy({ where: { id: params.id } }).then(function (count) {
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

    voucherProviderGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'voucherProvider';
            var mapBySchema = 'voucherProviderModel';
            var attributes = _.keys(VoucherProvider.attributes);
            var include = CrudHelper.include(VoucherProvider, [
                VoucherProviderHasRegionalSetting
            ]);
            var where = { id: params.id };
            async.series([
                // get tenantId from global session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        where.tenantId = tenantId;
                        return next();
                    });
                },
                function (next) {
                    VoucherProvider.findOne({
                        where: where,
                        attributes: attributes,
                        include: include
                    }).then(function (voucherProvider) {
                        if (!voucherProvider) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        var voucherProviderItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, voucherProvider);
                        response[responseItem] = voucherProviderItem;
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

    voucherProviderList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'voucherProviderModel';
            var attributes = _.keys(VoucherProvider.attributes);
            var orderBy = CrudHelper.orderBy(params, VoucherProvider);
            var searchBy = CrudHelper.searchBy(params, VoucherProvider);
            var voucherProviderItems = [];
            var total = 0;
            async.series([
                // add tenantId filter
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
                    var include = CrudHelper.includeDefined(VoucherProvider, [
                        VoucherProviderHasRegionalSetting
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return VoucherProvider.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (voucherProviders) {
                        total = voucherProviders.count;
                        if (!_.has(searchBy, 'id')) {
                            var voucherProvidersIds = [];
                            _.forEach(voucherProviders.rows, function (voucherProvider) {
                                voucherProvidersIds.push(voucherProvider.id);
                            });
                            searchBy = {
                                id: { '$in': voucherProvidersIds }
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
                    var include = CrudHelper.include(VoucherProvider, [
                        VoucherProviderHasRegionalSetting
                    ], params, mapBySchema, INSTANCE_MAPPINGS);
                    return VoucherProvider.findAll({
                        where: searchBy,
                        order: orderBy,
                        attributes: attributes,
                        include: include,
                        subQuery: false
                    }).then(function (voucherProviders) {
                        voucherProviderItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, voucherProviders);
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
                    items: voucherProviderItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: total,
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    voucherGetForPublish: function (params, message, clientSession, callback) {
        try {
            var voucherPublish;
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
                // Get voucher record by tenant
                function (next) {
                    var voucherItem = {};
                    return Voucher.findOne({
                        where: { id: params.id },
                        include: { model: VoucherProvider, where: { tenantId: sessionTenantId } }
                    }).then(function (voucher) {
                        if (!voucher) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (voucher.status === Voucher.constants().STATUS_ARCHIVED) {
                            return next(Errors.VoucherApi.VoucherIsArchived);
                        }
                        // Custom Voucher mapper object
                        var voucherPlain = voucher.get({ plain: true });
                        AutoMapper.mapDefined(voucherPlain, voucherItem)
                            .forMember('id')
                            .forMember('name', 'title')
                            .forMember('company')
                            .forMember('description')
                            .forMember('shortTitle')
                            .forMember('bonuspointsCosts')
                            .forMember('miniImage', 'miniImageId')
                            .forMember('normalImage', 'normalImageId')
                            .forMember('bigImage', 'bigImageId')
                            .forMember('redemptionURL');
                        if (_.has(voucherPlain, 'expirationDate')) {
                            voucherItem.expirationDate = DateUtils.isoPublish(voucherPlain.expirationDate);
                        }
                        if (_.has(voucherPlain, 'isQRCode')) {
                            voucherItem.isQRCode = voucherPlain.isQRCode === 1;
                        }
                        if (_.has(voucherPlain, 'isExchange')) {
                            voucherItem.isExchange = voucherPlain.isExchange === 1;
                        }
                        if (_.has(voucherPlain, 'isSpecialPrice')) {
                            voucherItem.isSpecialPrize = voucherPlain.isSpecialPrice === 1;
                        }
                        voucherPublish = AutoMapper.mapDefinedBySchema('voucherPublishModel', INSTANCE_MAPPINGS, voucherItem);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                logger.info('VoucherApiFactory.voucherGetForPublish', JSON.stringify(voucherPublish));
                return CrudHelper.callbackSuccess(voucherPublish, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    voucherPublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var voucher;
            var voucherProviderItem;
            var voucherList;
            var regionalSettingItems;
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
                // Get voucher record by tenant
                function (next) {
                    var voucherItem = {};
                    return Voucher.findOne({
                        where: { id: params.id },
                        include: { model: VoucherProvider, where: { tenantId: sessionTenantId } }
                    }).then(function (foundVoucher) {
                        if (!foundVoucher) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (foundVoucher.status === Voucher.constants().STATUS_ARCHIVED) {
                            return next(Errors.VoucherApi.VoucherIsArchived);
                        }
                        voucherProviderItem = foundVoucher.voucherProvider;
                        var voucherPlain = foundVoucher.get({ plain: true });
                        AutoMapper.mapDefined(voucherPlain, voucherItem)
                            .forMember('id')
                            .forMember('name', 'title')
                            .forMember('company')
                            .forMember('description')
                            .forMember('shortTitle')
                            .forMember('bonuspointsCosts')
                            .forMember('miniImage', 'miniImageId')
                            .forMember('normalImage', 'normalImageId')
                            .forMember('bigImage', 'bigImageId')
                            .forMember('redemptionURL');
                        if (_.has(voucherPlain, 'expirationDate')) {
                            voucherItem.expirationDate = DateUtils.isoPublish(voucherPlain.expirationDate);
                        }
                        if (_.has(voucherPlain, 'isQRCode')) {
                            voucherItem.isQRCode = voucherPlain.isQRCode === 1;
                        }
                        if (_.has(voucherPlain, 'isExchange')) {
                            voucherItem.isExchange = voucherPlain.isExchange === 1;
                        }
                        if (_.has(voucherPlain, 'isSpecialPrice')) {
                            voucherItem.isSpecialPrize = voucherPlain.isSpecialPrice === 1;
                        }
                        voucher = AutoMapper.mapDefinedBySchema('voucherPublishModel', INSTANCE_MAPPINGS, voucherItem);
                        voucher.id = params.id;
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Get regional settings data for publishing
                function (next) {
                    var include = CrudHelper.include(VoucherProvider, [
                        VoucherProviderHasRegionalSetting
                    ]);
                    var where = { id: voucherProviderItem.id };
                    VoucherProvider.findOne({
                        where: where,
                        include: include
                    }).then(function (voucherProvider) {
                        if (!voucherProvider) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        voucherProviderItem = AutoMapper.mapDefinedBySchema('voucherProviderModel', INSTANCE_MAPPINGS, voucherProvider);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Get regional settings data for publishing
                function (next) {
                    try {
                        RegionalSetting.findAll({
                            where: {
                                id: voucherProviderItem.regionalSettingsIds.items
                            },
                        }).then(function (regionalSetting) {
                            regionalSettingItems = regionalSetting
                                .map(setting => setting.name)
                                .join(', ');
                            logger.info('regionalSettingItems', JSON.stringify(regionalSettingItems));
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get voucher list data for publishing
                function (next) {
                    try {
                        return self.voucherListGetForPublish(params, message, clientSession, function (err, voucherListPublish) {
                            if (err) {
                                return next(err);
                            }
                            voucherList = voucherListPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish voucher
                function (next) {
                    try {
                        voucher.regionalSettings = regionalSettingItems;
                        logger.info('voucher.regionalSettings', JSON.stringify(voucher.regionalSettings));
                        return AerospikeVoucher.publish(voucher, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish voucher list
                function (next) {
                    try {
                        if (!voucherList.isExchange) {
                            voucherList.regionalSettings = regionalSettingItems;
                            return AerospikeVoucherList.unpublish(voucherList, next);
                        } else {
                            voucherList.regionalSettings = regionalSettingItems;
                            return AerospikeVoucherList.publish(voucherList, next);
                        }
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Set voucher deployment status
                function (next) {
                    try {
                        voucherParams = {};
                        voucherParams.id = params.id;
                        voucherParams.status = Voucher.constants().STATUS_ACTIVE;
                        return self.voucherSetStatus(voucherParams, next);
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

    voucherUnpublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var voucher;
            var voucherList;
            var skip = false;
            var sessionTenantId;
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
                // Get voucher data for publishing
                function (next) {
                    try {
                        return self.voucherGetForPublish(params, message, clientSession, function (err, voucherPublish) {
                            if (err) {
                                return next(err);
                            }
                            voucher = voucherPublish;
                            voucher.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get voucher list data for publishing
                function (next) {
                    try {
                        return self.voucherListGetForPublish(params, message, clientSession, function (err, voucherListPublish) {
                            if (err) {
                                return next(err);
                            }
                            voucherList = voucherListPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Check voucher if it is used
                function (next) {
                    try {
                        return AerospikeVoucherCounter.getUsedAmount({ id: params.id }, function (err, usedAmount) {
                            if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                return next(err);
                            }
                            return AerospikeVoucherCounter.getAmount({ id: params.id }, function (err, amount) {
                                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                                    return next(err);
                                }
                                if (amount !== usedAmount) {
                                    skip = true;
                                }
                                return next();
                            });
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Process voucher deactivation dependencies
                //function (next) {
                //    try {
                //        return DependencyHelper.process(message, clientSession, 'voucher', 'deactivate', params.id, next);
                //    } catch (ex) {
                //        return setImmediate(next, ex);
                //    }
                //},
                // Unpublish voucher only if it is not used yet
                function (next) {
                    try {
                        if (skip) {
                            return setImmediate(next);
                        }
                        return AerospikeVoucher.unpublish(voucher, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Unpublish voucher list always
                function (next) {
                    try {
                        if (!voucherList.isExchange) {
                            return setImmediate(next);
                        }
                        return AerospikeVoucherList.unpublish({ id: params.id, tenantId: sessionTenantId }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Reset voucher deployment status
                function (next) {
                    try {
                        var voucherParams = {};
                        voucherParams.id = params.id;
                        voucherParams.status = Voucher.constants().STATUS_INACTIVE;
                        return self.voucherSetStatus(voucherParams, next);
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

    voucherListGetForPublish: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var voucherList = {
                vouchers: []
            };
            async.series([
                // Get voucher data for publishing
                function (next) {
                    try {
                        return self.voucherGetForPublish(params, message, clientSession, function (err, voucherPublish) {
                            if (err) {
                                return next(err);
                            }
                            voucherList = voucherPublish;
                            voucherList.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get tenantId from global session
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        voucherList.tenantId = tenantId;
                        return next();
                    });
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                logger.info('VoucherApiFactory.voucherListGetForPublish', JSON.stringify(voucherList));
                return CrudHelper.callbackSuccess(voucherList, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    voucherListPublish: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var voucherList;
            async.series([
                // Get voucher list data for publishing
                function (next) {
                    try {
                        return self.voucherListGetForPublish(params, message, clientSession, function (err, voucherListPublish) {
                            if (err) {
                                return next(err);
                            }
                            voucherList = voucherListPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish voucher list
                function (next) {
                    try {
                        if (!voucherList.isExchange) {
                            return setImmediate(next);
                        }
                        return AerospikeVoucherList.publish(voucherList, next);
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

    voucherListUnpublish: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var sessionTenantId;
            var voucherList;
            async.series([
                // Get voucher list data for publishing
                function (next) {
                    try {
                        return self.voucherListGetForPublish(params, message, clientSession, function (err, voucherListPublish) {
                            if (err) {
                                return next(err);
                            }
                            voucherList = voucherListPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Unpublish voucher list
                function (next) {
                    try {
                        if (!voucherList.isExchange) {
                            return setImmediate(next);
                        }
                        return AerospikeVoucherList.unpublish({ id: params.id, tenantId: sessionTenantId }, next);
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

    voucherGenerate: function (params, message, clientSession, callback) {
        var self = this;
        var sessionTenantId;
        var inventoryCount = 0;
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
                // Count voucher record by tenant
                function (next) {
                    return Voucher.count({
                        where: { id: params.id },
                        include: { model: VoucherProvider, where: { tenantId: sessionTenantId } }
                    }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update voucher expiration date
                function (next) {
                    try {
                        return Voucher.update(
                            { expirationDate: params.expirationDate },
                            { where: { id: params.id }, individualHooks: true }
                        ).then(function (count) {
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
                // Publish voucher
                function (next) {
                    try {
                        return self.voucherPublish(params, message, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish voucher list
                function (next) {
                    try {
                        return self.voucherListPublish(params, message, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Generate vouchers
                function (next) {
                    try {
                        return AerospikeVoucherCounter.generate(params, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get voucher used amount
                function (next) {
                    try {
                        return AerospikeVoucherCounter.getUsedAmount({ id: params.id }, function (err, usedAmount) {
                            if (err) {
                                return next(err);
                            }
                            inventoryCount = usedAmount;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Update Voucher statistics
                function (next) {
                    try {
                        var voucherItem = {
                            stat_inventoryCount: inventoryCount
                        };
                        return Voucher.update(voucherItem, { where: { id: params.id }, individualHooks: false }).then(function (count) {
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

    voucherGenerateFromFile: function (params, message, clientSession, callback) {
        var self = this;
        var sessionTenantId;
        var inventoryCount = 0;
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
                // Count voucher record by tenant
                function (next) {
                    return Voucher.count({
                        where: { id: params.id },
                        include: { model: VoucherProvider, where: { tenantId: sessionTenantId } }
                    }).then(function (count) {
                        if (count === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Get voucher codes and their expiration dates
                function (next) {
                    try {
                        return VoucherService.getCodes(params.key, function (err, codes) {
                            if (err || !codes || codes.length === 0) {
                                return next(Errors.VoucherApi.VoucherCodesAreNotAvailable);
                            }
                            params.codes = codes;
                            params.amount = codes.length;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish voucher
                function (next) {
                    try {
                        return self.voucherPublish(params, message, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish voucher list
                function (next) {
                    try {
                        return self.voucherListPublish(params, message, clientSession, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Setup voucher creation counter and generate instances
                function (next) {
                    try {
                        return AerospikeVoucherCounter.generateCodes(params, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get voucher used amount
                function (next) {
                    try {
                        return AerospikeVoucherCounter.getUsedAmount({ id: params.id }, function (err, usedAmount) {
                            if (err) {
                                return next(err);
                            }
                            inventoryCount = usedAmount;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Update Voucher statistics
                function (next) {
                    try {
                        var voucherItem = {
                            stat_inventoryCount: inventoryCount
                        };
                        return Voucher.update(voucherItem, { where: { id: params.id }, individualHooks: false }).then(function (count) {
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
                // Delete codes from S3
                function (next) {
                    try {
                        return VoucherService.deleteCodes(params.key, next);
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

};