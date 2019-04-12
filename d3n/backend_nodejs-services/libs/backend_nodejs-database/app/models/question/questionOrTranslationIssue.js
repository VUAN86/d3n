var _constants = {
    ISSUE_TYPE_SPELLING_AND_GRAMMAR: 'spelling_and_grammar',
    ISSUE_TYPE_NOT_CLEAR: 'not_clear',
    ISSUE_TYPE_WRONG_ANSWER: 'wrong_answer',
    ISSUE_TYPE_WRONG_COMPLEXITY: 'wrong_complexity',
    ISSUE_TYPE_WRONG_CATEGORY: 'wrong_category',
    ISSUE_TYPE_OFFENSIVE_CONTENT: 'offensive_content',
    ISSUE_TYPE_OTHER: 'other',
        
    ISSUE_FOR_QUESTION: 'question',
    ISSUE_FOR_TRANSLATION: 'translation'
};
var QuestionOrTranslationIssue = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionOrTranslationIssue', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        issueFor: {
            type: DataTypes.ENUM(
                _constants.ISSUE_FOR_QUESTION,
                _constants.ISSUE_FOR_TRANSLATION
            ),
            allowNull: false
        },
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        questionTranslationId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        version: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        resourceId: {
            type: DataTypes.STRING,
            allowNull: false
        },
        issueType: {
            type: DataTypes.ENUM(
                _constants.ISSUE_TYPE_SPELLING_AND_GRAMMAR,
                _constants.ISSUE_TYPE_NOT_CLEAR,
                _constants.ISSUE_TYPE_WRONG_ANSWER,
                _constants.ISSUE_TYPE_WRONG_COMPLEXITY,
                _constants.ISSUE_TYPE_WRONG_CATEGORY,
                _constants.ISSUE_TYPE_OFFENSIVE_CONTENT,
                _constants.ISSUE_TYPE_OTHER
            ),
            allowNull: false
        },
        issueText: {
            type: DataTypes.TEXT,
            allowNull: true
        },
        deleted: {
            type: DataTypes.INTEGER(1).UNSIGNED,
            allowNull: false,
            defaultValue: 0
        }
    },
    {
        timestamps: false,
        tableName: 'question_or_translation_issue',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.questionTranslation, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationEventLog, { foreignKey: 'issueId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};