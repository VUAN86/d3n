package de.ascendro.f4m.service.profile.dao;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.profile.ProfileMessageTypeMapper;
import de.ascendro.f4m.service.profile.config.ProfileConfig;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.schema.ProfileMessageSchemaMapper;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.random.RandomUtilImpl;

public class ProfileAerospikeDaoImplTest extends RealAerospikeTestBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileAerospikeDaoImplTest.class);
	
	private static final String USER_ID = "ea2d13d6-b0a6-11e6-80f5-76304dec7eb7";

	private static final String NAMESPACE = "test";
	private static final String PROFILE_SET = "profile_test";
	private static final String PROFILE_SYNC_SET = "profile_sync_test";
	private static final String PROFILE_SYNC_RANDOM_VALUE_INDEX_NAME = "randomValue_test-i";

	
	private JsonMessageUtil jsonMessageUtil;
	private final JsonUtil jsonUtil = new JsonUtil();
	private final JsonLoader jsonLoader = new JsonLoader(this);

	private ProfileAerospikeDaoImpl profileAerospikeDao;
	private AerospikeDao aerospikeDao;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;

	@Before
	@Override
	public void setUp() {
		jsonMessageUtil = new JsonMessageUtil(
				new GsonProvider(new JsonMessageDeserializer(new ProfileMessageTypeMapper())), null,
				new JsonMessageValidator(new ProfileMessageSchemaMapper(), new F4MConfigImpl()));
		
		super.setUp();
		
		if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
			clearTestProfileSet();
			clearTestProfileSyncSet();
		}
	}
	
	@After
	@Override
	public void tearDown() {
		if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
			try {
				clearTestProfileSet();
				
				clearTestProfileSyncSet();
				dropIndex(NAMESPACE, PROFILE_SYNC_SET, PROFILE_SYNC_RANDOM_VALUE_INDEX_NAME);
			} finally {
				super.tearDown();
			}
		}
	}
	
	private void clearTestProfileSet() {
		LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", PROFILE_SET, NAMESPACE);
		clearSet(NAMESPACE, PROFILE_SET);
		
	}
	
	private void clearTestProfileSyncSet() {
		LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", PROFILE_SYNC_SET, NAMESPACE);
		clearSet(NAMESPACE, PROFILE_SYNC_SET);		
	}
	
	@Override
	protected Config createConfig() {
		final Config config = new ProfileConfig();
		
		config.setProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET, PROFILE_SET);
		config.setProperty(ProfileConfig.AEROSPIKE_PROFILE_SYNC_SET, PROFILE_SYNC_SET);
		config.setProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE, NAMESPACE);
		config.setProperty(ProfileConfig.AEROSPIKE_PROFILE_RANDOM_VALUE_INDEX_NAME, PROFILE_SYNC_RANDOM_VALUE_INDEX_NAME);
		
		return config;
	}

	@Override
	protected void setUpAerospike() {
		profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
		profileAerospikeDao = new ProfileAerospikeDaoImpl(config, profilePrimaryKeyUtil, aerospikeClientProvider,
				jsonMessageUtil, jsonUtil, new RandomUtilImpl());
		if(!(aerospikeClientProvider instanceof AerospikeClientMockProvider)){
			dropIndex(NAMESPACE, PROFILE_SYNC_SET, PROFILE_SYNC_RANDOM_VALUE_INDEX_NAME);
			profileAerospikeDao.init();
		}
		aerospikeDao = new AerospikeDaoImpl<ProfilePrimaryKeyUtil>(config, profilePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Test
	public void testDeleteProfile() throws IOException {
		final String sourceJsonString = getProfileJson(USER_ID, "anyName", "anySurname");

		final String profileKey = profilePrimaryKeyUtil.createPrimaryKey(USER_ID);
		aerospikeClientProvider.get().put(null, new Key(NAMESPACE, PROFILE_SET, profileKey),
				new Bin(ProfileAerospikeDaoImpl.BLOB_BIN_NAME, sourceJsonString.getBytes()));

		final String mail1EmailKey = profilePrimaryKeyUtil.createPrimaryKey(ProfileIdentifierType.EMAIL,
				"mail1@mail1.com");
		aerospikeClientProvider.get().put(null, new Key(NAMESPACE, PROFILE_SET, mail1EmailKey),
				new Bin(ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME, profileKey));

		final String originalAtOriginEmailKey = profilePrimaryKeyUtil.createPrimaryKey(ProfileIdentifierType.EMAIL,
				"original@origin.com");
		aerospikeClientProvider.get().put(null, new Key(NAMESPACE, PROFILE_SET, originalAtOriginEmailKey),
				new Bin(ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME, profileKey));

		final String facebookKey = profilePrimaryKeyUtil.createPrimaryKey(ProfileIdentifierType.FACEBOOK,
				"FB-TEST_TOKEN1");
		aerospikeClientProvider.get().put(null, new Key(NAMESPACE, PROFILE_SET, facebookKey),
				new Bin(ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME, profileKey));

		assertNotNull(aerospikeDao.readJson(PROFILE_SET, profileKey, ProfileAerospikeDaoImpl.BLOB_BIN_NAME));
		assertNotNull(
				aerospikeDao.readString(PROFILE_SET, mail1EmailKey, ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME));
		assertNotNull(aerospikeDao.readString(PROFILE_SET, originalAtOriginEmailKey,
				ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME));
		assertNotNull(
				aerospikeDao.readString(PROFILE_SET, facebookKey, ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME));

		profileAerospikeDao.deleteProfile(USER_ID);

		assertNull(aerospikeDao.readJson(PROFILE_SET, profileKey, ProfileAerospikeDaoImpl.BLOB_BIN_NAME));
		assertNull(aerospikeDao.readString(PROFILE_SET, mail1EmailKey, ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME));
		assertNull(aerospikeDao.readString(PROFILE_SET, originalAtOriginEmailKey,
				ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME));
		assertNull(aerospikeDao.readString(PROFILE_SET, facebookKey, ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME));
		assertEquals(USER_ID,aerospikeDao.readString(PROFILE_SYNC_SET, profileKey, ProfileAerospikeDaoImpl.PROFILE_ID_BIN_NAME));
	}

	@Test
	public void testCreateProfileAsBin() throws IOException {
		final String sourceJsonString = getProfileJson(USER_ID, "anyName", "anySurname");
		final JsonElement jsonElement = JsonTestUtil.getGson().fromJson(sourceJsonString, JsonElement.class);

		final Profile profile = new Profile(jsonElement);

		profileAerospikeDao.createProfile(USER_ID, profile);

		assertProfileValueIsBin();
	}
	
	@Test
	public void profileNicknameUniquenessTest() throws Exception {
		String profileJson = jsonLoader.getPlainTextJsonFromResources("profileWithNickname.json")
				.replaceFirst("<<userId>>", USER_ID) //
				.replaceFirst("<<firstName>>", "anyName") //
				.replaceFirst("<<lastName>>", "anySurname") //
				.replaceFirst("<<nickname>>", "anyNickname");

		final Profile profile = createTestProfile(profileJson);
		List<Pair<ProfileIdentifierType, String>> identifiers = profile.getIdentifiers();
		//reserve last identifier
		Pair<ProfileIdentifierType, String> last = Iterables.getLast(identifiers);
		profileAerospikeDao.createProfileIdentifierTypeKeys(USER_ID, Arrays.asList(last), new ArrayList<String>());
		
		try {
			ProfileUser profileUser = profile.getPersonWrapper();
			profileUser.setNickname("ANYNICKNAME");
			profile.setPersonWrapper(profileUser);
			profileAerospikeDao.createProfile(USER_ID, profile);
			fail("Should have failed since last identifier is already reserved");
		} catch (F4MEntryAlreadyExistsException e) {
			assertEquals("Profile with specified identifier " + last.getValue() + " and type " + last.getKey()
					+ " already exists", e.getMessage());
		}
	}

	@Test
	public void profileNicknameUpdateTest() throws Exception {
		String nickname = "anyNickname";
		String newNickname = "newNickname";
		String profileJson = jsonLoader.getPlainTextJsonFromResources("profileWithNickname.json")
				.replaceFirst("<<userId>>", USER_ID) //
				.replaceFirst("<<firstName>>", "anyName") //
				.replaceFirst("<<lastName>>", "anySurname") //
				.replaceFirst("<<nickname>>", nickname);
		final Profile profile = createTestProfile(profileJson);
		profileAerospikeDao.createProfile(USER_ID, profile);

		final Profile profileUpdateDiff = new Profile();
		ProfileUser profileUser = new ProfileUser();
		profileUser.setNickname(newNickname);
		profileUpdateDiff.setPersonWrapper(profileUser);
		profileUpdateDiff.setProfileIdentifier(ProfileIdentifierType.EMAIL, profile.getProfileEmails().get(0));
		Profile newProfile = profileAerospikeDao.updateProfile(USER_ID, profileUpdateDiff, profile);

		Pair<ProfileIdentifierType, String> nicknameIdentifier = profile
				.getIdentifiersByType(ProfileIdentifierType.NICKNAME).get(0);
		Pair<ProfileIdentifierType, String> newNicknameIdentifier = newProfile
				.getIdentifiersByType(ProfileIdentifierType.NICKNAME).get(0);

		assertNotEquals("Nickname identifiers should be different", nicknameIdentifier.getValue(),
				newNicknameIdentifier.getValue());
		assertNotNull("New nickname identifier has not saved", profileAerospikeDao
				.findUserIdByIdentifier(newNicknameIdentifier.getKey(), newNicknameIdentifier.getValue()));
		assertEquals("User identifiers count is incorrect", newProfile.getIdentifiers().size(), 4);
		
		newProfile.getIdentifiers().forEach(ident -> {
			if (ProfileIdentifierType.NICKNAME == ident.getKey()) {
				assertEquals("New nickname identifier is incorrect", newNickname.toLowerCase(), ident.getValue());
			} else {
				assertNotNull("Old non nickname identifier has deleted",
						profileAerospikeDao.findUserIdByIdentifier(ident.getKey(), ident.getValue()));
			}
		});
	}

	@Test
	public void testCreateProfileWithTakenIdentifiers() throws Exception {
		final Profile profile = createTestProfile(getProfileJson(USER_ID, "anyName", "anySurname"));
		List<Pair<ProfileIdentifierType, String>> identifiers = profile.getIdentifiers();
		//reserve last identifier
		Pair<ProfileIdentifierType, String> last = Iterables.getLast(identifiers);
		profileAerospikeDao.createProfileIdentifierTypeKeys(USER_ID, Arrays.asList(last), new ArrayList<String>());
		
		try {
			profileAerospikeDao.createProfile(USER_ID, profile);
			fail("Should have failed since last identifier is already reserved");
		} catch (F4MEntryAlreadyExistsException e) {
			assertEquals("Profile with specified identifier " + last.getValue() + " and type " + last.getKey()
					+ " already exists", e.getMessage());
		}
		for (Pair<ProfileIdentifierType, String> identifier : identifiers) {
			if (!identifier.equals(last)) {
				assertNull(profileAerospikeDao.findUserIdByIdentifier(identifier.getKey(), identifier.getValue()));
			}
		}
		//check that findByIdentifier removed unused keys with no corresponding profile in db - faulty last identifier is removed
		assertNotNull("Last identifier expected to be found in db",
				profileAerospikeDao.findUserIdByIdentifier(last.getKey(), last.getValue()));
		assertNull("Expect no profile, since create failed",
				profileAerospikeDao.findByIdentifierWithCleanup(last.getKey(), last.getValue()));
		assertNull("Last identifier expected to be deleted by findByIdentifierWithCleanup",
				profileAerospikeDao.findUserIdByIdentifier(last.getKey(), last.getValue()));
	}

	@Test
	public void testUpdateMissingProfile() throws IOException {
		final Profile profile = createTestProfile(getProfileJson(USER_ID, "anyName", "anySurname"));
		final String profilePrimaryKey = profilePrimaryKeyUtil.createPrimaryKey(USER_ID);

		assertFalse(aerospikeDao.exists(PROFILE_SYNC_SET, profilePrimaryKey));
		try {
			//to be hones, this test does not make much sense, since profileAerospikeDao relies on existing record from parameter... Delete whole test?
			profileAerospikeDao.updateProfile(USER_ID, profile, null);
			fail("Should have failed since there is no entry in db");
		} catch (F4MEntryNotFoundException e) {
			assertEquals("Record not found", e.getMessage());
		}
	}

	@Test
	public void testUpdateProfileAsBin() throws IOException {
		final String sourceJsonString = getProfileJson(USER_ID, "anyName", "anySurname");
		final Profile profile = createTestProfile(sourceJsonString);
		profile.setCity("Old city");

		final String profilePrimaryKey = profilePrimaryKeyUtil.createPrimaryKey(USER_ID);
		final Key profileKey = new Key(NAMESPACE, PROFILE_SET, profilePrimaryKey);
		final Bin profileBin = new Bin(ProfileAerospikeDaoImpl.BLOB_BIN_NAME, sourceJsonString.getBytes());
		aerospikeClientProvider.get().put(null, profileKey, profileBin);

		assertFalse(aerospikeDao.exists(PROFILE_SYNC_SET, profilePrimaryKey));

		final Profile profileUpdateDiff = new Profile();
		profileUpdateDiff.setCity("New city");
		
		final Profile finalProfile = profileAerospikeDao.updateProfile(USER_ID, profileUpdateDiff, profile);
		assertProfileValueIsBin();
		assertEquals(finalProfile.getCity(), "New city");

		assertTrue(aerospikeDao.exists(PROFILE_SYNC_SET, profilePrimaryKey));
	}

	private Profile createTestProfile(final String profileJsonString) {
		final Gson gson = JsonTestUtil.getGson();
		final JsonElement jsonElement = gson.fromJson(profileJsonString, JsonElement.class);
		return new Profile(jsonElement);
	}

	@Test
	public void testWhenGetProfileWithNoProfileThenReturnNull() throws Exception {
		Profile profile = profileAerospikeDao.getProfile("nonExisting");
		// if empty profile would be returned, most places containing if
		// (profile != null) should be changed to check if profile is not empty
		assertNull(profile);
	}

	private void assertProfileValueIsBin() {
		final Record record = aerospikeClientProvider.get().get(null, new Key(NAMESPACE, PROFILE_SET, "profile:" + USER_ID));
		assertNotNull(record.bins.get("value"));
		assertTrue(record.bins.get("value") instanceof byte[]);
	}

	private String getProfileJson(String userId, String name, String surname) throws IOException {
		final String profileJson = jsonLoader.getPlainTextJsonFromResources("profile.json")
				.replaceFirst("<<userId>>", userId).replaceFirst("<<firstName>>", name)
				.replaceFirst("<<lastName>>", surname);
		return profileJson;
	}

	@Test
	public void testFindNewIdentifiers() throws Exception {
		Profile newProfileData = new Profile();
		Profile actualProfile = new Profile();
		actualProfile.setProfileIdentifier(ProfileIdentifierType.EMAIL, "old");
		actualProfile.setProfileIdentifier(ProfileIdentifierType.EMAIL, "exists");
		newProfileData.setProfileIdentifier(ProfileIdentifierType.EMAIL, "exists");
		newProfileData.setProfileIdentifier(ProfileIdentifierType.EMAIL, "new");
		List<Pair<ProfileIdentifierType, String>> newIdentifiers = profileAerospikeDao
				.findNewIdentifiers(newProfileData, actualProfile);
		List<String> emails = newIdentifiers.stream().map(i -> i.getRight()).collect(Collectors.toList());
		assertThat(emails, contains("new"));
		assertThat(emails, not(contains("exists")));
		assertThat(emails, not(contains("old")));
	}

}
