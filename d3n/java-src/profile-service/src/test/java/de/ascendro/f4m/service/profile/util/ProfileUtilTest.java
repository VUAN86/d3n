package de.ascendro.f4m.service.profile.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.profile.config.ProfileConfig;
import de.ascendro.f4m.service.profile.dao.ProfileAerospikeDao;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.api.ApiProfilePerson;
import de.ascendro.f4m.service.profile.model.api.ApiUpdateableProfile;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;
import de.ascendro.f4m.service.util.ServiceUtil;

public class ProfileUtilTest {
	@Mock
	private ProfileAerospikeDao profileAerospikeDao;

	@Mock
	private EventServiceClient eventServiceClient;

	private JsonMessageUtil jsonUtil;

	@Mock
	private Config config;

	@Mock
	private ServiceUtil serviceUtil;

	@Mock
	private JsonMessageValidator jsonMessageValidator;

	@Mock
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;

	@Mock
	private CommonProfileAerospikeDao commonProfileAerospikeDao;
	
	private ProfileUtil profileUtil;
	private Gson gson;

	@Mock
	private GsonProvider gsonProvider;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		gson = JsonTestUtil.getGson();
		when(gsonProvider.get()).thenReturn(gson);

		final Config config = new ProfileConfig();
		
		jsonUtil = new JsonMessageUtil(gsonProvider, serviceUtil, jsonMessageValidator);
		profileUtil = new ProfileUtil(config, profileAerospikeDao, eventServiceClient, jsonUtil, profilePrimaryKeyUtil,
				commonProfileAerospikeDao);
	}

	
	
	@Test
	public void testMergeProfiles() throws Exception {
		final String sourceJsonString = getJson("sourceProfile.json");
		final Profile sourceProfile = new Profile(gson.fromJson(sourceJsonString, JsonElement.class));
		//FIXME: mock aerospikeDao instead of using spy!
		//when(profileUtil.getProfile("5555")).thenReturn(sourceProfile);
		when(profileAerospikeDao.getActiveProfile("5555")).thenReturn(sourceProfile);

		final String targetJsonString = getJson("targetProfile.json");
		final Profile targetProfile = new Profile(gson.fromJson(targetJsonString, JsonElement.class));
		//when(profileUtil.getProfile("8888")).thenReturn(targetProfile);
		when(profileAerospikeDao.getActiveProfile("8888")).thenReturn(targetProfile);

		profileUtil.mergeProfiles(sourceProfile, targetProfile);

		assertNotNull(targetProfile);

		final JsonObject targetJsonObject = targetProfile.getJsonObject();
		assertEquals("8888", targetJsonObject.get("userId").getAsString());

		assertEquals(4, targetJsonObject.get("roles").getAsJsonArray().size());

		assertEquals("G-TEST_TOKEN1", targetJsonObject.get("google").getAsString());
		assertEquals("FB-TEST_TOKEN1", targetJsonObject.get("facebook").getAsString());

		assertEquals(1, targetJsonObject.get("devices").getAsJsonArray().size());

		assertEquals(2, targetJsonObject.get("emails").getAsJsonArray().size());

		assertEquals("Test street0", targetJsonObject.get("address").getAsJsonObject().get("street").getAsString());
		assertEquals("56A", targetJsonObject.get("address").getAsJsonObject().get("streetNumber").getAsString());
		
		assertEquals(5.5, targetJsonObject.get("handicap").getAsDouble(), 0);
	}

	private String getJson(String name) throws IOException {
		return IOUtils.toString(this.getClass().getResourceAsStream(name), "UTF-8");
	}

	@Test
	public void testChangingOfNonupdateableProperties() throws Exception {
		Profile existing = getProfileJson();
		Profile changed = getProfileJson();
		profileUtil.verifyThatNonUpdateablePropertiesAreNotChanged(existing, changed);

		changed.setProperty(Profile.USER_ID_PROPERTY, "different");
		try {
			profileUtil.verifyThatNonUpdateablePropertiesAreNotChanged(existing, changed);
			fail();
		} catch (F4MValidationFailedException e) {
			// expected
		}

		// This should not fail, since not changing anything
		changed = new Profile();
		profileUtil.verifyThatNonUpdateablePropertiesAreNotChanged(existing, changed);
	}

	@Test
	public void testChangingOfUserNonupdateableProperties() throws Exception {
		Profile existing = getProfileJson();
		Profile changed = getProfileJson();
		profileUtil.verifyThatNonUpdateablePropertiesByUserAreNotChanged(existing, changed);

		changed.setProperty(Profile.USER_ID_PROPERTY, "different");
		changed.setArray(Profile.ROLES_PROPERTY, "COMMUNITY", "ANONYMOUS", "REGISTERED"); //same roles as defined
		profileUtil.verifyThatNonUpdateablePropertiesByUserAreNotChanged(existing, changed);
		
		changed.setArray(Profile.ROLES_PROPERTY, "ANONYMOUS", "REGISTERED", "COMMUNITY"); //same roles as defined, different order
		expectVerificationToFail(existing, changed);
		
		changed.setArray(Profile.ROLES_PROPERTY, "FULLY_REGISTERED");
		expectVerificationToFail(existing, changed);
		changed.getJsonObject().remove(Profile.ROLES_PROPERTY);
		profileUtil.verifyThatNonUpdateablePropertiesByUserAreNotChanged(existing, changed);

		// This should not fail, since not changing anything
		changed = new Profile();
		profileUtil.verifyThatNonUpdateablePropertiesByUserAreNotChanged(existing, changed);
	}

	@Test
	public void testCheckForExistingNickname() throws IOException {
		Profile existing = getProfileJson();

		final String firstName = existing.getPersonWrapper().getFirstName();
		final String lastName = existing.getPersonWrapper().getLastName();
		final String nickname = firstName + lastName;
		existing.getPersonWrapper().setNickname(nickname);

		when(profileAerospikeDao.findUserIdByIdentifierWithCleanup(ProfileIdentifierType.NICKNAME,
				firstName + lastName)).thenReturn("anotherUser");

		profileUtil.checkAndGenerateNickname(existing.getUserId(), existing, existing);
		assertTrue(existing.getPersonWrapper().getNickname().equals(nickname));
	}

	/**
	 * Test that nickname is not generated, when it is added
	 */
	@Test
	public void testUpdateNicknameNotExists() throws IOException {
		Profile existing = getProfileJson();
		final String nickname = "MyUniqueNickname";
		existing.getPersonWrapper().setNickname(nickname);
		profileUtil.checkAndGenerateNickname(existing.getUserId(), existing, existing);
		assertEquals(nickname, existing.getPersonWrapper().getNickname());
	}	
	
	/**
	 * Test that nickname is not generated, if it already exists
	 */
	@Test
	public void testNoUpdateNicknameExists() throws IOException {
		Profile existing = getProfileJson();
		final String nickname = "MyUniqueNickname";
		existing.getPersonWrapper().setNickname(nickname);
		profileUtil.checkAndGenerateNickname(existing.getUserId(), existing, existing);
		assertEquals(nickname, existing.getPersonWrapper().getNickname());
	}	

	
	/**
	 * Tests if exception is thrown on too many attempts
	 * */
	@Test(expected=F4MFatalErrorException.class)
	public void testTooManyAttemtpsToGenerate() throws IOException {
		Profile existing = getProfileJson();
		existing.getPersonWrapper().setNickname(null);

		final String firstName = existing.getPersonWrapper().getFirstName();
		final String lastName = existing.getPersonWrapper().getLastName();
		ProfileUser person = existing.getPersonWrapper();
		person.setFirstName(firstName);
		person.setLastName(lastName);

		when(profileAerospikeDao.findUserIdByIdentifierWithCleanup(ProfileIdentifierType.NICKNAME,
				firstName + lastName )).thenReturn("anotherUser");
		
		// fill all possible variants with mock
		for (int i = 0; i < 101; i++) {
			when(profileAerospikeDao.findUserIdByIdentifierWithCleanup(ProfileIdentifierType.NICKNAME,
					firstName + lastName + i)).thenReturn("anotherUser" + i);
		}

		profileUtil.checkAndGenerateNickname(existing.getUserId(), existing,existing);
	}
	
	/**
	 * If there is no name and surname, then no nickname is generated.
	 * */
	@Test
	public void testNoNameNoNickname() throws IOException {
		ProfileUser profileUser = new ProfileUser();
		ApiProfilePerson person = new ApiProfilePerson(profileUser);
		ApiUpdateableProfile apiUpdateableProfile = new ApiUpdateableProfile(person, null, null);
		Profile profile = apiUpdateableProfile.toProfile();

		profileUtil.checkAndGenerateNickname(profile.getUserId(), profile,profile);
		assertEquals(null, profile.getPersonWrapper().getNickname());
	}
	
	/**
	 * Test if nickname is changed if there is new surname
	 * */
	@Test
	public void testNicknameUpdateNewSurname() throws IOException {
		
		ProfileUser changedUser = new ProfileUser();
		changedUser.setFirstName("Changed");
		Profile profile = new ApiUpdateableProfile(new ApiProfilePerson(changedUser), null, null).toProfile();
		
		ProfileUser actualDbUser = new ProfileUser();
		actualDbUser.setFirstName("MyName");
		actualDbUser.setLastName("MySurname");
		ApiProfilePerson anotherPerson = new ApiProfilePerson(actualDbUser);
		ApiUpdateableProfile actualDbUserProfile = new ApiUpdateableProfile(anotherPerson, null, null);
		
		profileUtil.checkAndGenerateNickname(actualDbUserProfile.toProfile().getUserId(), profile,actualDbUserProfile.toProfile());
		
		assertEquals("ChangedMySurname", profile.getPersonWrapper().getNickname());
		
	}
	
	@Test
	public void testgetFirstNameLastNameChangedFirstname() throws IOException {
		ProfileUser changedUser = new ProfileUser();
		changedUser.setFirstName("Changed");
		Profile profile = new ApiUpdateableProfile(new ApiProfilePerson(changedUser), null, null).toProfile();
		String firstName = profileUtil.getProfileUserField(profile, generateActualDbUserProfile(),(input) -> input.getFirstName() );
		assertEquals("Changed", firstName);
	}

	@Test
	public void testgetFirstNameLastNameChangedSurname() throws IOException {
		ProfileUser changedUser = new ProfileUser();
		changedUser.setLastName("Changed");
		Profile profile = new ApiUpdateableProfile(new ApiProfilePerson(changedUser), null, null).toProfile();
		String lastName = profileUtil.getProfileUserField(profile, generateActualDbUserProfile(), (input) -> input.getLastName());
		assertEquals("Changed", lastName);
	}

	@Test
	public void testgetFirstNameLastNameChangedNickName() throws IOException {
		ProfileUser changedUser = new ProfileUser();
		changedUser.setNickname("Changed");
		Profile profile = new ApiUpdateableProfile(new ApiProfilePerson(changedUser), null, null).toProfile();
		String nickname = profileUtil.getProfileUserField(profile, generateActualDbUserProfile(), (input) -> input.getNickname());
		assertEquals("Changed", nickname);
	}
	
	
	/**
	 * Tests if checking for not existing profile wrapper works.
	 */
	@Test
	public void testNullActualDbProfile() throws IOException {
		ProfileUser changedUser = new ProfileUser();
		Profile profile = new ApiUpdateableProfile(new ApiProfilePerson(changedUser), null, null).toProfile();
		String lastName = profileUtil.getProfileUserField(profile,new Profile(),(input) -> input.getLastName() );
		assertEquals(null, lastName);
	}
	
	@Test
	public void testGenerateNickname() {
		String firstName = "first";
		String lastName = "second";
		String nickName = ProfileUtil.generateNickname(firstName, lastName);
		// first name + last name + random number [1..99] if it is taken
		assertTrue(nickName.matches(
				StringUtils.removeAll(firstName, "\\s") + StringUtils.removeAll(lastName, "\\s")) );

	}

	private Profile generateActualDbUserProfile(){
		ProfileUser actualDbUser = new ProfileUser();
		actualDbUser.setFirstName("MyName");
		actualDbUser.setLastName("MySurname");
		ApiProfilePerson anotherPerson = new ApiProfilePerson(actualDbUser);
		ApiUpdateableProfile actualDbUserProfile = new ApiUpdateableProfile(anotherPerson, null, null);
		return actualDbUserProfile.toProfile();
	}

	
	private void expectVerificationToFail(Profile existing, Profile changed) {
		try {
			profileUtil.verifyThatNonUpdateablePropertiesByUserAreNotChanged(existing, changed);
			fail();
		} catch (F4MValidationFailedException e) {
			// expected
		}
	}

	private Profile getProfileJson() throws IOException {
		String profileServiceGetProfileResponseString = IOUtils.toString(
				ProfileServiceTestHelper.class.getResourceAsStream("/integration/profile/profileServiceGetProfileResponse.json"), "UTF-8");
		JsonObject existing = gson.<JsonObject> fromJson(profileServiceGetProfileResponseString, JsonElement.class);
		return new Profile(existing.get("content").getAsJsonObject().get("profile").getAsJsonObject());
	}
}
