var _ = require('lodash');
var fs = require('fs');
var path = require('path');
var async = require('async');
var cmaster = require('cron-master');
var logger = require('nodejs-logger')();
var heapdump = null;
var memwatch = null;

process.on('message', (message) => {
    if (_.has(message, 'start') && message.start) {
        process.send({ started: true });
        _loadJobs(message.config);
    }
});

var BackgroundProcessingService = function (config) {
    var self = this;
    self._config = config;
}

BackgroundProcessingService.prototype.init = function () {
    _loadJobs(this._config);
}

module.exports = BackgroundProcessingService;

function _loadJobs(config) {
    if (!_.isObject(config)) {
        return true;
    }
    if (_.isObject(config.memoryWatching) && config.memoryWatching.enabled && 
        _.isObject(config.backgroundProcesses) && config.backgroundProcesses.jobHeapDirrerences) {
        memwatch = require('memwatch-next');
    }
    if (_.isObject(config.memoryWatching) && config.memoryWatching.enabled && 
        _.isObject(config.backgroundProcesses) && config.backgroundProcesses.jobHeapSnapshots) {
        heapdump = require('heapdump');
    }
    var heapDiffs = [];
    logger.info('BackgroundProcessingService: initializing jobs');
    var allJobs = [];
    async.series([
        // Load common jobs
        function (next) {
            return cmaster.loadJobs(path.join(__dirname, '../', 'common-jobs'), function (err, jobs) {
                if (err) {
                    logger.error('Failed to load common jobs');
                    return next(err);
                }
                allJobs = _.union(allJobs, jobs);
                return next();
            });
        },
        // Load external service jobs
        function (next) {
            if (!_.isObject(config.backgroundProcesses) || !fs.existsSync(config.backgroundProcesses.jobs)) {
                logger.error('Failed to load "%s" service jobs', config.serviceName);
                return setImmediate(next, 'ERR_FATAL_ERROR');
            }
            return cmaster.loadJobs(config.backgroundProcesses.jobs, function (err, jobs) {
                if (err) {
                    logger.error('Failed to load jobs');
                    return next(err);
                }
                allJobs = _.union(allJobs, jobs);
                return next();
            });
        },
    ], function (err) {
        if (err) {
            return false;
        }
        logger.info('BackgroundProcessingService: %d jobs found', allJobs.length);
        allJobs.forEach(function (job) {
            if (job.meta.enabled) {
                job.meta._config = config;
                job.on(cmaster.EVENTS.TICK_STARTED, function () {
                    logger.info('Job "%s" tick starting', job.meta.name);
                    if (_.isObject(config.memoryWatching) && config.memoryWatching.enabled && 
                        _.isObject(config.backgroundProcesses) && config.backgroundProcesses.jobHeapDirrerences) {
                        memwatch.gc();
                        heapDiffs[job.meta.name] = new memwatch.HeapDiff();
                    }
                    if (_.isObject(config.memoryWatching) && config.memoryWatching.enabled && 
                        _.isObject(config.backgroundProcesses) && config.backgroundProcesses.jobHeapSnapshots) {
                        var file = '/tmp/' + config.serviceName + '-' + process.pid + '-' + job.meta.name.replace(' ', '-') + '-tick-starting-' + config.getHRTimestamp() + '.heapsnapshot';
                        heapdump.writeSnapshot(file, function (err) {
                            if (err) {
                                logger.error(err);
                            }
                            else {
                                logger.warn('BackgroundProcessingService: job "%s" [TICK_STARTED] - wrote heap snapshot to %s', job.meta.name, file);
                            }
                        });
                    }
                });
                job.on('tick-complete', function (err, res, time) {
                    logger.info('Job "%s" tick complete in %d ms', job.meta.name, time);
                    if (err) {
                        logger.error('Error running job "%s": "%s"', job.meta.name, err);
                    } else {
                        logger.info('Job "%s" completed with "%s"', job.meta.name, res);
                    }
                    if (_.isObject(config.memoryWatching) && config.memoryWatching.enabled && 
                        _.isObject(config.backgroundProcesses) && config.backgroundProcesses.jobHeapDirrerences) {
                        if (heapDiffs[job.meta.name]) {
                            var hde = heapDiffs[job.meta.name].end();
                            logger.info('Memory heap difference for "%s" job: %s', job.meta.name, JSON.stringify(hde, null, 2));
                            heapDiffs[job.meta.name] = null;
                        }
                    }
                    if (_.isObject(config.memoryWatching) && config.memoryWatching.enabled && 
                        _.isObject(config.backgroundProcesses) && config.backgroundProcesses.jobHeapSnapshots) {
                        var file = '/tmp/' + config.serviceName + '-' + process.pid + '-' + job.meta.name.replace(' ', '-') + '-tick-complete-' + config.getHRTimestamp() + '.heapsnapshot';
                        heapdump.writeSnapshot(file, function (err) {
                            if (err) {
                                logger.error(err);
                            }
                            else {
                                logger.warn('BackgroundProcessingService: job "%s" [TICK_COMPLETE] - wrote heap snapshot to %s', job.meta.name, file);
                            }
                        });
                    }
                });
                job.on(cmaster.EVENTS.TIME_WARNING, function () {
                    logger.info('Job "%s" has exceeded expected run time', job.meta.name);
                });
                job.on('overlapping-call', function () {
                    logger.info('Job "%s" attempting to run before previous tick is complete', job.meta.name);
                });
                job.start(function (err) {
                    logger.info('BackgroundProcessingService: job "%s" started', job.meta.name);
                });
            } else {
                logger.info('BackgroundProcessingService: job "%s" disabled', job.meta.name);
            }
        });
    });
}