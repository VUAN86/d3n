package de.ascendro.f4m.service.game.engine.server.subscription;

import java.util.concurrent.ConcurrentHashMap;

import de.ascendro.f4m.service.event.store.EventSubscription;

public class LiveTournamentEventSubscription extends EventSubscription {
	private final ConcurrentHashMap<String, String> clientGameInstanceIds = new ConcurrentHashMap<>();

	public LiveTournamentEventSubscription(boolean virtual, String consumerName, String topic) {
		super(virtual, consumerName, topic);
	}

	public void addClientGameInstanceId(String clientId, String gameInstanceId) {
		clientGameInstanceIds.put(clientId, gameInstanceId);	
	}

	public String getClientGameInstanceId(String clientId) {
		return clientGameInstanceIds.get(clientId);
	}
}
