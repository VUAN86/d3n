package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class NewWebsocketMessageRequest extends MessageInfo {
	
	@JsonRequiredNullable
	private String userId;
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NewWebsocketMessageRequest [userId=");
		builder.append(userId);
		builder.append(", messageId=");
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
