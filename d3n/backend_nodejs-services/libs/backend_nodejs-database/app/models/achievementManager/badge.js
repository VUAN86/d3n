var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_ARCHIVED: 'archived',
    TYPE_GAME : 'Game',
    TYPE_COMMUNITY : 'Community',
    TYPE_BATTLE : 'Battle',
    TIME_PERMANENT: 'Permanent',
    TIME_TEMPORARY: 'Temporary',
    USAGE_SINGLE: 'Single',
    USAGE_MULTIPLE: 'Multiple'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('badge', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_GAME,
                _constants.TYPE_COMMUNITY,
                _constants.TYPE_BATTLE
            ),
            allowNull: false,
            defaultValue: _constants.TYPE_GAME
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ACTIVE,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_INACTIVE
        },
        time: {
            type: DataTypes.ENUM(
                _constants.TIME_PERMANENT,
                _constants.TIME_TEMPORARY
            ),
            allowNull: false,
            defaultValue: _constants.TIME_PERMANENT
        },
        timePeriod: {
            type: DataTypes.INTEGER(11).UNSIGNED,
            allowNull: true
        },
        imageId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        usage: {
            type: DataTypes.ENUM(
                _constants.USAGE_SINGLE,
                _constants.USAGE_MULTIPLE
            ),
            allowNull: false,
            defaultValue: _constants.USAGE_SINGLE
        },
        isReward: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        bonusPointsReward: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
            defaultValue: 0
        },
        creditReward: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
            defaultValue: 0
        },
        paymentMultiplier: {
            type: DataTypes.FLOAT(),
            allowNull: true,
            defaultValue: 1,
        },
        accessRules: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('accessRules', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('accessRules')) {
                    return JSON.parse(this.getDataValue('accessRules'));
                } else {
                    return null;
                }
            }
        },
        earningRules: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('earningRules', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('earningRules')) {
                    return JSON.parse(this.getDataValue('earningRules'));
                } else {
                    return null;
                }
            }
        },
        messaging: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('messaging', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('messaging')) {
                    return JSON.parse(this.getDataValue('messaging'));
                } else {
                    return null;
                }
            }
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
        updateDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        version: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 1
        },
        publishedVersion: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        publishingDate: {
            type: DataTypes.DATE,
            allowNull: true,
        }
    }, {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'badge',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.badgeHasBadge, { foreignKey: 'badgeId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.badgeHasBadge, { as: 'relatedBadges', foreignKey: 'relatedBadgeId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        },
        hooks: {
            beforeUpdate: function(record, options) {
                return ModelFactory.beforeUpdate(record, options);
            }
        }
    });
};
