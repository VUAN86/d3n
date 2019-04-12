package de.ascendro.f4m.service.friend.integration;

import static de.ascendro.f4m.service.friend.builder.BuddyBuilder.createBuddy;
import static de.ascendro.f4m.service.friend.builder.ContactBuilder.createContact;
import static de.ascendro.f4m.service.friend.builder.ProfileBuilder.createProfile;
import static de.ascendro.f4m.service.friend.model.api.player.PlayerListResultType.BUDDY;
import static de.ascendro.f4m.service.friend.model.api.player.PlayerListResultType.CONTACT;
import static de.ascendro.f4m.service.friend.model.api.player.PlayerListResultType.PLAYER;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.dao.BuddyAerospikeDao;
import de.ascendro.f4m.service.friend.dao.ContactAerospikeDao;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.LeaderboardPositionResponse;
import de.ascendro.f4m.service.friend.model.api.player.ApiPlayerListResult;
import de.ascendro.f4m.service.friend.model.api.player.PlayerLeaderboardListResponse;
import de.ascendro.f4m.service.friend.model.api.player.PlayerListResponse;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class PlayerTest extends FriendManagerTestBase {
	
	private static final int ELASTIC_PORT = 9207;
	
	public static final String USER_ID_1 = "user_id_1";
	public static final String USER_ID_2 = "user_id_2";
	public static final String USER_ID_3 = "user_id_3";
	
	private BuddyAerospikeDao buddyDao;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	private CommonProfileElasticDao profileElasticDao;
	private ContactAerospikeDao contactDao;

	@BeforeClass
	public static void setUpClass() {
		F4MServiceWithMockIntegrationTestBase.setUpClass();
		System.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		System.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
	}
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);
	
	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		buddyDao = injector.getInstance(BuddyAerospikeDao.class);
		profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
		profileElasticDao = injector.getInstance(CommonProfileElasticDao.class);
		contactDao = injector.getInstance(ContactAerospikeDao.class);
	}
	
	@Test
	public void testPlayerBlock() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).withApplications(APP_ID).buildProfile();
		Profile profile2 = createProfile(USER_ID_2).withApplications(APP_ID).buildProfile();
		saveProfiles(profile1, profile2);
		String[] userIds = new String[] { USER_ID_1, USER_ID_2 };
		
		String requestJson = jsonLoader.getPlainTextJsonFromResources("playerBlockRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("\"<<userIds>>\"", jsonUtil.toJson(Arrays.asList(USER_ID_1, USER_ID_2)));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_BLOCK_RESPONSE);
		
		List<Buddy> buddies = buddyDao.getBuddies(ANONYMOUS_USER_ID, Arrays.asList(userIds));
		assertThat(buddies, hasSize(userIds.length));
		assertThat(buddies.stream().map(b -> b.getUserId()).toArray(size -> new String[size]), arrayContainingInAnyOrder(userIds));
		buddies.stream().forEach(b -> {
			assertThat(b.getRelationTypes(), hasSize(1));
			assertTrue(b.getRelationTypes().contains(BuddyRelationType.BLOCKED));
		});
	}
	
	@Test
	public void testPlayerUnblock() throws Exception {
		Profile profile = createProfile(USER_ID_1).withApplications(APP_ID).buildProfile();
		saveProfiles(profile);
		buddyDao.createBuddyRelation(ANONYMOUS_USER_ID, USER_ID_1, TENANT_ID, BuddyRelationType.BLOCKED, null);
		
		String requestJson = jsonLoader.getPlainTextJsonFromResources("playerUnblockRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("\"<<userIds>>\"", jsonUtil.toJson(Arrays.asList(USER_ID_1)));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_UNBLOCK_RESPONSE);
		
		Buddy buddy = buddyDao.getBuddies(ANONYMOUS_USER_ID, Arrays.asList(USER_ID_1)).get(0);
		assertTrue(buddy.getRelationTypes().isEmpty());
	}
	
	@Test
	public void testPlayerList() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).withApplications(APP_ID).buildProfile();
		Profile profile2 = createProfile(USER_ID_2).withApplications(APP_ID).buildProfile();
		Profile profile3 = createProfile(USER_ID_3).withApplications(APP_ID).buildProfile();
		saveProfiles(profile1, profile2, profile3);

		Buddy buddy = createBuddy(ANONYMOUS_USER_ID, USER_ID_1, TENANT_ID).buildBuddy();
		buddyDao.createOrUpdateBuddy(ANONYMOUS_USER_ID, TENANT_ID, buddy);
		
		Contact contact = createContact(ANONYMOUS_USER_ID, "any_id", TENANT_ID).withUserId(USER_ID_3).buildContact();
		contactDao.createOrUpdateContact(contact);
		
		String requestJson = jsonLoader.getPlainTextJsonFromResources("playerListRequest.json", ANONYMOUS_CLIENT_INFO)
				.replace("<<searchTerm>>", StringUtils.EMPTY);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_LIST_RESPONSE);
		
		JsonMessage<PlayerListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.PLAYER_LIST_RESPONSE);
		assertNotNull(response);
		
		PlayerListResponse content = response.getContent();
		List<ApiPlayerListResult> results = content.getItems();
		assertThat(results, hasSize(3));
		results.forEach(r -> {
			if (USER_ID_1.equals(r.getUserId())) {
				assertEquals(BUDDY, r.getResultType());
				assertNotNull(r.getBuddy());
				assertNull(r.getContact());
				assertNull(r.getProfile());
			} else if (USER_ID_2.equals(r.getUserId())) {
				assertEquals(PLAYER, r.getResultType());
				assertNull(r.getBuddy());
				assertNull(r.getContact());
				assertNotNull(r.getProfile());
			} else if (USER_ID_3.equals(r.getUserId())) {
				assertEquals(CONTACT, r.getResultType());
				assertNull(r.getBuddy());
				assertNotNull(r.getContact());
				assertNull(r.getProfile());
			} else {
				throw new IllegalArgumentException("Invalid result");
			}
		});
	}
	
	@Test
	public void testPlayerLeaderboardList() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).withApplications(APP_ID).buildProfile();
		Profile profile2 = createProfile(USER_ID_2).withApplications(APP_ID).buildProfile();
		Profile profile3 = createProfile(USER_ID_3).withApplications(APP_ID).buildProfile();
		saveProfiles(profile1, profile2, profile3);

		String requestJson = jsonLoader.getPlainTextJsonFromResources("playerListLeaderboardRequest.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_LEADERBOARD_LIST_RESPONSE);
		
		JsonMessage<PlayerLeaderboardListResponse> response = 
				testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.PLAYER_LEADERBOARD_LIST_RESPONSE);
		assertNotNull(response);
		
		PlayerLeaderboardListResponse content = response.getContent();
		List<ApiProfile> results = content.getItems();
		assertThat(results, hasSize(3));
	}
	
	@Test
	public void testLeaderboardPosition() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).withApplications(APP_ID).buildProfile();
		Profile profile2 = createProfile(USER_ID_2).withApplications(APP_ID).buildProfile();
		Profile profile3 = createProfile(USER_ID_3).withApplications(APP_ID).buildProfile();
		saveProfiles(profile1, profile2, profile3);

		String requestJson = jsonLoader.getPlainTextJsonFromResources("leaderboardPositionRequest.json", ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.LEADERBOARD_POSITION_RESPONSE);
		
		JsonMessage<LeaderboardPositionResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.LEADERBOARD_POSITION_RESPONSE);
		LeaderboardPositionResponse content = response.getContent();
		assertNotNull(content);
		
		assertEquals(1, content.getBuddyRank());
		assertEquals(1, content.getBuddyTotal());
		assertEquals(4, content.getGlobalRank());
		assertEquals(4, content.getGlobalTotal());
	}
	
	@Override
	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		return null;
	}
	
	private void saveProfiles(Profile... profiles) {
		Arrays.stream(profiles).forEach(profile -> {
			ProfileUser person = new ProfileUser();
			person.setNickname("Nick");
			person.setFirstName("First");
			person.setLastName("Last");
			profile.setPersonWrapper(person);

			ProfileAddress address = new ProfileAddress();
			address.setCountry("LV");
			address.setCity("Riga");
			profile.setAddress(address);

			String key = profilePrimaryKeyUtil.createPrimaryKey(profile.getUserId());
			String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
			aerospikeDao.createJson(set, key, CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
			profileElasticDao.createOrUpdate(profile);
		});
	}
	
}
