var Config = require('./../../config/config.js');
var DataIds = require('./_id.data.js');
var Database = require('nodejs-database').getInstance(Config);
var RdbmsService = Database.RdbmsService;
var QuestionRating = RdbmsService.Models.QuestionRating.QuestionRating;

module.exports = {
    QUESTION_RATING_1: {
        id: DataIds.QUESTION_RATING_1_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 2,
        reason: 'ugly',
        resourceId: DataIds.MEDIA_1_ID
    },
    QUESTION_RATING_2: {
        id: DataIds.QUESTION_RATING_2_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 2,
        reason: 'ugly',
        resourceId: DataIds.MEDIA_1_ID
    },
    QUESTION_RATING_3: {
        id: DataIds.QUESTION_RATING_3_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 2.5,
        reason: 'so so',
        resourceId: DataIds.MEDIA_1_ID
    },
    QUESTION_RATING_DELETE: {
        id: DataIds.QUESTION_RATING_4_ID,
        questionId: DataIds.QUESTION_1_ID,
        rating: 5,
        reason: 'excellent delete',
        resourceId: DataIds.MEDIA_1_ID
    },
    QUESTION_RATING_NEW: {
        questionId: DataIds.QUESTION_1_ID,
        rating: 5,
        reason: 'excellent',
        resourceId: DataIds.MEDIA_1_ID
    },
    QUESTION_RATING_AGG: {
        questionId: DataIds.QUESTION_2_ID,
        rating: 1,
        reason: '',
        resourceId: DataIds.MEDIA_1_ID
    },

    cleanEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(QuestionRating, [self.QUESTION_RATING_1, self.QUESTION_RATING_2, self.QUESTION_RATING_3, self.QUESTION_RATING_DELETE, self.QUESTION_RATING_NEW, self.QUESTION_RATING_AGG])
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
            .createSeries(QuestionRating, [self.QUESTION_RATING_1, self.QUESTION_RATING_2, self.QUESTION_RATING_3, self.QUESTION_RATING_DELETE])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};