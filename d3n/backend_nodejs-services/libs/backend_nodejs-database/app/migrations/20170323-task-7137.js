// Add Tombola.isOpenCheckout
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'tombola',
            'isOpenCheckout',
            {
                type: Sequelize.INTEGER(1),
                allowNull: false,
                defaultValue: 0
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'tombola',
            'isOpenCheckout'
        );
    }
};