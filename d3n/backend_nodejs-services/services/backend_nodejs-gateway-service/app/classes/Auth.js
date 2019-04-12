var jwtModule = require('jsonwebtoken');

function Auth(config) {
    this.config = config;
};

var o = Auth.prototype;

o.verifyToken = function (token, cb) {
    jwtModule.verify(token, this.config.publicKey, {algorithms: [this.config.algorithm]}, cb);
};

module.exports = Auth;
