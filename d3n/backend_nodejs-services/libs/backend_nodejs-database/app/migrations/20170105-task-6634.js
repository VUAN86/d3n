// Add unit:integer to paymentStructure
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'payment_structure',
            'unit',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'payment_structure',
            'unit'
        );
    }
};