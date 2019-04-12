var _ = require('lodash');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var WorkorderBillingApiFactory = require('./../factories/workorderBillingApiFactory.js');

module.exports = {

    /**
     * WS API workorder/workorderBillingUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    workorderBillingUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderBillingApiFactory.workorderBillingUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderBillingDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    workorderBillingDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderBillingApiFactory.workorderBillingDelete(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderBillingGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    workorderBillingGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderBillingApiFactory.workorderBillingGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API workorder/workorderBillingList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    workorderBillingList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            WorkorderBillingApiFactory.workorderBillingList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    }

};