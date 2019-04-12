var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('gamePoolValidationResult', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        gameId: {
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
        tableName: 'game_pool_validation_result',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.game, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
