var _ = require('lodash');
var logger = require('nodejs-logger')();
var jjv = require('jjv')();
var protocolSchemas = require('nodejs-protocol-schemas');
var schemas = protocolSchemas.schemas;
jjv.addSchema('addressModel', schemas['shop'].definitions.addressModel);

module.exports = {

    validateAddress: function(obj) {
        if (!_.isObject(obj)) {
            var err = "address is not an object";
            logger.error("Shop: error validating address", err);
            throw err;
        }
        var err = jjv.validate('addressModel', obj);
        if (err) {
            var err = "invalid address structure";
            logger.error("Shop: error validating address", err);
            throw err;
        }
    },

    isValidPrice: function(price) {
        //price should contain a positive number
        var isValidPrice = !isNaN(price) && price > 0;
        return isValidPrice;
    },

    isValidTransactionId: function(transactionId) {
        //transaction id should contain a positive integer
        var n = Math.floor(Number(transactionId));
        var isValidTransactionId = String(n) === transactionId && n >= 0;
        return isValidTransactionId;
    }
}