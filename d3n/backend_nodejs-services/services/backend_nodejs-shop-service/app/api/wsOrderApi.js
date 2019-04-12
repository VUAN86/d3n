var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var CrudHelper = require('nodejs-automapper').getInstance(Config).CrudHelper;
var Errors = require('./../config/errors.js');
var PaymentError = require('./../helpers/paymentError.js');
var PaymentRollbackError = require('./../helpers/paymentRollbackError.js');
var RollbackError = require('./../helpers/rollbackError.js');
var Validator = require('./../helpers/validator.js');
var ArticleApiFactory = require('./../factories/articleApiFactory.js');
var EmailApiFactory = require('./../factories/emailApiFactory.js');
var OrderApiFactory = require('./../factories/orderApiFactory.js');
var PaymentApiFactory = require('./../factories/paymentApiFactory.js');
var TenantAdminApiFactory = require('./../factories/tenantAdminApiFactory.js');
var UserProfileApiFactory = require('./../factories/userProfileApiFactory.js');
var UserShopApiFactory = require('./../factories/userShopApiFactory.js');
var AnalyticFactory = require('./../factories/analyticFactory.js');

module.exports = {

    /**
     * WS API shop/initiateOrder
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    initiateOrder: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });

            async.waterfall([
                async.apply(_getOrderData, clientSession, params), //addresses and article
                _checkArticleValidAnyPrice, //addresses and article
                async.apply(UserShopApiFactory.syncUser, clientSession), //sync user in shop
                async.apply(_loginUser, clientSession),
                async.apply(OrderApiFactory.setShoppingCart, clientSession), //add article in shop basket
            ], function (err, orderData) {
                CrudHelper.handleProcessed(err, orderData, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/completeOrder
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    completeOrder: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });

            var isFullPrice = _.get(params, "isFullPrice", false);
            
            async.waterfall([
                async.apply(_getOrderArticle, clientSession), //article
                async.apply(_checkArticleValidPrice, isFullPrice), //article
                async.apply(_loginUser, clientSession),
                async.apply(OrderApiFactory.checkShoppingCart, clientSession), //article and user credentials
                async.apply(_purchaseArticle, clientSession, isFullPrice), //transaction ids and order id
                async.apply(_clearShoppingCart, clientSession), //credit transaction id, order id, tenant information
                async.apply(_addAnalyticEvent, clientSession), //credit transaction id, order id, tenant information and clear shopping cart error
            ], function (err, shopOrderId) {
                CrudHelper.handleProcessed(err, { orderId: shopOrderId }, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/orderList
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    orderList: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            OrderApiFactory.orderList(params, clientSession, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  

    /**
     * WS API shop/finalizeOrder
     * @param message Incoming message
     * @param clientSession Default Service client session object
     */
    finalizeOrder: function (message, clientSession) {
        var params = {};
        try {
            _.each(_.keys(message.getContent()), function (name) {
                CrudHelper.pushParam(message, clientSession, params, name);
            });
            OrderApiFactory.finalizeOrder(clientSession, params, function (err, data) {
                CrudHelper.handleProcessed(err, data, message, clientSession);
            });
        } catch (ex) {
            CrudHelper.handleFailure(ex, message, clientSession);
        }
    },  
};

function _getOrderData(clientSession, params, callback) {
    var isShippingAddress = _.get(params, "isShippingAddress", false);
    async.parallel({
        article: async.apply(_getOrderArticle, clientSession),
        shippingAddress: async.apply(_getShippingAddress, clientSession, isShippingAddress),
        billingAddress: async.apply(UserProfileApiFactory.getProfileAddress, clientSession),
    }, callback);
}

function _getOrderArticle(clientSession, callback) {
    async.waterfall([
        async.apply(UserProfileApiFactory.getSelectedArticleId, clientSession),
        async.apply(ArticleApiFactory.article, ArticleApiFactory.GET, clientSession)
    ], callback);
}

function _getShippingAddress(clientSession, isShippingAddress, callback) {
    if (isShippingAddress) {
        UserProfileApiFactory.getShippingAddress(clientSession, function (err, shippingAddress) {
            if (err == Errors.NotFound) {
                return UserProfileApiFactory.getProfileAddress(clientSession, callback);
            }
            setImmediate(callback, err, shippingAddress);
        });
    } else {
        return UserProfileApiFactory.getProfileAddress(clientSession, callback);
    }
}

function _loginUser(clientSession, orderData, callback) {
    async.waterfall([
        UserShopApiFactory.logout,
        async.apply(UserShopApiFactory.login, clientSession),
    ], function(err) {
        setImmediate(callback, err, orderData);
    });
}

function _checkArticleValidAnyPrice(data, callback) {
    try {
        var isPriceValid = Validator.isValidPrice(data.article.fullMoneyPrice) ||
            Validator.isValidPrice(data.article.moneyPrice) || Validator.isValidPrice(data.article.bonusPrice);
        setImmediate(callback, isPriceValid ? null : Errors.MissingPrice, data);
    } catch (ex) {
        setImmediate(callback, Errors.FatalError);
    }
}

function _checkArticleValidPrice(isFullPrice, article, callback) {
    try {
        var isPriceValid = isFullPrice ? Validator.isValidPrice(article.fullMoneyPrice) :
            Validator.isValidPrice(article.moneyPrice) || Validator.isValidPrice(article.bonusPrice);
        setImmediate(callback, isPriceValid ? null : Errors.MissingPrice, article);
    } catch (ex) {
        setImmediate(callback, Errors.FatalError);
    }
}
function _purchaseArticle(clientSession, isFullPrice, article, userCredentials, callback) {
    _getTenantInformation(clientSession, function(err, tenantInformation) {
        if (err) {
            setImmediate(callback, err);
        } else {
            async.waterfall([
                async.apply(_payArticle, clientSession, article, tenantInformation, isFullPrice),
                async.apply(_payCredit, clientSession, article, tenantInformation),
                async.apply(_completeOrder, clientSession, article, userCredentials, tenantInformation, isFullPrice),
            ], callback);
        }
    });
}

function _getTenantInformation(clientSession, callback) {
    async.parallel({
        name: async.apply(TenantAdminApiFactory.getName, clientSession),
        adminList: async.apply(TenantAdminApiFactory.getAdminList, clientSession),
    }, callback);
}

function _payArticle(clientSession, article, tenantInformation, isFullPrice, callback) {
    if (isFullPrice) {
        _payMoney(clientSession, article, tenantInformation, true, callback);
    } else {
        async.waterfall([
            async.apply(_payMoney, clientSession, article, tenantInformation, false),
            async.apply(_payBonusWithRollback, clientSession, article, tenantInformation),
        ], callback);
    }
}

function _completeOrder(clientSession, article, userCredentials, tenantInformation, isFullPrice, transactions, callback) {
    OrderApiFactory.completeOrder(userCredentials, transactions, function(err, order) {
        if (err) {
            //finalize order failed: rollback transactions
            _rollbackPayment(clientSession, article, tenantInformation, transactions, isFullPrice, callback);
        } else {
            var result = {
                order: order,
                creditTransactionId: transactions.credit,
                tenantInformation: tenantInformation
            }
            setImmediate(callback, null, result);
        }
    });
}

function _clearShoppingCart(clientSession, shopOrder, callback) {
    UserProfileApiFactory.clearSelectedArticleId(clientSession, function(err) {
        //store error, not throw - purchase will continue
        shopOrder.clearShoppingCartError = err;
        setImmediate(callback, null, shopOrder);
    });
}

function _addAnalyticEvent(clientSession, shopOrder, callback) {
    AnalyticFactory.addShopOrderEvent(clientSession, shopOrder.order, function(err) {
        if (err) {
            //adding the analytic event fails - send email to tenant admins
            EmailApiFactory.sendEmailAddingAnalyticEventFailed(clientSession, shopOrder.tenantInformation, 
                    err, shopOrder.order.id, function(eventErr) {
                if (eventErr) {
                    logger.error("Error sending email about analytics. Shop order id: '" + shopOrder.order.id + "'", eventErr);
                }
                //failing adding analytic event is silent to FE - no callback followup
            });
        }
        //as final step in ordering, check non-blocking errors: from paying credits reward and clearing shopping cart
        var finalErr = shopOrder.clearShoppingCartError;
        if (shopOrder.creditTransactionId && Errors.isInstance(shopOrder.creditTransactionId)) {
            finalErr = finalErr ? Errors.NoRewardAndClearingShoppingCart : Errors.NoReward;
        }
        setImmediate(callback, finalErr, shopOrder.order.orderNumber);
    });
}

function _payMoney(clientSession, article, tenantInformation, isFullPrice, callback) {
    var moneyPrice = isFullPrice ? article.fullMoneyPrice : article.moneyPrice;
    if (Validator.isValidPrice(moneyPrice)) {
        PaymentApiFactory.pay(clientSession, moneyPrice, Config.currencyType.money, article.id,
            function(err, transactionId) {
                if (err && !Errors.is(err, Errors.PaymentNotCovered)) {
                    //payment with money failed: send email to tenant admins
                    logger.error("Error paying money", err);
                    EmailApiFactory.sendEmailMoneyPaymentFailed(clientSession, tenantInformation,
                            moneyPrice, err, function(emailErr) {
                        var errMessage = PaymentError.message(emailErr);
                        logger.error("Error paying money after sending email (" + emailErr + ")", errMessage);
                        setImmediate(callback, errMessage, transactionId);
                    });
                } else {
                    setImmediate(callback, err, isFullPrice ? { money: transactionId, bonus: null } : transactionId);
                }
            }
        );
    } else {
        // if not full price, no payment nor error needed (null transactionId, bonus pay will follow)
        setImmediate(callback, isFullPrice ? Errors.MissingPrice : null, null);
    }
}

function _payBonus(clientSession, article, tenantInformation, moneyTransactionId, callback) {
    if (Validator.isValidPrice(article.bonusPrice)) {
        PaymentApiFactory.pay(clientSession, article.bonusPrice, Config.currencyType.bonus, article.id,
            function(err, bonusTransactionId) {
                var transactions = {
                    money: moneyTransactionId,
                    bonus: bonusTransactionId
                }
                if (err && !Errors.is(err, Errors.PaymentNotCovered)) {
                    //payment with bonus failed: send email to tenant admins
                    logger.error("Error paying bonus", err);
                    EmailApiFactory.sendEmailBonusPaymentFailed(clientSession, tenantInformation, 
                            article.bonusPrice, err, moneyTransactionId, function(emailErr) {
                        var errMessage = PaymentError.message(emailErr);
                        logger.error("Error paying bonus after sending email (" + emailErr + ")", errMessage);
                        setImmediate(callback, errMessage, transactions);
                    });
                } else {
                    setImmediate(callback, err, transactions);
                }
            }
        );
    } else {
        //no payment needed (null transactionId)
        setImmediate(callback, null, {
            money: moneyTransactionId,
            bonus: null
        });
    }
}

function _payCredit(clientSession, article, tenantInformation, transactions, callback) {
    if (Validator.isValidPrice(article.creditPrice)) {
        PaymentApiFactory.pay(clientSession, article.creditPrice, Config.currencyType.credit, article.id,
            function(err, creditTransactionId) {
                if (err) { //no special case for payment not covered
                    //payment with credit failed: send email to tenant admins
                    logger.error("Error paying credit", err);
                    EmailApiFactory.sendEmailCreditPaymentFailed(clientSession, tenantInformation, 
                        article.creditPrice, err, function(emailErr) {
                            //store error, not throw - purchase will continue
                            var errMessage = PaymentError.message(emailErr);
                            logger.error("Error paying credit after sending email (" + emailErr + ")", errMessage);
                            transactions.credit = errMessage;
                            setImmediate(callback, null, transactions);
                        });
                } else {
                    transactions.credit = creditTransactionId;
                    setImmediate(callback, err, transactions);
                }
            }
        );
    } else {
        //no payment needed (null transactionId)
        transactions.credit = null;
        setImmediate(callback, null, transactions);
    }
}

function _payBonusWithRollback(clientSession, article, tenantInformation, moneyTransactionId, callback) {
    _payBonus(clientSession, article, tenantInformation, moneyTransactionId, function(err, transactions) {
            if (err) {
                //payment with bonus failed: rollback money payment
                _rollbackMoney(clientSession, article, tenantInformation, transactions.money, false, callback);
            } else {
                setImmediate(callback, err, transactions);
            }
        }
    );
}

function _rollbackPayment(clientSession, article, tenantInformation, transactions, isFullPrice, callback) {
    async.parallel(async.reflectAll([ //ensure all errors are catched
        async.apply(_rollbackMoney, clientSession, article, tenantInformation, transactions.money, isFullPrice),
        async.apply(_rollbackBonus, clientSession, article, tenantInformation, transactions.bonus, isFullPrice),
        async.apply(_rollbackCredit, clientSession, article, tenantInformation, transactions.credit),
    ]), function(error, results) {
        var rollbackErr = _getRollbackError(results);
        setImmediate(callback, rollbackErr);
    });
}

function _rollbackMoney(clientSession, article, tenantInformation, moneyTransactionId, isFullPrice, callback) {
    var moneyPrice = isFullPrice ? article.fullMoneyPrice : article.moneyPrice;
    if (Validator.isValidPrice(moneyPrice) && moneyTransactionId) {
        PaymentApiFactory.rollback(clientSession, moneyPrice, Config.currencyType.money, article.id,
            function(err, rollbackTransactionId) {
                if (err) {
                    //rollback with money failed: send email to tenant admins
                    EmailApiFactory.sendEmailMoneyRollbackFailed(clientSession, tenantInformation,
                        moneyPrice, err, moneyTransactionId, function(emailErr) {
                            setImmediate(callback, RollbackError.message(emailErr));
                        });
                } else {
                    var transactions = {
                        money: moneyTransactionId,
                        rollback: rollbackTransactionId
                    }
                    //rollback with money succeeded: send email to tenant admins
                    EmailApiFactory.sendEmailMoneyRollbackSucceeded(clientSession, tenantInformation,
                        moneyPrice, moneyTransactionId, function(emailErr) {
                            setImmediate(callback, PaymentRollbackError.message(emailErr), transactions);
                        });
                }
            }
        );
    } else {
        //no rollback needed (null transactionId)
        setImmediate(callback, null, {
            money: moneyTransactionId,
            rollback: null
        });
    }
}

function _rollbackBonus(clientSession, article, tenantInformation, bonusTransactionId, isFullPrice, callback) {
    if (!isFullPrice && Validator.isValidPrice(article.bonusPrice) && bonusTransactionId) {
        PaymentApiFactory.rollback(clientSession, article.bonusPrice, Config.currencyType.bonus, article.id,
            function(err, rollbackTransactionId) {
                if (err) {
                    //rollback with bonus failed: send email to tenant admins
                    EmailApiFactory.sendEmailBonusRollbackFailed(clientSession, tenantInformation,
                        article.bonusPrice, err, bonusTransactionId, function(emailErr) {
                            setImmediate(callback, RollbackError.message(emailErr));
                        });
                } else {
                    var transactions = {
                        bonus: bonusTransactionId,
                        rollback: rollbackTransactionId
                    }
                    //rollback with bonus succeeded: send email to tenant admins
                    EmailApiFactory.sendEmailBonusRollbackSucceeded(clientSession, tenantInformation,
                        article.bonusPrice, bonusTransactionId, function(emailErr) {
                            setImmediate(callback, PaymentRollbackError.message(emailErr), transactions);
                        });
                }
            }
        );
    } else {
        //no payment needed (null transactionId)
        setImmediate(callback, null, {
            bonus: bonusTransactionId,
            rollback: null
        });
    }
}

function _rollbackCredit(clientSession, article, tenantInformation, creditTransactionId, callback) {
    if (Validator.isValidPrice(article.creditPrice) && creditTransactionId &&
            //if credit payment fails, creditTransactionId contains error - so nothing to rollback
            !Errors.isInstance(creditTransactionId)) {
        PaymentApiFactory.rollback(clientSession, article.creditPrice, Config.currencyType.credit, article.id,
            function(err, rollbackTransactionId) {
                if (err) {
                    //rollback with credit failed: send email to tenant admins
                    EmailApiFactory.sendEmailCreditRollbackFailed(clientSession, tenantInformation,
                        article.creditPrice, err, creditTransactionId, function(emailErr) {
                            setImmediate(callback, RollbackError.message(emailErr));
                        });
                } else {
                    var transactions = {
                        credit: creditTransactionId,
                        rollback: rollbackTransactionId
                    }
                    //rollback with credit succeeded: send email to tenant admins
                    EmailApiFactory.sendEmailCreditRollbackSucceeded(clientSession, tenantInformation,
                        article.creditPrice, creditTransactionId, function(emailErr) {
                            setImmediate(callback, PaymentRollbackError.message(emailErr), transactions);
                        });
                }
            }
        );
    } else {
        //no payment needed (null transactionId)
        setImmediate(callback, null, {
            credit: creditTransactionId,
            rollback: null
        });
    }
}
function _getRollbackError(results) {
    var rollbackErr = Errors.PaymentRollback; //least important error
    _.forEach([
        //possible errors ordered by importance
        Errors.RollbackWithoutNotificationRecipient,
        Errors.RollbackWithoutNotification,
        Errors.Rollback,
        Errors.PaymentRollbackWithoutNotificationRecipient,
        Errors.PaymentRollbackWithoutNotification
    ], function (err) {
        if (_.includes([ results[0].error, results[1].error], err)) {
            rollbackErr = err;
            return false;
        }
    }); 
    return rollbackErr;   
}