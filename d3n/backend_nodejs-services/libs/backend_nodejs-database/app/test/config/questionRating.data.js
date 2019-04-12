var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var QuestionRating = RdbmsService.Models.QuestionRating.QuestionRating;

module.exports = {
    QUESTION_RATING_1: {
        $id: DataIds.QUESTION_RATING_1_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 2,
        reason: 'ugly',
        $resourceId: '111-222-444'
    },
    QUESTION_RATING_2: {
        $id: DataIds.QUESTION_RATING_2_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 3,
        reason: 'so so',
        $resourceId: '111-222-444'
    },
    QUESTION_RATING_3: {
        $id: DataIds.QUESTION_RATING_3_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 2.5,
        reason: 'so so',
        $resourceId: '111-222-444'
    },
    QUESTION_RATING_4: {
        $id: DataIds.QUESTION_RATING_4_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 5,
        reason: 'excellent',
        $resourceId: '111-222-444'
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(QuestionRating, [this.QUESTION_RATING_1, this.QUESTION_RATING_2, this.QUESTION_RATING_3, this.QUESTION_RATING_4])
    }
};