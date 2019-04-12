// Add statistics entries to questionTranslation instead of question entity
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question_translation',
            'stat_associatedPools',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'stat_associatedGames',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'stat_gamesPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'stat_rightAnswers',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'stat_wrongAnswers',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'stat_averageAnswerSpeed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question_translation',
                'stat_rating',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_associatedPools'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_associatedGames'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_gamesPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_rightAnswers'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_wrongAnswers'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_averageAnswerSpeed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_rating'
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question_translation',
            'stat_associatedPools'
        ).then(function () {
            return queryInterface.removeColumn(
                'question_translation',
                'stat_associatedGames'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question_translation',
                'stat_gamesPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question_translation',
                'stat_rightAnswers'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question_translation',
                'stat_wrongAnswers'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question_translation',
                'stat_averageAnswerSpeed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'question_translation',
                'stat_rating'
            );
        });
    }
};