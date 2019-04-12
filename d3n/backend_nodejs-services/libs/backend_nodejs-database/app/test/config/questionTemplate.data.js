var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('./../../../index.js').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var QuestionTemplate = RdbmsService.Models.QuestionTemplate.QuestionTemplate;

module.exports = {
    QUESTION_TEMPLATE_NEW: {
        $id: DataIds.QUESTION_TEMPLATE_NEW_ID,
        name: 'QTN',
        $mediaId: DataIds.MEDIA_1_ID,
        status: 'inactive',
        isMultiStepQuestion: 0,
        hasResolution: 0,
        answerTemplates: 'test',
        structureDefinition: 'structure QTN',
        help: '{"help": "new"}'
    },
    QUESTION_TEMPLATE_APPROVED: {
        $id: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
        name: 'QTA',
        $mediaId: DataIds.MEDIA_2_ID,
        status: 'active',
        isMultiStepQuestion: 1,
        hasResolution: 0,
        answerTemplates: 'test',
        structureDefinition: 'structure QTA',
        help: '{"help": "approved"}'
    },
    QUESTION_TEMPLATE_TEST: {
        $id: DataIds.QUESTION_TEMPLATE_TEST_ID,
        name: 'QTT',
        $mediaId: DataIds.MEDIA_2_ID,
        status: 'inactive',
        isMultiStepQuestion: 1,
        hasResolution: 0,
        answerTemplates: 'test',
        structureDefinition: 'structure QTT',
        help: '{"help": "test"}'
    },

    loadEntities: function (testSet) {
        return testSet
            .createSeries(QuestionTemplate, [this.QUESTION_TEMPLATE_NEW, this.QUESTION_TEMPLATE_APPROVED])
    },

};