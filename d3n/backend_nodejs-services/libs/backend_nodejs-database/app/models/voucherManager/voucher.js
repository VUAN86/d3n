var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    TYPE_SAVING: 'saving',
    TYPE_GIFT: 'gift',
    TYPE_PURCHASE: 'purchase',
    TYPE_CROSS: 'cross',
    TYPE_CASH: 'cash',
    CATEGORY_FASHION: 'Fashion',
    CATEGORY_TRAVEL: 'Travel',
    CATEGORY_GOING_OUT: 'Going Out',
    CATEGORY_HOME_AND_GARDEN: 'Home and Garden',
    CATEGORY_GIFT_AND_FLOWERS: 'Gifts & Flowers',
    CATEGORY_LIFESTYLE: 'Lifestyle',
    CATEGORY_CAR_AND_MOTIVE: 'Car & Motive',
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    CODE_TYPE_UNIQUE: 'unique',
    CODE_TYPE_FIXED: 'fixed'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('voucher', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        name: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        company: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        shortTitle: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_SAVING,
                _constants.TYPE_GIFT,
                _constants.TYPE_PURCHASE,
                _constants.TYPE_CROSS,
                _constants.TYPE_CASH
            ),
            allowNull: true,
        },
        category: {
            type: DataTypes.ENUM(
                _constants.CATEGORY_FASHION,
                _constants.CATEGORY_TRAVEL,
                _constants.CATEGORY_GOING_OUT,
                _constants.CATEGORY_HOME_AND_GARDEN,
                _constants.CATEGORY_GIFT_AND_FLOWERS,
                _constants.CATEGORY_LIFESTYLE,
                _constants.CATEGORY_CAR_AND_MOTIVE
            ),
            allowNull: true,
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ACTIVE,
                _constants.STATUS_DIRTY,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: true,
            defaultValue: _constants.STATUS_INACTIVE
        },
        codeType: {
              type: DataTypes.ENUM(
                  _constants.CODE_TYPE_UNIQUE,
                  _constants.CODE_TYPE_FIXED
              ),
              allowNull: true,
        },
        isSpecialPrice: {
              type: DataTypes.INTEGER(1),
              allowNull: true,
        },
        isQRCode: {
              type: DataTypes.INTEGER(1),
              allowNull: true,
        },
        isExchange: {
              type: DataTypes.INTEGER(1),
              allowNull: true,
        },
        isTombolaPrize: {
              type: DataTypes.INTEGER(1),
              allowNull: true,
        },
        bonuspointsCosts: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        redemptionURL: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        winningCondition: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        miniImage: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        normalImage: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        bigImage: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        expirationDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        voucherProviderId: {
          type: DataTypes.INTEGER(11),
          allowNull: false,
        },
        stat_inventoryCount: {
          type: DataTypes.INTEGER(11),
          allowNull: false,
          defaultValue: 0
        },
        stat_won: {
          type: DataTypes.INTEGER(11),
          allowNull: false,
          defaultValue: 0
        },
        stat_archieved: {
          type: DataTypes.INTEGER(11),
          allowNull: false,
          defaultValue: 0
        },
        stat_specialPrizes: {
          type: DataTypes.INTEGER(11),
          allowNull: false,
          defaultValue: 0
        }
    }, {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'voucher',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.voucherProvider, { foreignKey: 'voucherProviderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
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
