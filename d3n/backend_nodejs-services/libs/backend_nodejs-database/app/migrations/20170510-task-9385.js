module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'tenant_contract',
            'externalId',
            {
                type: Sequelize.STRING,
                allowNull: true
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.removeColumn(
            'tenant_contract',
            'externalId'
        );
    }
};