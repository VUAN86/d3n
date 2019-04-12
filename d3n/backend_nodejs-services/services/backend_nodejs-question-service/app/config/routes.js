var _ = require('lodash');
var cors = require('cors');
var WsBillingApi = require('./../api/wsBillingApi.js');
var WsQuestionApi = require('./../api/wsQuestionApi.js');
var WsQuestionVotingApi = require('./../api/wsQuestionVotingApi.js');
var WsQuestionTranslationApi = require('./../api/wsQuestionTranslationApi.js');
var WsQuestionTemplateApi = require('./../api/wsQuestionTemplateApi.js');
var WsPoolApi = require('./../api/wsPoolApi.js');
var WsRegionalSettingApi = require('./../api/wsRegionalSettingApi.js');
var WsWorkorderApi = require('./../api/wsWorkorderApi.js');
var WsWorkorderBillingApi = require('./../api/wsWorkorderBillingApi.js');
var WsQuestionLifetimeApi = require('./../api/wsQuestionLifetimeApi.js');
var WorkorderEventHandler = require('./../events/workorderEventHandler.js');
var httpQuestionApi = require('./../api/httpQuestionApi.js');


module.exports = function (service) {

    // HTTP routing
    if (_.has(service, 'http') && !_.isUndefined(service.http) && !_.isNull(service.http)) {
        service.http.options('/loadImages', cors());
        service.http.post('/loadImages',
            [cors(), service.httpUploadMiddleware.single('file_content')],
            httpQuestionApi.loadImages
        );

        service.http.options('/loadQuestions', cors());
        service.http.post('/loadQuestions',
            [cors(), service.httpUploadMiddleware.single('file_content')],
            httpQuestionApi.loadQuestions
        );

        service.http.options('/healthCheck', cors());
        service.http.get('/healthCheck', function (req, res) {
            res.send('OK');
        });
    }

    // WS message handlers
    if (_.has(service, 'ws') && !_.isUndefined(service.ws) && !_.isNull(service.ws)) {

        _.each(_.functions(WsQuestionApi), function (name) {
            service.ws.messageHandlers[name] = WsQuestionApi[name];
        });
        _.each(_.functions(WsQuestionTranslationApi), function (name) {
            service.ws.messageHandlers[name] = WsQuestionTranslationApi[name];
        });
        _.each(_.functions(WsPoolApi), function (name) {
            service.ws.messageHandlers[name] = WsPoolApi[name];
        });
        _.each(_.functions(WsQuestionTemplateApi), function (name) {
            service.ws.messageHandlers[name] = WsQuestionTemplateApi[name];
        });
        _.each(_.functions(WsRegionalSettingApi), function (name) {
            service.ws.messageHandlers[name] = WsRegionalSettingApi[name];
        });
        _.each(_.functions(WsWorkorderApi), function (name) {
            service.ws.messageHandlers[name] = WsWorkorderApi[name];
        });
        _.each(_.functions(WsWorkorderBillingApi), function (name) {
            service.ws.messageHandlers[name] = WsWorkorderBillingApi[name];
        });
        _.each(_.functions(WsBillingApi), function (name) {
            service.ws.messageHandlers[name] = WsBillingApi[name];
        });
        _.each(_.functions(WsQuestionVotingApi), function (name) {
            service.ws.messageHandlers[name] = WsQuestionVotingApi[name];
        });
        _.each(_.functions(WsQuestionLifetimeApi), function (name) {
            service.ws.messageHandlers[name] = WsQuestionLifetimeApi[name];
        });
        // Event handlers
        _.each(_.functions(WorkorderEventHandler), function (name) {
            service.ws.eventHandlers[name] = WorkorderEventHandler[name];
        });
    }

};
