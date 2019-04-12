var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ShopService = require('./../services/shopService.js');
var Mapper = require('./../helpers/mapper.js');
var Request = require('./../helpers/request.js');
var Timestamp = require('./../helpers/timestamp.js');
var ShopCredentialsGenerator = require('./../helpers/shopCredentialsGenerator.js');

module.exports = {
    setShoppingCart: function (clientSession, orderData, callback) {
        try {
            var userCredentials = ShopCredentialsGenerator.generate(clientSession);
            var shopService = new ShopService("addBasket", userCredentials);
            var queryString = _getShoppingCartQueryString(orderData);
            shopService.setQueryString(queryString);
            shopService.call(function (err, body) {
                var err = _getShoppingCartError(err, body);
                setImmediate(callback, err, orderData); //send back order data
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    checkShoppingCart: function (clientSession, article, callback) {
        try {
            var userCredentials = ShopCredentialsGenerator.generate(clientSession);
            var shopService = new ShopService("viewBasket", userCredentials);
            shopService.setQueryString("/"); //fix OXID shop API inconsistency
            shopService.call(function (err, body) {
                var err = _checkArticleInShoppingCart(err, body, article);
                setImmediate(callback, err, article, userCredentials); //send back article and user credentials
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    completeOrder: function (userCredentials, transactions, callback) {
        try {
            var shopService = new ShopService("orderComplete", userCredentials);
            var orderData = _getCompleteOrderData(transactions);
            shopService.setBody(orderData);
            shopService.call(function (err, body) {
                var errObj = { "value" : err };
                var order = "";
                if (body) {
                    order = _getOrder(errObj, body);
                } else  {
                    errObj.value = Errors.NotFound;
                }
                if (!Errors.isInstance(errObj.value)) errObj.value = Errors.ShopCall;
                setImmediate(callback, errObj.value, order);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, Errors.FatalError);
        }
    },

    orderList: function (params, clientSession, callback) {
        try {
            var shopService = new ShopService("orders");
            var queryString = _getOrderListQueryString(params,  clientSession);
            shopService.setQueryString(queryString);
            shopService.call(function (err, body) {
                var errObj = { "value" : err };
                var orders = _getOrders(errObj, body, params);
                if (!Errors.isInstance(errObj.value)) errObj.value = Errors.ShopCall;
                setImmediate(callback, errObj.value, orders);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, ex == Errors.RequestMapping ? Errors.RequestMapping : Errors.FatalError);
        }
    },

    finalizeOrder: function (clientSession, params, callback) {
        try {
            var shopService = new ShopService("orderFinalize");
            var queryString = _getFinalizeOrderQueryString(params);
            shopService.setQueryString(queryString);
            var order = _getRequestOrder(params, clientSession);
            shopService.setBody(order);
            shopService.call(function (err, body) {
                var errObj = { "value" : err };
                var order = "";
                if (body) {
                    order = _getOrder(errObj, body);
                } else  {
                    errObj.value = Errors.NotFound;
                }
                if (!Errors.isInstance(errObj.value)) errObj.value = Errors.ShopCall;
                setImmediate(callback, errObj.value, order);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, ex == Errors.RequestMapping ? Errors.RequestMapping : Errors.FatalError);
        }
    },
};

function _getShoppingCartError(err, body) {
    return err ? err : body != null ? Errors.ShopCall : null;
}

function _checkArticleInShoppingCart(err, body, article) {
    if (err) {
        return err;
    }
    try {
        if (body[Object.keys(body)[0]].oxid == article.id) {
            return null;
        }
    } catch (ex) {
        logger.error("Exception reading shopping cart content", ex);
        return Errors.NotFound;
    }
}

function _getShoppingCartQueryString(orderData) {
    var article = orderData.article;
    var articleId = article.id;
    if (!articleId) {
        var err = "invalid article id";
        logger.error("Error building shop request for shopping cart", err);
        throw err;
    }
    return "/" + articleId + "/1"; //always one piece
}

function _getCompleteOrderData(transactions) {
    var orderMapper = new Mapper("CompleteOrder");
    var orderData = orderMapper.get(transactions);

    return JSON.stringify(orderData);
}

function _getOrders(errObj, body, params) {
    if (!errObj.value) {
        try {
            var listMapper = new Mapper("List");
            var orderMapper = new Mapper("Order");

            var orders = listMapper.get(body);
            //restore limit and offset from request
            orders.offset = _.get(params, "offset");
            orders.limit = _.get(params, "limit");
            
            var items = [];
            _.forEach(orders.items, function(value, key) {
                var order = orderMapper.get(value);
                _setOrderCompoundFields(order, value);
                items.push(order);
            });
            orders.items = items;

            return orders;
        } catch (ex) {
            logger.error("Exception mapping shop/orderListResponse", ex);
            errObj.value = Errors.ResponseMapping;
        }
    }
}

function _getOrder(errObj, body) {
    if (!errObj.value) {
        if (body === Config.shop.messages.notFound) {
            errObj.value = Errors.NotFound;
        } else if (body === Config.shop.messages.alreadyFinalized) {
            errObj.value = Errors.AlreadyFinalized;
        } else {
            try {
                var orderMapper = new Mapper("Order");
                var order = orderMapper.get(body);
                _setOrderCompoundFields(order, body);
                return order;
            } catch (ex) {
                logger.error("Exception mapping shop/finalizeOrderResponse", ex);
                errObj.value = Errors.ResponseMapping;
            }
        }
    }
}
function _setOrderCompoundFields(dest, src) {
    var transactionInfoMapper = new Mapper("TransactionInfo");
    var articleMapper = new Mapper("Article");
    var billingAddressMapper = new Mapper("BillingAddress");
    var shippingAddressMapper = new Mapper("ShippingAddress");

    var transactionInfo = transactionInfoMapper.get(src);
    var article = articleMapper.get(src);
    var billingAddress = billingAddressMapper.get(src);
    var shippingAddress = shippingAddressMapper.get(src);

    dest.transactionInfo = transactionInfo;
    dest.article = article;
    dest.billingAddress = billingAddress;
    dest.shippingAddress = shippingAddress;
}
function _getOrderListQueryString(params, clientSession) {
    var requestParameters = Request.getRequestParameters(Request.ORDER, params, clientSession);

    var status = _.has(params, "searchBy.status") && (params.searchBy.status === "1" ||
        params.searchBy.status === "0") ? (params.searchBy.status === "1" ? "finished" : "pending") : "0";

    var queryString = "/" + requestParameters.tenantId + "/" + status + "/" + 
        requestParameters.pageNumber + "/" + requestParameters.itemsPerPage + "/" +
        requestParameters.orderBy + "/" + requestParameters.direction;

    return queryString;
}

function _getRequestOrder(params, clientSession) {
    if (!params.id) {
        var err = "invalid order id";
        logger.error("Error building shop request for finalizing order", err);
        throw err;
    }
    //for testing purposes - hidden parameter - allow reseting status to pending
    var isReset = params.reset;

    var timestamp = isReset ? Timestamp.EMPTY : Timestamp.now();
    var userId = isReset ? "" : clientSession.getUserId();
    
    var order = {
        "oxid": params.id,
        "oxsenddate": timestamp,
        "oxapprover": userId
    };
    return JSON.stringify(order);
}

function _getFinalizeOrderQueryString(params) {
    if (!(_.has(params, "id") && params.id)) {
            var err = "invalid order id";
            logger.error("Error building shop request for finalizing order", err);
            throw err;
    }
    return "/" + params.id;
}
