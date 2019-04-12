var StatisticsService = require('./../../services/statisticsService.js');

var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('gameHasPool', {
        gameId: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        poolId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'game_has_pool',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.game, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.pool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        },
        hooks:{
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
    tableName: 'game_has_pool',
    fieldNames: {
        game: 'gameId',
        question: 'poolId'
    }
}