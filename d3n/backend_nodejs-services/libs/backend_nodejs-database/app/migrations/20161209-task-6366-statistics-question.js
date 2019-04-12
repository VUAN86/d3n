// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question',
            'stat_associatedPools',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'question',
                'stat_associatedGames',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question',
                'stat_gamesPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question',
                'stat_rightAnswers',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question',
                'stat_wrongAnswers',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question',
                'stat_averageAnswerSpeed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'question',
                'stat_rating',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question',
            'stat_associatedPools'
        ).then(function () {
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
    }
};