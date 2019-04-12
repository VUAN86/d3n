var _ = require('lodash');
var async = require('async');
var Config = require('./../../config/config.js');
var Database = require('nodejs-database').getInstance(Config);
var TenantData = require('./tenant.data.js');
var MediaData = require('./media.data.js');
var BillingData = require('./billing.data.js');
var ProfileData = require('./profile.data.js');
var SessionData = require('./session.data.js');
var AdvertisementData = require('./advertisement.data.js');
var ApplicationData = require('./application.data.js');
var GameData = require('./game.data.js');
var WinningData = require('./winning.data.js');
var VoucherData = require('./voucher.data.js');
var QuestionTemplateData = require('./questionTemplate.data.js');
var QuestionData = require('./question.data.js');
var QuestionRatingData = require('./questionRating.data.js');
var QuestionVotingData = require('./questionVoting.data.js');
var WorkorderData = require('./workorder.data.js');
var WorkorderSendMessageData = require('./workorderEvent.data.js');
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
                function (callback) { MediaData.load(callback) },
                function (callback) { TenantData.load(callback) },
                function (callback) { BillingData.loadClassifiers(callback) },
                function (callback) { QuestionData.loadClassifiers(callback) },
                function (callback) { ApplicationData.loadClassifiers(callback) },
                function (callback) { GameData.loadClassifiers(callback) },
                function (callback) { WinningData.loadClassifiers(callback) },
                function (callback) { VoucherData.loadClassifiers(callback) },
                function (callback) { AdvertisementData.loadClassifiers(callback) },
                // Load Entities
                function (callback) { BillingData.loadEntities(callback) },
                function (callback) { WorkorderData.loadEntities(callback) },
                function (callback) { QuestionTemplateData.loadEntities(callback) },
                function (callback) { QuestionData.loadEntities(callback) },
                function (callback) { QuestionRatingData.loadEntities(callback) },
                function (callback) { QuestionVotingData.loadEntities(callback) },
                function (callback) { ApplicationData.loadEntities(callback) },
                function (callback) { GameData.loadEntities(callback) },
                function (callback) { ProfileData.loadEntities(callback) },
                // Load Many-to-Many
                function (callback) { WorkorderData.loadManyToMany(callback) },
                function (callback) { QuestionData.loadManyToMany(callback) },
                function (callback) { BillingData.loadManyToMany(callback) },
                function (callback) { ApplicationData.loadManyToMany(callback) },
                function (callback) { GameData.loadManyToMany(callback) },
                function (callback) { ProfileData.loadManyToMany(callback) },
                function (callback) { VoucherData.loadManyToMany(callback) },
                function (callback) { AdvertisementData.loadManyToMany(callback) },
                function (callback) { WorkorderSendMessageData.load(callback); },
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
