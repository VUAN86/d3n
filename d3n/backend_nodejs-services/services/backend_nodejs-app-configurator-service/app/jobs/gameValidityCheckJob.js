var _ = require('lodash');
var logger = require('nodejs-logger')();
var GameValidityCheckService = require('./../services/gameValidityCheckService.js');
var CronMasterJob = require('cron-master').CronMasterJob;

module.exports = new CronMasterJob({
    timeThreshold: (10 * 60 * 1000), // Warning after 10 minute work
    
    meta: {
        enabled: true,
        name: 'Game Validity Check'
    },
    
    cronParams: {
        cronTime: '00 00 12 * * *', // daily at 12:00, crontab-generator.org
        start: false, 
        onTick: function (job, done) {
            logger.info('Running job "%s"', job.meta.name);
            var self = this;
            return GameValidityCheckService.gameValidityCheck(function (err) {
                if (err) {
                    return done(err);
                }
                return done(null, true);
            });
        }
    }
});