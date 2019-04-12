// Adjust field Game.gameEntryCurrency enum
module.exports = {
    up: function (queryInterface, Sequelize) {
        var _constants = {
            GAME_ENTRY_CURRENCY_MONEY : 'MONEY',
            GAME_ENTRY_CURRENCY_CREDIT : 'CREDIT',
            GAME_ENTRY_CURRENCY_BONUS : 'BONUS',
        };
        return queryInterface.changeColumn(
            'game',
            'gameEntryCurrency',
            {
                type: Sequelize.ENUM(
                    _constants.GAME_ENTRY_CURRENCY_MONEY,
                    _constants.GAME_ENTRY_CURRENCY_CREDIT,
                    _constants.GAME_ENTRY_CURRENCY_BONUS
                ),
                allowNull: true,
            }
        );
    },
    
    down: function (queryInterface, Sequelize) {
        var _constants = {
            GAME_ENTRY_CURRENCY_EURO : 'euro',
            GAME_ENTRY_CURRENCY_CREDIT : 'credit',
            GAME_ENTRY_CURRENCY_BONUS_POINT : 'bonusPoint',
        };
        return queryInterface.changeColumn(
            'game',
            'gameEntryCurrency',
            {
                type: Sequelize.ENUM(
                    _constants.GAME_ENTRY_CURRENCY_EURO,
                    _constants.GAME_ENTRY_CURRENCY_CREDIT,
                    _constants.GAME_ENTRY_CURRENCY_BONUS_POINT
                ),
                allowNull: true,
            }
        );
    }
};