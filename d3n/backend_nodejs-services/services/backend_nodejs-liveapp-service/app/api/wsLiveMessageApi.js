var _ = require('lodash');
var LiveMessageApiFactory = require('./../factories/liveMessageApiFactory.js');
var Config = require('./../config/config.js');
var CrudHelper = require('nodejs-automapper').getInstance(Config).CrudHelper;

module.exports = {
    /**
     * WS API messageCenter/liveMessageCreate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    liveMessageCreate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            LiveMessageApiFactory.update(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API messageCenter/liveMessageUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    liveMessageUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            LiveMessageApiFactory.update(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API messageCenter/liveMessageGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    liveMessageGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            LiveMessageApiFactory.get(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API messageCenter/liveMessageList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    liveMessageList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            LiveMessageApiFactory.list(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API messageCenter/liveMessageSend
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    liveMessageSend: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            LiveMessageApiFactory.send(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  
};
