var _constants = {
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('schedulerItem', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        
        itemId: {
            type: DataTypes.STRING,
            allowNull: false
        },
        
        /*
         * Schedule definition.
         * 
         * live tournament:
         * {
         *      startDate: timestamp, 
         *      endDate: timestamp
         *      repetition: {
         *          unit: 'day|week|month',
         *          repeat: 2|5, // repeat every 2/5 days/weeks/months
         *          repeatOn: 'Monday 10:23|Sunday 11:00|23 10:00|14 23:01' // day of week or day of month. make sense only for weeks or months interval
         *          exceptDays: [timestamp, timestamp] // timestamp for YYYY-MM-DD:00:00:00,
         *          offsetOpenRegistration: {
         *              unit: 'minute|hour|day',
         *              offset: 2|5
         *          },
         *          offsetStartGame: {
         *              unit: 'minute|hour|day',
         *              offset: 2|5
         *          }
         *      }
         * }
         * 
         * tombola:
         * {
         *      startDate: timestamp,
         *      endDate: timestamp
         *      targetDate: timestamp
         * }
         */
        definition: {
            type: DataTypes.TEXT,
            allowNull: false
        }
        
    }, {
        timestamps: false,
        tableName: 'scheduler_item',
        indexes: [
            {
                unique: true,
                fields:['itemId']
            }
        ],
        classMethods: {
            associate: function (models) {
                this.hasMany(models.schedulerEvent, { foreignKey: 'schedulerItemId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
