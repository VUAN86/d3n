var _ = require('lodash');
var logger = require('nodejs-logger')();
var heapdump = require('heapdump');
var CronMasterJob = require('cron-master').CronMasterJob;

module.exports = new CronMasterJob({
    timeThreshold: (10 * 60 * 1000), // Warning after 10 minute work
    
    meta: {
        enabled: false,
        name: 'Heap Snapshot'
    },
    
    cronParams: {
        cronTime: '* * 1 * * *', // every 1 hour, crontab-generator.org
        start: false, 
        onTick: function (job, done) {
            logger.info('Running job "%s"', job.meta.name);
            if (_.isObject(job.meta._config) && 
                _.isObject(job.meta._config.memoryWatching) && job.meta._config.memoryWatching.enabled && 
                _.isObject(job.meta._config.backgroundProcesses) && !job.meta._config.backgroundProcesses.jobHeapSnapshots) {
                // Make snapshot only if jobHeapSnapshots disabled
                var file = '/tmp/' + job.meta._config.serviceName + '-' + process.pid + '-' + job.meta.name.replace(' ', '-') + '-' + job.meta._config.getHRTimestamp() + '.heapsnapshot';
                heapdump.writeSnapshot(file, function (err) {
                    if (err) {
                        logger.error(err);
                    }
                    else {
                        logger.warn('Job "%s": wrote heap snapshot to %s', job.meta.name, file);
                    }
                });
            } else {
                logger.warn('Job "%s": memory watching is disabled, skipping', job.meta.name, file);
            }
            return setImmediate(function (done) {
                return done(null, true);
            }, done);
        }
    }
});