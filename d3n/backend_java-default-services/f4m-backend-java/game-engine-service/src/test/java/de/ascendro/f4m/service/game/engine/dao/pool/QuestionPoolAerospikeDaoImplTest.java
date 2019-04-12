package de.ascendro.f4m.service.game.engine.dao.pool;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.PLAYING_LANGUAGES;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.POOL_LANGUAGES;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionCannotBeReadFromPool;
import de.ascendro.f4m.service.game.engine.integration.TestDataLoader;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;

public class QuestionPoolAerospikeDaoImplTest extends RealAerospikeTestBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionPoolAerospikeDaoImplTest.class);
	
	private static final String POOL_ID = "22";
	private static final String QUESTION_TYPE = "35";
	private static final String USER_LANGUAGE = "en";
	private static final int COMPLEXITY = 3;

	private final Config config = new GameEngineConfig();
	private final QuestionPoolPrimaryKeyUtil questionPoolPrimaryKeyUtil = new QuestionPoolPrimaryKeyUtil(config);
	private final JsonUtil jsonUtil = new JsonUtil();

	private final String namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
	private final String testQuestionPoolSet = config.getProperty(GameEngineConfig.AEROSPIKE_QUESTION_POOL_SET) + "2";

	private QuestionPoolAerospikeDaoImpl questionPoolAerospikeDao;
	private TestDataLoader testDataLoader;

	@Override
	public void setUp() {
		super.setUp();
		config.setProperty(GameEngineConfig.AEROSPIKE_QUESTION_POOL_SET, testQuestionPoolSet);
		
		testDataLoader = new TestDataLoader((AerospikeDao) questionPoolAerospikeDao, aerospikeClientProvider.get(),
				config);		
	}

	@Override
	protected void setUpAerospike() {
		questionPoolAerospikeDao =
				new QuestionPoolAerospikeDaoImpl(config, questionPoolPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Test
	public void testGetQuestion() throws IOException {
		final InternationalQuestionKey interQuestionKey =
				new InternationalQuestionKey(POOL_ID, QUESTION_TYPE, COMPLEXITY, PLAYING_LANGUAGES);
		final QuestionKey questionKey = new QuestionKey(interQuestionKey, USER_LANGUAGE);

		assertEquals(0, questionPoolAerospikeDao.getTotalQuestionCount(questionKey));
		assertEquals(0, questionPoolAerospikeDao.getTotalQuestionCount(interQuestionKey));

		LOGGER.info("Preparing Question Pool data within Aerospike set {} at namespace {}", testQuestionPoolSet, namespace);
		testDataLoader.prepareTestGameQuestionPool(POOL_ID, POOL_LANGUAGES, PLAYING_LANGUAGES, true);

		LOGGER.info("Asserting that Question Pool data within Aerospike set {} at namespace {} is created", testQuestionPoolSet, namespace);
		
		//Contains questions
		assertThat(questionPoolAerospikeDao.getTotalQuestionCount(questionKey), greaterThan(0L));
		assertNotNull(questionPoolAerospikeDao.getQuestion(questionKey, 0));
		assertNotNull(questionPoolAerospikeDao.getQuestion(questionKey, 1));

		//Contains index
		assertThat(questionPoolAerospikeDao.getTotalQuestionCount(interQuestionKey), greaterThan(0L));
		assertNotNull(questionPoolAerospikeDao.getInternationalQuestionIndex(interQuestionKey, 0));
		assertNotNull(questionPoolAerospikeDao.getInternationalQuestionIndex(interQuestionKey, 1));
	}
	
	@Test(expected = F4MQuestionCannotBeReadFromPool.class)
	public void testGetQuestionByInternationlIndex() {
		questionPoolAerospikeDao.getQuestionByInternationlIndex(new QuestionIndex(), "any");
	}
	
}
