var cp = require('child_process');
var async = require('async');
var assert = require('chai').assert;
var ProtocolMessage = require('nodejs-protocol');
var _ = require('lodash');

module.exports = {
    'createServiceWithWorker': function (config, cb) {
        var args = [JSON.stringify({service: config.service})];

        var workerService = cp.fork(config.workerScript, args);

        workerService.on('message', function (message) {
            if(message.success === true) {
                return cb(false, workerService);
            }
            return cb(new Error('error starting service'));
        });
    },
    
    'kill': function (clients, instances, workers) {
        for(var i=0; i<clients.length; i++) {
            try {
                clients[i].disconnect(1000, '', true);
            } catch (e) {
            }
        }
        for(var i=0; i<instances.length; i++) {
            try {
                instances[i].shutdown();
            } catch (e) {
            }
        }
        for(var i=0; i<workers.length; i++) {
            try {
                workers[i].kill();
            } catch (e) {
            }
        }
    },
    
    'sendMessagesSuccess': function (items, client, cb) {
        async.map(items, function (item, cbItem) {
            var m = new ProtocolMessage(item);
            if(_.isUndefined(m.getContent())) {
                m.setContent(null);
            }
            if(_.isUndefined(m.getError())) {
                m.setError(null);
            }
            if(_.isUndefined(m.getAck())) {
                m.setAck(null);
            }
            
            var seq = m.getSeq();
            client.sendMessage(m, function (err, response) {
                try {
                    if (seq === null) {
                        assert.ifError(err);
                        return cbItem(false, response);
                    } else {
                        assert.ifError(err);
                        assert.isNull(response.getError());
                        assert.isNotNull(response.getAck());
                        assert.strictEqual(response.getAck()[0], seq);
                        return cbItem(false, response);
                    }
                } catch (e) {
                    return cbItem(e);
                }
            });
        }, function (err, results) {
            if (err) {
                return cb(err);
            }
            try {
                assert.strictEqual(results.length, items.length);
                return cb();
            } catch (e) {
                return cb(e);
            }
        });
    }
};