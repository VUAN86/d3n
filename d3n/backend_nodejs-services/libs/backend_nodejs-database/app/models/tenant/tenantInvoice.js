var _constants = {
    TYPE_CREDIT: 'credit',
    TYPE_DEBIT: 'debit',
    
    STATUS_UNPAID: 'unpaid',
    STATUS_PAID: 'paid',
    STATUS_REFUNDED: 'refunded',
    STATUS_RESOLVED: 'resolved',
    STATUS_OVER_DUE: 'over_due'
    
};

var TenantInvoice = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tenantInvoice', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        number: {
            type: DataTypes.STRING,
            allowNull: false
        },
        url: {
            type: DataTypes.STRING,
            allowNull: false
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_CREDIT,
                _constants.TYPE_DEBIT
            ),
            allowNull: true
        },
        date: {
            type: DataTypes.DATE,
            allowNull: true
        },
        paymentDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        totalNet: {
            type: DataTypes.FLOAT(),
            allowNull: true
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_UNPAID,
                _constants.STATUS_PAID,
                _constants.STATUS_REFUNDED,
                _constants.STATUS_RESOLVED,
                _constants.STATUS_OVER_DUE
            ),
            allowNull: true
        },
        invoiceDetails: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('invoiceDetails', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('invoiceDetails')) {
                    return JSON.parse(this.getDataValue('invoiceDetails'));
                } else {
                    return null;
                }
            }
        },
        totalVat: {
            type: DataTypes.FLOAT(),
            allowNull: true
        },
        vatAppliedPercentage: {
            type: DataTypes.FLOAT(),
            allowNull: true
        },
        totalGross: {
            type: DataTypes.FLOAT(),
            allowNull: true
        }
    }, {
        timestamps: false,
        tableName: 'tenant_invoice',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
