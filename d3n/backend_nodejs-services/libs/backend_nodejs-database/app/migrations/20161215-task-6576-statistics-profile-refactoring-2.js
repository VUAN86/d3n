// Add back stat_moneyPlayed field
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'profile',
            'stat_moneyPlayed',
            {
                type: Sequelize.DECIMAL(10,2),
                allowNull: false,
                defaultValue: '0.00'
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'profile',
            'stat_moneyPlayed'
        );
    }
};