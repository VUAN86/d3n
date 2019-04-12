// Add game.jokerConfiguration
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'jokerConfiguration',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );        
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('game', 'jokerConfiguration');
    }
};