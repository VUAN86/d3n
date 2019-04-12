package de.ascendro.f4m.service.usermessage.model;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Information about a direct message to be sent to user.
 */
public class MessageInfo implements JsonMessageContent {
	protected String messageId;
	protected String message;
	protected JsonElement payload;
	protected WebsocketMessageType type;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public JsonElement getPayload() {
		return payload;
	}

	public void setPayload(JsonElement payload) {
		this.payload = payload;
	}

	public WebsocketMessageType getType() {
		return type;
	}

	public void setType(WebsocketMessageType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MessageInfo [messageId=");
		builder.append(messageId);
		builder.append(", message=");
		builder.append(message);
		builder.append(", payload=");
		builder.append(payload);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}
}
