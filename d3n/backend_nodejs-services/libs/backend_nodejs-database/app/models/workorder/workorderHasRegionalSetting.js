var _constants = {};

var WorkorderHasRegionalSetting = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('workorderHasRegionalSetting', {
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        regionalSettingId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'workorder_has_regional_setting',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.regionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
