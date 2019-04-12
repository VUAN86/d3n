var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationAnimation', {
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
        },
        areaUsage: {
            type: DataTypes.STRING,
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'application_animation',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.applicationHasApplicationAnimation, { foreignKey: 'applicationAnimationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
