package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Response in case of successfully sending an email to user.
 */
public class SendEmailWrapperResponse implements JsonMessageContent {
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
		builder.append("SendEmailResponse [messageId=");
		builder.append(messageId);
		builder.append("]");
		return builder.toString();
	}
}
