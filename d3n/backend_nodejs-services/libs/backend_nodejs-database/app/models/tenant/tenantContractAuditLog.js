var _constants = {
    TYPE_NONE: 'none',
    TYPE_HOLD_REQUEST: 'holdRequest',
    TYPE_CLOSE_REQUEST: 'closeRequest',
    TYPE_RENEW_REQUEST: 'renewContractRequest'
};

var TenantContractAuditLog = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tenantContractAuditLog', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        contractId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        creatorProfileId: {
            type: DataTypes.STRING,
            allowNull: false
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_NONE,
                _constants.TYPE_HOLD_REQUEST,
                _constants.TYPE_CLOSE_REQUEST,
                _constants.TYPE_RENEW_REQUEST
            ),
            allowNull: false,
            defaultValue: _constants.TYPE_NONE
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
        tableName: 'tenant_contract_audit_log',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenantContract, { foreignKey: 'contractId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
