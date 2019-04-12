var _ = require('lodash');
var async = require('async');
var cp = require('child_process');
var logger = require('nodejs-logger')();

function SchedulerServiceWorker () {
    this._worker = null;
};

var o = SchedulerServiceWorker.prototype;

o.build = function (cb) {
    try {
        var self = this;
        
        self._worker = cp.fork('./schedulerServiceWorker.js');
       //var 
    } catch (e) {
        return setImmediate(cb, e);
    }
};

process.on('message', function (m) {
    try {
        if (m.type === 'build') {
            
        }
    } catch (e) {
        logger.error('SchedulerServiceWorker on message error:', e);
    }
});