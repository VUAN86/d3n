var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var Tenant = Database.RdbmsService.Models.Tenant.Tenant;
var Application = Database.RdbmsService.Models.Application.Application;
var TenantContract = Database.RdbmsService.Models.Tenant.TenantContract;
var TenantInvoice = Database.RdbmsService.Models.Tenant.TenantInvoice;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeApp = KeyvalueService.Models.AerospikeApp;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var logger = require('nodejs-logger')();
var ApplicationApiFactory = require('./applicationApiFactory.js');

var AuditService = require('../services/auditService.js');
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'tenantModel': [
        {
            destination: '$root',
            model: Tenant
        }
    ],

    'tenantCreateModel': [
        {
            destination: '$root',
            model: Tenant
        }
    ],

    'tenantContractChangeModel': [
        {
            destination: '$root',
            model: TenantContract
        }
    ],
    'tenantContractModel': [
        {
            destination: '$root',
            model: TenantContract
        }
    ],

    'tenantInvoiceModel': [
        {
            destination: '$root',
            model: TenantInvoice
        }
    ],
};

INSTANCE_MAPPINGS['tenantUpdateModel'] = INSTANCE_MAPPINGS['tenantCreateModel'];

module.exports = {

    /**
     * List Tenants
     * @param params
     * @param callback
     * @returns
     */
    /*
    tenantList: function (params, callback) {
        try {
            var orderBy = CrudHelper.orderBy(params, Tenant);
            var searchBy = CrudHelper.searchBy(params, Tenant);
            return Tenant.findAndCountAll({
                limit: params.limit === 0 ? null : params.limit,
                offset: params.limit === 0 ? null : params.offset,
                where: searchBy,
                order: orderBy
            }).then(function (tenants) {
                var tenantItems = AutoMapper.mapListDefinedBySchema('tenantModel', INSTANCE_MAPPINGS, tenants.rows);
                return CrudHelper.callbackSuccess({
                    items: tenantItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: tenants.count,
                }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    */

    /**
     * Create/Update Tenant
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    tenantUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var oldTenantItem, newTenantItem;
            var create = true;
            var mapBySchema = 'tenantUpdateModel';
            var tenantItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // Get tenantId from global session
                function (next) {
                    try {
                        return TenantService.getSessionTenantId(message, clientSession, function (err, sessionTenantId) {
                            if (err) {
                                logger.error('Error getting tenantId from global session', err);
                                return next(err);
                            }
                            if (!sessionTenantId) {
                                logger.error('Wrong tenantId returned from global session', sessionTenantId);
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            tenantItem.id = sessionTenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('Error on getting tenantId from global session', ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        TenantService.retrieveTenantById(tenantItem.id, function (err, tenant) {
                            if (err) return setImmediate(next, err);

                            oldTenantItem = _.clone(tenant);

                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantApiFactory:tenantUpdate could not retrieve tenant by id', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Update main entity
                function (next) {
                    try {
                        return Tenant.update(tenantItem, { where: { id: tenantItem.id } }).then(function (count) {
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
                        TenantService.retrieveTenantById(tenantItem.id, function (err, tenant) {
                            if (err) return setImmediate(next, err);

                            newTenantItem = _.clone(tenant);
                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error("tenantApiFactory:tenantUpdate could not retrieve updated tenant", ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        AuditService.createTenantAuditLog(
                            clientSession.getUserId(),
                            oldTenantItem,
                            newTenantItem,
                            next);
                    } catch (ex) {
                        logger.error('tenantApiFactory:tenantUpdate - audit log create error', ex);
                        return setImmediate(next, ex);
                    }
                },
                // publish data for getAppConfiguration
                function (next) {
                    ApplicationApiFactory.buildGetAppConfigurationWithoutApp(tenantItem.id, function (err, item) {
                        if (err) {
                            return next(err);
                        }
                        return AerospikeApp.publishApplication(item, next);
                    });
                },
                // make all tenant apps dirty
                function (next) {
                    var where = {
                        tenantId: tenantItem.id,
                        status: Application.constants().STATUS_ACTIVE
                    };
                    
                    Application.update({
                        status: Application.constants().STATUS_DIRTY
                    }, {where: where}).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return self.tenantGet({}, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Get Tenant by ID from global session
     * @param params
     * @param callback
     * @returns
     */
    tenantGet: function (params, message, clientSession, callback) {
        try {
            var response = {};
            var responseItem = 'tenant';
            var mapBySchema = 'tenantModel';
            var tenantId = null;
            async.series([
                // Get tenantId from global session
                function (next) {
                    try {
                        return TenantService.getSessionTenantId(message, clientSession, function (err, sessionTenantId) {
                            if (err) {
                                logger.error('tenantApiFactory.tenantGet Error getting tenantId from global session', err);
                                return next(err);
                            }
                            if (!sessionTenantId) {
                                logger.error('tenantApiFactory.tenantGet Wrong tenantId returned from global session', sessionTenantId);
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            tenantId = sessionTenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('tenantApiFactory.tenantGet Error on getting tenantId from global session', ex);
                        return setImmediate(next, ex);
                    }
                },
                // Get tenant data
                function (next) {
                    try {
                        var attributes = _.keys(Tenant.attributes);
                        return Tenant.findOne({
                            where: { id: tenantId },
                            attributes: attributes,
                        }).then(function (tenant) {
                            if (!tenant) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            var tenantItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, tenant);
                            response[responseItem] = tenantItem;
                            return next();
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
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * List Contracts
     * @param params
     * @param callback
     * @returns
     */
    contractList: function (params, message, clientSession, callback) {
        try {
            var tenantId, response;

            var orderBy = CrudHelper.orderBy(params, TenantContract);
            var searchBy = CrudHelper.searchBy(params, TenantContract);

            async.series([
                // Get tenantId from global session
                function (next) {
                    try {
                        return TenantService.getSessionTenantId(message, clientSession, function (err, sessionTenantId) {
                            if (err) {
                                logger.error('tenantApiFactory.contractList Error getting tenantId from global session', err);
                                return next(err);
                            }
                            if (!sessionTenantId) {
                                logger.error('tenantApiFactory.contractList Wrong tenantId returned from global session', sessionTenantId);
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            tenantId = sessionTenantId;
                            searchBy.tenantId = tenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('tenantApiFactory.contractList Error on getting tenantId from global session', ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        TenantContract.findAndCountAll({
                            limit: params.limit === 0 ? null : params.limit,
                            offset: params.limit === 0 ? null : params.offset,
                            where: searchBy,
                            order: orderBy
                        }).then(function (contracts) {
                            var contractItems = AutoMapper.mapListDefinedBySchema('tenantContractModel', INSTANCE_MAPPINGS, contracts.rows);
                            response = {
                                items: contractItems,
                                limit: params.limit,
                                offset: params.offset,
                                total: contracts.count,
                            };
                            return next();
                        }).catch(function (err) {
                            logger.error('tenantApiFactory.contractList error finding contracts', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        logger.error('tenantApiFactory.contractList error searching tenant contract entities', ex);
                        return setImmediate(next, ex);
                    }
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

    /**
     * Update Contract status
     * @param params
     * @param callback
     * @returns
     */
    contractChange: function (params, message, clientSession, callback) {
        try {
            var tenantId, response = {};
            var oldContractItem, newContractItem;

            var contractItem = AutoMapper.mapDefinedBySchema('tenantContractChangeModel', INSTANCE_MAPPINGS, params, true);

            async.series([
                // Get tenantId from global session
                function (next) {
                    try {
                        return TenantService.getSessionTenantId(message, clientSession, function (err, sessionTenantId) {
                            if (err) {
                                logger.error('tenantApiFactory.contractChange Error getting tenantId from global session', err);
                                return next(err);
                            }
                            if (!sessionTenantId) {
                                logger.error('tenantApiFactory.contractChange Wrong tenantId returned from global session', sessionTenantId);
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            tenantId = sessionTenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('tenantApiFactory.contractChange Error on getting tenantId from global session', ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        TenantService.retrieveContractForTenant(contractItem.id, tenantId, function (err, contract) {
                            if (err) return setImmediate(next, err);

                            oldContractItem = _.clone(contract);

                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantApiFactory:contractChange could not retrieve old contract', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Update main entity
                function (next) {
                    try {
                        return TenantContract.update(contractItem, { where: { id: contractItem.id } }).then(function (count) {
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
                        TenantService.retrieveContractForTenant(contractItem.id, tenantId, function (err, contract) {
                            if (err) return setImmediate(next, err);

                            newContractItem = _.clone(contract);

                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantApiFactory:contractChange could not retrieve new contract', ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        AuditService.createContractAuditLog(
                            clientSession.getUserId(),
                            oldContractItem,
                            newContractItem,
                            next);
                    } catch (ex) {
                        logger.error('tenantApiFactory:contractChange - audit log create error', ex);
                        return setImmediate(next, ex);
                    }
                },

            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }

                response['contract'] = AutoMapper.mapDefinedBySchema('tenantContractModel', INSTANCE_MAPPINGS, newContractItem);
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {

        }
    },

    /**
     * List invoices for a given tenant.
     * @param params
     * @param callback
     * @returns
     */
    invoiceList: function (params, message, clientSession, callback) {
        try {
            CrudHelper.setupLimitDefaults(params);

            if (_.isEmpty(params.orderBy)) {
                params.orderBy = [];
            }
            if (_.isEmpty(params.searchBy)) {
                params.searchBy = {};
            }
            var mapBySchema = 'tenantInvoiceModel';
            var invoiceItems = null;
            var invoiceCount = 0;

            var orderBy = CrudHelper.orderBy(params, TenantInvoice);
            var searchBy = CrudHelper.searchBy(params, TenantInvoice);

            async.series([
                // Get tenantId from global session
                function (next) {
                    try {
                        return TenantService.getSessionTenantId(message, clientSession, function (err, sessionTenantId) {
                            if (err) {
                                logger.error('Error getting tenantId from global session', err);
                                return next(err);
                            }
                            if (!sessionTenantId) {
                                logger.error('Wrong tenantId returned from global session', sessionTenantId);
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            searchBy.tenantId = sessionTenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('Error on getting tenantId from global session', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Retrieve invoice list by tenantId
                function (next) {
                    return TenantInvoice.findAndCountAll({
                        limit: params.limit == 0 ? null : params.limit,
                        offset: params.offset == 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy
                    }).then(function (invoices) {
                        invoiceItems = AutoMapper.mapListDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, invoices.rows);
                        invoiceCount = invoices.count;
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
                    items: invoiceItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: invoiceCount
                }, callback);
            });
        } catch (ex) {
            logger.error("tenantApiFactory.invoiceList error listing tenant invoices", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

};