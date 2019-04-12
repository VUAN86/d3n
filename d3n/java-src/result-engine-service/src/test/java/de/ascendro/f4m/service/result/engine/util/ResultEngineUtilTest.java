package de.ascendro.f4m.service.result.engine.util;

import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.CALCULATED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.REGISTERED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.STARTED;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_CLIENT_INFO;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.country.nogambling.NoGamblingCountry;
import de.ascendro.f4m.server.event.log.EventLogAerospikeDao;
import de.ascendro.f4m.server.game.GameResponseSanitizer;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.result.AnswerResults;
import de.ascendro.f4m.server.result.GameOutcome;
import de.ascendro.f4m.server.result.MultiplayerResults;
import de.ascendro.f4m.server.result.QuestionInfo;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.server.result.ResultItem;
import de.ascendro.f4m.server.result.ResultType;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.game.engine.model.Answer;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.selection.model.game.CorrectAnswerPointCalculationType;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.game.selection.model.game.HandicapRange;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.SpecialPrizeWinningRule;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.EventLog;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.result.engine.client.ServiceCommunicator;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.dao.CompletedGameHistoryAerospikeDao;
import de.ascendro.f4m.service.result.engine.dao.GameStatisticsDao;
import de.ascendro.f4m.service.result.engine.dao.MultiplayerGameResultElasticDao;
import de.ascendro.f4m.service.result.engine.dao.QuestionStatisticsAerospikeDao;
import de.ascendro.f4m.service.result.engine.dao.ResultEngineAerospikeDao;
import de.ascendro.f4m.service.result.engine.dao.ResultEngineAerospikeDao.UpdateResultsAction;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;
import de.ascendro.f4m.service.result.engine.model.GameStatistics;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.TestJsonMessageUtil;
import de.ascendro.f4m.service.util.random.RandomUtilImpl;

public class ResultEngineUtilTest {

	private static final String VOUCHER_ID = "1";
	private static final String MPGI_ID = "multiplayerGameInstanceId";

	private static final ClientInfo CLIENT_INFO_1 = new ClientInfo("tenantId", "appId", "tu1", "ip", 5.0);
	private static final ClientInfo CLIENT_INFO_5 = new ClientInfo("tenantId", "appId", "tu5", "ip", 5.0);
	
	@Mock
	private ServiceCommunicator serviceCommunicator;

	@Mock
	ResultEngineAerospikeDao resultEngineAerospikeDao;

	@Mock
	CommonGameHistoryDao gameHistoryDao;
	
	@Mock
	CompletedGameHistoryAerospikeDao completedGameHistoryAerospikeDao;
	
	@Mock
	QuestionStatisticsAerospikeDao questionStatisticsAerospikeDao;

	@Mock
	EventLogAerospikeDao eventLogAerospikeDao;
	
	@Mock
	GameStatisticsDao gameStatisticsDao;

	@Mock
	CommonUserWinningAerospikeDao userWinningDao;
	
	@Mock
	CommonMultiplayerGameInstanceDao multiplayerGameInstanceDao;
	
	@Mock
	CommonGameInstanceAerospikeDao gameInstanceDao;

	@Mock
	CommonProfileAerospikeDaoImpl profileDao;
	
	@Mock
	MultiplayerGameResultElasticDao multiplayerGameResultDao;
	
	@Mock
	Tracker traker;
	
	@Mock
	NoGamblingCountry noGamblingCountry;
	@Spy
	RandomUtilImpl randomUtilImpl;
	
	GameStatistics stats = new GameStatistics();

	@Spy
	ResultEngineConfig config = new ResultEngineConfig();
	@Spy
	JsonMessageUtil jsonMessageUtil = new TestJsonMessageUtil();
	@Spy
	JsonUtil jsonUtil = new JsonUtil();
	@Spy
	GameResponseSanitizer gameResponseSanitizer = new GameResponseSanitizer();
	@InjectMocks
	ResultEngineUtilImpl utils;

	JsonLoader jsonLoader;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		jsonLoader = new JsonLoader(this);

		when(gameStatisticsDao.updateGameStatistics(eq("1"), any(Long.class), any(Long.class), any(Long.class)))
			.thenAnswer(new org.mockito.stubbing.Answer<GameStatistics>() {
				@Override
				public GameStatistics answer(InvocationOnMock invocation) throws Throwable {
					stats.setPlayedCount(stats.getPlayedCount() + (long) invocation.getArgument(1));
					stats.setSpecialPrizeAvailableCount(stats.getSpecialPrizeAvailableCount() + (long) invocation.getArgument(2));
					stats.setSpecialPrizeWonCount(stats.getSpecialPrizeWonCount() + (long) invocation.getArgument(3));
					return stats;
				}
			});
		
		when(noGamblingCountry.userComesFromNonGamblingCountry(CLIENT_INFO_1)).thenReturn(true);
	}

	@Test
	/**
	 * Covers testing of following correct answer calculation scenarios:
	 * 1) single-step question answered in time and correctly
	 * 2) single-step question answers wrong
	 * 3) multi-step question answered in time and correctly
	 * 4) multi-step question answered correctly, but timed out
	 */
	public void testCalculateCorrectAnswers() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		Results results = new Results();
		
		// Test
		utils.calculateCorrectlyAnsweredQuestionsAndAnswerTime(results, instance, Collections.emptySet(), true);
		
		// Verify
		assertEquals(2, results.getCorrectAnswerCount());
		assertEquals(2.0, results.getResultItems().get(ResultType.CORRECT_ANSWERS).getAmount(), 0);
		assertEquals(5.0, results.getResultItems().get(ResultType.TOTAL_QUESTIONS).getAmount(), 0);
		assertEquals(134800.0, results.getResultItems().get(ResultType.ANSWER_TIME).getAmount(), 0);

		// First question answer time
		Map<ResultType, ResultItem> questionResults = results.getCorrectAnswerResults().get(0).getResultItems();
		assertEquals(4000.0, questionResults.get(ResultType.ANSWER_TIME).getAmount(), 0);
		
		// First question answer time
		questionResults = results.getCorrectAnswerResults().get(1).getResultItems();
		assertEquals(62300.0, questionResults.get(ResultType.ANSWER_TIME).getAmount(), 0);
	}

	@Test
	public void testGameAndBonusPointCalculationWithPointCalculator_CorrectAnswersBasedOnComplexity_Paid() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		Game game = instance.getGame();
		game.setHideCategories(Boolean.TRUE);
		game.setWinningComponents(new GameWinningComponentListItem[] { new GameWinningComponentListItem("someWC", true, BigDecimal.TEN, Currency.MONEY, 25) });
		instance.setGame(game);
		when(multiplayerGameInstanceDao.getConfig(instance.getMgiId())).thenReturn(prepareCustomConfigWithPoolInfo());

		// Test
		final Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		results.setGameInstanceId("gi1");

		// needed to invoke further calculation of bonus points
		when(resultEngineAerospikeDao.updateResults(anyString(), any()))
			.thenAnswer(new org.mockito.stubbing.Answer<Results>() {
				@Override
				public Results answer(InvocationOnMock invocation) throws Throwable {
					UpdateResultsAction action = invocation.getArgument(1);
					return action.updateResults(results);
				}
			});
		utils.storeUserWinningComponent(CLIENT_INFO_1, results, "userWCId", true);

		// Verify
		assertEquals(2, results.getCorrectAnswerCount());
		
		// First question results
		Map<ResultType, ResultItem> questionResults = results.getCorrectAnswerResults().get(0).getResultItems();
		assertEquals(5.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // under 5 seconds = 5pts
		assertEquals(4000.0, questionResults.get(ResultType.ANSWER_TIME).getAmount(), 0);
		assertEquals(2.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 2 = 2pts
		assertEquals(5.0 + 2.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		assertEquals(5.0 + 2.0, questionResults.get(ResultType.MAXIMUM_GAME_POINTS).getAmount(), 0);
		
		// Second question results
		questionResults = results.getCorrectAnswerResults().get(1).getResultItems();
		assertEquals(0.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // more than 5 seconds = 0pts
		assertEquals(62300.0, questionResults.get(ResultType.ANSWER_TIME).getAmount(), 0);
		assertEquals(4.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 4 = 4pts
		assertEquals(0.0 + 4.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		assertEquals(5.0 + 4.0, questionResults.get(ResultType.MAXIMUM_GAME_POINTS).getAmount(), 0);
		
		// Game results
		assertEquals(11.0, results.getResultItems().get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		assertEquals(11.5, results.getResultItems().get(ResultType.TOTAL_GAME_POINTS_WITH_BONUS).getAmount(), 0);
		assertEquals(39.0, results.getResultItems().get(ResultType.MAXIMUM_GAME_POINTS).getAmount(), 0);
		assertEquals(35.0, results.getResultItems().get(ResultType.BONUS_POINTS).getAmount(), 0); // 11.5 * 3, rounded up
		assertEquals(16.1, results.getResultItems().get(ResultType.HANDICAP_POINTS).getAmount(), 0);
		assertEquals(5.6, results.getUserHandicap(), 0);
		assertEquals(5.3, results.getResultItems().get(ResultType.NEW_HANDICAP).getAmount(), 0);

		ArgumentCaptor<CompletedGameHistoryInfo> completedGameHistoryInfo = ArgumentCaptor.forClass(CompletedGameHistoryInfo.class);
		verify(completedGameHistoryAerospikeDao).saveResultsForHistory(anyString(), anyString(), any(), any(),
				completedGameHistoryInfo.capture(), any());
		assertNull(completedGameHistoryInfo.getValue().getPoolIds());
		assertNull(completedGameHistoryInfo.getValue().getGame().getAssignedPools());
		assertNull(completedGameHistoryInfo.getValue().getGame().getAssignedPoolsColors());
		assertNull(completedGameHistoryInfo.getValue().getGame().getAssignedPoolsIcons());
		assertNull(completedGameHistoryInfo.getValue().getGame().getAssignedPoolsNames());
	}
	
	@Test
	public void testGameAndBonusPointCalculationWithPointCalculator_CorrectAnswersBasedOnFixedValue_Paid() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		
		Game game = instance.getGame();
		ResultConfiguration resultConfiguration = game.getResultConfiguration();
		resultConfiguration.setProperty(
				ResultConfiguration.PROPERTY_CORRECT_ANSWER_POINT_CALCULATION_TYPE, 
				CorrectAnswerPointCalculationType.BASED_ON_FIX_VALUE.getValue());
		game.setResultConfiguration(resultConfiguration);
		game.setWinningComponents(new GameWinningComponentListItem[] { new GameWinningComponentListItem("someWC", true, BigDecimal.TEN, Currency.MONEY, 25) });
		instance.setGame(game);
		when(multiplayerGameInstanceDao.getConfig(instance.getMgiId())).thenReturn(prepareCustomConfigWithPoolInfo());
		
		// Test
		Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		results.setGameInstanceId("gi1");

		// needed to invoke further calculation of bonus points
		when(resultEngineAerospikeDao.updateResults(anyString(), any()))
		.thenAnswer(new org.mockito.stubbing.Answer<Results>() {
			@Override
			public Results answer(InvocationOnMock invocation) throws Throwable {
				UpdateResultsAction action = invocation.getArgument(1);
				return action.updateResults(results);
			}
		});
		utils.storeUserWinningComponent(CLIENT_INFO_1, results, "userWCId", true);
		
		// Verify
		assertEquals(2, results.getCorrectAnswerCount());
		
		// First question results
		Map<ResultType, ResultItem> questionResults = results.getCorrectAnswerResults().get(0).getResultItems();
		assertEquals(5.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // under 5 seconds = 3pts 
		assertEquals(7.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 2 = 7pts
		assertEquals(5.0 + 7.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0); // speed + complexity
		
		// Second question results
		questionResults = results.getCorrectAnswerResults().get(1).getResultItems();
		assertEquals(0.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // more than 5 seconds = 0pts
		assertEquals(13.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 4 = 13pts
		assertEquals(0.0 + 13.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		
		// Game results
		assertEquals(25.0, results.getResultItems().get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		assertEquals(25.5, results.getResultItems().get(ResultType.TOTAL_GAME_POINTS_WITH_BONUS).getAmount(), 0);
		assertEquals(74.0, results.getResultItems().get(ResultType.MAXIMUM_GAME_POINTS).getAmount(), 0);
		assertEquals(77.0, results.getResultItems().get(ResultType.BONUS_POINTS).getAmount(), 0); // 3 * 25.5, round up
		assertEquals(36.8, results.getResultItems().get(ResultType.HANDICAP_POINTS).getAmount(), 0);
		assertEquals(5.6, results.getUserHandicap(), 0);
		assertEquals(5.5, results.getResultItems().get(ResultType.NEW_HANDICAP).getAmount(), 0);
		
		ArgumentCaptor<CompletedGameHistoryInfo> completedGameHistoryInfo = ArgumentCaptor.forClass(CompletedGameHistoryInfo.class);
		verify(completedGameHistoryAerospikeDao).saveResultsForHistory(anyString(), anyString(), any(), any(),
				completedGameHistoryInfo.capture(), any());
		assertArrayEquals(getTestPoolIds(), completedGameHistoryInfo.getValue().getPoolIds());
		assertArrayEquals(new String[] { "football", "tenis" },
				completedGameHistoryInfo.getValue().getGame().getAssignedPools());
		assertArrayEquals(new String[] { "red", "green" },
				completedGameHistoryInfo.getValue().getGame().getAssignedPoolsColors());
		assertArrayEquals(new String[] { "some", "icons" },
				completedGameHistoryInfo.getValue().getGame().getAssignedPoolsIcons());
		assertArrayEquals(new String[] { "some", "names" },
				completedGameHistoryInfo.getValue().getGame().getAssignedPoolsNames());
	}

	private CustomGameConfig prepareCustomConfigWithPoolInfo() {
		CustomGameConfig customGameConfig = new CustomGameConfig();
		String[] poolIds = getTestPoolIds();
		customGameConfig.setPoolIds(poolIds);
		return customGameConfig;
	}

	private String[] getTestPoolIds() {
		String[] poolIds = new String[] { "poolIdValue" };
		return poolIds;
	}
	
	@Test
	public void testGameAndBonusPointCalculationWithPointCalculator_CorrectAnswersBasedOnFixedValue_TreatPaidAsUnpaid() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		Game game = instance.getGame();
		ResultConfiguration resultConfiguration = game.getResultConfiguration();
		resultConfiguration.setProperty(
				ResultConfiguration.PROPERTY_CORRECT_ANSWER_POINT_CALCULATION_TYPE, 
				CorrectAnswerPointCalculationType.BASED_ON_FIX_VALUE.getValue());
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_TREAT_PAID_LIKE_UNPAID, true);
		game.setResultConfiguration(resultConfiguration);
		instance.setGame(game);
		
		// Test
		Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		
		// Verify
		assertEquals(2, results.getCorrectAnswerCount());
		
		// First question results
		Map<ResultType, ResultItem> questionResults = results.getCorrectAnswerResults().get(0).getResultItems();
		assertEquals(5.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // under 5 seconds = 5pts 
		assertEquals(7.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 2 = 7pts
		assertEquals(5.0 + 7.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		
		// Second question results
		questionResults = results.getCorrectAnswerResults().get(1).getResultItems();
		assertEquals(0.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // more than 5 seconds = 0pts
		assertEquals(13.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 4 = 13pts
		assertEquals(0.0 + 13.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		
		// Game results
		assertEquals(25.0, results.getResultItems().get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		assertEquals(110.0, results.getResultItems().get(ResultType.BONUS_POINTS).getAmount(), 0); // 55 * 2
		assertEquals(36.8, results.getResultItems().get(ResultType.HANDICAP_POINTS).getAmount(), 0);
		assertEquals(5.6, results.getUserHandicap(), 0);
		assertEquals(5.5, results.getResultItems().get(ResultType.NEW_HANDICAP).getAmount(), 0);
	}
	
	@Test
	public void testGameAndBonusPointCalculationWithPointCalculator_CorrectAnswersBasedOnFixedValue_Unpaid() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		Game game = instance.getGame();
		ResultConfiguration resultConfiguration = game.getResultConfiguration();
		resultConfiguration.setProperty(
				ResultConfiguration.PROPERTY_CORRECT_ANSWER_POINT_CALCULATION_TYPE, 
				CorrectAnswerPointCalculationType.BASED_ON_FIX_VALUE.getValue());
		game.setResultConfiguration(resultConfiguration);
		instance.setGame(game);
		
		// Test
		Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		
		// Verify
		assertEquals(2, results.getCorrectAnswerCount());
		
		// First question results
		Map<ResultType, ResultItem> questionResults = results.getCorrectAnswerResults().get(0).getResultItems();
		assertEquals(5.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // under 5 seconds = 3pts 
		assertEquals(4000.0, questionResults.get(ResultType.ANSWER_TIME).getAmount(), 0);
		assertEquals(7.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 2 = 7pts
		assertEquals(5.0 + 7.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0); // speed + complexity
		
		// Second question results
		questionResults = results.getCorrectAnswerResults().get(1).getResultItems();
		assertEquals(0.0, questionResults.get(ResultType.GAME_POINTS_FOR_SPEED).getAmount(), 0); // more than 5 seconds = 0pts
		assertEquals(62300.0, questionResults.get(ResultType.ANSWER_TIME).getAmount(), 0);
		assertEquals(13.0, questionResults.get(ResultType.GAME_POINTS_FOR_CORRECT_ANSWER).getAmount(), 0); // Complexity 4 = 13pts
		assertEquals(0.0 + 13.0, questionResults.get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0); // speed + complexity
		
		// Game results
		assertEquals(25.0, results.getResultItems().get(ResultType.TOTAL_GAME_POINTS).getAmount(), 0);
		assertEquals(25.5, results.getResultItems().get(ResultType.TOTAL_GAME_POINTS_WITH_BONUS).getAmount(), 0);
		assertEquals(74.0, results.getResultItems().get(ResultType.MAXIMUM_GAME_POINTS).getAmount(), 0);
		assertEquals(110.0, results.getResultItems().get(ResultType.BONUS_POINTS).getAmount(), 0); // 55 * 2
		assertEquals(36.8, results.getResultItems().get(ResultType.HANDICAP_POINTS).getAmount(), 0);
		assertEquals(5.6, results.getUserHandicap(), 0);
		assertEquals(5.5, results.getResultItems().get(ResultType.NEW_HANDICAP).getAmount(), 0);
	}
	
	@Test
	public void testGameAndBonusPointCalculationWithoutPointCalculator() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		Game game = instance.getGame();
		ResultConfiguration resultConfiguration = game.getResultConfiguration();
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_POINT_CALCULATOR, false);
		game.setResultConfiguration(resultConfiguration);
		instance.setGame(game);
		
		// Test
		Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		
		// Verify
		assertNull(results.getResultItems().get(ResultType.TOTAL_GAME_POINTS)); // Points not calculated
		assertEquals(500.0, results.getResultItems().get(ResultType.BONUS_POINTS).getAmount(), 0);
	}

	@Test
	public void testUpdateAverageAnswerTimeAndWriteWarningsOnTooBigDeviations() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		
		when(questionStatisticsAerospikeDao.updateAverageAnswerTime(anyString(), eq(4000.0))).thenReturn(30000.0); // this one will be answered too quickly
		when(questionStatisticsAerospikeDao.updateAverageAnswerTime(anyString(), eq(72000.0))).thenReturn(80000.0); // this one will be ok
		
		// Test
		utils.updateAverageAnswerTimeAndWriteWarningsOnTooBigDeviations(results, instance);
		
		// Verify
		ArgumentCaptor<EventLog> eventLog = ArgumentCaptor.forClass(EventLog.class);
		verify(eventLogAerospikeDao).createEventLog(eventLog.capture());
		assertEquals(EventLog.CATEGORY_POTENTIAL_FRAUD_WARNING, eventLog.getValue().getCategory());
		assertEquals(instance.getUserId(), eventLog.getValue().getUserId());
		assertEquals(instance.getId(), eventLog.getValue().getGameInstanceId());
		assertEquals(0, eventLog.getValue().getQuestionIndex().intValue());

		verifyNoMoreInteractions(eventLogAerospikeDao);
	}
	
	@Test
	public void testQuestionsAddedToResults() throws Exception {
		// Prepare
		GameInstance instance = prepareTestGameInstance();
		Results results = new Results();
		Answer[] answers = instance.getGameState().getAnswers();

		// Test
		for (int i = 0; i < answers.length; i++) {
			if (utils.isAnsweredCorrectly(instance.getQuestion(i), answers[i])) {
				results.addCorrectAnswer(i, instance.getQuestion(i), answers[i]);
			} else {
				results.addIncorrectAnswer(i, instance.getQuestion(i), answers[i]);
			}
		}

		// Verify
		Map<Integer, AnswerResults> answerResults = results.getAnswerResults();
		assertEquals(instance.getNumberOfQuestionsIncludingSkipped(), answerResults.size());

		answerResults.forEach((index, answerResult) -> {
			QuestionInfo info = answerResult.getQuestionInfo();
			Question question = instance.getQuestion(info.getQuestionIndex());
			Answer answer = answers[index];

			assertThat(question.getQuestionBlobKeys().toArray(),
					arrayContainingInAnyOrder(info.getQuestionBlobKeys().toArray()));
			assertThat(question.getCorrectAnswers(), arrayContainingInAnyOrder(info.getCorrectAnswers()));
			assertThat(answer == null ? new String[0] : answer.getAnswers(),
					arrayContainingInAnyOrder(info.getProvidedAnswers()));
			assertEquals(question.getResolutionImage(), info.getResolutionImage());
			assertEquals(question.getSource(), info.getSource());
			assertEquals(question.getResolutionText(), info.getResolutionText());
			assertEquals(question.getExplanation(), info.getExplanation());
		});
	}

	@Test
	public void testCalculateMultiplayerGameOutcomeDuel() throws Exception {
		prepareDuelResults(1, 2);
		prepareGameInstances(2, GameType.DUEL);

		UserResultsByHandicapRange results = utils.calculateMultiplayerGameOutcome(MPGI_ID); 
		assertEquals(2, results.getPaidPlayerCount());
		assertEquals(2, results.getFinishedPlayerCount());
		assertEquals(1, results.getHandicapRangeCount());
		assertEquals("gi2", results.getDefaultHandicapRangePlacements().get(0).getGameInstanceId());
		assertEquals("gi1", results.getDefaultHandicapRangePlacements().get(0).getDuelOpponentGameInstanceId());

		assertEquals("gi1", results.getDefaultHandicapRangePlacements().get(1).getGameInstanceId());
		assertEquals("gi2", results.getDefaultHandicapRangePlacements().get(1).getDuelOpponentGameInstanceId());
		
		List<UserResults> mutliplayerResults = results.getDefaultHandicapRangePlacements();

		// Winnings - 2 * entryFee * 0.95 = 20.045
		assertPlaces(mutliplayerResults, asList(1, 2), asList("testUser2", "testUser1"),
				asList(true, true), asList("20.04", null),
				10.55, Currency.MONEY, Currency.MONEY);
		assertNull(results.getRefundReason());
		assertEquals(GameOutcome.WINNER, results.getDefaultHandicapRangePlacements().get(0).getGameOutcome());
		assertEquals(GameOutcome.LOSER, results.getDefaultHandicapRangePlacements().get(1).getGameOutcome());
	}
	
	@Test
	public void testCalculateMultiplayerGameOutcomeDuel_Pat() throws Exception {
		prepareDuelResults(1, 5);
		prepareMultiplayerGameInstances(GameType.DUEL, new MultiplayerUserGameInstance("gi1", CLIENT_INFO_1),
				new MultiplayerUserGameInstance("gi5", CLIENT_INFO_5));

		UserResultsByHandicapRange results = utils.calculateMultiplayerGameOutcome(MPGI_ID); 
		assertEquals(2, results.getPaidPlayerCount());
		assertEquals(2, results.getFinishedPlayerCount());
		assertEquals(1, results.getHandicapRangeCount());
		
		List<UserResults> multiplayerResults = results.getDefaultHandicapRangePlacements();

		assertPlaces(multiplayerResults, asList(1, 1), asList("testUser5", "testUser1"),
				asList(true, true), asList(null, null),
				10.55, Currency.MONEY, Currency.MONEY);
		assertEquals(RefundReason.GAME_WAS_A_PAT, results.getRefundReason());
		assertEquals(GameOutcome.TIE, results.getDefaultHandicapRangePlacements().get(0).getGameOutcome());
		assertEquals(GameOutcome.TIE, results.getDefaultHandicapRangePlacements().get(1).getGameOutcome());
	}
	
	@Test
	public void testCalculateMultiplayerGameOutcomeDuel_NotFinished() throws Exception {
		prepareDuelResults(1);
		prepareMultiplayerGameInstances(GameType.DUEL, new MultiplayerUserGameInstance("gi1", CLIENT_INFO_1),
				new MultiplayerUserGameInstance("gi5", CLIENT_INFO_5));
		prepareTestGameInstance(5);

		UserResultsByHandicapRange results = utils.calculateMultiplayerGameOutcome(MPGI_ID); 
		assertEquals(2, results.getPaidPlayerCount());
		assertEquals(1, results.getFinishedPlayerCount());
		assertEquals(1, results.getHandicapRangeCount());
		
		List<UserResults> multiplayerResults = results.getDefaultHandicapRangePlacements();

		assertPlaces(multiplayerResults, asList(1, 2), asList("testUser1", "tu5"),
				asList(true, false), asList(null, null),
				10.55, Currency.MONEY, Currency.MONEY);
		assertEquals(RefundReason.GAME_NOT_FINISHED, results.getRefundReason());
		assertEquals(GameOutcome.GAME_NOT_FINISHED, results.getDefaultHandicapRangePlacements().get(0).getGameOutcome());
		assertEquals(GameOutcome.GAME_NOT_FINISHED, results.getDefaultHandicapRangePlacements().get(1).getGameOutcome());
	}
	
	@Test
	public void testCalculateMultiplayerGameOutcomeDuel_NoOpponent() throws Exception {
		prepareDuelResults(1);
		prepareMultiplayerGameInstances(GameType.DUEL, new MultiplayerUserGameInstance("gi1", CLIENT_INFO_1));

		UserResultsByHandicapRange results = utils.calculateMultiplayerGameOutcome(MPGI_ID); 
		assertEquals(1, results.getPaidPlayerCount());
		assertEquals(1, results.getFinishedPlayerCount());
		assertEquals(1, results.getHandicapRangeCount());
		
		List<UserResults> multiplayerResults = results.getDefaultHandicapRangePlacements();

		assertPlaces(multiplayerResults, asList(1), asList("testUser1"),
				asList(true), asList((String) null),
				10.55, Currency.MONEY, Currency.MONEY);
		assertEquals(RefundReason.NO_OPPONENT, results.getRefundReason());
		assertEquals(GameOutcome.NO_OPPONENT, results.getDefaultHandicapRangePlacements().get(0).getGameOutcome());
	}
	
	@Test
	public void testCalculateMultiplayerGameOutcomeTournament() throws Exception {
		// gameInstance2 -> most correct answers
		// gameInstance4 -> more game points
		// gameInstance1 and gameInstance5 -> equal
		// gameInstance6 -> did not finish
		// gameInstance3 -> another handicap category
		
		// Prepare
		GameInstance instance = prepareResult(1);
		Game game = instance.getGame();
		IntStream.range(2, 6).forEach(i -> prepareResult(i));
		prepareTestGameInstance(6);

		// Handicaps and user names should be taken from game results
		prepareGameInstances(6, GameType.TOURNAMENT);
		UserResultsByHandicapRange results = utils.calculateMultiplayerGameOutcome(MPGI_ID); 
		assertEquals(6, results.getPaidPlayerCount());
		assertEquals(5, results.getFinishedPlayerCount());
		assertEquals(2, results.getHandicapRangeCount());
		assertEquals(2, results.getHandicapRangeCount());
		
		assertEquals(5, results.getPaidPlayerCount(handicapRange(1)));
		assertEquals(4, results.getFinishedPlayerCount(handicapRange(1)));
		
		// Jackpot amount = 6 (players) * 10.55 (entry fee) * 0.95 (configured payout amount) = 60.135
		// Payout to each handicap group = 50.1125 / 2 (handicap group count) = 30.0675
		// Payout to first place = 25.05625 * 0.3 (payout percent for 1st place) = 9.02025
		// Payout to second place = 25.05625 * 0.2 (payout percent for 2nd place) = 6.0135
		// Payout to third place = 25.05625 * 0.1 (payout percent for 3rd place) = 3.00675
		// Payout to split third place = 2.505625 / 2 (number of players at third place) = 1.503375
		
		assertPlaces(results.getPlacements(handicapRange(1)), asList(1, 2, 3, 3, 4), asList("testUser2", "testUser4", "testUser5", "testUser1", "tu6"),
				asList(true, true, true, true, false), asList("9.02", "6.01", "1.50", "1.50", null),
				10.55, Currency.MONEY, Currency.MONEY);

		assertEquals(1, results.getPaidPlayerCount(handicapRange(2)));
		assertEquals(1, results.getFinishedPlayerCount(handicapRange(2)));
		assertPlaces(results.getPlacements(handicapRange(2)), asList(1), asList("testUser3"), asList(true), asList("9.02"),
				10.55, Currency.MONEY, Currency.MONEY);
		assertNull(results.getRefundReason());
	
		// Set minimum jackpot guarantee and add payout percent for fourth place
		ResultConfiguration resultConfiguration = game.getResultConfiguration();
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_MINIMUM_JACKPOT_AMOUNT, BigDecimal.valueOf(100));
		resultConfiguration.getArray(ResultConfiguration.PROPERTY_TOURNAMENT_PAYOUT_STRUCTURE).add(BigDecimal.valueOf(5));
		game.setResultConfiguration(resultConfiguration);
		instance.setGame(game);
		
		results = utils.calculateMultiplayerGameOutcome(MPGI_ID);
		// Minimum guaranteed Jackpot = 100
		// Payout to each handicap group = 100 / 2 (handicap group count) = 50
		// Payout to first place = 50 * 0.3 (payout percent for 1st place) = 15
		// Payout to second place = 50 * 0.2 (payout percent for 2nd place) = 10
		// Payout to third place = 50 * 0.1 (payout percent for 3rd place) = 5
		// Payout to fourth place = 50 * 0.05 (payout percent for 4th place) = 2.5
		// Split payout to two players in third place = 5 + 2.5 / 2 = 3.25
		
		assertPlaces(results.getPlacements(handicapRange(1)), asList(1, 2, 3, 3, 4), asList("testUser2", "testUser4", "testUser5", "testUser1", "tu6"),
				asList(true, true, true, true, false), asList("15.00", "10.00", "3.75", "3.75", null),
				10.55, Currency.MONEY, Currency.MONEY);
		assertPlaces(results.getPlacements(handicapRange(2)), asList(1), asList("testUser3"), asList(true), asList("15.00"),
				10.55, Currency.MONEY, Currency.MONEY);
		assertNull(results.getRefundReason());
		
		// Set target jackpot amount
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_MINIMUM_JACKPOT_AMOUNT, (BigDecimal) null);
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_TARGET_JACKPOT_AMOUNT, BigDecimal.valueOf(100));
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_TARGET_JACKPOT_CURRENCY, Currency.CREDIT.name());
		game.setResultConfiguration(resultConfiguration);
		instance.setGame(game);

		results = utils.calculateMultiplayerGameOutcome(MPGI_ID);
		assertPlaces(results.getPlacements(handicapRange(1)), asList(1, 2, 3, 3, 4), asList("testUser2", "testUser4", "testUser5", "testUser1", "tu6"),
				asList(true, true, true, true, false), asList("15", "10", "3", "3", null),
				10.55, Currency.MONEY, Currency.CREDIT);
		assertPlaces(results.getPlacements(handicapRange(2)), asList(1), asList("testUser3"), asList(true), asList("15"),
				10.55, Currency.MONEY, Currency.CREDIT);
		assertNull(results.getRefundReason());
		results.forEach(userResults -> {
			if (userResults.getPlace()==1) {
				assertEquals(GameOutcome.WINNER, userResults.getGameOutcome());
			} else if (userResults.isGameFinished()) {
				assertEquals(GameOutcome.LOSER, userResults.getGameOutcome());
			} else {
				assertEquals(GameOutcome.GAME_NOT_FINISHED, userResults.getGameOutcome());
			}
		});
	}
	
	@Test
	public void testAccessToWinningComponentGranted() throws Exception {
		// 2 correct answers => not eligible
		GameInstance instance1 = prepareTestGameInstance("gameInstance1.json");
		Results results1 = utils.calculateResults(instance1, CLIENT_INFO_1);
		assertEquals(2, results1.getCorrectAnswerCount());
		assertFalse(results1.isEligibleToWinnings());
		
		// 3 correct answers => eligible
		GameInstance instance2 = prepareTestGameInstance("gameInstance2.json"); 
		Results results2 = utils.calculateResults(instance2, CLIENT_INFO_1);
		assertEquals(3, results2.getCorrectAnswerCount());
		assertTrue(results2.isEligibleToWinnings());
	}

	@Test
	public void testSpecialPrizeWon() throws Exception {
		// 2 correct answers => no winning
		GameInstance instance = prepareTestGameInstance("gameInstance1.json");
		Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertEquals(2, results.getCorrectAnswerCount());
		assertNull(results.getSpecialPrizeVoucherId());
		assertStats(1, 0, 0);

		// 3 correct answers => voucher won
		instance = prepareTestGameInstance("gameInstance2.json");
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertEquals(3, results.getCorrectAnswerCount());
		assertEquals(VOUCHER_ID, results.getSpecialPrizeVoucherId());
		assertStats(2, 1, 1);
		
		Game game = instance.getGame();
		ResultConfiguration conf = game.getResultConfiguration();
		conf.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE, false);
		game.setResultConfiguration(conf);
		instance.setGame(game);
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertEquals(3, results.getCorrectAnswerCount());
		assertNull(results.getSpecialPrizeVoucherId());
		assertStats(3, 1, 1);
	}
	
	@Test
	public void testSpecialPrizeRules() throws Exception {
		GameInstance instance = prepareTestGameInstance("gameInstance2.json");

		// Every player
		Results results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(1, 1, 1);
		assertEquals(VOUCHER_ID, results.getSpecialPrizeVoucherId());

		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(2, 2, 2);
		assertEquals(VOUCHER_ID, results.getSpecialPrizeVoucherId());

		// Every X Player
		stats = new GameStatistics();
		Game game = instance.getGame();
		ResultConfiguration conf = game.getResultConfiguration();
		conf.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE_WINNING_RULE, SpecialPrizeWinningRule.EVERY_X_PLAYER.getValue());
		conf.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE_WINNING_RULE_AMOUNT, 2);
		game.setResultConfiguration(conf);
		instance.setGame(game);

		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(1, 1, 0);
		assertNull(results.getSpecialPrizeVoucherId());
		
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(2, 2, 1);
		assertEquals(VOUCHER_ID, results.getSpecialPrizeVoucherId());
		
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(3, 3, 1);
		assertNull(results.getSpecialPrizeVoucherId());
		
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(4, 4, 2);
		assertEquals(VOUCHER_ID, results.getSpecialPrizeVoucherId());
		
		// First X Players
		stats = new GameStatistics();
		conf.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE_WINNING_RULE, SpecialPrizeWinningRule.FIRST_X_PLAYERS.getValue());
		conf.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE_WINNING_RULE_AMOUNT, 2);
		game.setResultConfiguration(conf);
		instance.setGame(game);

		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(1, 1, 1);
		assertEquals(VOUCHER_ID, results.getSpecialPrizeVoucherId());
		
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(2, 2, 2);
		assertEquals(VOUCHER_ID, results.getSpecialPrizeVoucherId());
		
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(3, 3, 2);
		assertNull(results.getSpecialPrizeVoucherId());
		
		results = utils.calculateResults(instance, CLIENT_INFO_1);
		assertStats(4, 4, 2);
		assertNull(results.getSpecialPrizeVoucherId());
	}
	
	@Test
	public void testGetResultsWithUserInfo() throws Exception {
		GameInstance gameInstance = prepareResult(1);
		String firstName = "John";
		String lastName = "von Neumann";
		String nickname = "Johny";
		String country = "Hungary";
		String city = "Budapest";
		Profile profile = prepareProfile(gameInstance.getUserId(), firstName, lastName, nickname, country, city);
		
		when(profileDao.getProfiles(anyList())).thenReturn(Arrays.asList(profile));
		when(profileDao.getProfileBasicInfo(anyString())).thenCallRealMethod();
		when(profileDao.getProfileBasicInfo(anyList())).thenCallRealMethod();
		
		Results results = utils.getResults(gameInstance.getId());
		
		ApiProfileBasicInfo userInfo = jsonUtil.fromJson(results.getUserInfo(), ApiProfileBasicInfo.class);
		assertThat(userInfo.getFirstName(), nullValue());
		assertThat(userInfo.getLastName(), nullValue());
		assertThat(userInfo.getNickname(), equalTo(nickname));
		assertThat(userInfo.getCountry(), equalTo(country));
		assertThat(userInfo.getCity(), equalTo(city));
	}
	
	@Test
	public void testCalculateGameInstanceWithOneAnswerReceived() throws IOException {
		final String gameInstanceWithOneAnswerReceivedJson = jsonLoader
				.getPlainTextJsonFromResources("gameInstanceWithOneAnswerReceived.json");
		final GameInstance gameInstanceWithOneAnswerReceived = jsonUtil
				.toJsonObjectWrapper(gameInstanceWithOneAnswerReceivedJson, GameInstance::new);
		final ClientInfo registeredUserWithCustomId = ClientInfo.cloneOf(REGISTERED_CLIENT_INFO);
		registeredUserWithCustomId.setUserId("1492598330188331ca3f1-0999-429d-ae8e-60321844ab0c");
		final Results results = utils.calculateResults(gameInstanceWithOneAnswerReceived, REGISTERED_CLIENT_INFO);
		assertEquals(1, results.getCorrectAnswerCount());
		assertNull(results.getGameOutcome());
	}
	
	private Profile prepareProfile(String userId, String firstName, String lastName, String nickname, String country, String city) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		profile.setShowFullName(false);
		
		ProfileUser person = new ProfileUser();
		person.setFirstName(firstName);
		person.setLastName(lastName);
		person.setNickname(nickname);
		profile.setPersonWrapper(person);
		
		ProfileAddress address = new ProfileAddress();
		address.setCountry(country);
		address.setCity(city);
		profile.setAddress(address);
		
		return profile;
	}

	private void prepareGameInstances(int number, GameType gameType) {
		prepareMultiplayerGameInstances(gameType,
				IntStream.range(1, number + 1).mapToObj(i -> new MultiplayerUserGameInstance("gi" + i, new ClientInfo("t1", "a1", "tu" + i, "ip", 5.0)))
					.toArray(size -> new MultiplayerUserGameInstance[size]));
	}

	private void prepareMultiplayerGameInstances(GameType gameType, MultiplayerUserGameInstance... userGameInstances) {
		when(multiplayerGameInstanceDao.getGameInstances(MPGI_ID, REGISTERED, STARTED, CALCULATED)).thenReturn(Arrays.asList(userGameInstances));
		
		MultiplayerResults multiplayerResults = new MultiplayerResults(MPGI_ID, gameType);
		when(resultEngineAerospikeDao.getMultiplayerResults(MPGI_ID)).thenReturn(multiplayerResults);
	}
	
	private void assertPlaces(List<UserResults> multiplayerResults, List<Integer> places, List<String> userIds, List<Boolean> gameFinishedFlags,
			List<String> jackpotWinnings, double entryFeeAmount, Currency entryFeeCurrency, Currency jackpotWinningCurrency) {
		assertEquals(places.size(), userIds.size()); // for safety only
		for (int i = 0 ; i < places.size() ; i++) {
			final int place = places.get(i);
			final String userId = userIds.get(i);
			final boolean gameFinished = gameFinishedFlags.get(i);
			final String jackpotWinning = jackpotWinnings.get(i);
			Optional<UserResults> resultOption = multiplayerResults.stream().filter(r -> r.getUserId().equals(userId) && r.getPlace() == place).findFirst();
			assertTrue("Could not find " + userId + " in " + place +". place", resultOption.isPresent());
			UserResults result = resultOption.get();
			assertEquals(place, result.getPlace());
			
			assertEquals(BigDecimal.valueOf(entryFeeAmount), result.getEntryFeePaid());
			assertEquals(entryFeeCurrency, result.getEntryFeeCurrency());
			assertEquals(gameFinished, result.isGameFinished());

			if (jackpotWinning == null) {
				assertNull(result.getJackpotWinning());
				assertNull(result.getJackpotWinningCurrency());
			} else {
				assertEquals(new BigDecimal(jackpotWinning), result.getJackpotWinning());
				assertEquals(jackpotWinningCurrency, result.getJackpotWinningCurrency());
			}
		}
	}

	private GameInstance prepareResult(int i) {
		try {
			GameInstance instance = prepareTestGameInstance("gameInstance" + i + ".json");
			when(gameInstanceDao.getGameInstance(instance.getId())).thenReturn(instance);
			Results results = utils.calculateResults(instance, CLIENT_INFO_1);
			when(resultEngineAerospikeDao.getResults(instance.getId())).thenReturn(results);
			return instance;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void prepareDuelResults(int... is) {
		for (int i : is) {
			GameInstance result = prepareResult(i);
			Game game = result.getGame();
			game.setType(GameType.DUEL);
			result.setGame(game);
			result.setMgiId(MPGI_ID);
		}
	}

	private GameInstance prepareTestGameInstance() throws IOException {
		return prepareTestGameInstance(1);
	}

	private GameInstance prepareTestGameInstance(int i) throws IOException {
		GameInstance gi = prepareTestGameInstance("gameInstance" + i + ".json");
		when(gameInstanceDao.getGameInstance("gi" + i)).thenReturn(gi);
		return gi;
	}

	private GameInstance prepareTestGameInstance(String file) throws IOException {
		return new GameInstance(JsonTestUtil.getGson()
				.fromJson(jsonLoader.getPlainTextJsonFromResources(file), JsonObject.class));
	}

	private HandicapRange handicapRange(int i) {
		return new HandicapRange(i, 0, 0);
	}

	private void assertStats(int played, int specialPrizeAvailable, int specialPrizeWon) {
		assertEquals(played,  stats.getPlayedCount());
		assertEquals(specialPrizeAvailable,  stats.getSpecialPrizeAvailableCount());
		assertEquals(specialPrizeWon,  stats.getSpecialPrizeWonCount());
	}

}
