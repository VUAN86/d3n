var _constants = {
    STATUS_POTENTIAL: 'potential',
    STATUS_ACCEPTED: 'accepted',
    STATUS_REJECTED: 'rejected',
    TYPE_CREATION: 'creation',
    TYPE_REVIEW: 'review',
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('paymentAction', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
        },
        resourceId: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        questionTranslationId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_POTENTIAL,
                _constants.STATUS_ACCEPTED,
                _constants.STATUS_REJECTED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_POTENTIAL
        },
        rejectionReason: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        isBillable: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
        },
        paymentStructureId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_CREATION,
                _constants.TYPE_REVIEW
            ),
            allowNull: true,
        },
        paymentTenantBillOfMaterialId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        paymentResourceBillOfMaterialId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'payment_action',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.questionTranslation, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentStructure, { foreignKey: 'paymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentTenantBillOfMaterial, { foreignKey: 'paymentTenantBillOfMaterialId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentResourceBillOfMaterial, { foreignKey: 'paymentResourceBillOfMaterialId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
