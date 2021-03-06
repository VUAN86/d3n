module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'isTombolaPrize',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true,
                defaultValue: 0
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'isTombolaPrize'
        );
    }
};