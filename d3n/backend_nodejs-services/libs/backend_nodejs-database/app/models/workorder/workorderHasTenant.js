var _constants = {};

var WorkorderHasTenant = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('workorderHasTenant', {
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'workorder_has_tenant',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
