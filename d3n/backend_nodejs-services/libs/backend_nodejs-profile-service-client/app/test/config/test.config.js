var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var TestData = require('./psc.data.js');

module.exports = {
    auth: {
        algorithm: 'RS256',
        privateKey: fs.readFileSync(path.join(__dirname, './../jwt-keys/privkey.pem'), 'utf8'),
        publicKey: fs.readFileSync(path.join(__dirname, './../jwt-keys/pubkey.pem'), 'utf8')
    },
    jwt: TestData.TEST_TOKEN,
    key: path.join(__dirname, './../ssl-certificate/key.pem'),
    cert: path.join(__dirname, './../ssl-certificate/cert.pem'),
    validateFullMessage: _.isUndefined(process.env.VALIDATE_FULL_MESSAGE) ? true : (process.env.VALIDATE_FULL_MESSAGE === 'true'),
    protocolLogging: _.isUndefined(process.env.PROTOCOL_LOGGING) ? false : (process.env.PROTOCOL_LOGGING === 'true'),
    sendEmailFile: path.join(__dirname, '/../../../temp-send-email.txt'),
};