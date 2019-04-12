var _constants = {
    TYPE_INSTANT: 'instant',
    TYPE_USAGE: 'usage',
    USAGE_PAYMENT_TYPE_AVERAGE_PER_QUESTION_USAGE: 'average_per_question_usage',
    USAGE_PAYMENT_TYPE_REVENUE_PAYMENT_PERCENT: 'revenue_payment_percent',
    USAGE_PAYMENT_TYPE_PER_QUESTION_USED: 'per_question_used'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('paymentStructure', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        isActive: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
        },
        autoPayment: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
            defaultValue: '0',
        },
        autoPaymentLimit: {
            type: DataTypes.DECIMAL,
            allowNull: true,
            defaultValue: '0.00',
        },
        payUnit: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0,
        },
        unit: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_INSTANT,
                _constants.TYPE_USAGE
            ),
            allowNull: false,
        },
        usagePaymentType: {
            type: DataTypes.ENUM(
                _constants.USAGE_PAYMENT_TYPE_AVERAGE_PER_QUESTION_USAGE,
                _constants.USAGE_PAYMENT_TYPE_REVENUE_PAYMENT_PERCENT,
                _constants.USAGE_PAYMENT_TYPE_PER_QUESTION_USED
            ),
            allowNull: true,
        },
        usagePaymentAmountMoneyEur: {
            type: DataTypes.DECIMAL,
            allowNull: true,
            defaultValue: '0.00',
        },
        usagePaymentAmountBonus: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
            defaultValue: '0',
        },
        usagePaymentAmountCredits: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
            defaultValue: '0',
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        creatorResourceId: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        }
    }, {
        timestamps: false,
        tableName: 'payment_structure',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentAction, { foreignKey: 'paymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentResourceBillOfMaterial, { foreignKey: 'paymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentTenantBillOfMaterial, { foreignKey: 'paymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentStructureTier, { foreignKey: 'paymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorder, { foreignKey: 'questionCreatePaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorder, { foreignKey: 'questionReviewPaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorder, { foreignKey: 'translationCreatePaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorder, { foreignKey: 'translationReviewPaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
