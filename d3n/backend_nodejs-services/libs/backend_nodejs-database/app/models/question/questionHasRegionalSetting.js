var _constants = {};

var QuestionHasRegionalSetting = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionHasRegionalSetting', {
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        regionalSettingId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'question_has_regional_setting',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.regionalSetting, { foreignKey: 'regionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
