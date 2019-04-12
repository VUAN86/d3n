var _ = require('lodash'),
    FakeApi = require('./../api/src.fakeApi.js');

module.exports = function (serviceProviders) {
    // WS routing
    if (_.has(serviceProviders, 'ws') && !_.isUndefined(serviceProviders.ws) && !_.isNull(serviceProviders.ws)) {
        _.each(_.functions(FakeApi), function (name) {
            serviceProviders.ws.messageHandlers[name] = FakeApi[name];
        });
    }
};