package de.ascendro.f4m.service.event.model.publish;

import java.time.ZonedDateTime;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PublishMessageContent implements JsonMessageContent {
	private String topic;

	@SerializedName("notification-content")
	private JsonElement notificationContent;

	private boolean virtual;
	
	private ZonedDateTime publishDate;
	
	public PublishMessageContent() {
	}

	public PublishMessageContent(String topic) {
		this(topic, false);
	}

	public PublishMessageContent(String topic, boolean virtual) {
		setTopic(topic);
		setVirtual(virtual);
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

	public boolean isVirtual() {
		return virtual;
	}

	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	public ZonedDateTime getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(ZonedDateTime publishDate) {
		this.publishDate = publishDate;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PublishMessageContent [topic=");
		builder.append(topic);
		builder.append(", notificationContent=");
		builder.append(notificationContent);
		builder.append(", virtual=");
		builder.append(virtual);
		builder.append(", publishDate=");
		builder.append(publishDate);
		builder.append("]");
		return builder.toString();
	}
}
