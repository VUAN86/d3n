package de.ascendro.f4m.service.game.selection.subscription;

import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

import com.google.gson.JsonElement;

import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.store.EventSubscription;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.exception.SubscriptionNotFoundException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.game.selection.model.subscription.LiveUserTournamentStartGameEventNotificationContent;
import de.ascendro.f4m.service.game.selection.model.subscription.OpenRegistrationNotificationContent;
import de.ascendro.f4m.service.game.selection.model.subscription.StartGameEventNotificationContent;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.EventServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionManager.class);

	private final EventSubscriptionStore notificationSubscriptionStore;
	private final JsonUtil jsonUtil;
	private final MultiplayerGameInstanceManager mgiManager;
	private final GameStartScheduler gameStartScheduler;
	private final GameAerospikeDao gameAerospikeDao;
	private final Config config;
	private final EventServiceClient eventServiceClient;

	@Inject
	public SubscriptionManager(EventSubscriptionStore eventSubscriptionStore, JsonUtil jsonUtil,
			MultiplayerGameInstanceManager mgiManager, GameStartScheduler gameStartScheduler,
			GameAerospikeDao gameAerospikeDao, Config config, EventServiceClient eventServiceClient) {
		this.notificationSubscriptionStore = eventSubscriptionStore;
		this.jsonUtil = jsonUtil;
		this.mgiManager = mgiManager;
		this.gameStartScheduler = gameStartScheduler;
		this.gameAerospikeDao = gameAerospikeDao;
		this.config = config;
		this.eventServiceClient = eventServiceClient;
	}
	
	public void updateSubscriptionId(boolean virtualTopic, String subscriptionName, String topic, long id) {
		notificationSubscriptionStore.setSubscriptionId(virtualTopic, subscriptionName, topic, id);
	}

	/**
	 * Subscribes client for game start notifications:
	 * - game start
	 * - game count down
	 * Subscription is add only if MGI topic is created - sign-up for game start/count down is open
	 * @param clientId - id of the client
	 * @param mgiId - multiplayer game instance id
	 */
	public void subscribeUserToGame(String clientId, String mgiId) {
		if (notificationSubscriptionStore.hasSubscription(mgiId)) {
			notificationSubscriptionStore.addSubscriber(false, null, mgiId, clientId);
		} else {
			throw new SubscriptionNotFoundException(
					String.format("There is not start/count down topic for MGI [%s]", mgiId));
		}
	}

	public void unsubscribeUserFromGame(String clientId, String mgiId) {
		final EventSubscription subscription = notificationSubscriptionStore.getSubscription(mgiId);
		if (subscription != null) {
			notificationSubscriptionStore.removeSubscriber(mgiId, clientId);
		}
	}

	public void processEventServiceNotifyMessage(String topic, JsonElement notificationContent) {
		if (Game.isOpenRegistrationTopic(topic)) {
			OpenRegistrationNotificationContent content = jsonUtil.fromJson(notificationContent.toString(), OpenRegistrationNotificationContent.class);
			createTournament(content);
		} else if (Game.isStartMultiplayerGameTopic(topic)) {
			LiveUserTournamentStartGameEventNotificationContent content = jsonUtil.fromJson(notificationContent,
					LiveUserTournamentStartGameEventNotificationContent.class);
			final String gameId = content.getGameId();
			final String mgiId = content.getMgiId();
			
			createTopicAndSchedule(gameId, mgiId);
        } else if (Game.isStartGameTopic(topic)) {
            StartGameEventNotificationContent content = jsonUtil.fromJson(notificationContent, StartGameEventNotificationContent.class);
			final String gameId = content.getGameId();
			final String mgiId = gameStartScheduler.getMultiplayerGameInstanceId(gameId);
            createTopicAndSchedule(gameId, mgiId);
		} else {
			throw new F4MFatalErrorException(String.format("Failed to process unrecognized topic [%s]", topic));
		}
	}

	private void createTopicAndSchedule(final String gameId, final String mgiId) {
		createLiveTournamentNotificationTopic(mgiId);
		gameStartScheduler.notifyGameRegisteredUsers(gameId, mgiId);
		gameStartScheduler.scheduleGameStartNotifications(mgiId);
		gameStartScheduler.schedulePlayerReadinessNotification(gameId, mgiId);
		gameStartScheduler.scheduleGameCleanUpIfNoParticipats(mgiId);
		gameStartScheduler.scheduleLiveTournamentStartingSoonNotifications(gameId, mgiId);
	}
	

	private void createLiveTournamentNotificationTopic(String mgiId) {
		notificationSubscriptionStore.setSubscriptionId(false, null, mgiId, 0L);
	}

	private void createTournament(OpenRegistrationNotificationContent content) {
		final String gameId = content.getGameId();
		final String tenantId = content.getTenantId();
		final String appId = content.getAppId();
		final ZonedDateTime repetition = content.getRepetition();

		if (ObjectUtils.allNotNull(gameId, tenantId, repetition)) {
            String mgiManagerTournamentInstanceId = mgiManager.getTournamentInstanceId(gameId);

			if (mgiManagerTournamentInstanceId == null && gameAerospikeDao.getMgiId(gameId) == null) {
				final Game game = gameAerospikeDao.getGame(gameId);
				final MultiplayerGameParameters multiplayerGameParameters = new MultiplayerGameParameters(gameId);

				if (DateTimeUtil.getCurrentDateTime().compareTo(game.getStartDateTime()) < 0) {
					multiplayerGameParameters.setStartDateTime(game.getStartDateTime());//set tournament visible}
				} else
					multiplayerGameParameters.setStartDateTime(DateTimeUtil.getCurrentDateTime());//set tournament visible}

				if (game.isLiveGame()) {
					multiplayerGameParameters.setPlayDateTime(repetition);
				} else if (game.getType().isTournament()) {
					final ZonedDateTime maxEndDateTime = DateTimeUtil.getCurrentDateTime()
							.plusHours(config.getPropertyAsInteger(GameConfigImpl.GAME_MAX_PLAY_TIME_IN_HOURS));

					if (game.getEndDateTime() == null) {
						multiplayerGameParameters.setEndDateTime(maxEndDateTime);
					} else {
						multiplayerGameParameters.setEndDateTime(game.getEndDateTime());
					}
				}
				ClientInfo clientInfo = new ClientInfo();
				clientInfo.setTenantId(tenantId);
				clientInfo.setAppId(appId);

				CustomGameConfig customGameConfig = mgiManager.createMultiplayerGameInstance(
						multiplayerGameParameters,
						clientInfo,
						null, null,
						false, true); //tournament marked as public

				String mgiId = customGameConfig != null ? customGameConfig.getId() : null;

				if (game.getType().isLive()) {
					if (content.getStartGameDateTime() == null) {
						throw new F4MValidationFailedException(
								String.format("start date/time is not defined [%s]", content));
					}

					String topic = Game.getStartMultiplayerGameTopic(mgiId);
					eventServiceClient.publish(topic, true,
							jsonUtil.toJsonElement(
									new StartLiveUserTournamentEvent(mgiId, multiplayerGameParameters.getPlayDateTime())),
							content.getStartGameDateTime());
				}

				mgiManager.mapTournament(gameId, mgiId);
			}
		} else {
			throw new F4MValidationFailedException(String.format("Failed to process notification [%s]", content));
		}
	}
	
}
