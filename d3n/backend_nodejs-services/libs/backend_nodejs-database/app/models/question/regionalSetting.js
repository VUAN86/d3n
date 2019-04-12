var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive'
};

var RegionalSetting = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('regionalSetting', {
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
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_ACTIVE, 
                _constants.STATUS_INACTIVE
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_ACTIVE
        },
        defaultLanguageId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        }
    },
    {
        timestamps: false,
        tableName: 'regional_setting',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.language, { foreignKey: 'defaultLanguageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.question, { foreignKey: 'primaryRegionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionHasRegionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.regionalSettingHasLanguage, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorder, { foreignKey: 'primaryRegionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasRegionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasRegionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasRegionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profile, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.advertisementProviderHasRegionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.voucherProviderHasRegionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};