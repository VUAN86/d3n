var _constants = {
    VERIFICATION_STATUS_VERIFIED: 'verified',
    VERIFICATION_STATUS_NOT_VERIFIED: 'notVerified'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('profilePhone', {
        phone: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        },
        verificationStatus: {
            type: DataTypes.ENUM(
                _constants.VERIFICATION_STATUS_VERIFIED, 
                _constants.VERIFICATION_STATUS_NOT_VERIFIED
            ),
            allowNull: true,
            defaultValue: _constants.VERIFICATION_STATUS_NOT_VERIFIED,
        },
        profileId: {
            type: DataTypes.STRING,
            allowNull: false,
        }
    }, {
        timestamps: false,
        tableName: 'profile_phone',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.profile, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
