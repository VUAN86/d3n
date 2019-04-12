var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ShopService = require('./../services/shopService.js');
var Mapper = require('./../helpers/mapper.js');

var TenantApiFactory = {
    UPDATE: 1,
    ADD: 2,
    DELETE: 3,

    tenant: function (action, params, callback) {
        try {
            var serviceKey = _getServiceKey(action);
            var shopService = new ShopService(serviceKey);
            if (action != TenantApiFactory.ADD) {
                var queryString = _getTenantQueryString(params);
                shopService.setQueryString(queryString);
            }
            var tenant = _getRequestTenant(params);
            shopService.setBody(tenant);
            shopService.call(function (err, body) {
                var errObj = { "value" : err };
                var tenant = "";
                if (action != TenantApiFactory.DELETE) {
                    if (action == TenantApiFactory.UPDATE && !body) {
                        errObj.value = Errors.NotFound;
                    } else  {
                        tenant = _getTenant(errObj, body);
                    }
                } else {
                    if (!body) {
                        errObj.value = Errors.NotFound;
                    } else if (body !== Config.shop.messages.deleteOk) {
                        errObj.value = Errors.ShopCall;
                    }
                }
                if (!Errors.isInstance(errObj.value)) errObj.value = Errors.ShopCall;
                setImmediate(callback, errObj.value, tenant);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, ex == Errors.RequestMapping ? Errors.RequestMapping : Errors.FatalError);
        }
    },
};

function _getTenant(errObj, body) {
    if (!errObj.value) {
        if (body === Config.shop.messages.notFound) {
            errObj.value = Errors.NotFound;
        } else if (body === Config.shop.messages.alreadyExists) {
            errObj.value = Errors.AlreadyExists;
        } else {
            try {
                var tenantMapper = new Mapper("Tenant");
                var tenant = tenantMapper.get(body);
                return tenant;
            } catch (ex) {
                logger.error("Exception mapping shop/tenantGetResponse", ex);
                errObj.value = Errors.ResponseMapping;
            }
        }
    }
}

function _getRequestTenant(params) {
    var tenant = {};
    _.forOwn(params, function(value, key) {
        var fieldName = _getTenantFieldName(key);
        tenant[fieldName] = value;
    });
    return JSON.stringify(tenant);
}

function _getTenantQueryString(params) {
    if (!(_.has(params, "id") && params.id)) {
        var err = "missing tenant id";
        logger.error("Error building shop request for retrieving tenant", err);
        throw err;
    }
    return "/" + params.id;
}

function _getTenantFieldName(param) {
    var tenantMapper = new Mapper("Tenant");
    try {
        var fieldName = tenantMapper.getMapping(param, true);
        return fieldName;
    } catch (ex) {
        logger.error("Shop: error mapping tenant name field", ex);
        throw Errors.RequestMapping;
    }
}

function _getServiceKey(key) {
    switch(key) {
        case TenantApiFactory.ADD:
            return "tenantAdd";
        case TenantApiFactory.UPDATE:
            return "tenantUpdate";
        case TenantApiFactory.DELETE:
            return "tenantDelete";
        default:
            logger.error("Shop: unknown command for tenant call", key);
            throw Errors.RequestMapping;
    }
}

module.exports = TenantApiFactory;