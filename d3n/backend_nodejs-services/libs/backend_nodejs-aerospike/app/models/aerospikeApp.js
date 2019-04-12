var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeApp = function AerospikeApp(app) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(app);
};
AerospikeApp._namespace = Config.keyvalue.namespace;
AerospikeApp._set = 'appConfig';
AerospikeApp._ttl = 0;

module.exports = AerospikeApp;

//Copies attributes from a parsed json or other source
AerospikeApp.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the App in the database, overwrites existing items
AerospikeApp.prototype.save = function (callback) {
    var self = this;
    var err = _invalid(self);
    if (err) {
        return _error(err, callback);
    }
    AerospikeApp.toJSON(self, function (err, json) {
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikeApp,
                    key: key,
                    value: { value: new Buffer(JSON.stringify(json)) }
                }, function (err, res) {
                    var App = new AerospikeApp(json);
                    return _success(App, callback);
                });
            } catch (ex) {
                return _error(Errors.DatabaseApi.NoRecordId, callback);
            }
        } else {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    });
};

//Removes the App from the database
AerospikeApp.prototype.remove = function (callback) {
    var self = this;
    var key = _key(this);
    self.KeyvalueService.exec('remove', {
        model: AerospikeApp,
        key: key
    }, function (err, res) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
};

//Create new App and returns it if succesfull, null otherwise
AerospikeApp.create = function (params, callback) {
    var App = new AerospikeApp(params);
    return App.save(callback);
}

// Publish application in aerospike
AerospikeApp.publishApplication = function (params, callback) {
    AerospikeApp.create(params, callback);
}

// Unpublish application from aerospike
AerospikeApp.unpublishApplication = function (params, callback) {
    AerospikeApp.remove(params, callback);
}

//Update App template and returns it if succesfull, null otherwise
AerospikeApp.update = function (params, callback) {
    return AerospikeApp.findOne(params, function (err, App) {
        if (err || App !== null) {
            var newApp = new AerospikeApp(App);
            newApp = _copy(newApp, params);
            return newApp.save(callback);
        }
    });
}

AerospikeApp.remove = function (params, callback) {
    var App = new AerospikeApp(params);
    return App.remove(callback);
}

AerospikeApp.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeApp();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeApp,
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

AerospikeApp.toJSON = function (object, callback) {
    var plain = _copy({}, object, false);
    return _success(plain, callback);
};

function _copy(to, from, ext) {
    if (!to) {
        to = {};
    }
    if (!_.isObject(from)) {
        return to;
    }
    var data = AerospikeMapperService.map(from, 'applicationDataModel');
    return _.assign(to, data);
}

function _invalid(object) {
    return false;
}

function _key(params) {
    if (!_.isEmpty(params.application)) {
        return 'appConfig:' + params.tenant.id + ':' + params.application.id;
    } else {
        return 'appConfig:' + params.tenant.id;
    }
    
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var App = new AerospikeApp();
        result = App;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
