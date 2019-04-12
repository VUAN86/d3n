package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Object containing message identifier for usage in JSON arrays.
 */
public class MessageIdInfo implements JsonMessageContent {
	private String messageId;
	
	public MessageIdInfo(String messageId) {
		this.setMessageId(messageId);
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MessageIdInfo [messageId=");
		builder.append(messageId);
		builder.append("]");
		return builder.toString();
	}
}
