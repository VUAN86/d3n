var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ShopService = require('./../services/shopService.js');
var Mapper = require('./../helpers/mapper.js');
var ShopCredentialsGenerator = require('./../helpers/shopCredentialsGenerator.js');

module.exports = {

    syncUser: function (clientSession, orderData, callback) {
        try {
            var shopService = new ShopService("user");
            var userCredentials = ShopCredentialsGenerator.generate(clientSession);
            var userData = _getUserData(clientSession, orderData, userCredentials);
            shopService.setBody(userData);
            shopService.call(function (err, body) {
                var err = _getBusinessError(err, body);
                setImmediate(callback, err, orderData); //send back order data
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    login: function (clientSession, callback) {
        try {
            var userCredentials = ShopCredentialsGenerator.generate(clientSession);
            var shopService = new ShopService("login", userCredentials);
            shopService.call(function (err, body) {
                if (!err && !(_.has(body, 'status') && body.status == "OK")) err = Errors.ShopCall;
                setImmediate(callback, err);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },  

    logout: function (callback) {
        try {
            var shopService = new ShopService("logout");
            shopService.call(function (err, body) {
                if (!err && body !== true) err = Errors.ShopCall;
                setImmediate(callback, err);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, Errors.FatalError);
        }
    }  
};

function _getBusinessError(err, body) {
    if (err) {
        logger.error("Exception reading shop response for createUser", err);
        return Errors.ShopCall;
    }
    try {
        if(typeof body === "string") {
            logger.debug("Invalid response for createUser", body);
            throw "invalid response for createUser";
        }
        return null;
    } catch (ex) {
        logger.error("Exception reading shop response for createUser", ex);
        return Errors.ShopCall;
    }
}

function _getUserData(clientSession, orderData, userCredentials) {
    var userId = clientSession.getUserId();
    var addressMapper = new Mapper("ShopAddress");
    
    var userData = addressMapper.get(orderData.billingAddress);
    userData.oxid = userId;
    userData.oxusername = userCredentials.username;
    userData.oxpassword = userCredentials.password;

    var shippingAddress = addressMapper.get(orderData.shippingAddress);
    shippingAddress.oxuserid = userId;
    userData._oxaddress = [shippingAddress];

    return JSON.stringify(userData);
}
