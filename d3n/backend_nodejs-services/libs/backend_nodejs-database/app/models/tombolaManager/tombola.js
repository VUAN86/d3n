var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    STATUS_EXPIRED: 'expired',
    PAYOUT_TARGET_PERCENT_OF_TICKETS_USED: 'PERCENT_OF_TICKETS_USED',
    PAYOUT_TARGET_TARGET_DATE: 'TARGET_DATE',
    CURRENCY_MONEY : 'MONEY',
    CURRENCY_CREDIT : 'CREDIT',
    CURRENCY_BONUS : 'BONUS'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('tombola', {
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
        winningRules: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        imageId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        startDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        endDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        preCloseOffsetMinutes: {
            type: DataTypes.INTEGER(11).UNSIGNED,
            allowNull: true
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ACTIVE,
                _constants.STATUS_DIRTY,
                _constants.STATUS_ARCHIVED,
                _constants.STATUS_EXPIRED
            ),
            allowNull: true,
            defaultValue: _constants.STATUS_INACTIVE
        },
        isOpenCheckout: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        playoutTarget: {
            type: DataTypes.ENUM(
                _constants.PAYOUT_TARGET_PERCENT_OF_TICKETS_USED,
                _constants.PAYOUT_TARGET_TARGET_DATE
            ),
            allowNull: true,
        },
        targetDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        percentOfTicketsAmount: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
            defaultValue: 0
        },
        totalTicketsAmount: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        currency: {
            type: DataTypes.ENUM(
                _constants.CURRENCY_MONEY,
                _constants.CURRENCY_CREDIT,
                _constants.CURRENCY_BONUS
            ),
            allowNull: true,
        },
        bundles: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('bundles', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('bundles')) {
                    return JSON.parse(this.getDataValue('bundles'));
                } else {
                    return null;
                }
            }
        },
        prizes: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('prizes', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('prizes')) {
                    return JSON.parse(this.getDataValue('prizes'));
                } else {
                    return null;
                }
            }
        },
        consolationPrize: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('consolationPrize', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('consolationPrize')) {
                    return JSON.parse(this.getDataValue('consolationPrize'));
                } else {
                    return null;
                }
            }
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
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
        tableName: 'tombola',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasTombola, { foreignKey: 'tombolaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.tombolaHasRegionalSetting, { foreignKey: 'tombolaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
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
