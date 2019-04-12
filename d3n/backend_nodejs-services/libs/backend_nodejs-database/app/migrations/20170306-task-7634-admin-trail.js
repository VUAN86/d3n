// Add tenant.tenantAdminAuditLog
module.exports = {
    up: function (queryInterface, Sequelize) {
        return queryInterface.createTable(
            'tenant_admin_audit_log',
            {
                id: {
                    type: Sequelize.INTEGER(11),
                    primaryKey: true,
                    autoIncrement: true
                },
                userId: {
                    type: Sequelize.STRING,
                    allowNull: false
                },
                email: {
                    type: Sequelize.STRING,
                    allowNull: false
                },
                items: {
                    type: Sequelize.TEXT,
                    allowNull: true,
                },
                createDate: {
                    type: Sequelize.DATE,
                    allowNull: false,
                    defaultValue: Sequelize.NOW
                },
            }
        );
    },

    down: function (queryInterface, Sequelize) {
        return queryInterface.dropTable('tenant_admin_audit_log');
    }
};