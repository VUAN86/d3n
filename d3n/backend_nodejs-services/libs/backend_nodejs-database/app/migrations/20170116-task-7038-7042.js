// Add currency and version fields
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'application',
            'versionInfo',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        ).then(function () {
            return queryInterface.addColumn(
                'tenant',
                'currency',
                {
                    type: Sequelize.STRING(3),
                    allowNull: true
                }
            );
        });
    },
    
    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'application',
            'versionInfo'
        ).then(function () {
            return queryInterface.removeColumn(
                'tenant',
                'currency'
            );
        });
    }
};