package de.ascendro.f4m.server.dashboard.dao;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.selection.model.dashboard.MostPlayedGame;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.GameType;

public class DashboardDaoImplTest extends RealAerospikeTestBase {

	private static final String USER_ID_1 = "user_id_1";
	private static final String GAME_ID_1 = "game_id_1";
	private static final String GAME_ID_2 = "game_id_2";
	private static final String TITLE_SUFFIX = "_title";

	private final GameConfigImpl gameConfig = new GameConfigImpl();
	private final AerospikeConfigImpl aerospikeConfig = new AerospikeConfigImpl();
	private final DashboardPrimaryKeyUtil dashboardKeyUtil = new DashboardPrimaryKeyUtil(gameConfig);
	private final JsonUtil jsonUtil = new JsonUtil();
	private DashboardDao dashboardDao;

	@Override
	protected void setUpAerospike() {
		dashboardDao = new DashboardDaoImpl(new F4MConfigImpl(config, gameConfig), dashboardKeyUtil, jsonUtil,
				aerospikeClientProvider);
	}

	@Override
	public void setUp() {
		super.setUp();
		clearSet();
	}

	@Override
	public void tearDown() {
		try {
			clearSet();
		} finally {
			super.tearDown();
		}
	}

	private void clearSet() {
		String set = gameConfig.getProperty(GameConfigImpl.AEROSPIKE_DASHBOARD_SET);
		String namespace = aerospikeConfig.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
		if (aerospikeClientProvider != null && aerospikeClientProvider instanceof AerospikeClientProvider) {
			super.clearSet(namespace, set);
		}
	}

	@Test
	public void testLastPlayedGame() {
		dashboardDao.updateLastPlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_1, GameType.QUIZ24, GAME_ID_1 + TITLE_SUFFIX, false));
		assertLastPlayedQuiz(USER_ID_1, GAME_ID_1);

		dashboardDao.updateLastPlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_2, GameType.QUIZ24, GAME_ID_2 + TITLE_SUFFIX, false));
		assertLastPlayedQuiz(USER_ID_1, GAME_ID_2);
	}

	private void assertLastPlayedQuiz(String userId, String gameId) {
		PlayedGameInfo lastPlayedGame = dashboardDao.getLastPlayedGame(TENANT_ID, userId, GameType.QUIZ24);
		assertThat(lastPlayedGame.getGameId(), equalTo(gameId));
	}

	@Test
	public void testMostPlayedGame() {
		assertMostPlayedGame(null, 0L);
		
		dashboardDao.updateLastPlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_1, GameType.QUIZ24, GAME_ID_1 + TITLE_SUFFIX, false));
		assertMostPlayedGame(GAME_ID_1, 1L);

		dashboardDao.updateLastPlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_1, GameType.QUIZ24, GAME_ID_1 + TITLE_SUFFIX, false));
		dashboardDao.updateLastPlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_2, GameType.QUIZ24, GAME_ID_2 + TITLE_SUFFIX, false));
		assertMostPlayedGame(GAME_ID_1, 2L);

		dashboardDao.updateLastPlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_2, GameType.QUIZ24, GAME_ID_2 + TITLE_SUFFIX, false));
		dashboardDao.updateLastPlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_2, GameType.QUIZ24, GAME_ID_2 + TITLE_SUFFIX, false));
		assertMostPlayedGame(GAME_ID_2, 3L);
	}

	private void assertMostPlayedGame(String gameId, long numberOfPlays) {
		List<String> gameIds = Arrays.asList(GAME_ID_1, GAME_ID_2);
		MostPlayedGame mostPlayedGame = dashboardDao.getMostPlayedGame(gameIds);
		assertThat(mostPlayedGame.getGameInfo().getGameId(), equalTo(gameId));
		assertThat(mostPlayedGame.getNumberOfPlayers(), equalTo(numberOfPlays));
	}
	
	@Test
	public void testIndexOfMax() {
		Long[] emptyArray = new Long[] {};
		assertIndexOfMax(emptyArray, nullValue());

		Long[] arrayOfNulls = new Long[] { null, null };
		assertIndexOfMax(arrayOfNulls, nullValue());
		
		Long[] arrayOfNumbers = new Long[] { 3L, 7L, 3L, 5L };
		assertIndexOfMax(arrayOfNumbers, equalTo(1));
		
		Long[] arrayOfMixedValues = new Long[] { null, 4L, null, 7L };
		assertIndexOfMax(arrayOfMixedValues, equalTo(3));
	}
	
	private void assertIndexOfMax(Long[] array, Matcher<Object> matcher) {
		assertThat(DashboardDaoImpl.getIndexOfMax(array), matcher);
	}

}
