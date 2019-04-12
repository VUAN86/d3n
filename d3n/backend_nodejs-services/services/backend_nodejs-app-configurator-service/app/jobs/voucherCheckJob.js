var _ = require('lodash');
var logger = require('nodejs-logger')();
var VoucherCheckService = require('./../services/voucherCheckService.js');
var CronMasterJob = require('cron-master').CronMasterJob;

module.exports = new CronMasterJob({
    timeThreshold: (10 * 60 * 1000), // Warning after 10 minute work
    
    meta: {
        enabled: true,
        name: 'Voucher Check'
    },
    
    cronParams: {
        cronTime: '00 00 12 * * *', // daily at 12:00, crontab-generator.org
        start: false, 
        onTick: function (job, done) {
            logger.info('Running job "%s"', job.meta.name);
            var self = this;
            return VoucherCheckService.voucherCheck(function (err) {
                if (err) {
                    return done(err);
                }
                return done(null, true);
            });
        }
    }
});