var _constants = {
    CONTENT_TYPE_IMAGE: 'image',
    CONTENT_TYPE_AUDIO: 'audio',
    CONTENT_TYPE_VIDEO: 'video',

    NAMESPACE_APP: 'app',
    NAMESPACE_LANGUAGE: 'language',      // not used, only original resolution
    NAMESPACE_VOUCHER: 'voucher',
    NAMESPACE_VOUCHER_BIG: 'voucherBig',
    NAMESPACE_QUESTION: 'question',
    NAMESPACE_ANSWER: 'answer',
    NAMESPACE_GAME: 'game',
    NAMESPACE_GAME_TYPE: 'gameType',
    NAMESPACE_AD: 'ad',
    NAMESPACE_TOMBOLA: 'tombola',
    NAMESPACE_TOMBOLA_BUNDLES: 'tombolaBundles',
    NAMESPACE_PROMO: 'promo',
    NAMESPACE_POOLS: 'pools',
    NAMESPACE_TENANT_LOGO: 'tenantLogo',
    NAMESPACE_ACHIEVEMENT: 'achievement',
    NAMESPACE_BADGE: 'badge',
    NAMESPACE_ICON: 'icon',
    NAMESPACE_SHOP_ARTICLE: 'shopArticle',
    NAMESPACE_INTRO: 'intro',

    /*
     * Profile and Group images are not stored in mysql but directly by 
     * their userId an groupId
     */
    NAMESPACE_PROFILE: 'profile',
    NAMESPACE_GROUP: 'group'
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('media', {
        id: {
            type: DataTypes.STRING,
            primaryKey: true
        },
        parentId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        workorderId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        metaData: {
            type: DataTypes.TEXT('medium'),
            allowNull: false,
            comment: "JSON object that contains object specific metadata",
            set: function(val) {
                this.setDataValue('metaData', JSON.stringify(val));
            },
            get: function() {
                if (this.getDataValue('metaData')) {
                    return JSON.parse(this.getDataValue('metaData'));
                } else {
                    return null;
                }
            }
        },
        namespace: {
          type: DataTypes.ENUM(
              _constants.NAMESPACE_APP,
              _constants.NAMESPACE_LANGUAGE,
              _constants.NAMESPACE_VOUCHER,
              _constants.NAMESPACE_VOUCHER_BIG,
              _constants.NAMESPACE_PROFILE,
              _constants.NAMESPACE_QUESTION,
              _constants.NAMESPACE_ANSWER,
              _constants.NAMESPACE_GAME,
              _constants.NAMESPACE_GAME_TYPE,
              _constants.NAMESPACE_AD,
              _constants.NAMESPACE_TOMBOLA,
              _constants.NAMESPACE_TOMBOLA_BUNDLES,
              _constants.NAMESPACE_PROMO,
              _constants.NAMESPACE_POOLS,
              _constants.NAMESPACE_TENANT_LOGO,
              _constants.NAMESPACE_GROUP,
              _constants.NAMESPACE_ACHIEVEMENT,
              _constants.NAMESPACE_BADGE,
              _constants.NAMESPACE_ICON,
              _constants.NAMESPACE_SHOP_ARTICLE,
              _constants.NAMESPACE_INTRO
          ),
          allowNull: false,
          comment: "Namespace identifier"
        },
        contentType: {
            type: DataTypes.ENUM(
                _constants.CONTENT_TYPE_IMAGE,
                _constants.CONTENT_TYPE_AUDIO,
                _constants.CONTENT_TYPE_VIDEO
            ),
            allowNull: false,
            comment: "Media type: image, audio, video"
        },
        type: {
            type: DataTypes.STRING,
            allowNull: false,
            defaultValue: "original",
            comment: "Type identifier string for the media: original, mask, ..."
        },
        title: {
            type: DataTypes.STRING,
            allowNull: true
        },
        description: {
            type: DataTypes.STRING,
            allowNull: true
        },
        source: {
            type: DataTypes.STRING,
            allowNull: true
        },
        copyright: {
            type: DataTypes.STRING,
            allowNull: true
        },
        licence: {
            type: DataTypes.STRING,
            allowNull: true
        },
        usageInformation: {
            type: DataTypes.STRING,
            allowNull: true
        },
        validFromDate: {
            type: DataTypes.DATE,
            allowNull: true,
            comment: "Validity of the media starts from this date and time"
        },
        validToDate: {
            type: DataTypes.DATE,
            allowNull: true,
            comment: "Validity of the media ends at this date and time"
        },
        originalFilename: {
            type: DataTypes.STRING,
            allowNull: true
        },
        creatorResourceId: {
            type: DataTypes.STRING,
            allowNull: true
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.literal('CURRENT_TIMESTAMP'),
        }
    },
    {
        timestamps: false,
        tableName: 'media',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.workorder, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.media, { foreignKey: 'parentId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.media, { as: 'subMedias', foreignKey: 'parentId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.poolHasMedia, { foreignKey: 'mediaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.mediaHasTag, { foreignKey: 'mediaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionTemplate, { foreignKey: 'mediaId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.question, { foreignKey: 'resolutionImageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.game, { foreignKey: 'imageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
