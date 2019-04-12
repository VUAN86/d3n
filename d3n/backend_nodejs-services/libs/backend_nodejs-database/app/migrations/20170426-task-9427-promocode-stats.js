// Add statistics to promocode
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'promocode_campaign',
            'stat_inventoryCount',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'stat_used',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'stat_creditsPaid',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'stat_bonusPointsPaid',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        }).then(function () {
            return queryInterface.addColumn(
                'promocode_campaign',
                'stat_moneyPaid',
                {
                    type: Sequelize.DECIMAL(10,2),
                    allowNull: false,
                    defaultValue: '0.00'
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'promocode_campaign',
            'stat_inventoryCount'
        ).then(function () {
            return queryInterface.removeColumn(
                'promocode_campaign',
                'stat_used'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode_campaign',
                'stat_creditsPaid'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode_campaign',
                'stat_bonusPointsPaid'
            );
        }).then(function () {
            return queryInterface.removeColumn(
                'promocode_campaign',
                'stat_moneyPaid'
            );
        });
    }
};