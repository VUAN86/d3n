module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'tombola',
            'preCloseOffsetMinutes',
            {
                type: Sequelize.INTEGER(11).UNSIGNED,
                allowNull: true
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'tombola',
            'preCloseOffsetMinutes'
        );
    }
};
