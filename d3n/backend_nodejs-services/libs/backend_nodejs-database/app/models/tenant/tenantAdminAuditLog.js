var TenantAdminAuditLog = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tenantAdminAuditLog', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        userId: {
            type: DataTypes.STRING,
            allowNull: false
        },
        email: {
            type: DataTypes.STRING,
            allowNull: false
        },
        items: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('items', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('items')) {
                    return JSON.parse(this.getDataValue('items'));
                } else {
                    return null;
                }
            }
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
    }, {
        timestamps: false,
        tableName: 'tenant_admin_audit_log'
    });
};
