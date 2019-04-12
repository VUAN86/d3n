var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('applicationHasTombola', {
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        tombolaId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'application_has_tombola',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tombola, { foreignKey: 'tombolaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
