var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('gameHasRegionalSetting', {
        gameId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        regionalSettingId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'game_has_regional_setting',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.game, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.regionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
