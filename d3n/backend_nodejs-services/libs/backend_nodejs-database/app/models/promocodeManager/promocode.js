var _constants = {
    USAGE_ONCE_PER_PLAYER: 'oncePerPlayer',
    USAGE_MULTI_PER_PLAYER: 'multiPerPlayer'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('promocode', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        promocodeCampaignId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        isUnique: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 1
        },
        number: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 1
        },
        code: {
            type: DataTypes.STRING,
            allowNull: false
        },
        isQRCodeRequired: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        usage: {
            type: DataTypes.ENUM(
                _constants.USAGE_ONCE_PER_PLAYER,
                _constants.USAGE_MULTI_PER_PLAYER
            ),
            allowNull: true
        },
        numberOfUses: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        }
    }, {
        timestamps: false,
        tableName: 'promocode',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.promocodeCampaign, { foreignKey: 'promocodeCampaignId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
