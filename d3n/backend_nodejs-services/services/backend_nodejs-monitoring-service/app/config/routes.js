var _ = require('lodash');
var HttpMonitoringApi = require('./../api/httpMonitoringApi.js');

module.exports = function (service) {
    // HTTP routing
    if (_.has(service, 'http') && !_.isUndefined(service.http) && !_.isNull(service.http)) {
        service.http.get('/',
          HttpMonitoringApi.home.bind(service.self)
        );
    }

};
