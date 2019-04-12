var _ = require('lodash');
var async = require('async');
var Config = require('./../../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var TenantData = require('./tenant.data.js');
var ApplicationData = require('./application.data.js');
var LiveMessageData = require('./liveMessage.data.js');
var logger = require('nodejs-logger')();

module.exports = {
    loadData: function (done) {
        Database.RdbmsService.sync({
            logging: Config.rdbms.logging,
            force: Config.rdbms.force
        }, function (err) {
            if (err) {
                if (process.env.DATABASE_LOGGER === 'true') {
                    logger.error('Error synchronizing relational database.');
                }
                process.exit(1);
            }
            if (process.env.DATABASE_LOGGER === 'true') {
                logger.info('Relational database synchronized.');
            }
            async.series([
                // Load Classifiers
                function (callback) { ApplicationData.loadClassifiers(callback) },
                // Load Entities
                function (callback) { TenantData.load(callback) },
                function (callback) { ApplicationData.loadEntities(callback) },
                function (callback) { LiveMessageData.loadEntities(callback) },
                // Load Many-to-Many
                //function (callback) { ApplicationData.loadManyToMany(callback) },
            ], function (err) {
                if (err) {
                    if (process.env.DATABASE_LOGGER === 'true') {
                        logger.error('Relational database data error.');
                    }
                    return done(err);
                }
                if (process.env.DATABASE_LOGGER === 'true') {
                    logger.info('Relational database data loaded.');
                }
                return done();
            });
        });
    },

    loadSession: function (done) {
        done();
    },

    cleanSession: function (done) {
        done();
    }
};
