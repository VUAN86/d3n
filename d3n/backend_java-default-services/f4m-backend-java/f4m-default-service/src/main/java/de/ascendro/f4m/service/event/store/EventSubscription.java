package de.ascendro.f4m.service.event.store;

import java.util.HashSet;
import java.util.Set;

import de.ascendro.f4m.service.cache.CachedObject;

public class EventSubscription extends CachedObject {

	private final String topic;
	private Long subscriptionId;
	private final Set<String> clients;
	private String consumerName;
	private boolean virtual;
	
	public EventSubscription(boolean virtual, String consumerName, String topic) {
		this.topic = topic;
		this.clients = new HashSet<>();
		this.consumerName = consumerName;
		this.virtual = virtual;
	}

	public String getTopic() {
		return topic;
	}

	public Long getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(long subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public Set<String> getClients() {
		return clients;
	}

	public void addClient(String clientId) {
		clients.add(clientId);
	}

	public boolean removeClient(String clientId) {
		return clients.remove(clientId);
	}
	
	public String getConsumerName() {
		return consumerName;
	}
	
	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}
	
	public boolean isVirtual() {
		return virtual;
	}
	
	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}
	
}
