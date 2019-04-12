var _ = require('lodash');
var TenantApiFactory = require('./../factories/tenantApiFactory.js');
var Config = require('./../config/config.js');
var CrudHelper = require('nodejs-automapper').getInstance(Config).CrudHelper;

module.exports = {
    /**
     * WS API shop/tenantAdd
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tenantAdd: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.tenant(TenantApiFactory.ADD, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/tenantUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tenantUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.tenant(TenantApiFactory.UPDATE, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/tenantDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    tenantDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            TenantApiFactory.tenant(TenantApiFactory.DELETE, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  
};
