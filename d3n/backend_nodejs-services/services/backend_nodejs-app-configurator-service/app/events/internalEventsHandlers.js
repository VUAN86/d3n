var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var Database = require('nodejs-database').getInstance(Config);
var Tombola = Database.RdbmsService.Models.TombolaManager.Tombola;
var TombolaApiFactory = require('./../factories/tombolaApiFactory.js');
var TenantService = require('./../services/tenantService.js');
var logger = require('nodejs-logger')();

module.exports = {
    schedulerEvent: function (eventName) {
        logger.debug('schedulerEvent :', eventName);
        try {
            if (eventName.startsWith('tombola/openCheckout/')) {
                var tombolaId = parseInt(eventName.substring('tombola/openCheckout/'.length));
                return TombolaApiFactory.tombolaSetOpenCheckout({
                    id: tombolaId
                }, null, function (err) { });
            }
            if (eventName.startsWith('tombola/draw/')) {
                var tombolaId = parseInt(eventName.substring('tombola/draw/'.length));
                return TombolaApiFactory.tombolaSetStatus({
                    id: tombolaId,
                    status: Tombola.constants().STATUS_ARCHIVED
                }, null, function (err) { });
            }
        } catch (ex) {
            logger.error('InternalEventsHandler.schedulerEvent', ex);
        }
    }
};
