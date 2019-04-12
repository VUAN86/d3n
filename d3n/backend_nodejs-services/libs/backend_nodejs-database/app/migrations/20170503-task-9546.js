module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'tombola',
            'percentOfTicketsAmount',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true,
                defaultValue: 0
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'tombola',
            'percentOfTicketsAmount'
        );
    }
};