// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'advertisement',
            'stat_appsUsed',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'advertisement',
                'stat_gamesUsed',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'advertisement',
                'stat_views',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'advertisement',
                'stat_earnedCredits',
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
            'advertisement',
            'stat_appsUsed'
        ).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_gamesUsed'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_views'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'advertisement',
                'stat_earnedCredits'
            );
        });
    }
};