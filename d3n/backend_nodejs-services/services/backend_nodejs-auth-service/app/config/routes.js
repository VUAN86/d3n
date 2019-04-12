var _ = require('lodash');
//var HttpApi = require('./../api/httpApi.js');
var WsApi = require('./../api/wsAuthApi.js');

module.exports = function (serviceProviders) {
    // HTTP routing
    //if (_.has(serviceProviders, 'http') && !_.isUndefined(serviceProviders.http) && !_.isNull(serviceProviders.http)) {
    //    serviceProviders.http.post('/register', HttpApi.register);
    //    serviceProviders.http.post('/auth', HttpApi.auth);
    //    serviceProviders.http.post('/refresh', HttpApi.refresh);
    //    serviceProviders.http.post('/changePassword', HttpApi.changePassword);
    //    serviceProviders.http.get('/getPublicKey', HttpApi.getPublicKey);
    //    serviceProviders.http.post('/setUserRole', HttpApi.setUserRole);
    //}
    // WS routing
    if (_.has(serviceProviders, 'ws') && !_.isUndefined(serviceProviders.ws) && !_.isNull(serviceProviders.ws)) {
        _.each(_.functions(WsApi), function (name) {
            serviceProviders.ws.messageHandlers[name] = WsApi[name];
        });
    }
};