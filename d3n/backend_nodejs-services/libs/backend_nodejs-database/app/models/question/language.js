var _constants = {};

var Language = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('language', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        iso: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        name: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        }
    },
    {
        timestamps: false,
        tableName: 'language',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionTranslation, { foreignKey: 'languageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.regionalSetting, { foreignKey: 'defaultLanguageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.regionalSettingHasLanguage, { foreignKey: 'languageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profile, { foreignKey: 'languageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
