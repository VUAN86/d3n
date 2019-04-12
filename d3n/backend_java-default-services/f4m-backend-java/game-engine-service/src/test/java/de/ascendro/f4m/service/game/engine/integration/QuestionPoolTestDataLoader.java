package de.ascendro.f4m.service.game.engine.integration;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.ASSIGNED_POOLS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MAX_COMPLEXITY;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.PLAYING_LANGUAGES;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.QUESTION_TYPES;
import static de.ascendro.f4m.service.game.engine.json.GameJsonBuilder.createGame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameAerospikeDaoImpl;
import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDao;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDaoImpl;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDaoImplTest;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolPrimaryKeyUtil;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class QuestionPoolTestDataLoader extends RealAerospikeTestBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionPoolAerospikeDaoImplTest.class);
	
	private static final String TENANT_ID = "1";
	private static final String APP_ID = "appId";
	//indexes
	private static final String ALL_INDEX_KEY = String.format("game:tenant:%s:app:%s", TENANT_ID, APP_ID);
	private static final String FREE_INDEX_KEY = String.format("game:tenant:%s:app:%s:free:%b", TENANT_ID, APP_ID, true);
	private static final String ONLINE_INDEX_KEY = String.format("game:tenant:%s:app:%s:offline:%b", TENANT_ID, APP_ID, false);
	private static final String QUIZ_INDEX_KEY = String.format("game:tenant:%s:app:%s:type:QUIZ24", TENANT_ID, APP_ID);
	private static final String DUEL_INDEX_KEY = String.format("game:tenant:%s:app:%s:type:DUEL", TENANT_ID, APP_ID);
	private static final String TOURNAMENT_INDEX_KEY = String.format("game:tenant:%s:app:%s:type:TOURNAMENT", TENANT_ID, APP_ID);
	private static final String LIVE_TOURNAMENT_INDEX_KEY = String.format("game:tenant:%s:app:%s:type:LIVE_TOURNAMENT", TENANT_ID, APP_ID);
	private static final String USER_TOURNAMENT_INDEX_KEY = String.format("game:tenant:%s:app:%s:type:USER_TOURNAMENT", TENANT_ID, APP_ID);

	private final JsonUtil jsonUtil = new JsonUtil();
	
	private QuestionPoolPrimaryKeyUtil questionPoolPrimaryKeyUtil;
	private GamePrimaryKeyUtil gamePrimaryKeyUtil;

	private String namespace;
	private String questionPoolSet;
	private String gameSet;

	private QuestionPoolAerospikeDao questionPoolAerospikeDao;
	private GameAerospikeDao gameAerospikeDao;

	@Override
	public void setUp() {
		super.setUp();
		Assume.assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
		
		questionPoolPrimaryKeyUtil = new QuestionPoolPrimaryKeyUtil(config);
		gamePrimaryKeyUtil = new GamePrimaryKeyUtil(config);
		
		namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
		questionPoolSet = config.getProperty(GameEngineConfig.AEROSPIKE_QUESTION_POOL_SET);
		gameSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);
		
		TestDataLoader.setQuestionsJsonFileName("realTestQuestions.json");
		TestDataLoader.setAssignedPool(new String[] { "-1" });
		TestDataLoader.setQuestionTypes(new String[] { "-1" });
	}
	
	@Override
	public void tearDown() {
		TestDataLoader.resetQuestionsJsonFileName();
		super.tearDown();
	}
	
	@Override
	protected Config createConfig(){
		return new GameEngineConfig(); 
	}
	
	@Override
	protected void setUpAerospike() {
		questionPoolAerospikeDao =
				new QuestionPoolAerospikeDaoImpl(config, questionPoolPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		gameAerospikeDao = new GameAerospikeDaoImpl(config, gamePrimaryKeyUtil, aerospikeClientProvider, jsonUtil);
	}

	@Test
	public void fillRealAerospikeWithGameData() throws IOException {		
		LOGGER.info("PreparingGame data within Aerospike set {} at namespace {}", gameSet, namespace);
		final AerospikeDao aerospikeDao = (AerospikeDao)gameAerospikeDao;
		final TestDataLoader testDataLoader =
				new TestDataLoader((AerospikeDao) questionPoolAerospikeDao, aerospikeClientProvider.get(), config);
		
		final String gameId1 = "-1";
		((AerospikeDao)gameAerospikeDao).deleteSilently(gameSet, gamePrimaryKeyUtil.createPrimaryKey(gameId1));
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });	
		testDataLoader.prepareTestGameData(createGame(gameId1)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(300))
				.withNumberOfQuestions(3)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback());
		
		final String gameId2 = "-2";
		((AerospikeDao)gameAerospikeDao).deleteSilently(gameSet, gamePrimaryKeyUtil.createPrimaryKey(gameId2));
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });
		testDataLoader.prepareTestGameData(createGame(gameId2)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(300))
				.withNumberOfQuestions(5)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback());
		
		final String gameId3 = "-3";
		((AerospikeDao)gameAerospikeDao).deleteSilently(gameSet, gamePrimaryKeyUtil.createPrimaryKey(gameId3));
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });	
		testDataLoader.prepareTestGameData(createGame(gameId3)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(300))
				.withTimeToAcceptInvites(5)//5 minutes to approve/decline
				.withNumberOfQuestions(3)
				.withGameType(GameType.DUEL)
				.withInstantAnswerFeedback());
		
		final String gameId4 = "-4";
		((AerospikeDao)gameAerospikeDao).deleteSilently(gameSet, gamePrimaryKeyUtil.createPrimaryKey(gameId4));
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });	
		testDataLoader.prepareTestGameData(createGame(gameId4)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(300))
				.withNumberOfQuestions(3)
				.withGameType(GameType.TOURNAMENT)
				.withInstantAnswerFeedback());
		
		final String gameId5 = "-5";
		((AerospikeDao)gameAerospikeDao).deleteSilently(gameSet, gamePrimaryKeyUtil.createPrimaryKey(gameId5));
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });	
		testDataLoader.prepareTestGameData(createGame(gameId5)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(300))
				.withNumberOfQuestions(0)
				.withGameType(GameType.USER_TOURNAMENT)
				.withEntryFeeDecidedByPlayer(false)
				.withCustomUserPools(true)
				.withInstantAnswerFeedback());
		
		final String gameId6 = "-6";
		((AerospikeDao)gameAerospikeDao).deleteSilently(gameSet, gamePrimaryKeyUtil.createPrimaryKey(gameId6));
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });	
		testDataLoader.prepareTestGameData(createGame(gameId6)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(300))
				.withNumberOfQuestions(0)
				.withGameType(GameType.DUEL)
				.withEntryFeeDecidedByPlayer(true)
				.withEntryFee(new BigDecimal("3"), Currency.BONUS)
				.withCustomUserPools(true)
				.withInstantAnswerFeedback());
		
		int ltGameId = -1000;
		while(aerospikeDao.exists("mgi", String.format("mgi:tournament:%d", ltGameId))){
			ltGameId--;
		}
		final String gameIdLT1000 = String.valueOf(ltGameId);
		((AerospikeDao)gameAerospikeDao).deleteSilently(gameSet, gamePrimaryKeyUtil.createPrimaryKey(gameIdLT1000));
		TestDataLoader.setAmountOfQuestions(new int[] { 100 });
		TestDataLoader.setComplexityStructure(new int[] { 40, 30, 20, 5, 5 });	
		testDataLoader.prepareTestGameData(createGame(gameIdLT1000)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusDays(300))
				.withNumberOfQuestions(3)
				.withGameType(GameType.LIVE_TOURNAMENT));

		LOGGER.info("Asserting that Game data within Aerospike set {} at namespace {} is created", gameSet, namespace);
		
		//Update index
		
		
		//Game 1
		final Game game1Index = jsonUtil.fromJson("{\"gameId\":\"-1\",\"title\":\"Free QUIZ24 with 3 questions\",\"handicap\":0.5,\"numberOfQuestions\":3}", Game.class);
		Stream.of(ALL_INDEX_KEY, FREE_INDEX_KEY, ONLINE_INDEX_KEY, QUIZ_INDEX_KEY)
			.forEach(indexKey -> updateGameIndex(indexKey, game1Index, aerospikeDao));
		
		//Game 2
		final Game game2Index = jsonUtil.fromJson("{\"gameId\":\"-2\",\"title\":\"Free QUIZ24 with 5 questions\",\"handicap\":0.5,\"numberOfQuestions\":5}", Game.class);
		Stream.of(ALL_INDEX_KEY, FREE_INDEX_KEY, ONLINE_INDEX_KEY, QUIZ_INDEX_KEY)
			.forEach(indexKey -> updateGameIndex(indexKey, game2Index, aerospikeDao));

		//Game 3
		final Game game3Index = jsonUtil.fromJson("{\"gameId\":\"-3\",\"title\":\"Free duel\",\"handicap\":0.5,\"numberOfQuestions\":3}", Game.class);
		Stream.of(ALL_INDEX_KEY, FREE_INDEX_KEY, ONLINE_INDEX_KEY, DUEL_INDEX_KEY)
			.forEach(indexKey -> updateGameIndex(indexKey, game3Index, aerospikeDao));
		
		//Game 4 - (normal) TOURNAMNET
		aerospikeDao.deleteSilently("game", TOURNAMENT_INDEX_KEY);
		final Game game4Index = jsonUtil.fromJson("{\"gameId\":\"-4\",\"title\":\"Free normal tournament\",\"handicap\":0.5,\"numberOfQuestions\":3}", Game.class);
		Stream.of(ALL_INDEX_KEY, FREE_INDEX_KEY, ONLINE_INDEX_KEY, TOURNAMENT_INDEX_KEY)
			.forEach(indexKey -> updateGameIndex(indexKey, game4Index, aerospikeDao));

		//Game 5 - USER_TOURNAMENT
		aerospikeDao.deleteSilently("game", USER_TOURNAMENT_INDEX_KEY);
		final Game game5Index = jsonUtil.fromJson("{\"gameId\":\"-5\",\"title\":\"Customizable user tournament\",\"handicap\":0.5,\"numberOfQuestions\":3}", Game.class);
		Stream.of(ALL_INDEX_KEY, FREE_INDEX_KEY, ONLINE_INDEX_KEY, USER_TOURNAMENT_INDEX_KEY)
			.forEach(indexKey -> updateGameIndex(indexKey, game5Index, aerospikeDao));
		
		//Game 6 - 3BP DUEL
		aerospikeDao.deleteSilently("game", DUEL_INDEX_KEY);
		final Game game6Index = jsonUtil.fromJson("{\"gameId\":\"-6\",\"title\":\"Customizable duel with 3BP entry fee (3 questions by default)\",\"handicap\":0.5,\"numberOfQuestions\":3}", Game.class);
		Stream.of(ALL_INDEX_KEY, FREE_INDEX_KEY, ONLINE_INDEX_KEY, DUEL_INDEX_KEY)
			.forEach(indexKey -> updateGameIndex(indexKey, game6Index, aerospikeDao));
		
		//Game 1*** - free LIVE_TOURNAMENT
		aerospikeDao.deleteSilently("game", LIVE_TOURNAMENT_INDEX_KEY);	
		
		final String gameLT1000Index = String.format("{\"gameId\":\"%d\",\"title\":\"Free live tournament v.%d\",\"handicap\":0.5,\"numberOfQuestions\":3}", ltGameId, ltGameId);
		final Game gameLT1000IndexAsJson = jsonUtil.fromJson(gameLT1000Index, Game.class);
		Stream.of(ALL_INDEX_KEY, FREE_INDEX_KEY, ONLINE_INDEX_KEY, LIVE_TOURNAMENT_INDEX_KEY)
			.forEach(indexKey -> updateGameIndex(indexKey, gameLT1000IndexAsJson, aerospikeDao));
	}
	
	private void updateGameIndex(String indexKey, Game newGame, AerospikeDao aerospikeDao) {
		final String gameListAsString = aerospikeDao.readJson("game", indexKey, "value");
		final JsonArray gameListAsJsonArray;
		boolean alreadyExists = false;
		
		if(StringUtils.isNotBlank(gameListAsString)){
			gameListAsJsonArray = jsonUtil.fromJson(gameListAsString, JsonArray.class);
			for(int i = 0; i < gameListAsJsonArray.size(); i++){
				final JsonElement gameAsJson = gameListAsJsonArray.get(i);
				final Game game = jsonUtil.fromJson(gameAsJson, Game.class);
				if(game.getGameId().equals(newGame.getGameId())){
					gameListAsJsonArray.set(i, jsonUtil.toJsonElement(newGame));
					alreadyExists = true;
					break;
				}
			}
		}else{
			gameListAsJsonArray = new JsonArray();
		}
		
		if(!alreadyExists){
			gameListAsJsonArray.add(jsonUtil.toJsonElement(newGame));
		}
		aerospikeDao.createOrUpdateJson("game", indexKey, "value", (v, wp) -> jsonUtil.toJson(gameListAsJsonArray));
	}

	@Test
	public void fillRealAerospikeWithQuestionPoolData() throws IOException {
		LOGGER.info("Preparing Question Pool data within Aerospike set {} at namespace {}", questionPoolSet, namespace);
		final TestDataLoader testDataLoader =
				new TestDataLoader((AerospikeDao) questionPoolAerospikeDao, aerospikeClientProvider.get(), config);
		final AerospikeDao aerospikeDao = (AerospikeDao)gameAerospikeDao;
		clearQuestionPoolMetaKeys(aerospikeDao);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);

		LOGGER.info("Asserting that Question Pool data within Aerospike set {} at namespace {} is created", questionPoolSet, namespace);
		for (String lang : PLAYING_LANGUAGES) {
			for (int complexity = 1; complexity <= MAX_COMPLEXITY; complexity++) {
				for (String type : QUESTION_TYPES) {
					for (String pool : ASSIGNED_POOLS) {

						//Question blob
						final QuestionKey questionKey = new QuestionKey(pool, type, complexity, lang);
						final InternationalQuestionKey interQuestionKey = new InternationalQuestionKey(questionKey, PLAYING_LANGUAGES);
						final int questionCount = testDataLoader.getQuestions(pool, type, complexity).length;
						for (int i = 0; i < questionCount; i++) {
							final Question question = questionPoolAerospikeDao.getQuestion(questionKey, i);
							assertQuestion(lang, complexity, type, pool, question);
							
							final QuestionIndex questionIndex = questionPoolAerospikeDao.getInternationalQuestionIndex(interQuestionKey, i);
							assertNotNull(questionIndex);
							assertEquals(i, questionIndex.getIndex(lang).intValue());
							
							final Question questionByIndex = questionPoolAerospikeDao.getQuestionByInternationlIndex(questionIndex, lang);
							assertEquals("Question id does not match question from index", question.getId(), questionByIndex.getId());
						}
					}
				}
			}
		}
	}

	private void clearQuestionPoolMetaKeys(final AerospikeDao aerospikeDao) {
		for (String lang : PLAYING_LANGUAGES) {
			for (int c = 1; c <= MAX_COMPLEXITY; c++) {
				for (String type : QUESTION_TYPES) {
					for (String pool : ASSIGNED_POOLS) {
						aerospikeDao.deleteSilently(questionPoolSet, questionPoolPrimaryKeyUtil
								.createQuestionIndexMetaKey(new InternationalQuestionKey(pool, type, c, PLAYING_LANGUAGES)));
						aerospikeDao.deleteSilently(questionPoolSet, questionPoolPrimaryKeyUtil
								.createSingleQuestionMetaKey(new QuestionKey(pool, type, c, lang)));
					}
				}
			}
		}
	}
	
	private void assertQuestion(String lang, int c, String type, String pool, final Question question) {
		assertNotNull(question);
		assertEquals(lang, question.getLanguage());
		assertEquals(c, question.getComplexity());
		assertEquals(type, question.getType());
		assertEquals(pool, question.getPoolId());
		
		assertNotNull(question.getQuestionBlobKeys());
		assertEquals(1, question.getQuestionBlobKeys().size());
		
		assertNotNull(question.getDecryptionKeys());
		assertEquals(1, question.getDecryptionKeys().size());
	}
}
