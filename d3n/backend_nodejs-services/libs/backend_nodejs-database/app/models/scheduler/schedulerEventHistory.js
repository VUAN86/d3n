var _constants = {
    EMITTED_YES: 'yes',
    EMITTED_NO: 'no'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('schedulerEventHistory', {
        id: {
            type: DataTypes.INTEGER(11).UNSIGNED,
            primaryKey: true,
            autoIncrement: true
        },
        
        schedulerItemId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        
        scheduledAt: {
            type: DataTypes.INTEGER(11).UNSIGNED,
            allowNull: false
        },
        
        /**
         * 
         * {
         *      name: '', // e.g. tombola/openCheckout/23423
         *      data: Object|null
         * }
         */
        event: {
            type: DataTypes.TEXT,
            allowNull: false
        },
        
        emitted: {
            type: DataTypes.ENUM(
                _constants.EMITTED_YES,
                _constants.EMITTED_NO
            ),
            allowNull: false,
            defaultValue: _constants.EMITTED_NO
        }
        
    }, {
        timestamps: false,
        tableName: 'scheduler_event_history',
        indexes: [
            {
                unique: false,
                fields:['scheduledAt']
            }
        ],
        classMethods: {
            associate: function (models) {
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
