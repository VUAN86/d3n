module.exports = function (sequelize, DataTypes) {
    return sequelize.define('paymentStructureTier', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        quantityMin: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: '0',
        },
        amountMoneyEur: {
            type: DataTypes.DECIMAL,
            allowNull: true,
            defaultValue: '0.00',
        },
        amountBonus: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
            defaultValue: '0',
        },
        amountCredits: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
            defaultValue: '0',
        },
        paymentStructureId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        }
    }, {
        timestamps: false,
        tableName: 'payment_structure_tier',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.paymentStructure, { foreignKey: 'paymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
