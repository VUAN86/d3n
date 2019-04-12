var StatisticsService = require('./../../services/statisticsService.js');
var _constants = {};

var PoolHasQuestion = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('poolHasQuestion', {
        poolId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'pool_has_question',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.pool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
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
    tableName: 'pool_has_question',
    fieldNames: {
        question: 'questionId',
        question_translation: 'questionId',
        pool: 'poolId'
    }
}