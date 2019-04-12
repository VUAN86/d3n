package de.ascendro.f4m.server.dashboard.move.dao;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.dashboard.dao.DashboardDao;
import de.ascendro.f4m.server.dashboard.dao.DashboardDaoImpl;
import de.ascendro.f4m.server.dashboard.dao.DashboardPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.GameType;

public class MoveDashboardDaoImplTest extends RealAerospikeTestBase {

	private static final String DASHBOARD_SET = "dashboard";
	
	private static final String DUEL_ID = "duel_id";
	private static final String QUIZ_ID = "quiz_id";

	private final AerospikeConfigImpl aerospikeConfig = new AerospikeConfigImpl();
	private final F4MConfigImpl gameConfig = new F4MConfigImpl(aerospikeConfig, new GameConfigImpl());
	private final DashboardPrimaryKeyUtil primaryKeyUtil = new DashboardPrimaryKeyUtil(gameConfig);
	private final JsonUtil jsonUtil = new JsonUtil();

	private MoveDashboardDaoImpl moveDashboardDao;
	private DashboardDao dashboardDao;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		clearSet(DASHBOARD_SET);
	}

	@Override
	@After
	public void tearDown() {
		try {
			clearSet(DASHBOARD_SET);
		} finally {
			super.tearDown();
		}
	}

	private void clearSet(String set) {
		String namespace = aerospikeConfig.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
		if (aerospikeClientProvider != null && aerospikeClientProvider instanceof AerospikeClientProvider) {
			super.clearSet(namespace, set);
		}
	}

	@Override
	protected void setUpAerospike() {
		moveDashboardDao = new MoveDashboardDaoImpl(gameConfig, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
		dashboardDao = new DashboardDaoImpl(gameConfig, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Test
	public void testMoveLastPlayedGame() throws Exception {
		// prepare: DUEL and QUIZ24 played by ANONYMOUS_USER_ID
		PlayedGameInfo playedDuel = getPlayedGameInfo(DUEL_ID, GameType.DUEL);
		PlayedGameInfo playedQuiz = getPlayedGameInfo(QUIZ_ID, GameType.QUIZ24);
		dashboardDao.updateLastPlayedGame(TENANT_ID, ANONYMOUS_USER_ID, playedDuel);
		dashboardDao.updateLastPlayedGame(TENANT_ID, ANONYMOUS_USER_ID, playedQuiz);
		
		// validate: ANONYMOUS_USER_ID has last played games
		assertLastPlayedGame(ANONYMOUS_USER_ID, GameType.DUEL, DUEL_ID);
		assertLastPlayedGame(ANONYMOUS_USER_ID, GameType.QUIZ24, QUIZ_ID);
		assertLastPlayedGame(REGISTERED_USER_ID, GameType.DUEL, null);
		assertLastPlayedGame(REGISTERED_USER_ID, GameType.QUIZ24, null);
		
		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		moveDashboardDao.moveLastPlayedGame(TENANT_ID, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		
		// validate: REGISTERED_USER_ID has last played games
		assertLastPlayedGame(ANONYMOUS_USER_ID, GameType.DUEL, null);
		assertLastPlayedGame(ANONYMOUS_USER_ID, GameType.QUIZ24, null);
		assertLastPlayedGame(REGISTERED_USER_ID, GameType.DUEL, DUEL_ID);
		assertLastPlayedGame(REGISTERED_USER_ID, GameType.QUIZ24, QUIZ_ID);
	}
	
	private void assertLastPlayedGame(String userId, GameType type, String gameId) {
		PlayedGameInfo lastPlayedGame = dashboardDao.getLastPlayedGame(TENANT_ID, userId, type);
		assertThat(lastPlayedGame.getGameId(), equalTo(gameId));
	}
	
	private PlayedGameInfo getPlayedGameInfo(String gameId, GameType type) {
		return new PlayedGameInfo(gameId, type, "any game title", false);
	}

}
