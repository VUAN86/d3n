var _ = require('lodash');
var logger = require('nodejs-logger')();
var heapdump = require('heapdump');
var CronMasterJob = require('cron-master').CronMasterJob;

module.exports = new CronMasterJob({
    timeThreshold: (10 * 60 * 1000), // Warning after 10 minute work
    
    meta: {
        enabled: process.env.EXPOSE_GC === 'true',
        name: 'Expose Garbage Collecting Manually'
    },
    
    cronParams: {
        cronTime: '* */2 * * * *', // every 2 minutes, crontab-generator.org
        start: false, 
        onTick: function (job, done) {
            logger.info('Running job "%s"', job.meta.name);
            global.gc();
            return setImmediate(function (done) {
                return done(null, true);
            }, done);
        }
    }
});