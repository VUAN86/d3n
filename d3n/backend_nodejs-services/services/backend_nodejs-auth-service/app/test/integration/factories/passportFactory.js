var httpRequest = require('superagent'),
    Config = require('./../../../config/config.js'),
    Errors = require('./../../../config/errors.js'),
    JwtUtils = require('./../../../utils/jwtUtils.js'),
    Data = require('./../../config/profile.data.js'),
    ProtocolMessage = require('nodejs-protocol');

module.exports = {
    
    localLogin: function (req, username, password, callback) {
        process.nextTick(function () {
            _register(req, 'local', password, _auth, callback);
        });
    },

    facebookProfile: function (req, accessToken, refreshToken, profile, callback) {
        process.nextTick(function () {
            _register(req, 'facebook', accessToken, _auth, callback);
        });
    },

    googleProfile: function (req, accessToken, refreshToken, profile, callback) {
        process.nextTick(function () {
            _register(req, 'google', accessToken, _auth, callback);
        })
    },

    refresh: function (req, provider, data, callback) {
        process.nextTick(function () {
            _refresh(req, provider, data, callback);
        })
    },

    changePassword: function (req, data, callback) {
        process.nextTick(function () {
            _changePassword(req, data, callback);
        })
    }

}

function _register(req, provider, password, authCallback, callback) {
    var options = {
        pass: null,
        salt: null,
        facebook: null,
        google: null
    };
    if (provider === 'local') {
        options.pass = JwtUtils.encodePass(JwtUtils.md5(password), provider);
        options.salt = provider;
    } else if (provider === 'facebook') {
        options.facebook = password;
    } else if (provider === 'google') {
        options.google = password;
    }
    _authApi('register', options, function (err, message) {
        if (err) {
            return callback(null, false, req.flash('indexMessage', err.message));
        }
        authCallback(req, provider, message.getContent().userid, password, callback);
    });
}

function _auth(req, provider, userid, password, callback) {
    var options = {
        userid: userid,
        pass: null,
        facebook: null,
        google: null
    };
    if (provider === 'local') {
        options.pass = JwtUtils.md5(password);
    } else if (provider === 'facebook') {
        options.facebook = password;
    } else if (provider === 'google') {
        options.google = password;
    }
    _authApi('auth', options, function (err, message) {
        if (err) {
            return callback(null, false, req.flash('loginMessage', err.message));
        } else {
            var data = JwtUtils.decode(message.getContent().token);
            if (data) {
                data.token = message.getContent().token;
                data.provider = provider;
                return callback(null, data);
            } else {
                return callback(null, false, { message: req.flash('loginMessage', 'ERR_INVALID_JWT') });
            }
        }
    });
}

function _refresh(req, provider, data, callback) {
    var options = {
        token: data.token
    };
    _authApi('refresh', options, function (err, message) {
        if (err) {
            return callback(null, false, req.flash('indexMessage', err.message));
        } else {
            var exp = JwtUtils.decode(message.getContent().token).payload.exp;
            if (message.getContent().token) {
                return callback(null, { token: message.getContent().token, exp: exp }, req.flash('indexMessage', 'Token has been refreshed'));
            } else {
                return callback(null, false, { message: req.flash('indexMessage', 'ERR_INVALID_JWT') });
            }
        }
    });
}

function _changePassword(req, data, callback) {
    var options = {
        userid: data.userid,
        oldpass: JwtUtils.md5(req.body.oldpass),
        newpass: JwtUtils.encodePass(JwtUtils.md5(req.body.newpass), 'local'),
        salt: 'local',
    };
    _authApi('changePassword', options, function (err, message) {
        if (err) {
            return callback(null, false, req.flash('indexMessage', err.message));
        }
        return callback(null, true, req.flash('indexMessage', 'Password has been changed'));
    });
}

function _authApi(api, data, callback) {
    try {
        httpRequest
            .post(Config.httpURL('/' + api))
            .send(data)
            .set('Accept', 'application/json')
            .end(function (err, res) {
                var message = new ProtocolMessage(JSON.parse(res.text));
                if (message.getError()) {
                    return callback(message.getError());
                }
                return callback(null, message);
            });
    } catch (ex) {
        return callback(Errors.AuthApi.FatalError);
    }
}
