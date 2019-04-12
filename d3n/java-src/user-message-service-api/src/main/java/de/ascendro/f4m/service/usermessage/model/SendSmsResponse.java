package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Response in case of successful sending an SMS to a user.
 */
public class SendSmsResponse implements JsonMessageContent {
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
		builder.append("SendSmsResponse [messageId=");
		builder.append(messageId);
		builder.append("]");
		return builder.toString();
	}
}
