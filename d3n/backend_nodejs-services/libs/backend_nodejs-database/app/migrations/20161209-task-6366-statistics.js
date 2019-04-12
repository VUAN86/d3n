// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'stat_players',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_gamesPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_adsViewed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_creditsPayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_moneyPayed',
                {
                    type: Sequelize.DECIMAL(10,2),
                    allowNull: false,
                    defaultValue: '0.00'
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_bonusPointsPaid',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_voucherWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_bonusPointsWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_creditsWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_moneyWon',
                {
                    type: Sequelize.DECIMAL(10,2),
                    allowNull: false,
                    defaultValue: '0.00'
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_questionsAnswered',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_questionsAnsweredRight',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_questionsAnsweredWrong',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_averageAnswerSpeed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_poolsCount',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'game',
                'stat_adsCount',
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
            'game',
            'stat_players'
        ).then(function () {
            return queryInterface.removeColumn(
                'game',
                'stat_gamesPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'game',
                'stat_adsViewed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_creditsPayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_moneyPayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_bonusPointsPaid'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_voucherWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_bonusPointsWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_creditsWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_moneyWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_questionsAnswered'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_questionsAnsweredRight'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_questionsAnsweredWrong'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_averageAnswerSpeed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_poolsCount'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_adsCount'
            );
        });
    }
};