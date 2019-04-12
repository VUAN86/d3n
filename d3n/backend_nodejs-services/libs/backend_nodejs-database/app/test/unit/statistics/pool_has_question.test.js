var should = require('should');
var Util = require('./../../util/statistics.js');
var DataIds = require('./../../config/_id.data.js');
var PoolHasQuestion = Util.RdbmsModels.Question.PoolHasQuestion;
var Pool = Util.RdbmsModels.Question.Pool;
var Question = Util.RdbmsModels.Question.Question;
var QuestionTranslation = Util.RdbmsModels.Question.QuestionTranslation;

describe('POOL_HAS_QUESTION', function () {
    it('create bulk', function (done) {
        PoolHasQuestion.bulkCreate([
            { poolId: DataIds.POOL_2_ID, questionId: DataIds.QUESTION_1_ID },
            { poolId: DataIds.POOL_2_ID, questionId: DataIds.QUESTION_2_ID }
        ]).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_2_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedQuestions.should.equal(2);
            pool.stat_questionsComplexityLevel1.should.equal(1);
            pool.stat_questionsComplexityLevel2.should.equal(1);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(2);
        }).then(function() { 
            return QuestionTranslation.findOne({ where: {
                id: DataIds.QUESTION_1_TRANSLATION_EN_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('create single instance', function (done) {
        PoolHasQuestion.create({
            poolId: DataIds.POOL_2_ID, questionId: DataIds.QUESTION_1_ID
        }).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_2_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedQuestions.should.equal(1);
            pool.stat_questionsComplexityLevel1.should.equal(1);
            pool.stat_questionsComplexityLevel2.should.equal(1);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(2);
        }).then(function() { 
            return QuestionTranslation.findOne({ where: {
                id: DataIds.QUESTION_1_TRANSLATION_EN_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete single instance', function (done) {
        PoolHasQuestion.findOne({ where: {
            poolId: DataIds.POOL_1_ID, questionId: DataIds.QUESTION_1_ID
        }}).then(function (poolQuestion) {
            return poolQuestion.destroy();
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_2_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedQuestions.should.equal(0);
            pool.stat_questionsComplexityLevel1.should.equal(0);
            pool.stat_questionsComplexityLevel2.should.equal(0);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(0);
        }).then(function() { 
            return QuestionTranslation.findOne({ where: {
                id: DataIds.QUESTION_1_TRANSLATION_EN_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(0);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete bulk', function (done) {
        PoolHasQuestion.destroy({ 
            where: { questionId: DataIds.QUESTION_1_ID  },
            individualHooks: true
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_2_ID
            }});
        }).then(function (pool) {
            pool.stat_assignedQuestions.should.equal(0);
            pool.stat_questionsComplexityLevel1.should.equal(0);
            pool.stat_questionsComplexityLevel2.should.equal(0);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_1_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(0);
        }).then(function() { 
            return QuestionTranslation.findOne({ where: {
                id: DataIds.QUESTION_1_TRANSLATION_EN_ID
            }});
        }).then(function (question) {
            question.stat_associatedPools.should.equal(0);
            done();
        }).catch(function(err) {
            done(err);
        });
    });
});
