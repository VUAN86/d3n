package de.ascendro.f4m.server.profile;

import static de.ascendro.f4m.server.config.AerospikeConfigImpl.AEROSPIKE_NAMESPACE;
import static de.ascendro.f4m.server.profile.CommonProfileAerospikeDao.BLOB_BIN_NAME;
import static de.ascendro.f4m.server.profile.CommonProfileAerospikeDao.LAST_INVITED_DUEL_OPPONENTS_BLOB_NAME;
import static de.ascendro.f4m.server.profile.CommonProfileAerospikeDao.PAUSED_DUEL_OPPONENTS_BLOB_NAME;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.util.JsonLoader;

public class CommonProfileAerospikeDaoImplTest extends RealAerospikeTestBase {

	private static final String USER_ID = "user_id_1";
	private static final String USER_ID_2 = "user_id_2";
	private static final String APP_ID = "app_id_1";
	private static final String APP_ID_2 = "app_id_2";
	private static final List<String> USERS_1 = Arrays.asList("user_11", "user_2", "user_13", "user_4");
	private static final List<String> USERS_TO_REMOVE = Arrays.asList("user_2", "user_4");
	private static final List<String> REMAINING_USERS_1 = Arrays.asList("user_11", "user_13");
	private static final List<String> USERS_2 = Arrays.asList("user_21", "user_2", "user_23", "user_4");
	private static final List<String> REMAINING_USERS_2 = Arrays.asList("user_21", "user_23");
	private static final List<String> USERS_3 = Arrays.asList("user_31", "user_2", "user_33", "user_4");
	private static final List<String> USERS_4 = Arrays.asList("user_41", "user_2", "user_43", "user_4");

	private static final Type MAP_STRING_TO_LIST_OF_STRING_TYPE = new TypeToken<Map<String, List<String>>>(){}.getType();
	public static final String PROFILE_SET = "profile";

	private final ProfilePrimaryKeyUtil profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
	private final JsonUtil jsonUtil = new JsonUtil();
	private final JsonLoader jsonLoader = new JsonLoader(this);

	private CommonProfileAerospikeDao commonProfileAerospikeDao;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		clearSet(PROFILE_SET);
	}

	@Override
	@After
	public void tearDown() {
		try {
			clearSet(PROFILE_SET);
		} finally {
			super.tearDown();
		}
	}

	protected void clearSet(String set) {
		final String namespace = config.getProperty(AEROSPIKE_NAMESPACE);
		if (aerospikeClientProvider != null && aerospikeClientProvider instanceof AerospikeClientProvider) {
			super.clearSet(namespace, set);
		}
	}

	@Override
	protected void setUpAerospike() {
		commonProfileAerospikeDao = new CommonProfileAerospikeDaoImpl(config, profilePrimaryKeyUtil,
				aerospikeClientProvider, jsonUtil);
	}

	@Test
	public void testSetLastInvitedDuelOponents() throws IOException {
		// Prepare
		createTestProfiles();

		// Test
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID, APP_ID, USERS_1);
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID, APP_ID_2, USERS_2);
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID_2, APP_ID, USERS_3);
		// Overwrite
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID_2, APP_ID_2, USERS_1);
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID_2, APP_ID_2, USERS_4);

		// Validate
		validateSetOpponents(LAST_INVITED_DUEL_OPPONENTS_BLOB_NAME);
	}

	@Test
	public void testGetLastInvitedDuelOponents() throws IOException {
		// Prepare
		createTestProfiles();
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID, APP_ID, USERS_1);
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID, APP_ID_2, USERS_2);
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID_2, APP_ID, USERS_3);

		// Test
		List<String> duelOpponents1 = commonProfileAerospikeDao.getLastInvitedDuelOpponents(USER_ID, APP_ID);
		List<String> duelOpponents2 = commonProfileAerospikeDao.getLastInvitedDuelOpponents(USER_ID, APP_ID_2);
		List<String> duelOpponents3 = commonProfileAerospikeDao.getLastInvitedDuelOpponents(USER_ID_2, APP_ID);
		List<String> duelOpponents4 = commonProfileAerospikeDao.getLastInvitedDuelOpponents(USER_ID_2, APP_ID_2);

		// Validate
		validateGetOpponents(duelOpponents1, duelOpponents2, duelOpponents3, duelOpponents4);
	}

	@Test
	public void testRemoveUsersFromLastInvitedDuelOponents() throws IOException {
		// Prepare
		createTestProfiles();
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID, APP_ID, USERS_1);
		commonProfileAerospikeDao.setLastInvitedDuelOponents(USER_ID, APP_ID_2, USERS_2);

		// Test
		commonProfileAerospikeDao.removeUsersFromLastInvitedDuelOponents(USER_ID, USERS_TO_REMOVE);

		// Validate
		validateRemoveOpponents(LAST_INVITED_DUEL_OPPONENTS_BLOB_NAME);
	}

	@Test
	public void testSetPausedDuelOponents() throws IOException {
		// Prepare
		createTestProfiles();

		// Test
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID, APP_ID, USERS_1);
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID, APP_ID_2, USERS_2);
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID_2, APP_ID, USERS_3);
		// Overwrite
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID_2, APP_ID_2, USERS_1);
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID_2, APP_ID_2, USERS_4);

		// Validate
		validateSetOpponents(PAUSED_DUEL_OPPONENTS_BLOB_NAME);
	}

	@Test
	public void testGetPausedDuelOponents() throws IOException {
		// Prepare
		createTestProfiles();
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID, APP_ID, USERS_1);
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID, APP_ID_2, USERS_2);
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID_2, APP_ID, USERS_3);

		// Test
		List<String> duelOpponents1 = commonProfileAerospikeDao.getPausedDuelOpponents(USER_ID, APP_ID);
		List<String> duelOpponents2 = commonProfileAerospikeDao.getPausedDuelOpponents(USER_ID, APP_ID_2);
		List<String> duelOpponents3 = commonProfileAerospikeDao.getPausedDuelOpponents(USER_ID_2, APP_ID);
		List<String> duelOpponents4 = commonProfileAerospikeDao.getPausedDuelOpponents(USER_ID_2, APP_ID_2);

		// Validate
		validateGetOpponents(duelOpponents1, duelOpponents2, duelOpponents3, duelOpponents4);
	}

	@Test
	public void testRemoveUsersFromPausedDuelOponents() throws IOException {
		// Prepare
		createTestProfiles();
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID, APP_ID, USERS_1);
		commonProfileAerospikeDao.setPausedDuelOponents(USER_ID, APP_ID_2, USERS_2);

		// Test
		commonProfileAerospikeDao.removeUsersFromPausedDuelOponents(USER_ID, USERS_TO_REMOVE);

		// Validate
		validateRemoveOpponents(PAUSED_DUEL_OPPONENTS_BLOB_NAME);
	}

	private void validateRemoveOpponents(String lastInvitedDuelOpponentsBlobName) {
		String duelOpponentsKey = profilePrimaryKeyUtil.createSubRecordKeyByUserId(USER_ID,
				lastInvitedDuelOpponentsBlobName);
		String duelOpponentsListString = commonProfileAerospikeDao.readJson(PROFILE_SET, duelOpponentsKey, BLOB_BIN_NAME);
		assertNotNull(duelOpponentsListString);
		Map<String, List<String>> duelOpponentsMap = jsonUtil.fromJson(duelOpponentsListString,
				MAP_STRING_TO_LIST_OF_STRING_TYPE);
		assertThat(duelOpponentsMap.keySet(), hasSize(2));
		assertThat(duelOpponentsMap.get(APP_ID), containsInAnyOrder(REMAINING_USERS_1.toArray()));
		assertThat(duelOpponentsMap.get(APP_ID_2), containsInAnyOrder(REMAINING_USERS_2.toArray()));
	}

	private void validateSetOpponents(String lastInvitedDuelOpponentsBlobName) {
		String duelOpponentsKey = profilePrimaryKeyUtil.createSubRecordKeyByUserId(USER_ID,
				lastInvitedDuelOpponentsBlobName);
		String duelOpponentsListString = commonProfileAerospikeDao.readJson(PROFILE_SET, duelOpponentsKey, BLOB_BIN_NAME);
		assertNotNull(duelOpponentsListString);
		Map<String, List<String>> duelOpponentsMap = jsonUtil.fromJson(duelOpponentsListString,
				MAP_STRING_TO_LIST_OF_STRING_TYPE);
		assertThat(duelOpponentsMap.keySet(), hasSize(2));
		assertThat(duelOpponentsMap.get(APP_ID), containsInAnyOrder(USERS_1.toArray()));
		assertThat(duelOpponentsMap.get(APP_ID_2), containsInAnyOrder(USERS_2.toArray()));

		duelOpponentsKey = profilePrimaryKeyUtil.createSubRecordKeyByUserId(USER_ID_2,
				lastInvitedDuelOpponentsBlobName);
		duelOpponentsListString = commonProfileAerospikeDao.readJson(PROFILE_SET, duelOpponentsKey, BLOB_BIN_NAME);
		assertNotNull(duelOpponentsListString);
		duelOpponentsMap = jsonUtil.fromJson(duelOpponentsListString, MAP_STRING_TO_LIST_OF_STRING_TYPE);
		assertThat(duelOpponentsMap.keySet(), hasSize(2));
		assertThat(duelOpponentsMap.get(APP_ID), containsInAnyOrder(USERS_3.toArray()));
		assertThat(duelOpponentsMap.get(APP_ID_2), containsInAnyOrder(USERS_4.toArray()));
	}

	private void validateGetOpponents(List<String> duelOpponents1, List<String> duelOpponents2, List<String> duelOpponents3, List<String> duelOpponents4) {
		assertThat(duelOpponents1, containsInAnyOrder(USERS_1.toArray()));
		assertThat(duelOpponents2, containsInAnyOrder(USERS_2.toArray()));
		assertThat(duelOpponents3, containsInAnyOrder(USERS_3.toArray()));
		assertThat(duelOpponents4, hasSize(0));
	}

	private void createTestProfiles() throws IOException {
		final String sourceJsonString1 = getProfileJson(USER_ID, "anyName", "anySurname");

		final String profile1Key = profilePrimaryKeyUtil.createPrimaryKey(USER_ID);
		commonProfileAerospikeDao.createJson(PROFILE_SET, profile1Key, BLOB_BIN_NAME, sourceJsonString1);

		final String sourceJsonString2 = getProfileJson(USER_ID_2, "anyName", "anySurname");

		final String profile2Key = profilePrimaryKeyUtil.createPrimaryKey(USER_ID_2);
		commonProfileAerospikeDao.createJson(PROFILE_SET, profile2Key, BLOB_BIN_NAME, sourceJsonString2);
	}

	private String getProfileJson(String userId, String name, String surname) throws IOException {
		final String profileJson = jsonLoader.getPlainTextJsonFromResources("profile.json")
				.replaceFirst("<<userId>>", userId).replaceFirst("<<firstName>>", name)
				.replaceFirst("<<lastName>>", surname);
		return profileJson;
	}

}
