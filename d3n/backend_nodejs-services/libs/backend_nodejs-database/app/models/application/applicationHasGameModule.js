var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive',
};

var ApplicationHasGameModule = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationHasGameModule', {
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true
        },
        gameModuleId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_ACTIVE,
                _constants.STATUS_INACTIVE
            ),
            allowNull: true,
            defaultValue: _constants.STATUS_ACTIVE
        }
    },
    {
        timestamps: false,
        tableName: 'application_has_game_module',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.gameModule, { foreignKey: 'gameModuleId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};