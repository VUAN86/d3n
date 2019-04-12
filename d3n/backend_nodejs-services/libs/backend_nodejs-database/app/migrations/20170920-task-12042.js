// Add fields to Game: entryFeeBatchSize, isMultiplePurchaseAllowed
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'game',
            'entryFeeBatchSize',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 1
            }
        ).then(function () {
            return queryInterface.addColumn(
                'game',
                'isMultiplePurchaseAllowed',
                {
                    type: Sequelize.INTEGER(1),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        });
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'game',
            'entryFeeBatchSize'
        ).then(function () {
            return queryInterface.removeColumn(
                'game',
                'isMultiplePurchaseAllowed'
            );
        });
    }
};