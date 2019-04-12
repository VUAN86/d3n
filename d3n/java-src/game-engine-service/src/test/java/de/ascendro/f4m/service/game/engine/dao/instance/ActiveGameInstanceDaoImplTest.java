package de.ascendro.f4m.service.game.engine.dao.instance;

import static de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl.END_STATUS_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl.ENTRY_FEE_AMOUNT_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl.ENTRY_FEE_CURRENCY_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl.ID_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl.MGI_ID_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl.STATUS_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl.USER_ID_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.game.engine.json.GameJsonBuilder.createGame;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.integration.TestDataLoader;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class ActiveGameInstanceDaoImplTest extends RealAerospikeTestBase {
	private static final String AGI_SET = "activeGameInstance";
	private static final String AGI_NAMESPACE = "active";

	private static final String GAME_INSTANCE_ID = "game_instance_id";

	private final Config config = new GameEngineConfig();

	private final GameEnginePrimaryKeyUtil primaryKeyUtil = new GameEnginePrimaryKeyUtil(config);
	private final JsonUtil jsonUtil = new JsonUtil();
	private final String recordKey = primaryKeyUtil.createPrimaryKey(GAME_INSTANCE_ID);

	private ActiveGameInstanceDaoImpl activeGameInstanceDao;
	private AerospikeDao aerospikeDao;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		config.setProperty(GameEngineConfig.AEROSPIKE_ACTIVE_GAME_INSTANCE_NAMESPACE, AGI_NAMESPACE);
	}

	@Override
	protected void setUpAerospike() {
		activeGameInstanceDao = new ActiveGameInstanceDaoImpl(config, primaryKeyUtil, aerospikeClientProvider,
				jsonUtil);
		aerospikeDao = (AerospikeDao) activeGameInstanceDao;
	}

	@Test
	public void testGetNamespace() {
		assertEquals(AGI_NAMESPACE, activeGameInstanceDao.getNamespace());
	}

	@Test
	public void testCreate() throws IOException {
		final GameInstance gameInstance = new GameInstance(createGame(TestDataLoader.GAME_ID)
				.withGameType(GameType.DUEL)
				.buildGame(jsonUtil));
		gameInstance.setId(GAME_INSTANCE_ID);
		gameInstance.setUserId(REGISTERED_USER_ID);
		gameInstance.setMgiId(MGI_ID);
		activeGameInstanceDao.create(gameInstance, DateTimeUtil.getCurrentDateTime().plusHours(1));

		assertEquals(GAME_INSTANCE_ID, aerospikeDao.readString(AGI_SET, recordKey, ID_BIN_NAME));
		assertEquals(GameStatus.REGISTERED.name(), aerospikeDao.readString(AGI_SET, recordKey, STATUS_BIN_NAME));
		assertEquals(REGISTERED_USER_ID, aerospikeDao.readString(AGI_SET, recordKey, USER_ID_BIN_NAME));
		assertEquals(MGI_ID, aerospikeDao.readString(AGI_SET, recordKey, MGI_ID_BIN_NAME));
		assertEquals(GameType.DUEL.name(), aerospikeDao.readString(AGI_SET, recordKey, ActiveGameInstanceDaoImpl.GAME_TYPE_BIN_NAME));
		assertNull(aerospikeDao.readString(AGI_SET, recordKey, END_STATUS_BIN_NAME));
		assertNull(aerospikeDao.readString(AGI_SET, recordKey, ENTRY_FEE_CURRENCY_BIN_NAME));
		assertNull(aerospikeDao.readString(AGI_SET, recordKey, ENTRY_FEE_AMOUNT_BIN_NAME));

		aerospikeDao.delete(AGI_SET, recordKey);

		final GameState gameState = new GameState(GameStatus.IN_PROGRESS);
		gameState.setGameEndStatus(GameEndStatus.TERMINATED);
		gameInstance.setGameState(gameState);
		gameInstance.setEntryFeeAmount(BigDecimal.TEN);
		gameInstance.setEntryFeeCurrency(Currency.BONUS);
		activeGameInstanceDao.create(gameInstance, DateTimeUtil.getCurrentDateTime().plusHours(1));
		assertEquals(GameEndStatus.TERMINATED.name(), aerospikeDao.readString(AGI_SET, recordKey, END_STATUS_BIN_NAME));
		assertEquals(Currency.BONUS.name(), aerospikeDao.readString(AGI_SET, recordKey, ENTRY_FEE_CURRENCY_BIN_NAME));
		assertEquals("10", aerospikeDao.readString(AGI_SET, recordKey, ENTRY_FEE_AMOUNT_BIN_NAME));
	}

	@Test
	public void testUpdateStatus() throws IOException {
		final GameInstance gameInstance = new GameInstance(createGame(TestDataLoader.GAME_ID)
				.withGameType(GameType.DUEL)
				.buildGame(jsonUtil));
		gameInstance.setId(GAME_INSTANCE_ID);
		gameInstance.setUserId(REGISTERED_USER_ID);
		activeGameInstanceDao.create(gameInstance, DateTimeUtil.getCurrentDateTime().plusHours(1));

		activeGameInstanceDao.updateStatus(GAME_INSTANCE_ID, GameStatus.COMPLETED, GameEndStatus.CALCULATED_RESULT);

		assertEquals(GameStatus.COMPLETED.name(), aerospikeDao.readString(AGI_SET, recordKey, STATUS_BIN_NAME));
		assertEquals(GameEndStatus.CALCULATED_RESULT.name(),
				aerospikeDao.readString(AGI_SET, recordKey, END_STATUS_BIN_NAME));
	}

}
