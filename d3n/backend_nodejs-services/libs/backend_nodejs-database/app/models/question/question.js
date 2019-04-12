var StatisticsService = require('./../../services/statisticsService.js');
var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    STATUS_DRAFT: 'draft',
    STATUS_REVIEW: 'review',
    STATUS_APPROVED: 'approved',
    STATUS_DECLINED: 'declined',
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    PRIORITY_HIGH: 'high'
};

var Question = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('question', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        questionTemplateId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        complexity: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        accessibleDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        expirationDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        renewDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_DRAFT,
                _constants.STATUS_REVIEW,
                _constants.STATUS_APPROVED,
                _constants.STATUS_DECLINED,
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ACTIVE,
                _constants.STATUS_DIRTY,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_DRAFT
        },
        exampleQuestionId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        resolutionImageId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        primaryRegionalSettingId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        isInternational: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        rating: {
            type: DataTypes.FLOAT(),
            allowNull: true
        },
        source: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        isTranslationNeeded: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        priority: {
            type: DataTypes.ENUM(
                _constants.PRIORITY_HIGH
            ),
            allowNull: true,
            defaultValue: null
        },
        creatorResourceId: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        updaterResourceId: {
            type: DataTypes.STRING,
            allowNull: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
        updateDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        version: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 1
        },
        publishedVersion: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        publishingDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        isActivatedManually: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        isEscalated: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        stat_associatedPools: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_associatedGames: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        } 
    },
    {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'question',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.question, { as: 'exampleQuestions', foreignKey: 'exampleQuestionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasQuestion, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionHasRelatedQuestion, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionHasRelatedQuestion, { foreignKey: 'relatedQuestionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionHasTag, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionHasRegionalSetting, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionTranslation, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationEventLog, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationIssue, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationReview, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasQuestionExample, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentAction, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.question, { foreignKey: 'exampleQuestionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.questionTemplate, { foreignKey: 'questionTemplateId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.regionalSetting, { foreignKey: 'primaryRegionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.media, { foreignKey: 'resolutionImageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        },
        hooks: {
            beforeUpdate: function(record, options) {
                return ModelFactory.beforeUpdate(record, options);
            },
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
    tableName: 'question',
    fieldNames: {
        pool: 'id',
        workorder: 'workorderId'
    }
}