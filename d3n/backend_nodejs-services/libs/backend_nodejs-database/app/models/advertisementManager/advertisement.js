var StatisticsService = require('./../../services/statisticsService.js');
var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    TYPE_PICTURE: 'picture',
    TYPE_TEXT: 'text', 
    TYPE_VIDEO: 'video',
    TYPE_PICTURE_VIDEO: 'picture_video',
    
    CATEGORY_FASHION: 'fashion',
    CATEGORY_TRAVEL: 'travel',
    CATEGORY_GOING_OUT: 'going_out',
    CATEGORY_HOME_GARDEN: 'home_garden',
    CATEGORY_GIFT_FLOWERS: 'gifts_flowers',
    CATEGORY_HEALTH_BEAUTY: 'health_beauty',
    CATEGORY_TECHNOLOGY: 'technology',
    CATEGORY_SPORTS: 'sports',
    CATEGORY_OUTDOOR: 'outdoor',
    CATEGORY_UTILITIES: 'utilities',
    CATEGORY_FOOD_DRINKS: 'food_drinks',
    CATEGORY_MEDIA: 'media',
    CATEGORY_FITNESS: 'fitness',
    CATEGORY_FINANCE: 'finance',
    CATEGORY_BABY_CHILD: 'baby_child',
    CATEGORY_BUSINESS: 'business',
    CATEGORY_ENTERTAINMENT: 'entertainment',
    CATEGORY_GAMBLING: 'gambling',
    
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    
    USAGE_RANDOM: 'random',
    USAGE_FIXED: 'fixed'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('advertisement', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        company: {
            type: DataTypes.STRING,
            allowNull: true
        },
        summary: {
            type: DataTypes.STRING,
            allowNull: true
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_PICTURE,
                _constants.TYPE_TEXT,
                _constants.TYPE_VIDEO,
                _constants.TYPE_PICTURE_VIDEO
            ),
            allowNull: true
        },
        category: {
            type: DataTypes.ENUM(
                _constants.CATEGORY_FASHION,
                _constants.CATEGORY_TRAVEL,
                _constants.CATEGORY_GOING_OUT,
                _constants.CATEGORY_HOME_GARDEN,
                _constants.CATEGORY_GIFT_FLOWERS,
                _constants.CATEGORY_HEALTH_BEAUTY,
                _constants.CATEGORY_TECHNOLOGY,
                _constants.CATEGORY_SPORTS,
                _constants.CATEGORY_OUTDOOR,
                _constants.CATEGORY_UTILITIES,
                _constants.CATEGORY_FOOD_DRINKS,
                _constants.CATEGORY_MEDIA,
                _constants.CATEGORY_FITNESS,
                _constants.CATEGORY_FINANCE,
                _constants.CATEGORY_BABY_CHILD,
                _constants.CATEGORY_BUSINESS,
                _constants.CATEGORY_ENTERTAINMENT,
                _constants.CATEGORY_GAMBLING
            ),
            allowNull: true
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
        usage: {
            type: DataTypes.ENUM(
                _constants.USAGE_RANDOM,
                _constants.USAGE_FIXED
            ),
            allowNull: true
        },
        earnCredits: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        creditsAmount: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        videoAvailable: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        videoURL: {
            type: DataTypes.STRING,
            allowNull: true
        },
        campaignURL: {
            type: DataTypes.STRING,
            allowNull: true
        },
        linkedVoucherId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        miniImage: {
            type: DataTypes.STRING,
            allowNull: true
        },
        normalImage: {
            type: DataTypes.STRING,
            allowNull: true
        },
        emailImage: {
            type: DataTypes.STRING,
            allowNull: true
        },
        advertisementProviderId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        publishIdx: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        startDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        endDate: {
            type: DataTypes.DATE,
            allowNull: true
        },
        title: {
            type: DataTypes.STRING,
            allowNull: true
        },
        text: {
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
        stat_appsUsed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesUsed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_views: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_earnedCredits: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }
    }, {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'advertisement',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.advertisementProvider, { foreignKey: 'advertisementProviderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.voucher, { foreignKey: 'linkedVoucherId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        },
        hooks: {
            beforeUpdate: function(record, options) {
                return ModelFactory.beforeUpdate(record, options);
            },
            afterCreate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterBulkCreate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterUpdate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterBulkUpdate: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterDestroy: StatisticsService.prototype.handleEvent.bind(statisticsContext),
            afterBulkDestroy: StatisticsService.prototype.handleEvent.bind(statisticsContext),
        }
    });
};

var statisticsContext = {
    tableName: 'advertisement',
    fieldNames: {
        game: 'advertisementProviderId'
    }
}