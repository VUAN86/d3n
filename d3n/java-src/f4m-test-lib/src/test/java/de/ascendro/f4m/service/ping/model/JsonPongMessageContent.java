package de.ascendro.f4m.service.ping.model;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class JsonPongMessageContent implements JsonMessageContent {
	public static final String MESSAGE_NAME = JsonMessage.getResponseMessageName(JsonPingMessageContent.MESSAGE_NAME);
	private String pong;

	public JsonPongMessageContent() {
	}

	public JsonPongMessageContent(String pong) {
		this.pong = pong;
	}

	public void setPong(String pong) {
		this.pong = pong;
	}

	public String getPong() {
		return pong;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonPongMessageContent [pong=");
		builder.append(pong);
		builder.append("]");
		return builder.toString();
	}
}
