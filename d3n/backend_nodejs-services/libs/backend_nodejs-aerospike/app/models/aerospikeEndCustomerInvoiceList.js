var _ = require('lodash');
var async = require('async');
var TextDecoder = require('text-encoding').TextDecoder;
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var AerospikeMapperService = require('./../services/mapperService.js').getInstance(Config);

var AerospikeEndCustomerInvoiceList = function AerospikeEndCustomerInvoiceList(invoiceEndCustomerList) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self = self.copy(invoiceEndCustomerList);
};
AerospikeEndCustomerInvoiceList._namespace = Config.keyvalue.namespace;
AerospikeEndCustomerInvoiceList._set = 'endConsumerInvoice';
AerospikeEndCustomerInvoiceList._ttl = 0;

module.exports = AerospikeEndCustomerInvoiceList;

//Copies attributes from a parsed json or other source
AerospikeEndCustomerInvoiceList.prototype.copy = function (object) {
    var self = this;
    return _copy(self, object, true);
}

//Saves the EndCustomerInvoiceList in the database, overwrites existing items
AerospikeEndCustomerInvoiceList.prototype.save = function (callback) {
    var self = this;
    var key = _key(self);
    if (key) {
        try {
            return self.KeyvalueService.exec('get', {
                model: AerospikeEndCustomerInvoiceList,
                key: key
            }, function (err, res) {
                if (!err && res !== null) {
                    self = self.copy(res.endConsInvoice);
                }
                return AerospikeEndCustomerInvoiceList.toJSON(self, function (err, json) {
                    if (err) {
                        return _error(err, callback);
                    }
                    return self.KeyvalueService.exec('put', {
                        model: AerospikeEndCustomerInvoiceList,
                        key: key,
                        value: { endConsInvoice: json.data }
                    }, function (err, res) {
                        var EndCustomerInvoiceList = new AerospikeEndCustomerInvoiceList(self);
                        return _success(EndCustomerInvoiceList, callback);
                    });
                });
            });
        } catch (ex) {
            return _error(Errors.DatabaseApi.NoRecordId, callback);
        }
    } else {
        return _error(Errors.DatabaseApi.NoRecordId, callback);
    }
};

//Removes the EndCustomerInvoiceList from the database
AerospikeEndCustomerInvoiceList.prototype.remove = function (callback) {
    var self = this;
    var key = _key(self);
    try {
        self.KeyvalueService.exec('remove', {
            model: AerospikeEndCustomerInvoiceList,
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

// Publish EndCustomerInvoiceList in aerospike
AerospikeEndCustomerInvoiceList.publish = function (params, callback) {
    var invoiceEndCustomer = new AerospikeEndCustomerInvoiceList(params);
    return invoiceEndCustomer.save(callback);
}

// Unpublish invoiceEndCustomerList in aerospike
AerospikeEndCustomerInvoiceList.unpublish = function (params, callback) {
    var invoiceEndCustomer = new AerospikeEndCustomerInvoiceList(params);
    return invoiceEndCustomer.remove(callback);
}

AerospikeEndCustomerInvoiceList.findOne = function (params, callback) {
    try {
        var key = _key(params);
        var instance = new AerospikeEndCustomerInvoiceList();
        return instance.KeyvalueService.exec('get', {
            model: AerospikeEndCustomerInvoiceList,
            key: key
        }, function (err, res) {
            if (err || res === null) {
                return _error(Errors.DatabaseApi.NoRecordFound, callback);
            } else {
                instance = instance.copy(res.endConsInvoice);
                return _success(instance, callback);
            }
        });
    } catch (ex) {
        return _error(Errors.DatabaseApi.NoRecordFound, callback);
    }
};

AerospikeEndCustomerInvoiceList.toJSON = function (object, callback) {
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
    if (!to.key) {
        to.key = {};
    }
    if (!_.isArray(to.data)) {
        to.data = [];
    }
    if (!_.isObject(from) && !_.isArray(from)) {
        return to;
    }
    if (_.isArray(from)) {
        _.forEach(from, function (item) {
            if (_.isString(item)) {
                to.data.push(item);
            } else {
                to.data.push(JSON.stringify(item));
            }
        });
        return to;
    }
    if (_.isObject(from.key)) {
        to.key = _.clone(from.key);
    } else {
        to.key.tenantId = from.tenantId;
        to.key.profileId = from.profileId;
    }
    if (_.isArray(from.data)) {
        _.forEach(from.data, function (item) {
            if (_.isString(item)) {
                to.data.push(item);
            } else {
                to.data.push(JSON.stringify(item));
            }
        });
    }
    var newItem = {};
    if (_.has(from, 'invoiceSeriesPrefix') && _.has(from, 'invoiceSeriesNumber')) {
        newItem = AerospikeMapperService.map(from, 'invoiceEndCustomerDataModel');
    }
    if (!_.isEmpty(newItem)) {
        to.data.push(JSON.stringify(newItem));
    }
    return to;
}

function _key(params) {
    var key = params;
    if (_.has(params, 'key') && _.isObject(params.key)) {
        key = params.key;
    }
    return 'endConsumerInvoiceList:' + key.tenantId + ':' + key.profileId;
}

function _error(err, callback) {
    setImmediate(function (err, callback) {
        return callback(err);
    }, err, callback);
}

function _success(data, callback) {
    var result = data;
    if (!result) {
        var EndCustomerInvoiceList = new AerospikeEndCustomerInvoiceList();
        result = EndCustomerInvoiceList;
    }
    setImmediate(function (result, callback) {
        return callback(null, result);
    }, result, callback);
}
