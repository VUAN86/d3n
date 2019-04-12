// Add field Game.prizes
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'prizes',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'prizes'
        );
    }
};