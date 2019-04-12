package de.ascendro.f4m.service.game.selection.subscription;

import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.APP_ID;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_ID_1;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.START_MGI_TOPIC;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.TENANT_ID;
import static de.ascendro.f4m.service.util.DateTimeUtil.parseISODateTimeString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.store.EventSubscription;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.game.selection.builder.GameBuilder;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.game.selection.model.subscription.LiveUserTournamentStartGameEventNotificationContent;
import de.ascendro.f4m.service.game.selection.model.subscription.OpenRegistrationNotificationContent;
import de.ascendro.f4m.service.game.selection.model.subscription.StartGameEventNotificationContent;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.EventServiceClientImpl;
import de.ascendro.f4m.service.util.ServiceUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class SubscriptionManagerTest {

	private static final Integer DUEL_PLAYER_READINESS = 30;
	private static final Integer TOURNAMENT_PLAYER_READINESS = 45;
	private static final int CLIENT_COUNT = 10;
	private static final ZonedDateTime NEXT_TOURNAMENT_START_DATE_TIME_UTC = parseISODateTimeString(
			"2017-03-06T14:55:37Z");
	private static final ZonedDateTime NEXT_TOURNAMENT_START_GAME_DATE_TIME_UTC = parseISODateTimeString(
			"2017-03-06T14:54:37Z");
	private static String[] CLIENTS = new String[CLIENT_COUNT];
	
	private static final String EVENT_SERVICE_NAME = EventMessageTypes.SERVICE_NAME;
	private static final URI EVENT_SERVICE_URI = URI.create("wss://127.0.0.1:8890");
	private static final ServiceConnectionInformation EVENT_SERVICE_INFO = new ServiceConnectionInformation(EVENT_SERVICE_NAME, EVENT_SERVICE_URI, EventMessageTypes.NAMESPACE);

	@Mock
	private EventSubscriptionStore eventSubscriptionStore;
	@Mock
	private SessionPool sessionPool;
	@Mock
	private SessionWrapper sessionWrapper;
	@Mock
	private JsonMessageValidator jsonMessageValidator;
	@Mock
	private MultiplayerGameInstanceManager mgiManager;
	@Mock
	private GameStartScheduler gameStartScheduler;
	@Mock
	private GameAerospikeDao gameAerospikeDao;
	@Mock
	private Config config;

	private JsonUtil jsonUtil;
	
	@Mock
	JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	@Mock
	ServiceRegistryClient serviceRegistryClient;
	@Mock
	LoggingUtil loggingUtil;
	
	private EventServiceClientImpl eventServiceClient;
	JsonMessageUtil jsonMessageUtil;

	private SubscriptionManager subscriptionManager;
	private EventSubscription eventSubscription;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		jsonUtil = new JsonUtil();

		eventSubscription = new EventSubscription(false, null, START_MGI_TOPIC);
		eventSubscription.setSubscriptionId(42L);
		IntStream.range(0, CLIENT_COUNT).forEach(i -> {
			CLIENTS[i] = String.valueOf(i + 1);
			eventSubscription.addClient(CLIENTS[i]);
		});
		when(eventSubscriptionStore.getSubscription(START_MGI_TOPIC)).thenReturn(eventSubscription);
		when(sessionPool.getSessionByClientIdServiceName(any(), any())).thenReturn(sessionWrapper);

		when(serviceRegistryClient.getServiceConnectionInformation(EVENT_SERVICE_NAME)).thenReturn(EVENT_SERVICE_INFO);
		
		jsonMessageUtil = new JsonMessageUtil(new GsonProvider(new JsonMessageDeserializer(new EventMessageTypeMapper())), 
				new ServiceUtil(), new JsonMessageValidator(new DefaultMessageSchemaMapper(), config));
		
		eventServiceClient = new EventServiceClientImpl(jsonWebSocketClientSessionPool, jsonMessageUtil, serviceRegistryClient,
				eventSubscriptionStore, config, loggingUtil);

		subscriptionManager = new SubscriptionManager(eventSubscriptionStore, jsonUtil, mgiManager, gameStartScheduler,
				gameAerospikeDao, config,eventServiceClient);
	}

	@Test
	public void testProcessOpenRegistrationEventForLiveTournament() throws F4MEntryNotFoundException, Exception {
		when(gameAerospikeDao.getGame(GAME_ID_1)).thenReturn(GameBuilder.createGame(GAME_ID_1, GameType.LIVE_TOURNAMENT)
				.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS).build());
		
		final OpenRegistrationNotificationContent content = new OpenRegistrationNotificationContent(GAME_ID_1, 
				TENANT_ID, APP_ID, NEXT_TOURNAMENT_START_DATE_TIME_UTC, NEXT_TOURNAMENT_START_GAME_DATE_TIME_UTC);
		
		subscriptionManager.processEventServiceNotifyMessage(Game.getOpenRegistrationTopic(GAME_ID_1),
				jsonUtil.toJsonElement(content));

		ArgumentCaptor<MultiplayerGameParameters> configArgument = ArgumentCaptor
				.forClass(MultiplayerGameParameters.class);
		ArgumentCaptor<ClientInfo> clientInfoArgument = ArgumentCaptor.forClass(ClientInfo.class);
		
		verify(mgiManager).createMultiplayerGameInstance(configArgument.capture(), clientInfoArgument.capture(),
				isNull(), isNull(), eq(false));

		final MultiplayerGameParameters config = configArgument.getValue();
		assertThat(config.getGameId(), equalTo(GAME_ID_1));
		final ClientInfo clientInfo = clientInfoArgument.getValue();
		assertThat(clientInfo.getTenantId(), equalTo(TENANT_ID));
		assertThat(clientInfo.getUserId(), nullValue());

		assertTrue("Current date-time is before open registration event",
				DateTimeUtil.getCurrentDateTime().compareTo(config.getStartDateTime()) >= 0);
		assertThat(config.getPlayDateTime(), equalTo(NEXT_TOURNAMENT_START_DATE_TIME_UTC));
	}

	@Test
	public void testProcessOpenRegistrationEventForNormalTournament() throws F4MEntryNotFoundException, Exception {
		Game game = GameBuilder.createGame(GAME_ID_1, GameType.TOURNAMENT)
				.withPlayerReadiness(TOURNAMENT_PLAYER_READINESS, DUEL_PLAYER_READINESS).build();
		game.setEndDateTime(null);
		when(gameAerospikeDao.getGame(GAME_ID_1)).thenReturn(game);
		final OpenRegistrationNotificationContent content = new OpenRegistrationNotificationContent(GAME_ID_1,
				TENANT_ID, APP_ID, NEXT_TOURNAMENT_START_DATE_TIME_UTC);
		subscriptionManager.processEventServiceNotifyMessage(Game.getOpenRegistrationTopic(GAME_ID_1),
				jsonUtil.toJsonElement(content));

		ArgumentCaptor<MultiplayerGameParameters> configArgument = ArgumentCaptor
				.forClass(MultiplayerGameParameters.class);
		ArgumentCaptor<ClientInfo> clientInfoArgument = ArgumentCaptor.forClass(ClientInfo.class);
		verify(mgiManager).createMultiplayerGameInstance(configArgument.capture(), clientInfoArgument.capture(),
				isNull(), isNull(), eq(false));

		final MultiplayerGameParameters config = configArgument.getValue();
		assertThat(config.getGameId(), equalTo(GAME_ID_1));
		final ClientInfo clientInfo = clientInfoArgument.getValue();
		assertThat(clientInfo.getTenantId(), equalTo(TENANT_ID));
		assertThat(clientInfo.getUserId(), nullValue());

		assertTrue("Current date-time is before open registration event",
				DateTimeUtil.getCurrentDateTime().compareTo(config.getStartDateTime()) >= 0);
		assertTrue("Normal tournament end date-time is less than " + GameConfigImpl.GAME_MAX_PLAY_TIME_IN_HOURS_DEFAULT,
				DateTimeUtil.getCurrentDateTime().plusHours(GameConfigImpl.GAME_MAX_PLAY_TIME_IN_HOURS_DEFAULT)
						.compareTo(config.getEndDateTime()) >= 0);
		assertNull(config.getPlayDateTime());
	}

	@Test
	public void testProcessStartGameEvent() throws F4MEntryNotFoundException {
		when(mgiManager.getTournamentInstanceId(GAME_ID_1)).thenReturn(MGI_ID);

		when(gameStartScheduler.getMultiplayerGameInstanceId(GAME_ID_1)).thenReturn(MGI_ID);
		
		StartGameEventNotificationContent content = new StartGameEventNotificationContent(GAME_ID_1);
		subscriptionManager.processEventServiceNotifyMessage(Game.getStartGameTopic(GAME_ID_1),
				jsonUtil.toJsonElement(content));

		when(gameStartScheduler.getMultiplayerGameInstanceId(GAME_ID_1)).thenReturn(MGI_ID);

		verify(gameStartScheduler).notifyGameRegisteredUsers(GAME_ID_1, MGI_ID);
		verify(gameStartScheduler).scheduleGameStartNotifications(MGI_ID);
		verify(gameStartScheduler).schedulePlayerReadinessNotification(GAME_ID_1, MGI_ID);
		verify(gameStartScheduler).scheduleGameCleanUpIfNoParticipats(MGI_ID);
		verify(gameStartScheduler).scheduleLiveTournamentStartingSoonNotifications(GAME_ID_1, MGI_ID);
		verify(eventSubscriptionStore).setSubscriptionId(false, null, MGI_ID, 0L);
	}

	@Test
	public void testProcessStartGameUserLiveTournamentEvent() throws F4MEntryNotFoundException {
		
		LiveUserTournamentStartGameEventNotificationContent content = new LiveUserTournamentStartGameEventNotificationContent(
				GAME_ID_1, MGI_ID);
		
		subscriptionManager.processEventServiceNotifyMessage(Game.getStartMultiplayerGameTopic(MGI_ID),
				jsonUtil.toJsonElement(content));

		when(gameStartScheduler.getMultiplayerGameInstanceId(GAME_ID_1)).thenReturn(MGI_ID);

		verify(gameStartScheduler).notifyGameRegisteredUsers(GAME_ID_1, MGI_ID);
		verify(gameStartScheduler).scheduleGameStartNotifications(MGI_ID);
		verify(gameStartScheduler).schedulePlayerReadinessNotification(GAME_ID_1, MGI_ID);
		verify(gameStartScheduler).scheduleGameCleanUpIfNoParticipats(MGI_ID);
		verify(gameStartScheduler).scheduleLiveTournamentStartingSoonNotifications(GAME_ID_1, MGI_ID);
		verify(eventSubscriptionStore).setSubscriptionId(false, null, MGI_ID, 0L);
	}
	
	
}
