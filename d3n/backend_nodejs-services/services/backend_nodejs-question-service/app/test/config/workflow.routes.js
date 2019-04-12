var _ = require('lodash');
var FakeApi = require('./../api/workflow.fakeApi.js');

module.exports = function (serviceProviders) {
    // WS routing
    if (_.has(serviceProviders, 'ws') && !_.isUndefined(serviceProviders.ws) && !_.isNull(serviceProviders.ws)) {
        _.each(_.functions(FakeApi), function (name) {
            serviceProviders.ws.messageHandlers[name] = FakeApi[name];
        });
        
        //console.log(_.functions(serviceProviders.ws.messageHandlers));
    }
};