package de.ascendro.f4m.service.util;

import java.time.ZonedDateTime;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeSubscriptionsRequest;

public interface EventServiceClient {

	void publish(String topic, JsonElement notificationContent, ZonedDateTime publishDateTime);

	void publish(String topic, boolean virtual, JsonElement notificationContent, ZonedDateTime publishDateTime);

	void publish(String topic, JsonElement notificationContent);

	void publish(String topic, boolean virtual, JsonElement notificationContent);

	void subscribe(boolean virtual, String consumerName, String topic);

	void unsubscribe(long subscription, String topic);

	void resubscribe(ResubscribeSubscriptionsRequest... subscriptions);

	void resubscribe();

	void notifySubscriber(long subscription, String topic, JsonElement notificationContent);

	/**
	 * Method to be invoked when event service is connected in order to immediately proceed with event message sending from local queue.
	 */
	void signalThatEventServiceConnected();

}