module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'isTombolaPrize'
        ).then(function () {
            return queryInterface.addColumn(
                'voucher',
                'isTombolaPrize',
                {
                    type: Sequelize.INTEGER(1),
                    allowNull: true,
                    defaultValue: 0
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'voucher',
            'isTombolaPrize'
        );
    }
};