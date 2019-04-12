module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'repetition',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'repetition'
        );
    }
};
