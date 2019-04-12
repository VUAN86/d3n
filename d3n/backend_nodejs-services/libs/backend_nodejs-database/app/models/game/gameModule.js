var _constants = {};

var GameModule = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('gameModule', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: false
        },
        parentId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        key: {
            type: DataTypes.STRING,
            allowNull: true
        }
    },
    {
        timestamps: false,
        tableName: 'game_module',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.applicationHasGameModule, { foreignKey: 'gameModuleId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.game, { foreignKey: 'gameModuleId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameModule, { as: 'subModules', foreignKey: 'parentId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.gameModule, { foreignKey: 'parentId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
