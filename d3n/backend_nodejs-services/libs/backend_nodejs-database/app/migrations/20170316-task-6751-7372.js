// Optimize promocodes
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'promocode',
            'number',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 1
            }
        ).then(function () {
            return queryInterface.addColumn(
                'promocode',
                'isUnique',
                {
                    type: Sequelize.INTEGER(1),
                    allowNull: false,
                    defaultValue: 1
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};