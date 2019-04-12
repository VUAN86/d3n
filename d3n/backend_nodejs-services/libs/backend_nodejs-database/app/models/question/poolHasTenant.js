var _constants = {
    ROLE_VIEW: 'view',
    ROLE_SELL: 'sell'
};

var PoolHasTenant = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('poolHasTenant', {
        poolId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        role: {
            type: DataTypes.ENUM(
                _constants.ROLE_VIEW,
                _constants.ROLE_SELL
            ),
            allowNull: false
        }
    },
    {
        timestamps: false,
        tableName: 'pool_has_tenant',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.pool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};