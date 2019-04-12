var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive'
};

var QuestionTemplate = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('questionTemplate', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        mediaId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_ACTIVE, 
                _constants.STATUS_INACTIVE
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_ACTIVE
        },
        isMultiStepQuestion: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        hasResolution: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        answerTemplates: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        structureDefinition: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function(val) {
                this.setDataValue('structureDefinition', JSON.stringify(val));
            },
            get: function() {
                if (this.getDataValue('structureDefinition')) {
                    return JSON.parse(this.getDataValue('structureDefinition'));
                } else {
                    return null;
                }
            }
        },
        help: {
            type: DataTypes.TEXT,
            allowNull: true
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        }
    },
    {
        timestamps: false,
        tableName: 'question_template',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.media, { foreignKey: 'mediaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.question, { foreignKey: 'questionTemplateId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasQuestionTemplate, { foreignKey: 'questionTemplateId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};