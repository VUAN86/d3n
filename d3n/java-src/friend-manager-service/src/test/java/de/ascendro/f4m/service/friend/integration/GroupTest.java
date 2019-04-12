package de.ascendro.f4m.service.friend.integration;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.dao.GroupAerospikeDAO;
import de.ascendro.f4m.service.friend.dao.GroupAerospikeDAOImpl;
import de.ascendro.f4m.service.friend.model.Group;
import de.ascendro.f4m.service.friend.model.api.ApiGroup;
import de.ascendro.f4m.service.friend.model.api.group.GroupAddPlayersResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupCreateResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupGetResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupListMemberOfResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupListPlayersResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupListResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupRemovePlayersResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupUpdateResponse;
import de.ascendro.f4m.service.friend.model.api.group.PlayerIsGroupMemberResponse;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

/**
 * Test class to test {@link GroupAerospikeDAO} method implementations
 */
public class GroupTest extends FriendManagerTestBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GroupTest.class);
	
	private static String SOURCE_USER_ID = "uid1";
	private static String TARGET_USER_ID = "uid2";
	
	private static String EXISTING_MEMBER_1_ID = "existing_member 1";
	private static String EXISTING_MEMBER_2_ID = "existing_member 2";
	private static String ADDED_MEMBER_1_ID = "added_member 1";
	private static String ADDED_MEMBER_2_ID = "added_member 2";
	
    private AerospikeDao aerospikeDao;
	private GroupAerospikeDAO groupAerospikeDAO;
    private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
    private String profileSet;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		assertServiceStartup(UserMessageMessageTypes.SERVICE_NAME);
		groupAerospikeDAO = jettyServerRule.getServerStartup().getInjector().getInstance(GroupAerospikeDAOImpl.class);
        aerospikeDao = (AerospikeDao) jettyServerRule.getServerStartup().getInjector().getInstance(GroupAerospikeDAO.class);
        profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
        profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
		createMockProfiles();
	}

	private void createMockProfiles() {
		createProfile(EXISTING_MEMBER_1_ID, "Firstname 1");
		createProfile(EXISTING_MEMBER_2_ID, "Firstname 2");
		createProfile(ANONYMOUS_USER_ID, "Firstname 3");
		createProfile(ADDED_MEMBER_1_ID,"Firstname 4", TENANT_ID);
		createProfile(ADDED_MEMBER_2_ID, "Firstname 5");
		createProfile("XXXX", "Firstname 6", TENANT_ID);
	}

	private Profile createProfile(String userId, String firstName, String... tenants) {
		Profile profile = new Profile();
		profile.setUserId(userId);
        Arrays.stream(tenants).forEach(tenant -> profile.addTenant(tenant));
        ProfileAddress address = new ProfileAddress();
        address.setCity("City");
        address.setCountry("DE");
        profile.setAddress(address);
        ProfileUser person = new ProfileUser();
        person.setFirstName(firstName);
        person.setLastName("Lastname");
        profile.setPersonWrapper(person);
        aerospikeDao.createJson(profileSet, profilePrimaryKeyUtil
                .createPrimaryKey(userId), CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
        return profile;
    }

	@Test
	public void testGroupMembersRequests() throws Exception {
        // Create a group
		Map<String, String> existingMembers = new HashMap<>();
		existingMembers.put(EXISTING_MEMBER_1_ID, "1");
		existingMembers.put(EXISTING_MEMBER_2_ID, "2");
		
		Group group = groupAerospikeDAO.createGroup(ANONYMOUS_USER_ID, TENANT_ID, "Awesome group 1", "img", existingMembers);
		String groupId = group.getGroupId();
		Group group2 = groupAerospikeDAO.createGroup(ANONYMOUS_USER_ID, TENANT_ID, "Awesome group 2", "img", Collections.singletonMap(ADDED_MEMBER_1_ID, "3"));
		String group2Id = group2.getGroupId();

		// Add players to group
        final String groupAddPlayersRequest = getPlainTextJsonFromResources("groupAddPlayersRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupAddPlayersRequest.replaceFirst("<<groupId>>", groupId));

        RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
        assertMessageContentType(testClientReceivedMessageCollector.getReceivedMessageList().get(0), GroupAddPlayersResponse.class);
        
        //check notification sent
        assertPlayerAddedNotifications(ADDED_MEMBER_1_ID, ADDED_MEMBER_2_ID);

		// Check both list and individual bins that players were added
		ListResult<Group> groups = groupAerospikeDAO.getGroupsList(ANONYMOUS_USER_ID, TENANT_ID, 10, 0);
		group = groups.getItems().stream().filter(g -> groupId.equals(g.getGroupId())).findAny().get();
        assertEquals(0, group.getMemberUserIds().size());
		assertEquals(4, group.getBuddyCount());

		group = groupAerospikeDAO.getGroup(ANONYMOUS_USER_ID, TENANT_ID, groupId, true);
		Map<String, String> memberUserIds = group.getMemberUserIds();
        assertEquals(4, memberUserIds.size());
		assertEquals(4, group.getBuddyCount());
		assertTrue(memberUserIds.containsKey(EXISTING_MEMBER_1_ID));
		assertTrue(memberUserIds.containsKey(EXISTING_MEMBER_2_ID));
		assertTrue(memberUserIds.containsKey(ADDED_MEMBER_1_ID));
		assertTrue(memberUserIds.containsKey(ADDED_MEMBER_2_ID));

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Block group
		ClientInfo addedMember1ClientInfo = new ClientInfo(TENANT_ID, ADDED_MEMBER_1_ID);
		addedMember1ClientInfo.setRoles(UserRole.ANONYMOUS.name());
        final String groupBlockRequest = getPlainTextJsonFromResources("groupBlockRequest.json", addedMember1ClientInfo);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupBlockRequest.replace("\"<<groupIds>>\"", jsonUtil.toJson(Arrays.asList(groupId))));
		
        assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_BLOCK_RESPONSE);

        JsonMessage<JsonMessageContent> groupBlockResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_BLOCK_RESPONSE);
        assertNull(groupBlockResponse.getError());
        
		// Check both list and individual bins that players were removed
		groups = groupAerospikeDAO.getGroupsList(ANONYMOUS_USER_ID, TENANT_ID, 10, 0);
		group = groups.getItems().stream().filter(g -> groupId.equals(g.getGroupId())).findAny().get();
		assertEquals(3, group.getBuddyCount());

		group = groupAerospikeDAO.getGroup(ANONYMOUS_USER_ID, TENANT_ID, groupId, true);
		memberUserIds = group.getMemberUserIds();
        assertEquals(3, memberUserIds.size());
		assertEquals(3, group.getBuddyCount());
		assertTrue(memberUserIds.containsKey(EXISTING_MEMBER_1_ID));
		assertTrue(memberUserIds.containsKey(EXISTING_MEMBER_2_ID));
		assertTrue(memberUserIds.containsKey(ADDED_MEMBER_2_ID));

		testClientReceivedMessageCollector.clearReceivedMessageList();
        
		// Remove players from group
        final String groupRemovePlayersRequest = getPlainTextJsonFromResources("groupRemovePlayersRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupRemovePlayersRequest.replaceFirst("<<groupId>>", groupId));

        RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
        assertMessageContentType(testClientReceivedMessageCollector.getReceivedMessageList().get(0), GroupRemovePlayersResponse.class);
		
		// Check both list and individual bins that players were removed
		groups = groupAerospikeDAO.getGroupsList(ANONYMOUS_USER_ID, TENANT_ID, 10, 0);
		group = groups.getItems().stream().filter(g -> groupId.equals(g.getGroupId())).findAny().get();
		assertEquals(2, group.getBuddyCount());

		group = groupAerospikeDAO.getGroup(ANONYMOUS_USER_ID, TENANT_ID, groupId, true);
		memberUserIds = group.getMemberUserIds();
        assertEquals(2, memberUserIds.size());
		assertEquals(2, group.getBuddyCount());
		assertTrue(memberUserIds.containsKey(EXISTING_MEMBER_2_ID));
		assertTrue(memberUserIds.containsKey(ADDED_MEMBER_2_ID));

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// List group members
        final String groupListPlayersRequest = getPlainTextJsonFromResources("groupListPlayersRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupListPlayersRequest
        		.replaceAll("\"blocked\": <<blocked>>,", "")
        		.replaceAll("<<groupId>>", groupId)
        		.replaceAll("<<includeProfileInfo>>", "true")
        		.replaceAll("<<limit>>", "10")
        		.replaceAll("<<offset>>", "0"));

        RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
        assertMessageContentType(testClientReceivedMessageCollector.getReceivedMessageList().get(0), GroupListPlayersResponse.class);
        
        GroupListPlayersResponse listResponse = (GroupListPlayersResponse) testClientReceivedMessageCollector.getReceivedMessageList().get(0).getContent();
        assertEquals(3, listResponse.getItems().size());
        // ref #10423 - the group owner should be returned in the list
		assertEquals(EXISTING_MEMBER_2_ID, listResponse.getItems().get(0).getUserId());
		assertEquals(ANONYMOUS_USER_ID, listResponse.getItems().get(1).getUserId());
		assertEquals("Firstname 3 Lastname", listResponse.getItems().get(1).getName());
        assertEquals(ADDED_MEMBER_2_ID, listResponse.getItems().get(2).getUserId());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Is player a member? (yes)
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPlainTextJsonFromResources("playerIsGroupMember.json")
        		.replaceAll("<<groupId>>", groupId)
        		.replaceAll("<<userId>>", ADDED_MEMBER_2_ID));
        
        assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
		JsonMessage<PlayerIsGroupMemberResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
        assertTrue(response.getContent().isUserIsMember());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Is player a member? (no)
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPlainTextJsonFromResources("playerIsGroupMember.json")
        		.replaceAll("<<groupId>>", groupId)
        		.replaceAll("<<userId>>", ADDED_MEMBER_1_ID));
        
        assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
		response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
        assertFalse(response.getContent().isUserIsMember());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Is player a member? (no)
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPlainTextJsonFromResources("playerIsGroupMember.json")
        		.replaceAll("<<groupId>>", groupId)
        		.replaceAll("<<userId>>", "XXXX"));
        
        assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
		response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
        assertFalse(response.getContent().isUserIsMember());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// List group members, with limit and offset, without profile
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupListPlayersRequest
        		.replaceAll("<<groupId>>", groupId)
        		.replaceAll("<<includeProfileInfo>>", "false")
        		.replaceAll("<<limit>>", "1")
        		.replaceAll("<<offset>>", "1"));

        RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
        assertMessageContentType(testClientReceivedMessageCollector.getReceivedMessageList().get(0), GroupListPlayersResponse.class);
        
        listResponse = (GroupListPlayersResponse) testClientReceivedMessageCollector.getReceivedMessageList().get(0).getContent();
        assertEquals(1, listResponse.getItems().size());
        assertEquals(ANONYMOUS_USER_ID, listResponse.getItems().get(0).getUserId());
        assertNull(listResponse.getItems().get(0).getName());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Check member-of list
        final String groupListMemberOf = getPlainTextJsonFromResources("groupListMemberOfRequest.json", addedMember1ClientInfo);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupListMemberOf
        		.replaceAll("\"blocked\": <<blocked>>,", "")
        		.replaceAll("<<limit>>", "10")
        		.replaceAll("<<offset>>", "0"));
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF_RESPONSE);

		JsonMessage<GroupListMemberOfResponse> groupListMemberOfResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF_RESPONSE);
		List<ApiGroup> results = groupListMemberOfResponse.getContent().getItems();
        
		assertEquals(2, results.size());
		assertEquals(groupId, results.get(0).getGroupId());
		assertEquals(group2Id, results.get(1).getGroupId());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		// Check member-of list (blocked)
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupListMemberOf
        		.replaceAll("<<blocked>>", "true")
        		.replaceAll("<<limit>>", "10")
        		.replaceAll("<<offset>>", "0"));
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF_RESPONSE);

		groupListMemberOfResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF_RESPONSE);
		results = groupListMemberOfResponse.getContent().getItems();
        
		assertEquals(1, results.size());
		assertEquals(groupId, results.get(0).getGroupId());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		// Check member-of list (non-blocked)
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupListMemberOf
        		.replaceAll("<<blocked>>", "false")
        		.replaceAll("<<limit>>", "10")
        		.replaceAll("<<offset>>", "0"));
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF_RESPONSE);

		groupListMemberOfResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF_RESPONSE);
		results = groupListMemberOfResponse.getContent().getItems();
        
		assertEquals(1, results.size());
		assertEquals(group2Id, results.get(0).getGroupId());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		// Unblock group
        final String groupUnblockRequest = getPlainTextJsonFromResources("groupUnblockRequest.json", addedMember1ClientInfo);
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupUnblockRequest.replace("\"<<groupIds>>\"", jsonUtil.toJson(Arrays.asList(groupId))));
		
        assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_UNBLOCK_RESPONSE);

        JsonMessage<JsonMessageContent> groupUnblockResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_UNBLOCK_RESPONSE);
        assertNull(groupUnblockResponse.getError());
        
		// Is player a member? (yes)
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPlainTextJsonFromResources("playerIsGroupMember.json")
        		.replaceAll("<<groupId>>", groupId)
        		.replaceAll("<<userId>>", ADDED_MEMBER_1_ID));
        
        assertReceivedMessagesWithWait(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
		response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE);
        assertTrue(response.getContent().isUserIsMember());

		testClientReceivedMessageCollector.clearReceivedMessageList();
    }

	@Test
	public void testGroupRequests() throws IOException, URISyntaxException {
		// Group list should return no results at the beginning
		String groupsListRequest = getPlainTextJsonFromResources("groupListRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupsListRequest.replaceAll("<<offset>>", "0").replaceAll("<<limit>>", "10"));

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> groupListResponse = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(0, ((GroupListResponse) groupListResponse.getContent()).getItems().size());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Create first group
		final String groupCreateRequestJson = getPlainTextJsonFromResources("groupCreateRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);

		String groupCreateRequest = groupCreateRequestJson.replaceFirst("<<name>>", "Awesome group 1");

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupCreateRequest);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_CREATE_RESPONSE);

		JsonMessage<GroupCreateResponse> groupCreateRequestResponse = 
				testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_CREATE_RESPONSE);
		assertMessageContentType(groupCreateRequestResponse, GroupCreateResponse.class);
		assertEquals("Awesome group 1", groupCreateRequestResponse.getContent().getGroup().getName());
		String groupOneId = ((GroupCreateResponse) groupCreateRequestResponse.getContent()).getGroup().getGroupId();
		assertEquals(2, ((GroupCreateResponse) groupCreateRequestResponse.getContent()).getGroup().getBuddyCount());
        
        //check notification sent
        assertPlayerAddedNotifications(ADDED_MEMBER_1_ID, ADDED_MEMBER_2_ID);

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Try creating group with same name
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupCreateRequest);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_CREATE_RESPONSE);
		groupCreateRequestResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_CREATE_RESPONSE);
		assertEquals(ExceptionCodes.ERR_ENTRY_ALREADY_EXISTS, groupCreateRequestResponse.getError().getCode());
        
        //check notification sent
        assertPlayerAddedNotifications();

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// create second group
		groupCreateRequest = groupCreateRequestJson.replaceFirst("<<name>>", "Awesome group 2");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupCreateRequest);

		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_CREATE_RESPONSE);
		groupCreateRequestResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_CREATE_RESPONSE);
		String groupTwoId = ((GroupCreateResponse) groupCreateRequestResponse.getContent()).getGroup().getGroupId();

		assertEquals("Awesome group 2", ((GroupCreateResponse) groupCreateRequestResponse.getContent()).getGroup().getName());
        
        //check notification sent
        assertPlayerAddedNotifications(ADDED_MEMBER_1_ID, ADDED_MEMBER_2_ID);

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// update first group
		String updateGroupRequest = getPlainTextJsonFromResources("groupUpdateRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<groupId>>", groupOneId)
				.replaceFirst("<<name>>", "Awesome Group one edited");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateGroupRequest);

		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_UPDATE_RESPONSE);
		JsonMessage<? extends JsonMessageContent> groupUpdateResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_UPDATE_RESPONSE);

		assertEquals("Awesome Group one edited", ((GroupUpdateResponse) groupUpdateResponse.getContent()).getGroup().getName());
		assertEquals(1, ((GroupUpdateResponse) groupUpdateResponse.getContent()).getGroup().getBuddyCount());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// update with existing name fails
		updateGroupRequest = getPlainTextJsonFromResources("groupUpdateRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<groupId>>", groupOneId)
				.replaceFirst("<<name>>", "Awesome group 2");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), updateGroupRequest);

		assertReceivedMessagesWithWait(FriendManagerMessageTypes.GROUP_UPDATE_RESPONSE);
		groupUpdateResponse = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.GROUP_UPDATE_RESPONSE);
		assertEquals(ExceptionCodes.ERR_ENTRY_ALREADY_EXISTS, groupUpdateResponse.getError().getCode());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// get second group
		String groupGetRequestJson = getPlainTextJsonFromResources("groupGetRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		groupGetRequestJson = groupGetRequestJson.replaceFirst("<<groupId>>", groupTwoId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupGetRequestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));

		final JsonMessage<? extends JsonMessageContent> groupGetResponse = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertMessageContentType(groupGetResponse, GroupGetResponse.class);
		assertEquals("Awesome group 2", ((GroupGetResponse) groupGetResponse.getContent()).getGroup().getName());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Get groups list
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupsListRequest.replaceAll("<<offset>>", "0").replaceAll("<<limit>>", "10"));
		
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		JsonMessage<? extends JsonMessageContent> groupListWithResults = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(2, ((GroupListResponse) groupListWithResults.getContent()).getItems().size());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Get groups list, setting limit
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupsListRequest.replaceAll("<<offset>>", "0").replaceAll("<<limit>>", "1"));
		
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		groupListWithResults = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(1, ((GroupListResponse) groupListWithResults.getContent()).getItems().size());
		assertEquals(groupOneId, ((GroupListResponse) groupListWithResults.getContent()).getItems().get(0).getGroupId());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Get groups list, seting offset
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupsListRequest.replaceAll("<<offset>>", "1").replaceAll("<<limit>>", "10"));
		
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		groupListWithResults = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(1, ((GroupListResponse) groupListWithResults.getContent()).getItems().size());
		assertEquals(groupTwoId, ((GroupListResponse) groupListWithResults.getContent()).getItems().get(0).getGroupId());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		// Delete group
		String groupDeleteRequest = getPlainTextJsonFromResources("groupDeleteRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupDeleteRequest.replaceFirst("<<groupId>>", groupOneId));
		
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		
		testClientReceivedMessageCollector.clearReceivedMessageList();

		// See if it was successfully deleted
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupsListRequest.replaceAll("<<offset>>", "0").replaceAll("<<limit>>", "10"));
		
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		groupListWithResults = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(1, ((GroupListResponse) groupListWithResults.getContent()).getItems().size());
		assertEquals(groupTwoId, ((GroupListResponse) groupListWithResults.getContent()).getItems().get(0).getGroupId());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();

		// used to test leave group functionality :
		Map<String, String> initialMembers = new HashMap<>();
		initialMembers.put(KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO.getUserId(), "User 1");
		// now try with a group not belonging to the user :
		Group createdGroup = groupAerospikeDAO.createGroup("otherUser", TENANT_ID, "other group", "img", initialMembers);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupDeleteRequest.replaceFirst("<<groupId>>", createdGroup.getGroupId()));

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> unsuccessfulGroupDeleteResponse = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(ExceptionCodes.ERR_INSUFFICIENT_RIGHTS, unsuccessfulGroupDeleteResponse.getError().getCode());

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// Leave group
		String groupLeaveRequest = getPlainTextJsonFromResources("groupLeaveRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupLeaveRequest.replaceFirst("<<groupId>>", createdGroup.getGroupId()));
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> successfulGroupLeaveRequest = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertNull(successfulGroupLeaveRequest.getError());

		// check that indeed the user was removed :
		assertFalse(groupAerospikeDAO.isPlayerInGroup(KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO.getUserId(), TENANT_ID, createdGroup.getGroupId()));

		testClientReceivedMessageCollector.clearReceivedMessageList();

		// call leaveGroup with a non-member :
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupLeaveRequest.replaceFirst("<<groupId>>", createdGroup.getGroupId()));
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> unsuccessfulGroupLeaveRequest = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, unsuccessfulGroupLeaveRequest.getError().getCode());
	}

	@Test
	public void testGroupGetNonExistingRequest() throws IOException, URISyntaxException {
		String groupGetRequestJson = getPlainTextJsonFromResources("groupGetRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		groupGetRequestJson = groupGetRequestJson.replaceFirst("<<id>>", "non-existingID");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), groupGetRequestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> groupGetResponse = testClientReceivedMessageCollector.getReceivedMessageList().get(0);

		assertEquals(null, groupGetResponse.getContent());
		assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, groupGetResponse.getError().getCode());
	}

	@Test
	public void testMoveGroupsRequest() throws Exception {
		// Prepare profiles
		String someOtherTenant = "tenant1";
		createProfile(SOURCE_USER_ID, "source user first name", someOtherTenant, TENANT_ID);
		createProfile(TARGET_USER_ID, "target user first name", TENANT_ID);
		
		// Prepare groups
		Map<String, String> members = new HashMap<>();
		members.put(TARGET_USER_ID, "searchString");
		members.put("user3", "searchString");
		groupAerospikeDAO.createGroup(SOURCE_USER_ID, TENANT_ID, "groupName", "image", members); // create group for user 1, containing user 2 and 3
		
		groupAerospikeDAO.createGroup(SOURCE_USER_ID, someOtherTenant, "groupName1", "image1", Collections.singletonMap(SOURCE_USER_ID, "searchString1")); // create group for user 1, containing himself
		Group tenant2Group = groupAerospikeDAO.createGroup(TARGET_USER_ID, TENANT_ID, "groupName2", "image2", Collections.singletonMap(SOURCE_USER_ID, "searchString1")); // create group for user 1, containing user 1

		// Assure correct group contents before test
		assertEquals(1, groupAerospikeDAO.getGroupsList(SOURCE_USER_ID, someOtherTenant, 10, 0).getItems().size());
		assertEquals(1, groupAerospikeDAO.getGroupsList(SOURCE_USER_ID, TENANT_ID, 10, 0).getItems().size());
		assertEquals(0, groupAerospikeDAO.getGroupsList(TARGET_USER_ID, someOtherTenant, 10, 0).getItems().size());
		assertEquals(1, groupAerospikeDAO.getGroupsList(TARGET_USER_ID, TENANT_ID, 10, 0).getItems().size());
		
		// Act
		String moveGroupsRequest = getPlainTextJsonFromResources("moveRequest.json")
				.replace("<<entity>>", "Groups")
				.replace("<<sourceUserId>>", SOURCE_USER_ID)
				.replace("<<targetUserId>>", TARGET_USER_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), moveGroupsRequest);
		
		// Verify message
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.MOVE_GROUPS_RESPONSE);
		
		// Verify no more groups for source user
		assertEquals(0, groupAerospikeDAO.getGroupsList(SOURCE_USER_ID, someOtherTenant, 10, 0).getItems().size());
		
		// Verify moved group 1
		List<Group> groups = groupAerospikeDAO.getGroupsList(TARGET_USER_ID, someOtherTenant, 10, 0).getItems();
		assertEquals(1, groups.size());
		assertEquals(TARGET_USER_ID, groups.get(0).getUserId());
		
		Group group = groupAerospikeDAO.getGroup(TARGET_USER_ID, someOtherTenant, groups.get(0).getGroupId(), true);
		assertEquals(1, group.getMemberUserIds().size());
		assertTrue(group.getMemberUserIds().containsKey(TARGET_USER_ID));
		
		// Verify moved groups
		groups = groupAerospikeDAO.getGroupsList(TARGET_USER_ID, TENANT_ID, 10, 0).getItems();
		assertEquals(2, groups.size());
		assertEquals(TARGET_USER_ID, groups.get(0).getUserId());
		assertEquals(TARGET_USER_ID, groups.get(1).getUserId());
		
		// Verify moved group contents
		String movedGroup2Id = groups.get(0).getGroupId().equals(tenant2Group.getGroupId()) ? groups.get(1).getGroupId() : groups.get(0).getGroupId();
		group = groupAerospikeDAO.getGroup(TARGET_USER_ID, TENANT_ID, movedGroup2Id, true);
		assertEquals(2, group.getMemberUserIds().size());
		assertTrue(group.getMemberUserIds().containsKey(TARGET_USER_ID));
		assertTrue(group.getMemberUserIds().containsKey("user3"));

		// Verify updated user2 group
		String user2UpdatedGroup3Id = groups.get(0).getGroupId().equals(tenant2Group.getGroupId()) ? groups.get(0).getGroupId() : groups.get(1).getGroupId();
		group = groupAerospikeDAO.getGroup(TARGET_USER_ID, TENANT_ID, user2UpdatedGroup3Id, true);
		assertEquals(1, group.getMemberUserIds().size());
		assertTrue(group.getMemberUserIds().containsKey(TARGET_USER_ID));
	}

	@Override
	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> message) throws Exception {
		JsonMessageContent response;
		if (UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE == message.getType(UserMessageMessageTypes.class)) { 
			response = null;
		} else {
			LOGGER.error("Mocked Service received unexpected message [{}]", message);
			throw new UnexpectedTestException("Unexpected message");
		}
		return response;
	}

}
