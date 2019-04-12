package de.ascendro.f4m.service.result.engine.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.model.GameStatistics;
import de.ascendro.f4m.service.result.engine.util.GameStatisticsPrimaryKeyUtil;

public class GameStatisticsAerospikeDaoImplTest extends RealAerospikeTestBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameStatisticsAerospikeDaoImplTest.class);
	
	private static final String GAME_ID = "testGame";
	
	private GameStatisticsPrimaryKeyUtil gameStatisticsPrimaryKeyUtil;
	private GameStatisticsDao gameStatisticsDao;
	private final JsonUtil jsonUtil = new JsonUtil();	

	private static final String GAME_STATISTICS_SET = "gameStatistics_test";
	private static final String NAMESPACE = "test";
	
	@Before
	@Override
	public void setUp() {
		super.setUp();
		
		if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
			clearTestSet();
		}
	}
	
	@Override
	protected void setUpAerospike() {
		gameStatisticsPrimaryKeyUtil = new GameStatisticsPrimaryKeyUtil(config);
		gameStatisticsDao = new GameStatisticsAerospikeDaoImpl(config, gameStatisticsPrimaryKeyUtil, aerospikeClientProvider, jsonUtil);
	}

	@Override
	protected Config createConfig() {
		final Config config = new ResultEngineConfig();
		config.setProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE, NAMESPACE);
		config.setProperty(ResultEngineConfig.AEROSPIKE_GAME_STATISTICS_SET, GAME_STATISTICS_SET);
		return config;
	}
	
	private void clearTestSet() {
		LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", GAME_STATISTICS_SET, NAMESPACE);
		clearSet(NAMESPACE, GAME_STATISTICS_SET);
	}

	@Test
	public void testUpdateGameStatistics() {
		assertStats(gameStatisticsDao.updateGameStatistics(GAME_ID, 0, 0, 0), 0, 0, 0);
		assertStats(gameStatisticsDao.updateGameStatistics(GAME_ID, 5, 3, 9), 5, 3, 9);
		assertStats(gameStatisticsDao.updateGameStatistics(GAME_ID, 1, 2, 3), 6, 5, 12);
		assertStats(gameStatisticsDao.updateGameStatistics(GAME_ID, 0, 0, 0), 6, 5, 12);
	}

	private void assertStats(GameStatistics stats, int playedCount, int specialPrizeAvailableCount, int specialPrizeWonCount) {
		assertEquals(playedCount, stats.getPlayedCount());
		assertEquals(specialPrizeAvailableCount, stats.getSpecialPrizeAvailableCount());
		assertEquals(specialPrizeWonCount, stats.getSpecialPrizeWonCount());
	}
	
}
