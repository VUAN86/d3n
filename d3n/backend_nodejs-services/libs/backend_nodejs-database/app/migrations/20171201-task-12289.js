// QuestionOrTranslationEventLog: add issueId, add missing constraints; QuestionOrTranslationIssue: add missing constraints
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question_or_translation_event_log',
            'issueId',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        ).then(function () {
            var sqlRaw = 'ALTER TABLE `question_or_translation_event_log`' + 
                         ' ADD CONSTRAINT `question_or_translation_event_log_issueId_idx`' +
                         ' FOREIGN KEY (`issueId`) REFERENCES `question_or_translation_issue` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw);
        }).then(function () {
            var sqlRaw = 'ALTER TABLE `question_or_translation_event_log`' + 
                         ' ADD CONSTRAINT `question_or_translation_event_log_questionId_idx`' +
                         ' FOREIGN KEY (`questionId`) REFERENCES `question` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw);
        }).then(function () {
            var sqlRaw = 'ALTER TABLE `question_or_translation_issue`' + 
                         ' ADD CONSTRAINT `question_or_translation_issue_questionId_idx`' +
                         ' FOREIGN KEY (`questionId`) REFERENCES `question` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw);
        }).then(function () {
            var sqlRaw = 'ALTER TABLE `question_or_translation_issue`' + 
                         ' ADD CONSTRAINT `question_or_translation_issue_questionTranslationId_idx`' +
                         ' FOREIGN KEY (`questionTranslationId`) REFERENCES `question_translation` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw);
        }).then(function () {
            var sqlRaw = 'ALTER TABLE `question_or_translation_review`' + 
                         ' ADD CONSTRAINT `question_or_translation_review_questionId_idx`' +
                         ' FOREIGN KEY (`questionId`) REFERENCES `question` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw);
        }).then(function () {
            var sqlRaw = 'ALTER TABLE `question_or_translation_review`' + 
                         ' ADD CONSTRAINT `question_or_translation_review_questionTranslationId_idx`' +
                         ' FOREIGN KEY (`questionTranslationId`) REFERENCES `question_translation` (`id`)' +
                         ' ON DELETE NO ACTION ON UPDATE NO ACTION;';
            return queryInterface.sequelize.query(sqlRaw);
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return true;
    }
};