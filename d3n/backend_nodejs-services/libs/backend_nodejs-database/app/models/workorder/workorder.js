var _constants = {
    STATUS_DRAFT: 'draft',
    STATUS_INPROGRESS: 'inprogress',
    STATUS_INACTIVE: 'inactive',
    STATUS_BILLING: 'billing',
    STATUS_COMPLETE: 'complete',
    STATUS_CLOSED: 'closed',
    STATUS_ARCHIVED: 'archived',
    TYPE_QUESTION: 'question',
    TYPE_TRANSLATION: 'translation',
    TYPE_MEDIA: 'media',
    COMMUNITY_PROMOTION_CURRENCY_MONEY: 'MONEY',
    COMMUNITY_PROMOTION_CURRENCY_CREDIT: 'CREDIT',
    COMMUNITY_PROMOTION_CURRENCY_BONUS: 'BONUS',
};

var Workorder = module.exports = function (sequelize, DataTypes) {
    return sequelize.define('workorder', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        ownerResourceId: {
            type: DataTypes.STRING,
            allowNull: false,
        },
        createDate: {
            type: DataTypes.DATE,
            allowNull: false,
            defaultValue: sequelize.NOW
        },
        title: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        questionCreatePaymentStructureId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        questionReviewPaymentStructureId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        translationCreatePaymentStructureId: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        translationReviewPaymentStructureId: {
            type: DataTypes.INTEGER(11),
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
        status: {
            type: DataTypes.ENUM(
                _constants.STATUS_DRAFT,
                _constants.STATUS_INPROGRESS,
                _constants.STATUS_INACTIVE,
                _constants.STATUS_BILLING,
                _constants.STATUS_COMPLETE,
                _constants.STATUS_CLOSED,
                _constants.STATUS_ARCHIVED
            ),
            allowNull: false,
            defaultValue: _constants.STATUS_DRAFT
        },
        iconId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        color: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        itemsRequired: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        type: {
            type: DataTypes.ENUM(
                _constants.TYPE_QUESTION,
                _constants.TYPE_TRANSLATION,
                _constants.TYPE_MEDIA
            ),
            allowNull: true
        },
        primaryRegionalSettingId: {
            type: DataTypes.INTEGER(11),
            allowNull: false
        },
        questionCreateIsCommunity: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        questionReviewIsCommunity: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        translationCreateIsCommunity: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        translationReviewIsCommunity: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        isCommunityPromoted: {
            type: DataTypes.INTEGER(1),
            allowNull: true
        },
        communityPromotionFromDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        communityPromotionToDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        communityPromotionCurrency: {
            type: DataTypes.ENUM(
                _constants.COMMUNITY_PROMOTION_CURRENCY_MONEY,
                _constants.COMMUNITY_PROMOTION_CURRENCY_CREDIT,
                _constants.COMMUNITY_PROMOTION_CURRENCY_BONUS
            ),
            allowNull: true
        },
        communityPromotionAmount: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        communityPromotionDescription: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        stat_questionsRequested: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsCompleted: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsInProgress: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_progressPercentage: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsInTranslation: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionTranslationsCompleted: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionTranslationsInProgress: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_translationsProgressPercentage: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsPublished: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_translationsPublished: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsComplexityLevel1: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsComplexityLevel2: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsComplexityLevel3: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsComplexityLevel4: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_numberOfLanguages: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }
    },
    {
        timestamps: false,
        tableName: 'workorder',
        classMethods: {
            associate: function (models) {
                this.hasMany(models.question, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.media, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.questionTranslation, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasPool, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasRegionalSetting, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasQuestionExample, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasResource, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.workorderHasTenant, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentAction, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.paymentResourceBillOfMaterial, { foreignKey: 'workorderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentStructure, { as: 'questionCreatePaymentStructure', foreignKey: 'questionCreatePaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentStructure, { as: 'questionReviewPaymentStructure', foreignKey: 'questionReviewPaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentStructure, { as: 'translationCreatePaymentStructure', foreignKey: 'translationCreatePaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.paymentStructure, { as: 'translationReviewPaymentStructure', foreignKey: 'translationReviewPaymentStructureId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.regionalSetting, { foreignKey: 'primaryRegionalSettingId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
            },
            constants: function () {
                return _constants;
            }
        }
    });
};
