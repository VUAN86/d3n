var _constants = {
    LIVE_MESSAGE_CURRENCY_MONEY : 'MONEY',
    LIVE_MESSAGE_CURRENCY_CREDIT : 'CREDIT',
    LIVE_MESSAGE_CURRENCY_BONUS : 'BONUS'
};

var LiveMessage = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('liveMessage', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        applicationId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        creatorResourceId: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        updaterResourceId: {
            type: DataTypes.STRING,
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
        content: {
            type: DataTypes.TEXT,
            allowNull: false,
        },
        sendingCompleteDate: {
            type: DataTypes.DATE,
            allowNull: true,
            defaultValue: sequelize.NOW
        },
        filter: {
            type: DataTypes.TEXT,
            allowNull: false,
        },
        sms: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        email: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        inApp: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        push: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        incentive: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        incentiveAmount: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        incentiveCurrency: {
            type: DataTypes.ENUM(
                _constants.LIVE_MESSAGE_CURRENCY_MONEY,
                _constants.LIVE_MESSAGE_CURRENCY_CREDIT,
                _constants.LIVE_MESSAGE_CURRENCY_BONUS
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_INACTIVE
        }
    }, {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'live_message',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.application, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
