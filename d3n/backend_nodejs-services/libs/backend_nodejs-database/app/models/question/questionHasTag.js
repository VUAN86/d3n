var _constants = {};

var QuestionHasTag = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionHasTag', {
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        tagTag: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'question_has_tag',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tag, { foreignKey: 'tagTag', onDelete: 'NO ACTION', onUpdate: 'CASCADE' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
