var _constants = {
    ACTION_QUESTION_CREATE: 'questionCreate',
    ACTION_QUESTION_REVIEW: 'questionReview',
    ACTION_TRANSLATION_CREATE: 'translationCreate',
    ACTION_TRANSLATION_REVIEW: 'translationReview'
};

var WorkorderHasResource = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('workorderHasResource', {
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        resourceId: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        },
        action: {
            type: DataTypes.ENUM(
                _constants.ACTION_QUESTION_CREATE,
                _constants.ACTION_QUESTION_REVIEW,
                _constants.ACTION_TRANSLATION_CREATE,
                _constants.ACTION_TRANSLATION_REVIEW
            ),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'workorder_has_resource',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
