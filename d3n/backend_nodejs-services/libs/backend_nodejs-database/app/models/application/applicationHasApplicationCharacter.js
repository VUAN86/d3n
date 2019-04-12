var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive',
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationHasApplicationCharacter', {
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        applicationCharacterId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_ACTIVE,
                _constants.STATUS_INACTIVE
            ),
            allowNull: true,
            defaultValue: _constants.STATUS_ACTIVE
        }
    }, {
        timestamps: false,
        tableName: 'application_has_application_character',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.applicationCharacter, { foreignKey: 'applicationCharacterId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
