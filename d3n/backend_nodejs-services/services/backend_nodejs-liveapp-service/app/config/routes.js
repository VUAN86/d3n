var _ = require('lodash');
var WsLiveMessageApi = require('./../api/wsLiveMessageApi.js');

module.exports = function (service) {
    // WS message handlers
    _.each(_.functions(WsLiveMessageApi), function (name) {
        service.messageHandlers[name] = WsLiveMessageApi[name];
    });
};
