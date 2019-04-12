var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('gameHasQuestionTemplate', {
        gameId: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        questionTemplateId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        amount: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        order: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        }
    }, {
        timestamps: false,
        tableName: 'game_has_question_template',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.game, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.questionTemplate, { foreignKey: 'questionTemplateId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
