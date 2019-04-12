// Add statistics fields to question table
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'question',
            'stat_associatedPools',
            {
                type: Sequelize.INTEGER(11),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'question',
                'stat_associatedGames',
                {
                    type: Sequelize.INTEGER(11),
                    allowNull: false,
                    defaultValue: 0
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'question',
            'stat_associatedPools'
        ).then(function () {
            return queryInterface.removeColumn(
                'question',
                'stat_associatedGames'
            );
        });
    }
};