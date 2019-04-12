var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
};

var Application = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('application', {
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
        configuration: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function(val) {
                this.setDataValue('configuration', JSON.stringify(val));
            },
            get: function() {
                if (this.getDataValue('configuration')) {
                    return JSON.parse(this.getDataValue('configuration'));
                } else {
                    return null;
                }
            }
        },
        deployment: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('deployment', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('deployment')) {
                    return JSON.parse(this.getDataValue('deployment'));
                } else {
                    return null;
                }
            }
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
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        releaseDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        versionInfo: {
            type: DataTypes.STRING,
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
        },
        stat_games: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesPlayed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_players: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_friendsInvited: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_quizGames: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_moneyGames: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_bettingGames: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_downloads: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_installations: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_deinstallations: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_moneyCharged: {
            type: DataTypes.DECIMAL(10,2),
            allowNull: false,
            defaultValue: '0.00',
        },
        stat_creditsPurchased: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_moneyWon: {
            type: DataTypes.DECIMAL(10,2),
            allowNull: false,
            defaultValue: '0.00',
        },
        stat_bonusPointsWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_creditsWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_voucherWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_adsViewed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }
    }, {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'application',
        indexes: [
            {unique: true, fields: ['tenantId', 'title']}
        ],
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasGame, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasGameModule, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasApplicationAnimation, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasApplicationCharacter, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasPaymentType, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasRegionalSetting, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasPromocodeCampaign, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.profileHasApplication, { foreignKey: 'applicationId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
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
