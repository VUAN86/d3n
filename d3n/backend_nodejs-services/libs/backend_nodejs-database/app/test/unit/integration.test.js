var async = require('async');
var should = require('should');
var ConfigurationData = require('./../config/configuration.data.js');
var TenantData = require('./../config/tenant.data.js');
var ProfileData = require('./../config/profile.data.js');
var QuestionData = require('./../config/question.data.js');
var QuestionTemplateData = require('./../config/questionTemplate.data.js');
var QuestionRatingData = require('./../config/questionRating.data.js');
var QuestionVotingData = require('./../config/questionVoting.data.js');
var WorkorderData = require('./../config/workorder.data.js');
var MediaData = require('./../config/media.data.js');
var BillingData = require('./../config/billing.data.js');
var ApplicationData = require('./../config/application.data.js');
var GameData = require('./../config/game.data.js');
var AchievementData = require('./../config/achievement.data.js');
var AdvertisementData = require('./../config/advertisement.data.js');
var WinningData = require('./../config/winning.data.js');
var VoucherData = require('./../config/voucher.data.js');
var TombolaData = require('./../config/tombola.data.js');
var PromocodeData = require('./../config/promocode.data.js');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var Database = require('./../../../index.js').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var Workorder = RdbmsService.Models.Workorder.Workorder;
var TestService = Database.TestService;
var logger = require('nodejs-logger')();

describe('Integration', function () {
    this.timeout(60000);

    beforeEach(function (done) {
        _loadData(function (err, res) {
            if (err) {
                return done(err);
            }
            done();
        });
    });

    describe('LOAD', function () {
        it('OK', function (done) {
            done();
        });
    });
});

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
            .setupSets(2)
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
        AchievementData.loadClassifiers(testSuite);
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
        AchievementData.loadManyToMany(testSuite);
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
