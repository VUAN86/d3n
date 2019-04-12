var _constants = {
    ENTRY_FEE_CURRENCY_MONEY : 'MONEY',
    ENTRY_FEE_CURRENCY_CREDIT : 'CREDIT',
    ENTRY_FEE_CURRENCY_BONUS : 'BONUS',
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('gameHasWinningComponent', {
        gameId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        winningComponentId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        rightAnswerPercentage: {
            type: DataTypes.DECIMAL(5,2),
            allowNull: false,
            defaultValue: '100.00'
        },
        isFree: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 1
        },
        entryFeeAmount: {
            type: DataTypes.DECIMAL(10, 2),
            allowNull: false,
            defaultValue: '0.00'
        },
        entryFeeCurrency: {
            type: DataTypes.ENUM(
                _constants.ENTRY_FEE_CURRENCY_MONEY,
                _constants.ENTRY_FEE_CURRENCY_CREDIT,
                _constants.ENTRY_FEE_CURRENCY_BONUS
            ),
            allowNull: false,
            defaultValue: _constants.ENTRY_FEE_CURRENCY_BONUS
        }
    }, {
        timestamps: false,
        tableName: 'game_has_winning_component',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.game, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.winningComponent, { foreignKey: 'winningComponentId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
