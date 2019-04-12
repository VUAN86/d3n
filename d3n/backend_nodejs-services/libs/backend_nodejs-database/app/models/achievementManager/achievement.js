var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_ARCHIVED: 'archived'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('achievement', {
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
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ACTIVE,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_INACTIVE
        },
        imageId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        isTimeLimit: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        timePeriod: {
            type: DataTypes.INTEGER(11).UNSIGNED,
            allowNull: true
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
        tableName: 'achievement',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.achievementHasBadge, { foreignKey: 'achievementId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
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
