package de.ascendro.f4m.service.usermessage.model;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Response containing list of messages that are unread in user's device.
 */
public class ListUnreadWebsocketMessagesResponse implements JsonMessageContent {
	private List<MessageInfo> messages;

	public List<MessageInfo> getMessages() {
		return messages;
	}

	public void setMessages(List<MessageInfo> messages) {
		this.messages = messages;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ListUnreadWebsocketMessagesResponse [messages=");
		builder.append(messages);
		builder.append("]");
		return builder.toString();
	}
	
}
