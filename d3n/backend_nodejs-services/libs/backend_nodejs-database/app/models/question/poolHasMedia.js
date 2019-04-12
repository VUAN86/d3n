var _constants = {};

var PoolHasMedia = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('poolHasMedia', {
        poolId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        mediaId: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        }
    },
    {
        timestamps: false,
        tableName: 'pool_has_media',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.pool, { foreignKey: 'poolId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.media, { foreignKey: 'mediaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
