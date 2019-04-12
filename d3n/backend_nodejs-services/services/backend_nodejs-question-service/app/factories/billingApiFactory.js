var logger = require('nodejs-logger')();
var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var PaymentStructure = Database.RdbmsService.Models.Billing.PaymentStructure;
var PaymentStructureTier = Database.RdbmsService.Models.Billing.PaymentStructureTier;
var PaymentAction = Database.RdbmsService.Models.Billing.PaymentAction;
var PaymentTenantBillOfMaterial = Database.RdbmsService.Models.Billing.PaymentTenantBillOfMaterial;
var PaymentResourceBillOfMaterial = Database.RdbmsService.Models.Billing.PaymentResourceBillOfMaterial;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var WorkorderHasTenant = Database.RdbmsService.Models.Workorder.WorkorderHasTenant;
var Question = Database.RdbmsService.Models.Question.Question;
var QuestionTranslation = Database.RdbmsService.Models.Question.QuestionTranslation;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'paymentStructureModel': [
        {
            destination: '$root',
            model: PaymentStructure
        },
        {
            destination: 'tiersIds',
            model: PaymentStructureTier,
            attribute: PaymentStructureTier.tableAttributes.id
        },
    ],
    'paymentStructureCreateModel': [
        {
            destination: '$root',
            model: PaymentStructure
        },
        {
            destination: 'tiersIds',
            model: PaymentStructureTier,
            attribute: PaymentStructureTier.tableAttributes.id
        },
    ],
    'paymentStructureTierModel': [
        {
            destination: '$root',
            model: PaymentStructureTier
        }
    ],
    'paymentStructureTierCreateModel': [
        {
            destination: '$root',
            model: PaymentStructureTier
        },
    ],
    'paymentTenantBillOfMaterialModel': [
        {
            destination: '$root',
            model: PaymentTenantBillOfMaterial
        },
        {
            destination: 'actionsIds',
            model: PaymentAction,
            attribute: PaymentAction.tableAttributes.id
        },
    ],
    'paymentTenantBillOfMaterialCreateModel': [
        {
            destination: '$root',
            model: PaymentTenantBillOfMaterial
        },
        {
            destination: 'actionsIds',
            model: PaymentAction,
            attribute: PaymentAction.tableAttributes.id
        },
    ],
    'paymentResourceBillOfMaterialModel': [
        {
            destination: '$root',
            model: PaymentResourceBillOfMaterial
        },
        {
            destination: 'actionsIds',
            model: PaymentAction,
            attribute: PaymentAction.tableAttributes.id
        },
    ],
    'paymentResourceBillOfMaterialCreateModel': [
        {
            destination: '$root',
            model: PaymentResourceBillOfMaterial
        },
        {
            destination: 'actionsIds',
            model: PaymentAction,
            attribute: PaymentAction.tableAttributes.id
        },
    ],
    'paymentActionModel': [
        {
            destination: '$root',
            model: PaymentAction
        }
    ],
    'paymentActionCreateModel': [
        {
            destination: '$root',
            model: PaymentAction
        },
    ],
};
INSTANCE_MAPPINGS['paymentStructureUpdateModel'] = INSTANCE_MAPPINGS['paymentStructureCreateModel'];
INSTANCE_MAPPINGS['paymentStructureTierUpdateModel'] = INSTANCE_MAPPINGS['paymentStructureTierCreateModel'];
INSTANCE_MAPPINGS['paymentTenantBillOfMaterialUpdateModel'] = INSTANCE_MAPPINGS['paymentTenantBillOfMaterialCreateModel'];
INSTANCE_MAPPINGS['paymentResourceBillOfMaterialUpdateModel'] = INSTANCE_MAPPINGS['paymentResourceBillOfMaterialCreateModel'];
INSTANCE_MAPPINGS['paymentActionUpdateModel'] = INSTANCE_MAPPINGS['paymentActionCreateModel'];

module.exports = {

    /**
     *
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var paymentStructureId = undefined;
            var mapBySchema = 'paymentStructureCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                paymentStructureId = params.id;
                mapBySchema = 'paymentStructureUpdateModel';
            }
            var paymentStructureItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
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
                // Create/update main entity
                function (next) {
                    if (create) {
                        paymentStructureItem.tenantId = sessionTenantId;
                        if (!_.has(paymentStructureItem, 'createDate')) {
                            paymentStructureItem.createDate = _.now();
                        }
                        // Creator not set -> set current user
                        if (!_.has(paymentStructureItem, 'creatorResourceId')) {
                            paymentStructureItem.creatorResourceId = clientSession.getUserId();
                        }
                        return PaymentStructure.create(paymentStructureItem).then(function (paymentStructureModel) {
                            paymentStructureId = paymentStructureModel.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return PaymentStructure.update(paymentStructureItem, { where: { id: paymentStructureItem.id, tenantId: sessionTenantId } }).then(function (count) {
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
                params.id = paymentStructureId;
                return self.paymentStructureGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureDelete: function (params, callback) {
        try {
            return PaymentStructure.destroy({ where: { id: params.id } }).then(function (count) {
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

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureGet: function (params, message, clientSession, callback) {
        try {
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return PaymentStructure.findOne({where: { id: params.id, tenantId: tenantId }}).then(function (paymentStructureModel) {
                    if (!paymentStructureModel) {
                        return CrudHelper.callbackError(Errors.DatabaseApi.NoRecordFound, callback);
                    }
                    var paymentStructureItem = AutoMapper.mapBySchema('paymentStructureModel', INSTANCE_MAPPINGS, paymentStructureModel);
                    return CrudHelper.callbackSuccess({ paymentStructure: paymentStructureItem }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
            
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureList: function (params, message, clientSession, callback) {
        try {
            var orderBy = CrudHelper.orderBy(params, PaymentStructure);
            var searchBy = CrudHelper.searchBy(params, PaymentStructure);
            TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                searchBy.tenantId = tenantId;
                return PaymentStructure.findAndCountAll({
                    limit: params.limit === 0 ? null : params.limit,
                    offset: params.limit === 0 ? null : params.offset,
                    where: searchBy,
                    order: orderBy,
                    subQuery: false
                }).then(function (paymentStructureModels) {
                    var paymentStructureItems = AutoMapper.mapListBySchema('paymentStructureModel', INSTANCE_MAPPINGS, paymentStructureModels.rows);
                    return CrudHelper.callbackSuccess({
                        items: paymentStructureItems,
                        limit: params.limit,
                        offset: params.offset,
                        total: paymentStructureModels.count,
                    }, callback);
                }).catch(function (err) {
                    return CrudHelper.callbackError(err, callback);
                });
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureSetActive: function (params, callback) {
        var self = this;
        try {
            return PaymentStructure.update({ isActive: params.isActive }, { where: { id: params.id } }).then(function (count) {
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
     *
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureTierUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var paymentStructureTierId = undefined;
            var mapBySchema = 'paymentStructureTierCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                paymentStructureTierId = params.id;
                mapBySchema = 'paymentStructureTierUpdateModel';
            }
            var paymentStructureTierItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // Create/update main entity
                function (next) {
                    if (create) {
                        return PaymentStructureTier.create(paymentStructureTierItem).then(function (paymentStructureTierModel) {
                            paymentStructureTierId = paymentStructureTierModel.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return PaymentStructureTier.update(paymentStructureTierItem, { where: { id: paymentStructureTierItem.id } }).then(function (count) {
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
                params.id = paymentStructureTierId;
                return self.paymentStructureTierGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureTierDelete: function (params, callback) {
        try {
            return PaymentStructureTier.destroy({ where: { id: params.id } }).then(function (count) {
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

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureTierGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var paymentStructureTierItem;
        try {
            var include = CrudHelper.include(PaymentStructureTier, [
                PaymentStructure
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get payment structure record
                function (next) {
                    include = CrudHelper.includeWhere(include, PaymentStructure, { tenantId: sessionTenantId }, true);
                    return PaymentStructureTier.findOne({
                        where: { id: params.id },
                        include: include,
                        subQuery: false
                    }).then(function (paymentStructureTierModel) {
                        if (!paymentStructureTierModel) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        paymentStructureTierItem = AutoMapper.mapBySchema('paymentStructureTierModel', INSTANCE_MAPPINGS, paymentStructureTierModel);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ paymentStructureTier: paymentStructureTierItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentStructureTierList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var paymentStructureTierItems = [];
        var paymentStructureTierTotal = 0;
        try {
            var orderBy = CrudHelper.orderBy(params, PaymentStructureTier);
            var searchBy = CrudHelper.searchBy(params, PaymentStructureTier);
            var include = CrudHelper.include(PaymentStructureTier, [
                PaymentStructure
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get payment structure tier record
                function (next) {
                    include = CrudHelper.includeWhere(include, PaymentStructure, { tenantId: sessionTenantId }, true);
                    return PaymentStructureTier.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (paymentStructureTierModels) {
                        paymentStructureTierTotal = paymentStructureTierModels.count;
                        paymentStructureTierItems = AutoMapper.mapListBySchema('paymentStructureTierModel', INSTANCE_MAPPINGS, paymentStructureTierModels.rows);
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
                    items: paymentStructureTierItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: paymentStructureTierTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     *
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentTenantBillOfMaterialUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var paymentTenantBillOfMaterialId = undefined;
            var mapBySchema = 'paymentTenantBillOfMaterialCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                paymentTenantBillOfMaterialId = params.id;
                mapBySchema = 'paymentTenantBillOfMaterialUpdateModel';
            }
            var paymentTenantBillOfMaterialItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
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
                            paymentTenantBillOfMaterialItem.tenantId = sessionTenantId;
                            return next();
                        });
                    } catch (ex) {
                        logger.error('Error on getting tenantId from global session', ex);
                        return setImmediate(next, ex);
                    }
                },
                
                // Create/update main entity
                function (next) {
                    if (create) {
                        paymentTenantBillOfMaterialItem.status = PaymentTenantBillOfMaterial.constants().STATUS_UNAPPROVED;
                        return PaymentTenantBillOfMaterial.create(paymentTenantBillOfMaterialItem).then(function (paymentTenantBillOfMaterialModel) {
                            paymentTenantBillOfMaterialId = paymentTenantBillOfMaterialModel.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        paymentTenantBillOfMaterialItem.status = PaymentTenantBillOfMaterial.constants().STATUS_CHANGED;
                        return PaymentTenantBillOfMaterial.update(paymentTenantBillOfMaterialItem, { where: { id: paymentTenantBillOfMaterialItem.id } }).then(function (count) {
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
                params.id = paymentTenantBillOfMaterialId;
                return self.paymentTenantBillOfMaterialGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentTenantBillOfMaterialGet: function (params, message, clientSession, callback) {
        var paymentTenantBillOfMaterialItem;
        try {
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        params.tenantId = tenantId;
                        return next();
                    });
                },
                // Get payment tenant bill of material record
                function (next) {
                    return PaymentTenantBillOfMaterial.findOne({
                        where: { id: params.id, tenantId: params.tenantId }
                    }).then(function (paymentTenantBillOfMaterialModel) {
                        if (!paymentTenantBillOfMaterialModel) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        paymentTenantBillOfMaterialItem = AutoMapper.mapBySchema('paymentTenantBillOfMaterialModel', INSTANCE_MAPPINGS, paymentTenantBillOfMaterialModel);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ paymentTenantBillOfMaterial: paymentTenantBillOfMaterialItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentTenantBillOfMaterialList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var paymentTenantBillOfMaterialItems = [];
        var paymentTenantBillOfMaterialTotal = 0;
        try {
            var orderBy = CrudHelper.orderBy(params, PaymentTenantBillOfMaterial);
            var searchBy = CrudHelper.searchBy(params, PaymentTenantBillOfMaterial);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        params.tenantId = tenantId;
                        return next();
                    });
                },
                // Get payment tenant bill of material record
                function (next) {
                    return PaymentTenantBillOfMaterial.findAndCountAll({
                        limit: params.limit,
                        offset: params.offset,
                        where: searchBy,
                        order: orderBy,
                        subQuery: false
                    }).then(function (paymentTenantBillOfMaterialModels) {
                        paymentTenantBillOfMaterialTotal = paymentTenantBillOfMaterialModels.count;
                        paymentTenantBillOfMaterialItems = AutoMapper.mapListBySchema('paymentTenantBillOfMaterialModel', INSTANCE_MAPPINGS, paymentTenantBillOfMaterialModels.rows);
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
                    items: paymentTenantBillOfMaterialItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: paymentTenantBillOfMaterialTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentTenantBillOfMaterialSetStatus: function(params, callback) {
        var self = this;
        try {
            return PaymentTenantBillOfMaterial.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
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
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentTenantBillOfMaterialPay: function(params, message, clientSession, callback) {
        try {
            if (!_.has(params, 'id') || !params.id) {
                return CrudHelper.callbackError(Errors.BillingApi.NoBillSelected, callback);
            }
            var paymentTenantBillOfMaterialId = params.id;
            var paymentTenantBillOfMaterialItem = { status: PaymentTenantBillOfMaterial.constants().STATUS_PAYED };
            async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        params.tenantId = tenantId;
                        return next();
                    });
                },
                // Check if current status is approved
                function (next) {
                    return PaymentTenantBillOfMaterial.findOne({
                        where: {
                            id: params.id, tenantId: params.tenantId
                        }
                    }).then(function (paymentTenantBillOfMaterialModel) {
                        if (!paymentTenantBillOfMaterialModel) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (paymentTenantBillOfMaterialModel.status !== PaymentTenantBillOfMaterial.constants().STATUS_APPROVED) {
                            return next(Errors.BillingApi.BillNotApproved);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return PaymentTenantBillOfMaterial.update(paymentTenantBillOfMaterialItem, { where: { id: paymentTenantBillOfMaterialId } }).then(function (count) {
                        if (count[0] === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
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

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentResourceBillOfMaterialUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var paymentResourceBillOfMaterialId = undefined;
            var mapBySchema = 'paymentResourceBillOfMaterialCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                paymentResourceBillOfMaterialId = params.id;
                mapBySchema = 'paymentResourceBillOfMaterialUpdateModel';
            }
            var paymentResourceBillOfMaterialItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // Create/update main entity
                function (next) {
                    if (create) {
                        paymentResourceBillOfMaterialItem.status = PaymentResourceBillOfMaterial.constants().STATUS_UNAPPROVED;
                        return PaymentResourceBillOfMaterial.create(paymentResourceBillOfMaterialItem).then(function (paymentResourceBillOfMaterialModel) {
                            paymentResourceBillOfMaterialId = paymentResourceBillOfMaterialModel.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        paymentResourceBillOfMaterialItem.status = PaymentResourceBillOfMaterial.constants().STATUS_CHANGED;
                        return PaymentResourceBillOfMaterial.update(paymentResourceBillOfMaterialItem, { where: { id: paymentResourceBillOfMaterialItem.id } }).then(function (count) {
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
                params.id = paymentResourceBillOfMaterialId;
                return self.paymentResourceBillOfMaterialGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentResourceBillOfMaterialGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var paymentResourceBillOfMaterialItem;
        try {
            var include = CrudHelper.include(PaymentResourceBillOfMaterial, [
                Workorder
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get payment resource bill of material record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Workorder, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    return PaymentResourceBillOfMaterial.findOne({
                        where: { id: params.id },
                        include: include,
                        subQuery: false
                    }).then(function (paymentResourceBillOfMaterialModel) {
                        if (!paymentResourceBillOfMaterialModel) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        paymentResourceBillOfMaterialItem = AutoMapper.mapBySchema('paymentResourceBillOfMaterialModel', INSTANCE_MAPPINGS, paymentResourceBillOfMaterialModel);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ paymentResourceBillOfMaterial: paymentResourceBillOfMaterialItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentResourceBillOfMaterialList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var paymentResourceBillOfMaterialItems = [];
        var paymentResourceBillOfMaterialTotal = 0;
        try {
            var orderBy = CrudHelper.orderBy(params, PaymentResourceBillOfMaterial);
            var searchBy = CrudHelper.searchBy(params, PaymentResourceBillOfMaterial);
            var include = CrudHelper.include(PaymentResourceBillOfMaterial, [
                Workorder
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get payment resource bill of material record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Workorder, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    return PaymentResourceBillOfMaterial.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (paymentResourceBillOfMaterialModels) {
                        paymentResourceBillOfMaterialTotal = paymentResourceBillOfMaterialModels.count;
                        paymentResourceBillOfMaterialItems = AutoMapper.mapListBySchema('paymentResourceBillOfMaterialModel', INSTANCE_MAPPINGS, paymentResourceBillOfMaterialModels.rows);
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
                    items: paymentResourceBillOfMaterialItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: paymentResourceBillOfMaterialTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentResourceBillOfMaterialSetStatus: function(params, callback) {
        var self = this;
        try {
            return PaymentResourceBillOfMaterial.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
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
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentResourceBillOfMaterialPay: function (params, message, clientSession, callback) {
        try {
            if (!_.has(params, 'id') || !params.id) {
                return CrudHelper.callbackError(Errors.BillingApi.NoBillSelected, callback);
            }
            var paymentResourceBillOfMaterialId = params.id;
            var paymentResourceBillOfMaterialItem = { status: PaymentResourceBillOfMaterial.constants().STATUS_PAYED };
            var include = CrudHelper.include(PaymentResourceBillOfMaterial, [
                Workorder
            ]);
            async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        params.tenantId = tenantId;
                        return next();
                    });
                },
                // Check if current status is approved
                function(next) {
                    include = CrudHelper.includeNestedWhere(include, Workorder, WorkorderHasTenant, { tenantId: params.tenantId }, true);
                    return PaymentResourceBillOfMaterial.findOne({
                        where: { id: params.id },
                        include: include,
                        subQuery: false
                    }).then(function (paymentResourceBillOfMaterialModel) {
                        if (!paymentResourceBillOfMaterialModel) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        if (paymentResourceBillOfMaterialModel.status !== PaymentResourceBillOfMaterial.constants().STATUS_APPROVED) {
                            return next(Errors.BillingApi.BillNotApproved);
                        }
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                },
                // Update status
                function (next) {
                    return PaymentResourceBillOfMaterial.update(paymentResourceBillOfMaterialItem, { where: { id: paymentResourceBillOfMaterialId } }).then(function (count) {
                        if (count[0] === 0) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        return next();
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

    /**
     * Gets payment action
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentActionGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var paymentActionItem;
        try {
            var include = CrudHelper.include(PaymentAction, [
                Workorder
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get payment action record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Workorder, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    return PaymentAction.findOne({
                        where: { id: params.id },
                        include: include,
                        subQuery: false
                    }).then(function (paymentActionModel) {
                        if (!paymentActionModel) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        var paymentActionItem = AutoMapper.mapBySchema('paymentActionModel', INSTANCE_MAPPINGS, paymentActionModel);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ paymentAction: paymentActionItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Lists all payment actions
     * TODO implement user checks for list within one tenant for tenantBill or for one user if resourceBill
     * TODO Should paymentTenantBillOfMaterialId or paymentResourceBillOfMaterialId be obligatory?
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentActionList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var paymentActionItems = [];
        var paymentActionTotal = 0;
        try {
            var orderBy = CrudHelper.orderBy(params, PaymentAction);
            var searchBy = CrudHelper.searchBy(params, PaymentAction);
            var include = CrudHelper.include(PaymentAction, [
                Workorder
            ]);
            return async.series([
                // Get tenant id from client session object
                function (next) {
                    TenantService.getSessionTenantId(message, clientSession, function (err, tenantId) {
                        if (err) {
                            return next(err);
                        }
                        sessionTenantId = tenantId;
                        return next();
                    });
                },
                // Get quetion rating record
                function (next) {
                    include = CrudHelper.includeNestedWhere(include, Workorder, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    return PaymentAction.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy,
                        include: include,
                        subQuery: false
                    }).then(function (paymentActionModels) {
                        paymentActionTotal = paymentActionModels.count;
                        paymentActionItems = AutoMapper.mapListBySchema('paymentActionModel', INSTANCE_MAPPINGS, paymentActionModels.rows);
                        return next(err);
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({
                    items: paymentActionItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: paymentActionTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * Updates a payment action. Currently not for API 
     * @private
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentActionUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var paymentActionId = undefined;
            var mapBySchema = 'paymentActionCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                paymentActionId = params.id;
                mapBySchema = 'paymentActionUpdateModel';
            }
            var paymentActionItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // Create/update main entity
                function (next) {
                    if (create) {
                        if (!_.has(paymentActionItem, 'createDate')) {
                            paymentActionItem.createDate = _.now();
                        }
                        paymentActionItem.status = PaymentAction.constants().STATUS_UNAPPROVED;
                        return PaymentAction.create(paymentActionItem).then(function (paymentActionModel) {
                            paymentActionId = paymentActionModel.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        paymentActionItem.status = PaymentAction.constants().STATUS_CHANGED;
                        return PaymentAction.update(paymentActionItem, { where: { id: paymentActionItem.id } }).then(function (count) {
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
                params.id = paymentActionId;
                return self.paymentActionGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    /**
     * @param {{}} params
     * @param {Function} callback
     * @returns {*}
     */
    paymentActionSetStatus: function(params, callback) {
        var self = this;
        try {
            return PaymentAction.update({ status: params.status }, { where: { id: params.id } }).then(function (count) {
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

};