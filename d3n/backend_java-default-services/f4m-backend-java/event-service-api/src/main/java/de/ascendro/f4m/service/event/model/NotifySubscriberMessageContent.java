package de.ascendro.f4m.service.event.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class NotifySubscriberMessageContent implements JsonMessageContent {
	private long subscription;
	private String topic;

	@SerializedName("notification-content")
	private JsonElement notificationContent;

	public NotifySubscriberMessageContent() {
	}

	public NotifySubscriberMessageContent(long subscription, String topic) {
		this.subscription = subscription;
		this.topic = topic;
	}

	public long getSubscription() {
		return subscription;
	}

	public void setSubscription(long subscription) {
		this.subscription = subscription;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public JsonElement getNotificationContent() {
		return notificationContent;
	}

	public void setNotificationContent(JsonElement notificationContent) {
		this.notificationContent = notificationContent;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NotifySubscriberMessageContent [subscription=");
		builder.append(subscription);
		builder.append(", topic=");
		builder.append(topic);
		builder.append(", notificationContent=");
		builder.append(notificationContent);
		builder.append("]");
		return builder.toString();
	}
	
}
