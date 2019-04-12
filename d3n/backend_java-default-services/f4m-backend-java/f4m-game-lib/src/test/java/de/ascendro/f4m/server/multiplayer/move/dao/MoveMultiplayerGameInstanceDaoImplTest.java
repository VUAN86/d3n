package de.ascendro.f4m.server.multiplayer.move.dao;

import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.INVITED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.REGISTERED;
import static de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl.INDEX_BIN_NAME;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.collection.IsMapContaining;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.MultiplayerGameInstancePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CreatedBy;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class MoveMultiplayerGameInstanceDaoImplTest extends RealAerospikeTestBase {
	
	private static final String NOTIF_ID_2 = "notif-ID-2";
	private static final String NOTIF_ID_1 = "notif-ID-1";
	private static final String MGI_SET = "mgi";
	private static final String MGI_INDEX_SET = "mgiIndex";
	
	private static final String GAME_INSTANCE_ID = "gi_id";
	private static final String USER_ID_1 = "user_id_1";
	private static final String USER_ID_2 = "user_id_2";
	
	private final AerospikeConfigImpl aerospikeConfig = new AerospikeConfigImpl();
	private final F4MConfigImpl gameConfig = new F4MConfigImpl(aerospikeConfig, new GameConfigImpl());
	private final MultiplayerGameInstancePrimaryKeyUtil primaryKeyUtil = new MultiplayerGameInstancePrimaryKeyUtil(aerospikeConfig);
	private final JsonUtil jsonUtil = new JsonUtil();
	
	private CommonMultiplayerGameInstanceDao mgiDao;
	private MoveMultiplayerGameInstanceDaoImpl moveMgiDao;
	private AerospikeOperateDao aerospikeDao;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		clearSets(MGI_SET, MGI_INDEX_SET);
	}
	
	@Override
	@After
	public void tearDown() {
		try {
			clearSets(MGI_SET, MGI_INDEX_SET);
		} finally {
			super.tearDown();
		}
	}
	
	protected void clearSets(String... sets) {
		Stream.of(sets).forEach(set -> {
			String namespace = aerospikeConfig.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
			if (aerospikeClientProvider != null && aerospikeClientProvider instanceof AerospikeClientProvider) {
				super.clearSet(namespace, set);
			}
		});
	}
	
	@Override
	protected void setUpAerospike() {
		mgiDao = new CommonMultiplayerGameInstanceDaoImpl(gameConfig, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
		moveMgiDao = new MoveMultiplayerGameInstanceDaoImpl(gameConfig, primaryKeyUtil, jsonUtil, aerospikeClientProvider, mgiDao);
		aerospikeDao = new AerospikeOperateDaoImpl<PrimaryKeyUtil<?>>(aerospikeConfig, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Test
	public void testMoveUserUserNotifications() throws Exception {
		// prepare: invitations for ANONYMOUS_USER_ID in states INVITED and REGISTERED
		CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.withTenant(TENANT_ID).withApp(APP_ID);
		String mgiIdWithInvited = mgiDao.create(ANONYMOUS_USER_ID, customGameConfigBuilder.build());
		String mgiIdWithRegistered = mgiDao.create(ANONYMOUS_USER_ID, customGameConfigBuilder.build());
		mgiDao.setInvitationExpirationNotificationId(mgiIdWithInvited, ANONYMOUS_USER_ID, NOTIF_ID_1);
		mgiDao.setInvitationExpirationNotificationId(mgiIdWithRegistered, ANONYMOUS_USER_ID, NOTIF_ID_2);
		
		// validate: ANONYMOUS_USER_ID notifications
		assertUserNotifications(ANONYMOUS_USER_ID, mgiIdWithInvited, NOTIF_ID_1);
		assertUserNotifications(ANONYMOUS_USER_ID, mgiIdWithRegistered, NOTIF_ID_2);
		
		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		moveMgiDao.moveUserMgiData(TENANT_ID, APP_ID, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		
		// validate: REGISTERED_USER_ID notifications
		assertUserNotifications(REGISTERED_USER_ID, mgiIdWithInvited, NOTIF_ID_1);
		assertUserNotifications(REGISTERED_USER_ID, mgiIdWithRegistered, NOTIF_ID_2);
	}
	
	private void assertUserNotifications(String userId, String mgiId, String value) {		
		String notificationId = mgiDao.getInvitationExpirationNotificationId(mgiId, userId);
		assertThat(notificationId, equalTo(value));
	}
	
	@Test
	public void testMoveUserUserInvitations() throws Exception {
		// prepare: invitations for ANONYMOUS_USER_ID in states INVITED and REGISTERED
		CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.withTenant(TENANT_ID).withApp(APP_ID);
		String mgiIdWithInvited = mgiDao.create(ANONYMOUS_USER_ID, customGameConfigBuilder.build());
		String mgiIdWithRegistered = mgiDao.create(ANONYMOUS_USER_ID, customGameConfigBuilder.build());
		mgiDao.registerForGame(mgiIdWithRegistered, new ClientInfo("t1", "a1", ANONYMOUS_USER_ID, "ip", null), GAME_INSTANCE_ID);
		
		// validate: ANONYMOUS_USER_ID invitations
		assertUserInvitations(ANONYMOUS_USER_ID, mgiIdWithInvited, INVITED);
		assertUserInvitations(ANONYMOUS_USER_ID, mgiIdWithRegistered, REGISTERED);
		assertUserInvitations(REGISTERED_USER_ID, null, INVITED);
		assertUserInvitations(REGISTERED_USER_ID, null, REGISTERED);
		
		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		moveMgiDao.moveUserMgiData(TENANT_ID, APP_ID, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		
		// validate: REGISTERED_USER_ID invitations
		assertUserInvitations(ANONYMOUS_USER_ID, null, INVITED);
		assertUserInvitations(ANONYMOUS_USER_ID, null, REGISTERED);
		assertUserInvitations(REGISTERED_USER_ID, mgiIdWithInvited, INVITED);
		assertUserInvitations(REGISTERED_USER_ID, mgiIdWithRegistered, REGISTERED);
	}
	
	private void assertUserInvitations(String userId, String mgiId, MultiplayerGameInstanceState state) {
		FilterCriteria filterCriteria = new FilterCriteria();
		filterCriteria.setLimit(Integer.MAX_VALUE);
		List<Invitation> invitationList = mgiDao.getInvitationList(TENANT_ID, APP_ID, userId, Arrays.asList(state), CreatedBy.ALL, filterCriteria);
		if (mgiId != null) {
			assertThat(invitationList, contains(hasProperty("multiplayerGameInstanceId", equalTo(mgiId))));
		} else {
			assertThat(invitationList, is(empty()));
		}
	}
	
	@Test
	public void testMoveReferencesToMgi() throws Exception {
		// prepare: MGI with invited ANONYMOUS_USER_ID
		CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID).withTenant(TENANT_ID);
		String mgiId = mgiDao.create(ANONYMOUS_USER_ID, customGameConfigBuilder.build());
		
		// validate: exists reference for ANONYMOUS_USER_ID
		assertReferenceToMgi(mgiId, ANONYMOUS_USER_ID, notNullValue());
		assertReferenceToMgi(mgiId, REGISTERED_USER_ID, nullValue());
		
		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		moveMgiDao.moveReferencesToMgi(TENANT_ID, APP_ID, Collections.singletonMap(mgiId, ANONYMOUS_USER_ID), ANONYMOUS_USER_ID, REGISTERED_USER_ID);

		// validate: exists reference for REGISTERED_USER_ID
		assertReferenceToMgi(mgiId, ANONYMOUS_USER_ID, nullValue());
		assertReferenceToMgi(mgiId, REGISTERED_USER_ID, notNullValue());
	}
	
	private void assertReferenceToMgi(String mgiId, String userId, Matcher<Object> matcher) {
		String indexKey = primaryKeyUtil.createIndexKeyToInstanceRecord(mgiId, userId);
		String reference = aerospikeDao.readString(MGI_INDEX_SET, indexKey, INDEX_BIN_NAME);
		assertThat(reference, matcher);
	}
	
	@Test
	public void testMoveUserDataInMgi() throws Exception {
		// prepare: MGI with registered ANONYMOUS_USER_ID
		CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID).withTenant(TENANT_ID);
		String mgiId = mgiDao.create(ANONYMOUS_USER_ID, customGameConfigBuilder.build());
		mgiDao.registerForGame(mgiId, new ClientInfo("t1", "a1", ANONYMOUS_USER_ID, "ip", null), GAME_INSTANCE_ID);
		
		// validate: MGI has ANONYMOUS_USER_ID in state REGISTERED
		assertUserInMgiInvitedBin(mgiId, ANONYMOUS_USER_ID, REGISTERED);
		assertUserInMgiInvitedBin(mgiId, REGISTERED_USER_ID, null);
		
		// validate: ANONYMOUS_USER_ID is in MultiplayerUserGameInstance
		assertUserInUserGameInstance(mgiId, GAME_INSTANCE_ID, ANONYMOUS_USER_ID, REGISTERED);
		
		// validate: ANONYMOUS_USER_ID has reference to Game Instance ID
		assertReferenceToGameInstanceId(mgiId, ANONYMOUS_USER_ID, GAME_INSTANCE_ID);
		assertReferenceToGameInstanceId(mgiId, REGISTERED_USER_ID, null);
		
		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		String mgiKey = primaryKeyUtil.createInstanceRecordKey(mgiId, 1);
		moveMgiDao.changeUserDataInMgi(TENANT_ID, mgiId, mgiKey, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		
		// validate: MGI has REGISTERED_USER_ID in state REGISTERED
		assertUserInMgiInvitedBin(mgiId, ANONYMOUS_USER_ID, null);
		assertUserInMgiInvitedBin(mgiId, REGISTERED_USER_ID, REGISTERED);
		
		// validate: REGISTERED_USER_ID is in MultiplayerUserGameInstance
		assertUserInUserGameInstance(mgiId, GAME_INSTANCE_ID, REGISTERED_USER_ID, REGISTERED);
		
		// validate: REGISTERED_USER_ID has reference to Game Instance ID
		assertReferenceToGameInstanceId(mgiId, ANONYMOUS_USER_ID, null);
		assertReferenceToGameInstanceId(mgiId, REGISTERED_USER_ID, GAME_INSTANCE_ID);
	}
	
	private void assertUserInMgiInvitedBin(String mgiId, String userId, MultiplayerGameInstanceState state) {
		Map<String, String> allUsersOfMgi = mgiDao.getAllUsersOfMgi(mgiId);
		if (state != null) {
			assertThat(allUsersOfMgi, IsMapContaining.hasEntry(userId, state.name()));
		} else {
			assertThat(allUsersOfMgi, not(IsMapContaining.hasKey(userId)));
		}
	}
	
	private void assertUserInUserGameInstance(String mgiId, String giId, String expectedUserId, MultiplayerGameInstanceState state) {
		String mgiKey = primaryKeyUtil.createInstanceRecordKey(mgiId, 1);
		String userGameInstanceJson = aerospikeDao.getByKeyFromMap(MGI_SET, mgiKey, state.getBinName(), giId);
		MultiplayerUserGameInstance userGameInstance = jsonUtil.fromJson(userGameInstanceJson, MultiplayerUserGameInstance.class);
		assertThat(userGameInstance.getUserId(), equalTo(expectedUserId));
	}
	
	private void assertReferenceToGameInstanceId(String mgiId, String userId, String expectedGiId) {
		String giId = mgiDao.getGameInstanceId(mgiId, userId);
		assertThat(giId, equalTo(expectedGiId));
	}
	
	@Test
	public void testChangeInviter() throws Exception {
		// prepare: ANONYMOUS_USER_ID invited USER_ID_1 (in state INVITED) and USER_ID_2 (in state REGISTERED)
		CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.withTenant(TENANT_ID).withApp(APP_ID);
		String mgiId = mgiDao.create(null, customGameConfigBuilder.build());
		mgiDao.addUser(mgiId, USER_ID_1, ANONYMOUS_USER_ID, MultiplayerGameInstanceState.INVITED);
		mgiDao.addUser(mgiId, USER_ID_2, ANONYMOUS_USER_ID, MultiplayerGameInstanceState.INVITED);
		mgiDao.registerForGame(mgiId, new ClientInfo("t1", "a1", USER_ID_2, "ip", null), GAME_INSTANCE_ID);
		
		// validate: USER_ID_1 and USER_ID_2 are invited by ANONYMOUS_USER_ID
		assertInviter(mgiId, USER_ID_1, INVITED, ANONYMOUS_USER_ID);
		assertInviter(mgiId, USER_ID_2, REGISTERED, ANONYMOUS_USER_ID);
		
		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		moveMgiDao.changeInviter(TENANT_ID, APP_ID, mgiId, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		
		// validate: USER_ID_1 and USER_ID_2 are invited by REGISTERED_USER_ID
		assertInviter(mgiId, USER_ID_1, INVITED, REGISTERED_USER_ID);
		assertInviter(mgiId, USER_ID_2, REGISTERED, REGISTERED_USER_ID);
	}

	private void assertInviter(String mgiId, String inviteeId, MultiplayerGameInstanceState state, String expectedInviterId) {
		String inviterId = mgiDao.getInviterId(inviteeId, mgiId, state);
		assertThat(inviterId, equalTo(expectedInviterId));
	}
	
	@Test
	public void testChangeGameCreatorId() throws Exception {
		// prepare: MGI created by ANONYMOUS_USER_ID
		CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID).withTenant(TENANT_ID);
		String mgiId = mgiDao.create(null, customGameConfigBuilder.build());
		
		// validate: MGI creator is ANONYMOUS_USER_ID
		assertGameCreator(mgiId, ANONYMOUS_USER_ID);
		
		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		moveMgiDao.changeGameCreatorId(mgiId, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		
		// validate: MGI creator is ANONYMOUS_USER_ID
		assertGameCreator(mgiId, REGISTERED_USER_ID);
	}
	
	private void assertGameCreator(String mgiId, String expectedCreatorId) {
		CustomGameConfig customGameConfig = mgiDao.getConfig(mgiId);
		assertThat(customGameConfig.getGameCreatorId(), equalTo(expectedCreatorId));
	}
}
