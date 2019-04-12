var _ = require('lodash');
var WsCategoryApi = require('./../api/wsCategoryApi.js');
var WsArticleApi = require('./../api/wsArticleApi.js');
var WsOrderApi = require('./../api/wsOrderApi.js');
var WsUserProfileApi = require('./../api/wsUserProfileApi.js');
var WsTenantApi = require('./../api/wsTenantApi.js');

module.exports = function (service) {
    // WS message handlers
    _.each(_.functions(WsCategoryApi), function (name) {
        service.messageHandlers[name] = WsCategoryApi[name];
    });
    _.each(_.functions(WsArticleApi), function (name) {
        service.messageHandlers[name] = WsArticleApi[name];
    });
    _.each(_.functions(WsOrderApi), function (name) {
        service.messageHandlers[name] = WsOrderApi[name];
    });
    _.each(_.functions(WsUserProfileApi), function (name) {
        service.messageHandlers[name] = WsUserProfileApi[name];
    });
    _.each(_.functions(WsTenantApi), function (name) {
        service.messageHandlers[name] = WsTenantApi[name];
    });
};
