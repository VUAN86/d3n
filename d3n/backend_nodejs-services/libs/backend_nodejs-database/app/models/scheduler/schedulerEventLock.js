var _constants = {
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('schedulerEventLock', {
        shedulerEventId: {
            type: DataTypes.INTEGER(11).UNSIGNED,
            primaryKey: true,
            allowNull: false
        }
    }, {
        timestamps: false,
        tableName: 'scheduler_event_lock',
        classMethods: {
            associate: function (models) {
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
