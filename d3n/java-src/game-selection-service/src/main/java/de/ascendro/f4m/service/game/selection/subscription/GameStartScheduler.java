package de.ascendro.f4m.service.game.selection.subscription;

import static de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes.GAME_START_COUNT_DOWN_NOTIFICATION;
import static de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes.GAME_START_NOTIFICATION;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.client.NotificationMessagePreparer;
import de.ascendro.f4m.service.game.selection.config.GameSelectionConfig;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDao;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartCountDownNotification;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartNotification;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class GameStartScheduler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GameStartScheduler.class);

	private final GameSelectionConfig config;
	private final NotificationMessagePreparer notificationMsgPreparer;
	private final MultiplayerGameInstanceManager mgiManager;
	private final EventSubscriptionStore notificationSubscriptionStore;
	private final JsonMessageUtil jsonMessageUtil;
	private final SessionPool sessionPool;
	private final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	private final GameSelectorAerospikeDao gameDao;
	
	private final ScheduledExecutorService executor;

	@Inject
	public GameStartScheduler(GameSelectionConfig config, NotificationMessagePreparer notificationMsgPreparer,
			MultiplayerGameInstanceManager mgiManager, EventSubscriptionStore eventSubscriptionStore,
			JsonMessageUtil jsonMessageUtil, SessionPool sessionPool,
			CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao, GameSelectorAerospikeDao gameDao) {
		this.config = config;
		this.notificationMsgPreparer = notificationMsgPreparer;
		this.mgiManager = mgiManager;
		this.notificationSubscriptionStore = eventSubscriptionStore;
		this.jsonMessageUtil = jsonMessageUtil;
		this.sessionPool = sessionPool;
		this.commonMultiplayerGameInstanceDao = commonMultiplayerGameInstanceDao;
		this.gameDao = gameDao;

		int poolSize = config.getPropertyAsInteger(GameSelectionConfig.GAME_NOTIFICATION_THREAD_POOL_SIZE);
		executor = Executors.newScheduledThreadPool(poolSize,
				new ThreadFactoryBuilder().setNameFormat("GameStartScheduler-%d").build());
	}
	
	@PreDestroy
	public void finialize() {
		if (executor != null) {
			try {
				final List<Runnable> scheduledRunnabled = executor.shutdownNow();
				LOGGER.info("Stopping GameStartNotificationManager with {} scheduled tasks", scheduledRunnabled != null ? scheduledRunnabled.size() : 0);
			} catch (Exception e) {
				LOGGER.error("Failed to shutdown GameStartNotificationManager scheduler", e);
			}
		}
	}

	/**
	 * Notify users via mobile push about game soon start
	 * @param gameId
	 * @param mgiId multiplayerGameInstanceId
	 */
	public void notifyGameRegisteredUsers(String gameId, String mgiId) {
		List<MultiplayerUserGameInstance> userInstances = mgiManager.getMultiplayerUserGameInstances(mgiId, MultiplayerGameInstanceState.REGISTERED);
		userInstances.forEach(instance -> {
			ClientInfo userClientInfo = buildClientInfo(instance);
			notificationMsgPreparer.sendGameStartingSoonNotification(instance.getUserId(), instance.getGameInstanceId(), mgiId, gameId, userClientInfo);
		});
	}

	private ClientInfo buildClientInfo(MultiplayerUserGameInstance instance) {
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setTenantId(instance.getTenantId());
		clientInfo.setAppId(instance.getAppId());
		clientInfo.setUserId(instance.getUserId());
		clientInfo.setIp(instance.getClientIp());
		return clientInfo;
	}
	
	public void scheduleGameStartNotifications(String mgiId) {
		long gameStartTimestamp = getGameStartTimestamp(mgiId);
		if(isCountDownRequired(gameStartTimestamp)){
			scheduleCountDown(mgiId, gameStartTimestamp);
		}
		scheduleGameStart(mgiId, gameStartTimestamp);
	}
	
	protected boolean isCountDownRequired(long gameStartTimestamp) {
		int millisToGameStart = config.getPropertyAsInteger(GameSelectionConfig.GAME_START_NOTIFICATION_ADVANCE);
		return getDelayForNextNotification(gameStartTimestamp, 1, millisToGameStart) > 0;
	}
	
	private void scheduleCountDown(String mgiId, long gameStartTimestamp) {
		int count = config.getPropertyAsInteger(GameSelectionConfig.GAME_COUNT_DOWN_NOTIFICATION_COUNT);
		int frequency = config.getPropertyAsInteger(GameSelectionConfig.GAME_COUNT_DOWN_NOTIFICATION_FREQUENCY);
		
		for (int i = count; i > 0; i--) {
			long delayForNextCountDown = getDelayForNextNotification(gameStartTimestamp, i, frequency);
			GameStartCountDownNotification content = new GameStartCountDownNotification(mgiId, i * frequency);
			executor.schedule(() -> sendNotificationAsDirectMessage(mgiId, GAME_START_COUNT_DOWN_NOTIFICATION, content), delayForNextCountDown, TimeUnit.MILLISECONDS);
		}
	}

	private void scheduleGameStart(String mgiId, long gameStartTimestamp) {
		int millisToGameStart = config.getPropertyAsInteger(GameSelectionConfig.GAME_START_NOTIFICATION_ADVANCE);
		long delayForGameStart = getDelayForNextNotification(gameStartTimestamp, 1, millisToGameStart);
		
		GameStartNotification content = new GameStartNotification(mgiId);
		executor.schedule(() -> sendNotificationAsDirectMessage(mgiId, GAME_START_NOTIFICATION, content), delayForGameStart, TimeUnit.MILLISECONDS);
	}
	
	private void sendNotificationAsDirectMessage(String mgiId, GameSelectionMessageTypes messageType, GameStartNotification content) {
		notificationSubscriptionStore.getSubscribers(mgiId).forEach(clientId -> {
			JsonMessage<? extends GameStartNotification> gameStartNotificationMessage = jsonMessageUtil
					.createNewGatewayMessage(messageType, content, clientId);
			sendJsonMessageToClient(clientId, gameStartNotificationMessage);
		});
		if (messageType == GAME_START_NOTIFICATION) {
			notificationSubscriptionStore.unregisterSubscription(mgiId);
		}
	}
	
	private void sendJsonMessageToClient(String clientId, final JsonMessage<? extends JsonMessageContent> message) {
		try {
			final SessionWrapper clientSession = sessionPool.getSessionByClientIdServiceName(clientId, GatewayMessageTypes.SERVICE_NAME);
			if (clientSession != null) {
				clientSession.sendAsynMessage(message);
			} else {
				LOGGER.error("Client session is not found within opened connection set by client id [{}]", clientId);
			}
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException(String.format("Unable to send [%s] to Gateway", message.getTypeName()), e);
		}
	}

	/**
	 * Calculate delay until next notification
	 * @param gameStartMillis - time stamp of game start
	 * @param notificationIndex - amount of notifications until game start, e.g. 3, 2, 1
	 * @param interval - milliseconds between notifications, e.g. 1000ms
	 * @return delay until next notification
	 */
	private long getDelayForNextNotification(long gameStartMillis, int notificationIndex, int interval) {
		long millisToGameStart = (long) notificationIndex * interval;
		long now = DateTimeUtil.getUTCTimestamp();
		
		long delayForNextNotification;
		if (gameStartMillis - millisToGameStart > now) {
			delayForNextNotification = gameStartMillis - millisToGameStart - now;
		} else {
			delayForNextNotification = 0;
			LOGGER.warn("Scheduling next notification for now, invalid game delay calculated: {} gameStartMillis -{} millisToGameStart <= {} now",
					gameStartMillis, millisToGameStart, now);
		}
		return delayForNextNotification;
	}

	public String getMultiplayerGameInstanceId(String gameId) {
		String mgiId = mgiManager.getTournamentInstanceId(gameId);
		if (StringUtils.isBlank(mgiId)) {
			throw new F4MValidationFailedException(String.format("Game [%s] must contain multiplayer game instance ID", gameId));
		}
		return mgiId;
	}

	private long getGameStartTimestamp(String mgiId) {
		final CustomGameConfig customConfig = commonMultiplayerGameInstanceDao.getConfig(mgiId);
		final ZonedDateTime startDateTime = customConfig.getPlayDateTime();

		long gameStartTimestamp;
		if (startDateTime != null) {
			gameStartTimestamp = startDateTime.toInstant().toEpochMilli();
		} else {
			throw new F4MValidationFailedException(String.format("Game play start date time not found for mgi [%s]", mgiId));
		}
		return gameStartTimestamp;
	}

	public void schedulePlayerReadinessNotification(String gameId, String mgiId) {
		long gameStartTimestamp = getGameStartTimestamp(mgiId);
		
		Game game = gameDao.getGame(gameId);
		Integer playerGameReadiness = game.getTypeConfiguration().getPlayerGameReadiness(game.getType());
		int millisToGameStart = config.getPropertyAsInteger(GameSelectionConfig.GAME_PLAYER_READINESS_DEFAULT);
		if (playerGameReadiness != null) {
			millisToGameStart = playerGameReadiness * 1000;
		}
		long delayForGameStart = getDelayForNextNotification(gameStartTimestamp, 1, millisToGameStart);
		executor.schedule(() -> sendPlayerReadinessNotification(gameId, mgiId), delayForGameStart, TimeUnit.MILLISECONDS);
	}
	
	private void sendPlayerReadinessNotification(String gameId, String mgiId) {
		List<MultiplayerUserGameInstance> userInstances = mgiManager.getMultiplayerUserGameInstances(mgiId, MultiplayerGameInstanceState.REGISTERED);
		for (MultiplayerUserGameInstance instance : userInstances) {
			ClientInfo userClientInfo = buildClientInfo(instance);
			notificationMsgPreparer.sendGamePlayerReadinessNotification(instance.getUserId(),
					instance.getGameInstanceId(), mgiId, gameId, userClientInfo);
		}
	}
	
	public void scheduleGameCleanUpIfNoParticipats(String mgiId) {
		long delay = getGameStartTimestamp(mgiId) - DateTimeUtil.getUTCTimestamp();
		executor.schedule(() -> gameCleanUpIfNoParticipants(mgiId), delay, TimeUnit.MILLISECONDS);
	}

	private void gameCleanUpIfNoParticipants(String mgiId) {
		List<MultiplayerUserGameInstance> userInstances = mgiManager.getMultiplayerUserGameInstances(mgiId,
				MultiplayerGameInstanceState.REGISTERED, MultiplayerGameInstanceState.STARTED);
		if (userInstances.isEmpty()) {
			mgiManager.deletePublicGameSilently(mgiId);
		}
	}

	public void scheduleLiveTournamentStartingSoonNotifications(String gameId, String mgiId) {
		List<MultiplayerUserGameInstance> userInstances = mgiManager.getMultiplayerUserGameInstances(mgiId,
				MultiplayerGameInstanceState.REGISTERED);
		userInstances.forEach(instance ->  
			notificationMsgPreparer.sendLiveTournamentScheduledNotifications(
				instance.getUserId(), instance.getGameInstanceId(), mgiId, gameId)
		);
	}

}
