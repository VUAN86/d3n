var jwtModule = require('jsonwebtoken');
var Errors = require('nodejs-errors');

/**
 * @typedef {object} Auth.TokenPayload
 * @property {string} userId
 * @property {string[]} roles
 */

/**
 * @typedef {object} AuthConfig
 * @property {string} publicKey
 * @property {string} algorithm
 */
/**
 * @typedef {object} Auth
 *
 * Auth Class validates user tokens and stores the token and validation state
 * @param {AuthConfig} config
 * @constructor
 */
function Auth(config) {
    this.config = config;

    this._token = null;

    this._isAuthenticated = false;
}

var o = Auth.prototype;



/**
 * Sets the token, resets authentication state
 * @param {string} token
 * @private
 */
o._setToken = function (token) {
    this._isAuthenticated = false;
    this._token = token;
};


/**
 * Get the token, if set
 * @returns {string|null}
 * @private
 */
o._getToken = function () {
    return this._token;
};

/**
 * Verifies token, if exists
 * @param {Function} cb
 * @returns {Number}
 * @private
 */
o._verifyToken = function (cb) {
    try {
        jwtModule.verify(this._getToken(), this.config.publicKey, {algorithms: [this.config.algorithm]}, cb);
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Authenticates and sets a token in Auths cache
 * @param {string} token
 * @param {Function} cb
 * @returns {Number}
 */
o.authenticate = function (token, cb) {
    try {
        var self = this;

        self._setToken(token);

        // verify the token first
        self._verifyToken(function (err) {
            if (err) {
                self._isAuthenticated = false;
                return cb(Errors.ERR_TOKEN_NOT_VALID);
            }

            self._isAuthenticated = true;
            return cb();
        });
    } catch (e) {
        return setImmediate(cb, e);
    }
};

/**
 * Check if client is authenticated ot not
 * @returns {Boolean}
 */
o.isAuthenticated = function () {
    return this._isAuthenticated === true;
};

/**
 * Returns client id of the token
 * @returns {Auth.TokenPayload|null}
 */
o.getTokenPayload = function () {
    if (this.isAuthenticated()) {
        return jwtModule.decode(this._getToken());
    } else {
        return null;
    }
};

module.exports = Auth;
