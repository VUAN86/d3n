var _ = require('lodash');
var Config = require('./../config/config.js');
var CrudHelper = require('nodejs-automapper').getInstance(Config).CrudHelper;
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

module.exports = ProfileService;
    
/**
 * Create a profile service for a specific method
 * @param clientSession session info about client
 */
function ProfileService(clientSession) {
    if (!clientSession) {
        var err = "missing client session";
        logger.error("Error building shop request for profile", err);
        throw err;
    }
    var connectionService = clientSession.getConnectionService();
    if (!connectionService) {
        var err = "missing connection service";
        logger.error("Error building shop request for profile", err);
        throw err;
    }
    var service = connectionService.getProfileService();
    if (!service) {
        var err = "missing profile service";
        logger.error("Error building shop request for profile", err);
        throw err;
    }
    this.service = service;
    var userId = clientSession.getUserId();
    if (!userId) {
        var err = "missing user id";
        logger.error("Error building shop request for profile", err);
        throw err;
    }
    this.userId = userId;
}

/**
 * Get user shop data from profile blob
 * @param callback method to be called when receiving response
 */
ProfileService.prototype.getShopData = function (callback) {
    try {
        // call profile service
        this.service.getUserProfileBlob({
            userId: this.userId,
            name: Config.userProfileShop.key
        }, undefined, function (err, message) {
            var shopData = _getShopData(message);
            setImmediate(callback, null, shopData);
        });
    } catch (ex) {
        logger.error("Error calling profile service", ex);
        setImmediate(callback, Errors.ProfileCall);
    }
};

/**
 * Set user shop data in profile blob
 * @param shopData user shop data
 * @param callback method to be called when receiving response
 */
ProfileService.prototype.setShopData = function (shopData, callback) {
    try {
        // call profile service
        this.service.updateUserProfileBlob({
            userId: this.userId,
            name: Config.userProfileShop.key,
            value: shopData
        }, undefined, function (err, message) {
            var messageError = message ? message.getError() : null;
            err = !err && messageError ? messageError : err;
            if (!Errors.isInstance(err)) err = Errors.ProfileCall;
            setImmediate(callback, err);
        });
    } catch (ex) {
        logger.error("Error calling profile service", ex);
        setImmediate(callback, Errors.ProfileCall);
    }
};

/**
 * Get user profile from profile service
 * @param callback method to be called when receiving response
 */
ProfileService.prototype.getUserProfile = function (callback) {
    try {
        // call profile service
        this.service.getUserProfile({
            userId: this.userId
        }, undefined, function (err, message) {
            try {
                var profile = _getUserProfile(message);
                setImmediate(callback, null, profile);
            } catch (e) {
                logger.error("Error calling profile service", e);
                setImmediate(callback, Errors.ProfileCall);
            }
        });
    } catch (ex) {
        logger.error("Error calling profile service", ex);
        setImmediate(callback, Errors.ProfileCall);
    }
};

function _getShopData(message) {
    try {
        var shopData = message.getContent().value;
        if (shopData) {
            return shopData;
        }
    } catch (ex) {
        logger.error("Exception reading shop data from user profile", ex);
    }
    return {}; //avoid error throwing, return empty shop data
}

function _getUserProfile(message) {
    try {
        var userProfile = message.getContent();
        if (userProfile && userProfile.profile) {
            return userProfile.profile;
        }
        else {
            throw "profile not found";
        }
    } catch (ex) {
        logger.error("Exception reading user profile", ex);
        throw Errors.NotFound;
    }
}
