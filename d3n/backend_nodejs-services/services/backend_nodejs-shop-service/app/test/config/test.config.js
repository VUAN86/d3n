var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var TestData = require('./profile.data.js');

module.exports = {
    auth: {
        algorithm: 'RS256',
        privateKey: fs.readFileSync(path.join(__dirname, '/../../config/jwt-keys/privkey.pem'), 'utf8'),
        publicKey: fs.readFileSync(path.join(__dirname, '/../../config/jwt-keys/pubkey.pem'), 'utf8')
    },
    jwt: TestData.TEST_TOKEN,
    key: path.join(__dirname, './../../config/ssl-certificate/key.pem'),
    cert: path.join(__dirname, './../../config/ssl-certificate/cert.pem'),
};