var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('profileHasApplication', {
        profileId: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        },
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'profile_has_application',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.profile, { foreignKey: 'profileId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
