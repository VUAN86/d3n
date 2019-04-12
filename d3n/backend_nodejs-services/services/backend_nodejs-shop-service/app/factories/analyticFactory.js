var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var ClientInfo = require('nodejs-utils').ClientInfo;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var Validator = require('./../helpers/validator.js');
var KeyvalueService = require('nodejs-aerospike').getInstance(Config).KeyvalueService;
var AerospikeAnalyticEvent = KeyvalueService.Models.AerospikeAnalyticEvent;

var AnalyticFactory = {
    EVENT_TYPE: 'de.ascendro.f4m.server.analytics.model.ShopInvoiceEvent',
    EVENT_KEY: 'shopInvoice_#',

    addShopOrderEvent: function (clientSession, order, callback) {
        try {
            var clientInfo = new ClientInfo(clientSession.getClientInfo());
            var eventData = getAnalyticEventData(order);
            var event = {
                key: AnalyticFactory.EVENT_KEY.replace("#", order.id),
                content: {
                    eventType: AnalyticFactory.EVENT_TYPE,
                    userId: clientInfo.userId || null,
                    appId: clientInfo.appId || null,
                    tenantId: clientInfo.tenantId || null,
                    sessionIp: clientInfo.sessionIp || null,
                    eventData: eventData
                },
                timestamp: Date.now()
            };
            
            logger.debug("Adding shop order analytics event", event);
            AerospikeAnalyticEvent.add(event, callback);
        } catch (ex) {
            logger.error("Error adding shop order analytics event", ex);
            setImmediate(callback, ex);
        }
    } 
};

module.exports = AnalyticFactory;

function getAnalyticEventData(order) {
    var eventData = {
        "articleId": order.article.id,
        "articleNumber": order.article.articleNumber,
        "articleTitle": order.article.title,
        "originalPurchaseCost": 0,
        "paymentMoneyAmount": getPrice(order, Config.currencyType.money),
        "paymentBonusAmount": getPrice(order, Config.currencyType.bonus),
        "paymentCreditAmount": getPrice(order, Config.currencyType.credit)
    };
    return eventData;
}

function getPrice(order, currency) {
    var price = 0;
    switch (currency) {
        case Config.currencyType.money:
            if (Validator.isValidTransactionId(order.transactionInfo.money)) {
                if (Validator.isValidTransactionId(order.transactionInfo.bonus)) {
                    if (Validator.isValidPrice(order.article.moneyPrice)) {
                        price = Number(order.article.moneyPrice);
                    }
                } else {
                    if (Validator.isValidPrice(order.article.fullMoneyPrice)) {
                        price = Number(order.article.fullMoneyPrice);
                    }
                }
            }
            break;
        case Config.currencyType.bonus:
            if (Validator.isValidTransactionId(order.transactionInfo.bonus)) {
                if (Validator.isValidPrice(order.article.bonusPrice)) {
                    price = Number(order.article.bonusPrice);
                }
            }
            break;
        case Config.currencyType.credit:
            if (Validator.isValidTransactionId(order.transactionInfo.credit)) {
                if (Validator.isValidPrice(order.article.creditPrice)) {
                    price = Number(order.article.creditPrice);
                }
            }
            break;
        default:
    }
    return price;
}