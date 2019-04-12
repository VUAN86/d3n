// Change game.gameEntryAmount: setup default value
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.sequelize.query(
            'UPDATE `game` SET `gameEntryAmount` = 0 WHERE `gameEntryAmount` IS NULL;'
        ).then(function () {
            return queryInterface.changeColumn(
                'game',
                'gameEntryAmount',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'game',
            'gameEntryAmount',
            {
                type: Sequelize.INTEGER(11),
                allowNull: true
            }
        );
    }
};