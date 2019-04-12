// Add configuration_setting table
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.createTable(
            'configuration_setting',
            {
                id: {
                    type: Sequelize.STRING,
                    primaryKey: true,
                    comment: "Configuration setting object identifier."
                },
                data: {
                    type: Sequelize.TEXT('long'),
                    allowNull: false,
                    comment: "JSON object that contains configuration settings."
                }
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.dropTable('configuration_setting');
    }
};