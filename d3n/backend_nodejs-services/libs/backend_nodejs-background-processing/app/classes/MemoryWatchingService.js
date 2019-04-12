var _ = require('lodash');
var util = require('util');
var memwatch = require('memwatch-next');
var heapdump = require('heapdump');
var logger = require('nodejs-logger')();

var MemoryWatchingService = function (config) {
    var self = this;
    logger.info('MemoryWatchingService: initializing');
    self._start = _.now();
    function msFromStart() {
        return _.now() - self._start;
    }
    memwatch.on('stats', function (d) {
        logger.warn('MemoryWatchingService stats event: postgc info', msFromStart(), d.current_base);
    });
    memwatch.on('leak', function (info) {
        logger.warn('MemoryWatchingService leak event: memwatch info', info);
        if (!self._hd) {
            self._hd = new memwatch.HeapDiff();
        } else {
            var diff = self._hd.end();
            logger.warn('MemoryWatchingService leak event: heap difference', util.inspect(diff, true, null));
            self._hd = null;
        }
        var file = '/tmp/' + config.serviceName + '-' + process.pid + '-' + config.getHRTimestamp() + '.heapsnapshot';
        heapdump.writeSnapshot(file, function (err) {
            if (err) {
                logger.error(err);
            }
            else {
                logger.warn('MemoryWatchingService leak event: wrote heap snapshot to %s', file);
            }
        });
    });
    // Initial heap state on service startup
    var file = '/tmp/' + config.serviceName + '-' + process.pid + '-init-' + config.getHRTimestamp() + '.heapsnapshot';
    heapdump.writeSnapshot(file, function (err) {
        if (err) {
            logger.error(err);
        }
        else {
            logger.warn('MemoryWatchingService startup: wrote heap snapshot to %s', file);
        }
    });
};

module.exports = MemoryWatchingService;