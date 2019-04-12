var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikePromocodeClass = function AerospikePromocodeClass(promocodeClass) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(promocodeClass);
};
AerospikePromocodeClass._namespace = Config.keyvalue.namespace;
AerospikePromocodeClass._set = 'promocodeClass';
AerospikePromocodeClass._ttl = 0;

module.exports = AerospikePromocodeClass;

//Copies attributes from a parsed json or other source
AerospikePromocodeClass.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the PromocodeClass in the database, overwrites existing items
AerospikePromocodeClass.prototype.save = function (callback) {
    var self = this;
    AerospikePromocodeClass.toJSON(self, function (err, json) {
        if (err) {
            return _error(err, callback);
        }
        var key = _key(self);
        if (key) {
            try {
                self.KeyvalueService.exec('put', {
                    model: AerospikePromocodeClass,
                    key: key,
                    value: { promocodeClass: new Buffer(JSON.stringify(json)) }
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

//Removes the PromocodeClass from the database
AerospikePromocodeClass.prototype.remove = function (callback) {
    var self = this;
    try {
        var key = _key(self);
        self.KeyvalueService.exec('remove', {
            model: AerospikePromocodeClass,
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

// Publish promocode classes in aerospike
AerospikePromocodeClass.publish = function (params, callback) {
    return async.mapSeries(params.classes, function (classTemplate, nextClassTemplate) {
        var classInstances = [];
        // if (classTemplate.isUnique) {
        //     for (var instance = 1; instance <= classTemplate.number; instance++) {
        //         classInstances.push({ id: (classTemplate.code + ':' + instance), number: 1, isQRCode: classTemplate.isQRCode });
        //     }
        // } else {
            classInstances.push({ id: classTemplate.code, number: classTemplate.number, isQRCode: classTemplate.isQRCode });
        // }
        return async.mapSeries(classInstances, function (classInstance, nextClassInstance) {
            var promocodeClass = new AerospikePromocodeClass(classInstance);
            return promocodeClass.save(nextClassInstance);
        }, nextClassTemplate);
    }, function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

// Unpublish promocode classes from aerospike
AerospikePromocodeClass.unpublish = function (params, callback) {
    return async.mapSeries(params.classes, function (classTemplate, nextClassTemplate) {
        var classInstances = [];
        if (classTemplate.isUnique) {
            for (var instance = 1; instance <= classTemplate.number; instance++) {
                classInstances.push({ id: (classTemplate.code + ':' + instance), number: 1, isQRCode: classTemplate.isQRCode });
            }
        } else {
            classInstances.push({ id: classTemplate.code, number: classTemplate.number, isQRCode: classTemplate.isQRCode });
        }
        return async.mapSeries(classInstances, function (classInstance, nextClassInstance) {
            var promocodeClass = new AerospikePromocodeClass(classInstance);
            return promocodeClass.remove(nextClassInstance);
        }, nextClassTemplate);
    }, function (err) {
        if (err) {
            return _error(err, callback);
        }
        return _success(null, callback);
    });
}

AerospikePromocodeClass.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikePromocodeClass();
        return instance.KeyvalueService.exec('get', {
            model: AerospikePromocodeClass,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                var resJson = JSON.parse(new TextDecoder("utf-8").decode(res.promocodeClass));
                instance = instance.copy(resJson);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikePromocodeClass.toJSON = function (object, callback) {
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
    var data = AerospikeMapperService.map(from, 'promocodeClassDataModel');
    return _.assign(to, data);
}

function _key(params) {
    return 'promocodeClass:' + params.id;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var PromocodeClass = new AerospikePromocodeClass();
        result = PromocodeClass;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
