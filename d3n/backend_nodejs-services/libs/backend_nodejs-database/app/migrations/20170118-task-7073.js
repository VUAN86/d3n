// Add fields to Pool: iconId, color
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'pool',
            'iconId',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'pool',
                'color',
                {
                    type: Sequelize.STRING,
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'pool',
            'iconId'
        ).then(function () {
            return queryInterface.removeColumn(
                'pool',
                'color'
            );
        });
    }
};