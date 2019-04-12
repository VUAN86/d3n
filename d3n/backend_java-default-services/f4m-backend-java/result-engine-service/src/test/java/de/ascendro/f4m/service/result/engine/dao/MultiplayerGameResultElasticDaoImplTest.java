package de.ascendro.f4m.service.result.engine.dao;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.model.MultiplayerGameResult;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class MultiplayerGameResultElasticDaoImplTest {

	private static final int ELASTIC_PORT = 9213;

	private static final String USER_ID_1 = "uid1";
	private static final String USER_ID_2 = "uid2";
	private static final String USER_ID_3 = "uid3";
	private static final String USER_ID_4 = "uid4";
	private static final String USER_ID_5 = "uid5";
	private static final String USER_ID_6 = "uid6";
	private static final String USER_ID_7 = "uid7";

	private static final int HANDICAP_RANGE_1 = 1;
	private static final int HANDICAP_RANGE_2 = 2;

	private static final String MGI_ID = "mgi1";
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);
	
	private ElasticClient client;
	
	private MultiplayerGameResultElasticDao multiplayerGameResultDao;
	
	@Before
	public void setUp() throws Exception {
		ResultEngineConfig config = new ResultEngineConfig();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		ElasticUtil elasticUtil = new ElasticUtil();
		client = new ElasticClient(config, elasticUtil, new ServiceMonitoringRegister());
		multiplayerGameResultDao = new MultiplayerGameResultElasticDaoImpl(client, config);
	}
	
	@After
	public void shutdown() {
		client.close();
	}
	
	@Test
	public void testIndex() {
		prepareData();
		
		// Wrong mgiId
		assertContents("otherId", null, null, 100, 0);

		// Test handicap ranges
		assertContents(MGI_ID, null, null, 100, 0, USER_ID_1, USER_ID_2, USER_ID_5, USER_ID_3, USER_ID_6, USER_ID_4, USER_ID_7);
		assertContents(MGI_ID, HANDICAP_RANGE_1, null, 100, 0, USER_ID_1, USER_ID_2, USER_ID_3, USER_ID_4);
		assertContents(MGI_ID, HANDICAP_RANGE_2, null, 100, 0, USER_ID_5, USER_ID_6, USER_ID_7);
		
		// Test buddy user Ids
		assertContents(MGI_ID, null, Arrays.asList(USER_ID_5, USER_ID_3, USER_ID_7), 100, 0, USER_ID_5, USER_ID_3, USER_ID_7);
		assertContents(MGI_ID, null, Arrays.asList(USER_ID_1, USER_ID_4), 100, 0, USER_ID_1, USER_ID_4);
		assertContents(MGI_ID, HANDICAP_RANGE_1, Arrays.asList(USER_ID_5, USER_ID_3, USER_ID_7), 100, 0, USER_ID_3);
		assertContents(MGI_ID, HANDICAP_RANGE_2, Arrays.asList(USER_ID_5, USER_ID_3, USER_ID_7), 100, 0, USER_ID_5, USER_ID_7);

		// Test paging
		assertContents(MGI_ID, null, null, 3, 0, USER_ID_1, USER_ID_2, USER_ID_5);
		assertContents(MGI_ID, null, null, 2, 2, USER_ID_5, USER_ID_3);
		
		// Test all
		assertContents(MGI_ID, HANDICAP_RANGE_1, Arrays.asList(USER_ID_1, USER_ID_3, USER_ID_4), 1, 1, USER_ID_3);
	}

	@Test
	public void testRank() {
		prepareData();
		
		// Wrong mgiId
		assertEquals(1, multiplayerGameResultDao.getMultiplayerGameRankByResults("otherId", USER_ID_1, 3, 20.5, null, null));

		// Test handicap ranges
		assertEquals(1, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_1, 3, 20.5, null, null));

		assertEquals(2, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_2, 3, 20.5, null, null));
		assertEquals(2, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_2, 3, 20.5, HANDICAP_RANGE_1, null));
		assertEquals(1, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_2, 3, 20.5, HANDICAP_RANGE_2, null));
		
		assertEquals(5, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_6, 3, 18.5, null, null));
		assertEquals(4, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_6, 3, 18.5, HANDICAP_RANGE_1, null));
		assertEquals(2, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_6, 3, 18.5, HANDICAP_RANGE_2, null));

		// Test buddy user Ids
		assertEquals(4, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_6, 3, 18.5, null, Arrays.asList(USER_ID_2, USER_ID_5, USER_ID_3, USER_ID_6)));
		assertEquals(3, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_6, 3, 18.5, HANDICAP_RANGE_1, Arrays.asList(USER_ID_2, USER_ID_5, USER_ID_3, USER_ID_6)));
		assertEquals(1, multiplayerGameResultDao.getMultiplayerGameRankByResults(MGI_ID, USER_ID_6, 3, 18.5, HANDICAP_RANGE_2, Arrays.asList(USER_ID_6)));
	}
	
	private void assertContents(String mgiId, Integer handicapRangeId, List<String> buddyIds, int limit, long offset, String... userIds) {
		assertThat(multiplayerGameResultDao.listResults(mgiId, handicapRangeId, buddyIds, limit, offset, true).getItems()
				.stream().map(b -> b.getUserId()).collect(Collectors.toList()), 
				userIds == null || userIds.length == 0 ? hasSize(0) : contains(userIds));
	}
	
	private void prepareData() {
		// handicap range 1
		prepareMultiplayerGameResult(MGI_ID, USER_ID_1, 1, 1, 3, 20.5);
		prepareMultiplayerGameResult(MGI_ID, USER_ID_2, 1, 1, 3, 20.5);
		prepareMultiplayerGameResult(MGI_ID, USER_ID_3, 2, 1, 3, 18.5);
		prepareMultiplayerGameResult(MGI_ID, USER_ID_4, 3, 1, 2, 30.5);
		
		// handicap range 2
		prepareMultiplayerGameResult(MGI_ID, USER_ID_5, 1, 2, 3, 20.5);
		prepareMultiplayerGameResult(MGI_ID, USER_ID_6, 2, 2, 3, 18.5);
		prepareMultiplayerGameResult(MGI_ID, USER_ID_7, 3, 2, 2, 30.5);
	}

	private void prepareMultiplayerGameResult(String mgiId, String userId, int place, int handicapRangeId, int correctAnswers, double gamePointsWithBonus) {
		multiplayerGameResultDao.createOrUpdate(new MultiplayerGameResult(mgiId, userId, handicapRangeId, 0, 100, place, correctAnswers, gamePointsWithBonus));
	}
	
}
