// Add field to Game: iconId
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'iconId',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'iconId'
        );
    }
};