package de.ascendro.f4m.service.ping;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.ping.model.JsonPingMessageContent;
import de.ascendro.f4m.service.ping.model.JsonPongMessageContent;

public class PingPongMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = -4596263965647568339L;

	public PingPongMessageTypeMapper() {
		this.register(JsonPingMessageContent.MESSAGE_NAME, new TypeToken<JsonPingMessageContent>() {
		}.getType());
		this.register(JsonPongMessageContent.MESSAGE_NAME, new TypeToken<JsonPongMessageContent>() {
		}.getType());
	}
}
