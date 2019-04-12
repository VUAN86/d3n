var md5 = require('md5');
var crypto = require('crypto');
var jwt = require('jsonwebtoken');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');

module.exports = {

    md5: function (payload) {
        return md5(payload);
    },
    
    encode: function (payload, options, callback) {
        var jwtOptions = {
            algorithm: Config.auth.algorithm,
            issuer: Config.jwt.issuer,
            expiresIn: Config.jwt.expiresInSeconds
        };
        if (options && options.expiresIn) {
            jwtOptions.expiresIn = options.expiresIn;
        }
        return jwt.sign(payload, Config.auth.privateKey, jwtOptions, callback);
    },

    encodePass: function (pass, salt) {
        var password = pass;
        if (salt) {
            password += salt.toString('utf8');
        }
        var encodedPassword = crypto.createHash('sha256').update(password).digest('hex');
        return encodedPassword;
    },
    
    encodePassWithFixedSalt: function (pass) {
        var encodedPassword = this.encodePass(pass, Config.fixedSalt);
        return encodedPassword;
    },

    validate: function (token, callback) {
        jwt.verify(token, Config.auth.publicKey, {
            algorithms: [Config.auth.algorithm]
        }, callback);
    },

    decode: function (token) {
        return jwt.decode(token, { complete: true });
    },

    getPublicKey: function () {
        // Remove CrLf symbols (required by Java Services) and then convert to Base64
        var publicKey = new Buffer(Config.auth.publicKey).toString().replace(/[\n\r]/g, '');
        var publicKeyBase64 = new Buffer(publicKey).toString('base64');
        return publicKeyBase64;
    },

    /**
     * Extracts payload info from JWT token
     * @param message Incoming message
     * @returns
     */
    getPayload: function (token) {
        var payload = null;
        try {
            payload = this.decode(token).payload;
        } catch (e) {
            throw new Error(Errors.AuthApi.ValidationFailed);
        }
        return payload;
    },

};