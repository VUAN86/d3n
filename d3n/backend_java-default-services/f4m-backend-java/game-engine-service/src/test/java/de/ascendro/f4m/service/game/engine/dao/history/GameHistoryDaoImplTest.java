package de.ascendro.f4m.service.game.engine.dao.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDao;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDaoImpl;
import de.ascendro.f4m.server.history.dao.GameHistoryPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.integration.TestDataLoader;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.ServiceUtil;

public class GameHistoryDaoImplTest extends RealAerospikeTestBase{
	private static final Logger LOGGER = LoggerFactory.getLogger(GameHistoryDaoImplTest.class);
	
	private static final long TIMESTAMP = 42L;
	private static final String GAME_INSTANCE_ID = "01181fa8-a678-11e6-80f5-76304dec7eb7";
	private static final String GAME_INSTANCE_ID2 = "01181fa8-a678-11e6-80f5-76304dec7eb8";
	private static final String GAME_ID = "21181fa8-a678-11e6-80f5-76304dec7eb7";
	private static final String USER_ID = "41181fa8-a678-11e6-80f5-76304dec7eb7";
	private static final String MGI_ID = "71181fa8-a678-11e6-80f5-76304dec7eb7";

	private static final String GAME_HISTORY_SET = "gameHistory_test";
	private static final String NAMESPACE = "test";

	private final JsonUtil jsonUtil = new JsonUtil();	
	private GameHistoryPrimaryKeyUtil gameHistoryPrimaryKeyUtil;
	private ServiceUtil serviceUtil;

	private CommonGameHistoryDao commonGameHistoryDao;
	private GameHistoryDao gameHistoryDao;
	private TestDataLoader testDataLoader;

	@Before
	@Override
	public void setUp(){
		serviceUtil = mock(ServiceUtil.class);
		when(serviceUtil.getMessageTimestamp()).thenReturn(TIMESTAMP);
		
		super.setUp();
		
		if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
			clearTestHistorySet();
		}
		
		
	}
	
	@Override
	protected void setUpAerospike() {
		gameHistoryPrimaryKeyUtil = new GameHistoryPrimaryKeyUtil(config);
		commonGameHistoryDao = new CommonGameHistoryDaoImpl(config, gameHistoryPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		gameHistoryDao = new GameHistoryDaoImpl(config, gameHistoryPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		testDataLoader = new TestDataLoader((AerospikeDao)gameHistoryDao, aerospikeClientProvider.get(), config);
	}
	
	@Override
	@After
	public void tearDown() {
		if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
			try {
				clearTestHistorySet();
			} finally {
				super.tearDown();
			}
		}
	}

	private void clearTestHistorySet() {
		LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", GAME_HISTORY_SET, NAMESPACE);
		clearSet(NAMESPACE, GAME_HISTORY_SET);
	}
	
	@Override
	protected Config createConfig() {
		final Config config = new GameEngineConfig();
		
		config.setProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE, NAMESPACE);
		config.setProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET, GAME_HISTORY_SET);
		
		return config;
	}
	
	private GameHistory createUserGameHistoryEntry(GameHistory gameHistoryEntry){
		final GameHistory savedGameHistory = gameHistoryDao.createUserGameHistoryEntry(USER_ID, GAME_INSTANCE_ID, gameHistoryEntry);
		
		assertNotNull(savedGameHistory);
		assertEquals(GAME_INSTANCE_ID, savedGameHistory.getGameInstanceId());
		assertEquals(gameHistoryEntry.getStatus(), savedGameHistory.getStatus());
		assertEquals(gameHistoryEntry.getEndStatus(), savedGameHistory.getEndStatus());
		assertEquals(gameHistoryEntry.getGameId(), savedGameHistory.getGameId());
		assertEquals(gameHistoryEntry.getMgiId(), savedGameHistory.getMgiId());
		
		return savedGameHistory;
	}

	@Test
	public void testUpdateAndGetGameHistory() {
		final GameHistory preparedGameEntry = getGameHistory(GAME_INSTANCE_ID, GameStatus.PREPARED, null);

		//Create new entry
		createUserGameHistoryEntry(preparedGameEntry);

		//get list of history
		final List<String> gameHistoryListAfterCreate = commonGameHistoryDao.getUserGameHistory(USER_ID);
		assertNotNull(gameHistoryListAfterCreate);
		assertEquals("Created game history not found", 1, gameHistoryListAfterCreate.size());
		JsonTestUtil.assertJsonContentEqual(preparedGameEntry.getAsString(), gameHistoryListAfterCreate.get(0));

		//Get game particular instance game
		final GameHistory particularGameInstanceAfterCreate = testDataLoader.getGameHistory(USER_ID, GAME_INSTANCE_ID);
		assertNotNull(particularGameInstanceAfterCreate);
		JsonTestUtil.assertJsonContentEqual(preparedGameEntry.getAsString(), particularGameInstanceAfterCreate.getAsString());
		
		//Complete game
		final GameHistory completedGame = getGameHistory(GAME_INSTANCE_ID, GameStatus.COMPLETED, GameEndStatus.CALCULATING_RESULT);
		createUserGameHistoryEntry(completedGame);

		//get list of history
		final List<String> gameHistoryListAfterUpdate = commonGameHistoryDao.getUserGameHistory(USER_ID);
		assertNotNull(gameHistoryListAfterUpdate);
		assertEquals("Created game history not found", 1, gameHistoryListAfterUpdate.size());
		JsonTestUtil.assertJsonContentEqual(completedGame.getAsString(), gameHistoryListAfterUpdate.get(0));

		//Get game particular instance game
		final GameHistory particularCompletedGameInstance = testDataLoader.getGameHistory(USER_ID, GAME_INSTANCE_ID);
		assertNotNull(particularCompletedGameInstance);
		JsonTestUtil.assertJsonContentEqual(completedGame.getAsString(), particularCompletedGameInstance.getAsString());
	}

	@Test
	public void testAddAndIsQuestionPlayed() {
		final String[] initialPlayedQuestions = new String[]{"1", "1", "2", "3", "5"};
		Arrays.stream(initialPlayedQuestions)
			.forEach(q -> gameHistoryDao.addPlayedQuestions(USER_ID, q));
		
		Arrays.stream(initialPlayedQuestions)
			.forEach(q -> assertTrue(gameHistoryDao.isQuestionPlayed(USER_ID, q)));
		
		final String[] additionalPlayedQuestions = new String[]{"3", "5", "8", "13", "21"};
		Arrays.stream(additionalPlayedQuestions)
			.forEach(q -> gameHistoryDao.addPlayedQuestions(USER_ID, q));
		
		final Set<String> allPlayedQuestions = new HashSet<>();
		Collections.addAll(allPlayedQuestions, initialPlayedQuestions);
		Collections.addAll(allPlayedQuestions, additionalPlayedQuestions);
		
		allPlayedQuestions.stream()
			.forEach(q -> assertTrue(gameHistoryDao.isQuestionPlayed(USER_ID, q)));
	}
	
	@Test
	public void testCreateUserGameHistoryEntry(){
		final GameHistory history1 = new GameHistory();
		history1.setGameInstanceId(GAME_INSTANCE_ID);
		
		final GameHistory history2 = new GameHistory();
		history2.setGameInstanceId(GAME_INSTANCE_ID2);
		
		final GameHistory newGameHistory1 = gameHistoryDao.createUserGameHistoryEntry(USER_ID, GAME_INSTANCE_ID, history1);
		assertNotNull(newGameHistory1);
		assertEquals(GAME_INSTANCE_ID, newGameHistory1.getGameInstanceId());
		
		final GameHistory newGameHistory2 = gameHistoryDao.createUserGameHistoryEntry(USER_ID, GAME_INSTANCE_ID2, history2);
		assertNotNull(newGameHistory2);
		assertEquals(GAME_INSTANCE_ID2, newGameHistory2.getGameInstanceId());		
	}
	
	private GameHistory getGameHistory(String gameInstanceId, GameStatus gameStatus,
			GameEndStatus gameEndStatus) {
		final GameHistory gameHistory = new GameHistory();

		gameHistory.setGameInstanceId(gameInstanceId);
		gameHistory.setEndStatus(gameEndStatus);
		gameHistory.setStatus(gameStatus);
		gameHistory.setMgiId(MGI_ID);
		gameHistory.setGameId(GAME_ID);

		return gameHistory;
	}

}
