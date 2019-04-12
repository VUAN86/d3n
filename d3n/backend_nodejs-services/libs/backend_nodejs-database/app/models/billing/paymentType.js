var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('paymentType', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: false,
        }
    }, {
        timestamps: false,
        tableName: 'payment_type',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.applicationHasPaymentType, { foreignKey: 'paymentTypeId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
