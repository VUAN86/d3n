var ModelFactory = require('nodejs-utils').ModelUtils;

var _constants = {
    GAME_TYPE_DUEL: 'duel',
    GAME_TYPE_QUICK_QUIZ: 'quickQuiz',
    GAME_TYPE_QUIZ_BATTLE: 'quizBattle',
    GAME_TYPE_TOURNAMENT: 'tournament',
    QUESTION_TYPE_SPREAD_FIXED: 'fixed',
    QUESTION_TYPE_SPREAD_PERCENTAGE: 'percentage',
    COMPLEXITY_SPREAD_FIXED: 'fixed',
    COMPLEXITY_SPREAD_PERCENTAGE: 'percentage',
    QUESTION_TYPE_USAGE_RANDOM : 'random',
    QUESTION_TYPE_USAGE_ORDERED : 'ordered',
    QUESTION_SELECTION_RANDOM_PER_PLAYER : 'randomPerPlayer',
    QUESTION_SELECTION_SAME_FOR_EACH_PLAYER : 'sameForEachPlayer',
    OVER_WRITE_USAGE_REPEAT_QUESTIONS : 'repeatQuestions',
    OVER_WRITE_USAGE_ONLY_ONCE : 'onlyOnce',
    GAME_ENTRY_CURRENCY_MONEY : 'MONEY',
    GAME_ENTRY_CURRENCY_CREDIT : 'CREDIT',
    GAME_ENTRY_CURRENCY_BONUS : 'BONUS',
    DEFAULT_INSTANT_ANSWER_FEEDBACK_DELAY : 2, // 2 seconds
    ADS_FREQUENCY_BEFORE_THE_GAME: 'beforeTheGame',
    ADS_FREQUENCY_AFTER_THE_GAME: 'afterTheGame',
    ADS_FREQUENCY_AFTER_EACH_QUESTION: 'afterEachQuestion',
    ADS_FREQUENCY_AFTER_X_QUESTION: 'afterXQuestion',
    STATUS_INACTIVE: 'inactive',
    STATUS_ACTIVE: 'active',
    STATUS_DIRTY: 'dirty',
    STATUS_ARCHIVED: 'archived',
    ENTRY_FEE_TYPE_STEP: 'step',
    ENTRY_FEE_TYPE_FIXED: 'fixed',    
};

module.exports = function (sequelize, DataTypes) {
    return sequelize.define('game', {
        id: {
            type: DataTypes.INTEGER(11),
            primaryKey: true,
            autoIncrement: true
        },
        title: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        description: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        gameModuleId: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
        },
        gameType: {
            type: DataTypes.ENUM(
                _constants.GAME_TYPE_DUEL,
                _constants.GAME_TYPE_QUICK_QUIZ,
                _constants.GAME_TYPE_QUIZ_BATTLE,
                _constants.GAME_TYPE_TOURNAMENT
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
        gameTypeConfiguration: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('gameTypeConfiguration', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('gameTypeConfiguration')) {
                    return JSON.parse(this.getDataValue('gameTypeConfiguration'));
                } else {
                    return null;
                }
            }
        },
        jokerConfiguration: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('jokerConfiguration', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('jokerConfiguration')) {
                    return JSON.parse(this.getDataValue('jokerConfiguration'));
                } else {
                    return null;
                }
            }
        },
        resultConfiguration: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('resultConfiguration', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('resultConfiguration')) {
                    return JSON.parse(this.getDataValue('resultConfiguration'));
                } else {
                    return null;
                }
            }
        },
        playerEntryFeeType: {
            type: DataTypes.ENUM(
                _constants.ENTRY_FEE_TYPE_STEP,
                _constants.ENTRY_FEE_TYPE_FIXED
            ),
            allowNull: false,
            defaultValue: _constants.ENTRY_FEE_TYPE_STEP
        },
        playerEntryFeeSettings: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('playerEntryFeeSettings', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('playerEntryFeeSettings')) {
                    return JSON.parse(this.getDataValue('playerEntryFeeSettings'));
                } else {
                    return null;
                }
            }
        },
        playerEntryFeeValues: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('playerEntryFeeValues', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('playerEntryFeeValues')) {
                    return JSON.parse(this.getDataValue('playerEntryFeeValues'));
                } else {
                    return null;
                }
            }
        },
        isAutoValidationEnabled: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
            defaultValue: 1
        },
        startDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        endDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        repetition: { // repetition definition for tournament
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('repetition', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('repetition')) {
                    return JSON.parse(this.getDataValue('repetition'));
                } else {
                    return null;
                }
            }
        },
        isMoneyGame: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        isLiveGame: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        isInternationalGame: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        isTenantLocked: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        isRegionalLimitationEnabled: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
            defaultValue: 0
        },
        questionTypeSpread: {
            type: DataTypes.ENUM(
                _constants.QUESTION_TYPE_SPREAD_FIXED,
                _constants.QUESTION_TYPE_SPREAD_PERCENTAGE
            ),
            allowNull: true,
        },
        complexitySpread: {
            type: DataTypes.ENUM(
                _constants.COMPLEXITY_SPREAD_FIXED,
                _constants.COMPLEXITY_SPREAD_PERCENTAGE
            ),
            allowNull: true,
        },
        complexityLevel: {
            type: DataTypes.TEXT,
            allowNull: true,
            set: function (val) {
                this.setDataValue('complexityLevel', JSON.stringify(val));
            },
            get: function () {
                if (this.getDataValue('complexityLevel')) {
                    return JSON.parse(this.getDataValue('complexityLevel'));
                } else {
                    return null;
                }
            }
        },
        questionComplexity: {
              type: DataTypes.TEXT,
              allowNull: true,
              set: function (val) {
                  this.setDataValue('questionComplexity', JSON.stringify(val));
              },
              get: function () {
                  if (this.getDataValue('questionComplexity')) {
                      return JSON.parse(this.getDataValue('questionComplexity'));
                  } else {
                      return null;
                  }
              }
        },
        prizes: {
            type: DataTypes.TEXT,
            allowNull: true,
        },
        numberOfQuestionsToPlay: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        tournamentLimits: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        questionSelectionTill: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        questionTypeUsage: {
            type: DataTypes.ENUM(
                _constants.QUESTION_TYPE_USAGE_RANDOM,
                _constants.QUESTION_TYPE_USAGE_ORDERED
            ),
            allowNull: true,
        },
        questionSelection: {
            type: DataTypes.ENUM(
                _constants.QUESTION_SELECTION_RANDOM_PER_PLAYER,
                _constants.QUESTION_SELECTION_SAME_FOR_EACH_PLAYER
            ),
            allowNull: true,
        },
        minimumGameLevel: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        overWriteUsage: {
            type: DataTypes.ENUM(
                _constants.OVER_WRITE_USAGE_REPEAT_QUESTIONS,
                _constants.OVER_WRITE_USAGE_ONLY_ONCE
            ),
            allowNull: true,
        },
        waitTimeBetweenQuestions: {
            type: DataTypes.INTEGER(11),
            allowNull: true,
        },
        vibration: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        sound: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        abortGame: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        instantAnswerFeedback: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        instantAnswerFeedbackDelay: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        entryFeeDecidedByPlayer: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        userCanOverridePools: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
        },
        entryFeeBatchSize: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 1
        },
        isMultiplePurchaseAllowed: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 1
        },
        gameEntryAmount: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        gameEntryCurrency: {
            type: DataTypes.ENUM(
                _constants.GAME_ENTRY_CURRENCY_MONEY,
                _constants.GAME_ENTRY_CURRENCY_CREDIT,
                _constants.GAME_ENTRY_CURRENCY_BONUS
            ),
            allowNull: true,
        },
        isClosed: {
            type: DataTypes.INTEGER(1),
            allowNull: false,
            defaultValue: 0
        },
        advertisement: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
            defaultValue: 0
        },
        advertisementProviderId: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        adsFrequency: {
            type: DataTypes.ENUM(
                _constants.ADS_FREQUENCY_BEFORE_THE_GAME,
                _constants.ADS_FREQUENCY_AFTER_THE_GAME,
                _constants.ADS_FREQUENCY_AFTER_EACH_QUESTION,
                _constants.ADS_FREQUENCY_AFTER_X_QUESTION
            ),
            allowNull: true,
        },
        adsFrequencyAmount: {
            type: DataTypes.INTEGER(11),
            allowNull: true
        },
        hideCategories: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
            defaultValue: 0
        },
        iconId: {
            type: DataTypes.STRING,
            allowNull: true,
        },
        imageId: {
            type: DataTypes.STRING,
            allowNull: true,
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
        },
        isPromotion: {
            type: DataTypes.INTEGER(1),
            allowNull: true,
            defaultValue: 0
        },        
        promotionStartDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        promotionEndDate: {
            type: DataTypes.DATE,
            allowNull: true,
        },
        stat_players: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_gamesPlayed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }, 
        stat_adsViewed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_creditsPayed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0,
        },
        stat_moneyPayed: {
            type: DataTypes.DECIMAL(10,2),
            allowNull: false,
            defaultValue: '0.00',
        },
        stat_bonusPointsPaid: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_voucherWon: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
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
        stat_moneyWon: {
            type: DataTypes.DECIMAL(10,2),
            allowNull: false,
            defaultValue: '0.00',
        },
        stat_questionsAnswered: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsAnsweredRight: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_questionsAnsweredWrong: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_averageAnswerSpeed: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_poolsCount: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        },
        stat_adsCount: {
            type: DataTypes.INTEGER(11),
            allowNull: false,
            defaultValue: 0
        }
    }, {
        timestamps: true,
        createdAt: 'createDate',
        updatedAt: 'updateDate',
        tableName: 'game',
        classMethods: {
            associate: function (models) {
                this.belongsTo(models.advertisementProvider, { foreignKey: 'advertisementProviderId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.tenant, { foreignKey: 'tenantId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.gameModule, { foreignKey: 'gameModuleId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.belongsTo(models.media, { foreignKey: 'imageId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.applicationHasGame, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasPool, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasQuestionTemplate, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasRegionalSetting, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gameHasWinningComponent, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
                this.hasMany(models.gamePoolValidationResult, { foreignKey: 'gameId', onDelete: 'NO ACTION', onUpdate: 'NO ACTION' });
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
