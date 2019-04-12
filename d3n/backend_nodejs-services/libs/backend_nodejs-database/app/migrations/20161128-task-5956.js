// Add game.playerEntryFeeSettings JSON BLOB
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'playerEntryFeeSettings',
            {
                type: Sequelize.TEXT,
                allowNull: true
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'playerEntryFeeSettings'
        );
    }
};