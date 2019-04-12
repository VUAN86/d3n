var _constants = {
    STATUS_APPLIED: 'applied',
    STATUS_APPROVED: 'approved',

    ROLE_COMMUNITY: 'COMMUNITY',
    ROLE_ADMIN: 'ADMIN',
    ROLE_INTERNAL: 'INTERNAL',
    ROLE_EXTERNAL: 'EXTERNAL'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('profileHasRole', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true,
        },
        profileId: {
            type: DataTypes.STRING,
            allowNull: false,
            comment: "The id of the user in the aerospike database",
            unique: "profileTenantRole"
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            comment: "The tenant identifier, the role applies to a certain tenant only",
            unique: "profileTenantRole"
        },
        role: {
            type: DataTypes.ENUM(
                _constants.ROLE_COMMUNITY,
                _constants.ROLE_ADMIN,
                _constants.ROLE_INTERNAL,
                _constants.ROLE_EXTERNAL
            ),
            allowNull: false,
            comment: "Role identifier string, these are roles within a given tenant.",
            unique: "profileTenantRole"
        },
        requestDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        acceptanceDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_APPLIED,
                _constants.STATUS_APPROVED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_APPROVED,
            comment: "Status of this entry, only approved roles are assigned to a user."
        }
    },
    {
        timestamps: false,
        tableName: 'profile_has_role',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.profile, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
