var _constants = {
    TYPE_CHANGE: 'dataChange'
};

var TenantAuditLog = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tenantAuditLog', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        creatorProfileId: {
            type: DataTypes.STRING,
            allowNull: false
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_CHANGE
            ),
            allowNull: false,
            defaultValue: _constants.TYPE_CHANGE
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
        tableName: 'tenant_audit_log',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
