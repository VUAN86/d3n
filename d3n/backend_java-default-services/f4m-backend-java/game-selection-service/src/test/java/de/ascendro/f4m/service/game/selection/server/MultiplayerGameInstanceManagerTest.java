package de.ascendro.f4m.service.game.selection.server;

import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.END_DATE_TIME;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_ID_1;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.START_DATE_TIME;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.core.CombinableMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Sets;

import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.result.dao.CommonResultEngineDao;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.engine.exception.F4MGameFlowViolation;
import de.ascendro.f4m.service.game.selection.client.NotificationMessagePreparer;
import de.ascendro.f4m.service.game.selection.exception.GameAlreadyStartedByUserException;
import de.ascendro.f4m.service.game.selection.exception.SubscriptionNotFoundException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitedUser;
import de.ascendro.f4m.service.game.selection.model.schema.GameSelectionMessageSchemaMapper;
import de.ascendro.f4m.service.game.selection.subscription.SubscriptionManager;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserPermission;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.api.ApiProfileExtendedBasicInfo;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class MultiplayerGameInstanceManagerTest {

	private static final String NOTIFICATION_ID = "notificationId";
	private static final String USER_ID_1 = "user_id_1";
	private static final String USER_ID_2 = "user_id_2";
	private static final String USER_ID_3 = "user_id_3";
	public static final double USER_1_HANDICAP = 5.5;
	public static final double USER_2_HANDICAP = 10.5;
	public static final double USER_3_HANDICAP = 2.3;

	@Mock
	private CommonMultiplayerGameInstanceDao mgiDao;
	@Mock
	private SubscriptionManager subscriptionManager;
	@Mock
	private NotificationMessagePreparer notificationMessagePreparer;
	@Mock
	private GameAerospikeDao gameDao;
	@Mock
	private CommonProfileAerospikeDaoImpl profileDao;
	@Mock
	private EventSubscriptionStore eventSubscriptionStore;
	@Mock
	private GameSelectionMessageSchemaMapper gameSelectionMessageSchemaMapper;
	@Mock
	private CommonResultEngineDao commonResultEngineDao;
	@Mock
	private CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao;
	
	private Game game = new Game();
	
	@InjectMocks
	private MultiplayerGameInstanceManagerImpl mgiManager;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		game.setGameId(GAME_ID_1);
		game.setTitle("Game 1");
		game.setType(GameType.DUEL);
		game.setStartDateTime(DateTimeUtil.formatISODateTime(START_DATE_TIME));
		game.setEndDateTime(DateTimeUtil.formatISODateTime(END_DATE_TIME));
		when(gameDao.getGame(GAME_ID_1)).thenAnswer(new Answer<Game>() {
			@Override
			public Game answer(InvocationOnMock invocation) throws Throwable {
				return game;
			}
		});
		
		when(mgiDao.getGameInstancesCount(MGI_ID)).thenReturn(0);
	}
	
	@Test(expected = SubscriptionNotFoundException.class)
	public void testReceiveGameStartNotificationsForStartedMultiplayerGame() {
		when(mgiDao.getConfig(MGI_ID)).thenReturn(CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.forGame(GAME_ID_1).build(MGI_ID));
		when(mgiDao.getUserState(MGI_ID, ANONYMOUS_USER_ID)).thenReturn(MultiplayerGameInstanceState.STARTED);
		mgiManager.validateUserState(ANONYMOUS_USER_ID, MGI_ID);
	}

	@Test(expected = GameAlreadyStartedByUserException.class)
	public void testReceiveGameStartNotificationsAtStartedState() {
		when(eventSubscriptionStore.hasSubscription(MGI_ID)).thenReturn(true);
		when(mgiDao.getConfig(MGI_ID)).thenReturn(CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.forGame(GAME_ID_1).build(MGI_ID));
		when(mgiDao.getUserState(MGI_ID, ANONYMOUS_USER_ID)).thenReturn(MultiplayerGameInstanceState.STARTED);
		mgiManager.validateUserState(ANONYMOUS_USER_ID, MGI_ID);
	}
	
	@Test
	public void testReceiveGameStartNotificationsAtRegisteredState() {
		when(eventSubscriptionStore.hasSubscription(MGI_ID)).thenReturn(true);
		when(mgiDao.getConfig(MGI_ID)).thenReturn(CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.forGame(GAME_ID_1).build(MGI_ID));
		when(mgiDao.getUserState(MGI_ID, ANONYMOUS_USER_ID)).thenReturn(MultiplayerGameInstanceState.REGISTERED);
		
		assertTrue(mgiManager.validateUserState(ANONYMOUS_USER_ID, MGI_ID));
	}
	
	@Test(expected = SubscriptionNotFoundException.class)
	public void testReceiveGameStartNotificationsWhenNoTopic() {
		when(eventSubscriptionStore.hasSubscription(MGI_ID)).thenReturn(false);
		assertTrue(mgiManager.validateUserState(ANONYMOUS_USER_ID, MGI_ID));
	}
	
	@Test(expected = F4MGameFlowViolation.class)
	public void testReceiveGameStartNotificationsAtInvitedState() {
		when(eventSubscriptionStore.hasSubscription(MGI_ID)).thenReturn(true);
		when(mgiDao.getConfig(MGI_ID)).thenReturn(CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.forGame(GAME_ID_1).build(MGI_ID));
		when(mgiDao.getUserState(MGI_ID, ANONYMOUS_USER_ID)).thenReturn(MultiplayerGameInstanceState.INVITED);
		mgiManager.validateUserState(ANONYMOUS_USER_ID, MGI_ID);
	}

	@Test(expected = F4MEntryNotFoundException.class)
	public void validateConfirmInvitationNotFoundTest() {
		mgiManager.validateConfirmInvitation(ANONYMOUS_USER_ID, MGI_ID);
	}

	@Test(expected = F4MGameFlowViolation.class)
	public void validateConfirmInvitationWrongStatusTest() {
		when(mgiDao.getUserState(ANONYMOUS_USER_ID, MGI_ID)).thenReturn(MultiplayerGameInstanceState.EXPIRED);
		mgiManager.validateConfirmInvitation(ANONYMOUS_USER_ID, MGI_ID);
	}

	@Test
	public void validateConfirmInvitationTest() {
		when(mgiDao.getUserState(ANONYMOUS_USER_ID, MGI_ID)).thenReturn(MultiplayerGameInstanceState.INVITED);
		assertTrue(mgiManager.validateConfirmInvitation(ANONYMOUS_USER_ID, MGI_ID));
	}

	@Test
	public void testAddInvitationUserInfo() throws Exception {
		List<Profile> profiles = prepareTestProfilesWithHandicap();
		
		when(profileDao.getProfiles(anyList())).thenReturn(profiles);
		when(profileDao.getProfileExtendedBasicInfo(anyList())).thenCallRealMethod();
		
		List<Invitation> invitations = mockedInvitationList(USER_ID_1, USER_ID_2, USER_ID_3, USER_ID_1);
		
		mgiManager.addInvitationUserInfo(invitations, i -> i.getInviter().getUserId(), (i, p) -> i.setInviter(p));
		
		invitations.forEach(i -> {
			Profile profile = profiles.stream()
					.filter(p -> i.getInviter().getUserId().equals(p.getUserId()))
					.findFirst()
					.get(); // profile must be present
			ApiProfileExtendedBasicInfo inviterInfo = i.getInviter();
			assertThat(inviterInfo.getNickname(), equalTo(profile.getPersonWrapper().getNickname()));
			if (profile.isShowFullName()) {
				assertThat(inviterInfo.getFirstName(), equalTo(profile.getPersonWrapper().getFirstName()));
				assertThat(inviterInfo.getLastName(), equalTo(profile.getPersonWrapper().getLastName()));
			} else {
				assertNull(inviterInfo.getFirstName());
				assertNull(inviterInfo.getLastName());
			}
			assertThat(inviterInfo.getCountry(), equalTo(profile.getAddress().getCountry()));
			assertThat(inviterInfo.getCity(), equalTo(profile.getAddress().getCity()));
			assertThat(inviterInfo.getHandicap(), equalTo(profile.getHandicap()));
		});
	}
	
	private List<Invitation> mockedInvitationList(String... userIds) {
		return Arrays.stream(userIds)
				.map(id -> {
					Invitation invitation = new Invitation();
					invitation.getGame().setId(GAME_ID_1);
					invitation.getCreator().setUserId(id);
					invitation.getInviter().setUserId(id);
					return invitation;
				})
				.collect(Collectors.toList());
	}
	
	private Profile getProfile(String userId, String firstName, String lastName, String nickname, String country, String city) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		
		ProfileUser person = new ProfileUser();
		person.setFirstName(firstName);
		person.setLastName(lastName);
		person.setNickname(nickname);
		profile.setPersonWrapper(person);
		
		ProfileAddress address = new ProfileAddress();
		address.setCountry(country);
		address.setCity(city);
		profile.setAddress(address);
		
		return profile;
	}

	private Profile getProfile(String userId, String firstName, String lastName, String nickname, String country, String city, Double handicap) {
		Profile profile = getProfile(userId, firstName, lastName, nickname, country, city);
		profile.setHandicap(handicap);
		return profile;
	}
	@Test
	public void testValidateUserPermissionsForEntryFee() throws Exception {
		HashSet<String> permissions = Sets.newHashSet(UserPermission.GAME.name());
		when(gameSelectionMessageSchemaMapper.getMessagePermissions(anyString())).thenReturn(permissions);
		when(gameSelectionMessageSchemaMapper.getRolePermissions(any(String[].class))).thenReturn(permissions);

		Profile profile = mock(Profile.class);
		when(profile.isOver18()).thenReturn(true);
		when(profileDao.getProfile(USER_ID_1)).thenReturn(profile);
		
		CustomGameConfig entryFee = mock(CustomGameConfig.class);
		when(entryFee.isFree()).thenReturn(false);
		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.BONUS);
		
		mgiManager.validateUserPermissionsForEntryFee("any_message_name", new ClientInfo(USER_ID_1, new String[] { "any_user_role" }), entryFee);
		
		when(mgiDao.getConfig(MGI_ID)).thenReturn(entryFee);
		mgiManager.validateUserPermissionsForEntryFee("any_message_name", new ClientInfo(USER_ID_1, new String[] { "any_user_role" }), MGI_ID);
		
		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.CREDIT);
		try {
			mgiManager.validateUserPermissionsForEntryFee("any_message_name", new ClientInfo(USER_ID_1, new String[] { "any_user_role" }), entryFee);
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException irEx) {
			assertThat(irEx.getMessage(), equalTo("Missing game entry fee required permission [GAME_CREDIT] for currency [CREDIT]"));
		}
		
		try {
			mgiManager.validateUserPermissionsForEntryFee("any_message_name", new ClientInfo(USER_ID_1, new String[] { "any_user_role" }), MGI_ID);
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException irEx) {
			assertThat(irEx.getMessage(), equalTo("Missing game entry fee required permission [GAME_CREDIT] for currency [CREDIT]"));
		}
	}
	
	@Test
	public void testFilterInvitationsByResults() throws Exception {
		Invitation invitation = new Invitation();
		invitation.setMultiplayerGameInstanceId(MGI_ID);
		
		// invitation has results
		when(commonResultEngineDao.hasMultiplayerResults(MGI_ID)).thenReturn(Boolean.TRUE);
		assertThat(mgiManager.filterInvitationsByResults(Arrays.asList(invitation), true), emptyCollectionOf(Invitation.class));
		assertThat(mgiManager.filterInvitationsByResults(Arrays.asList(invitation), false), containsInAnyOrder(invitation));
		
		// invitation doesn't have results
		when(commonResultEngineDao.hasMultiplayerResults(MGI_ID)).thenReturn(Boolean.FALSE);
		assertThat(mgiManager.filterInvitationsByResults(Arrays.asList(invitation), true), containsInAnyOrder(invitation));
		assertThat(mgiManager.filterInvitationsByResults(Arrays.asList(invitation), false), emptyCollectionOf(Invitation.class));
	}
	
	@Test
	public void testAddInvitationOpponents() throws Exception {
		List<Profile> profiles = prepareTestProfilesWithHandicap();
		InvitedUser invitedUser1 = new InvitedUser(USER_ID_1, MultiplayerGameInstanceState.REGISTERED.name());
		InvitedUser calculatedUser2 = new InvitedUser(USER_ID_2, MultiplayerGameInstanceState.CALCULATED.name());
		InvitedUser cancelledUser3 = new InvitedUser(USER_ID_3, MultiplayerGameInstanceState.CANCELLED.name());

		when(profileDao.getProfileBasicInfo(anyString())).thenCallRealMethod();
		when(profileDao.getProfileExtendedBasicInfo(anyString())).thenCallRealMethod();

		when(profileDao.getProfileBasicInfo(anyList())).thenCallRealMethod();
		when(profileDao.getProfileExtendedBasicInfo(anyList())).thenCallRealMethod();
		when(profileDao.getProfiles(anyList())).thenReturn(profiles);
		when(mgiDao.getInvitedList(MGI_ID, Integer.MAX_VALUE, 0, Collections.emptyList(), Collections.emptyMap()))
				.thenReturn(Arrays.asList(invitedUser1, calculatedUser2, cancelledUser3));

		Invitation invitation = new Invitation();
		invitation.setMultiplayerGameInstanceId(MGI_ID);
		mgiManager.addInvitationOpponents(Arrays.asList(invitation), calculatedUser2.getUserId());
		
		assertThat(invitation.getOpponents(), hasSize(1));
		assertThat(invitation.getOpponents().get(0).getUserId(), equalTo(USER_ID_1));
		assertThat(invitation.getOpponents().get(0).getHandicap(), equalTo(USER_1_HANDICAP));
	}

	private List<Profile> prepareTestProfilesWithHandicap() {
		Profile profile1 = getProfile(USER_ID_1, "George W.", "Bush", "Bush 2.0", "United States", "New Haven", USER_1_HANDICAP);
		Profile profile2 = getProfile(USER_ID_2, "Barack", "Obama", "Barry O'Bomber", "United States", "Honolulu", USER_2_HANDICAP);
		Profile profile3 = getProfile(USER_ID_3, "Donald", "Trump", "John", "United States", "New York City", USER_3_HANDICAP);
		return Arrays.asList(profile1, profile2, profile3);
	}

	@Test
	public void testGetMillisToPlayDateTime() throws Exception {
		long minutes = 5;
		long millis = minutes * 60 * 1000;
		
		CustomGameConfig tournament = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.withGameType(GameType.TOURNAMENT)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().plusMinutes(minutes))
				.build();
		CustomGameConfig liveTournament = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.withGameType(GameType.LIVE_TOURNAMENT)
				.withPlayDateTime(DateTimeUtil.getCurrentDateTime().plusMinutes(minutes))
				.build();
		
		when(mgiDao.getConfig(MGI_ID)).thenReturn(tournament);
		assertThat(mgiManager.getMillisToPlayDateTime(MGI_ID), between(millis - 1000, millis)); // one second boundary
		
		when(mgiDao.getConfig(MGI_ID)).thenReturn(liveTournament);
		assertThat(mgiManager.getMillisToPlayDateTime(MGI_ID), between(millis - 1000, millis)); // one second boundary
	}

	private CombinableMatcher<Long> between(long from, long to) {
		return both(greaterThanOrEqualTo(from)).and(lessThanOrEqualTo(to));
	}
	
	@Test
	public void testSetScheduledNotification() {
		mgiManager.setScheduledNotification(MGI_ID, USER_ID_1, NOTIFICATION_ID);
		
		verify(mgiDao).setInvitationExpirationNotificationId(eq(MGI_ID), eq(USER_ID_1), eq(NOTIFICATION_ID));
	}
	
	@Test
	public void testSetScheduledNotificationNull() {
		mgiManager.setScheduledNotification(MGI_ID, USER_ID_1, null);
		
		verifyZeroInteractions(mgiDao);
	}
	
	@Test
	public void testCancelScheduledNotification() {
		when(mgiDao.getInvitationExpirationNotificationId(MGI_ID, USER_ID_1)).thenReturn(NOTIFICATION_ID);
		ClientInfo clientInfo = getClientInfo(USER_ID_1);
		mgiManager.cancelScheduledNotification(MGI_ID, USER_ID_1, clientInfo);
		
		verify(mgiDao).getInvitationExpirationNotificationId(eq(MGI_ID), eq(USER_ID_1));
		verify(notificationMessagePreparer).cancelInvitationExpirationNotification(eq(NOTIFICATION_ID), eq(USER_ID_1), eq(MGI_ID), eq(clientInfo));
	}
	
	@Test
	public void testResetScheduledNotification() {
		mgiManager.resetScheduledNotification(MGI_ID, USER_ID_1);
		
		verify(mgiDao).resetInvitationExpirationNotificationId(eq(MGI_ID), eq(USER_ID_1));
	}
	
	@Test
	public void testAcceptedDuelInvitation() {
		long minutes = 5;
		CustomGameConfig duel = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.forGame(GAME_ID_1)
				.withGameType(GameType.DUEL)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().plusMinutes(minutes))
				.build();
		when(mgiDao.getInviterId(MGI_ID, USER_ID_1, MultiplayerGameInstanceState.REGISTERED)).thenReturn(USER_ID_2);
		when(mgiDao.getConfig(MGI_ID)).thenReturn(duel);
		Map<String, String> map = new HashMap<>();
		map.put(USER_ID_1, MultiplayerGameInstanceState.INVITED.name());
		map.put(USER_ID_2, MultiplayerGameInstanceState.INVITED.name());
		map.put(USER_ID_3, MultiplayerGameInstanceState.INVITED.name());
		when(mgiDao.getAllUsersOfMgi(MGI_ID)).thenReturn(map);
		when(mgiDao.getInvitationExpirationNotificationId(eq(MGI_ID), any())).thenReturn(NOTIFICATION_ID);
		ClientInfo clientInfo = getClientInfo(USER_ID_1);
		
		mgiManager.acceptedInvitation(USER_ID_1, MGI_ID, clientInfo);
		
		verify(mgiDao).deleteNotAcceptedInvitations(eq(MGI_ID));
		ClientInfo expClientInfo1 = getClientInfo(USER_ID_1);
		expClientInfo1.setIp(null);
		verify(notificationMessagePreparer).cancelInvitationExpirationNotification(eq(NOTIFICATION_ID), eq(USER_ID_1), eq(MGI_ID), eq(expClientInfo1));
		ClientInfo expClientInfo2 = getClientInfo(USER_ID_2);
		expClientInfo2.setIp(null);
		verify(notificationMessagePreparer).cancelInvitationExpirationNotification(eq(NOTIFICATION_ID), eq(USER_ID_2), eq(MGI_ID), eq(expClientInfo2));
		ClientInfo expClientInfo3 = getClientInfo(USER_ID_3);
		expClientInfo3.setIp(null);
		verify(notificationMessagePreparer).cancelInvitationExpirationNotification(eq(NOTIFICATION_ID), eq(USER_ID_3), eq(MGI_ID), eq(expClientInfo3));
		
	}
	
	@Test
	public void testAcceptedUserTournamentsInvitation() {
		long minutes = 5;
		CustomGameConfig duel = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.forGame(GAME_ID_1)
				.withGameType(GameType.USER_TOURNAMENT)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().plusMinutes(minutes))
				.build();
		when(mgiDao.getInviterId(MGI_ID, USER_ID_1, MultiplayerGameInstanceState.REGISTERED)).thenReturn(USER_ID_2);
		when(mgiDao.getConfig(MGI_ID)).thenReturn(duel);
		Map<String, String> map = new HashMap<>();
		map.put(USER_ID_1, MultiplayerGameInstanceState.INVITED.name());
		when(mgiDao.getAllUsersOfMgi(MGI_ID)).thenReturn(map);
		when(mgiDao.getInvitationExpirationNotificationId(eq(MGI_ID), any())).thenReturn(NOTIFICATION_ID);
		ClientInfo clientInfo = getClientInfo(USER_ID_1);
		
		mgiManager.acceptedInvitation(USER_ID_1, MGI_ID, clientInfo);
		
		verify(mgiDao).deleteNotAcceptedInvitations(eq(MGI_ID));
		ClientInfo expClientInfo = getClientInfo(USER_ID_1);
		expClientInfo.setIp(null);
		verify(notificationMessagePreparer).cancelInvitationExpirationNotification(eq(NOTIFICATION_ID), eq(USER_ID_1), eq(MGI_ID), eq(expClientInfo));
	}
	
	@Test
	public void testDeclineInvitation() {
		when(mgiDao.getInviterId(USER_ID_1, MGI_ID, MultiplayerGameInstanceState.INVITED)).thenReturn(USER_ID_2);
		when(mgiDao.getInvitationExpirationNotificationId(eq(MGI_ID), eq(USER_ID_1))).thenReturn(NOTIFICATION_ID);
		ClientInfo clientInfo = getClientInfo(USER_ID_1);
		
		mgiManager.declineInvitation(USER_ID_1, MGI_ID, clientInfo);

		verify(notificationMessagePreparer).sendInvitationResponseNotification(eq(USER_ID_1), eq(false), eq(USER_ID_2), eq(MGI_ID), eq(clientInfo));
		verify(mgiDao).getInvitationExpirationNotificationId(eq(MGI_ID), eq(USER_ID_1));
		verify(notificationMessagePreparer).cancelInvitationExpirationNotification(eq(NOTIFICATION_ID), eq(USER_ID_1), eq(MGI_ID), eq(clientInfo));
		verify(mgiDao).declineInvitation(eq(USER_ID_1), eq(MGI_ID));
	}
	
	@Test
	public void testRejectInvitation() {
		when(mgiDao.getInviterId(USER_ID_1, MGI_ID, MultiplayerGameInstanceState.INVITED)).thenReturn(USER_ID_2);
		when(mgiDao.getInvitationExpirationNotificationId(eq(MGI_ID), eq(USER_ID_1))).thenReturn(NOTIFICATION_ID);
		ClientInfo clientInfo = getClientInfo(USER_ID_1);
		
		mgiManager.rejectInvitation(USER_ID_1, MGI_ID, clientInfo);

		verify(mgiDao).rejectInvitation(eq(USER_ID_1), eq(MGI_ID));
		verify(mgiDao).getInvitationExpirationNotificationId(eq(MGI_ID), eq(USER_ID_1));
		verify(notificationMessagePreparer).cancelInvitationExpirationNotification(eq(NOTIFICATION_ID), eq(USER_ID_1), eq(MGI_ID), eq(clientInfo));
		verify(mgiDao).rejectInvitation(eq(USER_ID_1), eq(MGI_ID));
	}

	private ClientInfo getClientInfo(String userId) {
		ClientInfo clientInfo = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		clientInfo.setUserId(userId);
		return clientInfo;
	}
}
