var _constants = {
    ERROR_TYPE_SPELLING: 'spelling',
    ERROR_TYPE_WRONG_ANSWER: 'wrong_answer',
    ERROR_TYPE_TOO_EASY: 'too_easy',
    ERROR_TYPE_TOO_COMPLICATED: 'too_complicated',
    ERROR_TYPE_WRONG_CATEGORY: 'wrong_category',
    ERROR_TYPE_OTHER: 'other',
    
    REVIEW_FOR_QUESTION: 'question',
    REVIEW_FOR_TRANSLATION: 'translation'
};
var QuestionOrTranslationReview = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionOrTranslationReview', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        reviewFor: {
            type: DataTypes.ENUM(
                _constants.REVIEW_FOR_QUESTION,
                _constants.REVIEW_FOR_TRANSLATION
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
        rating: {
            type: DataTypes.DECIMAL(2,1).UNSIGNED,
            allowNull: true
        },
        difficulty: {
            type: DataTypes.INTEGER(1).UNSIGNED,
            allowNull: true
        },
        isAccepted: {
            type: DataTypes.INTEGER(1).UNSIGNED,
            allowNull: false
        },
        errorType: {
            type: DataTypes.ENUM(
                _constants.ERROR_TYPE_SPELLING,
                _constants.ERROR_TYPE_WRONG_ANSWER,
                _constants.ERROR_TYPE_TOO_EASY,
                _constants.ERROR_TYPE_TOO_COMPLICATED,
                _constants.ERROR_TYPE_WRONG_CATEGORY,
                _constants.ERROR_TYPE_OTHER
            ),
            allowNull: true
        },
        errorText: {
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
        tableName: 'question_or_translation_review',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.questionTranslation, { foreignKey: 'questionTranslationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionOrTranslationEventLog, { foreignKey: 'reviewId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};