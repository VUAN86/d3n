var _constants = {};

var WorkorderHasQuestionExample = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('workorderHasQuestionExample', {
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        number: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        }
    },
    {
        timestamps: false,
        tableName: 'workorder_has_question_example',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
