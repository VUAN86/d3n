var _constants = {};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('badgeHasBadge', {
        badgeId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        },
        relatedBadgeId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            primaryKey: true,
        }
    }, {
        timestamps: false,
        tableName: 'badge_has_badge',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.badge, { foreignKey: 'badgeId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.badge, { foreignKey: 'relatedBadgeId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
