package de.ascendro.f4m.service.result.engine.util;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.analytics.model.GameEndEvent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.country.nogambling.NoGamblingCountry;
import de.ascendro.f4m.server.event.log.EventLogAerospikeDao;
import de.ascendro.f4m.server.game.GameResponseSanitizer;
import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.result.*;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.engine.model.*;
import de.ascendro.f4m.service.game.selection.model.game.*;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.EventLog;
import de.ascendro.f4m.service.json.model.EventLogType;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.result.engine.client.ServiceCommunicator;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.dao.*;
import de.ascendro.f4m.service.result.engine.dao.ResultEngineAerospikeDao.UpdateResultsAction;
import de.ascendro.f4m.service.result.engine.model.*;
import de.ascendro.f4m.service.result.engine.model.get.CompletedGameListResponse;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsStatus;
import de.ascendro.f4m.service.result.engine.model.notification.GameEndNotification;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.random.RandomUtil;
import de.ascendro.f4m.service.winning.model.UserWinning;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.*;

/**
 * Utility functions for result engine.
 */
public class ResultEngineUtilImpl implements ResultEngineUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultEngineUtilImpl.class);

    private static final Comparator<UserResults> USER_RESULTS_COMPARATOR =
            new UserResultsComparator();

    private final ResultEngineAerospikeDao resultEngineAerospikeDao;
    private final CompletedGameHistoryAerospikeDao completedGameHistoryAerospikeDao;
    private final CommonGameHistoryDao gameHistoryDao;
    private final QuestionStatisticsAerospikeDao questionStatisticsAerospikeDao;
    private final ResultEngineConfig config;
    private final EventLogAerospikeDao eventLogAerospikeDao;
    private final JsonMessageUtil jsonMessageUtil;
    private final ServiceCommunicator serviceCommunicator;
    private final GameStatisticsDao gameStatisticsDao;
    private final CommonUserWinningAerospikeDao userWinningDao;
    private final RandomUtil randomUtil;
    private final CommonMultiplayerGameInstanceDao multiplayerGameInstanceDao;
    private final CommonProfileAerospikeDao profileDao;
    private final CommonGameInstanceAerospikeDao gameInstanceDao;
    private final JsonUtil jsonUtil;
    private final Tracker tracker;
    private final MultiplayerGameResultElasticDao multiplayerGameResultDao;

    private final NoGamblingCountry noGamblingCountry;

    private GameResponseSanitizer gameResponseSanitizer;

    @Inject
    public ResultEngineUtilImpl(ResultEngineAerospikeDao resultEngineAerospikeDao,
                                CompletedGameHistoryAerospikeDao completedGameHistoryAerospikeDao,
                                QuestionStatisticsAerospikeDao questionStatisticsAerospikeDao,
                                EventLogAerospikeDao eventLogAerospikeDao, ResultEngineConfig config,
                                JsonMessageUtil jsonMessageUtil, ServiceCommunicator serviceCommunicator,
                                GameStatisticsDao gameStatisticsDao, CommonUserWinningAerospikeDao userWinningDao,
                                RandomUtil randomUtil, CommonMultiplayerGameInstanceDao multiplayerGameInstanceDao,
                                CommonGameInstanceAerospikeDao gameInstanceDao, JsonUtil jsonUtil, Tracker tracker,
                                CommonGameHistoryDao gameHistoryDao, CommonProfileAerospikeDao profileDao,
                                MultiplayerGameResultElasticDao multiplayerGameResultDao, NoGamblingCountry noGamblingCountry,
                                GameResponseSanitizer gameResponseSanitizer) {
        this.resultEngineAerospikeDao = resultEngineAerospikeDao;
        this.completedGameHistoryAerospikeDao = completedGameHistoryAerospikeDao;
        this.eventLogAerospikeDao = eventLogAerospikeDao;
        this.questionStatisticsAerospikeDao = questionStatisticsAerospikeDao;
        this.config = config;
        this.jsonMessageUtil = jsonMessageUtil;
        this.serviceCommunicator = serviceCommunicator;
        this.gameStatisticsDao = gameStatisticsDao;
        this.userWinningDao = userWinningDao;
        this.randomUtil = randomUtil;
        this.multiplayerGameInstanceDao = multiplayerGameInstanceDao;
        this.gameInstanceDao = gameInstanceDao;
        this.jsonUtil = jsonUtil;
        this.tracker = tracker;
        this.gameHistoryDao = gameHistoryDao;
        this.profileDao = profileDao;
        this.multiplayerGameResultDao = multiplayerGameResultDao;
        this.noGamblingCountry = noGamblingCountry;
        this.gameResponseSanitizer = gameResponseSanitizer;
    }

    @Override
    public Results getResults(String gameInstanceId) throws F4MEntryNotFoundException {
        Results results = resultEngineAerospikeDao.getResults(gameInstanceId);
        if (results != null) {
            ApiProfileBasicInfo profileBasicInfo = profileDao.getProfileBasicInfo(results.getUserId());
            results.setUserInfo(jsonUtil.toJsonElement(profileBasicInfo));
        }
        return results;
    }

    @Override
    public void storeResults(String gameInstanceId, Results results, boolean overwriteExisting) {
        resultEngineAerospikeDao.saveResults(gameInstanceId, results, overwriteExisting);
    }

    @Override
    public void saveGameEndEvent(Game game, ClientInfo clientInfo) {
        GameEndEvent gameEndEvent = new GameEndEvent();
        gameEndEvent.setGameId(game.getGameId());
        tracker.addEvent(clientInfo, gameEndEvent);
    }


    @Override
    public void savePlayerGameEndEvent(GameInstance gameInstance, Results results, ClientInfo clientInfo) {
        PlayerGameEndEvent gameEndEvent = new PlayerGameEndEvent();
        gameEndEvent.setGameId(gameInstance.getGame().getGameId());
        gameEndEvent.setTotalQuestions(results.getTotalQuestionCount());
        gameEndEvent.setTotalCorrectQuestions(results.getCorrectAnswerCount());
        gameEndEvent.setTotalIncorrectQuestions(results.getTotalQuestionCount() - results.getCorrectAnswerCount());
        gameEndEvent.setPlayerHandicap(gameInstance.getUserHandicap());
        gameEndEvent.setSecondsPlayed(gameInstance.getGameDuration());
        gameEndEvent.setGameType(gameInstance.getGame().getType());

        long answerTime = (long)results.getResultItems().get(ResultType.ANSWER_TIME).getAmount();
        gameEndEvent.setAverageAnswerSpeed(answerTime/results.getTotalQuestionCount());

        results.getAnswerResults().values().forEach(a -> {
            de.ascendro.f4m.server.analytics.model.Question eventQuestion = new de.ascendro.f4m.server.analytics.model.Question();
            Question question = gameInstance.getQuestion(a.getQuestionIndex());

            eventQuestion.setQuestionId(Long.valueOf(question.getId()));
            eventQuestion.setAnswerIsCorrect(a.getQuestionInfo().isAnsweredCorrectly());
            eventQuestion.setAnswerSpeed(getAnswerTime(question,
                    gameInstance.getGameState().getAnswers()[a.getQuestionIndex()]));
            eventQuestion.setOwnerId(question.getCreatorResourceId());
            gameEndEvent.addQuestion(eventQuestion);

            ResultConfiguration resultConfiguration = gameInstance.getGame().getResultConfiguration();
            if (isFastCorrectAnswer(a, resultConfiguration)) {
                gameEndEvent.setNumOfCorrectQuestionsFastAnswered(gameEndEvent.getNumOfCorrectQuestionsFastAnswered() + 1);
            }
        });

        tracker.addEvent(clientInfo, gameEndEvent);
    }

    /**
     * returns true if user response time is within preconfigured
     * quickResponseBonusPointsMs; the question needs to be answered correctly
     * otherwise the method returns false.
     */
    private boolean isFastCorrectAnswer(AnswerResults answer, ResultConfiguration resultConfig) {
        boolean isFastCorrectAnswer = false;
        if (null != answer && answer.getQuestionInfo().isAnsweredCorrectly()) {
            long quickResponseMs = Optional.ofNullable(resultConfig.getBonusPointsForQuickAnswerMs())
                    .orElse(config.getPropertyAsInteger(ResultEngineConfig.DEFAULT_BONUS_POINTS_FOR_QUICK_RESPONSE_MS));
            long totalAnswerMsT = (long) answer.getResultItems().get(ResultType.ANSWER_TIME).getAmount();
            isFastCorrectAnswer = totalAnswerMsT <= quickResponseMs;
        }
        return isFastCorrectAnswer;
    }

    private void saveMultiplayerGameEndEvent(GameInstance gameInstance, Results results) {
        ClientInfo clientInfo = new ClientInfo(results.getTenantId(), results.getUserId());
        clientInfo.setAppId(results.getAppId());
        clientInfo.setIp(results.getClientIp());

        MultiplayerGameEndEvent multiplayerGameEndEvent =  new MultiplayerGameEndEvent();
        multiplayerGameEndEvent.setGameId(gameInstance.getGame().getGameId());
        multiplayerGameEndEvent.setPlacement(Long.valueOf(results.getPlacement()));
        multiplayerGameEndEvent.setGameInstanceId(gameInstance.getId());
        multiplayerGameEndEvent.setGameOutcome(results.getGameOutcome(), gameInstance.getGame().getType());

        String duelOpponentGameInstanceId = results.getDuelOpponentGameInstanceId();

        if (duelOpponentGameInstanceId != null) {
            GameInstance duelOpponentGameInstance = gameInstanceDao.getGameInstance(duelOpponentGameInstanceId);
            multiplayerGameEndEvent.setOpponentId(duelOpponentGameInstance.getUserId());
        }

        BigDecimal jackpotWinning = results.getJackpotWinning();
        if (jackpotWinning != null) {
            multiplayerGameEndEvent.setJackpot(jackpotWinning);
            multiplayerGameEndEvent.setJackpotCurrency(results.getJackpotWinningCurrency().name());
        }

        tracker.addEvent(clientInfo, multiplayerGameEndEvent);
    }

    @Override
    public void saveGameHistoryForUser(GameInstance gameInstance, Results results, GameHistoryUpdateKind updateKind) {
        ZonedDateTime endDateTime = gameInstance.getEndDateTime();
        if (endDateTime == null) {
            LOGGER.warn("Game Instance does not have endDateTime set, using now()");
            endDateTime = DateTimeUtil.getCurrentDateTime();
        }
        gameInstance.setGame(gameResponseSanitizer.removeExcessInfo(gameInstance.getGame()));
        CompletedGameHistoryInfo completedGameHistoryInfo = new CompletedGameHistoryInfo(gameInstance, results, endDateTime);
        GameType gameType = gameInstance.getGame().getType();
        if (gameType.isMultiUser()) {
            addMultiplayerData(completedGameHistoryInfo, gameInstance, results, updateKind);
        } else if (!gameResponseSanitizer.needsRemovingExcessInfo(gameInstance.getGame())) {
            completedGameHistoryInfo.setPlayingRegions(gameInstance.getGame().getPlayingRegions());
            completedGameHistoryInfo.setPoolIds(gameInstance.getGame().getAssignedPools());
        }

        String duelOpponentGameInstanceId = results.getDuelOpponentGameInstanceId();
        if (duelOpponentGameInstanceId != null) {
            GameInstance duelOpponentGameInstance = gameInstanceDao.getGameInstance(duelOpponentGameInstanceId);
            ApiProfileBasicInfo opponent = profileDao.getProfileBasicInfo(duelOpponentGameInstance.getUserId());
            completedGameHistoryInfo.addOpponent(opponent);
        }

        completedGameHistoryAerospikeDao.saveResultsForHistory(gameInstance.getUserId(), results.getTenantId(), gameType,
                endDateTime.toLocalDate(), completedGameHistoryInfo, updateKind);
    }

    private void addMultiplayerData(CompletedGameHistoryInfo completedGameHistoryInfo, GameInstance gameInstance,
                                    Results results, GameHistoryUpdateKind updateKind) {
        CustomGameConfig gameConfig = multiplayerGameInstanceDao.getConfig(gameInstance.getMgiId());
        if (!gameResponseSanitizer.needsRemovingExcessInfo(gameInstance.getGame())) {
            completedGameHistoryInfo.setPlayingRegions(gameConfig.getPlayingRegions());
            completedGameHistoryInfo.setPoolIds(gameConfig.getPoolIds());
        }
        String creatorId = gameConfig.getGameCreatorId();
        if (creatorId != null) {
            completedGameHistoryInfo.setCreator(profileDao.getProfileBasicInfo(creatorId));
        }
        String inviterId = multiplayerGameInstanceDao.getInviterId(gameInstance.getUserId(), gameInstance.getMgiId());
        completedGameHistoryInfo.setIsPublicGame(gameConfig.getPublicGame());
        completedGameHistoryInfo.setGameEntryFeeCurrency(gameConfig.getEntryFeeCurrency());
        completedGameHistoryInfo.setGameEntryFeeAmount(gameConfig.getEntryFeeAmount());
        if (inviterId != null) {
            completedGameHistoryInfo.setInviter(profileDao.getProfileBasicInfo(inviterId));
        }
        if (updateKind == GameHistoryUpdateKind.MULTIPLAYER_GAME_FINISH) {
            completedGameHistoryInfo.setMultiplayerGameFinished(true);
            completedGameHistoryInfo.setPlacement(results.getPlacement());
            completedGameHistoryInfo.setGameOutcome(results.getGameOutcome());
            BigDecimal prizeWonAmount = results.getJackpotWinning();
            Currency prizeWonCurrency = results.getJackpotWinningCurrency();
            if (prizeWonAmount != null && prizeWonCurrency != null) {
                completedGameHistoryInfo.setPrizeWonAmount(prizeWonAmount);
                completedGameHistoryInfo.setPrizeWonCurrency(prizeWonCurrency);
            }
            completedGameHistoryInfo.setPrizeWonCurrency(results.getJackpotWinningCurrency());
        }
    }

    @Override
    public void storeUserWinningComponent(ClientInfo clientInfo, Results results, String userWinningComponentId, boolean paid) {
        GameInstance gameInstance = gameInstanceDao.getGameInstance(results.getGameInstanceId());
        Game game = gameInstance.getGame();
        ResultConfiguration resultConfig = game == null ? null : game.getResultConfiguration();
        int bonusPoints = calculateBonusPoints(clientInfo, results, gameInstance, resultConfig, paid, game);

        Results updatedResults = updateResults(gameInstance.getId(), new UpdateResultsAction(jsonMessageUtil) {
            @Override
            public Results updateResults(Results results) {
                if (userWinningComponentId != null) {
                    results.setUserWinningComponentId(userWinningComponentId);
                }
                results.addResultItem(new ResultItem(ResultType.BONUS_POINTS, bonusPoints));
                if (bonusPoints > 0) {
                    results.addUserInteraction(UserInteractionType.BONUS_POINT_TRANSFER);
                }
                results.removeUserInteraction(UserInteractionType.BUY_WINNING_COMPONENT);
                return results;
            }
        });

        // Update completed game history item for user
        saveGameHistoryForUser(gameInstance, updatedResults, GameHistoryUpdateKind.BONUS_POINTS);
    }


    private int calculateBonusPoints(ClientInfo clientInfo, Results results, GameInstance gameInstance,
                                     ResultConfiguration resultConfig, boolean paid, Game game) {
        // Bonus points
        int bonusPoints = getBonusPoints(results, paid, resultConfig);
        results.addResultItem(new ResultItem(ResultType.BONUS_POINTS, bonusPoints));
        if (bonusPoints > 0) {
            results.addUserInteraction(UserInteractionType.BONUS_POINT_TRANSFER);
            userWinningDao.saveUserWinning(clientInfo.getAppId(), results.getUserId(), new UserWinning(gameInstance.getId(), game,
                    BigDecimal.valueOf(results.getBonusPointsWon()), Currency.BONUS));
        }
        return bonusPoints;
    }

    /**
     * Calculate bonus points based on settings and game point calculation results.
     */
    private int getBonusPoints(Results results, boolean paid, ResultConfiguration resultConfig) {
        if (resultConfig == null || resultConfig.isPointCalculator() == null || resultConfig.isPointCalculator()) {
            return getPointCalculatorBonusPoints(results, resultConfig, paid);
        } else {
            int alternativeBonusPointsPerCorrectQuestion = resultConfig.getAlternativeBonusPointsPerCorrectAnswer() == null
                    ? config.getPropertyAsInteger(ResultEngineConfig.DEFAULT_ALTERNATIVE_BONUS_POINTS_PER_CORRECT_ANSWER)
                    : resultConfig.getAlternativeBonusPointsPerCorrectAnswer();
            return results.getCorrectAnswerCount() * alternativeBonusPointsPerCorrectQuestion;
        }
    }

    private int getPointCalculatorBonusPoints(Results results, ResultConfiguration resultConfig, boolean paid) {
        // Calculate bonus points
        boolean treatPaidAsUnpaid = resultConfig == null || resultConfig.isTreatPaidLikeUnpaid() == null
                ? false : resultConfig.isTreatPaidLikeUnpaid();
        int bonusPoints;
        if (treatPaidAsUnpaid || !paid) {
            LOGGER.debug("getPointCalculatorBonusPoints 123");
//            int bonusPointsPerCorrectAnswer = config.getPropertyAsInteger(ResultEngineConfig.DEFAULT_BONUS_POINTS_PER_CORRECT_ANSWER_FOR_UNPAID);
            int bonusPointsPerCorrectAnswer = resultConfig == null || resultConfig.getBonusPointsPerCorrectAnswerForUnpaid() == null
                    ? config.getPropertyAsInteger(ResultEngineConfig.DEFAULT_BONUS_POINTS_PER_CORRECT_ANSWER_FOR_UNPAID)
                    : resultConfig.getBonusPointsPerCorrectAnswerForUnpaid();
            bonusPoints = results.getCorrectAnswerCount() * bonusPointsPerCorrectAnswer;
        } else {
//            int bonusPointsPerGamePoint = config.getPropertyAsInteger(ResultEngineConfig.DEFAULT_BONUS_POINTS_PER_GAME_POINT_FOR_PAID);
            int bonusPointsPerGamePoint = resultConfig == null || resultConfig.getBonusPointsPerGamePointForPaid() == null
                    ? config.getPropertyAsInteger(ResultEngineConfig.DEFAULT_BONUS_POINTS_PER_GAME_POINT_FOR_PAID)
                    : resultConfig.getBonusPointsPerGamePointForPaid();
            bonusPoints = roundUpToInt(results.getResultItems().get(ResultType.TOTAL_GAME_POINTS_WITH_BONUS).getAmount() * bonusPointsPerGamePoint);
        }

//         Add bonus points for all correct answers
        if (getCorrectAnswerPointCalculationType(resultConfig) != CorrectAnswerPointCalculationType.DONT_USE
                && results.getCorrectAnswerCount() == results.getTotalQuestionCount()) { // All questions correct
            bonusPoints += resultConfig == null || resultConfig.getBonusPointsForAllCorrectAnswers() == null
                    ? 0
                    : resultConfig.getBonusPointsForAllCorrectAnswers();
        }

        return bonusPoints;
    }

    @Override
    public void storeTotalBonusPoints(String gameInstanceId, BigDecimal bonusPoints) {
        if (bonusPoints != null) {
            updateResults(gameInstanceId, new UpdateResultsAction(jsonMessageUtil) {
                @Override
                public Results updateResults(Results results) {
                    if (! results.getResultItems().containsKey(ResultType.TOTAL_BONUS_POINTS)) {
                        results.addResultItem(new ResultItem(ResultType.TOTAL_BONUS_POINTS, bonusPoints.doubleValue(), Currency.BONUS));
                    }
                    return results;
                }
            });
        }
    }

    private Results updateResults(String gameInstanceId, UpdateResultsAction updateResultsAction) {
        return resultEngineAerospikeDao.updateResults(gameInstanceId, updateResultsAction);
    }

    @Override
    public void removeUserInteraction(String gameInstanceId, UserInteractionType type) {
        updateResults(gameInstanceId, new UpdateResultsAction(jsonMessageUtil) {
            @Override
            public Results updateResults(Results results) {
                results.removeUserInteraction(type);
                return results;
            }
        });
    }

    @Override
    public Results calculateResults(GameInstance gameInstance, ClientInfo clientInfo) {
        // If game is finished, answer array length has to be same as question array length (though may contain null elements)
        Answer[] answers = gameInstance.getGameState().getAnswers();
        Validate.isTrue(gameInstance.getNumberOfQuestionsIncludingSkipped() == answers.length);

        Results results = new Results(gameInstance.getId(), gameInstance.getGame().getGameId(), gameInstance.getUserId(),
                clientInfo.getTenantId(), clientInfo.getAppId(), clientInfo.getIp(),
                gameInstance.getMgiId(), gameInstance.getUserHandicap(), true);
        ResultConfiguration resultConfig = gameInstance.getGame().getResultConfiguration();
        boolean pointCalculator = resultConfig == null || resultConfig.isPointCalculator() == null || resultConfig.isPointCalculator();
        Game game = gameInstance.getGame();
        Set<Integer> skippedQuestions = gameInstance.getJokersUsed(JokerType.SKIP);
        if (resultConfig != null && !resultConfig.isJackpotCalculateByEntryFee() && game.getEntryFeeCurrency() != Currency.MONEY && game.isTournament()) {
            results.setJackpotWinning(Currency.MONEY);
        }else results.setJackpotWinning(gameInstance.getEntryFeeCurrency());
        // Entry fee
        CustomGameConfig customGameConfig = gameInstance.getMgiId() != null
                ? multiplayerGameInstanceDao.getConfig(gameInstance.getMgiId()) : null;
        if (!GameUtil.isFreeGame(game, customGameConfig)) {
            results.setEntryFeePaid(gameInstance.getEntryFeeAmount(), gameInstance.getEntryFeeCurrency());
        }
        if (customGameConfig != null) {
            results.setExpiryDateTime(customGameConfig.getExpiryDateTime());
        }
        results.setEndDateTime(gameInstance.getEndDateTime());
        // Correct answers
        calculateCorrectlyAnsweredQuestionsAndAnswerTime(results, gameInstance, skippedQuestions, pointCalculator);

        // Statistics
        calculateStatistics(results);

        // Calculate points and handicap only if point calculator is turned on
        results.setTrainingMode(gameInstance.isTrainingMode());
        if (pointCalculator) {
            // Game points (maximum and earned)
            Map<Integer, AnswerResults> correctAnswers = results.getCorrectAnswerResults();
            int maxGamePoints = 0;
            for (int i = 0 ; i < answers.length ; i++) {
                if (!skippedQuestions.contains(i)) {
                    maxGamePoints += calculatePerAnswerGamePoints(correctAnswers.get(i), gameInstance.getQuestion(i), answers[i], resultConfig);
                }
            }
            results.addResultItem(new ResultItem(ResultType.MAXIMUM_GAME_POINTS, maxGamePoints));
            double totalGamePoints = calculateTotalGamePoints(results, correctAnswers);

            // Game points with bonus
            calculateTotalGamePointsWithBonus(results, totalGamePoints, correctAnswers, answers.length);

            // Handicap points
            double handicapPoints = calculateHandicapPoints(results, totalGamePoints, results.getUserHandicap());

            // New Handicap
            calculateNewHandicap(gameInstance, results, handicapPoints, results.getUserHandicap(), maxGamePoints, gameInstance.isTrainingMode());
        }
        calculateHandicapRange(results, game);

        // Calculate if user is eligible to winnings
        if (!gameInstance.getGameState().isCancelled()) {
            calculateAccessToWinningComponent(results, game, clientInfo);
        }

        // Calculate if user has won a special prize
        if (!UserRole.isAnonymous(clientInfo)) {
            calculateSpecialPrizeWon(results, game.getGameId(), resultConfig);
        }

        // Request bonus point retrieval
        results.addUserInteraction(UserInteractionType.BONUS_POINT_ENQUIRY);

        // If there is no winning component buy choice, bonus points have to be calculated immediately
        if (!results.getUserInteractions().contains(UserInteractionType.BUY_WINNING_COMPONENT)) {
            calculateBonusPoints(clientInfo, results, gameInstance, resultConfig, false, game);
        }

        ResultItem totalBonusPointsResultItem = results.getResultItems().get(ResultType.TOTAL_BONUS_POINTS);
        if (totalBonusPointsResultItem != null) {
            BigDecimal totalBonusPoints = new BigDecimal(totalBonusPointsResultItem.getAmount());
            // Update total bonus points
            // as per task #10659, should store BONUS_POINTS + TOTAL_BONUS_POINTS

            totalBonusPoints = totalBonusPoints.add(BigDecimal.valueOf(results.getResultItems().get(ResultType.BONUS_POINTS).getAmount()));
            storeTotalBonusPoints(results.getGameInstanceId(), totalBonusPoints);
            totalBonusPointsResultItem.setAmount(totalBonusPoints.doubleValue());
            results.addResultItem(totalBonusPointsResultItem);
        }

        return results;
    }

    /**
     * Add list of answered questions to the results. Also, add correct answer count, total question count,
     * answer time and total answer time result items.
     */
    private void calculateCorrectlyAnsweredQuestionsAndAnswerTime(Results results, GameInstance gameInstance, Set<Integer> skippedQuestions, boolean pointCalculator) {
        Answer[] answers = gameInstance.getGameState().getAnswers();
        long totalAnswerMsT = 0;
        for (int i = 0; i < answers.length; i++) {
            Question question = gameInstance.getQuestion(i);

            AnswerResults answerResults;
            boolean answeredCorrectly = false;
            if (skippedQuestions.contains(i)) {
                answerResults = results.addSkippedAnswer(i, question);
            } else {
                Answer answer = answers[i];
                if (isAnsweredCorrectly(question, answer)) {
                    answerResults = results.addCorrectAnswer(i, question, answer);
                    answeredCorrectly = true;
                } else {
                    answerResults = results.addIncorrectAnswer(i, question, answer);
                }

                Long answerMsT = getAnswerTime(question, answer);
                if (answerMsT != null) {
                    answerResults.addResultItem(new ResultItem(ResultType.ANSWER_TIME, answerMsT));
                    totalAnswerMsT += answerMsT;
                }
            }
            if (pointCalculator && !answeredCorrectly) {
                answerResults.addResultItem(new ResultItem(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER, 0));
                answerResults.addResultItem(new ResultItem(ResultType.GAME_POINTS_FOR_SPEED, 0));
                answerResults.addResultItem(new ResultItem(ResultType.TOTAL_GAME_POINTS, 0));
            }
        }
        results.addResultItem(new ResultItem(ResultType.TOTAL_QUESTIONS, answers.length - skippedQuestions.size()));
        results.addResultItem(new ResultItem(ResultType.CORRECT_ANSWERS, results.getCorrectAnswerCount()));
        results.addResultItem(new ResultItem(ResultType.ANSWER_TIME, totalAnswerMsT));
    }

    /**
     * Calculate answer time. Calculated only for provided answers.
     */
    private Long getAnswerTime(Question question, Answer answer) {
        if (answer != null) {

            final long[] clientMsT = answer.getClientMsT();
            final Long[] prDelMsT = answer.getPrDelMsT();
            final long[] maxMsT = question.getAnswerMaxTimes();
            final long[] serverMsT = answer.getServerMsT();

            long totalAnswerMsT = 0;
            for (int step = 0; step < question.getStepCount(); step++) {
                if (clientMsT != null && step < clientMsT.length) {
                    final Long prDel = prDelMsT != null && step < prDelMsT.length ? prDelMsT[step] : null;
                    totalAnswerMsT += calculateStepAnswerTime(clientMsT[step], serverMsT[step], prDel, maxMsT[step]);
                } else {
                    // There is no response registered => use max time for particular step
                    totalAnswerMsT += maxMsT[step];
                }
            }
            return totalAnswerMsT;
        } else {
            return question.getTotalAnswerMaxTimes();
        }
    }

    private long calculateStepAnswerTime(long clientMsT, long serverMsT, Long prDelMsT, long maxMsT) {
        // There is an answer => calculate time
        // Choose client for answer time measurement, if it does not exceed server time plus propagation delay limits, otherwise use server time.
        long answerTime = ((clientMsT >= serverMsT - getPropagationDelay(prDelMsT)) && (clientMsT <= serverMsT)) ? clientMsT : serverMsT;
        if (answerTime > maxMsT) {
            answerTime = maxMsT;
        }
        return answerTime;
    }

    /**
     * Determine if question is answered correctly.
     */
    private boolean isAnsweredCorrectly(Question question, Answer answer) {
        // There has to be an answer
        if (answer == null) {
            return false;
        }
        // Validate that all steps answered in time
        long[] maxMsT = question.getAnswerMaxTimes();
        long[] serverMsT = answer.getServerMsT();

        if (ArrayUtils.isNotEmpty(serverMsT)) {
            Long[] prDelMsT = answer.getPrDelMsT();
            for (int i = 0; i < maxMsT.length; i++) {
                long maxServerMsT = maxMsT[i] + getPropagationDelay(prDelMsT == null ? null : prDelMsT[i]); // includes propagation delay
                if (serverMsT[i] > maxServerMsT) {
                    return false; // If answer server time exceeds max server time => don't accept answer
                }
            }
        } else {//has no answer times
            return false;
        }
        // Now validate answers. Order matters for sort questions, so no sorting before comparing.
        return Arrays.equals(question.getCorrectAnswers(), answer.getAnswers());
    }

    /**
     * Get the propagation delay: use the given, if provided, otherwise take default from config.
     */
    private long getPropagationDelay(Long measuredPropagationDelay) {
        return measuredPropagationDelay == null
                ? config.getPropertyAsLong(ResultEngineConfig.DEFAULT_PROPAGATION_DELAY) : measuredPropagationDelay;
    }

    /**
     * Add statistical values to the results.
     */
    private void calculateStatistics(Results results) {
        // TODO implement
        results.getAnswerResults().values().forEach(answerResults -> {
            answerResults.addResultItem(new ResultItem(ResultType.ANSWERED_CORRECTLY_PERCENT, 0));
        });
    }

    /**
     * Calculate per-answer game points. If results == <code>null</code>, it will be assumed that question was answered incorrectly.
     * In such case, also answer may be <code>null</code>.
     * @return Maximum number of game points that can be obtained by correctly answering the question.
     */
    private int   calculatePerAnswerGamePoints(AnswerResults results, Question question, Answer answer, ResultConfiguration resultConfig) {
        // Speed game points
        int maxSpeedGamePoints = calculatePerAnswerGamePointsForSpeed(results, answer, resultConfig);
        // Correct answer game points
        CorrectAnswerPointCalculationType calculationType = getCorrectAnswerPointCalculationType(resultConfig);
        LOGGER.debug("calculationType    {}   ", calculationType);
        int maxCorrectAnswerGamePoints = 0;
        yyy(results, 1);
        if (calculationType != CorrectAnswerPointCalculationType.DONT_USE) {
            Map<Integer, Integer> questionComplexityGamePoints =
                    (calculationType == CorrectAnswerPointCalculationType.BASED_ON_QUESTION_COMMPLEXITY && resultConfig != null && resultConfig.getCorrectAnswerQuestionComplexityGamePoints() != null)
                            ? resultConfig.getCorrectAnswerQuestionComplexityGamePoints() : Collections.emptyMap();
            LOGGER.debug("calculationType    {}   {}   {} ", calculationType != CorrectAnswerPointCalculationType.BASED_ON_QUESTION_COMMPLEXITY, calculationType, CorrectAnswerPointCalculationType.BASED_ON_QUESTION_COMMPLEXITY);
            LOGGER.debug("calculationType    {}   {}   {} ", resultConfig != null, resultConfig.getCorrectAnswerQuestionComplexityGamePoints() != null, resultConfig.getCorrectAnswerQuestionComplexityGamePoints());
                maxCorrectAnswerGamePoints = calculatePerAnswerGamePointsForCorrectAnswer(results, question.getComplexity(), questionComplexityGamePoints);
        }
        yyy(results, 2);
        LOGGER.debug("ResultConfiguration resultConfig {} ", resultConfig);
        // Total game points
        if (results != null) { // If answered correctly
            calculatePerAnswerTotalGamePoints(results);
        }
        yyy(results, 3);
        return maxSpeedGamePoints + maxCorrectAnswerGamePoints;
    }

    private void yyy(AnswerResults results, Integer count) {
        try {
            if (results.getResultItems() != null) {
                for (Entry<ResultType, ResultItem> entry : results.getResultItems().entrySet()) {
                    LOGGER.debug("count{}  calculatePerAnswerGamePoints entry {}   ---   {} ", count, entry.getKey(), entry.getValue());
                    if (entry.getKey() == ResultType.GAME_POINTS_FOR_SPEED || entry.getKey() == ResultType.GAME_POINTS_FOR_CORRECT_ANSWER) {
                        LOGGER.debug("count{}   calculatePerAnswerGamePoints entry if {}   ---   {} ", count, entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("ERROR CALCULATE {} ", e.getMessage());
        }
    }

    private CorrectAnswerPointCalculationType getCorrectAnswerPointCalculationType(ResultConfiguration resultConfig) {
        return resultConfig == null || resultConfig.getCorrectAnswerPointCalculationType() == null
                ? CorrectAnswerPointCalculationType.BASED_ON_QUESTION_COMMPLEXITY
                : resultConfig.getCorrectAnswerPointCalculationType();
    }

    /**
     * Calculate the points for answer speed. If results == <code>null</code>, it will be assumed that question was answered incorrectly.
     * In such case also answer may be <code>null</code>.
     * @return Maximum number of speed points that can be obtained by correctly quickly answering the question.
     */
    private int calculatePerAnswerGamePointsForSpeed(AnswerResults results, Answer answer,
                                                     ResultConfiguration resultConfig) {
        int speedPoints = 0;
        if (results != null && answer != null) { // if answered correctly
            long answerMaxMsT = results.getQuestionInfo().getAnswerMaxMsT()[0] == 0 ? 15 : results.getQuestionInfo().getAnswerMaxMsT()[0];
            long totalAnswerMsT = roundingToAnLongValue(0, (long) results.getResultItems().get(ResultType.ANSWER_TIME).getAmount());
            if (totalAnswerMsT <= answerMaxMsT / 3) {
                speedPoints = 3;
            } else if (totalAnswerMsT <= (answerMaxMsT / 3) * 2) {
                speedPoints = 2;
            } else if (totalAnswerMsT <= answerMaxMsT) {
                speedPoints = 1;
            }
                results.addResultItem(new ResultItem(ResultType.GAME_POINTS_FOR_SPEED, speedPoints));
            LOGGER.debug("results.getQuestionInfo().getAnswerMaxMsT {} ", results.getQuestionInfo().getAnswerMaxMsT());
        }
        return 3; // maximum number of points that can be obtained == pointsForQuickAnswer

    }


    /**
     * Calculate the points for correct answer. If results == <code>null</code>, it will be assumed that question was answered incorrectly.
     * @return Maximum number of game points that can be obtained by correctly answering the question.
     */
    private int calculatePerAnswerGamePointsForCorrectAnswer(AnswerResults results, int complexity, Map<Integer, Integer> questionComplexityGamePoints) {
        Integer complexityPoints = questionComplexityGamePoints.get(complexity);
        int points = complexityPoints == null ? complexity : complexityPoints;
        if (results != null) { // if answered correctly
            results.addResultItem(new ResultItem(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER, points));
        }
        return points;
    }

    /**
     * Calculate the total game points for correctly answered questions (sum of speed game points and correct answer game points).
     */
    private void calculatePerAnswerTotalGamePoints(AnswerResults results) {
        int totalPoints = 0;
        if (results.getResultItems() != null) {
            for (Entry<ResultType, ResultItem> entry : results.getResultItems().entrySet()) {
                if (entry.getKey() == ResultType.GAME_POINTS_FOR_SPEED || entry.getKey() == ResultType.GAME_POINTS_FOR_CORRECT_ANSWER) {
                    totalPoints += entry.getValue().getAmount();
                }
            }
        }
        results.addResultItem(new ResultItem(ResultType.TOTAL_GAME_POINTS, totalPoints));
    }

    /**
     * Calculate total game points based on calculations for individual questions.
     */
    private double calculateTotalGamePoints(Results results, Map<Integer, AnswerResults> correctAnswers) {
        double totalGamePoints = 0;
        for (AnswerResults questionResults : correctAnswers.values()) {
            ResultItem questionPoints = questionResults.getResultItems() == null
                    ? null : questionResults.getResultItems().get(ResultType.TOTAL_GAME_POINTS);
            if (questionPoints != null) {
                totalGamePoints += questionPoints.getAmount();
            }
        }
        results.addResultItem(new ResultItem(ResultType.TOTAL_GAME_POINTS, totalGamePoints));
        return totalGamePoints;
    }

    /**
     * Calculate total game points with bonus, based on calculations for individual questions.
     */
    private void calculateTotalGamePointsWithBonus(Results results, double totalGamePoints, Map<Integer, AnswerResults> correctAnswers, int countAllAnswers) {
        double bonusPerCorrectAnswer = config.getPropertyAsDouble(ResultEngineConfig.BONUS_PER_CORRECT_ANSWER);
        double totalGamePointsWithBonus = (totalGamePoints + bonusPerCorrectAnswer * correctAnswers.size()) + (correctAnswers.size() != countAllAnswers ? 0 : 0.1 * countAllAnswers);
        double extraGamePoints = bonusPerCorrectAnswer * correctAnswers.size() + (correctAnswers.size() != countAllAnswers ? 0 : 0.1 * countAllAnswers);
        totalGamePointsWithBonus = roundDownToOneDecimalPlace(2,totalGamePointsWithBonus);
        results.addResultItem(new ResultItem(ResultType.TOTAL_GAME_POINTS_WITH_BONUS, totalGamePointsWithBonus));
        results.addResultItem(new ResultItem(ResultType.EXTRA_GAME_POINT, extraGamePoints));
        LOGGER.debug("calculateTotalGamePointsWithBonus bonusPerCorrectAnswer {} totalGamePointsWithBonusBefore {} " +
                        "totalGamePointsWithBonusAfter  {} ,totalGamePoints {} , bonusPerCorrectAnswer {} , correctAnswers.size() {}" +
                        "countAllAnswers {} extraGamePoints {} ",
                bonusPerCorrectAnswer,
                totalGamePointsWithBonus,
                totalGamePointsWithBonus,
                totalGamePoints,
                bonusPerCorrectAnswer,
                correctAnswers.size()
                , countAllAnswers
                , extraGamePoints
        );
    }

    /**
     * Calculate handicap points.
     */
    private double calculateHandicapPoints(Results results, double totalGamePoints, double oldHandicap) {
        double handicapPoints = roundHalfUpToOneDecimalPlace(2, totalGamePoints * ((100 - oldHandicap) / 200 + 1));
        LOGGER.debug("calculateHandicapPoints handicapPoints {} ", handicapPoints);
        results.addResultItem(new ResultItem(ResultType.HANDICAP_POINTS, handicapPoints));
        return handicapPoints;
    }

    /**
     * Calculate new handicap.
     */
    private void calculateNewHandicap(GameInstance gameInstance, Results results, double handicapPoints, double oldHandicap, int maxGamePoints, boolean trainingMode) {
        double minusHandicapFactorPercent = roundHalfUpToOneDecimalPlace(2, (100 - oldHandicap) / 2);
        double handicapKey = roundHalfUpToOneDecimalPlace(2, (maxGamePoints * (100 - minusHandicapFactorPercent)) / 100);
        double handicapDelta = roundHalfUpToOneDecimalPlace(2, handicapPoints - handicapKey);
        double handicapDeltaFactor = roundHalfUpToOneDecimalPlace(2, config
                .getPropertyAsDouble(handicapDelta < 0 ? ResultEngineConfig.HANDICAP_DELTA_FACTOR_NEGATIVE
                        : ResultEngineConfig.HANDICAP_DELTA_FACTOR_POSITIVE));
        double handicapValue = roundHalfUpToOneDecimalPlace(2, handicapDeltaFactor * handicapDelta);
        double bonusForCorrectAnswers = roundHalfUpToOneDecimalPlace(2, results.getCorrectAnswerCount() * config.getPropertyAsDouble(ResultEngineConfig.HANDICAP_BONUS_PER_CORRECT_ANSWER));
        double newHandicap = roundHalfUpToOneDecimalPlace(1, oldHandicap + handicapValue + bonusForCorrectAnswers);

        // Ensure handicap is in range 0..100
        if (newHandicap < 0) {
            newHandicap = 0;
        } else if (newHandicap > 100) {
            newHandicap = 100;
        }

        // round to one decimal place
        newHandicap = roundHalfUpToOneDecimalPlace(1, newHandicap);

        results.addResultItem(new ResultItem(ResultType.OLD_HANDICAP, roundHalfUpToOneDecimalPlace(1, oldHandicap)));
        results.addResultItem(new ResultItem(ResultType.NEW_HANDICAP, newHandicap));

        if (!trainingMode) {
            results.setUserHandicap(newHandicap);
            gameInstance.setUserHandicap(newHandicap);
            results.addUserInteraction(UserInteractionType.HANDICAP_ADJUSTMENT);
        }
        LOGGER.debug("calculateNewHandicap minusHandicapFactorPercent {} handicapKey {} handicapDelta {} handicapDeltaFactor {} handicapValue {} bonusForCorrectAnswers {} newHandicap {} " +
                        "handicapPoints {} maxGamePoints {} oldHandicap {}         ",
                minusHandicapFactorPercent,
                handicapKey,
                handicapDelta,
                handicapDeltaFactor,
                handicapValue,
                bonusForCorrectAnswers,
                newHandicap,
                handicapPoints,
                maxGamePoints,
                oldHandicap);
    }

    /**
     * Calculate if user is eligible to winning component.
     * @param clientInfo
     */
    private void calculateAccessToWinningComponent(Results results, Game game, ClientInfo clientInfo) {
        boolean userCountryCanGamble = noGamblingCountry.userComesFromNonGamblingCountry(clientInfo);

        final GameWinningComponentListItem[] winningComponents = game.getWinningComponents();

        boolean winningComponentAvailable = ArrayUtils.isNotEmpty(winningComponents) && userCountryCanGamble;

        if (winningComponentAvailable) {
            final int correctAnswerPercent = results.getCorrectAnswerCount() * 100 / results.getTotalQuestionCount();
            final List<GameWinningComponentListItem> eligibleWinningComponents =
                    Arrays.stream(winningComponents)
                            .filter(winningComponent -> correctAnswerPercent >= winningComponent.getRightAnswerPercentage())
                            .collect(Collectors.toList());
            results.setEligibleToWinnings(! eligibleWinningComponents.isEmpty());

            // Choose one free and one paid winning component
            results.setPaidWinningComponentId(chooseOne(eligibleWinningComponents, true));
            results.setFreeWinningComponentId(chooseOne(eligibleWinningComponents, false));

            if (results.getPaidWinningComponentId() != null || results.getFreeWinningComponentId() != null) {
                // If there is a paid winning component available, have to wait for user to answer if he wants to buy the winning component
                results.addUserInteraction(UserInteractionType.BUY_WINNING_COMPONENT);
            }
        } else {
            results.setEligibleToWinnings(false);
        }
    }


    private String chooseOne(List<GameWinningComponentListItem> winningComponents, boolean paid) {
        final List<GameWinningComponentListItem> winningComponentsOfType = winningComponents.stream()
                .filter(winningComponent -> winningComponent.isPaid() == paid).collect(Collectors.toList());
        if (winningComponentsOfType.isEmpty()) {
            return null;
        } else if (winningComponentsOfType.size() == 1) {
            return winningComponentsOfType.get(0).getWinningComponentId();
        }
        return winningComponentsOfType.get(randomUtil.nextInt(winningComponentsOfType.size())).getWinningComponentId();
    }

    /**
     * Calculate if user has won a special prize. Will also update the game statistics.
     */
    private void calculateSpecialPrizeWon(Results results, String gameId, ResultConfiguration resultConfig) {
        if (resultConfig != null && resultConfig.isSpecialPrizeEnabled()
                && results.getCorrectAnswerCount() * 100 / results.getTotalQuestionCount() >= resultConfig.getSpecialPrizeCorrectAnswersPercent()) {
            GameStatistics stats = gameStatisticsDao.updateGameStatistics(gameId, 1, 1, 0);
            SpecialPrizeWinningRule winningRule = resultConfig.getSpecialPrizeWinningRule();
            String voucherId = resultConfig.getSpecialPrizeVoucherId();
            if (voucherId == null) {
                LOGGER.warn("Special prize turned on, but no voucher specified");
            }
            if (winningRule == null) {
                LOGGER.warn("Special prize turned on, but no recognized rule defined: {}", resultConfig
                        .getPropertyAsString(ResultConfiguration.PROPERTY_SPECIAL_PRIZE_WINNING_RULE));
            }
            if (voucherId != null && winningRule != null) {
                calculateSpecialPrizeWon(results, gameId, voucherId, winningRule,
                        resultConfig.getSpecialPrizeWinningRuleAmount(), stats);
            }
        } else {
            gameStatisticsDao.updateGameStatistics(gameId, 1, 0, 0);
        }
    }

    private void calculateSpecialPrizeWon(Results results, String gameId, String voucherId,
                                          SpecialPrizeWinningRule winningRule, Integer winningRuleAmount, GameStatistics stats) {
        if (winningRule == SpecialPrizeWinningRule.EVERY_PLAYER) {
            addSpecialPrize(results, gameId, voucherId);
        } else {
            if (winningRuleAmount == null || winningRuleAmount < 1) {
                LOGGER.warn("Invalid special prize rule configuration. For rule {} winning rule amount \"{}\" is invalid.", winningRule.name(), winningRuleAmount);
            } else {
                switch (winningRule) {
                    case EVERY_X_PLAYER:
                        if (stats.getPlayedCount() % winningRuleAmount == 0) {
                            addSpecialPrize(results, gameId, voucherId);
                        }
                        break;
                    case FIRST_X_PLAYERS:
                        if (stats.getSpecialPrizeAvailableCount() <= winningRuleAmount) {
                            addSpecialPrize(results, gameId, voucherId);
                        }
                        break;
                    default:
                        LOGGER.warn("Unrecognized winning rule: {}", winningRule.name());
                        break;
                }
            }
        }
    }

    private void addSpecialPrize(Results results, String gameId, String voucherId) {
        results.setSpecialPrizeVoucherId(voucherId);
        results.addUserInteraction(UserInteractionType.SPECIAL_PRIZE);
        gameStatisticsDao.updateGameStatistics(gameId, 0, 0, 1);
    }

    /**
     * Round value down to one decimal place.
     */
    private double roundDownToOneDecimalPlace(int round,double value) {
        return BigDecimal.valueOf(value).setScale(round, RoundingMode.DOWN).doubleValue();
    }
    /**
     * Rounding to an long value.
     */
    private long roundingToAnLongValue(int round, double value) {
        return BigDecimal.valueOf(value).setScale(round, RoundingMode.HALF_UP).longValue();
    }

    private double roundHalfUpToOneDecimalPlace(int round, double value) {
        return BigDecimal.valueOf(value).setScale(round, RoundingMode.HALF_UP).doubleValue();
    }
    /**
     * Round value up to integer.
     */
    private int roundUpToInt(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.UP).intValue();
    }

    @Override
    public void updateAverageAnswerTimeAndWriteWarningsOnTooBigDeviations(Results results, GameInstance gameInstance) {
        results.getCorrectAnswerResults().forEach((index, result) -> {
            String questionId = gameInstance.getQuestion(index).getId();
            double answerMsT = result.getResultItems().get(ResultType.ANSWER_TIME).getAmount();
            Double averageAnswerMsT = questionStatisticsAerospikeDao.updateAverageAnswerTime(questionId, answerMsT);
            if (averageAnswerMsT != null) {
                double threshold = config.getPropertyAsDouble(ResultEngineConfig.THRESHOLD_FOR_WARNING_ON_TOO_QUICK_ANSWER_PERCENT);
                if (answerMsT < averageAnswerMsT * threshold) {
                    eventLogAerospikeDao.createEventLog(new EventLog(EventLogType.WARNING, EventLog.CATEGORY_POTENTIAL_FRAUD_WARNING,
                            "Question answered too quickly", gameInstance.getUserId(), gameInstance.getId(), index));
                }
            }
        });
    }

    @Override
    public CompletedGameListResponse getCompletedGameListResponse(String userId, String tenantId,
                                                                  List<GameType> gameTypes, long offset, int limit, ZonedDateTime dateFrom, ZonedDateTime dateTo,
                                                                  String isMultiplayerGameFinished,
                                                                  String gameOutcome) {
        CompletedGameHistoryList completedGameHistoryList = new CompletedGameHistoryList();
        for (GameType gameType : gameTypes) {
            CompletedGameHistoryList gameListByType = completedGameHistoryAerospikeDao.getCompletedGamesList(userId,
                    tenantId, gameType, offset, limit, dateFrom, dateTo,
                    isMultiplayerGameFinished,
                    gameOutcome);
            completedGameHistoryList.addCompletedGameList(gameListByType);
        }

        return new CompletedGameListResponse(limit, offset, completedGameHistoryList.getTotal(),
                completedGameHistoryList.getCompletedGameList());
    }

    @Override
    public MultiplayerResults getMultiplayerResults(String multiplayerGameInstanceId) throws F4MEntryNotFoundException {
        return resultEngineAerospikeDao.getMultiplayerResults(multiplayerGameInstanceId);
    }

    @Override
    public boolean hasMultiplayerResults(String multiplayerGameInstanceId) {
        return resultEngineAerospikeDao.hasMultiplayerResults(multiplayerGameInstanceId);
    }

    @Override
    public void storeMultiplayerResults(MultiplayerResults results, boolean overwriteExisting) {
        resultEngineAerospikeDao.saveMultiplayerResults(results, overwriteExisting);
    }

    @Override
    public UserResultsByHandicapRange calculateMultiplayerGameOutcome(String multiplayerGameInstanceId) {
        // Get the game instances
        List<MultiplayerUserGameInstance> userGameInstances =
                multiplayerGameInstanceDao.getGameInstances(multiplayerGameInstanceId, REGISTERED, STARTED, CALCULATED);

        String firstGameInstanceId = CollectionUtils.isEmpty(userGameInstances)
                ? null : userGameInstances.get(0).getGameInstanceId();
        GameInstance firstGameInstance = firstGameInstanceId == null
                ? null : gameInstanceDao.getGameInstance(firstGameInstanceId);

        Game game = firstGameInstance == null ? null : firstGameInstance.getGame();
        if (game == null) {
            throw new F4MEntryNotFoundException("No game instance found with ID " + firstGameInstanceId);
        }

        if (! game.isDuel() && ! (game.isTournament() && ! game.isPlayoff())) {
            throw new UnsupportedOperationException("Only duel and tournament game types implemented");
        }

        if (game.isDuel()) {
            // In duels exactly 2 players are competing.
            Validate.isTrue(! userGameInstances.isEmpty() && userGameInstances.size() <= 2,
                    "There has to be 1 or 2 participants in a duel / quiz battle");
        }

        // Calculate placements
        UserResultsByHandicapRange resultsByHandicapRange = calculatePlacements(multiplayerGameInstanceId, game,
                firstGameInstance.getTenantId(), firstGameInstance.getAppId(),
                firstGameInstance.getEntryFeeCurrency(), userGameInstances);

        // Calculate game outcome
        calculateGameOutcome(resultsByHandicapRange);

        // Calculate duel additional information
        calculateDuelAdditionalInformation(resultsByHandicapRange);

        // Calculate jackpot payout
        calculateJackpotPayout(resultsByHandicapRange);

        return resultsByHandicapRange;
    }

    /**
     * Calculate the placements, initialize the game results by handicap range.
     */
    private UserResultsByHandicapRange calculatePlacements(String multiplayerGameInstanceId, Game game, String tenantId,
                                                           String appId, Currency entryFeeCurrency,
                                                           List<MultiplayerUserGameInstance> userGameInstances) {
        // Load the results
        List<UserResults> multiplayerResults =
                loadResults(multiplayerGameInstanceId, userGameInstances, game, entryFeeCurrency);
        BigDecimal totalEntryFeeAmount = multiplayerResults.stream().map(UserResults::getEntryFeePaid)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);


        // Depending on game type, calculate placements
        UserResultsByHandicapRange resultsByHandicapRange = new UserResultsByHandicapRange(game, tenantId, appId, multiplayerGameInstanceId,
                totalEntryFeeAmount, entryFeeCurrency);
        if (game.isDuel()) {
            // For duels there is no possibility to split results in multiple handicap groups (there are only 2 participants)
            resultsByHandicapRange.addDefaultHandicapRangeResults(calculatePlacement(multiplayerResults));
        } else if (game.isTournament() && ! game.isPlayoff()) {
            // Fill results based on handicap groups
            List<HandicapRange> tournamentHandicapStructure = getHandicapRanges(game);
            if (tournamentHandicapStructure.size() > 1) {
                // There are multiple handicap ranges
                for (HandicapRange range : tournamentHandicapStructure) {
                    List<UserResults> handicapRangeResults = multiplayerResults.stream()
                            .filter(mpr -> isInHandicapRange(mpr.getUserHandicap(), range)).collect(Collectors.toList());
                    resultsByHandicapRange.addHandicapRangeResults(range, calculatePlacement(handicapRangeResults));
                }
            } else {
                // Single handicap range
                assert tournamentHandicapStructure.size() == 1;
                resultsByHandicapRange.addHandicapRangeResults(tournamentHandicapStructure.get(0),
                        calculatePlacement(multiplayerResults));
            }
        } else {
            // Placements can be calculated only for duels and tournaments
            throw new IllegalStateException("Placements can be calculated only for duels and tournaments");
        }
        return resultsByHandicapRange;
    }

    private List<HandicapRange> getHandicapRanges(Game game) {
        List<HandicapRange> tournamentHandicapStructure = game.getResultConfiguration() == null
                ? null : game.getResultConfiguration().getTournamentHandicapStructure();
        if (tournamentHandicapStructure == null || tournamentHandicapStructure.isEmpty()) {
            tournamentHandicapStructure = Arrays.asList(HandicapRange.DEFAULT_HANDICAP_RANGE);
        }
        return tournamentHandicapStructure;
    }

    private boolean isInHandicapRange(double userHandicap, HandicapRange range) {
        return userHandicap >= range.getHandicapFrom()
                && userHandicap <= range.getHandicapTo();
    }

    protected void calculateHandicapRange(Results results, Game game) {
        //handicap range will be recalculated later in calculatePlacements -
        // probably unnecessary, but the way of calculation differs a bit and placements are calculated there too
        HandicapRange range = null;
        List<HandicapRange> handicapRanges = getHandicapRanges(game);
        if (!handicapRanges.isEmpty()) {
            range = handicapRanges.stream().filter(r -> isInHandicapRange(results.getUserHandicap(), r))
                    .findFirst().orElse(handicapRanges.get(0));
        }
        if (range != null) {
            results.setHandicapRangeId(range.getHandicapRangeId());
            results.setHandicapFrom(range.getHandicapFrom());
            results.setHandicapTo(range.getHandicapTo());
        }
    }

    /**
     * Load the individual game results from Aerospike.
     */
    private List<UserResults> loadResults(String multiplayerGameInstanceId, List<MultiplayerUserGameInstance> userGameInstances,
                                          Game game, Currency entryFeeCurrency) {
        List<UserResults> multiplayerResults = new ArrayList<>(userGameInstances.size());
        userGameInstances.forEach(userGameInstance -> {
            Results results;
            try {
                results = StringUtils.isBlank(userGameInstance.getGameInstanceId()) ? null : resultEngineAerospikeDao.getResults(userGameInstance.getGameInstanceId());
            } catch (F4MEntryNotFoundException e) {
                // If no results => assume game was not finished
                results = null;
            }
            if (results == null) {
                results = new Results(userGameInstance.getGameInstanceId(), game.getGameId(), userGameInstance.getUserId(),
                        userGameInstance.getTenantId(), userGameInstance.getAppId(), userGameInstance.getClientIp(),
                        multiplayerGameInstanceId, userGameInstance.getUserHandicap(), false);
                GameInstance gameInstance = gameInstanceDao.getGameInstance(userGameInstance.getGameInstanceId());
                BigDecimal entryFeeAmount = gameInstance.getEntryFeeAmount();
                if (ObjectUtils.allNotNull(entryFeeAmount, entryFeeCurrency)) {
                    results.setEntryFeePaid(entryFeeAmount, entryFeeCurrency);
                }
            }
            multiplayerResults.add(new UserResults(results));
        });
        return multiplayerResults;
    }

    /**
     * Calculate placements among results. Will set the place in results and order the list.
     */
    private List<UserResults> calculatePlacement(List<UserResults> results) {
        results.sort(USER_RESULTS_COMPARATOR);

        int place = 0;
        UserResults previousResult = null;
        for (UserResults result : results) {
            if (previousResult == null || USER_RESULTS_COMPARATOR.compare(previousResult, result) != 0) {
                place++;
                previousResult = result;
            }
            result.setPlace(place);
        }

        return results;
    }

    @Override
    public void updateIndividualResults(UserResultsByHandicapRange results) {
        ClientInfo clientInfo = new ClientInfo();
        Game game = results.getGame();
        clientInfo.setTenantId(results.getTenantId());
        clientInfo.setAppId(results.getAppId());
        saveGameEndEvent(game, clientInfo);
        results.forEachRange(range -> {
            results.forEach(range, result -> {
                if (result.getGameInstanceId() != null) {
                    if (result.isGameFinished()) {
                        Results updatedResults = updateResults(result.getGameInstanceId(), new UpdateResultsAction(jsonMessageUtil) {
                            @Override
                            public Results updateResults(Results results) {
                                results.addResultItem(new ResultItem(ResultType.PLACE, result.getPlace()));
                                if(result.getJackpotWinning() != null) {
                                    results.setBonusPointsWon(result.getJackpotWinning().intValue());
                                }
                                results.setJackpotWinning(result.getJackpotWinning(), result.getJackpotWinningCurrency());
                                results.setRefundReason(results.getRefundReason());
                                results.setDuelOpponentGameInstanceId(result.getDuelOpponentGameInstanceId());
                                results.setGameOutcome(result.getGameOutcome());
                                results.setHandicapRangeId(range.getHandicapRangeId());
                                results.setHandicapFrom(range.getHandicapFrom());
                                results.setHandicapTo(range.getHandicapTo());
                                if (multiplayerGameInstanceDao.getGameInstancesCount(results.getMultiplayerGameInstanceId()) == 1) {
                                    results.setGameOutcome(GameOutcome.NO_OPPONENT);
                                }
                                    return results;

                            }
                        });
                        // Update completed game history item for user
                        GameInstance gameInstance = gameInstanceDao.getGameInstance(result.getGameInstanceId());
                        if (gameInstance != null) {
                            saveGameHistoryForUser(gameInstance, updatedResults, GameHistoryUpdateKind.MULTIPLAYER_GAME_FINISH);
                            saveMultiplayerGameEndEvent(gameInstance, updatedResults);
                        }
                    } else {
                        // For unfinished game, even if there are already results, we want to overwrite them, since we
                        // don't expect to care about them anyway
                        String mgiId = results.getMultiplayerGameInstanceId();
                        Results unfinishedResults = new Results(result.getGameInstanceId(), game.getGameId(),
                                result.getUserId(), result.getTenantId(), result.getAppId(), result.getClientIp(),
                                mgiId, result.getUserHandicap(), false);
                        unfinishedResults.setGameOutcome(result.getGameOutcome());
                        CustomGameConfig customGameConfig = mgiId != null
                                ? multiplayerGameInstanceDao.getConfig(mgiId) : null;
                        if (customGameConfig != null) {
                            unfinishedResults.setExpiryDateTime(customGameConfig.getExpiryDateTime());
                        }
                        storeResults(result.getGameInstanceId(), unfinishedResults, true);
                    }
                }
            });
        });
    }

    @Override
    public void sendGameEndNotificationsToInvolvedUsers(UserResultsByHandicapRange results) {
        LOGGER.debug("SendGameEndNotificationsToInvolvedUsers count {} ", multiplayerGameInstanceDao.getGameInstancesCount(results.getMultiplayerGameInstanceId()));
        if (multiplayerGameInstanceDao.getGameInstancesCount(results.getMultiplayerGameInstanceId()) > 1) {

            final GameType gameType = results.getGame().getType();
            results.forEach(result -> {
                final GameEndNotification gameEndNotification = new GameEndNotification(gameType, results.getMultiplayerGameInstanceId(), result.getGameInstanceId());

                ClientInfo clientInfo = new ClientInfo();
                clientInfo.setTenantId(result.getTenantId());
                clientInfo.setAppId(result.getAppId());
                clientInfo.setUserId(result.getUserId());
                clientInfo.setIp(result.getClientIp());

                serviceCommunicator.pushMessageToUser(result.getUserId(), gameEndNotification, clientInfo);
            });
        }
    }

    /**
     * Calculate the game outcome.
     */
    private void calculateGameOutcome(UserResultsByHandicapRange resultsByHandicapRange) {
        Game game = resultsByHandicapRange.getGame();
        LOGGER.debug("F4M resultsByHandicapRange" + game);
        if (game.isDuel()) {
            calculateDuelOutcome(resultsByHandicapRange);
        } else if (game.isTournament()) {
            calculateTournamentOutcome(resultsByHandicapRange);
        } else {
            // No particular outcome for other game types
            resultsByHandicapRange.forEach(userResults -> userResults.setGameOutcome(GameOutcome.UNSPECIFIED));
        }
    }

    private void calculateDuelOutcome(UserResultsByHandicapRange resultsByHandicapRange) {
        // Game outcome for duels
        List<UserResults> placements = resultsByHandicapRange.getDefaultHandicapRangePlacements();
        assert(resultsByHandicapRange.getHandicapRangeCount() == 1 && ! CollectionUtils.isEmpty(placements)
                && placements.size() <= 2); // one or two participants in duel

        if (placements.size() == 1) {
            // No other player
            placements.get(0).setGameOutcome(GameOutcome.NO_OPPONENT);
        } else {
            UserResults first = placements.get(0);
            UserResults second = placements.get(1);
            if (! first.isGameFinished() || ! second.isGameFinished()) {
                // Any of players did not finish
                first.setGameOutcome(GameOutcome.GAME_NOT_FINISHED);
                second.setGameOutcome(GameOutcome.GAME_NOT_FINISHED);
            } else if (placements.get(0).getPlace() == placements.get(1).getPlace()) {
                // This is tie
                first.setGameOutcome(GameOutcome.TIE);
                second.setGameOutcome(GameOutcome.TIE);
            } else {
                // There has to be just one winner and one loser
                assert(first.getPlace() == 1);
                assert(second.getPlace() == 2);
                first.setGameOutcome(GameOutcome.WINNER);
                second.setGameOutcome(GameOutcome.LOSER);
            }
        }
    }

    private void calculateTournamentOutcome(UserResultsByHandicapRange resultsByHandicapRange) {
        resultsByHandicapRange.forEach(userResults -> {
            if (userResults.isGameFinished()) {
                if (userResults.getPlace()==1) {
                    userResults.setGameOutcome(GameOutcome.WINNER);
                } else {
                    userResults.setGameOutcome(GameOutcome.LOSER);
                }
            } else {
                userResults.setGameOutcome(GameOutcome.GAME_NOT_FINISHED);
            }
        });
    }

    /**
     * Calculate additional information needed for results.
     */
    private void calculateDuelAdditionalInformation(UserResultsByHandicapRange resultsByHandicapRange) {
        if (resultsByHandicapRange.getGame().isDuel()) {
            List<UserResults> results = resultsByHandicapRange.getDefaultHandicapRangePlacements();
            if (results.size() == 2) {
                results.get(0).setDuelOpponentGameInstanceId(results.get(1).getGameInstanceId());
                results.get(1).setDuelOpponentGameInstanceId(results.get(0).getGameInstanceId());
            }
        }
    }

    /**
     * Calculate jackpot payout.
     */
    private void calculateJackpotPayout(UserResultsByHandicapRange resultsByHandicapRange) {
        Game game = resultsByHandicapRange.getGame();
        ResultConfiguration resultConfig = game.getResultConfiguration();
        LOGGER.debug("calculateJackpotPayout game {} resultConfig {}  multiplayer {} ", game, resultConfig, resultsByHandicapRange.getMultiplayerGameInstanceId());
        CustomGameConfig customGameConfig = multiplayerGameInstanceDao.getConfig(resultsByHandicapRange.getMultiplayerGameInstanceId());

        if (game.isDuel() && ! GameUtil.isFreeGame(game, customGameConfig)) {
            if (resultsByHandicapRange.getTotalEntryFeeAmount() == null || resultsByHandicapRange.getEntryFeeCurrency() == null) {
                throw new F4MFatalErrorException(String.format(
                        "Game [%s] in muliplayer [%s] marked as paid duel, but does not have entry fee defined",
                        game.getGameId(), resultsByHandicapRange.getMultiplayerGameInstanceId()));
            }
            calculateDuelPayout(resultsByHandicapRange);
        } else if (game.isTournament() && ! game.isPlayoff() && resultConfig.isJackpotGame()) {
            BigDecimal jackpotAmount;
            BigDecimal totalEntryFeeAmount = resultsByHandicapRange.getTotalEntryFeeAmount();
            Currency entryFeeCurrency = resultsByHandicapRange.getEntryFeeCurrency();
            Currency jackpotCurrency;

            if(resultConfig.isJackpotCalculateByEntryFee()){
                //JackpotCalculateByEntryFee
                jackpotCurrency = entryFeeCurrency;

                jackpotAmount = totalEntryFeeAmount;
                if (jackpotCurrency == Currency.MONEY){
                    jackpotAmount = jackpotAmount.multiply(config.getPropertyAsBigDecimal(ResultEngineConfig.JACKPOT_PAYOUT_PERCENT));
                }
                LOGGER.debug("calculateJackpotPayout game  1");

            } else {
                //target jackpot
                jackpotCurrency = Currency.MONEY;
                BigDecimal minimumJackpotAmount = BigDecimal.valueOf(game.getMinimumJackpotGarantie());
                LOGGER.debug("calculateJackpotPayout game  2");

                if(entryFeeCurrency == Currency.MONEY){
                    if (minimumJackpotAmount != null
                            && totalEntryFeeAmount != null
                            && totalEntryFeeAmount.compareTo(minimumJackpotAmount) <= 0) {
                        jackpotAmount = minimumJackpotAmount;
                    } else if (minimumJackpotAmount != null
                            && totalEntryFeeAmount != null
                            && totalEntryFeeAmount.compareTo(minimumJackpotAmount) > 0) {
                        jackpotAmount = totalEntryFeeAmount
                                .multiply(config.getPropertyAsBigDecimal(ResultEngineConfig.JACKPOT_PAYOUT_PERCENT));
                    } else {
                        jackpotAmount = null;
                    }
                } else {
                    jackpotAmount = minimumJackpotAmount;
                }
            }

            if (jackpotAmount == null || jackpotCurrency == null) {
                throw new F4MFatalErrorException("Game " + game.getGameId() + " marked as jackpot tournament, but no jackpot amount or currency could be determined");
            }

            final Currency calculatedJackpotCurrency = jackpotCurrency;
            final BigDecimal calculatedJackpotAmount;
            int handicapRangeCount = resultsByHandicapRange.getHandicapRangeCount();
            if (handicapRangeCount > 1) {
                calculatedJackpotAmount = jackpotAmount
                        .divide(BigDecimal.valueOf(handicapRangeCount), RoundingMode.DOWN);
            } else {
                calculatedJackpotAmount = jackpotAmount;
            }
            LOGGER.debug("calculateJackpotPayout game  2");
            if (multiplayerGameInstanceDao.getGameInstancesCount(resultsByHandicapRange.getMultiplayerGameInstanceId()) == 1) {
                resultsByHandicapRange.forEachRange(range -> calculateTournamentPayout(resultsByHandicapRange, range, game.getEntryFeeAmount(), game.getEntryFeeCurrency()));
            } else if (!resultConfig.isJackpotCalculateByEntryFee() && game.getEntryFeeCurrency() != Currency.MONEY && game.isTournament()) {
                //Jackpot Configuration Target jackpot amount(only bonus and credit)
                final BigDecimal amount = new BigDecimal(customGameConfig.getMinimumJackpotGarantie());
                resultsByHandicapRange.forEachRange(range -> calculateTournamentPayout(resultsByHandicapRange, range, amount, Currency.MONEY));
            } else {
                resultsByHandicapRange.forEachRange(range -> calculateTournamentPayout(resultsByHandicapRange, range, calculatedJackpotAmount, calculatedJackpotCurrency));
            }
        }
    }

    /**
     * Calculate duel payout.
     */
    private void calculateDuelPayout(UserResultsByHandicapRange userResultsByHandicapRange) {
        List<UserResults> placements = userResultsByHandicapRange.getDefaultHandicapRangePlacements();
        assert(userResultsByHandicapRange.getHandicapRangeCount() == 1 && ! CollectionUtils.isEmpty(placements)
                && placements.size() <= 2); // one or two participants in duel

        if (placements.size() == 1) {
            // No other player => refund
            userResultsByHandicapRange.setRefundReason(RefundReason.NO_OPPONENT);
        } else if (! placements.get(0).isGameFinished() || ! placements.get(1).isGameFinished()) {
            // Any of players did not finish => refund entry fees
            userResultsByHandicapRange.setRefundReason(RefundReason.GAME_NOT_FINISHED);
        } else {
            if (placements.get(0).getPlace() == placements.get(1).getPlace()) {
                // This is a pat => there has to be 2 players in the same place
                userResultsByHandicapRange.setRefundReason(RefundReason.GAME_WAS_A_PAT);
            } else {
                // There has to be just one winner and one loser
                UserResults winner = placements.get(0);
                assert(winner.getPlace() == 1);
                assert(placements.get(1).getPlace() == 2);

                // Calculate and set the winning amount
                Currency entryFeeCurrency = userResultsByHandicapRange.getEntryFeeCurrency();
                BigDecimal amount = userResultsByHandicapRange.getTotalEntryFeeAmount();
                if(entryFeeCurrency == Currency.MONEY)
                    amount = amount.multiply(config.getPropertyAsBigDecimal(ResultEngineConfig.JACKPOT_PAYOUT_PERCENT));
                setWinnings(winner, amount, entryFeeCurrency);
            }
        }
    }

    /**
     * Calculate tournament payout.
     *
     * @param multiplayerResults - the result of the tournament.
     * @param range
     * @param jackpotAmount - the jackpot amount.
     * @param jackpotCurrency - the jackpot currency.
     */
    private void calculateTournamentPayout(UserResultsByHandicapRange multiplayerResults, HandicapRange range, BigDecimal jackpotAmount, Currency jackpotCurrency) {
        List<UserResults> placements = multiplayerResults.getPlacements(range);

        LOGGER.debug("calculateTournamentPayout  placements(1) {} ", placements);

        if (!placements.isEmpty()) {
            LogNormalDistribution logNormDist = new LogNormalDistribution(1.0, 10.0);

            double factorBase = placements.stream()
                    .limit(limitPlace(placements))
                    .mapToDouble(userResults -> logNormDist.density(userResults.getPlace()) * 1000)
                    .sum();

            // the calculation of the payout for each winner.
            LOGGER.debug("calculateTournamentPayout factorBase {} ", factorBase);
            placements.stream()
                    .limit(limitPlace(placements))
                    .forEach(userPlacement ->
                            calculateTournamentPayout(userPlacement, factorBase, jackpotAmount, jackpotCurrency, logNormDist));

            LOGGER.debug("calculateTournamentPayout sumGamePoinWithBonus {} ", factorBase);
        }
        LOGGER.debug("calculateTournamentPayout  placements(2) {} ", placements);
    }

    /**
     30 percent of participants are awarded.
     If 10 people played, two took the first place, two the second place, the others lost, the awarded will be four. that's over 30 percent.
     *
     * @param placements - list of users with results.
     * @return - number of winners.
     */
    private int limitPlace(List<UserResults> placements) {
        int placementsSize = new BigDecimal(placements.size() * 0.3).setScale(0, RoundingMode.UP).intValue();
        long numberOfParticipants;
        int levelPlaces = 2;
        while (true) {
            final int levelFinal = levelPlaces;
            numberOfParticipants = placements.stream().filter(places -> places.getPlace() < levelFinal).count();
            if (numberOfParticipants >= placementsSize) {
                return (int)numberOfParticipants;
            }
            levelPlaces++;
        }
    }

    /**
     *
     * @param result - list of users with results.
     * @param factorBase - the degree of compensation.
     * @param jackpotAmount - the jackpot amount.
     * @param jackpotCurrency - the jackpot currency.
     * @param logNormDist - a function to count the jackpot.
     */
    private void calculateTournamentPayout(UserResults result, double factorBase, BigDecimal jackpotAmount, Currency jackpotCurrency, LogNormalDistribution logNormDist) {
        LOGGER.debug("calculateTournamentPayout 3");
        if (result.isGameFinished()) {
            double jackpot = jackpotAmount.doubleValue() / factorBase * logNormDist.density(result.getPlace()) * 1000;
            if (jackpotCurrency == Currency.MONEY) {
                result.setJackpotWinning(BigDecimal.valueOf(jackpot)
                        .setScale(2, RoundingMode.DOWN), jackpotCurrency);
            } else {
                if (BigDecimal.valueOf(jackpot).setScale(2, RoundingMode.DOWN).compareTo(BigDecimal.valueOf(jackpot).setScale(0, RoundingMode.DOWN)) >= 1) {
                    result.setAdditionalPaymentForWinning(true);
                }
                result.setJackpotWinning(BigDecimal.valueOf(jackpot)
                        .setScale(0, RoundingMode.UP), jackpotCurrency);
            }

        }
    }




    /**
     * Round down the won jackpot amount and set it in the user game results.
     */
    private void setWinnings(UserResults userResults, BigDecimal amount, Currency currency) {
        // Round the winning
        BigDecimal winningAmount = amount.setScale(currency == Currency.MONEY ? 2 : 0, RoundingMode.DOWN);

        // Set the winning in game results
        userResults.setJackpotWinning(winningAmount, currency);
    }

    @Override
    public void storeUserPayout(UserResultsByHandicapRange results) {
        Game game = results.getGame();
        results.forEach(userResult -> {
            if (userResult.getJackpotWinning() != null && userResult.getJackpotWinningCurrency() != null) {
                // Store winning
                userWinningDao.saveUserWinning(userResult.getAppId(), userResult.getUserId(), new UserWinning(
                        userResult.getGameInstanceId(), game, userResult.getJackpotWinning(),
                        userResult.getJackpotWinningCurrency()));
            }
        });
    }

    @Override
    public void initiatePayout(UserResultsByHandicapRange results, boolean isTournament) {
        serviceCommunicator.requestMultiplayerGamePaymentTransfer(results, isTournament);
    }

    @Override
    public void moveResults(String sourceUserId, String targetUserId) {
        // Move completed game history
        Profile sourceProfile = profileDao.getProfile(sourceUserId);
        Profile targetProfile = profileDao.getProfile(targetUserId);
        Set<String> tenantIds = sourceProfile != null ? sourceProfile.getTenants() : Collections.emptySet();
        if (CollectionUtils.isEmpty(tenantIds)) {
            tenantIds = targetProfile != null ? targetProfile.getTenants() : Collections.emptySet();
        }
        if (CollectionUtils.isNotEmpty(tenantIds)) {
            tenantIds.forEach(tenantId ->
                    completedGameHistoryAerospikeDao.moveCompletedGameHistory(sourceUserId, targetUserId, tenantId));
        }  else {
            LOGGER.warn("Tenants not found; sourceUserId [{}]; targetUserId [{}]", sourceUserId, targetUserId);
        }

        // Update user results
        updateUserResults(sourceUserId, targetUserId);
    }

    private void updateUserResults(String sourceUserId, String targetUserId) {
        // Find all user results by game history
        List<String> gameHistory = gameHistoryDao.getUserGameHistory(sourceUserId);
        // It could be that game service already have moved the game history to new user, so
        // to keep on the safe side, update users also in target user game results
        gameHistory.addAll(gameHistoryDao.getUserGameHistory(targetUserId));

        // Update user results
        gameHistory.forEach(h -> {
            GameHistory history = new GameHistory(jsonUtil.fromJson(h, JsonObject.class));
            if (history.getStatus() == GameStatus.COMPLETED && resultEngineAerospikeDao.exists(history.getGameInstanceId())) {
                resultEngineAerospikeDao.updateResults(history.getGameInstanceId(), new UpdateResultsAction(jsonMessageUtil) {
                    @Override
                    public Results updateResults(Results results) {
                        results.setUserId(targetUserId);
                        return results;
                    }
                });
            }
        });
    }

    @Override
    public boolean isMultiplayerInstanceAvailable(String multiplayerGameInstanceId) {
        return multiplayerGameInstanceDao.isMultiplayerInstanceAvailable(multiplayerGameInstanceId);
    }

    @Override
    public ListResult<ApiProfileWithResults> listMultiplayerResults(String mgiId, String userId,
                                                                    Integer handicapRangeId, List<String> buddyIds, int limit, long offset,
                                                                    MultiplayerResultsStatus status) {
        List<String> includedUserIds = null;
        if (buddyIds != null) {
            includedUserIds = new ArrayList<>(buddyIds);
            includedUserIds.add(userId);
        }
        boolean placeCalculated = MultiplayerResultsStatus.FINAL == status;
        ListResult<MultiplayerGameResult> results = multiplayerGameResultDao.listResults(mgiId, handicapRangeId,
                includedUserIds, limit, offset, placeCalculated);
        if (!placeCalculated || handicapRangeId == null || CollectionUtils.isNotEmpty(includedUserIds)) {
            // If searching among all handicap ranges or buddies, need to use position for place, instead of real place
            for (int i = 0 ; i < results.getSize() ; i++) {

                results.getItems().get(i).setPlace((int) results.getOffset() + i + 1);
            }
        }
        Currency currency = results.getItems().get(0).getPrizeWonCurrency();
        results.getItems().forEach(a -> a.setPrizeWonCurrency(currency));
        List<Profile> profiles = results.getSize() == 0 ? Collections.emptyList()
                : profileDao.getProfiles(results.getItems().stream().map(MultiplayerGameResult::getUserId).collect(Collectors.toList()));
        Map<String, Profile> profileMap = profiles.stream().collect(Collectors.toMap(Profile::getUserId, p -> p));
        return new ListResult<>(results.getLimit(), results.getOffset(), results.getTotal(),
                results.getItems().stream().map(r -> new ApiProfileWithResults(profileMap.get(r.getUserId()), r)).collect(Collectors.toList()));
    }

    @Override
    public int getMultiplayerGameRankByResults(String multiplayerGameInstanceId, String userId, int correctAnswerCount,
                                               double gamePointsWithBonus, Integer handicapRangeId, List<String> buddyIds) {
        return multiplayerGameResultDao.getMultiplayerGameRankByResults(multiplayerGameInstanceId, userId, correctAnswerCount,
                gamePointsWithBonus, handicapRangeId, buddyIds);
    }

    @Override
    public void resyncSinglePlayerResults(Results result) {
        storeMultiplayerGameResult(result.getMultiplayerGameInstanceId(), result, null);
    }

    @Override
    public void resyncMultiplayerResults(String mgiId) {
        // Get the game instances
        List<MultiplayerUserGameInstance> userGameInstances =
                multiplayerGameInstanceDao.getGameInstances(mgiId, REGISTERED, STARTED, CALCULATED);

        for (MultiplayerUserGameInstance instance : userGameInstances) {
            Results result = getResults(instance.getGameInstanceId());
            int placement = result.getPlacement();
            storeMultiplayerGameResult(mgiId, result, placement);
        }
    }

    private void storeMultiplayerGameResult(String mgiId, Results result, Integer placement) {
        LOGGER.debug("storeMultiplayerGameResult result.getHandicapRangeId() {} result.getHandicapFrom() {} result.getHandicapTo() {}",
                result.getHandicapRangeId(), result.getHandicapFrom(), result.getHandicapTo());
        multiplayerGameResultDao.createOrUpdate(new MultiplayerGameResult(mgiId, result.getUserId(),
                result.getHandicapRangeId(), result.getHandicapFrom(), result.getHandicapTo(), placement,
                result.getCorrectAnswerCount(), result.getGamePointsWithBonus(),
                result.getJackpotWinning() == null ? BigDecimal.valueOf(0) : result.getJackpotWinning(), result.getJackpotWinningCurrency()));
    }

    @Override
    public void resyncMultiplayerResults(UserResultsByHandicapRange results) {
        results.forEachRange(range ->
                results.forEach(range, result ->
                        multiplayerGameResultDao.createOrUpdate(new MultiplayerGameResult(results.getMultiplayerGameInstanceId(), result.getUserId(),
                                range.getHandicapRangeId(), range.getHandicapFrom(), range.getHandicapTo(), result.getPlace(),
                                result.getCorrectAnswerCount(), result.getGamePointsWithBonus(),
                                result.getJackpotWinning() == null ? BigDecimal.valueOf(0) : result.getJackpotWinning(),
                                result.getJackpotWinningCurrency()))
                )
        );

    }

}
