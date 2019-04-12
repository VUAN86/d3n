var StatisticsService = require('./../../services/statisticsService.js');
var _constants = {};

var WorkorderHasPool = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('workorderHasPool', {
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        poolId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'workorder_has_pool',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.pool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        },
        hooks: {
            afterCreate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterBulkCreate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterUpdate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterBulkUpdate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterDestroy: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterBulkDestroy: StatisticsService.prototype.handleEvent.bind(statisticsContext),
        }
    });
};

var statisticsContext = {
    tableName: 'workorder_has_pool',
    fieldNames: {
        pool: 'poolId'
    }
}