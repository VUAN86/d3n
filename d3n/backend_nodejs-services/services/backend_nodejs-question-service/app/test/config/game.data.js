var _ = require('lodash');
var Config = require('./../../config/config.js');
var Errors = require('./../../config/errors.js');
var DataIds = require('./_id.data.js');
var DateUtils = require('nodejs-utils').DateUtils;
var Database = require('nodejs-database').getInstance(Config);
var DatabaseErrors = Database.Errors;
var RdbmsService = Database.RdbmsService;
var Game = RdbmsService.Models.Game.Game;
var GameModule = RdbmsService.Models.Game.GameModule;
var LiveTournament = RdbmsService.Models.Game.LiveTournament;
var GameHasPool = RdbmsService.Models.Game.GameHasPool;
var GameHasQuestionTemplate = RdbmsService.Models.Game.GameHasQuestionTemplate;
var GameHasRegionalSetting = RdbmsService.Models.Game.GameHasRegionalSetting;
var GamePoolValidationResult = RdbmsService.Models.Game.GamePoolValidationResult;
var GameHasWinningComponent = RdbmsService.Models.Game.GameHasWinningComponent;

module.exports = {
    GAME_1: {
        id: DataIds.GAME_1_ID,
        title: 'Game one title',
        description: 'Game one description',
        gameModuleId: 1,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        isMoneyGame: 1,
        isLiveGame: 1,
        isDeployed: 0,
        isInternationalGame: 1,
        isTenantLocked: 1,
        questionTypeSpread: 'fixed',
        complexitySpread: 'fixed',
        numberOfQuestionsToPlay: 10,
        tournamentLimits: 1,
        questionSelectionTill: 1,
        questionTypeUsage: 'random',
        questionSelection: 'randomPerPlayer',
        minimumGameLevel: 1,
        overWriteUsage: 'repeatQuestions',
        waitTimeBetweenQuestions: 10,
        vibration: 1,
        sound: 1,
        abortGame: 1,
        instantAnswerFeedback: 1,
        entryFeeDecidedByPlayer: 50,
        gameEntryAmount: 100,
        gameEntryCurrency: 'CREDIT',
        complexityLevel: [
            { 1: 20 },
            { 2: 50 },
            { 3: 100 }
        ],
        gameTypeConfiguration: {
            gameDuel: {
                duelType: 'normal',
                playerInviteAccess: 'friendsOnly',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 10,
                gameStartWarningMessage: 10,
                emailNotification: 1,
                playerGameReadiness: 10,
                groupParing: 'ranking',
                groupSize: 10,
                minimumPlayerNeeded: 2,
                maximumPlayerAllowed: 100
            },
            gameQuizBattle: {
              liveTournamentId: 1,
              lastGroupBehaviour: 'cancelAndReject',
              lastGroupAtQualifyEnd: 'cancelAndReject',
              startDate: DateUtils.isoNow(),
              endDate: DateUtils.isoFuture()
            },
            gameQuickQuiz: {
                gameId: 1,
                chance5050: 80,
                chance5050BonusPoints: 5,
                questionHint: 2,
                questionHintBonusPoints: 7,
                skipQuestion: 2,
                skipQuestionBonusPoints: 6,
                skipQuestionAmount: 5
            },
            gameTournament: {
                duelType: 'normal',
                playerInviteAccess: 'friendsOnly',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 10,
                gameStartWarningMessage: 10,
                emailNotification: 1,
                playerGameReadiness: 10,
                groupParing: 'random',
                groupSize: 10,
                minimumPlayerNeeded: 2,
                maximumPlayerAllowed: 100,
                jackpotGame: 10,
                minimumJackpotGarantie: 3,
                targetJackpotAmount: 100,
                targetJackpotCurrency: 0,
                jackpotCalculateByEntryFee: 10
            }
        },
        resultConfiguration: {
          pointCalculator: 2,
          alternativeBonusPointsPerCorrect: 5,
          bonusPointsPerRightAnswer: 5,
          calculatingBonuspointsPerRightAnswer: 10,
          treatPaidLikeUnpaid: 2,
          pointCalculationRightAnswer: 'basedOnFixValue',
          duelGroupWinnerPayout: 'calculateValueOfJackpot',
          groupPrizePaidOutTo: 'partsOfTheWinners',
          questionComplexity: 'very',
          fixedValueAmount: 15.50,
          fixedValueCurrency: 'credit',
          percentageOfJackpot: 50.5,
          payoutToEveryXWinner: 5000,
          gameLevelStructure: '{}',
          payoutStructureUsed: '{}',
          winningComponent: 1,
          singleWinningComponent: 1,
          randomWinningComponent: 1,
          paymentCostForTheWinningComponent: 100,
          bonusCostForTheUnpaidWinningComponent: 100,
          specialPrice: 1,
          winningRule: 'overXWinnerWins',
          winningRuleAmount: 1000,
          gameResultConfigurationcol: '{}',
          specialPrizeVoucherId: 1,
          advertisement: 1,
          adsFrequency: 'afterEachQuestion',
          adsFrequencyAmount: 10,
          loadingScreen: 2,
          gameStart: 1,
          advertisementProviderId: 2,
          voucher: 1,
          voucherProviderId: 2
        },
        gameType: 'duel',
        isClosed: 0,
        poolsIds: [DataIds.POOL_1_ID],
        questionTemplates: [{questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID}],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID]
    },
    GAME_2: {
        id: DataIds.GAME_2_ID,
        title: 'Game two title',
        description: 'Game two description',
        gameModuleId: 2,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        isMoneyGame: 2,
        isLiveGame: 2,
        isDeployed: 0,
        isInternationalGame: 2,
        isTenantLocked: 1,
        questionTypeSpread: 'percentage',
        complexitySpread: 'percentage',
        numberOfQuestionsToPlay: 10,
        tournamentLimits: 1,
        questionSelectionTill: 2,
        questionTypeUsage: 'ordered',
        questionSelection: 'sameForEachPlayer',
        minimumGameLevel: 2,
        overWriteUsage: 'onlyOnce',
        waitTimeBetweenQuestions: 20,
        vibration: 1,
        sound: 2,
        abortGame: 2,
        instantAnswerFeedback: 2,
        entryFeeDecidedByPlayer: 50,
        gameEntryAmount: 100,
        gameEntryCurrency: 'MONEY',
        complexityLevel: [
            { 1: 10 },
            { 2: 20 },
            { 3: 30 }
        ],
        gameTypeConfiguration: {
            gameDuel: {
                duelType: 'knockOut',
                playerInviteAccess: 'inviteOnly',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 20,
                gameStartWarningMessage: 5,
                emailNotification: 2,
                playerGameReadiness: 10,
                groupParing: 'gameEntered',
                groupSize: 5,
                minimumPlayerNeeded: 5,
                maximumPlayerAllowed: 50
            },
            gameQuizBattle: {
              liveTournamentId: 2,
              lastGroupBehaviour: 'rolloverToNextDay',
              lastGroupAtQualifyEnd: 'rolloverIntoNextGame',
              startDate: DateUtils.isoNow(),
              endDate: DateUtils.isoFuture()
            },
            gameQuickQuiz: {
                chance5050: 30,
                chance5050BonusPoints: 10,
                questionHint: 4,
                questionHintBonusPoints: 9,
                skipQuestion: 3,
                skipQuestionBonusPoints: 2,
                skipQuestionAmount: 10
            },
            gameTournament: {
                duelType: 'knockOut',
                playerInviteAccess: 'allPlayer',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 10,
                gameStartWarningMessage: 10,
                emailNotification: 2,
                playerGameReadiness: 10,
                groupParing: 'ranking',
                groupSize: 10,
                minimumPlayerNeeded: 5,
                maximumPlayerAllowed: 50,
                jackpotGame: 10,
                minimumJackpotGarantie: 5,
                targetJackpotAmount: 50,
                targetJackpotCurrency: 0,
                jackpotCalculateByEntryFee: 10
            }
        },
        resultConfiguration: {
          pointCalculator: 1,
          alternativeBonusPointsPerCorrect: 5,
          bonusPointsPerRightAnswer: 5,
          calculatingBonuspointsPerRightAnswer: 10,
          treatPaidLikeUnpaid: 1,
          pointCalculationRightAnswer: 'basedOnQuestionCommplexity',
          duelGroupWinnerPayout: 'fixedPayoutValue',
          groupPrizePaidOutTo: 'allWinners',
          questionComplexity: 'very',
          fixedValueAmount: 5.50,
          fixedValueCurrency: 'euro',
          percentageOfJackpot: 50.5,
          payoutToEveryXWinner: 10000,
          gameLevelStructure: '{}',
          payoutStructureUsed: '{}',
          winningComponent: 1,
          singleWinningComponent: 1,
          randomWinningComponent: 1,
          paymentCostForTheWinningComponent: 100,
          bonusCostForTheUnpaidWinningComponent: 100,
          specialPrice: 1,
          winningRule: 'everyPlayer',
          winningRuleAmount: 1000,
          gameResultConfigurationcol: '{}',
          specialPrizeVoucherId: 1,
          advertisement: 1,
          adsFrequency: 'beforeTheGame',
          adsFrequencyAmount: 10,
          loadingScreen: 1,
          gameStart: 1,
          advertisementProviderId: 1,
          voucher: 1,
          voucherProviderId: 1
        },
        gameType: 'tournament',
        isClosed: 0,
        poolsIds: [DataIds.POOL_2_ID],
        questionTemplates: [{questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID}],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID]
    },
    GAME_TEST: {
        id: DataIds.GAME_TEST_ID,
        title: 'Game two title',
        description: 'Game two description',
        gameModuleId: 2,
        startDate:  DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        isMoneyGame: 2,
        isLiveGame: 2,
        isDeployed: 0,
        isInternationalGame: 2,
        isTenantLocked: 1,
        questionTypeSpread: 'percentage',
        complexitySpread: 'percentage',
        numberOfQuestionsToPlay: 10,
        tournamentLimits: 1,
        questionSelectionTill: 2,
        questionTypeUsage: 'ordered',
        questionSelection: 'sameForEachPlayer',
        minimumGameLevel: 2,
        overWriteUsage: 'onlyOnce',
        waitTimeBetweenQuestions: 20,
        vibration: 1,
        sound: 2,
        abortGame: 2,
        instantAnswerFeedback: 2,
        entryFeeDecidedByPlayer: 50,
        gameEntryAmount: 100,
        gameEntryCurrency: 'MONEY',
        complexityLevel: [
            { 1: 10 },
            { 2: 20 },
            { 3: 30 }
        ],
        gameTypeConfiguration: {
            gameDuel: {
                duelType: 'knockOut',
                playerInviteAccess: 'inviteOnly',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 20,
                gameStartWarningMessage: 5,
                emailNotification: 2,
                playerGameReadiness: 10,
                groupParing: 'gameEntered',
                groupSize: 5,
                minimumPlayerNeeded: 5,
                maximumPlayerAllowed: 50
            },
            gameQuizBattle: {
              liveTournamentId: 2,
              lastGroupBehaviour: 'rolloverToNextDay',
              lastGroupAtQualifyEnd: 'rolloverIntoNextGame',
              startDate: DateUtils.isoNow(),
              endDate: DateUtils.isoFuture()
            },
            gameQuickQuiz: {
                chance5050: 30,
                chance5050BonusPoints: 10,
                questionHint: 4,
                questionHintBonusPoints: 9,
                skipQuestion: 3,
                skipQuestionBonusPoints: 2,
                skipQuestionAmount: 10
            },
            gameTournament: {
                duelType: 'knockOut',
                playerInviteAccess: 'allPlayer',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 10,
                gameStartWarningMessage: 10,
                emailNotification: 2,
                playerGameReadiness: 10,
                groupParing: 'ranking',
                groupSize: 10,
                minimumPlayerNeeded: 5,
                maximumPlayerAllowed: 50,
                jackpotGame: 10,
                minimumJackpotGarantie: 5,
                targetJackpotAmount: 50,
                targetJackpotCurrency: 0,
                jackpotCalculateByEntryFee: 10
            }
        },
        resultConfiguration: {
          pointCalculator: 1,
          alternativeBonusPointsPerCorrect: 5,
          bonusPointsPerRightAnswer: 5,
          calculatingBonuspointsPerRightAnswer: 10,
          treatPaidLikeUnpaid: 1,
          pointCalculationRightAnswer: 'basedOnQuestionCommplexity',
          duelGroupWinnerPayout: 'fixedPayoutValue',
          groupPrizePaidOutTo: 'allWinners',
          questionComplexity: 'very',
          fixedValueAmount: 5.50,
          fixedValueCurrency: 'euro',
          percentageOfJackpot: 50.5,
          payoutToEveryXWinner: 10000,
          gameLevelStructure: '{}',
          payoutStructureUsed: '{}',
          winningComponent: 1,
          singleWinningComponent: 1,
          randomWinningComponent: 1,
          paymentCostForTheWinningComponent: 100,
          bonusCostForTheUnpaidWinningComponent: 100,
          specialPrice: 1,
          winningRule: 'everyPlayer',
          winningRuleAmount: 1000,
          gameResultConfigurationcol: '{}',
          specialPrizeVoucherId: 1,
          advertisement: 1,
          adsFrequency: 'beforeTheGame',
          adsFrequencyAmount: 10,
          loadingScreen: 1,
          gameStart: 1,
          advertisementProviderId: 1,
          voucher: 1,
          voucherProviderId: 1
        },
        gameType: 'tournament',
        isClosed: 0,
    },
    GAME_NO_DEPENDENCIES: {
        id: DataIds.GAME_NO_DEPENDENCIES_ID,
        title: 'Game nodeps title',
        description: 'Game nodeps description',
        gameModuleId: 2,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        isMoneyGame: 2,
        isLiveGame: 2,
        isInternationalGame: 2,
        isTenantLocked: 1,
        questionTypeSpread: 'percentage',
        complexitySpread: 'percentage',
        numberOfQuestionsToPlay: 10,
        tournamentLimits: 1,
        questionSelectionTill: 2,
        questionTypeUsage: 'ordered',
        questionSelection: 'sameForEachPlayer',
        minimumGameLevel: 2,
        overWriteUsage: 'onlyOnce',
        waitTimeBetweenQuestions: 20,
        vibration: 1,
        sound: 2,
        abortGame: 2,
        instantAnswerFeedback: 2,
        entryFeeDecidedByPlayer: 50,
        gameEntryAmount: 100,
        gameEntryCurrency: 'MONEY',
        complexityLevel: [
            { 1: 10 },
            { 2: 20 },
            { 3: 30 }
        ],
        gameTypeConfiguration: {
            gameDuel: {
                duelType: 'knockOut',
                playerInviteAccess: 'inviteOnly',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 20,
                gameStartWarningMessage: 5,
                emailNotification: 2,
                playerGameReadiness: 10,
                groupParing: 'gameEntered',
                groupSize: 5,
                minimumPlayerNeeded: 5,
                maximumPlayerAllowed: 50
            },
            gameQuizBattle: {
                liveTournamentId: 2,
                lastGroupBehaviour: 'rolloverToNextDay',
                lastGroupAtQualifyEnd: 'rolloverIntoNextGame',
                startDate: DateUtils.isoNow(),
                endDate: DateUtils.isoFuture()
            },
            gameQuickQuiz: {
                chance5050: 30,
                chance5050BonusPoints: 10,
                questionHint: 4,
                questionHintBonusPoints: 9,
                skipQuestion: 3,
                skipQuestionBonusPoints: 2,
                skipQuestionAmount: 10
            },
            gameTournament: {
                duelType: 'knockOut',
                playerInviteAccess: 'allPlayer',
                timeToAcceptInvites: 10,
                gameCancellationPriorGameStart: 10,
                gameStartWarningMessage: 10,
                emailNotification: 2,
                playerGameReadiness: 10,
                groupParing: 'ranking',
                groupSize: 10,
                minimumPlayerNeeded: 5,
                maximumPlayerAllowed: 50,
                jackpotGame: 10,
                minimumJackpotGarantie: 5,
                targetJackpotAmount: 50,
                targetJackpotCurrency: 0,
                jackpotCalculateByEntryFee: 10
            }
        },
        resultConfiguration: {
            pointCalculator: 1,
            alternativeBonusPointsPerCorrect: 5,
            bonusPointsPerRightAnswer: 5,
            calculatingBonuspointsPerRightAnswer: 10,
            treatPaidLikeUnpaid: 1,
            pointCalculationRightAnswer: 'basedOnQuestionCommplexity',
            duelGroupWinnerPayout: 'fixedPayoutValue',
            groupPrizePaidOutTo: 'allWinners',
            questionComplexity: 'very',
            fixedValueAmount: 5.50,
            fixedValueCurrency: 'euro',
            percentageOfJackpot: 50.5,
            payoutToEveryXWinner: 10000,
            gameLevelStructure: '{}',
            payoutStructureUsed: '{}',
            winningComponent: 1,
            singleWinningComponent: 1,
            randomWinningComponent: 1,
            paymentCostForTheWinningComponent: 100,
            bonusCostForTheUnpaidWinningComponent: 100,
            specialPrice: 1,
            winningRule: 'everyPlayer',
            winningRuleAmount: 1000,
            gameResultConfigurationcol: '{}',
            specialPrizeVoucherId: 1,
            advertisement: 1,
            adsFrequency: 'beforeTheGame',
            adsFrequencyAmount: 10,
            loadingScreen: 1,
            gameStart: 1,
            advertisementProviderId: 1,
            voucher: 1,
            voucherProviderId: 1
        },
        gameType: 'tournament',
        isClosed: 0,
    },

    GAME_MODULE_1: {
        id: DataIds.GAME_MODULE_1_ID,
        name: 'gm 1"\'fd'
    },
    GAME_MODULE_2: {
        id: DataIds.GAME_MODULE_2_ID,
        name: 'gm 2"\'fd'
    },
    GAME_MODULE_TEST: {
        id: DataIds.GAME_MODULE_TEST_ID,
        name: 'gm 2"\'fd'
    },

    GAME_1_POOL_VALIDATION_RESULT_1: {
        id: DataIds.GAME_1_POOL_VALIDATION_RESULT_1_ID,
        createDate: DateUtils.isoNow(),
        gameId: DataIds.GAME_1_ID,
        result: '{}',
        warningMessage: 'Congratulations!!!'
    },

    GAME_2_POOL_VALIDATION_RESULT_2: {
        id: DataIds.GAME_2_POOL_VALIDATION_RESULT_2_ID,
        createDate: DateUtils.isoNow(),
        gameId: DataIds.GAME_2_ID,
        result: '{}',
        warningMessage: 'Congratulations!!!'
    },

    LIVE_TOURNAMENT_1: {
        id: DataIds.LIVE_TOURNAMENT_1_ID,
        name: 'Tournament one'
    },
    LIVE_TOURNAMENT_2: {
        id: DataIds.LIVE_TOURNAMENT_2_ID,
        name: 'Tournament two'
    },

    GAME_1_HAS_POOL_1: {
      gameId: DataIds.GAME_1_ID,
      poolId: DataIds.POOL_1_ID
    },
    GAME_2_HAS_POOL_2: {
      gameId: DataIds.GAME_2_ID,
      poolId: DataIds.POOL_2_ID
    },

    GAME_1_HAS_QUESTION_TEMPLATE_1: {
      gameId: DataIds.GAME_1_ID,
      questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
      order: 1,
      amount: 10
    },
    GAME_2_HAS_QUESTION_TEMPLATE_2: {
      gameId: DataIds.GAME_2_ID,
      questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
      order: 2,
      amount: 5
    },

    GAME_1_HAS_REGIONAL_SETTING_1: {
      gameId: DataIds.GAME_1_ID,
      regionalSettingId: DataIds.REGIONAL_SETTING_1_ID
    },
    GAME_2_HAS_REGIONAL_SETTING_2: {
      gameId: DataIds.GAME_2_ID,
      regionalSettingId: DataIds.REGIONAL_SETTING_2_ID
    },

    GAME_1_HAS_WINNING_COMPONENT_1: {
      gameId: DataIds.GAME_1_ID,
      winningComponentId: DataIds.WINNING_COMPONENT_1_ID,
      type: 'paid'
    },
    GAME_2_HAS_WINNING_COMPONENT_2: {
      gameId: DataIds.GAME_2_ID,
      winningComponentId: DataIds.WINNING_COMPONENT_2_ID,
      type: 'unpaid'
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(GameHasPool, [self.GAME_1_HAS_POOL_1, self.GAME_2_HAS_POOL_2], 'gameId')
            .removeSeries(GameHasQuestionTemplate, [self.GAME_1_HAS_QUESTION_TEMPLATE_1, self.GAME_2_HAS_QUESTION_TEMPLATE_2], 'gameId')
            .removeSeries(GameHasRegionalSetting, [self.GAME_1_HAS_REGIONAL_SETTING_1, self.GAME_2_HAS_REGIONAL_SETTING_2], 'gameId')
            .removeSeries(GameHasWinningComponent, [self.GAME_1_HAS_WINNING_COMPONENT_1, self.GAME_2_HAS_WINNING_COMPONENT_2], 'gameId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(GameHasWinningComponent, [self.GAME_1_HAS_WINNING_COMPONENT_1, self.GAME_2_HAS_WINNING_COMPONENT_2], 'gameId')
            .createSeries(GameHasRegionalSetting, [self.GAME_1_HAS_REGIONAL_SETTING_1, self.GAME_2_HAS_REGIONAL_SETTING_2], 'gameId')
            .createSeries(GameHasQuestionTemplate, [self.GAME_1_HAS_QUESTION_TEMPLATE_1, self.GAME_2_HAS_QUESTION_TEMPLATE_2], 'gameId')
            .createSeries(GameHasPool, [self.GAME_1_HAS_POOL_1, self.GAME_2_HAS_POOL_2], 'gameId')
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    cleanEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(GamePoolValidationResult, [self.GAME_1_POOL_VALIDATION_RESULT_1, self.GAME_2_POOL_VALIDATION_RESULT_2])
            .removeSeries(Game, [self.GAME_1, self.GAME_2, self.GAME_NO_DEPENDENCIES])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadEntities: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(Game, [self.GAME_1, self.GAME_2, self.GAME_NO_DEPENDENCIES])
            .createSeries(GamePoolValidationResult, [self.GAME_1_POOL_VALIDATION_RESULT_1, self.GAME_2_POOL_VALIDATION_RESULT_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    cleanClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(LiveTournament, [self.LIVE_TOURNAMENT_1, self.LIVE_TOURNAMENT_2])
            .removeSeries(GameModule, [self.GAME_MODULE_1, self.GAME_MODULE_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    },

    loadClassifiers: function (done) {
        var self = this;
        RdbmsService.load()
            .createSeries(GameModule, [self.GAME_MODULE_1, self.GAME_MODULE_2])
            .createSeries(LiveTournament, [self.LIVE_TOURNAMENT_1, self.LIVE_TOURNAMENT_2])
            .process(function (err) {
                if (err) {
                    return done(err);
                }
                return done();
            });
    }
};
