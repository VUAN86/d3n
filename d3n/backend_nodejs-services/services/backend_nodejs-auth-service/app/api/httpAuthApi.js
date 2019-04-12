var _ = require('lodash');
var Errors = require('./../config/errors.js');
var ApiFactory = require('./../factories/apiFactory.js');
var ProtocolMessage = require('nodejs-protocol');
var logger = require('nodejs-logger')();

module.exports = {
    // POST /register
    register: function (req, res, next) {
        var params = {};
        try {
            if (_.has(req.body, 'pass') && req.body['pass']) {
                _pushParam(req.body, params, 'pass');
                _pushParam(req.body, params, 'salt');
            } else if (_.has(req.body, 'facebook') && req.body['facebook']) {
                _pushParam(req.body, params, 'facebook', 'profile');
                params['provider'] = 'facebook';
            } else if (_.has(req.body, 'google') && req.body['google']) {
                _pushParam(req.body, params, 'google', 'profile');
                params['provider'] = 'google';
            }
            if (_.has(params, 'pass') && _.has(params, 'salt')) {
                ApiFactory.registerLogin(params, _message('auth/register'), function (err, data) {
                    _processData(err, data, res, next);
                });
            } else if (_.has(params, 'profile') && _.has(params, 'provider')) {
                ApiFactory.registerProvider(params, _message('auth/register'), function (err, data) {
                    _processData(err, data, res, next);
                });
            } else {
                _apiError('register', Errors.AuthApi.ValidationFailed, res, next);
            }
        } catch (e) {
            _apiError('register', e, res, next);
        }
    },
    
    // POST /auth
    auth: function (req, res, next) {
        var params = {};
        try {
            _pushParam(req.body, params, 'userid');
            if (_.has(req.body, 'pass') && req.body['pass']) {
                _pushParam(req.body, params, 'pass');
            } else if (_.has(req.body, 'facebook') && req.body['facebook']) {
                _pushParam(req.body, params, 'facebook', 'profile');
                params['provider'] = 'facebook';
            } else if (_.has(req.body, 'google') && req.body['google']) {
                _pushParam(req.body, params, 'google', 'profile');
                params['provider'] = 'google';
            }
            if (_.has(params, 'pass')) {
                ApiFactory.authLogin(params, _message('auth/auth'), function (err, data) {
                    _processData(err, data, res, next);
                });
            } else if (_.has(params, 'profile') && _.has(params, 'provider')) {
                ApiFactory.authProvider(params, _message('auth/auth'), function (err, data) {
                    _processData(err, data, res, next);
                });
            } else {
                _apiError('auth', Errors.AuthApi.ValidationFailed, res, next);
            };
        } catch (e) {
            _apiError('auth', e, res, next);
        }
    },

    // POST /refresh
    refresh: function (req, res, next) {
        var params = {};
        try {
            _pushParam(req.body, params, 'token');
            ApiFactory.refresh(params, _message('auth/refresh'), function (err, data) {
                _processData(err, data, res, next);
            });
        } catch (e) {
            _apiError('refresh', e, res, next);
        }
    },

    // POST /changePassword
    changePassword: function (req, res, next) {
        var params = {};
        try {
            _pushParam(req.body, params, 'userid');
            _pushParam(req.body, params, 'oldpass');
            _pushParam(req.body, params, 'newpass');
            _pushParam(req.body, params, 'salt');
            ApiFactory.changePassword(params, _message('auth/changePassword'), function (err, data) {
                _processData(err, data, res, next);
            });
        } catch (e) {
            _apiError('changePassword', e, res, next);
        }
    },
    
    // GET /getPublicKey
    getPublicKey: function (req, res, next) {
        var params = {};
        try {
            ApiFactory.getPublicKey(params, _message('auth/getPublicKey'), function (err, data) {
                _processData(err, data, res, next);
            });
        } catch (e) {
            _apiError('getPublicKey', e, res, next);
        }
    },
    
    // POST /setUserRole
    setUserRole: function (req, res, next) {
        var params = {};
        try {
            _pushParam(req.body, params, 'userid');
            _pushParamColl(req.body, params, 'toadd');
            _pushParamColl(req.body, params, 'toremove');
            ApiFactory.setUserRole(params, _message('auth/setUserRole'), function (err, data) {
                _processData(err, data, res, next);
            });
        } catch (e) {
            _apiError('setUserRole', e, res, next);
        }
    },

}

function _message(api) {
    var pm = new ProtocolMessage();
    pm.setMessage(api);
    return pm;
}

function _pushParam(content, params, names, dest) {
    if (_.isArray(names)) {
        _.each(names, function (name) {
            if (_.has(content, name)) {
                var param = content[name];
                if (param) {
                    params[dest || name] = param;
                }
            }
        });
    } else {
        var name = names;
        if (_.has(content, name)) {
            var param = content[name];
            if (param) {
                params[dest || name] = param;
            }
        }
    }
}

function _pushParamColl(content, params, name, dest) {
    if (_.has(content, name)) {
        var param = [];
        if (_.isArray(content[name])) {
            param = content[name];
        } else if (content[name]) {
            param = content[name].split(',');
        }
        if (param) {
            params[dest || name] = param;
        }
    } else {
        params[dest || name] = [];
    }
}

function _processData(err, data, res, next) {
    if (err) {
        var message = err.getMessageContainer();
        logger.error(message);
        res.status(401).send(message);
        res.end();
        if (next) {
            next(true);
        }
    } else {
        var message = data.getMessageContainer();
        logger.debug(message);
        res.send(message);
        res.end();
        if (next) {
            next(false);
        }
    }
}

function _apiError(api, err, res, next) {
    var error = new ProtocolMessage();
    error.setMessage(api + 'Response');
    if (err && _.isObject(err) && _.has(err, 'stack')) {
        error.setError(Errors.AuthApi.FatalError);
    } else {
        error.setError(err);
    }
    var message = error.getMessageContainer();
    logger.error(message);
    res.status(401).send(message);
    res.end();
    if (next) {
        next(true);
    }
}
