package de.ascendro.f4m.service.game.engine.server;

import static com.google.common.primitives.Longs.asList;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.ASSIGNED_POOLS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.GAME_ID;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.PLAYING_LANGUAGES;
import static de.ascendro.f4m.service.game.engine.json.GameJsonBuilder.createGame;
import static de.ascendro.f4m.service.game.engine.server.GamePlayer.registerAndStart;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_HANDICAP;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_LANGUAGE;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_HANDICAP;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.game.engine.GameEngineServiceStartup;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDao;
import de.ascendro.f4m.service.game.engine.exception.F4MGameFlowViolation;
import de.ascendro.f4m.service.game.engine.exception.F4MGameNotAvailableException;
import de.ascendro.f4m.service.game.engine.exception.F4MGameQuestionAlreadyAnswered;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionsNotAvailableInPool;
import de.ascendro.f4m.service.game.engine.exception.F4MUnexpectedGameQuestionAnswered;
import de.ascendro.f4m.service.game.engine.integration.GameEngineServiceStartupUsingAerospikeMock;
import de.ascendro.f4m.service.game.engine.integration.TestDataLoader;
import de.ascendro.f4m.service.game.engine.model.ActiveGameInstance;
import de.ascendro.f4m.service.game.engine.model.Answer;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.model.schema.GameEngineMessageSchemaMapper;
import de.ascendro.f4m.service.game.engine.multiplayer.TestCustomGameConfigBuilder;
import de.ascendro.f4m.service.game.engine.server.GamePlayer.PlayUntil;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserPermission;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class GameEngineTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameEngineTest.class);

	private GameEngineServiceStartup gameEngineStartup;
	private GameEngineImpl gameEngine;
	private GameInstanceAerospikeDao gameInstanceDao;
	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	private AerospikeClientProvider aerospikeClientProvider;
	private JsonUtil jsonUtil;

	private TestDataLoader testDataLoader;

	@Before
	public void setUp() throws Exception {
		gameEngineStartup = getServiceStartup();

		final Injector gameEngineInjector = gameEngineStartup.getInjector();
		gameEngine = gameEngineInjector.getInstance(GameEngineImpl.class);
		aerospikeClientProvider = gameEngineInjector.getInstance(AerospikeClientProvider.class);
		testDataLoader = new TestDataLoader(
				(AerospikeDao) gameEngineInjector.getInstance(GameInstanceAerospikeDao.class),
				aerospikeClientProvider.get(),
				gameEngineInjector.getInstance(Config.class));
		gameInstanceDao = gameEngineInjector.getInstance(GameInstanceAerospikeDao.class);
		commonMultiplayerGameInstanceDao = gameEngineInjector.getInstance(CommonMultiplayerGameInstanceDao.class);
		jsonUtil = gameEngineInjector.getInstance(JsonUtil.class);
		gameInstanceDao = gameEngineInjector.getInstance(GameInstanceAerospikeDao.class);
		commonMultiplayerGameInstanceDao = gameEngineInjector.getInstance(CommonMultiplayerGameInstanceDao.class);
	}
	
	
	@After
	public void tearDown() {
		TestDataLoader.resetQuestionsJsonFileName();
		gameEngineStartup.closeInjector();
	}

	private GameEngineServiceStartupUsingAerospikeMock getServiceStartup() {
		return new GameEngineServiceStartupUsingAerospikeMock(
				F4MIntegrationTestBase.DEFAULT_TEST_STAGE){
			@Override
			protected void bindAdditional(GameEngineMockModule module) {
				module.bindToModule(JsonMessageSchemaMap.class).toInstance(getGameEngineMessageSchemaMapperMock());
			}
		};
	}
	
	private GameEngineMessageSchemaMapper getGameEngineMessageSchemaMapperMock() {
		return new GameEngineMessageSchemaMapper() {
			private static final long serialVersionUID = 7475113988053116392L;

			@Override
			protected Map<String, Set<String>> loadRolePermissions() {
				final Map<String, Set<String>> rolePermissions = new HashMap<>();
				
				//GAME
				final Set<String> onlyGamePermission = new HashSet<>(Arrays.asList(UserPermission.GAME.name()));
				rolePermissions.put(UserRole.ANONYMOUS.name(), onlyGamePermission);
				rolePermissions.put(UserRole.REGISTERED.name(), onlyGamePermission);
				rolePermissions.put(UserRole.NOT_VALIDATED.name(), onlyGamePermission);
				
				//GAME + GAME_CREDIT
				rolePermissions.put(UserRole.FULLY_REGISTERED.name(),
						new HashSet<>(Arrays.asList(UserPermission.GAME.name(), 
								UserPermission.GAME_CREDIT.name())));
				
				//GAME + GAME_CREDIT + GAME_MONEY
				rolePermissions.put(UserRole.FULLY_REGISTERED_BANK.name(),
						new HashSet<>(Arrays.asList(UserPermission.GAME.name(), 
								UserPermission.GAME_CREDIT.name(),
								UserPermission.GAME_MONEY.name())));
				
				return rolePermissions;
			}
		};
	}

	@Test
	public void testStartDuel() throws InterruptedException, IOException {
		//Prepare test data
		final int numberOfQuestions = 5;

		final CustomGameConfigBuilder customGameConfigBuilder = TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID);

		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withNumberOfQuestions(numberOfQuestions)
				.withGameType(GameType.DUEL));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, customGameConfigBuilder, 
				Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID)));
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		testDataLoader.createProfile(REGISTERED_USER_ID, REGISTERED_USER_HANDICAP);

		
		final Map<String, String> friendship = new HashMap<>();
		friendship.put(ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		friendship.put(REGISTERED_USER_ID, ANONYMOUS_USER_ID);

		//Start games
		final GamePlayer gameStarter1 = new GamePlayer(gameEngine, GameType.DUEL, "en");
		gameStarter1.setClientInfo(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP, UserRole.ANONYMOUS);

		final GamePlayer gameStarter2 = new GamePlayer(gameEngine, GameType.DUEL, "de");
		gameStarter2.setClientInfo(REGISTERED_USER_ID, REGISTERED_USER_HANDICAP, UserRole.ANONYMOUS);

		final GameInstance[] gameInstances = registerAndStart(gameStarter1, gameStarter2);

		assertTrue(gameStarter1.getErrorMessages(), gameStarter1.getErrors().isEmpty());
		assertTrue(gameStarter2.getErrorMessages(), gameStarter2.getErrors().isEmpty());

		assertEquals(2, gameInstances.length);
		
		final GameInstance gameInstance1 = gameStarter1.getGameInstance();
		final GameInstance gameInstance2 = gameStarter2.getGameInstance();
		assertNotEquals(gameInstance1.getId(), gameInstance2.getId());
		assertNotEquals(gameInstance1.getUserId(), gameInstance2.getUserId());
		// Game instance 1
		final String gameInstance1FriendId = friendship.get(gameInstance1.getUserId());
		assertDuelGameInstance(gameInstance1, numberOfQuestions, "en");

		// Game instance 2
		final String gameInstance2FriendId = friendship.get(gameInstance2.getUserId());
		assertNotEquals(gameInstance1FriendId, gameInstance2FriendId);
		assertDuelGameInstance(gameInstance2, numberOfQuestions, "de");
		
		// Instance relation
		assertThat(asList(gameInstance1.getAllQuestionBlobKeys()),
				is(contains(gameInstance2.getAllQuestionBlobKeys())));

		//Multiplayer game entries
		final List<MultiplayerUserGameInstance> duelMultiplayerGameInstances = commonMultiplayerGameInstanceDao
				.getGameInstances(MGI_ID, MultiplayerGameInstanceState.STARTED);
		
		F4MAssert.assertSize(2, duelMultiplayerGameInstances);
		final MultiplayerUserGameInstance expectedInstance1 = new MultiplayerUserGameInstance(gameInstance1.getId(), gameStarter1.getClientInfo());
		final MultiplayerUserGameInstance expectedInstance2 = new MultiplayerUserGameInstance(gameInstance2.getId(), gameStarter2.getClientInfo());
		assertThat(duelMultiplayerGameInstances, containsInAnyOrder(expectedInstance1, expectedInstance2));
		assertFalse(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID));
	}
	
	@Test
	public void testStartLiveTournament() throws InterruptedException, IOException {
		//Prepare test data
		final int tournamentPlayerCount = 100;
		final int numberOfQuestions = 5;
		final String firstUser = "userId-0";
		
		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withNumberOfQuestions(numberOfQuestions)
				.withGameType(GameType.LIVE_TOURNAMENT));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, firstUser,
				TestCustomGameConfigBuilder.create(firstUser), null);

		final String[] users = new String[tournamentPlayerCount];
		final GamePlayer[] gameStarters = new GamePlayer[tournamentPlayerCount];
		for(int i = 0; i < tournamentPlayerCount; i++){
			if(i == 0){
				users[0] = firstUser;
			}else{
				users[i] = "userId-" + i;
			}
			
			testDataLoader.createProfile(users[i], Double.valueOf(i));
			
			final String userLanguage = TestDataLoader.PLAYING_LANGUAGES[i % TestDataLoader.PLAYING_LANGUAGES.length];
			
			if(i > 0){//chained invitations
				commonMultiplayerGameInstanceDao.addUser(MGI_ID, users[i], users[i-1], MultiplayerGameInstanceState.INVITED);
			}
			
			gameStarters[i] = new GamePlayer(gameEngine, GameType.LIVE_TOURNAMENT, userLanguage);
			gameStarters[i].setClientInfo(users[i], null, i % 10 == 0 ? UserRole.REGISTERED : UserRole.ANONYMOUS);
		}
		
		//Start tournament games
		final GameInstance[] gameInstances = registerAndStart(gameStarters);
		assertEquals(tournamentPlayerCount, gameInstances.length); //all started
		
		//No Starting errors
		Arrays.stream(gameStarters)
			.forEach(s -> assertTrue(s.getErrorMessages(), s.getErrors().isEmpty()));

		// Game instances
		final Set<String> questionBlobKeys = new HashSet<>();
		for(GameInstance gameInstance : gameInstances){
			assertNotNull(gameInstance.getUserLanguage());
			assertDuelGameInstance(gameInstance, numberOfQuestions, gameInstance.getUserLanguage());
			
			//All questions are the same
			final String[] allQuestionBlobKeys = gameInstance.getAllQuestionBlobKeys();
			Collections.addAll(questionBlobKeys, allQuestionBlobKeys);//First iteration, nothing in there
			
			assertEquals(questionBlobKeys.size(), allQuestionBlobKeys.length);
			assertThat(questionBlobKeys, is(contains(allQuestionBlobKeys)));
			
		}

		//Multiplayer game entries
		final List<MultiplayerUserGameInstance> tournamentMultiplayerGameInstances = commonMultiplayerGameInstanceDao
				.getGameInstances(MGI_ID, MultiplayerGameInstanceState.STARTED);
		F4MAssert.assertSize(tournamentPlayerCount, tournamentMultiplayerGameInstances);
		for(MultiplayerUserGameInstance userGameInstance : tournamentMultiplayerGameInstances){
			assertNotNull(userGameInstance.getGameInstanceId());
			
			assertNotNull(userGameInstance.getUserId());
			assertTrue(ArrayUtils.indexOf(users, userGameInstance.getUserId()) > -1);
			
			assertNotNull(userGameInstance.getUserHandicap());
			assertEquals(ArrayUtils.indexOf(users, userGameInstance.getUserId()), userGameInstance.getUserHandicap().intValue());
		}
		assertFalse(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID));
	}

	private void assertDuelGameInstance(GameInstance gameInstance, int numberOfQuestions, String language) {
		assertNotNull(gameInstance);
        assertEquals(numberOfQuestions, gameInstance.getNumberOfQuestionsIncludingSkipped());        
        F4MAssert.assertSize(numberOfQuestions, asList(gameInstance.getQuestionIds()));

		assertEquals(GameStatus.PREPARED, gameInstance.getGameState().getGameStatus());
		assertEquals(GameStatus.PREPARED, testDataLoader.readActiveGameInstance(gameInstance.getId()).getStatus());

		final JsonObject questionMap = gameInstance.getQuestionsMap();
		assertNotNull(questionMap);
		final Set<String> questionLanguages = questionMap.entrySet()
				.stream()
				.map(e -> jsonUtil.fromJson(e.getValue(), Question.class).getLanguage())
				.collect(Collectors.toSet());
		assertThat(questionLanguages, contains(language));

		final GameHistory gameHistory = testDataLoader.getGameHistory(gameInstance.getUserId(), gameInstance.getId());
		assertNotNull(gameHistory);
		assertEquals(GameStatus.PREPARED, gameHistory.getStatus());
		assertEquals(gameInstance.getId(), gameHistory.getGameInstanceId());
	}
	
	@Test
	public void testStartQuizWithFiveQuestionsOnly() throws IOException{
		Assume.assumeNotNull(gameEngineStartup.getInjector().getInstance(Config.class)
				.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
		TestDataLoader.setQuestionsJsonFileName("realTestQuestions.json");
		TestDataLoader.setAssignedPool(new String[] { "testPool" });
		TestDataLoader.setQuestionTypes(new String[] { "text" });

		final Injector gameEngienInjector = gameEngineStartup.getInjector();
		final QuestionPoolAerospikeDao questionPoolAerospikeDao = gameEngienInjector
				.getInstance(QuestionPoolAerospikeDao.class);
		final AerospikeClientProvider aerospikeClientProvider = gameEngienInjector
				.getInstance(AerospikeClientProvider.class);
		final GameAerospikeDao gameAerospikeDao = gameEngienInjector.getInstance(GameAerospikeDao.class);
		final Config config = gameEngienInjector.getInstance(Config.class);

		final TestDataLoader testDataLoader = new TestDataLoader((AerospikeDao) questionPoolAerospikeDao,
				aerospikeClientProvider.get(), config);

		// Load game
		final String gameId = "gameId-x";
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });
		testDataLoader.prepareTestGameData(createGame(gameId)
				.withNumberOfQuestions(5)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback());

		final Game game = gameAerospikeDao.getGame(gameId);
		assertNotNull(game);
		assertFalse(game.isDuel());
		assertEquals(GameType.QUIZ24, game.getType());
		assertEquals(TestDataLoader.MAX_COMPLEXITY, game.getComplexityStructure().length);
		assertEquals(TestDataLoader.QUESTION_TYPES.length, game.getAmountOfQuestions().length);

		// Load questions
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);

		// Test question selection
		for (int i = 0; i < 100; i++) {
			final String gameInstanceId = gameEngine.registerForFreeQuiz24(ANONYMOUS_CLIENT_INFO, gameId, null);
			final GameInstance gameInstance = gameEngine.startGame(ANONYMOUS_USER_ID, gameInstanceId, "en");
			F4MAssert.assertSize(5, new HashSet<>(Arrays.asList(gameInstance.getQuestionIds())));
		}
	}

	@Test
	public void testStartGameWithMissingUserLanguage() throws InterruptedException, IOException {
		final int numberOfQuestions = 5;
		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withNumberOfQuestions(numberOfQuestions)
				.withGameType(GameType.QUIZ24));
		testDataLoader.prepareTestGameQuestionPools(TestDataLoader.ASSIGNED_POOLS, new String[] { "de" }, //only de
				PLAYING_LANGUAGES, false);

		final GamePlayer gamePlayer = new GamePlayer(gameEngine, GameType.QUIZ24, ANONYMOUS_USER_LANGUAGE);//request start in en
		gamePlayer.setClientInfo(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP, UserRole.REGISTERED);

		registerAndStart(gamePlayer);
		assertThat(gamePlayer.getErrors(), hasItem(isA(F4MQuestionsNotAvailableInPool.class)));
	}

	@Test
	public void testReadyToPlay() throws IOException {
		final CustomGameConfigBuilder customGameConfigBuilder = TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID);
		
		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withNumberOfQuestions(1)
				.withGameType(GameType.DUEL));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);//All playing regions
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, customGameConfigBuilder, 
				Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID)));

		final GamePlayer gamePlayer = new GamePlayer(gameEngine, GameType.DUEL, ANONYMOUS_USER_LANGUAGE);
		final GameInstance startedGameInstance = gamePlayer.registerAndStart();
		assertNotNull(startedGameInstance);
		assertEquals(1, startedGameInstance.getNumberOfQuestionsIncludingSkipped());
		assertEquals(1, startedGameInstance.getQuestionsMap()
				.entrySet()
				.size());

		gameEngine.readyToPlay(startedGameInstance.getId(), System.currentTimeMillis(), false);
		assertGameState(GameStatus.READY_TO_PLAY, startedGameInstance.getId());
	}
	
	private void assertGameState(GameStatus gameStatus, String gameInstanceId){
		final GameInstance readyToPlayInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		assertNotNull(readyToPlayInstance);
		assertNotNull(readyToPlayInstance.getGameState());
		assertEquals(gameStatus, readyToPlayInstance.getGameState().getGameStatus());

		final GameHistory gameHistory = testDataLoader.getGameHistory(ANONYMOUS_USER_ID, readyToPlayInstance.getId());
		assertNotNull(gameHistory);
		assertEquals(gameStatus, gameHistory.getStatus());
		assertEquals(readyToPlayInstance.getId(), gameHistory.getGameInstanceId());
		
		assertEquals(gameStatus, testDataLoader.readActiveGameInstance(gameInstanceId).getStatus());
	}

	@Test
	public void testAnswerQuestion() throws IOException {
		final int numberOfQuestions = 2;
		final CustomGameConfigBuilder customGameConfigBuilder = TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID);
		
		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withNumberOfQuestions(numberOfQuestions)
				.withGameType(GameType.DUEL));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, customGameConfigBuilder, 
				Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID)));
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		//Start
		final GamePlayer gamePlayer = new GamePlayer(gameEngine, GameType.DUEL, ANONYMOUS_USER_LANGUAGE);
		final GameInstance startedGameInstance = gamePlayer.registerAndStart();

		//Step until the end of the game
		final Map<Integer, String[]> questionSelectedAnswers = new HashMap<>();
		final Map<Integer, List<Long>> questionSelectedClientMsT = new HashMap<>();

		GameInstance stepGameInstance = gameEngine.readyToPlay(startedGameInstance.getId(), System.currentTimeMillis(), true);
		assertNotNull("Unble to start first step when ready to play", stepGameInstance);

		GameInstance previousInstance = null;
		do {
			stepGameInstance = assertStep(startedGameInstance.getId(), previousInstance, stepGameInstance);
			assertNotNull(stepGameInstance);

			final QuestionStep currentQuestionStep = stepGameInstance.getGameState().getCurrentQuestionStep();
			final Question question = stepGameInstance.getQuestion(currentQuestionStep.getQuestion());
			if (currentQuestionStep.getStepCount() - 1 == currentQuestionStep.getStep()
					&& !questionSelectedAnswers.containsKey(currentQuestionStep.getQuestion())) {
				final String[] answers = testDataLoader.selectRandomAnswers(question);
				questionSelectedAnswers.put(currentQuestionStep.getQuestion(), answers);

				final long clientMsT = testDataLoader.selectRandomClientMsT(question, currentQuestionStep.getStep());
				putClientMsT(questionSelectedClientMsT, currentQuestionStep.getQuestion(), clientMsT);

				stepGameInstance = gameEngine.answerQuestion(ANONYMOUS_CLIENT_ID, startedGameInstance.getId(),
						currentQuestionStep.getQuestion(), clientMsT, System.currentTimeMillis(), answers);
			} else {
				final long clientMsT = testDataLoader.selectRandomClientMsT(question, currentQuestionStep.getStep());
				putClientMsT(questionSelectedClientMsT, currentQuestionStep.getQuestion(), clientMsT);
				stepGameInstance = gameEngine.registerStepSwitch(ANONYMOUS_CLIENT_ID, startedGameInstance.getId(), clientMsT,
						System.currentTimeMillis());
			}
			previousInstance = stepGameInstance;
			stepGameInstance = performNextAction(stepGameInstance);
		} while (stepGameInstance.getGameState().getGameStatus() != GameStatus.COMPLETED);

		assertEquals(startedGameInstance.getNumberOfQuestionsIncludingSkipped() - 1, stepGameInstance.getGameState()
				.getCurrentQuestionStep()
				.getQuestion());

		//Validate answers
		assertAnswers(numberOfQuestions, questionSelectedAnswers, questionSelectedClientMsT, stepGameInstance);

		final GameHistory gameHistory = testDataLoader.getGameHistory(ANONYMOUS_USER_ID, stepGameInstance.getId());
		assertNotNull(gameHistory);
		assertEquals(GameStatus.COMPLETED, gameHistory.getStatus());
		assertEquals(GameEndStatus.CALCULATING_RESULT, gameHistory.getEndStatus());
		assertEquals(stepGameInstance.getId(), gameHistory.getGameInstanceId());

		//Active game
		final ActiveGameInstance activeGameInstance = testDataLoader.readActiveGameInstance(stepGameInstance.getId());
		assertEquals(GameStatus.COMPLETED, activeGameInstance.getStatus());
		assertEquals(GameEndStatus.CALCULATING_RESULT, activeGameInstance.getEndStatus());
		assertEquals(stepGameInstance.getId(), activeGameInstance.getId());
		
		//Multiplayer game entries
		final List<MultiplayerUserGameInstance> multiplayerGameInstances = commonMultiplayerGameInstanceDao.getGameInstances(MGI_ID, MultiplayerGameInstanceState.STARTED);
		F4MAssert.assertSize(1, multiplayerGameInstances);
		assertThat(multiplayerGameInstances,
				contains(new MultiplayerUserGameInstance(stepGameInstance.getId(), gamePlayer.getClientInfo())));
		assertFalse(commonMultiplayerGameInstanceDao.hasNoRemainingGameInstances(MGI_ID));
	}

	private GameInstance performNextAction(GameInstance gameInstance) {
		GameInstance previouseInstance;
		if(gameInstance.hasAnyStepOrQuestion()){
			previouseInstance = gameEngine.performNextGameStepOrQuestion(gameInstance.getId(), System.currentTimeMillis(), false); // next
		}else{
			previouseInstance = gameEngine.performEndGame(gameInstance.getId()); // next
		}
		return previouseInstance;
	}		

	/**
	 * @param QUESTION_COUNT
	 * @param questionSelectedAnswers
	 * @param questionSelectedClientMsT
	 * @param gameState
	 */
	private void assertAnswers(final int QUESTION_COUNT, final Map<Integer, String[]> questionSelectedAnswers,
			final Map<Integer, List<Long>> questionSelectedClientMsT, final GameInstance gameInstance) {
		final GameState gameState = gameInstance.getGameState();
		final Answer[] answers = gameState.getAnswers();
		assertNotNull(answers);
		assertEquals(QUESTION_COUNT, answers.length);

		for (int i = 0; i < answers.length; i++) {
			final String[] expectedAnswers = questionSelectedAnswers.get(i);
			final List<Long> expectedClientMsT = questionSelectedClientMsT.get(i);
			assertAnswer(expectedAnswers, expectedClientMsT, i, answers[i], gameInstance.getQuestion(i));
		}
	}

	/**
	 * @param questionSelectedAnswers
	 * @param questionSelectedClientMsT
	 * @param answers
	 * @param i
	 */
	private void assertAnswer(String[] expectedAnswers, List<Long> expectedClientMsT, int expectedQuestion,
			final Answer answer, final Question question) {
		assertNotNull(answer);
		assertEquals(expectedQuestion, answer.getQuestion());

		assertThat(asList(answer.getClientMsT()), contains(expectedClientMsT.toArray(new Long[expectedClientMsT.size()])));

		//No tPRDEL as health check is not working
		assertThat(Arrays.<Long> asList(answer.getPrDelMsT()), Matchers.<Long> everyItem(IsNull.nullValue(Long.class)));
		assertThat(asList(answer.getPrDelMsT()), hasSize(equalTo(question.getStepCount())));

		assertThat(asList(answer.getAnswers()), contains(expectedAnswers));

		assertThat("Expected answer time for each step", asList(answer
				.getAnswerReceiveMsT()), hasSize(equalTo(question.getStepCount())));
		assertThat(asList(answer.getAnswerReceiveMsT()), everyItem(greaterThan(0L)));

		assertThat("Expected step start time for each step", asList(answer
				.getStepStartMsT()), hasSize(equalTo(question.getStepCount())));
		assertThat(asList(answer.getStepStartMsT()), everyItem(greaterThan(0L)));

		assertThat("Expected server time for each step", asList(answer
				.getServerMsT()), hasSize(equalTo(question.getStepCount())));

		IntStream.range(0, question.getStepCount())
				.forEach(i -> assertEquals("tServerMs[" + answer.getServerMsT()[i] + "] = answerReceiveMsT["
						+ answer.getAnswerReceiveMsT()[i] + "] - stepStartMsT[" + answer.getStepStartMsT()[i]
						+ "]", answer.getServerMsT()[i], answer.getAnswerReceiveMsT()[i] - answer.getStepStartMsT()[i]));
	}

	private void putClientMsT(Map<Integer, List<Long>> questionSelectedClientMsT, int question, long clientMsT) {
		final List<Long> clientMsTimes;
		if (questionSelectedClientMsT.containsKey(question)) {
			clientMsTimes = questionSelectedClientMsT.get(question);
		} else {
			clientMsTimes = new ArrayList<>();
			questionSelectedClientMsT.put(question, clientMsTimes);
		}
		clientMsTimes.add(clientMsT);
	}

	@Test
	public void testPerformNextGameStep() throws IOException {
		final int numberOfQuestions = 3;

		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withNumberOfQuestions(numberOfQuestions)
				.withGameType(GameType.DUEL));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID,
				TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID), null);

		//Start
		final GamePlayer gamePlayer = new GamePlayer(gameEngine, GameType.DUEL, ANONYMOUS_USER_LANGUAGE);
		final GameInstance startedGameInstance = gamePlayer.registerAndStart();

		//Ready to Play
		GameInstance stepGameInstance = gameEngine.readyToPlay(startedGameInstance.getId(), System.currentTimeMillis(), true);
		assertNotNull("Unble to start first step when ready to play", stepGameInstance);

		GameInstance previousInstance = null;
		do {
			stepGameInstance = assertStep(startedGameInstance.getId(), previousInstance, stepGameInstance);

			previousInstance = stepGameInstance;
			
			stepGameInstance = performNextAction(stepGameInstance);
			assertNotNull(stepGameInstance);
		} while (stepGameInstance.getGameState()
				.getGameStatus() != GameStatus.COMPLETED);

		assertEquals(startedGameInstance.getNumberOfQuestionsIncludingSkipped() - 1, stepGameInstance.getGameState()
				.getCurrentQuestionStep()
				.getQuestion());
		
		F4MAssert.assertSize(numberOfQuestions, new HashSet<>(Arrays.asList(startedGameInstance.getQuestionIds())));
		assertNotNull(stepGameInstance.getStartDateTime());
		assertNotNull(stepGameInstance.getEndDateTime());
	}
		
	@Test
	public void testMarkNotAcceptedInvitesIfCustomGameExpired() {
		CustomGameConfig customGameConfig = mock(CustomGameConfig.class);
		when(customGameConfig.getStartDateTime()).thenReturn(DateTimeUtil.getCurrentDateTime().minusDays(1));
		when(customGameConfig.getEndDateTime()).thenReturn(DateTimeUtil.getCurrentDateTime().plusDays(1));
		when(customGameConfig.getExpiryDateTime()).thenReturn(DateTimeUtil.getCurrentDateTime().minusMinutes(5));

		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID,
				TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID),
				Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID)));
		try {
			gameEngine.validateMultiplayerGameForRegistration(null, MGI_ID, customGameConfig);
			fail("Exception must be thrown");
		} catch (F4MGameNotAvailableException ex) {
			Map<String, String> allInvitedUsers = commonMultiplayerGameInstanceDao.getAllUsersOfMgi(MGI_ID);
			allInvitedUsers.entrySet().forEach(e -> assertThat(e.getValue(), is(MultiplayerGameInstanceState.EXPIRED.name())));
		} catch (Exception ex) {
			fail("Unexpected expired duel exception type: " + ex.getClass().getSimpleName());
		}
	}
	
	@Test(expected = F4MGameQuestionAlreadyAnswered.class)
	public void testQuestionAnsweredTwice() throws IOException{
		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withNumberOfQuestions(3));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		
		GameInstance gameInstance = new GamePlayer(gameEngine, GameType.QUIZ24, ANONYMOUS_USER_LANGUAGE)
				.registerAndStart();
		
		final String[] answers = new String[]{"a", "b"};
		
		gameEngine.readyToPlay(gameInstance.getId(), System.currentTimeMillis(), true);
		
		gameInstance = gameEngine.answerQuestion("anyClientId", gameInstance.getId(), 0, 100L, System.currentTimeMillis(), answers);
		assertThat(gameInstance.getGameState().getAnswers()[0].getAnswers(), is(answers));

		gameInstance = gameEngine.answerQuestion("anyClientId", gameInstance.getId(), 0, 100L, System.currentTimeMillis(), answers);
	}
	
	@Test(expected = F4MUnexpectedGameQuestionAnswered.class)
	public void testUnexpectedQuestionAnswered() throws IOException{
		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withNumberOfQuestions(3));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		
		GameInstance gameInstance = new GamePlayer(gameEngine, GameType.QUIZ24, ANONYMOUS_USER_LANGUAGE).registerAndStart();
		
		final String[] answers = new String[]{"a", "b"};
		
		gameEngine.readyToPlay(gameInstance.getId(), System.currentTimeMillis(), true);
		
		gameInstance = gameEngine.answerQuestion("anyClientId", gameInstance.getId(), 1, 100L, System.currentTimeMillis(), answers);
	}
	
	@Test(expected = F4MInsufficientRightsException.class)
	public void validateGameRegistrationPermissionsNoRoles() {
		final EntryFee entryFee = mock(EntryFee.class);
		gameEngine.validateGameRegistrationPermissions(entryFee, new ClientInfo());
		verifyNoMoreInteractions(entryFee);
	}
	
	private void assertQuestionStep(GameState gameState, int expectedQuestion, int expectedStep,
			Integer previousQuestion, Integer previousStep) {
		assertNotNull(gameState.getCurrentQuestionStep());

		final QuestionStep currentQuestionStep = gameState.getCurrentQuestionStep();
		final int currentQuestion = currentQuestionStep.getQuestion();
		final int currentStep = currentQuestionStep.getStep();
		final int currentStepCount = currentQuestionStep.getStepCount();

		assertEquals("Different question expected [" + previousQuestion + "->" + expectedQuestion + "], actual["
				+ previousQuestion + "->" + currentQuestion + "]", expectedQuestion, currentQuestion);
		assertEquals("Different step expected [" + previousStep + "->" + expectedStep + "], actual[" + previousStep
				+ "->" + currentStep + "]", expectedStep, currentStep);

		assertThat(currentStep, lessThan(currentStepCount));
	}

	private GameInstance assertStep(String gameInstanceId, final GameInstance previousGameInstance,
			final GameInstance stepInstance) {
		assertNotNull("Unkown state for next step", stepInstance.getGameState());
		final GameState gameState = stepInstance.getGameState();
		final QuestionStep currentQuestionStep = gameState.getCurrentQuestionStep();

		LOGGER.debug("Performed {}-{} of {}-{}", currentQuestionStep.getQuestion(), currentQuestionStep.getStep(),
				stepInstance.getNumberOfQuestionsIncludingSkipped(), currentQuestionStep.getStepCount());

		if (previousGameInstance == null) {//0-question 0-step
			assertQuestionStep(gameState, 0, 0, null, null);
		} else {
			final GameState previousGameState = previousGameInstance.getGameState();
			final QuestionStep previousQuestionStep = previousGameState.getCurrentQuestionStep();
			final int previousQuestion = previousQuestionStep.getQuestion();
			final int previousStep = previousQuestionStep.getStep();
			final int previousStateQuestionStepCount = previousQuestionStep.getStepCount();

			if (gameState.getGameStatus() == GameStatus.IN_PROGRESS) {
				final int nextQuestion =
						previousStateQuestionStepCount - 1 == previousStep ? previousQuestion + 1 : previousQuestion;
				final int nextStep = nextQuestion == previousQuestion ? 1 : 0;
				assertQuestionStep(gameState, nextQuestion, nextStep, previousQuestion, previousQuestionStep.getStep());
			} else {
				assertQuestionStep(gameState, previousQuestion, previousStateQuestionStepCount
						- 1, previousQuestion, previousStep);//if game ends it stops at last questions step
			}
		}

		return stepInstance;
	}
	
	@Test
	public void testGameCancelScenarios() throws IOException {
		final int numberOfQuestions = 3;

		final Runnable prepareData = () -> {
			try {
				aerospikeClientProvider.get().close();
				testDataLoader.prepareTestGameData(createGame(GAME_ID)
						.withNumberOfQuestions(numberOfQuestions)
						.withGameType(GameType.DUEL));
				testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
				testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID,
						TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		//Register
		prepareData.run();
		GameInstance gameInstance = playUntilAndCancel(PlayUntil.REGISTER);
		assertGameState(gameInstanceDao.getGameInstance(gameInstance.getId()).getGameState(), 
				GameStatus.CANCELLED, null, true, RefundReason.GAME_NOT_PLAYED.name());
		assertEquals(MultiplayerGameInstanceState.CANCELLED, commonMultiplayerGameInstanceDao.getUserState(MGI_ID, ANONYMOUS_USER_ID));

		//Start
		prepareData.run();
		gameInstance = playUntilAndCancel(PlayUntil.START);
		assertGameState(gameInstanceDao.getGameInstance(gameInstance.getId()).getGameState(), 
				GameStatus.CANCELLED, null, true, RefundReason.GAME_NOT_PLAYED.name());
		assertEquals(MultiplayerGameInstanceState.CANCELLED, commonMultiplayerGameInstanceDao.getUserState(MGI_ID, ANONYMOUS_USER_ID));
		
		//ReadyToPlay
		prepareData.run();
		gameInstance = playUntilAndCancel(PlayUntil.READY_TO_PLAY);
		assertGameState(gameInstanceDao.getGameInstance(gameInstance.getId()).getGameState(), 
				GameStatus.CANCELLED, null, false, null);
		assertEquals(MultiplayerGameInstanceState.CANCELLED, commonMultiplayerGameInstanceDao.getUserState(MGI_ID, ANONYMOUS_USER_ID));
		
		//Answer only first question, so rest of questions are filled in empty and result calculation is requested
		prepareData.run();
		gameInstance = playUntilAndCancel(PlayUntil.ANSWER_FIRST);
		assertGameState(gameInstanceDao.getGameInstance(gameInstance.getId()).getGameState(), 
				GameStatus.CANCELLED, GameEndStatus.CALCULATING_RESULT, false, null);
		assertEquals(MultiplayerGameInstanceState.CANCELLED, commonMultiplayerGameInstanceDao.getUserState(MGI_ID, ANONYMOUS_USER_ID));
		assertThat(gameInstanceDao.getGameInstance(gameInstance.getId()).getGameState().getAnswers(),
				arrayWithSize(numberOfQuestions));
		
		//Answer all questions so calculate is requested
		prepareData.run();
		try {
			playUntilAndCancel(PlayUntil.ANSWER_ALL);
			fail("Cannot cancel already ended game");
		} catch (F4MGameFlowViolation vEx) {
			assertEquals("Cannot cancel already ended game", vEx.getMessage());
		}
	}
	
	private GameInstance playUntilAndCancel(PlayUntil playUntil){
		final GamePlayer gamePlayer = new GamePlayer(gameEngine, GameType.DUEL, ANONYMOUS_USER_LANGUAGE);
		final GameInstance gameInstance = gamePlayer.playUntil(playUntil);
		gameEngine.cancelGameByClient(ANONYMOUS_CLIENT_INFO, gameInstance.getId());
		return gameInstance;
	}
	
	private void assertGameState(GameState state, GameStatus status, GameEndStatus endStatus, boolean refundable, String refundReason) {
		assertEquals(GameStatus.CANCELLED, state.getGameStatus());
		assertEquals(endStatus, state.getGameEndStatus());
		assertEquals(refundable, state.isRefundable());
		assertEquals(refundReason, state.getRefundReason());
	}

	
}
