var _ = require('lodash');
var ArticleApiFactory = require('./../factories/articleApiFactory.js');
var Config = require('./../config/config.js');
var CrudHelper = require('nodejs-automapper').getInstance(Config).CrudHelper;

module.exports = {

    /**
     * WS API shop/articleList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    articleList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ArticleApiFactory.articleList(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/articleGet
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    articleGet: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ArticleApiFactory.article(ArticleApiFactory.GET, clientSession, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/articleAdd
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    articleAdd: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ArticleApiFactory.article(ArticleApiFactory.ADD, clientSession, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/articleUpdate
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    articleUpdate: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ArticleApiFactory.article(ArticleApiFactory.UPDATE, clientSession, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/articleDelete
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    articleDelete: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            ArticleApiFactory.article(ArticleApiFactory.DELETE, clientSession, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  
};
