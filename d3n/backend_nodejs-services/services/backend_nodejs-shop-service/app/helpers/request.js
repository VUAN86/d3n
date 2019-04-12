var _ = require('lodash');
var logger = require('nodejs-logger')();
var Errors = require('./../config/errors.js');
var Mapper = require('./mapper.js');

ASC = "asc";
DESC = "desc";

var Request = {
    ARTICLE: {
        mapperName: "Article",
        defaultParam: "title"
    },
    ORDER: {
        mapperName: "Order",
        defaultParam: "finalizeDate"
    },

    getRequestParameters: function(listType, params, clientSession) {
        var tenantId = clientSession.getTenantId();
        if (!tenantId) {
            var err = "missing tenant id";
            logger.error("Error building shop request parameters", err);
            throw err;
        }

        var limit = _.get(params, "limit", 20);
        if (limit !== parseInt(limit, 10) && limit <= 0) {
            var err = "invalid limit";
            logger.error("Error building shop request parameters", err);
            throw err;
        }
    
        var offset = _.get(params, "offset", 0);
        if (offset !== parseInt(offset, 10) && offset < 0) {
            var err = "invalid offset";
            logger.error("Error building shop request parameters", err);
            throw err;
        }
        var pageNumber = Math.floor(offset / limit); // shop only allows page number
    
        var orderBy = _.has(params, "orderBy.field") && params.orderBy.field ?
            params.orderBy.field : listType.defaultParam;
        orderBy = Request.getMappedField(orderBy, listType.mapperName);

        var direction = _.has(params, "orderBy.direction") &&
            (params.orderBy.direction.toLowerCase() == ASC || params.orderBy.direction.toLowerCase() == DESC)
            ? params.orderBy.direction : ASC;

        return {
            tenantId: tenantId,
            itemsPerPage: limit,
            pageNumber: pageNumber,
            orderBy: orderBy,
            direction: direction
        };
    },

    getMappedField(param, mapperName) {
        var mapper = new Mapper(mapperName);
        try {
            var fieldName = mapper.getMapping(param, true);
            return fieldName;
        } catch (ex) {
            throw Errors.RequestMapping;
        }
    }
}

module.exports = Request;