var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var AdvertisementApiFactory = require('./../factories/advertisementApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var Advertisement = Database.RdbmsService.Models.AdvertisementManager.Advertisement;
var AdvertisementProvider = Database.RdbmsService.Models.AdvertisementManager.AdvertisementProvider;

module.exports = {
    /**
     * WS advertisementManager/advertisementCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    advertisementCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    advertisementUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementPublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementUnpublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementArchive
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementArchive: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = Advertisement.constants().STATUS_ARCHIVED;
            AdvertisementApiFactory.advertisementSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    
    /**
     * WS advertisementManager/advertisementDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementProviderCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    advertisementProviderCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementProviderUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementProviderUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    advertisementProviderUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementProviderUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementProviderActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementProviderActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = AdvertisementProvider.constants().STATUS_ACTIVE;
            AdvertisementApiFactory.advertisementProviderSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementProviderDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementProviderDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = AdvertisementProvider.constants().STATUS_INACTIVE;
            AdvertisementApiFactory.advertisementProviderSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementProviderArchive
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementProviderArchive: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = AdvertisementProvider.constants().STATUS_ARCHIVED;
            AdvertisementApiFactory.advertisementProviderSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API advertisementManager/advertisementProviderGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementProviderGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementProviderGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API advertisementManager/advertisementProviderList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementProviderList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementProviderList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS advertisementManager/advertisementProviderDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    advertisementProviderDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementProviderDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API advertisementManager/advertisementProviderAddRemoveTenant
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    // Code not commented out per #5458 - based on conversation with Maxim a connection
    // between advertisementProvider and tenant may not be needed.
    /*
    advertisementProviderAddRemoveTenant: function(message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            AdvertisementApiFactory.advertisementCampaignAddRemoveTenant(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    }
    */
}
