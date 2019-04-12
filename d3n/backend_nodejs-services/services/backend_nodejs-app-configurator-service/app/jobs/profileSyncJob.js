var _ = require('lodash');
var logger = require('nodejs-logger')();
var ProfileSyncService = require('./../services/profileSyncService.js');
var CronMasterJob = require('cron-master').CronMasterJob;

module.exports = new CronMasterJob({
    timeThreshold: (10 * 60 * 1000), // Warning after 10 minute work
    
    meta: {
        enabled: true,
        name: 'Profile Synchronization'
    },
    
    cronParams: {
        cronTime: '*/5 * * * * *', // every 5 seconds, crontab-generator.org
        start: false, 
        onTick: function (job, done) {
            logger.info('Running job "%s"', job.meta.name);
            var self = this;
            return ProfileSyncService.profileSync(null, null, function (err) {
                if (err) {
                    return done(err);
                }
                return done(null, true);
            });
        }
    }
});