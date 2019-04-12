var _constants = {};

var WorkorderBillingModel = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('workorderBillingModel', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: false,
        }
    },
    {
        timestamps: false,
        tableName: 'workorder_billing_model',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.workorder, { foreignKey: 'workorderBillingModelId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
