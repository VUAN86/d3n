package de.ascendro.f4m.service.game.selection.subscription;

import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.REGISTERED;
import static de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState.STARTED;
import static de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes.GAME_START_COUNT_DOWN_NOTIFICATION;
import static de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes.GAME_START_NOTIFICATION;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_ID_1;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.Gson;

import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.builder.GameBuilder;
import de.ascendro.f4m.service.game.selection.client.NotificationMessagePreparer;
import de.ascendro.f4m.service.game.selection.config.GameSelectionConfig;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDao;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.ServiceUtil;

public class GameStartSchedulerTest {
	
	private static final String GAME_INSTANCE_ID_1 = "game_instance_id_1";
	private static final String USER_ID_1 = "user_id_1";

	@Mock
	private MultiplayerGameInstanceManager mgiManager;
	@Mock
	private GameBuilder gameBuilder;
	@Mock
	private NotificationMessagePreparer notificationMsgPreparer;
	@Mock
	private EventSubscriptionStore eventSubscriptionStore;
	@Mock
	private SessionPool sessionPool;
	@Mock
	private GsonProvider gsonProvider;
	@Mock
	private ServiceUtil serviceUtil;
	@Mock
	private JsonMessageValidator jsonMessageValidator;
	@Mock
	private SessionWrapper sessionWrapper;
	@Mock
	private CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	@Mock
	private GameSelectorAerospikeDao gameDao;
	
	private JsonMessageUtil jsonMessageUtil;
	private GameSelectionConfig config;
	private GameStartScheduler gameStartScheduler;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		gameBuilder = GameBuilder.createGame(GAME_ID_1, GameType.QUIZ24);

		when(gsonProvider.get()).thenReturn(new Gson());
		jsonMessageUtil = new JsonMessageUtil(gsonProvider, serviceUtil, jsonMessageValidator);
		
		when(mgiManager.getTournamentInstanceId(GAME_ID_1)).thenReturn(MGI_ID);
		when(eventSubscriptionStore.getSubscribers(MGI_ID))
				.thenReturn(new HashSet<>(Arrays.asList(ANONYMOUS_CLIENT_ID)));
		
		when(sessionPool.getSessionByClientIdServiceName(ANONYMOUS_CLIENT_ID, GatewayMessageTypes.SERVICE_NAME))
			.thenReturn(sessionWrapper);
		
		config = new GameSelectionConfig();
		config.setProperty(GameSelectionConfig.GAME_START_NOTIFICATION_ADVANCE, 0);
		config.setProperty(GameSelectionConfig.GAME_COUNT_DOWN_NOTIFICATION_COUNT, 1);
		config.setProperty(GameSelectionConfig.GAME_COUNT_DOWN_NOTIFICATION_FREQUENCY, 1000);
		
		gameStartScheduler = new GameStartScheduler(config, notificationMsgPreparer, mgiManager,
				eventSubscriptionStore, jsonMessageUtil, sessionPool, commonMultiplayerGameInstanceDao, gameDao);
	}

	@Test
	public void testScheduleGameStartNotifications() {
		gameBuilder.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(5));//open registration 5 days before game play
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID))
			.thenReturn(CustomGameConfigBuilder.create(null)
					.withPlayDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(1))//now + 1s
					.build());
		
		
		gameStartScheduler.scheduleGameStartNotifications(MGI_ID);
		
		final ArgumentCaptor<JsonMessage<?>> sentMessageCapturer = ArgumentCaptor.forClass(JsonMessage.class);
		RetriedAssert.assertWithWait(() ->
			verify(sessionWrapper, times(2)).sendAsynMessage(sentMessageCapturer.capture()));
		
		final Set<GameSelectionMessageTypes> messageTypes = sentMessageCapturer.getAllValues()
			.stream()
			.map(m -> m.getType(GameSelectionMessageTypes.class))
			.collect(Collectors.toSet());
		assertThat(messageTypes, containsInAnyOrder(GAME_START_NOTIFICATION, GAME_START_COUNT_DOWN_NOTIFICATION));
	}
	
	@Test
	public void testScheduleGameStartNotificationsWithImmediateStart() {
		gameBuilder.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(5));//open registration 5 days before game play
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID))
			.thenReturn(CustomGameConfigBuilder.create(null)
					.withPlayDateTime(DateTimeUtil.getCurrentDateTime())
					.build());
		
		gameStartScheduler.scheduleGameStartNotifications(MGI_ID);
		
		final ArgumentCaptor<JsonMessage<?>> sentMessageCapturer = ArgumentCaptor.forClass(JsonMessage.class);
		RetriedAssert.assertWithWait(() ->
			verify(sessionWrapper, times(1)).sendAsynMessage(sentMessageCapturer.capture()));
		final Set<GameSelectionMessageTypes> messageTypes = sentMessageCapturer.getAllValues()
				.stream()
				.map(m -> m.getType(GameSelectionMessageTypes.class))
				.collect(Collectors.toSet());
		assertThat(messageTypes, containsInAnyOrder(GAME_START_NOTIFICATION));
	}
	
	@Test
	public void testSchedulePlayerReadinessNotification() throws Exception {
		ClientInfo clientInfo = getClientInfo(USER_ID_1);
		gameBuilder = GameBuilder.createGame(GAME_ID_1, GameType.LIVE_TOURNAMENT);
		gameBuilder.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(5)) //open registration 5 days before game play
			.withPlayerReadiness(30, 30);
		when(gameDao.getGame(GAME_ID_1)).thenReturn(gameBuilder.build());
		when(mgiManager.getMultiplayerUserGameInstances(MGI_ID, REGISTERED))
			.thenReturn(Arrays.asList(new MultiplayerUserGameInstance(GAME_INSTANCE_ID_1, clientInfo)));
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID))
			.thenReturn(CustomGameConfigBuilder.create(null)
					.withPlayDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(1))//now + 1s
					.build());
		
		gameStartScheduler.schedulePlayerReadinessNotification(GAME_ID_1, MGI_ID);
		
		ClientInfo expClientInfo = new ClientInfo(clientInfo.getTenantId(), clientInfo.getAppId(), 
				clientInfo.getUserId(), clientInfo.getIp(), null);
		RetriedAssert.assertWithWait(() -> verify(notificationMsgPreparer, times(1))
				.sendGamePlayerReadinessNotification(USER_ID_1, GAME_INSTANCE_ID_1, MGI_ID, GAME_ID_1, expClientInfo));
	}

	@Test
	public void testScheduleOneHourBeforeTournamentNotification() throws Exception {
		ClientInfo clientInfo = getClientInfo(USER_ID_1);
		gameBuilder.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusDays(5)) //open registration 5 days before game play
				.withPlayerReadiness(30, 30);
		when(gameDao.getGame(GAME_ID_1)).thenReturn(gameBuilder.build());
		when(mgiManager.getMultiplayerUserGameInstances(MGI_ID, REGISTERED)).thenReturn(
				Arrays.asList(new MultiplayerUserGameInstance(GAME_INSTANCE_ID_1, clientInfo)));
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID)).thenReturn(
				CustomGameConfigBuilder.create(null).withPlayDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(1))//now + 1s
						.build());
		
		gameStartScheduler.scheduleLiveTournamentStartingSoonNotifications(GAME_ID_1, MGI_ID);

		RetriedAssert.assertWithWait(() -> verify(notificationMsgPreparer, times(1))
				.sendLiveTournamentScheduledNotifications(USER_ID_1, GAME_INSTANCE_ID_1, MGI_ID, GAME_ID_1));
	}
	
	@Test
	public void testScheduleGameCleanUpIfNoParticipats() throws Exception {
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID)).thenReturn(
				CustomGameConfigBuilder.create(null).withPlayDateTime(DateTimeUtil.getCurrentDateTime())
						.build());
		when(mgiManager.getTournamentInstanceId(GAME_ID_1)).thenReturn(MGI_ID);
		when(mgiManager.getMultiplayerUserGameInstances(MGI_ID, REGISTERED, STARTED))
				.thenReturn(Collections.emptyList());
		
		gameStartScheduler.scheduleGameCleanUpIfNoParticipats(MGI_ID);
		
		RetriedAssert.assertWithWait(() -> verify(mgiManager).deletePublicGameSilently(MGI_ID));
	}
	
	@Test
	public void testScheduleGameCleanUpWithParticipats() throws Exception {
		when(commonMultiplayerGameInstanceDao.getConfig(MGI_ID)).thenReturn(
				CustomGameConfigBuilder.create(null).withPlayDateTime(DateTimeUtil.getCurrentDateTime())
						.build());
		when(mgiManager.getTournamentInstanceId(GAME_ID_1)).thenReturn(MGI_ID);
		when(mgiManager.getMultiplayerUserGameInstances(MGI_ID, REGISTERED, STARTED)).thenReturn(
				Arrays.asList(new MultiplayerUserGameInstance(GAME_INSTANCE_ID_1, new ClientInfo(USER_ID_1))));
		
		gameStartScheduler.scheduleGameCleanUpIfNoParticipats(MGI_ID);
		
		verify(mgiManager, never()).deletePublicGameSilently(MGI_ID);
	}
	
	@Test
	public void testIsCountDownRequired() {
		assertFalse(gameStartScheduler.isCountDownRequired(DateTimeUtil.getUTCTimestamp()));
		assertTrue(gameStartScheduler.isCountDownRequired(DateTimeUtil.getUTCTimestamp() + 60_000));
	}

	private ClientInfo getClientInfo(String userId) {
		ClientInfo clientInfo = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		clientInfo.setUserId(userId);
		return clientInfo;
	}

}
