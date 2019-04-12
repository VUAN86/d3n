// Adjust field application.applicationEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'application',
            'stat_games',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_gamesPlayed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_players',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_friendsInvited',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_quizGames',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_moneyGames',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_bettingGames',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_downloads',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_installations',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_deinstallations',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_moneyCharged',
                {
                    type: Sequelize.DECIMAL(10,2),
                    allowNull: false,
                    defaultValue: '0.00'
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_creditsPurchased',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_moneyWon',
                {
                    type: Sequelize.DECIMAL(10,2),
                    allowNull: false,
                    defaultValue: '0.00'
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_bonusPointsWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_creditsWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_voucherWon',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'application',
                'stat_adsViewed',
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
            'application',
            'stat_games'
        ).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_gamesPlayed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_players'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_friendsInvited'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_quizGames'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_moneyGames'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_bettingGames'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_downloads'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_installations'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_deinstallations'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_moneyCharged'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_creditsPurchased'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_moneyWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_bonusPointsWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_creditsWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_voucherWon'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'application',
                'stat_adsViewed'
            );
        });
    }
};