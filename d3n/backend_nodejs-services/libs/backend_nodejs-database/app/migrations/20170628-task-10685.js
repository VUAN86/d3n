// Add fields to Workorder: iconId, color
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'workorder',
            'iconId',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'workorder',
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
            'workorder',
            'iconId'
        ).then(function () {
            return queryInterface.removeColumn(
                'workorder',
                'color'
            );
        });
    }
};