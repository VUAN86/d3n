var _constants = {
    ROLE_ANONYMOUS: 'ANONYMOUS',
    ROLE_NOT_VALIDATED: 'NOT_VALIDATED',
    ROLE_REGISTERED: 'REGISTERED',
    ROLE_FULLY_REGISTERED: 'FULLY_REGISTERED',
    ROLE_FULLY_REGISTERED_BANK: 'FULLY_REGISTERED_BANK',
    ROLE_FULLY_REGISTERED_BANK_O18: 'FULLY_REGISTERED_BANK_O18'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('profileHasGlobalRole', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true,
        },
        profileId: {
            type: DataTypes.STRING,
            allowNull: false,
            comment: "The id of the user in the aerospike database.",
            unique: "profileGlobalRole"
        },
        role: {
            type: DataTypes.ENUM(
                _constants.ROLE_ANONYMOUS,
                _constants.ROLE_NOT_VALIDATED,
                _constants.ROLE_REGISTERED,
                _constants.ROLE_FULLY_REGISTERED,
                _constants.ROLE_FULLY_REGISTERED_BANK,
                _constants.ROLE_FULLY_REGISTERED_BANK_O18
            ),
            allowNull: false,
            comment: "Role identifier string, these are roles.",
            unique: "profileGlobalRole"
        }
    },
    {
        timestamps: false,
        tableName: 'profile_has_global_role',
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
