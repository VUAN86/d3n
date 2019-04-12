var _ = require('lodash');
var fs = require('fs');
var os = require('os');
var path = require('path');
var S3Client = require('nodejs-s3client').getInstance();

var Config = {
    serviceName: 'media',
    numCPUs: process.env.DEBUG ? 1 : require('os').cpus().length,
    ip: process.env.HOST || 'localhost',
    port: process.env.PORT || '4000',
    secure: true,
    key: fs.readFileSync(path.join(__dirname, 'ssl-certificate/key.pem'), 'utf8'),
    cert: fs.readFileSync(path.join(__dirname, 'ssl-certificate/cert.pem'), 'utf8'),
    registryServiceURIs: process.env.REGISTRY_SERVICE_URIS || 'ws://localhost:9093',
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? false : (process.env.VALIDATE_PERMISSIONS === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    timeoutUpdateProfilePicture: !_.isUndefined(process.env.TIMEOUT_UPDATE_PROFILE_PICTURE) ? parseInt(process.env.TIMEOUT_UPDATE_PROFILE_PICTURE) : 60000, // 1 minute
    gateway: {
        ip: process.env.HOST || 'localhost',
        port: (parseInt(process.env.PORT) + 9) || '9099',
        secure: true,
        key: path.join(__dirname, 'ssl-certificate/key.pem'),
        cert: path.join(__dirname, 'ssl-certificate/cert.pem'),
        auth: {
            algorithm: 'RS256',
            publicKey: fs.readFileSync(path.join(__dirname, '..', 'jwt-keys/pubkey.pem'), 'utf8'),
        },
    },
    auth: {
        algorithm: 'RS256',
        publicKey: fs.readFileSync(path.join(__dirname, '..', 'jwt-keys/pubkey.pem'), 'utf8'),
        privateKey: fs.readFileSync(path.join(__dirname, '..', 'jwt-keys/privkey.pem'), 'utf8')
    },
    httpUpload: {
        tempFolder: process.platform === 'win32' ? os.tmpdir() : '/tmp'
    }
};

var Database = require('nodejs-database').getInstance();
Config = _.assign(Config, Database.Config, S3Client.Config);

// Use a different bucket for S3 media
Config.amazon_s3.bucket = process.env.S3_API_BUCKET || S3Client.Config.buckets.medias.medias;

Config.httpURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'http' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}

Config.wsURL = function (uri, testport) {
    var secure = !(process.env.TEST === 'true') && Config.secure;
    return 'ws' + (secure ? 's' : '') + '://' + Config.ip + ':' + Config.port + (uri ? uri : '/');
}

module.exports = Config;
