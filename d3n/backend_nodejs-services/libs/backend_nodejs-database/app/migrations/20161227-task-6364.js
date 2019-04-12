// Add game.playerEntryFeeSettings JSON BLOB
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'userCanOverridePools',
            {
                type: Sequelize.INTEGER(1),
                allowNull: true,
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'userCanOverridePools'
        );
    }
};