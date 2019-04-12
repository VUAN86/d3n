package de.ascendro.f4m.service.usermessage.model;

import java.util.Arrays;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.usermessage.translation.MessageWithParams;

public abstract class SendPushRequest implements MessageWithParams {
	private String message;
	private String[] parameters;
	private String[] appIds;
	private JsonElement payload;
	private WebsocketMessageType type;

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String[] getParameters() {
		return parameters;
	}

	public void setParameters(String[] parameters) {
		this.parameters = parameters;
	}

	public String[] getAppIds() {
		return appIds;
	}

	public void setAppIds(String[] appIds) {
		this.appIds = appIds;
	}

	public WebsocketMessageType getType() {
		return type;
	}

	public void setType(WebsocketMessageType type) {
		this.type = type;
	}

	public JsonElement getPayload() {
		return payload;
	}

	public void setPayload(JsonElement payload) {
		this.payload = payload;
	}

	protected void contentsToString(StringBuilder builder) {
		builder.append("message=");
		builder.append(message);
		builder.append(", parameters=");
		builder.append(Arrays.toString(parameters));
		builder.append(", appIds=");
		builder.append(Arrays.toString(appIds));
		builder.append(", type=");
		builder.append(type);
		builder.append(", payload=");
		builder.append(payload);
	}
}
