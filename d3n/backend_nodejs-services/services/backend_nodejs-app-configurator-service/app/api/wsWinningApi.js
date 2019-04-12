var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var WinningApiFactory = require('./../factories/winningApiFactory.js');
var Database = require('nodejs-database').getInstance(Config);
var WinningComponent = Database.RdbmsService.Models.WinningManager.WinningComponent;

module.exports = {

    /**
     * WS winningManager/winningComponentCreate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    winningComponentCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WinningApiFactory.winningComponentUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS winningManager/winningComponentUpdate
     * @param {ProtocolMessage} message Incoming message
     * @param {ClientSession} clientSession Default Service client session object
     */
    winningComponentUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WinningApiFactory.winningComponentUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS winningManager/winningComponentActivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    winningComponentActivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WinningApiFactory.winningComponentPublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS winningManager/winningComponentDeactivate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    winningComponentDeactivate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WinningApiFactory.winningComponentUnpublish(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS winningManager/winningComponentArchive
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    winningComponentArchive: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            params.status = WinningComponent.constants().STATUS_ARCHIVED;
            WinningApiFactory.winningComponentSetStatus(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS winningManager/winningComponentGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    winningComponentGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WinningApiFactory.winningComponentGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS winningManager/winningComponentList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    winningComponentList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WinningApiFactory.winningComponentList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS winningManager/winningComponentDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    winningComponentDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WinningApiFactory.winningComponentDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

}
