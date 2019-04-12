var _ = require('lodash');
var logger = require('nodejs-logger')();
var Config = require('./../config/config.js');
var request = require("request");

module.exports = ShopService;
    
/**
 * Create a shop service for a specific method
 * @param methodName name of method, as defined in config
 * @param shop user credentials (will use impersonation if null)
 */
function ShopService(methodName, credentials) {
    this.method = methodName ? _.get(Config, "shop.entryPoints." + methodName) : null;
    if (!(this.method && _.has(this.method, "url") && _.has(this.method, "method"))) {
        var err = "http method not found";
        logger.error("Error building shop request", err);
        throw err;
    }

    this.authorizationHeader = _getAuthorizationHeader(credentials);
    this.queryString = this.body = "";
}

/**
 * Setter for request query string
 * @param queryString request query string
 */
ShopService.prototype.setQueryString = function (queryString) {
    this.queryString = queryString;
}

/**
 * Setter for request body
 * @param body request body
 */
ShopService.prototype.setBody = function (body) {
    this.body = body;
}

/**
 * Call shop method
 * @param callback method to be called when receiving response
 */
ShopService.prototype.call = function (callback) {
    var uri = _getUri(this.method.url, this.queryString);

    var requestObj = {
        uri: uri,
        method: this.method.method,
        headers: {
            "Authorization": this.authorizationHeader,
            "Content-Type": "application/json"
        }
    };
    if (this.method.method == "POST" || this.method.method == "PUT") {
        requestObj.body = this.body;
    }

    logger.debug("Shop " + this.method.method + " request " + uri +
        " \nbody: " + (requestObj.body ? requestObj.body : " <<empty>>"));
    request(requestObj, function (err, response, body) {
        if (!err && typeof body === "string") {
            try {
                logger.debug("Shop response for " + uri + " \nbody: " +
                    (requestObj.body ? requestObj.body : "<<no request body>>") +
                    ": \nResponse:" + (body ? body : "<<empty>>"));
                body = body ? JSON.parse(body) : null;
            } catch (ex) {
                logger.error("Shop response for " + uri + " - error: ", ex);
                return callback(ex, body);
            }
        }
        callback(err, body);
    });
};

function _getUri (methodUrl, queryString) {
    if (!methodUrl || methodUrl.startsWith("/") || methodUrl.endsWith("/")) {
        var err = "empty or invalid method url";
        logger.error("Error building shop request", err);
        throw err;
    }

    var shopUrl = _.get(Config, "shop.url");
    if (!shopUrl || !shopUrl.endsWith("/")) {
        var err = "empty or invalid shop url";
        logger.error("Error building shop request", err);
        throw err;
    }

    if (queryString && !queryString.startsWith("/")) {
        var err = "invalid query string " + queryString;
        logger.error("Error building shop request", err);
        throw err;
    }

    var uri = shopUrl + methodUrl + queryString;
    return uri;
};

function _getAuthorizationHeader (credentials) {
    if (!_.isObject(credentials)) {
        credentials = _.get(Config, "shop.admin");
        if (!credentials) {
            var err = "missing admin account set in config";
            logger.error("Error building shop request", err);
            throw err;
        }
    }

    var authorizationPrefix = _.get(Config, "shop.authorizationPrefix");
    if (!authorizationPrefix) {
        var err = "missing shop authorization prefix set in config";
        logger.error("Error building shop request", err);
        throw err;
    }

    var username = _.get(credentials, "username");
    var password = _.get(credentials, "password");
    if (!(username && password)) {
        var err = "invalid credentials structure set in config";
        logger.error("Error building shop request", err);
        throw err;
    }

    var authorizationToBeEncoded = username + ":" + password;
    var encodedAuthorization = new Buffer(authorizationToBeEncoded).toString('base64');

    return authorizationPrefix + encodedAuthorization;
}
