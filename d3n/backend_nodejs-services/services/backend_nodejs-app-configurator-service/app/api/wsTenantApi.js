var _ = require('lodash');
var Errors = require('./../config/errors.js');
var Config = require('./../config/config.js');
var AutoMapperInstance = require('nodejs-automapper').getInstance(Config);
var CrudHelper = AutoMapperInstance.CrudHelper;
var TenantApiFactory = require('./../factories/tenantApiFactory.js');

module.exports = {

    /**
     * WS API tenant/tenantUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tenantUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.tenantUpdate(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API tenant/tenantGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tenantGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.tenantGet(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    // Ref #5810 - tenantList is not available for everyone, users have access only to the current tenant information
    /**
     * WS API tenant/tenantList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    /*
    tenantList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.tenantList(params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },
    */

    /**
     * WS API tenant/contractList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    contractList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.contractList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API tenant/contractChange
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    contractChange: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.contractChange(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

    /**
     * WS API tenant/invoiceList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    invoiceList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.invoiceList(params, message, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },

}