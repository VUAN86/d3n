var _ = require('lodash');
var async = require('async');
var uuid = require('node-uuid');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var DateUtils = require('nodejs-utils').DateUtils;
var ProfileApiFactory = require('./profileApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var ConfigurationSetting = Database.RdbmsService.Models.Configuration.Setting;
var Tenant = Database.RdbmsService.Models.Tenant.Tenant;
var TenantAuditLog = Database.RdbmsService.Models.Tenant.TenantAuditLog;
var TenantContract = Database.RdbmsService.Models.Tenant.TenantContract;
var TenantInvoice = Database.RdbmsService.Models.Tenant.TenantInvoice;
var TenantContractAuditLog = Database.RdbmsService.Models.Tenant.TenantContractAuditLog;
var TenantAdminAuditLog = Database.RdbmsService.Models.Tenant.TenantAdminAuditLog;
var Profile = Database.RdbmsService.Models.ProfileManager.Profile;
var ProfileEmail = Database.RdbmsService.Models.ProfileManager.ProfileEmail;
var ProfilePhone = Database.RdbmsService.Models.ProfileManager.ProfilePhone;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var ProfileService = require('../services/profileService.js');
var ProfileSyncService = require('./../services/profileSyncService.js');
var logger = require('nodejs-logger')();
var ApplicationApiFactory = require('./applicationApiFactory.js');
var Application = Database.RdbmsService.Models.Application.Application;
var AerospikeApp = KeyvalueService.Models.AerospikeApp;
var AerospikeTenant = KeyvalueService.Models.AerospikeTenant;
var AerospikeCountryVat = KeyvalueService.Models.AerospikeCountryVat;
var AerospikeEndCustomerInvoiceList = KeyvalueService.Models.AerospikeEndCustomerInvoiceList;
var ClientInfo = require('nodejs-utils').ClientInfo;

var jjv = require('jjv')();
var protocolSchemas = require('nodejs-protocol-schemas');
var schemas = protocolSchemas.schemas;

function makePropertiesRequired(item) {
    var schema, properties;
    
    schema = _.cloneDeep(item);
    schema.required = _.map(schema.properties, function (value, key) {
        return key;
    });

    return schema;
}
var applicationPublishF4mModelRequired = makePropertiesRequired(schemas['application'].definitions.applicationPublishF4mModel);

jjv.addSchema('applicationPublishF4mModel', applicationPublishF4mModelRequired);
jjv.addSchema('tenantConfigurationModel', schemas['tenantManagementConnector'].definitions.tenantConfigurationModel);
jjv.addSchema('tenantInvoiceCreateModel', schemas['tenantManagementConnector'].definitions.tenantInvoiceCreateModel);
jjv.addSchema('tenantInvoiceEndCustomerCreateModel', schemas['tenantManagementConnector'].definitions.tenantInvoiceEndCustomerCreateModel);
jjv.addSchema('vatAddModel', schemas['tenantManagementConnector'].definitions.vatAddModel);
jjv.addSchema('tmcProfileGetRequest', schemas['tenantManagementConnector'].definitions.tmcProfileGetRequest);
jjv.addSchema('tenantAdminContractCreateModel', schemas['tenantManagementConnector'].definitions.tenantAdminContractCreateModel);

var INSTANCE_MAPPINGS = {
    'tenantManagementModel': [
        {
            destination: '$root',
            model: Tenant
        }
    ],
    'tenantCreateManagementModel': [
        {
            destination: '$root',
            model: Tenant
        }
    ],
    'tenantUpdateManagementModel': [
        {
            destination: '$root',
            model: Tenant
        }
    ],
    'tenantAuditLogModel': [
        {
            destination: '$root',
            model: TenantAuditLog
        }
    ],

    // Tenant Contract entities
    'tenantContractModel': [
        {
            destination: '$root',
            model: TenantContract
        }
    ],
    'tenantContractCreateModel': [
        {
            destination: '$root',
            model: TenantContract
        }
    ],
    'tenantContractUpdateModel': [
        {
            destination: '$root',
            model: TenantContract
        }
    ],

    // Tenant Invoice entities
    'tenantInvoiceModel': [
        {
            destination: '$root',
            model: TenantInvoice
        }
    ],
    'tenantInvoiceCreateModel': [
        {
            destination: '$root',
            model: TenantInvoice
        }
    ],
    'tenantInvoiceUpdateModel': [
        {
            destination: '$root',
            model: TenantInvoice
        }
    ],
    
    // Tenant Invoice End Customer entities
    'tenantInvoiceEndCustomerCreateModel': [
        {
            destination: '$root',
            model: TenantInvoice
        }
    ],

    // Tenant Audit Log
    'tenantContractAuditLogModel': [
        {
            destination: '$root',
            model: TenantContractAuditLog
        }
    ],

    // TMC Profile Get
    'tmcProfileGetModel': [
        {
            destination: '$root',
            model: Profile
        },
        {
            destination: 'emails',
            model: ProfileEmail,
            attributes: [
                { 'email': ProfileEmail.tableAttributes.email },
                { 'verificationStatus': ProfileEmail.tableAttributes.verificationStatus }
            ]
        },
        {
            destination: 'phones',
            model: ProfilePhone,
            attributes: [
                { 'phone': ProfilePhone.tableAttributes.phone },
                { 'verificationStatus': ProfilePhone.tableAttributes.verificationStatus }
            ]
        },
    ],
    
};

module.exports = {
    tenantAdminContractCreate: function (req, res, callback) {
        try {
            var self = this;
            var params = req.body;
            var transaction;
            var tenantId;
            var contractId;
            var userId;
            var tenantPublished = false;
            var response = {};
            
            async.series([
                function (next) {
                    try {
                        var err = jjv.validate('tenantAdminContractCreateModel', params, {checkRequired: true});
                        if (err) {
                            logger.error('tenantManagementConnectorApiFactory.tenantAdminContractCreate invalid json request', params, JSON.stringify(err));
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tenantManagementConnectorApiFactory.tenantAdminContractCreate exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },

                /*
                // create/start transaction
                function (next) {
                    CrudHelper.createTransaction({
                        isolationLevel: CrudHelper.RdbmsService.getStorage().Transaction.ISOLATION_LEVELS.SERIALIZABLE
                    }, function (err, t) {
                        if (err) {
                            return next(err);
                        }
                        
                        transaction = t;
                        return next();
                    });
                },
                */
                
                // create tenant
                function (next) {
                    try {
                        self._tenantUpdate({
                            params: params.tenant,
                            transaction: transaction
                        }, function (err, res) {
                            if(err) {
                                return next(err);
                            }

                            tenantId = res.tenant.id;
                            response.tenant = res.tenant;
                            return next();
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // create admin
                function (next) {
                    try {
                        params.administrator.tenantId = tenantId;
                        self._administratorCreate({
                            params: params.administrator,
                            transaction: transaction,
                            ip: req.ip,
                            clientInstances: req.app.locals.clientInstances
                        }, function (err, res) {
                            if (err) {
                                return next(err);
                            }
                            userId = res.administrator.id;
                            response.administrator = res.administrator;
                            return next();
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // create contract
                function (next) {
                    try {
                        params.contract.tenantId = tenantId;
                        self._contractUpdate({
                            params: params.contract,
                            transaction: transaction
                        }, function (err, res) {
                            if (err) {
                                return next(err);
                            }
                            contractId = res.contract.id;
                            response.contract = res.contract;
                            return next();
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                /*
                // commit transaction
                function (next) {
                    transaction.commit().then(function () {return next();}).catch(next);
                }
                */
                
            ], function (err) {
                if (err) {
                    self._tenantAdminContractCreateRollback({
                        transaction: transaction,
                        tenantId: tenantId,
                        userId: userId,
                        contractId: contractId,
                        clientInstances: req.app.locals.clientInstances
                    }, function (errRollback) {
                        if(errRollback) {
                            logger.error('tenantManagementConnectorApiFactory.tenantAdminContractCreate() rollback error:', errRollback, params);
                        } else {
                            logger.debug('tenantManagementConnectorApiFactory.tenantAdminContractCreate() rollback success:', params);
                        }
                        return CrudHelper.callbackError(err, callback);
                    });
                } else {
                    return CrudHelper.callbackSuccess(response, callback);
                }
            });
        } catch (e) {
            logger.error("tenantManagementConnectorApiFactory.tenantAdminContractCreate tc exception", e);
            return CrudHelper.callbackError(e, callback);
        }
        
    },
    
    _tenantAdminContractCreateRollback: function (args, cb) {
        try {
            var transaction = args.transaction;
            var tenantId = args.tenantId;
            var userId = args.userId;
            var contractId = args.contractId;
            var clientInstances = args.clientInstances;

            async.series([
                // delete contract
                function (next) {
                    if(!contractId) {
                        return setImmediate(next);
                    }
                    TenantContract.destroy({where: {id: contractId}}).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // delete user admin roles
                function (next) {
                    if (!userId) {
                        return setImmediate(next);
                    }
                    try {
                        ProfileService.deleteTenantAdmin(
                            clientInstances.authServiceClientInstance,
                            userId,
                            [tenantId],
                            false,
                            next
                        );
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },
                // delete tenant from mysql
                function (next) {
                    try {
                        if(!tenantId) {
                            return setImmediate(next);
                        }
                        Tenant.destroy({where: {id: tenantId}}).then(function () {
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                },

                // unpublish tenant
                function (next) {
                    if(!tenantId) {
                        return setImmediate(next);
                    }
                    AerospikeApp.unpublishApplication({
                        tenant: {
                            id: tenantId
                        }
                    }, function (err) {
                        if(err) {
                            logger.warn('tenantManagementConnectorApiFactory.tenantAdminContractCreate tenantAdminContractCreateRollback unpublishApplication err:', err);
                        }

                        return next();
                    });
                }
            ], cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    /**
     * Get Tenant by ID as requested via HTTP request. This API is used by the tenant
     * admin HTTP service to manage tenant related information. 
     * 
     * @param object
     * @param object
     * @returns
     */
    tenantGet: function (req, res, callback) {
        try {
            var response = {};
            var responseItem = 'tenant';
            var mapBySchema = 'tenantManagementModel';
            var tenantId = null;

            async.series([
                // Check if there is tenant ID defined
                function (next) {
                    try {
                        if (_.has(req.query, 'id') && req.query.id) {
                            tenantId = req.query.id;
                            return setImmediate(next, null);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory:tenantGet tenant id missing in the request query:", JSON.stringify(req.query));
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:tenantGet tenant id get exception");
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                    }
                },
                function (next) {
                    try {
                        var attributes = _.keys(Tenant.attributes);
                        return Tenant.findOne({
                            where: { id: tenantId },
                            attributes: attributes,
                        }).then(function (tenant) {
                            if (!tenant) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                            }
                            var tenantItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, tenant);
                            response[responseItem] = tenantItem;
                            return next();
                        }).catch(function (err) {
                            logger.error("tenantManagementConnectorApiFactory:tenantGet could not retrieve tenant", tenantId, 'error', err);
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
     * List tenants as requested via HTTP request. This API is used by the tenant
     * admin HTTP service to manage tenant related information. 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    tenantList: function (req, res, callback) {
        try {
            var params, orderBy, searchBy;

            params = req.body;

            CrudHelper.setupLimitDefaults(params);

            // Inject default ordering by tenant.id DESC 
            if (_.isEmpty(params.orderBy)) {
                params.orderBy = [{
                    'field': 'createDate',
                    'direction': 'DESC'
                }];
            }
            orderBy = CrudHelper.orderBy(params, Tenant);
            searchBy = CrudHelper.searchBy(params, Tenant);

            return Tenant.findAndCountAll({
                limit: params.limit == 0 ? null : params.limit,
                offset: params.offset == 0 ? null : params.offset,
                where: searchBy,
                order: orderBy
            }).then(function (tenants) {
                var tenantItems = AutoMapper.mapListDefinedBySchema('tenantManagementModel', INSTANCE_MAPPINGS, tenants.rows);
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
            logger.error("tenantManagementConnectorApiFactory.tenantList error listing tenants", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * List tenant audit log entries. 
     * This API is used by the tenant admin HTTP service to manage tenant related information. 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    tenantAuditLogList: function (req, res, callback) {
        try {
            var params, orderBy, searchBy;

            params = req.body;

            CrudHelper.setupLimitDefaults(params);

            // Inject default ordering by tenant.id DESC 
            if (_.isEmpty(params.orderBy)) {
                params.orderBy = [{
                    'field': 'createDate',
                    'direction': 'DESC'
                }];
            }

            if (_.isEmpty(params.searchBy)) {
                params.searchBy = {};
            }

            orderBy = CrudHelper.orderBy(params, TenantAuditLog);
            searchBy = CrudHelper.searchBy(params, TenantAuditLog);

            return TenantAuditLog.findAndCountAll({
                limit: params.limit == 0 ? null : params.limit,
                offset: params.offset == 0 ? null : params.offset,
                where: searchBy,
                order: orderBy
            }).then(function (auditLogs) {
                var tenantAuditLogItems = AutoMapper.mapListDefinedBySchema('tenantAuditLogModel', INSTANCE_MAPPINGS, auditLogs.rows);
                return CrudHelper.callbackSuccess({
                    items: tenantAuditLogItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: auditLogs.count,
                }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            logger.error("tenantManagementConnectorApiFactory.tenantAuditLogList error listing audit logs for a tenant", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Create/Update tenant as requested via HTTP request. This API is used by the tenant
     * admin HTTP service to manage tenant related information. 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    tenantUpdate: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            
            var create = true;
            var mapBySchema = 'tenantCreateManagementModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                mapBySchema = 'tenantUpdateManagementModel';
            }

            var tenantItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);

            async.series([
                function (next) {
                    try {
                        if (create) {
                            
                            tenantItem.createDate = _.now();
                            tenantItem.updateDate = _.now();

                            return Tenant.create(tenantItem).then(function(result) {
                                tenantItem = _.clone(result);
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            })

                        } else {
                            tenantItem.updateDate = _.now();
                            return Tenant.update(tenantItem, { where: { id: tenantItem.id } }).then(function (count) {
                                if (count[0] === 0) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
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
                req.query = { 'id': tenantItem.id };
                return self.tenantGet(req, res, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    _tenantUpdate: function (args, callback) {
        try {
            
            var params = args.params;
            var transaction = args.transaction;
            
            var create = true;
            if (_.has(params, 'id') && params.id) {
                create = false;
            }
            tenantItem = params;
            
            async.series([
                function (next) {
                    try {
                        if (create) {
                            
                            tenantItem.createDate = _.now();
                            tenantItem.updateDate = _.now();

                            return Tenant.create(tenantItem, {transaction: transaction}).then(function(result) {
                                tenantItem = _.clone(result);
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });

                        } else {
                            tenantItem.updateDate = _.now();
                            return Tenant.update(tenantItem, { where: { id: tenantItem.id } , transaction: transaction}).then(function (count) {
                                if (count[0] === 0) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
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
                // publish data for getAppConfiguration
                function (next) {
                    ApplicationApiFactory.buildGetAppConfigurationWithoutApp({tenantId: tenantItem.id, transaction: transaction}, function (err, item) {
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
                    }, {where: where, transaction: transaction}).then(function () {
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
                
            ], function (err) {
                if (err) {
                    return callback(err);
                }
                return callback(false, {tenant: {id: tenantItem.id}});
            });
            
            
        } catch (e) {
            return setImmediate(callback, e);
        }
    },
    
    
    tenantBulkUpdate: function (req, res, callback) {
        var self = this;
        var params = req.body;
        var mapBySchema = 'tenantCreateManagementModel';
        var response = { 'tenant': { }};
        var updateCount = 0;
        var tenantIds = [];

        try {
            var tenantItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            tenantItem.updateDate = _.now();

            async.series([
                function (next) {
                    try {
                        if (_.has(params, 'ids') && _.isArray(params.ids) && params.ids.length > 0) {
                            tenantIds = params.ids;
                            return setImmediate(next, null);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory:tenantBulkUpdate tenant ids missing", params);
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:tenantBulkUpdate - could not ");
                    }
                },
                function (next) {
                    try {
                        return Tenant.update(tenantItem, { where: { id: tenantIds } }).then(function (count) {
                            if (count[0] === 0) {
                                return next(Errors.DatabaseApi.NoRecordFound);
                            }
                            response.tenant.updates = count[0];
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // publish data for getAppConfiguration
                function (next) {
                    
                    function _publish(tenantId, cbItem) {
                        ApplicationApiFactory.buildGetAppConfigurationWithoutApp(tenantId, function (err, item) {
                            if (err) {
                                return next(err);
                            }
                            return AerospikeApp.publishApplication(item, cbItem);
                        });
                    };
                    
                    async.mapSeries(tenantIds, _publish, next);
                },
                // make all tenant apps dirty
                function (next) {
                    function _makeDirty(tenantId, cbItem) {
                        var where = {
                            tenantId: tenantId,
                            status: Application.constants().STATUS_ACTIVE
                        };

                        Application.update({
                            status: Application.constants().STATUS_DIRTY
                        }, {where: where}).then(function () {
                            return cbItem();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, cbItem);
                        });
                    };
                    async.mapSeries(tenantIds, _makeDirty, next);
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
     * Update tenant configuration.
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    tenantConfigurationUpdate: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            var tenantItem;

            var response = {};

            async.series([
                function (next) {
                    try {
                        var err = jjv.validate('tenantConfigurationModel', params, {checkRequired: true} );
                        if (err) {
                            logger.error('tenantManagementConnectorApiFactory.tenantConfigurationUpdate invalid json request', params);
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }

                        tenantItem = AutoMapper.mapDefinedBySchema('tenantConfigurationModel', INSTANCE_MAPPINGS, params, true);
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tenantManagementConnector.tenantConfigurationUpdate exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return AerospikeTenant.update(tenantItem, function (err, tenant) {
                            if (err) {
                                logger.error('tenantManagementConnector.tenantConfigurationUpdate aerospike data update error', err);
                                return setImmediate(next, err);
                            }

                            response.tenant = AutoMapper.mapDefinedBySchema('tenantConfigurationModel', INSTANCE_MAPPINGS, tenant, true);
                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantManagementConnector.tenantConfigurationUpdate - could not update tenant configuration');
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
     * Update paydent api key.
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    tenantPaydentApiKeyUpdate: function (req, res, callback) {
        var self = this;

        try {
            var params = req.body;
            var tenantItem;

            var response = {};

            async.series([
                function (next) {
                    try {
                        // Check if tenant is available in MySQL table
                        return Tenant.findOne({
                            where: { id: params.tenantId }
                        }).then(function (tenant) {
                            if (!tenant) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error("tenantManagementConnectorApiFactory.tenantPaydentApiKeyUpdate could not retrieve mysql tenant", params.tenantId, 'error', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        if (_.has(params, 'apiId') && params.apiId.length > 0) {
                            return setImmediate(next, false);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory.tenantPaydentApiKeyUpdate api key not set", params.apiId);
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }
                    } catch (ex) {
                        logger.error('tenantManagementConnector.tenantPaydentApiKeyUpdate exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },
                // Retrieve tenant entity from aerospike
                function (next) {
                    try {
                        return AerospikeTenant.findOne(params, function (err, tenant) {
                            if (err && err != Errors.DatabaseApi.NoRecordFound) {
                                logger.error('tenantManagementConnector.tenantPaydentApiKeyUpdate aerospike tenant retrieve error', err);
                                return setImmediate(next, err);
                            }

                            // Tenant is not in Aerospike yet
                            if (err) {
                                tenantItem = {
                                    'tenantId': params.tenantId,
                                    'mainCurrency': 'EUR',
                                    'exchangeRates': []
                                }
                            } else {
                                tenantItem = AutoMapper.mapDefinedBySchema('tenantConfigurationModel', INSTANCE_MAPPINGS, tenant, true);
                            }

                            tenantItem.apiId = params.apiId;
                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantManagementConnector.tenantPaydentApiKeyUpdate - could not retrieve tenant configuration from aerospike');
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return AerospikeTenant.update(tenantItem, function (err, tenant) {
                            if (err) {
                                logger.error('tenantManagementConnector.tenantPaydentApiKeyUpdate aerospike data update error', err);
                                return setImmediate(next, err);
                            }

                            response.tenant = AutoMapper.mapDefinedBySchema('tenantConfigurationModel', INSTANCE_MAPPINGS, tenant, true);
                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantManagementConnector.tenantConfigurationUpdate - could not update tenant configuration');
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
     * Update exchange and currency fields.
     * 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    tenantExchangeCurrencyUpdate: function (req, res, callback) {
        var self = this;

        try {
            var params = req.body;
            var tenantItem;

            var response = {};

            async.series([
                function (next) {
                    try {
                        // Check if tenant is available in MySQL table
                        return Tenant.findOne({
                            where: { id: params.tenantId }
                        }).then(function (tenant) {
                            if (!tenant) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                            }
                            return next();
                        }).catch(function (err) {
                            logger.error("tenantManagementConnectorApiFactory.tenantExchangeCurrencyUpdate could not retrieve mysql tenant", params.tenantId, 'error', err);
                            return CrudHelper.callbackError(err, next);
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                // Retrieve tenant entity from aerospike
                function (next) {
                    try {
                        return AerospikeTenant.findOne(params, function (err, tenant) {
                            if (err && err != Errors.DatabaseApi.NoRecordFound) {
                                logger.error('tenantManagementConnector.tenantExchangeCurrencyUpdate aerospike tenant retrieve error', err);
                                return setImmediate(next, err);
                            }

                            // Tenant is not in Aerospike yet
                            if (err) {
                                tenantItem = {
                                    'tenantId': params.tenantId,
                                    'apiId': ''
                                }
                            } else {
                                tenantItem = AutoMapper.mapDefinedBySchema('tenantConfigurationModel', INSTANCE_MAPPINGS, tenant, true);
                            }

                            tenantItem.mainCurrency = params.mainCurrency;
                            tenantItem.exchangeRates = params.exchangeRates;
                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantManagementConnector.tenantExchangeCurrencyUpdate - could not retrieve tenant configuration from aerospike');
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        return AerospikeTenant.update(tenantItem, function (err, tenant) {
                            if (err) {
                                logger.error('tenantManagementConnector.tenantExchangeCurrencyUpdate aerospike data update error', err);
                                return setImmediate(next, err);
                            }

                            response.tenant = AutoMapper.mapDefinedBySchema('tenantConfigurationModel', INSTANCE_MAPPINGS, tenant, true);
                            return setImmediate(next);
                        });
                    } catch (ex) {
                        logger.error('tenantManagementConnector.tenantConfigurationUpdate - could not update tenant configuration');
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
     * Get contract for a given tenant id.
     * 
     * @param object
     * @param object
     * @returns
     */
    contractGet: function (req, res, callback) {
        try {
            var response = {};
            var responseItem = 'contract';
            var mapBySchema = 'tenantContractModel';
            var tenantId = null, contractId = null;

            async.series([
                // Check if there is tenant ID defined
                function (next) {
                    try {
                        if (_.has(req.params, 'tenantId') && req.params.tenantId && 
                            _.has(req.query, 'id') && req.query.id ) {
                            tenantId = req.params.tenantId;
                            contractId = req.query.id; 
                            return setImmediate(next, null);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory:contractGet tenant or contract id missing params:", 
                                JSON.stringify(req.params), "query: ", JSON.stringify(req.query));
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:contractorGet id get exception");
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                    }
                },
                function (next) {
                    try {
                        var attributes = _.keys(TenantContract.attributes);
                        return TenantContract.findOne({
                            where: { id: contractId, tenantId: tenantId },
                            attributes: attributes,
                        }).then(function (contract) {
                            if (!contract) {
                                return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                            }
                            var contractItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, contract);
                            response[responseItem] = contractItem;
                            return next();
                        }).catch(function (err) {
                            logger.error("tenantManagementConnectorApiFactory:contractGet exception retrieving contract", err);
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
     * List contracts for a given tenant.
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    contractList: function (req, res, callback) {
        try {
            var params, orderBy, searchBy;

            params = req.body;

            CrudHelper.setupLimitDefaults(params);

            // Inject default ordering by id DESC 
            if (_.isEmpty(params.orderBy)) {
                params.orderBy = [{
                    'field': 'createDate',
                    'direction': 'DESC'
                }];
            }

            if (_.isEmpty(params.searchBy)) {
                params.searchBy = {};
            }
            params.searchBy.tenantId = req.params.tenantId;

            orderBy = CrudHelper.orderBy(params, TenantContract);
            searchBy = CrudHelper.searchBy(params, TenantContract);

            return TenantContract.findAndCountAll({
                limit: params.limit == 0 ? null : params.limit,
                offset: params.offset == 0 ? null : params.offset,
                where: searchBy,
                order: orderBy
            }).then(function (contracts) {
                var contractItems = AutoMapper.mapListDefinedBySchema('tenantContractModel', INSTANCE_MAPPINGS, contracts.rows);
                return CrudHelper.callbackSuccess({
                    items: contractItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: contracts.count,
                }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            logger.error("tenantManagementConnectorApiFactory.contractList error listing tenant contracts", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Create/Update tenant contract as requested via HTTP request. 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    contractUpdate: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            
            var create = true;
            var tenantId = req.params.tenantId;
            var mapBySchema = 'tenantContractCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                mapBySchema = 'tenantContractUpdateModel';
            }

            var contractItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);

            async.series([
                function (next) {
                    try {
                        if (create) {
                            
                            contractItem.createDate = _.now();
                            contractItem.updateDate = _.now();

                            return TenantContract.create(contractItem).then(function(result) {
                                contractItem = _.clone(result);
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            })

                        } else {
                            contractItem.updateDate = _.now();
                            return TenantContract.update(contractItem, { where: { id: contractItem.id } }).then(function (count) {
                                if (count[0] === 0) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        }
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                req.query = { 'id': contractItem.id };
                return self.contractGet(req, res, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    _contractUpdate: function (args, callback) {
        var self = this;
        try {
            var params = args.params;
            var transaction = args.transaction;
            
            var create = true;
            var tenantId = params.tenantId;
            if (_.has(params, 'id') && params.id) {
                create = false;
            }

            var contractItem = params;

            async.series([
                function (next) {
                    try {
                        if (create) {
                            
                            contractItem.createDate = _.now();
                            contractItem.updateDate = _.now();

                            return TenantContract.create(contractItem, {transaction: transaction}).then(function(result) {
                                contractItem = _.clone(result);
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        } else {
                            contractItem.updateDate = _.now();
                            return TenantContract.update(contractItem, { where: { id: contractItem.id } , transaction: transaction}).then(function (count) {
                                if (count[0] === 0) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }
                                return next();
                            }).catch(function (err) {
                                return CrudHelper.callbackError(err, next);
                            });
                        }
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    return callback(err);
                }
                return callback(false, {contract: {id:contractItem.id}});
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },
    /**
     * List audit logs for a given contract.
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    contractAuditLogList: function (req, res, callback) {
        var self = this;

        try {
            var params, orderBy, searchBy;

            params = req.body;

            CrudHelper.setupLimitDefaults(params);

            if (_.isEmpty(params.orderBy)) {
                params.orderBy = [{
                    'field': 'createDate',
                    'direction': 'DESC'
                }];
            }

            if (_.isEmpty(params.searchBy)) {
                params.searchBy = {};
            }

            orderBy = CrudHelper.orderBy(params, TenantContractAuditLog);
            searchBy = CrudHelper.searchBy(params, TenantContractAuditLog);

            return TenantContractAuditLog.findAndCountAll({
                limit: params.limit == 0 ? null : params.limit,
                offset: params.offset == 0 ? null : params.offset,
                where: searchBy,
                order: orderBy
            }).then(function (logs) {
                var auditLogs = AutoMapper.mapListDefinedBySchema('tenantContractAuditLogModel', INSTANCE_MAPPINGS, logs.rows);
                return CrudHelper.callbackSuccess({
                    items: auditLogs,
                    limit: params.limit,
                    offset: params.offset,
                    total: logs.count,
                }, callback);
            }).catch(function (err) {
                return CrudHelper.callbackError(err, callback);
            });
        } catch (ex) {
            logger.error("tenantManagementConnectorApiFactory.contractAuditLogList error listing tenant contracts", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Get administrator for a given tenant id.
     * 
     * @param object
     * @param object
     * @returns
     */
    administratorGet: function (req, res, callback) {
        try {
            var response = {'administrator': { }};
            var tenantId = null, email = null;

            async.series([
                // Check if there is tenant ID defined
                function (next) {
                    try {
                        if (_.has(req.params, 'tenantId') && req.params.tenantId && 
                            _.has(req.query, 'email') && req.query.email ) {
                            tenantId = req.params.tenantId;
                            email = req.query.email; 
                            return setImmediate(next, null);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory:administratorGet tenant or email missing params:", 
                                JSON.stringify(req.params), "query: ", JSON.stringify(req.query));
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorGet exception");
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                    }
                },
                function (next) {
                    try {
                        CrudHelper.rawQuery(
                            'SELECT p.userId AS id, pe.email, pr.tenantId, p.firstName, p.lastName, pp.phone ' +
                            'FROM profile_email pe ' +
                            'INNER JOIN profile p ON p.userId = pe.profileId ' +
                            'INNER JOIN profile_has_role pr ON pr.profileId = p.userId ' +
                            'LEFT JOIN profile_phone pp ON pp.profileId = p.userId ' +
                            'WHERE pe.email = :email AND pr.tenantId = :tenantId AND pr.role = :role LIMIT 1', 
                            { 
                                email: email, 
                                tenantId: tenantId, 
                                role: ProfileHasRole.constants().ROLE_ADMIN 
                            },
                            function (err, data) {
                                if (err) {
                                    return next(Errors.QuestionApi.FatalError);
                                }

                                // No record found, no admin setup return 404
                                if (_.isEmpty(data)) {
                                    logger.info("tenantManagementConnectorApiFactory.administratorGet no admin found for given email and tenantId", email, tenantId);
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }

                                response.administrator = _.head(data);
                                return next(false);
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
     * Impersonate the given administrator for the given tenantId
     * 
     * @param object
     * @param object
     * @returns
     */
    administratorImpersonate: function (req, res, callback) {
        try {
            var response = {'token': ''};
            var tenantId = null, email = null;
            var token = '';

            async.series([
                // Check if there is tenant ID defined and email supplied
                function (next) {
                    try {
                        if (_.has(req.params, 'tenantId') && req.params.tenantId && 
                            _.has(req.query, 'email') && req.query.email ) {
                            tenantId = req.params.tenantId;
                            email = req.query.email;
                            return setImmediate(next, null);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory.administratorImpersonate tenant or email missing", JSON.stringify(req.params), "query:", JSON.stringify(req.query));
                            return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory.administratorImpersonate validation exception");
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, next);
                    }
                },
                function(next) {
                    try {
                        var authClient = req.app.locals.clientInstances.authServiceClientInstance;
                        authClient.generateImpersonateToken({
                            email: email,
                            tenantId: tenantId,
                            waitResponse: true,
                            clientInfo: null
                        }, function (err, data) {
                            if (err) {
                                logger.error("tenantManagementConnectorApiFactory.administratorImpersonate AuthService.generateImpersonateToken", email, tenantId, ex);
                                return setImmediate(next, err);
                            }

                            if (_.has(data.getContent(), 'token')) {
                                response.token = data.getContent().token;
                                return setImmediate(next);
                            } else {
                                logger.error("tenantManagementModel.administratorImpersonate AuthService.generateImpersonateToken no token returned", data);
                                return setImmediate(next, Errors.DatabaseApi.NoRecordFound);
                            }
                        });
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorCreate AuthService.generateImpersonateToken auth service exception", ex);
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
     * Create a new administrator for the given tenant.
     * 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    administratorCreate: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            var tenantId = params.tenantId;
            var mapBySchema = 'tenantAdministratorModel';
            var fakeClientInfo = createFakeClientInfo(req.ip);

            var adminItem = params;

            var emailEntry = null, profileEntry = null, phoneEntry = null, tenantId = null;
            var AerospikeProfile;

            async.series([
                // Validate that tenantId is specified
                function (next) {
                    try {
                        if (_.has(adminItem, 'tenantId') && adminItem.tenantId > 0 && 
                            _.isString(adminItem.email) && adminItem.email.length >= 5 ) { // TODO: add proper e-mail validation here
                            tenantId = adminItem.tenantId;
                            return setImmediate(next);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory:administratorCreate - email or tenantId missing", JSON.stringify(adminItem));
                            return setImmediate(next, Errors.QuestionApi.FatalError);
                        }
                    } catch(ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorCreate - exception retrieving email or tenantId", adminItem, ex);
                        return setImmediate(next, ex);
                    }
                },

                /*
                 * Call account creation will do the following:
                 * A. Already existing account
                 *    - will check if phone exists and update it, will sync this back to mysql
                 *    - will update person information if not set
                 * B. New account
                 *    - create a new user account
                 *    - setup role as registered
                 */
                function (next) {
                    try {
                        // Try to add/update user in Aerospike and sync it
                        async.series([
                            function(nextAuth) {
                                try {
                                    authClient = req.app.locals.clientInstances.authServiceClientInstance;
                                    authClient.addConfirmedUser({
                                        userInfo: adminItem,
                                        waitResponse: true,
                                        clientInfo: fakeClientInfo
                                    }, function (err, data) {
                                        if (err) {
                                            logger.error("tenantManagementConnectorApiFactory:administratorCreate AuthService.addConfirmedUser", adminItem, ex);
                                            return setImmediate(nextAuth, err);
                                        }

                                        if (_.has(data.getContent(), 'profile.userId')) {
                                            AerospikeProfile = data.getContent().profile;
                                            return setImmediate(nextAuth);
                                        } else {
                                            logger.error("tenantManagementModel:administratorCreate AuthService.addConfirmedUser profile has no userId", data);
                                            return setImmediate(nextAuth, Errors.QuestionApi.FatalError);
                                        }
                                    });
                                } catch (ex) {
                                    logger.error("tenantManagementConnectorApiFactory:administratorCreate AuthService.addConfirmedUser exception", ex);
                                    return setImmediate(nextAuth, ex);
                                }
                            },

                            function(nextAuth) {
                                try {
                                    return ProfileSyncService.profileSync({ usersIds: [AerospikeProfile.userId] }, req.app.locals.clientInstances, function (err) {
                                        if (err) {
                                            logger.error("tenantManagementConnectorApiFactory:administratorCreate profileSync", err);
                                            return setImmediate(nextAuth, err);
                                        }
                                        return setImmediate(nextAuth);
                                    });
                                } catch (ex) {
                                    logger.error("tenantManagementConnectorApiFactory:administratorCreate ProfileSyncService.profileSync exception", ex);
                                    return setImmediate(nextAuth, ex);
                                }
                            },
                        ], function (err) {
                            try {
                                if (err) {
                                    logger.error("tenantManagementConnectorApiFactory:administratorCreate - exception creating or syncing user", err);
                                    return setImmediate(next, err);
                                }

                                return setImmediate(next);
                            } catch (ex) {
                                logger.error("tenantManagementConnectorApiFactory:administratorCreate addConfirmedUser exception", ex);
                                return setImmediate(next, ex);
                            }
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory:administratorCreate - exception account update', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Retrieve email and identify the admin account using that id
                function (next) {
                    try {
                        ProfileEmail.findOne({where: {email: adminItem.email}})
                          .then(function(profileEmail) {
                              if (!profileEmail) {
                                  logger.info("tenantManagementConnectorApiFactory:administratorCreate - email not found, will create a new entry", adminItem);
                                  return setImmediate(next);
                              }
                              emailEntry = profileEmail;
                              return setImmediate(next);
                          })
                          .catch(function (err) {
                              logger.error("tenantManagementConnectorApiFactory:administratorCreate - could not retrieve profile by id", adminItem, err);
                              return CrudHelper.callbackError(err, next);
                          });
                    } catch(ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorCreate - exception retrieving profile via email", adminItem, ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        if (emailEntry) {
                            return Profile.findOne({where: { userId: emailEntry.profileId }})
                                .then(function(profile) {
                                    if (!profile) {
                                        logger.error("tenantManagementConnectorApiFactory:administratorCreate - no profile found", adminItem, ex);
                                        return setImmediate(next, Errors.DatabaseApi.NoRecordFound);
                                    }
                                    profileEntry = profile;
                                    return setImmediate(next);
                                })
                                .catch(function (err) {
                                    logger.error("tenantManagementConnectorApiFactory:administratorCreate - could not retrieve profile by id", adminItem, err);
                                    return CrudHelper.callbackError(err, next);
                                });
                        } else {
                            
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorCreate - exception creating new profile entry", ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        ProfileService.setTenantAdmin(
                            req.app.locals.clientInstances.authServiceClientInstance,
                            profileEntry.userId,
                            adminItem.tenantId,
                            next
                        )
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorCreate - setup admin if missing error", ex);
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                try {
                    if (err) {
                        return CrudHelper.callbackError(err, callback);
                    }

                    var response = {
                        administrator: {
                            'id': profileEntry.userId,
                            'tenantId': tenantId,
                            'email': adminItem.email,
                            'phone': adminItem.phone,
                            'firstName': adminItem.firstName,
                            'lastName': adminItem.lastName
                        }
                    };

                    return CrudHelper.callbackSuccess(response, callback);
                } catch (ex) {
                    logger.error("tenantManagementConnectorApiFactory:administratorCreate - error returning result", ex);
                    return CrudHelper.callbackError(ex, callback);
                }
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    _administratorCreate: function (args, callback) {
        var self = this;
        try {
            var params = args.params;
            var transaction = args.transaction;
            var ip = args.ip;
            var clientInstances = args.clientInstances;
            
            var tenantId = params.tenantId;
            var mapBySchema = 'tenantAdministratorModel';
            var fakeClientInfo = createFakeClientInfo(ip);

            var adminItem = params;

            var emailEntry = null, profileEntry = null, phoneEntry = null, tenantId = null;
            var AerospikeProfile;

            async.series([
                // Validate that tenantId is specified
                function (next) {
                    try {
                        if (_.has(adminItem, 'tenantId') && adminItem.tenantId > 0 && 
                            _.isString(adminItem.email) && adminItem.email.length >= 5 ) { // TODO: add proper e-mail validation here
                            tenantId = adminItem.tenantId;
                            return setImmediate(next);
                        } else {
                            logger.error("tenantManagementConnectorApiFactory:_administratorCreate - email or tenantId missing", JSON.stringify(adminItem));
                            return setImmediate(next, Errors.QuestionApi.FatalError);
                        }
                    } catch(ex) {
                        logger.error("tenantManagementConnectorApiFactory:_administratorCreate - exception retrieving email or tenantId", adminItem, ex);
                        return setImmediate(next, ex);
                    }
                },

                /*
                 * Call account creation will do the following:
                 * A. Already existing account
                 *    - will check if phone exists and update it, will sync this back to mysql
                 *    - will update person information if not set
                 * B. New account
                 *    - create a new user account
                 *    - setup role as registered
                 */
                function (next) {
                    try {
                        // Try to add/update user in Aerospike and sync it
                        async.series([
                            function(nextAuth) {
                                try {
                                    var authClient = clientInstances.authServiceClientInstance;
                                    authClient.addConfirmedUser({
                                        userInfo: adminItem,
                                        waitResponse: true,
                                        clientInfo: fakeClientInfo
                                    }, function (err, data) {
                                        if (err) {
                                            logger.error("tenantManagementConnectorApiFactory:_administratorCreate AuthService.addConfirmedUser", adminItem, err);
                                            return setImmediate(nextAuth, err);
                                        }

                                        if (_.has(data.getContent(), 'profile.userId')) {
                                            AerospikeProfile = data.getContent().profile;
                                            return setImmediate(nextAuth);
                                        } else {
                                            logger.error("tenantManagementModel:_administratorCreate AuthService.addConfirmedUser profile has no userId", data);
                                            return setImmediate(nextAuth, Errors.QuestionApi.FatalError);
                                        }
                                    });
                                } catch (ex) {
                                    logger.error("tenantManagementConnectorApiFactory:_administratorCreate AuthService.addConfirmedUser exception", ex);
                                    return setImmediate(nextAuth, ex);
                                }
                            },

                            function(nextAuth) {
                                try {
                                    return ProfileSyncService.profileSync({ usersIds: [AerospikeProfile.userId] }, clientInstances, function (err) {
                                        if (err) {
                                            logger.error("tenantManagementConnectorApiFactory:_administratorCreate profileSync", err);
                                            return setImmediate(nextAuth, err);
                                        }
                                        return setImmediate(nextAuth);
                                    });
                                } catch (ex) {
                                    logger.error("tenantManagementConnectorApiFactory:_administratorCreate ProfileSyncService.profileSync exception", ex);
                                    return setImmediate(nextAuth, ex);
                                }
                            },
                        ], function (err) {
                            try {
                                if (err) {
                                    logger.error("tenantManagementConnectorApiFactory:_administratorCreate - exception creating or syncing user", err);
                                    return setImmediate(next, err);
                                }

                                return setImmediate(next);
                            } catch (ex) {
                                logger.error("tenantManagementConnectorApiFactory:_administratorCreate addConfirmedUser exception", ex);
                                return setImmediate(next, ex);
                            }
                        });
                    } catch (ex) {
                        logger.error('profileManagerApiFactory:_administratorCreate - exception account update', ex);
                        return setImmediate(next, ex);
                    }
                },

                // Retrieve email and identify the admin account using that id
                function (next) {
                    try {
                        ProfileEmail.findOne({where: {email: adminItem.email}, transaction: transaction})
                          .then(function(profileEmail) {
                              
                              if (!profileEmail) {
                                  logger.info("tenantManagementConnectorApiFactory:_administratorCreate - email not found, will create a new entry", adminItem);
                                  return setImmediate(next);
                              }
                              emailEntry = profileEmail;
                              return setImmediate(next);
                          })
                          .catch(function (err) {
                              logger.error("tenantManagementConnectorApiFactory:_administratorCreate - could not retrieve profile by id", adminItem, err);
                              return CrudHelper.callbackError(err, next);
                          });
                    } catch(ex) {
                        logger.error("tenantManagementConnectorApiFactory:_administratorCreate - exception retrieving profile via email", adminItem, ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        if (emailEntry) {
                            return Profile.findOne({where: { userId: emailEntry.profileId }, transaction: transaction})
                                .then(function(profile) {
                                    if (!profile) {
                                        logger.error("tenantManagementConnectorApiFactory:_administratorCreate - no profile found", adminItem, emailEntry.profileId);
                                        return setImmediate(next, Errors.DatabaseApi.NoRecordFound);
                                    }
                                    profileEntry = profile;
                                    return setImmediate(next);
                                })
                                .catch(function (err) {
                                    logger.error("tenantManagementConnectorApiFactory:_administratorCreate - could not retrieve profile by id", adminItem, err);
                                    return CrudHelper.callbackError(err, next);
                                });
                        } else {
                            
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:_administratorCreate - exception creating new profile entry", ex);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        ProfileService.setTenantAdmin(
                            clientInstances.authServiceClientInstance,
                            profileEntry.userId,
                            adminItem.tenantId,
                            next
                        );
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:_administratorCreate - setup admin if missing error", ex);
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                try {
                    if (err) {
                        return callback(err);
                    }

                    var response = {
                        administrator: {
                            'id': profileEntry.userId
                        }
                    };
                    
                    return callback(false, response);
                } catch (ex) {
                    logger.error("tenantManagementConnectorApiFactory:_administratorCreate - error returning result", ex);
                    return callback(ex);
                }
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },
    /**
     * Delete administrator role for a given tenant or all tenants.
     * The original user entry is not being deleted only it's role in the tenant system.
     * 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    administratorDelete: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;

            var email = null;
            var tenantIds = _.isEmpty(params.tenantIds) ? [] : params.tenantIds;
            var allTenants = _.isBoolean(params.allTenants) ? params.allTenants : false;
            var userId = false;

            async.series([
                // Validate that tenantId is specified
                function (next) {
                    try {
                        // TODO: add proper e-mail validation here
                        if (_.isString(params.email) && params.email.length >= 5 ) { 
                            return ProfileEmail.findOne({where: { email: params.email }})
                                .then(function(pe) {
                                    if (!pe) {
                                        logger.error("tenantManagementConnectorApiFactory:administratorDelete - no profile via email found", params.email, ex);
                                        return setImmediate(next, Errors.DatabaseApi.NoRecordFound);
                                    }
                                    userId = pe.profileId;
                                    return setImmediate(next);
                                })
                                .catch(function (err) {
                                    logger.error("tenantManagementConnectorApiFactory:administratorDelete - could not retrieve profile by id", adminItem, err);
                                    return CrudHelper.callbackError(err, next);
                                });
                        } else {
                            logger.error("tenantManagementConnectorApiFactory:administratorDelete - email missing or invalid", JSON.stringify(params));
                            return setImmediate(next, Errors.QuestionApi.FatalError);
                        }
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorDelete - exception retrieving profile id by email", params, ex);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        ProfileService.deleteTenantAdmin(
                            req.app.locals.clientInstances.authServiceClientInstance,
                            userId,
                            tenantIds,
                            allTenants,
                            next
                        )
                    } catch (ex) {
                        logger.error("tenantManagementConnectorApiFactory:administratorDelete - delete admin error", ex);
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                try {
                    if (err) {
                        return CrudHelper.callbackError(err, callback);
                    }

                    var response = {
                        administrator: {
                            'id': userId
                        }
                    };

                    return CrudHelper.callbackSuccess(response, callback);
                } catch (ex) {
                    logger.error("tenantManagementConnectorApiFactory:administratorDelete - error returning result", ex);
                    return CrudHelper.callbackError(ex, callback);
                }
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }        
    },

    /**
     * List audit logs for administration user changes.
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    administratorAuditLogList: function (req, res, callback) {
        try {
            var params, email, orderBy, searchBy;

            params = req.body;

            CrudHelper.setupLimitDefaults(params);

            // Inject default ordering by id DESC 
            if (_.isEmpty(params.orderBy)) {
                params.orderBy = [{
                    'field': 'createDate',
                    'direction': 'DESC'
                }];
            }

            orderBy = CrudHelper.orderBy(params, TenantContractAuditLog);
            searchBy = CrudHelper.searchBy(params, TenantContractAuditLog);

            return TenantAdminAuditLog.findAndCountAll({
                limit: params.limit == 0 ? null : params.limit,
                offset: params.offset == 0 ? null : params.offset,
                where: searchBy,
                order: orderBy
            }).then(function (logs) {
                var auditLogs = AutoMapper.mapListDefinedBySchema('tenantAdministratorAuditLogModel', INSTANCE_MAPPINGS, logs.rows);
                return CrudHelper.callbackSuccess({
                    items: auditLogs,
                    limit: params.limit,
                    offset: params.offset,
                    total: logs.count,
                }, callback);
            })
            .catch(function (ex) {
                logger.error("tenantManagementConnectorApiFactory.administratorAuditLogList db find error", ex);
                return CrudHelper.callbackError(ex, callback);
            });
        } catch (ex) {
            logger.error("tenantManagementConnectorApiFactory.administratorAuditLogList error administrator trail", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Update configuration setting related to f4m legal entity.
     * 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    f4mConfigurationUpdate: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            var result = {};

            async.series([
                function (next) {
                    try {
                        var err = jjv.validate('applicationPublishF4mModel', params, {checkRequired: true});
                        if (err) {
                            logger.error('tenantManagementConnectorApiFactory.f4mConfigurationUpdate invalid json request', params);
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tenantManagementConnectorApiFactory.f4mConfigurationUpdate exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },
                function (next) {
                    try {
                        var configurationData = {
                            id: ConfigurationSetting.constants().ID_F4M_LEGAL,
                            data: params
                        };
                        ConfigurationSetting.upsert(configurationData)
                            .then(function(status) {
                                return setImmediate(next, null);
                            })
                            .catch(function (err) {
                                logger.error('tenantManagementConnectorApiFactory.f4mConfigurationUpdate could not  save configuration setting', err);
                                return CrudHelper.callbackError(err, next);
                            });
                    } catch (ex) {
                        logger.error('tenantManagementConnectorApiFactory.f4mConfigurationUpdate exception saving configuration setting', ex);
                        return setImmediate(next, ex);
                    }
                },
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess(params, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    /**
     * Create tenant invoice as requested via HTTP request. 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    invoiceUpdate: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            var mapBySchema = 'tenantInvoiceCreateModel';
            var invoiceItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            
            async.series([
                function (next) {
                    try {
                        var err = jjv.validate(mapBySchema, params, {checkRequired: true});
                        if (err) {
                            logger.error('tenantManagementConnectorApiFactory.invoiceUpdate invalid json request', params);
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tenantManagementConnectorApiFactory.invoiceUpdate exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },
                
                function (next) {
                    try {
                        return TenantInvoice.create(invoiceItem).then(function (result) {
                            invoiceItem = _.clone(result);
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
                return CrudHelper.callbackSuccess({}, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    /**
     * Create tenant invoice for end customer data as requested via HTTP request. 
     * @param {object} req
     * @param {object} res
     * @param {callback} callback
     * @returns
     */
    invoiceEndCustomerUpdate: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            var mapBySchema = 'tenantInvoiceEndCustomerCreateModel';
            var invoiceItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);

            async.series([
                function (next) {
                    try {
                        var err = jjv.validate(mapBySchema, params, { checkRequired: true });
                        if (err) {
                            logger.error('tenantManagementConnectorApiFactory.invoiceEndCustomerUpdate invalid json request', params);
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tenantManagementConnectorApiFactory.invoiceEndCustomerUpdate exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },

                function (next) {
                    try {
                        invoiceItem.id = uuid.v4();
                        return AerospikeEndCustomerInvoiceList.publish(invoiceItem, next);
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({}, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    vatAdd: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            
            var currentVats = [];
            async.series([
                function (next) {
                    try {
                        var err = jjv.validate('vatAddModel', params, {checkRequired: true});
                        if (err) {
                            logger.error('tenantManagementConnectorApiFactory.vatAddModel invalid json request', params, err);
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tenantManagementConnectorApiFactory.vatAddModel exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },
                
                function (next) {
                    try {
                        params.country = params.country.toUpperCase();
                        AerospikeCountryVat.findOne({country: params.country}, function (err, result) {
                            if (err) {
                                if (err === Errors.DatabaseApi.NoRecordFound) {
                                    currentVats = [];
                                } else {
                                    return next(err);
                                }
                            } else {
                                currentVats = result.vats;
                            }
                            
                            return next();
                        });
                    } catch (ex) {
                        return setImmediate(next, ex);
                    }
                },
                
                function (next) {
                    try {
                        var newVats = currentVats.slice(0);
                        var newVatStartDate = parseInt(Date.parse(params.startDate)/1000);
                        var newVatEndDate = params.endDate;
                        if (newVatEndDate === null || _.isUndefined(newVatEndDate)) {
                            newVatEndDate = null;
                            // set endDate for current/active vat
                            for(var i=0; i<newVats.length; i++) {
                                var vat = newVats[i];
                                if (vat.endDate === null) {
                                    vat.endDate = newVatStartDate-1;
                                    break;
                                }
                            }
                            
                            newVats.push({
                                percent: params.percent,
                                startDate: newVatStartDate,
                                endDate: newVatEndDate
                            });
                            
                        } else {
                            newVatEndDate = parseInt(newVatEndDate/1000);
                            newVats.push({
                                percent: params.percent,
                                startDate: newVatStartDate,
                                endDate: newVatEndDate
                            });
                        }
                        AerospikeCountryVat.save({
                            country: params.country,
                            vats: newVats
                        }, next);
                        
                    } catch (e) {
                        return setImmediate(next, e);
                    }
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({}, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
    
    profileGet: function (req, res, callback) {
        var self = this;
        try {
            var params = req.body;
            var response = {};
            var responseItem = 'profile';
            var mapBySchema = 'tmcProfileGetModel';
            async.series([
                function (next) {
                    try {
                        var err = jjv.validate('tmcProfileGetRequest', params, {checkRequired: true});
                        if (err) {
                            logger.error('tenantManagementConnectorApiFactory.profileGet invalid json request', params, err);
                            return setImmediate(next, 'ERR_VALIDATION_FAILED');
                        }
                        return setImmediate(next);
                    } catch (ex) {
                        logger.error('tenantManagementConnectorApiFactory.profileGet exception validating input data', ex, params);
                        return setImmediate(next, ex);
                    }
                },
                
                function (next) {
                    try {
                        var attributes = _.keys(Profile.attributes);
                        var include = CrudHelper.include(Profile, [
                            ProfileEmail,
                            ProfilePhone
                        ]);
                        
                        Profile.findOne({
                            where: { userId: params.userId },
                            attributes: attributes,
                            include: include
                        }).then(function (profile) {
                            try {
                                if (!profile) {
                                    return next(Errors.DatabaseApi.NoRecordFound);
                                }
                                var profileItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, profile);
                                response[responseItem] = profileItem;
                                return next();
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
                return CrudHelper.callbackSuccess(response, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },
};

function createFakeClientInfo(ip) {
    var clientInfo = new ClientInfo();
    clientInfo.setAppId('0');
    clientInfo.setTenantId('0');
    clientInfo.setIp(ip);

    return clientInfo.getClientInfo();
};