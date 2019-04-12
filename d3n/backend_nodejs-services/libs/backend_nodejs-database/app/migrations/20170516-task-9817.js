var logger = require('nodejs-logger')();
module.exports = {
    up: function (queryInterface, Sequelize) {
        logger.warn('Adding version to question_or_translation_review');
        return queryInterface.addColumn(
            'question_or_translation_review',
            'version',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true,
                defaultValue: 0
            }
        ).then(function () {
            logger.warn('Set version to existing reviews for questions');
            var sqlRaw = 'UPDATE question_or_translation_review qtr INNER JOIN question q ON q.id = qtr.questionId SET qtr.version = IF(q.publishedVersion > 0, q.publishedVersion, q.version);';
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            logger.warn('Set version to existing reviews for question translations');
            var sqlRaw = 'UPDATE question_or_translation_review qtr INNER JOIN question_translation qt ON qt.id = qtr.questionTranslationId SET qtr.version = IF(qt.publishedVersion > 0, qt.publishedVersion, qt.version);';
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            logger.warn('Set version 1  to existing reviews where version if it is null');
            var sqlRaw = 'UPDATE question_or_translation_review SET version = 1 WHERE version IS NULL;';
            return queryInterface.sequelize.query(
                sqlRaw
            );
        }).then(function () {
            logger.warn('Setup version of question_or_translation_review as mandatory attribute');
            return queryInterface.changeColumn(
                'question_or_translation_review',
                'version',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            )
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question_or_translation_review',
            'version'
        );
    }
};