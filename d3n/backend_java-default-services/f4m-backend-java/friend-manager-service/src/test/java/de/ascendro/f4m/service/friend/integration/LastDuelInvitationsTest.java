package de.ascendro.f4m.service.friend.integration;

import static de.ascendro.f4m.service.friend.builder.ProfileBuilder.createProfile;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.friend.model.api.LastDuelInvitationListResponse;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class LastDuelInvitationsTest extends FriendManagerTestBase {

	public static final String USER_ID_1 = "user_id_1";
	public static final String USER_ID_2 = "user_id_2";
	public static final String USER_ID_3 = "user_id_3";
	public static final String USER_ID_4 = "user_id_4";

	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	private CommonProfileAerospikeDao profileAerospikeDao;

	@BeforeClass
	public static void setUpClass() {
		F4MServiceWithMockIntegrationTestBase.setUpClass();
	}
	

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
		profileAerospikeDao = injector.getInstance(CommonProfileAerospikeDao.class);
	}

	@Test
	public void testLastDuelInvitationList() throws Exception {
		Profile profile = createProfile(ANONYMOUS_USER_ID).withApplications(APP_ID).buildProfile();
		Profile profile1 = createProfile(USER_ID_1).withApplications(APP_ID).buildProfile();
		Profile profile2 = createProfile(USER_ID_2).withApplications(APP_ID).buildProfile();
		Profile profile3 = createProfile(USER_ID_3).withApplications(APP_ID).buildProfile();
		Profile profile4 = createProfile(USER_ID_4).withApplications(APP_ID).buildProfile();
		saveProfiles(profile, profile1, profile2, profile3, profile4);
		final List<String> lastOpponents = Arrays.asList(USER_ID_1, USER_ID_2);
		final List<String> pausedOpponents = Arrays.asList(USER_ID_3, USER_ID_4);
		profileAerospikeDao.setLastInvitedDuelOponents(ANONYMOUS_USER_ID, APP_ID, lastOpponents);
		profileAerospikeDao.setPausedDuelOponents(ANONYMOUS_USER_ID, APP_ID, pausedOpponents);

		String requestJson = getPlainTextJsonFromResources("emptyContentRequest.json",
				KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO).replaceFirst("<<messageName>>", "friend/lastDuelInvitationList");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.LAST_DUEL_INVITATION_LIST_RESPONSE);
		
		JsonMessage<LastDuelInvitationListResponse> response = testClientReceivedMessageCollector.getMessageByType(
				FriendManagerMessageTypes.LAST_DUEL_INVITATION_LIST_RESPONSE);
		assertNotNull(response);
		
		LastDuelInvitationListResponse content = response.getContent();
		List<ApiProfile> invitedUsers = content.getInvitedUsers();
		assertThat(invitedUsers, hasSize(2));
		assertThat(invitedUsers.stream().map(ApiProfile::getUserId).collect(Collectors.toList()),
				containsInAnyOrder(lastOpponents.toArray()));
		List<ApiProfile> pausedUsers = content.getPausedUsers();
		assertThat(pausedUsers, hasSize(2));
		assertThat(pausedUsers.stream().map(ApiProfile::getUserId).collect(Collectors.toList()),
				containsInAnyOrder(pausedOpponents.toArray()));
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
		});
	}
	
}
