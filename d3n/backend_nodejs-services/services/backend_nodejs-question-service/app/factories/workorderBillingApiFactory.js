var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var AutoMapper = AutoMapperInstance.AutoMapper;
var CrudHelper = AutoMapperInstance.CrudHelper;
var Database = require('nodejs-database').getInstance(Config);
var WorkorderBillingModel = Database.RdbmsService.Models.Workorder.WorkorderBillingModel;
var Workorder = Database.RdbmsService.Models.Workorder.Workorder;
var WorkorderHasTenant = Database.RdbmsService.Models.Workorder.WorkorderHasTenant;
var TenantService = require('../services/tenantService.js');

var INSTANCE_MAPPINGS = {
    'workorderBillingModel': [
        {
            destination: '$root',
            model: WorkorderBillingModel
        }
    ],
    'workorderBillingCreateModel': [
        {
            destination: '$root',
            model: WorkorderBillingModel
        }
    ]
};
INSTANCE_MAPPINGS['workorderBillingUpdateModel'] = INSTANCE_MAPPINGS['workorderBillingCreateModel'];

module.exports = {
    /**
     *
     * @param {{}} params
     * @param {{}} callback
     * @returns {*}
     */
    workorderBillingUpdate: function (params, message, clientSession, callback) {
        var self = this;
        try {
            var create = true;
            var workorderBillingModelId = undefined;
            var mapBySchema = 'workorderBillingCreateModel';
            if (_.has(params, 'id') && params.id) {
                create = false;
                workorderBillingModelId = params.id;
                mapBySchema = 'workorderBillingUpdateModel';
            }
            var workorderBillingModelItem = AutoMapper.mapDefinedBySchema(mapBySchema, INSTANCE_MAPPINGS, params, true);
            async.series([
                // Create/update main entity
                function (next) {
                    if (create) {
                        return WorkorderBillingModel.create(workorderBillingModelItem).then(function (workorderBillingModel) {
                            workorderBillingModelId = workorderBillingModel.get({ plain: true }).id;
                            return next();
                        }).catch(function (err) {
                            return CrudHelper.callbackError(err, next);
                        });
                    } else {
                        return WorkorderBillingModel.update(workorderBillingModelItem, { where: { id: workorderBillingModelItem.id } }).then(function (count) {
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
                params.id = workorderBillingModelId;
                return self.workorderBillingGet(params, message, clientSession, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    workorderBillingDelete: function (params, callback) {
        try {
            return WorkorderBillingModel.destroy({ where: { id: params.id } }).then(function (count) {
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

    workorderBillingGet: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var workorderBillingItem;
        try {
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
                    var include = CrudHelper.include(WorkorderBilling, [
                        Workorder
                    ]);
                    include = CrudHelper.includeNestedWhere(include, Workorder, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    return WorkorderBillingModel.findOne({
                        where: { id: params.id }
                    }).then(function (workorderBillingModel) {
                        if (!workorderBillingModel) {
                            return next(Errors.DatabaseApi.NoRecordFound);
                        }
                        workorderBillingItem = AutoMapper.mapDefinedBySchema('workorderBillingModel', INSTANCE_MAPPINGS, workorderBillingModel);
                        return next();
                    }).catch(function (err) {
                        return CrudHelper.callbackError(err, next);
                    });
                }
            ], function (err) {
                if (err) {
                    return CrudHelper.callbackError(err, callback);
                }
                return CrudHelper.callbackSuccess({ workorderBilling: workorderBillingItem }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    },

    workorderBillingList: function (params, message, clientSession, callback) {
        var sessionTenantId;
        var workorderBillingItems = [];
        var workorderBillingTotal = 0;
        try {
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
                    var orderBy = CrudHelper.orderBy(params, WorkorderBilling);
                    var searchBy = CrudHelper.searchBy(params, WorkorderBilling);
                    var include = CrudHelper.include(WorkorderBilling, [
                        Workorder
                    ]);
                    include = CrudHelper.includeNestedWhere(include, Workorder, WorkorderHasTenant, { tenantId: sessionTenantId }, true);
                    return WorkorderBillingModel.findAndCountAll({
                        limit: params.limit === 0 ? null : params.limit,
                        offset: params.limit === 0 ? null : params.offset,
                        where: searchBy,
                        order: orderBy
                    }).then(function (workorderBillingModels) {
                        workorderBillingTotal = questionRatings.count;
                        workorderBillingItems = AutoMapper.mapListDefinedBySchema('workorderBillingModel', INSTANCE_MAPPINGS, workorderBillingModels.rows);
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
                    items: workorderBillingItems,
                    limit: params.limit,
                    offset: params.offset,
                    total: workorderBillingTotal
                }, callback);
            });
        } catch (ex) {
            return CrudHelper.callbackError(ex, callback);
        }
    }

};