var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Tenant = RdbmsService.Models.Tenant.Tenant;
var ProfileHasRole = Database.RdbmsService.Models.ProfileManager.ProfileHasRole;

module.exports = {

    getName: function (clientSession, callback) {
        try {
            Tenant.findOne({where:
                {id: _getTenantId(clientSession)},
                attributes: [Tenant.tableAttributes.name.field]
            }).then(function (dbItem) {
                var name = dbItem.get({plain: true}).name;
                setImmediate(callback, null, name);
            }).catch(function(err) {
                logger.error("Error retrieving tenant name", err);
                setImmediate(callback, Errors.FatalError);
            });
        } catch (ex) {
            logger.error("Error retrieving tenant name", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    getAdminList: function (clientSession, callback) {
        try {
            ProfileHasRole.findAll({ where: {
                'role': ProfileHasRole.constants().ROLE_ADMIN,
                'tenantId': _getTenantId(clientSession)
            }}).then(function (admins) {
                var adminList = _.map(admins, 'profileId');
                setImmediate(callback, null, adminList);
            }).catch(function(err) {
                logger.error("Error retrieving tenant admin list", err);
                setImmediate(callback, Errors.FatalError);
            });
        } catch (ex) {
            logger.error("Error retrieving tenant admin list", ex);
            setImmediate(callback, Errors.FatalError);
        }
    }  
};

function _getTenantId(clientSession) {
    var tenantId = clientSession.getTenantId();
    if (!tenantId) {
        var err = "missing tenant id";
        logger.error("Shop: error finding tenant", err);
        throw err;
    }
    return tenantId;
}
