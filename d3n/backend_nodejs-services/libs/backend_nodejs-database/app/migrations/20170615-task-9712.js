// Remove badge.battleRules
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn('badge', 'battleRules');
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'badge',
            'battleRules',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );        
    }
};