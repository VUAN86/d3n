package de.ascendro.f4m.server.multiplayer.dao;

import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.DELETED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.EXPIRED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.INVITED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.REGISTERED;
import static de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl.FINISHED_MGI_ID_BIN_NAME;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CreatedBy;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class CommonMultiplayerGameInstanceDaoImplTest extends RealAerospikeTestBase {

	private static final String NOTIFICATION_ID = "notificationId";
	private static final String NOTIFICATION_ID1 = "notificationId1";

	private static final String MGI_SET = "mgi";
	
	private static final String USER_ID_1 = "user_id_1";
	private static final String USER_ID_2 = "user_id_2";
	private static final String USER_ID_3 = "user_id_3";
	private static final ClientInfo CLIENT_INFO_1 = new ClientInfo("t1", "a1", USER_ID_1, "ip", 5.0);
	private static final ClientInfo CLIENT_INFO_3 = new ClientInfo("t1", "a1", USER_ID_3, "ip", 5.0);

	private static final String GAME_ID1 = "game_id_1";
	
	private static final String MGI_ID1 = "mgi_id_1";
	private static final String MGI_ID2 = "mgi_id_2";
	
	private static final String GAME_INSTANCE_ID1 = "game_instance_id_1";
	private static final String GAME_INSTANCE_ID3 = "game_instance_id_3";

	private final AerospikeConfigImpl aerospikeConfig = new AerospikeConfigImpl();
	private final F4MConfigImpl gameConfig = new F4MConfigImpl(aerospikeConfig, new GameConfigImpl());
	private final MultiplayerGameInstancePrimaryKeyUtil primaryKeyUtil = new MultiplayerGameInstancePrimaryKeyUtil(
			gameConfig);
	private final JsonUtil jsonUtil = new JsonUtil();

	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		
		gameConfig.setProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_RECORD_CAPACITY, 2L);
	}

	@Override
	protected void setUpAerospike() {
		commonMultiplayerGameInstanceDao = new CommonMultiplayerGameInstanceDaoImpl(gameConfig, 
				primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Test
	public void testOperateAdd() {
		String inviterId = "user1";
		String[] inviteesIds = { "user2", "user3", "user4", "user5", "user6", "user7" };

		String instanceId = commonMultiplayerGameInstanceDao.create(inviterId, new CustomGameConfig());

		Arrays.asList(inviteesIds).forEach(id -> commonMultiplayerGameInstanceDao.addUser(instanceId, id, inviterId, MultiplayerGameInstanceState.INVITED));
	}
	
	@Test
	public void testGetUserState(){
		final CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(USER_ID_1);

		//USER 1
		final String mgiId = commonMultiplayerGameInstanceDao.create(USER_ID_1, customGameConfigBuilder.build());
		assertEquals(MultiplayerGameInstanceState.INVITED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_1));
		
		commonMultiplayerGameInstanceDao.registerForGame(mgiId, CLIENT_INFO_1, GAME_INSTANCE_ID1);
		assertEquals(MultiplayerGameInstanceState.REGISTERED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_1));
		
		commonMultiplayerGameInstanceDao.joinGame(mgiId, GAME_INSTANCE_ID1, USER_ID_1, 0.5d);
		assertEquals(MultiplayerGameInstanceState.STARTED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_1));
		
		commonMultiplayerGameInstanceDao.markGameInstanceAsCalculated(mgiId, GAME_INSTANCE_ID1, USER_ID_1);
		assertEquals(MultiplayerGameInstanceState.CALCULATED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_1));
		
		//USER 2
		commonMultiplayerGameInstanceDao.addUser(mgiId, USER_ID_2, USER_ID_1, MultiplayerGameInstanceState.INVITED);
		assertEquals(MultiplayerGameInstanceState.INVITED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_2));
		
		commonMultiplayerGameInstanceDao.declineInvitation(USER_ID_2, mgiId);
		assertEquals(MultiplayerGameInstanceState.DECLINED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_2));
		
		//USER 3
		commonMultiplayerGameInstanceDao.addUser(mgiId, USER_ID_3, USER_ID_2, MultiplayerGameInstanceState.INVITED);
		assertEquals(MultiplayerGameInstanceState.INVITED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_3));
		
		commonMultiplayerGameInstanceDao.registerForGame(mgiId, CLIENT_INFO_3, GAME_INSTANCE_ID3);
		assertEquals(MultiplayerGameInstanceState.REGISTERED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_3));
		
		commonMultiplayerGameInstanceDao.cancelGameInstance(mgiId, GAME_INSTANCE_ID3, USER_ID_3);
		assertEquals(MultiplayerGameInstanceState.CANCELLED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_3));
		
		//ALL:final
		assertEquals(MultiplayerGameInstanceState.CALCULATED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_1));
		assertEquals(MultiplayerGameInstanceState.DECLINED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_2));
		assertEquals(MultiplayerGameInstanceState.CANCELLED, commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_3));
	}
	
	@Test
	public void testGetInvitationListByStates() throws Exception {
		// user2 invited for mgi1, registered for mgi2
		String mgi1Id = createTestMgi(null, USER_ID_1, USER_ID_2);
		String mgi2Id = createTestMgi(null, USER_ID_1, USER_ID_2);
		commonMultiplayerGameInstanceDao.registerForGame(mgi2Id, new ClientInfo(USER_ID_2), GAME_INSTANCE_ID1);
		
		List<Invitation> invited = getInvitationList(USER_ID_2, CreatedBy.ALL, INVITED);
		assertInvitationMgiIds(invited, mgi1Id);
		
		List<Invitation> registered = getInvitationList(USER_ID_2, CreatedBy.ALL, REGISTERED);
		assertInvitationMgiIds(registered, mgi2Id);
		
		List<Invitation> invitedAndRegistered = getInvitationList(USER_ID_2, CreatedBy.ALL, INVITED, REGISTERED);
		assertInvitationMgiIds(invitedAndRegistered, mgi1Id, mgi2Id);
	}
	
	private void assertInvitationMgiIds(List<Invitation> invitations, String... expectedMgiIds) {
		List<String> actualMgiIds = invitations.stream().map(i -> i.getMultiplayerGameInstanceId()).collect(Collectors.toList());
		assertThat(actualMgiIds, containsInAnyOrder(expectedMgiIds));
	}
	
	@Test
	public void testGetInvitationListByCreator() {
		createTestMgi(null, USER_ID_1, USER_ID_2, USER_ID_3);
		createTestMgi(null, USER_ID_2, USER_ID_3);
		createTestMgi(null, USER_ID_3, USER_ID_2, USER_ID_1);
		
		List<Invitation> invitedByDefault = getInvitationList(USER_ID_1, null, INVITED);
		assertInvitations(invitedByDefault, USER_ID_1, USER_ID_3);
		
		List<Invitation> invitedAll = getInvitationList(USER_ID_1, CreatedBy.ALL, INVITED);
		assertInvitations(invitedAll, USER_ID_1, USER_ID_3);
		
		List<Invitation> invitedByMyself = getInvitationList(USER_ID_1, CreatedBy.MYSELF, INVITED);
		assertInvitations(invitedByMyself, USER_ID_1);
		
		List<Invitation> invitedByOthers = getInvitationList(USER_ID_1, CreatedBy.OTHERS, INVITED);
		assertInvitations(invitedByOthers, USER_ID_3);
	}
	
	@Test
	public void testInvitationListWithoutExpiredGames() {
		ZonedDateTime currentDateTime = DateTimeUtil.getCurrentDateTime();
		createTestMgi(currentDateTime.minusMinutes(3), USER_ID_2, USER_ID_1);
		createTestMgi(currentDateTime.plusMinutes(3), USER_ID_3, USER_ID_1);
		
		List<Invitation> invitedAll = getInvitationList(USER_ID_1, CreatedBy.ALL, INVITED);
		assertInvitations(invitedAll, USER_ID_3);
	}

	private List<Invitation> getInvitationList(String userId, CreatedBy createdBy,
			MultiplayerGameInstanceState... states) {
		FilterCriteria filterCriteria = new FilterCriteria();
		filterCriteria.setLimit(100);
		return commonMultiplayerGameInstanceDao.getInvitationList(TENANT_ID, APP_ID, userId, Arrays.asList(states),
				createdBy, filterCriteria);
	}
	
	private void assertInvitations(List<Invitation> invitations, String... expectedCreatorIds) {
		assertThat(invitations, hasSize(expectedCreatorIds.length));
		List<String> actualCreatorIds = invitations.stream()
				.map(i -> i.getCreator().getUserId())
				.collect(Collectors.toList());
		assertThat(actualCreatorIds, containsInAnyOrder(expectedCreatorIds));
	}
	
	@Test
	public void testRejectInvitation() throws Exception {
		String mgiId = createTestMgi(null, USER_ID_1, USER_ID_2, USER_ID_3);
		assertThat(commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_2), equalTo(INVITED));
		assertThat(commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_3), equalTo(INVITED));
		assertThat(getInvitationList(USER_ID_2, CreatedBy.ALL, INVITED), hasSize(1));
		assertThat(getInvitationList(USER_ID_3, CreatedBy.ALL, INVITED), hasSize(1));
		
		// test INVITED -> EXPIRED
		commonMultiplayerGameInstanceDao.rejectInvitation(USER_ID_2, mgiId);
		assertThat(commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_2), equalTo(EXPIRED));
		assertThat(getInvitationList(USER_ID_2, CreatedBy.ALL, EXPIRED), hasSize(1));
		
		commonMultiplayerGameInstanceDao.deleteNotAcceptedInvitations(mgiId);
		assertThat(commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_3), equalTo(DELETED));

		// test DELETED -> EXPIRED
		commonMultiplayerGameInstanceDao.rejectInvitation(USER_ID_3, mgiId);
		assertThat(commonMultiplayerGameInstanceDao.getUserState(mgiId, USER_ID_3), equalTo(EXPIRED));
		assertThat(getInvitationList(USER_ID_3, CreatedBy.ALL, EXPIRED), hasSize(1));
	}
	
	private String createTestMgi(ZonedDateTime expiryDateTime, String creatorId, String... invitedUserIds) {
		CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(creatorId)
				.withTenant(TENANT_ID)
				.withApp(APP_ID)
				.withExpiryDateTime(expiryDateTime);
		String mgiId = commonMultiplayerGameInstanceDao.create(creatorId, customGameConfigBuilder.build());
		Stream.of(invitedUserIds).forEach(id -> commonMultiplayerGameInstanceDao.addUser(mgiId, id, creatorId, INVITED));
		return mgiId;
	}
	
	@Test
	public void testMapTournament() {
		commonMultiplayerGameInstanceDao.mapTournament(GAME_ID1, MGI_ID1);
		assertEquals(MGI_ID1, commonMultiplayerGameInstanceDao.getTournamentInstanceId(GAME_ID1));
		
		commonMultiplayerGameInstanceDao.mapTournament(GAME_ID1, MGI_ID2);
		assertEquals(MGI_ID2, commonMultiplayerGameInstanceDao.getTournamentInstanceId(GAME_ID1));

		//finished
		final String tournamentMappingKey = primaryKeyUtil.createTournamentMappingKey(GAME_ID1);
		final AerospikeOperateDao aerospikeOperateDao = (AerospikeOperateDao)commonMultiplayerGameInstanceDao;
		assertThat(aerospikeOperateDao.<String, Long>getByKeyFromMap(MGI_SET, tournamentMappingKey, FINISHED_MGI_ID_BIN_NAME, MGI_ID1),
				lessThan(DateTimeUtil.getUTCTimestamp() + 1));
		assertNull(aerospikeOperateDao.<String, Long>getByKeyFromMap(MGI_SET, tournamentMappingKey, FINISHED_MGI_ID_BIN_NAME, MGI_ID2));
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testSetInvitationExpirationNotificationId() {
		final CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(USER_ID_1)
				.withTenant(TENANT_ID)
				.withApp(APP_ID);

		final String mgiId = commonMultiplayerGameInstanceDao.create(USER_ID_1, customGameConfigBuilder.build());
		final String mgiId1 = commonMultiplayerGameInstanceDao.create(USER_ID_1, customGameConfigBuilder.build());
		
		commonMultiplayerGameInstanceDao.setInvitationExpirationNotificationId(mgiId, USER_ID_1, NOTIFICATION_ID);
		commonMultiplayerGameInstanceDao.setInvitationExpirationNotificationId(mgiId1, USER_ID_1, NOTIFICATION_ID1);
		
		AerospikeOperateDaoImpl dao = new AerospikeOperateDaoImpl(gameConfig, 
				primaryKeyUtil, jsonUtil, aerospikeClientProvider);
		String key = primaryKeyUtil.createInvitationListKey(TENANT_ID, APP_ID, USER_ID_1);
		Map<String, String> allMap = dao.getAllMap(gameConfig.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_SET), key, CommonMultiplayerGameInstanceDaoImpl.NOTIFICATION_BIN_NAME);
		
		assertEquals(2, allMap.size());
		assertThat(allMap.keySet(), containsInAnyOrder(mgiId, mgiId1));
		assertThat(allMap.values(), containsInAnyOrder(NOTIFICATION_ID, NOTIFICATION_ID1));
	}	
	
	@Test
	public void testGetInvitationExpirationNotificationId() {
		final CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(USER_ID_1)
				.withTenant(TENANT_ID)
				.withApp(APP_ID);

		final String mgiId = commonMultiplayerGameInstanceDao.create(USER_ID_1, customGameConfigBuilder.build());
		commonMultiplayerGameInstanceDao.setInvitationExpirationNotificationId(mgiId, USER_ID_1, NOTIFICATION_ID);
		
		String id = commonMultiplayerGameInstanceDao.getInvitationExpirationNotificationId(mgiId, USER_ID_1);
		
		assertEquals(NOTIFICATION_ID, id);
	}
	
	
	@Test
	public void testResetInvitationExpirationNotificationId() {
		final CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.create(USER_ID_1)
				.withTenant(TENANT_ID)
				.withApp(APP_ID);

		final String mgiId = commonMultiplayerGameInstanceDao.create(USER_ID_1, customGameConfigBuilder.build());
		final String mgiId1 = commonMultiplayerGameInstanceDao.create(USER_ID_1, customGameConfigBuilder.build());
		
		commonMultiplayerGameInstanceDao.setInvitationExpirationNotificationId(mgiId, USER_ID_1, NOTIFICATION_ID);
		commonMultiplayerGameInstanceDao.setInvitationExpirationNotificationId(mgiId1, USER_ID_1, NOTIFICATION_ID1);		
		
		commonMultiplayerGameInstanceDao.resetInvitationExpirationNotificationId(mgiId, USER_ID_1);
		AerospikeOperateDaoImpl<MultiplayerGameInstancePrimaryKeyUtil> dao = new AerospikeOperateDaoImpl<>(gameConfig, primaryKeyUtil, jsonUtil, aerospikeClientProvider);

		String key = primaryKeyUtil.createInvitationListKey(TENANT_ID, APP_ID, USER_ID_1);
		Map<String, String> allMap = dao.getAllMap(gameConfig.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_SET), key, CommonMultiplayerGameInstanceDaoImpl.NOTIFICATION_BIN_NAME);
		
		assertEquals(1, allMap.size());
		assertThat(allMap.keySet(), containsInAnyOrder(mgiId1));
		assertThat(allMap.values(), containsInAnyOrder(NOTIFICATION_ID1));
	}

}
