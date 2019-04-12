var _ = require('lodash');
var async = require('async');
var Config = require('./../../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var MediaData = require('./media.data.js');
var TenantData = require('./tenant.data.js');
var SessionData = require('./session.data.js');
var QuestionData = require('./question.data.js');
var QuestionTemplate = require('./questionTemplate.data.js');
var WorkorderData = require('./workorder.data.js');
var logger = require('nodejs-logger')();

module.exports = {
    load: function (done) {
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
                // clean RDBMS
                function (callback) { QuestionData.cleanManyToMany(callback); },
                function (callback) { MediaData.cleanManyToMany(callback); },
                function (callback) { QuestionData.cleanEntities(callback); },
                function (callback) { QuestionData.cleanClassifiers(callback); },
                function (callback) { WorkorderData.cleanClassifiers(callback); },
                function (callback) { QuestionTemplate.clean(callback); },
                function (callback) { MediaData.cleanEntities(callback); },
                function (callback) { WorkorderData.cleanEntities(callback); },
                function (callback) { TenantData.clean(callback) },

                // load RDBMS
                function (callback) { TenantData.load(callback) },
                function (callback) { WorkorderData.loadClassifiers(callback); },
                function (callback) { QuestionData.loadClassifiers(callback); },
                function (callback) { WorkorderData.loadEntities(callback); },
                function (callback) { MediaData.loadEntities(callback); },
                function (callback) { QuestionTemplate.load(callback); },
                function (callback) { QuestionData.loadEntities(callback); },
                function (callback) { QuestionData.loadManyToMany(callback); },
                function (callback) { MediaData.loadManyToMany(callback); }
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
        SessionData.loadAerospike(function (err) {
            if (err) {
                return done(err);
            }
            return done();
        });
    },

    cleanSession: function (done) {
        SessionData.cleanAerospike(function (err) {
            if (err) {
                return done(err);
            }
            return done();
        });
    }
};
