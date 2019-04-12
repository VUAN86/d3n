var _ = require('lodash');
var UserProfileApiFactory = require('./../factories/userProfileApiFactory.js');
var Config = require('./../config/config.js');
var CrudHelper = require('nodejs-automapper').getInstance(Config).CrudHelper;

module.exports = {

    /**
     * WS API shop/articleGetSelect
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    articleGetSelect: function (message, clientSession) {
        try {
            UserProfileApiFactory.getSelectedArticleId(clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, { articleId: data.id }, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/articleSetSelect
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    articleSetSelect: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            UserProfileApiFactory.setSelectedArticleId(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/shippingGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    shippingGet: function (message, clientSession) {
        try {
            UserProfileApiFactory.getShippingAddress(clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/shippingSet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    shippingSet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            UserProfileApiFactory.setShippingAddress(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  
};
