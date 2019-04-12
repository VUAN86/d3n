var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var PromocodeApiFactory = require('./../factories/promocodeApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var PromocodeCampaign = Database.RdbmsService.Models.PromocodeManager.PromocodeCampaign;

module.exports = {
    /**
     * WS promocodeManager/promocodeCampaignCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    promocodeCampaignCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    promocodeCampaignUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignActivate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    promocodeCampaignActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignPublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignDeactivate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    promocodeCampaignDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignUnpublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignGenerateFromFile
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    promocodeCampaignGenerateFromFile: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignGenerateFromFile(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignArchive
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    promocodeCampaignArchive: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = PromocodeCampaign.constants().STATUS_ARCHIVED;
            PromocodeApiFactory.promocodeCampaignSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    promocodeCampaignDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    promocodeCampaignGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCampaignList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    promocodeCampaignList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCampaignList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    promocodeCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeCreate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    promocodeList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS promocodeManager/promocodeInstanceList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    promocodeInstanceList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            PromocodeApiFactory.promocodeInstanceList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    }
};


