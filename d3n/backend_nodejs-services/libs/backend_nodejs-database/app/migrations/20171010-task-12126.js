// Adjust GameHasWinningComponent.entryFeeAmount to allow double values
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.changeColumn(
            'game_has_winning_component',
            'entryFeeAmount',
            {
                type: Sequelize.DECIMAL(10, 2),
                allowNull: false,
                defaultValue: '0.00'
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return true;
    }
};