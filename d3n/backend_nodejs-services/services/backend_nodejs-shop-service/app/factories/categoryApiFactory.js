var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ShopService = require('./../services/shopService.js');
var Mapper = require('./../helpers/mapper.js');

module.exports = {

    categoryList: function (params, clientSession, callback) {
        try {
            var shopService = new ShopService("categories");
            var queryString = _getCategoryListQueryString(params, clientSession);
            shopService.setQueryString(queryString);
            shopService.call(function (err, body) {
                var errObj = { "value" : err };
                var categories = _getCategories(errObj, body);
                if (!Errors.isInstance(errObj.value)) errObj.value = Errors.ShopCall;
                setImmediate(callback, errObj.value, categories);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, Errors.FatalError);
        }
    }  
};

function _getCategories(errObj, body) {
    if (!errObj.value) {
        try {
            var listMapper = new Mapper("List");
            var categoryMapper = new Mapper("Category");

            var categories = listMapper.get(body);
            var items = [];
            _.forEach(categories.items, function(value, key) {
                var category = categoryMapper.get(value);
                items.push(category);
            });
            categories.items = items;

            return categories;
        } catch (ex) {
            logger.error("Exception mapping shop/categoryListResponse", ex);
            errObj.value = Errors.ResponseMapping;
        }
    }
}

function _getCategoryListQueryString(params, clientSession) {
    var tenantId = clientSession.getTenantId();
    if (!tenantId) {
        var err = "missing tenant id";
        logger.error("Error building shop request for category list", err);
        throw err;
    }
    return "/" + tenantId;
}