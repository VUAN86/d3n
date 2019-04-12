// Set default of Game.isMultiplePurchaseAllowed to 1
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'game',
            'isMultiplePurchaseAllowed',
            {
                type: Sequelize.INTEGER(1),
                allowNull: false,
                defaultValue: 1
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};