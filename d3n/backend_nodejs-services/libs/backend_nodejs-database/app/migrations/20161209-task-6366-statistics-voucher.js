// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'voucher',
            'stat_inventoryCount',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'voucher',
                'stat_won',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'voucher',
                'stat_archieved',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'voucher',
                'stat_specialPrizes',
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
            'voucher',
            'stat_inventoryCount'
        ).then(function () {
            return queryInterface.removeColumn(
                'voucher',
                'stat_won'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'voucher',
                'stat_archieved'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'voucher',
                'stat_specialPrizes'
            );
        });
    }
};