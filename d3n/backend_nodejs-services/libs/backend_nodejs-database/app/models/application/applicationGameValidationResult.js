var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationGameValidationResult', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        result: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        warningMessage: {
            type: DataTypes.STRING,
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'application_game_validation_result',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
