var _ = require('lodash');
var errors = require('nodejs-errors');

errors.ERR_CALLBACK_NOT_PROVIDED = {
    message: 'ERR_CALLBACK_NOT_PROVIDED'
};

errors.ERR_WRONG_REGISTRY_SERVICE_URIS = {
    message: 'ERR_WRONG_REGISTRY_SERVICE_URIS'
};


module.exports = errors;