package de.ascendro.f4m.service.game.engine.dao.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.model.CloseUpReason;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.util.JsonLoader;

public class GameInstanceAerospikeDaoImplTest extends RealAerospikeTestBase{
	
	private static final String GAME_INSTANCE_ID = "14932893993000828d611-a065-49f7-a940-cfcddb1cdc7d";
	private static final String GAME_INSTANCE_WITH_JOKERS_ID = "jokers";

	private final GameEnginePrimaryKeyUtil questionPoolPrimaryKeyUtil = new GameEnginePrimaryKeyUtil(createConfig());
	private final JsonUtil jsonUtil = new JsonUtil();
	private final JsonLoader jsonLoader = new JsonLoader(this);
	
	private GameInstanceAerospikeDao gameInstanceAerospikeDao;

	@Override
	public void setUp() {
		super.setUp();
		try {
			prepareGameInstanceInProgressWithOneAnswer(GAME_INSTANCE_ID, "gameInstanceInProgressWithOneAnswer.json");
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	private void prepareGameInstanceInProgressWithOneAnswer(String gameInstanceId, String fileName) throws IOException {
		final String gameInstanceSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_INSTANCE_SET);
		final String gameInstanceKey = questionPoolPrimaryKeyUtil.createPrimaryKey(gameInstanceId);
		gameInstanceAerospikeDao.deleteSilently(gameInstanceSet, gameInstanceKey);
		final String gameInstanceInProgressWithOneAnswerJson = jsonLoader
				.getPlainTextJsonFromResources(fileName);
		gameInstanceAerospikeDao.createJson(gameInstanceSet, gameInstanceKey,
				CommonGameInstanceAerospikeDao.BLOB_BIN_NAME, gameInstanceInProgressWithOneAnswerJson);
	}

	@Override
	protected Config createConfig() {
		return new GameEngineConfig();
	}

	@Override
	protected void setUpAerospike() {
		gameInstanceAerospikeDao = new GameInstanceAerospikeDaoImpl(config, questionPoolPrimaryKeyUtil,
				aerospikeClientProvider, jsonUtil);
	}

	@Test
	public void testCalculateIfUnfinishedForGameInProgress() {
		final GameInstance resultGameInstance = gameInstanceAerospikeDao.calculateIfUnfinished(GAME_INSTANCE_ID,
				CloseUpReason.NO_INVITEE, null, null);
		final GameState gameState = resultGameInstance.getGameState();
		assertEquals(GameStatus.CANCELLED, gameState.getGameStatus());
		assertEquals(GameEndStatus.CALCULATING_RESULT, gameState.getGameEndStatus());
	}

	@Test
	public void testNullJokerConfiguration() {
		final Game game = gameInstanceAerospikeDao.getGameByInstanceId(GAME_INSTANCE_ID);
		assertEquals(0, game.getJokerConfiguration().size());
	}
	
	@Test
	public void testWithJokerConfiguration() throws Exception {
		prepareGameInstanceInProgressWithOneAnswer(GAME_INSTANCE_WITH_JOKERS_ID, "gameInstanceWithJokerConfiguration.json");
		final Game game = gameInstanceAerospikeDao.getGameByInstanceId(GAME_INSTANCE_WITH_JOKERS_ID);
		assertEquals(1, game.getJokerConfiguration().size());
		assertEquals(Integer.valueOf(3), game.getJokerConfiguration().get(JokerType.FIFTY_FIFTY).getAvailableCount());
	}
}
