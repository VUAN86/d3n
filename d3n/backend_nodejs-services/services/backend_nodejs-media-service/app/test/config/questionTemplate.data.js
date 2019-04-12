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
        status: 'inactive',
        isMultiStepQuestion: 0,
        answerTemplates: 'test',
        structureDefinition: 'structure QTN',
        help: '{"help": "new"}'
    },

    clean: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(QuestionTemplate, [self.QUESTION_TEMPLATE_NEW])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    load: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(QuestionTemplate, [self.QUESTION_TEMPLATE_NEW])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

};
