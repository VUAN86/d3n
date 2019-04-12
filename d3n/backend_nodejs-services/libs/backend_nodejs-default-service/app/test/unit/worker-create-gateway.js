var Gateway = require('nodejs-gateway');



module.exports = function(config) {
    var gateway = new Gateway({
        gateway: config.gateway,
        serviceInstances: config.serviceInstances
    });

    return gateway;
};