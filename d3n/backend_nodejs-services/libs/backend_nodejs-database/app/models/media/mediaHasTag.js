var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('mediaHasTag', {
        mediaId: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true,
        },
        tagTag: {
            type: DataTypes.STRING,
            allowNull: false,
            primaryKey: true
        }
    },
    {
        timestamps: false,
        tableName: 'media_has_tag',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.media, { foreignKey: 'mediaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tag, { foreignKey: 'tagTag', onDelete: 'NO ACTION', onUpdate: 'CASCADE' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
