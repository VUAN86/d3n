var Gateway = require('nodejs-gateway');
var Config = require('../../config/config.js');
var Errors = require('../../config/errors.js');
var logger = require('nodejs-logger')();

var gateway = new Gateway({
    gateway: Config.gateway,
    serviceInstances: {
        'question': [
            {
                ip: Config.ip,
                port: Config.port,
                secure: Config.secure
            }
        ]
    }
});

gateway.build(function (err) {
    if (err) {
        logger.error('Error starting Gateway Service: ', Errors.FatalError);
        process.send({ built: false, err: err });
        process.exit(1);
    }
    logger.info('NodeJS Gateway Service started successfully');
    process.send({ built: true, err: null });
});
