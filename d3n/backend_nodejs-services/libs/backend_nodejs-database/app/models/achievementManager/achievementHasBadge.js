var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('achievementHasBadge', {
        achievementId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        badgeId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'achievement_has_badge',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.achievement, { foreignKey: 'achievementId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.badge, { foreignKey: 'badgeId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
