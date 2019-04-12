var _ = require('lodash');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
let logger = require('nodejs-logger')();

var AerospikeGameInstances = function AerospikeGameInstances(game) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(game);
};

//Copies attributes from a parsed json or other source
AerospikeGameInstances.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

AerospikeGameInstances._namespace = Config.keyvalue.namespace;
AerospikeGameInstances._set = 'gameInstance';
AerospikeGameInstances._ttl = 0;

module.exports = AerospikeGameInstances;

AerospikeGameInstances.findInstances = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeGameInstances();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeGameInstances,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                try {
                    var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.value));
                    return _success(resJson, callback);
                } catch (ex) {
                    return _error(Errors.DatabaseApi.NoRecordFound, callback);
                }
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};


function _key(params) {
    return 'game:' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var Game = new AerospikeGameInstances();
        result = Game;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'gameDataModel');
    return _.assign(to, data);
}
//Copies attributes from a parsed json or other source
AerospikeGameInstances.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}