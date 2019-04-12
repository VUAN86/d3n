var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var ClientInfo = require('nodejs-utils').ClientInfo;
var AdvertisementApiFactory = require('./../factories/advertisementApiFactory.js');
var GameApiFactory = require('./../factories/gameApiFactory.js');

module.exports = {

    advertisementList: function (params, message, clientSession, callback) {
        try {
            // Execute advertisement list with callback
            return AdvertisementApiFactory.advertisementList(params, message, clientSession, function (err, data) {
                if (err) {
                    return callback(err);
                }
                return callback(null, data);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    advertisementDeactivate: function (params, message, clientSession, callback) {
        try {
            // Process advertisement deactivate action with callback
            return AdvertisementApiFactory.advertisementUnpublish(params, message, clientSession, callback);
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    gameList: function (params, message, clientSession, callback) {
        try {
            // Execute game list with callback
            return GameApiFactory.gameList({
                params: params,
                clientInfo: new ClientInfo(clientSession.getClientInfo())
            }, function (err, data) {
                if (err) {
                    return callback(err);
                }
                return callback(null, data);
            });
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

    gameDeactivate: function (params, message, clientSession, callback) {
        try {
            // Process game deactivate action with callback
            return GameApiFactory.gameUnpublish({
                params: params,
                clientInfo: new ClientInfo(clientSession.getClientInfo()),
                schedulerService: clientSession.getConnectionService()._schedulerService
            }, callback);
        } catch (ex) {
            return setImmediate(callback, ex);
        }
    },

}
