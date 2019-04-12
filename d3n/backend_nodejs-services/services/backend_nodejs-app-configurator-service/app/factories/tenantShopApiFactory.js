var _ = require('lodash');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var ShopClient = require('nodejs-shop-client');

var shopClient = new ShopClient({
    registryServiceURIs: Config.registryServiceURIs,
    ownerServiceName: Config.serviceName
});

module.exports = {
    /**
     * Create/Update tenant in shop. 
     * @param {object} req
     * @param {callback} callback
     * @returns
     */
    tenantUpdate: function (req, callback) {
        var self = this;
        try {
            var params = req.body;
            var tenantItem = GetTenant(params);
            var create = !(_.has(params, 'id') && params.id);
            
            if (create) {
                shopClient.tenantAdd(tenantItem, callback);
            } else {
                shopClient.tenantUpdate(tenantItem, callback);
            }
        } catch (ex) {
            logger.error("tenantShopApiFactory:tenantUpdate error", ex);
            return CrudHelper.callbackError(ex, callback);
        }
    }
};

function GetTenant(params) {
    var tenant = {
        title: params.name ? params.name : '',
        description: params.description ? params.description : '',
        iconURL: params.logoUrl ? params.logoUrl : '',
        termsURL: params.url ? params.url : '',
        street: params.address ? params.address : '',
        streetNumber: '', // number contained in address
        city: params.city ? params.city : '',
        country: params.country ? params.country.substring(0, 2).toUpperCase() : '', // shop requires 2 letters country ISO code
        zip: '', // no zip available
        phone: params.phone ? params.phone : '',
        email: params.email ? params.email : '',
        taxNumber: params.vat ? params.vat : ''
    };

    if (_.has(params, 'id') && params.id) {
        tenant.id = params.id;
    }

    return tenant;
};