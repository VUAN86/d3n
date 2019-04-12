var _constants = {};
var QuestionRating = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionRating', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        questionId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        rating: {
            type: DataTypes.FLOAT(1),
            allowNull: false
        },
        reason: {
            type: DataTypes.STRING,
            allowNull: false
        },
        resourceId: {
            type: DataTypes.STRING,
            allowNull: false
        }
        
    },
    {
        timestamps: false,
        tableName: 'question_rating',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.question, { foreignKey: 'questionId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });                
            },
            constants: function () {
                return _constants;
            }
        }
    });
};