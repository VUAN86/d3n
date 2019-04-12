var _ = require('lodash');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeGlobalClientSession = KeyvalueService.Models.AerospikeGlobalClientSession;

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
        
        /*
        try {
            AerospikeGlobalClientSession.getTenantId(clientSession.getAuthentication().getTokenPayload().userId, message.getClientId(), function (err, sessionTenantId) {
                if (err) {
                    logger.error('Error getting tenantId from global session', err);
                    return cb(err);
                }
                if (!sessionTenantId) {
                    logger.error('Wrong tenantId returned from global session', sessionTenantId);
                    return cb(Errors.DatabaseApi.NoRecordFound);
                }
                return cb(false, parseInt(sessionTenantId));
            });
        } catch (e) {
            logger.error('Error on getting tenantId from global session', e);
            return setImmediate(cb, e);
        }
        */
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
    }
};