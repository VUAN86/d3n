var _constants = {};

var PoolHasTag = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('poolHasTag', {
        poolId: {
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
        tableName: 'pool_has_tag',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.pool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tag, { foreignKey: 'tagTag', onDelete: 'NO ACTION', onUpdate: 'CASCADE' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
