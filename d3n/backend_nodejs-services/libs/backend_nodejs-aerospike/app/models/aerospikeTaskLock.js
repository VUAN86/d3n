var _ = require('lodash');
var async = require('async');
var Config = require('./../config/config.js');
var Errors = require('./../config/errors.js');
var logger = require('nodejs-logger')();

var KEY_PREFIX = 'task:';
var BIN_TIMESTAMP = 'timestamp';

function AerospikeTaskLock(taskId) {
    var self = this;
    self.KeyvalueService = require('./../../index.js').getInstance().KeyvalueService;
    self.taskId = taskId;
};
AerospikeTaskLock._namespace = Config.keyvalue.namespace;
AerospikeTaskLock._set = 'tasklock';
AerospikeTaskLock._ttl = 60*60*24*3; // 3 days

AerospikeTaskLock.prototype.lock = function (callback) {
    try {
        var self = this;
        var key = _key(self.taskId);
        var value = {};
        value[BIN_TIMESTAMP] = parseInt(Date.now()/1000);
        self.KeyvalueService.exec('putWithPolicy', {
            model: AerospikeTaskLock,
            key: key,
            value: value,
            policy: {
                exists: "policy.exists.CREATE"
            }
        }, function (err) {
            if (err) {
                if (err === Errors.DatabaseApi.RecordExists) {
                    // alredy locked
                    return callback(false, false);
                }
                
                return callback(err);
            }
            
            return callback(false, true);
        });
    } catch (e) {
        return setImmediate(callback, e);
    }
};

AerospikeTaskLock.prototype.remove = function (callback) {
    try {
        var self = this;
        var key = _key(self.taskId);
        self.KeyvalueService.exec('remove', {
            model: AerospikeTaskLock,
            key: key
        }, function (err) {
            if (err) {
                if (err === Errors.DatabaseApi.NoRecordFound) {
                    return callback();
                }
                return callback(err);
            }
            return callback();
        });
        
    } catch (e) {
        return setImmediate(callback, e);
    }
    
};


AerospikeTaskLock.lock = function(taskId, callback) {
    var inst = new AerospikeTaskLock(taskId);
    return inst.lock(callback);
};

AerospikeTaskLock.remove = function(taskId, callback) {
    var inst = new AerospikeTaskLock(taskId);
    return inst.remove(callback);
};



function _key(taskId) {
    return KEY_PREFIX + taskId;
}

module.exports = AerospikeTaskLock;
