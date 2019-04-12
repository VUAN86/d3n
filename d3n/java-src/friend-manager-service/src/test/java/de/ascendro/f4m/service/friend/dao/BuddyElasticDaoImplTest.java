package de.ascendro.f4m.service.friend.dao;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDaoImpl;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListOrderType;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class BuddyElasticDaoImplTest {

	private static final int ELASTIC_PORT = 9203;

	private static final String USER_ID_1 = "uid1";
	private static final String USER_ID_2 = "uid2";
	private static final String USER_ID_3 = "uid3";
	private static final String USER_ID_4 = "uid4";
	private static final String USER_ID_5 = "uid5";
	private static final String USER_ID_6 = "uid6";
	private static final String USER_ID_7 = "uid7";

	private static final String APP_ID_1 = "1";
	private static final String APP_ID_2 = "2";

	private static final String TENANT_ID = "tenant1";
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);
	
	private ElasticClient client;
	
	private BuddyElasticDao buddyDao;
	private CommonBuddyElasticDao commonBuddyDao;
	private CommonProfileElasticDao profileDao;
	
	@Before
	public void setUp() throws Exception {
		FriendManagerConfig config = new FriendManagerConfig();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		ElasticUtil elasticUtil = new ElasticUtil();
		client = new ElasticClient(config, elasticUtil, new ServiceMonitoringRegister());
		profileDao = new CommonProfileElasticDaoImpl(client, config, elasticUtil);
		buddyDao = new BuddyElasticDaoImpl(client, config, elasticUtil);
		commonBuddyDao = new CommonBuddyElasticDaoImpl(client, config);
	}
	
	@After
	public void shutdown() {
		client.close();
	}
	
	@Test
	public void testIndex() {
		prepareData(false);
		
		// Test paging
		assertContents(USER_ID_1, APP_ID_1, null, null, null, 10, 0, null, USER_ID_5, USER_ID_3, USER_ID_4, USER_ID_7, USER_ID_6, USER_ID_2);
		assertContents(USER_ID_1, APP_ID_1, null, null, null, 2, 1, null, USER_ID_3, USER_ID_4);
		assertContents(USER_ID_1, APP_ID_1, null, null, null, 10, 3, null, USER_ID_7, USER_ID_6, USER_ID_2);

		// Test owner
		assertContents(USER_ID_2, APP_ID_1, null, null, null, 10, 0, null);
		assertContents(USER_ID_3, APP_ID_1, null, null, null, 10, 0, null, USER_ID_4);

		// Test app ID
		assertContents(USER_ID_1, APP_ID_2, null, null, null, 10, 0, null, USER_ID_4, USER_ID_7, USER_ID_6, USER_ID_2);
		assertContents(USER_ID_1, "unknownAppId", null, null, null, 10, 0, null);
		
		// Test search term
		assertContents(USER_ID_1, APP_ID_1, "lumpenene", null, null, 10, 0, null);
		assertContents(USER_ID_1, APP_ID_1, "lumpe", null, null, 10, 0, null, USER_ID_4, USER_ID_7, USER_ID_6);
		assertContents(USER_ID_1, APP_ID_1, "ueBEr great muchacho", null, null, 10, 0, null, USER_ID_2); // 2 matches are enough
		assertContents(USER_ID_1, APP_ID_1, "ueber muchacho", null, null, 10, 0, null); // 1 match is not enough, more than 1 word specified
		
		// Test relation types
		assertContents(USER_ID_1, APP_ID_1, null, new BuddyRelationType[] { BuddyRelationType.BLOCKED }, null, 10, 0, null, USER_ID_3, USER_ID_2);
		assertContents(USER_ID_1, APP_ID_1, null, new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY }, null, 10, 0, null, USER_ID_5, USER_ID_4);
		assertContents(USER_ID_1, APP_ID_1, null, new BuddyRelationType[] { BuddyRelationType.BUDDY }, null, 10, 0, null, USER_ID_5, USER_ID_3, USER_ID_7, USER_ID_6);
		assertContents(USER_ID_1, APP_ID_1, null, new BuddyRelationType[] { BuddyRelationType.BUDDY }, 
				new BuddyRelationType[] { BuddyRelationType.BLOCKED }, 10, 0, null, USER_ID_5, USER_ID_7, USER_ID_6);
		assertContents(USER_ID_1, APP_ID_1, null, new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY, BuddyRelationType.BUDDY }, 
				null, 10, 0, null, USER_ID_5, USER_ID_3, USER_ID_4, USER_ID_7, USER_ID_6);
		
		// Test order type
		assertContents(USER_ID_1, APP_ID_1, null, null, null, 10, 0, BuddyListOrderType.NONE, USER_ID_5, USER_ID_3, USER_ID_4, USER_ID_7, USER_ID_6, USER_ID_2);
		assertContents(USER_ID_1, APP_ID_1, null, null, null, 10, 0, BuddyListOrderType.STRENGH, USER_ID_5, USER_ID_4, USER_ID_6, USER_ID_7, USER_ID_2, USER_ID_3);
		assertContents(USER_ID_1, APP_ID_1, null, null, null, 10, 0, BuddyListOrderType.RECENT, USER_ID_3, USER_ID_2, USER_ID_4, USER_ID_6, USER_ID_7, USER_ID_5);
		assertContents(USER_ID_1, APP_ID_1, null, null, null, 10, 0, BuddyListOrderType.HANDICAP, USER_ID_2, USER_ID_5, USER_ID_3, USER_ID_4, USER_ID_6, USER_ID_7);
		
		// Test all
		assertContents(USER_ID_1, APP_ID_1, null, new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY, BuddyRelationType.BUDDY }, 
				null, 10, 0, BuddyListOrderType.STRENGH, USER_ID_5, USER_ID_4, USER_ID_6, USER_ID_7, USER_ID_3);
		assertContents(USER_ID_1, APP_ID_1, "heym", new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY, BuddyRelationType.BUDDY }, 
				null, 10, 0, BuddyListOrderType.STRENGH, USER_ID_5);
	}

	@Test
	public void testGetAllUserIds() throws Exception {
		prepareData(false);
		
		// Test batching
		client.batchSize = 1;
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, null, null, null, null, null), contains(USER_ID_2, USER_ID_3, USER_ID_4, USER_ID_5, USER_ID_6, USER_ID_7));
		client.batchSize = 2;
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, null, null, null, null, null), contains(USER_ID_2, USER_ID_3, USER_ID_4, USER_ID_5, USER_ID_6, USER_ID_7));
		client.batchSize = 4;
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, null, null, null, null, null), contains(USER_ID_2, USER_ID_3, USER_ID_4, USER_ID_5, USER_ID_6, USER_ID_7));
		client.batchSize = 2;
		
		// Test app ID
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, APP_ID_2, null, null, null, null), contains(USER_ID_2, USER_ID_4, USER_ID_6, USER_ID_7));
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, "unknownAppId", null, null, null, null), hasSize(0));

		// Test tenant ID
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, APP_ID_1, TENANT_ID, null, null, null), contains(USER_ID_2, USER_ID_3, USER_ID_4, USER_ID_5, USER_ID_6, USER_ID_7));
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, APP_ID_1, "unknownTenant", null, null, null), hasSize(0));
		
		// Test relation types
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, APP_ID_1, TENANT_ID, new BuddyRelationType[] { BuddyRelationType.BLOCKED }, null, null), 
				contains(USER_ID_2, USER_ID_3));
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, APP_ID_1, TENANT_ID, new BuddyRelationType[] { BuddyRelationType.BLOCKED }, new BuddyRelationType[] { BuddyRelationType.BUDDY }, null), 
				contains(USER_ID_2));
		
		// Test buddy user ID
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, null, null, null, null, USER_ID_2), contains(USER_ID_2));
		assertThat(commonBuddyDao.getAllBuddyIds(USER_ID_1, null, null, null, null, USER_ID_1), hasSize(0));
	}
	
	@Test
	public void testRank() {
		prepareData(true);
		
		Pair<Long, Long> result = buddyDao.getRank(USER_ID_1, APP_ID_1, TENANT_ID, 0.0);
		assertEquals(4l, result.getLeft().longValue());
		assertEquals(7l, result.getRight().longValue());
		
		result = buddyDao.getRank(USER_ID_1, APP_ID_2, TENANT_ID, 0.0);
		assertEquals(2l, result.getLeft().longValue());
		assertEquals(5l, result.getRight().longValue());

		result = buddyDao.getRank(USER_ID_1, APP_ID_1, TENANT_ID, 15.0);
		assertEquals(2l, result.getLeft().longValue());
		assertEquals(7l, result.getRight().longValue());

		result = buddyDao.getRank(USER_ID_1, APP_ID_1, TENANT_ID, 5.3);
		assertEquals(2l, result.getLeft().longValue());
		assertEquals(7l, result.getRight().longValue());

		result = buddyDao.getRank(USER_ID_1, APP_ID_1, TENANT_ID, null); // Will be last, since all have handicap
		assertEquals(7l, result.getLeft().longValue());
		assertEquals(7l, result.getRight().longValue());

		result = buddyDao.getRank(USER_ID_1, APP_ID_1, TENANT_ID, 0.0);
		assertEquals(4l, result.getLeft().longValue());
		assertEquals(7l, result.getRight().longValue());
	}
	
	private void assertContents(String ownerId, String appId, String searchTerm, BuddyRelationType[] includedRelationTypes, 
			BuddyRelationType[] excludedRelationTypes, int limit, long offset, BuddyListOrderType orderType, String... userIds) {
		assertThat(buddyDao.getBuddyList(ownerId, appId, TENANT_ID, searchTerm, includedRelationTypes, excludedRelationTypes, null, null, orderType, limit, offset).getItems()
				.stream().map(b -> b.getUserId()).collect(Collectors.toList()), 
				userIds == null || userIds.length == 0 ? hasSize(0) : contains(userIds));
	}
	
	private void prepareData(boolean allBuddies) {
		prepareBuddy(USER_ID_1, USER_ID_2, "Some ueber great nickname", 
				allBuddies ? new BuddyRelationType[] { BuddyRelationType.BUDDY } : new BuddyRelationType[] { BuddyRelationType.BLOCKED }, 
				5, ZonedDateTime.of(2016, 11, 16, 10, 0, 0, 0, ZoneOffset.UTC), 33.6, APP_ID_1, APP_ID_2); // 1
		prepareBuddy(USER_ID_1, USER_ID_3, "Johnny B", 
				allBuddies ? new BuddyRelationType[] { BuddyRelationType.BUDDY } : new BuddyRelationType[] { BuddyRelationType.BUDDY, BuddyRelationType.BLOCKED }, 
				3, ZonedDateTime.of(2016, 11, 17, 10, 0, 0, 0, ZoneOffset.UTC), 1.0, APP_ID_1); // 3
		prepareBuddy(USER_ID_1, USER_ID_4, "Lumpen1", 
				allBuddies ? new BuddyRelationType[] { BuddyRelationType.BUDDY } : new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY }, 
				10, ZonedDateTime.of(2016, 11, 13, 10, 0, 0, 0, ZoneOffset.UTC), null, APP_ID_1, APP_ID_2); // 4
		prepareBuddy(USER_ID_1, USER_ID_5, "Heyman", 
				allBuddies ? new BuddyRelationType[] { BuddyRelationType.BUDDY } : new BuddyRelationType[] { BuddyRelationType.BUDDY, BuddyRelationType.BLOCKED_BY }, 
				100, ZonedDateTime.of(2015, 11, 1, 10, 0, 0, 0, ZoneOffset.UTC), 5.3, APP_ID_1); // 2
		prepareBuddy(USER_ID_1, USER_ID_6, "Lumpen3", new BuddyRelationType[] { BuddyRelationType.BUDDY }, 10, ZonedDateTime.of(2016, 11, 13, 10, 0, 0, 0, ZoneOffset.UTC), 0.0, APP_ID_1, APP_ID_2); // 5
		prepareBuddy(USER_ID_1, USER_ID_7, "Lumpen2", new BuddyRelationType[] { BuddyRelationType.BUDDY }, 10, ZonedDateTime.of(2016, 11, 13, 10, 0, 0, 0, ZoneOffset.UTC), null, APP_ID_1, APP_ID_2); // 6
		
		// Profiles already created
		prepareBuddy(USER_ID_3, USER_ID_4, null, 
				allBuddies ? new BuddyRelationType[] { BuddyRelationType.BUDDY } : new BuddyRelationType[] { BuddyRelationType.BUDDY }, 
				20, ZonedDateTime.of(2016, 11, 16, 10, 0, 0, 0, ZoneOffset.UTC), 42.2, false, APP_ID_1);
	}

	private void prepareBuddy(String ownerId, String userId, String nickname, BuddyRelationType[] relationTypes, 
			int interactionCount, ZonedDateTime lastInteractionTimestamp, Double handicap, String... appIds) {
		prepareBuddy(ownerId, userId, nickname, relationTypes, interactionCount, lastInteractionTimestamp, handicap, true, appIds);
	}
	
	private void prepareBuddy(String ownerId, String userId, String nickname, BuddyRelationType[] relationTypes, 
			int interactionCount, ZonedDateTime lastInteractionTimestamp, Double handicap, boolean prepareProfile, String... appIds) {
		Profile profile = new Profile();
		profile.setShowFullName(false);
		if (prepareProfile) {
			ProfileUser person = new ProfileUser();
			person.setNickname(nickname);
			person.setFirstName("First");
			person.setLastName("Last");
			profile.setPersonWrapper(person);
			
			ProfileAddress address = new ProfileAddress();
			address.setCountry("LV");
			address.setCity("Riga");
			profile.setAddress(address);
			
			profile.setUserId(userId);
			profile.setHandicap(handicap);
			Arrays.stream(appIds).forEach(appId -> profile.addApplication(appId));
			profileDao.createOrUpdate(profile);
		}

		Buddy buddy = new Buddy();
		buddy.setOwnerId(ownerId);
		buddy.setUserId(userId);
		buddy.addRelationTypes(relationTypes);
		buddy.setInteractionCount(interactionCount);
		buddy.setLastInteractionTimestamp(lastInteractionTimestamp);
		buddy.setLastPlayedGameInstanceId("gameInstanceId");
		buddy.addTenantIds(TENANT_ID);
		buddyDao.createOrUpdate(profile.getSearchName(), buddy);
		
	}
	
}
