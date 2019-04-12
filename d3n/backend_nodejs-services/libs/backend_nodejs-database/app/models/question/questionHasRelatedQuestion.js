var _constants = {};

var QuestionHasRelatedQuestion = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionHasRelatedQuestion', {
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        relatedQuestionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'question_has_related_question',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.question, { foreignKey: 'relatedQuestionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
