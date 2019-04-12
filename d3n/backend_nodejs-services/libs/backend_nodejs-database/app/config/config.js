var fs = require('fs');
var logger = require('nodejs-logger')();

var Config = {
    rdbms: {
        host: process.env.RDBMS_HOST || 'localhost',
        port: process.env.RDBMS_PORT || '3306',
        schema: process.env.RDBMS_SCHEMA || 'f4m',
        username: process.env.RDBMS_USERNAME || 'root',
        password: process.env.RDBMS_PASSWORD || 'masterkey',
        pool: {
            max: 5,
            min: 0,
            idle: 10000
        },
        limit: 20,
        logging: process.env.DATABASE_LOGGER === 'true' ? logger.info : null,
        force: true
    },
    statisticsEnabled: process.env.STATISTICS_ENABLED === 'true'
};

module.exports = Config;
