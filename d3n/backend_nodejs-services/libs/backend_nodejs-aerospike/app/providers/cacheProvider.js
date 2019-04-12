var _ = require('lodash');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

module.exports = {
    get: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        var value = storage.get(params.key);
        if (_.isNull(value)) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
        return _success(value, callback);
    },

    'map.getByKey': function (engine, storage, params, callback) {
        var self = this;
        return self.get(engine, storage, params, callback);
    },

    list: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'keys'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        var values = [];
        _.each(params.keys, function (key) {
            var value = storage.get(key);
            if (value) {
                values.push(value);
            }
        });
        if (values.length !== params.keys.length) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
        return _success(values, callback);
    },

    query: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'filters'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        var values = [];
        _.each(storage.keys(), function (key) {
            var value = storage.get(key);
            _.each(params.filters, function (filter) {
                _.each(_.keys(filter), function (filterFunc) {
                    var filterBin = _.keys(filter[filterFunc])[0];
                    var filterValue = _.values(filter[filterFunc])[0];
                    if (filterFunc === 'equal'
                        && _.has(value, filterBin)
                        && value[filterBin] === filterValue) {
                        values.push(value);
                    } else if (filterFunc === 'range'
                        && _.has(value, filterBin) && _.isArray(filterValue) && _.size(filterValue) === 2
                        && value[filterBin] >= filterValue[0] && value[filterBin] <= filterValue[1]) {
                        values.push(value);
                    } else if (filterFunc === 'contains'
                        && _.has(value, filterBin) && (_.hasOwnProperty(filterValue, 'contains'))
                        && filterValue.contains(value[filterBin])) {
                        values.push(value);
                    }
                });
            });
        });
        if (_.size(values) === 0) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
        return _success(values, callback);
    },

    incr: function (engine, storage, params, callback) {
        return this.incrAndGet(engine, storage, params, callback);
    },

    incrAndGet: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params, 'bin'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params, 'value'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        var value = storage.get(params.key);
        if (_.isNull(value) || !value[params.bin]) {
            value = {};
            value[params.bin] = 0;
        }
        value[params.bin] += params.value;
        var cachedValue = storage.put(params.key, value);
        return _success(cachedValue[params.bin], callback);
    },

    put: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params, 'value'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        var cachedValue = storage.put(params.key, params.value);
        return _success(cachedValue, callback);
    },

    'map.put': function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!(_.isObject(params) && _.has(params, 'mapValue'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        var cachedValue = storage.put(params.key, params.mapValue);
        return _success(cachedValue, callback);
    },

    remove: function (engine, storage, params, callback) {
        if (!(_.isObject(params) && _.has(params, 'key'))) {
            return _error(Errors.DatabaseApi.ValidationFailed, callback);
        }
        if (!storage.get(params.key)) {
            return _error(Errors.DatabaseApi.NoRecordFound, callback);
        }
        var result = storage.del(params.key);
        return _success(result, callback);
    },

    'map.removeByKey': function (engine, storage, params, callback) {
        var self = this;
        return self.remove(engine, storage, params, callback);
    },
}

function _success(res, callback) {
    return setImmediate(function (res, callback) {
        return callback(null, res);
    }, res, callback);
}

function _error(err, callback) {
    return setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}
