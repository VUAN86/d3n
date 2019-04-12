var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var QuestionVoting = RdbmsService.Models.QuestionVoting.QuestionVoting;

module.exports = {
    QUESTION_VOTING_1: {
        $id: DataIds.QUESTION_VOTING_1_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 2,
        reason: 'ugly',
        $resourceId: '111-222-444',
        createDate: _.now()
    },
    QUESTION_VOTING_2: {
        $id: DataIds.QUESTION_VOTING_2_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 3,
        reason: 'so so',
        $resourceId: '111-222-444',
        createDate: _.now()
    },
    QUESTION_VOTING_3: {
        $id: DataIds.QUESTION_VOTING_3_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 2.5,
        reason: 'so so',
        $resourceId: '111-222-444',
        createDate: _.now()
    },
    QUESTION_VOTING_4: {
        $id: DataIds.QUESTION_VOTING_4_ID,
        $questionId: DataIds.QUESTION_1_ID,
        rating: 5,
        reason: 'excellent',
        $resourceId: '111-222-444',
        createDate: _.now()
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(QuestionVoting, [this.QUESTION_VOTING_1, this.QUESTION_VOTING_2, this.QUESTION_VOTING_3, this.QUESTION_VOTING_4])
    }
};