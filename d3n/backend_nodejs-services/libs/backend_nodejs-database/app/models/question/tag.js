var _constants = {};

var Tag = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tag', {
        tag: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        },
        isApproved: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        approvedDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        }
    },
    {
        timestamps: false,
        tableName: 'tag',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasTag, { foreignKey: 'tagTag', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionHasTag, { foreignKey: 'tagTag', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.mediaHasTag, { foreignKey: 'tagTag', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
