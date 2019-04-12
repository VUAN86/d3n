var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ProfileService = require('./../services/profileService.js');
var Mapper = require('./../helpers/mapper.js');
var Validator = require('./../helpers/validator.js');

module.exports = {

    setSelectedArticleId: function (params, clientSession, callback) {
        try {
            var articleId = _getArticleIdFromRequest(params);
            var tenantId = clientSession.getTenantId();
            var profileService = new ProfileService(clientSession);
            async.waterfall([
                profileService.getShopData.bind(profileService), // Get shop data from profile blob
                async.apply(_setSelectedArticleId, articleId, tenantId), // Update article id in shop data
                profileService.setShopData.bind(profileService) // Update and set shop data to profile blob
            ], function (err) {
                setImmediate(callback, err);
            });
        } catch (ex) {
            logger.error("Error setting article id in shop data", ex);
            setImmediate(callback, Errors.FatalError);
        }
    }, 

    getSelectedArticleId: function (clientSession, callback) {
        try {
            var profileService = new ProfileService(clientSession);
            profileService.getShopData(function (err, shopData) {
                var errObj = { "value" : err};
                var selectedArticleId = _getSelectedArticleId(errObj, clientSession, shopData);
                setImmediate(callback, errObj.value, { id: selectedArticleId });
            });
        } catch (ex) {
            logger.error("Error retrieving article id from shop data", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    clearSelectedArticleId: function (clientSession, callback) {
        try {
            var tenantId = clientSession.getTenantId();
            var profileService = new ProfileService(clientSession);
            async.waterfall([
                profileService.getShopData.bind(profileService), // Get shop data from profile blob
                async.apply(_clearSelectedArticleId, tenantId), // Clear article id in shop data
                profileService.setShopData.bind(profileService) // Update and set shop data to profile blob
            ], function (err) {
                if (err) {
                    logger.error("Error clearing article id in shop data", err);
                    err = Errors.ClearingShoppingCart;
                }
                setImmediate(callback, err);
            });
        } catch (ex) {
            logger.error("Error clearing article id in shop data", ex);
            setImmediate(callback, Errors.FatalError);
        }
    }, 

    setShippingAddress: function (params, clientSession, callback) {
        try {
            Validator.validateAddress(params);
            var profileService = new ProfileService(clientSession);
            async.waterfall([
                profileService.getShopData.bind(profileService), // Get shop data from profile blob
                async.apply(_setShippingAddress, params), // Update shipping address in shop data
                profileService.setShopData.bind(profileService) // Update and set shop data to profile blob
            ], function (err) {
                setImmediate(callback, err);
            });
        } catch (ex) {
            logger.error("Error setting shipping address in shop data", ex);
            setImmediate(callback, Errors.FatalError);
        }
    }, 

    getShippingAddress: function (clientSession, callback) {
        try {
            var profileService = new ProfileService(clientSession);
            profileService.getShopData(function (err, shopData) {
                try {
                    var errObj = { "value" : err};
                    var shippingAddress = _getShippingAddress(errObj, shopData);
                    setImmediate(callback, errObj.value, shippingAddress);
                } catch (e) {
                    logger.error("Error retrieving shipping address from shop data", e);
                    setImmediate(callback, Errors.FatalError);
                }
            });
        } catch (ex) {
            logger.error("Error retrieving shipping address from shop data", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    getProfileAddress: function (clientSession, callback) {
        try {
            var profileService = new ProfileService(clientSession);
            profileService.getUserProfile(function (err, profile) {
                try {
                    var profileAddress = _getUserProfileAddress(profile);
                    setImmediate(callback, err, profileAddress);
                } catch (e) {
                    logger.error("Error retrieving user address from profile", e);
                    setImmediate(callback, Errors.FatalError);
                }
            });
        } catch (ex) {
            logger.error("Error retrieving user address from profile", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },
};

function _getSelectedArticleId(errObj, clientSession, shopData) {
    if (!errObj.value) {
        try {
            var tenantId = clientSession.getTenantId();
            if (shopData && _.has(shopData, Config.userProfileShop.shoppingCart + "." + tenantId)) {
                return shopData[Config.userProfileShop.shoppingCart][tenantId];
            } else {
                errObj.value = Errors.NotFound;
            }
        } catch (ex) {
            logger.error("Exception reading selected article id from shop data", ex);
            errObj.value = Errors.FatalError;
        }
    }
}

function _setSelectedArticleId(articleId, tenantId, shopData, callback) {
    try {
        if (!_.isObject(shopData)) {
            shopData = {};
        }
        if (!_.isObject(shopData[Config.userProfileShop.shoppingCart])) {
            shopData[Config.userProfileShop.shoppingCart] = {};
        }
        shopData[Config.userProfileShop.shoppingCart][tenantId] = articleId;
        setImmediate(callback, null, shopData);
    } catch (ex) {
        logger.error("Exception setting selected article id to shop data", ex);
        setImmediate(callback, Errors.FatalError);
    }
}

function _clearSelectedArticleId(tenantId, shopData, callback) {
    try {
        if (_.isObject(shopData) && _.isObject(shopData[Config.userProfileShop.shoppingCart]) &&
                shopData[Config.userProfileShop.shoppingCart].hasOwnProperty(tenantId)) {
            delete shopData[Config.userProfileShop.shoppingCart][tenantId];
        }
        setImmediate(callback, null, shopData);
    } catch (ex) {
        logger.error("Exception clearing selected article id to shop data", ex);
        setImmediate(callback, Errors.FatalError);
    }
}
function _getShippingAddress(errObj, shopData) {
    if (!errObj.value) {
        try {
            if (shopData && _.has(shopData, Config.userProfileShop.shippingAddress)) {
                var shippingAddress = shopData[Config.userProfileShop.shippingAddress];
                Validator.validateAddress(shippingAddress);
                return shippingAddress;
            } else {
                errObj.value = Errors.NotFound;
            }
        } catch (ex) {
            logger.error("Exception reading shipping address from shop data", ex);
            errObj.value = Errors.FatalError;
        }
    }
}

function _setShippingAddress(shippingAddress, shopData, callback) {
    try {
        if (!_.isObject(shopData)) {
            shopData = {};
        }
        Validator.validateAddress(shippingAddress);
        shopData[Config.userProfileShop.shippingAddress] = shippingAddress;
        setImmediate(callback, null, shopData);
    } catch (ex) {
        logger.error("Exception setting shipping address to shop data", ex);
        setImmediate(callback, Errors.FatalError);
    }
}

function _getArticleIdFromRequest(params) {
    if (!(_.has(params, "articleId") && params.articleId)) {
        var err = "invalid article id";
        logger.error("Shop: error selecting article id in cart", err);
        throw err;
    }
    return params.articleId;
}

function _getUserProfileAddress(profile, clientSession) {
    try {
        var personMapper = new Mapper("ProfilePerson");
        var addressMapper = new Mapper("ProfileAddress");
        var profilePerson = personMapper.get(profile.person);
        var profileAddress = addressMapper.get(profile.address);
        profileAddress = _.assign(profileAddress, profilePerson);
        profileAddress.email = profile.emails[0].email;
        //manually add missing street number - to match shop mapping
        profileAddress.streetNumber = "";
        
        Validator.validateAddress(profileAddress);
    } catch (ex) {
        logger.error("Exception getting address from user profile", ex);
        throw Errors.FatalError;
    }
    return profileAddress;          
}