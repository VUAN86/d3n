var _ = require('lodash');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

var AerospikeConnection = function AerospikeConnection() {
    this.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
};
AerospikeConnection._namespace = Config.keyvalue.namespace;
AerospikeConnection._set = 'test';
AerospikeConnection._ttl = 0;

module.exports = AerospikeConnection;

AerospikeConnection.getStatus = function (cb) {
    try {
        var instance = new AerospikeConnection();
        if (instance.KeyvalueService._storage === null) {
            return setImmediate(cb, null, 'NS'); // not started
        }
        if (process.platform === 'win32') {
            return setImmediate(cb, null, 'OK');
        }
        if (!instance.KeyvalueService._storage.isConnected()) {
            return setImmediate(cb, null, 'NOK');
        }
        return instance.KeyvalueService.exec('get', {
            model: AerospikeConnection,
            key: 'test:connection'
        }, function (err, res) {
            if (err && err !== Errors.DatabaseApi.NoRecordFound) {
                return cb(null, 'NOK');
            }
            return cb(null, 'OK');
        });
    } catch (ex) {
        logger.error('AerospikeConnection.getStatus', ex);
        return setImmediate(cb, null, 'NS'); // not started
    }
};

AerospikeConnection.connect = function (cb) {
    var instance = new AerospikeConnection();
    return instance.KeyvalueService.connect(cb);
};