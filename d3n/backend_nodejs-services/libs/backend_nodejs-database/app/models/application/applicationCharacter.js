var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationCharacter', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'application_character',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.applicationHasApplicationCharacter, { foreignKey: 'applicationCharacterId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
