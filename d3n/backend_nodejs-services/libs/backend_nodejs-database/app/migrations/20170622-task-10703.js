// Add game.jokerConfiguration
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'isRegionalLimitationEnabled',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true,
                defaultValue: 0
            }
        );        
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('game', 'isRegionalLimitationEnabled');
    }
};