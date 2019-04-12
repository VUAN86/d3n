package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Request to send an mobile push to topic/tag.
 */
public class SendTopicPushRequest extends SendPushRequest implements JsonMessageContent {
	private String topic;

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendTopicPushRequest [topic=");
		builder.append(topic);
		builder.append(", ");
		contentsToString(builder);
		builder.append("]");
		return builder.toString();
	}
}