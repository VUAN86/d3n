package de.ascendro.f4m.service.result.engine.dao;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.After;
import org.junit.Test;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.history.dao.GameHistoryPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryList;
import de.ascendro.f4m.service.result.engine.model.GameHistoryUpdateKind;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class CompleteGameHistoryAerospikeDaoImplTest extends RealAerospikeTestBase {

	private static final String USER_ID = "userId";
	private static final String TENANT_ID = "tenantId";
	private static final String TARGET_USER_ID = "targetUserId";
	private static final ZonedDateTime DATETIME = DateTimeUtil.getCurrentDateTime();
	private static final LocalDate DATE = DATETIME.toLocalDate();
	private static final GameType GAME_TYPE = GameType.DUEL;
	private static final String GAME_INSTANCE_ID = "gameInstanceId";
	
	private final Config config = new ResultEngineConfig();
	private final GameHistoryPrimaryKeyUtil pkUtil = new GameHistoryPrimaryKeyUtil(config);
	private final JsonUtil jsonUtil = new JsonUtil();

	private final String namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
	private final String set = config.getProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET) + "_test";
	private CompletedGameHistoryAerospikeDaoImpl dao;

	@Override
	public void setUp() {
		config.setProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET, set);
		super.setUp();
	}

	@Override
	protected void setUpAerospike() {
		dao = new CompletedGameHistoryAerospikeDaoImpl(config, pkUtil, aerospikeClientProvider, jsonUtil);
	}
	
	@After
	@Override
	public void tearDown(){
		try {
			clearSet(namespace, set);
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void testMoveGameHistory() {
		final String key = pkUtil.createCompletedGameHistoryPrimaryKey(USER_ID, TENANT_ID, GAME_TYPE,
				DateTimeUtil.formatISODate(DATE));
		assertEquals("history:profile:" + USER_ID + ":tenant:" + TENANT_ID + ":gameType:" + GAME_TYPE.name() +
				":completedAt:" + DATE, key);

		// Save
		CompletedGameHistoryInfo completedGameHistoryInfo = new CompletedGameHistoryInfo();
		completedGameHistoryInfo.setUserId(USER_ID);
		completedGameHistoryInfo.setTenantId(TENANT_ID);
		completedGameHistoryInfo.setGameInstanceId(GAME_INSTANCE_ID);
		completedGameHistoryInfo.setEndDateTime(DATETIME);
		dao.saveResultsForHistory(USER_ID, TENANT_ID, GameType.DUEL, DATE, completedGameHistoryInfo, GameHistoryUpdateKind.INITIAL);

		// Verify saved
		CompletedGameHistoryList result = dao.getCompletedGamesList(USER_ID, TENANT_ID, GAME_TYPE, 0L, 100, 
				ZonedDateTime.of(DATE, LocalTime.MIN, ZoneOffset.UTC), ZonedDateTime.of(DATE, LocalTime.MAX, ZoneOffset.UTC));
		assertEquals(1, result.getTotal());
		
		// Move
		dao.moveCompletedGameHistory(USER_ID, TARGET_USER_ID, TENANT_ID);
		
		// Verify moved
		result = dao.getCompletedGamesList(TARGET_USER_ID, TENANT_ID, GAME_TYPE, 0L, 100, 
				ZonedDateTime.of(DATE, LocalTime.MIN, ZoneOffset.UTC), ZonedDateTime.of(DATE, LocalTime.MAX, ZoneOffset.UTC));
		assertEquals(1, result.getTotal());
		
		// Previous should be deleted
		result = dao.getCompletedGamesList(USER_ID, TENANT_ID, GAME_TYPE, 0L, 100, 
				ZonedDateTime.of(DATE, LocalTime.MIN, ZoneOffset.UTC), ZonedDateTime.of(DATE, LocalTime.MAX, ZoneOffset.UTC));
		assertEquals(0, result.getTotal());
	}
	
	@Test
	public void testSaveResultsForHistoryWithoutOverwritingState() throws Exception {
		// prepare
		CompletedGameHistoryInfo history = new CompletedGameHistoryInfo();
		history.setGameInstanceId(GAME_INSTANCE_ID);
		history.setMultiplayerGameFinished(false);
		saveCompletedGameHistoryAndAssertState(history, GameHistoryUpdateKind.INITIAL, false, 0);
		
		// change state to calculated
		history.setMultiplayerGameFinished(true);
		history.setPlacement(4);
		saveCompletedGameHistoryAndAssertState(history, GameHistoryUpdateKind.INITIAL, true, 4);
		
		// test that state cannot be changed from finished to not-finished
		history.setMultiplayerGameFinished(false);
		history.setPlacement(2);
		saveCompletedGameHistoryAndAssertState(history, GameHistoryUpdateKind.INITIAL, true, 2);

		// test placement not updated if updating bonus points
		history.setMultiplayerGameFinished(false);
		history.setPlacement(3);
		saveCompletedGameHistoryAndAssertState(history, GameHistoryUpdateKind.BONUS_POINTS, true, 2);
	}

	private void saveCompletedGameHistoryAndAssertState(CompletedGameHistoryInfo history, GameHistoryUpdateKind updateKind, boolean isMultiplayerGameFinished, int place) {
		dao.saveResultsForHistory(USER_ID, TENANT_ID, GAME_TYPE, DATE, history, updateKind);
		
		CompletedGameHistoryList historyList = dao.getCompletedGamesList(USER_ID, TENANT_ID, GAME_TYPE, 0L, 1, DATETIME.minusDays(1), DATETIME.plusDays(1));
		CompletedGameHistoryInfo updatedHistory = historyList.getCompletedGameList().get(0);

		assertThat(updatedHistory.isMultiplayerGameFinished(), equalTo(isMultiplayerGameFinished));
		assertEquals(place, updatedHistory.getPlacement());
	}

}
