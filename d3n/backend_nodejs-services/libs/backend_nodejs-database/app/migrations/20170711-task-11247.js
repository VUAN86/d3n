// Change voucher.description, voucher.winningCondition type to TEXT
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'voucher',
            'description',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'voucher',
                'winningCondition',
                {
                    type: Sequelize.TEXT,
                    allowNull: true
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'voucher',
            'description',
            {
                type: Sequelize.STRING,
                allowNull: true,
            }
        ).then(function () {
            return queryInterface.changeColumn(
                'voucher',
                'winningCondition',
                {
                    type: Sequelize.STRING,
                    allowNull: true,
                }
            );
        });
    }
};