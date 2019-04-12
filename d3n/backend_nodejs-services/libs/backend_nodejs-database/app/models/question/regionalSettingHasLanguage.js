var _constants = {};

var RegionalSettingHasLanguage = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('regionalSettingHasLanguage', {
        regionalSettingId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        languageId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'regional_setting_has_language',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.regionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.language, { foreignKey: 'languageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
