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
        iconId: DataIds.MEDIA_1_ID,
        description: 'Game one description',
        imageId: DataIds.MEDIA_1_ID,
        gameModuleId: 1,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        repetition: null,
        isMoneyGame: 1,
        isLiveGame: 1,
        isInternationalGame: 1,
        isTenantLocked: 1,
        questionTypeSpread: 'fixed',
        complexitySpread: 'fixed',
        numberOfQuestionsToPlay: 1,
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
        isPromotion: 1,
        promotionStartDate: DateUtils.isoNow(),
        promotionEndDate: DateUtils.isoFuture(),
        complexityLevel: {
            '1': 1,
            '2': 0,
            '3': 0
        },
        questionComplexity: [
            {
                level: 1,
                number: 1
            },
            {
                level: 2,
                number: 0
            },
            {
                level: 3,
                number: 0
            },
        ],
        status: Game.constants().STATUS_INACTIVE,
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
                maximumPlayerAllowed: 100,
                quickResponseBonuspoints: 20,
                quickResponseTimeLimitMs: 60000*3
            },
            gameQuizBattle: {
                liveTournamentId: DataIds.LIVE_TOURNAMENT_1_ID,
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
                skipQuestionAmount: 5,
                special: true,
                weight: 1.1,
                bannerMediaId: DataIds.MEDIA_1_ID
            },
            gameTournament: {
                duelType: 'normal',
                type: 'normal',
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
                jackpotCalculateByEntryFee: 10,
                gameLevelStructure: [
                    10,
                    20,
                    60
                ]
            }
        },
        jokerConfiguration: [
            {
                type: 'HINT',
                enabled: true,
                availableCount: 10,
                price: 10,
                currency: 'BONUS'
            },
            {
                type: 'FIFTY_FIFTY',
                enabled: true,
                availableCount: 10,
                price: 20,
                currency: 'CREDIT'
            },
            {
                type: 'SKIP',
                enabled: true
            },
            {
                type: 'IMMEDIATE_ANSWER',
                enabled: true,
                availableCount: 10,
                price: 30,
                currency: 'MONEY',
                displayTime: 5
            }
        ],
        resultConfiguration: {
            pointCalculator: false,
            alternativeBonusPointsPerCorrect: 5,
            bonusPointsPerRightAnswer: 5,
            calculatingBonuspointsPerRightAnswer: 10,
            treatPaidLikeUnpaid: false,
            pointCalculationRightAnswer: 'basedOnFixValue',
            duelGroupWinnerPayout: 'calculateValueOfJackpot',
            groupPrizePaidOutTo: 'partsOfTheWinners',
            questionComplexity: 'very',
            fixedValueAmount: 15.50,
            fixedValueCurrency: 'CREDIT',
            percentageOfJackpot: 50.5,
            payoutToEveryXWinner: 5000,
            gameLevelStructure: '{}',
            payoutStructureUsed: '{}',
            winningComponent: 1,
            singleWinningComponent: 1,
            randomWinningComponent: 1,
            paymentCostForTheWinningComponent: 100,
            bonusCostForTheUnpaidWinningComponent: 100,
            specialPrice: true,
            winningRule: 'firstXPlayers',
            winningRuleAmount: 1000,
            gameResultConfigurationcol: '{}',
            specialPrizeVoucherId: 1,
            advertisement: 1,
            adsFrequency: 'afterEachQuestion',
            adsFrequencyAmount: 10,
            loadingScreen: 2,
            gameStart: 1,
            advertisementProviderId: null,
            advertisementDuration: 5,
            voucher: 1,
            voucherProviderId: 2,
            paidWinningComponentPayoutStructure: {
                items: [
                    {
                        prizeId: "1",
                        prizeIndex: 0,
                        prizeName: "1st",
                        type: "MONEY",
                        amount: 10.1,
                        probability: 1,
                        imageId: DataIds.MEDIA_1_ID
                    },
                    {
                        prizeId: "2",
                        prizeIndex: 1,
                        prizeName: "2nd",
                        type: "BONUS",
                        amount: 20.1,
                        probability: 2,
                        imageId: DataIds.MEDIA_2_ID
                    }
                ]
            },
            freeWinningComponentPayoutStructure: {
                items: [
                    {
                        prizeId: "1",
                        prizeIndex: 0,
                        prizeName: "1st",
                        type: "MONEY",
                        amount: 30.1,
                        probability: 3,
                        imageId: DataIds.MEDIA_1_ID
                    },
                    {
                        prizeId: "2",
                        prizeIndex: 1,
                        prizeName: "2nd",
                        type: "BONUS",
                        amount: 40.1,
                        probability: 4,
                        imageId: DataIds.MEDIA_2_ID
                    }
                ]
            }
        },
        playerEntryFeeType: "fixed",
        playerEntryFeeSettings: {
            moneyMin: 10.0,
            moneyMax: 1000.0,
            moneyIncrement: 0.01,
            bonuspointsMin: 5,
            bonuspointsMax: 500,
            bonuspointsIncrement: 1,
            creditsMin: 1,
            creditsMax: 100,
            creditsIncrement: 1
        },
        playerEntryFeeValues: {
            money: [1.0, 2.2, 3.0],
            bonusPoints: [1, 2, 3],
            credits: [1, 2, 3],
        },
        advertisement: 1,
        adsFrequency: 'afterEachQuestion',
        adsFrequencyAmount: 10,
        advertisementProviderId: null,
        isAutoValidationEnabled: 1,
        prizes: 'prizes',
        gameType: 'duel',
        isClosed: 0,
        hideCategories: 1,
        entryFeeBatchSize: 1,
        isMultiplePurchaseAllowed: 1,
        instantAnswerFeedbackDelay: 0,
        isPromotion: 0,
        isRegionalLimitationEnabled: 0,
        poolsIds: [DataIds.POOL_1_ID, DataIds.POOL_2_ID],
        applicationsIds: [DataIds.APPLICATION_1_ID],
        questionTemplates: [
            {
                questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
                amount: 1,
                order: 2
            }
        ],
        winningComponents: [
            {
                winningComponentId: DataIds.WINNING_COMPONENT_1_ID,
                isFree: 1,
                rightAnswerPercentage: 100,
                entryFeeAmount: 10,
                entryFeeCurrency: 'BONUS'
            },
            {
                winningComponentId: DataIds.WINNING_COMPONENT_2_ID,
                isFree: 0,
                rightAnswerPercentage: 90,
                entryFeeAmount: 8.5,
                entryFeeCurrency: 'CREDIT'
            },
        ],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_1_ID],
        tenantId: DataIds.TENANT_1_ID
    },
    GAME_2: {
        id: DataIds.GAME_2_ID,
        title: 'Game two title',
        iconId: DataIds.MEDIA_2_ID,
        description: 'Game two description',
        imageId: DataIds.MEDIA_2_ID,
        gameModuleId: 2,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        isMoneyGame: 2,
        isLiveGame: 2,
        isInternationalGame: 2,
        isTenantLocked: 1,
        questionTypeSpread: 'percentage',
        complexitySpread: 'percentage',
        numberOfQuestionsToPlay: 1,
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
        complexityLevel: {
            '1': 1,
            '2': 0,
            '3': 0
        },
        status: Game.constants().STATUS_INACTIVE,
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
                liveTournamentId: DataIds.LIVE_TOURNAMENT_2_ID,
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
                skipQuestionAmount: 10,
                special: true,
                weight: 1.2,
                bannerMediaId: DataIds.MEDIA_2_ID
            },
            gameTournament: {
                duelType: 'knockOut',
                type: 'user',
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
        jokerConfiguration: [
            {
                type: 'HINT',
                enabled: true,
                availableCount: 10,
                price: 10,
                currency: 'BONUS'
            },
            {
                type: 'FIFTY_FIFTY',
                enabled: true,
                availableCount: 10,
                price: 20,
                currency: 'CREDIT'
            },
            {
                type: 'SKIP',
                enabled: true
            },
            {
                type: 'IMMEDIATE_ANSWER',
                enabled: true,
                availableCount: 10,
                price: 30,
                currency: 'MONEY',
                displayTime: 5
            }
        ],
        resultConfiguration: {
            pointCalculator: true,
            alternativeBonusPointsPerCorrect: 5,
            bonusPointsPerRightAnswer: 5,
            calculatingBonuspointsPerRightAnswer: 10,
            treatPaidLikeUnpaid: true,
            pointCalculationRightAnswer: 'basedOnQuestionCommplexity',
            duelGroupWinnerPayout: 'fixedPayoutValue',
            groupPrizePaidOutTo: 'allWinners',
            questionComplexity: 'very',
            fixedValueAmount: 5.50,
            fixedValueCurrency: 'MONEY',
            percentageOfJackpot: 50.5,
            payoutToEveryXWinner: 10000,
            gameLevelStructure: '{}',
            payoutStructureUsed: '{}',
            winningComponent: 1,
            singleWinningComponent: 1,
            randomWinningComponent: 1,
            paymentCostForTheWinningComponent: 100,
            bonusCostForTheUnpaidWinningComponent: 100,
            specialPrice: true,
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
            advertisementDuration: 5,
            voucher: 1,
            voucherProviderId: 1
        },
        playerEntryFeeType: "step",
        playerEntryFeeSettings: {
            moneyMin: 10.0,
            moneyMax: 1000.0,
            moneyIncrement: 0.01,
            bonuspointsMin: 5,
            bonuspointsMax: 500,
            bonuspointsIncrement: 1,
            creditsMin: 1,
            creditsMax: 100,
            creditsIncrement: 1
        },
        playerEntryFeeValues: {
            money: [1.0, 2.2, 3.0],
            bonusPoints: [1, 2, 3],
            credits: [1, 2, 3],
        },
        advertisement: 1,
        adsFrequency: 'beforeTheGame',
        adsFrequencyAmount: 10,
        advertisementProviderId: 1,
        isAutoValidationEnabled: 1,
        prizes: 'prizes',
        gameType: 'tournament',
        isClosed: 0,
        hideCategories: 1,
        entryFeeBatchSize: 1,
        isMultiplePurchaseAllowed: 1,
        instantAnswerFeedbackDelay: 0,
        isPromotion: 0,
        isRegionalLimitationEnabled: 0,
        poolsIds: [DataIds.POOL_2_ID],
        applicationsIds: [DataIds.APPLICATION_2_ID],
        questionTemplates: [
            {
                questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
                amount: 1,
                order: 2
            }
        ],
        winningComponents: [
            {
                winningComponentId: DataIds.WINNING_COMPONENT_1_ID,
                isFree: 1,
                rightAnswerPercentage: 100,
                entryFeeAmount: 10,
                entryFeeCurrency: 'BONUS'
            },
            {
                winningComponentId: DataIds.WINNING_COMPONENT_2_ID,
                isFree: 0,
                rightAnswerPercentage: 90,
                entryFeeAmount: 8.5,
                entryFeeCurrency: 'CREDIT'
            },
        ],
        regionalSettingsIds: [DataIds.REGIONAL_SETTING_2_ID],
        tenantId: DataIds.TENANT_1_ID
    },
    GAME_TEST: {
        id: DataIds.GAME_TEST_ID,
        title: 'Game two title',
        iconId: DataIds.MEDIA_2_ID,
        description: 'Game two description',
        imageId: DataIds.MEDIA_2_ID,
        gameModuleId: 2,
        startDate:  DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        repetition: {
            unit: 'day',
            repeat: 2,
            repeatOn: null,
            exceptDays: [],
            offsetOpenRegistration: {
                unit: 'minute',
                offset: 30
            },
            offsetStartGame: {
                unit: 'minute',
                offset: 5
            }
        },
        isMoneyGame: 2,
        isLiveGame: 2,
        isInternationalGame: 2,
        isTenantLocked: 1,
        questionTypeSpread: 'percentage',
        complexitySpread: 'percentage',
        numberOfQuestionsToPlay: 1,
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
        complexityLevel: {
            '1': 1,
            '2': 0,
            '3': 0
        },
        status: Game.constants().STATUS_INACTIVE,
        jokerConfiguration: [
            {
                type: 'HINT',
                enabled: true,
                availableCount: 10,
                price: 10,
                currency: 'BONUS'
            },
            {
                type: 'FIFTY_FIFTY',
                enabled: true,
                availableCount: 10,
                price: 20,
                currency: 'CREDIT'
            },
            {
                type: 'SKIP',
                enabled: true
            },
            {
                type: 'IMMEDIATE_ANSWER',
                enabled: true,
                availableCount: 10,
                price: 30,
                currency: 'MONEY',
                displayTime: 5
            }
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
                liveTournamentId: DataIds.LIVE_TOURNAMENT_2_ID,
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
                skipQuestionAmount: 10,
                special: true,
                weight: 1.2,
                bannerMediaId: DataIds.MEDIA_2_ID
            },
            gameTournament: {
                duelType: 'knockOut',
                type: 'user_live',
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
            pointCalculator: true,
            alternativeBonusPointsPerCorrect: 5,
            bonusPointsPerRightAnswer: 5,
            calculatingBonuspointsPerRightAnswer: 10,
            treatPaidLikeUnpaid: true,
            pointCalculationRightAnswer: 'basedOnQuestionCommplexity',
            duelGroupWinnerPayout: 'fixedPayoutValue',
            groupPrizePaidOutTo: 'allWinners',
            questionComplexity: 'very',
            fixedValueAmount: 5.50,
            fixedValueCurrency: 'MONEY',
            percentageOfJackpot: 50.5,
            payoutToEveryXWinner: 10000,
            gameLevelStructure: '{}',
            payoutStructureUsed: '{}',
            winningComponent: 1,
            singleWinningComponent: 1,
            randomWinningComponent: 1,
            paymentCostForTheWinningComponent: 100,
            bonusCostForTheUnpaidWinningComponent: 100,
            specialPrice: true,
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
            advertisementDuration: 5,
            voucher: 1,
            voucherProviderId: 1
        },
        playerEntryFeeType: "step",
        playerEntryFeeSettings: {
            moneyMin: 10.0,
            moneyMax: 1000.0,
            moneyIncrement: 0.01,
            bonuspointsMin: 5,
            bonuspointsMax: 500,
            bonuspointsIncrement: 1,
            creditsMin: 1,
            creditsMax: 100,
            creditsIncrement: 1
        },
        playerEntryFeeValues: {
            money: [1.0, 2.2, 3.0],
            bonusPoints: [1, 2, 3],
            credits: [1, 2, 3],
        },
        advertisement: 1,
        adsFrequency: 'beforeTheGame',
        adsFrequencyAmount: 10,
        advertisementProviderId: 1,
        isAutoValidationEnabled: 1,
        prizes: 'prizes',
        gameType: 'tournament',
        isClosed: 0,
        hideCategories: 1,
        entryFeeBatchSize: 1,
        isMultiplePurchaseAllowed: 1,
        instantAnswerFeedbackDelay: 0,
        isPromotion: 0,
        isRegionalLimitationEnabled: 0,
        applicationsIds: [DataIds.APPLICATION_1_ID],
        winningComponents: [
            {
                winningComponentId: DataIds.WINNING_COMPONENT_1_ID,
                isFree: 1,
                rightAnswerPercentage: 100,
                entryFeeAmount: 10,
                entryFeeCurrency: 'BONUS'
            },
            {
                winningComponentId: DataIds.WINNING_COMPONENT_2_ID,
                isFree: 0,
                rightAnswerPercentage: 90,
                entryFeeAmount: 8.5,
                entryFeeCurrency: 'CREDIT'
            },
        ],
    },
    GAME_NO_DEPENDENCIES: {
        id: DataIds.GAME_NO_DEPENDENCIES_ID,
        title: 'Game nodeps title',
        iconId: DataIds.MEDIA_2_ID,
        description: 'Game nodeps description',
        imageId: DataIds.MEDIA_2_ID,
        gameModuleId: 2,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        isMoneyGame: 2,
        isLiveGame: 2,
        isInternationalGame: 2,
        isTenantLocked: 1,
        questionTypeSpread: 'percentage',
        complexitySpread: 'percentage',
        numberOfQuestionsToPlay: 1,
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
        complexityLevel: {
            '1': 10,
            '2': 20,
            '3': 30
        },
        status: Game.constants().STATUS_INACTIVE,
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
                liveTournamentId: DataIds.LIVE_TOURNAMENT_2_ID,
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
                skipQuestionAmount: 10,
                special: true,
                weight: 1.2,
                bannerMediaId: DataIds.MEDIA_2_ID
            },
            gameTournament: {
                duelType: 'knockOut',
                type: 'live',
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
        jokerConfiguration: [
            {
                type: 'HINT',
                enabled: true,
                availableCount: 10,
                price: 10,
                currency: 'BONUS'
            },
            {
                type: 'FIFTY_FIFTY',
                enabled: true,
                availableCount: 10,
                price: 20,
                currency: 'CREDIT'
            },
            {
                type: 'SKIP',
                enabled: true
            },
            {
                type: 'IMMEDIATE_ANSWER',
                enabled: true,
                availableCount: 10,
                price: 30,
                currency: 'MONEY',
                displayTime: 5
            }
        ],
        resultConfiguration: {
            pointCalculator: true,
            alternativeBonusPointsPerCorrect: 5,
            bonusPointsPerRightAnswer: 5,
            calculatingBonuspointsPerRightAnswer: 10,
            treatPaidLikeUnpaid: true,
            pointCalculationRightAnswer: 'basedOnQuestionCommplexity',
            duelGroupWinnerPayout: 'fixedPayoutValue',
            groupPrizePaidOutTo: 'allWinners',
            questionComplexity: 'very',
            fixedValueAmount: 5.50,
            fixedValueCurrency: 'MONEY',
            percentageOfJackpot: 50.5,
            payoutToEveryXWinner: 10000,
            gameLevelStructure: '{}',
            payoutStructureUsed: '{}',
            winningComponent: 1,
            singleWinningComponent: 1,
            randomWinningComponent: 1,
            paymentCostForTheWinningComponent: 100,
            bonusCostForTheUnpaidWinningComponent: 100,
            specialPrice: true,
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
            advertisementDuration: 5,
            voucher: 1,
            voucherProviderId: 1
        },
        playerEntryFeeType: "step",
        playerEntryFeeSettings: {
            moneyMin: 10.0,
            moneyMax: 1000.0,
            moneyIncrement: 0.01,
            bonuspointsMin: 5,
            bonuspointsMax: 500,
            bonuspointsIncrement: 1,
            creditsMin: 1,
            creditsMax: 100,
            creditsIncrement: 1
        },
        playerEntryFeeValues: {
            money: [1.0, 2.2, 3.0],
            bonusPoints: [1, 2, 3],
            credits: [1, 2, 3],
        },
        advertisement: 1,
        adsFrequency: 'beforeTheGame',
        adsFrequencyAmount: 10,
        advertisementProviderId: 1,
        isAutoValidationEnabled: 1,
        prizes: 'prizes',
        gameType: 'tournament',
        isClosed: 0,
        hideCategories: 1,
        entryFeeBatchSize: 1,
        isMultiplePurchaseAllowed: 1,
        instantAnswerFeedbackDelay: 0,
        isPromotion: 0,
        isRegionalLimitationEnabled: 0,
        tenantId: DataIds.TENANT_1_ID
    },
    GAME_INACTIVE: {
        id: DataIds.GAME_INACTIVE_ID,
        title: 'Game inactive title',
        iconId: DataIds.MEDIA_2_ID,
        description: 'Game inactive description',
        imageId: DataIds.MEDIA_2_ID,
        gameModuleId: 2,
        startDate: DateUtils.isoNow(),
        endDate: DateUtils.isoFuture(),
        isMoneyGame: 2,
        isLiveGame: 2,
        isInternationalGame: 2,
        isTenantLocked: 1,
        questionTypeSpread: 'percentage',
        complexitySpread: 'percentage',
        numberOfQuestionsToPlay: 1,
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
        complexityLevel: {
            '1': 10,
            '2': 20,
            '3': 30
        },
        status: Game.constants().STATUS_INACTIVE,
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
                liveTournamentId: DataIds.LIVE_TOURNAMENT_2_ID,
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
                skipQuestionAmount: 10,
                special: true,
                weight: 1.2,
                bannerMediaId: DataIds.MEDIA_2_ID
            },
            gameTournament: {
                duelType: 'knockOut',
                type: 'normal',
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
        jokerConfiguration: [
            {
                type: 'HINT',
                enabled: true,
                availableCount: 10,
                price: 10,
                currency: 'BONUS'
            },
            {
                type: 'FIFTY_FIFTY',
                enabled: true,
                availableCount: 10,
                price: 20,
                currency: 'CREDIT'
            },
            {
                type: 'SKIP',
                enabled: true
            },
            {
                type: 'IMMEDIATE_ANSWER',
                enabled: true,
                availableCount: 10,
                price: 30,
                currency: 'MONEY',
                displayTime: 5
            }
        ],
        resultConfiguration: {
            pointCalculator: true,
            alternativeBonusPointsPerCorrect: 5,
            bonusPointsPerRightAnswer: 5,
            calculatingBonuspointsPerRightAnswer: 10,
            treatPaidLikeUnpaid: true,
            pointCalculationRightAnswer: 'basedOnQuestionCommplexity',
            duelGroupWinnerPayout: 'fixedPayoutValue',
            groupPrizePaidOutTo: 'allWinners',
            questionComplexity: 'very',
            fixedValueAmount: 5.50,
            fixedValueCurrency: 'MONEY',
            percentageOfJackpot: 50.5,
            payoutToEveryXWinner: 10000,
            gameLevelStructure: '{}',
            payoutStructureUsed: '{}',
            winningComponent: 1,
            singleWinningComponent: 1,
            randomWinningComponent: 1,
            paymentCostForTheWinningComponent: 100,
            bonusCostForTheUnpaidWinningComponent: 100,
            specialPrice: true,
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
            advertisementDuration: 5,
            voucher: 1,
            voucherProviderId: 1
        },
        playerEntryFeeType: "step",
        playerEntryFeeSettings: {
            moneyMin: 10.0,
            moneyMax: 1000.0,
            moneyIncrement: 0.01,
            bonuspointsMin: 5,
            bonuspointsMax: 500,
            bonuspointsIncrement: 1,
            creditsMin: 1,
            creditsMax: 100,
            creditsIncrement: 1
        },
        playerEntryFeeValues: {
            money: [1.0, 2.2, 3.0],
            bonusPoints: [1, 2, 3],
            credits: [1, 2, 3],
        },
        advertisement: 1,
        adsFrequency: 'beforeTheGame',
        adsFrequencyAmount: 10,
        advertisementProviderId: 1,
        isAutoValidationEnabled: 1,
        prizes: 'prizes',
        gameType: 'tournament',
        isClosed: 0,
        hideCategories: 1,
        entryFeeBatchSize: 1,
        isMultiplePurchaseAllowed: 1,
        instantAnswerFeedbackDelay: 0,
        isPromotion: 0,
        isRegionalLimitationEnabled: 0,
        tenantId: DataIds.TENANT_1_ID
    },

    GAME_MODULE_1: {
        id: DataIds.GAME_MODULE_1_ID,
        name: 'gm 1"\'fd',
        key: 'test 1'
    },
    GAME_MODULE_2: {
        id: DataIds.GAME_MODULE_2_ID,
        name: 'gm 2"\'fd',
        key: 'test 2',
        parentId: DataIds.GAME_MODULE_1_ID
    },
    GAME_MODULE_TEST: {
        id: DataIds.GAME_MODULE_TEST_ID,
        name: 'gm 2"\'fd',
        key: 'test'
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
    GAME_1_HAS_POOL_2: {
        gameId: DataIds.GAME_1_ID,
        poolId: DataIds.POOL_2_ID
    },
    GAME_2_HAS_POOL_2: {
      gameId: DataIds.GAME_2_ID,
      poolId: DataIds.POOL_2_ID
    },

    GAME_1_HAS_QUESTION_TEMPLATE_1: {
      gameId: DataIds.GAME_1_ID,
      questionTemplateId: DataIds.QUESTION_TEMPLATE_NEW_ID,
      amount: 1,
      order: 2,
    },
    GAME_2_HAS_QUESTION_TEMPLATE_2: {
      gameId: DataIds.GAME_2_ID,
      questionTemplateId: DataIds.QUESTION_TEMPLATE_APPROVED_ID,
      amount: 1,
      order: 2,
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
      rightAnswerPercentage: 90.00,
      isFree: 1,
      entryFeeAmount: 1.1,
      entryFeeCurrency: GameHasWinningComponent.constants().ENTRY_FEE_CURRENCY_MONEY
    },
    GAME_2_HAS_WINNING_COMPONENT_2: {
      gameId: DataIds.GAME_2_ID,
      winningComponentId: DataIds.WINNING_COMPONENT_2_ID,
      rightAnswerPercentage: 80.00,
      isFree: 0,
      entryFeeAmount: 2.1,
      entryFeeCurrency: GameHasWinningComponent.constants().ENTRY_FEE_CURRENCY_BONUS
    },

    cleanManyToMany: function (done) {
        var self = this;
        RdbmsService.load()
            .removeSeries(GameHasPool, [self.GAME_1_HAS_POOL_1, self.GAME_1_HAS_POOL_2, self.GAME_2_HAS_POOL_2], 'gameId')
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
            .createSeries(GameHasPool, [self.GAME_1_HAS_POOL_1, self.GAME_1_HAS_POOL_2, self.GAME_2_HAS_POOL_2], 'gameId')
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
            .removeSeries(Game, [self.GAME_1, self.GAME_2, self.GAME_NO_DEPENDENCIES, self.GAME_INACTIVE])
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
            .createSeries(Game, [self.GAME_1, self.GAME_2, self.GAME_NO_DEPENDENCIES, self.GAME_INACTIVE])
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
