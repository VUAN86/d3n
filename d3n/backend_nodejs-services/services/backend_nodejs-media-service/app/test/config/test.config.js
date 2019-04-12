var _ = require('lodash');
var fs = require('fs');
var TestData = require('./profile.data.js');

module.exports = {
    auth: {
        algorithm: 'RS256',
        privateKey: fs.readFileSync(__dirname + '/../../jwt-keys/privkey.pem', 'utf8'),
        publicKey: fs.readFileSync(__dirname + '/../../jwt-keys/pubkey.pem', 'utf8')
    },
    jwt: TestData.TEST_TOKEN,
    key: __dirname + './../../config/ssl-certificate/key.pem',
    cert: __dirname + './../../config/ssl-certificate/cert.pem',
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    validatePermissions: _.isUndefined(process.env.VALIDATE_PERMISSIONS) ? true : (process.env.VALIDATE_PERMISSIONS === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    sendEmailFile: __dirname + '/../../../temp-send-email.txt'
};
