package de.ascendro.f4m.service.analytics;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Injector;

import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDaoImpl;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.analytics.module.achievement.IEventProcessor;
import de.ascendro.f4m.service.analytics.module.achievement.processor.EventProcessor;
import de.ascendro.f4m.service.analytics.notification.di.AnalyticsDefaultMessageTypeMapper;
import de.ascendro.f4m.service.analytics.notification.schema.AnalyticsMessageSchemaMapper;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class AnalyticsServiceStartupClientTest extends F4MServiceWithMockIntegrationTestBase {
	
	private static final String BADGE_LIST_SET_BLOB_BIN_NAME = "badgeList";
	private static final String ACHIEVEMENT_LIST_BIN_NAME = "achieveList";
	private static final String TENANTS_WITH_ACHIEVEMENTS_BLOB_BIN_NAME = "tenantIdSet";
	private static final String TENANT1 = "TENANT1";

	private JsonUtil jsonUtil;
	private JsonMessageUtil jsonMessageUtil;
	private IEventProcessor eventProcessor;
	private AchievementAerospikeDao achievementAerospikeDao;
	private ClientInfo clientInfo;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		
		jsonUtil = injector.getInstance(JsonUtil.class);
		jsonMessageUtil = injector.getInstance(JsonMessageUtil.class);
		eventProcessor = injector.getInstance(EventProcessor.class);
		achievementAerospikeDao = injector.getInstance(AchievementAerospikeDaoImpl.class);
		
		setClientInfo(ANONYMOUS_CLIENT_INFO);
	}

	private void setClientInfo(ClientInfo clientInfo) {
		this.clientInfo = ClientInfo.cloneOf(clientInfo);
	}

	@Test
	public void testAchievementsRefresh() throws Exception {
		updateTestData("TenantsWithAchievementsTestData.json", config.getProperty(AchievementConfig.AEROSPIKE_TENANTS_WITH_ACHIEVEMENTS_SET),
			TENANTS_WITH_ACHIEVEMENTS_BLOB_BIN_NAME);
		updateBadgeListTestData("BadgeListTestData.json");
		updateBadgeListTestData("BadgeListTestData1.json");
		updateAchievementListTestData("AchievementListTestData.json");
		updateAchievementListTestData("AchievementListTestData1.json");
		
		EventContent eventContent = new EventContent();
		eventContent.setTenantId(clientInfo.getTenantId());
		eventContent.setEventData(new InviteEvent());
		eventContent.setEventType(InviteEvent.class.getCanonicalName());
		
		EventContent anotherEventContent = new EventContent();
		anotherEventContent.setTenantId(clientInfo.getTenantId());
		anotherEventContent.setEventType(PlayerGameEndEvent.class.getCanonicalName());
		
		EventContent notSupportedEventContent = new EventContent();
		notSupportedEventContent.setTenantId(clientInfo.getTenantId());
		anotherEventContent.setEventType(AdEvent.class.getCanonicalName());
		
		EventContent anotherTenantEventContent = new EventContent();
		anotherTenantEventContent.setTenantId(TENANT1);
		anotherTenantEventContent.setEventType(PlayerGameEndEvent.class.getCanonicalName());

		final String eventJson = getPlainTextJsonFromResources("AchievementsRefreshNotification.json");
		sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

		RetriedAssert.assertWithWait(
				() -> assertEquals(false, eventProcessor.getAssociatedBadges(eventContent).isEmpty()), 2000);
		
		List<Badge> associatedBadges = eventProcessor.getAssociatedBadges(eventContent);
		assertTrue(!associatedBadges.isEmpty());
		Badge badge = associatedBadges.get(0);
		assertEquals("BADGE1", badge.getId());
		assertEquals("60551c1e-a718-444-80f5-76304dec7eb7", badge.getTenantId());
		
		assertTrue(eventProcessor.getAssociatedBadges(anotherEventContent).isEmpty());
		assertTrue(eventProcessor.getAssociatedBadges(notSupportedEventContent).isEmpty());
		
		List<Achievement> associatedAchievements = eventProcessor.getAssociatedAchievements(clientInfo.getTenantId(), badge);
		assertTrue(!associatedAchievements.isEmpty());
		Achievement achievement = associatedAchievements.get(0);
		assertEquals("ACHIEVEMENT2", achievement.getId());
		assertEquals("60551c1e-a718-444-80f5-76304dec7eb7", achievement.getTenantId());
		
		List<Badge> associatedBadges1 = eventProcessor.getAssociatedBadges(anotherTenantEventContent);
		assertTrue(!associatedBadges1.isEmpty());
		Badge badge1 = associatedBadges1.get(0);
		assertEquals("BADGE_B4", badge1.getId());
		assertEquals(TENANT1, badge1.getTenantId());
		
		List<Achievement> associatedAchievements1 = eventProcessor.getAssociatedAchievements(TENANT1, badge1);
		assertTrue(!associatedAchievements1.isEmpty());
		Achievement achievement1 = associatedAchievements1.get(0);
		assertEquals("ACHIEVEMENT_X5", achievement1.getId());
		assertEquals(TENANT1, achievement1.getTenantId());
		
		badge1.setId("BADGE_B1");
		List<Achievement> associatedAchievements2 = eventProcessor.getAssociatedAchievements(TENANT1, badge1);
		assertEquals(2, associatedAchievements2.size());
		Achievement achievement2 = associatedAchievements2.get(0);
		assertEquals("ACHIEVEMENT_X5", achievement2.getId());
		assertEquals(TENANT1, achievement2.getTenantId());
		Achievement achievement3 = associatedAchievements2.get(1);
		assertEquals("ACHIEVEMENT_X3", achievement3.getId());
		assertEquals(TENANT1, achievement3.getTenantId());

	}

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return null;
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new AnalyticsServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(AnalyticsDefaultMessageTypeMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(AnalyticsMessageSchemaMapper.class);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(AnalyticsDefaultMessageTypeMapper.class,
				AnalyticsMessageSchemaMapper.class);
	}

	private void updateAchievementListTestData(String fileName) throws IOException {
		String set = "achievementList";
		updateMapTestData(fileName, set, ACHIEVEMENT_LIST_BIN_NAME);
	}

	private void updateBadgeListTestData(String fileName) throws IOException {
		String set = "badgeList";
		updateMapTestData(fileName, set, BADGE_LIST_SET_BLOB_BIN_NAME);
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


}
