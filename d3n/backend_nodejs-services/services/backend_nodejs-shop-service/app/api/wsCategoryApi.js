var _ = require('lodash');
var CategoryApiFactory = require('./../factories/categoryApiFactory.js');
var Config = require('./../config/config.js');
var CrudHelper = require('nodejs-automapper').getInstance(Config).CrudHelper;

module.exports = {

    /**
     * WS API shop/categoryList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    categoryList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            CategoryApiFactory.categoryList(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  
};