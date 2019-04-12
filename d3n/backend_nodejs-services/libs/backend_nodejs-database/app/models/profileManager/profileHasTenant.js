var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('profileHasTenant', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true,
        },
        profileId: {
            type: DataTypes.STRING,
            allowNull: false,
            comment: "The id of the user in the aerospike database",
            unique: "profileTenantUsage"
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            comment: "The tenant identifier used by profile",
            unique: "profileTenantUsage"
        }
    },
    {
        timestamps: false,
        tableName: 'profile_has_tenant',
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
