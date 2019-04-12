var _ = require('lodash');
var logger = require('nodejs-logger')();
var SchedulerCleanupService = require('./../services/schedulerCleanupService.js');
var CronMasterJob = require('cron-master').CronMasterJob;

module.exports = new CronMasterJob({
    timeThreshold: (30 * 60 * 1000), // Warning after 30 minute work
    
    meta: {
        enabled: true,
        name: 'Cleanup for scheduler service'
    },
    
    cronParams: {
        cronTime: '0 10 1 * * *', // 01:10:00 every night(UTC), crontab-generator.org
        start: false, 
        onTick: function (job, done) {
            logger.info('Running job "%s"', job.meta.name);
            return SchedulerCleanupService.cleanup(function (err) {
                if (err) {
                    return done(err);
                }
                return done(null, true);
            });
        }
    }
});