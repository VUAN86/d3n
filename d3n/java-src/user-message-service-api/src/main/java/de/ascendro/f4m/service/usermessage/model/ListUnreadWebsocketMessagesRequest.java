package de.ascendro.f4m.service.usermessage.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Request to get a list of unread messages in specified user's device.
 */
public class ListUnreadWebsocketMessagesRequest implements JsonMessageContent {
	private String userId;
	private String deviceUUID;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDeviceUUID() {
		return deviceUUID;
	}

	public void setDeviceUUID(String deviceId) {
		this.deviceUUID = deviceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ListUnreadWebsocketMessagesRequest [userId=");
		builder.append(userId);
		builder.append(", deviceUUID=");
		builder.append(deviceUUID);
		builder.append("]");
		return builder.toString();
	}
}
