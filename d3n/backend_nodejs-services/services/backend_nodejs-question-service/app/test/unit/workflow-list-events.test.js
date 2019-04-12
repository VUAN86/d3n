var _ = require('lodash');
var async = require('async');
var should = require('should');
var assert = require('chai').assert;
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var ProfileData = require('./../config/profile.data.js');
var IdsData = require('./../config/_id.data.js');
var WorkflowCommon = require('./../../workflows/WorkflowCommon.js');
var QuestionOrTranslationReview = Database.RdbmsService.Models.Question.QuestionOrTranslationReview;
var QuestionOrTranslationEventLog = Database.RdbmsService.Models.Question.QuestionOrTranslationEventLog;

describe('WORKFLOW LIST EVENTS', function () {
    global.wsHelper.series().forEach(function (serie) {
        
        
        describe('[' + serie + '] ' + 'listEvents', function () {
            it('listEvents by tanslation', function (done) {
                var questionTranslationId = 1;
                var review = {
                    reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_QUESTION,
                    questionTranslationId: questionTranslationId,
                    resourceId: '11-22',
                    rating: 3,
                    difficulty: 5,
                    isAccepted: 1
                };

                var log = {
                    questionTranslationId: questionTranslationId,
                    triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                    resourceId: IdsData.LOCAL_USER_ID,
                    createDate: DateUtils.isoNow(),
                    oldStatus: 'a',
                    newStatus: 'b'
                };
                async.series([
                    function (next) {
                        emptyReviewAndEventLog(next);
                    },

                    // add log without review
                    function (next) {
                        WorkflowCommon.addEventLog(log, next);
                    },
                    
                    // add log with review
                    function (next) {
                        log.questionTranslationId = questionTranslationId+1;
                        review.questionTranslationId = questionTranslationId+1;
                        WorkflowCommon.addReviewAndEventLog(review, log, next);
                    },
                    
                    function (next) {
                        var reqContent = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { questionTranslationId: 1 },
                            orderBy: [{ field: 'id', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/eventList', ProfileData.CLIENT_INFO_1, reqContent, function (responseContent) {
                            console.log('>>>>responseContent:', require('util').inspect(responseContent, { showHidden: true, depth: null }));
                            assert.strictEqual(responseContent.items.length, 1);
                            //assert.deepEqual(responseContent.items[0].review, {});
                            assert.strictEqual(responseContent.items[0].oldStatus, log.oldStatus);
                            assert.strictEqual(responseContent.items[0].newStatus, log.newStatus);
                            //assert.strictEqual(responseContent.items[0].createDate, log.createDate);
                        }, next);
                    }

                ], done);
            });
            
            it('listEvents by question', function (done) {
                return done();
                var questionTranslationId = 1;
                var review = {
                    reviewFor: QuestionOrTranslationReview.constants().REVIEW_FOR_QUESTION,
                    questionTranslationId: questionTranslationId,
                    resourceId: '1111-222',
                    rating: 3,
                    difficulty: 5,
                    isAccepted: 1
                };

                var log = {
                    questionTranslationId: questionTranslationId,
                    triggeredBy: QuestionOrTranslationEventLog.constants().TRIGGERED_BY_USER,
                    createDate: DateUtils.isoNow()
                };
                async.series([
                    function (next) {
                        emptyReviewAndEventLog(next);
                    },
                    
                    // add log
                    function (next) {
                        WorkflowCommon.addEventLog(log, next);
                    },
                    
                    // add review and log
                    function (next) {
                        log.questionTranslationId = 2;
                        WorkflowCommon.addEventLog(log, next);
                    },
                    
                    // list events for first translation only
                    function (next) {
                        var reqContent = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { questionId: 1},
                            orderBy: [{ field: 'id', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/eventList', ProfileData.CLIENT_INFO_1, reqContent, function (responseContent) {
                            console.log('>>>>responseContent:', responseContent);
                            //questionId = responseContent.question.id;
                        }, next);
                    },
                    
                    // list events for all translations
                    function (next) {
                        var reqContent = {
                            limit: Config.rdbms.limit,
                            offset: 0,
                            searchBy: { questionId: 1, questionTranslationId: -1},
                            orderBy: [{ field: 'id', direction: 'asc' }]
                        };
                        global.wsHelper.apiSecureSucc(serie, 'questionLifetime/eventList', ProfileData.CLIENT_INFO_1, reqContent, function (responseContent) {
                            console.log('>>>>responseContent:', responseContent);
                            //questionId = responseContent.question.id;
                        }, next);
                    }
                    

                ], done);
            });
            
            
        });
    });
});


function emptyReviewAndEventLog(cb) {
    async.series([
        function (next) {
            QuestionOrTranslationEventLog.destroy({where: {id: {$gt: 0}}}).then(function () {
                QuestionOrTranslationEventLog.count().then(function(c) {
                    try {
                        assert.strictEqual(c, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });                       
            }).catch(function (err) {
                return next(err);
            });
        },
        function (next) {
            QuestionOrTranslationReview.destroy({where: {id: {$gt: 0}}}).then(function () {
                QuestionOrTranslationReview.count().then(function(c) {
                    try {
                        assert.strictEqual(c, 0);
                        return next();
                    } catch (e) {
                        return next(e);
                    }
                });                       
            }).catch(function (err) {
                return next(err);
            });
        }
        
    ], cb);
}