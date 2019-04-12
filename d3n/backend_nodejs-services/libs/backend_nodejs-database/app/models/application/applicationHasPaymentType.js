var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationHasPaymentType', {
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        paymentTypeId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'application_has_payment_type',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentType, { foreignKey: 'paymentTypeId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
