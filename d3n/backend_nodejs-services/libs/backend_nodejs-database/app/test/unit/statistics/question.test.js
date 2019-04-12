var should = require('should');
var Util = require('./../../util/statistics.js');
var DataIds = require('./../../config/_id.data.js');
var Pool = Util.RdbmsModels.Question.Pool;
var Question = Util.RdbmsModels.Question.Question;
var Workorder = Util.RdbmsModels.Workorder.Workorder;

describe('QUESTION', function () {
    it('create bulk', function (done) {
        Question.bulkCreate([
            {
                id: DataIds.QUESTION_3_ID,
                questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
                creatorResourceId: DataIds.LOCAL_USER_ID,
                createDate: new Date(),
                isMultiStepQuestion: 0,
                complexity: 1,
                workorderId: DataIds.WORKORDER_1_ID,
                accessibleDate: new Date(),
                expirationDate: null,
                renewDate: null,
                isDeleted: 0,
                status: 'draft',
                resolutionImageId: null,
                exampleQuestionId: null,
                primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
                isInternational: 0,
                rating: 0,
                deploymentStatus: 'unpublished',
                deploymentDate: null
            }
        ]).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(0);
            pool.stat_questionsComplexityLevel1.should.equal(0);
            pool.stat_questionsComplexityLevel2.should.equal(0);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsRequested.should.equal(0);
            workorder.stat_questionsCompleted.should.equal(0);
            workorder.stat_questionsInProgress.should.equal(2);
            workorder.stat_progressPercentage.should.equal(0);
            workorder.stat_questionsPublished.should.equal(0);
            workorder.stat_questionsComplexityLevel1.should.equal(2);
            workorder.stat_questionsComplexityLevel2.should.equal(0);
            workorder.stat_questionsComplexityLevel3.should.equal(0);
            workorder.stat_questionsComplexityLevel4.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('create single instance', function (done) {
        Question.create({
            id: DataIds.QUESTION_3_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
            creatorResourceId: DataIds.LOCAL_USER_ID,
            createDate: new Date(),
            isMultiStepQuestion: 0,
            complexity: 1,
            workorderId: DataIds.WORKORDER_1_ID,
            accessibleDate: new Date(),
            expirationDate: null,
            renewDate: null,
            isDeleted: 0,
            status: 'draft',
            resolutionImageId: null,
            exampleQuestionId: null,
            primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
            isInternational: 0,
            rating: 0,
            deploymentStatus: 'unpublished',
            deploymentDate: null
        }).then(function() { 
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(0);
            pool.stat_questionsComplexityLevel1.should.equal(0);
            pool.stat_questionsComplexityLevel2.should.equal(0);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsRequested.should.equal(0);
            workorder.stat_questionsCompleted.should.equal(0);
            workorder.stat_questionsInProgress.should.equal(2);
            workorder.stat_progressPercentage.should.equal(0);
            workorder.stat_questionsPublished.should.equal(0);
            workorder.stat_questionsComplexityLevel1.should.equal(2);
            workorder.stat_questionsComplexityLevel2.should.equal(0);
            workorder.stat_questionsComplexityLevel3.should.equal(0);
            workorder.stat_questionsComplexityLevel4.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete single instance', function (done) {
        Question.create({
            id: DataIds.QUESTION_3_ID,
            questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
            creatorResourceId: DataIds.LOCAL_USER_ID,
            createDate: new Date(),
            isMultiStepQuestion: 0,
            complexity: 1,
            workorderId: DataIds.WORKORDER_1_ID,
            accessibleDate: new Date(),
            expirationDate: null,
            renewDate: null,
            isDeleted: 0,
            status: 'draft',
            resolutionImageId: null,
            exampleQuestionId: null,
            primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
            isInternational: 0,
            rating: 0,
            deploymentStatus: 'unpublished',
            deploymentDate: null
        }).then(function() { 
            return Question.findOne({ where: {
                id: DataIds.QUESTION_3_ID
            }});
        }).then(function (question) {
            return question.destroy();
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(0);
            pool.stat_questionsComplexityLevel1.should.equal(0);
            pool.stat_questionsComplexityLevel2.should.equal(0);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsRequested.should.equal(0);
            workorder.stat_questionsCompleted.should.equal(0);
            workorder.stat_questionsInProgress.should.equal(1);
            workorder.stat_progressPercentage.should.equal(0);
            workorder.stat_questionsPublished.should.equal(0);
            workorder.stat_questionsComplexityLevel1.should.equal(1);
            workorder.stat_questionsComplexityLevel2.should.equal(0);
            workorder.stat_questionsComplexityLevel3.should.equal(0);
            workorder.stat_questionsComplexityLevel4.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });

    it('delete bulk', function (done) {
        Question.bulkCreate([
            {
                id: DataIds.QUESTION_3_ID,
                questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
                creatorResourceId: DataIds.LOCAL_USER_ID,
                createDate: new Date(),
                isMultiStepQuestion: 0,
                complexity: 1,
                workorderId: DataIds.WORKORDER_1_ID,
                accessibleDate: new Date(),
                expirationDate: null,
                renewDate: null,
                isDeleted: 0,
                status: 'draft',
                resolutionImageId: null,
                exampleQuestionId: null,
                primaryRegionalSettingId: DataIds.REGIONAL_SETTING_1_ID,
                isInternational: 0,
                rating: 0,
                deploymentStatus: 'unpublished',
                deploymentDate: null
            }
        ]).then(function() { 
            return Question.destroy({ 
                where: { id: DataIds.QUESTION_3_ID  },
                individualHooks: true
            });
        }).then(function () {
            return Util.waitForStatisticsUpdated();
        }).then(function() { 
            return Pool.findOne({ where: {
                id: DataIds.POOL_1_ID
            }});
        }).then(function (pool) {
            pool.stat_numberOfLanguages.should.equal(0);
            pool.stat_questionsComplexityLevel1.should.equal(0);
            pool.stat_questionsComplexityLevel2.should.equal(0);
            pool.stat_questionsComplexityLevel3.should.equal(0);
            pool.stat_questionsComplexityLevel4.should.equal(0);
        }).then(function() { 
            return Workorder.findOne({ where: {
                id: DataIds.WORKORDER_1_ID
            }});
        }).then(function (workorder) {
            workorder.stat_questionsRequested.should.equal(0);
            workorder.stat_questionsCompleted.should.equal(0);
            workorder.stat_questionsInProgress.should.equal(1);
            workorder.stat_progressPercentage.should.equal(0);
            workorder.stat_questionsPublished.should.equal(0);
            workorder.stat_questionsComplexityLevel1.should.equal(1);
            workorder.stat_questionsComplexityLevel2.should.equal(0);
            workorder.stat_questionsComplexityLevel3.should.equal(0);
            workorder.stat_questionsComplexityLevel4.should.equal(0);
            workorder.stat_numberOfLanguages.should.equal(2);
            done();
        }).catch(function(err) {
            done(err);
        });
    });
});
