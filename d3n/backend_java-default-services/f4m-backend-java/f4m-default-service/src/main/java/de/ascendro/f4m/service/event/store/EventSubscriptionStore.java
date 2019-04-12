package de.ascendro.f4m.service.event.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.cache.CacheManager;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.logging.LoggingUtil;

public class EventSubscriptionStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriptionStore.class);

	private final CacheManager<String, EventSubscription> subscriptions;
	private final EventSubscriptionCreator eventSubscriptionCreator;

	@Inject
	public EventSubscriptionStore(Config config, EventSubscriptionCreator eventSubscriptionCreator, LoggingUtil loggingUtil) {
		final long cleanUpInterval = config.getPropertyAsLong(F4MConfigImpl.SUBSCRIPTION_STORE_CLEAN_UP_INTERVAL);

		this.subscriptions = new CacheManager<>(cleanUpInterval, loggingUtil);
		this.eventSubscriptionCreator = eventSubscriptionCreator;
	}

	public void unregisterSubscription(String topic) {
		EventSubscription removedSubscription = subscriptions.remove(topic);
		if (removedSubscription == null) {
			LOGGER.error("Failed to unregister subscription on topic [{}] - subscription doesn't exist", topic);
		}
	}

	public void addSubscriber(boolean virtual, String consumerName, String topic, String clientId) {
		EventSubscription subscription = subscriptions.getAndRefresh(topic);
		if (subscription == null) {
			subscription = eventSubscriptionCreator.createEventSubscription(virtual, consumerName, topic);
			subscriptions.put(topic, subscription);
		}
		subscription.addClient(clientId);
	}
	
	public boolean hasSubscription(String topic){
		return subscriptions.contains(topic);
	}

	public void removeSubscriber(String topic, String clientId) {
		EventSubscription subscription = subscriptions.getAndRefresh(topic);
		if (subscription != null) {
			subscription.removeClient(clientId);
		} else {
			LOGGER.error("Failed to remove subscriber from topic [{}] - subscription doesn't exist", topic);
		}
	}

	public Set<String> getSubscribers(String topic) {
		Set<String> subscribers;
		EventSubscription subcription = subscriptions.getAndRefresh(topic);
		if (subcription != null) {
			subscribers = subcription.getClients();
		} else {
			subscribers = Collections.emptySet();
		}
		return subscribers;
	}

	public EventSubscription getSubscription(String topic) {
		return subscriptions.getAndRefresh(topic);
	}

	public Long getSubscriptionId(String topic) {
		final Long subscriptionId;

		EventSubscription subscription = subscriptions.getAndRefresh(topic);
		if (subscription != null) {
			subscriptionId = subscription.getSubscriptionId();
		} else {
			subscriptionId = null;
		}

		return subscriptionId;
	}

	public void setSubscriptionId(boolean virtual, String consumerName, String topic, long subscriptionId) {
		EventSubscription subscription = subscriptions.getAndRefresh(topic);
		if (subscription == null) {
			subscription = eventSubscriptionCreator.createEventSubscription(virtual, consumerName, topic);
			subscriptions.put(topic, subscription);
		}
		subscription.setSubscriptionId(subscriptionId);
	}

	public Collection<EventSubscription> getSubscriptions() {
		return subscriptions.getValues();
	}

	@PreDestroy
	public void shutdown() {
		subscriptions.destroy();
	}
}
