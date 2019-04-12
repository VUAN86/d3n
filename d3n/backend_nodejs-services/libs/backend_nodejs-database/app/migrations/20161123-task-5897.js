// Add advertisement/voucher.isDeployed
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.addColumn(
            'advertisement',
            'isDeployed',
            {
                type: Sequelize.INTEGER(1),
                allowNull: false,
                defaultValue: 0
            }
        ).then(function () {
            return queryInterface.addColumn(
                'voucher',
                'isDeployed',
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
            'advertisement',
            'isDeployed'
        ).then(function () {
            return queryInterface.removeColumn(
                'voucher',
                'isDeployed'
            );
        });
    }
};