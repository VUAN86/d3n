var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeWinningComponent = function AerospikeWinningComponent(winningComponent) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(winningComponent);
};
AerospikeWinningComponent._namespace = Config.keyvalue.namespace;
AerospikeWinningComponent._set = 'winningComponent';
AerospikeWinningComponent._ttl = 0;

module.exports = AerospikeWinningComponent;

//Copies attributes from a parsed json or other source
AerospikeWinningComponent.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the WinningComponent in the database, overwrites existing items
AerospikeWinningComponent.prototype.save = function (callback) {
    var self = this;
    AerospikeWinningComponent.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeWinningComponent,
                    key: key,
                    value: { value: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    if (err) {
                        return _error(err, callback);
                    }
                    return _success(self, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });    
};

//Removes the WinningComponent from the database
AerospikeWinningComponent.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikeWinningComponent,
            key: key
        }, function (err, res) {
            if (err) {
                return _error(err, callback);
            }
            return _success(null, callback);
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

// Publish winningComponent in aerospike
AerospikeWinningComponent.publish = function (params, callback) {
    var winningComponent = new AerospikeWinningComponent(params);
    return winningComponent.save(callback);
}

// Unpublish winningComponent from aerospike
AerospikeWinningComponent.unpublish = function (params, callback) {
    var winningComponent = new AerospikeWinningComponent(params);
    return winningComponent.remove(callback);
}

AerospikeWinningComponent.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeWinningComponent();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeWinningComponent,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.value));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeWinningComponent.toJSON = function (object, callback) {
    try {
        var plain = _copy({}, object, false);
        return _success(plain, callback);
    } catch (ex) {
        return _error(ex, callback);
    }
};

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'winningComponentDataModel');
    return _.assign(to, data);
}

function _key(params) {
    var key = params.id;
    if (!key) {
        key = params.winningComponentId;
    }
    return 'winning:winningComponent:' + key;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var WinningComponent = new AerospikeWinningComponent();
        result = WinningComponent;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
