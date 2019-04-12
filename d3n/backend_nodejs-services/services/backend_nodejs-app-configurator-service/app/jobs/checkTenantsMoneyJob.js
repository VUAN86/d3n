var _ = require('lodash');
var logger = require('nodejs-logger')();
var CheckTenantsMoneyService = require('./../services/checkTenantsMoneyService.js');
var CronMasterJob = require('cron-master').CronMasterJob;

module.exports = new CronMasterJob({
    timeThreshold: (30 * 60 * 1000), // Warning after 30 minute work
    
    meta: {
        enabled: true,
        name: 'Check Tenants Money'
    },
    
    cronParams: {
        cronTime: '0 10 0 * * *', // 00:10:00 every night(UTC), crontab-generator.org
        start: false, 
        onTick: function (job, done) {
            logger.info('Running job "%s"', job.meta.name);
            if( !(process.env.ENABLE_CHECK_TENANT_MONEY === 'true') ) {
                return setImmediate(done, null, true);
            }
            return CheckTenantsMoneyService.checkTenantsMoney(function (err) {
                if (err) {
                    return done(err);
                }
                return done(null, true);
            });
        }
    }
});