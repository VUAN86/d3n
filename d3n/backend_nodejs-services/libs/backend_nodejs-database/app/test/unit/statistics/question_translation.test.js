var should = require('should');
var Util = require('./../../util/statistics.js');
var DataIds = require('./../../config/_id.data.js');
var Pool = Util.RdbmsModels.Question.Pool;
var QuestionTranslation = Util.RdbmsModels.Question.QuestionTranslation;
var Workorder = Util.RdbmsModels.Workorder.Workorder;

describe('QUESTION_TRANSLATION', function () {
    it('create bulk', function (done) {
        QuestionTranslation.bulkCreate([
            {
                id: DataIds.QUESTION_1_TRANSLATION_EN2_ID,
                name: 'Question 1 Translation English',
                creatorResourceId: DataIds.LOCAL_USER_ID,
                reateDate: new Date(),
                approverResourceId: null,
                approveDate: null,
                blockerResourceId: null,
                blockDate: null,
                languageId: DataIds.LANGUAGE_EN_ID,
                questionId: DataIds.QUESTION_1_ID,
                workorderId: DataIds.WORKORDER_1_ID,
                resolutionText: 'test 1EN',
                explanation: 'test1EN',
                hint: 'test1EN',
                content: null
            },
            {
                id: DataIds.QUESTION_1_TRANSLATION_DE2_ID,
                name: 'Question 1 Translation Deutsch',
                creatorResourceId: DataIds.LOCAL_USER_ID,
                createDate: new Date(),
                approverResourceId: null,
                approveDate: null,
                blockerResourceId: null,
                blockDate: null,
                languageId: DataIds.LANGUAGE_DE_ID,
                questionId: DataIds.QUESTION_1_ID,
                workorderId: DataIds.WORKORDER_1_ID,
                resolutionText: 'test 1DE',
                explanation: 'test1DE',
                hint: 'test1DE',
                content: null
            }
        ]).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(2);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsInTranslation.should.equal(1);
            workorder.stat_questionTranslationsCompleted.should.equal(0);
            workorder.stat_questionTranslationsInProgress.should.equal(4);
            workorder.stat_translationsProgressPercentage.should.equal(0);
            workorder.stat_translationsPublished.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('create single instance', function (done) {
        QuestionTranslation.create({
            id: DataIds.QUESTION_1_TRANSLATION_EN2_ID,
            name: 'Question 1 Translation English',
            creatorResourceId: DataIds.LOCAL_USER_ID,
            reateDate: new Date(),
            approverResourceId: null,
            approveDate: null,
            blockerResourceId: null,
            blockDate: null,
            languageId: DataIds.LANGUAGE_EN_ID,
            questionId: DataIds.QUESTION_1_ID,
            workorderId: DataIds.WORKORDER_1_ID,
            resolutionText: 'test 1EN',
            explanation: 'test1EN',
            hint: 'test1EN',
            content: null
        }).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(2);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsInTranslation.should.equal(1);
            workorder.stat_questionTranslationsCompleted.should.equal(0);
            workorder.stat_questionTranslationsInProgress.should.equal(3);
            workorder.stat_translationsProgressPercentage.should.equal(0);
            workorder.stat_translationsPublished.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete single instance', function (done) {
        QuestionTranslation.create({
            id: DataIds.QUESTION_1_TRANSLATION_EN2_ID,
            name: 'Question 1 Translation English',
            creatorResourceId: DataIds.LOCAL_USER_ID,
            reateDate: new Date(),
            approverResourceId: null,
            approveDate: null,
            blockerResourceId: null,
            blockDate: null,
            languageId: DataIds.LANGUAGE_EN_ID,
            questionId: DataIds.QUESTION_1_ID,
            workorderId: DataIds.WORKORDER_1_ID,
            resolutionText: 'test 1EN',
            explanation: 'test1EN',
            hint: 'test1EN',
            content: null
        }).then(function() { 
            return QuestionTranslation.findOne({ where: {
                id: DataIds.QUESTION_1_TRANSLATION_EN2_ID
            }});
        }).then(function (poolQuestion) {
            return poolQuestion.destroy();
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_2_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(0);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsInTranslation.should.equal(1);
            workorder.stat_questionTranslationsCompleted.should.equal(0);
            workorder.stat_questionTranslationsInProgress.should.equal(2);
            workorder.stat_translationsProgressPercentage.should.equal(0);
            workorder.stat_translationsPublished.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete bulk', function (done) {
        QuestionTranslation.bulkCreate([
            {
                id: DataIds.QUESTION_1_TRANSLATION_EN2_ID,
                name: 'Question 1 Translation English',
                creatorResourceId: DataIds.LOCAL_USER_ID,
                reateDate: new Date(),
                approverResourceId: null,
                approveDate: null,
                blockerResourceId: null,
                blockDate: null,
                languageId: DataIds.LANGUAGE_EN_ID,
                questionId: DataIds.QUESTION_1_ID,
                workorderId: DataIds.WORKORDER_1_ID,
                resolutionText: 'test 1EN',
                explanation: 'test1EN',
                hint: 'test1EN',
                content: null
            },
            {
                id: DataIds.QUESTION_1_TRANSLATION_DE2_ID,
                name: 'Question 1 Translation Deutsch',
                creatorResourceId: DataIds.LOCAL_USER_ID,
                createDate: new Date(),
                approverResourceId: null,
                approveDate: null,
                blockerResourceId: null,
                blockDate: null,
                languageId: DataIds.LANGUAGE_DE_ID,
                questionId: DataIds.QUESTION_1_ID,
                workorderId: DataIds.WORKORDER_1_ID,
                resolutionText: 'test 1DE',
                explanation: 'test1DE',
                hint: 'test1DE',
                content: null
            }
        ]).then(function() { 
            return QuestionTranslation.destroy({ 
                where: { id: DataIds.QUESTION_1_TRANSLATION_EN2_ID  },
                individualHooks: true
            });
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_2_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(0);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsInTranslation.should.equal(1);
            workorder.stat_questionTranslationsCompleted.should.equal(0);
            workorder.stat_questionTranslationsInProgress.should.equal(3);
            workorder.stat_translationsProgressPercentage.should.equal(0);
            workorder.stat_translationsPublished.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });
});
