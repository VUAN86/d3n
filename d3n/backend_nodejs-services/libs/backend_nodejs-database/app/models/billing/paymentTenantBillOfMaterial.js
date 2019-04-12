var _constants = {
    STATUS_UNAPPROVED: 'unapproved',
    STATUS_APPROVED: 'approved',
    STATUS_PAYED: 'payed',
    STATUS_DECLINED: 'declined',
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('paymentTenantBillOfMaterial', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        amountMoneyEur: {
            type: DataTypes.DECIMAL,
            allowNull: false,
        },
        amountBonus: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        amountCredits: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_UNAPPROVED,
                _constants.STATUS_APPROVED,
                _constants.STATUS_PAYED,
                _constants.STATUS_DECLINED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_UNAPPROVED
        },
        approvalDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        paymentDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        adjustmentReason: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        transactionId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        totalTransactions: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        paymentStructureId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'payment_tenant_bill_of_material',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentStructure, { foreignKey: 'paymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentAction, { foreignKey: 'paymentTenantBillOfMaterialId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
