var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var PaymentService = require('./../services/paymentService.js');

module.exports = {

    pay: function (clientSession, amount, currencyType, articleId, callback) {
        try {
            var paymentService = new PaymentService(clientSession);
            paymentService.pay(amount, currencyType, articleId, async.apply(_callback, callback));
        } catch (ex) {
            logger.error("Error calling payment service", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    rollback: function (clientSession, amount, currencyType, articleId, callback) {
        try {
            var paymentService = new PaymentService(clientSession);
            paymentService.rollback(amount, currencyType, articleId, async.apply(_callback, callback));
        } catch (ex) {
            logger.error("Error calling payment service", ex);
            setImmediate(callback, Errors.FatalError);
        }
    }
};

function _callback(callback, err, message) {
    var messageError = message ? message.getError() : null;
    err = !err && messageError ? messageError : err;
    if (!Errors.isInstance(err)) err = Errors.PaymentCall;
    var transactionId = _getTransactionId(message, err);
    setImmediate(callback, err, transactionId);
}

function _getTransactionId(message, err) {
    if (err) return "";
    try {
        var content = message.getContent();
        var transactionId = content.transactionId;
        return transactionId;
    } catch (ex) {
        logger.error("Error reading transaction id from payment response", ex);
        return ""; //avoid throwing error
    }
}