var TestConfig = require('./../config/config.js');
var Config = require('./../../config/config.js');
var Database = require('./../../../index.js').getInstance(Config);

module.exports = {
    RdbmsModels: Database.RdbmsService.Models,
    waitForStatisticsUpdated: function() {
        // wait until statistics get updated
        return new Promise(function (resolve, reject) {
            setTimeout(function() {
                return resolve();
            }, TestConfig.timeToWaitForUpdatingStatistics);
        });
    }
}