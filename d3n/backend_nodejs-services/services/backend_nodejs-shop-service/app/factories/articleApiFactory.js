var _ = require('lodash');
var async = require('async');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var ShopService = require('./../services/shopService.js');
var Mapper = require('./../helpers/mapper.js');
var Request = require('./../helpers/request.js');

var ArticleApiFactory = {
    GET: 1,
    UPDATE: 2,
    ADD: 3,
    DELETE: 4,

    articleList: function (params, clientSession, callback) {
        try {
            var shopService = new ShopService("articles");
            var queryString = _getArticleListQueryString(params,  clientSession);
            shopService.setQueryString(queryString);
            shopService.call(function (err, body) {
                var errObj = { "value" : err };
                var articles = _getArticles(errObj, body, params);
                if (!Errors.isInstance(errObj.value)) errObj.value = Errors.ShopCall;
                setImmediate(callback, errObj.value, articles);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, ex == Errors.RequestMapping ? Errors.RequestMapping : Errors.FatalError);
        }
    },

    article: function (action, clientSession, params, callback) {
        try {
            var serviceKey = _getServiceKey(action);
            var shopService = new ShopService(serviceKey);
            if (action != ArticleApiFactory.ADD) {
                var queryString = _getArticleQueryString(params);
                shopService.setQueryString(queryString);
            }
            if (action != ArticleApiFactory.GET) {
                var article = _getRequestArticle(params, clientSession);
                shopService.setBody(article);
            }
            shopService.call(function (err, body) {
                var errObj = { "value" : err };
                var article = "";
                if (action != ArticleApiFactory.DELETE) {
                    if (action == ArticleApiFactory.UPDATE && !body) {
                        errObj.value = Errors.NotFound;
                    } else  {
                        article = _getArticle(errObj, body);
                    }
                } else {
                    if (!body) {
                        errObj.value = Errors.NotFound;
                    } else if (body !== Config.shop.messages.deleteOk) {
                        errObj.value = Errors.ShopCall;
                    }
                }
                if (!Errors.isInstance(errObj.value)) errObj.value = Errors.ShopCall;
                setImmediate(callback, errObj.value, article);
            });
        } catch (ex) {
            logger.error("Error calling shop", ex);
            setImmediate(callback, ex == Errors.RequestMapping ? Errors.RequestMapping : Errors.FatalError);
        }
    },
};

function _getArticles(errObj, body, params) {
    if (errObj.value) {
        logger.error("Shop response error for shop/articleListResponse", errObj.value);
    } else {
        try {
            var listMapper = new Mapper("List");
            var articleMapper = new Mapper("Article");

            var articles = listMapper.get(body);
            //restore limit and offset from request
            articles.offset = _.get(params, "offset");
            articles.limit = _.get(params, "limit");
            
            var items = [];
            _.forEach(articles.items, function(value, key) {
                var article = articleMapper.get(value);
                _setArticleCompoundFields(article, value);
                items.push(article);
            });
            articles.items = items;

            return articles;
        } catch (ex) {
            logger.error("Exception mapping shop/articleListResponse", ex);
            errObj.value = Errors.ResponseMapping;
        }
    }
}

function _getArticle(errObj, body) {
    if (!errObj.value) {
        if (body === Config.shop.messages.notFound) {
            errObj.value = Errors.NotFound;
        } else if (body === Config.shop.messages.alreadyExists) {
            errObj.value = Errors.AlreadyExists;
        } else {
            try {
                var articleMapper = new Mapper("Article");
                var article = articleMapper.get(body);
                _setArticleCompoundFields(article, body);
                return article;
            } catch (ex) {
                logger.error("Exception mapping shop/articleGetResponse", ex);
                errObj.value = Errors.ResponseMapping;
            }
        }
    }
}

function _setArticleCompoundFields(dest, src) {
    dest.imageURLs = _getArticleImageUrls(src);
    dest.categoryId = _.get(src, "_oxcategory.oxcategories__oxid", "");
    dest.categoryTitle = _.get(src, "_oxcategory.oxcategories__oxtitle", "");
}

function _getArticleImageUrls(src) {
    var imageUrls = [];
    for (var i = 1; i <= 12; i++) {
        var key = "oxpic" + i;
        var oxpic = _.get(src, key, "");
        if (oxpic) imageUrls.push(oxpic);
    }
    return imageUrls;
}

function _getRequestArticle(params, clientSession) {
    var tenantId = clientSession.getTenantId();
    if (!tenantId) {
        var err = "missing tenant id";
        logger.error("Error building shop request for article", err);
        throw err;
    }
    var article = {
        "oxvendorid": "" + tenantId
    };
    _.forOwn(params, function(value, key) {
        if (key == "imageURLs" && params.imageURLs instanceof Array) {
            for (var i = 0; i < 12; i++) {
                article["oxpic" + (i + 1)] =
                    params.imageURLs[i] ? params.imageURLs[i] : "";
            }
        } else if (key == "categoryId") {
            article._oxcategory = {
                oxcategories__oxid: params.categoryId
            }
        } else if (key != "tenantId") { //ignore tenant id from params
            var fieldName = Request.getMappedField(key, "Article");
            article[fieldName] = value;
        }
    });
    return JSON.stringify(article);
}

function _getArticleListQueryString(params, clientSession) {
    var requestParameters = Request.getRequestParameters(Request.ARTICLE, params, clientSession);

    var categoryId = _.has(params, "searchBy.categoryId") && params.searchBy.categoryId ?
        params.searchBy.categoryId : "0";
    var searchBy = _.has(params, "searchBy.text") && params.searchBy.text ?
        params.searchBy.text : "0";
    var isActive = _.has(params, "isActive") && (params.isActive === "1" || params.isActive === "0") ?
        (params.isActive === "1" ? "active" : "inactive") : "0";
    var isInStock = _.has(params, "isInStock") ? "1" : "0";
    
    var queryString = "/" + requestParameters.pageNumber + "/" + requestParameters.itemsPerPage + "/" +
        requestParameters.tenantId + "/" + categoryId + "/" + searchBy + "/" +
        requestParameters.orderBy + "/" + requestParameters.direction + "/" + isActive + "/" + isInStock;

    return queryString;
}

function _getArticleQueryString(params) {
    if (!(_.has(params, "id") && params.id)) {
        var err = "invalid article id";
        logger.error("Error building shop request for article", err);
        throw err;
    }
    return "/" + params.id;
}

function _getServiceKey(key) {
    switch(key) {
        case ArticleApiFactory.ADD:
            return "articleAdd";
        case ArticleApiFactory.UPDATE:
            return "articleUpdate";
        case ArticleApiFactory.DELETE:
            return "articleDelete";
        default:
            return "article";
    }
}

module.exports = ArticleApiFactory;