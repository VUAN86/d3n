var _constants = {
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('schedulerEmitFrom', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        
        instanceId: {
            type: DataTypes.STRING,
            allowNull: false
        },
        
        emitFrom: {
            type: DataTypes.INTEGER(11).UNSIGNED,
            allowNull: false
        }
    }, {
        timestamps: false,
        tableName: 'scheduler_emit_from',
        classMethods: {
            associate: function (models) {
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
