package de.ascendro.f4m.service.achievement.integration;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDao;
import de.ascendro.f4m.server.achievement.dao.TopUsersElasticDaoImpl;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.AchievementStatus;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.MessagingType;
import de.ascendro.f4m.server.achievement.model.UserAchievement;
import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.server.achievement.model.UserProgressES;
import de.ascendro.f4m.server.achievement.model.UserScore;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.achievement.AchievementMessageTypes;
import de.ascendro.f4m.service.achievement.model.get.AchievementGetResponse;
import de.ascendro.f4m.service.achievement.model.get.AchievementListResponse;
import de.ascendro.f4m.service.achievement.model.get.BadgeGetResponse;
import de.ascendro.f4m.service.achievement.model.get.BadgeListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.TopUsersResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementGetResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserAchievementListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeGetResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeInstanceListResponse;
import de.ascendro.f4m.service.achievement.model.get.user.UserBadgeListResponse;
import de.ascendro.f4m.service.achievement.model.schema.AchievementMessageSchemaMapper;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.ImageUrl;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;
import de.ascendro.f4m.service.achievement.di.AchievementDefaultMessageMapper;

public class AchievementServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {

	private static final String NAME = "name";
	private static final String NICK = "nick";
	private static final int USER_COUNT = 15;
	private static final int TOP_10_SIZE = 10;
	private static final String BADGE_SET_BLOB_BIN_NAME = "badge";
	private static final String USER_ACHIEVEMENT_SET_BLOB_BIN_NAME = "userAchievemnt";
	private static final String ACHIEVEMENT_SET_BLOB_BIN_NAME = "achievement";
	private static final String BADGE_LIST_SET_BLOB_BIN_NAME = "badgeList";
	private static final String USER_BADGE_LIST_SET_BLOB_BIN_NAME = "userBadgeList";
	private static final String ACHIEVEMENT_LIST_BIN_NAME = "achieveList";
	private static final String USER_ACHIEVEMENT_LIST_BIN_NAME = "usrAchieveList";
	
	public static final String ELASTIC_TYPE_BUDDY = "elastic.type.buddy";

	private static final String PROPERTY_BUDDY_SEARCH_NAME = "buddySearchName";
	
	public static final String TENANTID = "TENANTID";
	public static final String USER = "USER";
	private static final String USERX = "USERX";
	private ReceivedMessageCollector receivedMessageCollector;
	private AchievementAerospikeDao achievementAerospikeDao;
	private IAerospikeClient aerospikeClient;
	private JsonUtil jsonUtil;
	private ClientInfo clientInfo;

	private static final String NAMESPACE = "test";

	private static final int ELASTIC_PORT = 9200;

	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);

	private ElasticClient client;
	
	private ElasticUtil elasticUtil;

	private TopUsersElasticDao topUsersElasticDao;
	private CommonProfileAerospikeDao commonProfileAerospikeDao;

	@Override
	protected MockServiceRule.ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	@Override
	protected void configureInjectionModuleMock(MockServiceRule.BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(AchievementDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(AchievementMessageSchemaMapper.class);
	}

	private JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		throw new UnexpectedTestException("Unexpected message " + originalMessageDecoded);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		AchievementConfig config = new AchievementConfig();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);

		elasticUtil = new ElasticUtil();
		client = new ElasticClient(config, elasticUtil, new ServiceMonitoringRegister());
		topUsersElasticDao = new TopUsersElasticDaoImpl(client, config, new JsonUtil());

		receivedMessageCollector = (ReceivedMessageCollector) clientInjector
				.getInstance(com.google.inject.Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class))
				.get();

		Injector injector = jettyServerRule.getServerStartup().getInjector();
		achievementAerospikeDao = injector.getInstance(AchievementAerospikeDaoImpl.class);
		commonProfileAerospikeDao = injector.getInstance(CommonProfileAerospikeDao.class);
		aerospikeClient = jettyServerRule.getServerStartup().getInjector().getInstance(AerospikeClientProvider.class)
				.get();

		jsonUtil = new JsonUtil();
		setClientInfo(ANONYMOUS_CLIENT_INFO);
	}

	@After
	public void tearDown() throws Exception {
		client.close();
	}

	private void setClientInfo(ClientInfo clientInfo) {
		this.clientInfo = ClientInfo.cloneOf(clientInfo);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(AchievementDefaultMessageMapper.class,
				AchievementMessageSchemaMapper.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new AchievementServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Test
	public void testAchievementList() throws Exception {
		updateAchievementListTestData("AchievementListTestData.json");
		String requestJson = getPlainTextJsonFromResources("AchievementListRequest.json", clientInfo)
				.replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.ACHIEVEMENT_LIST_RESPONSE);

		final JsonMessage<AchievementListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.ACHIEVEMENT_LIST_RESPONSE);

		assertNull(response.getError());
		assertAchievementListResponse(response.getContent());
	}

	@Test
	public void testAchievementListWrongTenant() throws Exception {
		updateAchievementListTestData("AchievementListTestData.json");
		clientInfo.setTenantId(TENANTID);
		String requestJson = getPlainTextJsonFromResources("AchievementListRequest.json", clientInfo)
				.replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.ACHIEVEMENT_LIST_RESPONSE);

		final JsonMessage<AchievementListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.ACHIEVEMENT_LIST_RESPONSE);

		assertNull(response.getError());
		assertEquals(0, response.getContent().getItems().size());
	}

	private void assertAchievementListResponse(AchievementListResponse response) {
		assertEquals(5, response.getItems().size());
		assertEquals("ACHIEVEMENT5", response.getItems().get(0).getId());
		assertEquals("ACHIEVEMENT3", response.getItems().get(1).getId());
		assertEquals("ACHIEVEMENT4", response.getItems().get(2).getId());
		assertEquals("ACHIEVEMENT1", response.getItems().get(3).getId());
		assertEquals("ACHIEVEMENT2", response.getItems().get(4).getId());
	}

	@Test
	public void testAchievementListWithPagination() throws Exception {
		updateAchievementListTestData("AchievementListTestData.json");
		String requestJson = getPlainTextJsonFromResources("AchievementListRequest.json", clientInfo)
				.replaceFirst("<<limit>>", "2").replaceFirst("<<offset>>", "2");

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.ACHIEVEMENT_LIST_RESPONSE);

		final JsonMessage<AchievementListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.ACHIEVEMENT_LIST_RESPONSE);

		assertNull(response.getError());
		assertPaginatedAchievementListResponse(response.getContent());
	}

	private void assertPaginatedAchievementListResponse(AchievementListResponse response) {
		assertEquals(5, response.getTotal());
		assertEquals(2, response.getSize());
		assertEquals(2, response.getLimit());
		assertEquals(2, response.getOffset());
		assertEquals("ACHIEVEMENT4", response.getItems().get(0).getId());
		assertEquals("ACHIEVEMENT1", response.getItems().get(1).getId());
	}

	@Test
	public void testAchievementGet() throws Exception {
		updateTestData("AchievementTestData.json", config.getProperty(AchievementConfig.AEROSPIKE_ACHIEVEMENT_SET),
				ACHIEVEMENT_SET_BLOB_BIN_NAME);
		String requestJson = getPlainTextJsonFromResources("AchievementGetRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.ACHIEVEMENT_GET_RESPONSE);

		final JsonMessage<AchievementGetResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.ACHIEVEMENT_GET_RESPONSE);

		assertNull(response.getError());
		Achievement achievement = response.getContent().getAchievement();
		assertEquals("1", achievement.getId());
		assertEquals("super-achievement", achievement.getName());
		assertEquals("11", achievement.getTenantId());
		assertEquals(AchievementStatus.ACTIVE, achievement.getStatus());
		assertEquals(true, achievement.isReward());
		assertEquals(Float.valueOf(1.1f), achievement.getPaymentMultiplier());
		assertEquals(MessagingType.IN_APP_NOTIFICATION, achievement.getMessaging().get(1));
	}

	@Test
	public void testUserAchievementList() throws Exception {
		updateUserAchievementListTestData("UserAchievementListTestData.json");
		String requestJson = getPlainTextJsonFromResources("UserAchievementListRequest.json", clientInfo);

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.USER_ACHIEVEMENT_LIST_RESPONSE);

		final JsonMessage<UserAchievementListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.USER_ACHIEVEMENT_LIST_RESPONSE);

		assertNull(response.getError());

		UserAchievementListResponse content = response.getContent();
		assertEquals(5, content.getTotal());
		assertEquals(3, content.getSize());
		assertEquals(3, content.getLimit());
		assertEquals(1, content.getOffset());
		String userId = "a0fdfd8b-174c-4a80-9d2e-94cde52386d5";
		String tenantId = "60551c1e-a718-444-80f5-76304dec7eb7";
		assertUserAchievement(content.getItems().get(0), "ACHIEVEMENT2", userId, tenantId);
		assertUserAchievement(content.getItems().get(1), "ACHIEVEMENT3", userId, tenantId);
		assertUserAchievement(content.getItems().get(2), "ACHIEVEMENT4", userId, tenantId);
	}

	private void assertUserAchievement(UserAchievement userAchievement, String achievementId, String userId, String tenantId) {
		assertEquals(achievementId, userAchievement.getAchievementId());
		assertEquals(userId, userAchievement.getUserId());
		assertEquals(tenantId, userAchievement.getTenantId());
	}

	@Test
	public void testUserAchievementGet() throws Exception {
		updateTestData("UserAchievementTestData.json",
				config.getProperty(AchievementConfig.AEROSPIKE_USER_ACHIEVEMENT_SET),
				USER_ACHIEVEMENT_SET_BLOB_BIN_NAME);
		String requestJson = getPlainTextJsonFromResources("UserAchievementGetRequest.json", clientInfo);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.USER_ACHIEVEMENT_GET_RESPONSE);

		final JsonMessage<UserAchievementGetResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.USER_ACHIEVEMENT_GET_RESPONSE);

		assertNull(response.getError());
		UserAchievement userAchievement = response.getContent().getUserAchievement();
		assertEquals("a0fdfd8b-174c-4a80-9d2e-94cde52386d5", userAchievement.getUserId());
		assertEquals("1", userAchievement.getAchievementId());
		assertEquals(true, userAchievement.getBadgesWonStatus().get("6453-8cd1-44a3"));
		assertEquals(null, userAchievement.getBadgesWonStatus().get("6453-8cd1-44ff"));
		assertNotNull(userAchievement.getLimitDate());
		assertNotNull(userAchievement.getStartDate());
		assertNotNull(userAchievement.getCompletedOn());
	}

	@Test
	public void testBadgeList() throws Exception {
		updateBadgeListTestData("BadgeListTestData.json");
		String requestJson = getPlainTextJsonFromResources("BadgeListRequest.json", clientInfo)
				.replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.BADGE_LIST_RESPONSE);

		final JsonMessage<BadgeListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.BADGE_LIST_RESPONSE);

		assertNull(response.getError());
		assertBadgeListResponse(response.getContent());
	}

	private void assertBadgeListResponse(BadgeListResponse response) {
		assertEquals(5, response.getItems().size());
		assertEquals("BADGE5", response.getItems().get(0).getId());
		assertEquals("BADGE4", response.getItems().get(1).getId());
		assertEquals("BADGE3", response.getItems().get(2).getId());
		assertEquals("BADGE2", response.getItems().get(3).getId());
		assertEquals("BADGE1", response.getItems().get(4).getId());
	}

	@Test
	public void testBadgeListWithPagination() throws Exception {
		updateBadgeListTestData("BadgeListTestData.json");
		String requestJson = getPlainTextJsonFromResources("BadgeListRequest.json", clientInfo)
				.replaceFirst("<<limit>>", "2").replaceFirst("<<offset>>", "2");

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.BADGE_LIST_RESPONSE);

		final JsonMessage<BadgeListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.BADGE_LIST_RESPONSE);

		assertNull(response.getError());
		assertPaginatedBadgeListResponse(response.getContent());
	}

	private void assertPaginatedBadgeListResponse(BadgeListResponse response) {
		assertEquals(5, response.getTotal());
		assertEquals(2, response.getSize());
		assertEquals(2, response.getLimit());
		assertEquals(2, response.getOffset());
		assertEquals("BADGE3", response.getItems().get(0).getId());
		assertEquals("BADGE2", response.getItems().get(1).getId());
	}

	@Test
	public void testBadgeGet() throws Exception {
		updateTestData("BadgeTestData.json", config.getProperty(AchievementConfig.AEROSPIKE_BADGE_SET),
				BADGE_SET_BLOB_BIN_NAME);
		String requestJson = getPlainTextJsonFromResources("BadgeGetRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.BADGE_GET_RESPONSE);

		final JsonMessage<BadgeGetResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.BADGE_GET_RESPONSE);

		assertNull(response.getError());
		Badge badge = response.getContent().getBadge();
		assertEquals("1", badge.getId());
		assertEquals("11", badge.getTenantId());
		assertEquals(BadgeType.COMMUNITY, badge.getType());
		assertEquals(AchievementStatus.ACTIVE, badge.getStatus());
		assertEquals(true, badge.isReward());
		assertEquals(Float.valueOf(1.05f), badge.getPaymentMultiplier());
		assertEquals(MessagingType.IN_APP_NOTIFICATION, badge.getMessaging().get(1));
		assertEquals(Integer.valueOf(10), badge.getRules().get(1).getLevel());
		assertEquals("2017-01-01T01:03:01Z", badge.getCreatedOn());
	}

	@Test
	public void testUserBadgeList() throws Exception {
		updateUserBadgeListTestData("UserBadgeListTestData.json");
		String requestJson = getPlainTextJsonFromResources("UserBadgeListRequest.json", clientInfo);

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.USER_BADGE_LIST_RESPONSE);

		final JsonMessage<UserBadgeListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.USER_BADGE_LIST_RESPONSE);

		assertNull(response.getError());

		UserBadgeListResponse content = response.getContent();
		assertEquals(2, content.getTotal());
		assertEquals(2, content.getSize());
		assertEquals(10, content.getLimit());
		assertEquals(0, content.getOffset());
		UserBadge userBadge = content.getItems().get(0);
		assertEquals("a0fdfd8b-174c-4a80-9d2e-94cde52386d5", userBadge.getUserId());
		assertEquals("60551c1e-a718-444-80f5-76304dec7eb7", userBadge.getTenantId());
		assertEquals("BADGE1", userBadge.getBadgeId());

	}

	@Test
	public void testUserBadgeGet() throws Exception {
		updateUserBadgeTestData("UserBadgeTestData.json",
				config.getProperty(AchievementConfig.AEROSPIKE_USER_BADGE_SET));
		String requestJson = getPlainTextJsonFromResources("UserBadgeGetRequest.json", clientInfo);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.USER_BADGE_GET_RESPONSE);

		final JsonMessage<UserBadgeGetResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.USER_BADGE_GET_RESPONSE);

		assertNull(response.getError());
		UserBadge userBadge = response.getContent().getUserBadge();
		assertEquals("1", userBadge.getBadgeId());
		assertEquals(clientInfo.getTenantId(), userBadge.getTenantId());
		assertEquals(clientInfo.getUserId(), userBadge.getUserId());

		// rules :
		assertEquals(2, userBadge.getRules().size());
		assertEquals(Integer.valueOf(1000), userBadge.getRules().get(0).getLevel());
		assertEquals(Integer.valueOf(500), userBadge.getRules().get(0).getProgress());
		assertEquals(Integer.valueOf(10), userBadge.getRules().get(1).getProgress());
	}

	@Test
	public void testUserBadgeInstanceList() throws Exception {
		updateUserBadgeTestData("UserBadgeTestData.json",
				config.getProperty(AchievementConfig.AEROSPIKE_USER_BADGE_SET));
		String requestJson = getPlainTextJsonFromResources("UserBadgeInstanceListRequest.json", clientInfo)
				.replaceFirst("<<limit>>", "10").replaceFirst("<<offset>>", "0");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.USER_BADGE_INSTANCE_LIST_RESPONSE);

		final JsonMessage<UserBadgeInstanceListResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.USER_BADGE_INSTANCE_LIST_RESPONSE);

		assertNull(response.getError());
		List<UserBadge> userBadges = response.getContent().getItems();
		assertEquals("1", userBadges.get(0).getBadgeId());
		assertEquals(clientInfo.getTenantId(), userBadges.get(0).getTenantId());
		assertEquals(clientInfo.getUserId(), userBadges.get(0).getUserId());

		assertEquals(2, userBadges.size());
	}

	@Test
	public void testTopUsersList() throws Exception {
		prepareElasticsearchTestData(USER,TENANT_ID);
		
		final JsonMessage<TopUsersResponse> response = getTop(clientInfo, "GAME", false);

		checkTop(response, USER, TENANT_ID);
	}

	@Test
	public void testTopUsersListWithMultiTenants() throws Exception {
		prepareElasticsearchTestData(USER, TENANT_ID);
		prepareElasticsearchTestData(USERX, TENANTID);		
		clientInfo.setTenantId(TENANTID);
		
		final JsonMessage<TopUsersResponse> response1 = getTop(clientInfo, "GAME", false);
		
		checkTop(response1, USERX, TENANTID);
	}

	@Test
	public void testTopFriendsList() throws Exception {
		prepareElasticsearchTestData(USER, TENANT_ID);
		prepareBuddy();
		clientInfo.setUserId(USER + 0);
		
		final JsonMessage<TopUsersResponse> response = getTop(clientInfo, "GAME", true);

		checkTop(response, USER, TENANT_ID);
	}

	@Test
	public void testTopFriendsList1() throws Exception {
		prepareElasticsearchTestData(USER, TENANT_ID);
		prepareBuddy();
		clientInfo.setUserId(USER + 3);
		
		final JsonMessage<TopUsersResponse> response = getTop(clientInfo, "GAME", true);

		assertEquals(2, response.getContent().getSize());
		assertEquals(USER + 11, response.getContent().getItems().get(0).getUserId());
		assertEquals(USER + 2, response.getContent().getItems().get(1).getUserId());
	}

	@Test
	public void testTopFriendsList2() throws Exception {
		prepareElasticsearchTestData(USER, TENANT_ID);
		prepareBuddy();
		clientInfo.setUserId(USER + 4);
		
		final JsonMessage<TopUsersResponse> response = getTop(clientInfo, "GAME", true);

		assertEquals(1, response.getContent().getSize());
		assertEquals(USER + 10, response.getContent().getItems().get(0).getUserId());
	}

	public void createOrUpdateBuddy(String searchName, Buddy buddy) throws F4MFatalErrorException, IOException {
		initIndex();
		JsonObject buddyJson = elasticUtil.copyJson(buddy.getJsonObject(), JsonObject.class);
		
		// Unfortunately elastic does not allow to sort by parent field, and AWS has disabled scripting, so best solution 
		// for searching by name is keeping a (possibly outdated) searchName field. Will update the field on any operation on buddy.
		if (searchName != null) {
			buddyJson.addProperty(PROPERTY_BUDDY_SEARCH_NAME, searchName);
		} else {
			buddyJson.remove(PROPERTY_BUDDY_SEARCH_NAME);
		}
		client.createOrUpdate(getProfileIndex(), getBuddyType(), buddy.getId(), buddyJson, buddy.getUserId());
	}
	
	private void initIndex() throws F4MFatalErrorException, IOException {
			synchronized(this) {
					String index = getProfileIndex();
					if (! client.indexExists(index)) {
						client.createIndex(index, config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_INDEX_PROFILE));
					}
					client.createMapping(index, config.getProperty(ElasticConfigImpl.ELASTIC_TYPE_PROFILE), config.getProperty(ElasticConfigImpl.ELASTIC_MAPPING_TYPE_PROFILE));
					client.createMapping(index, getBuddyType(), Resources.toString(Resources.getResource(getClass(), "BuddyESMapping.json"),Charsets.UTF_8));
			}
	}
	
	private String getProfileIndex() {
		return config.getProperty(ElasticConfigImpl.ELASTIC_INDEX_PROFILE);
	}

	private String getBuddyType() {
		return config.getProperty(ELASTIC_TYPE_BUDDY);
	}

	private void prepareElasticsearchTestData(String userPrefix, String tenantId) throws IOException {
		for(int i = 0; i< USER_COUNT; i++) {
			final String sourceJsonString = getProfileJson(userPrefix + i, userPrefix + i, NAME, NICK + i);
			final JsonElement jsonElement = JsonTestUtil.getGson().fromJson(sourceJsonString, JsonElement.class);
			final Profile profile = new Profile(jsonElement);
			profile.setShowFullName(showFullName(i));

			commonProfileAerospikeDao.createJson("profile", "profile:" + userPrefix + i, "value", profile.getAsString());
			
			UserProgressES userProgress = new UserProgressES(userPrefix + i, tenantId, i, i);
			topUsersElasticDao.createOrUpdate(userProgress, tenantId);
		}
	}

	private JsonMessage<TopUsersResponse> getTop(ClientInfo clientInfo, String type, boolean friendsOnly) throws IOException, URISyntaxException {
		String requestJson = getPlainTextJsonFromResources("TopTenUsersListRequest.json", clientInfo)
				.replaceFirst("<<BADGE_TYPE>>", type)
				.replaceFirst("<<FRIENDS_ONLY>>", String.valueOf(friendsOnly));

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(AchievementMessageTypes.TOP_USERS_LIST_RESPONSE);

		final JsonMessage<TopUsersResponse> response = receivedMessageCollector
				.getMessageByType(AchievementMessageTypes.TOP_USERS_LIST_RESPONSE);
		return response;
	}

	private void checkTop(final JsonMessage<TopUsersResponse> response, String userPrefix, String tenantId) {
		assertEquals(response.getContent().getSize(), TOP_10_SIZE);

		int index = 0;
		for(int i = USER_COUNT-1; i >= USER_COUNT - TOP_10_SIZE; i--) {
			UserScore userScore = response.getContent().getItems().get(index);
			assertEquals(userPrefix + i, userScore.getUserId());
			assertEquals(tenantId, userScore.getTenantId());
			if (showFullName(index)) {
				assertEquals(userPrefix + i + " " + NAME, userScore.getName());
			} else {
				assertEquals(NICK + i, userScore.getName());
			}
			assertEquals(true, userScore.getImage().endsWith(userPrefix + i + ImageUrl.IMAGE_EXTENSION));
			index++;
		}
	}

	private boolean showFullName(int index) {
		return index % 2 == 0; //every second user hides his name
	}

	private void prepareBuddy() throws F4MFatalErrorException, IOException {
		for(int i = 0; i< USER_COUNT; i++) {
			if (i > 0) {
				Buddy buddy = new Buddy(USER + 0, BuddyRelationType.BUDDY, USER + i, TENANT_ID, false);
				createOrUpdateBuddy(USER + 0, buddy);
			}
			Buddy buddy1 = new Buddy(USER + i, BuddyRelationType.BUDDY, USER + (15 - 1 - i), TENANT_ID, false);
			createOrUpdateBuddy(USER + i, buddy1);
			if (i % 2 == 1) {
				Buddy buddy2 = new Buddy(USER + i, BuddyRelationType.BUDDY, USER + (i - 1), TENANT_ID, false);
				createOrUpdateBuddy(USER + i, buddy2);
			}
		}
		
	}

	private void updateBadgeListTestData(String fileName) throws IOException {
		String set = config.getProperty(AchievementConfig.AEROSPIKE_BADGE_LIST_SET);
		updateMapTestData(fileName, set, BADGE_LIST_SET_BLOB_BIN_NAME);
	}

	private void updateUserBadgeListTestData(String fileName) throws IOException {
		String set = config.getProperty(AchievementConfig.AEROSPIKE_USER_BADGE_LIST_SET);
		updateMapTestData(fileName, set, USER_BADGE_LIST_SET_BLOB_BIN_NAME);
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

	private void updateUserBadgeTestData(String fileName, String set) throws IOException {
		String jsonDataSet = getPlainTextJsonFromResources(fileName);

		JsonParser parser = new JsonParser();
		JsonArray jsonArray = (JsonArray) parser.parse(jsonDataSet);

		for (int pos = 0; pos < jsonArray.size(); pos++) {
			JsonObject item = jsonArray.get(pos).getAsJsonObject();

			for (Map.Entry<String, JsonElement> entry : item.entrySet()) {
				Key key = new Key(NAMESPACE, set, entry.getKey());

				JsonObject jsonBins = entry.getValue().getAsJsonObject();

				Bin[] bins = new Bin[jsonBins.entrySet().size()];
				int i = 0;
				for (Map.Entry<String, JsonElement> property : jsonBins.entrySet()) {
					if (i == 0) {
						String value = property.getValue().toString();
						byte[] binValue = value == null ? null : value.getBytes(getDefaultCharset());
						bins[i] = new Bin(property.getKey(), binValue);
					} else {
						Integer binValue = property.getValue().getAsInt();
						bins[i] = new Bin(property.getKey(), binValue);
					}
					i++;
				}

				aerospikeClient.put(null, key, bins);
			}
		}
	}

	private void updateAchievementListTestData(String fileName) throws IOException {
		String set = config.getProperty(AchievementConfig.AEROSPIKE_ACHIEVEMENT_LIST_SET);
		updateMapTestData(fileName, set, ACHIEVEMENT_LIST_BIN_NAME);
	}

	private void updateUserAchievementListTestData(String fileName) throws IOException {
		String set = config.getProperty(AchievementConfig.AEROSPIKE_USER_ACHIEVEMENT_LIST_SET);
		updateMapTestData(fileName, set, USER_ACHIEVEMENT_LIST_BIN_NAME);
	}

	private void updateMapTestData(String fileName, String set, String binName) throws IOException {

		String itemList = getPlainTextJsonFromResources(fileName);

		JsonParser parser = new JsonParser();
		JsonObject jsonObject = (JsonObject) parser.parse(itemList);
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String binValue = entry.getValue().toString();
			achievementAerospikeDao.createOrUpdateMap(set, key, binName,
					(readResult, writePolicy) -> getMapObject(binValue));
		}
	}

	private Map<Object, Object> getMapObject(String data) {
		Map<Object, Object> mapObject = new HashMap<>();
		JsonObject jsonArray = jsonUtil.fromJson(data, JsonObject.class);
		for (Map.Entry<String, JsonElement> item : jsonArray.entrySet()) {
			mapObject.put(item.getKey(), item.getValue().toString());
		}
		return mapObject;
	}

	protected Charset getDefaultCharset() {
		final String defaultCharset = config.getProperty(AerospikeConfigImpl.AEROSPIKE_WRITE_BYTES_CHARSET);
		return Charset.forName(defaultCharset != null ? defaultCharset : "UTF-8");
	}

	private String getProfileJson(String userId, String name, String surname, String nickname) throws IOException {
		final String profileJson = jsonLoader.getPlainTextJsonFromResources("profile.json")
				.replaceFirst("<<userId>>", userId).replaceFirst("<<firstName>>", name)
				.replaceFirst("<<lastName>>", surname).replaceFirst("<<nickname>>", nickname);
		return profileJson;
	}

}
