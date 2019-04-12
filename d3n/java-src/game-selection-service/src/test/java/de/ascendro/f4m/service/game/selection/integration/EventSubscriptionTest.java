package de.ascendro.f4m.service.game.selection.integration;

import static de.ascendro.f4m.matchers.LambdaMatcher.matches;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_ID_1;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_INSTANCE_ID;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.TENANT_ID;
import static de.ascendro.f4m.service.integration.RetriedAssert.assertWithWait;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.dashboard.move.dao.MoveDashboardDao;
import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.MultiplayerGameInstancePrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.server.multiplayer.move.dao.MoveMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.builder.GameBuilder;
import de.ascendro.f4m.service.game.selection.config.GameSelectionConfig;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDaoImpl;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartCountDownNotification;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartNotification;
import de.ascendro.f4m.service.game.selection.subscription.StartLiveUserTournamentEvent;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageResponse;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class EventSubscriptionTest extends GameSelectionServiceTestBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriptionTest.class);

	private static final String BLOB_BIN_NAME = "value";
	private static final String TENANT_ID_2 = "tenant_id_2";
	
	private GameSelectorAerospikeDaoImpl gameSelectorAerospikeDao;
	private MultiplayerGameInstancePrimaryKeyUtil mgiPrimaryKeyUtil;
	private CommonMultiplayerGameInstanceDao mgiDao;
	private GamePrimaryKeyUtil gamePrimaryKeyUtil;
	private JsonUtil jsonUtil;
	private JsonMessageUtil jsonMessageUtil;
	private EventSubscriptionStore eventSubscriptionStore;
	private CommonProfileAerospikeDao profileDao;
	private MoveMultiplayerGameInstanceDao moveMgiDao;
	private MoveDashboardDao moveDashboardDao;
	private PublicGameElasticDao publicGameElasticDao;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		final Injector injector = jettyServerRule.getServerStartup().getInjector();
		gameSelectorAerospikeDao = injector.getInstance(GameSelectorAerospikeDaoImpl.class);
		mgiPrimaryKeyUtil = injector.getInstance(MultiplayerGameInstancePrimaryKeyUtil.class);
		mgiDao = injector.getInstance(CommonMultiplayerGameInstanceDao.class);
		gamePrimaryKeyUtil = injector.getInstance(GamePrimaryKeyUtil.class);
		jsonUtil = injector.getInstance(JsonUtil.class);
		jsonMessageUtil = injector.getInstance(JsonMessageUtil.class);
		eventSubscriptionStore = injector.getInstance(EventSubscriptionStore.class);
		profileDao = injector.getInstance(CommonProfileAerospikeDao.class);
		moveMgiDao = injector.getInstance(MoveMultiplayerGameInstanceDao.class);
		moveDashboardDao = injector.getInstance(MoveDashboardDao.class);
		publicGameElasticDao = injector.getInstance(PublicGameElasticDao.class);
	}
	
	@Override
	protected ServiceStartup getServiceStartup() {
		return new GameSelectionStartupUsingAerospikeMock(DEFAULT_TEST_STAGE) {
			@Override
			protected AbstractModule getGameSelectionServiceAerospikeOverrideModule() {
				return new AbstractModule() {
					@Override
					protected void configure() {
						bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
						
						bind(CommonProfileAerospikeDao.class).toInstance(mock(CommonProfileAerospikeDao.class));
						bind(MoveMultiplayerGameInstanceDao.class).toInstance(mock(MoveMultiplayerGameInstanceDao.class));
						bind(MoveDashboardDao.class).toInstance(mock(MoveDashboardDao.class));
						bind(PublicGameElasticDao.class).toInstance(mock(PublicGameElasticDao.class));
					}
				};
			}
		};
	}

	@Override
	protected JsonMessageContent onReceivedMessage(RequestContext requestContext)
			throws Exception {
		JsonMessageContent response;
		JsonMessage<?> message = requestContext.getMessage();
		LOGGER.debug("Mocked Service received request {}", message.getContent());
		mockServiceReceivedMessageServerCollector.onProcess(requestContext);
		
		if (UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE == message.getType(UserMessageMessageTypes.class)) {
			response = new SendWebsocketMessageResponse();
			((SendWebsocketMessageResponse) response).setMessageId("any_message_id");
		} else if (UserMessageMessageTypes.SEND_USER_PUSH == message.getType(UserMessageMessageTypes.class)) {
			response = new SendUserPushResponse();
			String[] notificationIds = new String[1];
			notificationIds[0] = "notification_id";
			((SendUserPushResponse) response).setNotificationIds(notificationIds);
		} else if(EventMessageTypes.SUBSCRIBE == message.getType(EventMessageTypes.class)){
			final SubscribeRequest subscribeRequest = (SubscribeRequest) message.getContent();
			return new SubscribeResponse(42L, subscribeRequest.isVirtual(), subscribeRequest.getConsumerName(), subscribeRequest.getTopic());
		} else if(EventMessageTypes.PUBLISH == message.getType(EventMessageTypes.class)){
			return null;
		} else {
			LOGGER.error("Mocked Service received unexpected message [{}]", message);
			throw new UnexpectedTestException("Unexpected message: " + message.getName());
		}
		
		return response;
	}

	@Test
	public void testOpenRegistrationEventLiveTournament() throws Exception {
		assertEventSubscribeRecived(true, Game.getOpenRegistrationTopic(), GameSelectionMessageTypes.SERVICE_NAME);
		Game game = GameBuilder.createGame(GAME_ID_1, GameType.LIVE_TOURNAMENT)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS)
				.build();
		saveGame(game);

		String eventJson = testDataLoader.getOpenRegistrationNotificationLiveTournamentJson(GAME_ID_1, TENANT_ID,
				DateTimeUtil.formatISODateTime(game.getStartDateTime().plusHours(2)),
				DateTimeUtil.formatISODateTime(game.getStartDateTime()));

		sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

		String set = config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_SET);
		String key = mgiPrimaryKeyUtil.createTournamentMappingKey(GAME_ID_1);
		assertWithWait(() -> assertNotNull(gameSelectorAerospikeDao.readString(set, key,
				CommonMultiplayerGameInstanceDaoImpl.CURRENT_MGI_ID_BIN_NAME)));

		List<JsonMessage<JsonMessageContent>> eventPublishList = mockServiceReceivedMessageServerCollector
				.getMessagesByType(EventMessageTypes.PUBLISH);
		assertEquals(1, eventPublishList.size());
		PublishMessageContent publishMessageContent = (PublishMessageContent) eventPublishList.get(0).getContent();
		StartLiveUserTournamentEvent notification = jsonUtil.fromJson(publishMessageContent.getNotificationContent(),
				StartLiveUserTournamentEvent.class);
		assertEquals(0, game.getStartDateTime().plusHours(2).toLocalDate()
				.compareTo(notification.getPlayDateTime().toLocalDate()));

		assertEquals(0,
				game.getStartDateTime().toLocalDate().compareTo(publishMessageContent.getPublishDate().toLocalDate()));
	}

	@Test
	public void testOpenRegistrationEvent() throws Exception {
		assertEventSubscribeRecived(true, Game.getOpenRegistrationTopic(), GameSelectionMessageTypes.SERVICE_NAME);
		Game game = GameBuilder.createGame(GAME_ID_1, GameType.TOURNAMENT)
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS).build();
		saveGame(game);
		
		String eventJson = testDataLoader.getOpenRegistrationNotificationJson(GAME_ID_1, TENANT_ID, 
				DateTimeUtil.formatISODateTime(game.getStartDateTime().plusHours(2)));
		sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

		String set = config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_SET);
		String key = mgiPrimaryKeyUtil.createTournamentMappingKey(GAME_ID_1);
		assertWithWait(() -> assertNotNull(gameSelectorAerospikeDao.readString(set, key, CommonMultiplayerGameInstanceDaoImpl.CURRENT_MGI_ID_BIN_NAME)));
	}
	
	
	@Test
	public void testStartGameEvent() throws Exception {
		assertEventSubscribeRecived(true, Game.getStartGameTopic(), GameSelectionMessageTypes.SERVICE_NAME);
		
		// count down notifications 1500, 1000 and 500 millis before start game
		config.setProperty(GameSelectionConfig.GAME_COUNT_DOWN_NOTIFICATION_COUNT, 3);
		config.setProperty(GameSelectionConfig.GAME_COUNT_DOWN_NOTIFICATION_FREQUENCY, 500);
		// start game notification 250 millis before start game
		config.setProperty(GameSelectionConfig.GAME_START_NOTIFICATION_ADVANCE, 250);
		
		final String mgiId = prepareLiveTournament();
		eventSubscriptionStore.setSubscriptionId(false, null, mgiId, 0);
		
        jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), 
                testDataLoader.getReceiveGameStartNotificationsJson(mgiId, true, KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO));
        assertReceivedMessagesWithWait(GameSelectionMessageTypes.RECEIVE_GAME_START_NOTIFICATIONS_RESPONSE);
        testClientReceivedMessageCollector.clearReceivedMessageList();
        
		String eventJson = testDataLoader.getStartGameNotificationJson(GAME_ID_1);
		sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));
		
		assertWithWait(() -> assertNotNull(mockServiceReceivedMessageServerCollector.getMessageByType(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE)));
		JsonMessage<SendWebsocketMessageRequest> jsonMessage = mockServiceReceivedMessageServerCollector
				.getMessageByType(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE);
		assertMessageContentType(jsonMessage, SendWebsocketMessageRequest.class);
		
		assertWithWait(() -> assertEquals(4, testClientReceivedMessageCollector.getReceivedMessageList().size()), 3000, 1000);
		List<JsonMessage<JsonMessageContent>> countDownMessages = testClientReceivedMessageCollector.getMessagesByType(GameSelectionMessageTypes.GAME_START_COUNT_DOWN_NOTIFICATION);
		assertEquals(3, countDownMessages.size());
		countDownMessages.forEach(m -> {
			GameStartCountDownNotification content = (GameStartCountDownNotification) m.getContent();
			assertEquals(mgiId, content.getMultiplayerGameInstanceId());
		});
		
		JsonMessage<JsonMessageContent> gameStartMessage = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.GAME_START_NOTIFICATION);
		GameStartNotification content = (GameStartNotification) gameStartMessage.getContent();
		assertEquals(mgiId, content.getMultiplayerGameInstanceId());
		
		assertFalse(eventSubscriptionStore.hasSubscription(mgiId));
	}

	private void assertEventSubscribeRecived(boolean virtual, String topic, String consumer) {
		RetriedAssert.assertWithWait(() -> mockServiceReceivedMessageServerCollector.getMessageByType(EventMessageTypes.SUBSCRIBE));
		final List<JsonMessage<SubscribeRequest>> subscribeMessages = mockServiceReceivedMessageServerCollector.getMessagesByType(EventMessageTypes.SUBSCRIBE);
		final List<SubscribeRequest> subscribeRequests = subscribeMessages.stream()
				.map(m -> m.getContent())
				.collect(Collectors.toList());
		assertThat(subscribeRequests, everyItem(matches("Subscribtion should be virtual",
				(subscribeRequest) -> virtual == subscribeRequest.isVirtual())));
		assertThat(subscribeRequests, IsCollectionContaining.<SubscribeRequest>hasItem(
				matches("Subscribtion should have start game topic using wildcard",
						(subscribeRequest) -> Objects.equals(subscribeRequest.getTopic(), topic)))				
			);
		assertThat(subscribeRequests, IsCollectionContaining.<SubscribeRequest>hasItem(
				matches("Subscribtion should have consumer as service name",
						(subscribeRequest) -> Objects.equals(subscribeRequest.getConsumerName(), consumer)))
			);
	}

	private String prepareLiveTournament() throws Exception {
		Game game = GameBuilder.createGame(GAME_ID_1, GameType.LIVE_TOURNAMENT)
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(5))
				.withPlayerReadiness(TestDataLoader.TOURNAMENT_PLAYER_READINESS, TestDataLoader.DUEL_PLAYER_READINESS)
				.build();
		saveGame(game);
		
		CustomGameConfig customGameConfig = CustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.forGame(GAME_ID_1)
				.withPlayDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(3))
				.withApp(APP_ID)
				.withGameTitle("Game Title")
				.withGameType(GameType.LIVE_TOURNAMENT)
				.build();
		String mgiId = mgiDao.create(null, customGameConfig);
		mgiDao.mapTournament(GAME_ID_1, mgiId);
		
		mgiDao.addUser(mgiId, ANONYMOUS_USER_ID, null, MultiplayerGameInstanceState.INVITED);
		mgiDao.registerForGame(mgiId, new ClientInfo("t1", "a1", ANONYMOUS_USER_ID, "ip", null), GAME_INSTANCE_ID);
		
		return mgiId;
	}
	
	private void saveGame(Game game) {
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);
		String key = gamePrimaryKeyUtil.createPrimaryKey(game.getGameId());
		((AerospikeDao) mgiDao).createOrUpdateJson(set, key, BLOB_BIN_NAME, (v, wp) -> jsonUtil.toJson(game));
	}
	
	@Test
	public void testProfileMergeEvent() throws Exception {
		List<String> tenants = Arrays.asList(TENANT_ID, TENANT_ID_2);
		when(profileDao.getProfile(anyString())).thenReturn(getMockedProfile(tenants));
		
		String eventJson = testDataLoader.getMergeProfileEventJson(ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));
		
		assertWithWait(() -> verify(publicGameElasticDao).changeUserOfPublicGame(ANONYMOUS_USER_ID, REGISTERED_USER_ID));
		tenants.forEach(tenantId -> {
			verify(moveMgiDao).moveUserMgiData(tenantId, null, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
			verify(moveDashboardDao).moveLastPlayedGame(tenantId, ANONYMOUS_USER_ID, REGISTERED_USER_ID);
		});
	}
	
	private Profile getMockedProfile(List<String> tenants) {
		Profile profile = new Profile();
		tenants.forEach(id -> profile.addTenant(id));
		return profile;
	}

}
