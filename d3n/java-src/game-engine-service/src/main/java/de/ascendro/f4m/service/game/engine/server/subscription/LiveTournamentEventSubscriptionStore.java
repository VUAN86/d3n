package de.ascendro.f4m.service.game.engine.server.subscription;

import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.store.EventSubscriptionCreator;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class LiveTournamentEventSubscriptionStore extends EventSubscriptionStore {

	@Inject
	public LiveTournamentEventSubscriptionStore(Config config, EventSubscriptionCreator eventSubscriptionCreator, LoggingUtil loggingUtil) {
		super(config, eventSubscriptionCreator, loggingUtil);
	}

	public void addSubscriber(boolean virtual, String consumerName, String topic, String clientId, String gameInstanceId) {
		super.addSubscriber(virtual, consumerName, topic, clientId);
		getSubscription(topic).addClientGameInstanceId(clientId, gameInstanceId);
	}

	@Override
	public LiveTournamentEventSubscription getSubscription(String topic) {
		return (LiveTournamentEventSubscription) super.getSubscription(topic);
	}
}
