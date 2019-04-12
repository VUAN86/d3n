var _ = require('lodash');
var fs = require('fs');
var path = require('path');

var Config = {
    serviceName: 'liveAppManager',
    serviceNamespaces: [
        'messageCenter'
    ],
    numCPUs: process.env.DEBUG ? 1 : require('os').cpus().length,
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || '9097',
    secure: true,
    key: fs.readFileSync(path.join(__dirname,  'ssl-certificate/key.pem'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname,  'ssl-certificate/cert.pem'), 'utf8'),
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093',
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: (parseInt(process.env.PORT) + 9) || '9099',
        secure: true,
        key: path.join(__dirname,  'ssl-certificate/key.pem'),
        cert: path.join(__dirname,  'ssl-certificate/cert.pem'),
        auth: {
            algorithm: 'RS256',
            publicKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/pubkey.pem'), 'utf8'),
        },
    },
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/pubkey.pem'), 'utf8'),
        privateKey: fs.readFileSync(path.join(__dirname,  'jwt-keys/privkey.pem'), 'utf8')
    },
    jwt: {
        issuer: 'F4M Development and Testing',
        expiresInSeconds: 60 * 60 * 24 * 30 * 12 * 10, // 10 years = 10y * 12m * 30d * 24h * 60m * 60s
        impersonateForSeconds: 3600 // 1 hour 
    }
};

var Database = require('nodejs-database').getInstance();
Config = _.assign(Config, Database.Config);

Config.wsURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'ws' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}

module.exports = Config;