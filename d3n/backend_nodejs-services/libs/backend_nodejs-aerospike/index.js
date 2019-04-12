var _ = require('lodash');
var Config = require('./app/config/config.js');
var Errors = require('./app/config/errors.js');
var KeyvalueService = require('./app/services/keyvalueService.js');

var Aerospike = function (config) {
    this._config = _.assign(Config, config);
    Aerospike.prototype.Config = this._config;
    Aerospike.prototype.Errors = Errors;
    Aerospike.prototype.KeyvalueService = KeyvalueService.getInstance(this._config);
}

Aerospike.getInstance = function (config) {
    if (_.isUndefined(Aerospike._instance)) {
        Aerospike._instance = new Aerospike(config);
    }
    return Aerospike._instance;
}

module.exports = Aerospike;