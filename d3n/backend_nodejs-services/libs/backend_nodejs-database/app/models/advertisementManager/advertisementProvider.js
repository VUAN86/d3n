var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive',
    STATUS_ARCHIVED: 'archived',
    TYPE_MANUAL: 'manual',
    TYPE_FEEDER: 'feeder'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('advertisementProvider', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_ACTIVE,
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: true,
            defaultValue: _constants.STATUS_ACTIVE
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_MANUAL,
                _constants.TYPE_FEEDER
            ),
            allowNull: true
        },
        feederAPI: {
            type: DataTypes.STRING,
            allowNull: true
        },
        feederAttributes: {
            type: DataTypes.STRING,
            allowNull: true
        },
        startDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        endDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        }
    }, {
        timestamps: false,
        tableName: 'advertisement_provider',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.advertisement, { foreignKey: 'advertisementProviderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.advertisementProviderHasRegionalSetting, { foreignKey: 'advertisementProviderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.game, { foreignKey: 'advertisementProviderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
