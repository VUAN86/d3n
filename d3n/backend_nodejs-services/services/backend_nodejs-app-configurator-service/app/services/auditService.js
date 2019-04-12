var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var Tenant = Database.RdbmsService.Models.Tenant.Tenant;
var TenantAuditLog = Database.RdbmsService.Models.Tenant.TenantAuditLog;
var TenantContract = Database.RdbmsService.Models.Tenant.TenantContract;
var TenantContractAuditLog = Database.RdbmsService.Models.Tenant.TenantContractAuditLog;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var logger = require('nodejs-logger')();

module.exports = {
    /**
     * Add an entry to the tenant trail system
     * @param {String} userId
     * @param {Object} oldTenant
     * @param {Object} newTenant
     * @param {callback} next
     */
    createTenantAuditLog: function(userId, oldTenant, newTenant, next) {
        var changes = {};
        
        try {
            // Search for changes between the two tenant objects except changing fields
            var checkAttributes = _.without(_.keys(Tenant.attributes), 'id', 'createDate', 'updateDate');

            _.forEach(checkAttributes, function(tenantAttribute) {
                if (!_.isEqual(oldTenant[tenantAttribute], newTenant[tenantAttribute])) {
                    changes[tenantAttribute] = {
                        old: oldTenant[tenantAttribute],
                        new: newTenant[tenantAttribute]
                    }
                }
            });

            TenantAuditLog.build({
                tenantId: newTenant.id,
                creatorProfileId: userId,
                type: TenantAuditLog.constants().TYPE_CHANGE,
                createDate: _.now(),
                items: changes
            }).save()
              .then(function (item) {
                  logger.debug("auditService:createTenantAuditLog entity created");
                  return setImmediate(next);
              })
              .catch(function (err) {
                  logger.error("auditService:createTenantAuditLog save exception", err);
                  return CrudHelper.callbackError(err, next);
              });
        } catch(ex) {
            logger.error("auditService:createTenantAuditLog general exception", ex);
            return setImmediate(next, ex);
        }
    },

    /**
     * Add an entry to the contract trail system
     * @param {String} userId
     * @param {string} contract The status change requested from the original contract
     * @param {callback} next
     */
    createContractAuditLog: function (userId, oldContract, newContract, next) {
        var changes = {}, type = TenantContractAuditLog.constants().TYPE_NONE;
        var statusActionMap = {
            'none': TenantContractAuditLog.constants().TYPE_NONE,
            'hold': TenantContractAuditLog.constants().TYPE_HOLD_REQUEST,
            'close': TenantContractAuditLog.constants().TYPE_CLOSE_REQUEST,
            // 'renew': TenantContractAuditLog.constants().TYPE_RENEW_REQUEST,
        };

        try {
            // Search for changes between the two tenant objects except changing fields
            var checkAttributes = _.without(_.keys(TenantContract.attributes), 'id', 'tenantId', 'updateDate', 'createDate');

            _.forEach(checkAttributes, function(contractAttribute) {
                if (!_.isEqual(oldContract[contractAttribute], newContract[contractAttribute])) {
                    changes[contractAttribute] = {
                        old: oldContract[contractAttribute],
                        new: newContract[contractAttribute]
                    }
                }
            });

            if (_.has(changes, 'action')) {
                type = statusActionMap[newContract.action];
            }

            TenantContractAuditLog.build({
                contractId: newContract.id,
                creatorProfileId: userId,
                type: type,
                items: changes,
                createDate: _.now()
            }).save()
              .then(function (item) {
                  logger.debug("auditService:createContractAuditLog entity created");
                  return setImmediate(next);
              })
              .catch(function (err) {
                  logger.error("auditService:createContractAuditLog save exception", err);
                  return CrudHelper.callbackError(err, next);
              });
        } catch (ex) {
            logger.error("auditService:createContractAuditLog general exception", ex);
            return setImmediate(next, ex);
        }

    }

}