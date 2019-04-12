var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive',

    ACTION_NONE: 'none',
    ACTION_HOLD: 'hold',
    ACTION_CLOSE: 'close',
    ACTION_RENEW: 'renew'
};

var TenantContract = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tenantContract', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        externalId: {
            type: DataTypes.STRING,
            allowNull: true,
            comment: 'The id of the contract in the tenant management system'
        },
        name: {
            type: DataTypes.STRING,
            allowNull: false
        },
        type: {
            type: DataTypes.STRING,
            allowNull: true
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_ACTIVE,
                _constants.STATUS_INACTIVE
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_ACTIVE,
            comment: 'Current status of the contract, active or inactive.'
        },
        action: {
            type: DataTypes.ENUM(
                _constants.ACTION_NONE,
                _constants.ACTION_HOLD,
                _constants.ACTION_CLOSE,
                _constants.ACTION_RENEW
            ),
            allowNull: false,
            defaultValue: _constants.ACTION_NONE,
            comment: 'The last action that was requested on the contract item.'
        },
        content: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        startDate: {
            type: DataTypes.DATE,
            allowNull: false
        },
        endDate: {
            type: DataTypes.DATE,
            allowNull: false
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
        updateDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
    }, {
        timestamps: false,
        tableName: 'tenant_contract',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.tenantContractAuditLog, { foreignKey: 'contractId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
