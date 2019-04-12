var _ = require('lodash');
var Config = require('./../config/config.js');
var ClientInfo = require('nodejs-utils').ClientInfo;
var DefaultClient = require('nodejs-default-client');
var ProtocolMessage = require('nodejs-protocol');
var RegistryServiceClient = require('nodejs-service-registry-client');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

module.exports = PaymentService;
    
/**
 * Create a payment service for a specific method
 * @param clientSession session info about client
 */
function PaymentService(clientSession) {
    this.paymentServiceClient = _getPaymentClient();
    this.tenantId = _getTenantId(clientSession);
    this.userId = _getUserId(clientSession);
    this.clientInfo = new ClientInfo(clientSession.getClientInfo());
}

/**
 * Pay from user to tenant
 * @param amount the amount to pay
 * @param currency the currency
 * @param callback method to be called when receiving response
 */
PaymentService.prototype.pay = function (amount, currencyType, articleId, callback) {
    try {
        // send message for payment service
        var pm = _getPaymentMessage(this, amount, currencyType, articleId);
        this.paymentServiceClient.sendMessage(pm, callback);
    } catch (ex) {
        logger.error("Error calling payment service", ex);
        setImmediate(callback, Errors.PaymentCall);
    }
};

/**
 * Pay from tenant to user (rollback)
 * @param amount the amount to pay
 * @param currency the currency
 * @param callback method to be called when receiving response
 */
PaymentService.prototype.rollback = function (amount, currencyType, articleId, callback) {
    try {
        // send message for payment service
        var pm = _getPaymentMessage(this, amount, currencyType, articleId, true);
        logger.debug("Sending new payment message: " + JSON.stringify(pm));
        this.paymentServiceClient.sendMessage(pm, callback);
    } catch (ex) {
        logger.error("Error calling payment service", ex);
        setImmediate(callback, Errors.PaymentCall);
    }
};

function _getPaymentClient() {
    return new DefaultClient({
        serviceNamespace: "payment",
        reconnectForever: false,
        autoConnect: true,
        serviceRegistryClient: new RegistryServiceClient({registryServiceURIs: Config.registryServiceURIs}),
        headers: {service_name: 'shop_service'}
    });

}

function _getTenantId(clientSession) {
    var tenantId = clientSession.getTenantId();
    if (!tenantId) {
        var err = "missing tenant id";
        logger.error("Error building shop request for payment", err);
        throw err;
    }
    return tenantId;
}

function _getUserId(clientSession) {
    var userId = clientSession.getUserId();
    if (!userId) {
        var err = "missing user id";
        logger.error("Error building shop request for payment", err);
        throw err;
    }
    return userId;
}

function _getPaymentMessage(paymentService, amount, currencyType, articleId, isRollback) {
    var content = _getMessageContent(paymentService, amount, currencyType, articleId, isRollback);
    var pm = new ProtocolMessage();
    var message = currencyType == Config.currencyType.money ? "payment/transferBetweenAccounts" :
        "payment/loadOrWithdrawWithoutCoverage";

    pm.setMessage(message);
    pm.setSeq(paymentService.paymentServiceClient.getSeq());
    pm.setToken(null);
    pm.setClientInfo(paymentService.clientInfo);
    pm.setContent(content);

    return pm;
}

function _getMessageContent(paymentService, amount, currencyType, articleId, isRollback) {
    if (currencyType == Config.currencyType.money) {
        return _getMoneyMessageContent(paymentService, amount, articleId, isRollback);
    }

    var content = {
        tenantId: "" + paymentService.tenantId,
        profileId: "" + paymentService.userId,
        amount: Number(amount),
        currency: currencyType,
        paymentDetails: {
            additionalInfo: Config.paymentDetails + articleId
        }
    }
    if (currencyType == Config.currencyType.bonus && !isRollback) { // no rolback for credit
        content.amount *= -1;
    }

    return content;
}

function _getMoneyMessageContent(paymentService, amount, articleId, isRollback) {
    var content = {
        tenantId: "" + paymentService.tenantId,
        amount: Number(amount),
        currency: Config.currencyType.money,
        paymentDetails: {
            additionalInfo: Config.paymentDetails + articleId
        }
    }
    content[(!!isRollback ? "to" : "from") + "ProfileId"] = "" + paymentService.userId;

    return content;
}