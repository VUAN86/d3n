package de.ascendro.f4m.service.analytics;

import static de.ascendro.f4m.service.analytics.data.EventTestData.PLAYER_GAMES;
import static de.ascendro.f4m.service.analytics.data.EventTestData.TENANT_ID;
import static de.ascendro.f4m.service.analytics.data.EventTestData.TOTAL_BONUS_POINTS_EARNED;
import static de.ascendro.f4m.service.analytics.data.EventTestData.TOTAL_CREDITS_EARNED;
import static de.ascendro.f4m.service.analytics.data.EventTestData.TOTAL_MONEY_EARNED;
import static de.ascendro.f4m.service.analytics.data.EventTestData.USER_FULL_REGISTRATION_BONUS;
import static de.ascendro.f4m.service.analytics.data.EventTestData.USER_FULL_REGISTRATION_CREDITS;
import static de.ascendro.f4m.service.analytics.data.EventTestData.USER_ID;
import static de.ascendro.f4m.service.analytics.data.EventTestData.USER_REGISTRATION_BONUS;
import static de.ascendro.f4m.service.analytics.data.EventTestData.USER_REGISTRATION_CREDITS;
import static de.ascendro.f4m.service.analytics.data.GameTestData.TOURNAMENT_INSTANCE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Module;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDao;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDaoImpl;
import de.ascendro.f4m.server.achievement.dao.UserAchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.UserAchievement;
import de.ascendro.f4m.server.achievement.model.UserAchievementStatus;
import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.server.achievement.model.UserProgressES;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.analytics.EventRecord;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.tracker.TrackerBuilders;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.data.EventTestData;
import de.ascendro.f4m.service.analytics.di.AchievementModule;
import de.ascendro.f4m.service.analytics.di.AnalyticsServiceModule;
import de.ascendro.f4m.service.analytics.di.JobModule;
import de.ascendro.f4m.service.analytics.di.StatisticModule;
import de.ascendro.f4m.service.analytics.module.achievement.AchievementProcessor;
import de.ascendro.f4m.service.analytics.module.achievement.processor.EventProcessor;
import de.ascendro.f4m.service.analytics.notification.di.NotificationWebSocketModule;
import de.ascendro.f4m.service.analytics.util.AnalyticServiceUtil;
import de.ascendro.f4m.service.analytics.util.NetworkUtil;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileStats;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class JobModuleTest extends BaseServiceTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticModuleTest.class);
	public static final String BADGE_ID = "1";
	public static final String ACHIEVEMENT_ID = "ACHI-1";

	private static DB db;
	private DBConfigurationBuilder configBuilder;
	private List<PayoutPrize> payoutPrizes;
	private Map<String, SendWebsocketMessageRequest> pushContentToCheck;

	private int paymentMessageCount;
	private final CountDownLatch lock = new CountDownLatch(1);
	private BadgeAerospikeDao badgeAerospikeDao;
	private UserBadgeAerospikeDao userBadgeAerospikeDao;
	private AchievementAerospikeDao achievementAerospikeDao;
	private CommonProfileAerospikeDao commonProfileAerospikeDao;
	private CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao;
	private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;
	private TopUsersElasticDao topUsersElasticDao;

    private AnalyticServiceUtil analyticServiceUtil;
	private UserAchievementAerospikeDao userAchievementAerospikeDao;

	private static final String BADGE_SET_BLOB_BIN_NAME = "badge";

	private static final String TENANTS_SET_BLOB_BIN_NAME = "tenantIdSet";

	private static final String ACHIEVEMENT_SET_BLOB_BIN_NAME = "achievement";


	private static final int ELASTIC_PORT = 9200;

	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);

	private ElasticClient client;

	@Mock
	private EventProcessor eventProcessor;

	private class PayoutPrize {
		private Currency currency;
		private BigDecimal amount;
		private String userId;

		private PayoutPrize(Currency currency, BigDecimal amount, String userId) {
			this.currency = currency;
			this.amount = amount;
			this.userId = userId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			PayoutPrize that = (PayoutPrize) o;
			return currency == that.currency && Objects.equals(amount, that.amount)
					&& Objects.equals(userId, that.userId);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((amount == null) ? 0 : amount.hashCode());
			result = prime * result + ((currency == null) ? 0 : currency.hashCode());
			result = prime * result + ((userId == null) ? 0 : userId.hashCode());
			return result;
		}

	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);

		assertServiceStartup(PaymentMessageTypes.SERVICE_NAME);

		badgeAerospikeDao = injector.getInstance(BadgeAerospikeDaoImpl.class);
		userBadgeAerospikeDao = injector.getInstance(UserBadgeAerospikeDaoImpl.class);
		achievementAerospikeDao = injector.getInstance(AchievementAerospikeDaoImpl.class);
		userAchievementAerospikeDao = injector.getInstance(UserAchievementAerospikeDao.class);

		commonProfileAerospikeDao = injector.getInstance(CommonProfileAerospikeDao.class);
		commonGameInstanceAerospikeDao = injector.getInstance(CommonGameInstanceAerospikeDao.class);
		applicationConfigurationAerospikeDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);

		AchievementConfig config = new AchievementConfig();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		ElasticUtil elasticUtil = new ElasticUtil();
		client = new ElasticClient(config, elasticUtil, new ServiceMonitoringRegister());

		topUsersElasticDao = new TopUsersElasticDaoImpl(client, config, new JsonUtil());

		profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);

		payoutPrizes = new ArrayList<>();
		pushContentToCheck = new HashMap<>();
		paymentMessageCount = 0;

		analyticServiceUtil = new AnalyticServiceUtil(commonProfileAerospikeDao, commonGameInstanceAerospikeDao, applicationConfigurationAerospikeDao, topUsersElasticDao);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();

		analyticsServiceStartup.stopScan();
		analyticsServiceStartup.stopActiveMQSubscribers();
		analyticsServiceStartup.stopActiveMQ();
		if (db != null) {
			LOGGER.info("Stopping database");
			db.stop();
			db = null;
			configBuilder = null;
		}
		client.close();
	}

	@Override
	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		PaymentMessageTypes paymentType = originalMessageDecoded.getType(PaymentMessageTypes.class);
		if (PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS == paymentType) {
			return onPaymentTransferReceived(originalMessageDecoded);
		} else if (PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE == paymentType) {
			return onLoadOrWithdrawWithoutCoveragerReceived(originalMessageDecoded);
		} else if (UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE == originalMessageDecoded
				.getType(UserMessageMessageTypes.class)) {
			return onSendPushReceived(originalMessageDecoded);
		} else {
			throw new UnexpectedTestException("Unexpected message");
		}
	}

	private JsonMessageContent onSendPushReceived(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		verifyPush((SendWebsocketMessageRequest) originalMessageDecoded.getContent());
		return new SendUserPushResponse();
	}

	private void verifyPush(SendWebsocketMessageRequest request) {
		SendWebsocketMessageRequest checkMessage = pushContentToCheck.get(request.getMessage());
		if (checkMessage != null) {
			assertEquals(checkMessage.getUserId(), request.getUserId());
			assertEquals(checkMessage.getMessage(), request.getMessage());
			assertTrue(checkMessage.isLanguageAuto());
			if (checkMessage.getParameters() != null) {
				IntStream.range(0, checkMessage.getParameters().length).forEach(
						(index) -> assertEquals(checkMessage.getParameters()[index], request.getParameters()[index]));
			} else {
				assertNull(request.getParameters());
			}

			lock.countDown();
		}
	}

	private JsonMessageContent onLoadOrWithdrawWithoutCoveragerReceived(
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		LoadOrWithdrawWithoutCoverageRequest request = (LoadOrWithdrawWithoutCoverageRequest) originalMessageDecoded
				.getContent();
		if (StringUtils.isNotBlank(request.getToProfileId())) {
			addPrizeToExpected(request.getCurrency(), request.getAmount(), request.getToProfileId());
			paymentMessageCount++;
		}

		return new TransactionId("123");
	}

	private JsonMessageContent onPaymentTransferReceived(
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		TransferBetweenAccountsRequest request = (TransferBetweenAccountsRequest) originalMessageDecoded.getContent();
		if (StringUtils.isNotBlank(request.getToProfileId())) {
			addPrizeToExpected(request.getCurrency(), request.getAmount(), request.getToProfileId());
			paymentMessageCount++;
		}

		return new TransactionId("123");
	}

	private boolean verifyPrizes() {
		return payoutPrizes.contains(new PayoutPrize(Currency.BONUS, new BigDecimal(10), USER_ID))
				&& paymentMessageCount == payoutPrizes.size();
	}

	private void addPrizeToExpected(Currency currency, BigDecimal amount, String userId) {
		payoutPrizes.add(new PayoutPrize(currency, amount, userId));
	}

	@Override
	protected AnalyticsServiceStartupUsingAerospikeMock getAnalyticsServiceStartupUsingAerospikeMock() {
		return new AnalyticsServiceStartupUsingAerospikeMock(F4MIntegrationTestBase.DEFAULT_TEST_STAGE) {
			@Override
			protected Iterable<? extends Module> getTestedModules() {
				return Arrays.asList(new AnalyticsServiceModule(), new JobModule(), new StatisticModule(),
						new NotificationWebSocketModule(), new AchievementModule());
			}

			@Override
			protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
				return Collections.emptyList();
			}
		};
	}

	@Override
	protected AnalyticsServiceStartupUsingAerospikeReal getAnalyticsServiceStartupUsingAerospikeReal() {
		return new AnalyticsServiceStartupUsingAerospikeReal(F4MIntegrationTestBase.DEFAULT_TEST_STAGE) {
			@Override
			protected Iterable<? extends Module> getTestedModules() {
				return Arrays.asList(new AnalyticsServiceModule(), new JobModule(), new StatisticModule(),
						new NotificationWebSocketModule(), new AchievementModule());
			}

			@Override
			protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
				return Collections.emptyList();
			}
		};
	}

	@Test
	public void testStart() throws Exception {
		//init analytics DAO
		AnalyticsDaoImpl analyticsDaoImpl = injector.getInstance(AnalyticsDaoImpl.class);

		//Init app config
		ApplicationConfigurationAerospikeDao appConfigDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);
		when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(AppConfigBuilder.buildDefaultAppConfigWithRegitrationBounus(BigDecimal.valueOf(USER_FULL_REGISTRATION_CREDITS),
				BigDecimal.valueOf(USER_FULL_REGISTRATION_BONUS),
				BigDecimal.valueOf(USER_REGISTRATION_CREDITS),
				BigDecimal.valueOf(USER_REGISTRATION_BONUS)));

		//Create user profile
		Profile profile = createProfile(analyticsDaoImpl, USER_ID);

		//init event test data
		EventTestData eventTestData = new EventTestData(analyticsDaoImpl);
		eventTestData.createInitialData();
		eventTestData.createMultiplayerEvent(profile, TOURNAMENT_INSTANCE_ID);

		final int timeout = 2 * RetriedAssert.DEFAULT_TIMEOUT_MS;

		RetriedAssert.assertWithWait(() -> assertTrue(NetworkUtil.portAvailable(AnalyticsTestConfig.MARIADB_PORT)), timeout);

		URL url = this.getClass().getClassLoader().getResource("mysql/db");

		assertNotNull(url);

		String dbFolder = url.getFile();
		FileUtils.deleteQuietly(new File(dbFolder));

		configBuilder = DBConfigurationBuilder.newBuilder();
		configBuilder.setPort(AnalyticsTestConfig.MARIADB_PORT);
		configBuilder.setDataDir(dbFolder);
		db = DB.newEmbeddedDB(configBuilder.build());
		LOGGER.info("Starting database");
		db.start();

		db.source("mysql/schema.sql");

		pushContentToCheck.put(Messages.MONTHLY_INVITES_PRIZE_PAYOUT_PUSH,
				getPushRequestWrapper(Messages.MONTHLY_INVITES_PRIZE_PAYOUT_PUSH, String.valueOf(USER_ID),
						new String[] { config.getProperty(AnalyticsTestConfig.MONTHLY_BONUS_NUMBER_OF_FRIENDS),
								new BigDecimal(config.getProperty(AnalyticsTestConfig.MONTHLY_BONUS_VALUE)).setScale(2)
										.toPlainString(),
								Currency.BONUS.getFullName() }));

		analyticsServiceStartup.startActiveMQ(false);
		analyticsServiceStartup.startScan();

		RetriedAssert.assertWithWait(() -> assertTrue(profileStatsUpdated()), 10000, 200);

		RetriedAssert.assertWithWait(() -> verifyPrizes(), 4000, 200);

		RetriedAssert.assertWithWait(() -> assertEquals(lock.getCount(), 0), 10000, 200);
	}

	private boolean profileStatsUpdated() {
		CommonProfileAerospikeDao commonProfileAerospikeDao = injector.getInstance(CommonProfileAerospikeDao.class);
		ProfileStats profileStats = commonProfileAerospikeDao.getStatsFromBlob(USER_ID, TENANT_ID);

		return profileStats != null && profileStats.getTotalGames() == PLAYER_GAMES
				&& profileStats.getPointsWon() == TOTAL_BONUS_POINTS_EARNED +  new Integer(config.getProperty(AnalyticsConfig.MONTHLY_BONUS_VALUE))
				&& profileStats.getCreditWon() == TOTAL_CREDITS_EARNED
				&& profileStats.getMoneyWon() == TOTAL_MONEY_EARNED && profileStats.getGamesWon() == 1;
	}

	public Profile createProfile(AnalyticsDaoImpl analyticsDaoImpl, String userId) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		ProfileAddress address = new ProfileAddress();
		address.setCity("City");
		address.setCountry("DE");
		profile.setAddress(address);
		ProfileUser person = new ProfileUser();
		person.setFirstName("Firstname");
		person.setLastName("Lastname");
		profile.setPersonWrapper(person);
		analyticsDaoImpl.createJson(profileSet, profilePrimaryKeyUtil.createPrimaryKey(userId),
				CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
		return profile;
	}

	@Test
	public void testAchievementProcessor() throws Exception {

		PlayerGameEndEvent gameEndEvent = new PlayerGameEndEvent();
		gameEndEvent.setGameId(1L);
		gameEndEvent.setPlayedWithFriend(true);
		gameEndEvent.setTotalQuestions(10);
		gameEndEvent.setAverageAnswerSpeed(50L);
		gameEndEvent.setPlayerHandicap(77D);
		gameEndEvent.setGameType(GameType.DUEL);

		EventRecord gameEventRecord = new TrackerBuilders.EventBuilder()
				.setAnalyticsContent(new TrackerBuilders.ContentBuilder().setUserId(USER_ID).setTenantId(TENANT_ID)
						.setApplicationId("1").setSessionIP("0.0.0.0").setEventData(gameEndEvent))
				.build();

		// prepare some mock data :
		updateTestData("tenantIdsList.json",
				config.getProperty(AchievementConfig.AEROSPIKE_TENANTS_WITH_ACHIEVEMENTS_SET),
				TENANTS_SET_BLOB_BIN_NAME);
		updateTestData("badgeData.json", config.getProperty(AchievementConfig.AEROSPIKE_BADGE_SET),
				BADGE_SET_BLOB_BIN_NAME);
		updateTestData("achievementData.json", config.getProperty(AchievementConfig.AEROSPIKE_ACHIEVEMENT_SET),
				ACHIEVEMENT_SET_BLOB_BIN_NAME);

		final String sourceJsonString = getProfileJson(USER_ID, "anyName", "anySurname");
		final JsonElement jsonElement = JsonTestUtil.getGson().fromJson(sourceJsonString, JsonElement.class);

		final Profile profile = new Profile(jsonElement);

		commonProfileAerospikeDao.createJson("profile", "profile:" + USER_ID, "value", profile.getAsString());

		List<Badge> badges = new ArrayList<>();
		badges.add(badgeAerospikeDao.getBadgeById(BADGE_ID));
		when(eventProcessor.getAssociatedBadges(any())).thenReturn(badges);

		List<Achievement> achievements = new ArrayList<>();
		achievements.add(achievementAerospikeDao.getAchievementById(ACHIEVEMENT_ID));
		when(eventProcessor.getAssociatedAchievements(TENANT_ID, badges.get(0))).thenReturn(achievements);

		AchievementProcessor processor = new AchievementProcessor(eventProcessor, userBadgeAerospikeDao,
				userAchievementAerospikeDao, analyticServiceUtil);
		processor.handleEvent(gameEventRecord.getAnalyticsContent());
		checkUserBadgeHasBeenIncremented();
		// call this another 4 times, and check that badge instance is created
		for (int i = 0; i < 4; i++) {
			processor.handleEvent(gameEventRecord.getAnalyticsContent());
		}

		checkUserBadgeHasBeenWon();
		checkUserAchievementHasBeenWon();
		checkUserBadgeInElasticSearch();
	}

	private void checkUserBadgeInElasticSearch() {
		List<UserProgressES> listResult = topUsersElasticDao.getTopUsers(TENANT_ID, BadgeType.GAME, 0, 10);
		assertEquals(1,listResult.size());
		assertEquals(1, listResult.get(0).getNumOfCommunityBadges().intValue());
		assertEquals(0, listResult.get(0).getNumOfGameBadges().intValue());
	}

	private void checkUserBadgeHasBeenIncremented() {
		ListResult<UserBadge> userBadgeListResult = userBadgeAerospikeDao.getUserBadgeList(TENANT_ID, USER_ID, 0, 10,
				BadgeType.COMMUNITY);

		assertEquals(1, userBadgeListResult.getSize());
		assertEquals(1, userBadgeListResult.getItems().get(0).getRules().get(0).getProgress().intValue());
		assertEquals(1, userBadgeListResult.getItems().get(0).getRules().get(1).getProgress().intValue());
	}

	private void checkUserAchievementHasBeenWon() {
		UserAchievement userAchievement = userAchievementAerospikeDao.getUserAchievementByAchievementId(USER_ID,
				ACHIEVEMENT_ID);
		assertNotNull(userAchievement.getCompletedOn());

		ListResult<UserAchievement> userAchievementList = userAchievementAerospikeDao.getUserAchievementList(TENANT_ID,
				USER_ID, 0, 10, UserAchievementStatus.WON);

		assertEquals(1, userAchievementList.getItems().size());
		assertEquals(userAchievement.getCompletedOn(), userAchievementList.getItems().get(0).getCompletedOn());

	}

	private void checkUserBadgeHasBeenWon() {
		ListResult<UserBadge> userBadgeListResult = userBadgeAerospikeDao.getUserBadgeList(TENANT_ID, USER_ID, 0, 10,
				BadgeType.COMMUNITY);

		assertEquals(1, userBadgeListResult.getSize());
		assertEquals(5, userBadgeListResult.getItems().get(0).getRules().get(0).getProgress().intValue());
		assertEquals(5, userBadgeListResult.getItems().get(0).getRules().get(1).getProgress().intValue());

		// check userBadge instances
		List<UserBadge> userBadgeInstances = userBadgeAerospikeDao.getUserBadgeInstanceList(TENANT_ID, 0, 10, BADGE_ID,
				USER_ID);
		assertEquals(1, userBadgeInstances.size());
		assertEquals(BADGE_ID, userBadgeInstances.get(0).getBadgeId());
		assertEquals(USER_ID, userBadgeInstances.get(0).getCreatorId());

	}

	private void updateTestData(String fileName, String set, String binName) throws IOException {
		String jsonDataSet = getPlainTextJsonFromResources(fileName);

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = (JsonObject) parser.parse(jsonDataSet);

		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String binValue = entry.getValue().toString();
			achievementAerospikeDao.createJson(set, key, binName, binValue);
		}
	}

	private String getProfileJson(String userId, String name, String surname) throws IOException {
		final String profileJson = jsonLoader.getPlainTextJsonFromResources("profile.json")
				.replaceFirst("<<userId>>", userId).replaceFirst("<<firstName>>", name)
				.replaceFirst("<<lastName>>", surname);
		return profileJson;
	}
}