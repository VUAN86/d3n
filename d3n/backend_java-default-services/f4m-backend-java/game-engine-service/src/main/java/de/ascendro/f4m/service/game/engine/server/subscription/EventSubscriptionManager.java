package de.ascendro.f4m.service.game.engine.server.subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.gson.JsonElement;
import de.ascendro.f4m.server.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.history.GameHistoryManager;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.end.EndGameRequest;
import de.ascendro.f4m.service.game.engine.model.start.step.StartStepRequest;
import de.ascendro.f4m.service.game.engine.server.MessageCoordinator;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.ServiceUtil;

public class EventSubscriptionManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriptionManager.class);
	
	private final EventServiceClient eventServiceClient;
	private final LiveTournamentEventSubscriptionStore liveTournamentEventSubscriptionStore;
	private final SessionPool sessionPool;

	private final ServiceUtil serviceUtil;

	private final MessageCoordinator messageCoordinator;

	private final GameInstanceAerospikeDao gameInstanceAerospikeDao;
	
	private final GameHistoryManager gameHistoryManager;

	@Inject
	public EventSubscriptionManager(EventServiceClient eventServiceClient,
			LiveTournamentEventSubscriptionStore startStepEventSubscriptionStore, SessionPool sessionPool,
			ServiceUtil serviceUtil, MessageCoordinator messageCoordinator, GameInstanceAerospikeDao gameInstanceAerospikeDao, 
			GameHistoryManager gameHistoryManager) {
		this.eventServiceClient = eventServiceClient;
		this.liveTournamentEventSubscriptionStore = startStepEventSubscriptionStore;
		this.sessionPool = sessionPool;
		this.serviceUtil = serviceUtil;
		this.messageCoordinator = messageCoordinator;
		this.gameInstanceAerospikeDao = gameInstanceAerospikeDao;
		this.gameHistoryManager = gameHistoryManager;
	}

	public void subscribeClientGameInstanceId(String clientId, GameInstance gameInstance) {
		final Game game = gameInstance.getGame();
		if(game.getType().isLive()){
			String mgiId = gameInstance.getMgiId();
			final String topic = Game.getLiveTournamentStartStepTopic(mgiId);
			subscribeToTopic(topic, clientId, gameInstance);
			final String endGameTopic = Game.getLiveTournamentEndGameTopic(mgiId);
			subscribeToTopic(endGameTopic, clientId, gameInstance);
		}
	}

	private void subscribeToTopic(final String topic, String clientId, GameInstance gameInstance) {
		if (!liveTournamentEventSubscriptionStore.hasSubscription(topic)) {
			eventServiceClient.subscribe(false, null, topic);
		}
		liveTournamentEventSubscriptionStore.addSubscriber(false, null, topic, clientId, gameInstance.getId());
	}

	public void notifyStep(String topic, boolean endGame) {
		LOGGER.debug("start notify step for topic [{}] with endGame [{}]", topic, endGame);
		final LiveTournamentEventSubscription subscription = liveTournamentEventSubscriptionStore.getSubscription(topic);
		
		final List<SessionWrapper> gatewaySessionCache = new ArrayList<>(2);
		boolean gameCompleted = true;
		for (String clientId : subscription.getClients()) {
			final SessionWrapper gatewaySession = getClientSessionWithGateway(clientId, gatewaySessionCache);
			final String gameInstanceId = subscription.getClientGameInstanceId(clientId);
			LOGGER.debug("notify step for client [{}] on topic [{}] with endGame [{}] with game instance [{}] and session [{}]", clientId, topic, endGame, gameInstanceId, gatewaySession);

			if (gameInstanceId != null) {
				try {
					if(endGame) {
						GameInstance gameInstance = gameInstanceAerospikeDao.getGameInstance(gameInstanceId);
						ClientInfo clientInfo = gameHistoryManager.buildClientInfo(gameInstance);
						if (gatewaySession != null) {
							clientInfo.setClientId(clientId);
						}
						messageCoordinator.forceGameEnd(gatewaySession, gameInstanceId, clientInfo);
					} else {
						final GameInstance resultGameInstance = messageCoordinator.performNextGameStepOrQuestion(
								gatewaySession, clientId, gameInstanceId, serviceUtil.getMessageTimestamp(), false);
						if(gameCompleted){
							gameCompleted = !resultGameInstance.hasAnyStepOrQuestion();
						}
					}

				} catch (F4MException e) {
					//log and continue with other subscribers for other game instances on this topic
					LOGGER.error(
							"Error processing topic [{}] for subscriptionId [{}], clientId [{}], gameInstanceId [{}]",
							topic, subscription.getSubscriptionId(), clientId, gameInstanceId, e);
				}
			} else {
				LOGGER.warn("Client [{}] has subscribed to [{}], but there is no gameInstance registered", clientId, endGame ? StartStepRequest.class : EndGameRequest.class);
				assert false : "No game instance for client " + clientId;
			}
		}
		if(endGame) {
			LOGGER.debug("Request unsubscribe from event topic [{}] and endGame [{}] with subscription [{}]", topic, endGame, subscription.getSubscriptionId());
			eventServiceClient.unsubscribe(subscription.getSubscriptionId(), topic);
		} else {
			if (gameCompleted) {
				LOGGER.debug("Request unsubscribe from event topic [{}] with subscription [{}] as game completed", topic, subscription.getSubscriptionId());
				eventServiceClient.unsubscribe(subscription.getSubscriptionId(), topic);
			}
		}
	}


	public void updateSubscriptionIdForTopic(boolean virtual, String consumerName, String topic, long subscriptionId) {
		liveTournamentEventSubscriptionStore.setSubscriptionId(virtual, consumerName, topic, subscriptionId);
	}
	
	private SessionWrapper getClientSessionWithGateway(String clientId, List<SessionWrapper> gatewaySessionCache){
		final Optional<SessionWrapper> cachedSession = gatewaySessionCache.stream()
			.filter(s -> s.getSessionStore().hasClient(clientId))
			.findAny();	
		final SessionWrapper gatewaySession;
		if(cachedSession.isPresent()){
			gatewaySession = cachedSession.get();
		}else{
			gatewaySession = sessionPool.getSessionByClientIdServiceName(clientId, GatewayMessageTypes.SERVICE_NAME);
			if (gatewaySession != null) { // in case the client is disconnected during the game --> session is null
				gatewaySessionCache.add(gatewaySession);
			}
		}
		return gatewaySession;
	}
}
