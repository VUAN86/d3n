package de.ascendro.f4m.service.game.engine.integration;
import static de.ascendro.f4m.server.history.dao.CommonGameHistoryDaoImpl.HISTORY_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDaoImpl.INDEX_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDaoImpl.META_BIN_NAME;
import static de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDaoImpl.QUESTION_BIN_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapReturnType;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDaoImpl;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementPrimaryKeyUtil;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.game.GameAerospikeDaoImpl;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.server.history.dao.GameHistoryPrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.MultiplayerGameInstancePrimaryKeyUtil;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDaoImpl;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolPrimaryKeyUtil;
import de.ascendro.f4m.service.game.engine.json.GameJsonBuilder;
import de.ascendro.f4m.service.game.engine.model.ActiveGameInstance;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.game.selection.model.game.QuestionComplexitySpread;
import de.ascendro.f4m.service.game.selection.model.game.QuestionOverwriteUsage;
import de.ascendro.f4m.service.game.selection.model.game.QuestionTypeSpread;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.JsonLoader;

public class TestDataLoader extends JsonLoader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestDataLoader.class);
	
	private static String QUESTIONS_JSON_FILE_NAME_ORIGINAL = "questions.json";
	private static String QUESTIONS_JSON_FILE_NAME = QUESTIONS_JSON_FILE_NAME_ORIGINAL;

	public static final QuestionOverwriteUsage NO_REPEATS = QuestionOverwriteUsage.NO_REPEATS;
	public static final String[] QUESTION_POOLS = new String[] { "22", "23", "24", "25", "26" };
	public static final int MAX_COMPLEXITY = 4;

	public static QuestionComplexitySpread COMPLEXITY_PERCENTAGE = QuestionComplexitySpread.PERCENTAGE;
	public static QuestionTypeSpread TYPE_PERCENTAGE = QuestionTypeSpread.PERCENTAGE;
	public static int[] COMPLEXITY_STRUCTURE = new int[] { 40, 30, 20, 10 };
	public static int[] AMOUNT_OF_QUESTIONS = new int[] { 30, 70 };

	public static String[] QUESTION_TYPES = new String[] { "35", "36" };
	public static String[] ASSIGNED_POOLS = new String[] { "22", "23" };

	public static final GameWinningComponentListItem[] WINNING_COMPONENTS = new GameWinningComponentListItem[] {
			new GameWinningComponentListItem(UUID.randomUUID().toString(), false, null, null, 100),
			new GameWinningComponentListItem(UUID.randomUUID().toString(), false, null, null, 50),
			new GameWinningComponentListItem(UUID.randomUUID().toString(), false, null, null, 0),
			new GameWinningComponentListItem(UUID.randomUUID().toString(), true, BigDecimal.ONE, Currency.BONUS, 100),
			new GameWinningComponentListItem(UUID.randomUUID().toString(), true, BigDecimal.TEN, Currency.CREDIT, 50),
			new GameWinningComponentListItem(UUID.randomUUID().toString(), true, BigDecimal.TEN, Currency.MONEY, 0)
	};
	public static final String[] PAID_WINNING_COMPONENT_IDS = new String[] { UUID.randomUUID().toString(),
			UUID.randomUUID().toString(), UUID.randomUUID().toString() };
	public static final String[] VOUCHER_IDS = new String[] { UUID.randomUUID().toString(),
			UUID.randomUUID().toString(), UUID.randomUUID().toString() };
	public static final String[] ADVERTISEMENT_BLOB_KEYS = new String[] { "provider_2_advertisement_1.json",
			"provider_2_advertisement_2.json" };
	public static final long ADVERTISEMENT_PROVIDER_ID = 5;
	
	private static final Random RANDOM = new SecureRandom();

	//Game settings
	public static final String[] PLAYING_REGIONS = new String[] { "GB", "DE", "FR"};
    public static final String[] PLAYING_LANGUAGES = new String[] { "en", "de"};
    
    //Pool settings
	public static final String[] POOL_LANGUAGES = new String[] { "en", "de", "fr", "ro", "it" };
	
	public static final String GAME_ID = "47847847847";
	public static final String MGI_ID = "60551c1e-a718-11e6-555-76304dec7eb7";
	
	private final String questionPoolSet;
	private final String profileSet;
	private final String gameHistorySet;
	private final String namespace;

	private final AerospikeDao aerospikeDao;
	private final Config config;
	private final QuestionPoolPrimaryKeyUtil questionPoolPrimaryKeyUtil;
	private final ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	private final AdvertisementPrimaryKeyUtil advertisementPrimaryKeyUtil;
	private final GamePrimaryKeyUtil gamePrimaryKeyUtil;
	private final GameEnginePrimaryKeyUtil gameEnginePrimaryKeyUtil;
	private GameHistoryPrimaryKeyUtil gameHistoryPrimaryKeyUtil;
	
	private final JsonUtil jsonUtil = new JsonUtil();

	private final IAerospikeClient aerospikeClient;

	public TestDataLoader(AerospikeDao aerospikeDao, IAerospikeClient aerospikeClient, Config config) {
		super(new DefaultServiceStartupTest());
	
		this.aerospikeDao = aerospikeDao;
		this.aerospikeClient = aerospikeClient;

		this.config = config;
		this.questionPoolPrimaryKeyUtil = new QuestionPoolPrimaryKeyUtil(config);
		this.profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
		this.advertisementPrimaryKeyUtil = new AdvertisementPrimaryKeyUtil(config);
		this.gamePrimaryKeyUtil = new GamePrimaryKeyUtil(config);
		this.gameHistoryPrimaryKeyUtil = new GameHistoryPrimaryKeyUtil(config);
		this.gameEnginePrimaryKeyUtil = new GameEnginePrimaryKeyUtil(config); 

		this.questionPoolSet = config.getProperty(GameEngineConfig.AEROSPIKE_QUESTION_POOL_SET);
		this.profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		this.gameHistorySet = config.getProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET);
		this.namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
	}

	/**
	 * Generate question pools and their indexes using default pool and playing languages
	 * @param poolIds -  question pool ids
	 * @throws IOException
	 */
	public void prepareTestGameQuestionPools(String[] poolIds, boolean generateInternationalIndex) throws IOException {
		prepareTestGameQuestionPools(poolIds, POOL_LANGUAGES, PLAYING_LANGUAGES, generateInternationalIndex);
	}

	public void prepareTestGameQuestionPools(String[] poolIds, String[] poolLanguages, String[] playingLanguages, boolean generateQuestionIndex) throws IOException {
		for (String poolId : poolIds) {
			prepareTestGameQuestionPool(poolId, poolLanguages, playingLanguages, generateQuestionIndex);
		}
	}

	public void prepareTestGameQuestionPool(String poolId, String[] poolLanguages, String[] playingLanguages, boolean generateInternationalIndex) throws IOException {
		for (String questionType : QUESTION_TYPES) {
			for (int complexity = 1; complexity <= MAX_COMPLEXITY; complexity++) {
				prepareTestGameQuestionPool(new InternationalQuestionKey(poolId, questionType, complexity, playingLanguages), 
						poolLanguages, generateInternationalIndex);
			}
		}
	}

	private void prepareTestGameQuestionPool(InternationalQuestionKey interQuestionKey, String[] poolLanguages, boolean generateInternationalIndex)
			throws IOException {
		final String questionPoolSet = config.getProperty(GameEngineConfig.AEROSPIKE_QUESTION_POOL_SET);
		final String poolIndexKey = questionPoolPrimaryKeyUtil.createQuestionIndexMetaKey(interQuestionKey);
		
		final String[] questions = getQuestions(interQuestionKey.getPoolId(), interQuestionKey.getType(), interQuestionKey.getComplexity());
		for (int i = 0; i < questions.length; i++) {
			final Question question = jsonUtil.fromJson(questions[i], Question.class);
			
			if (generateInternationalIndex) {
				// Question index
				final QuestionIndex questionIndex = buildQuestionIndex(interQuestionKey, i);
				prepareQuestionIndex(interQuestionKey, questionIndex, i);
				incrementPoolSize(poolIndexKey);
				question.setMultiIndex(questionIndex);
			}
			
			//Prepare questions for each question index language
			final int j = i;
			Arrays.stream(poolLanguages)
				.map(lang -> new QuestionKey(interQuestionKey, lang))
				.forEach(questionKey -> prepareSingleQuestion(questionKey, j, question));
		}
		
		//Assert question meta size
		Arrays.stream(poolLanguages)
			.map(lang -> new QuestionKey(interQuestionKey, lang))
			.map(questionKey -> questionPoolPrimaryKeyUtil.createSingleQuestionMetaKey(questionKey))
			.map(questionMetaKey -> aerospikeDao.readLong(questionPoolSet, questionMetaKey, META_BIN_NAME))
			.forEach(meta -> assertThat(meta, equalTo(Long.valueOf(questions.length))));
		if (generateInternationalIndex) {
			//Assert question index meta size
			final Long poolIndexSize = aerospikeDao.readLong(questionPoolSet, poolIndexKey, META_BIN_NAME);
			assertEquals(Long.valueOf(questions.length), poolIndexSize);
		}
	}

	private QuestionIndex buildQuestionIndex(InternationalQuestionKey interQuestionKey, final long questionIndex) {
		final QuestionIndex interIndex = new QuestionIndex();
		interIndex.setPoolId(interQuestionKey.getPoolId());
		interIndex.setType(interQuestionKey.getType());
		interIndex.setComplexity(interQuestionKey.getComplexity());

		final Map<String, Long> index = Arrays.stream(interQuestionKey.getPlayingLanguages())
			.collect(Collectors.toMap(lang -> lang, any -> questionIndex));			
		interIndex.setIndex(index);
		
		return interIndex;
	}
	
	private void prepareQuestionIndex(final InternationalQuestionKey interQuestionKey,
			final QuestionIndex questionIndex, int i) {
		final String questionPoolSet = config.getProperty(GameEngineConfig.AEROSPIKE_QUESTION_POOL_SET);
		final String questionIndexKey = questionPoolPrimaryKeyUtil.createQuestionIndexKey(interQuestionKey, i);
		aerospikeDao.createOrUpdateJson(questionPoolSet, questionIndexKey, INDEX_BIN_NAME,
				(v, wp) -> jsonUtil.toJson(questionIndex));
	}
	
	private void prepareSingleQuestion(QuestionKey questionKey, int index, Question question) {
		question.setLanguage(questionKey.getLanguage());
		final String singleQuestionKey = questionPoolPrimaryKeyUtil.createSingleQuestionKey(questionKey, index);
		aerospikeDao.createOrUpdateJson(questionPoolSet, singleQuestionKey, QUESTION_BIN_NAME,
				(v, wp) -> jsonUtil.toJson(question));

		final String poolSizeMetaKey = questionPoolPrimaryKeyUtil.createSingleQuestionMetaKey(questionKey);
		incrementPoolSize(poolSizeMetaKey);
	}

	protected void incrementPoolSize(String metaRecordKey) {
		final Key recordKey = new Key(namespace, questionPoolSet, metaRecordKey);
		final Record record = aerospikeClient.get(null, recordKey);
		long count;
		if (record != null) {
			count = record.getLong(META_BIN_NAME);
		} else {
			count = 0;
		}
		aerospikeClient.put(null, recordKey, new Bin(META_BIN_NAME, ++count));
	}

	public String[] getQuestions(String poolId, String type, int complexity) throws IOException {
		return getPlainTextJsonFromResources(QUESTIONS_JSON_FILE_NAME)
			.replaceAll("<<poolId>>", poolId)
			.replaceAll("<<type>>", type)
			.replaceAll("<<complexity>>", Integer.toString(complexity))
			.split("\n");
	}

	public void prepareTestGameData(GameJsonBuilder gameJsonBuilder) throws IOException {
		final String gameSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);
		final String gameKey = gamePrimaryKeyUtil.createPrimaryKey(gameJsonBuilder.getGameId());

		final String gameJson = gameJsonBuilder.buildJson(jsonUtil);
		
		aerospikeDao.createOrUpdateJson(gameSet, gameKey, GameAerospikeDaoImpl.BLOB_BIN_NAME, (v, wp) -> gameJson);
	}
	
	public String getReadyToPlayJson(String gameInstanceId, ClientInfo clientInfo) throws IOException {
		return getPlainTextJsonFromResources("readyToPlay.json", clientInfo)
			.replace("<<gameInstanceId>>", gameInstanceId);
	}

	public String getJokersAvailableJson(String gameInstanceId, ClientInfo clientInfo) throws IOException {
		return getPlainTextJsonFromResources("jokersAvailable.json", clientInfo)
				.replace("<<gameInstanceId>>", gameInstanceId);
	}

	public String getPurchaseHintJson(String gameInstanceId, ClientInfo clientInfo) throws IOException {
		return getPlainTextJsonFromResources("purchaseHint.json", clientInfo)
				.replace("<<gameInstanceId>>", gameInstanceId);
	}

	public String getPurchaseFiftyFiftyJson(String gameInstanceId, ClientInfo clientInfo) throws IOException {
		return getPlainTextJsonFromResources("purchaseFiftyFifty.json", clientInfo)
				.replace("<<gameInstanceId>>", gameInstanceId);
	}

	public String getPurchaseSkipJson(String gameInstanceId, ClientInfo clientInfo) throws IOException {
		return getPlainTextJsonFromResources("purchaseSkip.json", clientInfo)
				.replace("<<gameInstanceId>>", gameInstanceId);
	}

	public String getAnswerQuestionJson(ClientInfo clientInfo, String gameInstanceId, int question, int step,
			String[] answers, long tClientMs) throws IOException {
		final String answersString = Arrays.stream(answers)
				.map(Object::toString)
				.collect(Collectors.joining("\",\"", "\"", "\""));

		return getPlainTextJsonFromResources("answerQuestion.json", clientInfo)
			.replace("<<gameInstanceId>>", gameInstanceId)
			.replace("\"<<question>>\"", String.valueOf(question))
			.replace("\"<<step>>\"", String.valueOf(step))
			.replace("\"<<answers>>\"", answersString)	
			.replace("\"<<tClientMs>>\"", String.valueOf(tClientMs));
	}

	public String getNextStepJson(ClientInfo clientInfo, String gameInstanceId, int question, int step, long tClientMs)
			throws IOException {
		return getPlainTextJsonFromResources("nextStep.json", clientInfo)
			.replace("<<gameInstanceId>>", gameInstanceId)
			.replace("\"<<tClientMs>>\"", String.valueOf(tClientMs))
			.replace("\"<<question>>\"", String.valueOf(question))
			.replace("\"<<step>>\"", String.valueOf(step));
	}

	public void clearAerospikeData() {
		aerospikeClient.close();
	}

	public long selectRandomClientMsT(Question question, int step) {
		assertThat(step, lessThan(question.getStepCount()));
		final long maxAnswerTime = question.getAnswerMaxTime(step);
		return (long) RANDOM.nextInt((int) maxAnswerTime) + 1;//1..maxAnswerTime+1
	}

	public String[] selectRandomAnswers(Question question) {
		final String[] correctAnswers = question.getCorrectAnswers();
		final String[] availableAnswers = question.getAnswers();

		final String[] selectedAnswers = new String[correctAnswers.length];

		IntStream.range(0, correctAnswers.length)
				.forEach(i -> selectedAnswers[i] = availableAnswers[RANDOM.nextInt(availableAnswers.length)]);

		return selectedAnswers;
	}

	public static void setAssignedPool(String[] assignedPools) {
		TestDataLoader.ASSIGNED_POOLS = assignedPools;
	}

	public static void setQuestionTypes(String[] questionTypes) {
		TestDataLoader.QUESTION_TYPES = questionTypes;
	}

	public static void setQuestionsJsonFileName(String questionsJsonFileName) {
		TestDataLoader.QUESTIONS_JSON_FILE_NAME = questionsJsonFileName;
	}
	
	public static void resetQuestionsJsonFileName() {
		TestDataLoader.QUESTIONS_JSON_FILE_NAME = QUESTIONS_JSON_FILE_NAME_ORIGINAL;
	}

	public static void setComplexitySpread(QuestionComplexitySpread questionComplexitySpread) {
		TestDataLoader.COMPLEXITY_PERCENTAGE = questionComplexitySpread;
	}

	public static void setTypeSpread(QuestionTypeSpread questionTypeSpread) {
		TestDataLoader.TYPE_PERCENTAGE = questionTypeSpread;
	}

	public static void setComplexityStructure(int[] complexityStucture) {
		TestDataLoader.COMPLEXITY_STRUCTURE = complexityStucture;
	}

	public static void setAmountOfQuestions(int[] amountOfQuestionsSpread) {
		TestDataLoader.AMOUNT_OF_QUESTIONS = amountOfQuestionsSpread;
	}

	public void createProfile(String userId, Double userHandicap) {
		createProfile(userId, userHandicap, null);
	}
	
	public void createProfile(String userId, Double userHandicap, Integer age) {
		final Profile profile = new Profile();
		profile.setUserId(userId);
		profile.setHandicap(userHandicap);
		if (age != null) {
			ProfileUser person = new ProfileUser();
			person.setBirthDate(getTodayStarts().minusYears(age));
			profile.setPersonWrapper(person);
		}

		aerospikeDao.createJson(profileSet, profilePrimaryKeyUtil
				.createPrimaryKey(userId), CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}
	
	public static ZonedDateTime getTodayStarts() {
		return ZonedDateTime.of(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate(), LocalTime.MIN, ZoneOffset.UTC);
	}
	
	public static ZonedDateTime getTodayEnds() {
		return ZonedDateTime.of(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate(), LocalTime.MAX, ZoneOffset.UTC);
	}
	
	public void createAdvertisementProvider(long providerId, int advertisementCount){
		final String advertisementSet = config.getProperty(GameEngineConfig.AEROSPIKE_ADVERTISEMENT_SET);
		final Key key = new Key(namespace, advertisementSet, advertisementPrimaryKeyUtil.createPrimaryKey(providerId));
		final Bin bin = new Bin(AdvertisementDaoImpl.COUNTER_BIN_NAME, advertisementCount);
		aerospikeClient.put(null, key, bin);
	}
	
	public List<Record> getTransactionLogs(){
		final String transactionLogSet = config.getProperty(AerospikeConfigImpl.TRANSACTION_LOG_SET);
		final String namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
		
		final String[] binNames = Arrays.stream(TransactionLog.class.getDeclaredFields())
			.map(f -> readTransactionLogFieldStringValue(f.getName()))
			.toArray(size -> new String[size]);

		final List<Record> transactions = new CopyOnWriteArrayList<>();
		aerospikeClient.scanAll(null, namespace, transactionLogSet, new ScanCallback() {
			
			@Override
			public void scanCallback(Key key, Record record) throws AerospikeException {
				if(key.namespace.equalsIgnoreCase(namespace) && key.setName.equalsIgnoreCase(transactionLogSet)){
					transactions.add(record);				
				}
			}
		}, binNames);
		return transactions;
	}
	
	public void clearTransactionLog(){
		final String transactionLogSet = config.getProperty(AerospikeConfigImpl.TRANSACTION_LOG_SET);
		final String namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
		
		aerospikeClient.scanAll(null, namespace, transactionLogSet, new ScanCallback() {
			
			@Override
			public void scanCallback(Key key, Record record) throws AerospikeException {
				if (key.namespace.equals(namespace) && key.setName.equals(transactionLogSet)) {
					aerospikeClient.delete(null, key);
				}
			}
		}, TransactionLog.PROPERTY_ID);
	}
	
	private String readTransactionLogFieldStringValue(String fieldName){
		String value = null;
		try{
			value = (String) FieldUtils.readStaticField(TransactionLog.class, fieldName);
		}catch(Exception ex){
			LOGGER.error("Failed to read static field {} from TransactionLog class", fieldName);
		}
		return value;
	}

	public String getGameCancellationJson(String gameInstanceId, ClientInfo clientInfo) throws IOException {
		return getPlainTextJsonFromResources("cancelGame.json", clientInfo)
			.replace("<<gameInstanceId>>", gameInstanceId);
	}
	
	public GameHistory getGameHistory(String userId, String gameInstanceId) {
		final String createPrimaryKey = gameHistoryPrimaryKeyUtil.createPrimaryKey(userId);
		
		final Operation historyGetOperation = MapOperation.getByKey(HISTORY_BIN_NAME, Value.get(gameInstanceId), MapReturnType.VALUE);
		final Record historyGetResultRecord = aerospikeClient.operate(null, new Key(namespace, gameHistorySet, createPrimaryKey), historyGetOperation);

		final GameHistory gameHistory;
		if(historyGetResultRecord.bins.containsKey(HISTORY_BIN_NAME)){
			final String historyAsString = historyGetResultRecord.getString(HISTORY_BIN_NAME);
			gameHistory = new GameHistory(jsonUtil.fromJson(historyAsString, JsonObject.class));
		}else{
			gameHistory = null;
		}
		return gameHistory;
	}
	
	/**
	 * Create multiplayer game instance by specific id and add additional to be added
	 * @param aerospikeClientProvider - Aerospike target
	 * @param mgiId - mgi to used for create
	 * @param userId - creator id
	 * @param customGameConfigBuilder - custom game config to be created
	 * @param additionalInvitations - optional list of additional invitations
	 */
	public void inviteFriends(AerospikeClientProvider aerospikeClientProvider, String mgiId, String userId,
			CustomGameConfigBuilder customGameConfigBuilder, List<Pair<String, String>> additionalInvitations) {
		final MultiplayerGameInstancePrimaryKeyUtil multiplayerGameInstancePrimaryKeyUtil = new MultiplayerGameInstancePrimaryKeyUtil(config){
			@Override
			public String generateId() {
				return mgiId;
			}
		};
		final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao = new CommonMultiplayerGameInstanceDaoImpl(
				config, multiplayerGameInstancePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		commonMultiplayerGameInstanceDao.create(userId, customGameConfigBuilder.build(mgiId));
		
		if(!CollectionUtils.isEmpty(additionalInvitations)){
			additionalInvitations
				.forEach(p -> commonMultiplayerGameInstanceDao.addUser(mgiId, p.getKey(), p.getValue(), MultiplayerGameInstanceState.INVITED));
		}
		Assert.assertNotNull(commonMultiplayerGameInstanceDao.getConfig(mgiId));
	}
	
	public void joinTournament(AerospikeClientProvider aerospikeClientProvider, String mgiId, String userId) {
		final MultiplayerGameInstancePrimaryKeyUtil multiplayerGameInstancePrimaryKeyUtil = new MultiplayerGameInstancePrimaryKeyUtil(config);
		final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao = new CommonMultiplayerGameInstanceDaoImpl(
				config, multiplayerGameInstancePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		commonMultiplayerGameInstanceDao.addUser(mgiId, userId, userId, MultiplayerGameInstanceState.INVITED);
	}

	public void createLiveTournament(GameJsonBuilder gameJsonBuilder, CustomGameConfigBuilder customGameConfigBuilder, AerospikeClientProvider aerospikeClientProvider) throws IOException {
		prepareTestGameData(gameJsonBuilder);
		inviteFriends(aerospikeClientProvider, gameJsonBuilder.getMultiplayerGameInstanceId(), null, customGameConfigBuilder, null);
	}
	
	public EntryFee createEntryFee(BigDecimal entryFeeAmount, Currency entryFeeCurrency) throws IOException {
		final GameJsonBuilder entryFee = GameJsonBuilder.createGame("anyId").withGameType(GameType.DUEL);//any game config
		if (entryFeeAmount != null && entryFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
			entryFee.withEntryFee(entryFeeAmount, entryFeeCurrency);
		}
		return entryFee.buildGame(jsonUtil);
	}
	
	public ActiveGameInstance readActiveGameInstance(String gameInstanceId){
		final String namespace = config.getProperty(GameEngineConfig.AEROSPIKE_ACTIVE_GAME_INSTANCE_NAMESPACE);
		final String set = config.getProperty(GameEngineConfig.AEROSPIKE_ACTIVE_GAME_INSTANCE_SET);
		final String key = gameEnginePrimaryKeyUtil.createPrimaryKey(gameInstanceId);
		final Record record = aerospikeClient.get(null, new Key(namespace, set, key));
		return ActiveGameInstanceDaoImpl.fromRecord(record);
	}
	
	public void removeAllQuestions(){
		RealAerospikeTestBase.clearSet(aerospikeClient, namespace, questionPoolSet);
	}
}
