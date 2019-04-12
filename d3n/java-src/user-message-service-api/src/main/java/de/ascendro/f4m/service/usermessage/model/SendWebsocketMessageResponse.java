package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Response in case of successfully sent direct message to single device of a user (first comes gets
 * it) via WebSocket.
 */
public class SendWebsocketMessageResponse implements JsonMessageContent {
	private String messageId;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendWebsocketMessageResponse [messageId=");
		builder.append(messageId);
		builder.append("]");
		return builder.toString();
	}
}
