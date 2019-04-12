// Adjust field Game.prizes -> TEXT
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'game',
            'prizes',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'game',
            'prizes',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        );
    }
};