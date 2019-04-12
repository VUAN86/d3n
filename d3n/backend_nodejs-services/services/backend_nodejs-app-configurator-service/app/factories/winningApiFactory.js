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
var Database = require('nodejs-database').getInstance(Config);
var WinningComponent = Database.RdbmsService.Models.WinningManager.WinningComponent;
var GameHasWinningComponent = Database.RdbmsService.Models.Game.GameHasWinningComponent;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var AerospikeWinningComponent = KeyvalueService.Models.AerospikeWinningComponent;
var AerospikeWinningComponentList = KeyvalueService.Models.AerospikeWinningComponentList;
let Voucher = Database.RdbmsService.Models.VoucherManager.Voucher;
var logger = require('nodejs-logger')();

var INSTANCE_MAPPINGS = {
    'winningComponentModel': [
        {
            destination: '$root',
            model: WinningComponent
        }
    ],
};
INSTANCE_MAPPINGS['winningComponentCreateModel'] = INSTANCE_MAPPINGS['winningComponentModel'];
INSTANCE_MAPPINGS['winningComponentUpdateModel'] = INSTANCE_MAPPINGS['winningComponentModel'];
INSTANCE_MAPPINGS['winningComponentPublishModel'] = INSTANCE_MAPPINGS['winningComponentModel'];
INSTANCE_MAPPINGS['winningComponentListPublishModel'] = INSTANCE_MAPPINGS['winningComponentModel'];

module.exports = {
    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    winningComponentUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var winningComponentId = undefined;
            var mapBySchema = 'winningComponentCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                winningComponentId = params.id;
                mapBySchema = 'winningComponentUpdateModel';
            }
            var winningComponentItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            var sessionTenantId;

            async.series([
                // fill vouchers short titles
                function (next) {
                    try {
                        const items = [];
                        if(winningComponentItem.winningConfiguration.payoutStructure.items.length === 0) return next();
                        _.forEach(winningComponentItem.winningConfiguration.payoutStructure.items, function (item) {
                            if (item && (item.type === "VOUCHER")) {
                                Voucher.findOne({where: {id: item.prizeId}}).then(function (voucher) {
                                    if (voucher) {
                                        item.title = voucher.shortTitle;
                                    }
                                    items.push(item);
                                    if (winningComponentItem.winningConfiguration.payoutStructure.items.length === items.length) {
                                        winningComponentItem.winningConfiguration.payoutStructure.items = items;
                                        return next();
                                    }
                                }).catch(function (err) {
                                    items.push(item);
                                    logger.error("winningComponentUpdate fill vouchers short titles ex:",ex);
                                    return CrudHelper.callbackError(err, next);
                                });
                            } else {
                                items.push(item);
                                if (winningComponentItem.winningConfiguration.payoutStructure.items.length === items.length) {
                                    winningComponentItem.winningConfiguration.payoutStructure.items = items;
                                    return next();
                                }
                            }
                        });
                    } catch (ex) {
                        logger.error("winningComponentUpdate ex:",ex);
                        return next();
                    }
                },
                    // get tenantId from global session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
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
                        winningComponentItem.tenantId = sessionTenantId;
                        return WinningComponent.create(winningComponentItem).then(function (winningComponent) {
                            winningComponentId = winningComponent.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return WinningComponent.update(winningComponentItem, { where: { id: winningComponentItem.id, tenantId: sessionTenantId }, individualHooks: true }).then(function (count) {
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
                params.id = winningComponentId;
                return self.winningComponentGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Set WinningComponent status
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    winningComponentSetStatus: function (params, callback) {
        var self = this;
        if (!params.hasOwnProperty('status') || (
            params.status !== WinningComponent.constants().STATUS_ACTIVE &&
            params.status !== WinningComponent.constants().STATUS_INACTIVE &&
            params.status !== WinningComponent.constants().STATUS_DIRTY &&
            params.status !== WinningComponent.constants().STATUS_ARCHIVED)) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        try {
            async.series([
                // Check current status
                function (next) {
                    return WinningComponent.findOne({ where: { id: params.id } }).then(function (winningComponent) {
                        if (!winningComponent) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        // Only undeployed and inactive (but not archived) winningComponents can be archived/activated
                        if (params.status === WinningComponent.constants().STATUS_ARCHIVED || params.status === WinningComponent.constants().STATUS_ACTIVE) {
                            if (winningComponent.status === WinningComponent.constants().STATUS_ARCHIVED) {
                                return next(Errors.WinningApi.WinningComponentIsArchived);
                            }
                        }
                        // Only undeployed and active winningComponents can be deactivated
                        if (params.status === WinningComponent.constants().STATUS_INACTIVE) {
                            if (winningComponent.status === WinningComponent.constants().STATUS_INACTIVE) {
                                return next(Errors.WinningApi.WinningComponentIsDeactivated);
                            }
                            if (winningComponent.status === WinningComponent.constants().STATUS_ARCHIVED) {
                                return next(Errors.WinningApi.WinningComponentIsArchived);
                            }
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return WinningComponent.update({ status: params.status }, { where: { id: params.id }, individualHooks: true }).then(function (count) {
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

    winningComponentDelete: function (params, callback) {
        try {
            return WinningComponent.destroy({ where: { id: params.id } }).then(function (count) {
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

    winningComponentGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'winningComponent';
            var mapBySchema = 'winningComponentModel';
            var attributes = _.keys(WinningComponent.attributes);
            return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return WinningComponent.findOne({
                    where: { id: params.id, tenantId: tenantId },
                    attributes: attributes,
                    subQuery: false
                }).then(function (winningComponent) {
                    if (!winningComponent) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var winningComponentItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, winningComponent);
                    response[responseItem] = winningComponentItem;
                    return CrudHelper.callbackSuccess(response, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    winningComponentList: function (params, message, clientSession, callback) {
        try {
            var mapBySchema = 'winningComponentModel';
            var attributes = _.keys(WinningComponent.attributes);
            var orderBy = CrudHelper.orderBy(params, WinningComponent);
            var searchBy = CrudHelper.searchBy(params, WinningComponent);
            return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                searchBy.tenantId = tenantId;
                return WinningComponent.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy
                }).then(function (winningComponents) {
                    var winningComponentItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, winningComponents.rows);
                    return CrudHelper.callbackSuccess({
                        items: winningComponentItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: winningComponents.count,
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    winningComponentGetForPublish: function (params, message, clientSession, callback) {
        try {
            var winningComponentPublish;
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
                // Get winningComponent record by tenant
                function (next) {
                    var winningComponentItem = {};
                    return WinningComponent.findOne({
                        where: { id: params.id, tenantId: sessionTenantId }
                    }).then(function (winningComponent) {
                        if (!winningComponent) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (winningComponent.status === WinningComponent.constants().STATUS_ARCHIVED) {
                            return next(Errors.WinningApi.WinningComponentIsArchived);
                        }
                        // Custom WinningComponent mapper object
                        var winningComponentPlain = winningComponent.get({ plain: true });
                        AutoMapper.mapDefined(winningComponentPlain, winningComponentItem)
                            .forMember('id', 'winningComponentId').toString()
                            .forMember('type', 'gameType')
                            .forMember('title')
                            .forMember('description')
                            .forMember('rules')
                            .forMember('prizeDescription', 'info')
                            .forMember('imageId');
                        winningComponentItem.tenants = [winningComponentPlain.tenantId];
                        winningComponentItem.type = 'FREE';
                        if (_.has(winningComponentPlain, 'isFree')) {
                            winningComponentItem.type = winningComponentPlain.isFree === 1 ? 'FREE' : 'PAID';
                        }
                        winningComponentItem.casinoComponent = winningComponentPlain.winningConfiguration.casinoComponent;
                        winningComponentItem.winningOptions = [];
                        if (winningComponentPlain.winningConfiguration.payoutStructure && _.isArray(winningComponentPlain.winningConfiguration.payoutStructure.items)) {
                            _.forEach(winningComponentPlain.winningConfiguration.payoutStructure.items, function (winningOption) {
                                var winningOptionItem = _.cloneDeep(winningOption);
                                winningOptionItem.winningOptionId = winningComponentPlain.id + '-' + winningComponentItem.winningOptions.length;
                                winningComponentItem.winningOptions.push(winningOptionItem);
                            });
                        }
                        winningComponentPublish = AutoMapper.mapDefinedBySchema('winningComponentPublishModel', INSTANCE_MAPPINGS, winningComponentItem);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                logger.info('WinningApiFactory.winningComponentGetForPublish', JSON.stringify(winningComponentPublish));
                return CrudHelper.callbackSuccess(winningComponentPublish, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    winningComponentPublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var winningComponent;
            var winningComponentList;
            async.series([
                // Get winningComponent data for publishing
                function (next) {
                    try {
                        return self.winningComponentGetForPublish(params, message, clientSession, function (err, winningComponentPublish) {
                            if (err) {
                                return next(err);
                            }
                            winningComponent = winningComponentPublish;
                            winningComponent.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get winningComponent list data for publishing
                function (next) {
                    try {
                        return self.winningComponentListGetForPublish(params, message, clientSession, function (err, winningComponentListPublish) {
                            if (err) {
                                return next(err);
                            }
                            winningComponentList = winningComponentListPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish winningComponent
                function (next) {
                    try {
                        return AerospikeWinningComponent.publish(winningComponent, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Publish winningComponent list
                function (next) {
                    try {
                        if (!winningComponentList.isExchange) {
                            return setImmediate(next);
                        }
                        return AerospikeWinningComponentList.publish(winningComponentList, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Set winningComponent deployment status
                function (next) {
                    try {
                        winningComponentParams = {};
                        winningComponentParams.id = params.id;
                        winningComponentParams.status = WinningComponent.constants().STATUS_ACTIVE;
                        return self.winningComponentSetStatus(winningComponentParams, next);
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

    winningComponentUnpublish: function (params, message, clientSession, callback) {
        if (!params.hasOwnProperty('id')) {
            return CrudHelper.callbackError(Errors.QuestionApi.ValidationFailed, callback);
        }
        var self = this;
        try {
            var winningComponent;
            var winningComponentList;
            var skip = false;
            var sessionTenantId;
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
                // Get winningComponent data for publishing
                function (next) {
                    try {
                        return self.winningComponentGetForPublish(params, message, clientSession, function (err, winningComponentPublish) {
                            if (err) {
                                return next(err);
                            }
                            winningComponent = winningComponentPublish;
                            winningComponent.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get winningComponent list data for publishing
                function (next) {
                    try {
                        return self.winningComponentListGetForPublish(params, message, clientSession, function (err, winningComponentListPublish) {
                            if (err) {
                                return next(err);
                            }
                            winningComponentList = winningComponentListPublish;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Check winningComponent if it is used
                //function (next) {
                //    try {
                //        return AerospikeWinningComponentCounter.getUsedAmount({ id: params.id }, function (err, usedAmount) {
                //            if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                //                return next(err);
                //            }
                //            return AerospikeWinningComponentCounter.getAmount({ id: params.id }, function (err, amount) {
                //                if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                //                    return next(err);
                //                }
                //                if (amount !== usedAmount) {
                //                    skip = true;
                //                }
                //                return next();
                //            });
                //        });
                //    } catch (ex) {
                //        return setImmediate(next, ex);
                //    }
                //},
                // Process winningComponent deactivation dependencies
                //function (next) {
                //    try {
                //        return DependencyHelper.process(message, clientSession, 'winningComponent', 'deactivate', params.id, next);
                //    } catch (ex) {
                //        return setImmediate(next, ex);
                //    }
                //},
                // Unpublish winningComponent only if it is not used yet
                function (next) {
                    try {
                        if (skip) {
                            return setImmediate(next);
                        }
                        return AerospikeWinningComponent.unpublish(winningComponent, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Unpublish winningComponent list always
                function (next) {
                    try {
                        if (!winningComponentList.isExchange) {
                            return setImmediate(next);
                        }
                        return AerospikeWinningComponentList.unpublish({ id: params.id, tenantId: sessionTenantId }, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Reset winningComponent deployment status
                function (next) {
                    try {
                        var winningComponentParams = {};
                        winningComponentParams.id = params.id;
                        winningComponentParams.status = WinningComponent.constants().STATUS_INACTIVE;
                        return self.winningComponentSetStatus(winningComponentParams, next);
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

    winningComponentListGetForPublish: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var winningComponentList = {
                winningComponents: []
            };
            async.series([
                // Get winningComponent data for publishing
                function (next) {
                    try {
                        return self.winningComponentGetForPublish(params, message, clientSession, function (err, winningComponentPublish) {
                            if (err) {
                                return next(err);
                            }
                            winningComponentList = winningComponentPublish;
                            winningComponentList.id = params.id;
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Get tenantId from global session
                function (next) {
                    return TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        winningComponentList.tenantId = tenantId;
                        return next();
                    });
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                logger.info('WinningApiFactory.winningComponentListGetForPublish', JSON.stringify(winningComponentList));
                return CrudHelper.callbackSuccess(winningComponentList, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

};