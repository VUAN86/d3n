if (process.env.NEW_RELIC_LICENSE_KEY) {
    try {
    	global.serviceNewrelic = require('newrelic');
    } catch (e) {
    	console.log('Error loading newrelic:', e);
    }
}

var config = require('./app/config/config.js'),
    Gateway = require('./app/classes/Gateway.js'),
    logger = require('nodejs-logger')()
;


var gateway = new Gateway(config);
gateway.build(function (err) {
    if(err) {
        logger.error('Error starting gateway', err);
        process.exit(1);
    }
    
    logger.info('Gateway started: port=' + config.gateway.port + ', ip=' + config.gateway.ip);
});