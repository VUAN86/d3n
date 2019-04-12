package de.ascendro.f4m.service.game.selection.integration;

import static de.ascendro.f4m.service.game.selection.builder.GameBuilder.createGame;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_INSTANCE_ID;
import static de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder.create;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.FULLY_REGISTERED_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.FULLY_REGISTERED_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Key;
import com.google.gson.JsonObject;
import com.google.inject.Injector;
import com.google.inject.Stage;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailRequest;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailResponse;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddForUserRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupListPlayersResponse;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.register.RegisterRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterResponse;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.builder.GameBuilder;
import de.ascendro.f4m.service.game.selection.exception.F4MGameInvitationNotValidException;
import de.ascendro.f4m.service.game.selection.exception.F4MGameParticipantCountExceededException;
import de.ascendro.f4m.service.game.selection.exception.GameSelectionExceptionCodes;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.MultiplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitedUser;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.CreatePublicGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InvitationListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteExternalPersonToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteGroupToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteUsersToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InvitedListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.JoinMultiplayerGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.PublicGameListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.RespondToInvitationResponse;
import de.ascendro.f4m.service.game.selection.model.notification.InvitationExpirationNotification;
import de.ascendro.f4m.service.game.selection.model.notification.InvitationNotification;
import de.ascendro.f4m.service.game.selection.model.notification.InvitationRespondNotification;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.game.selection.subscription.StartLiveUserTournamentEvent;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.internal.CreateJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.CancelUserPushRequest;
import de.ascendro.f4m.service.usermessage.model.SendUserPushRequest;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageResponse;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;


public class MultiplayerGameInstanceTest extends GameSelectionServiceTestBase {

	private static final String NOTIFICATION_ID = "notification-ID";
	private static final String NOTIFICATION_ID1 = "notification-ID1";
	private static final String NOTIFICATION_ID2 = "notification-ID2";
	private static final String NOTIFICATION_ID3 = "notification-ID3";

	private static final int TIME_TO_ACCEPT_INVITES_IN_MINUTES = 30;

	private static final String GAME_TITLE = "game-title";

	private static final String ANOTHER_APP_ID = "another-app";

	private static final String AEROSPIKE_NAMESPACE = "test";

	private static final Logger LOGGER = LoggerFactory.getLogger(MultiplayerGameInstanceTest.class);

	private static final String USER_ID_PATTERN = "userId-%d";

	private static final String BLOB_BIN_NAME = "value";

	private static final String NULL = "null";
	private static final String NOT_EXISTING_MGI_ID = "not_existing_mgi_id";
	private static final String GAME_ID = "game_id_1";
	private static final String DUEL_ID = "game_id_duel";
	private static final String USER_ID_0 = "user_id_0";
	private static final String USER_ID_1 = "user_id_1";
	private static final String USER_ID_2 = "user_id_2";
	private static final String USER_ID_3 = "user_id_3";
	private static final String USER_ID_4 = "user_id_4";
	private static final String PERSON_ID_1 = "person_id_1";
	private static final String GROUP_ID_1 = "group_id_1";
	
	private static final String FIRST_NAME = "Name";
	private static final String LAST_NAME = "van der Last Name";
	
	private static final int ELASTIC_PORT = 9212;
	
	private static CustomGameConfigBuilder CUSTOM_CONFIG_BUILDER;
	private static boolean INSUFFICIENT_FUNDS;

	private String mgiId = null;

	private GameBuilder gameBuilder;
	
	private JsonUtil jsonUtil;
	
	private CommonMultiplayerGameInstanceDao mgiDao;
	private CommonProfileAerospikeDao profileDao;
	private Config config;
	private GamePrimaryKeyUtil gamePrimaryKeyUtil;
	private MultiplayerGameInstanceManager mgiManager;
	private PublicGameElasticDao publicGameDao;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	private AerospikeClientProvider aerospikeClientProvider;

	@Mock
	private CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao;

	@BeforeClass
	public static void setUpClass() {
		F4MServiceWithMockIntegrationTestBase.setUpClass();
		System.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		System.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
	}
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);


	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		super.setUp();

		final Injector injector = jettyServerRule.getServerStartup().getInjector();
		
		jsonUtil = injector.getInstance(JsonUtil.class);
		
		mgiDao = injector.getInstance(CommonMultiplayerGameInstanceDao.class);
		profileDao = injector.getInstance(CommonProfileAerospikeDao.class);
		config = injector.getInstance(Config.class);
		gamePrimaryKeyUtil = injector.getInstance(GamePrimaryKeyUtil.class);
		mgiManager = injector.getInstance(MultiplayerGameInstanceManager.class);
		publicGameDao = injector.getInstance(PublicGameElasticDao.class);
		profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
		commonGameInstanceAerospikeDao = injector.getInstance(CommonGameInstanceAerospikeDao.class);
		aerospikeClientProvider = injector.getInstance(AerospikeClientProvider.class);

		gameBuilder = createGame(GAME_ID, GameType.USER_TOURNAMENT)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS);
		CUSTOM_CONFIG_BUILDER = create(TENANT_ID, APP_ID, GAME_ID, FULLY_REGISTERED_USER_ID);
		
		INSUFFICIENT_FUNDS = false;
		addProfile(FULLY_REGISTERED_USER_ID);
	}
	
	@Override
	protected ServiceStartup getServiceStartup() {
		return new GameSelectionStartupUsingAerospikeMock(Stage.PRODUCTION);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected JsonMessageContent onReceivedMessage(RequestContext requestContext) throws Exception {
		mockServiceReceivedMessageServerCollector.onProcess(requestContext);
		final JsonMessage<? extends JsonMessageContent> message = requestContext.getMessage();
		LOGGER.debug("Mocked Service received request {}", message.getContent());
		JsonMessageContent response;
		if (AuthMessageTypes.INVITE_USER_BY_EMAIL == message.getType(AuthMessageTypes.class)) {
			response = onRegisterEmail((InviteUserByEmailRequest) message.getContent());
		} else if (GameEngineMessageTypes.REGISTER == message.getType(GameEngineMessageTypes.class)) {
			response = onRegister((JsonMessage<RegisterRequest>) message);
		} else if (UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE == message.getType(UserMessageMessageTypes.class)) {
			response = getSendPushResponse();
		} else if (UserMessageMessageTypes.SEND_USER_PUSH == message.getType(UserMessageMessageTypes.class)) {
			response = getSendUserPushResponse();
		} else if (UserMessageMessageTypes.CANCEL_USER_PUSH == message.getType(UserMessageMessageTypes.class)) {
			response = getCancelUserPushResponse();
		} else if (message.getType(AuthMessageTypes.class) != null
				|| message.getType(EventMessageTypes.class) != null) {
			response = null;// ignore particular messages
		} else if (message.getType(PaymentMessageTypes.class) != null) {
			if (PaymentMessageTypes.CREATE_JACKPOT == message.getType(PaymentMessageTypes.class)) {
				response = new EmptyJsonMessageContent();
			}
			else if (PaymentMessageTypes.GET_JACKPOT == message.getType(PaymentMessageTypes.class)) {
					GetJackpotResponse jack = new GetJackpotResponse();
					jack.setBalance(new BigDecimal("10"));
					jack.setCurrency(Currency.CREDIT);
					response = jack;
			} else {
				response = null;// ignore other payment message
			}
		} else if (FriendManagerMessageTypes.GROUP_LIST_PLAYERS == message.getType(FriendManagerMessageTypes.class)) {
			response = onGroupListPlayers();
		} else if (FriendManagerMessageTypes.BUDDY_ADD_FOR_USER == message.getType(FriendManagerMessageTypes.class)) {
			response = onBuddyAddForUser();
		} else {
			LOGGER.error("Mocked Service received unexpected message [{}]", message);
			throw new UnexpectedTestException("Unexpected message");
		}
		
		return response;
	}

	private InviteUserByEmailResponse onRegisterEmail(InviteUserByEmailRequest content) throws IOException {
		errorCollector.checkThat(
				content.getInvitationPerson(),
				equalTo(StringUtils.joinWith(StringUtils.SPACE, FIRST_NAME, LAST_NAME).trim()));
		return new InviteUserByEmailResponse("person_id_1", "randomTokenJustForValidation");
	}
	
	private RegisterResponse onRegister(JsonMessage<RegisterRequest> registerRequestMessage) {
		final RegisterRequest registerRequest = (RegisterRequest) registerRequestMessage.getContent();
		if (!INSUFFICIENT_FUNDS) {
			final ClientInfo clientInfo = registerRequestMessage.getClientInfo();
			registerUser(registerRequest.getMgiId(), clientInfo.getUserId());
			return new RegisterResponse(GAME_INSTANCE_ID);
		} else {
			mgiId = registerRequest.getMgiId();
			throw new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS,
					"Not enough money");
		}
	}
	
	private SendWebsocketMessageResponse getSendPushResponse() {
		SendWebsocketMessageResponse response = new SendWebsocketMessageResponse(); 
		response.setMessageId("message_id_1");
		return response;
	}
	
	private JsonMessageContent getSendUserPushResponse() {
		SendUserPushResponse response = new SendUserPushResponse(); 
		response.setNotificationIds(new String[] {"notification_id_1"});
		return response;
	}

	private JsonMessageContent getCancelUserPushResponse() {
		return new EmptyJsonMessageContent();
	}
	
	private GroupListPlayersResponse onGroupListPlayers() {
		List<ApiProfile> players = Stream.of(FULLY_REGISTERED_USER_ID, USER_ID_0, USER_ID_1, USER_ID_2)
				.map(ApiProfile::new)
				.collect(Collectors.toList());
		return new GroupListPlayersResponse(players.size(), 0, players.size(), players);
	}

	private EmptyJsonMessageContent onBuddyAddForUser() {
		return new EmptyJsonMessageContent();
	}

	@Test
	public void testInviteUsersToGame() throws Exception {
		saveGame();
		final String[] invitees = new String[] { USER_ID_1, USER_ID_2 };
		final String[] pausedUsers = new String[] { USER_ID_3, USER_ID_4 };
		final MultiplayerGameParameters mgParams = CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();
		
		final String requestJson = testDataLoader.getInviteUsersToGameJson(invitees, mgParams, pausedUsers);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		
		final JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertNotNull(response.getContent());
		
		//Response
		final InviteUsersToGameResponse content = response.getContent();
		assertNotNull(content.getMultiplayerGameInstanceId());
		assertEquals(GAME_INSTANCE_ID, content.getGameInstanceId());
		assertEquals(invitees.length, content.getUsersIds().size());
		assertThat(Arrays.asList(invitees), containsInAnyOrder(content.getUsersIds().toArray()));
		assertNull(content.getPausedUsersIds());

		List<String> lastInvitedDuelOpponents = profileDao.getLastInvitedDuelOpponents(FULLY_REGISTERED_USER_ID, APP_ID);
		assertThat(lastInvitedDuelOpponents, hasSize(0));
		List<String> pausedDuelOpponents = profileDao.getPausedDuelOpponents(FULLY_REGISTERED_USER_ID, APP_ID);
		assertThat(pausedDuelOpponents, hasSize(0));
	}

	@Test
	public void testInviteUsersToDuelGame() throws Exception {
		saveGame(createGame(DUEL_ID, GameType.DUEL).withApplications(APP_ID)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS).build());
		final MultiplayerGameParameters mgParams = create(TENANT_ID, APP_ID, DUEL_ID, FULLY_REGISTERED_USER_ID).buildMultiplayerGameParameters();
		final String[] invitees = new String[] { USER_ID_1, USER_ID_2 };
		final String[] pausedUsers = new String[] { USER_ID_3, USER_ID_4 };

		final String requestJson = testDataLoader.getInviteUsersToGameJson(invitees, mgParams, pausedUsers);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);

		final JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertNotNull(response.getContent());

		//Response
		final InviteUsersToGameResponse content = response.getContent();
		assertNotNull(content.getMultiplayerGameInstanceId());
		assertEquals(GAME_INSTANCE_ID, content.getGameInstanceId());
		assertEquals(invitees.length, content.getUsersIds().size());
		assertThat(Arrays.asList(invitees), containsInAnyOrder(content.getUsersIds().toArray()));
		assertThat(Arrays.asList(pausedUsers), containsInAnyOrder(content.getPausedUsersIds().toArray()));

		List<String> lastInvitedDuelOpponents = profileDao.getLastInvitedDuelOpponents(FULLY_REGISTERED_USER_ID, APP_ID);
		assertThat(Arrays.asList(invitees), containsInAnyOrder(lastInvitedDuelOpponents.toArray()));

		List<String> pausedDuelOpponents = profileDao.getPausedDuelOpponents(FULLY_REGISTERED_USER_ID, APP_ID);
		assertThat(Arrays.asList(pausedUsers), containsInAnyOrder(pausedDuelOpponents.toArray()));
	}

	@Test
	public void testInviteUsersToGameUserLiveTournament() throws Exception {
		
		Game game = new Game();
		game.setGameId("game_ulv");
		
		GameTypeConfiguration gtc = new GameTypeConfiguration();
		MultiplayerGameTypeConfigurationData mt =new MultiplayerGameTypeConfigurationData();
		mt.setMinimumPlayerNeeded(2);
		gtc.setGameTournament(mt);
		game.setTypeConfiguration(gtc);
		game.setType(GameType.USER_LIVE_TOURNAMENT);
		game.setTitle("user live tournament test");
		saveGame(game);
		
		final String[] invitees = new String[] { USER_ID_1, USER_ID_2 };
		final MultiplayerGameParameters mgParams =  CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();
		mgParams.setGameId("game_ulv");
		ZonedDateTime playDateTime = DateTimeUtil.getCurrentDateTime().plusHours(9);
		mgParams.setPlayDateTime(playDateTime);
		
		final String requestJson = testDataLoader.getInviteUsersToGameJson(invitees, mgParams);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		
		final JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertNotNull(response.getContent());
		
		final InviteUsersToGameResponse content = response.getContent();
		String myMgiId = content.getMultiplayerGameInstanceId();
		assertNotNull(myMgiId);
		assertEquals(invitees.length, content.getUsersIds().size());
		CustomGameConfig savedConfig = mgiDao.getConfig(myMgiId);
		Comparator<ZonedDateTime> comparator = Comparator.comparing(zdt -> zdt.truncatedTo(ChronoUnit.SECONDS));
		assertEquals(0, comparator.compare(playDateTime, savedConfig.getPlayDateTime()));		
		
		List<JsonMessage<JsonMessageContent>> eventPublishList =  mockServiceReceivedMessageServerCollector.getMessagesByType(EventMessageTypes.PUBLISH);
		assertThat(eventPublishList, hasSize(1));
		StartLiveUserTournamentEvent notif = jsonUtil.fromJson(
				((PublishMessageContent) eventPublishList.get(0).getContent()).getNotificationContent(),
				StartLiveUserTournamentEvent.class);
		assertEquals(0, playDateTime.toLocalDate().compareTo(notif.getPlayDateTime().toLocalDate()));		
	}
	
	
	@Test
	public void testInviteUsersToPaidGame() throws Exception {
		gameBuilder.withFree(false)
		 	.withEntryFee("100", "0", "200", "1", Currency.CREDIT);
		saveGame();
		final String[] invitees = new String[] { USER_ID_1, USER_ID_2 };
		final MultiplayerGameParameters mgParams = CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();
		
		final String requestJson = testDataLoader.getInviteUsersToGameJson(invitees, mgParams);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		
		final JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertNotNull(response.getContent());
		
		//Response
		final InviteUsersToGameResponse inviteUsersToGameResponse = response.getContent();
		assertNotNull(inviteUsersToGameResponse.getMultiplayerGameInstanceId());
		assertEquals(GAME_INSTANCE_ID, inviteUsersToGameResponse.getGameInstanceId());
		assertEquals(invitees.length, inviteUsersToGameResponse.getUsersIds().size());
		assertThat(Arrays.asList(invitees), containsInAnyOrder(inviteUsersToGameResponse.getUsersIds().toArray()));
		
		//requested create jackpot
		final JsonMessage<CreateJackpotRequest> createJackpotMessage = mockServiceReceivedMessageServerCollector.getMessageByType(PaymentMessageTypes.CREATE_JACKPOT);
		assertMessageContentType(createJackpotMessage, CreateJackpotRequest.class);
		assertEquals(inviteUsersToGameResponse.getMultiplayerGameInstanceId(), createJackpotMessage.getContent().getMultiplayerGameInstanceId());
		assertEquals(KeyStoreTestUtil.TENANT_ID, createJackpotMessage.getContent().getTenantId());
		assertEquals(Currency.CREDIT, createJackpotMessage.getContent().getCurrency());
		
		//requested register
		final JsonMessage<RegisterRequest> registerMessage = mockServiceReceivedMessageServerCollector.getMessageByType(GameEngineMessageTypes.REGISTER);
		assertMessageContentType(registerMessage, RegisterRequest.class);
		assertEquals(inviteUsersToGameResponse.getMultiplayerGameInstanceId(), registerMessage.getContent().getMgiId());
	}
	
	@Test
	public void testActivateInvitations() throws Exception {
		// prepare MGI with USER_ID_0 and USER_ID_1 in state PENDING
		String mgiId = prepareMultiplayerGameInstance(FULLY_REGISTERED_USER_ID);
		mgiDao.addUser(mgiId, USER_ID_0, FULLY_REGISTERED_USER_ID, MultiplayerGameInstanceState.PENDING);
		mgiDao.addUser(mgiId, USER_ID_1, FULLY_REGISTERED_USER_ID, MultiplayerGameInstanceState.PENDING);
		
		// activate PENDING invitations
		String requestJson = getPlainTextJsonFromResources("activateInvitationsRequest.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<mgiId>>", mgiId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		
		// assert invite notifications
		assertInvitationNotificationPushRequest(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0, USER_ID_1);
		
		// assert invitation expiration notifications
		assertInvitationExpirationNotificationPushRequest(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0, USER_ID_1);
		
		// assert state of MGI users
		List<InvitedUser> invitedUsers = mgiDao.getInvitedList(mgiId, 100, 0, Collections.emptyList(), Collections.emptyMap());
		assertThat(invitedUsers, hasSize(3));
		assertTrue(containsInvitedUser(invitedUsers, FULLY_REGISTERED_USER_ID, MultiplayerGameInstanceState.REGISTERED));
		assertTrue(containsInvitedUser(invitedUsers, USER_ID_0, MultiplayerGameInstanceState.INVITED));
		assertTrue(containsInvitedUser(invitedUsers, USER_ID_1, MultiplayerGameInstanceState.INVITED));
	}
	
	private boolean containsInvitedUser(List<InvitedUser> invitedUsers, String userId,
			MultiplayerGameInstanceState state) {
		return invitedUsers.stream()
				.anyMatch(user -> user.getUserId().equals(userId) && user.getStatus().equals(state.name()));
	}

	private void assertInvitationNotificationPushRequest(String multiplayerGameInstanceId, String inviterId, final String... invitees) {
		RetriedAssert.assertWithWait(() -> assertThat(mockServiceReceivedMessageServerCollector
				.getMessagesByType(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE), hasSize(invitees.length)));
		
		//--invitees
		final List<SendWebsocketMessageRequest> notificationRequests = mockServiceReceivedMessageServerCollector
				.<SendWebsocketMessageRequest>getMessagesByType(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE).stream()
				.map(m -> m.getContent())
				.collect(Collectors.toList());
		assertThat(notificationRequests.stream().map(r -> r.getUserId()).collect(Collectors.toSet()), containsInAnyOrder(invitees));
				
		//--notification contents
		final List<InvitationNotification> notifications = notificationRequests.stream()
				.map(r -> jsonUtil.fromJson(r.getPayload(), InvitationNotification.class))
				.collect(Collectors.toList());
		assertThat(notifications, hasItem(new InvitationNotification(multiplayerGameInstanceId, inviterId)));
		notifications.forEach(n -> assertEquals(WebsocketMessageType.INVITATION_NOTIFICATION, n.getType()));
	}

	private void assertInvitationExpirationNotificationPushRequest(String multiplayerGameInstanceId, String inviterId, final String... invitees) {
		RetriedAssert.assertWithWait(() -> assertThat(mockServiceReceivedMessageServerCollector
				.getMessagesByType(UserMessageMessageTypes.SEND_USER_PUSH), hasSize(invitees.length)));
		
		//--invitees
		final List<SendUserPushRequest> notificationRequests = mockServiceReceivedMessageServerCollector
				.<SendUserPushRequest>getMessagesByType(UserMessageMessageTypes.SEND_USER_PUSH).stream()
				.map(m -> m.getContent())
				.collect(Collectors.toList());
		assertThat(notificationRequests.stream().map(r -> r.getUserId()).collect(Collectors.toSet()), containsInAnyOrder(invitees));
				
		//--notification contents
		final List<InvitationExpirationNotification> notifications = notificationRequests.stream()
				.map(r -> jsonUtil.fromJson(r.getPayload(), InvitationExpirationNotification.class))
				.collect(Collectors.toList());
		long expirationTimeMillis = DateTimeUtil.getCurrentDateTime().plusMinutes(TIME_TO_ACCEPT_INVITES_IN_MINUTES).toInstant().toEpochMilli();
		assertThat(notifications, hasSize(invitees.length));
		notifications.forEach(n -> assertEquals(WebsocketMessageType.INVITATION_EXPIRATION_NOTIFICATION, n.getType()));
		notifications.forEach(n -> assertEquals(multiplayerGameInstanceId, n.getMultiplayerGameInstanceId()));
		notifications.forEach(n -> assertEquals(inviterId, n.getInviterId()));
		notifications.forEach(n -> assertTrue(expirationTimeMillis >= n.getExpirationTimeMillis()));
	}
	
	private void assertInvitationRespondNotificationPushRequest(String multiplayerGameInstanceId, String inviteeId, boolean accept, String inviterId) {
		//--inviter
		final List<SendWebsocketMessageRequest> notificationRequests = mockServiceReceivedMessageServerCollector
				.<SendWebsocketMessageRequest>getMessagesByType(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE).stream()
				.map(m -> m.getContent())
				.collect(Collectors.toList());
		assertThat(notificationRequests.stream().map(r -> r.getUserId()).collect(Collectors.toSet()), hasItem(inviterId));
				
		//--notification contents
		final List<InvitationRespondNotification> notifications = notificationRequests.stream()
				.map(r -> jsonUtil.fromJson(r.getPayload(), InvitationRespondNotification.class))
				.collect(Collectors.toList());
		assertThat(notifications, hasItem(new InvitationRespondNotification(multiplayerGameInstanceId, inviteeId, accept, inviterId)));
		notifications.forEach(n -> assertEquals(WebsocketMessageType.INVITATION_RESPOND_NOTIFICATION, n.getType()));
	}
	
	private void assertCancelInvitationExpirationNotificationPushRequest(String mgi, String...notificationIds) {
		final List<CancelUserPushRequest> notificationRequests = mockServiceReceivedMessageServerCollector
				.<CancelUserPushRequest>getMessagesByType(UserMessageMessageTypes.CANCEL_USER_PUSH).stream()
				.map(m -> m.getContent())
				.collect(Collectors.toList());
		assertThat(notificationRequests.stream().map(r -> r.getMessageIds()[0]).collect(Collectors.toSet()), containsInAnyOrder(notificationIds));
	}
	
	private void assertNoCancelInvitationExpirationNotificationPushRequest(String mgiId, String...notificationIda) {
		final List<CancelUserPushRequest> notificationRequests = mockServiceReceivedMessageServerCollector
				.<CancelUserPushRequest>getMessagesByType(UserMessageMessageTypes.CANCEL_USER_PUSH).stream()
				.map(m -> m.getContent())
				.collect(Collectors.toList());
		assertThat(notificationRequests, not(containsInAnyOrder((Object[]) notificationIda)));
	}
	
	@Test
	public void testInviteUsersToGameWhenAlreadyInvited() throws Exception {
		saveGame();
		final String[] usersIds = new String[] { USER_ID_1, USER_ID_1, USER_ID_2 };
		final MultiplayerGameParameters mgParams = CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();
		
		final String requestJson = testDataLoader.getInviteUsersToGameJson(usersIds, mgParams);
		
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertNotNull(response.getContent());
		
		InviteUsersToGameResponse content = response.getContent();
		assertNotNull(content.getMultiplayerGameInstanceId());
		assertEquals(GAME_INSTANCE_ID, content.getGameInstanceId());
		final Set<String> uniqueUsersIds = new HashSet<>();
		Collections.addAll(uniqueUsersIds, usersIds);
		assertEquals(uniqueUsersIds.size(), content.getUsersIds().size());
		assertThat(uniqueUsersIds, containsInAnyOrder(content.getUsersIds().toArray()));
	}
	
	@Test
	public void testInviteUsersToGameToExistingMGI() throws Exception {
		final String[] usersIds = new String[] { USER_ID_1, USER_ID_2 };
		final String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		
		final String requestJson = testDataLoader.getInviteUsersToGameJson(usersIds, mgiId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertNotNull(response.getContent());
		
		InviteUsersToGameResponse content = response.getContent();
		assertEquals(mgiId, content.getMultiplayerGameInstanceId());
		assertNull(content.getGameInstanceId());
		assertEquals(usersIds.length, content.getUsersIds().size());
		assertThat(Arrays.asList(usersIds), containsInAnyOrder(content.getUsersIds().toArray()));
	}

	@Test
	public void testInviteUsersToGameToNotExistingMGI() throws Exception {
		final String[] userIds = new String[] { USER_ID_1, USER_ID_2 };
		
		final String requestJson = testDataLoader.getInviteUsersToGameJson(userIds, NOT_EXISTING_MGI_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		
		JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertResponseContainsError(response, ExceptionCodes.ERR_ENTRY_NOT_FOUND);
	}
	
	@Test
	public void testInviteUsersToGameWhenInsufficientFunds() throws Exception {
		INSUFFICIENT_FUNDS = true;
		errorCollector.setExpectedErrors(new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS, "any message"));
		
		saveGame();
		final String[] invitees = new String[] { USER_ID_1, USER_ID_2 };
		final MultiplayerGameParameters mgParams = CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();

		final String requestJson = testDataLoader.getInviteUsersToGameJson(invitees, mgParams);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		JsonMessage<InviteUsersToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertResponseContainsError(response, PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS);

		CustomGameConfig config = mgiDao.getConfig(mgiId);
		assertTrue(config.getExpiryDateTime().isBefore(DateTimeUtil.getCurrentDateTime()));
		assertEquals(MultiplayerGameInstanceState.EXPIRED, mgiDao.getUserState(mgiId, config.getGameCreatorId()));
	}
	
	@Test
	public void testInviteExternalPersonToGame() throws Exception {
		saveGame();
		MultiplayerGameParameters mgParams = CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();
		
		String requestJson = getPlainTextJsonFromResources("inviteExternalPersonToGame.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("\"<<multiplayerGameInstanceId>>\"", NULL)
				.replace("\"<<multiplayerGameParameters>>\"", jsonUtil.toJsonElement(mgParams).toString());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_EXTERNAL_PERSON_TO_GAME_RESPONSE);
		JsonMessage<InviteExternalPersonToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_EXTERNAL_PERSON_TO_GAME_RESPONSE);
		RetriedAssert.assertWithWait(() -> assertNotNull(response.getContent()));
		
		InviteExternalPersonToGameResponse content = response.getContent();
		assertNotNull(content.getMultiplayerGameInstanceId());
		assertEquals(GAME_INSTANCE_ID, content.getGameInstanceId());
		assertEquals(PERSON_ID_1, content.getPersonId());
	}
	
	@Test
	public void testInviteExternalPersonToGameToExistingMGI() throws Exception {
		final String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		
		final String requestJson = getPlainTextJsonFromResources("inviteExternalPersonToGame.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<multiplayerGameInstanceId>>", mgiId)
				.replace("\"<<multiplayerGameParameters>>\"", NULL);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_EXTERNAL_PERSON_TO_GAME_RESPONSE);

		final JsonMessage<InviteExternalPersonToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_EXTERNAL_PERSON_TO_GAME_RESPONSE);
		RetriedAssert.assertWithWait(() -> assertNotNull(response.getContent()));
		
		InviteExternalPersonToGameResponse content = response.getContent();
		assertNull(content.getGameInstanceId());
		assertEquals(mgiId, content.getMultiplayerGameInstanceId());
		assertEquals(PERSON_ID_1, content.getPersonId());
	}
	
	@Test
	public void testInviteGroupToCustomizedGame() throws Exception {
		saveGame();
		MultiplayerGameParameters mgParams = CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();
		
		String requestJson = getInviteGroupToGameJson(GROUP_ID_1, NULL, jsonUtil.toJsonElement(mgParams).toString());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_GROUP_TO_GAME_RESPONSE);
		
		JsonMessage<InviteGroupToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_GROUP_TO_GAME_RESPONSE);
		InviteGroupToGameResponse content = response.getContent();
		
		assertThat(content.getMultiplayerGameInstanceId(), not(nullValue()));
		assertThat(content.getGameInstanceId(), equalTo(GAME_INSTANCE_ID));
		assertThat(content.getUsersIds(), containsInAnyOrder(USER_ID_0, USER_ID_1, USER_ID_2));
		
		// wait while game is stored in elastic
		RetriedAssert.assertWithWait(() -> assertNotNull(publicGameDao.getPublicGame(APP_ID, content.getMultiplayerGameInstanceId())), 5 * 1000, 100);
	}
	
	@Test
	public void testInviteGroupToExistingGame() throws Exception {
		String mgiId = prepareMultiplayerGameInstance(FULLY_REGISTERED_USER_ID);
		
		String requestJson = getInviteGroupToGameJson(GROUP_ID_1, mgiId, NULL);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_GROUP_TO_GAME_RESPONSE);
		
		JsonMessage<InviteGroupToGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITE_GROUP_TO_GAME_RESPONSE);
		InviteGroupToGameResponse content = response.getContent();
		
		assertThat(content.getMultiplayerGameInstanceId(), equalTo(mgiId));
		assertThat(content.getGameInstanceId(), nullValue());
		assertThat(content.getUsersIds(), containsInAnyOrder(USER_ID_0, USER_ID_1, USER_ID_2));
	}
	
	private String getInviteGroupToGameJson(String groupId, String mgiId, String params) throws IOException {
		return getPlainTextJsonFromResources("inviteGroupToGame.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<groupId>>", groupId)
				.replace("\"<<multiplayerGameInstanceId>>\"", mgiId)
				.replace("\"<<multiplayerGameParameters>>\"", params);
	}
	
	@Test
	public void testCreatePublicGame() throws Exception {
		saveGame(createGame(DUEL_ID, GameType.DUEL).withApplications(APP_ID)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS).build());
		MultiplayerGameParameters mgParams = create(TENANT_ID, APP_ID, DUEL_ID, FULLY_REGISTERED_USER_ID).buildMultiplayerGameParameters();
		
		String requestJson = getPlainTextJsonFromResources("createPublicGame.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("\"<<multiplayerGameParameters>>\"", jsonUtil.toJsonElement(mgParams).toString());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.CREATE_PUBLIC_GAME_RESPONSE);
		JsonMessage<CreatePublicGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.CREATE_PUBLIC_GAME_RESPONSE);
		assertNotNull(response.getContent());
		
		CreatePublicGameResponse content = response.getContent();
		String mgiId = content.getMultiplayerGameInstanceId();
		assertNotNull(mgiId);
		assertEquals(GAME_INSTANCE_ID, content.getGameInstanceId());
		
		RetriedAssert.assertWithWait(() -> {
			Invitation publicGame = mgiManager.getPublicGame(APP_ID, mgiId);
			assertNotNull(publicGame);
			assertThat(publicGame.getPlayerGameReadiness(), equalTo(TestDataLoader.DUEL_PLAYER_READINESS));
			assertThat(publicGame.getEmailNotification(), equalTo(false));
		}, 5 * 1000, 100);
	}
	
	@Test
	public void testPublicGameList() throws Exception {
		String userTournamentId = UUID.randomUUID().toString();
		saveGame(createGame(userTournamentId,GameType.USER_TOURNAMENT)
				.withApplications(APP_ID)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS)
				.withFree(false).withEntryFee("1.00", "1.00", "2.00", "0.01", Currency.MONEY).build());
		createPublicGame(GameType.USER_TOURNAMENT, userTournamentId);
		//This is not exactly a correct assumption - jackpot creation must be also processed. But there should be engough time for that until verification of user tournament
		RetriedAssert.assertWithWait(() -> assertNotNull(mockServiceReceivedMessageServerCollector.getMessageByType(PaymentMessageTypes.CREATE_JACKPOT)));

		String duelId = savePublicGame(GameType.DUEL);
		gameBuilder.withType(GameType.DUEL);
		prepareMultiplayerGameInstance(USER_ID_0);//create private duel which must not be available
		String liveTournamentId = savePublicGame(GameType.LIVE_TOURNAMENT);
		String brokenLiveTournamentId = savePublicGame(GameType.LIVE_TOURNAMENT);
		deleteGame(brokenLiveTournamentId); //remove only the game
		
		testPublicGameListByGameType(null, duelId); // default filtering must be by game type DUEL
		testPublicGameListByGameType(GameType.DUEL, duelId);
		testPublicGameListByGameType(GameType.LIVE_TOURNAMENT, liveTournamentId);
		testPublicGameListByGameType(GameType.USER_TOURNAMENT, userTournamentId);
	}

	@Test
	public void testPublicGameListJackpot() throws Exception {

		String userTournamentId = UUID.randomUUID().toString() + "_0";
		createUserTournament(userTournamentId, false);
		String userTournamentId2 = UUID.randomUUID().toString() + "_1";
		createUserTournament(userTournamentId2, true);
		String userTournamentId3 = UUID.randomUUID().toString() + "_2";
		createUserTournament(userTournamentId3, false);
		String userTournamentId4 = UUID.randomUUID().toString() + "_3";
		createUserTournament(userTournamentId4, true);
		
		//This is not exactly a correct assumption - jackpot creation must be also processed. But there should be engough time for that until verification of user tournament
		RetriedAssert.assertWithWait(() -> assertNotNull(mockServiceReceivedMessageServerCollector.getMessageByType(PaymentMessageTypes.CREATE_JACKPOT)));

		String duelId = savePublicGame(GameType.DUEL);
		gameBuilder.withType(GameType.DUEL);
		prepareMultiplayerGameInstance(USER_ID_0);//create private duel which must not be available
		savePublicGame(GameType.LIVE_TOURNAMENT);
		String brokenLiveTournamentId = savePublicGame(GameType.LIVE_TOURNAMENT);
		deleteGame(brokenLiveTournamentId); //remove only the game
		testPublicGameListByGameType(null, duelId); // default filtering must be by game type DUEL
		testPublicGameListByGameType(GameType.DUEL, duelId);
		PublicGameListResponse content = getPublicGameListResponse(GameType.USER_TOURNAMENT);
		
		assertThat(content.getItems(), not(empty()));
		assertThat(content.getItems(), hasSize(4));

		int withoutJackpot = 0;
		int withJackpot = 0;
		
		for (Invitation invitation : content.getItems()){
			if (invitation.getGame().getJackpot() != null){
				withJackpot++;
				assertThat(invitation.getGame().getJackpot().getBalance(), equalTo(new BigDecimal("10")));
			} else {
				withoutJackpot++;
			}
		}
		
		assertEquals(2, withJackpot);
		assertEquals(2, withoutJackpot);
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}	
	
	private void createUserTournament(String userTournamentId,boolean freeGame) throws Exception{
		saveGame(createGame(userTournamentId,GameType.USER_TOURNAMENT)
				.withApplications(APP_ID)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS)
				.withFree(freeGame).withEntryFee("1.00", "1.00", "2.00", "0.01", Currency.MONEY).build());
		createPublicGame(GameType.USER_TOURNAMENT, userTournamentId);
	}
	
	private void deleteGame(String brokenLiveTournamentId) {
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);
		String key = gamePrimaryKeyUtil.createPrimaryKey(brokenLiveTournamentId);
		aerospikeClientProvider.get().delete(null, new Key(AEROSPIKE_NAMESPACE, set, key));
	}

	private void testPublicGameListByGameType(GameType gameType, String expectedGameId) throws Exception {
		PublicGameListResponse content = getPublicGameListResponse(gameType, expectedGameId);
				
		assertThat(content.getItems(), not(empty()));
		assertThat(content.getItems(), hasSize(1));
		assertThat(content.getItems().get(0).getGame().getId(), equalTo(expectedGameId));
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}
	
	private PublicGameListResponse getPublicGameListResponse(GameType gameType) throws Exception{
		String requestJson = getPlainTextJsonFromResources("publicGameList.json", FULLY_REGISTERED_CLIENT_INFO);
		if (gameType != null) {
			requestJson = requestJson.replace("\"<<gameType>>\"", gameType.name());
		} else {
			requestJson = requestJson.replace("\"gameType\": \"<<gameType>>\",", StringUtils.EMPTY);
		}
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.PUBLIC_GAME_LIST_RESPONSE);
		JsonMessage<PublicGameListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.PUBLIC_GAME_LIST_RESPONSE);
		PublicGameListResponse content = response.getContent();
		
		return content;
	}
	
	private PublicGameListResponse getPublicGameListResponse(GameType gameType, String expectedGameId) throws Exception{
		PublicGameListResponse content = getPublicGameListResponse(gameType);
		
		assertThat(content.getItems(), not(empty()));
		assertThat(content.getItems(), hasSize(1));
		assertThat(content.getItems().get(0).getGame().getId(), equalTo(expectedGameId));
		if (gameType == GameType.DUEL) {
			assertThat(content.getItems().get(0).getPlayerGameReadiness(), equalTo(TestDataLoader.DUEL_PLAYER_READINESS));
			assertThat(content.getItems().get(0).getEmailNotification(), equalTo(false));
		}
		if (gameType == GameType.LIVE_TOURNAMENT) {
			assertThat(content.getItems().get(0).getPlayerGameReadiness(), equalTo(TestDataLoader.TOURNAMENT_PLAYER_READINESS));
			assertThat(content.getItems().get(0).getEmailNotification(), equalTo(true));
		}

return content;

	}
	
	private String savePublicGame(GameType gameType) throws Exception {
		String gameId = UUID.randomUUID().toString();
		saveGame(createGame(gameId,gameType).withApplications(APP_ID)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS).build());
		return createPublicGame(gameType, gameId);
	}
	
	private String createPublicGame(GameType gameType, String gameId) {
		ClientInfo clientInfo = new ClientInfo(TENANT_ID, APP_ID, null, null, null);
		MultiplayerGameParameters mgParams = create(TENANT_ID, APP_ID, gameId, null).buildMultiplayerGameParameters();
		if (gameType.isDuel()) {
			mgiManager.createPublicGame(mgParams, clientInfo, null, null);
		} else if (gameType.isTournament()) {
			//create mgi in a manner of live tournaments - with empty sourceMessage and sourceSession
			mgiManager.createMultiplayerGameInstance(mgParams, clientInfo, null, null, false);
		}
		return gameId;
	}

	@Test
	public void validateInvitationTest() throws Exception {
		saveGame(createGame(DUEL_ID, GameType.DUEL)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS).build());
		MultiplayerGameParameters mgParams = create(TENANT_ID, APP_ID, DUEL_ID, FULLY_REGISTERED_USER_ID).buildMultiplayerGameParameters();
		
		int inviteeCount = 10;
		String[] userIds = IntStream.range(0, inviteeCount).mapToObj(i -> String.format(USER_ID_PATTERN, i)).toArray(String[]::new);
		
		final String inviteUsersToGameRequestJson = testDataLoader.getInviteUsersToGameJson(userIds, mgParams);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), inviteUsersToGameRequestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		final JsonMessage<InviteUsersToGameResponse> inviteResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertEquals(GameSelectionExceptionCodes.ERR_GAME_INVITATION_NOT_VALID, inviteResponseMessage.getError().getCode());
	}
	
	@Test
	public void testDeclineInvitation() throws Exception {
		final String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0);
		setInvitationExpirationNotificationId(mgiId, FULLY_REGISTERED_USER_ID, NOTIFICATION_ID);
		
		final String requestJson = testDataLoader.getRespondToInvitationJson(mgiId, false);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);

		final JsonMessage<RespondToInvitationResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);
		assertNotNull(response.getContent());
		assertNull(response.getContent().getGameInstanceId());
		
		assertInvitationRespondNotificationPushRequest(mgiId, FULLY_REGISTERED_USER_ID, false, USER_ID_0);
		assertCancelInvitationExpirationNotificationPushRequest(mgiId, NOTIFICATION_ID);
	}

	@Test
	public void testDeclineInvitationWhenRegistered() throws Exception {
		errorCollector.setExpectedErrors(new F4MGameInvitationNotValidException("anyMessage"));
		
		final String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0);
		registerUser(mgiId, FULLY_REGISTERED_USER_ID);
		setInvitationExpirationNotificationId(mgiId, FULLY_REGISTERED_USER_ID, NOTIFICATION_ID);
		
		final String requestJson = testDataLoader.getRespondToInvitationJson(mgiId, false);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);

		JsonMessage<RespondToInvitationResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);
		assertResponseContainsError(response, GameSelectionExceptionCodes.ERR_GAME_INVITATION_NOT_VALID);
		
		Map<String, String> allInvitedUsers = mgiDao.getAllUsersOfMgi(mgiId);
		assertEquals(MultiplayerGameInstanceState.REGISTERED.name(), allInvitedUsers.get(FULLY_REGISTERED_USER_ID));
		assertNoCancelInvitationExpirationNotificationPushRequest(mgiId, NOTIFICATION_ID);
	}

	@Test
	public void testAcceptInvitation() throws Exception {
		final String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0);
		setInvitationExpirationNotificationId(mgiId, USER_ID_0, NOTIFICATION_ID1);
		setInvitationExpirationNotificationId(mgiId, FULLY_REGISTERED_USER_ID, NOTIFICATION_ID);
		
		final String requestJson = testDataLoader.getRespondToInvitationJson(mgiId, true);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);

		final JsonMessage<RespondToInvitationResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);
		assertNotNull(response.getContent());
		assertEquals(GAME_INSTANCE_ID, response.getContent().getGameInstanceId());
		
		assertInvitationRespondNotificationPushRequest(mgiId, FULLY_REGISTERED_USER_ID, true, USER_ID_0);
		assertCancelInvitationExpirationNotificationPushRequest(mgiId, NOTIFICATION_ID);
		
		// assert that inviter is added as buddy
		JsonMessage<BuddyAddForUserRequest> buddyAddForUserRequest = mockServiceReceivedMessageServerCollector.getMessageByType(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER);
		String inviterId = buddyAddForUserRequest.getContent().getUserId();
		String[] buddyIds = buddyAddForUserRequest.getContent().getUserIds();
		assertThat(inviterId, equalTo(USER_ID_0));
		assertThat(Arrays.asList(buddyIds), containsInAnyOrder(FULLY_REGISTERED_USER_ID));
	}
	
	@Test
	public void testAcceptInvitationWhenParticipantCountExceeded() throws Exception {
		errorCollector.setExpectedErrors(new F4MGameParticipantCountExceededException("any message"));
		
		gameBuilder.withType(GameType.DUEL);
		String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		
		inviteUser(mgiId, USER_ID_1, USER_ID_0);
		registerUser(mgiId, USER_ID_1);
		inviteUser(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0);
		
		String requestJson = testDataLoader.getRespondToInvitationJson(mgiId, true);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);
		
		JsonMessage<RespondToInvitationResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);
		assertResponseContainsError(response, GameSelectionExceptionCodes.ERR_GAME_PARTICIPANT_COUNT_EXCEEDED);
	}

	@Test
	public void testAcceptInvitationForDuel() throws Exception {
		gameBuilder.withType(GameType.DUEL);
		final String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId, USER_ID_1, USER_ID_0);
		inviteUser(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0);
		inviteUser(mgiId, USER_ID_2, USER_ID_0);
		setInvitationExpirationNotificationId(mgiId, USER_ID_0, NOTIFICATION_ID1);
		setInvitationExpirationNotificationId(mgiId, FULLY_REGISTERED_USER_ID, NOTIFICATION_ID);
		setInvitationExpirationNotificationId(mgiId, USER_ID_1, NOTIFICATION_ID2);
		setInvitationExpirationNotificationId(mgiId, USER_ID_2, NOTIFICATION_ID3);
		
		final String requestJson = testDataLoader.getRespondToInvitationJson(mgiId, true);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);
		
		final JsonMessage<RespondToInvitationResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE);
		assertNotNull(response.getContent());
		assertEquals(GAME_INSTANCE_ID, response.getContent().getGameInstanceId());
		
		final Map<String, String> allInvitedUsers = mgiDao.getAllUsersOfMgi(mgiId);
		assertEquals(4, allInvitedUsers.size());
		assertEquals(MultiplayerGameInstanceState.REGISTERED.name(), allInvitedUsers.get(USER_ID_0));
		assertEquals(MultiplayerGameInstanceState.REGISTERED.name(), allInvitedUsers.get(FULLY_REGISTERED_USER_ID));
		assertEquals(MultiplayerGameInstanceState.DELETED.name(), allInvitedUsers.get(USER_ID_1));
		assertEquals(MultiplayerGameInstanceState.DELETED.name(), allInvitedUsers.get(USER_ID_2));
		
		assertInvitationRespondNotificationPushRequest(mgiId, FULLY_REGISTERED_USER_ID, true, USER_ID_0);
		assertCancelInvitationExpirationNotificationPushRequest(mgiId, NOTIFICATION_ID, NOTIFICATION_ID1, NOTIFICATION_ID2, NOTIFICATION_ID3);
	}
	
	@Test
	public void testInvitationList() throws Exception {
		String mgiId1 = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId1, FULLY_REGISTERED_USER_ID, USER_ID_0);
		String mgiId2 = prepareMultiplayerGameInstance(USER_ID_1);
		inviteUser(mgiId2, FULLY_REGISTERED_USER_ID, USER_ID_1);
		String mgiId3 = prepareMultiplayerGameInstance(USER_ID_2);
		inviteUser(mgiId3, FULLY_REGISTERED_USER_ID, USER_ID_2);
		
		ClientInfo clientInfo = FULLY_REGISTERED_CLIENT_INFO;
		InvitationListResponse content = getInvitationList(clientInfo);
		
		assertInvitationList(content, mgiId1, mgiId2, mgiId3);
	}
	
	@Test
	public void testInvitationListAnotherApp() throws Exception {
		String mgiId1 = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId1, FULLY_REGISTERED_USER_ID, USER_ID_0);
		String mgiId2 = prepareMultiplayerGameInstance(USER_ID_1);
		inviteUser(mgiId2, FULLY_REGISTERED_USER_ID, USER_ID_1);
		String mgiId3 = prepareMultiplayerGameInstance(USER_ID_2);
		inviteUser(mgiId3, FULLY_REGISTERED_USER_ID, USER_ID_2);
		
		ClientInfo clientInfo = ClientInfo.cloneOf(FULLY_REGISTERED_CLIENT_INFO);
		clientInfo.setAppId(ANOTHER_APP_ID);
		InvitationListResponse content = getInvitationList(clientInfo);
		
		assertInvitationList(content);
	}

	private void assertInvitationList(InvitationListResponse content, String...expectedMgiIds) {
		assertNotNull(content);
		assertEquals(expectedMgiIds.length, content.getTotal());
		if (expectedMgiIds.length > 0) {
			List<Invitation> invitations = content.getItems();
			List<String> mgiIds = invitations.stream()
					.map(Invitation::getMultiplayerGameInstanceId)
					.collect(Collectors.toList());
			assertThat(Arrays.asList(expectedMgiIds), containsInAnyOrder(mgiIds.toArray()));
		}
	}

	private InvitationListResponse getInvitationList(ClientInfo clientInfo) throws IOException, URISyntaxException {
		String requestJson = getPlainTextJsonFromResources("invitationList.json", clientInfo)
				.replace("<<states>>", MultiplayerGameInstanceState.INVITED.toString())
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", StringUtils.EMPTY)
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(new HashMap<>()));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		JsonMessage<InvitationListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		InvitationListResponse content = response.getContent();
		return content;
	}
	
	@Test
	public void testInvitationListWithGameInstanceIds() throws Exception {
		prepareTestGameInstance();
		String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId, FULLY_REGISTERED_USER_ID, USER_ID_0);
		registerUser(mgiId, FULLY_REGISTERED_USER_ID);
		
		String requestJson = getPlainTextJsonFromResources("invitationList.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<states>>", MultiplayerGameInstanceState.REGISTERED.name())
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", StringUtils.EMPTY)
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(new HashMap<>()));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		JsonMessage<InvitationListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		InvitationListResponse content = response.getContent();
		
		assertNotNull(content);
		assertEquals(1, content.getTotal());
		Invitation invitation = content.getItems().get(0);
		assertThat(invitation.getMultiplayerGameInstanceId(), equalTo(mgiId));
		assertThat(invitation.getGameInstanceId(), equalTo(GAME_INSTANCE_ID));
		assertThat(invitation.getGameInstance().getGameInstanceId(), equalTo(GAME_INSTANCE_ID));
		assertThat(invitation.getGameInstance().getStatus(), equalTo((GameStatus.READY_TO_PLAY).toString()));
		assertNull(invitation.getGame().getAssignedPools());
	}
	
	@Test
	public void testInvitationListOrdered() throws Exception {
		gameBuilder.withFree(false).withEntryFeeDecidedByPlayer(true)
			.withEntryFee("1.00", "1.00", "2.00", "0.01", Currency.MONEY)
			.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS)
			.withType(GameType.LIVE_TOURNAMENT);
		
		CUSTOM_CONFIG_BUILDER.withEntryFee(new BigDecimal("1.10"), Currency.MONEY);
		String mgiId1 = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId1, FULLY_REGISTERED_USER_ID, USER_ID_0);
		
		CUSTOM_CONFIG_BUILDER.withEntryFee(new BigDecimal("1.11"), Currency.MONEY);
		String mgiId2 = prepareMultiplayerGameInstance(USER_ID_1);
		inviteUser(mgiId2, FULLY_REGISTERED_USER_ID, USER_ID_1);
		
		CUSTOM_CONFIG_BUILDER.withEntryFee(new BigDecimal("1.01"), Currency.MONEY);
		String mgiId3 = prepareMultiplayerGameInstance(USER_ID_2);
		inviteUser(mgiId3, FULLY_REGISTERED_USER_ID, USER_ID_2);
		
		Map<String, String> orderBy = new HashMap<>();
		orderBy.put("field", "entryFeeAmount");
		orderBy.put("direction", "asc");
		
		String requestJson = getPlainTextJsonFromResources("invitationList.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<states>>", MultiplayerGameInstanceState.INVITED.toString())
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", jsonUtil.toJson(orderBy))
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(new HashMap<>()));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		JsonMessage<InvitationListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		InvitationListResponse content = response.getContent();
		
		assertNotNull(content);
		assertEquals(3, content.getTotal());
		
		List<Invitation> invitations = content.getItems();
		List<String> mgiIds = invitations.stream()
				.map(Invitation::getMultiplayerGameInstanceId)
				.collect(Collectors.toList());
		assertThat(Arrays.asList(mgiId3, mgiId1, mgiId2), contains(mgiIds.toArray()));
		assertEquals(TestDataLoader.TOURNAMENT_PLAYER_READINESS, invitations.get(0).getPlayerGameReadiness());
		assertEquals(true, invitations.get(0).getEmailNotification());
	}
	
	@Test
	public void testInvitationListWithSearchBy() throws Exception {
		gameBuilder.withFree(false)
			.withEntryFeeDecidedByPlayer(true)
			.withEntryFee("1.00", "0.90", "1.00", "0.01", Currency.MONEY);
		
		CUSTOM_CONFIG_BUILDER.withEntryFee(new BigDecimal("0.99"), Currency.MONEY);
		String mgiId1 = prepareMultiplayerGameInstance(USER_ID_0);
		inviteUser(mgiId1, FULLY_REGISTERED_USER_ID, USER_ID_0);
		
		CUSTOM_CONFIG_BUILDER.withEntryFee(new BigDecimal("1.00"), Currency.MONEY);
		String mgiId2 = prepareMultiplayerGameInstance(USER_ID_1);
		inviteUser(mgiId2, FULLY_REGISTERED_USER_ID, USER_ID_1);
		
		CUSTOM_CONFIG_BUILDER.withEntryFee(new BigDecimal("0.99"), Currency.MONEY);
		String mgiId3 = prepareMultiplayerGameInstance(USER_ID_2);
		inviteUser(mgiId3, FULLY_REGISTERED_USER_ID, USER_ID_2);
		
		Map<String, String> searchBy = new HashMap<>();
		searchBy.put("entryFeeAmount", "0.99");
		
		String requestJson = getPlainTextJsonFromResources("invitationList.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<states>>", MultiplayerGameInstanceState.INVITED.toString())
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", StringUtils.EMPTY)
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(searchBy));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		JsonMessage<InvitationListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		InvitationListResponse content = response.getContent();
		
		assertNotNull(content);
		assertEquals(2, content.getTotal());
		
		List<Invitation> invitations = content.getItems();
		List<String> mgiIds = invitations.stream()
				.map(Invitation::getMultiplayerGameInstanceId)
				.collect(Collectors.toList());
		assertThat(Arrays.asList(mgiId1, mgiId3), containsInAnyOrder(mgiIds.toArray()));
	}
	
	@Test
	public void testInvitationListWithJackpot() throws Exception {
		String mgiId1 = prepareMultiplayerGameInstance(USER_ID_0,false);
		inviteUser(mgiId1, FULLY_REGISTERED_USER_ID, USER_ID_0);
		String requestJson = getPlainTextJsonFromResources("invitationList.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<states>>", MultiplayerGameInstanceState.INVITED.toString())
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", StringUtils.EMPTY)
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(new HashMap<>()));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		JsonMessage<InvitationListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE);
		InvitationListResponse content = response.getContent();
		assertNotNull(content);
		assertEquals(1, content.getTotal());
		
		Invitation invitation = content.getItems().get(0);
		assertThat(invitation.getMultiplayerGameInstanceId(), equalTo(mgiId1));
		assertThat(invitation.getGame().getJackpot().getBalance() , equalTo(new BigDecimal("10")));
		assertThat(invitation.getGame().getJackpot().getCurrency(), equalTo(Currency.CREDIT));
	}
	
	
	@Test
	public void testInvitedList() throws Exception {
		String mgiId = prepareMultiplayerGameInstance(FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_0, FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_1, FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_2, USER_ID_1);
		
		String requestJson = getPlainTextJsonFromResources("invitedList.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<multiplayerGameInstanceId>>", mgiId)
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", StringUtils.EMPTY)
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(new HashMap<>()));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITED_LIST_RESPONSE);
		JsonMessage<InvitedListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITED_LIST_RESPONSE);
		InvitedListResponse content = response.getContent();
		
		assertNotNull(content);
		assertEquals(4, content.getTotal());
		
		List<String> usersIds = content.getItems().stream()
				.map(InvitedUser::getUserId)
				.collect(Collectors.toList());
		assertThat(Arrays.asList(FULLY_REGISTERED_USER_ID, USER_ID_0, USER_ID_1, USER_ID_2), containsInAnyOrder(usersIds.toArray()));
	}
	
	@Test
	public void testInvitedListOrdered() throws Exception {
		String mgiId = prepareMultiplayerGameInstance(FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_0, FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_1, FULLY_REGISTERED_USER_ID);
		mgiDao.declineInvitation(USER_ID_0, mgiId);
		
		Map<String, String> orderBy = new HashMap<>();
		orderBy.put("field", "status");
		orderBy.put("direction", "desc");
		
		String requestJson = getPlainTextJsonFromResources("invitedList.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<multiplayerGameInstanceId>>", mgiId)
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", jsonUtil.toJson(orderBy))
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(new HashMap<>()));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITED_LIST_RESPONSE);
		JsonMessage<InvitedListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITED_LIST_RESPONSE);
		InvitedListResponse content = response.getContent();
		
		assertNotNull(content);
		assertEquals(3, content.getTotal());
		
		List<String> usersIds = content.getItems().stream()
				.map(InvitedUser::getUserId)
				.collect(Collectors.toList());
		assertThat(Arrays.asList(FULLY_REGISTERED_USER_ID, USER_ID_1, USER_ID_0), contains(usersIds.toArray()));
	}
	
	@Test
	public void testInvitedListWithSearchBy() throws Exception {
		String mgiId = prepareMultiplayerGameInstance(FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_0, FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_1, FULLY_REGISTERED_USER_ID);
		inviteUser(mgiId, USER_ID_2, USER_ID_1);
		mgiDao.declineInvitation(USER_ID_1, mgiId);
		
		Map<String, String> searchBy = new HashMap<>();
		searchBy.put("status", MultiplayerGameInstanceState.INVITED.toString());
		
		String requestJson = getPlainTextJsonFromResources("invitedList.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<multiplayerGameInstanceId>>", mgiId)
				.replace("\"<<limit>>\"", "100")
				.replace("\"<<offset>>\"", "0")
				.replace("\"<<orderBy>>\"", StringUtils.EMPTY)
				.replace("\"<<searchBy>>\"", jsonUtil.toJson(searchBy));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITED_LIST_RESPONSE);
		JsonMessage<InvitedListResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.INVITED_LIST_RESPONSE);
		InvitedListResponse content = response.getContent();
		
		assertNotNull(content);
		assertEquals(2, content.getTotal());
		
		List<String> usersIds = content.getItems().stream()
				.map(InvitedUser::getUserId)
				.collect(Collectors.toList());
		assertThat(Arrays.asList(USER_ID_0, USER_ID_2), containsInAnyOrder(usersIds.toArray()));
	}
	
	@Test
	public void testJoinMultiplayerGameInstance() throws Exception {
		String mgiId = prepareMultiplayerGameInstance(USER_ID_0);
		
		String requestJson = getPlainTextJsonFromResources("joinMultiplayerGame.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<mgiId>>", mgiId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		JsonMessage<JoinMultiplayerGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		JoinMultiplayerGameResponse content = response.getContent();
		
		assertNotNull(content);
		assertEquals(GAME_INSTANCE_ID, content.getGameInstanceId());
	}

	@Test
	public void testJoinMultiplayerGameWhenInsufficientFundsThenTryAgain() throws Exception {
		INSUFFICIENT_FUNDS = true;
		errorCollector.setExpectedErrors(new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS, "any message"));

		String mgiId = prepareMultiplayerGameInstance(USER_ID_0);

		String requestJson = getPlainTextJsonFromResources("joinMultiplayerGame.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<mgiId>>", mgiId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		JsonMessage<JoinMultiplayerGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		assertResponseContainsError(response, PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS);

		INSUFFICIENT_FUNDS = false;
		testClientReceivedMessageCollector.clearReceivedMessageList();

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		JoinMultiplayerGameResponse content = response.getContent();

		assertNotNull(content);
		assertEquals(GAME_INSTANCE_ID, content.getGameInstanceId());
	}

	@Test
	public void testJoinMultiplayerGameWhenParticipantCountExceeded() throws Exception {
		errorCollector.setExpectedErrors(new F4MGameParticipantCountExceededException("any message"));

		gameBuilder.withType(GameType.DUEL);
		String mgiId = prepareMultiplayerGameInstance(USER_ID_0);

		inviteUser(mgiId, USER_ID_1, USER_ID_0);
		registerUser(mgiId, USER_ID_1);

		String requestJson = getPlainTextJsonFromResources("joinMultiplayerGame.json", FULLY_REGISTERED_CLIENT_INFO)
				.replace("<<mgiId>>", mgiId);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		JsonMessage<JoinMultiplayerGameResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE);
		assertResponseContainsError(response, GameSelectionExceptionCodes.ERR_GAME_PARTICIPANT_COUNT_EXCEEDED);
		assertNull(mgiDao.getUserState(mgiId, FULLY_REGISTERED_USER_ID));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAcceptedInvitationForDuelInParallel() throws Exception {
		errorCollector.setExpectedErrors(new F4MGameInvitationNotValidException("anyMessage"),
				new F4MGameParticipantCountExceededException("anyMessage"));
		saveGame();
		
		final MultiplayerGameParameters mgParams = CustomGameConfigBuilder
				.createDuel(FULLY_REGISTERED_USER_ID)
				.forGame(GAME_ID)
				.buildMultiplayerGameParameters();
		final int inviteeCount = 100;
		//Prepare users
		final ClientInfo[] clients = new ClientInfo[inviteeCount];
		IntStream.range(0, inviteeCount)
			.forEach(i -> {
				clients[i] = new ClientInfo();
				clients[i].setUserId(String.format(USER_ID_PATTERN, i + 1));
				clients[i].setRoles(UserRole.FULLY_REGISTERED.name());
				clients[i].setAppId(APP_ID);
			});
		
		//invite user to game
		final String inviteUsersToGameRequestJson = testDataLoader.getInviteUsersToGameJson(new String[]{clients[0].getUserId()}, mgParams);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), inviteUsersToGameRequestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		final JsonMessage<InviteUsersToGameResponse> inviteResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE);
		assertMessageContentType(inviteResponseMessage, InviteUsersToGameResponse.class);
		
		final String mgiId = inviteResponseMessage.getContent().getMultiplayerGameInstanceId();
		assertNotNull(mgiId);

		// Send invitations
		final Thread[] invitationThreads = new Thread[inviteeCount-1];
		IntStream.range(0, invitationThreads.length) //
				.forEach(i -> invitationThreads[i] = new Thread(() -> {
					sendInvitation(mgiId, clients[i+1].getUserId());
				}));

		startSendingThreads(invitationThreads);
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(inviteeCount, testClientReceivedMessageCollector.getReceivedMessageList()));
		mgiDao.activateInvitations(mgiId);
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		// Accept invitations		
		final Thread[] respondToInvitationThreads = new Thread[inviteeCount];
		IntStream.range(0, respondToInvitationThreads.length)
			.forEach(i -> respondToInvitationThreads[i] = new Thread(() -> {
					respondToInvitation(mgiId, true, clients[i]);
				})
			);

		startSendingThreads(respondToInvitationThreads);
		
		// Wait for invitation messages being processed + 1 add buddy request being sent
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(inviteeCount, testClientReceivedMessageCollector.getReceivedMessageList()));
		
		//Assert result
		F4MAssert.assertSize(1, testClientReceivedMessageCollector.getReceivedMessageList()
			.stream()
			.filter(m -> m.getError() == null)
			.map(m -> (JsonMessage<RespondToInvitationResponse>) m)
			.map(m -> m.getContent().getGameInstanceId())
			.collect(Collectors.toList()));
		
		final Set<String> respondToInvitationErrorCodes = testClientReceivedMessageCollector.getReceivedMessageList()
				.stream()
				.filter(m -> m.getError() != null)
				.map(m -> m.getError().getCode())
				.collect(Collectors.toSet());
		
		assertThat(respondToInvitationErrorCodes, anyOf(hasItem(GameSelectionExceptionCodes.ERR_GAME_INVITATION_NOT_VALID),
				hasItem(GameSelectionExceptionCodes.ERR_GAME_PARTICIPANT_COUNT_EXCEEDED)));
		
		assertNotNull(mockServiceReceivedMessageServerCollector.getMessagesByType(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER_RESPONSE));
	}

	private void startSendingThreads(final Thread[] invitationThreads) throws InterruptedException {
		IntStream.range(0, invitationThreads.length) //
				.parallel() //
				.forEach(i -> invitationThreads[i].start());
		// wait until all responses are received
		for (Thread invitationThread : invitationThreads) {
			invitationThread.join();
		}
	}
	
	private void respondToInvitation(String mgiId, boolean accept, ClientInfo clientInfo){
		try {
			final String respondToInvitationJson = testDataLoader.getRespondToInvitationJson(mgiId, accept, clientInfo);
			jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), respondToInvitationJson);
		} catch (IOException | URISyntaxException e) {
			LOGGER.error("Failed to load respondToInvitation.json", e);
			errorCollector.addError(e);
		}
	}

	private void sendInvitation(String mgiId, String userId){
		try {
			final String invitationJson = testDataLoader.getInviteUsersToGameJson(new String[] { userId }, mgiId);
			jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), invitationJson);
		} catch (IOException | URISyntaxException e) {
			LOGGER.error("Failed to send invitations to users", e);
			errorCollector.addError(e);
		}
	}
	
	private void assertResponseContainsError(JsonMessage<? extends JsonMessageContent> response, String exceptionCode) {
		assertNull(response.getContent());
		assertNotNull(response.getError());
		assertEquals(exceptionCode, response.getError().getCode());
	}
	private void saveGame() throws Exception {
		saveGame(true);
	}
	
	private void saveGame(boolean isFree) throws Exception {
		if (isFree){
			saveGame(gameBuilder.withApplications(APP_ID).build());
		} else {
			saveGame(gameBuilder.withApplications(APP_ID).withFree(false).withEntryFee("1", "1", "1", "1", Currency.CREDIT).build());
		}
		saveGame(gameBuilder
				.withApplications(APP_ID)
				.withTitle(GAME_TITLE)
				.withTimeToAcceptInvites(TIME_TO_ACCEPT_INVITES_IN_MINUTES)
				.withHideCategories(Boolean.TRUE)
				.withAssignedPools("x")
				.build());
	}

	private void saveGame(Game game) throws Exception {
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);
		String key = gamePrimaryKeyUtil.createPrimaryKey(game.getGameId());
		((AerospikeDao) mgiDao).createOrUpdateJson(set, key, BLOB_BIN_NAME, (v, wp) -> jsonUtil.toJson(game));
	}

	private String prepareMultiplayerGameInstance(String userId) throws Exception {
		return prepareMultiplayerGameInstance(userId, true);
	}
	
	private String prepareMultiplayerGameInstance(String userId, boolean isFree) throws Exception {
		saveGame(isFree);
		MultiplayerGameParameters params = CUSTOM_CONFIG_BUILDER.buildMultiplayerGameParameters();
		
		ClientInfo clientInfo = new ClientInfo(TENANT_ID, APP_ID, userId, null, null);
		String mgiId = mgiManager.createMultiplayerGameInstance(params, clientInfo, null, null, false).getId();
		registerUser(mgiId, userId);
		
		return mgiId;
	}
	
	private void inviteUser(String mgiId, String userId, String inviterId) {
		mgiDao.addUser(mgiId, userId, inviterId, MultiplayerGameInstanceState.INVITED);
	}

	private void setInvitationExpirationNotificationId(String mgiId, String userId, String notificationId) {
		mgiDao.setInvitationExpirationNotificationId(mgiId, userId, notificationId);
	}
	
	private void registerUser(String mgiId, String userId) {
		mgiDao.registerForGame(mgiId, new ClientInfo("t1", "a1", userId, "ip", null), GAME_INSTANCE_ID);
		mgiDao.addToGameInstancesCounter(mgiId, +1);
	}
	
	private void addProfile(String userId) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		
		ProfileUser person = new ProfileUser();
		person.setFirstName(FIRST_NAME);
		person.setLastName(LAST_NAME);
		profile.setPersonWrapper(person);
		
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		String key = profilePrimaryKeyUtil.createPrimaryKey(userId);
		((AerospikeDao) mgiDao).createJson(set, key, CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}

	private void prepareTestGameInstance() throws IOException {
		GameInstance gi = new GameInstance(JsonTestUtil.getGson()
				.fromJson(jsonLoader.getPlainTextJsonFromResources("gameInstance.json"), JsonObject.class));

		commonGameInstanceAerospikeDao.createJson(config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_INSTANCE_SET),
				"gameEngine:" + GAME_INSTANCE_ID, BLOB_BIN_NAME, gi.getAsString());
	}
}
