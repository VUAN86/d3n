var KeyvalueService = require('nodejs-aerospike').getInstance().KeyvalueService;
var AerospikeTaskLock = KeyvalueService.Models.AerospikeTaskLock;

var logger = require('nodejs-logger')();


module.exports = {
    lock: function (taskId, cb) {
        try {
            AerospikeTaskLock.lock(taskId, cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    },
    
    remove: function (taskId, cb) {
        try {
            AerospikeTaskLock.remove(taskId, cb);
        } catch (e) {
            return setImmediate(cb, e);
        }
    }
    
};
