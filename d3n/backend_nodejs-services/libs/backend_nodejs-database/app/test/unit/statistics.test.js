var async = require('async');
var _ = require('lodash');
var should = require('should');
var Config = require('./../../config/config.js');
var DataIds = require('./../config/_id.data.js');
var TenantData = require('./../config/tenant.data.js');
var ConfigurationData = require('./../config/configuration.data.js');
var ProfileData = require('./../config/profile.data.js');
var QuestionData = require('./../config/question.data.js');
var QuestionTemplateData = require('./../config/questionTemplate.data.js');
var QuestionRatingData = require('./../config/questionRating.data.js');
var QuestionVotingData = require('./../config/questionVoting.data.js');
var WorkorderData = require('./../config/workorder.data.js');
var MediaData = require('./../config/media.data.js');
var BillingData = require('./../config/billing.data.js');
var GameData = require('./../config/game.data.js');
var ApplicationData = require('./../config/application.data.js');
var AdvertisementData = require('./../config/advertisement.data.js');
var WinningData = require('./../config/winning.data.js');
var VoucherData = require('./../config/voucher.data.js');
var TombolaData = require('./../config/tombola.data.js');
var PromocodeData = require('./../config/promocode.data.js');
var Config = require('./../../config/config.js');
var Database = require('./../../../index.js').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var StatisticsService = RdbmsService.StatisticsService;
var StatisticsEvents = require('./../../factories/statisticsEvents.js');
var TestService = Database.TestService;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var Game = RdbmsService.Models.Game.Game;
var GameHasPool = RdbmsService.Models.Game.GameHasPool;
var Application = RdbmsService.Models.Application.Application;
var ApplicationHasGame = RdbmsService.Models.Application.ApplicationHasGame;
var PoolHasQuestion = RdbmsService.Models.Question.PoolHasQuestion;
var Question = RdbmsService.Models.Question.Question;
var QuestionTranslation = RdbmsService.Models.Question.QuestionTranslation;
var logger = require('nodejs-logger')();

if (Config.statisticsEnabled) {
    describe('Statistics', function () {
        this.timeout(10000);
    
        beforeEach('Load Data', function (done) {

            StatisticsService.disable();
            _loadData(function (err, res) {
                if (err) {
                    logger.info("Error loading data", err);
                    return done(err);
                }

                logger.info("Data Load complete");
                StatisticsService.enable();
                done();
            });
        });

        require('./statistics/application_has_game.test.js');
        require('./statistics/game_has_pool.test.js');
        require('./statistics/pool_has_question.test.js');
        require('./statistics/question_translation.test.js');
        require('./statistics/question.test.js');
        require('./statistics/workorder_has_pool.test.js');
        require('./statistics/advertisement.test.js');
    });
} else {
    logger.info("Statistics not enabled");
}

function _loadData(done) {
    // load Data into empty test database (schema is recreated outside)
    RdbmsService.sync({
        logging: Config.rdbms.logging,
        force: Config.rdbms.force
    }, function (err) {
        if (err) {
            logger.error('Error synchronizing relational database.', err);
            process.exit(1);
        }
        logger.info('Relational database synchronized.');
        // Create integration test set
        var testSuite = TestService
            .newSuite()
            .forService('integration')
            .setupSets(1)
            .setupStep(100);
        // Load Classifiers
        MediaData.load(testSuite);
        TenantData.load(testSuite);
        ConfigurationData.load(testSuite);
        BillingData.loadClassifiers(testSuite);
        QuestionData.loadClassifiers(testSuite);
        ApplicationData.loadClassifiers(testSuite);
        GameData.loadClassifiers(testSuite);
        WinningData.loadClassifiers(testSuite);
        VoucherData.loadClassifiers(testSuite);
        AdvertisementData.loadClassifiers(testSuite);
        // Load Entities
        BillingData.loadEntities(testSuite);
        WorkorderData.loadEntities(testSuite);
        QuestionTemplateData.loadEntities(testSuite);
        QuestionData.loadEntities(testSuite);
        QuestionRatingData.loadEntities(testSuite);
        QuestionVotingData.loadEntities(testSuite);
        ApplicationData.loadEntities(testSuite);
        GameData.loadEntities(testSuite);
        ProfileData.loadEntities(testSuite);
        TombolaData.loadEntities(testSuite);
        PromocodeData.loadEntities(testSuite);
        // Load Many-to-Many
        WorkorderData.loadManyToMany(testSuite);
        QuestionData.loadManyToMany(testSuite);
        BillingData.loadManyToMany(testSuite);
        ApplicationData.loadManyToMany(testSuite);
        GameData.loadManyToMany(testSuite);
        ProfileData.loadManyToMany(testSuite);
        VoucherData.loadManyToMany(testSuite);
        AdvertisementData.loadManyToMany(testSuite);
        TenantData.loadManyToMany(testSuite);
        TombolaData.loadManyToMany(testSuite);

        // Process integration test set
        testSuite.process(function (err) {
            if (err) {
                logger.error('Relational database data error.');
                return done(err);
            }
            logger.info('Relational database data loaded.');
            // Pull sample set, test workorder
            testSuite.pull(function (err, set) {
                if (err) {
                    return done(err);
                }
                var workorderId = testSuite.id(WorkorderData.WORKORDER_1.$id, set);
                Workorder.findOne({
                    where: { id: workorderId }
                }).then(function (workorder) {
                    if (workorder.id === workorderId) {
                        logger.info('Integration test set ' + set + ' pulled and tested successfully.');
                        return done();
                    } else {
                        return done('Integration test set ' + set + ' failed.');
                    }
                })
            });
        });
    });
}