var _constants = {
    SEX_MALE: 'male',
    SEX_FEMALE: 'female'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('profile', {
        userId: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        },
        languageId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        regionalSettingId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        silentCountryCode: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        autoShare: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        firstName: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        lastName: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        nickname: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        birthDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        sex: {
            type: DataTypes.ENUM(
                _constants.SEX_MALE,
                _constants.SEX_FEMALE
            ),
            allowNull: true,
        },
        addressStreet: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        addressStreetNumber: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        addressCity: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        addressPostalCode: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        addressCountry: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        organization: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        profilePhoto: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        recommendedByProfileId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        lastActive: {
            type: DataTypes.DATE,
            allowNull: true,
        },

        preferences: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('preferences', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('preferences')) {
                    return JSON.parse(this.getDataValue('preferences'));
                } else {
                    return null;
                }
            }
        }
    }, {
        timestamps: false,
        tableName: 'profile',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.language, { foreignKey: 'languageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.regionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.profile, { foreignKey: 'recommendedByProfileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profile, { as: 'recommendedByProfiles', foreignKey: 'recommendedByProfileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileEmail, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profilePhone, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasApplication, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasRole, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasStats, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasGlobalRole, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasTenant, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
