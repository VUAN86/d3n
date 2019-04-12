var _ = require('lodash');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var Tenant = Database.RdbmsService.Models.Tenant.Tenant;
var TenantContract = Database.RdbmsService.Models.Tenant.TenantContract;
var ConfigurationSetting = Database.RdbmsService.Models.Configuration.Setting;
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;

module.exports = {
    getSessionTenantId: function (message, clientSession, cb) {
        try {
            if (_.has(message, 'tenantId') && message.tenantId) {
                return setImmediate(cb, false, message.tenantId);
            }
            
            var tenantId = parseInt(clientSession.getTenantId());
            if (isNaN(tenantId)) {
                logger.error('getSessionTenantId() tenantId not a number:', clientSession.getTenantId());
                return setImmediate(cb, new Error('NOT_A_NUMBER'));
            }
            
            return setImmediate(cb, false, tenantId);
        } catch (e) {
            logger.error('Error on getting tenantId from global session', e);
            return setImmediate(cb, e);
        }
    },

    hasTenantAutoCommunity: function (tenantId, callback) {
        return Tenant.findById(tenantId).then(function (tenant) {
            var tenantItem = tenant.get({ plain: true });
            return callback(null, tenantItem.autoCommunity === 1);
        }).catch(function (err) {
            logger.error("tenantService:hasTenantOpenAccess could not retrieve tenant", tenantId, 'error', err);
            return CrudHelper.callbackError(err, callback);
        });
    },

    /**
     * Retrieve a tenant with a given ID
     * 
     * @params {Integer} id
     * @params {callback} callback
     */
    retrieveTenantById: function (id, callback) {
        return Tenant.findById(id)
            .then(function (tenant) {
                return setImmediate(callback, null, tenant.get({plain: true}));
            }).catch(function (err) {
                logger.error("tenantService:retrieveTenantById could not retrieve tenant", id, 'error', err);
                return CrudHelper.callbackError(err, callback);
            });
    },

    /**
     *  Retrieve a given contract for a tenant§
     * 
     * @parmas {Integer} id
     * @params {Integer} tenantId
     */
    retrieveContractForTenant: function (id, tenantId, callback) {
        return TenantContract.findOne({
            where: {
                id: id,
                tenantId: tenantId
            }
            }).then(function (contract) {
                return setImmediate(callback, null, contract.get({plain: true}));
            }).catch(function (err) {
                logger.error("tenantService:retrieveContractForTenant could not retrieve contract", id, tenantId, 'error', err);
                return CrudHelper.callbackError(err, callback);
            });
    },

    cleanTags: function (tags) {
        var tagsToRemove = [];
        _.forEach(tags, function (tag, key) {
            if (_.isNull(tag.tag)) {
                tagsToRemove.push(key);
            }
        });
        _.remove(tags, function (value, key) {
            return _.includes(tagsToRemove, key);
        });
    },

    retrieveF4mLegalEntities: function (callback) {
        try {
            var initialData = Config.f4mLegalEntities;

            return ConfigurationSetting.findById(ConfigurationSetting.constants().ID_F4M_LEGAL)
                .then(function (config) {
                    var result = initialData;
                    if (!_.isEmpty(config)) {
                        result = _.extend(initialData, config.data);
                    }

                    return setImmediate(callback, false, result);
                }).catch(function (err) {
                    logger.error('tenantService.retrieveF4mLegalEntities could not retrieve configuration', err);
                    return CrudHelper.callbackError(err, callback);
                });
        } catch (ex) {
            logger.error('tenantService.retrieveF4mLegalEntities exception', ex);
            return setImmediate(callback, ex);
        }
    }
};