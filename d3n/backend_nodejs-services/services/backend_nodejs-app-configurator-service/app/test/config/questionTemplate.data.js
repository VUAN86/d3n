var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var QuestionTemplate = RdbmsService.Models.QuestionTemplate.QuestionTemplate;

module.exports = {
    QUESTION_TEMPLATE_NEW: {
        id: DataIds.QUESTION_TEMPLATE_NEW_ID,
        name: 'QTN',
        mediaId: DataIds.MEDIA_1_ID,
        status: 'active',
        answerTemplates: 'test',
        structureDefinition: 'structure QTN',
        help: '{"help": "new"}'
    },
    QUESTION_TEMPLATE_APPROVED: {
        id: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
        name: 'QTA',
        mediaId: DataIds.MEDIA_2_ID,
        status: 'active',
        answerTemplates: 'test',
        structureDefinition: 'structure QTA',
        help: '{"help": "approved"}'
    },
    QUESTION_TEMPLATE_NO_DEPENDENCIES: {
        id: DataIds.QUESTION_TEMPLATE_NO_DEPENDENCIES_ID,
        name: 'QTND',
        mediaId: DataIds.MEDIA_2_ID,
        status: 'active',
        answerTemplates: 'test',
        structureDefinition: 'structure QTND',
        help: '{"help": "no dependecies"}'
    },
    QUESTION_TEMPLATE_TEST: {
        id: DataIds.QUESTION_TEMPLATE_TEST_ID,
        name: 'QTT',
        mediaId: DataIds.MEDIA_2_ID,
        status: 'active',
        answerTemplates: 'test',
        structureDefinition: 'structure QTT',
        help: '{"help": "test"}'
    },

    cleanEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(QuestionTemplate, [self.QUESTION_TEMPLATE_NEW, self.QUESTION_TEMPLATE_APPROVED, self.QUESTION_TEMPLATE_NO_DEPENDENCIES, self.QUESTION_TEMPLATE_TEST])
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
            .createSeries(QuestionTemplate, [self.QUESTION_TEMPLATE_NEW, self.QUESTION_TEMPLATE_APPROVED, self.QUESTION_TEMPLATE_NO_DEPENDENCIES])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

};