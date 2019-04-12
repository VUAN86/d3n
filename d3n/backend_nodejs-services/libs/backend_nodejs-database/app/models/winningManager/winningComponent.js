var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    TYPE_CASINO_COMPONENT: 'CASINO_COMPONENT',
    TYPE_WORD_GUESSING_GAME: 'WORD_GUESSING_GAME',
    DEFAULT_ENTRY_FEE_CURRENCY_MONEY : 'MONEY',
    DEFAULT_ENTRY_FEE_CURRENCY_CREDIT : 'CREDIT',
    DEFAULT_ENTRY_FEE_CURRENCY_BONUS : 'BONUS',
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('winningComponent', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        tenantId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        title: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        rules: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_INACTIVE,
                _constants.STATUS_ACTIVE,
                _constants.STATUS_DIRTY,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_INACTIVE
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_CASINO_COMPONENT,
                _constants.TYPE_WORD_GUESSING_GAME
            ),
            allowNull: false
        },
        startDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        endDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        imageId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        prizeDescription: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        defaultIsFree: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        defaultEntryFeeAmount: {
            type: DataTypes.DECIMAL(10, 2),
            allowNull: true,
        },
        defaultEntryFeeCurrency: {
            type: DataTypes.ENUM(
                _constants.DEFAULT_ENTRY_FEE_CURRENCY_MONEY,
                _constants.DEFAULT_ENTRY_FEE_CURRENCY_CREDIT,
                _constants.DEFAULT_ENTRY_FEE_CURRENCY_BONUS
            ),
            allowNull: true,
        },
        winningConfiguration: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('winningConfiguration', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('winningConfiguration')) {
                    return JSON.parse(this.getDataValue('winningConfiguration'));
                } else {
                    return null;
                }
            }
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
        tableName: 'winning_component',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasWinningComponent, { foreignKey: 'winningComponentId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
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
