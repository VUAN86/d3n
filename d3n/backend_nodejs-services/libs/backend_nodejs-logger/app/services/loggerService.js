var _ = require('lodash'),
    winston = require('winston'),
    DefaultConfig = require('./../config/config.js');

/**
 * Creates a logger object, which can be used to log messages on console, file and or http server
 *
 * Using the environment variables LOG_FILE, LOG_SERVER and LOG_SERVER_PORT
 *
 * @param config Configuration, potentially overwriting settings from Default Configuration
 * @returns {*}
 */
module.exports = function (config) {
    config = _.assign(new DefaultConfig(), config || {});
    var logger = new (winston.Logger)({
        transports: [
            new (winston.transports.Console)(config)
        ]
    });

    if (process.env.LOG_FILE && process.env.LOG_FILE !== 'undefined' && process.env.LOG_FILE !== 'null') {
        logger.add(winston.transports.File, _.assign({}, config, {
            filename: process.env.LOG_FILE,
            handleExceptions: true,
            humanReadableUnhandledException: true
        }));
    }
    return logger;
};