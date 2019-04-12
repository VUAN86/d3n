var _constants = {
    STATUS_ACTIVE: 'active',
    STATUS_INACTIVE: 'inactive'
};

var Pool = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('pool', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        creatorResourceId: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        description: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        parentPoolId: {
            type: DataTypes.INTEGER(11),
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
        iconId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        color: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        minimumQuestions: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        minimumQuestionsPerLanguage: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        minimumQuestionsPerRegion: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        stat_assignedWorkorders: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_assignedQuestions: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }, 
        stat_questionsComplexityLevel1: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsComplexityLevel2: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsComplexityLevel3: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsComplexityLevel4: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_regionsSupported: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_numberOfLanguages: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }
    },
    {
        timestamps: false,
        tableName: 'pool',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.pool, { foreignKey: 'parentPoolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.pool, { as: 'subPools', foreignKey: 'parentPoolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasPool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasTenant, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasTag, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasQuestion, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasMedia, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasPool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};