var _ = require('lodash');
var crypto = require('crypto');
var Config = require('./../config/config.js');

module.exports = {

    generate: function(clientSession) {
        var userId = clientSession.getUserId();
        //credential algorithm subject to internal agreement
        var username = userId;
        var password = generatePassword(userId);

        return {
            username: username,
            password: password
        };
    }
}

function generatePassword(str) {
    var salt = Config.shop.pwdSalt;
    //var password = crypto.createHash('sha256').update(str + salt).digest('hex');
    var password = str;

    return password;
    
}