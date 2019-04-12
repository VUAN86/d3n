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
    STATUS_AUTOMATIC_TRANSLATION: 'automatic_translation',
    PRIORITY_HIGH: 'high'
};

var QuestionTranslation = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionTranslation', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        approverResourceId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        approveDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        blockerResourceId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        blockDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        languageId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
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
                _constants.STATUS_ARCHIVED,
                _constants.STATUS_AUTOMATIC_TRANSLATION
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_DRAFT
        },
        priority: {
            type: DataTypes.ENUM(
                _constants.PRIORITY_HIGH
            ),
            allowNull: true,
            defaultValue: null
        },
        resolutionText: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        explanation: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        hint: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        content:  {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('content', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('content')) {
                    return JSON.parse(this.getDataValue('content'));
                } else {
                    return null;
                }
            }
        },
        questionBlobKeys:  {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('questionBlobKeys', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('questionBlobKeys')) {
                    return JSON.parse(this.getDataValue('questionBlobKeys'));
                } else {
                    return null;
                }
            }
        },
        publishIndex: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        publishMultiIndex: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        publishLanguageIndex:  {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('publishLanguageIndex', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('publishLanguageIndex')) {
                    return JSON.parse(this.getDataValue('publishLanguageIndex'));
                } else {
                    return null;
                }
            }
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
        },
        stat_gamesPlayed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_rightAnswers: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_wrongAnswers: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_averageAnswerSpeed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_rating: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }
    },
    {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'question_translation',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.language, { foreignKey: 'languageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentAction, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationEventLog, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationIssue, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationReview, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        },
        hooks: {
            beforeUpdate: function(record, options) {
                record.blockerResourceId = null;
                record.blockDate = null;
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
    tableName: 'question_translation',
    fieldNames: {
        workorder: 'workorderId',
        pool: 'id'
    }
}