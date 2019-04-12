package de.ascendro.f4m.service.ping.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class JsonPingMessageContent implements JsonMessageContent {
	public static final String MESSAGE_NAME = "test/ping";
	public static final String MESSAGE_WITH_PERMISSIONS_NAME = "test/pingWithPermissions";
	private String ping;

	public JsonPingMessageContent() {
	}

	public JsonPingMessageContent(String ping) {
		this.ping = ping;
	}

	public String getPing() {
		return ping;
	}

	public void setPing(String ping) {
		this.ping = ping;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonPingMessageContent [ping=");
		builder.append(ping);
		builder.append("]");
		return builder.toString();
	}
}
