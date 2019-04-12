package de.ascendro.f4m.service.usermessage.model;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.usermessage.translation.TranslatableMessage;

/**
 * Request to send a direct message to single device of a user (first comes gets
 * it) via WebSocket.
 */
public class SendWebsocketMessageRequest extends TranslatableMessage implements MessageToUserIdRequest {
	private String userId;
	private Integer timeout;
	private JsonElement payload;
	private WebsocketMessageType type;
	
	public SendWebsocketMessageRequest() {
		//default constructor
	}

	public SendWebsocketMessageRequest(boolean languageAuto) {
		if (languageAuto) {
			this.setLanguageAuto();
		}
	}

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
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
	protected void contentsToString(StringBuilder builder) {
		builder.append("userId=");
		builder.append(userId);
		builder.append(", timeout=");
		builder.append(timeout);
		builder.append(", payload=");
		builder.append(payload);
		builder.append(", type=");
		builder.append(type);
		builder.append(", ");
		super.contentsToString(builder);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendWebsocketMessageRequest [");
		contentsToString(builder);
		builder.append("]");
		return builder.toString();
	}
}
