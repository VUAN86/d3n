var _constants = {
    TRIGGERED_BY_USER: 'user',
    TRIGGERED_BY_WORKFLOW: 'workflow'
};
var QuestionOrTranslationEventLog = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionOrTranslationEventLog', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        questionTranslationId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        triggeredBy: {
            type: DataTypes.ENUM(
                _constants.TRIGGERED_BY_USER,
                _constants.TRIGGERED_BY_WORKFLOW
            ),
            allowNull: false
        },
        resourceId: {
            type: DataTypes.STRING,
            allowNull: true
        },
        oldStatus: {
            type: DataTypes.STRING,
            allowNull: true
        },
        newStatus: {
            type: DataTypes.STRING,
            allowNull: true
        },
        reviewId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        issueId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        }
    },
    {
        timestamps: false,
        tableName: 'question_or_translation_event_log',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.questionOrTranslationReview, { foreignKey: 'reviewId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.questionOrTranslationIssue, { foreignKey: 'issueId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.questionTranslation, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};