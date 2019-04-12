package de.ascendro.f4m.service.result.engine.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.aerospike.client.policy.WritePolicy;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.UpdateCall;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.AerospikeMockDaoImpl;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.model.QuestionStatistics;
import de.ascendro.f4m.service.result.engine.util.QuestionStatisticsPrimaryKeyUtil;
import de.ascendro.f4m.service.util.TestGsonProvider;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class QuestionStatisticsAerospikeDaoImplTest {

	private static final String QUESTION_ID = "q1";

	private Config config = new ResultEngineConfig();
	private QuestionStatisticsPrimaryKeyUtil primaryKeyUtil = new QuestionStatisticsPrimaryKeyUtil(config);
	private JsonUtil jsonUtil = new JsonUtil();

	private JsonMessageUtil jsonMessageUtil;

	private AerospikeClientMockProvider aerospikeClientMockProvider;
	private String set;
	private String key;
	private AerospikeDao aerospikeDao;

	private QuestionStatisticsAerospikeDao questionStatisticsAerospikeDao;
	
	
	@Before
	public void setUp() throws Exception {
		jsonMessageUtil = new JsonMessageUtil(new TestGsonProvider(), null, null);
		
		set = config.getProperty(ResultEngineConfig.AEROSPIKE_QUESTION_STATISTICS_SET);
		key = primaryKeyUtil.createPrimaryKey(QUESTION_ID);
		aerospikeClientMockProvider = new AerospikeClientMockProvider(config, new ServiceMonitoringRegister());
		aerospikeDao = new AerospikeMockDaoImpl(config, primaryKeyUtil, jsonUtil, aerospikeClientMockProvider);
		
		questionStatisticsAerospikeDao = new QuestionStatisticsAerospikeDaoImpl(config, primaryKeyUtil, aerospikeClientMockProvider,
				jsonUtil, jsonMessageUtil);
	}

	@Test
	public void testUpdateAverageAnswerTime() throws Exception {
		
		assertNull(getStats());
		assertNull(questionStatisticsAerospikeDao.updateAverageAnswerTime(QUESTION_ID, 15)); // too few results => no result
		assertStats(1, 15.0);
		
		assertNull(questionStatisticsAerospikeDao.updateAverageAnswerTime(QUESTION_ID, 35)); // too few results => no result
		assertStats(2, 25.0);
		
		QuestionStatistics stats = getStats();
		stats.setUsersAnswered(100L); // Have info already from 100 users
		aerospikeDao.updateJson(set, key, QuestionStatisticsAerospikeDao.BLOB_BIN_NAME, new UpdateCall<String>() {
			@Override
			public String update(String readResult, WritePolicy writePolicy) {
				return stats.getAsString();
			}
		});
		
		assertEquals(25.0, questionStatisticsAerospikeDao.updateAverageAnswerTime(QUESTION_ID, 350).doubleValue(), 0); // extreme ignored
		assertStats(100, 25.0);

		assertEquals(25.11, questionStatisticsAerospikeDao.updateAverageAnswerTime(QUESTION_ID, 37).doubleValue(), 2);
		assertStats(101, 25.11);
	}
	
	
	private QuestionStatistics getStats() {
		String json = aerospikeDao.readJson(set, key, QuestionStatisticsAerospikeDao.BLOB_BIN_NAME);
		return json == null ? null : new QuestionStatistics(jsonMessageUtil.fromJson(json, JsonObject.class));
	}
	
	private void assertStats(long usersAnswered, double averageAnswerTime) {
		QuestionStatistics stats = getStats();
		assertEquals(QUESTION_ID, stats.getId());
		assertEquals(usersAnswered, stats.getUsersAnswered().longValue());
		assertEquals(averageAnswerTime, stats.getAverageAnswerTime().doubleValue(), 2);
	}

}
