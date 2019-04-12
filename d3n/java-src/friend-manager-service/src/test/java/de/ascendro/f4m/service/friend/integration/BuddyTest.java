package de.ascendro.f4m.service.friend.integration;

import static de.ascendro.f4m.service.friend.builder.BuddyBuilder.createBuddy;
import static de.ascendro.f4m.service.friend.builder.BuddyListRequestBuilder.createBuddyListRequest;
import static de.ascendro.f4m.service.friend.builder.ProfileBuilder.createProfile;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.dao.BuddyAerospikeDao;
import de.ascendro.f4m.service.friend.dao.BuddyElasticDao;
import de.ascendro.f4m.service.friend.dao.GroupAerospikeDAO;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.Group;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.buddy.ApiBuddyListResult;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyLeaderboardListResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListResponse;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class BuddyTest extends FriendManagerTestBase {
	
	private static final int ELASTIC_PORT = 9205;
	
	public static final String USER_ID_1 = "user_id_1";
	public static final String USER_ID_2 = "user_id_2";
	public static final String USER_ID_3 = "user_id_3";

	private BuddyAerospikeDao buddyDao;
	private GroupAerospikeDAO groupDao;
	private BuddyElasticDao buddyElasticDao;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	private CommonProfileElasticDao profileElasticDao;
	private CommonProfileAerospikeDao profileAerospikeDao;
	
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
		groupDao = injector.getInstance(GroupAerospikeDAO.class);
		buddyElasticDao = injector.getInstance(BuddyElasticDao.class);
		profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
		profileElasticDao = injector.getInstance(CommonProfileElasticDao.class);
		profileAerospikeDao = injector.getInstance(CommonProfileAerospikeDao.class);
	}
	
	@Test
	public void testBuddyAdd() throws Exception {
		String requestJson = jsonLoader.getPlainTextJsonFromResources("buddyAddRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("\"<<userIds>>\"", jsonUtil.toJson(Arrays.asList(USER_ID_1, USER_ID_2)))
				.replace("\"<<favorite>>\"", "true");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_ADD_RESPONSE);
		
		assertBuddies(true, ANONYMOUS_USER_ID, USER_ID_1, USER_ID_2);
	}
	
	@Test
	public void testBuddyAddForUser() throws Exception {
		final ClientInfo clientInfo3 = ClientInfo.cloneOf(KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		clientInfo3.setUserId(USER_ID_3);
		String requestJson = jsonLoader.getPlainTextJsonFromResources("buddyAddForUserRequest.json", clientInfo3)
				.replace("<<userId>>", USER_ID_3)
				.replace("\"<<userIds>>\"", jsonUtil.toJson(Arrays.asList(USER_ID_1, USER_ID_2)));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER_RESPONSE);
		
		assertBuddies(false, USER_ID_3, USER_ID_1, USER_ID_2);
	}
	
	private void assertBuddies(Boolean favorite, String userId, String... userIds) throws Exception {
		List<Buddy> buddies = buddyDao.getBuddies(userId, Arrays.asList(userIds));
		assertEquals(userIds.length, buddies.size());
		
		String[] userIdsFromAerospike = buddies.stream().map(b -> b.getUserId()).toArray(size -> new String[size]);
		assertThat(userIds, arrayContainingInAnyOrder(userIdsFromAerospike));
		buddies.forEach(buddy -> assertEquals(favorite, buddy.isFavorite()));
	}
	
	@Test
	public void testBuddyRemove() throws Exception {
		saveProfiles(createProfile(ANONYMOUS_USER_ID).withTenants(TENANT_ID).buildProfile());

		createProfile(USER_ID_1).buildProfile();
		createProfile(USER_ID_2).buildProfile();
		Buddy buddy1 = createBuddy(ANONYMOUS_USER_ID, USER_ID_1, TENANT_ID).buildBuddy();
		Buddy buddy2 = createBuddy(ANONYMOUS_USER_ID, USER_ID_2, TENANT_ID).buildBuddy();
		saveBuddies(ANONYMOUS_USER_ID, buddy1, buddy2);

		// Create a group
		Map<String, String> existingMembers = new HashMap<>();
		existingMembers.put(USER_ID_1, "1");
		existingMembers.put(USER_ID_2, "2");
		Group group = groupDao.createGroup(ANONYMOUS_USER_ID, TENANT_ID, "Awesome group 1", "img", existingMembers);
		String groupId = group.getGroupId();

		profileAerospikeDao.setLastInvitedDuelOponents(ANONYMOUS_USER_ID, "appId1", Arrays.asList(USER_ID_1, USER_ID_2));
		profileAerospikeDao.setPausedDuelOponents(ANONYMOUS_USER_ID, "appId1", Arrays.asList(USER_ID_1, USER_ID_2));
		profileAerospikeDao.setLastInvitedDuelOponents(ANONYMOUS_USER_ID, "appId2", Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3	));
		profileAerospikeDao.setPausedDuelOponents(ANONYMOUS_USER_ID, "appId2", Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3	));

		String requestJson = jsonLoader.getPlainTextJsonFromResources("buddyRemoveRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("\"<<userIds>>\"", jsonUtil.toJson(Arrays.asList(USER_ID_1)));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_REMOVE_RESPONSE);
		
		Buddy buddy1After = buddyDao.getBuddies(ANONYMOUS_USER_ID, Arrays.asList(USER_ID_1)).get(0);
		assertTrue(buddy1After.getRelationTypes().isEmpty());
		Buddy buddy2After = buddyDao.getBuddies(ANONYMOUS_USER_ID, Arrays.asList(USER_ID_2)).get(0);
		assertFalse(buddy2After.getRelationTypes().isEmpty());

		Group groupAfter = groupDao.getGroup(ANONYMOUS_USER_ID, TENANT_ID, groupId, false);
		assertThat(groupAfter.getMemberUserIds().keySet(), hasSize(1));
		assertThat(groupAfter.getMemberUserIds().keySet(), contains(USER_ID_2));

		List<String> invitedDuelOpponents1 = profileAerospikeDao.getLastInvitedDuelOpponents(ANONYMOUS_USER_ID, "appId1");
		assertThat(invitedDuelOpponents1, hasSize(1));
		assertThat(invitedDuelOpponents1, contains(USER_ID_2));
		List<String> pausedDuelOpponents1 = profileAerospikeDao.getPausedDuelOpponents(ANONYMOUS_USER_ID, "appId1");
		assertThat(pausedDuelOpponents1, hasSize(1));
		assertThat(pausedDuelOpponents1, contains(USER_ID_2));
		List<String> invitedDuelOpponents2 = profileAerospikeDao.getLastInvitedDuelOpponents(ANONYMOUS_USER_ID, "appId2");
		assertThat(invitedDuelOpponents2, hasSize(2));
		assertThat(invitedDuelOpponents2, containsInAnyOrder(USER_ID_2, USER_ID_3));
		List<String> pausedDuelOpponents2 = profileAerospikeDao.getPausedDuelOpponents(ANONYMOUS_USER_ID, "appId2");
		assertThat(pausedDuelOpponents2, hasSize(2));
		assertThat(pausedDuelOpponents2, containsInAnyOrder(USER_ID_2, USER_ID_3));
	}
	
	@Test
	public void testEmptyBuddyList() throws Exception {
		String requestJson = createBuddyListRequest().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);

		JsonMessage<BuddyListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		BuddyListResponse content = response.getContent();
		assertNotNull(content);
		assertTrue(content.getItems().isEmpty());
	}

	@Test
	public void testBuddyList() throws Exception {
		List<Buddy> buddies = prepareBuddies();
		
		String requestJson = createBuddyListRequest()
				.withIncludedRelationTypes(BuddyRelationType.BUDDY)
				.withExcludedRelationTypes(BuddyRelationType.BLOCKED)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		
		JsonMessage<BuddyListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		List<ApiBuddyListResult> results = response.getContent().getItems();
		assertEquals(buddies.size(), results.size());
		assertThat(results.stream().map(b -> b.getBuddy().getUserId()).toArray(size -> new String[size]),
				arrayContainingInAnyOrder(USER_ID_1, USER_ID_2));
	}
	
	@Test
	public void testBuddyOfABuddyList() throws Exception {
		prepareBuddies();
		Buddy buddy = createBuddy(USER_ID_1, USER_ID_2, TENANT_ID).buildBuddy();
		saveBuddies(USER_ID_1, buddy);
		
		String requestJson = createBuddyListRequest()
				.withIncludedRelationTypes(BuddyRelationType.BUDDY)
				.withExcludedRelationTypes(BuddyRelationType.BLOCKED)
				.withUserId(USER_ID_1)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		
		JsonMessage<BuddyListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		List<ApiBuddyListResult> results = response.getContent().getItems();
		assertEquals(1, results.size());
		assertThat(results.stream().map(b -> b.getBuddy().getUserId()).toArray(size -> new String[size]),
				arrayContainingInAnyOrder(USER_ID_2));
	}
	
	@Test
	public void testBuddyOfABuddyList_NotABuddy() throws Exception {
		prepareBuddies();
		Buddy buddy = createBuddy("anyid", USER_ID_2, TENANT_ID).buildBuddy();
		saveBuddies("anyid", buddy);
		
		String requestJson = createBuddyListRequest()
				.withIncludedRelationTypes(BuddyRelationType.BUDDY)
				.withExcludedRelationTypes(BuddyRelationType.BLOCKED)
				.withUserId("anyid")
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		
		JsonMessage<BuddyListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		assertEquals(ExceptionCodes.ERR_INSUFFICIENT_RIGHTS, response.getError().getCode());
	}
	
	@Test
	public void testBuddyList_Favorite() throws Exception {
		prepareBuddies();
	
		String requestJson = createBuddyListRequest()
				.withIncludedRelationTypes(BuddyRelationType.BUDDY)
				.withExcludedRelationTypes(BuddyRelationType.BLOCKED)
				.withFavorite(true)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		
		JsonMessage<BuddyListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		List<ApiBuddyListResult> results = response.getContent().getItems();
		assertEquals(1, results.size());
		assertThat(results.stream().map(b -> b.getBuddy().getUserId()).toArray(size -> new String[size]),
				arrayContainingInAnyOrder(USER_ID_2));
	}

	@Test
	public void testBuddyList_NonFavorite() throws Exception {
		prepareBuddies();
		
		String requestJson = createBuddyListRequest()
				.withIncludedRelationTypes(BuddyRelationType.BUDDY)
				.withExcludedRelationTypes(BuddyRelationType.BLOCKED)
				.withFavorite(false)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		
		JsonMessage<BuddyListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE);
		List<ApiBuddyListResult> results = response.getContent().getItems();
		assertEquals(1, results.size());
		assertThat(results.stream().map(b -> b.getBuddy().getUserId()).toArray(size -> new String[size]),
				arrayContainingInAnyOrder(USER_ID_1));
	}
	
	@Test
	public void testBuddyLeaderboardList() throws Exception {
		List<Buddy> buddies = prepareBuddies();
		saveProfiles(createProfile(ANONYMOUS_USER_ID).withTenants(TENANT_ID).buildProfile());
		
		String requestJson = jsonLoader.getPlainTextJsonFromResources("buddyListLeaderboardRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LEADERBOARD_LIST_RESPONSE);
		
		JsonMessage<BuddyLeaderboardListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LEADERBOARD_LIST_RESPONSE);
		BuddyLeaderboardListResponse content = response.getContent();
		assertNotNull(content);
		
		List<ApiProfile> results = content.getItems();
		assertEquals(buddies.size() + 1, results.size()); // owner included
		assertThat(results.stream().map(b -> b.getUserId()).toArray(size -> new String[size]),
				arrayContainingInAnyOrder(ANONYMOUS_USER_ID, USER_ID_1, USER_ID_2));
	}
	
	@Test
	public void testBuddyListAllIds() throws Exception {
		List<Buddy> buddies = prepareBuddies();
		
		String requestJson = jsonLoader.getPlainTextJsonFromResources("buddyListAllIdsRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<userId>>", ANONYMOUS_USER_ID)
				.replace("\"<<includedRelationTypes>>\"", jsonUtil.toJson(Arrays.asList(BuddyRelationType.BUDDY)))
				.replace("\"<<excludedRelationTypes>>\"", jsonUtil.toJson(Arrays.asList(BuddyRelationType.BLOCKED)));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.BUDDY_LIST_ALL_IDS_RESPONSE);
		
		JsonMessage<BuddyListAllIdsResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_LIST_ALL_IDS_RESPONSE);
		BuddyListAllIdsResponse content = response.getContent();
		assertNotNull(content);
		
		String[] userIds = content.getUserIds();
		assertEquals(buddies.size(), userIds.length);
		assertThat(buddies.stream().map(b -> b.getUserId()).toArray(size -> new String[size]), arrayContainingInAnyOrder(userIds));
	}
	
	@Test
	public void testMoveBuddies() throws Exception {
		// Prepare profiles
		saveProfiles(
			createProfile(USER_ID_1).withTenants(TENANT_ID).buildProfile(),
			createProfile(USER_ID_2).withTenants(TENANT_ID).buildProfile(),
			createProfile(USER_ID_3).withTenants(TENANT_ID).buildProfile()
		);
		
		// Prepare buddies
		Buddy buddy1_1 = createBuddy(USER_ID_1, USER_ID_3, TENANT_ID).buildBuddy();
		Buddy buddy1_2 = createBuddy(USER_ID_1, USER_ID_2, TENANT_ID).buildBuddy(); // reference to user2
		saveBuddies(USER_ID_1, buddy1_1, buddy1_2);
		Buddy buddy2 = createBuddy(USER_ID_2, USER_ID_1, TENANT_ID).buildBuddy(); // will have reference to user1
		saveBuddies(USER_ID_2, buddy2);
		
		// Assure correct buddy lists
		assertEquals(2, buddyElasticDao.getBuddyList(USER_ID_1, null, TENANT_ID, null, null, null, null, null, null, 10, 0).getSize());
		assertEquals(1, buddyElasticDao.getBuddyList(USER_ID_2, null, TENANT_ID, null, null, null, null, null, null, 10, 0).getSize());
		assertEquals(2, buddyDao.getBuddies(USER_ID_1, Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3)).size());
		assertEquals(1, buddyDao.getBuddies(USER_ID_2, Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3)).size());
		
		// Act
		String moveContactsRequest = getPlainTextJsonFromResources("moveRequest.json")
				.replace("<<entity>>", "Buddies")
				.replace("<<sourceUserId>>", USER_ID_1)
				.replace("<<targetUserId>>", USER_ID_2);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), moveContactsRequest);
		
		// Verify message
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.MOVE_BUDDIES_RESPONSE);
		
		// Verify buddies
		assertEquals(0, buddyElasticDao.getBuddyList(USER_ID_1, null, TENANT_ID, null, null, null, null, null, null, 10, 0).getSize());
		List<Buddy> newElasticBuddies = buddyElasticDao.getBuddyList(USER_ID_2, null, TENANT_ID, null, null, null, null, null, null, 10, 0).getItems();
		assertEquals(2, newElasticBuddies.size());
		
		assertEquals(0, buddyDao.getBuddies(USER_ID_1, Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3)).size());
		List<Buddy> newBuddies = buddyDao.getBuddies(USER_ID_2, Arrays.asList(USER_ID_1, USER_ID_2, USER_ID_3));
		assertEquals(2, newBuddies.size());
		assertTrue(newBuddies.stream().anyMatch(b -> b.getUserId().equals(USER_ID_2)));
		assertTrue(newBuddies.stream().anyMatch(b -> b.getUserId().equals(USER_ID_3)));
	}

	@Override
	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		return null;
	}
	
	private List<Buddy> prepareBuddies() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).withApplications(APP_ID).buildProfile();
		Profile profile2 = createProfile(USER_ID_2).withApplications(APP_ID).buildProfile();
		Profile profile3 = createProfile(USER_ID_3).withApplications(APP_ID).buildProfile();
		saveProfiles(profile1, profile2, profile3);
		
		Buddy buddy1 = createBuddy(ANONYMOUS_USER_ID, USER_ID_1, TENANT_ID).withFavorite(false).buildBuddy();
		Buddy buddy2 = createBuddy(ANONYMOUS_USER_ID, USER_ID_2, TENANT_ID).withFavorite(true).buildBuddy();
		Buddy buddy3 = createBuddy("some_user_id", USER_ID_3, TENANT_ID).buildBuddy();
		saveBuddies(ANONYMOUS_USER_ID, buddy1, buddy2, buddy3);
		
		// returns buddies for user ANONYMOUS_USER_ID
		return Arrays.asList(buddy1, buddy2);
	}
	
	private void saveBuddies(String userId, Buddy... buddies) {
		Arrays.stream(buddies).forEach(b -> buddyDao.createOrUpdateBuddy(userId, TENANT_ID, b));
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
