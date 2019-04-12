var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var QuestionVoting = RdbmsService.Models.QuestionVoting.QuestionVoting;

module.exports = {
    QUESTION_VOTING_1: {
        id: DataIds.QUESTION_VOTING_1_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 2,
        reason: 'ugly',
        resourceId: DataIds.MEDIA_1_ID,
        createDate: DateUtils.isoNow()
    },
    QUESTION_VOTING_2: {
        id: DataIds.QUESTION_VOTING_2_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 2,
        reason: 'ugly',
        resourceId: DataIds.MEDIA_1_ID,
        createDate: DateUtils.isoNow()
    },
    QUESTION_VOTING_3: {
        id: DataIds.QUESTION_VOTING_3_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 2.5,
        reason: 'so so',
        resourceId: DataIds.MEDIA_2_ID,
        createDate: DateUtils.isoNow()
    },
    QUESTION_VOTING_DELETE: {
        id: DataIds.QUESTION_VOTING_4_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 5,
        reason: 'excellent delete',
        resourceId: DataIds.MEDIA_2_ID,
        createDate: DateUtils.isoNow()
    },
    QUESTION_VOTING_NEW: {
        questionId: DataIds.QUESTION_1_ID,
        rating: 5,
        reason: 'excellent',
        resourceId: DataIds.MEDIA_2_ID
    },

    cleanEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(QuestionVoting, [self.QUESTION_VOTING_1, self.QUESTION_VOTING_2, self.QUESTION_VOTING_3, self.QUESTION_VOTING_DELETE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(QuestionVoting, [self.QUESTION_VOTING_1, self.QUESTION_VOTING_2, self.QUESTION_VOTING_3, self.QUESTION_VOTING_DELETE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};