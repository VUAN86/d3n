var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var TombolaApiFactory = require('./../factories/tombolaApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var Tombola = Database.RdbmsService.Models.TombolaManager.Tombola;
var logger = require('nodejs-logger')();

module.exports = {

    /**
     * WS tombolaManager/tombolaCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    tombolaCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    tombolaUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaPublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaUnpublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaArchive
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaArchive: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = Tombola.constants().STATUS_ARCHIVED;
            TombolaApiFactory.tombolaSetStatus(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaDelete(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaGetBoost
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaGetBoost: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaGetBoost(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error("getTombolaBoost error: ", ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaBoost
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaBoost: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaBoost(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            logger.error("tombolaBoost error: ", ex);
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS tombolaManager/tombolaList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tombolaList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TombolaApiFactory.tombolaList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

}
