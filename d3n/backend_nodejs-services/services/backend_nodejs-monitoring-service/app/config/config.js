var _ = require('lodash');
var fs = require('fs');
var path = require('path');

var Config = {
    serviceName: 'monitoring',
    numCPUs: process.env.DEBUG ? 1 : require('os').cpus().length,
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || '4000',
    secure: true,
    key: fs.readFileSync(path.join(__dirname, 'ssl-certificate/key.pem'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname, 'ssl-certificate/cert.pem'), 'utf8'),
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    pullStatisticsInterval: process.env.PULL_STATISTICS_INTERVAL || 10000,
    dbStatisticsFile: path.join(__dirname, '../db/data.db'),
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: (parseInt(process.env.PORT) + 9) || '9099',
        secure: true,
        key: path.join(__dirname, 'ssl-certificate/key.pem'),
        cert: path.join(__dirname, 'ssl-certificate/cert.pem'),
        auth: {
            algorithm: 'RS256',
            publicKey: fs.readFileSync(path.join(__dirname, '..', 'jwt-keys/pubkey.pem'), 'utf8'),
        }
    },
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(path.join(__dirname, '..', 'jwt-keys/pubkey.pem'), 'utf8'),
        privateKey: fs.readFileSync(path.join(__dirname, '..', 'jwt-keys/privkey.pem'), 'utf8')
    },
    envSegistryServiceURIs: {
        development: process.env.REGISTRY_SERVICE_URIS_DEVELOPMENT,
        nightly: process.env.REGISTRY_SERVICE_URIS_NIGHTLY,
        staging: process.env.REGISTRY_SERVICE_URIS_STAGING,
        production: process.env.REGISTRY_SERVICE_URIS_PRODUCTION,
        testing: process.env.REGISTRY_SERVICE_URIS_TESTING,
        nightlyTesting: process.env.REGISTRY_SERVICE_URIS_NIGHTLY_TESTING
    },
    
    services: process.env.SERVICES ? process.env.SERVICES.split(',') : [],
    //question,winning,workflow,auth,voucher,appConfigurator,profile,promocode,media,userMessage,gameSelection,resultEngine,friend,gameEngine,payment,gateway,event,serviceRegistry
    httpAuth: {
        username: 'monitoring',
        password: '2Xd)"re2rj:,qm7~'
    }
};

Config.httpURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'http' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
};

module.exports = Config;
